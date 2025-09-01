package services;

// Entidad y Repositorio
import entities.ToolEntity;
import entities.enums.ToolStatus;
import repositories.ToolRepository;

import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class ToolService {

    private final ToolRepository toolRepository;

    public ToolService(ToolRepository toolRepository) {
        this.toolRepository = toolRepository;
    }

    // Obtener todas las herramientas
    public List<ToolEntity> getAllTools() {
        return toolRepository.findAll();
    }

    // Obtener herramienta por ID
    public Optional<ToolEntity> getToolById(Long id) {
        return toolRepository.findById(id);
    }

    // Registrar nueva herramienta (con reglas de negocio)
    public ToolEntity createTool(ToolEntity tool) {
        if (tool.getName() == null || tool.getCategory() == null || tool.getReplacementValue() == null) {
            throw new IllegalArgumentException("Tool must have name, category and replacement value");
        }
        tool.setStatus(ToolStatus.AVAILABLE); // siempre arranca disponible
        if (tool.getStock() == null || tool.getStock() < 0) {
            tool.setStock(0);
        }
        return toolRepository.save(tool);
    }

    // Dar de baja herramienta (solo admin → se controla en capa seguridad, aquí lógica)
    public ToolEntity decommissionTool(Long id) {
        ToolEntity tool = toolRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Tool not found"));

        tool.setStatus(ToolStatus.DECOMMISSIONED);
        tool.setStock(0);
        return toolRepository.save(tool);
    }

    // Actualizar herramienta (ej: reparación, stock, etc.)
    public ToolEntity updateTool(Long id, ToolEntity updatedTool) {
        ToolEntity existing = toolRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Tool not found"));

        if (updatedTool.getCategory() != null) existing.setCategory(updatedTool.getCategory());
        if (updatedTool.getReplacementValue() != null) existing.setReplacementValue(updatedTool.getReplacementValue());
        if (updatedTool.getStock() != null && updatedTool.getStock() >= 0) existing.setStock(updatedTool.getStock());
        if (updatedTool.getStatus() != null) existing.setStatus(updatedTool.getStatus());

        return toolRepository.save(existing);
    }

    // Eliminar herramienta completamente (no recomendado en negocio, pero como opción)
    public void deleteTool(Long id) {
        if (!toolRepository.existsById(id)) {
            throw new IllegalArgumentException("Tool not found");
        }
        toolRepository.deleteById(id);
    }
}
