package com.ProyectoTGS.BackCore.Services;

import entities.ClientEntity;
import entities.LoanEntity;
import entities.ToolEntity;
import entities.enums.ClientStatus;
import entities.enums.LoanStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import repositories.LoanRepository;
import services.ReportService;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ReportServiceTest {

    @Mock
    private LoanRepository loanRepository;

    @InjectMocks
    private ReportService reportService;

    private ClientEntity client1;
    private ClientEntity client2;
    private ToolEntity toolA;
    private ToolEntity toolB;


    @BeforeEach
    void setUp() {

        // Test entities
        client1 = ClientEntity.builder().id(1L).name("Cliente Juan").build();
        client2 = ClientEntity.builder().id(2L).name("Cliente Maria").build();
        toolA = ToolEntity.builder().id(10L).name("Martillo").build();
        toolB = ToolEntity.builder().id(20L).name("Taladro").build();
        
    }

    // =======================================================================
    // EPIC 6: RF 6.1 - LOANS BY STATUS (getLoansByStatus)
    // =======================================================================

    @Test
    void getLoansByStatus_Active_Success() {
        // ARRANGE
        LoanEntity activeLoan = LoanEntity.builder().status(LoanStatus.ACTIVE).build();
        when(loanRepository.findByStatus(LoanStatus.ACTIVE)).thenReturn(List.of(activeLoan));

        // ACT
        List<LoanEntity> result = reportService.getLoansByStatus("ACTIVE", null, null);

        // ASSERT
        assertEquals(1, result.size());
        assertEquals(LoanStatus.ACTIVE, result.get(0).getStatus());
    }
    
    @Test
    void getLoansByStatus_ThrowsExceptionForInvalidStatus() {
        // ACT & ASSERT: an unknown status cannot be converted to LoanStatus
        assertThrows(IllegalArgumentException.class, () -> {
            reportService.getLoansByStatus("INVALID_STATUS", null, null);
        }, "Must throw when the status does not exist in the enum.");
    }

    // =======================================================================
    // EPIC 6: RF 6.2 - CLIENTS WITH LATE LOANS (getClientsWithLateLoans)
    // =======================================================================

    @Test
    void getClientsWithLateLoans_ReturnsUniqueClients() {
        // ARRANGE: client 1 has two LATE loans, client 2 has one
        LoanEntity lateLoanForC1 = LoanEntity.builder().client(client1).status(LoanStatus.LATE).build();
        LoanEntity anotherLateLoanForC1 = LoanEntity.builder().client(client1).status(LoanStatus.LATE).build();
        LoanEntity lateLoanForC2 = LoanEntity.builder().client(client2).status(LoanStatus.LATE).build();

        when(loanRepository.findByStatus(LoanStatus.LATE)).thenReturn(List.of(lateLoanForC1, anotherLateLoanForC1, lateLoanForC2));

        // ACT
        List<ClientEntity> result = reportService.getClientsWithLateLoans(null, null);

        // ASSERT: 2 distinct clients (.distinct())
        assertEquals(2, result.size());
        assertTrue(result.stream().anyMatch(c -> c.getId().equals(1L)), "Must contain client 1");
        assertTrue(result.stream().anyMatch(c -> c.getId().equals(2L)), "Must contain client 2");
    }
    
    // =======================================================================
    // EPIC 6: RF 6.3 - TOOL RANKING (getTopTools)
    // =======================================================================

    @Test
    void getTopTools_ReturnsRankingFromRepository() {
        // ARRANGE: result of the query (Object[]: ToolEntity, count)
        LocalDate start = LocalDate.of(2025, 1, 1);
        LocalDate end = LocalDate.of(2025, 1, 31);
        
        List<Object[]> mockRanking = List.of(
            new Object[]{toolA, 5L}, // loaned 5 times
            new Object[]{toolB, 3L} // loaned 3 times
        );

        when(loanRepository.findTopToolsByDateRange(start, end)).thenReturn(mockRanking);

        // ACT
        List<Object[]> result = reportService.getTopTools(start, end);

        // ASSERT
        assertEquals(2, result.size());
        // order and data
        assertEquals(toolA, result.get(0)[0]);
        assertEquals(5L, result.get(0)[1]);
        verify(loanRepository, times(1)).findTopToolsByDateRange(start, end);
    }
    
    // =======================================================================
    // METHOD: getLoansByStatus (with date filter)
    // =======================================================================

    @Test
    void getLoansByStatus_WithDateFilter_Success() {
        // ARRANGE
        LocalDate from = LocalDate.of(2025, 1, 1);
        LocalDate to = LocalDate.of(2025, 1, 31);
        LoanEntity loan = LoanEntity.builder().status(LoanStatus.ACTIVE).startDate(from.plusDays(5)).build();
        when(loanRepository.findByStatusAndStartDateBetween(LoanStatus.ACTIVE, from, to)).thenReturn(List.of(loan));

        // ACT
        List<LoanEntity> result = reportService.getLoansByStatus("ACTIVE", from, to);

        // ASSERT
        assertEquals(1, result.size());
        verify(loanRepository, times(1)).findByStatusAndStartDateBetween(LoanStatus.ACTIVE, from, to);
        verify(loanRepository, never()).findByStatus(any()); // the unfiltered query is NOT used
    }

    @Test
    void getLoansByStatus_WithDateFilter_FailsIfFromAfterTo() {
        // ARRANGE
        LocalDate from = LocalDate.of(2025, 1, 31);
        LocalDate to = LocalDate.of(2025, 1, 1);

        // ACT & ASSERT
        assertThrows(IllegalArgumentException.class, () -> {
            reportService.getLoansByStatus("ACTIVE", from, to);
        });
        verify(loanRepository, never()).findByStatusAndStartDateBetween(any(), any(), any());
    }

    // =======================================================================
    // METHOD: getClientsWithLateLoans (with date filter)
    // =======================================================================

     @Test
    void getClientsWithLateLoans_WithDateFilter_Success() {
        // ARRANGE
        LocalDate from = LocalDate.of(2025, 2, 1);
        LocalDate to = LocalDate.of(2025, 2, 28);
        when(loanRepository.findDistinctClientsByStatusAndStartDateBetween(LoanStatus.LATE, from, to))
                .thenReturn(List.of(client1)); // only client1 matches

        // ACT
        List<ClientEntity> result = reportService.getClientsWithLateLoans(from, to);

        // ASSERT
        assertEquals(1, result.size());
        assertEquals(client1.getId(), result.get(0).getId());
        verify(loanRepository, times(1)).findDistinctClientsByStatusAndStartDateBetween(LoanStatus.LATE, from, to);
        verify(loanRepository, never()).findByStatus(LoanStatus.LATE); // the unfiltered query is NOT used
    }

     @Test
    void getClientsWithLateLoans_WithDateFilter_FailsIfFromAfterTo() {
        // ARRANGE
        LocalDate from = LocalDate.of(2025, 2, 28);
        LocalDate to = LocalDate.of(2025, 2, 1);

        // ACT & ASSERT
        assertThrows(IllegalArgumentException.class, () -> {
            reportService.getClientsWithLateLoans(from, to);
        });
        verify(loanRepository, never()).findDistinctClientsByStatusAndStartDateBetween(any(), any(), any());
    }

    // =======================================================================
    // METHOD: getTopTools (date validations)
    // =======================================================================

    @Test
    void getTopTools_FailsIfFromDateIsNull() {
         // ACT & ASSERT
        assertThrows(IllegalArgumentException.class, () -> {
            reportService.getTopTools(null, LocalDate.now());
        });
        verify(loanRepository, never()).findTopToolsByDateRange(any(), any());
    }

    @Test
    void getTopTools_FailsIfToDateIsNull() {
        // ACT & ASSERT
        assertThrows(IllegalArgumentException.class, () -> {
            reportService.getTopTools(LocalDate.now(), null);
        });
        verify(loanRepository, never()).findTopToolsByDateRange(any(), any());
    }

    @Test
    void getTopTools_FailsIfFromDateAfterToDate() {
        // ARRANGE
        LocalDate from = LocalDate.of(2025, 1, 31);
        LocalDate to = LocalDate.of(2025, 1, 1);

        // ACT & ASSERT
        assertThrows(IllegalArgumentException.class, () -> {
            reportService.getTopTools(from, to);
        });
        verify(loanRepository, never()).findTopToolsByDateRange(any(), any());
    }
}