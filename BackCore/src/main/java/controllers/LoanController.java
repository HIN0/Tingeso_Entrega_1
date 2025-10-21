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

    // Constructor con inyección de SecurityUtils (para evitar errores al inicio)
    public LoanController(LoanService loanService, SecurityUtils securityUtils) { 
        this.loanService = loanService;
        this.securityUtils = securityUtils;
    }

    // **********************************************
    // MÉTODO AÑADIDO: GET para la ruta base /loans
    // **********************************************
    @GetMapping // Mapea a GET /loans
        @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
        public List<LoanEntity> getAllLoans() {
            // Por ahora, devolvemos activos + atrasados para que se vean en la lista principal.
            List<LoanEntity> active = loanService.getActiveLoans();
            List<LoanEntity> late = loanService.getLateLoans();
            active.addAll(late);
            return active;
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

    // --- ENDPOINT PARA MARCAR COMO PAGADO ---
    @PatchMapping("/{loanId}/pay")
    // Permitir a Admin o Empleado registrar un pago
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<ClientEntity> markLoanAsPaid(@PathVariable Long loanId) {
        // La autenticación (quién lo hizo) no se usa aquí, pero podría añadirse para auditoría
        ClientEntity updatedClient = loanService.markLoanAsPaid(loanId);
        // Devolver el estado actualizado del cliente (puede seguir RESTRICTED o cambiar a ACTIVE)
        return ResponseEntity.ok(updatedClient);
        // Alternativa: Devolver solo un 200 OK sin cuerpo
        // loanService.markLoanAsPaid(loanId);
        // return ResponseEntity.ok().build();
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

    @GetMapping("/active") // Esta ruta ahora es redundante, pero se mantiene sin conflicto
    public List<LoanEntity> getActiveLoans() {
        return loanService.getActiveLoans();
    }
}