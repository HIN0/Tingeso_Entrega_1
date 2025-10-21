package services;

import app.exceptions.ResourceNotFoundException;
import dtos.UpdateClientRequest; 
import entities.ClientEntity;
import entities.enums.ClientStatus;
import jakarta.validation.Valid; 
import repositories.ClientRepository;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; 
import org.springframework.validation.annotation.Validated;

@Service
@Validated // Habilitar validación para DTOs
public class ClientService {

    private final ClientRepository clientRepository;

    public ClientService(ClientRepository clientRepository) {
        this.clientRepository = clientRepository;
    }

    public List<ClientEntity> getAllClients() {
        return clientRepository.findAll();
    }

    // --- OBTENER CLIENTE POR ID  ---
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
            // Podríamos usar una excepción personalizada aquí también
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

    // --- MÉTODO PARA ACTUALIZAR DATOS ---
    @Transactional
    public ClientEntity updateClientDetails(Long id, @Valid UpdateClientRequest updateRequest) {
        ClientEntity client = getClientById(id); // Obtener cliente existente

        // Actualizar solo los campos permitidos desde el DTO
        client.setName(updateRequest.name());
        client.setPhone(updateRequest.phone());
        client.setEmail(updateRequest.email());
        // RUT y Status no se modifican aquí

        return clientRepository.save(client); // Guardar los cambios
    }
}