package pl.rentowne.settlement;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.rentowne.common.controler.AbstractController;

@RestController
@RequiredArgsConstructor
@Tag(name = "Statystyki rozliczeń")
@RequestMapping("/api/settlements/stats")
class SettlementStatsController extends AbstractController {

    private final SettlementStatsService settlementStatsService;

    @GetMapping
    public SettlementStats getSettlementStatistics() {
        return settlementStatsService.getStatistics();
    }
}

