package controllers;

// Entidades, Servicios
import entities.ClientEntity;
import entities.LoanEntity;
import entities.ToolEntity;
import services.ReportService;

import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    // RF6.1 - Préstamos activos (vigentes y atrasados)
    @GetMapping("/loans/active")
    public List<LoanEntity> getActiveLoans(@RequestParam(required = false) String start,
                                           @RequestParam(required = false) String end) {
        LocalDate startDate = (start != null) ? LocalDate.parse(start) : null;
        LocalDate endDate = (end != null) ? LocalDate.parse(end) : null;
        return reportService.getActiveLoans(startDate, endDate);
    }

    // RF6.2 - Clientes con atrasos
    @GetMapping("/clients/overdue")
    public List<ClientEntity> getClientsWithOverdueLoans(@RequestParam(required = false) String start,
                                                         @RequestParam(required = false) String end) {
        LocalDate startDate = (start != null) ? LocalDate.parse(start) : null;
        LocalDate endDate = (end != null) ? LocalDate.parse(end) : null;
        return reportService.getClientsWithOverdueLoans(startDate, endDate);
    }

    // RF6.3 - Ranking de herramientas más prestadas
    @GetMapping("/tools/ranking")
    public List<Map.Entry<ToolEntity, Long>> getMostBorrowedTools(@RequestParam(required = false) String start,
                                                                  @RequestParam(required = false) String end) {
        LocalDate startDate = (start != null) ? LocalDate.parse(start) : null;
        LocalDate endDate = (end != null) ? LocalDate.parse(end) : null;
        return reportService.getMostBorrowedTools(startDate, endDate);
    }
}
