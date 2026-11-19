package com.ProyectoTGS.BackCore.Services;

import entities.ToolEntity;
import entities.UserEntity;
import entities.enums.MovementType;
import entities.enums.ToolStatus;
import app.exceptions.InvalidOperationException; 
import app.exceptions.ResourceNotFoundException; 
import dtos.UpdateToolRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import repositories.ToolRepository;
import services.KardexService;
import services.ToolService;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ToolServiceTest {

        @Mock
        private ToolRepository toolRepository;

        @Mock
        private KardexService kardexService;

        @InjectMocks
        private ToolService toolService;

        // --- Test entities (setup) ---
        private UserEntity testUser;
        private ToolEntity toolInput;
        private ToolEntity toolSaved;

        @BeforeEach
        void setUp() {
                // Generic user for the tests
                testUser = UserEntity.builder()
                        .id(1L)
                        .username("test_user")
                        .build();
        }

        // =======================================================================
        // METHOD: createTool
        // Epic 1: register new tools
        // =======================================================================

        /**
         * Happy path: create a tool with positive stock.
         * Checks that:
         * 1. Default values are assigned (status AVAILABLE, inRepair 0).
         * 2. The repository saves it.
         * 3. An INCOME movement is registered in the Kardex.
         */
        @Test
        void createTool_Success_WhenStockIsPositive_RegistersKardex() {
                // ARRANGE
                // 1. INPUT tool (as sent by the controller)
                toolInput = ToolEntity.builder()
                        .name("Taladro Nuevo")
                        .category("Electric Tools")
                        .stock(5) // positive stock
                        .replacementValue(50000)
                        .inRepair(null) // arrives as null
                        .status(null) // arrives as null
                        .build();

                // 2. OUTPUT tool (returned by the repository after saving)
                toolSaved = ToolEntity.builder()
                        .id(1L) // ID generated on save
                        .name("Taladro Nuevo")
                        .category("Electric Tools")
                        .stock(5)
                        .replacementValue(50000)
                        .inRepair(0) // the service sets 0
                        .status(ToolStatus.AVAILABLE) // the service sets AVAILABLE
                        .build();

                // 3. Repository mocks:
                // save() with ANY ToolEntity...
                when(toolRepository.save(any(ToolEntity.class))).thenReturn(toolSaved);
                // registerMovement() does nothing (void)
                // (doNothing() for void methods)
                doNothing().when(kardexService).registerMovement(any(ToolEntity.class), any(MovementType.class), anyInt(), any(UserEntity.class));

                // ACT
                // Call the method under test
                ToolEntity result = toolService.createTool(toolInput, testUser);

                // ASSERT
                // 1. Returned object
                assertNotNull(result);
                assertEquals(1L, result.getId()); // it is the "saved" object
                assertEquals(ToolStatus.AVAILABLE, result.getStatus()); // status assigned
                assertEquals(0, result.getInRepair()); // inRepair assigned

                // 2. toolRepository.save() was called once
                // argThat inspects the object passed to save()
                verify(toolRepository, times(1)).save(argThat(toolToSave ->
                        toolToSave.getName().equals("Taladro Nuevo") &&
                        toolToSave.getStatus().equals(ToolStatus.AVAILABLE) && // status assigned before saving
                        toolToSave.getInRepair().equals(0) // inRepair assigned before saving
                ));

                // 3. kardexService.registerMovement() was called once with the expected data
                verify(kardexService, times(1)).registerMovement(
                        eq(toolSaved), // the saved tool (with ID)
                        eq(MovementType.INCOME), // movement type
                        eq(5), // stock quantity
                        eq(testUser) // user
                );
        }

        /**
         * Happy path: create a tool with ZERO stock.
         * Checks that:
         * 1. Default values are assigned (status AVAILABLE).
         * 2. The repository saves it.
         * 3. NO Kardex movement is registered (stock is 0).
         */
        @Test
        void createTool_Success_WhenStockIsZero_DoesNotRegisterKardex() {
                // ARRANGE
                // 1. INPUT tool with stock 0
                toolInput = ToolEntity.builder()
                        .name("Nivel Burbuja")
                        .category("Hand Tools")
                        .stock(0) // ZERO stock
                        .replacementValue(15000)
                        .inRepair(null)
                        .status(null)
                        .build();

                // 2. Expected OUTPUT tool
                toolSaved = ToolEntity.builder()
                        .id(2L) // different ID to avoid confusion
                        .name("Nivel Burbuja")
                        .category("Hand Tools")
                        .stock(0)
                        .replacementValue(15000)
                        .inRepair(0)
                        .status(ToolStatus.AVAILABLE) // still AVAILABLE
                        .build();

                // 3. Mock the save
                when(toolRepository.save(any(ToolEntity.class))).thenReturn(toolSaved);
                // (KardexService is not mocked: it must not be called)

                // ACT
                ToolEntity result = toolService.createTool(toolInput, testUser);

                // ASSERT
                // 1. Returned object
                assertNotNull(result);
                assertEquals(2L, result.getId());
                assertEquals(ToolStatus.AVAILABLE, result.getStatus());
                assertEquals(0, result.getStock());

                // 2. It was saved
                verify(toolRepository, times(1)).save(argThat(toolToSave ->
                        toolToSave.getStock() == 0 &&
                        toolToSave.getStatus().equals(ToolStatus.AVAILABLE)
                ));

                // 3. CRITICAL: the Kardex was NEVER called
                verify(kardexService, never()).registerMovement(
                        any(ToolEntity.class),
                        any(MovementType.class),
                        anyInt(),
                        any(UserEntity.class)
                );
        }
        // ========================================================================
        // METHOD: updateTool
        // Epic 1: update tool data
        // =======================================================================

        /**
         * Successful update of the tool details.
         * Only the DTO fields change (name, category, replacementValue)
         * and nothing is registered in the Kardex.
         */
                @Test
        void updateTool_Success() {
                // ARRANGE
                Long toolId = 1L;
                // 1. Update request (DTO)
                UpdateToolRequest updateRequest = new UpdateToolRequest(
                        "Martillo Actualizado", // new name
                        "Herramientas Manuales", // new category
                        25000 // new value
                );

                // 2. Tool as stored in the database BEFORE the update
                ToolEntity existingTool = ToolEntity.builder()
                        .id(toolId)
                        .name("Martillo Viejo")
                        .category("Martillos")
                        .replacementValue(20000) // value > 1000 (passes the validation)
                        .stock(10) // must NOT change
                        .status(ToolStatus.AVAILABLE) // must NOT change
                        .inRepair(0) // must NOT change
                        .build();
                
                // 3. Repository mocks:
                // findById(1L) returns the existing tool
                when(toolRepository.findById(toolId)).thenReturn(Optional.of(existingTool));
                // save() returns the entity it receives
                // (so the applied changes can be checked)
                when(toolRepository.save(any(ToolEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

                // ACT
                ToolEntity updatedTool = toolService.updateTool(toolId, updateRequest, testUser);

                // ASSERT
                // 1. Updated fields in the returned object
                assertNotNull(updatedTool);
                assertEquals("Martillo Actualizado", updatedTool.getName());
                assertEquals("Herramientas Manuales", updatedTool.getCategory());
                assertEquals(25000, updatedTool.getReplacementValue());

                // 2. CRITICAL: the other fields did NOT change
                assertEquals(toolId, updatedTool.getId()); // ID
                assertEquals(10, updatedTool.getStock()); // stock
                assertEquals(ToolStatus.AVAILABLE, updatedTool.getStatus()); // status
                assertEquals(0, updatedTool.getInRepair()); // inRepair

                // 3. findById and save were called exactly once
                verify(toolRepository, times(1)).findById(toolId);
                verify(toolRepository, times(1)).save(existingTool); // saves the same updated instance

                // 4. CRITICAL: the Kardex was NEVER called
                verify(kardexService, never()).registerMovement(any(), any(), anyInt(), any());
        }

        @Test
        void updateTool_Fails_WhenToolNotFound() {
                // ARRANGE
                Long nonExistentToolId = 99L; // an ID that does not exist
                // The DTO is required by the method signature even if it is not used
                UpdateToolRequest updateRequest = new UpdateToolRequest(
                        "Nombre Ficticio",
                        "Categoría Ficticia",
                        10000
                );

                // 3. Repository mocks:
                // findById(99L) returns an empty Optional (not found)
                when(toolRepository.findById(nonExistentToolId)).thenReturn(Optional.empty());

                // ACT & ASSERT
                // ResourceNotFoundException expected
                assertThrows(ResourceNotFoundException.class, () -> {
                        // fails inside getToolById()
                        toolService.updateTool(nonExistentToolId, updateRequest, testUser);
                }, "Must throw ResourceNotFoundException when the tool is not found.");

                // the tool was looked up
                verify(toolRepository, times(1)).findById(nonExistentToolId);
                // CRITICAL: nothing was saved
                verify(toolRepository, never()).save(any(ToolEntity.class));
                // CRITICAL: the Kardex was NEVER called
                verify(kardexService, never()).registerMovement(any(), any(), anyInt(), any());
        }

        /**
         * The update fails when the EXISTING replacement value
         * is below 1000 (rule implemented in the service).
         */
        @Test
        void updateTool_Fails_WhenExistingReplacementValueIsLessThan1000() {
                // ARRANGE
                Long toolId = 3L;
                // The update request is valid (new value > 1000)
                UpdateToolRequest updateRequest = new UpdateToolRequest(
                        "Nombre Actualizado",
                        "Categoría Actualizada",
                        15000 // the new value IS valid
                );

                // 2. Tool as stored in the database (OLD value < 1000)
                ToolEntity existingToolWithLowValue = ToolEntity.builder()
                        .id(toolId)
                        .name("Herramienta Antigua Barata")
                        .category("Varios")
                        .replacementValue(900) // <-- EXISTING value < 1000
                        .stock(5)
                        .status(ToolStatus.AVAILABLE)
                        .inRepair(0)
                        .build();

                // 3. Repository mocks:
                // findById(3L) returns the tool with the low value
                when(toolRepository.findById(toolId)).thenReturn(Optional.of(existingToolWithLowValue));

                // ACT & ASSERT
                // InvalidOperationException expected
                assertThrows(InvalidOperationException.class, () -> {
                // fails in the validation of the service
                toolService.updateTool(toolId, updateRequest, testUser);
                }, "Must throw InvalidOperationException when the existing replacement value is < 1000.");

                // the tool was looked up
                verify(toolRepository, times(1)).findById(toolId);
                // CRITICAL: nothing was saved
                verify(toolRepository, never()).save(any(ToolEntity.class));
                // CRITICAL: the Kardex was NEVER called
                verify(kardexService, never()).registerMovement(any(), any(), anyInt(), any());
        }

        // =======================================================================
        // METHOD: decommissionTool
        // Epic 1: decommission tools
        // =======================================================================

        /**
         * Successful decommission of an available tool with stock.
         * The stock becomes 0, the status DECOMMISSIONED
         * and the Kardex records the original stock quantity.
         */
        @Test
        void decommissionTool_Success_WhenToolIsAvailableWithStock() {
                // ARRANGE
                Long toolId = 4L;

                // 1. Existing AVAILABLE tool with stock > 0
                ToolEntity existingTool = ToolEntity.builder()
                        .id(toolId)
                        .name("Sierra Circular")
                        .category("Electric Tools")
                        .replacementValue(50000)
                        .stock(5) // <-- positive stock
                        .status(ToolStatus.AVAILABLE) // <-- valid status
                        .inRepair(0)
                        .build();

                // 2. Repository mocks:
                // findById(4L) returns the tool
                when(toolRepository.findById(toolId)).thenReturn(Optional.of(existingTool));
                // save returns the entity it receives
                when(toolRepository.save(any(ToolEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
                // void Kardex method
                doNothing().when(kardexService).registerMovement(any(ToolEntity.class), any(MovementType.class), anyInt(), any(UserEntity.class));

                // ACT
                ToolEntity decommissionedTool = toolService.decommissionTool(toolId, testUser);

                // ASSERT
                // 1. Returned object
                assertNotNull(decommissionedTool);
                assertEquals(ToolStatus.DECOMMISSIONED, decommissionedTool.getStatus()); // status updated
                assertEquals(0, decommissionedTool.getStock()); // stock updated

                // 2. findById and save were called
                verify(toolRepository, times(1)).findById(toolId);
                verify(toolRepository, times(1)).save(argThat(savedTool ->
                        savedTool.getId().equals(toolId) &&
                        savedTool.getStatus().equals(ToolStatus.DECOMMISSIONED) &&
                        savedTool.getStock() == 0
                ));

                // 3. CRITICAL (Kardex):
                // records the quantity that WAS in stock (5)
                verify(kardexService, times(1)).registerMovement(
                        eq(decommissionedTool), // the saved entity
                        eq(MovementType.DECOMMISSION), // movement type
                        eq(5), // quantity (original stock)
                        eq(testUser) // user
                );
        }

        /**
         * Decommission fails when units are LOANED.
         * InvalidOperationException is thrown and nothing is saved.
         */
        @Test
        void decommissionTool_Fails_WhenToolIsLoaned() {
                // ARRANGE
                Long toolId = 5L;

                // 1. Existing LOANED tool
                ToolEntity loanedTool = ToolEntity.builder()
                        .id(toolId)
                        .name("Taladro Prestado")
                        .status(ToolStatus.LOANED) // <-- INVALID status for decommission
                        .stock(0) // usually 0 when the last unit is loaned
                        .replacementValue(50000)
                        .inRepair(0)
                        .loaned(1) // the rule now checks the loaned-units counter
                        .build();

                // 2. Repository mocks:
                // findById(5L) returns the loaned tool
                when(toolRepository.findById(toolId)).thenReturn(Optional.of(loanedTool));

                // ACT & ASSERT
                // InvalidOperationException expected
                assertThrows(InvalidOperationException.class, () -> {
                // fails in the loaned-units validation
                toolService.decommissionTool(toolId, testUser);
                }, "Must throw InvalidOperationException when the tool is LOANED.");

                // the tool was looked up
                verify(toolRepository, times(1)).findById(toolId);
                // CRITICAL: nothing was saved
                verify(toolRepository, never()).save(any(ToolEntity.class));
                // CRITICAL: the Kardex was NEVER called
                verify(kardexService, never()).registerMovement(any(), any(), anyInt(), any());
        }

        /**
         * Decommission fails when units are UNDER REPAIR (REPAIRING).
         * InvalidOperationException is thrown and nothing is saved.
         */
        @Test
        void decommissionTool_Fails_WhenToolIsRepairing() {
                // ARRANGE
                Long toolId = 6L;

                // 1. Existing REPAIRING tool
                ToolEntity repairingTool = ToolEntity.builder()
                        .id(toolId)
                        .name("Taladro en Reparación")
                        .status(ToolStatus.REPAIRING) // <-- INVALID status for decommission
                        .stock(0)
                        .replacementValue(50000)
                        .inRepair(1) // 1 unit in repair
                        .build();

                // 2. Repository mocks:
                // findById(6L) returns the tool under repair
                when(toolRepository.findById(toolId)).thenReturn(Optional.of(repairingTool));

                // ACT & ASSERT
                // InvalidOperationException expected
                assertThrows(InvalidOperationException.class, () -> {
                // fails in the units-in-repair validation
                toolService.decommissionTool(toolId, testUser);
                }, "Must throw InvalidOperationException when the tool is REPAIRING.");

                // the tool was looked up
                verify(toolRepository, times(1)).findById(toolId);
                // CRITICAL: nothing was saved
                verify(toolRepository, never()).save(any(ToolEntity.class));
                // CRITICAL: the Kardex was NEVER called
                verify(kardexService, never()).registerMovement(any(), any(), anyInt(), any());
        }

        /**
         * Decommission fails when the tool is ALREADY DECOMMISSIONED.
         * InvalidOperationException is thrown immediately.
         */
        @Test
        void decommissionTool_Fails_WhenToolIsAlreadyDecommissioned() {
                // ARRANGE
                Long toolId = 7L;

                // 1. Existing tool already decommissioned
                ToolEntity alreadyDecommissionedTool = ToolEntity.builder()
                        .id(toolId)
                        .name("Herramienta Rota")
                        .status(ToolStatus.DECOMMISSIONED) // <-- INVALID status (already decommissioned)
                        .stock(0)
                        .replacementValue(50000)
                        .inRepair(0)
                        .build();

                // 2. Repository mocks:
                // findById(7L) returns the decommissioned tool
                when(toolRepository.findById(toolId)).thenReturn(Optional.of(alreadyDecommissionedTool));

                // ACT & ASSERT
                // InvalidOperationException expected
                assertThrows(InvalidOperationException.class, () -> {
                // fails in the FIRST status validation
                toolService.decommissionTool(toolId, testUser);
                }, "Must throw InvalidOperationException when the tool is already DECOMMISSIONED.");

                // the tool was looked up
                verify(toolRepository, times(1)).findById(toolId);
                // CRITICAL: nothing was saved
                verify(toolRepository, never()).save(any(ToolEntity.class));
                // CRITICAL: the Kardex was NEVER called
                verify(kardexService, never()).registerMovement(any(), any(), anyInt(), any());
        }

        // =======================================================================
        // METHOD: incrementStockForReturn
        // Epic 2: support for loan returns
        // =======================================================================

        /**
         * Returning a tool (incrementing stock):
         * 1. Stock increases by 1.
         * 2. Status changes from LOANED to AVAILABLE.
         * 3. A RETURN movement is registered in the Kardex.
         */
        @Test
        void incrementStockForReturn_Success_WhenStatusWasLoaned() {
                // ARRANGE
                Long toolId = 8L;

                // 1. Loaned tool (last unit)
                ToolEntity loanedTool = ToolEntity.builder()
                        .id(toolId)
                        .name("Herramienta Prestada")
                        .status(ToolStatus.LOANED) // <-- key status
                        .stock(0) // <-- key stock
                        .replacementValue(10000)
                        .inRepair(0)
                        .build();
                
                // (findById is not needed: the tool is passed as a parameter)
                
                // 2. Repository mocks:
                // save returns the entity it receives
                when(toolRepository.save(any(ToolEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
                // void Kardex method
                doNothing().when(kardexService).registerMovement(any(ToolEntity.class), any(MovementType.class), anyInt(), any(UserEntity.class));

                // ACT
                // Call the method directly
                toolService.incrementStockForReturn(loanedTool, testUser);

                // ASSERT
                // 1. Fields of the (directly modified) entity
                assertEquals(1, loanedTool.getStock()); // stock must be 1
                assertEquals(ToolStatus.AVAILABLE, loanedTool.getStatus()); // status must be AVAILABLE

                // 2. save was called
                verify(toolRepository, times(1)).save(loanedTool); // the modified entity is saved

                // 3. The Kardex was called
                verify(kardexService, times(1)).registerMovement(
                        eq(loanedTool), // the entity
                        eq(MovementType.RETURN), // movement type
                        eq(1), // quantity (always 1)
                        eq(testUser) // user
                );
        }

        /**
         * Returning a tool (incrementing stock):
         * 1. Stock increases by 1.
         * 2. Status does NOT change if it was already AVAILABLE (more units in stock).
         * 3. A RETURN movement is registered in the Kardex.
         */
        @Test
        void incrementStockForReturn_Success_WhenStatusWasAvailable() {
                // ARRANGE
                ToolEntity availableTool = ToolEntity.builder()
                        .id(9L)
                        .name("Herramienta Común")
                        .status(ToolStatus.AVAILABLE) // already AVAILABLE
                        .stock(5) // had stock
                        .replacementValue(10000)
                        .inRepair(0)
                        .build();

                when(toolRepository.save(any(ToolEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
                doNothing().when(kardexService).registerMovement(any(ToolEntity.class), any(MovementType.class), anyInt(), any(UserEntity.class));

                // ACT
                toolService.incrementStockForReturn(availableTool, testUser);

                // ASSERT
                assertEquals(6, availableTool.getStock()); // stock increased
                assertEquals(ToolStatus.AVAILABLE, availableTool.getStatus()); // status did NOT change
                verify(toolRepository, times(1)).save(availableTool);
                verify(kardexService, times(1)).registerMovement(
                        eq(availableTool), eq(MovementType.RETURN), eq(1), eq(testUser)
                );
        }

        // =======================================================================
        // METHOD: decrementStockForLoan
        // Epic 2: support for loan creation
        // =======================================================================

        /**
         * Lending a tool (decrementing stock):
         * 1. Stock decreases by 1.
         * 2. Status does NOT change while stock is still positive.
         * 3. A LOAN movement is registered in the Kardex.
         */
        @Test
        void decrementStockForLoan_Success_WhenStockRemainsPositive() {
                // ARRANGE
                ToolEntity toolWithStock = ToolEntity.builder()
                        .id(10L)
                        .name("Herramienta con Stock")
                        .status(ToolStatus.AVAILABLE)
                        .stock(5) // <-- positive stock > 1
                        .replacementValue(10000)
                        .inRepair(0)
                        .build();

                when(toolRepository.save(any(ToolEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
                doNothing().when(kardexService).registerMovement(any(ToolEntity.class), any(MovementType.class), anyInt(), any(UserEntity.class));

                // ACT
                toolService.decrementStockForLoan(toolWithStock, testUser);

                // ASSERT
                assertEquals(4, toolWithStock.getStock()); // stock decreased
                assertEquals(ToolStatus.AVAILABLE, toolWithStock.getStatus()); // status did NOT change
                verify(toolRepository, times(1)).save(toolWithStock);
                verify(kardexService, times(1)).registerMovement(
                        eq(toolWithStock), eq(MovementType.LOAN), eq(1), eq(testUser)
                );
        }

        /**
         * Lending the LAST unit of a tool:
         * 1. Stock decreases to 0.
         * 2. Status changes from AVAILABLE to LOANED.
         * 3. A LOAN movement is registered in the Kardex.
         */
        @Test
        void decrementStockForLoan_Success_WhenStockReachesZero() {
                // ARRANGE
                ToolEntity lastUnitTool = ToolEntity.builder()
                        .id(11L)
                        .name("Última Unidad")
                        .status(ToolStatus.AVAILABLE)
                        .stock(1) // <-- last unit
                        .replacementValue(10000)
                        .inRepair(0)
                        .build();

                when(toolRepository.save(any(ToolEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
                doNothing().when(kardexService).registerMovement(any(ToolEntity.class), any(MovementType.class), anyInt(), any(UserEntity.class));

                // ACT
                toolService.decrementStockForLoan(lastUnitTool, testUser);

                // ASSERT
                assertEquals(0, lastUnitTool.getStock()); // stock is ZERO
                assertEquals(ToolStatus.LOANED, lastUnitTool.getStatus()); // status changed to LOANED
                verify(toolRepository, times(1)).save(lastUnitTool);
                verify(kardexService, times(1)).registerMovement(
                        eq(lastUnitTool), eq(MovementType.LOAN), eq(1), eq(testUser)
                );
        }

        // =======================================================================
        // METHOD: markAsRepairing
        // Epic 2: support for returns with light damage
        // =======================================================================

        /**
         * Damaged return: the unit moves from "loaned" to "in repair"; available stock is untouched.
         */
        @Test
        void markAsRepairing_Success() {
                // ARRANGE
                ToolEntity toolToRepair = ToolEntity.builder()
                        .id(12L)
                        .name("Herramienta a Reparar")
                        .status(ToolStatus.AVAILABLE)
                        .stock(5) // available stock
                        .replacementValue(10000)
                        .inRepair(0) // initial counter
                        .loaned(1)   // the unit being returned
                        .build();

                // ACT
                toolService.markAsRepairing(toolToRepair, testUser);

                // ASSERT
                assertEquals(1, toolToRepair.getInRepair()); // inRepair increased
                assertEquals(0, toolToRepair.getLoaned());   // the unit is no longer with the client
                assertEquals(5, toolToRepair.getStock()); // stock did not change
                assertEquals(ToolStatus.AVAILABLE, toolToRepair.getStatus()); // still has stock to lend

                verify(toolRepository, times(1)).save(toolToRepair);

                // The Kardex was called
                verify(kardexService, times(1)).registerMovement(
                        eq(toolToRepair), eq(MovementType.REPAIR), eq(1), eq(testUser)
                );
        }

        /**
         * A decommissioned tool cannot be sent to repair.
         */
        @Test
        void markAsRepairing_Fails_WhenToolIsDecommissioned() {
                // ARRANGE
                ToolEntity decommissionedTool = ToolEntity.builder()
                        .id(13L)
                        .status(ToolStatus.DECOMMISSIONED) // <-- invalid status
                        .stock(0)
                        .inRepair(0)
                        .build();

                // ACT & ASSERT
                assertThrows(InvalidOperationException.class, () -> {
                toolService.markAsRepairing(decommissionedTool, testUser);
                }, "Must throw InvalidOperationException when the tool is DECOMMISSIONED.");

                // the Kardex was not called
                verify(kardexService, never()).registerMovement(any(), any(), anyInt(), any());
                verify(toolRepository, never()).save(any());
        }

        // =======================================================================
        // METHOD: markAsDecommissioned
        // Epic 2: support for returns with irreparable damage
        // =======================================================================

        /**
         * Irreparable return: the loaned unit disappears (DECOMMISSION of 1 unit in the Kardex).
         * Other units keep the tool alive, so the tool itself is not decommissioned.
         */
        @Test
        void markAsDecommissioned_Success() {
                // ARRANGE: last loaned unit comes back destroyed; another unit is in repair
                ToolEntity toolToDecommission = ToolEntity.builder()
                        .id(14L)
                        .name("Herramienta Irreparable")
                        .status(ToolStatus.LOANED)
                        .stock(0)
                        .inRepair(1)
                        .loaned(1)
                        .build();

                // ACT
                toolService.markAsDecommissioned(toolToDecommission, testUser);

                // ASSERT
                assertEquals(0, toolToDecommission.getLoaned());
                assertEquals(0, toolToDecommission.getStock());
                assertEquals(1, toolToDecommission.getInRepair());
                assertEquals(ToolStatus.REPAIRING, toolToDecommission.getStatus()); // only the unit in repair remains
                verify(toolRepository, times(1)).save(toolToDecommission);

                // The Kardex was called
                verify(kardexService, times(1)).registerMovement(
                        eq(toolToDecommission), eq(MovementType.DECOMMISSION), eq(1), eq(testUser)
                );
        }

        /**
         * Cannot register a decommission in the Kardex if the tool is already DECOMMISSIONED.
         */
        @Test
        void markAsDecommissioned_Fails_WhenToolIsAlreadyDecommissioned() {
                // ARRANGE
                ToolEntity decommissionedTool = ToolEntity.builder()
                        .id(15L)
                        .status(ToolStatus.DECOMMISSIONED) // <-- invalid status
                        .build();

                // ACT & ASSERT
                assertThrows(InvalidOperationException.class, () -> {
                toolService.markAsDecommissioned(decommissionedTool, testUser);
                }, "Must throw InvalidOperationException when the tool is already DECOMMISSIONED.");

                // the Kardex was not called
                verify(kardexService, never()).registerMovement(any(), any(), anyInt(), any());
                verify(toolRepository, never()).save(any());
        }

        // =======================================================================
        // METHOD: adjustStock (manual adjustment)
        // =======================================================================

        /**
         * Successful manual stock INCREASE.
         * The stock increases, the status (if it was LOANED) becomes AVAILABLE,
         * and an INCOME movement is registered in the Kardex.
         */
        @Test
        void adjustStock_Success_WhenIncreasingStock() {
                // ARRANGE
                Long toolId = 16L;
                ToolEntity tool = ToolEntity.builder()
                        .id(toolId)
                        .status(ToolStatus.LOANED) // starts as LOANED
                        .stock(0) // with stock 0
                        .replacementValue(10000)
                        .inRepair(0)
                        .build();
                
                when(toolRepository.findById(toolId)).thenReturn(Optional.of(tool));
                when(toolRepository.save(any(ToolEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
                doNothing().when(kardexService).registerMovement(any(), any(), anyInt(), any());

                // ACT: increase stock by 5
                ToolEntity updatedTool = toolService.adjustStock(toolId, 5, testUser);

                // ASSERT
                assertEquals(5, updatedTool.getStock()); // stock increased
                assertEquals(ToolStatus.AVAILABLE, updatedTool.getStatus()); // status changed to AVAILABLE
                verify(toolRepository, times(1)).save(tool);
                verify(kardexService, times(1)).registerMovement(
                        eq(tool), eq(MovementType.INCOME), eq(5), eq(testUser) // quantity 5
                );
        }

        /**
         * Manual decrease to zero with no units loaned or in repair.
         * Previously this wrongly set the status to LOANED; nobody has the tool, so it stays AVAILABLE
         * (registered, with no stock to lend until an income).
         */
        @Test
        void adjustStock_Success_WhenDecreasingStockToZero() {
                // ARRANGE
                Long toolId = 17L;
                ToolEntity tool = ToolEntity.builder()
                        .id(toolId)
                        .status(ToolStatus.AVAILABLE)
                        .stock(3) // initial stock 3
                        .replacementValue(10000)
                        .inRepair(0)
                        .build();
                
                when(toolRepository.findById(toolId)).thenReturn(Optional.of(tool));
                when(toolRepository.save(any(ToolEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
                doNothing().when(kardexService).registerMovement(any(), any(), anyInt(), any());

                // ACT: decrease stock by 3
                ToolEntity updatedTool = toolService.adjustStock(toolId, -3, testUser);

                // ASSERT
                assertEquals(0, updatedTool.getStock()); // stock is ZERO
                assertEquals(ToolStatus.AVAILABLE, updatedTool.getStatus()); // nobody has it: not LOANED
                verify(toolRepository, times(1)).save(tool);
                verify(kardexService, times(1)).registerMovement(
                        eq(tool), eq(MovementType.MANUAL_DECREASE), eq(3), eq(testUser) // quantity 3 (absolute value)
                );
        }

        /**
         * A tool whose only unit is in repair receives new stock: it can be lent again, so it becomes
         * AVAILABLE while the damaged unit stays counted in inRepair.
         */
        @Test
        void adjustStock_Success_BecomesAvailableWhenStockArrivesDuringRepair() {
                // ARRANGE
                Long toolId = 18L;
                ToolEntity tool = ToolEntity.builder()
                        .id(toolId)
                        .status(ToolStatus.REPAIRING) // <-- key status
                        .stock(0)
                        .replacementValue(10000)
                        .inRepair(1)
                        .build();
                
                when(toolRepository.findById(toolId)).thenReturn(Optional.of(tool));
                when(toolRepository.save(any(ToolEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
                doNothing().when(kardexService).registerMovement(any(), any(), anyInt(), any());

                // ACT: Aumentar stock en 2 (ej. llegaron repuestos)
                ToolEntity updatedTool = toolService.adjustStock(toolId, 2, testUser);

                // ASSERT
                assertEquals(2, updatedTool.getStock()); // stock increased
                assertEquals(1, updatedTool.getInRepair()); // the damaged unit is still in repair
                assertEquals(ToolStatus.AVAILABLE, updatedTool.getStatus()); // it can be lent again
                verify(toolRepository, times(1)).save(tool);
                verify(kardexService, times(1)).registerMovement(
                        eq(tool), eq(MovementType.INCOME), eq(2), eq(testUser)
                );
        }

        @Test
        void adjustStock_Fails_WhenQuantityIsZero() {
                // ARRANGE
                Long toolId = 16L; // the ID does not matter
                
                // ACT & ASSERT
                assertThrows(InvalidOperationException.class, () -> {
                // quantityChange = 0
                toolService.adjustStock(toolId, 0, testUser);
                }, "Must throw InvalidOperationException when quantityChange is 0.");
                
                verify(toolRepository, never()).findById(anyLong());
                verify(kardexService, never()).registerMovement(any(), any(), anyInt(), any());
        }

        @Test
        void adjustStock_Fails_WhenToolNotFound() {
                // ARRANGE
                Long nonExistentToolId = 99L;
                when(toolRepository.findById(nonExistentToolId)).thenReturn(Optional.empty());

                // ACT & ASSERT
                assertThrows(ResourceNotFoundException.class, () -> {
                toolService.adjustStock(nonExistentToolId, 5, testUser);
                }, "Must throw ResourceNotFoundException when the tool is not found.");
                
                verify(toolRepository, times(1)).findById(nonExistentToolId);
                verify(toolRepository, never()).save(any());
                verify(kardexService, never()).registerMovement(any(), any(), anyInt(), any());
        }

        @Test
        void adjustStock_Fails_WhenToolIsDecommissioned() {
                // ARRANGE
                Long toolId = 19L;
                ToolEntity tool = ToolEntity.builder()
                        .id(toolId)
                        .status(ToolStatus.DECOMMISSIONED) // <-- invalid status
                        .stock(0)
                        .build();
                
                when(toolRepository.findById(toolId)).thenReturn(Optional.of(tool));

                // ACT & ASSERT
                assertThrows(InvalidOperationException.class, () -> {
                toolService.adjustStock(toolId, 5, testUser);
                }, "Must throw InvalidOperationException when the tool is DECOMMISSIONED.");
                
                verify(toolRepository, times(1)).findById(toolId);
                verify(toolRepository, never()).save(any());
                verify(kardexService, never()).registerMovement(any(), any(), anyInt(), any());
        }

        @Test
        void adjustStock_Fails_WhenResultingStockIsNegative() {
                // ARRANGE
                Long toolId = 20L;
                ToolEntity tool = ToolEntity.builder()
                        .id(toolId)
                        .status(ToolStatus.AVAILABLE)
                        .stock(3) // initial stock 3
                        .replacementValue(10000)
                        .inRepair(0)
                        .build();
                
                when(toolRepository.findById(toolId)).thenReturn(Optional.of(tool));

                // ACT & ASSERT
                assertThrows(InvalidOperationException.class, () -> {
                // Try to remove 4 (3 - 4 = -1)
                toolService.adjustStock(toolId, -4, testUser);
                }, "Must throw InvalidOperationException when the resulting stock is negative.");

                verify(toolRepository, times(1)).findById(toolId);
                verify(toolRepository, never()).save(any());
                verify(kardexService, never()).registerMovement(any(), any(), anyInt(), any());
        }

        // =======================================================================
        // METHOD: getAllTools (query)
        // =======================================================================

        @Test
        void getAllTools_ReturnsListOfTools() {
                // ARRANGE
                ToolEntity tool1 = ToolEntity.builder().id(1L).name("Tool 1").build();
                ToolEntity tool2 = ToolEntity.builder().id(2L).name("Tool 2").build();
                List<ToolEntity> mockList = List.of(tool1, tool2);

                when(toolRepository.findAll()).thenReturn(mockList);

                // ACT
                List<ToolEntity> result = toolService.getAllTools();

                // ASSERT
                assertNotNull(result);
                assertEquals(2, result.size());
                assertEquals("Tool 1", result.get(0).getName());
                verify(toolRepository, times(1)).findAll();
        }

        @Test
        void getAllTools_ReturnsEmptyList() {
                // ARRANGE
                when(toolRepository.findAll()).thenReturn(Collections.emptyList());

                // ACT
                List<ToolEntity> result = toolService.getAllTools();

                // ASSERT
                assertNotNull(result);
                assertTrue(result.isEmpty());
                verify(toolRepository, times(1)).findAll();
        }

        // =======================================================================
        // METHOD: getToolById (query)
        // =======================================================================

        @Test
        void getToolById_Success_WhenToolExists() {
                // ARRANGE
                Long toolId = 1L;
                ToolEntity tool = ToolEntity.builder().id(toolId).name("Test Tool").build();
                when(toolRepository.findById(toolId)).thenReturn(Optional.of(tool));

                // ACT
                ToolEntity result = toolService.getToolById(toolId);

                // ASSERT
                assertNotNull(result);
                assertEquals(toolId, result.getId());
                assertEquals("Test Tool", result.getName());
                verify(toolRepository, times(1)).findById(toolId);
        }

        @Test
        void getToolById_Fails_WhenToolNotFound() {
                // ARRANGE
                Long nonExistentToolId = 99L;
                when(toolRepository.findById(nonExistentToolId)).thenReturn(Optional.empty());

                // ACT & ASSERT
                assertThrows(ResourceNotFoundException.class, () -> {
                toolService.getToolById(nonExistentToolId);
                }, "Must throw ResourceNotFoundException when the tool is not found.");

                verify(toolRepository, times(1)).findById(nonExistentToolId);
        }

        // =======================================================================
        // REPAIR FLOW: completeRepair / writeOffRepair
        // =======================================================================

        @Test
        void completeRepair_MovesUnitBackToStock() {
                ToolEntity tool = ToolEntity.builder().id(30L).status(ToolStatus.REPAIRING)
                        .stock(0).loaned(0).inRepair(2).replacementValue(10000).build();
                when(toolRepository.findById(30L)).thenReturn(Optional.of(tool));
                when(toolRepository.save(tool)).thenReturn(tool);

                ToolEntity result = toolService.completeRepair(30L, testUser);

                assertEquals(1, result.getInRepair());
                assertEquals(1, result.getStock());
                assertEquals(ToolStatus.AVAILABLE, result.getStatus());
                verify(kardexService).registerMovement(tool, MovementType.REPAIRED, 1, testUser);
        }

        @Test
        void writeOffRepair_RemovesUnitPermanently() {
                ToolEntity tool = ToolEntity.builder().id(31L).status(ToolStatus.REPAIRING)
                        .stock(0).loaned(1).inRepair(1).replacementValue(10000).build();
                when(toolRepository.findById(31L)).thenReturn(Optional.of(tool));
                when(toolRepository.save(tool)).thenReturn(tool);

                ToolEntity result = toolService.writeOffRepair(31L, testUser);

                assertEquals(0, result.getInRepair());
                assertEquals(0, result.getStock());
                assertEquals(ToolStatus.LOANED, result.getStatus()); // the remaining unit is with a client
                verify(kardexService).registerMovement(tool, MovementType.DECOMMISSION, 1, testUser);
        }

        @Test
        void completeRepair_FailsWhenNoUnitsAreInRepair() {
                ToolEntity tool = ToolEntity.builder().id(32L).status(ToolStatus.AVAILABLE)
                        .stock(3).loaned(0).inRepair(0).build();
                when(toolRepository.findById(32L)).thenReturn(Optional.of(tool));

                assertThrows(InvalidOperationException.class, () -> toolService.completeRepair(32L, testUser));
                assertThrows(InvalidOperationException.class, () -> toolService.writeOffRepair(32L, testUser));
                verify(toolRepository, never()).save(any());
                verify(kardexService, never()).registerMovement(any(), any(), anyInt(), any());
        }

        // =======================================================================
        // STATUS RULES (derived from the unit counters)
        // =======================================================================

        @Test
        void decommissionTool_Fails_WhenUnitsAreInRepairEvenIfStatusIsAvailable() {
                // Status AVAILABLE (has stock) but one unit is still waiting for repair evaluation
                ToolEntity tool = ToolEntity.builder().id(33L).status(ToolStatus.AVAILABLE)
                        .stock(4).loaned(0).inRepair(1).build();
                when(toolRepository.findById(33L)).thenReturn(Optional.of(tool));

                assertThrows(InvalidOperationException.class, () -> toolService.decommissionTool(33L, testUser));
                verify(toolRepository, never()).save(any());
        }

        @Test
        void decommissionTool_Fails_WhenSomeUnitsAreLoanedEvenIfStatusIsAvailable() {
                ToolEntity tool = ToolEntity.builder().id(34L).status(ToolStatus.AVAILABLE)
                        .stock(4).loaned(1).inRepair(0).build();
                when(toolRepository.findById(34L)).thenReturn(Optional.of(tool));

                assertThrows(InvalidOperationException.class, () -> toolService.decommissionTool(34L, testUser));
                verify(toolRepository, never()).save(any());
        }

        @Test
        void decrementStockForLoan_LastUnitMakesToolLoaned_AndCountsIt() {
                ToolEntity tool = ToolEntity.builder().id(35L).status(ToolStatus.AVAILABLE)
                        .stock(1).inRepair(0).build(); // loaned null: legacy row before the counter existed

                toolService.decrementStockForLoan(tool, testUser);

                assertEquals(0, tool.getStock());
                assertEquals(1, tool.getLoaned());
                assertEquals(ToolStatus.LOANED, tool.getStatus());
        }

        @Test
        void incrementStockForReturn_DecreasesLoanedCounter() {
                ToolEntity tool = ToolEntity.builder().id(36L).status(ToolStatus.LOANED)
                        .stock(0).loaned(2).inRepair(0).build();

                toolService.incrementStockForReturn(tool, testUser);

                assertEquals(1, tool.getStock());
                assertEquals(1, tool.getLoaned());
                assertEquals(ToolStatus.AVAILABLE, tool.getStatus());
        }

        @Test
        void createTool_IgnoresIncomingCountersAndStatus() {
                ToolEntity tool = ToolEntity.builder().name("Sierra").category("Carpentry")
                        .stock(0).inRepair(7).loaned(3).status(ToolStatus.DECOMMISSIONED)
                        .replacementValue(20000).build();
                when(toolRepository.save(tool)).thenReturn(tool);

                ToolEntity saved = toolService.createTool(tool, testUser);

                assertEquals(0, saved.getInRepair());
                assertEquals(0, saved.getLoaned());
                assertEquals(ToolStatus.AVAILABLE, saved.getStatus());
        }
}