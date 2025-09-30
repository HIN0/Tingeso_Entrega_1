package controllers;

import dtos.LoanRequest;
import dtos.ReturnLoanRequest;
import entities.LoanEntity;
import entities.UserEntity;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import services.LoanService;

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
    public LoanEntity createLoanJson(@RequestBody @Valid LoanRequest req) {
        var currentUser = UserEntity.builder().id(2L).username("employee").build(); 
        return loanService.createLoan(req.clientId(), req.toolId(), req.dueDate(), currentUser);
    }

    @PutMapping(path = "/{id}/return", consumes = "application/json", produces = "application/json")
    public LoanEntity returnLoanJson(@PathVariable Long id, @RequestBody @Valid ReturnLoanRequest req) {
        var currentUser = UserEntity.builder().id(2L).username("employee").build();
        return loanService.returnLoan(
            id,
            req.toolId(),
            req.damaged(),
            req.irreparable(),
            currentUser
        );
    }


    @PostMapping(params = {"clientId","toolId","dueDate"})
    public LoanEntity createLoan(@RequestParam Long clientId,
                                 @RequestParam Long toolId,
                                 @RequestParam String dueDate) {
        var currentUser = UserEntity.builder().id(2L).username("employee").build();
        return loanService.createLoan(clientId, toolId, /*start*/ java.time.LocalDate.now(),
                currentUser);
    }

    @PutMapping(path = "/{id}/return", params = {"toolId","damaged","irreparable"})
    public LoanEntity returnLoan(@PathVariable Long id,
                                 @RequestParam Long toolId,
                                 @RequestParam boolean damaged,
                                 @RequestParam boolean irreparable) {
        var currentUser = UserEntity.builder().id(2L).username("employee").build();
        return loanService.returnLoan(id, toolId, damaged, irreparable, currentUser);
    }

    @GetMapping("/active")
    public List<LoanEntity> getActiveLoans() {
        return loanService.getActiveLoans();
    }
}
