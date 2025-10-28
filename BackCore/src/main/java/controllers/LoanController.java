package controllers;

import dtos.LoanRequest;
import dtos.ReturnLoanRequest;
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

    // --- ENDPOINT PARA PAGAR (PATCH /loans/{loanId}/pay) ---
    @PatchMapping("/{loanId}/pay")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')") // O solo ADMIN si prefieres
    public ResponseEntity<LoanEntity> markLoanAsPaid(@PathVariable Long loanId) {
        // Llama al método modificado que ahora devuelve LoanEntity
        LoanEntity updatedLoan = loanService.markLoanAsPaid(loanId);
        // Devuelve el préstamo actualizado (con estado CLOSED y penalty 0)
        return ResponseEntity.ok(updatedLoan);
    }

    // --- NUEVO ENDPOINT: Obtener préstamos pendientes de pago por ID de cliente ---
    @GetMapping("/client/{clientId}/unpaid")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')") // Permitir a ambos roles ver las deudas
    public ResponseEntity<List<LoanEntity>> getUnpaidLoansForClient(@PathVariable Long clientId) {
        List<LoanEntity> unpaidLoans = loanService.getUnpaidReceivedLoansByClientId(clientId);
        return ResponseEntity.ok(unpaidLoans);
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