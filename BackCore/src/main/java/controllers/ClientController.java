package controllers;

import entities.ClientEntity;
import entities.UserEntity;
import entities.enums.ClientStatus;
import services.ClientService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import app.utils.SecurityUtils;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/clients")
@CrossOrigin("*")
@PreAuthorize("hasRole('ADMIN')") // Protege todos los métodos para solo ADMIN
public class ClientController {

    private final ClientService clientService;
    private final SecurityUtils securityUtils; // Inyectar SecurityUtils

    public ClientController(ClientService clientService, SecurityUtils securityUtils) {
        this.clientService = clientService;
        this.securityUtils = securityUtils;
    }

    @GetMapping
    public List<ClientEntity> getAllClients() {
        return clientService.getAllClients();
    }

    @PostMapping
    // No necesita Authentication ni SecurityUtils, pero se podría usar para Kardex futuro.
    public ClientEntity createClient(@RequestBody ClientEntity client) { 
        return clientService.createClient(client);
    }

    @PatchMapping("/{id}/status")
    public ClientEntity updateClientStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String statusStr = body.get("status");
        if (statusStr == null) {
            throw new IllegalArgumentException("Status is required");
        }
        ClientStatus status = ClientStatus.valueOf(statusStr.toUpperCase());
        return clientService.updateStatus(id, status);
    }

    @PutMapping("/{id}/restrict")
    public void restrictClient(@PathVariable Long id) {
        clientService.updateStatus(id, ClientStatus.RESTRICTED);
    }
}