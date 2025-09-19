package controllers;

import entities.ClientEntity;
import services.ClientService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

    @PostMapping
    public ClientEntity createClient(@RequestBody ClientEntity client) {
        return clientService.createClient(client);
    }

    @PutMapping("/{id}/restrict")
    public void restrictClient(@PathVariable Long id) {
        clientService.restrictClient(id);
    }
}
