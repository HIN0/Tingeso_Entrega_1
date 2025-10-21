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

    @Transactional
    public void registerMovement(ToolEntity tool, MovementType type, int quantity, UserEntity user) {
        // Validar si la herramienta existe antes de registrar (buena práctica)
        if (tool == null || tool.getId() == null || !toolRepository.existsById(tool.getId())) {
             throw new ResourceNotFoundException("Cannot register movement for non-existent tool.");
        }
        KardexEntity movement = KardexEntity.builder()
                .tool(tool)
                .type(type)
                .movementDate(LocalDateTime.now())
                .quantity(quantity)
                .user(user)
                .build();
        kardexRepository.save(movement);
    }

    @Transactional(readOnly = true)
    public List<KardexEntity> getMovementsByTool(ToolEntity tool) {
        if (tool == null || tool.getId() == null) {
             throw new IllegalArgumentException("Tool entity cannot be null.");
        }
        return kardexRepository.findByTool_Id(tool.getId());
    }

    @Transactional(readOnly = true)
    public List<KardexEntity> getMovementsByToolId(Long toolId) {
        // 1. Validar que el ID no sea nulo (aunque PathVariable usualmente lo previene)
        if (toolId == null) {
            throw new IllegalArgumentException("Tool ID cannot be null.");
        }
        // 2. Verificar si la herramienta existe (opcional pero bueno para devolver 404 claro)
        if (!toolRepository.existsById(toolId)) {
            throw new ResourceNotFoundException("Tool not found with id: " + toolId);
        }
        // 3. Buscar los movimientos por el ID de la herramienta
        return kardexRepository.findByTool_Id(toolId);
    }


    @Transactional(readOnly = true)
    public List<KardexEntity> getMovementsByDate(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) {
            throw new IllegalArgumentException("Start and end dates cannot be null.");
        }
        if (end.isBefore(start)) {
             throw new IllegalArgumentException("End date cannot be before start date.");
        }
        return kardexRepository.findByMovementDateBetween(start, end);
    }
}