package services;

// Entidades ,Enums y Repositorios
import entities.*;
import entities.enums.LoanStatus;
import repositories.ClientRepository;
import repositories.LoanRepository;
import repositories.ToolRepository;

import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ReportService {

    private final LoanRepository loanRepository;
    private final ClientRepository clientRepository;
    private final ToolRepository toolRepository;

    public ReportService(LoanRepository loanRepository,
                         ClientRepository clientRepository,
                         ToolRepository toolRepository) {
        this.loanRepository = loanRepository;
        this.clientRepository = clientRepository;
        this.toolRepository = toolRepository;
    }

    // RF6.1 - Préstamos activos (vigentes y atrasados)
    public List<LoanEntity> getActiveLoans(LocalDate start, LocalDate end) {
        if (start != null && end != null) {
            return loanRepository.findByStartDateBetween(start, end)
                    .stream()
                    .filter(loan -> loan.getStatus() == LoanStatus.ACTIVE || loan.getStatus() == LoanStatus.LATE)
                    .collect(Collectors.toList());
        }
        return loanRepository.findAll().stream()
                .filter(loan -> loan.getStatus() == LoanStatus.ACTIVE || loan.getStatus() == LoanStatus.LATE)
                .collect(Collectors.toList());
    }

    // RF6.2 - Clientes con atrasos
    public List<ClientEntity> getClientsWithOverdueLoans(LocalDate start, LocalDate end) {
        List<LoanEntity> loans;

        if (start != null && end != null) {
            loans = loanRepository.findByStartDateBetween(start, end);
        } else {
            loans = loanRepository.findAll();
        }

        return loans.stream()
                .filter(loan -> loan.getStatus() == LoanStatus.LATE)
                .map(LoanEntity::getClient)
                .distinct()
                .collect(Collectors.toList());
    }

    // RF6.3 - Ranking de herramientas más prestadas
    public List<Map.Entry<ToolEntity, Long>> getMostBorrowedTools(LocalDate start, LocalDate end) {
        List<LoanEntity> loans;

        if (start != null && end != null) {
            loans = loanRepository.findByStartDateBetween(start, end);
        } else {
            loans = loanRepository.findAll();
        }

        Map<ToolEntity, Long> counts = loans.stream()
                .collect(Collectors.groupingBy(LoanEntity::getTool, Collectors.counting()));

        return counts.entrySet().stream()
                .sorted(Map.Entry.<ToolEntity, Long>comparingByValue().reversed())
                .collect(Collectors.toList());
    }
}
