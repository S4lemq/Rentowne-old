package pl.rentowne.rental_document;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import lombok.RequiredArgsConstructor;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/rental-documents")
@RequiredArgsConstructor
@Validated
class RentalDocumentController {
    private final DocumentManagementService service;

    record Revision(@Min(0) long revision) {}

    @GetMapping
    DocumentPage list(
            @RequestParam(defaultValue = "ACTIVE") String tab,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long apartmentId,
            @RequestParam(required = false) String mailState,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate to,
            @RequestParam(required = false) Long settlementId,
            @RequestParam(required = false) Long leaseId,
            @RequestParam(required = false) Long tenantId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return service.list(
                tab,
                type,
                search,
                apartmentId,
                mailState,
                from,
                to,
                settlementId,
                leaseId,
                tenantId,
                page,
                size);
    }

    @GetMapping("/apartments")
    List<DocumentApartment> apartments() {
        return service.apartments();
    }

    @GetMapping("/{id}")
    DocumentDetail detail(@PathVariable UUID id) {
        return service.detail(id);
    }

    @GetMapping("/{id}/pdf")
    ResponseEntity<byte[]> pdf(
            @PathVariable UUID id, @RequestParam(defaultValue = "false") boolean download) {
        var detail = service.detail(id);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .cacheControl(CacheControl.noStore())
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        (download ? ContentDisposition.attachment() : ContentDisposition.inline())
                                .filename(
                                        DocumentMapper.filename(detail.data(), detail.cancelled()),
                                        StandardCharsets.UTF_8)
                                .build()
                                .toString())
                .body(service.bytes(id));
    }

    @PostMapping("/{id}/regenerate")
    DocumentDetail regenerate(@PathVariable UUID id, @Valid @RequestBody Revision request) {
        return service.regenerate(id, request.revision());
    }

    @DeleteMapping("/{id}/pdf")
    DocumentDetail delete(@PathVariable UUID id, @RequestParam @Min(0) long revision) {
        return service.deletePdf(id, revision);
    }

    @PostMapping("/{id}/send")
    DocumentDetail send(@PathVariable UUID id, @Valid @RequestBody DocumentSendRequest request) {
        return service.send(id, request);
    }
}
