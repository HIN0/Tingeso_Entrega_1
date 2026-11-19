package controllers;

import dtos.TariffRequest;
import entities.TariffEntity;
import jakarta.validation.Valid;
import services.TariffService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/tariffs")
@CrossOrigin("*")
@PreAuthorize("hasRole('ADMIN')") // Only administrators can see and change tariffs (Epic 4)
public class TariffController {

    private final TariffService tariffService;

    public TariffController(TariffService tariffService) {
        this.tariffService = tariffService;
    }

    @GetMapping
    public TariffEntity getTariff() {
        return tariffService.getTariff();
    }

    @PutMapping
    public TariffEntity updateTariff(@Valid @RequestBody TariffRequest request) {
        return tariffService.updateTariff(request.toEntity());
    }
}
