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
import app.exceptions.ResourceNotFoundException;
import app.exceptions.InvalidOperationException;

import java.util.List;

/**
 * Inventory (Epic 1). A tool is a type with unit counters:
 *   stock    = units available to lend
 *   loaned   = units in the hands of clients
 *   inRepair = damaged units waiting for evaluation
 * The status is never set by hand: it is derived from the counters in refreshStatus(),
 * except DECOMMISSIONED, which is a final state set by an administrator.
 */
@Service
@Validated
public class ToolService {

    private final ToolRepository toolRepository;
    private final KardexService kardexService;

    public ToolService(ToolRepository toolRepository, KardexService kardexService) {
        this.toolRepository = toolRepository;
        this.kardexService = kardexService;
    }

    // --- QUERIES ---

    public List<ToolEntity> getAllTools() {
        return toolRepository.findAll();
    }

    public ToolEntity getToolById(Long id) {
        return toolRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tool not found with id: " + id));
    }

    // --- STATUS RULE ---

    /**
     * Single place where the status is decided:
     *   stock > 0              -> AVAILABLE  (it can be lent)
     *   no stock, some loaned  -> LOANED
     *   no stock, some repair  -> REPAIRING
     *   no units anywhere      -> AVAILABLE  (registered, waiting for an income of stock)
     */
    static void refreshStatus(ToolEntity tool) {
        if (tool.getStatus() == ToolStatus.DECOMMISSIONED) {
            return;
        }
        if (count(tool.getStock()) > 0) {
            tool.setStatus(ToolStatus.AVAILABLE);
        } else if (count(tool.getLoaned()) > 0) {
            tool.setStatus(ToolStatus.LOANED);
        } else if (count(tool.getInRepair()) > 0) {
            tool.setStatus(ToolStatus.REPAIRING);
        } else {
            tool.setStatus(ToolStatus.AVAILABLE);
        }
    }

    // Rows created before a counter existed may hold null: treat it as 0
    private static int count(Integer value) {
        return value == null ? 0 : value;
    }

    // --- INVENTORY MANAGEMENT (ADMIN) ---

    @Transactional
    public ToolEntity createTool(@Valid ToolEntity tool, UserEntity user) {
        // A new tool has no units loaned or in repair; its status follows from the initial stock
        tool.setInRepair(0);
        tool.setLoaned(0);
        tool.setStatus(null);
        refreshStatus(tool);

        ToolEntity saved = toolRepository.save(tool);
        // Business rule: registering a tool generates an INCOME movement in the Kardex
        if (saved.getStock() > 0) {
            kardexService.registerMovement(saved, MovementType.INCOME, saved.getStock(), user);
        }
        return saved;
    }

    @Transactional
    public ToolEntity updateTool(Long id, UpdateToolRequest updateRequest, UserEntity user) {
        ToolEntity existingTool = getToolById(id);

        if (existingTool.getReplacementValue() < 1000) {
            throw new InvalidOperationException("Cannot update a tool with replacement value less than $1000.");
        }

        existingTool.setName(updateRequest.name());
        existingTool.setCategory(updateRequest.category());
        existingTool.setReplacementValue(updateRequest.replacementValue());
        return toolRepository.save(existingTool);
    }

    // RF1.2: the administrator decommissions the whole tool (all available units)
    @Transactional
    public ToolEntity decommissionTool(Long id, UserEntity user) {
        ToolEntity tool = getToolById(id);

        if (tool.getStatus() == ToolStatus.DECOMMISSIONED) {
            throw new InvalidOperationException("Tool is already decommissioned.");
        }
        if (count(tool.getLoaned()) > 0) {
            throw new InvalidOperationException("Cannot decommission a tool while units are loaned.");
        }
        if (count(tool.getInRepair()) > 0) {
            throw new InvalidOperationException("Cannot decommission a tool while units are under repair. Complete or write off the repairs first.");
        }

        int quantityToDecommission = count(tool.getStock());
        tool.setStatus(ToolStatus.DECOMMISSIONED);
        tool.setStock(0);
        ToolEntity saved = toolRepository.save(tool);

        kardexService.registerMovement(saved, MovementType.DECOMMISSION, quantityToDecommission, user);
        return saved;
    }

    @Transactional
    public ToolEntity adjustStock(Long id, int quantityChange, UserEntity user) {
        if (quantityChange == 0) {
            throw new InvalidOperationException("Quantity change cannot be zero.");
        }
        // Business rule: the sign of the change defines the Kardex movement type
        MovementType movementType = quantityChange > 0 ? MovementType.INCOME : MovementType.MANUAL_DECREASE;

        ToolEntity tool = getToolById(id);
        if (tool.getStatus() == ToolStatus.DECOMMISSIONED) {
            throw new InvalidOperationException("Cannot adjust stock for a decommissioned tool.");
        }

        int newStock = count(tool.getStock()) + quantityChange;
        if (newStock < 0) {
            throw new InvalidOperationException("Stock adjustment would result in negative stock.");
        }

        tool.setStock(newStock);
        refreshStatus(tool);

        ToolEntity saved = toolRepository.save(tool);
        kardexService.registerMovement(saved, movementType, Math.abs(quantityChange), user);
        return saved;
    }

    // --- REPAIR FLOW (ADMIN): evaluation of a damaged unit ---

    // The unit was repaired: it leaves repair and goes back to the available stock
    @Transactional
    public ToolEntity completeRepair(Long id, UserEntity user) {
        ToolEntity tool = getToolUnderRepair(id);
        tool.setInRepair(tool.getInRepair() - 1);
        tool.setStock(count(tool.getStock()) + 1);
        refreshStatus(tool);

        ToolEntity saved = toolRepository.save(tool);
        kardexService.registerMovement(saved, MovementType.REPAIRED, 1, user);
        return saved;
    }

    // The damage turned out to be irreparable: the unit is permanently removed
    @Transactional
    public ToolEntity writeOffRepair(Long id, UserEntity user) {
        ToolEntity tool = getToolUnderRepair(id);
        tool.setInRepair(tool.getInRepair() - 1);
        refreshStatus(tool);

        ToolEntity saved = toolRepository.save(tool);
        kardexService.registerMovement(saved, MovementType.DECOMMISSION, 1, user);
        return saved;
    }

    private ToolEntity getToolUnderRepair(Long id) {
        ToolEntity tool = getToolById(id);
        if (count(tool.getInRepair()) == 0) {
            throw new InvalidOperationException("Tool has no units under repair.");
        }
        return tool;
    }

    // --- SUPPORT FOR LOANS / RETURNS (called by LoanService) ---

    @Transactional
    public void decrementStockForLoan(ToolEntity tool, UserEntity user) {
        tool.setStock(count(tool.getStock()) - 1);
        tool.setLoaned(count(tool.getLoaned()) + 1);
        refreshStatus(tool);
        toolRepository.save(tool);
        kardexService.registerMovement(tool, MovementType.LOAN, 1, user);
    }

    // Returned in good condition: back to the available stock
    @Transactional
    public void incrementStockForReturn(ToolEntity tool, UserEntity user) {
        tool.setLoaned(Math.max(0, count(tool.getLoaned()) - 1));
        tool.setStock(count(tool.getStock()) + 1);
        refreshStatus(tool);
        toolRepository.save(tool);
        kardexService.registerMovement(tool, MovementType.RETURN, 1, user);
    }

    // Returned with light damage: the unit waits for evaluation (completeRepair / writeOffRepair)
    @Transactional
    public void markAsRepairing(ToolEntity tool, UserEntity user) {
        if (tool.getStatus() == ToolStatus.DECOMMISSIONED) {
            throw new InvalidOperationException("Cannot mark a decommissioned tool as repairing.");
        }
        tool.setLoaned(Math.max(0, count(tool.getLoaned()) - 1));
        tool.setInRepair(count(tool.getInRepair()) + 1);
        refreshStatus(tool);
        toolRepository.save(tool);
        kardexService.registerMovement(tool, MovementType.REPAIR, 1, user);
    }

    // Returned with irreparable damage: the unit is removed (the client pays the replacement value)
    @Transactional
    public void markAsDecommissioned(ToolEntity tool, UserEntity user) {
        if (tool.getStatus() == ToolStatus.DECOMMISSIONED) {
            throw new InvalidOperationException("Tool is already decommissioned.");
        }
        tool.setLoaned(Math.max(0, count(tool.getLoaned()) - 1));
        refreshStatus(tool);
        toolRepository.save(tool);
        kardexService.registerMovement(tool, MovementType.DECOMMISSION, 1, user);
    }
}
