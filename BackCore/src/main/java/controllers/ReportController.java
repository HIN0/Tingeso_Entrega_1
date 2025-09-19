package controllers;

import entities.ClientEntity;
import entities.LoanEntity;
import entities.ToolEntity;
import services.ReportService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/active-loans")
    public List<LoanEntity> getActiveLoans() {
        return reportService.getActiveLoans();
    }

    @GetMapping("/restricted-clients")
    public List<ClientEntity> getRestrictedClients() {
        return reportService.getRestrictedClients();
    }

    @GetMapping("/top-tools")
    public List<ToolEntity> getMostLoanedTools() {
        return reportService.getMostLoanedTools();
    }
}
