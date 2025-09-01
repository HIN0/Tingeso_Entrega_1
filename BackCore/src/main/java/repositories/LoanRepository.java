package repositories;

// Entidades, Enums
import entities.ClientEntity;
import entities.LoanEntity;
import entities.enums.LoanStatus;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface LoanRepository extends JpaRepository<LoanEntity, Long> {
    List<LoanEntity> findByClientAndStatus(ClientEntity client, LoanStatus status);
    List<LoanEntity> findByStatus(LoanStatus status);
    List<LoanEntity> findByStartDateBetween(LocalDate start, LocalDate end);
}
