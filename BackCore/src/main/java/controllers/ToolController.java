package controllers;

import entities.ToolEntity;
import entities.UserEntity;
import services.ToolService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
    public ToolEntity getToolById(@PathVariable Long id) {
        return toolService.getToolById(id);
    }

    @PostMapping
    public ToolEntity createTool(@RequestBody ToolEntity tool) {
        // Simulamos un usuario admin hasta tener seguridad real
        UserEntity fakeUser = UserEntity.builder().id(1L).username("admin").build();
        return toolService.createTool(tool, fakeUser);
    }

    @PutMapping("/{id}/decommission")
    public ToolEntity decommissionTool(@PathVariable Long id) {
        // Simulamos un usuario admin hasta tener seguridad real
        UserEntity fakeUser = UserEntity.builder().id(1L).username("admin").build();
        return toolService.decommissionTool(id, fakeUser);
    }
}
