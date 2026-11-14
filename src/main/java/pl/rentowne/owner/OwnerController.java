package pl.rentowne.owner;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@Tag(name = "Właściciele nieruchomości")
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
class OwnerController {
    private final OwnerService ownerService;

    @GetMapping("/api/owners")
    @Operation(summary = "Pobiera właścicieli do wyboru w edycji nieruchomości")
    List<OwnerDto> getOwners() {
        return ownerService.getOwners();
    }
}
