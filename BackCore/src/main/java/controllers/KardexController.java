package controllers;

// Entidad, Servicios
import entities.KardexEntity;
import services.KardexService;

import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/kardex")
public class KardexController {

    private final KardexService kardexService;

    public KardexController(KardexService kardexService) {
        this.kardexService = kardexService;
    }

    // Listar todos los movimientos
    @GetMapping
    public List<KardexEntity> getAllMovements() {
        return kardexService.getAllMovements();
    }

    // Movimientos de una herramienta
    @GetMapping("/tool/{toolId}")
    public List<KardexEntity> getMovementsByTool(@PathVariable Long toolId) {
        return kardexService.getMovementsByTool(toolId);
    }

    // Movimientos por rango de fechas (ej: ?start=2025-08-01T00:00:00&end=2025-08-29T23:59:59)
    @GetMapping("/date-range")
    public List<KardexEntity> getMovementsByDateRange(@RequestParam String start,
                                                      @RequestParam String end) {
        LocalDateTime startDate = LocalDateTime.parse(start);
        LocalDateTime endDate = LocalDateTime.parse(end);
        return kardexService.getMovementsByDateRange(startDate, endDate);
    }
}
