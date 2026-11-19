package services;

import app.exceptions.InvalidOperationException;
import app.exceptions.ResourceNotFoundException;
import dtos.UpdateClientRequest; 
import entities.ClientEntity;
import entities.LoanEntity;
import entities.enums.ClientStatus;
import entities.enums.LoanStatus;
import jakarta.validation.Valid; 
import repositories.ClientRepository;
import repositories.LoanRepository;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; 
import org.springframework.validation.annotation.Validated;

@Service
@Validated
public class ClientService {

    private final ClientRepository clientRepository;
    private final LoanRepository loanRepository;

    public ClientService(ClientRepository clientRepository, LoanRepository loanRepository) {
        this.clientRepository = clientRepository;
        this.loanRepository = loanRepository;
    }

    public List<ClientEntity> getAllClients() {
        return clientRepository.findAll();
    }

    public ClientEntity getClientById(Long id) {
        return clientRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Client not found with id: " + id));
    }

    @Transactional
    public ClientEntity createClient(ClientEntity client) {
        // Business rule (Epic 3): name, RUT, phone and email are mandatory (blank counts as missing)
        if (isBlank(client.getName()) || isBlank(client.getRut()) || isBlank(client.getPhone()) || isBlank(client.getEmail())) {
            throw new IllegalArgumentException("Client must have name, rut, phone, and email");
        }
        // Store the RUT in one canonical format so "12345678-5" and "12.345.678-5" are the same client
        client.setRut(RutValidator.normalize(client.getRut()));
        if (clientRepository.existsByRut(client.getRut())) {
            throw new IllegalArgumentException("Client with this RUT already exists");
        }
        // A new client always starts ACTIVE (restrictions come from late loans or debts)
        client.setStatus(ClientStatus.ACTIVE);
        return clientRepository.save(client);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    @Transactional 
    public ClientEntity updateStatus(Long id, ClientStatus status) {
        // Reuse getClientById (throws 404 if missing)
        ClientEntity client = getClientById(id);
        client.setStatus(status);
        return clientRepository.save(client);
    }

    @Transactional
    public ClientEntity updateClientDetails(Long id, @Valid UpdateClientRequest updateRequest) {
        ClientEntity client = getClientById(id);
        // Update only the fields allowed by the DTO
        client.setName(updateRequest.name());
        client.setPhone(updateRequest.phone());
        client.setEmail(updateRequest.email());
        // RUT and status are not changed here (status follows RF3.2)
        return clientRepository.save(client); // save changes
    }

    @Transactional
    public ClientEntity attemptClientReactivation(Long clientId) {
        // 1. Load the client
        ClientEntity client = getClientById(clientId);

        // 2. Already active: nothing to do
        if (client.getStatus() == ClientStatus.ACTIVE) {
            System.out.println("Client " + clientId + " is already active.");
            return client;
        }

        // 3. Must not have LATE loans
        long lateLoanCount = loanRepository.countByClientAndStatus(client, LoanStatus.LATE);
        if (lateLoanCount > 0) {
            throw new InvalidOperationException("Cannot reactivate client: " + lateLoanCount + " late loan(s) found.");
        }

        // 4. Must not have unpaid debts (RECEIVED with totalPenalty > 0)
        List<LoanEntity> unpaidReceivedLoans = loanRepository.findByClientAndStatusAndTotalPenaltyGreaterThan(
                client, LoanStatus.RECEIVED, 0.0);
        if (!unpaidReceivedLoans.isEmpty()) {
            throw new InvalidOperationException("Cannot reactivate client: " + unpaidReceivedLoans.size() + " unpaid loan(s) found.");
        }

        // 5. All checks passed: reactivate
        System.out.println("Client " + clientId + " meets criteria for reactivation. Setting status to ACTIVE.");
        return updateStatus(clientId, ClientStatus.ACTIVE); // reuse the existing method
    }
}