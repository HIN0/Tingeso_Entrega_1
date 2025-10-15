package com.ProyectoTGS.BackCore.Services;

import entities.KardexEntity;
import entities.ToolEntity;
import entities.UserEntity;
import entities.enums.MovementType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import repositories.KardexRepository;
import services.KardexService;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class KardexServiceTest {

    @Mock
    private KardexRepository kardexRepository;

    @InjectMocks
    private KardexService kardexService;

    private ToolEntity testTool;
    private UserEntity testUser;

    @BeforeEach
    void setUp() {
        testTool = ToolEntity.builder().id(1L).name("Test Tool").build();
        testUser = UserEntity.builder().id(10L).username("test_user").build();
    }

    // =======================================================================
    // ÉPICA 5: REGISTRO DE MOVIMIENTOS (registerMovement)
    // =======================================================================

    @Test
    void registerMovement_Success() {
        // ARRANGE: solo verificamos la llamada a save
        
        // ACT
        kardexService.registerMovement(testTool, MovementType.INCOME, 5, testUser);

        // ASSERT: Verifica que el repositorio fue llamado con la entidad Kardex
        verify(kardexRepository, times(1)).save(
            argThat(kardex -> 
                kardex.getTool().equals(testTool) &&
                kardex.getType().equals(MovementType.INCOME) &&
                kardex.getQuantity().equals(5) &&
                kardex.getUser().equals(testUser)
            )
        );
    }

    // =======================================================================
    // ÉPICA 5: CONSULTAS DE MOVIMIENTOS
    // =======================================================================

    @Test
    void getMovementsByTool_Success() {
        // ARRANGE
        KardexEntity movement1 = KardexEntity.builder().id(1L).tool(testTool).type(MovementType.LOAN).build();
        KardexEntity movement2 = KardexEntity.builder().id(2L).tool(testTool).type(MovementType.RETURN).build();
        
        when(kardexRepository.findByTool_Id(testTool.getId())).thenReturn(List.of(movement1, movement2));

        // ACT
        List<KardexEntity> result = kardexService.getMovementsByTool(testTool);

        // ASSERT
        assertEquals(2, result.size());
        verify(kardexRepository, times(1)).findByTool_Id(1L);
    }

    @Test
    void getMovementsByDate_Success() {
        // ARRANGE
        LocalDateTime start = LocalDateTime.now().minusDays(1);
        LocalDateTime end = LocalDateTime.now();
        KardexEntity movement = KardexEntity.builder().id(3L).movementDate(start.plusHours(1)).build();
        
        when(kardexRepository.findByMovementDateBetween(start, end)).thenReturn(List.of(movement));

        // ACT
        List<KardexEntity> result = kardexService.getMovementsByDate(start, end);

        // ASSERT
        assertEquals(1, result.size());
        verify(kardexRepository, times(1)).findByMovementDateBetween(start, end);
    }
}