package services;

// Entidad y Repositorio
import entities.TariffEntity;
import repositories.TariffRepository;

import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class TariffService {

    private final TariffRepository tariffRepository;

    public TariffService(TariffRepository tariffRepository) {
        this.tariffRepository = tariffRepository;
    }

    // Obtener todas las tarifas
    public List<TariffEntity> getAllTariffs() {
        return tariffRepository.findAll();
    }

    // Obtener tarifa por ID
    public TariffEntity getTariffById(Long id) {
        return tariffRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Tariff not found"));
    }

    // Crear nueva tarifa (solo Admin debería poder usar este endpoint)
    public TariffEntity createTariff(TariffEntity tariff) {
        validateTariff(tariff);
        return tariffRepository.save(tariff);
    }

    // Actualizar tarifa (solo Admin)
    public TariffEntity updateTariff(Long id, TariffEntity updated) {
        TariffEntity existing = getTariffById(id);

        if (updated.getDailyRate() != null) existing.setDailyRate(updated.getDailyRate());
        if (updated.getLateFeeRate() != null) existing.setLateFeeRate(updated.getLateFeeRate());
        if (updated.getRepairFee() != null) existing.setRepairFee(updated.getRepairFee());

        validateTariff(existing);
        return tariffRepository.save(existing);
    }

    // Eliminar tarifa (no siempre recomendable, pero lo dejamos)
    public void deleteTariff(Long id) {
        if (!tariffRepository.existsById(id)) {
            throw new IllegalArgumentException("Tariff not found");
        }
        tariffRepository.deleteById(id);
    }

    // Validación de negocio
    private void validateTariff(TariffEntity tariff) {
        if (tariff.getDailyRate() == null || tariff.getDailyRate() <= 0) {
            throw new IllegalArgumentException("Daily rate must be greater than 0");
        }
        if (tariff.getLateFeeRate() == null || tariff.getLateFeeRate() <= 0) {
            throw new IllegalArgumentException("Late fee rate must be greater than 0");
        }
    }
}
