package repositories;

import entities.KardexEntity;
import entities.ToolEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface KardexRepository extends JpaRepository<KardexEntity, Long> {
    List<KardexEntity> findByTool(ToolEntity tool);
    List<KardexEntity> findByDateBetween(LocalDateTime start, LocalDateTime end);
}
