package controllers;

// Entidad, Servicios
import entities.LoanEntity;
import entities.UserEntity;
import services.LoanService;

import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/loans")
public class LoanController {

    private final LoanService loanService;

    public LoanController(LoanService loanService) {
        this.loanService = loanService;
    }

    @GetMapping
    public List<LoanEntity> getAllLoans() {
        return loanService.getAllLoans();
    }

    @GetMapping("/{id}")
    public LoanEntity getLoanById(@PathVariable Long id) {
        return loanService.getLoanById(id);
    }

    @PostMapping
    public LoanEntity createLoan(@RequestParam Long clientId,
                                 @RequestParam Long toolId,
                                 @RequestParam String dueDate,
                                 @RequestBody UserEntity user) {
        return loanService.createLoan(clientId, toolId, LocalDate.parse(dueDate), user);
    }

    @PutMapping("/{loanId}/return")
    public LoanEntity returnLoan(@PathVariable Long loanId,
                                 @RequestParam boolean damaged,
                                 @RequestParam boolean irreparable,
                                 @RequestBody UserEntity user) {
        return loanService.returnLoan(loanId, damaged, irreparable, user);
    }
}
