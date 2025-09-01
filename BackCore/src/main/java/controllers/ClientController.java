package controllers;

// Entidad, Enum y Servicio
import entities.ClientEntity;
import entities.enums.ClientStatus;
import services.ClientService;

import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/clients")
public class ClientController {

    private final ClientService clientService;

    public ClientController(ClientService clientService) {
        this.clientService = clientService;
    }

    @GetMapping
    public List<ClientEntity> getAllClients() {
        return clientService.getAllClients();
    }

    @GetMapping("/{id}")
    public Optional<ClientEntity> getClientById(@PathVariable Long id) {
        return clientService.getClientById(id);
    }

    @GetMapping("/rut/{rut}")
    public Optional<ClientEntity> getClientByRut(@PathVariable String rut) {
        return clientService.getClientByRut(rut);
    }

    @PostMapping
    public ClientEntity createClient(@RequestBody ClientEntity client) {
        return clientService.createClient(client);
    }

    @PutMapping("/{id}")
    public ClientEntity updateClient(@PathVariable Long id, @RequestBody ClientEntity client) {
        return clientService.updateClient(id, client);
    }

    @PutMapping("/{id}/status")
    public ClientEntity changeClientStatus(@PathVariable Long id, @RequestParam ClientStatus status) {
        return clientService.changeClientStatus(id, status);
    }

    @DeleteMapping("/{id}")
    public void deleteClient(@PathVariable Long id) {
        clientService.deleteClient(id);
    }
}
