package services;

// Entidades
import entities.KardexEntity;
import entities.ToolEntity;

import repositories.KardexRepository;
import repositories.ToolRepository;
import org.springframework.stereotype.Service;

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

    // Consultar historial por herramienta
    public List<KardexEntity> getMovementsByTool(Long toolId) {
        ToolEntity tool = toolRepository.findById(toolId)
                .orElseThrow(() -> new IllegalArgumentException("Tool not found"));
        return kardexRepository.findByTool(tool);
    }

    // Consultar movimientos por rango de fechas
    public List<KardexEntity> getMovementsByDateRange(LocalDateTime start, LocalDateTime end) {
        if (start.isAfter(end)) {
            throw new IllegalArgumentException("Start date must be before end date");
        }
        return kardexRepository.findByDateBetween(start, end);
    }

    // Listar todos los movimientos
    public List<KardexEntity> getAllMovements() {
        return kardexRepository.findAll();
    }
}
