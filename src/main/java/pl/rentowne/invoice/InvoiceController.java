package pl.rentowne.invoice;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import pl.rentowne.exception.RentowneBusinessException;
import pl.rentowne.exception.RentowneErrorCode;
import pl.rentowne.user.model.dto.UploadResponse;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@RestController
@RequiredArgsConstructor
@Tag(name = "Faktury")
class InvoiceController {

    private final InvoiceService invoiceService;
    private final InvoicePdfService invoicePdfService;
    private final RecurringInvoiceService recurringInvoiceService;

    @Operation(
            summary = "Dodaje nową fakturę",
            description = "Metoda służy do dodawania nowej faktury do systemu",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Fakturę dodano pomyślnie"),
                    @ApiResponse(responseCode = "400", description = "Niepoprawne dane wejściowe"),
                    @ApiResponse(responseCode = "404", description = "Nie znaleziono danych"),
                    @ApiResponse(responseCode = "500", description = "Wewnętrzny błąd serwera")
            }
    )
    @PostMapping("/api/invoices")
    ResponseEntity<Long> addInvoice(@RequestBody InvoiceDto dto) {
        return new ResponseEntity<>(invoiceService.addInvoice(dto), HttpStatus.CREATED);
    }

    @Operation(
            summary = "Pobiera fakturę po id",
            description = "Metoda służy do pobrania faktury po identyfikatorze",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Fakturę pobrano pomyślnie"),
                    @ApiResponse(responseCode = "400", description = "Niepoprawne dane wejściowe"),
                    @ApiResponse(responseCode = "404", description = "Nie znaleziono faktury"),
                    @ApiResponse(responseCode = "500", description = "Wewnętrzny błąd serwera")
            }
    )
    @GetMapping("/api/invoices/{id}")
    ResponseEntity<InvoiceDto> getInvoice(@PathVariable Long id) {
        return new ResponseEntity<>(invoiceService.getInvoiceById(id), HttpStatus.OK);
    }

    @Operation(
            summary = "Aktualizuje dane faktury",
            description = "Metoda służy do aktualizowania danych istniejącej faktury",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Dane faktury zaktualizowano pomyślnie"),
                    @ApiResponse(responseCode = "400", description = "Niepoprawne dane wejściowe"),
                    @ApiResponse(responseCode = "404", description = "Nie znaleziono faktury"),
                    @ApiResponse(responseCode = "500", description = "Wewnętrzny błąd serwera")
            }
    )
    @PutMapping("/api/invoices")
    ResponseEntity<Void> updateInvoice(@RequestBody InvoiceDto dto) {
        invoiceService.updateInvoice(dto);
        return ResponseEntity.ok().build();
    }

    @Operation(
            summary = "Usuwa fakturę",
            description = "Metoda służy do usuwania faktury z systemu",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Fakturę usunięto pomyślnie"),
                    @ApiResponse(responseCode = "404", description = "Nie znaleziono faktury"),
                    @ApiResponse(responseCode = "500", description = "Wewnętrzny błąd serwera")
            }
    )
    @DeleteMapping("/api/invoices/{id}")
    ResponseEntity<Void> deleteInvoice(@PathVariable Long id) {
        invoiceService.deleteInvoice(id);
        return ResponseEntity.ok().build();
    }

    @Operation(
            summary = "Pobiera paginowaną listę faktur",
            description = "Metoda służy do pobrania listy faktur z możliwością paginacji i sortowania po wszystkich kolumnach",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Lista faktur została pobrana pomyślnie"),
                    @ApiResponse(responseCode = "400", description = "Niepoprawne dane wejściowe"),
                    @ApiResponse(responseCode = "500", description = "Wewnętrzny błąd serwera")
            }
    )
    @GetMapping("/api/invoices")
    ResponseEntity<Page<InvoiceListDto>> getInvoices(
            @PageableDefault(size = 20, sort = "id") Pageable pageable,
            @RequestParam(required = false) String filter,
            InvoiceFilterDto invoiceFilterDto) {
        InvoiceFilterType filterType = InvoiceFilterType.fromString(filter);
        return new ResponseEntity<>(invoiceService.getInvoices(pageable, filterType, invoiceFilterDto), HttpStatus.OK);
    }

    @Operation(
            summary = "Pobiera podsumowanie faktur",
            description = "Metoda służy do pobrania podsumowania zawierającego sumy płatności w różnych kategoriach",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Podsumowanie zostało pobrane pomyślnie"),
                    @ApiResponse(responseCode = "500", description = "Wewnętrzny błąd serwera")
            }
    )
    @GetMapping("/api/invoices/summary")
    ResponseEntity<InvoiceSummaryDto> getInvoiceSummary() {
        return ResponseEntity.ok(invoiceService.getInvoiceSummary());
    }

    @Operation(
            summary = "Pobiera sumy amount i consumption na podstawie filtrów",
            description = "Metoda służy do pobrania sum amount i consumption dla faktur pasujących do podanych filtrów",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Sumy zostały pobrane pomyślnie"),
                    @ApiResponse(responseCode = "500", description = "Wewnętrzny błąd serwera")
            }
    )
    @GetMapping("/api/invoices/filter-summary")
    ResponseEntity<InvoiceFilterSummaryDto> getInvoiceFilterSummary(InvoiceFilterDto invoiceFilterDto) {
        return ResponseEntity.ok(invoiceService.getInvoiceSummaryByFilters(invoiceFilterDto));
    }

    @Operation(
            summary = "Wgrywa plik PDF faktury",
            description = "Metoda służy do wgrywania pliku PDF faktury. " +
                    "Jeśli podano invoiceId, plik jest automatycznie przypisywany do faktury. " +
                    "Jeśli nie podano invoiceId, plik jest tylko wgrywany i może być później przypisany przy tworzeniu faktury.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Plik PDF został wgrany pomyślnie"),
                    @ApiResponse(responseCode = "400", description = "Niepoprawny plik"),
                    @ApiResponse(responseCode = "404", description = "Nie znaleziono faktury (jeśli podano invoiceId)"),
                    @ApiResponse(responseCode = "500", description = "Wewnętrzny błąd serwera")
            }
    )
    @PostMapping("/api/invoices/upload-pdf")
    ResponseEntity<UploadResponse> uploadInvoicePdf(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "invoiceId", required = false) Long invoiceId) {
        if (file.isEmpty()) {
            throw new RentowneBusinessException(RentowneErrorCode.FILE_UPLOAD_ERROR);
        }

        // Sprawdź czy to PDF
        String contentType = file.getContentType();
        if (contentType == null || !contentType.equals("application/pdf")) {
            throw new RentowneBusinessException(RentowneErrorCode.INVALID_FILE_TYPE);
        }

        try (InputStream inputStream = file.getInputStream()) {
            String savedFileName;

            if (invoiceId != null) {
                // Wgraj i przypisz do istniejącej faktury
                savedFileName = invoiceService.uploadAndAttachPdf(invoiceId, file.getOriginalFilename(), inputStream);
            } else {
                // Tylko wgraj plik bez przypisania
                savedFileName = invoiceService.uploadPdfOnly(file.getOriginalFilename(), inputStream);
            }

            return ResponseEntity.ok(new UploadResponse(savedFileName));
        } catch (IOException e) {
            throw new RuntimeException("Błąd podczas wgrywania pliku PDF faktury", e);
        }
    }

    @Operation(
            summary = "Pobiera plik PDF faktury",
            description = "Metoda służy do pobrania pliku PDF faktury po nazwie pliku",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Plik PDF został pobrany pomyślnie"),
                    @ApiResponse(responseCode = "404", description = "Nie znaleziono pliku"),
                    @ApiResponse(responseCode = "500", description = "Wewnętrzny błąd serwera")
            }
    )
    @GetMapping("/api/invoices/pdf/download/{filename}")
    ResponseEntity<Resource> getInvoicePdf(
            @PathVariable String filename) {
        Resource file = invoicePdfService.getInvoicePdf(filename);

        if (!file.exists()) {
            throw new RentowneBusinessException(RentowneErrorCode.FILE_NOT_FOUND);
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_PDF_VALUE)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=31536000")
                .body(file);
    }

    @Operation(
            summary = "Usuwa plik PDF faktury",
            description = "Metoda służy do usuwania pliku PDF faktury",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Plik PDF został usunięty pomyślnie"),
                    @ApiResponse(responseCode = "404", description = "Nie znaleziono pliku"),
                    @ApiResponse(responseCode = "500", description = "Wewnętrzny błąd serwera")
            }
    )
    @DeleteMapping("/api/invoices/pdf/{filename}")
    ResponseEntity<Void> deleteInvoicePdf(@PathVariable String filename) {
        invoicePdfService.deleteInvoicePdf(filename);
        return ResponseEntity.ok().build();
    }

    @Operation(
            summary = "Generuje faktury cykliczne",
            description = "Metoda służy do automatycznego generowania faktur dla dostawców z flagą recurringPayment = true. " +
                    "Generuje faktury dla mieszkań przypisanych do dostawców, jeśli nie istnieje już faktura dla bieżącego miesiąca.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Faktury zostały wygenerowane pomyślnie"),
                    @ApiResponse(responseCode = "500", description = "Wewnętrzny błąd serwera")
            }
    )
    @PostMapping("/api/invoices/generate-recurring")
    ResponseEntity<List<RecurringInvoiceDto>> generateRecurringInvoices() {
        List<RecurringInvoiceDto> generatedInvoices = recurringInvoiceService.generateRecurringInvoices();
        return ResponseEntity.ok(generatedInvoices);
    }
}
