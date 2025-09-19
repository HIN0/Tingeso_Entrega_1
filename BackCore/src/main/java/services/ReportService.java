package services;

import entities.ClientEntity;
import entities.LoanEntity;
import entities.ToolEntity;
import entities.enums.LoanStatus;
import repositories.ClientRepository;
import repositories.LoanRepository;
import repositories.ToolRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ReportService {

    private final LoanRepository loanRepository;
    private final ClientRepository clientRepository;
    private final ToolRepository toolRepository;

    public ReportService(LoanRepository loanRepository, ClientRepository clientRepository, ToolRepository toolRepository) {
        this.loanRepository = loanRepository;
        this.clientRepository = clientRepository;
        this.toolRepository = toolRepository;
    }

    public List<LoanEntity> getActiveLoans() {
        return loanRepository.findByStatus(LoanStatus.ACTIVE);
    }

    public List<ClientEntity> getRestrictedClients() {
        return clientRepository.findByStatus(entities.enums.ClientStatus.RESTRICTED);
    }

    public List<ToolEntity> getMostLoanedTools() {
        // Aquí podrías implementar ranking con JPQL o query nativa
        return toolRepository.findAll(); // placeholder
    }
}
