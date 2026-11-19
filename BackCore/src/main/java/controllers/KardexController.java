package controllers;

import entities.KardexEntity;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import services.KardexService;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/kardex")
@CrossOrigin("*")
public class KardexController {

    private final KardexService kardexService;

    public KardexController(KardexService kardexService) {
        this.kardexService = kardexService;
    }

    // RF5.2: movement history of a tool (404 handled by GlobalExceptionHandler)
    @GetMapping("/tool/{toolId}")
    public List<KardexEntity> getMovementsByToolId(@PathVariable Long toolId) {
        return kardexService.getMovementsByToolId(toolId);
    }

    // RF5.3: movements in a date range, ISO format: 2025-01-31T00:00:00
    @GetMapping("/date")
    public List<KardexEntity> getMovementsByDate(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        return kardexService.getMovementsByDate(start, end);
    }
}
