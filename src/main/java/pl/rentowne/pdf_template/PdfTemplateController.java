package pl.rentowne.pdf_template;

import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pl.rentowne.apartment.model.dto.ApartmentDto;
import pl.rentowne.apartment.service.ApartmentService;
import pl.rentowne.exception.RentowneBusinessException;
import pl.rentowne.pdf_template.dto.ApartmentListTemplateDto;
import pl.rentowne.pdf_template.dto.InvoiceTemplateDto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Kontroler REST do generowania dokumentów PDF z szablonów HTML.
 */
@RestController
@RequestMapping("/api/pdf-template")
@RequiredArgsConstructor
class PdfTemplateController {

    private final PdfTemplateService pdfTemplateService;
    private final ApartmentService apartmentService;

    /**
     * Generuje przykładową fakturę PDF z szablonu HTML.
     *
     * @param filename opcjonalna nazwa pliku
     * @return dokument PDF
     */
    @GetMapping("/invoice")
    @NonNull
    ResponseEntity<ByteArrayResource> generateInvoicePdf(
            @RequestParam(defaultValue = "faktura") String filename
    ) {
        InvoiceTemplateDto invoice = createSampleInvoice();

        PdfTemplateResponse response = pdfTemplateService.generatePdf(
                "pdf/invoice-template",
                Map.of("invoice", invoice),
                filename
        );

        return PdfTemplateResponseHelper.buildResponse(response);
    }

    /**
     * Generuje podgląd faktury PDF w przeglądarce.
     *
     * @param filename opcjonalna nazwa pliku
     * @return dokument PDF do wyświetlenia inline
     */
    @GetMapping("/invoice/preview")
    @NonNull
    ResponseEntity<ByteArrayResource> previewInvoicePdf(
            @RequestParam(defaultValue = "faktura-podglad") String filename
    ) {
        InvoiceTemplateDto invoice = createSampleInvoice();

        PdfTemplateResponse response = pdfTemplateService.generatePdf(
                "pdf/invoice-template",
                Map.of("invoice", invoice),
                filename
        );

        return PdfTemplateResponseHelper.buildPreviewResponse(response);
    }

    /**
     * Generuje PDF z listą wszystkich mieszkań zalogowanego użytkownika.
     *
     * @param filename    opcjonalna nazwa pliku
     * @param authentication autentykacja użytkownika
     * @return dokument PDF
     */
    @GetMapping("/apartments")
    @NonNull
    ResponseEntity<ByteArrayResource> generateApartmentsListPdf(
            @RequestParam(defaultValue = "lista-mieszkan") String filename,
            Authentication authentication
    ) throws RentowneBusinessException {
        ApartmentListTemplateDto apartmentList = createApartmentListData(authentication);

        PdfTemplateResponse response = pdfTemplateService.generatePdf(
                "pdf/apartment-list-template",
                Map.of("apartmentList", apartmentList),
                filename
        );

        return PdfTemplateResponseHelper.buildResponse(response);
    }

    /**
     * Generuje podgląd PDF z listą mieszkań w przeglądarce.
     *
     * @param filename    opcjonalna nazwa pliku
     * @param authentication autentykacja użytkownika
     * @return dokument PDF do wyświetlenia inline
     */
    @GetMapping("/apartments/preview")
    @NonNull
    ResponseEntity<ByteArrayResource> previewApartmentsListPdf(
            @RequestParam(defaultValue = "lista-mieszkan-podglad") String filename,
            Authentication authentication
    ) throws RentowneBusinessException {
        ApartmentListTemplateDto apartmentList = createApartmentListData(authentication);

        PdfTemplateResponse response = pdfTemplateService.generatePdf(
                "pdf/apartment-list-template",
                Map.of("apartmentList", apartmentList),
                filename
        );

        return PdfTemplateResponseHelper.buildPreviewResponse(response);
    }

    /**
     * Tworzy dane do szablonu z listą mieszkań.
     */
    private ApartmentListTemplateDto createApartmentListData(Authentication authentication) throws RentowneBusinessException {
        List<ApartmentDto> apartments = apartmentService.getAllApartmentsByLoggedUser();

        List<ApartmentListTemplateDto.ApartmentItemDto> apartmentItems = new java.util.ArrayList<>();
        int ordinalNumber = 1;

        for (ApartmentDto apartment : apartments) {
            String address = formatAddress(apartment);
            String status = apartment.isRented() ? "Wynajęte" : "Dostępne";

            ApartmentListTemplateDto.ApartmentItemDto item = ApartmentListTemplateDto.ApartmentItemDto.builder()
                    .ordinalNumber(ordinalNumber++)
                    .apartmentName(apartment.getApartmentName())
                    .address(address)
                    .area(apartment.getArea() != null ? apartment.getArea() : BigDecimal.ZERO)
                    .leasesCount(apartment.getLeasesNumber() != null ? apartment.getLeasesNumber().intValue() : 0)
                    .status(status)
                    .imageName(apartment.getImage())
                    .build();

            apartmentItems.add(item);
        }

        // Obliczanie statystyk
        int totalApartments = apartments.size();
        int rentedApartments = (int) apartments.stream().filter(ApartmentDto::isRented).count();
        int availableApartments = totalApartments - rentedApartments;

        BigDecimal totalArea = apartments.stream()
                .map(ApartmentDto::getArea)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long apartmentsWithArea = apartments.stream()
                .map(ApartmentDto::getArea)
                .filter(java.util.Objects::nonNull)
                .count();

        BigDecimal averageArea = apartmentsWithArea > 0
                ? totalArea.divide(BigDecimal.valueOf(apartmentsWithArea), 2, java.math.RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        ApartmentListTemplateDto.SummaryDto summary = ApartmentListTemplateDto.SummaryDto.builder()
                .totalApartments(totalApartments)
                .rentedApartments(rentedApartments)
                .availableApartments(availableApartments)
                .totalArea(totalArea)
                .averageArea(averageArea)
                .build();

        String username = authentication != null && authentication.getName() != null
                ? authentication.getName()
                : "System";

        return ApartmentListTemplateDto.builder()
                .title("Lista Mieszkań")
                .generatedDate(LocalDate.now())
                .generatedBy(username)
                .apartments(apartmentItems)
                .summary(summary)
                .build();
    }

    /**
     * Formatuje adres z DTO mieszkania.
     */
    private String formatAddress(ApartmentDto apartment) {
        if (apartment.getAddressDto() == null) {
            return "Brak adresu";
        }

        pl.rentowne.address.model.dto.AddressDto address = apartment.getAddressDto();
        StringBuilder sb = new StringBuilder();

        if (address.getStreetName() != null && !address.getStreetName().isEmpty()) {
            sb.append(address.getStreetName());

            if (address.getBuildingNumber() != null && !address.getBuildingNumber().isEmpty()) {
                sb.append(" ").append(address.getBuildingNumber());
            }

            if (address.getApartmentNumber() != null && !address.getApartmentNumber().isEmpty()) {
                sb.append("/").append(address.getApartmentNumber());
            }
        }

        if (address.getZipCode() != null || address.getCityName() != null) {
            if (!sb.isEmpty()) {
                sb.append(", ");
            }

            if (address.getZipCode() != null) {
                sb.append(address.getZipCode()).append(" ");
            }

            if (address.getCityName() != null) {
                sb.append(address.getCityName());
            }
        }

        return !sb.isEmpty() ? sb.toString() : "Brak adresu";
    }

    /**
     * Tworzy przykładową fakturę do celów demonstracyjnych.
     */
    private InvoiceTemplateDto createSampleInvoice() {
        return InvoiceTemplateDto.builder()
                .invoiceNumber("FV/2024/12/001")
                .issueDate(LocalDate.now())
                .saleDate(LocalDate.now())
                .dueDate(LocalDate.now().plusDays(14))
                .paymentMethod("Przelew bankowy")
                .seller(InvoiceTemplateDto.SellerDto.builder()
                        .name("Rentowne Sp. z o.o.")
                        .addressLine1("ul. Przykładowa 123")
                        .postalCode("00-001")
                        .city("Warszawa")
                        .nip("1234567890")
                        .phone("+48 123 456 789")
                        .email("kontakt@rentowne.pl")
                        .bankName("Bank Przykładowy S.A.")
                        .bankAccount("PL 12 3456 7890 1234 5678 9012 3456")
                        .build())
                .buyer(InvoiceTemplateDto.BuyerDto.builder()
                        .name("Jan Kowalski")
                        .addressLine1("ul. Testowa 45/12")
                        .postalCode("01-234")
                        .city("Kraków")
                        .nip("9876543210")
                        .build())
                .items(List.of(
                        InvoiceTemplateDto.InvoiceItemDto.builder()
                                .ordinalNumber(1)
                                .name("Czynsz za mieszkanie - grudzień 2024")
                                .unit("szt.")
                                .quantity(BigDecimal.ONE)
                                .unitPriceNet(new BigDecimal("2000.00"))
                                .vatRate(23)
                                .netAmount(new BigDecimal("2000.00"))
                                .vatAmount(new BigDecimal("460.00"))
                                .grossAmount(new BigDecimal("2460.00"))
                                .build(),
                        InvoiceTemplateDto.InvoiceItemDto.builder()
                                .ordinalNumber(2)
                                .name("Opłata za media - grudzień 2024")
                                .unit("szt.")
                                .quantity(BigDecimal.ONE)
                                .unitPriceNet(new BigDecimal("350.00"))
                                .vatRate(23)
                                .netAmount(new BigDecimal("350.00"))
                                .vatAmount(new BigDecimal("80.50"))
                                .grossAmount(new BigDecimal("430.50"))
                                .build(),
                        InvoiceTemplateDto.InvoiceItemDto.builder()
                                .ordinalNumber(3)
                                .name("Miejsce parkingowe - grudzień 2024")
                                .unit("szt.")
                                .quantity(BigDecimal.ONE)
                                .unitPriceNet(new BigDecimal("150.00"))
                                .vatRate(23)
                                .netAmount(new BigDecimal("150.00"))
                                .vatAmount(new BigDecimal("34.50"))
                                .grossAmount(new BigDecimal("184.50"))
                                .build()
                ))
                .notes("Dziękujemy za terminowe płatności.")
                .build();
    }
}

