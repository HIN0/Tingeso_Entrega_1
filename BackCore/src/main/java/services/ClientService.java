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
    public ClientEntity createClient(@Valid ClientEntity client) {
        // Validaciones básicas ya en la entidad o vía @Valid
        if (client.getName() == null || client.getRut() == null || client.getPhone() == null || client.getEmail() == null) {
            throw new IllegalArgumentException("Client must have name, rut, phone, and email");
        }
        if (clientRepository.existsByRut(client.getRut())) {
            // Excepción personalizada
            throw new IllegalArgumentException("Client with this RUT already exists");
        }
        // Asignar estado inicial explícitamente si no viene (aunque ya lo hace)
        if (client.getStatus() == null) {
            client.setStatus(ClientStatus.ACTIVE);
        }
        return clientRepository.save(client);
    }

    @Transactional 
    public ClientEntity updateStatus(Long id, ClientStatus status) {
        // Usar método auxiliar getClientById
        ClientEntity client = getClientById(id);
        client.setStatus(status);
        return clientRepository.save(client);
    }

    @Transactional
    public ClientEntity updateClientDetails(Long id, @Valid UpdateClientRequest updateRequest) {
        ClientEntity client = getClientById(id);
        // Actualizar solo los campos permitidos desde el DTO
        client.setName(updateRequest.name());
        client.setPhone(updateRequest.phone());
        client.setEmail(updateRequest.email());
        // RUT y Status no se modifican aquí
        return clientRepository.save(client); // Guardar cambios
    }

// --- NUEVO MÉTODO PARA INTENTAR REACTIVAR UN CLIENTE ---
    @Transactional
    public ClientEntity attemptClientReactivation(Long clientId) {
        // 1. Obtener el cliente
        ClientEntity client = getClientById(clientId);

        // 2. Si ya está activo, no hacer nada
        if (client.getStatus() == ClientStatus.ACTIVE) {
            System.out.println("Client " + clientId + " is already active.");
            return client;
            // O podrías lanzar: throw new InvalidOperationException("Client is already active.");
        }

        // 3. Verificar si tiene préstamos ATRASADOS (LATE)
        long lateLoanCount = loanRepository.countByClientAndStatus(client, LoanStatus.LATE);
        if (lateLoanCount > 0) {
            throw new InvalidOperationException("Cannot reactivate client: " + lateLoanCount + " late loan(s) found.");
        }

        // 4. Verificar si tiene deudas PENDIENTES (RECEIVED con totalPenalty > 0)
        List<LoanEntity> unpaidReceivedLoans = loanRepository.findByClientAndStatusAndTotalPenaltyGreaterThan(
                client, LoanStatus.RECEIVED, 0.0);
        if (!unpaidReceivedLoans.isEmpty()) {
            throw new InvalidOperationException("Cannot reactivate client: " + unpaidReceivedLoans.size() + " unpaid loan(s) found.");
        }

        // 5. Si pasa las validaciones, reactivar
        System.out.println("Client " + clientId + " meets criteria for reactivation. Setting status to ACTIVE.");
        return updateStatus(clientId, ClientStatus.ACTIVE); // Reutiliza el método existente
    }
}