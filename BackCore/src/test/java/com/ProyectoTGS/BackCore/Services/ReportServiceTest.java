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
import repositories.ClientRepository;
import repositories.LoanRepository;
import repositories.ToolRepository;
import services.ReportService;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ReportServiceTest {

    @Mock
    private LoanRepository loanRepository;
    @Mock
    private ClientRepository clientRepository;
    @Mock 
    private ToolRepository toolRepository; 

    @InjectMocks
    private ReportService reportService;

    private ClientEntity client1;
    private ClientEntity client2;
    private ToolEntity toolA;
    private ToolEntity toolB;


    @BeforeEach
    void setUp() {

        // Inicializar entidades de prueba
        client1 = ClientEntity.builder().id(1L).name("Cliente Juan").build();
        client2 = ClientEntity.builder().id(2L).name("Cliente Maria").build();
        toolA = ToolEntity.builder().id(10L).name("Martillo").build();
        toolB = ToolEntity.builder().id(20L).name("Taladro").build();
        
    }

    // =======================================================================
    // ÉPICA 6: RF 6.1 - PRÉSTAMOS POR ESTADO (getLoansByStatus)
    // =======================================================================

    @Test
    void getLoansByStatus_Active_Success() {
        // ARRANGE
        LoanEntity activeLoan = LoanEntity.builder().status(LoanStatus.ACTIVE).build();
        when(loanRepository.findByStatus(LoanStatus.ACTIVE)).thenReturn(List.of(activeLoan));

        // ACT
        List<LoanEntity> result = reportService.getLoansByStatus("ACTIVE");

        // ASSERT
        assertEquals(1, result.size());
        assertEquals(LoanStatus.ACTIVE, result.get(0).getStatus());
    }
    
    @Test
    void getLoansByStatus_ThrowsExceptionForInvalidStatus() {
        // ACT & ASSERT: Debe fallar al intentar convertir un string inválido a LoanStatus
        assertThrows(IllegalArgumentException.class, () -> {
            reportService.getLoansByStatus("INVALID_STATUS");
        }, "Debe lanzar excepción si el estado no existe en el Enum.");
    }

    // =======================================================================
    // ÉPICA 6: RF 6.2 - CLIENTES CON PRÉSTAMOS ATRASADOS (getClientsWithLateLoans)
    // =======================================================================

    @Test
    void getClientsWithLateLoans_ReturnsUniqueClients() {
        // ARRANGE: Cliente 1 tiene dos préstamos LATE, Cliente 2 tiene uno
        LoanEntity lateLoanForC1 = LoanEntity.builder().client(client1).status(LoanStatus.LATE).build();
        LoanEntity anotherLateLoanForC1 = LoanEntity.builder().client(client1).status(LoanStatus.LATE).build();
        LoanEntity lateLoanForC2 = LoanEntity.builder().client(client2).status(LoanStatus.LATE).build();

        when(loanRepository.findByStatus(LoanStatus.LATE)).thenReturn(List.of(lateLoanForC1, anotherLateLoanForC1, lateLoanForC2));

        // ACT
        List<ClientEntity> result = reportService.getClientsWithLateLoans();

        // ASSERT: Debe devolver 2 clientes únicos (se usa .distinct())
        assertEquals(2, result.size());
        assertTrue(result.stream().anyMatch(c -> c.getId().equals(1L)), "Debe contener al Cliente 1");
        assertTrue(result.stream().anyMatch(c -> c.getId().equals(2L)), "Debe contener al Cliente 2");
    }

    // =======================================================================
    // ÉPICA 6: RF 6.3 - RANKING DE HERRAMIENTAS (getTopTools)
    // =======================================================================

    @Test
    void getTopTools_ReturnsRankingFromRepository() {
        // ARRANGE: Simular el resultado de la consulta SQL (Object[]: ToolEntity, Count)
        LocalDate start = LocalDate.of(2025, 1, 1);
        LocalDate end = LocalDate.of(2025, 1, 31);
        
        List<Object[]> mockRanking = List.of(
            new Object[]{toolA, 5L}, // 5 veces prestada
            new Object[]{toolB, 3L}  // 3 veces prestada
        );

        when(loanRepository.findTopToolsByDateRange(start, end)).thenReturn(mockRanking);

        // ACT
        List<Object[]> result = reportService.getTopTools(start, end);

        // ASSERT
        assertEquals(2, result.size());
        // Verificar el orden y los datos
        assertEquals(toolA, result.get(0)[0]);
        assertEquals(5L, result.get(0)[1]);
        verify(loanRepository, times(1)).findTopToolsByDateRange(start, end);
    }
    
    // =======================================================================
    // CLIENTES RESTRINGIDOS (getRestrictedClients)
    // =======================================================================
    
    @Test
    void getRestrictedClients_Success() {
        // ARRANGE
        ClientEntity restrictedClient = ClientEntity.builder().status(ClientStatus.RESTRICTED).build();
        when(clientRepository.findByStatus(ClientStatus.RESTRICTED)).thenReturn(List.of(restrictedClient));

        // ACT
        List<ClientEntity> result = reportService.getRestrictedClients();

        // ASSERT
        assertEquals(1, result.size());
        assertEquals(ClientStatus.RESTRICTED, result.get(0).getStatus());
    }
}