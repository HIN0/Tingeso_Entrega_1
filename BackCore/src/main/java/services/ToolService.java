package services;

import entities.ToolEntity;
import entities.UserEntity;
import entities.enums.MovementType;
import entities.enums.ToolStatus;
import entities.enums.UserRole;
import repositories.ToolRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ToolService {

    private final ToolRepository toolRepository;
    private final KardexService kardexService;

    public ToolService(ToolRepository toolRepository, KardexService kardexService) {
        this.toolRepository = toolRepository;
        this.kardexService = kardexService;
    }

    public List<ToolEntity> getAllTools() {
        return toolRepository.findAll();
    }

    public ToolEntity getToolById(Long id) {
        return toolRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tool not found"));
    }

    public ToolEntity createTool(ToolEntity tool, UserEntity user) {
        if (tool.getName() == null || tool.getCategory() == null || tool.getReplacementValue() == null) {
            throw new IllegalArgumentException("Tool must have name, category and replacement value");
        }
        tool.setStatus(ToolStatus.AVAILABLE);
        ToolEntity saved = toolRepository.save(tool);

        kardexService.registerMovement(saved, MovementType.INCOME, 1, user);
        return saved;
    }

    public ToolEntity decommissionTool(Long id, UserEntity user) {
        ToolEntity tool = getToolById(id);

        if (user.getRole() != UserRole.ADMIN) {
            throw new SecurityException("Only admins can decommission tools");
        }

        if (tool.getStatus() == ToolStatus.LOANED || tool.getStatus() == ToolStatus.REPAIRING) {
            throw new IllegalStateException("Cannot decommission a tool while loaned or under repair");
        }

        tool.setStatus(ToolStatus.DECOMMISSIONED);
        tool.setStock(0);
        ToolEntity saved = toolRepository.save(tool);

        kardexService.registerMovement(saved, MovementType.DECOMMISSION, 1, user);
        return saved;
    }
}
