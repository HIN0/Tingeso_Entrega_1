package controllers;

import entities.TariffEntity;
import services.TariffService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/tariffs")
@CrossOrigin("*")
@PreAuthorize("hasRole('ADMIN')") // Protege todos los métodos para solo ADMIN
public class TariffController {

    private final TariffService tariffService;

    public TariffController(TariffService tariffService) {
        this.tariffService = tariffService;
    }

    // Consultar tarifas actuales (Admin puede verlas)
    @GetMapping
    public TariffEntity getTariff() {
        return tariffService.getTariff();
    }

    // Modificar tarifas (Admin puede modificarlas)
    @PutMapping
    public TariffEntity updateTariff(@RequestBody TariffEntity updated) {
        return tariffService.updateTariff(updated);
    }
}