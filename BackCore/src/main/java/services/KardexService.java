package services;

import entities.KardexEntity;
import entities.ToolEntity;
import entities.UserEntity;
import entities.enums.MovementType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import repositories.KardexRepository;
import repositories.ToolRepository; 
import app.exceptions.ResourceNotFoundException;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class KardexService {

    private final KardexRepository kardexRepository;
    private final ToolRepository toolRepository;

    public KardexService(KardexRepository kardexRepository, ToolRepository toolRepository) {
        this.kardexRepository = kardexRepository;
        this.toolRepository = toolRepository;
    }

    // ---------------------------------------------------------------------------------------------------------------------
    @Transactional
    public void registerMovement(ToolEntity tool, MovementType type, int quantity, UserEntity user) {
        // 1. The tool must exist
        if (tool == null || tool.getId() == null || !toolRepository.existsById(tool.getId())) {
            throw new ResourceNotFoundException("Cannot register movement for non-existent tool.");
        }
        // 2. Create and save the Kardex movement
        KardexEntity movement = KardexEntity.builder()
                .tool(tool)
                .type(type)
                .movementDate(LocalDateTime.now())
                .quantity(quantity)
                .user(user)
                .build();
        kardexRepository.save(movement);
    }

    // ---------------------------------------------------------------------------------------------------------------------
    @Transactional(readOnly = true)
    public List<KardexEntity> getMovementsByToolId(Long toolId) {
        // 1. The ID is required
        if (toolId == null) {
            throw new IllegalArgumentException("Tool ID cannot be null.");
        }
        // 2. The tool must exist
        if (!toolRepository.existsById(toolId)) {
            throw new ResourceNotFoundException("Tool not found with id: " + toolId);
        }
        // 3. Movements of that tool
        return kardexRepository.findByTool_Id(toolId);
    }

    // ---------------------------------------------------------------------------------------------------------------------
    @Transactional(readOnly = true)
    public List<KardexEntity> getMovementsByDate(LocalDateTime startDate, LocalDateTime endDate) {
        // 1. Both dates are required
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("Start and end dates cannot be null.");
        }
        // 2. The end date cannot be before the start date
        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("End date cannot be before start date.");
        }
        // 3. Movements in the date range
        return kardexRepository.findByMovementDateBetween(startDate, endDate);
    }
}