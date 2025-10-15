package com.ProyectoTGS.BackCore.Services;

import entities.*;
import entities.enums.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import repositories.*;
import services.*;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class LoanServiceTest {

    // MOCKS: Simulamos las dependencias
    @Mock private LoanRepository loanRepository;
    @Mock private ClientRepository clientRepository;
    @Mock private ToolRepository toolRepository;
    @Mock private ToolService toolService;
    @Mock private TariffService tariffService;
    @Mock private ClientService clientService;
    @Mock private KardexService kardexService;

    @InjectMocks
    private LoanService loanService;

    // Entidades de prueba
    private ClientEntity clientActive;
    private ClientEntity clientRestricted;
    private ToolEntity toolAvailable;
    private UserEntity testUser;

    @BeforeEach
    void setUp() {
        // Inicialización de entidades simuladas
        clientActive = ClientEntity.builder().id(1L).status(ClientStatus.ACTIVE).build();
        clientRestricted = ClientEntity.builder().id(2L).status(ClientStatus.RESTRICTED).build();
        
        toolAvailable = ToolEntity.builder().id(10L).stock(1).status(ToolStatus.AVAILABLE).replacementValue(45000).build();
        testUser = UserEntity.builder().username("test_user").id(1L).build();
        
        // El constructor necesita todos los mocks para inicializarse
        loanService = new LoanService(loanRepository, clientRepository, toolRepository, toolService, kardexService, tariffService, clientService);
    }

    // =========================================================================================================
    // TESTS PARA createLoan (Validaciones de Negocio)
    // =========================================================================================================

    @Test
    void createLoan_Success() {
        // Arrange
        LocalDate today = LocalDate.now();
        LocalDate dueDate = today.plusDays(7);
        LoanEntity newLoan = LoanEntity.builder().client(clientActive).tool(toolAvailable).startDate(today).dueDate(dueDate).status(LoanStatus.ACTIVE).totalPenalty(0.0).build();

        // Simular que las entidades existen y no hay préstamos activos
        when(clientRepository.findById(1L)).thenReturn(Optional.of(clientActive));
        when(toolRepository.findById(10L)).thenReturn(Optional.of(toolAvailable));
        when(loanRepository.findAll()).thenReturn(Collections.emptyList());
        when(loanRepository.save(any(LoanEntity.class))).thenReturn(newLoan);

        // Act
        LoanEntity createdLoan = loanService.createLoan(1L, 10L, today, dueDate, testUser);

        // Assert
        assertNotNull(createdLoan);
        assertEquals(LoanStatus.ACTIVE, createdLoan.getStatus());
        // Verificar que se decrementó el stock y se registró el Kardex
        verify(toolService, times(1)).decrementStockForLoan(toolAvailable, testUser);
    }

    @Test
    void createLoan_FailsWhenClientIsRestricted() {
        // Arrange
        LocalDate today = LocalDate.now();
        LocalDate dueDate = today.plusDays(7);
        
        // 1. Configurar Cliente: Encontrar el cliente (Restricted)
        when(clientRepository.findById(2L)).thenReturn(Optional.of(clientRestricted));

        // 2. CORRECCIÓN: Configurar Herramienta: Debe encontrar la herramienta para que el código continúe.
        when(toolRepository.findById(10L)).thenReturn(Optional.of(toolAvailable)); 

        // Act & Assert
        // Debe lanzar IllegalStateException (RN)
        assertThrows(IllegalStateException.class, () -> 
            loanService.createLoan(2L, 10L, today, dueDate, testUser), 
            "Debe lanzar IllegalStateException porque el cliente está restringido.");
            
        // Ya que el código falló en la validación de negocio, no debe haber guardado nada.
        verify(loanRepository, never()).save(any());
        
        // Y verificamos que NO se intentó manipular el stock (porque falló antes de eso)
        verify(toolService, never()).decrementStockForLoan(any(), any());
    }
    
    @Test
    void createLoan_FailsWhenDueDateIsBeforeStartDate() {
        // Arrange
        LocalDate startDate = LocalDate.now();
        LocalDate dueDate = startDate.minusDays(1);
        when(clientRepository.findById(1L)).thenReturn(Optional.of(clientActive));
        when(toolRepository.findById(10L)).thenReturn(Optional.of(toolAvailable));

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> 
            loanService.createLoan(1L, 10L, startDate, dueDate, testUser));
        verify(loanRepository, never()).save(any());
    }

    @Test
    void createLoan_FailsWhenLimitOfFiveActiveLoansIsReached() {
        // ARRANGE: Simular 5 préstamos activos para el cliente 1L
        LocalDate today = LocalDate.now();
        LocalDate dueDate = today.plusDays(7);

        // Creamos una lista simulada de 5 préstamos activos
        List<LoanEntity> fiveActiveLoans = List.of(
            LoanEntity.builder().client(clientActive).status(LoanStatus.ACTIVE).build(),
            LoanEntity.builder().client(clientActive).status(LoanStatus.ACTIVE).build(),
            LoanEntity.builder().client(clientActive).status(LoanStatus.ACTIVE).build(),
            LoanEntity.builder().client(clientActive).status(LoanStatus.ACTIVE).build(),
            LoanEntity.builder().client(clientActive).status(LoanStatus.ACTIVE).build()
        );

        when(clientRepository.findById(1L)).thenReturn(Optional.of(clientActive));
        when(toolRepository.findById(10L)).thenReturn(Optional.of(toolAvailable));
        when(loanRepository.findAll()).thenReturn(fiveActiveLoans); // Devuelve 5 préstamos activos

        // ACT & ASSERT
        // Intenta crear el sexto préstamo (debe fallar)
        assertThrows(IllegalStateException.class, () -> 
            loanService.createLoan(1L, 10L, today, dueDate, testUser), 
            "Debe fallar al alcanzar el límite de 5 préstamos activos.");
        verify(loanRepository, never()).save(any());
    }

    @Test
    void createLoan_FailsWhenToolIsLoanedOrStockIsZero() {
        // ARRANGE: Caso 1: Herramienta prestada (LOANED)
        ToolEntity toolLoaned = ToolEntity.builder().id(20L).stock(0).status(ToolStatus.LOANED).build();
        when(clientRepository.findById(1L)).thenReturn(Optional.of(clientActive));
        when(toolRepository.findById(20L)).thenReturn(Optional.of(toolLoaned));

        // ACT & ASSERT: Debe fallar si no está AVAILABLE o stock > 0
        assertThrows(IllegalStateException.class, () -> 
            loanService.createLoan(1L, 20L, LocalDate.now(), LocalDate.now().plusDays(1), testUser));
        
        // ARRANGE: Caso 2: Stock es 0 pero estado es AVAILABLE (debe fallar)
        toolLoaned.setStock(0);
        toolLoaned.setStatus(ToolStatus.AVAILABLE);
        
        // ACT & ASSERT
        assertThrows(IllegalStateException.class, () -> 
            loanService.createLoan(1L, 20L, LocalDate.now(), LocalDate.now().plusDays(1), testUser));
        
        verify(loanRepository, never()).save(any());
    }
    
    @Test
    void createLoan_FailsWhenClientAlreadyHasThisToolActive() {
        // ARRANGE: Simular que el cliente 1 ya tiene la herramienta 10 activa
        LocalDate today = LocalDate.now();
        LocalDate dueDate = today.plusDays(7);

        LoanEntity activeLoanForSameTool = LoanEntity.builder().client(clientActive).tool(toolAvailable).status(LoanStatus.ACTIVE).build();

        when(clientRepository.findById(1L)).thenReturn(Optional.of(clientActive));
        when(toolRepository.findById(10L)).thenReturn(Optional.of(toolAvailable));
        when(loanRepository.findAll()).thenReturn(Collections.singletonList(activeLoanForSameTool));

        // ACT & ASSERT
        assertThrows(IllegalStateException.class, () -> 
            loanService.createLoan(1L, 10L, today, dueDate, testUser),
            "Debe fallar si el cliente ya tiene esta herramienta activa.");
        verify(loanRepository, never()).save(any());
    }
    
    @Test
    void returnLoan_AppliesRepairFeeMarksRepairingAndRestrictsClient_RN() {
        // Arrange
        Long loanId = 8L;
        
        // 1. Establecer la fecha de vencimiento a HOY para asegurar que NO haya atraso (delay = 0).
        LocalDate dueDate = LocalDate.now(); 
        
        LoanEntity loan = LoanEntity.builder()
            .id(loanId)
            .client(clientActive)
            .tool(toolAvailable)
            .dueDate(dueDate) // CORRECCIÓN 1: Asegurar que no esté atrasado.
            .status(LoanStatus.ACTIVE)
            .totalPenalty(0.0).build();
        
        when(loanRepository.findById(loanId)).thenReturn(Optional.of(loan));
        when(toolRepository.findById(10L)).thenReturn(Optional.of(toolAvailable));
        
        // CORRECCIÓN 2: Mockear ambas dependencias, pero para este test 
        when(tariffService.getRepairFee()).thenReturn(1500.0); 
        
        // Act (Devuelto dañado, NO irreparable)
        loanService.returnLoan(loanId, 10L, true, false, testUser, LocalDate.now());
        
        // Assert
        assertEquals(1500.0, loan.getTotalPenalty()); // Solo cargo por reparación
        verify(clientService, times(1)).updateStatus(clientActive.getId(), ClientStatus.RESTRICTED);
        verify(toolService, times(1)).markAsRepairing(toolAvailable, testUser);
    }
    
    @Test
    void returnLoan_FailsWhenLoanStatusIsClosed() {
        // ARRANGE
        Long loanId = 9L;
        // Simular un préstamo ya cerrado (CLOSED)
        LoanEntity loan = LoanEntity.builder().id(loanId).tool(toolAvailable).status(LoanStatus.CLOSED).build();

        when(loanRepository.findById(loanId)).thenReturn(Optional.of(loan));
        when(toolRepository.findById(10L)).thenReturn(Optional.of(toolAvailable));

        // ACT & ASSERT
        assertThrows(IllegalStateException.class, () -> 
            loanService.returnLoan(loanId, 10L, false, false, testUser, LocalDate.now()));
        verify(loanRepository, never()).save(any()); // No debe guardar nada
    }


    // =========================================================================================================
    // TESTS PARA returnLoan (Cálculo de Multas y RN)
    // =========================================================================================================

    @Test
    void returnLoan_SuccessOnTimeNoDamage() {
        // Arrange
        LocalDate dueDate = LocalDate.now();
        Long loanId  = 5L;
        
        LoanEntity loan = LoanEntity.builder().id(loanId).client(clientActive).tool(toolAvailable).startDate(dueDate.minusDays(5)).dueDate(dueDate).status(LoanStatus.ACTIVE).totalPenalty(0.0).build();

        when(loanRepository.findById(loanId)).thenReturn(Optional.of(loan));
        when(toolRepository.findById(10L)).thenReturn(Optional.of(toolAvailable));
        
        // Mockear la función save para que devuelva la entidad modificada.
        when(loanRepository.save(any(LoanEntity.class))).thenReturn(loan);

        // Act (Devuelto hoy, a tiempo)
        LoanEntity returnedLoan = loanService.returnLoan(loanId, 10L, false, false, testUser, LocalDate.now());
        
        // Assert
        assertEquals(LoanStatus.CLOSED, returnedLoan.getStatus());
        assertEquals(0.0, returnedLoan.getTotalPenalty());

        // Se debe incrementar stock y NO debe restringir al cliente
        verify(toolService, times(1)).incrementStockForReturn(toolAvailable, testUser);
        verify(clientService, never()).updateStatus(anyLong(), any());
    }

    @Test
    void returnLoan_AppliesLateFeeAndRestrictsClient_RN() {
        // Arrange
        LocalDate dueDate = LocalDate.now().minusDays(2); // Préstamo atrasado
        Long loanId = 6L;
        
        LoanEntity loan = LoanEntity.builder().id(loanId).client(clientActive).tool(toolAvailable).startDate(dueDate.minusDays(5)).dueDate(dueDate).status(LoanStatus.ACTIVE).totalPenalty(0.0).build();

        when(loanRepository.findById(loanId)).thenReturn(Optional.of(loan));
        when(toolRepository.findById(10L)).thenReturn(Optional.of(toolAvailable));
        when(tariffService.getDailyLateFee()).thenReturn(2000.0);
        
        // Act (Se devuelve hoy, 2 días tarde)
        loanService.returnLoan(loanId, 10L, false, false, testUser, LocalDate.now());
        
        // Assert
        // Multa: 2 días * 2000 = 4000
        assertEquals(4000.0, loan.getTotalPenalty());
        // RN CRÍTICO: Debe restringir al cliente por la multa
        verify(clientService, times(1)).updateStatus(clientActive.getId(), ClientStatus.RESTRICTED); 
        verify(toolService, times(1)).incrementStockForReturn(toolAvailable, testUser);
    }
    
    @Test
    void returnLoan_AppliesReplacementFeeAndRestrictsClient_RN() {
        // Arrange
        Long loanId = 7L;
        ToolEntity irreparableTool = ToolEntity.builder().id(11L).replacementValue(50000).build();
        
        // CLAVE: Añadir dueDate para evitar el NullPointerException.
        LoanEntity loan = LoanEntity.builder()
            .id(loanId)
            .client(clientActive)
            .tool(irreparableTool)
            .dueDate(LocalDate.now().minusDays(10)) // PRÉSTAMO CON VENCIMIENTO PASADO (Para un caso real)
            .status(LoanStatus.ACTIVE)
            .totalPenalty(0.0).build();
        
        when(loanRepository.findById(loanId)).thenReturn(Optional.of(loan));
        when(toolRepository.findById(11L)).thenReturn(Optional.of(irreparableTool));
        // Necesitamos simular que el cargo diario por atraso es 0 para aislar el cargo por reposición.
        when(tariffService.getDailyLateFee()).thenReturn(0.0); 

        // Act (Devuelto dañado e irreparable)
        loanService.returnLoan(loanId, 11L, true, true, testUser, LocalDate.now());
        
        // Assert
        // El cargo final debe ser: 50000 (reposición) + 0 (multa por atraso)
        assertEquals(50000.0, loan.getTotalPenalty());
        // RN CRÍTICO: Debe restringir al cliente por el cargo
        verify(clientService, times(1)).updateStatus(clientActive.getId(), ClientStatus.RESTRICTED);
        // Debe marcarse como DECOMMISSIONED
        verify(toolService, times(1)).markAsDecommissioned(irreparableTool, testUser);
    }
}