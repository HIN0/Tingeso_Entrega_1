package services;

import entities.ToolEntity;
import entities.UserEntity;
import entities.enums.MovementType;
import entities.enums.ToolStatus;
import entities.enums.UserRole;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import repositories.ToolRepository;

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

    @Transactional
    public ToolEntity createTool(ToolEntity tool, UserEntity user) {
        if (tool.getName() == null || tool.getCategory() == null || tool.getReplacementValue() == null) {
            throw new IllegalArgumentException("Tool must have name, category and replacement value");
        }
        if (tool.getStatus() == null) tool.setStatus(ToolStatus.AVAILABLE);
        if (tool.getStock() == null) tool.setStock(0);

        ToolEntity saved = toolRepository.save(tool);
        // ingreso de 1 unidad como semilla (si quieres registrar exactamente el stock, ajusta quantity)
        kardexService.registerMovement(saved, MovementType.INCOME, 1, user);
        return saved;
    }

    @Transactional
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

    // ===== Métodos de soporte para préstamos/devoluciones =====

    @Transactional
    public void decrementStockForLoan(ToolEntity tool, UserEntity user) {
        if (tool.getStatus() != ToolStatus.AVAILABLE || tool.getStock() == null || tool.getStock() <= 0) {
            throw new IllegalStateException("Tool is not available");
        }
        tool.setStock(tool.getStock() - 1);
        if (tool.getStock() == 0) {
            tool.setStatus(ToolStatus.LOANED);
        }
        toolRepository.save(tool);
        kardexService.registerMovement(tool, MovementType.LOAN, 1, user);
    }

    @Transactional
    public void incrementStockForReturn(ToolEntity tool, UserEntity user) {
        // Solo si vuelve a disponible (sin daño)
        tool.setStock((tool.getStock() == null ? 0 : tool.getStock()) + 1);
        tool.setStatus(ToolStatus.AVAILABLE);
        toolRepository.save(tool);
        kardexService.registerMovement(tool, MovementType.RETURN, 1, user);
    }

    @Transactional
    public void markAsRepairing(ToolEntity tool, UserEntity user) {
        tool.setStatus(ToolStatus.REPAIRING);
        toolRepository.save(tool);
        kardexService.registerMovement(tool, MovementType.REPAIR, 1, user);
    }

    @Transactional
    public void markAsDecommissioned(ToolEntity tool, UserEntity user) {
        tool.setStatus(ToolStatus.DECOMMISSIONED);
        tool.setStock(0);
        toolRepository.save(tool);
        kardexService.registerMovement(tool, MovementType.DECOMMISSION, 1, user);
    }
}
