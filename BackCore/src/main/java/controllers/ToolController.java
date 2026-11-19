package controllers;

import dtos.CreateToolRequest;
import dtos.StockAdjustmentRequest;
import dtos.UpdateToolRequest;
import entities.ToolEntity;
import entities.UserEntity;
import jakarta.validation.Valid;
import services.ToolService;
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
    public ToolEntity createTool(@Valid @RequestBody CreateToolRequest request, Authentication authentication) {
        UserEntity currentUser = securityUtils.getUserFromAuthentication(authentication);
        return toolService.createTool(request.toEntity(), currentUser);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ToolEntity updateTool(@PathVariable Long id, @Valid @RequestBody UpdateToolRequest updateRequest, Authentication authentication) {
        UserEntity currentUser = securityUtils.getUserFromAuthentication(authentication);
        return toolService.updateTool(id, updateRequest, currentUser);
    }

    @PatchMapping("/{id}/stock")
    @PreAuthorize("hasRole('ADMIN')")
    public ToolEntity adjustStock(@PathVariable Long id, @Valid @RequestBody StockAdjustmentRequest request, Authentication authentication) {
        UserEntity currentUser = securityUtils.getUserFromAuthentication(authentication);
        return toolService.adjustStock(id, request.quantityChange(), currentUser);
    }

    // Repair evaluation of one damaged unit: repaired -> back to stock
    @PatchMapping("/{id}/repair/complete")
    @PreAuthorize("hasRole('ADMIN')")
    public ToolEntity completeRepair(@PathVariable Long id, Authentication authentication) {
        UserEntity currentUser = securityUtils.getUserFromAuthentication(authentication);
        return toolService.completeRepair(id, currentUser);
    }

    // Repair evaluation of one damaged unit: irreparable -> removed permanently
    @PatchMapping("/{id}/repair/write-off")
    @PreAuthorize("hasRole('ADMIN')")
    public ToolEntity writeOffRepair(@PathVariable Long id, Authentication authentication) {
        UserEntity currentUser = securityUtils.getUserFromAuthentication(authentication);
        return toolService.writeOffRepair(id, currentUser);
    }

    @PutMapping("/{id}/decommission")
    @PreAuthorize("hasRole('ADMIN')")
    public ToolEntity decommissionTool(@PathVariable Long id, Authentication authentication) {
        UserEntity currentUser = securityUtils.getUserFromAuthentication(authentication);
        return toolService.decommissionTool(id, currentUser);
    }
}