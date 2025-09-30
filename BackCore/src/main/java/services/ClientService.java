package services;

import entities.ClientEntity;
import entities.enums.ClientStatus;
import repositories.ClientRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ClientService {

    private final ClientRepository clientRepository;

    public ClientService(ClientRepository clientRepository) {
        this.clientRepository = clientRepository;
    }

    public List<ClientEntity> getAllClients() {
        return clientRepository.findAll();
    }

    public ClientEntity createClient(ClientEntity client) {
        if (client.getName() == null || client.getRut() == null || client.getPhone() == null || client.getEmail() == null) {
            throw new IllegalArgumentException("Client must have name, rut, phone, and email");
        }
        if (clientRepository.existsByRut(client.getRut())) {
            throw new IllegalArgumentException("Client with this RUT already exists");
        }
        client.setStatus(ClientStatus.ACTIVE);
        return clientRepository.save(client);
    }

    public ClientEntity updateStatus(Long id, ClientStatus status) {
        ClientEntity client = clientRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Client not found"));
        client.setStatus(status);
        return clientRepository.save(client);
    }
}
