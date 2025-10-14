package controllers;

import dtos.LoanRequest;
import dtos.ReturnLoanRequest;
import entities.LoanEntity;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import services.LoanService;
import app.utils.SecurityUtils;

import java.util.List;

@CrossOrigin(origins = "http://localhost:5173")
@RestController
@RequestMapping("/loans")
public class LoanController {

    private final LoanService loanService;

    public LoanController(LoanService loanService) {
        this.loanService = loanService;
    }

    // ==== versión JSON (para frontend) ====
    @PostMapping(consumes = "application/json", produces = "application/json")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public LoanEntity createLoanJson(@RequestBody @Valid LoanRequest req, Authentication authentication) {
        // Obteniendo el usuario real del JWT
        var currentUser = SecurityUtils.getUserFromAuthentication(authentication); 
        return loanService.createLoan(req.clientId(), req.toolId(), req.startDate(), req.dueDate(), currentUser);
    }

    @PutMapping(path = "/{id}/return", consumes = "application/json", produces = "application/json")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public LoanEntity returnLoanJson(@PathVariable Long id, @RequestBody @Valid ReturnLoanRequest req, Authentication authentication) {
        // Obteniendo el usuario real del JWT
        var currentUser = SecurityUtils.getUserFromAuthentication(authentication);
        return loanService.returnLoan(
            id,
            req.toolId(),
            req.damaged(),
            req.irreparable(),
            currentUser,
            req.returnDate()
        );
    }


    @PostMapping(params = {"clientId","toolId","dueDate"})
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public LoanEntity createLoan(@RequestParam Long clientId,
                                 @RequestParam Long toolId,
                                 @RequestParam String dueDate,
                                 Authentication authentication) {
        // Obteniendo el usuario real del JWT
        var currentUser = SecurityUtils.getUserFromAuthentication(authentication);
        return loanService.createLoan(clientId, toolId, java.time.LocalDate.parse(dueDate), currentUser);
    }

    @PutMapping(path = "/{id}/return", params = {"toolId","damaged","irreparable"})
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public LoanEntity returnLoan(@PathVariable Long id,
                                 @RequestParam Long toolId,
                                 @RequestParam boolean damaged,
                                 @RequestParam boolean irreparable,
                                 Authentication authentication) {
        // Obteniendo el usuario real del JWT
        var currentUser = SecurityUtils.getUserFromAuthentication(authentication);
        return loanService.returnLoan(id, toolId, damaged, irreparable, currentUser);
    }

    @GetMapping("/active")
    public List<LoanEntity> getActiveLoans() {
        return loanService.getActiveLoans();
    }
}