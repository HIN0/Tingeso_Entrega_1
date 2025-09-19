package controllers;

import entities.TariffEntity;
import services.TariffService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/tariffs")
public class TariffController {

    private final TariffService tariffService;

    public TariffController(TariffService tariffService) {
        this.tariffService = tariffService;
    }

    @PutMapping
    public TariffEntity updateTariff(@RequestBody TariffEntity tariff) {
        return tariffService.updateTariff(tariff);
    }
}
