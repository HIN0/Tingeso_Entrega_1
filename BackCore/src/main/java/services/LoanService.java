package services;

import entities.ClientEntity;
import entities.LoanEntity;
import entities.ToolEntity;
import entities.UserEntity;
import entities.enums.ClientStatus;
import entities.enums.LoanStatus;
import entities.enums.ToolStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import repositories.LoanRepository;
import repositories.ClientRepository;
import repositories.ToolRepository;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class LoanService {

    private final LoanRepository loanRepository;
    private final ClientRepository clientRepository;
    private final ToolRepository toolRepository;
    private final ToolService toolService;
    private final KardexService kardexService;
    private final TariffService tariffService;

    public LoanService(LoanRepository loanRepository,
                       ClientRepository clientRepository,
                       ToolRepository toolRepository,
                       ToolService toolService,
                       KardexService kardexService,
                       TariffService tariffService) {
        this.loanRepository = loanRepository;
        this.clientRepository = clientRepository;
        this.toolRepository = toolRepository;
        this.toolService = toolService;
        this.kardexService = kardexService;
        this.tariffService = tariffService;
    }

    // Versión antigua (por compatibilidad con @RequestParam): usa startDate=Hoy
    @Transactional
    public LoanEntity createLoan(Long clientId, Long toolId, LocalDate dueDate, UserEntity user) {
        return createLoan(clientId, toolId, LocalDate.now(), dueDate, user);
    }

    // Versión nueva (JSON): incluye startDate
    @Transactional
    public LoanEntity createLoan(Long clientId, Long toolId, LocalDate startDate, LocalDate dueDate, UserEntity user) {

        // 1) Buscar cliente y herramienta
        ClientEntity client = clientRepository.findById(clientId)
                .orElseThrow(() -> new RuntimeException("Client not found"));
        ToolEntity tool = toolRepository.findById(toolId)
                .orElseThrow(() -> new RuntimeException("Tool not found"));

        // 2) Validaciones de negocio (enunciado)
        if (client.getStatus() == ClientStatus.RESTRICTED) {
            throw new IllegalStateException("Client is restricted and cannot request loans");
        }
        if (tool.getStatus() != ToolStatus.AVAILABLE || tool.getStock() == null || tool.getStock() <= 0) {
            throw new IllegalStateException("Tool is not available");
        }
        if (startDate == null) startDate = LocalDate.now();
        if (dueDate == null) throw new IllegalArgumentException("dueDate is required");
        if (dueDate.isBefore(startDate)) {
            throw new IllegalArgumentException("Due date must be greater or equal than start date");
        }

        // Máximo 5 préstamos activos por cliente
        long activeCount = loanRepository.findAll().stream()
                .filter(l -> l.getClient() != null && l.getClient().getId().equals(clientId))
                .filter(l -> l.getStatus() == LoanStatus.ACTIVE) // solo activos contabilizan
                .count();
        if (activeCount >= 5) {
            throw new IllegalStateException("Client has reached the maximum number of active loans (5)");
        }

        // No repetir misma herramienta activa sin devolver
        boolean hasSameToolActive = loanRepository.findAll().stream()
                .anyMatch(l ->
                        l.getClient() != null && l.getTool() != null &&
                        l.getClient().getId().equals(clientId) &&
                        l.getTool().getId().equals(toolId) &&
                        l.getStatus() == LoanStatus.ACTIVE);
        if (hasSameToolActive) {
            throw new IllegalStateException("Client already has this tool on loan");
        }

        // 3) Crear Loan (estado ACTIVE)
        LoanEntity loan = LoanEntity.builder()
                .client(client)
                .tool(tool)
                .startDate(startDate)
                .dueDate(dueDate)
                .status(LoanStatus.ACTIVE)
                .totalPenalty(0.0) // si usas Integer cámbialo; tu código mostraba double
                .build();

        // 4) Actualizar inventario (stock/estado) + Kardex LOAN
        toolService.decrementStockForLoan(tool, user);

        // 5) Persistir
        return loanRepository.save(loan);
    }

    // Devolución (firma antigua: usa returnDate = hoy)
    @Transactional
    public LoanEntity returnLoan(Long loanId, Long toolId, boolean damaged, boolean irreparable, UserEntity user) {
        return returnLoan(loanId, toolId, damaged, irreparable, user, LocalDate.now());
    }

    // Devolución (firma nueva: permite returnDate)
    @Transactional
    public LoanEntity returnLoan(Long loanId, Long toolId, boolean damaged, boolean irreparable,
                                 UserEntity user, LocalDate returnDate) {

        LoanEntity loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new RuntimeException("Loan not found"));
        ToolEntity tool = toolRepository.findById(toolId)
                .orElseThrow(() -> new RuntimeException("Tool not found"));

        if (loan.getStatus() != LoanStatus.ACTIVE && loan.getStatus() != LoanStatus.LATE) {
            throw new IllegalStateException("Loan is not active and cannot be returned");
        }

        if (returnDate == null) returnDate = LocalDate.now();

    // Calcular multa por atraso
    long delay = ChronoUnit.DAYS.between(loan.getDueDate(), returnDate);
    double totalPenalty = loan.getTotalPenalty(); // siempre tiene valor inicial (0.0)

    if (delay > 0) {
        double lateFee = delay * tariffService.getDailyLateFee();
        totalPenalty += lateFee;
    }

        // Daño / estado de herramienta / Kardex
        if (damaged) {
            if (irreparable) {
                // Baja definitiva: cobrar reposición
                totalPenalty += tool.getReplacementValue();
                toolService.markAsDecommissioned(tool, user);
            } else {
                // Reparación: cobrar tarifa de reparación
                totalPenalty += tariffService.getRepairFee();
                toolService.markAsRepairing(tool, user);
            }
        } else {
            // Devuelta en buen estado: vuelve a stock
            toolService.incrementStockForReturn(tool, user);
        }

        // Cerrar préstamo
        loan.setReturnDate(returnDate);
        loan.setStatus(LoanStatus.CLOSED);
        loan.setTotalPenalty(totalPenalty);

        return loanRepository.save(loan);
    }

    @Transactional(readOnly = true)
    public List<LoanEntity> getActiveLoans() {
        return loanRepository.findAll().stream()
                .filter(l -> l.getStatus() == LoanStatus.ACTIVE)
                .toList();
    }
}
