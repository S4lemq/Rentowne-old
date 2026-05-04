package pl.rentowne.invoice;

import lombok.Builder;
import lombok.Getter;
import pl.rentowne.housing_provider.ProviderType;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Builder
class InvoiceListDto {
    private Long id;
    private String sellerName;
    private String invoiceNumber;
    private String localName;
    private LocalDate month;
    private LocalDate dateFrom;
    private LocalDate dateTo;
    private BigDecimal amount;
    private LocalDate payDay;
    private BigDecimal consumption;
    private BigDecimal unitPrice;
    private BigDecimal meterValue;
    private LocalDate paymentDate;
    private LocalDate payDayMonth;
    private ProviderType category;
    private boolean hasFile;
}
