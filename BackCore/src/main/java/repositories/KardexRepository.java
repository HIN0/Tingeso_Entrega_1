package repositories;

import entities.KardexEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface KardexRepository extends JpaRepository<KardexEntity, Long> {

    // Por relación (si tienes @ManyToOne Tool tool)
    List<KardexEntity> findByTool_Id(Long toolId); // más cómodo para usar con IDs

    // Rango de fechas usando el nombre del atributo correcto
    List<KardexEntity> findByMovementDateBetween(LocalDateTime start, LocalDateTime end);

    // (Opcional) Filtrar por tipo de movimiento si lo necesitas para reportes
    List<KardexEntity> findByTool_IdAndType(Long toolId, entities.enums.MovementType type);

    // (Opcional) Rango + tipo
    List<KardexEntity> findByMovementDateBetweenAndType(LocalDateTime start, LocalDateTime end,
                                                        entities.enums.MovementType type);
}
