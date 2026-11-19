package repositories;

import entities.ClientEntity;
import entities.LoanEntity;
import entities.enums.LoanStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface LoanRepository extends JpaRepository<LoanEntity, Long> {
       List<LoanEntity> findByStatus(LoanStatus status);
       List<LoanEntity> findByStatusIn(List<LoanStatus> statuses);
       List<LoanEntity> findByClientAndStatus(ClientEntity client, LoanStatus status);
       List<LoanEntity> findByClientAndStatusAndTotalPenaltyGreaterThan(ClientEntity client, LoanStatus status, double penaltyThreshold);
       long countByClientAndStatus(ClientEntity client, LoanStatus status);
       List<LoanEntity> findByStatusAndDueDateBefore(LoanStatus status, LocalDate date);

@Query("SELECT l.tool, COUNT(l) as total " +
       "FROM LoanEntity l " +
       "WHERE l.startDate >= :from AND l.startDate <= :to " +
       "GROUP BY l.tool " +
       "ORDER BY total DESC")
       List<Object[]> findTopToolsByDateRange(@Param("from") LocalDate from, @Param("to") LocalDate to);

// RF6.1: loans by status whose start date is in the range
@Query("SELECT l FROM LoanEntity l WHERE l.status = :status AND l.startDate BETWEEN :from AND :to")
       List<LoanEntity> findByStatusAndStartDateBetween(@Param("status") LoanStatus status, @Param("from") LocalDate from, @Param("to") LocalDate to);

// RF6.2: distinct clients with LATE loans started in the range
@Query("SELECT DISTINCT l.client FROM LoanEntity l WHERE l.status = :status AND l.startDate BETWEEN :from AND :to")
       List<ClientEntity> findDistinctClientsByStatusAndStartDateBetween(@Param("status") LoanStatus status, @Param("from") LocalDate from, @Param("to") LocalDate to);

}