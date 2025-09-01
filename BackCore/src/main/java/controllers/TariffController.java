package controllers;

// Entidad, Servicios
import entities.TariffEntity;
import services.TariffService;

import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/tariffs")
public class TariffController {

    private final TariffService tariffService;

    public TariffController(TariffService tariffService) {
        this.tariffService = tariffService;
    }

    @GetMapping
    public List<TariffEntity> getAllTariffs() {
        return tariffService.getAllTariffs();
    }

    @GetMapping("/{id}")
    public TariffEntity getTariffById(@PathVariable Long id) {
        return tariffService.getTariffById(id);
    }

    @PostMapping
    public TariffEntity createTariff(@RequestBody TariffEntity tariff) {
        return tariffService.createTariff(tariff);
    }

    @PutMapping("/{id}")
    public TariffEntity updateTariff(@PathVariable Long id, @RequestBody TariffEntity tariff) {
        return tariffService.updateTariff(id, tariff);
    }

    @DeleteMapping("/{id}")
    public void deleteTariff(@PathVariable Long id) {
        tariffService.deleteTariff(id);
    }
}
