package services;

import dtos.UpdateToolRequest;
import entities.ToolEntity;
import entities.UserEntity;
import entities.enums.MovementType;
import entities.enums.ToolStatus;
import jakarta.validation.Valid;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import repositories.ToolRepository;

import java.util.List;

@Service
@Validated
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
                .orElseThrow(() -> new RuntimeException("Tool not found with id: " + id));
    }

    // ----- PARA CREAR HERRAMIENTAS -----
    @Transactional
    public ToolEntity createTool(@Valid ToolEntity tool, UserEntity user) {
        if (tool.getStock() == null || tool.getStock() < 0) {
            throw new IllegalArgumentException("Initial stock must be provided and cannot be negative.");
        }

        if (tool.getStatus() == null) {
            tool.setStatus(tool.getStock() > 0 ? ToolStatus.AVAILABLE : ToolStatus.AVAILABLE);

        } else if (tool.getStock() == 0 && tool.getStatus() == ToolStatus.AVAILABLE) {
        } else if (tool.getStock() > 0 && tool.getStatus() != ToolStatus.AVAILABLE) {
            tool.setStatus(ToolStatus.AVAILABLE);
        }

        ToolEntity saved = toolRepository.save(tool);

        // Registrar movimiento en Kardex con la cantidad inicial (si es mayor que 0)
        if (saved.getStock() > 0) {
            kardexService.registerMovement(saved, MovementType.INCOME, saved.getStock(), user);
        }
        return saved;
    }

    // --- UPDATE HERRAMIENTAS ---
    @Transactional
    public ToolEntity updateTool(Long id, UpdateToolRequest updateRequest, UserEntity user) {
        ToolEntity existingTool = getToolById(id);
        existingTool.setName(updateRequest.name());
        existingTool.setCategory(updateRequest.category());
        existingTool.setReplacementValue(updateRequest.replacementValue());

        return toolRepository.save(existingTool);
    }

    // --- DAR DE BAJA HERRAMIENTA ---
    @Transactional
    public ToolEntity decommissionTool(Long id, UserEntity user) {
        ToolEntity tool = getToolById(id);

        if (tool.getStatus() == ToolStatus.DECOMMISSIONED) {
             throw new IllegalStateException("Tool is already decommissioned.");
        }

        if (tool.getStatus() == ToolStatus.LOANED || tool.getStatus() == ToolStatus.REPAIRING) {
            throw new IllegalStateException("Cannot decommission a tool while loaned or under repair.");
        }

        int quantityToDecommission = tool.getStock() > 0 ? tool.getStock() : 1; // Registrar baja de las unidades existentes o al menos 1 conceptualmente
        tool.setStatus(ToolStatus.DECOMMISSIONED);
        tool.setStock(0);
        ToolEntity saved = toolRepository.save(tool);

        // Registrar la baja en el Kardex
        kardexService.registerMovement(saved, MovementType.DECOMMISSION, quantityToDecommission, user);
        return saved;
    }

    // ===== Métodos de soporte para préstamos/devoluciones =====

    @Transactional
    public void decrementStockForLoan(ToolEntity tool, UserEntity user) {
        if (tool.getStatus() != ToolStatus.AVAILABLE || tool.getStock() == null || tool.getStock() <= 0) {
            throw new IllegalStateException("Tool is not available or out of stock.");
        }
        tool.setStock(tool.getStock() - 1);
        if (tool.getStock() == 0) {
            tool.setStatus(ToolStatus.LOANED);
        }
        toolRepository.save(tool);
        kardexService.registerMovement(tool, MovementType.LOAN, 1, user);
    }

    @Transactional // por mejorar 
    public void incrementStockForReturn(ToolEntity tool, UserEntity user) {
        // Al devolver, el stock siempre aumenta en 1.
        int newStock = (tool.getStock() == null ? 0 : tool.getStock()) + 1;
        tool.setStock(newStock);
        // Si el stock era 0 (estado LOANED) y ahora es > 0, vuelve a AVAILABLE.
        // Si ya era AVAILABLE (había otras unidades), sigue AVAILABLE.
        if (newStock > 0) {
             tool.setStatus(ToolStatus.AVAILABLE);
        }
        toolRepository.save(tool);
        kardexService.registerMovement(tool, MovementType.RETURN, 1, user);
    }

    @Transactional
    public void markAsRepairing(ToolEntity tool, UserEntity user) {
        tool.setStatus(ToolStatus.REPAIRING);
        int quantityInRepair = 1; // Asumimos que se repara 1 unidad
        tool.setStock(tool.getStock() - quantityInRepair); // Quitar del stock disponible
        tool.setStock(Math.max(0, tool.getStock() - 1)); // Asegura no negativo

        toolRepository.save(tool);
        kardexService.registerMovement(tool, MovementType.REPAIR, 1, user); // Registra que 1 unidad entró a reparación
    }

    
    @Transactional
    public void markAsDecommissioned(ToolEntity tool, UserEntity user) {
        int quantityDecommissioned = tool.getStock() > 0 ? tool.getStock() : 1;
        tool.setStatus(ToolStatus.DECOMMISSIONED);
        tool.setStock(0);
        toolRepository.save(tool);
        kardexService.registerMovement(tool, MovementType.DECOMMISSION, quantityDecommissioned, user);  // Registra la baja en kardex
    }

    // --- MÉTODO PARA AJUSTE MANUAL DE STOCK ---
    @Transactional
    public ToolEntity adjustStock(Long id, int quantityChange, MovementType movementType, UserEntity user) {
        if (quantityChange == 0) {
            throw new IllegalArgumentException("Quantity change cannot be zero.");
        }

        if (movementType != MovementType.INCOME && movementType != MovementType.MANUAL_DECREASE) {
            throw new IllegalArgumentException("Invalid movement type for manual stock adjustment.");
        }

        ToolEntity tool = getToolById(id);
        if (tool.getStatus() == ToolStatus.DECOMMISSIONED) {
            throw new IllegalStateException("Cannot adjust stock for a decommissioned tool.");
        }

        int newStock = tool.getStock() + quantityChange;
        if (newStock < 0) {
            throw new IllegalArgumentException("Stock adjustment would result in negative stock.");
        }

        tool.setStock(newStock);

        // Actualizar estado basado en el nuevo stock
        if (newStock > 0 && tool.getStatus() != ToolStatus.REPAIRING && tool.getStatus() != ToolStatus.DECOMMISSIONED) {
            // Si hay stock y no está en reparación/baja -> Disponible
            tool.setStatus(ToolStatus.AVAILABLE);
        } else if (newStock == 0 && tool.getStatus() == ToolStatus.AVAILABLE) {
            // Si el ajuste manual deja stock en 0 y estaba Disponible -> Cambiar a LOANED (como si se hubiera prestado la última)
            tool.setStatus(ToolStatus.LOANED);
        }

        ToolEntity saved = toolRepository.save(tool);
        kardexService.registerMovement(saved, movementType, Math.abs(quantityChange), user); // Registrar cantidad absoluta

        return saved;
    }
}