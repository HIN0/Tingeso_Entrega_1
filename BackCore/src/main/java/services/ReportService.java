package services;

import entities.ClientEntity;
import entities.LoanEntity;
import entities.enums.LoanStatus;
import org.springframework.stereotype.Service;
import repositories.LoanRepository;

import java.time.LocalDate;
import java.util.List;

@Service
public class ReportService {

    private final LoanRepository loanRepository;

    public ReportService(LoanRepository loanRepository) {
        this.loanRepository = loanRepository;
    }

    // --- RF6.1: loans by status (optional date filter) ---
    public List<LoanEntity> getLoansByStatus(String status, LocalDate from, LocalDate to) {
        LoanStatus loanStatus = LoanStatus.valueOf(status);
        // No dates: no filter
        if (from == null || to == null) {
            return loanRepository.findByStatus(loanStatus);
        } else {
            // With dates: filter by start date
            // 'from' cannot be after 'to'
            if (from.isAfter(to)) {
                throw new IllegalArgumentException("Start date cannot be after end date.");
            }
            return loanRepository.findByStatusAndStartDateBetween(loanStatus, from, to);
        }
    }

    // --- RF6.2: clients with late loans (optional date filter) ---
    public List<ClientEntity> getClientsWithLateLoans(LocalDate from, LocalDate to) {
        // No dates: every client with a LATE loan
        if (from == null || to == null) {
            return loanRepository.findByStatus(LoanStatus.LATE).stream()
                    .map(LoanEntity::getClient)
                    .distinct()
                    .toList();
        } else {
            // With dates: filter by loan start date
            if (from.isAfter(to)) {
                throw new IllegalArgumentException("Start date cannot be after end date.");
            }
            return loanRepository.findDistinctClientsByStatusAndStartDateBetween(LoanStatus.LATE, from, to);
        }
    }

    // --- RF6.3: Ranking (sin cambios, ya acepta fechas) ---
    public List<Object[]> getTopTools(LocalDate from, LocalDate to) {
        if (from == null || to == null) {
            throw new IllegalArgumentException("Date range is required for Top Tools report.");
        }
        if (from.isAfter(to)) {
            throw new IllegalArgumentException("Start date cannot be after end date.");
        }
        return loanRepository.findTopToolsByDateRange(from, to);
    }
}