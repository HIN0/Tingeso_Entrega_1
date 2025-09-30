package controllers;

import entities.ClientEntity;
import entities.LoanEntity;
import org.springframework.web.bind.annotation.*;
import services.ReportService;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    // RF6.1: préstamos por estado (ACTIVE o LATE)
    @GetMapping("/loans")
    public List<LoanEntity> getLoansByStatus(@RequestParam String status) {
        return reportService.getLoansByStatus(status.toUpperCase());
    }

    // RF6.2: clientes con préstamos atrasados
    @GetMapping("/clients/late")
    public List<ClientEntity> getLateClients() {
        return reportService.getClientsWithLateLoans();
    }

    // RF6.3: ranking de herramientas más prestadas
    @GetMapping("/tools/top")
    public List<Object[]> getTopTools(
            @RequestParam("from") LocalDate from,
            @RequestParam("to") LocalDate to) {
        return reportService.getTopTools(from, to);
    }
}
