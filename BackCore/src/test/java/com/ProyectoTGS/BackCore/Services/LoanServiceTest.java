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

    // DECLARACIÓN DE DEPENDENCIAS (Mocks simulan el comportamiento real de otras capas)
    @Mock private LoanRepository loanRepository;
    @Mock private ClientRepository clientRepository;
    @Mock private ToolRepository toolRepository;
    @Mock private ToolService toolService;
    @Mock private TariffService tariffService;
    @Mock private ClientService clientService;
    @Mock private KardexService kardexService;

    @InjectMocks
    private LoanService loanService;

    // Entidades de prueba (SETUP)
    private ClientEntity clientActive;
    private ClientEntity clientRestricted;
    private ToolEntity toolAvailable;
    private UserEntity testUser;

    @BeforeEach
    void setUp() {
        // Configuramos entidades base que se usan en la mayoría de los tests
        clientActive = ClientEntity.builder().id(1L).status(ClientStatus.ACTIVE).build();
        clientRestricted = ClientEntity.builder().id(2L).status(ClientStatus.RESTRICTED).build();
        
        // La herramienta debe tener stock > 0 para que la mayoría de los préstamos pasen
        toolAvailable = ToolEntity.builder().id(10L).stock(1).status(ToolStatus.AVAILABLE).replacementValue(45000).build();
        testUser = UserEntity.builder().username("test_user").id(1L).build();
        
        // Re-inicializamos el servicio para cada prueba
        loanService = new LoanService(loanRepository, clientRepository, toolRepository, toolService, kardexService, tariffService, clientService);
    }

    // =========================================================================================================
    // ÉPICA 2: TESTS PARA createLoan (Validaciones de Préstamo)
    // =========================================================================================================

    @Test
    void createLoan_Success() {
        // ARRANGE
        LocalDate today = LocalDate.now();
        LocalDate dueDate = today.plusDays(7);
        LoanEntity newLoan = LoanEntity.builder().client(clientActive).tool(toolAvailable).startDate(today).dueDate(dueDate).status(LoanStatus.ACTIVE).totalPenalty(0.0).build();

        // MOCKEO: Simular que el cliente, herramienta existen, no hay préstamos y la persistencia funciona
        when(clientRepository.findById(1L)).thenReturn(Optional.of(clientActive));
        when(toolRepository.findById(10L)).thenReturn(Optional.of(toolAvailable));
        when(loanRepository.findAll()).thenReturn(Collections.emptyList());
        when(loanRepository.save(any(LoanEntity.class))).thenReturn(newLoan);

        // ACT
        LoanEntity createdLoan = loanService.createLoan(1L, 10L, today, dueDate, testUser);

        // ASSERT
        // Confirma el estado y la manipulación de inventario/Kardex (Épica 2, 5)
        assertNotNull(createdLoan);
        assertEquals(LoanStatus.ACTIVE, createdLoan.getStatus());
        verify(toolService, times(1)).decrementStockForLoan(toolAvailable, testUser);
    }

    @Test
    void createLoan_FailsWhenClientIsRestricted() {
        // ÉPICA 2 / 3: RN - No prestar a clientes restringidos
        // ARRANGE: Cliente 2 es RESTRICTED y las entidades existen
        LocalDate today = LocalDate.now();
        LocalDate dueDate = today.plusDays(7);
        when(clientRepository.findById(2L)).thenReturn(Optional.of(clientRestricted));
        when(toolRepository.findById(10L)).thenReturn(Optional.of(toolAvailable)); 

        // ACT & ASSERT: Debe lanzar la excepción de restricción antes de guardar
        assertThrows(IllegalStateException.class, () -> 
            loanService.createLoan(2L, 10L, today, dueDate, testUser), 
            "Debe lanzar IllegalStateException porque el cliente está restringido.");
            verify(loanRepository, never()).save(any());
    }
    
    @Test
    void createLoan_FailsWhenDueDateIsBeforeStartDate() {
        // ÉPICA 2: RN - Validación de fechas
        // ARRANGE: dueDate es anterior a startDate
        LocalDate startDate = LocalDate.now();
        LocalDate dueDate = startDate.minusDays(1);
        when(clientRepository.findById(1L)).thenReturn(Optional.of(clientActive));
        when(toolRepository.findById(10L)).thenReturn(Optional.of(toolAvailable));

        // ACT & ASSERT: Debe lanzar la excepción de argumento ilegal
        assertThrows(IllegalArgumentException.class, () -> 
            loanService.createLoan(1L, 10L, startDate, dueDate, testUser));
        verify(loanRepository, never()).save(any());
    }

    @Test
    void createLoan_FailsWhenLimitOfFiveActiveLoansIsReached() {
        // ÉPICA 2: RN - Límite de 5 préstamos activos
        // ARRANGE: Simular 5 préstamos activos para el cliente 1L
        LocalDate today = LocalDate.now();
        LocalDate dueDate = today.plusDays(7);

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

        // ACT & ASSERT: Intenta crear el sexto préstamo
        assertThrows(IllegalStateException.class, () -> 
            loanService.createLoan(1L, 10L, today, dueDate, testUser), 
            "Debe fallar al alcanzar el límite de 5 préstamos activos.");
        verify(loanRepository, never()).save(any());
    }

    @Test
    void createLoan_FailsWhenToolIsLoanedOrStockIsZero() {
        // ÉPICA 1 / 2: RN - Validación de disponibilidad y stock
        // ARRANGE: Caso 1: Herramienta prestada (LOANED)
        ToolEntity toolLoaned = ToolEntity.builder().id(20L).stock(0).status(ToolStatus.LOANED).build();
        when(clientRepository.findById(1L)).thenReturn(Optional.of(clientActive));
        when(toolRepository.findById(20L)).thenReturn(Optional.of(toolLoaned));

        // ACT & ASSERT: Debe fallar si el estado no es AVAILABLE
        assertThrows(IllegalStateException.class, () -> 
            loanService.createLoan(1L, 20L, LocalDate.now(), LocalDate.now().plusDays(1), testUser));
        
        // ARRANGE: Caso 2: Stock es 0 pero estado es AVAILABLE (debe fallar)
        toolLoaned.setStock(0);
        toolLoaned.setStatus(ToolStatus.AVAILABLE);
        
        // ACT & ASSERT: Debe fallar si el stock es cero
        assertThrows(IllegalStateException.class, () -> 
            loanService.createLoan(1L, 20L, LocalDate.now(), LocalDate.now().plusDays(1), testUser));
        
        verify(loanRepository, never()).save(any());
    }
    
    @Test
    void createLoan_FailsWhenClientAlreadyHasThisToolActive() {
        // ÉPICA 2: RN - No duplicidad de herramienta
        // ARRANGE: Simular que el cliente 1 ya tiene la herramienta 10 activa
        LocalDate today = LocalDate.now();
        LocalDate dueDate = today.plusDays(7);

        LoanEntity activeLoanForSameTool = LoanEntity.builder().client(clientActive).tool(toolAvailable).status(LoanStatus.ACTIVE).build();

        when(clientRepository.findById(1L)).thenReturn(Optional.of(clientActive));
        when(toolRepository.findById(10L)).thenReturn(Optional.of(toolAvailable));
        when(loanRepository.findAll()).thenReturn(Collections.singletonList(activeLoanForSameTool));

        // ACT & ASSERT: Intenta crear el préstamo con la misma herramienta
        assertThrows(IllegalStateException.class, () -> 
            loanService.createLoan(1L, 10L, today, dueDate, testUser),
            "Debe fallar si el cliente ya tiene esta herramienta activa.");
        verify(loanRepository, never()).save(any());
    }

    // =========================================================================================================
    // ÉPICA 2: TESTS PARA returnLoan (Devoluciones, Multas y Restricciones)
    // =========================================================================================================

    @Test
    void returnLoan_SuccessOnTimeNoDamage() {
        // ÉPICA 2: Camino feliz de devolución
        // ARRANGE: Préstamo devuelto a tiempo (dueDate = today) y sin multas/daños
        LocalDate dueDate = LocalDate.now();
        Long loanId = 5L;
        
        LoanEntity loan = LoanEntity.builder().id(loanId).client(clientActive).tool(toolAvailable).startDate(dueDate.minusDays(5)).dueDate(dueDate).status(LoanStatus.ACTIVE).totalPenalty(0.0).build();

        when(loanRepository.findById(loanId)).thenReturn(Optional.of(loan));
        when(toolRepository.findById(10L)).thenReturn(Optional.of(toolAvailable));
        when(loanRepository.save(any(LoanEntity.class))).thenReturn(loan); // MOCKEO CRÍTICO
        
        // ACT (Devuelto hoy, a tiempo)
        LoanEntity returnedLoan = loanService.returnLoan(loanId, 10L, false, false, testUser, LocalDate.now());
        
        // ASSERT
        // Confirma que el estado se cierra y no hay penalidad
        assertEquals(LoanStatus.CLOSED, returnedLoan.getStatus());
        assertEquals(0.0, returnedLoan.getTotalPenalty());

        // Confirma que se actualiza el stock (Épica 1) y NO se restringe al cliente (Épica 3)
        verify(toolService, times(1)).incrementStockForReturn(toolAvailable, testUser);
        verify(clientService, never()).updateStatus(anyLong(), any());
    }

    @Test
    void returnLoan_AppliesLateFeeAndRestrictsClient_RN() {
        // ÉPICA 2 / 4 / 3: RN - Multa por atraso y restricción de cliente
        // ARRANGE: Préstamo atrasado 2 días (dueDate = today - 2)
        LocalDate dueDate = LocalDate.now().minusDays(2); 
        Long loanId = 6L;
        
        LoanEntity loan = LoanEntity.builder().id(loanId).client(clientActive).tool(toolAvailable).startDate(dueDate.minusDays(5)).dueDate(dueDate).status(LoanStatus.ACTIVE).totalPenalty(0.0).build();

        when(loanRepository.findById(loanId)).thenReturn(Optional.of(loan));
        when(toolRepository.findById(10L)).thenReturn(Optional.of(toolAvailable));
        when(tariffService.getDailyLateFee()).thenReturn(2000.0); // Tarifa diaria de multa
        
        // ACT (Se devuelve hoy, 2 días tarde)
        loanService.returnLoan(loanId, 10L, false, false, testUser, LocalDate.now());
        
        // ASSERT
        // Multa: 2 días * 2000 = 4000
        assertEquals(4000.0, loan.getTotalPenalty());
        // RN CRÍTICO: Debe restringir al cliente por la multa (Épica 3)
        verify(clientService, times(1)).updateStatus(clientActive.getId(), ClientStatus.RESTRICTED); 
        verify(toolService, times(1)).incrementStockForReturn(toolAvailable, testUser);
    }
    
    @Test
    void returnLoan_AppliesRepairFeeMarksRepairingAndRestrictsClient_RN() {
        // ÉPICA 1 / 2 / 4: RN - Daño reparable
        // ARRANGE: Préstamo a tiempo (delay = 0).
        Long loanId = 8L;
        LocalDate dueDate = LocalDate.now(); 
        
        LoanEntity loan = LoanEntity.builder()
            .id(loanId)
            .client(clientActive)
            .tool(toolAvailable)
            .dueDate(dueDate) 
            .status(LoanStatus.ACTIVE)
            .totalPenalty(0.0).build();
        
        when(loanRepository.findById(loanId)).thenReturn(Optional.of(loan));
        when(toolRepository.findById(10L)).thenReturn(Optional.of(toolAvailable));
        when(tariffService.getRepairFee()).thenReturn(1500.0); 
        
        // ACT (Devuelto dañado, NO irreparable)
        loanService.returnLoan(loanId, 10L, true, false, testUser, LocalDate.now());
        
        // ASSERT
        // Penalización: 1500 (tarifa de reparación)
        assertEquals(1500.0, loan.getTotalPenalty());
        // RN CRÍTICO: Debe restringir al cliente por el cargo (Épica 3)
        verify(clientService, times(1)).updateStatus(clientActive.getId(), ClientStatus.RESTRICTED);
        // Debe marcarse como REPAIRING (Épica 1)
        verify(toolService, times(1)).markAsRepairing(toolAvailable, testUser);
    }

    @Test
    void returnLoan_AppliesReplacementFeeAndRestrictsClient_RN() {
        // ÉPICA 1 / 2 / 4: RN - Daño irreparable (Baja definitiva)
        // ARRANGE: Préstamo con valor de reposición de 50000
        Long loanId = 7L;
        ToolEntity irreparableTool = ToolEntity.builder().id(11L).replacementValue(50000).build();
        
        // CLAVE: Añadir dueDate para evitar el NullPointerException en ChronoUnit
        LoanEntity loan = LoanEntity.builder()
            .id(loanId)
            .client(clientActive)
            .tool(irreparableTool)
            .dueDate(LocalDate.now().minusDays(10)) 
            .status(LoanStatus.ACTIVE)
            .totalPenalty(0.0).build();
        
        when(loanRepository.findById(loanId)).thenReturn(Optional.of(loan));
        when(toolRepository.findById(11L)).thenReturn(Optional.of(irreparableTool));
        when(tariffService.getDailyLateFee()).thenReturn(0.0); // Aislamos el cargo de reposición

        // ACT (Devuelto dañado e irreparable)
        loanService.returnLoan(loanId, 11L, true, true, testUser, LocalDate.now());
        
        // ASSERT
        // El cargo final debe ser: 50000 (reposición)
        assertEquals(50000.0, loan.getTotalPenalty());
        // RN CRÍTICO: Debe restringir al cliente por el cargo
        verify(clientService, times(1)).updateStatus(clientActive.getId(), ClientStatus.RESTRICTED);
        // Debe marcarse como DECOMMISSIONED (Épica 1)
        verify(toolService, times(1)).markAsDecommissioned(irreparableTool, testUser);
    }
    
    @Test
    void returnLoan_FailsWhenLoanStatusIsClosed() {
        // ÉPICA 2: Validación de estado del préstamo
        // ARRANGE: Simular un préstamo ya cerrado (CLOSED)
        Long loanId = 9L;
        LoanEntity loan = LoanEntity.builder().id(loanId).tool(toolAvailable).status(LoanStatus.CLOSED).build();

        when(loanRepository.findById(loanId)).thenReturn(Optional.of(loan));
        when(toolRepository.findById(10L)).thenReturn(Optional.of(toolAvailable));

        // ACT & ASSERT: Debe fallar si el estado no es ACTIVE o LATE
        assertThrows(IllegalStateException.class, () -> 
            loanService.returnLoan(loanId, 10L, false, false, testUser, LocalDate.now()),
            "Solo se pueden devolver préstamos activos o atrasados.");
        verify(loanRepository, never()).save(any());
    }
}