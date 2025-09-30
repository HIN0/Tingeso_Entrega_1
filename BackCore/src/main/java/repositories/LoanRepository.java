package repositories;

import entities.LoanEntity;
import entities.enums.LoanStatus;
import entities.ClientEntity;
import entities.ToolEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface LoanRepository extends JpaRepository<LoanEntity, Long> {
    List<LoanEntity> findByClient(ClientEntity client);
    List<LoanEntity> findByTool(ToolEntity tool);
    List<LoanEntity> findByStatus(LoanStatus status);
        // Ranking de herramientas más prestadas en rango de fechas
    @Query("SELECT l.tool, COUNT(l) as total " +
           "FROM LoanEntity l " +
           "WHERE l.startDate >= :from AND l.startDate <= :to " +
           "GROUP BY l.tool " +
           "ORDER BY total DESC")
    List<Object[]> findTopToolsByDateRange(LocalDate from, LocalDate to);
    List<LoanEntity> findByClientAndStatus(ClientEntity client, LoanStatus status);
}
