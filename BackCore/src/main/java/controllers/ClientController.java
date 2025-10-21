package controllers;

import dtos.UpdateClientRequest;
import entities.ClientEntity;
import entities.enums.ClientStatus;
import jakarta.validation.Valid; 
import services.ClientService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/clients")
@CrossOrigin("*")
@PreAuthorize("hasRole('ADMIN')")
public class ClientController {

    private final ClientService clientService;

    public ClientController(ClientService clientService) { 
        this.clientService = clientService;
    }

    @GetMapping
    public List<ClientEntity> getAllClients() {
        return clientService.getAllClients();
    }

    // --- NUEVO ENDPOINT GET BY ID --- (Útil para el form de edición)
    @GetMapping("/{id}")
    public ClientEntity getClientById(@PathVariable Long id) {
        return clientService.getClientById(id);
    }


    @PostMapping
    public ClientEntity createClient(@Valid @RequestBody ClientEntity client) { // Añadir @Valid
        // El servicio asigna el estado ACTIVE por defecto
        return clientService.createClient(client);
    }

    // --- NUEVO ENDPOINT PARA EDITAR DATOS ---
    @PutMapping("/{id}")
    public ClientEntity updateClientDetails(@PathVariable Long id, @Valid @RequestBody UpdateClientRequest updateRequest) {
        return clientService.updateClientDetails(id, updateRequest);
    }


    @PatchMapping("/{id}/status")
    public ClientEntity updateClientStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String statusStr = body.get("status");
        if (statusStr == null) {
            throw new IllegalArgumentException("Status is required");
        }
        try {
            ClientStatus status = ClientStatus.valueOf(statusStr.toUpperCase());
            return clientService.updateStatus(id, status);
        } catch (IllegalArgumentException e) {
             throw new IllegalArgumentException("Invalid status value: " + statusStr + ". Must be ACTIVE or RESTRICTED.");
        }
    }

    // Endpoint obsoleto si usamos PATCH /status, pero lo dejamos por compatibilidad si se usó
    @PutMapping("/{id}/restrict")
    public ClientEntity restrictClient(@PathVariable Long id) { // Devolver entidad actualizada
        return clientService.updateStatus(id, ClientStatus.RESTRICTED);
    }
}