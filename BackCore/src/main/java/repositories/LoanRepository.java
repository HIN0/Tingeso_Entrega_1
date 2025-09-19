package repositories;

import entities.LoanEntity;
import entities.enums.LoanStatus;
import entities.ClientEntity;
import entities.ToolEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LoanRepository extends JpaRepository<LoanEntity, Long> {
    List<LoanEntity> findByClient(ClientEntity client);
    List<LoanEntity> findByTool(ToolEntity tool);
    List<LoanEntity> findByStatus(LoanStatus status);
    List<LoanEntity> findByClientAndStatus(ClientEntity client, LoanStatus status);
}
