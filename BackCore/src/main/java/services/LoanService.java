package services;

import entities.ClientEntity;
import entities.LoanEntity;
import entities.ToolEntity;
import entities.UserEntity;
import entities.enums.ClientStatus;
import entities.enums.LoanStatus;
import entities.enums.ToolStatus;
import repositories.LoanRepository;
import repositories.ClientRepository;
import repositories.ToolRepository;
import app.exceptions.InvalidOperationException; 
import app.exceptions.ResourceNotFoundException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LoanService {

    private final LoanRepository loanRepository;
    private final ClientRepository clientRepository;
    private final ToolRepository toolRepository;
    private final ToolService toolService;
    private final TariffService tariffService;
    private final ClientService clientService;

    // --- Constructor ---
    public LoanService(LoanRepository loanRepository,
                       ClientRepository clientRepository,
                       ToolRepository toolRepository,
                       ToolService toolService,
                       TariffService tariffService,
                       ClientService clientService) {
        this.loanRepository = loanRepository;
        this.clientRepository = clientRepository;
        this.toolRepository = toolRepository;
        this.toolService = toolService;
        this.tariffService = tariffService;
        this.clientService = clientService;
    }

    // ########################################################################################################################################################
    // ####################################################### LOANS ##########################################################################################
    // ########################################################################################################################################################
    // RF2.1 / RF2.2: register a loan after validating every business rule
    @Transactional
    public LoanEntity createLoan(Long clientId, Long toolId, LocalDate startDate, LocalDate dueDate, UserEntity user) {
        // 0. Load client and tool
        ClientEntity client = clientRepository.findById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Client not found with id: " + clientId));
        ToolEntity tool = toolRepository.findById(toolId)
                .orElseThrow(() -> new ResourceNotFoundException("Tool not found with id: " + toolId));

        //  ############################################# Business rules #############################################
        // 1. Client must be ACTIVE (not restricted)
        if (client.getStatus() == ClientStatus.RESTRICTED) {
            throw new InvalidOperationException("Client is restricted and cannot request loans.");
        }

        // 2. No LATE loans
        long lateLoanCount = loanRepository.countByClientAndStatus(client, LoanStatus.LATE);
        if (lateLoanCount > 0) {
            throw new InvalidOperationException("Client has " + lateLoanCount + " late loan(s) that must be returned.");
        }

        // 3. No unpaid debts (RECEIVED loans with totalPenalty > 0)
        List<LoanEntity> unpaidReceivedLoans = loanRepository.findByClientAndStatusAndTotalPenaltyGreaterThan(client, LoanStatus.RECEIVED, 0.0);
        if (!unpaidReceivedLoans.isEmpty()) {
            throw new InvalidOperationException("Client has outstanding payments due for " + unpaidReceivedLoans.size() + " previous loan(s).");
        }

        // 4. Tool must be AVAILABLE with stock >= 1
        if (tool.getStatus() != ToolStatus.AVAILABLE || tool.getStock() == null || tool.getStock() <= 0) {
            throw new InvalidOperationException("Tool is not available or out of stock.");
        }

        // 5. Valid dates: the due date cannot be before the start date
        if (startDate == null) startDate = LocalDate.now();
        if (dueDate == null) throw new IllegalArgumentException("dueDate is required.");
        if (dueDate.isBefore(startDate)) {
            throw new IllegalArgumentException("Due date cannot be before start date.");
        }

        // 5b. Overdue loans not yet flagged as LATE by the scheduler also block new loans
        List<LoanEntity> activeLoans = loanRepository.findByClientAndStatus(client, LoanStatus.ACTIVE);
        LocalDate today = LocalDate.now();
        boolean hasOverdueLoans = activeLoans.stream()
                .anyMatch(l -> l.getDueDate() != null && l.getDueDate().isBefore(today));
        if (hasOverdueLoans) {
            throw new InvalidOperationException("Client has overdue loan(s) that must be returned.");
        }

        // 6. At most 5 open loans (ACTIVE + LATE)
        long activeOrLateCount = activeLoans.size() + lateLoanCount;
        if (activeOrLateCount >= 5) {
            throw new InvalidOperationException("Client has reached the maximum number of active/late loans (5).");
        }

        // 7. Not the same tool twice at the same time.
        // Only ACTIVE loans need checking: rule 2 already rejected clients with any LATE loan.
        boolean hasSameToolOnLoan = activeLoans.stream()
                .anyMatch(l -> l.getTool() != null && l.getTool().getId().equals(toolId));
        if (hasSameToolOnLoan) {
            throw new InvalidOperationException("Client already has an active or late loan for this tool.");
        }

        // --- Create and save the loan ---
        LoanEntity loan = LoanEntity.builder()
                .client(client)
                .tool(tool)
                .startDate(startDate)
                .dueDate(dueDate)
                .status(LoanStatus.ACTIVE)
                .totalPenalty(0.0)
                .loanedBy(user)
                .loanedAt(LocalDateTime.now())
                .build();

        toolService.decrementStockForLoan(tool, user);
        return loanRepository.save(loan);
    }

    // ########################################################################################################################################################
    // ####################################################### RETURNS ########################################################################################
    // ########################################################################################################################################################
    // RF2.3 / RF2.4: register a return and compute the amount due
    @Transactional
    public LoanEntity returnLoan(Long loanId, Long toolId, boolean damaged, boolean irreparable, UserEntity user, LocalDate returnDate) {
        // 0. Load the loan
        LoanEntity loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new ResourceNotFoundException("Loan not found with id: " + loanId));

        // 1. The returned tool must be the one that was lent
        if (!loan.getTool().getId().equals(toolId)) {
            throw new IllegalArgumentException("Tool ID (" + toolId + ") does not match the tool ID in the loan (" + loan.getTool().getId() + ").");
        }
        ToolEntity tool = loan.getTool(); // the tool comes from the loan itself

        // --- Only open loans can be returned ---
        if (loan.getStatus() != LoanStatus.ACTIVE && loan.getStatus() != LoanStatus.LATE) {
            throw new InvalidOperationException("Loan is already closed and cannot be returned again.");
        }

        // --- Validate the return date ---
        if (returnDate == null) returnDate = LocalDate.now();
        if (returnDate.isBefore(loan.getStartDate())) {
            throw new IllegalArgumentException("Return date cannot be before the loan start date.");
        }

        // ----------------------------------------------------------------------------------------------------------------------------------------------------
        // --- Rental cost (Epic 4 / rules of Epic 2) ---
        long rentalDays = ChronoUnit.DAYS.between(loan.getStartDate(), loan.getDueDate());
        // Rule: the minimum charge is always 1 day
        if (rentalDays < 1) {
            rentalDays = 1;
        }
        // Daily rental fee configured by the administrator
        double rentalCost = rentalDays * tariffService.getDailyRentFee();
        
        // ----------------------------------------------------------------------------------------------------------------------------------------------------
        // --- Late fee = days late x daily late fee (Epics 2 / 4) ---
        long delayDays = ChronoUnit.DAYS.between(loan.getDueDate(), returnDate);
        double lateFee = 0.0;
        if (delayDays > 0) {
            lateFee = delayDays * tariffService.getDailyLateFee();
        }

        // ----------------------------------------------------------------------------------------------------------------------------------------------------
        // --- Damage penalties (Epics 1 / 2 / 4) ---
        double damagePenalty = 0.0;
        if (damaged) {
            if (irreparable) {
                // Irreparable: the client pays the replacement value
                damagePenalty = tool.getReplacementValue();
                toolService.markAsDecommissioned(tool, user); // registers DECOMMISSION in the Kardex
            } else {
                // Light damage: the client pays the repair fee
                damagePenalty = tariffService.getRepairFee();
                toolService.markAsRepairing(tool, user); // registers REPAIR in the Kardex
            }
        } else {
            // Good condition: the unit goes back to stock
            toolService.incrementStockForReturn(tool, user); // registers RETURN in the Kardex
        }

        // ----------------------------------------------------------------------------------------------------------------------------------------------------
        // --- Total amount due ---
        boolean ON_TIME = returnDate.isBefore(loan.getDueDate()) || returnDate.isEqual(loan.getDueDate());
        boolean DELAYED = returnDate.isAfter(loan.getDueDate());
        
        if (ON_TIME) {
            double totalAmountDue = rentalCost + damagePenalty;
            loan.setTotalPenalty(totalAmountDue);
        } else if (DELAYED) {
            double totalAmountDue = rentalCost + lateFee + damagePenalty;
            loan.setTotalPenalty(totalAmountDue);
        }

        loan.setReturnDate(returnDate);
        loan.setReturnedBy(user);
        loan.setReturnedAt(LocalDateTime.now());
        loan.setStatus(LoanStatus.RECEIVED); // tool received, payment pending

        // --- Save the loan ---
        LoanEntity savedLoan = loanRepository.save(loan);
        clientService.updateStatus(loan.getClient().getId(), ClientStatus.RESTRICTED); // the client stays RESTRICTED until paying

        // Return the updated loan
        return savedLoan;
    }

    // ########################################################################################################################################################
    // ####################################################### OTHER ########################################################################################
    // ########################################################################################################################################################
    // --------------------------------------------------------------------------------------------------------------------------------------------------------
    // --- Mark a received loan as paid (it does NOT reactivate the client) ---
    @Transactional
    public LoanEntity markLoanAsPaid(Long loanId) {
        // 1. Load the loan
        LoanEntity loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new ResourceNotFoundException("Loan not found with id: " + loanId));

        // 2. Only RECEIVED loans have a pending payment
        if (loan.getStatus() != LoanStatus.RECEIVED) {
            throw new InvalidOperationException("Only received loans can be marked as paid. Current status: " + loan.getStatus());
        }

        // 3. Mark as paid (penalty 0) and CLOSE the loan
        loan.setTotalPenalty(0.0);
        loan.setStatus(LoanStatus.CLOSED); // <- important: becomes CLOSED
        LoanEntity savedLoan = loanRepository.save(loan);

        // 4. Reactivation is a separate, explicit action (ClientService.attemptClientReactivation)
        return savedLoan;
    }


    @Transactional(readOnly = true)
        public List<LoanEntity> getUnpaidReceivedLoansByClientId(Long clientId) {
        // 1. The client must exist
        ClientEntity client = clientRepository.findById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Client not found with id: " + clientId));

        // 2. RECEIVED loans with penalty > 0 = unpaid debts
        return loanRepository.findByClientAndStatusAndTotalPenaltyGreaterThan(
                client,
                LoanStatus.RECEIVED,
                0.0 // penalty threshold
        );
    }

    @Transactional(readOnly = true)
    public LoanEntity getLoanById(Long loanId) {
        return loanRepository.findById(loanId)
                .orElseThrow(() -> new ResourceNotFoundException("Loan not found with id: " + loanId));
    }

    // Open loans = tool still in the client's hands (ACTIVE or LATE)
    @Transactional(readOnly = true)
    public List<LoanEntity> getOpenLoans() {
        return loanRepository.findByStatusIn(List.of(LoanStatus.ACTIVE, LoanStatus.LATE));
    }

    // RF2.5 / RF3.2: ACTIVE loans past their due date become LATE and their clients are restricted.
    // Runs at startup and then every hour (idempotent, safe to run on every backend replica).
    @Scheduled(initialDelay = 10_000, fixedRate = 3_600_000)
    @Transactional
    public int markOverdueLoansAsLate() {
        List<LoanEntity> overdueLoans = loanRepository.findByStatusAndDueDateBefore(LoanStatus.ACTIVE, LocalDate.now());
        for (LoanEntity loan : overdueLoans) {
            loan.setStatus(LoanStatus.LATE);
            loanRepository.save(loan);
            if (loan.getClient().getStatus() != ClientStatus.RESTRICTED) {
                clientService.updateStatus(loan.getClient().getId(), ClientStatus.RESTRICTED);
                loan.getClient().setStatus(ClientStatus.RESTRICTED);
            }
        }
        return overdueLoans.size();
    }
}