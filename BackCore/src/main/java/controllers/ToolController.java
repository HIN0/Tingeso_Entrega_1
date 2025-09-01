package controllers;

// Entidad, Servicio
import entities.ToolEntity;
import services.ToolService;

import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/tools")
public class ToolController {

    private final ToolService toolService;

    public ToolController(ToolService toolService) {
        this.toolService = toolService;
    }

    @GetMapping
    public List<ToolEntity> getAllTools() {
        return toolService.getAllTools();
    }

    @GetMapping("/{id}")
    public Optional<ToolEntity> getToolById(@PathVariable Long id) {
        return toolService.getToolById(id);
    }

    @PostMapping
    public ToolEntity createTool(@RequestBody ToolEntity tool) {
        return toolService.createTool(tool);
    }

    @PutMapping("/{id}")
    public ToolEntity updateTool(@PathVariable Long id, @RequestBody ToolEntity tool) {
        return toolService.updateTool(id, tool);
    }

    @PutMapping("/{id}/decommission")
    public ToolEntity decommissionTool(@PathVariable Long id) {
        return toolService.decommissionTool(id);
    }

    @DeleteMapping("/{id}")
    public void deleteTool(@PathVariable Long id) {
        toolService.deleteTool(id);
    }
}
