package com.ProyectoTGS.BackCore.Services;

import entities.*;
import entities.enums.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import app.exceptions.InvalidOperationException;
import app.exceptions.ResourceNotFoundException;
import repositories.*;
import services.*;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class LoanServiceTest {

    @Mock private LoanRepository loanRepository;
    @Mock private ClientRepository clientRepository;
    @Mock private ToolRepository toolRepository;
    @Mock private ToolService toolService;
    @Mock private TariffService tariffService;
    @Mock private ClientService clientService;
    @Mock private KardexService kardexService;

    @InjectMocks
    private LoanService loanService;

    // Test entities (SETUP)
    private ClientEntity clientActive;
    private ClientEntity clientRestricted;
    private ToolEntity toolAvailable;
    private UserEntity testUser;

    @BeforeEach
    void setUp() {
        // Base entities used by most tests
        clientActive = ClientEntity.builder().id(1L).status(ClientStatus.ACTIVE).build();
        clientRestricted = ClientEntity.builder().id(2L).status(ClientStatus.RESTRICTED).build();
        
        // The tool needs stock > 0 so that most loans succeed
        toolAvailable = ToolEntity.builder().id(10L).stock(1).status(ToolStatus.AVAILABLE).replacementValue(45000).build();
        testUser = UserEntity.builder().username("test_user").id(1L).build();
        
        // Fresh service instance for every test
        loanService = new LoanService(loanRepository, clientRepository, toolRepository, toolService, tariffService, clientService);
    }

    // =========================================================================================================
    // EPIC 2: createLoan (loan validations)
    // =========================================================================================================

    @Test
    void createLoan_Success() {
        // ARRANGE
        LocalDate today = LocalDate.now();
        LocalDate dueDate = today.plusDays(7);
        LoanEntity newLoan = LoanEntity.builder().client(clientActive).tool(toolAvailable).startDate(today).dueDate(dueDate).status(LoanStatus.ACTIVE).totalPenalty(0.0).build();

        // MOCK: client and tool exist, no previous loans, persistence works
        when(clientRepository.findById(1L)).thenReturn(Optional.of(clientActive));
        when(toolRepository.findById(10L)).thenReturn(Optional.of(toolAvailable));
        when(loanRepository.save(any(LoanEntity.class))).thenReturn(newLoan);

        // ACT
        LoanEntity createdLoan = loanService.createLoan(1L, 10L, today, dueDate, testUser);

        // ASSERT
        // Status and inventory/Kardex handling (Epics 2, 5)
        assertNotNull(createdLoan);
        assertEquals(LoanStatus.ACTIVE, createdLoan.getStatus());
        verify(toolService, times(1)).decrementStockForLoan(toolAvailable, testUser);
    }

    @Test
    void createLoan_FailsWhenClientIsRestricted() {
        // EPIC 2 / 3: rule - restricted clients cannot borrow
        // ARRANGE: client 2 (clientRestricted) is RESTRICTED
        LocalDate today = LocalDate.now();
        LocalDate dueDate = today.plusDays(7);
        Long restrictedClientId = clientRestricted.getId(); // 2L
        Long toolId = toolAvailable.getId(); // 10L

        // Mocks:
        when(clientRepository.findById(restrictedClientId)).thenReturn(Optional.of(clientRestricted));
        when(toolRepository.findById(toolId)).thenReturn(Optional.of(toolAvailable));

        // ACT & ASSERT: InvalidOperationException expected
        assertThrows(InvalidOperationException.class, () -> { 
            loanService.createLoan(restrictedClientId, toolId, today, dueDate, testUser);
        }, "Must throw InvalidOperationException because the client is restricted.");
        
        // VERIFY: it failed after loading client/tool and before saving
        verify(clientRepository, times(1)).findById(restrictedClientId);
        verify(toolRepository, times(1)).findById(toolId);

        // CRITICAL: the next validations were NOT executed
        verify(loanRepository, never()).countByClientAndStatus(any(), any());
        verify(loanRepository, never()).findByClientAndStatusAndTotalPenaltyGreaterThan(any(), any(), anyDouble());
        verify(loanRepository, never()).findByClientAndStatus(any(), any());
        
        // CRITICAL: nothing was saved
        verify(loanRepository, never()).save(any());
        verify(toolService, never()).decrementStockForLoan(any(), any());
    }
    
    @Test
    void createLoan_FailsWhenDueDateIsBeforeStartDate() {
        // EPIC 2: rule - date validation
        // ARRANGE: dueDate es anterior a startDate
        LocalDate startDate = LocalDate.now();
        LocalDate dueDate = startDate.minusDays(1);
        when(clientRepository.findById(1L)).thenReturn(Optional.of(clientActive));
        when(toolRepository.findById(10L)).thenReturn(Optional.of(toolAvailable));

        // ACT & ASSERT: IllegalArgumentException expected
        assertThrows(IllegalArgumentException.class, () -> 
            loanService.createLoan(1L, 10L, startDate, dueDate, testUser));
        verify(loanRepository, never()).save(any());
    }

    @Test
    void createLoan_FailsWhenLimitOfFiveActiveLoansIsReached() {
        // EPIC 2: rule - limit of 5 active loans
        // ARRANGE: client 1L already has 5 active loans
        LocalDate today = LocalDate.now();
        LocalDate dueDate = today.plusDays(7);

        // 5 ACTIVE loans
        List<LoanEntity> fiveActiveLoans = List.of(
            LoanEntity.builder().build(), // content does not matter, only the size
            LoanEntity.builder().build(),
            LoanEntity.builder().build(),
            LoanEntity.builder().build(),
            LoanEntity.builder().build()
        );

        // MOCK
        when(clientRepository.findById(1L)).thenReturn(Optional.of(clientActive));
        when(toolRepository.findById(10L)).thenReturn(Optional.of(toolAvailable));
        
        // 1. Previous validations pass (LATE and RECEIVED)
        when(loanRepository.countByClientAndStatus(clientActive, LoanStatus.LATE)).thenReturn(0L); // no LATE loans
        when(loanRepository.findByClientAndStatusAndTotalPenaltyGreaterThan(clientActive, LoanStatus.RECEIVED, 0.0))
                .thenReturn(Collections.emptyList()); // no RECEIVED debts

        // 2. The validation that must fail (limit of 5)
        // The service calls findByClientAndStatus(client, ACTIVE).size()
        when(loanRepository.findByClientAndStatus(clientActive, LoanStatus.ACTIVE)).thenReturn(fiveActiveLoans); // 5 active loans

        // ACT & ASSERT: try to create the sixth loan
        assertThrows(InvalidOperationException.class, () -> {
            loanService.createLoan(1L, 10L, today, dueDate, testUser);
        }, "Must fail when the limit of 5 active loans is reached.");

        // the expected queries were used (not findAll)
        verify(loanRepository, times(1)).countByClientAndStatus(clientActive, LoanStatus.LATE);
        verify(loanRepository, times(1)).findByClientAndStatusAndTotalPenaltyGreaterThan(clientActive, LoanStatus.RECEIVED, 0.0);
        verify(loanRepository, times(1)).findByClientAndStatus(clientActive, LoanStatus.ACTIVE);
        verify(loanRepository, never()).save(any()); // must not save
        verify(loanRepository, never()).findAll();
    }

    @Test
    void createLoan_Fails_WhenToolStatusIsNotAvailable() {
        // ARRANGE: tool with status LOANED
        Long toolId = 20L;
        ToolEntity toolLoaned = ToolEntity.builder()
                .id(toolId)
                .stock(0) // stock is irrelevant when the status is wrong
                .status(ToolStatus.LOANED) // <-- INVALID status
                .build();

        // Mocks:
        // 1. Client and debt validations pass
        when(clientRepository.findById(1L)).thenReturn(Optional.of(clientActive));
        when(loanRepository.countByClientAndStatus(clientActive, LoanStatus.LATE)).thenReturn(0L);
        when(loanRepository.findByClientAndStatusAndTotalPenaltyGreaterThan(clientActive, LoanStatus.RECEIVED, 0.0))
                .thenReturn(Collections.emptyList());
        
        // 2. The tool that fails the validation
        when(toolRepository.findById(toolId)).thenReturn(Optional.of(toolLoaned));

        // ACT & ASSERT: InvalidOperationException when the status is not AVAILABLE
        assertThrows(InvalidOperationException.class, () -> {
            loanService.createLoan(1L, toolId, LocalDate.now(), LocalDate.now().plusDays(1), testUser);
        }, "Must throw InvalidOperationException when the status is not AVAILABLE.");

        // VERIFY: it failed after the client validations and before saving
        verify(loanRepository, times(1)).countByClientAndStatus(clientActive, LoanStatus.LATE);
        verify(loanRepository, times(1)).findByClientAndStatusAndTotalPenaltyGreaterThan(clientActive, LoanStatus.RECEIVED, 0.0);
        verify(toolRepository, times(1)).findById(toolId);
        verify(loanRepository, never()).save(any()); // must never save
    }

    @Test
    void createLoan_Fails_WhenToolIsAvailableButStockIsZero() {
        // ARRANGE: AVAILABLE tool with stock 0
        Long toolId = 21L;
        ToolEntity toolOutOfStock = ToolEntity.builder()
                .id(toolId)
                .stock(0) // <-- INVALID stock
                .status(ToolStatus.AVAILABLE) // <-- VALID status
                .build();

        // Mocks:
        // 1. Client and debt validations pass
        when(clientRepository.findById(1L)).thenReturn(Optional.of(clientActive));
        when(loanRepository.countByClientAndStatus(clientActive, LoanStatus.LATE)).thenReturn(0L);
        when(loanRepository.findByClientAndStatusAndTotalPenaltyGreaterThan(clientActive, LoanStatus.RECEIVED, 0.0))
                .thenReturn(Collections.emptyList());
        
        // 2. The tool that fails the validation
        when(toolRepository.findById(toolId)).thenReturn(Optional.of(toolOutOfStock));

        // ACT & ASSERT: InvalidOperationException when the stock is 0
        assertThrows(InvalidOperationException.class, () -> {
            loanService.createLoan(1L, toolId, LocalDate.now(), LocalDate.now().plusDays(1), testUser);
        }, "Must throw InvalidOperationException when the stock is 0.");

        // VERIFY: it failed after the client validations and before saving
        verify(loanRepository, times(1)).countByClientAndStatus(clientActive, LoanStatus.LATE);
        verify(loanRepository, times(1)).findByClientAndStatusAndTotalPenaltyGreaterThan(clientActive, LoanStatus.RECEIVED, 0.0);
        verify(toolRepository, times(1)).findById(toolId);
        verify(loanRepository, never()).save(any()); // must never save
    }
    
    @Test
    void createLoan_FailsWhenClientAlreadyHasThisToolActive() {
        // EPIC 2: rule - the same tool cannot be loaned twice to a client
        // ARRANGE: client 1 already has tool 10 on loan
        LocalDate today = LocalDate.now();
        LocalDate dueDate = today.plusDays(7);

        // An existing ACTIVE loan with the SAME tool (toolAvailable)
        LoanEntity activeLoanForSameTool = LoanEntity.builder()
                .client(clientActive)
                .tool(toolAvailable) // <- the same tool being requested
                .status(LoanStatus.ACTIVE)
                .build();

        // MOCK
        when(clientRepository.findById(1L)).thenReturn(Optional.of(clientActive));
        when(toolRepository.findById(10L)).thenReturn(Optional.of(toolAvailable));
        
        // 1. Previous validations pass (LATE and RECEIVED)
        when(loanRepository.countByClientAndStatus(clientActive, LoanStatus.LATE)).thenReturn(0L); // no LATE loans (used by the limit check)
        when(loanRepository.findByClientAndStatusAndTotalPenaltyGreaterThan(clientActive, LoanStatus.RECEIVED, 0.0))
                .thenReturn(Collections.emptyList()); // no RECEIVED debts

        // 2. Limit of 5 check (fewer than 5 active loans)
        when(loanRepository.findByClientAndStatus(clientActive, LoanStatus.ACTIVE))
                .thenReturn(List.of(activeLoanForSameTool)); // returns the duplicated loan

        // ACT & ASSERT: request the same tool again
        assertThrows(InvalidOperationException.class, () -> {
            loanService.createLoan(1L, 10L, today, dueDate, testUser);
        }, "Must fail when the client already has this tool on loan.");

        // the expected queries were used (not findAll)
        verify(loanRepository, times(1)).countByClientAndStatus(clientActive, LoanStatus.LATE);
        verify(loanRepository, times(1)).findByClientAndStatusAndTotalPenaltyGreaterThan(clientActive, LoanStatus.RECEIVED, 0.0);
        // the ACTIVE loans are queried ONCE and reused by the overdue, limit and duplicate checks
        verify(loanRepository, times(1)).findByClientAndStatus(clientActive, LoanStatus.ACTIVE);

        // LATE loans are never listed: rule 2 already rejects clients with LATE loans
        verify(loanRepository, never()).findByClientAndStatus(clientActive, LoanStatus.LATE);
        
        verify(loanRepository, never()).save(any()); // must not save
        verify(loanRepository, never()).findAll(); // must NEVER call findAll
    }

    @Test
    void createLoan_FailsWhenClientHasUnpaidReceivedLoans() {
        // ARRANGE: active client with a pending debt
        LocalDate today = LocalDate.now();
        LocalDate dueDate = today.plusDays(7);

        // A previous RECEIVED loan with penalty > 0
        LoanEntity unpaidReceivedLoan = LoanEntity.builder()
                .client(clientActive) // clientActive from setUp
                .status(LoanStatus.RECEIVED)
                .totalPenalty(5000.0) // pending debt
                .build();

        // Mocks:
        // 1. Find the active client
        when(clientRepository.findById(clientActive.getId())).thenReturn(Optional.of(clientActive));
        // 2. Find the available tool
        when(toolRepository.findById(toolAvailable.getId())).thenReturn(Optional.of(toolAvailable));
        // 3. No LATE loans
        when(loanRepository.countByClientAndStatus(clientActive, LoanStatus.LATE)).thenReturn(0L);
        // 4. The client HAS pending debts (non-empty list)
        when(loanRepository.findByClientAndStatusAndTotalPenaltyGreaterThan(clientActive, LoanStatus.RECEIVED, 0.0))
                .thenReturn(List.of(unpaidReceivedLoan)); // list with the debt

        // ACT & ASSERT: InvalidOperationException expected
        assertThrows(InvalidOperationException.class, () -> {
            loanService.createLoan(clientActive.getId(), toolAvailable.getId(), today, dueDate, testUser);
        }, "Must throw InvalidOperationException because the client has pending debts.");

        // checks were made up to the failure point
        verify(clientRepository, times(1)).findById(clientActive.getId());
        verify(toolRepository, times(1)).findById(toolAvailable.getId()); // the tool was loaded too
        verify(loanRepository, times(1)).countByClientAndStatus(clientActive, LoanStatus.LATE);
        verify(loanRepository, times(1)).findByClientAndStatusAndTotalPenaltyGreaterThan(clientActive, LoanStatus.RECEIVED, 0.0);
        // CRITICAL: the new loan was NOT saved
        verify(loanRepository, never()).save(any(LoanEntity.class));
        // CRITICAL: the stock was NOT decremented
        verify(toolService, never()).decrementStockForLoan(any(ToolEntity.class), any(UserEntity.class));
    }

@Test
    void createLoan_FailsWhenClientHasLateLoans() {
        // ARRANGE: active client with late loans
        LocalDate today = LocalDate.now();
        LocalDate dueDate = today.plusDays(7);

        // Mocks:
        // 1. Find the active client
        when(clientRepository.findById(clientActive.getId())).thenReturn(Optional.of(clientActive));
        // 2. Find the available tool
        when(toolRepository.findById(toolAvailable.getId())).thenReturn(Optional.of(toolAvailable));
        // 3. The client HAS LATE loans (> 0)
        when(loanRepository.countByClientAndStatus(clientActive, LoanStatus.LATE)).thenReturn(1L); // 1 LATE loan

        // ACT & ASSERT: InvalidOperationException expected
        assertThrows(InvalidOperationException.class, () -> {
            loanService.createLoan(clientActive.getId(), toolAvailable.getId(), today, dueDate, testUser);
        }, "Must throw InvalidOperationException because the client has LATE loans.");

        // checks were made up to the failure point
        verify(clientRepository, times(1)).findById(clientActive.getId());
        verify(toolRepository, times(1)).findById(toolAvailable.getId());
        verify(loanRepository, times(1)).countByClientAndStatus(clientActive, LoanStatus.LATE);
        // CRITICAL: RECEIVED debts were NOT checked afterwards
        verify(loanRepository, never()).findByClientAndStatusAndTotalPenaltyGreaterThan(any(ClientEntity.class), any(LoanStatus.class), anyDouble());
        // CRITICAL: the new loan was NOT saved
        verify(loanRepository, never()).save(any(LoanEntity.class));
        // CRITICAL: the stock was NOT decremented
        verify(toolService, never()).decrementStockForLoan(any(ToolEntity.class), any(UserEntity.class));
    }

    // =========================================================================================================
    // EPIC 2: returnLoan (returns, fees and restrictions)
    // =========================================================================================================

    @Test
    void returnLoan_SuccessOnTimeNoDamage_SetsStatusToReceivedAndRestrictsClient() {
        // ARRANGE: returned on time (returnDate = dueDate), no damage
        LocalDate startDate = LocalDate.now().minusDays(5);
        LocalDate dueDate = LocalDate.now(); // due today
        LocalDate returnDate = LocalDate.now(); // returned today
        Long loanId = 5L;
        
        LoanEntity loan = LoanEntity.builder()
                .id(loanId)
                .client(clientActive)
                .tool(toolAvailable)
                .startDate(startDate)
                .dueDate(dueDate)
                .status(LoanStatus.ACTIVE)
                .totalPenalty(0.0)
                .build();

        // MOCK:
        when(loanRepository.findById(loanId)).thenReturn(Optional.of(loan));
        // Daily rental fee (the only tariff needed here)
        when(tariffService.getDailyRentFee()).thenReturn(1000.0);
        when(loanRepository.save(any(LoanEntity.class))).thenAnswer(invocation -> invocation.getArgument(0)); // returns the saved entity

        // ACT (returned today, on time)
        LoanEntity returnedLoan = loanService.returnLoan(loanId, toolAvailable.getId(), false, false, testUser, returnDate);
        
        // ASSERT
        // Agreed rental days = 5
        long rentalDays = ChronoUnit.DAYS.between(startDate, dueDate); // 5
        double expectedRentalCost = rentalDays * 1000.0; // 5000.0

        // 1. Status RECEIVED (payment pending)
        assertEquals(LoanStatus.RECEIVED, returnedLoan.getStatus());
        // 2. The penalty is the rental cost
        assertEquals(expectedRentalCost, returnedLoan.getTotalPenalty(), 0.01); // 5000.0

        // 3. The stock is updated
        verify(toolService, times(1)).incrementStockForReturn(toolAvailable, testUser);
        // 4. The client MUST be restricted (totalPenalty > 0)
        verify(clientService, times(1)).updateStatus(clientActive.getId(), ClientStatus.RESTRICTED);
    }

    @Test
    void returnLoan_AppliesLateFeeAndRentalFee_AndRestrictsClient() {
        // ARRANGE: returned 2 days late
        LocalDate startDate = LocalDate.now().minusDays(7); // started 7 days ago
        LocalDate dueDate = LocalDate.now().minusDays(2); // was due 2 days ago
        LocalDate returnDate = LocalDate.now(); // returned today
        Long loanId = 6L;
        
        LoanEntity loan = LoanEntity.builder()
                .id(loanId)
                .client(clientActive)
                .tool(toolAvailable)
                .startDate(startDate)
                .dueDate(dueDate)
                .status(LoanStatus.LATE) // or ACTIVE
                .totalPenalty(0.0)
                .build();

        when(loanRepository.findById(loanId)).thenReturn(Optional.of(loan));
        when(tariffService.getDailyRentFee()).thenReturn(1000.0); // rental fee
        when(tariffService.getDailyLateFee()).thenReturn(2000.0); // late fee
        when(loanRepository.save(any(LoanEntity.class))).thenReturn(loan);
        
        // ACT (returned today, 2 days late)
        LoanEntity returnedLoan = loanService.returnLoan(loanId, toolAvailable.getId(), false, false, testUser, returnDate);
        
        // ASSERT
        // Agreed rental days = 5
        long rentalDays = ChronoUnit.DAYS.between(startDate, dueDate); // 5
        double expectedRentalCost = rentalDays * 1000.0; // 5000.0
        // Days late = 2
        long delayDays = ChronoUnit.DAYS.between(dueDate, returnDate); // 2
        double expectedLateFee = delayDays * 2000.0; // 4000.0
        
        double expectedTotalPenalty = expectedRentalCost + expectedLateFee; // 5000 + 4000 = 9000.0

        assertEquals(expectedTotalPenalty, returnedLoan.getTotalPenalty(), 0.01); // Total 9000.0
        assertEquals(LoanStatus.RECEIVED, returnedLoan.getStatus());
        // CRITICAL RULE: the client is restricted because of the fee (Epic 3)
        verify(clientService, times(1)).updateStatus(clientActive.getId(), ClientStatus.RESTRICTED); 
        verify(toolService, times(1)).incrementStockForReturn(toolAvailable, testUser);
        verify(toolService, never()).markAsRepairing(any(), any());
        verify(toolService, never()).markAsDecommissioned(any(), any());
    }
    
    @Test
    void returnLoan_AppliesRepairFeeAndRentalFee_MarksRepairingAndRestrictsClient() {
        // ARRANGE: returned on time (returnDate = dueDate) but damaged
        Long loanId = 8L;
        LocalDate startDate = LocalDate.now().minusDays(7); // started 7 days ago
        LocalDate dueDate = LocalDate.now(); // due today
        LocalDate returnDate = LocalDate.now(); // returned today
        
        LoanEntity loan = LoanEntity.builder()
            .id(loanId)
            .client(clientActive)
            .tool(toolAvailable)
            .startDate(startDate)
            .dueDate(dueDate) 
            .status(LoanStatus.ACTIVE)
            .totalPenalty(0.0).build();
        
        when(loanRepository.findById(loanId)).thenReturn(Optional.of(loan));
        when(tariffService.getDailyRentFee()).thenReturn(1000.0); // rental fee
        when(tariffService.getRepairFee()).thenReturn(1500.0); // repair fee
        when(loanRepository.save(any(LoanEntity.class))).thenReturn(loan);
        
        // ACT (damaged, NOT irreparable)
        LoanEntity returnedLoan = loanService.returnLoan(loanId, toolAvailable.getId(), true, false, testUser, returnDate);
        
        // ASSERT
        // Agreed rental days = 7
        long rentalDays = ChronoUnit.DAYS.between(startDate, dueDate); // 7
        double expectedRentalCost = rentalDays * 1000.0; // 7000.0
        // Days late = 0
        // Repair charge = 1500.0
        
        double expectedTotalPenalty = expectedRentalCost + 1500.0; // 7000 + 1500 = 8500.0

        assertEquals(expectedTotalPenalty, returnedLoan.getTotalPenalty(), 0.01);
        assertEquals(LoanStatus.RECEIVED, returnedLoan.getStatus());
        // CRITICAL RULE: the client is restricted because of the charge (Epic 3)
        verify(clientService, times(1)).updateStatus(clientActive.getId(), ClientStatus.RESTRICTED);
        // Must be marked as REPAIRING (Epic 1)
        verify(toolService, times(1)).markAsRepairing(toolAvailable, testUser);
        verify(toolService, never()).incrementStockForReturn(any(), any()); // does not go back to stock
    }

    @Test
    void returnLoan_AppliesReplacementFeeAndLateFeeAndRentalFee_AndRestrictsClient() {
        // ARRANGE: 10 days LATE and irreparable damage
        Long loanId = 7L;
        LocalDate startDate = LocalDate.now().minusDays(15); // started 15 days ago
        LocalDate dueDate = LocalDate.now().minusDays(10); // was due 10 days ago
        LocalDate returnDate = LocalDate.now(); // returned today
        
        ToolEntity irreparableTool = ToolEntity.builder().id(11L).replacementValue(50000).build();
        
        LoanEntity loan = LoanEntity.builder()
            .id(loanId)
            .client(clientActive)
            .tool(irreparableTool)
            .startDate(startDate)
            .dueDate(dueDate) 
            .status(LoanStatus.LATE)
            .totalPenalty(0.0).build();
        
        when(loanRepository.findById(loanId)).thenReturn(Optional.of(loan));
        when(tariffService.getDailyRentFee()).thenReturn(1000.0); // rental fee
        when(tariffService.getDailyLateFee()).thenReturn(2000.0); // late fee

        when(loanRepository.save(any(LoanEntity.class))).thenReturn(loan);

        // ACT (damaged and irreparable)
        LoanEntity returnedLoan = loanService.returnLoan(loanId, 11L, true, true, testUser, returnDate);
        
        // ASSERT
        // Agreed rental days = 5
        long rentalDays = ChronoUnit.DAYS.between(startDate, dueDate); // 5
        double expectedRentalCost = rentalDays * 1000.0; // 5000.0
        // Days late = 10
        long delayDays = ChronoUnit.DAYS.between(dueDate, returnDate); // 10
        double expectedLateFee = delayDays * 2000.0; // 20000.0
        // Replacement charge = 50000.0
        
        double expectedTotalPenalty = expectedRentalCost + expectedLateFee + 50000.0; // 5000 + 20000 + 50000 = 75000.0

        assertEquals(expectedTotalPenalty, returnedLoan.getTotalPenalty(), 0.01);
        assertEquals(LoanStatus.RECEIVED, returnedLoan.getStatus());
        // CRITICAL RULE: the client is restricted because of the charge
        verify(clientService, times(1)).updateStatus(clientActive.getId(), ClientStatus.RESTRICTED);
        // Must be marked as DECOMMISSIONED (Epic 1)
        verify(toolService, times(1)).markAsDecommissioned(irreparableTool, testUser);
        verify(toolService, never()).incrementStockForReturn(any(), any());
    }
    
    @Test
    void returnLoan_FailsWhenLoanStatusIsClosed() {
        // ARRANGE: a loan that is already CLOSED
        Long loanId = 9L;
        LoanEntity loan = LoanEntity.builder()
                .id(loanId)
                .tool(toolAvailable) // toolAvailable has ID 10L
                .status(LoanStatus.CLOSED) // <-- invalid status
                .build();

        when(loanRepository.findById(loanId)).thenReturn(Optional.of(loan));
        
        // ACT & ASSERT: fails when the status is not ACTIVE or LATE
        assertThrows(InvalidOperationException.class, () -> 
            // toolAvailable.getId() is 10L
            loanService.returnLoan(loanId, toolAvailable.getId(), false, false, testUser, LocalDate.now()),
            "Only active or late loans can be returned.");

        verify(loanRepository, times(1)).findById(loanId);
        verify(loanRepository, never()).save(any());
        verify(toolService, never()).incrementStockForReturn(any(), any());
    }

    // =========================================================================================================
    // EPIC 4 / 2: RENTAL FEE CALCULATION (returnLoan)
    // =========================================================================================================

    @Test
    void returnLoan_CalculatesRentalFeeCorrectly_WhenReturnedEarly() {
        // ARRANGE: returned early
        Long loanId = 15L;
        LocalDate startDate = LocalDate.now().minusDays(3); // started 3 days ago
        LocalDate dueDate = LocalDate.now().plusDays(5); // due in 5 days
        LocalDate returnDate = LocalDate.now(); // returned today (early)

        LoanEntity loan = LoanEntity.builder()
            .id(loanId)
            .client(clientActive)
            .tool(toolAvailable)
            .startDate(startDate)
            .dueDate(dueDate)
            .status(LoanStatus.ACTIVE)
            .totalPenalty(0.0)
            .build();

        // MOCK:
        when(loanRepository.findById(loanId)).thenReturn(Optional.of(loan));
        when(tariffService.getDailyRentFee()).thenReturn(1000.0);
        when(loanRepository.save(any(LoanEntity.class))).thenReturn(loan);

        // ACT: return without damage and without delay
        LoanEntity returnedLoan = loanService.returnLoan(loanId, toolAvailable.getId(), false, false, testUser, returnDate);

        // ASSERT:
        // Agreed rental days = 8 (between startDate and dueDate)
        long rentalDays = ChronoUnit.DAYS.between(startDate, dueDate); // 8
        double expectedRentalCost = rentalDays * 1000.0; // 8000.0

        assertEquals(expectedRentalCost, returnedLoan.getTotalPenalty(), 0.01); // 8000.0
        assertEquals(LoanStatus.RECEIVED, returnedLoan.getStatus()); // status must be RECEIVED
        
        verify(toolService, times(1)).incrementStockForReturn(toolAvailable, testUser);
        verify(clientService, times(1)).updateStatus(clientActive.getId(), ClientStatus.RESTRICTED);
    }

    @Test
    void returnLoan_AppliesMinimumOneDayRentalFee_WhenStartDateEqualsDueDate() {
        // ARRANGE: loan due the same day (startDate = dueDate)
        Long loanId = 16L;
        LocalDate startDate = LocalDate.now();
        LocalDate dueDate = LocalDate.now(); // due the same day
        LocalDate returnDate = LocalDate.now(); // returned the same day

        LoanEntity loan = LoanEntity.builder()
            .id(loanId)
            .client(clientActive)
            .tool(toolAvailable)
            .startDate(startDate)
            .dueDate(dueDate)
            .status(LoanStatus.ACTIVE)
            .totalPenalty(0.0)
            .build();

        when(loanRepository.findById(loanId)).thenReturn(Optional.of(loan));
        when(tariffService.getDailyRentFee()).thenReturn(1000.0); // fee 1000
        when(loanRepository.save(any(LoanEntity.class))).thenReturn(loan);

        // ACT: return the same day, no damage or delay
        LoanEntity returnedLoan = loanService.returnLoan(loanId, toolAvailable.getId(), false, false, testUser, returnDate);

        // ASSERT:
        // Agreed rental days = 0.
        // The service applies the rule: if rentalDays < 1 then rentalDays = 1.
        double expectedRentalCost = 1 * 1000.0; // 1000.0 (minimum 1 day)

        assertEquals(expectedRentalCost, returnedLoan.getTotalPenalty(), 0.01);
        assertEquals(LoanStatus.RECEIVED, returnedLoan.getStatus());
        
        verify(toolService, times(1)).incrementStockForReturn(toolAvailable, testUser);
        verify(clientService, times(1)).updateStatus(clientActive.getId(), ClientStatus.RESTRICTED);
    }

@Test
    void returnLoan_AppliesLateFeeAndRepairFee_WhenLateAndDamagedRepairable() {
        // ARRANGE: 2 days late with repairable damage
        Long loanId = 11L;
        LocalDate startDate = LocalDate.now().minusDays(10); // started 10 days ago
        LocalDate dueDate = LocalDate.now().minusDays(2); // was due 2 days ago
        LocalDate returnDate = LocalDate.now(); // returned today (2 days late)

        LoanEntity loan = LoanEntity.builder()
                .id(loanId)
                .client(clientActive) // active client from setUp
                .tool(toolAvailable) // available tool from setUp
                .startDate(startDate)
                .dueDate(dueDate)
                .status(LoanStatus.LATE) // LATE or ACTIVE: the calculation is the same
                .totalPenalty(0.0)
                .build();

        // MOCK:
        when(loanRepository.findById(loanId)).thenReturn(Optional.of(loan));
        // The tool comes from the loan, no toolRepository.findById needed
        // when(toolRepository.findById(toolAvailable.getId())).thenReturn(Optional.of(toolAvailable));
        when(tariffService.getDailyLateFee()).thenReturn(2000.0); // daily late fee
        when(tariffService.getRepairFee()).thenReturn(1500.0); // repair fee
        when(tariffService.getDailyRentFee()).thenReturn(1000.0); // daily rental fee
        when(loanRepository.save(any(LoanEntity.class))).thenReturn(loan); // simulated save

        // ACT: late return with repairable damage
        LoanEntity returnedLoan = loanService.returnLoan(loanId, toolAvailable.getId(), true, false, testUser, returnDate);

        // ASSERT: expected total penalty
        // Rental days = 8 (from startDate to dueDate)
        long rentalDays = ChronoUnit.DAYS.between(startDate, dueDate);
        if (rentalDays < 1) rentalDays = 1; // minimum 1 day
        double expectedRentalCost = rentalDays * 1000.0; // 8 * 1000 = 8000
        // Days late = 2
        long delayDays = ChronoUnit.DAYS.between(dueDate, returnDate);
        double expectedLateFee = delayDays * 2000.0; // 2 * 2000 = 4000
        // Repair charge
        double expectedRepairFee = 1500.0;
        // Total
        double expectedTotalPenalty = expectedRentalCost + expectedLateFee + expectedRepairFee; // 8000 + 4000 + 1500 = 13500

        assertEquals(expectedTotalPenalty, returnedLoan.getTotalPenalty(), 0.01);
        assertEquals(LoanStatus.RECEIVED, returnedLoan.getStatus()); // status becomes RECEIVED
        assertNotNull(returnedLoan.getReturnDate()); // return date saved

        // Interactions with other services
        // Marked as REPAIRING (stock NOT incremented)
        verify(toolService, times(1)).markAsRepairing(eq(toolAvailable), eq(testUser));
        verify(toolService, never()).incrementStockForReturn(any(), any()); // must not increment stock
        verify(toolService, never()).markAsDecommissioned(any(), any()); // must not be decommissioned
        // The client stays RESTRICTED
        verify(clientService, times(1)).updateStatus(clientActive.getId(), ClientStatus.RESTRICTED);
        // The loan is saved
        verify(loanRepository, times(1)).save(loan);
    }

@Test
    void returnLoan_AppliesLateFeeAndReplacementFee_WhenLateAndDamagedIrreparable() {
        // ARRANGE: 3 days late with irreparable damage
        Long loanId = 12L;
        LocalDate startDate = LocalDate.now().minusDays(10); // started 10 days ago
        LocalDate dueDate = LocalDate.now().minusDays(3); // was due 3 days ago
        LocalDate returnDate = LocalDate.now(); // returned today (3 days late)

        // Tool from setUp with replacementValue = 45000
        LoanEntity loan = LoanEntity.builder()
                .id(loanId)
                .client(clientActive)
                .tool(toolAvailable) // toolAvailable has replacementValue = 45000
                .startDate(startDate)
                .dueDate(dueDate)
                .status(LoanStatus.LATE)
                .totalPenalty(0.0)
                .build();

        // MOCK:
        when(loanRepository.findById(loanId)).thenReturn(Optional.of(loan));
        // The tool comes from the loan, no toolRepository.findById needed
        when(tariffService.getDailyLateFee()).thenReturn(2000.0); // daily late fee
        when(tariffService.getDailyRentFee()).thenReturn(1000.0); // daily rental fee
        // tariffService.getRepairFee() is NOT needed
        when(loanRepository.save(any(LoanEntity.class))).thenReturn(loan); // simulated save

        // ACT: late return with irreparable damage
        LoanEntity returnedLoan = loanService.returnLoan(loanId, toolAvailable.getId(), true, true, testUser, returnDate);

        // ASSERT: expected total penalty
        // Rental days = 7 (from startDate to dueDate)
        long rentalDays = ChronoUnit.DAYS.between(startDate, dueDate);
        if (rentalDays < 1) rentalDays = 1;
        double expectedRentalCost = rentalDays * 1000.0; // 7 * 1000 = 7000
        // Days late = 3
        long delayDays = ChronoUnit.DAYS.between(dueDate, returnDate);
        double expectedLateFee = delayDays * 2000.0; // 3 * 2000 = 6000
        // Replacement charge (from toolAvailable.getReplacementValue())
        double expectedReplacementFee = toolAvailable.getReplacementValue(); // 45000
        // Total
        double expectedTotalPenalty = expectedRentalCost + expectedLateFee + expectedReplacementFee; // 7000 + 6000 + 45000 = 58000

        assertEquals(expectedTotalPenalty, returnedLoan.getTotalPenalty(), 0.01);
        assertEquals(LoanStatus.RECEIVED, returnedLoan.getStatus());
        assertEquals(returnDate, returnedLoan.getReturnDate());

        // Interactions with other services
        // Marked as DECOMMISSIONED
        verify(toolService, times(1)).markAsDecommissioned(eq(toolAvailable), eq(testUser));
        verify(toolService, never()).incrementStockForReturn(any(), any()); // must not increment stock
        verify(toolService, never()).markAsRepairing(any(), any()); // must not be sent to repair
        // The client stays RESTRICTED
        verify(clientService, times(1)).updateStatus(clientActive.getId(), ClientStatus.RESTRICTED);
        // The loan is saved
        verify(loanRepository, times(1)).save(loan);
    }

@Test
    void returnLoan_AppliesOnlyRentalFee_WhenReturnedEarlyNoDamage() {
        // ARRANGE: returned 2 days BEFORE the due date
        Long loanId = 13L;
        LocalDate startDate = LocalDate.now().minusDays(5); // started 5 days ago
        LocalDate dueDate = LocalDate.now().plusDays(2); // due in 2 days
        LocalDate returnDate = LocalDate.now(); // returned today (2 days early)

        LoanEntity loan = LoanEntity.builder()
                .id(loanId)
                .client(clientActive)
                .tool(toolAvailable)
                .startDate(startDate)
                .dueDate(dueDate)
                .status(LoanStatus.ACTIVE)
                .totalPenalty(0.0)
                .build();

        // MOCK:
        when(loanRepository.findById(loanId)).thenReturn(Optional.of(loan));
        when(tariffService.getDailyRentFee()).thenReturn(1000.0); // daily rental fee
        // getDailyLateFee and getRepairFee are NOT needed
        when(loanRepository.save(any(LoanEntity.class))).thenReturn(loan);

        // ACT: early return without damage
        LoanEntity returnedLoan = loanService.returnLoan(loanId, toolAvailable.getId(), false, false, testUser, returnDate);

        // ASSERT: expected total penalty
        // Elapsed days = 5 (from startDate to returnDate)
        // Business rule: the rent is charged up to the agreed date (dueDate) even if returned earlier
        // ("If the return happens before the agreed date there are no refunds; the minimum charge is always 1 day")
        // So the charge is computed as if it had been returned on the due date.
        long rentalDaysCharged = ChronoUnit.DAYS.between(startDate, dueDate); // 7 days
        if (rentalDaysCharged < 1) rentalDaysCharged = 1; // minimum 1 day
        double expectedRentalCost = rentalDaysCharged * 1000.0; // 7 * 1000 = 7000

        // Late fee must be ZERO
        double expectedLateFee = 0.0;
        // Damage charge must be ZERO
        double expectedDamagePenalty = 0.0;
        // Expected total = rent up to the agreed date only
        double expectedTotalPenalty = expectedRentalCost + expectedLateFee + expectedDamagePenalty; // 7000

        assertEquals(expectedTotalPenalty, returnedLoan.getTotalPenalty(), 0.01);
        assertEquals(LoanStatus.RECEIVED, returnedLoan.getStatus()); // becomes RECEIVED
        assertEquals(returnDate, returnedLoan.getReturnDate());

        // Interactions with other services
        // Stock is incremented
        verify(toolService, times(1)).incrementStockForReturn(eq(toolAvailable), eq(testUser));
        verify(toolService, never()).markAsRepairing(any(), any());
        verify(toolService, never()).markAsDecommissioned(any(), any());
        // The client stays RESTRICTED because totalPenalty > 0
        verify(clientService, times(1)).updateStatus(clientActive.getId(), ClientStatus.RESTRICTED);
        // The loan is saved
        verify(loanRepository, times(1)).save(loan);
    }

    // =========================================================================================================
    // METHOD: markLoanAsPaid
    // =========================================================================================================

    @Test
    void markLoanAsPaid_Success_WhenLoanIsReceivedWithPenalty() {
        // ARRANGE: a RECEIVED loan with a pending penalty
        Long loanId = 20L;
        LoanEntity receivedLoanWithPenalty = LoanEntity.builder()
                .id(loanId)
                .client(clientActive) // client of the loan
                .tool(toolAvailable) // tool of the loan
                .status(LoanStatus.RECEIVED) // the CORRECT status for this operation
                .totalPenalty(5000.0) // PENDING penalty
                .build();

        // Mocks:
        // 1. Find the loan
        when(loanRepository.findById(loanId)).thenReturn(Optional.of(receivedLoanWithPenalty));
        // 2. save returns the object it receives
        when(loanRepository.save(any(LoanEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // ACT: mark as paid
        LoanEntity paidLoan = loanService.markLoanAsPaid(loanId);

        // ASSERT: status and penalty changed
        assertNotNull(paidLoan);
        assertEquals(loanId, paidLoan.getId());
        assertEquals(LoanStatus.CLOSED, paidLoan.getStatus()); // status MUST become CLOSED
        assertEquals(0.0, paidLoan.getTotalPenalty(), 0.01); // penalty MUST be 0.0

        // findById and save were called
        verify(loanRepository, times(1)).findById(loanId);
        verify(loanRepository, times(1)).save(argThat(loan ->
                loan.getId().equals(loanId) &&
                loan.getStatus().equals(LoanStatus.CLOSED) && // final status
                loan.getTotalPenalty() == 0.0 // final penalty
        ));

        // IMPORTANT: the client status was NOT changed
        verify(clientService, never()).updateStatus(anyLong(), any(ClientStatus.class));
    }

    @Test
    void markLoanAsPaid_Fails_WhenLoanStatusIsNotReceived() {
        // ARRANGE: an ACTIVE loan (wrong status for this operation)
        Long loanId = 21L;
        LoanEntity activeLoan = LoanEntity.builder()
                .id(loanId)
                .client(clientActive)
                .tool(toolAvailable)
                .status(LoanStatus.ACTIVE) // WRONG status
                .totalPenalty(0.0) // irrelevant here
                .build();

        // Mock: find the active loan
        when(loanRepository.findById(loanId)).thenReturn(Optional.of(activeLoan));

        // ACT & ASSERT: InvalidOperationException expected
        assertThrows(InvalidOperationException.class, () -> {
            loanService.markLoanAsPaid(loanId);
        }, "Must throw InvalidOperationException when the status is not RECEIVED.");

        // findById was called
        verify(loanRepository, times(1)).findById(loanId);
        // CRITICAL: nothing was saved (status and penalty unchanged)
        verify(loanRepository, never()).save(any(LoanEntity.class));
        // CRITICAL: the client status was NOT changed
        verify(clientService, never()).updateStatus(anyLong(), any(ClientStatus.class));

    }

    @Test
    void markLoanAsPaid_Fails_WhenLoanNotFound() {
        // ARRANGE: unknown loan ID
        Long nonExistentLoanId = 99L;

        // Mock: findById returns an empty Optional
        when(loanRepository.findById(nonExistentLoanId)).thenReturn(Optional.empty());

        // ACT & ASSERT: ResourceNotFoundException expected
        assertThrows(ResourceNotFoundException.class, () -> {
            loanService.markLoanAsPaid(nonExistentLoanId);
        }, "Must throw ResourceNotFoundException when the loan is not found.");

        // the loan was looked up once
        verify(loanRepository, times(1)).findById(nonExistentLoanId);
        // CRITICAL: nothing was saved
        verify(loanRepository, never()).save(any(LoanEntity.class));
        // CRITICAL: the client status was NOT changed
        verify(clientService, never()).updateStatus(anyLong(), any(ClientStatus.class));
    }

    // =========================================================================================================
    // METHOD: getUnpaidReceivedLoansByClientId
    // =========================================================================================================

    @Test
    void getUnpaidReceivedLoansByClientId_Success_WhenClientHasUnpaidLoans() {
        // ARRANGE: existing client and a list of RECEIVED loans with debt
        Long clientId = clientActive.getId(); // active client from setUp
        LoanEntity unpaidLoan1 = LoanEntity.builder()
                .id(30L)
                .client(clientActive)
                .status(LoanStatus.RECEIVED)
                .totalPenalty(2000.0)
                .build();
        LoanEntity unpaidLoan2 = LoanEntity.builder()
                .id(31L)
                .client(clientActive)
                .status(LoanStatus.RECEIVED)
                .totalPenalty(1500.0)
                .build();
        List<LoanEntity> unpaidLoansList = List.of(unpaidLoan1, unpaidLoan2);

        // Mocks:
        // 1. Find the client
        when(clientRepository.findById(clientId)).thenReturn(Optional.of(clientActive));
        // 2. The repository returns the debts of the client
        when(loanRepository.findByClientAndStatusAndTotalPenaltyGreaterThan(
                clientActive, LoanStatus.RECEIVED, 0.0))
                .thenReturn(unpaidLoansList);

        // ACT
        List<LoanEntity> resultList = loanService.getUnpaidReceivedLoansByClientId(clientId);

        // ASSERT: the expected list is returned
        assertNotNull(resultList);
        assertEquals(2, resultList.size()); // 2 loans expected
        // IDs or penalties could also be checked
        assertTrue(resultList.stream().anyMatch(loan -> loan.getId().equals(30L) && loan.getTotalPenalty() == 2000.0));
        assertTrue(resultList.stream().anyMatch(loan -> loan.getId().equals(31L) && loan.getTotalPenalty() == 1500.0));

        // the expected repository methods were called
        verify(clientRepository, times(1)).findById(clientId);
        verify(loanRepository, times(1)).findByClientAndStatusAndTotalPenaltyGreaterThan(
                clientActive, LoanStatus.RECEIVED, 0.0);
    }

    @Test
    void getUnpaidReceivedLoansByClientId_Success_ReturnsEmptyListWhenNoUnpaidLoans() {
        // ARRANGE: existing client without pending debts
        Long clientId = clientActive.getId();

        // Mocks:
        // 1. Find the client
        when(clientRepository.findById(clientId)).thenReturn(Optional.of(clientActive));
        // 2. The repository returns an EMPTY list
        when(loanRepository.findByClientAndStatusAndTotalPenaltyGreaterThan(
                clientActive, LoanStatus.RECEIVED, 0.0))
                .thenReturn(Collections.emptyList()); // empty list = no debts

        // ACT
        List<LoanEntity> resultList = loanService.getUnpaidReceivedLoansByClientId(clientId);

        // ASSERT: the list is empty
        assertNotNull(resultList);
        assertTrue(resultList.isEmpty()); // the list must be empty

        // the expected repository methods were called
        verify(clientRepository, times(1)).findById(clientId);
        verify(loanRepository, times(1)).findByClientAndStatusAndTotalPenaltyGreaterThan(
                clientActive, LoanStatus.RECEIVED, 0.0);
    }

    @Test
    void getUnpaidReceivedLoansByClientId_Fails_WhenClientNotFound() {
        // ARRANGE: unknown client ID
        Long nonExistentClientId = 99L;

        // Mock: findById returns an empty Optional
        when(clientRepository.findById(nonExistentClientId)).thenReturn(Optional.empty());

        // ACT & ASSERT: ResourceNotFoundException expected
        assertThrows(ResourceNotFoundException.class, () -> {
            loanService.getUnpaidReceivedLoansByClientId(nonExistentClientId);
        }, "Must throw ResourceNotFoundException when the client is not found.");

        // the client was looked up
        verify(clientRepository, times(1)).findById(nonExistentClientId);
        // CRITICAL: loans were NOT queried
        verify(loanRepository, never()).findByClientAndStatusAndTotalPenaltyGreaterThan(
                any(ClientEntity.class), any(LoanStatus.class), anyDouble());
    }

    // =========================================================================================================
    // RF2.5 / RF3.2: overdue loans -> LATE, client restricted; audit fields (user + timestamp)
    // =========================================================================================================

    @Test
    void createLoan_FailsWhenClientHasOverdueActiveLoan() {
        LocalDate today = LocalDate.now();
        ToolEntity otherTool = ToolEntity.builder().id(99L).build();
        LoanEntity overdue = LoanEntity.builder().id(5L).client(clientActive).tool(otherTool)
                .startDate(today.minusDays(10)).dueDate(today.minusDays(1)).status(LoanStatus.ACTIVE).build();

        when(clientRepository.findById(1L)).thenReturn(Optional.of(clientActive));
        when(toolRepository.findById(10L)).thenReturn(Optional.of(toolAvailable));
        when(loanRepository.findByClientAndStatus(clientActive, LoanStatus.ACTIVE)).thenReturn(List.of(overdue));

        InvalidOperationException ex = assertThrows(InvalidOperationException.class,
                () -> loanService.createLoan(1L, 10L, today, today.plusDays(3), testUser));
        assertTrue(ex.getMessage().contains("overdue"));
        verify(loanRepository, never()).save(any(LoanEntity.class));
    }

    @Test
    void createLoan_RecordsResponsibleUserAndTimestamp() {
        LocalDate today = LocalDate.now();
        when(clientRepository.findById(1L)).thenReturn(Optional.of(clientActive));
        when(toolRepository.findById(10L)).thenReturn(Optional.of(toolAvailable));
        when(loanRepository.save(any(LoanEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        LoanEntity created = loanService.createLoan(1L, 10L, today, today.plusDays(3), testUser);

        assertEquals(testUser, created.getLoanedBy());
        assertNotNull(created.getLoanedAt());
    }

    @Test
    void returnLoan_RecordsResponsibleUserAndTimestamp() {
        LocalDate today = LocalDate.now();
        LoanEntity loan = LoanEntity.builder().id(7L).client(clientActive).tool(toolAvailable)
                .startDate(today.minusDays(2)).dueDate(today.plusDays(2)).status(LoanStatus.ACTIVE).build();
        when(loanRepository.findById(7L)).thenReturn(Optional.of(loan));
        when(tariffService.getDailyRentFee()).thenReturn(1000.0);
        when(loanRepository.save(any(LoanEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        LoanEntity returned = loanService.returnLoan(7L, 10L, false, false, testUser, today);

        assertEquals(testUser, returned.getReturnedBy());
        assertNotNull(returned.getReturnedAt());
        assertEquals(LoanStatus.RECEIVED, returned.getStatus());
    }

    @Test
    void markOverdueLoansAsLate_UpdatesStatusAndRestrictsClient() {
        LocalDate today = LocalDate.now();
        ClientEntity client = ClientEntity.builder().id(3L).status(ClientStatus.ACTIVE).build();
        LoanEntity overdue = LoanEntity.builder().id(1L).client(client).tool(toolAvailable)
                .dueDate(today.minusDays(2)).status(LoanStatus.ACTIVE).build();
        when(loanRepository.findByStatusAndDueDateBefore(LoanStatus.ACTIVE, today)).thenReturn(List.of(overdue));

        int updated = loanService.markOverdueLoansAsLate();

        assertEquals(1, updated);
        assertEquals(LoanStatus.LATE, overdue.getStatus());
        assertEquals(ClientStatus.RESTRICTED, client.getStatus());
        verify(loanRepository).save(overdue);
        verify(clientService).updateStatus(3L, ClientStatus.RESTRICTED);
    }

    @Test
    void markOverdueLoansAsLate_DoesNotRestrictAlreadyRestrictedClient() {
        LocalDate today = LocalDate.now();
        LoanEntity overdue = LoanEntity.builder().id(2L).client(clientRestricted).tool(toolAvailable)
                .dueDate(today.minusDays(1)).status(LoanStatus.ACTIVE).build();
        when(loanRepository.findByStatusAndDueDateBefore(LoanStatus.ACTIVE, today)).thenReturn(List.of(overdue));

        assertEquals(1, loanService.markOverdueLoansAsLate());
        assertEquals(LoanStatus.LATE, overdue.getStatus());
        verify(clientService, never()).updateStatus(anyLong(), any(ClientStatus.class));
    }

    @Test
    void getOpenLoans_QueriesActiveAndLateLoansInOneCall() {
        LoanEntity active = LoanEntity.builder().id(1L).status(LoanStatus.ACTIVE).build();
        LoanEntity late = LoanEntity.builder().id(2L).status(LoanStatus.LATE).build();
        when(loanRepository.findByStatusIn(List.of(LoanStatus.ACTIVE, LoanStatus.LATE))).thenReturn(List.of(active, late));

        List<LoanEntity> result = loanService.getOpenLoans();

        assertEquals(List.of(active, late), result);
    }

    @Test
    void markOverdueLoansAsLate_ReturnsZeroWhenNothingIsOverdue() {
        when(loanRepository.findByStatusAndDueDateBefore(eq(LoanStatus.ACTIVE), any(LocalDate.class)))
                .thenReturn(Collections.emptyList());

        assertEquals(0, loanService.markOverdueLoansAsLate());
        verify(loanRepository, never()).save(any(LoanEntity.class));
    }

}