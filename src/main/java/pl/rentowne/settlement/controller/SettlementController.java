package pl.rentowne.settlement.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import pl.rentowne.settlement.service.SettlementService;

@RestController
@RequiredArgsConstructor
@Tag(name = "Rozliczenia")
public class SettlementController {

    private final SettlementService settlementService;

    @DeleteMapping(value = "/api/settlements/{id}")
    public ResponseEntity<Void> deleteCalculate(@PathVariable Long id) {
        settlementService.deleteCalculate(id);
        return new ResponseEntity<>(HttpStatus.OK);
    }
}
