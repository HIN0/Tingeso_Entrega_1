package com.ProyectoTGS.BackCore.Services;

import entities.ToolEntity;
import entities.UserEntity;
import entities.enums.MovementType;
import entities.enums.ToolStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import repositories.ToolRepository;
import services.KardexService;
import services.ToolService;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Optional;

@ExtendWith(MockitoExtension.class)
public class ToolServiceTest {

    // MOCKS: Simulamos las dependencias
    @Mock
    private ToolRepository toolRepository;
    @Mock
    private KardexService kardexService;

    // INJECTMOCKS: Inyecta los mocks en la clase a probar
    @InjectMocks
    private ToolService toolService;

    private ToolEntity tool;
    private UserEntity testUser;

    @BeforeEach
    void setUp() {
        // Inicializamos una herramienta base para la mayoría de los tests
        tool = ToolEntity.builder()
                .id(1L)
                .name("Taladro Bosch")
                .category("Electric Tools")
                .status(ToolStatus.AVAILABLE)
                .stock(5)
                .replacementValue(45000)
                .build();
        // Usuario genérico para las operaciones que registran Kardex
        testUser = UserEntity.builder().username("test_user").id(1L).build();
    }
    
    // =======================================================================
    // ÉPICA 1: CREACIÓN DE HERRAMIENTAS (createTool)
    // =======================================================================

    @Test
    void createTool_SuccessAndRegistersKardex() {
        // ARRANGE: Simular que la persistencia funciona
        when(toolRepository.save(any(ToolEntity.class))).thenReturn(tool);

        // ACT
        ToolEntity created = toolService.createTool(tool, testUser);

        // ASSERT
        // Verifica que los valores por defecto se asignen y que se haya guardado
        assertNotNull(created);
        assertEquals(ToolStatus.AVAILABLE, created.getStatus()); 
        // Verifica el registro de auditoría (Kardex - Épica 5)
        verify(kardexService, times(1)).registerMovement(eq(tool), eq(MovementType.INCOME), eq(1), eq(testUser));
        verify(toolRepository, times(1)).save(tool);
    }
    
    @Test
    void createTool_FailsWhenRequiredFieldsAreNull() {
        // ARRANGE: Probar con un campo nulo (name)
        tool.setName(null);
        
        // ACT & ASSERT: Debe lanzar excepción de argumento ilegal
        assertThrows(IllegalArgumentException.class, () -> {
            toolService.createTool(tool, testUser);
        }, "Debe fallar si el nombre es nulo.");
        // Verifica que no se haya intentado guardar ni registrar Kardex
        verify(toolRepository, never()).save(any());
        verify(kardexService, never()).registerMovement(any(), any(), anyInt(), any());
    }

    // =======================================================================
    // ÉPICA 1: BAJA DE HERRAMIENTAS (decommissionTool)
    // =======================================================================

    @Test
    void decommissionTool_SuccessAndRegistersKardex() {
        // ARRANGE
        when(toolRepository.findById(1L)).thenReturn(Optional.of(tool));
        when(toolRepository.save(any(ToolEntity.class))).thenReturn(tool);

        // ACT
        ToolEntity decommissioned = toolService.decommissionTool(1L, testUser);

        // ASSERT
        // Verifica que el estado y stock se actualicen correctamente
        assertEquals(ToolStatus.DECOMMISSIONED, decommissioned.getStatus());
        assertEquals(0, decommissioned.getStock());
        // Verifica el registro de auditoría (Kardex - Épica 5)
        verify(kardexService, times(1)).registerMovement(eq(tool), eq(MovementType.DECOMMISSION), eq(1), eq(testUser));
        verify(toolRepository, times(1)).save(tool);
    }
    
    @Test
    void decommissionTool_FailsWhenToolIsLoaned() {
        // ÉPICA 1: RN - No se puede dar de baja si está prestada
        // ARRANGE
        tool.setStatus(ToolStatus.LOANED);
        when(toolRepository.findById(1L)).thenReturn(Optional.of(tool));

        // ACT & ASSERT
        assertThrows(IllegalStateException.class, () -> {
            toolService.decommissionTool(1L, testUser);
        }, "No se puede dar de baja una herramienta prestada.");
        verify(toolRepository, never()).save(any());
    }

    @Test
    void decommissionTool_FailsWhenToolIsUnderRepair() {
        // ÉPICA 1: RN - No se puede dar de baja si está en reparación
        // ARRANGE
        tool.setStatus(ToolStatus.REPAIRING);
        when(toolRepository.findById(1L)).thenReturn(Optional.of(tool));

        // ACT & ASSERT
        assertThrows(IllegalStateException.class, () -> {
            toolService.decommissionTool(1L, testUser);
        }, "No se puede dar de baja una herramienta en reparación.");
        verify(toolRepository, never()).save(any());
    }
    
    // =======================================================================
    // ÉPICA 2: MÉTODOS DE SOPORTE PARA PRÉSTAMOS/DEVOLUCIONES
    // =======================================================================

    @Test
    void decrementStockForLoan_Success() {
        // ÉPICA 2 / 5: Préstamo exitoso
        // ACT
        toolService.decrementStockForLoan(tool, testUser);

        // ASSERT
        assertEquals(4, tool.getStock()); // Stock decrementa de 5 a 4
        assertEquals(ToolStatus.AVAILABLE, tool.getStatus());
        // Verifica que se registre el Kardex (MovementType.LOAN)
        verify(kardexService, times(1)).registerMovement(eq(tool), eq(MovementType.LOAN), eq(1), eq(testUser));
        verify(toolRepository, times(1)).save(tool);
    }
    
    @Test
    void decrementStockForLoan_SetsStatusToLoanedWhenStockReachesZero() {
        // ÉPICA 2: RN - Actualizar estado a PRESTADA si el stock llega a 0
        // ARRANGE
        tool.setStock(1);
        
        // ACT
        toolService.decrementStockForLoan(tool, testUser);

        // ASSERT
        assertEquals(0, tool.getStock());
        assertEquals(ToolStatus.LOANED, tool.getStatus()); // Estado cambia a PRESTADA
    }
    
    @Test
    void incrementStockForReturn_Success() {
        // ÉPICA 2 / 5: Devolución exitosa (sin daño)
        // ARRANGE
        tool.setStock(0);
        tool.setStatus(ToolStatus.LOANED);
        
        // ACT
        toolService.incrementStockForReturn(tool, testUser);

        // ASSERT
        assertEquals(1, tool.getStock());
        assertEquals(ToolStatus.AVAILABLE, tool.getStatus()); // Estado vuelve a DISPONIBLE
        // Verifica que se registre el Kardex (MovementType.RETURN)
        verify(kardexService, times(1)).registerMovement(eq(tool), eq(MovementType.RETURN), eq(1), eq(testUser));
        verify(toolRepository, times(1)).save(tool);
    }
    
    @Test
    void markAsRepairing_Success() {
        // ÉPICA 1 / 5: Marcar para reparación
        // ACT
        toolService.markAsRepairing(tool, testUser);
        
        // ASSERT
        assertEquals(ToolStatus.REPAIRING, tool.getStatus());
        // Verifica que se registre el Kardex (MovementType.REPAIR)
        verify(kardexService, times(1)).registerMovement(eq(tool), eq(MovementType.REPAIR), eq(1), eq(testUser));
    }
    
    @Test
    void markAsDecommissioned_SetsStockToZeroAndRegistersKardex() {
        // ÉPICA 1 / 5: Marcar como dado de baja (usado después de una devolución irreparable)
        // ACT
        toolService.markAsDecommissioned(tool, testUser);
        
        // ASSERT
        assertEquals(ToolStatus.DECOMMISSIONED, tool.getStatus());
        assertEquals(0, tool.getStock());
        // Verifica que se registre el Kardex (MovementType.DECOMMISSION)
        verify(kardexService, times(1)).registerMovement(eq(tool), eq(MovementType.DECOMMISSION), eq(1), eq(testUser));
    }
}