package services;

import entities.KardexEntity;
import entities.ToolEntity;
import entities.UserEntity;
import entities.enums.MovementType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import repositories.KardexRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class KardexService {

    private final KardexRepository kardexRepository;

    public KardexService(KardexRepository kardexRepository) {
        this.kardexRepository = kardexRepository;
    }

    @Transactional
    public void registerMovement(ToolEntity tool, MovementType type, int quantity, UserEntity user) {
        KardexEntity movement = KardexEntity.builder()
                .tool(tool)
                .type(type)
                .movementDate(LocalDateTime.now())
                .quantity(quantity)
                .user(user)
                .build();
        kardexRepository.save(movement);
    }

    @Transactional(readOnly = true)
    public List<KardexEntity> getMovementsByTool(ToolEntity tool) {
        return kardexRepository.findByTool_Id(tool.getId());
    }

    @Transactional(readOnly = true)
    public List<KardexEntity> getMovementsByDate(LocalDateTime start, LocalDateTime end) {
        return kardexRepository.findByMovementDateBetween(start, end);
    }
}
