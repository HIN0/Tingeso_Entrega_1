package services;

import entities.ClientEntity;
import entities.LoanEntity;
import entities.enums.ClientStatus;
import entities.enums.LoanStatus;
import org.springframework.stereotype.Service;
import repositories.ClientRepository;
import repositories.LoanRepository;
import repositories.ToolRepository;

import java.time.LocalDate;
import java.util.List;

@Service
public class ReportService {

    private final LoanRepository loanRepository;
    private final ClientRepository clientRepository;

    public ReportService(LoanRepository loanRepository, ClientRepository clientRepository, ToolRepository toolRepository) {
        this.loanRepository = loanRepository;
        this.clientRepository = clientRepository;
    }

    // RF6.1: préstamos por estado
    public List<LoanEntity> getLoansByStatus(String status) {
        LoanStatus loanStatus = LoanStatus.valueOf(status);
        return loanRepository.findByStatus(loanStatus);
    }

    // RF6.2: clientes con préstamos atrasados
    public List<ClientEntity> getClientsWithLateLoans() {
        return loanRepository.findByStatus(LoanStatus.LATE).stream()
                .map(LoanEntity::getClient)
                .distinct()
                .toList();
    }

    // RF6.3: ranking de herramientas más prestadas en un rango
    public List<Object[]> getTopTools(LocalDate from, LocalDate to) {
        return loanRepository.findTopToolsByDateRange(from, to);
    }

    // Bonus: clientes restringidos (para administración)
    public List<ClientEntity> getRestrictedClients() {
        return clientRepository.findByStatus(ClientStatus.RESTRICTED);
    }
}
