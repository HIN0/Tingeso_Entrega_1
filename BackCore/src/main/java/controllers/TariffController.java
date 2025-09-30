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

    // Consultar tarifas actuales
    @GetMapping
    public TariffEntity getTariff() {
        return tariffService.getTariff();
    }

    // Modificar tarifas (solo admin debería tener acceso aquí)
    @PutMapping
    public TariffEntity updateTariff(@RequestBody TariffEntity updated) {
        return tariffService.updateTariff(updated);
    }
}
