package pl.rentowne.invoice.model.dto;

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
public class RecurringInvoiceDto {

    private String seller;
    private String invoiceNumber;
    private String apartment;
    private LocalDate settlementMonth;
    private String providerType;
    private BigDecimal amount;
}

