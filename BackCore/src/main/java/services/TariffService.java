package services;

import entities.TariffEntity;
import repositories.TariffRepository;
import org.springframework.stereotype.Service;

@Service
public class TariffService {

    private final TariffRepository tariffRepository;

    public TariffService(TariffRepository tariffRepository) {
        this.tariffRepository = tariffRepository;
    }

    public TariffEntity updateTariff(TariffEntity tariff) {
        return tariffRepository.save(tariff);
    }

    public double getDailyRentFee() {
        return tariffRepository.findAll().stream().findFirst()
                .orElseThrow(() -> new RuntimeException("No tariff configured"))
                .getDailyRentFee();
    }

    public double getDailyLateFee() {
        return tariffRepository.findAll().stream().findFirst()
                .orElseThrow(() -> new RuntimeException("No tariff configured"))
                .getDailyLateFee();
    }

    public double getRepairFee() {
        return tariffRepository.findAll().stream().findFirst()
                .orElseThrow(() -> new RuntimeException("No tariff configured"))
                .getRepairFee();
    }
}
