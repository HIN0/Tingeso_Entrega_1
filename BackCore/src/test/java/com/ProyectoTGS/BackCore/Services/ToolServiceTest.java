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

        // --- Entidades de Prueba (Setup) ---
        private UserEntity testUser;
        private ToolEntity toolInput;
        private ToolEntity toolSaved;

        @BeforeEach
        void setUp() {
                // Inicializar un usuario genérico para las pruebas
                testUser = UserEntity.builder()
                        .id(1L)
                        .username("test_user")
                        .build();
        }

        // =======================================================================
        // MÉTODO: createTool
        // Épica 1: Registrar nuevas herramientas
        // =======================================================================

        /**
         * Prueba el "camino feliz" de crear una herramienta con stock positivo.
         * Verifica que:
         * 1. Se asignen los valores por defecto (status AVAILABLE, inRepair 0).
         * 2. Se llame al repositorio para guardar.
         * 3. Se registre el movimiento de INGRESO (INCOME) en el Kardex.
         */
        @Test
        void createTool_Success_WhenStockIsPositive_RegistersKardex() {
                // ARRANGE (Preparar)
                // 1. Definir la herramienta de ENTRADA (la que envía el controlador)
                toolInput = ToolEntity.builder()
                        .name("Taladro Nuevo")
                        .category("Electric Tools")
                        .stock(5) // Stock positivo
                        .replacementValue(50000)
                        .inRepair(null) // Simular que viene nulo
                        .status(null)   // Simular que viene nulo
                        .build();

                // 2. Definir la herramienta de SALIDA (la que devuelve el repositorio tras guardar)
                toolSaved = ToolEntity.builder()
                        .id(1L) // El ID se genera al guardar
                        .name("Taladro Nuevo")
                        .category("Electric Tools")
                        .stock(5)
                        .replacementValue(50000)
                        .inRepair(0) // El servicio debe poner 0
                        .status(ToolStatus.AVAILABLE) // El servicio debe poner AVAILABLE
                        .build();

                // 3. Mockear el comportamiento del repositorio:
                // "Cuando se llame a toolRepository.save() con CUALQUIER ToolEntity..."
                when(toolRepository.save(any(ToolEntity.class))).thenReturn(toolSaved);
                // "Cuando se llame a kardexService.registerMovement(), no hagas nada (void)"
                // (Usamos doNothing() para métodos void)
                doNothing().when(kardexService).registerMovement(any(ToolEntity.class), any(MovementType.class), anyInt(), any(UserEntity.class));

                // ACT (Actuar)
                // Llamar al método que queremos probar
                ToolEntity result = toolService.createTool(toolInput, testUser);

                // ASSERT (Verificar)
                // 1. Verificar el objeto devuelto
                assertNotNull(result);
                assertEquals(1L, result.getId()); // Verifica que es el objeto "guardado"
                assertEquals(ToolStatus.AVAILABLE, result.getStatus()); // Verifica estado asignado
                assertEquals(0, result.getInRepair()); // Verifica inRepair asignado

                // 2. Verificar que toolRepository.save() fue llamado 1 vez
                // Usamos argThat para espiar qué objeto se le pasó a save()
                verify(toolRepository, times(1)).save(argThat(toolToSave ->
                        toolToSave.getName().equals("Taladro Nuevo") &&
                        toolToSave.getStatus().equals(ToolStatus.AVAILABLE) && // Estado asignado antes de guardar
                        toolToSave.getInRepair().equals(0) // inRepair asignado antes de guardar
                ));

                // 3. Verificar que kardexService.registerMovement() fue llamado 1 vez con los datos correctos
                verify(kardexService, times(1)).registerMovement(
                        eq(toolSaved),                 // La herramienta guardada (con ID)
                        eq(MovementType.INCOME),       // El tipo de movimiento
                        eq(5),                   // La cantidad de stock
                        eq(testUser)                   // El usuario
                );
        }

        /**
         * Prueba el "camino feliz" de crear una herramienta con stock CERO.
         * Verifica que:
         * 1. Se asignen los valores por defecto (status AVAILABLE).
         * 2. Se llame al repositorio para guardar.
         * 3. NO se registre ningún movimiento en el Kardex (porque el stock es 0).
         */
        @Test
        void createTool_Success_WhenStockIsZero_DoesNotRegisterKardex() {
                // ARRANGE (Preparar)
                // 1. Herramienta de ENTRADA con stock 0
                toolInput = ToolEntity.builder()
                        .name("Nivel Burbuja")
                        .category("Hand Tools")
                        .stock(0) // Stock CERO
                        .replacementValue(15000)
                        .inRepair(null)
                        .status(null)
                        .build();

                // 2. Herramienta de SALIDA esperada
                toolSaved = ToolEntity.builder()
                        .id(2L) // ID diferente para evitar confusiones
                        .name("Nivel Burbuja")
                        .category("Hand Tools")
                        .stock(0)
                        .replacementValue(15000)
                        .inRepair(0)
                        .status(ToolStatus.AVAILABLE) // Aún debe ser AVAILABLE
                        .build();

                // 3. Mockear el guardado
                when(toolRepository.save(any(ToolEntity.class))).thenReturn(toolSaved);
                // (No necesitamos mockear KardexService, ya que no esperamos que se llame)

                // ACT (Actuar)
                ToolEntity result = toolService.createTool(toolInput, testUser);

                // ASSERT (Verificar)
                // 1. Verificar el objeto devuelto
                assertNotNull(result);
                assertEquals(2L, result.getId());
                assertEquals(ToolStatus.AVAILABLE, result.getStatus());
                assertEquals(0, result.getStock());

                // 2. Verificar que se guardó
                verify(toolRepository, times(1)).save(argThat(toolToSave ->
                        toolToSave.getStock() == 0 &&
                        toolToSave.getStatus().equals(ToolStatus.AVAILABLE)
                ));

                // 3. VERIFICACIÓN CRÍTICA: Asegurarse de que NUNCA se llamó al Kardex
                verify(kardexService, never()).registerMovement(
                        any(ToolEntity.class),
                        any(MovementType.class),
                        anyInt(),
                        any(UserEntity.class)
                );
        }
        // ========================================================================
        // MÉTODO: updateTool
        // Épica 1: Actualizar datos de herramientas
        // =======================================================================

        /**
         * Prueba la actualización exitosa de los detalles de una herramienta.
         * Verifica que solo se actualicen los campos del DTO (name, category, replacementValue)
         * y que no se registre nada en el Kardex.
         */
                @Test
        void updateTool_Success() {
                // ARRANGE (Preparar)
                Long toolId = 1L;
                // 1. Datos de la solicitud de actualización (DTO)
                UpdateToolRequest updateRequest = new UpdateToolRequest(
                        "Martillo Actualizado",  // Nuevo nombre
                        "Herramientas Manuales", // Nueva categoría
                        25000                    // Nuevo valor
                );

                // 2. Herramienta tal como existe en la BD ANTES de la actualización
                ToolEntity existingTool = ToolEntity.builder()
                        .id(toolId)
                        .name("Martillo Viejo")
                        .category("Martillos")
                        .replacementValue(20000) // Valor > 1000 (pasa la validación)
                        .stock(10)               // Este campo NO debe cambiar
                        .status(ToolStatus.AVAILABLE) // Este campo NO debe cambiar
                        .inRepair(0)             // Este campo NO debe cambiar
                        .build();
                
                // 3. Mockear el repositorio:
                // "Cuando se llame a findById(1L), devuelve la herramienta existente"
                when(toolRepository.findById(toolId)).thenReturn(Optional.of(existingTool));
                // "Cuando se llame a save(), devuelve la entidad que se le pasó"
                // (Esto nos permite verificar la entidad con los cambios aplicados)
                when(toolRepository.save(any(ToolEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

                // ACT (Actuar)
                ToolEntity updatedTool = toolService.updateTool(toolId, updateRequest, testUser);

                // ASSERT (Verificar)
                // 1. Verificar los campos actualizados en el objeto devuelto
                assertNotNull(updatedTool);
                assertEquals("Martillo Actualizado", updatedTool.getName());
                assertEquals("Herramientas Manuales", updatedTool.getCategory());
                assertEquals(25000, updatedTool.getReplacementValue());

                // 2. VERIFICACIÓN CRÍTICA: Asegurarse de que los otros campos NO cambiaron
                assertEquals(toolId, updatedTool.getId()); // ID
                assertEquals(10, updatedTool.getStock()); // Stock
                assertEquals(ToolStatus.AVAILABLE, updatedTool.getStatus()); // Status
                assertEquals(0, updatedTool.getInRepair()); // InRepair

                // 3. Verificar que se llamó a findById y save (exactamente 1 vez)
                verify(toolRepository, times(1)).findById(toolId);
                verify(toolRepository, times(1)).save(existingTool); // Debe guardar la misma instancia que actualizó

                // 4. VERIFICACIÓN CRÍTICA: Asegurarse de que NUNCA se llamó al Kardex
                verify(kardexService, never()).registerMovement(any(), any(), anyInt(), any());
        }

        @Test
        void updateTool_Fails_WhenToolNotFound() {
                // ARRANGE (Preparar)
                Long nonExistentToolId = 99L; // Un ID que no existe
                // El DTO es necesario para la firma del método, aunque no se usará
                UpdateToolRequest updateRequest = new UpdateToolRequest(
                        "Nombre Ficticio",
                        "Categoría Ficticia",
                        10000
                );

                // 3. Mockear el repositorio:
                // "Cuando se llame a findById(99L), devuelve un Optional vacío (no encontrado)"
                when(toolRepository.findById(nonExistentToolId)).thenReturn(Optional.empty());

                // ACT & ASSERT (Actuar y Verificar)
                // Esperamos que el método lance ResourceNotFoundException
                assertThrows(ResourceNotFoundException.class, () -> {
                        // Esta llamada fallará dentro de getToolById()
                        toolService.updateTool(nonExistentToolId, updateRequest, testUser);
                }, "Debe lanzar ResourceNotFoundException si la herramienta no se encuentra.");

                // Verificar que se intentó buscar la herramienta
                verify(toolRepository, times(1)).findById(nonExistentToolId);
                // VERIFICACIÓN CRÍTICA: Asegurarse de que NUNCA se intentó guardar nada
                verify(toolRepository, never()).save(any(ToolEntity.class));
                // VERIFICACIÓN CRÍTICA: Asegurarse de que NUNCA se llamó al Kardex
                verify(kardexService, never()).registerMovement(any(), any(), anyInt(), any());
        }

        /**
         * Prueba la falla al intentar actualizar una herramienta si su valor de reposición
         * EXISTENTE es menor a 1000 (según la lógica implementada en el servicio).
         */
        @Test
        void updateTool_Fails_WhenExistingReplacementValueIsLessThan1000() {
                // ARRANGE (Preparar)
                Long toolId = 3L;
                // La solicitud de actualización es válida (nuevo valor > 1000)
                UpdateToolRequest updateRequest = new UpdateToolRequest(
                        "Nombre Actualizado",
                        "Categoría Actualizada",
                        15000 // El nuevo valor SÍ es válido
                );

                // 2. Herramienta tal como existe en la BD (CON VALOR ANTIGUO < 1000)
                ToolEntity existingToolWithLowValue = ToolEntity.builder()
                        .id(toolId)
                        .name("Herramienta Antigua Barata")
                        .category("Varios")
                        .replacementValue(900) // <-- VALOR EXISTENTE < 1000
                        .stock(5)
                        .status(ToolStatus.AVAILABLE)
                        .inRepair(0)
                        .build();

                // 3. Mockear el repositorio:
                // "Cuando se llame a findById(3L), devuelve la herramienta con valor bajo"
                when(toolRepository.findById(toolId)).thenReturn(Optional.of(existingToolWithLowValue));

                // ACT & ASSERT (Actuar y Verificar)
                // Esperamos que el método lance InvalidOperationException
                assertThrows(InvalidOperationException.class, () -> {
                // Esta llamada fallará en el 'if' de validación del servicio
                toolService.updateTool(toolId, updateRequest, testUser);
                }, "Debe lanzar InvalidOperationException si el valor de reposición existente es < 1000.");

                // Verificar que se intentó buscar la herramienta
                verify(toolRepository, times(1)).findById(toolId);
                // VERIFICACIÓN CRÍTICA: Asegurarse de que NUNCA se intentó guardar nada
                verify(toolRepository, never()).save(any(ToolEntity.class));
                // VERIFICACIÓN CRÍTICA: Asegurarse de que NUNCA se llamó al Kardex
                verify(kardexService, never()).registerMovement(any(), any(), anyInt(), any());
        }

        // =======================================================================
        // MÉTODO: decommissionTool
        // Épica 1: Dar de baja herramientas
        // =======================================================================

        /**
         * Prueba la baja exitosa de una herramienta que está disponible y tiene stock.
         * Verifica que el stock se reduzca a 0, el estado cambie a DECOMMISSIONED
         * y se registre en Kardex la cantidad de stock original.
         */
        @Test
        void decommissionTool_Success_WhenToolIsAvailableWithStock() {
                // ARRANGE (Preparar)
                Long toolId = 4L;

                // 1. Herramienta existente en estado AVAILABLE y con stock > 0
                ToolEntity existingTool = ToolEntity.builder()
                        .id(toolId)
                        .name("Sierra Circular")
                        .category("Electric Tools")
                        .replacementValue(50000)
                        .stock(5) // <-- Stock positivo
                        .status(ToolStatus.AVAILABLE) // <-- Estado válido
                        .inRepair(0)
                        .build();

                // 2. Mockear el repositorio:
                // "Cuando se busque por ID=4L, devolver la herramienta"
                when(toolRepository.findById(toolId)).thenReturn(Optional.of(existingTool));
                // "Cuando se guarde, devolver la entidad que se le pasó"
                when(toolRepository.save(any(ToolEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
                // "Mockear el método void de Kardex"
                doNothing().when(kardexService).registerMovement(any(ToolEntity.class), any(MovementType.class), anyInt(), any(UserEntity.class));

                // ACT (Actuar)
                ToolEntity decommissionedTool = toolService.decommissionTool(toolId, testUser);

                // ASSERT (Verificar)
                // 1. Verificar el objeto devuelto
                assertNotNull(decommissionedTool);
                assertEquals(ToolStatus.DECOMMISSIONED, decommissionedTool.getStatus()); // Estado actualizado
                assertEquals(0, decommissionedTool.getStock()); // Stock actualizado

                // 2. Verificar que se llamó a findById y save
                verify(toolRepository, times(1)).findById(toolId);
                verify(toolRepository, times(1)).save(argThat(savedTool ->
                        savedTool.getId().equals(toolId) &&
                        savedTool.getStatus().equals(ToolStatus.DECOMMISSIONED) &&
                        savedTool.getStock() == 0
                ));

                // 3. VERIFICACIÓN CRÍTICA (Kardex):
                // Debe registrar la cantidad que HABÍA en stock (5)
                verify(kardexService, times(1)).registerMovement(
                        eq(decommissionedTool),          // La entidad guardada
                        eq(MovementType.DECOMMISSION),   // El tipo de movimiento
                        eq(5),                           // La cantidad (el stock original)
                        eq(testUser)                     // El usuario
                );
        }

        /**
         * Prueba la falla al intentar dar de baja una herramienta que está PRESTADA (LOANED).
         * Verifica que el servicio lance InvalidOperationException y no se guarde nada.
         */
        @Test
        void decommissionTool_Fails_WhenToolIsLoaned() {
                // ARRANGE (Preparar)
                Long toolId = 5L;

                // 1. Herramienta existente en estado LOANED
                ToolEntity loanedTool = ToolEntity.builder()
                        .id(toolId)
                        .name("Taladro Prestado")
                        .status(ToolStatus.LOANED) // <-- Estado INVÁLIDO para la baja
                        .stock(0) // Típicamente 0 si está prestada la última unidad
                        .replacementValue(50000)
                        .inRepair(0)
                        .loaned(1) // the rule now checks the loaned-units counter
                        .build();

                // 2. Mockear el repositorio:
                // "Cuando se busque por ID=5L, devolver la herramienta prestada"
                when(toolRepository.findById(toolId)).thenReturn(Optional.of(loanedTool));

                // ACT & ASSERT (Actuar y Verificar)
                // Esperamos que el método lance InvalidOperationException
                assertThrows(InvalidOperationException.class, () -> {
                // Esta llamada debe fallar en la validación de estado
                toolService.decommissionTool(toolId, testUser);
                }, "Debe lanzar InvalidOperationException si la herramienta está en LOANED.");

                // Verificar que se intentó buscar la herramienta
                verify(toolRepository, times(1)).findById(toolId);
                // VERIFICACIÓN CRÍTICA: Asegurarse de que NUNCA se intentó guardar
                verify(toolRepository, never()).save(any(ToolEntity.class));
                // VERIFICACIÓN CRÍTICA: Asegurarse de que NUNCA se llamó al Kardex
                verify(kardexService, never()).registerMovement(any(), any(), anyInt(), any());
        }

        /**
         * Prueba la falla al intentar dar de baja una herramienta que está EN REPARACIÓN (REPAIRING).
         * Verifica que el servicio lance InvalidOperationException y no se guarde nada.
         */
        @Test
        void decommissionTool_Fails_WhenToolIsRepairing() {
                // ARRANGE (Preparar)
                Long toolId = 6L;

                // 1. Herramienta existente en estado REPAIRING
                ToolEntity repairingTool = ToolEntity.builder()
                        .id(toolId)
                        .name("Taladro en Reparación")
                        .status(ToolStatus.REPAIRING) // <-- Estado INVÁLIDO para la baja
                        .stock(0)
                        .replacementValue(50000)
                        .inRepair(1) // Tiene 1 unidad en reparación
                        .build();

                // 2. Mockear el repositorio:
                // "Cuando se busque por ID=6L, devolver la herramienta en reparación"
                when(toolRepository.findById(toolId)).thenReturn(Optional.of(repairingTool));

                // ACT & ASSERT (Actuar y Verificar)
                // Esperamos que el método lance InvalidOperationException
                assertThrows(InvalidOperationException.class, () -> {
                // Esta llamada debe fallar en la validación de estado
                toolService.decommissionTool(toolId, testUser);
                }, "Debe lanzar InvalidOperationException si la herramienta está en REPAIRING.");

                // Verificar que se intentó buscar la herramienta
                verify(toolRepository, times(1)).findById(toolId);
                // VERIFICACIÓN CRÍTICA: Asegurarse de que NUNCA se intentó guardar
                verify(toolRepository, never()).save(any(ToolEntity.class));
                // VERIFICACIÓN CRÍTICA: Asegurarse de que NUNCA se llamó al Kardex
                verify(kardexService, never()).registerMovement(any(), any(), anyInt(), any());
        }

        /**
         * Prueba la falla al intentar dar de baja una herramienta que YA ESTÁ en estado DECOMMISSIONED.
         * Verifica que el servicio lance InvalidOperationException inmediatamente.
         */
        @Test
        void decommissionTool_Fails_WhenToolIsAlreadyDecommissioned() {
                // ARRANGE (Preparar)
                Long toolId = 7L;

                // 1. Herramienta existente que ya está dada de baja
                ToolEntity alreadyDecommissionedTool = ToolEntity.builder()
                        .id(toolId)
                        .name("Herramienta Rota")
                        .status(ToolStatus.DECOMMISSIONED) // <-- Estado INVÁLIDO (ya está de baja)
                        .stock(0)
                        .replacementValue(50000)
                        .inRepair(0)
                        .build();

                // 2. Mockear el repositorio:
                // "Cuando se busque por ID=7L, devolver la herramienta ya dada de baja"
                when(toolRepository.findById(toolId)).thenReturn(Optional.of(alreadyDecommissionedTool));

                // ACT & ASSERT (Actuar y Verificar)
                // Esperamos que el método lance InvalidOperationException
                assertThrows(InvalidOperationException.class, () -> {
                // Esta llamada debe fallar en la PRIMERA validación de estado
                toolService.decommissionTool(toolId, testUser);
                }, "Debe lanzar InvalidOperationException si la herramienta ya está en DECOMMISSIONED.");

                // Verificar que se intentó buscar la herramienta
                verify(toolRepository, times(1)).findById(toolId);
                // VERIFICACIÓN CRÍTICA: Asegurarse de que NUNCA se intentó guardar
                verify(toolRepository, never()).save(any(ToolEntity.class));
                // VERIFICACIÓN CRÍTICA: Asegurarse de que NUNCA se llamó al Kardex
                verify(kardexService, never()).registerMovement(any(), any(), anyInt(), any());
        }

        // =======================================================================
        // MÉTODO: incrementStockForReturn
        // Épica 2: Soporte para devolución de préstamos
        // =======================================================================

        /**
         * Prueba que al devolver una herramienta (incrementar stock):
         * 1. El stock aumenta en 1.
         * 2. El estado cambia de LOANED a AVAILABLE.
         * 3. Se registra el Kardex como RETURN.
         */
        @Test
        void incrementStockForReturn_Success_WhenStatusWasLoaned() {
                // ARRANGE (Preparar)
                Long toolId = 8L;

                // 1. Herramienta que fue prestada (última unidad)
                ToolEntity loanedTool = ToolEntity.builder()
                        .id(toolId)
                        .name("Herramienta Prestada")
                        .status(ToolStatus.LOANED) // <-- Estado clave
                        .stock(0)                  // <-- Stock clave
                        .replacementValue(10000)
                        .inRepair(0)
                        .build();
                
                // (No necesitamos mockear findById porque la herramienta se pasa como parámetro)
                
                // 2. Mockear el repositorio:
                // "Cuando se guarde, devolver la entidad que se le pasó"
                when(toolRepository.save(any(ToolEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
                // "Mockear el método void de Kardex"
                doNothing().when(kardexService).registerMovement(any(ToolEntity.class), any(MovementType.class), anyInt(), any(UserEntity.class));

                // ACT (Actuar)
                // Llamamos al método directamente
                toolService.incrementStockForReturn(loanedTool, testUser);

                // ASSERT (Verificar)
                // 1. Verificar los campos de la entidad (que se modificó directamente)
                assertEquals(1, loanedTool.getStock()); // Stock debe ser 1
                assertEquals(ToolStatus.AVAILABLE, loanedTool.getStatus()); // Estado debe ser AVAILABLE

                // 2. Verificar que se llamó a save
                verify(toolRepository, times(1)).save(loanedTool); // Se guarda la entidad modificada

                // 3. Verificar que se llamó al Kardex
                verify(kardexService, times(1)).registerMovement(
                        eq(loanedTool),                // La entidad
                        eq(MovementType.RETURN),       // El tipo de movimiento
                        eq(1),                         // La cantidad (siempre 1)
                        eq(testUser)                   // El usuario
                );
        }

        /**
         * Prueba que al devolver una herramienta (incrementar stock):
         * 1. El stock aumenta en 1.
         * 2. El estado NO cambia si ya era AVAILABLE (porque había más unidades).
         * 3. Se registra el Kardex como RETURN.
         */
        @Test
        void incrementStockForReturn_Success_WhenStatusWasAvailable() {
                // ARRANGE
                ToolEntity availableTool = ToolEntity.builder()
                        .id(9L)
                        .name("Herramienta Común")
                        .status(ToolStatus.AVAILABLE) // Ya estaba AVAILABLE
                        .stock(5)                     // Tenía stock
                        .replacementValue(10000)
                        .inRepair(0)
                        .build();

                when(toolRepository.save(any(ToolEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
                doNothing().when(kardexService).registerMovement(any(ToolEntity.class), any(MovementType.class), anyInt(), any(UserEntity.class));

                // ACT
                toolService.incrementStockForReturn(availableTool, testUser);

                // ASSERT
                assertEquals(6, availableTool.getStock()); // Stock aumentó
                assertEquals(ToolStatus.AVAILABLE, availableTool.getStatus()); // Estado NO cambió
                verify(toolRepository, times(1)).save(availableTool);
                verify(kardexService, times(1)).registerMovement(
                        eq(availableTool), eq(MovementType.RETURN), eq(1), eq(testUser)
                );
        }

        // =======================================================================
        // MÉTODO: decrementStockForLoan
        // Épica 2: Soporte para creación de préstamos
        // =======================================================================

        /**
         * Prueba que al prestar una herramienta (decrementar stock):
         * 1. El stock disminuye en 1.
         * 2. El estado NO cambia si el stock sigue siendo positivo.
         * 3. Se registra el Kardex como LOAN.
         */
        @Test
        void decrementStockForLoan_Success_WhenStockRemainsPositive() {
                // ARRANGE
                ToolEntity toolWithStock = ToolEntity.builder()
                        .id(10L)
                        .name("Herramienta con Stock")
                        .status(ToolStatus.AVAILABLE)
                        .stock(5) // <-- Stock positivo > 1
                        .replacementValue(10000)
                        .inRepair(0)
                        .build();

                when(toolRepository.save(any(ToolEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
                doNothing().when(kardexService).registerMovement(any(ToolEntity.class), any(MovementType.class), anyInt(), any(UserEntity.class));

                // ACT
                toolService.decrementStockForLoan(toolWithStock, testUser);

                // ASSERT
                assertEquals(4, toolWithStock.getStock()); // Stock disminuyó
                assertEquals(ToolStatus.AVAILABLE, toolWithStock.getStatus()); // Estado NO cambió
                verify(toolRepository, times(1)).save(toolWithStock);
                verify(kardexService, times(1)).registerMovement(
                        eq(toolWithStock), eq(MovementType.LOAN), eq(1), eq(testUser)
                );
        }

        /**
         * Prueba que al prestar la ÚLTIMA unidad de una herramienta:
         * 1. El stock disminuye a 0.
         * 2. El estado cambia de AVAILABLE a LOANED.
         * 3. Se registra el Kardex como LOAN.
         */
        @Test
        void decrementStockForLoan_Success_WhenStockReachesZero() {
                // ARRANGE
                ToolEntity lastUnitTool = ToolEntity.builder()
                        .id(11L)
                        .name("Última Unidad")
                        .status(ToolStatus.AVAILABLE)
                        .stock(1) // <-- Última unidad
                        .replacementValue(10000)
                        .inRepair(0)
                        .build();

                when(toolRepository.save(any(ToolEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
                doNothing().when(kardexService).registerMovement(any(ToolEntity.class), any(MovementType.class), anyInt(), any(UserEntity.class));

                // ACT
                toolService.decrementStockForLoan(lastUnitTool, testUser);

                // ASSERT
                assertEquals(0, lastUnitTool.getStock()); // Stock es CERO
                assertEquals(ToolStatus.LOANED, lastUnitTool.getStatus()); // Estado cambió a LOANED
                verify(toolRepository, times(1)).save(lastUnitTool);
                verify(kardexService, times(1)).registerMovement(
                        eq(lastUnitTool), eq(MovementType.LOAN), eq(1), eq(testUser)
                );
        }

        // =======================================================================
        // MÉTODO: markAsRepairing
        // Épica 2: Soporte para devolución con daño leve
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
                        .stock(5) // Stock disponible
                        .replacementValue(10000)
                        .inRepair(0) // Contador inicial
                        .loaned(1)   // the unit being returned
                        .build();

                // ACT
                toolService.markAsRepairing(toolToRepair, testUser);

                // ASSERT
                assertEquals(1, toolToRepair.getInRepair()); // 'inRepair' aumentó
                assertEquals(0, toolToRepair.getLoaned());   // the unit is no longer with the client
                assertEquals(5, toolToRepair.getStock());    // 'stock' no cambió
                assertEquals(ToolStatus.AVAILABLE, toolToRepair.getStatus()); // still has stock to lend

                verify(toolRepository, times(1)).save(toolToRepair);

                // Verificar que se llamó al Kardex
                verify(kardexService, times(1)).registerMovement(
                        eq(toolToRepair), eq(MovementType.REPAIR), eq(1), eq(testUser)
                );
        }

        /**
         * Prueba que NO se puede marcar para reparación una herramienta ya dada de baja.
         */
        @Test
        void markAsRepairing_Fails_WhenToolIsDecommissioned() {
                // ARRANGE
                ToolEntity decommissionedTool = ToolEntity.builder()
                        .id(13L)
                        .status(ToolStatus.DECOMMISSIONED) // <-- Estado inválido
                        .stock(0)
                        .inRepair(0)
                        .build();

                // ACT & ASSERT
                assertThrows(InvalidOperationException.class, () -> {
                toolService.markAsRepairing(decommissionedTool, testUser);
                }, "Debe lanzar InvalidOperationException si la herramienta está DECOMMISSIONED.");

                // Verificar que no se llamó al Kardex
                verify(kardexService, never()).registerMovement(any(), any(), anyInt(), any());
                verify(toolRepository, never()).save(any());
        }

        // =======================================================================
        // MÉTODO: markAsDecommissioned
        // Épica 2: Soporte para devolución con daño irreparable
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

                // Verificar que se llamó al Kardex
                verify(kardexService, times(1)).registerMovement(
                        eq(toolToDecommission), eq(MovementType.DECOMMISSION), eq(1), eq(testUser)
                );
        }

        /**
         * Prueba que NO se puede marcar (registrar baja en Kardex) si ya está DECOMMISSIONED.
         */
        @Test
        void markAsDecommissioned_Fails_WhenToolIsAlreadyDecommissioned() {
                // ARRANGE
                ToolEntity decommissionedTool = ToolEntity.builder()
                        .id(15L)
                        .status(ToolStatus.DECOMMISSIONED) // <-- Estado inválido
                        .build();

                // ACT & ASSERT
                assertThrows(InvalidOperationException.class, () -> {
                toolService.markAsDecommissioned(decommissionedTool, testUser);
                }, "Debe lanzar InvalidOperationException si la herramienta ya está DECOMMISSIONED.");

                // Verificar que no se llamó al Kardex
                verify(kardexService, never()).registerMovement(any(), any(), anyInt(), any());
                verify(toolRepository, never()).save(any());
        }

        // =======================================================================
        // MÉTODO: adjustStock (Ajuste Manual)
        // =======================================================================

        /**
         * Prueba un AUMENTO de stock manual exitoso.
         * Verifica que el stock aumente, el estado (si era LOANED) cambie a AVAILABLE,
         * y se registre el Kardex como INCOME.
         */
        @Test
        void adjustStock_Success_WhenIncreasingStock() {
                // ARRANGE
                Long toolId = 16L;
                ToolEntity tool = ToolEntity.builder()
                        .id(toolId)
                        .status(ToolStatus.LOANED) // Empezamos en LOANED
                        .stock(0)                  // Con stock 0
                        .replacementValue(10000)
                        .inRepair(0)
                        .build();
                
                when(toolRepository.findById(toolId)).thenReturn(Optional.of(tool));
                when(toolRepository.save(any(ToolEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
                doNothing().when(kardexService).registerMovement(any(), any(), anyInt(), any());

                // ACT: Aumentar stock en 5
                ToolEntity updatedTool = toolService.adjustStock(toolId, 5, testUser);

                // ASSERT
                assertEquals(5, updatedTool.getStock()); // Stock aumentó
                assertEquals(ToolStatus.AVAILABLE, updatedTool.getStatus()); // Estado cambió a AVAILABLE
                verify(toolRepository, times(1)).save(tool);
                verify(kardexService, times(1)).registerMovement(
                        eq(tool), eq(MovementType.INCOME), eq(5), eq(testUser) // Cantidad 5
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
                        .stock(3) // Stock inicial 3
                        .replacementValue(10000)
                        .inRepair(0)
                        .build();
                
                when(toolRepository.findById(toolId)).thenReturn(Optional.of(tool));
                when(toolRepository.save(any(ToolEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
                doNothing().when(kardexService).registerMovement(any(), any(), anyInt(), any());

                // ACT: Disminuir stock en 3
                ToolEntity updatedTool = toolService.adjustStock(toolId, -3, testUser);

                // ASSERT
                assertEquals(0, updatedTool.getStock()); // Stock es CERO
                assertEquals(ToolStatus.AVAILABLE, updatedTool.getStatus()); // nobody has it: not LOANED
                verify(toolRepository, times(1)).save(tool);
                verify(kardexService, times(1)).registerMovement(
                        eq(tool), eq(MovementType.MANUAL_DECREASE), eq(3), eq(testUser) // Cantidad 3 (Math.abs)
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
                        .status(ToolStatus.REPAIRING) // <-- Estado clave
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
                assertEquals(2, updatedTool.getStock()); // Stock aumentó
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
                Long toolId = 16L; // ID no importa
                
                // ACT & ASSERT
                assertThrows(InvalidOperationException.class, () -> {
                // quantityChange = 0
                toolService.adjustStock(toolId, 0, testUser);
                }, "Debe lanzar InvalidOperationException si quantityChange es 0.");
                
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
                }, "Debe lanzar ResourceNotFoundException si la herramienta no se encuentra.");
                
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
                        .status(ToolStatus.DECOMMISSIONED) // <-- Estado inválido
                        .stock(0)
                        .build();
                
                when(toolRepository.findById(toolId)).thenReturn(Optional.of(tool));

                // ACT & ASSERT
                assertThrows(InvalidOperationException.class, () -> {
                toolService.adjustStock(toolId, 5, testUser);
                }, "Debe lanzar InvalidOperationException si la herramienta está DECOMMISSIONED.");
                
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
                        .stock(3) // Stock inicial 3
                        .replacementValue(10000)
                        .inRepair(0)
                        .build();
                
                when(toolRepository.findById(toolId)).thenReturn(Optional.of(tool));

                // ACT & ASSERT
                assertThrows(InvalidOperationException.class, () -> {
                // Intentar restar 4 (3 - 4 = -1)
                toolService.adjustStock(toolId, -4, testUser);
                }, "Debe lanzar InvalidOperationException si el stock resultante es negativo.");

                verify(toolRepository, times(1)).findById(toolId);
                verify(toolRepository, never()).save(any());
                verify(kardexService, never()).registerMovement(any(), any(), anyInt(), any());
        }

        // =======================================================================
        // MÉTODO: getAllTools (Consulta)
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
        // MÉTODO: getToolById (Consulta)
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
                }, "Debe lanzar ResourceNotFoundException si la herramienta no se encuentra.");

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