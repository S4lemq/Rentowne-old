package pl.rentowne.pdf_template.dto;

import lombok.Builder;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * DTO z danymi do szablonu faktury.
 */
@Builder
public record InvoiceTemplateDto(
        @NonNull String invoiceNumber,
        @NonNull LocalDate issueDate,
        @NonNull LocalDate saleDate,
        @NonNull LocalDate dueDate,
        @NonNull String paymentMethod,
        @NonNull SellerDto seller,
        @NonNull BuyerDto buyer,
        @NonNull List<InvoiceItemDto> items,
        @Nullable String notes
) {

    /**
     * Dane sprzedawcy.
     */
    @Builder
    public record SellerDto(
            @NonNull String name,
            @NonNull String addressLine1,
            @Nullable String addressLine2,
            @NonNull String postalCode,
            @NonNull String city,
            @NonNull String nip,
            @Nullable String phone,
            @Nullable String email,
            @Nullable String bankName,
            @Nullable String bankAccount
    ) {}

    /**
     * Dane nabywcy.
     */
    @Builder
    public record BuyerDto(
            @NonNull String name,
            @NonNull String addressLine1,
            @Nullable String addressLine2,
            @NonNull String postalCode,
            @NonNull String city,
            @Nullable String nip
    ) {}

    /**
     * Pozycja faktury.
     */
    @Builder
    public record InvoiceItemDto(
            int ordinalNumber,
            @NonNull String name,
            @NonNull String unit,
            @NonNull BigDecimal quantity,
            @NonNull BigDecimal unitPriceNet,
            int vatRate,
            @NonNull BigDecimal netAmount,
            @NonNull BigDecimal vatAmount,
            @NonNull BigDecimal grossAmount
    ) {}

    /**
     * Oblicza sumę netto wszystkich pozycji.
     */
    public BigDecimal getTotalNet() {
        return items.stream()
                .map(InvoiceItemDto::netAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Oblicza sumę VAT wszystkich pozycji.
     */
    public BigDecimal getTotalVat() {
        return items.stream()
                .map(InvoiceItemDto::vatAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Oblicza sumę brutto wszystkich pozycji.
     */
    public BigDecimal getTotalGross() {
        return items.stream()
                .map(InvoiceItemDto::grossAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}

