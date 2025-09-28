package services;

import entities.*;
import entities.enums.*;
import repositories.ClientRepository;
import repositories.LoanRepository;
import repositories.ToolRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class LoanService {

    private final LoanRepository loanRepository;
    private final ClientRepository clientRepository;
    private final ToolRepository toolRepository;
    private final KardexService kardexService;
    private final TariffService tariffService;

    public LoanService(LoanRepository loanRepository,
                       ClientRepository clientRepository,
                       ToolRepository toolRepository,
                       KardexService kardexService,
                       TariffService tariffService) {
        this.loanRepository = loanRepository;
        this.clientRepository = clientRepository;
        this.toolRepository = toolRepository;
        this.kardexService = kardexService;
        this.tariffService = tariffService;
    }


    /**
     * Crear un préstamo nuevo
     */
    public LoanEntity createLoan(Long clientId, Long toolId, LocalDate dueDate, UserEntity user) {

        System.out.println("=== DEBUG: Entrando a createLoan ===");
        System.out.println("Parámetros recibidos -> clientId=" + clientId + ", toolId=" + toolId + ", dueDate=" + dueDate);

        // Paso 1: Buscar cliente
        System.out.println("Paso 1: Buscando cliente con id=" + clientId);
        ClientEntity client = clientRepository.findById(clientId)
                .orElseThrow(() -> new RuntimeException("Client not found"));
        System.out.println("Cliente encontrado -> " + client.getName() + " (status=" + client.getStatus() + ")");

        // Paso 2: Buscar herramienta
        System.out.println("Paso 2: Buscando herramienta con id=" + toolId);
        ToolEntity tool = toolRepository.findById(toolId)
                .orElseThrow(() -> new RuntimeException("Tool not found"));
        System.out.println("Herramienta encontrada -> " + tool.getName() + " (status=" + tool.getStatus() + ", stock=" + tool.getStock() + ")");

        // Paso 3: Validaciones
        System.out.println("Paso 3: Validando negocio...");
        if (client.getStatus() == ClientStatus.RESTRICTED) {
            throw new IllegalStateException("Client is restricted and cannot request loans");
        }
        if (tool.getStatus() != ToolStatus.AVAILABLE || tool.getStock() <= 0) {
            throw new IllegalStateException("Tool is not available");
        }
        if (dueDate.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Due date cannot be in the past");
        }
        System.out.println("Validaciones OK");

        // Paso 4: Crear objeto Loan
        System.out.println("Paso 4: Creando objeto Loan...");
        LoanEntity loan = LoanEntity.builder()
                .client(client)
                .tool(tool)
                .startDate(LocalDate.now())
                .dueDate(dueDate)
                .status(LoanStatus.ACTIVE)
                .totalPenalty(0.0)
                .build();
        System.out.println("Loan creado en memoria: " + loan);

        // Paso 5: Actualizar herramienta
        System.out.println("Paso 5: Actualizando stock de herramienta...");
        tool.setStock(tool.getStock() - 1);
        if (tool.getStock() == 0) {
            tool.setStatus(ToolStatus.LOANED);
        }
        System.out.println("Nuevo estado de herramienta -> status=" + tool.getStatus() + ", stock=" + tool.getStock());

        // Paso 6: Guardar en BD
        System.out.println("Paso 6: Guardando Loan en BD...");
        LoanEntity savedLoan = loanRepository.save(loan);
        toolRepository.save(tool);
        System.out.println("Loan guardado con id=" + savedLoan.getId());

        // Paso 7: Registrar Kardex (puedes comentar si da problemas)
        System.out.println("Paso 7: Registrando movimiento en Kardex...");
        kardexService.registerMovement(tool, MovementType.LOAN, 1, user);
        System.out.println("Movimiento registrado");

        System.out.println("=== DEBUG: createLoan finalizado con éxito ===");
        return savedLoan;
}


    /**
     * Registrar devolución de un préstamo
     */
    public LoanEntity returnLoan(Long loanId, Long toolId, boolean damaged, boolean irreparable, UserEntity user) {
        LoanEntity loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new RuntimeException("Loan not found"));
        ToolEntity tool = toolRepository.findById(toolId)
                .orElseThrow(() -> new RuntimeException("Tool not found"));

        if (loan.getStatus() != LoanStatus.ACTIVE && loan.getStatus() != LoanStatus.LATE) {
            throw new IllegalStateException("Loan is not active and cannot be returned");
        }

        // Marcar devolución
        loan.setReturnDate(LocalDate.now());
        loan.setStatus(LoanStatus.CLOSED);

        // Calcular multa por atraso
        long delay = ChronoUnit.DAYS.between(loan.getDueDate(), loan.getReturnDate());
        if (delay > 0) {
            double lateFee = delay * tariffService.getDailyLateFee();
            loan.setTotalPenalty(loan.getTotalPenalty() + lateFee);
        }

        // Evaluar daño de herramienta
        if (damaged) {
            if (irreparable) {
                // Herramienta dada de baja
                loan.setTotalPenalty(loan.getTotalPenalty() + tool.getReplacementValue());
                tool.setStatus(ToolStatus.DECOMMISSIONED);
                tool.setStock(0);
                toolRepository.save(tool);
                kardexService.registerMovement(tool, MovementType.DECOMMISSION, 1, user);
            } else {
                // Herramienta en reparación
                loan.setTotalPenalty(loan.getTotalPenalty() + tariffService.getRepairFee());
                tool.setStatus(ToolStatus.REPAIRING);
                toolRepository.save(tool);
                kardexService.registerMovement(tool, MovementType.REPAIR, 1, user);
            }
        } else {
            // Herramienta devuelta sin daños
            tool.setStock(tool.getStock() + 1);
            tool.setStatus(ToolStatus.AVAILABLE);
            toolRepository.save(tool);
            kardexService.registerMovement(tool, MovementType.RETURN, 1, user);
        }

        return loanRepository.save(loan);
    }

    /**
     * Listar préstamos activos
     */
    public List<LoanEntity> getActiveLoans() {
        return loanRepository.findByStatus(LoanStatus.ACTIVE);
    }
}
