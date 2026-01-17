package pl.rentowne.invoice.mapper;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import pl.rentowne.apartment.Apartment;
import pl.rentowne.housing_provider.model.HousingProvider;
import pl.rentowne.invoice.model.Invoice;
import pl.rentowne.invoice.model.dto.InvoiceDto;
import pl.rentowne.invoice.model.dto.InvoiceListDto;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
@RequiredArgsConstructor
public class InvoiceMapper {

    public Invoice toEntity(InvoiceDto dto) {
        return Invoice.builder()
                .id(dto.getId())
                .housingProvider(dto.getHousingProviderId() != null
                        ? HousingProvider.builder().id(dto.getHousingProviderId()).build()
                        : null)
                .invoiceNumber(dto.getInvoiceNumber())
                .apartment(dto.getApartmentId() != null
                        ? Apartment.builder().id(dto.getApartmentId()).build()
                        : null)
                .month(dto.getMonth())
                .dateFrom(dto.getDateFrom())
                .dateTo(dto.getDateTo())
                .amount(dto.getAmount())
                .payDay(dto.getPayDay())
                .consumption(dto.getConsumption())
                .meterValue(dto.getMeterValue())
                .paymentDate(dto.getPaymentDate())
                .payDayMonth(dto.getPayDayMonth())
                .pdfFileName(dto.getPdfFileName())
                .build();
    }

    public InvoiceDto toDto(Invoice entity) {
        return InvoiceDto.builder()
                .id(entity.getId())
                .housingProviderId(entity.getHousingProvider() != null
                        ? entity.getHousingProvider().getId()
                        : null)
                .providerType(entity.getHousingProvider() != null
                        ? entity.getHousingProvider().getType()
                        : null)
                .invoiceNumber(entity.getInvoiceNumber())
                .apartmentId(entity.getApartment() != null
                        ? entity.getApartment().getId()
                        : null)
                .month(entity.getMonth())
                .dateFrom(entity.getDateFrom())
                .dateTo(entity.getDateTo())
                .amount(entity.getAmount())
                .payDay(entity.getPayDay())
                .consumption(entity.getConsumption())
                .meterValue(entity.getMeterValue())
                .paymentDate(entity.getPaymentDate())
                .payDayMonth(entity.getPayDayMonth())
                .pdfFileName(entity.getPdfFileName())
                .build();
    }

    public InvoiceListDto toListDto(Invoice entity) {
        BigDecimal unitPrice = null;
        if (entity.getAmount() != null && entity.getConsumption() != null
                && entity.getConsumption().compareTo(BigDecimal.ZERO) != 0) {
            unitPrice = entity.getAmount().divide(entity.getConsumption(), 4, RoundingMode.HALF_UP);
        }

        return InvoiceListDto.builder()
                .id(entity.getId())
                .sellerName(entity.getHousingProvider() != null
                        ? entity.getHousingProvider().getName()
                        : null)
                .invoiceNumber(entity.getInvoiceNumber())
                .localName(entity.getApartment() != null
                        ? entity.getApartment().getApartmentName()
                        : null)
                .month(entity.getMonth())
                .dateFrom(entity.getDateFrom())
                .dateTo(entity.getDateTo())
                .amount(entity.getAmount())
                .payDay(entity.getPayDay())
                .consumption(entity.getConsumption())
                .unitPrice(unitPrice)
                .meterValue(entity.getMeterValue())
                .paymentDate(entity.getPaymentDate())
                .payDayMonth(entity.getPayDayMonth())
                .category(entity.getHousingProvider() != null
                        ? entity.getHousingProvider().getType()
                        : null)
                .hasFile(entity.getPdfFileName() != null && !entity.getPdfFileName().trim().isEmpty())
                .build();
    }
}
