package com.ProyectoTGS.BackCore.Services;

import entities.ClientEntity;
import entities.enums.ClientStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import repositories.ClientRepository;
import services.ClientService;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ClientServiceTest {

    @Mock
    private ClientRepository clientRepository;

    @InjectMocks
    private ClientService clientService;

    private ClientEntity newClient;
    private ClientEntity existingClient;

    @BeforeEach
    void setUp() {
        // Cliente nuevo con todos los campos requeridos
        newClient = ClientEntity.builder()
                .name("Juan Perez")
                .rut("11.111.111-1")
                .phone("912345678")
                .email("juan@example.com")
                .build();
        
        // Cliente existente
        existingClient = ClientEntity.builder()
                .id(1L)
                .name("Maria Lopez")
                .rut("22.222.222-2")
                .status(ClientStatus.ACTIVE)
                .build();
    }

    // =======================================================================
    // ÉPICA 3: CREACIÓN DE CLIENTES (createClient)
    // =======================================================================

    @Test
    void createClient_Success() {
        // ARRANGE: Simular que el RUT no existe y que la persistencia devuelve el objeto guardado
        when(clientRepository.existsByRut(newClient.getRut())).thenReturn(false);
        when(clientRepository.save(any(ClientEntity.class))).thenReturn(newClient);

        // ACT
        ClientEntity created = clientService.createClient(newClient);

        // ASSERT: Verifica que se guarde como ACTIVE (estado por defecto)
        assertNotNull(created);
        assertEquals(ClientStatus.ACTIVE, created.getStatus());
        verify(clientRepository, times(1)).save(newClient);
    }

    @Test
    void createClient_FailsIfRutAlreadyExists() {
        // ARRANGE: Simular que el RUT ya está en la base de datos
        when(clientRepository.existsByRut(newClient.getRut())).thenReturn(true);

        // ACT & ASSERT
        assertThrows(IllegalArgumentException.class, () -> {
            clientService.createClient(newClient);
        }, "Debe fallar si el RUT ya existe.");
        verify(clientRepository, never()).save(any());
    }

    @Test
    void createClient_FailsIfRequiredFieldIsNull() {
        // ARRANGE: Probar un campo crítico como null (email)
        newClient.setEmail(null); 

        // ACT & ASSERT
        assertThrows(IllegalArgumentException.class, () -> {
            clientService.createClient(newClient);
        }, "Debe fallar si falta un campo requerido.");
        verify(clientRepository, never()).save(any());
    }

    // =======================================================================
    // ÉPICA 3: GESTIÓN DE ESTADO (updateStatus)
    // =======================================================================

    @Test
    void updateStatus_ToRestricted_Success() {
        // ARRANGE: Simular que el cliente existe y está activo
        when(clientRepository.findById(1L)).thenReturn(Optional.of(existingClient));
        when(clientRepository.save(any(ClientEntity.class))).thenReturn(existingClient);

        // ACT: Cambiar estado a RESTRICTED
        ClientEntity updated = clientService.updateStatus(1L, ClientStatus.RESTRICTED);

        // ASSERT
        assertEquals(ClientStatus.RESTRICTED, updated.getStatus());
        verify(clientRepository, times(1)).save(existingClient);
    }
    
    @Test
    void updateStatus_ToActive_Success() {
        // ARRANGE: Simular que el cliente existe y cambiar estado a ACTIVE
        existingClient.setStatus(ClientStatus.RESTRICTED);
        when(clientRepository.findById(1L)).thenReturn(Optional.of(existingClient));
        when(clientRepository.save(any(ClientEntity.class))).thenReturn(existingClient);

        // ACT: Cambiar estado a ACTIVE
        ClientEntity updated = clientService.updateStatus(1L, ClientStatus.ACTIVE);

        // ASSERT
        assertEquals(ClientStatus.ACTIVE, updated.getStatus());
        verify(clientRepository, times(1)).save(existingClient);
    }

    @Test
    void updateStatus_FailsIfClientNotFound() {
        // ARRANGE: Simular que el cliente NO existe
        when(clientRepository.findById(99L)).thenReturn(Optional.empty());

        // ACT & ASSERT
        assertThrows(RuntimeException.class, () -> {
            clientService.updateStatus(99L, ClientStatus.RESTRICTED);
        }, "Debe fallar si el cliente no es encontrado.");
        verify(clientRepository, never()).save(any());
    }
}