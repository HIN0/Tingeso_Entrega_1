package controllers;

import entities.ClientEntity;
import entities.enums.ClientStatus;
import services.ClientService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/clients")
@CrossOrigin("*")
public class ClientController {

    private final ClientService clientService;

    public ClientController(ClientService clientService) {
        this.clientService = clientService;
    }

    @GetMapping
    public List<ClientEntity> getAllClients() {
        return clientService.getAllClients();
    }

    @PostMapping
    public ClientEntity createClient(@RequestBody ClientEntity client) {
        return clientService.createClient(client);
    }

    // Nuevo endpoint: permite ACTIVE o RESTRICTED
    @PatchMapping("/{id}/status")
    public ClientEntity updateClientStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String statusStr = body.get("status");
        if (statusStr == null) {
            throw new IllegalArgumentException("Status is required");
        }
        ClientStatus status = ClientStatus.valueOf(statusStr.toUpperCase());
        return clientService.updateStatus(id, status);
    }

    // dejar este para compatibilidad, pero ya no es necesario
    @PutMapping("/{id}/restrict")
    public void restrictClient(@PathVariable Long id) {
        clientService.updateStatus(id, ClientStatus.RESTRICTED);
    }
}
