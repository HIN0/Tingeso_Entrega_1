package services;

// Entidad, Enum y Repositorio
import entities.ClientEntity;
import entities.enums.ClientStatus;
import repositories.ClientRepository;

import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class ClientService {

    private final ClientRepository clientRepository;

    public ClientService(ClientRepository clientRepository) {
        this.clientRepository = clientRepository;
    }

    // Obtener todos los clientes
    public List<ClientEntity> getAllClients() {
        return clientRepository.findAll();
    }

    // Buscar cliente por ID
    public Optional<ClientEntity> getClientById(Long id) {
        return clientRepository.findById(id);
    }

    // Buscar cliente por RUT
    public Optional<ClientEntity> getClientByRut(String rut) {
        return clientRepository.findByRut(rut);
    }

    // Registrar nuevo cliente (con reglas de negocio)
    public ClientEntity createClient(ClientEntity client) {
        if (client.getName() == null || client.getRut() == null ||
            client.getPhone() == null || client.getEmail() == null) {
            throw new IllegalArgumentException("Client must have name, RUT, phone and email");
        }

        // Verificar unicidad de RUT
        if (clientRepository.findByRut(client.getRut()).isPresent()) {
            throw new IllegalArgumentException("Client with RUT already exists");
        }

        client.setStatus(ClientStatus.ACTIVE); // por defecto entra Activo
        return clientRepository.save(client);
    }

    // Actualizar cliente
    public ClientEntity updateClient(Long id, ClientEntity updatedClient) {
        ClientEntity existing = clientRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Client not found"));

        if (updatedClient.getName() != null) existing.setName(updatedClient.getName());
        if (updatedClient.getPhone() != null) existing.setPhone(updatedClient.getPhone());
        if (updatedClient.getEmail() != null) existing.setEmail(updatedClient.getEmail());

        return clientRepository.save(existing);
    }

    // Cambiar estado del cliente (ej: restringido por atrasos)
    public ClientEntity changeClientStatus(Long id, ClientStatus status) {
        ClientEntity client = clientRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Client not found"));

        client.setStatus(status);
        return clientRepository.save(client);
    }

    // Eliminar cliente
    public void deleteClient(Long id) {
        if (!clientRepository.existsById(id)) {
            throw new IllegalArgumentException("Client not found");
        }
        clientRepository.deleteById(id);
    }
}
