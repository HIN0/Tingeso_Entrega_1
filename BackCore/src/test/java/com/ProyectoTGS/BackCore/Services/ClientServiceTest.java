package com.ProyectoTGS.BackCore.Services;

import entities.ClientEntity;
import entities.LoanEntity;
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
import services.ClientService;
import dtos.UpdateClientRequest;
import app.exceptions.InvalidOperationException;
import app.exceptions.ResourceNotFoundException;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.argThat;
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
        // New client with every required field
        newClient = ClientEntity.builder()
                .name("Juan Perez")
                .rut("11.111.111-1")
                .phone("912345678")
                .email("juan@example.com")
                .status(null) // to test the default status
                .build();

        // Existing client
        existingClient = ClientEntity.builder()
                .id(1L)
                .name("Maria Lopez")
                .rut("22.222.222-2")
                .phone("988887777")
                .email("maria@example.com")
                .status(ClientStatus.ACTIVE)
                .build();
    }

    // =======================================================================
    // METHOD: createClient
    // =======================================================================

    @Test
    void createClient_Success() {
        // ARRANGE: the RUT does not exist and save returns the saved object
        when(clientRepository.existsByRut(newClient.getRut())).thenReturn(false);
        // Capture the argument passed to save
        when(clientRepository.save(any(ClientEntity.class))).thenAnswer(invocation -> {
            ClientEntity clientToSave = invocation.getArgument(0);
            // the database assigns an ID
            if (clientToSave.getId() == null) clientToSave.setId(99L);
             // ACTIVE if it was null
            if (clientToSave.getStatus() == null) clientToSave.setStatus(ClientStatus.ACTIVE);
            return clientToSave;
        });

        // ACT
        ClientEntity created = clientService.createClient(newClient);

        // ASSERT: saved as ACTIVE (default status)
        assertNotNull(created);
        assertEquals(ClientStatus.ACTIVE, created.getStatus());
        verify(clientRepository, times(1)).save(newClient);
    }

    @Test
    void createClient_FailsIfRutAlreadyExists() {
        // ARRANGE: the RUT is already registered
        when(clientRepository.existsByRut(newClient.getRut())).thenReturn(true);

        // ACT & ASSERT
        assertThrows(IllegalArgumentException.class, () -> {
            clientService.createClient(newClient);
        }, "Must fail when the RUT already exists.");
        verify(clientRepository, never()).save(any());
    }

    @Test
    void createClient_FailsIfRequiredFieldIsNull_Name() {
        // ARRANGE: null name
        newClient.setName(null);

        // ACT & ASSERT
        assertThrows(IllegalArgumentException.class, () -> {
            clientService.createClient(newClient);
        }, "Must fail when the name is missing.");
        verify(clientRepository, never()).save(any());
    }

    @Test
    void createClient_FailsIfRequiredFieldIsNull_Rut() {
        // ARRANGE: null RUT
        newClient.setRut(null);

        // ACT & ASSERT
        assertThrows(IllegalArgumentException.class, () -> {
            clientService.createClient(newClient);
        }, "Must fail when the RUT is missing.");
        verify(clientRepository, never()).save(any());
    }

    @Test
    void createClient_FailsIfRequiredFieldIsNull_Phone() {
        // ARRANGE: null phone
        newClient.setPhone(null);

        // ACT & ASSERT
        assertThrows(IllegalArgumentException.class, () -> {
            clientService.createClient(newClient);
        }, "Must fail when the phone is missing.");
        verify(clientRepository, never()).save(any());
    }

    @Test
    void createClient_FailsIfNameIsBlank() {
        newClient.setName("   ");

        assertThrows(IllegalArgumentException.class, () -> clientService.createClient(newClient));
        verify(clientRepository, never()).save(any());
    }

    @Test
    void createClient_FailsIfRutCheckDigitIsInvalid() {
        newClient.setRut("11.111.111-2"); // correct check digit is 1

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> clientService.createClient(newClient));
        assertTrue(ex.getMessage().contains("check digit"));
        verify(clientRepository, never()).save(any());
    }

    @Test
    void createClient_NormalizesRutAndForcesActiveStatus() {
        newClient.setRut("111111111");                 // no dots or dash
        newClient.setStatus(ClientStatus.RESTRICTED);   // must be ignored for new clients
        when(clientRepository.existsByRut("11.111.111-1")).thenReturn(false);
        when(clientRepository.save(any(ClientEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        ClientEntity created = clientService.createClient(newClient);

        assertEquals("11.111.111-1", created.getRut());
        assertEquals(ClientStatus.ACTIVE, created.getStatus());
    }

     @Test
    void createClient_FailsIfRequiredFieldIsNull_Email() {
        // ARRANGE: null email
        newClient.setEmail(null);

        // ACT & ASSERT
        assertThrows(IllegalArgumentException.class, () -> {
            clientService.createClient(newClient);
        }, "Must fail when the email is missing.");
        verify(clientRepository, never()).save(any());
    }

    // =======================================================================
    // METHOD: updateStatus
    // =======================================================================

    @Test
    void updateStatus_ToRestricted_Success() {
        // ARRANGE: the client exists and is active
        when(clientRepository.findById(1L)).thenReturn(Optional.of(existingClient));
        when(clientRepository.save(any(ClientEntity.class))).thenReturn(existingClient);

        // ACT: change status to RESTRICTED
        ClientEntity updated = clientService.updateStatus(1L, ClientStatus.RESTRICTED);

        // ASSERT
        assertEquals(ClientStatus.RESTRICTED, updated.getStatus());
        verify(clientRepository, times(1)).save(existingClient);
    }

    @Test
    void updateStatus_ToActive_Success() {
        // ARRANGE: the client exists; change status to ACTIVE
        existingClient.setStatus(ClientStatus.RESTRICTED);
        when(clientRepository.findById(1L)).thenReturn(Optional.of(existingClient));
        when(clientRepository.save(any(ClientEntity.class))).thenReturn(existingClient);

        // ACT: change status to ACTIVE
        ClientEntity updated = clientService.updateStatus(1L, ClientStatus.ACTIVE);

        // ASSERT
        assertEquals(ClientStatus.ACTIVE, updated.getStatus());
        verify(clientRepository, times(1)).save(existingClient);
    }

    @Test
    void updateStatus_FailsIfClientNotFound() {
        // ARRANGE: the client does NOT exist
        when(clientRepository.findById(99L)).thenReturn(Optional.empty());

        // ACT & ASSERT: ResourceNotFoundException expected
        assertThrows(ResourceNotFoundException.class, () -> {
            clientService.updateStatus(99L, ClientStatus.RESTRICTED);
        }, "Must fail when the client is not found.");
        verify(clientRepository, never()).save(any());
    }

    // =======================================================================
    // METHOD: updateClientDetails
    // =======================================================================
    @Test
    void updateClientDetails_Success() {
        // ARRANGE: update data and repository mock
        Long clientId = 1L;
        UpdateClientRequest updateRequest = new UpdateClientRequest(
                "Maria Lopez Actualizada", // new name
                "987654321", // new phone
                "maria.actualizada@example.com" // new email
        );

        // Existing client BEFORE the update (original RUT and status)
        ClientEntity clientBeforeUpdate = ClientEntity.builder()
                .id(clientId)
                .name("Maria Lopez")
                .rut("22.222.222-2")
                .phone("911223344")
                .email("maria@example.com")
                .status(ClientStatus.ACTIVE)
                .build();

        when(clientRepository.findById(clientId)).thenReturn(Optional.of(clientBeforeUpdate));
        // save returns the entity it receives
        when(clientRepository.save(any(ClientEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // ACT
        ClientEntity updatedClient = clientService.updateClientDetails(clientId, updateRequest);

        // ASSERT: updated fields and the ones that must NOT change
        assertNotNull(updatedClient);
        assertEquals(clientId, updatedClient.getId()); // ID unchanged
        assertEquals("Maria Lopez Actualizada", updatedClient.getName()); // name updated
        assertEquals("987654321", updatedClient.getPhone()); // phone updated
        assertEquals("maria.actualizada@example.com", updatedClient.getEmail()); // email updated
        assertEquals("22.222.222-2", updatedClient.getRut()); // RUT must NOT change
        assertEquals(ClientStatus.ACTIVE, updatedClient.getStatus()); // status must NOT change

        // save was called once with the expected entity
        verify(clientRepository, times(1)).findById(clientId);
        verify(clientRepository, times(1)).save(argThat(client ->
                client.getId().equals(clientId) &&
                client.getName().equals("Maria Lopez Actualizada") &&
                client.getPhone().equals("987654321") &&
                client.getEmail().equals("maria.actualizada@example.com") &&
                client.getRut().equals("22.222.222-2") && // non-editable fields unchanged
                client.getStatus().equals(ClientStatus.ACTIVE)
        ));
    }

    @Test
    void updateClientDetails_FailsWhenClientNotFound() {
        // ARRANGE: unknown client ID and update data
        Long nonExistentClientId = 99L;
        UpdateClientRequest updateRequest = new UpdateClientRequest(
                "Nombre No Importa",
                "123456789",
                "noexiste@example.com"
        );

        // Mock: findById returns an empty Optional
        when(clientRepository.findById(nonExistentClientId)).thenReturn(Optional.empty());

        // ACT & ASSERT: ResourceNotFoundException expected
        assertThrows(ResourceNotFoundException.class, () -> {
            clientService.updateClientDetails(nonExistentClientId, updateRequest);
        }, "Must throw ResourceNotFoundException when the client is not found.");

        // findById was called
        verify(clientRepository, times(1)).findById(nonExistentClientId);
        // CRITICAL: nothing must be saved
        verify(clientRepository, never()).save(any(ClientEntity.class));
    }

// =======================================================================
    // METHOD: attemptClientReactivation
    // =======================================================================

    @Mock // LoanRepository is also needed
    private LoanRepository loanRepository;

    @Test
    void attemptClientReactivation_Success_WhenNoDebtsOrLateLoans() {
        // ARRANGE: restricted client without debts or late loans
        Long clientId = 3L;
        ClientEntity restrictedClient = ClientEntity.builder()
                .id(clientId)
                .name("Pedro Restringido")
                .status(ClientStatus.RESTRICTED)
                .build();

        // Mocks:
        // 1. Find the client
        when(clientRepository.findById(clientId)).thenReturn(Optional.of(restrictedClient));
        // 2. No LATE loans
        when(loanRepository.countByClientAndStatus(restrictedClient, LoanStatus.LATE)).thenReturn(0L); // 0 LATE loans
        // 3. No RECEIVED loans with debt
        when(loanRepository.findByClientAndStatusAndTotalPenaltyGreaterThan(restrictedClient, LoanStatus.RECEIVED, 0.0))
                .thenReturn(Collections.emptyList()); // empty list = no debts
        // 4. Saving (updateStatus is called internally)
        // returns the client already ACTIVE
        when(clientRepository.save(any(ClientEntity.class))).thenAnswer(invocation -> {
            ClientEntity clientToSave = invocation.getArgument(0);
            clientToSave.setStatus(ClientStatus.ACTIVE); // simulated status change
            return clientToSave;
        });


        // ACT: try to reactivate the client
        ClientEntity reactivatedClient = clientService.attemptClientReactivation(clientId);

        // ASSERT: status changed to ACTIVE
        assertNotNull(reactivatedClient);
        assertEquals(ClientStatus.ACTIVE, reactivatedClient.getStatus());

        // the required checks were made
        verify(clientRepository, times(2)).findById(clientId);
        verify(loanRepository, times(1)).countByClientAndStatus(restrictedClient, LoanStatus.LATE);
        verify(loanRepository, times(1)).findByClientAndStatusAndTotalPenaltyGreaterThan(restrictedClient, LoanStatus.RECEIVED, 0.0);
        // the status change was saved
        verify(clientRepository, times(1)).save(argThat(client ->
                client.getId().equals(clientId) && client.getStatus().equals(ClientStatus.ACTIVE)
        ));
    }

    @Test
    void attemptClientReactivation_DoesNothing_WhenClientAlreadyActive() {
        // ARRANGE: client already ACTIVE
        Long clientId = 4L;
        ClientEntity activeClient = ClientEntity.builder()
                .id(clientId)
                .name("Ana Activa")
                .status(ClientStatus.ACTIVE) // initial status is ACTIVE
                .build();

        // Mock: find the active client
        when(clientRepository.findById(clientId)).thenReturn(Optional.of(activeClient));

        // ACT: try to reactivate an active client
        ClientEntity resultClient = clientService.attemptClientReactivation(clientId);

        // ASSERT: the same client is returned, still ACTIVE
        assertNotNull(resultClient);
        assertEquals(clientId, resultClient.getId());
        assertEquals(ClientStatus.ACTIVE, resultClient.getStatus());

        // findById was called only once
        verify(clientRepository, times(1)).findById(clientId);
        // CRITICAL: no loan checks were made
        verify(loanRepository, never()).countByClientAndStatus(any(ClientEntity.class), any(LoanStatus.class));
        verify(loanRepository, never()).findByClientAndStatusAndTotalPenaltyGreaterThan(any(ClientEntity.class), any(LoanStatus.class), anyDouble());
        // CRITICAL: nothing must be saved
        verify(clientRepository, never()).save(any(ClientEntity.class));
    }

    @Test
    void attemptClientReactivation_Fails_WhenClientHasLateLoans() {
        // ARRANGE: restricted client with late loans
        Long clientId = 5L;
        ClientEntity restrictedClientWithLateLoan = ClientEntity.builder()
                .id(clientId)
                .name("Carlos Atrasado")
                .status(ClientStatus.RESTRICTED)
                .build();

        // Mocks:
        // 1. Find the client
        when(clientRepository.findById(clientId)).thenReturn(Optional.of(restrictedClientWithLateLoan));
        // 2. The client HAS LATE loans (> 0)
        when(loanRepository.countByClientAndStatus(restrictedClientWithLateLoan, LoanStatus.LATE)).thenReturn(1L); // 1 LATE loan

        // ACT & ASSERT: InvalidOperationException expected
        assertThrows(InvalidOperationException.class, () -> {
            clientService.attemptClientReactivation(clientId);
        }, "Must throw InvalidOperationException because the client has LATE loans.");

        // checks were made up to the failure point
        verify(clientRepository, times(1)).findById(clientId);
        verify(loanRepository, times(1)).countByClientAndStatus(restrictedClientWithLateLoan, LoanStatus.LATE);
        // CRITICAL: RECEIVED debts were NOT checked afterwards
        verify(loanRepository, never()).findByClientAndStatusAndTotalPenaltyGreaterThan(any(ClientEntity.class), any(LoanStatus.class), anyDouble());
        // CRITICAL: nothing must be saved (no status change)
        verify(clientRepository, never()).save(any(ClientEntity.class));
    }

    @Test
    void attemptClientReactivation_Fails_WhenClientHasUnpaidReceivedLoans() {
        // ARRANGE: restricted client without LATE loans but with RECEIVED debts
        Long clientId = 6L;
        ClientEntity restrictedClientWithDebt = ClientEntity.builder()
                .id(clientId)
                .name("Daniela Deudora")
                .status(ClientStatus.RESTRICTED)
                .build();

        // A RECEIVED loan with a pending penalty
        LoanEntity unpaidLoan = LoanEntity.builder()
                .id(101L)
                .client(restrictedClientWithDebt)
                .status(LoanStatus.RECEIVED)
                .totalPenalty(5000.0) // pending debt
                .build();

        // Mocks:
        // 1. Find the client
        when(clientRepository.findById(clientId)).thenReturn(Optional.of(restrictedClientWithDebt));
        // 2. No LATE loans
        when(loanRepository.countByClientAndStatus(restrictedClientWithDebt, LoanStatus.LATE)).thenReturn(0L);
        // 3. The client HAS pending debts (non-empty list)
        when(loanRepository.findByClientAndStatusAndTotalPenaltyGreaterThan(restrictedClientWithDebt, LoanStatus.RECEIVED, 0.0))
                .thenReturn(List.of(unpaidLoan)); // list with the unpaid loan

        // ACT & ASSERT: InvalidOperationException expected
        assertThrows(InvalidOperationException.class, () -> {
            clientService.attemptClientReactivation(clientId);
        }, "Must throw InvalidOperationException because the client has pending debts.");

        // checks were made up to the failure point
        verify(clientRepository, times(1)).findById(clientId);
        verify(loanRepository, times(1)).countByClientAndStatus(restrictedClientWithDebt, LoanStatus.LATE);
        verify(loanRepository, times(1)).findByClientAndStatusAndTotalPenaltyGreaterThan(restrictedClientWithDebt, LoanStatus.RECEIVED, 0.0);
        // CRITICAL: nothing must be saved (no status change)
        verify(clientRepository, never()).save(any(ClientEntity.class));
    }

    @Test
    void attemptClientReactivation_Fails_WhenClientNotFound() {
        // ARRANGE: unknown client ID
        Long nonExistentClientId = 99L;

        // Mock: findById returns an empty Optional
        when(clientRepository.findById(nonExistentClientId)).thenReturn(Optional.empty());

        // ACT & ASSERT: ResourceNotFoundException thrown by getClientById
        assertThrows(ResourceNotFoundException.class, () -> {
            clientService.attemptClientReactivation(nonExistentClientId);
        }, "Must throw ResourceNotFoundException when the client is not found.");

        // the client was looked up only once
        verify(clientRepository, times(1)).findById(nonExistentClientId);
        // CRITICAL: no loan checks were made
        verify(loanRepository, never()).countByClientAndStatus(any(ClientEntity.class), any(LoanStatus.class));
        verify(loanRepository, never()).findByClientAndStatusAndTotalPenaltyGreaterThan(any(ClientEntity.class), any(LoanStatus.class), anyDouble());
        // CRITICAL: nothing must be saved
        verify(clientRepository, never()).save(any(ClientEntity.class));
    }

    // =======================================================================
    // METHOD: getAllClients
    // =======================================================================

    @Test
    void getAllClients_ReturnsListOfClients() {
        // ARRANGE: list of clients
        ClientEntity client1 = ClientEntity.builder().id(1L).name("Cliente Uno").build();
        ClientEntity client2 = ClientEntity.builder().id(2L).name("Cliente Dos").build();
        List<ClientEntity> mockClientList = Arrays.asList(client1, client2);

        // Mock: findAll() returns the list
        when(clientRepository.findAll()).thenReturn(mockClientList);

        // ACT
        List<ClientEntity> resultList = clientService.getAllClients();

        // ASSERT: the list is not null and has the expected size
        assertNotNull(resultList);
        assertEquals(2, resultList.size());
        assertEquals("Cliente Uno", resultList.get(0).getName());
        assertEquals("Cliente Dos", resultList.get(1).getName());

        // findAll() was called exactly once
        verify(clientRepository, times(1)).findAll();
    }

    @Test
    void getAllClients_ReturnsEmptyListWhenNoClients() {
        // ARRANGE: findAll() returns an empty list
        when(clientRepository.findAll()).thenReturn(Collections.emptyList());

        // ACT
        List<ClientEntity> resultList = clientService.getAllClients();

        // ASSERT: the list is not null and is empty
        assertNotNull(resultList);
        assertTrue(resultList.isEmpty());

        // findAll() was called exactly once
        verify(clientRepository, times(1)).findAll();
    }

    // =======================================================================
    // METHOD: getClientById
    // =======================================================================

    @Test
    void getClientById_Success_WhenClientExists() {
        // ARRANGE: an existing client
        Long clientId = 1L;
        ClientEntity existingClient = ClientEntity.builder()
                .id(clientId)
                .name("Cliente Existente")
                .rut("11.111.111-1")
                .status(ClientStatus.ACTIVE)
                .build();

        // Mock: findById returns the client
        when(clientRepository.findById(clientId)).thenReturn(Optional.of(existingClient));

        // ACT
        ClientEntity foundClient = clientService.getClientById(clientId);

        // ASSERT: the expected client is returned
        assertNotNull(foundClient);
        assertEquals(clientId, foundClient.getId());
        assertEquals("Cliente Existente", foundClient.getName());

        // findById was called once
        verify(clientRepository, times(1)).findById(clientId);
    }

    @Test
    void getClientById_Fails_WhenClientNotFound() {
        // ARRANGE: unknown client ID
        Long nonExistentClientId = 99L;

        // Mock: findById returns an empty Optional
        when(clientRepository.findById(nonExistentClientId)).thenReturn(Optional.empty());

        // ACT & ASSERT: ResourceNotFoundException expected
        assertThrows(ResourceNotFoundException.class, () -> {
            clientService.getClientById(nonExistentClientId);
        }, "Must throw ResourceNotFoundException when the client is not found.");

        // findById was called once
        verify(clientRepository, times(1)).findById(nonExistentClientId);
    }
}