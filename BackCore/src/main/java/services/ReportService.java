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

    // --- RF6.1: Préstamos por estado (CON FILTRO DE FECHA OPCIONAL) ---
    public List<LoanEntity> getLoansByStatus(String status, LocalDate from, LocalDate to) {
        LoanStatus loanStatus = LoanStatus.valueOf(status);
        // Si las fechas son nulas, llama al método sin filtro
        if (from == null || to == null) {
            return loanRepository.findByStatus(loanStatus);
        } else {
            // Si hay fechas, llama al nuevo método con filtro
            // Validar que 'from' no sea posterior a 'to'
            if (from.isAfter(to)) {
                throw new IllegalArgumentException("Start date cannot be after end date.");
            }
            return loanRepository.findByStatusAndStartDateBetween(loanStatus, from, to);
        }
    }

    // --- RF6.2: Clientes con préstamos atrasados (CON FILTRO DE FECHA OPCIONAL) ---
    public List<ClientEntity> getClientsWithLateLoans(LocalDate from, LocalDate to) {
        // Si las fechas son nulas, busca todos los clientes con préstamos LATE
        if (from == null || to == null) {
            return loanRepository.findByStatus(LoanStatus.LATE).stream()
                    .map(LoanEntity::getClient)
                    .distinct()
                    .toList();
        } else {
            // Si hay fechas, llama al nuevo método del repositorio
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