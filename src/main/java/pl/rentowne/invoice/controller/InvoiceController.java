package pl.rentowne.invoice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pl.rentowne.invoice.model.InvoiceFilterType;
import pl.rentowne.invoice.model.dto.InvoiceDto;
import pl.rentowne.invoice.model.dto.InvoiceFilterDto;
import pl.rentowne.invoice.model.dto.InvoiceFilterSummaryDto;
import pl.rentowne.invoice.model.dto.InvoiceListDto;
import pl.rentowne.invoice.model.dto.InvoiceSummaryDto;
import pl.rentowne.invoice.service.InvoiceService;

@RestController
@RequiredArgsConstructor
@Tag(name = "Faktury")
public class InvoiceController {

    private final InvoiceService invoiceService;

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
    public ResponseEntity<Long> addInvoice(@RequestBody InvoiceDto dto) {
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
    public ResponseEntity<InvoiceDto> getInvoice(@PathVariable Long id) {
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
    public ResponseEntity<Void> updateInvoice(@RequestBody InvoiceDto dto) {
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
    public ResponseEntity<Void> deleteInvoice(@PathVariable Long id) {
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
    public ResponseEntity<Page<InvoiceListDto>> getInvoices(
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
    public ResponseEntity<InvoiceSummaryDto> getInvoiceSummary() {
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
    public ResponseEntity<InvoiceFilterSummaryDto> getInvoiceFilterSummary(InvoiceFilterDto invoiceFilterDto) {
        return ResponseEntity.ok(invoiceService.getInvoiceSummaryByFilters(invoiceFilterDto));
    }
}
