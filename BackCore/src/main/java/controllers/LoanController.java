package controllers;

import dtos.LoanRequest;
import dtos.ReturnLoanRequest;
import entities.LoanEntity;
import entities.UserEntity; // Asegúrate de tener esta importación
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import services.LoanService;
import app.utils.SecurityUtils;

import java.util.List;

@RestController
@RequestMapping("/loans")
@CrossOrigin("*") // Usando el comodín para resolver CORS
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
        return loanService.getActiveLoans();
    }

    // ==== versión JSON (para frontend) ====
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