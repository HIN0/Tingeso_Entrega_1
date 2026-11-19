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
import repositories.ToolRepository;
import services.KardexService;
import app.exceptions.ResourceNotFoundException;

import java.time.LocalDateTime;
import java.util.Collections; 
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class KardexServiceTest {

    @Mock
    private KardexRepository kardexRepository;

    @Mock
    private ToolRepository toolRepository;

    @InjectMocks
    private KardexService kardexService;

    private ToolEntity testTool;
    private ToolEntity toolWithoutId;
    private UserEntity testUser;

    @BeforeEach
    void setUp() {
        testTool = ToolEntity.builder().id(1L).name("Test Tool").build();
        toolWithoutId = ToolEntity.builder().name("Tool No ID").build(); // to test validations
        testUser = UserEntity.builder().id(10L).username("test_user").build();
    }

    // =======================================================================
    // METHOD: registerMovement
    // =======================================================================

    @Test
    void registerMovement_Success() {
        // ARRANGE: the tool exists
        when(toolRepository.existsById(testTool.getId())).thenReturn(true);

        // ACT
        kardexService.registerMovement(testTool, MovementType.INCOME, 5, testUser);

        // ASSERT: the repository received the expected Kardex entity
        verify(kardexRepository, times(1)).save(
            argThat(kardex ->
                kardex.getTool().equals(testTool) &&
                kardex.getType().equals(MovementType.INCOME) &&
                kardex.getQuantity().equals(5) &&
                kardex.getUser().equals(testUser) &&
                kardex.getMovementDate() != null // a date is assigned
            )
        );
    }

    @Test
    void registerMovement_FailsWhenToolIsNull() {
        // ACT & ASSERT: fails when the tool is null
        assertThrows(ResourceNotFoundException.class, () -> {
            kardexService.registerMovement(null, MovementType.INCOME, 1, testUser);
        }, "Must throw ResourceNotFoundException when the tool is null.");
        verify(kardexRepository, never()).save(any()); // must not save
    }

     @Test
    void registerMovement_FailsWhenToolIdIsNull() {
        // ACT & ASSERT: fails when the tool ID is null
        assertThrows(ResourceNotFoundException.class, () -> {
            kardexService.registerMovement(toolWithoutId, MovementType.INCOME, 1, testUser);
        }, "Must throw ResourceNotFoundException when the tool ID is null.");
        verify(kardexRepository, never()).save(any()); // must not save
    }


    @Test
    void registerMovement_FailsWhenToolDoesNotExistInRepo() {
        // ARRANGE: the tool does NOT exist in the database
        when(toolRepository.existsById(testTool.getId())).thenReturn(false);

        // ACT & ASSERT
        assertThrows(ResourceNotFoundException.class, () -> {
            kardexService.registerMovement(testTool, MovementType.INCOME, 1, testUser);
        }, "Must throw ResourceNotFoundException when the tool does not exist.");
        verify(kardexRepository, never()).save(any()); // must not save
    }

    // =======================================================================
    // METHOD: getMovementsByToolId
    // =======================================================================

    @Test
    void getMovementsByToolId_Success() {
        // ARRANGE
        Long toolId = 1L;
        KardexEntity movement1 = KardexEntity.builder().id(1L).tool(testTool).type(MovementType.LOAN).build();
        when(toolRepository.existsById(toolId)).thenReturn(true); // the tool exists
        when(kardexRepository.findByTool_Id(toolId)).thenReturn(List.of(movement1));

        // ACT
        List<KardexEntity> result = kardexService.getMovementsByToolId(toolId);

        // ASSERT
        assertEquals(1, result.size());
        verify(toolRepository, times(1)).existsById(toolId);
        verify(kardexRepository, times(1)).findByTool_Id(toolId);
    }

     @Test
    void getMovementsByToolId_ReturnsEmptyListWhenNoMovements() {
        // ARRANGE
        Long toolId = 1L;
        when(toolRepository.existsById(toolId)).thenReturn(true); // the tool exists
        when(kardexRepository.findByTool_Id(toolId)).thenReturn(Collections.emptyList()); // no movements

        // ACT
        List<KardexEntity> result = kardexService.getMovementsByToolId(toolId);

        // ASSERT
        assertTrue(result.isEmpty());
        verify(toolRepository, times(1)).existsById(toolId);
        verify(kardexRepository, times(1)).findByTool_Id(toolId);
    }

    @Test
    void getMovementsByToolId_FailsWhenToolIdIsNull() {
        // ACT & ASSERT
        assertThrows(IllegalArgumentException.class, () -> {
            kardexService.getMovementsByToolId(null);
        }, "Must throw IllegalArgumentException when the tool ID is null.");
        verify(toolRepository, never()).existsById(any());
        verify(kardexRepository, never()).findByTool_Id(any());
    }

    @Test
    void getMovementsByToolId_FailsWhenToolDoesNotExist() {
        // ARRANGE
        Long toolId = 99L;
        when(toolRepository.existsById(toolId)).thenReturn(false); // the tool does NOT exist

        // ACT & ASSERT
        assertThrows(ResourceNotFoundException.class, () -> {
            kardexService.getMovementsByToolId(toolId);
        }, "Must throw ResourceNotFoundException when the tool does not exist.");
        verify(toolRepository, times(1)).existsById(toolId); // existence was checked
        verify(kardexRepository, never()).findByTool_Id(any()); // movements must not be queried
    }


    // =======================================================================
    // METHOD: getMovementsByDate
    // =======================================================================

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

     @Test
    void getMovementsByDate_ReturnsEmptyListWhenNoMovementsInDateRange() {
        // ARRANGE
        LocalDateTime start = LocalDateTime.now().minusDays(1);
        LocalDateTime end = LocalDateTime.now();
        when(kardexRepository.findByMovementDateBetween(start, end)).thenReturn(Collections.emptyList());

        // ACT
        List<KardexEntity> result = kardexService.getMovementsByDate(start, end);

        // ASSERT
        assertTrue(result.isEmpty());
        verify(kardexRepository, times(1)).findByMovementDateBetween(start, end);
    }

    @Test
    void getMovementsByDate_FailsWhenStartDateIsNull() {
        // ACT & ASSERT
        assertThrows(IllegalArgumentException.class, () -> {
            kardexService.getMovementsByDate(null, LocalDateTime.now());
        }, "Must throw IllegalArgumentException when startDate is null.");
        verify(kardexRepository, never()).findByMovementDateBetween(any(), any());
    }

    @Test
    void getMovementsByDate_FailsWhenEndDateIsNull() {
        // ACT & ASSERT
        assertThrows(IllegalArgumentException.class, () -> {
            kardexService.getMovementsByDate(LocalDateTime.now(), null);
        }, "Must throw IllegalArgumentException when endDate is null.");
        verify(kardexRepository, never()).findByMovementDateBetween(any(), any());
    }

    @Test
    void getMovementsByDate_FailsWhenEndDateIsBeforeStartDate() {
        // ARRANGE
        LocalDateTime start = LocalDateTime.now();
        LocalDateTime end = start.minusDays(1); // end date before the start date

        // ACT & ASSERT
        assertThrows(IllegalArgumentException.class, () -> {
            kardexService.getMovementsByDate(start, end);
        }, "Must throw IllegalArgumentException when endDate is before startDate.");
        verify(kardexRepository, never()).findByMovementDateBetween(any(), any());
    }
}