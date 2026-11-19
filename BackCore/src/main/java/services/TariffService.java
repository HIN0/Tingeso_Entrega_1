package services;

import entities.TariffEntity;
import repositories.TariffRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TariffService {

    private final TariffRepository tariffRepository;

    public TariffService(TariffRepository tariffRepository) {
        this.tariffRepository = tariffRepository;
    }

    @Transactional(readOnly = true)
    public TariffEntity getTariff() {
        return tariffRepository.findAll().stream()
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No tariffs configured"));
    }

    @Transactional
    public TariffEntity updateTariff(TariffEntity updated) {
        // Business rule (Epic 4): every tariff is mandatory; rent must be positive, fees cannot be negative
        requireAtLeast(updated.getDailyRentFee(), 1, "Daily rent fee");
        requireAtLeast(updated.getDailyLateFee(), 0, "Daily late fee");
        requireAtLeast(updated.getRepairFee(), 0, "Repair fee");

        TariffEntity current = getTariff();
        current.setDailyRentFee(updated.getDailyRentFee());
        current.setDailyLateFee(updated.getDailyLateFee());
        current.setRepairFee(updated.getRepairFee());
        return tariffRepository.save(current);
    }

    private static void requireAtLeast(Integer value, int min, String field) {
        if (value == null || value < min) {
            throw new IllegalArgumentException(field + " is required and must be at least " + min + ".");
        }
    }

    public double getDailyLateFee() {
        return getTariff().getDailyLateFee();
    }

    public double getDailyRentFee() {
        return getTariff().getDailyRentFee();
    }

    public double getRepairFee() {
        return getTariff().getRepairFee();
    }
}
