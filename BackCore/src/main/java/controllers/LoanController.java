package controllers;

import dtos.LoanRequest;
import dtos.ReturnLoanRequest;
import entities.ClientEntity;
import entities.LoanEntity;
import entities.UserEntity;
import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import services.LoanService;
import app.utils.SecurityUtils;

import java.util.List;

@RestController
@RequestMapping("/loans")
@CrossOrigin("*")
public class LoanController {

    private final LoanService loanService;
    private final SecurityUtils securityUtils; 

    public LoanController(LoanService loanService, SecurityUtils securityUtils) { 
        this.loanService = loanService;
        this.securityUtils = securityUtils;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public List<LoanEntity> getAllLoans() {
        List<LoanEntity> active = loanService.getActiveLoans();
        List<LoanEntity> late = loanService.getLateLoans();
        active.addAll(late);
        return active;
        }

    // --- ENDPOINT GET /loans/{id} ---
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')") // Proteger el endpoint
    public ResponseEntity<LoanEntity> getLoanById(@PathVariable Long id) {
        LoanEntity loan = loanService.getLoanById(id);
        return ResponseEntity.ok(loan);
    }

    @PostMapping(consumes = "application/json", produces = "application/json")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public LoanEntity createLoanJson(@RequestBody @Valid LoanRequest req, Authentication authentication) {
        UserEntity currentUser = securityUtils.getUserFromAuthentication(authentication);
        return loanService.createLoan(req.clientId(), req.toolId(), req.startDate(), req.dueDate(), currentUser);
    }

    @PutMapping(path = "/{id}/return", consumes = "application/json", produces = "application/json")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public LoanEntity returnLoanJson(@PathVariable Long id, @RequestBody @Valid ReturnLoanRequest req, Authentication authentication) {
        UserEntity currentUser = securityUtils.getUserFromAuthentication(authentication);
        return loanService.returnLoan(
            id,
            req.toolId(),
            req.damaged(),
            req.irreparable(),
            currentUser,
            req.returnDate()
            );
        }

    @PatchMapping("/{loanId}/pay")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<ClientEntity> markLoanAsPaid(@PathVariable Long loanId) {
        ClientEntity updatedClient = loanService.markLoanAsPaid(loanId);
        return ResponseEntity.ok(updatedClient);
    }

    @PostMapping(params = {"clientId","toolId","dueDate"})
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public LoanEntity createLoan(@RequestParam Long clientId,
                                @RequestParam Long toolId,
                                @RequestParam String dueDate,
                                Authentication authentication) {
        UserEntity currentUser = securityUtils.getUserFromAuthentication(authentication);
        return loanService.createLoan(clientId, toolId, java.time.LocalDate.parse(dueDate), currentUser);
    }

    @PutMapping(path = "/{id}/return", params = {"toolId","damaged","irreparable"})
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public LoanEntity returnLoan(@PathVariable Long id,
                                @RequestParam Long toolId,
                                @RequestParam boolean damaged,
                                @RequestParam boolean irreparable,
                                Authentication authentication) {
        UserEntity currentUser = securityUtils.getUserFromAuthentication(authentication);
        return loanService.returnLoan(id, toolId, damaged, irreparable, currentUser);
    }

}