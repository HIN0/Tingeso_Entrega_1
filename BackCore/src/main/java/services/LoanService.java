package services;

// Entidades, Enums y Repositorios
import entities.*;
import entities.enums.ClientStatus;
import entities.enums.LoanStatus;
import entities.enums.MovementType;
import entities.enums.ToolStatus;
import repositories.LoanRepository;
import repositories.ToolRepository;
import repositories.ClientRepository;
import repositories.KardexRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class LoanService {

    private final LoanRepository loanRepository;
    private final ClientRepository clientRepository;
    private final ToolRepository toolRepository;
    private final KardexRepository kardexRepository;

    public LoanService(LoanRepository loanRepository,
                       ClientRepository clientRepository,
                       ToolRepository toolRepository,
                       KardexRepository kardexRepository) {
        this.loanRepository = loanRepository;
        this.clientRepository = clientRepository;
        this.toolRepository = toolRepository;
        this.kardexRepository = kardexRepository;
    }

    // Obtener todos los préstamos
    public List<LoanEntity> getAllLoans() {
        return loanRepository.findAll();
    }

    // Obtener préstamo por ID
    public LoanEntity getLoanById(Long id) {
        return loanRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Loan not found"));
    }

    // Registrar préstamo con todas las validaciones
    @Transactional
    public LoanEntity createLoan(Long clientId, Long toolId, LocalDate dueDate, UserEntity user) {
        ClientEntity client = clientRepository.findById(clientId)
                .orElseThrow(() -> new IllegalArgumentException("Client not found"));
        ToolEntity tool = toolRepository.findById(toolId)
                .orElseThrow(() -> new IllegalArgumentException("Tool not found"));

        // Validaciones de negocio
        if (client.getStatus() == ClientStatus.RESTRICTED) {
            throw new IllegalStateException("Client is restricted and cannot borrow tools");
        }

        // Verificar que no tenga préstamos vencidos o multas
        List<LoanEntity> activeLoans = loanRepository.findByClientAndStatus(client, LoanStatus.ACTIVE);
        if (activeLoans.size() >= 5) {
            throw new IllegalStateException("Client already has maximum number of active loans (5)");
        }
        for (LoanEntity loan : activeLoans) {
            if (loan.getDueDate().isBefore(LocalDate.now())) {
                throw new IllegalStateException("Client has overdue loans");
            }
        }

        if (tool.getStatus() != ToolStatus.AVAILABLE || tool.getStock() < 1) {
            throw new IllegalStateException("Tool is not available for loan");
        }

        if (dueDate.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Due date cannot be before loan start date");
        }

        // Crear préstamo
        LoanEntity loan = new LoanEntity();
        loan.setClient(client);
        loan.setTool(tool);
        loan.setStartDate(LocalDate.now());
        loan.setDueDate(dueDate);
        loan.setStatus(LoanStatus.ACTIVE);
        loan.setPenaltyAmount(0.0);

        // Actualizar stock y estado herramienta
        tool.setStock(tool.getStock() - 1);
        if (tool.getStock() == 0) tool.setStatus(ToolStatus.LOANED);
        toolRepository.save(tool);

        // Registrar movimiento en kardex
        KardexEntity movement = new KardexEntity();
        movement.setTool(tool);
        movement.setType(MovementType.LOAN);
        movement.setQuantity(1);
        movement.setDate(java.time.LocalDateTime.now());
        movement.setUser(user);
        kardexRepository.save(movement);

        return loanRepository.save(loan);
    }

    // Registrar devolución de herramienta
    @Transactional
    public LoanEntity returnLoan(Long loanId, boolean damaged, boolean irreparable, UserEntity user) {
        LoanEntity loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new IllegalArgumentException("Loan not found"));
        ToolEntity tool = loan.getTool();

        if (loan.getStatus() != LoanStatus.ACTIVE) {
            throw new IllegalStateException("Loan is not active");
        }

        loan.setReturnDate(LocalDate.now());
        loan.setStatus(LoanStatus.RETURNED);

        // Calcular multas por atraso
        long daysLate = ChronoUnit.DAYS.between(loan.getDueDate(), loan.getReturnDate());
        if (daysLate > 0) {
            double penalty = daysLate * 5000; // Ejemplo: 5000 por día de atraso
            loan.setPenaltyAmount(loan.getPenaltyAmount() + penalty);
        }

        // Manejo de daños
        if (damaged) {
            if (irreparable) {
                tool.setStatus(ToolStatus.DECOMMISSIONED);
                tool.setStock(tool.getStock()); // stock no aumenta
                loan.setPenaltyAmount(loan.getPenaltyAmount() + tool.getReplacementValue());

                // Kardex baja definitiva
                KardexEntity movement = new KardexEntity();
                movement.setTool(tool);
                movement.setType(MovementType.DECOMMISSION);
                movement.setQuantity(1);
                movement.setDate(java.time.LocalDateTime.now());
                movement.setUser(user);
                kardexRepository.save(movement);

            } else {
                tool.setStatus(ToolStatus.UNDER_REPAIR);
                // Kardex movimiento reparación
                KardexEntity movement = new KardexEntity();
                movement.setTool(tool);
                movement.setType(MovementType.REPAIR);
                movement.setQuantity(1);
                movement.setDate(java.time.LocalDateTime.now());
                movement.setUser(user);
                kardexRepository.save(movement);
            }
        } else {
            // Normal → devolución con stock aumentado
            tool.setStock(tool.getStock() + 1);
            tool.setStatus(ToolStatus.AVAILABLE);

            // Kardex devolución
            KardexEntity movement = new KardexEntity();
            movement.setTool(tool);
            movement.setType(MovementType.RETURN);
            movement.setQuantity(1);
            movement.setDate(java.time.LocalDateTime.now());
            movement.setUser(user);
            kardexRepository.save(movement);
        }

        toolRepository.save(tool);
        return loanRepository.save(loan);
    }
}
