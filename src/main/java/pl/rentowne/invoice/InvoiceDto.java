package pl.rentowne.invoice;

import lombok.Builder;
import lombok.Getter;
import pl.rentowne.housing_provider.ProviderType;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Builder
class InvoiceDto {
    private Long id;
    private Long housingProviderId;
    private ProviderType providerType;
    private String invoiceNumber;
    private Long apartmentId;
    private LocalDate month;
    private LocalDate dateFrom;
    private LocalDate dateTo;
    private BigDecimal amount;
    private LocalDate payDay;
    private BigDecimal consumption;
    private BigDecimal meterValue;
    private LocalDate paymentDate;
    private LocalDate payDayMonth;
    private String pdfFileName;
}
