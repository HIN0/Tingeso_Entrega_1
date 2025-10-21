package controllers;

import dtos.StockAdjustmentRequest;
import dtos.UpdateToolRequest;
import entities.ToolEntity;
import entities.UserEntity;
import entities.enums.MovementType; 
import jakarta.validation.Valid; 
import services.ToolService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import app.utils.SecurityUtils;

import java.util.List;

@RestController
@RequestMapping("/tools")
@CrossOrigin("*")
public class ToolController {

    private final ToolService toolService;
    private final SecurityUtils securityUtils;

    public ToolController(ToolService toolService, SecurityUtils securityUtils) {
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
    @PreAuthorize("hasRole('ADMIN')")
    public ToolEntity createTool(@Valid @RequestBody ToolEntity tool, Authentication authentication) {
        UserEntity currentUser = securityUtils.getUserFromAuthentication(authentication);
        return toolService.createTool(tool, currentUser);
    }

// --- ENDPOINT EDITAR ---
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    // Cambiar @RequestBody ToolEntity por UpdateToolRequest
    public ToolEntity updateTool(@PathVariable Long id, @Valid @RequestBody UpdateToolRequest updateRequest, Authentication authentication) {
        UserEntity currentUser = securityUtils.getUserFromAuthentication(authentication);
        // Pasamos el DTO al servicio
        return toolService.updateTool(id, updateRequest, currentUser);
    }

    // --- ENDPOINT PARA AJUSTAR STOCK ---
    @PatchMapping("/{id}/stock")
    @PreAuthorize("hasRole('ADMIN')")
    public ToolEntity adjustStock(@PathVariable Long id, @Valid @RequestBody StockAdjustmentRequest request, Authentication authentication) {
        UserEntity currentUser = securityUtils.getUserFromAuthentication(authentication);
        MovementType type = request.quantityChange() > 0 ? MovementType.INCOME : MovementType.RETURN; 
        if (request.quantityChange() < 0) {
            type = MovementType.MANUAL_DECREASE;
            }
        return toolService.adjustStock(id, request.quantityChange(), type, currentUser);
    }


    @PutMapping("/{id}/decommission")
    @PreAuthorize("hasRole('ADMIN')")
    public ToolEntity decommissionTool(@PathVariable Long id, Authentication authentication) {
        UserEntity currentUser = securityUtils.getUserFromAuthentication(authentication);
        return toolService.decommissionTool(id, currentUser);
    }
}