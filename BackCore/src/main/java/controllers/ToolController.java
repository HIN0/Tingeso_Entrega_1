package controllers;

import entities.ToolEntity;
import entities.UserEntity;
import services.ToolService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import app.utils.SecurityUtils; // Nueva importación

import java.util.List;

@RestController
@RequestMapping("/tools")
@CrossOrigin("*")
public class ToolController {

    private final ToolService toolService;
    private final SecurityUtils securityUtils; // CAMBIO: Inyectar SecurityUtils

    public ToolController(ToolService toolService, SecurityUtils securityUtils) { // CAMBIO: Constructor con SecurityUtils
        this.toolService = toolService;
        this.securityUtils = securityUtils;
    }

    @GetMapping
    public List<ToolEntity> getAllTools() {
        return toolService.getAllTools();
    }

    @GetMapping("/{id}")
    public ToolEntity getToolById(@PathVariable Long id) {
        return toolService.getToolById(id);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')") // Solo ADMIN puede crear
    public ToolEntity createTool(@RequestBody ToolEntity tool, Authentication authentication) {
        // Obteniendo el usuario real del JWT
        UserEntity currentUser = securityUtils.getUserFromAuthentication(authentication); // CAMBIO: Usar instancia
        return toolService.createTool(tool, currentUser);
    }

    @PutMapping("/{id}/decommission")
    @PreAuthorize("hasRole('ADMIN')") // Solo ADMIN puede dar de baja
    public ToolEntity decommissionTool(@PathVariable Long id, Authentication authentication) {
        // Obteniendo el usuario real del JWT
        UserEntity currentUser = securityUtils.getUserFromAuthentication(authentication); // CAMBIO: Usar instancia
        return toolService.decommissionTool(id, currentUser);
    }
}