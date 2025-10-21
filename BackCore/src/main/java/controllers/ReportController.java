package controllers;

// ... (imports existentes)
import entities.ClientEntity;
import entities.LoanEntity;
import org.springframework.format.annotation.DateTimeFormat; // Importar para formatear fechas
import org.springframework.web.bind.annotation.*;
import services.ReportService;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/reports")
@CrossOrigin("*")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    // --- RF6.1: Modificado para aceptar fechas opcionales ---
    @GetMapping("/loans")
    public List<LoanEntity> getLoansByStatus(
            @RequestParam String status,
            // Usar required = false para que las fechas sean opcionales
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        // Pasar las fechas (que pueden ser null) al servicio
        return reportService.getLoansByStatus(status.toUpperCase(), from, to);
    }

    // --- RF6.2: Modificado para aceptar fechas opcionales ---
    @GetMapping("/clients/late")
    public List<ClientEntity> getLateClients(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        // Pasar las fechas (que pueden ser null) al servicio
        return reportService.getClientsWithLateLoans(from, to);
    }

    // --- RF6.3: Sin cambios, ya requería fechas ---
    @GetMapping("/tools/top")
    public List<Object[]> getTopTools(
            // Mantener required = true o quitarlo si @RequestParam es obligatorio por defecto
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return reportService.getTopTools(from, to);
    }
}