package controllers;

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

    @PostMapping
    public LoanEntity createLoan(@RequestParam Long clientId,
                                 @RequestParam Long toolId,
                                 @RequestParam String dueDate) {
        UserEntity fakeUser = UserEntity.builder().id(2L).username("employee").build();
        return loanService.createLoan(clientId, toolId, LocalDate.parse(dueDate), fakeUser);
    }

    @PutMapping("/{id}/return")
    public LoanEntity returnLoan(@PathVariable Long id,
                                 @RequestParam Long toolId,
                                 @RequestParam boolean damaged,
                                 @RequestParam boolean irreparable) {
        UserEntity fakeUser = UserEntity.builder().id(2L).username("employee").build();
        return loanService.returnLoan(id, toolId, damaged, irreparable, fakeUser);
    }

    @GetMapping("/active")
    public List<LoanEntity> getActiveLoans() {
        return loanService.getActiveLoans();
    }
}
