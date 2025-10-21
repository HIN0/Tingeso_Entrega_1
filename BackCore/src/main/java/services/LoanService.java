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
import java.time.temporal.ChronoUnit;
import java.util.List;
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
    private final KardexService kardexService;

    // --- Constructor ---
    public LoanService(LoanRepository loanRepository,
                       ClientRepository clientRepository,
                       ToolRepository toolRepository,
                       ToolService toolService,
                       KardexService kardexService,
                       TariffService tariffService,
                       ClientService clientService) {
        this.loanRepository = loanRepository;
        this.clientRepository = clientRepository;
        this.toolRepository = toolRepository;
        this.toolService = toolService;
        this.kardexService = kardexService;
        this.tariffService = tariffService;
        this.clientService = clientService;
    }

    @Transactional
    public LoanEntity createLoan(Long clientId, Long toolId, LocalDate dueDate, UserEntity user) {
        return createLoan(clientId, toolId, LocalDate.now(), dueDate, user);
    }

    @Transactional
    public LoanEntity createLoan(Long clientId, Long toolId, LocalDate startDate, LocalDate dueDate, UserEntity user) {
        ClientEntity client = clientRepository.findById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Client not found with id: " + clientId)); 
        ToolEntity tool = toolRepository.findById(toolId)
                .orElseThrow(() -> new ResourceNotFoundException("Tool not found with id: " + toolId));

        if (client.getStatus() == ClientStatus.RESTRICTED) {
            throw new InvalidOperationException("Client is restricted and cannot request loans.");
        }

        if (tool.getStatus() != ToolStatus.AVAILABLE || tool.getStock() == null || tool.getStock() <= 0) {
            throw new InvalidOperationException("Tool is not available or out of stock.");
        }
        if (startDate == null) startDate = LocalDate.now();
        if (dueDate == null) throw new IllegalArgumentException("dueDate is required.");
        if (dueDate.isBefore(startDate)) {
            throw new IllegalArgumentException("Due date cannot be before start date.");
        }

        // Validación Límite 5 Préstamos (RN Épica 2)
        long activeCount = loanRepository.findByClientAndStatus(client, LoanStatus.ACTIVE).size()
                         + loanRepository.findByClientAndStatus(client, LoanStatus.LATE).size(); // Incluir LATE en el conteo
        if (activeCount >= 5) {
            throw new InvalidOperationException("Client has reached the maximum number of active/late loans (5)."); // Usa excepción personalizada
        }

        // Validación No Repetir Herramienta Activa (RN Épica 2)
        boolean hasSameToolActive = loanRepository.findByClientAndStatus(client, LoanStatus.ACTIVE).stream()
                .anyMatch(l -> l.getTool() != null && l.getTool().getId().equals(toolId))
                ||
                loanRepository.findByClientAndStatus(client, LoanStatus.LATE).stream()
                .anyMatch(l -> l.getTool() != null && l.getTool().getId().equals(toolId));
        if (hasSameToolActive) {
            throw new InvalidOperationException("Client already has an active or late loan for this tool."); // Usa excepción personalizada
        }

        LoanEntity loan = LoanEntity.builder()
                .client(client)
                .tool(tool)
                .startDate(startDate)
                .dueDate(dueDate)
                .status(LoanStatus.ACTIVE)
                .totalPenalty(0.0) // Penalidad inicial es 0
                .build();

        toolService.decrementStockForLoan(tool, user); // Esto ya registra LOAN en Kardex
        return loanRepository.save(loan);
    }

    // --- Método returnLoan ACTUALIZADO ---

    @Transactional
    public LoanEntity returnLoan(Long loanId, Long toolId, boolean damaged, boolean irreparable, UserEntity user) {
        return returnLoan(loanId, toolId, damaged, irreparable, user, LocalDate.now());
    }

    @Transactional
    public LoanEntity returnLoan(Long loanId, Long toolId, boolean damaged, boolean irreparable,
                                UserEntity user, LocalDate returnDate) {

        // --- Obtener entidades ---
        LoanEntity loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new ResourceNotFoundException("Loan not found with id: " + loanId)); // Excepción personalizada
        // Verificar que el toolId recibido coincida con el del préstamo original
        if (!loan.getTool().getId().equals(toolId)) {
            throw new IllegalArgumentException("Tool ID (" + toolId + ") does not match the tool ID in the loan (" + loan.getTool().getId() + ").");
        }
        ToolEntity tool = loan.getTool(); // Ya tenemos la herramienta desde el préstamo

        // --- Validar estado del préstamo ---
        // Excepción personalizada
        if (loan.getStatus() != LoanStatus.ACTIVE && loan.getStatus() != LoanStatus.LATE) {
            throw new InvalidOperationException("Loan is already closed and cannot be returned again.");
        }

        // --- Validar fecha de devolución ---
        if (returnDate == null) returnDate = LocalDate.now();
        // Excepción personalizada (o IllegalArgumentException)
        if (returnDate.isBefore(loan.getStartDate())) {
            throw new IllegalArgumentException("Return date cannot be before the loan start date.");
        }

        // --- Calcular Costo de Arriendo (ÉPICA 4 / RN Épica 2) ---
        long rentalDays = ChronoUnit.DAYS.between(loan.getStartDate(), returnDate);
        // RN: tarifa mínima siempre es 1 día
        if (rentalDays < 1) {
            rentalDays = 1;
        }
        // Obtener la tarifa diaria de arriendo (Necesita método en TariffService)
        double rentalCost = rentalDays * tariffService.getDailyRentFee(); // <- Uso getDailyRentFee()

        // --- Calcular Multa por Atraso (ÉPICA 2 / 4) ---
        long delayDays = ChronoUnit.DAYS.between(loan.getDueDate(), returnDate);
        double lateFee = 0.0;
        if (delayDays > 0) {
            lateFee = delayDays * tariffService.getDailyLateFee();
        }

        // --- Calcular Penalidades por Daño (ÉPICA 1 / 2 / 4) ---
        double damagePenalty = 0.0;
        if (damaged) {
            if (irreparable) {
                // Baja definitiva: cobrar reposición
                damagePenalty = tool.getReplacementValue();
                toolService.markAsDecommissioned(tool, user); // Esto ya registra DECOMMISSION en Kardex
            } else {
                // Reparación: cobrar tarifa de reparación
                damagePenalty = tariffService.getRepairFee();
                toolService.markAsRepairing(tool, user); // Esto ya registra REPAIR en Kardex y ajusta stock
            }
        } else {
            // Devuelta en buen estado: vuelve a stock
            toolService.incrementStockForReturn(tool, user); // Esto ya registra RETURN en Kardex
        }

        // --- Calcular Total a Pagar y Actualizar Préstamo ---
        // Sumamos todos los costos: arriendo + multa por atraso + penalidad por daño
        double totalAmountDue = rentalCost + lateFee + damagePenalty;
        loan.setTotalPenalty(totalAmountDue);
        loan.setReturnDate(returnDate);
        loan.setStatus(LoanStatus.CLOSED); // Cerrar el préstamo

        // --- Guardar Préstamo ---
        LoanEntity savedLoan = loanRepository.save(loan);

        // --- Restringir Cliente si hay Monto Pendiente (RN Épica 2 / 3) ---
        // Si el monto total es mayor a cero, se asume que queda pendiente y se restringe.
        if (totalAmountDue > 0.0) {
            clientService.updateStatus(loan.getClient().getId(), ClientStatus.RESTRICTED);
        }
        return savedLoan;
    }

    @Transactional(readOnly = true)
    public List<LoanEntity> getActiveLoans() {
        return loanRepository.findByStatus(LoanStatus.ACTIVE);
    }

    // --- Método para obtener préstamos LATE ---
    @Transactional(readOnly = true)
    public List<LoanEntity> getLateLoans() {
        return loanRepository.findByStatus(LoanStatus.LATE);
    }
}