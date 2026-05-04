package pl.rentowne.invoice;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class RecurringInvoiceDto {

    private String seller;
    private String invoiceNumber;
    private String apartment;
    private LocalDate settlementMonth;
    private String providerType;
    private BigDecimal amount;
}

