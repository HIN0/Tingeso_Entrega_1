package controllers;

import entities.KardexEntity;
import entities.ToolEntity;
import services.KardexService;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/kardex")
@CrossOrigin("*")
public class KardexController {

    private final KardexService kardexService;

    public KardexController(KardexService kardexService) {
        this.kardexService = kardexService;
    }

    @GetMapping("/tool/{toolId}")
    public List<KardexEntity> getMovementsByTool(@PathVariable ToolEntity tool) {
        return kardexService.getMovementsByTool(tool);
    }

    @GetMapping("/date")
    public List<KardexEntity> getMovementsByDate(@RequestParam String start,
                                                 @RequestParam String end) {
        return kardexService.getMovementsByDate(LocalDateTime.parse(start), LocalDateTime.parse(end));
    }
}
