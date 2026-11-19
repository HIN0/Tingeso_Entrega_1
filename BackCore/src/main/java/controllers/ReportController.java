package controllers;

import entities.ClientEntity;
import entities.LoanEntity;
import org.springframework.format.annotation.DateTimeFormat;
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

    // --- RF6.1: loans by status, optional date range ---
    @GetMapping("/loans")
    public List<LoanEntity> getLoansByStatus(
            @RequestParam String status,
            // required = false makes the dates optional
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        // dates may be null (no filter)
        return reportService.getLoansByStatus(status.toUpperCase(), from, to);
    }

    // --- RF6.2: clients with late loans, optional date range ---
    @GetMapping("/clients/late")
    public List<ClientEntity> getLateClients(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        // dates may be null (no filter)
        return reportService.getClientsWithLateLoans(from, to);
    }

    // --- RF6.3: most loaned tools (ranking) ---
    @GetMapping("/tools/top")
    public List<Object[]> getTopTools(
            // date range is mandatory for the ranking
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return reportService.getTopTools(from, to);
    }
}