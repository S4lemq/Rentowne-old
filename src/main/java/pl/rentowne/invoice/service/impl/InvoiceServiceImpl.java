package pl.rentowne.invoice.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.rentowne.apartment.model.Apartment;
import pl.rentowne.exception.RentowneBusinessException;
import pl.rentowne.exception.RentowneErrorCode;
import pl.rentowne.housing_provider.model.HousingProvider;
import pl.rentowne.invoice.mapper.InvoiceMapper;
import pl.rentowne.invoice.model.Invoice;
import pl.rentowne.invoice.model.InvoiceFilterType;
import pl.rentowne.invoice.model.dto.InvoiceDto;
import pl.rentowne.invoice.model.dto.InvoiceFilterDto;
import pl.rentowne.invoice.model.dto.InvoiceFilterSummaryDto;
import pl.rentowne.invoice.model.dto.InvoiceListDto;
import pl.rentowne.invoice.model.dto.InvoiceSummaryDto;
import pl.rentowne.invoice.repository.InvoiceRepository;
import pl.rentowne.invoice.service.InvoiceService;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class InvoiceServiceImpl implements InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final InvoiceMapper invoiceMapper;

    @Override
    @Transactional
    public Long addInvoice(InvoiceDto dto) {

        Invoice invoice = invoiceMapper.toEntity(dto);
        Invoice savedInvoice = invoiceRepository.save(invoice);

        return savedInvoice.getId();
    }

    @Override
    @Transactional(readOnly = true)
    public InvoiceDto getInvoiceById(Long id) {

        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new RentowneBusinessException(RentowneErrorCode.INVOICE_NOT_FOUND));

        return invoiceMapper.toDto(invoice);
    }

    @Override
    @Transactional
    public void updateInvoice(InvoiceDto dto) {

        Invoice existingInvoice = invoiceRepository.findById(dto.getId())
                .orElseThrow(() -> new RentowneBusinessException(RentowneErrorCode.INVOICE_NOT_FOUND));

        if (dto.getHousingProviderId() != null) {
            existingInvoice.setHousingProvider(
                    HousingProvider.builder()
                            .id(dto.getHousingProviderId())
                            .build()
            );
        } else {
            existingInvoice.setHousingProvider(null);
        }

        if (dto.getApartmentId() != null) {
            existingInvoice.setApartment(
                    Apartment.builder()
                            .id(dto.getApartmentId())
                            .build()
            );
        } else {
            existingInvoice.setApartment(null);
        }

        existingInvoice.setInvoiceNumber(dto.getInvoiceNumber());
        existingInvoice.setMonth(dto.getMonth());
        existingInvoice.setDateFrom(dto.getDateFrom());
        existingInvoice.setDateTo(dto.getDateTo());
        existingInvoice.setAmount(dto.getAmount());
        existingInvoice.setPayDay(dto.getPayDay());
        existingInvoice.setConsumption(dto.getConsumption());
        existingInvoice.setMeterValue(dto.getMeterValue());
        existingInvoice.setPaymentDate(dto.getPaymentDate());
        existingInvoice.setPayDayMonth(dto.getPayDayMonth());

        invoiceRepository.save(existingInvoice);
    }

    @Override
    @Transactional
    public void deleteInvoice(Long id) {

        if (!invoiceRepository.existsById(id)) {
            throw new RentowneBusinessException(RentowneErrorCode.INVOICE_NOT_FOUND);
        }

        invoiceRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<InvoiceListDto> getInvoices(Pageable pageable, InvoiceFilterType filterType, final InvoiceFilterDto invoiceFilterDto) {
        Page<Invoice> invoicesPage;

        if (filterType == null) {
            invoicesPage = invoiceRepository.findAllWithRelations(pageable, invoiceFilterDto);
        } else {
            LocalDate currentDate = LocalDate.now();
            int currentYear = currentDate.getYear();
            int currentMonth = currentDate.getMonthValue();

            invoicesPage = switch (filterType) {
                case PAID_THIS_MONTH -> invoiceRepository.findPaidInMonth(currentYear, currentMonth, pageable);
                case UPCOMING_PAYMENTS -> invoiceRepository.findUpcomingPayments(currentDate, pageable);
                case OVERDUE_PAYMENTS -> invoiceRepository.findOverduePayments(currentDate, pageable);
                case TO_SETTLE_THIS_MONTH -> invoiceRepository.findToSettleInMonth(currentYear, currentMonth, pageable);
            };
        }

        return invoicesPage.map(invoiceMapper::toListDto);
    }

    @Override
    @Transactional(readOnly = true)
    public InvoiceSummaryDto getInvoiceSummary() {
        LocalDate currentDate = LocalDate.now();
        int currentYear = currentDate.getYear();
        int currentMonth = currentDate.getMonthValue();

        return InvoiceSummaryDto.builder()
                .paidThisMonth(invoiceRepository.sumPaidInMonth(currentYear, currentMonth))
                .upcomingPayments(invoiceRepository.sumUpcomingPayments(currentDate))
                .overduePayments(invoiceRepository.sumOverduePayments(currentDate))
                .toSettleThisMonth(invoiceRepository.sumToSettleInMonth(currentYear, currentMonth))
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public InvoiceFilterSummaryDto getInvoiceSummaryByFilters(InvoiceFilterDto invoiceFilterDto) {
        return InvoiceFilterSummaryDto.builder()
                .totalAmount(invoiceRepository.sumAmountByFilters(invoiceFilterDto))
                .totalConsumption(invoiceRepository.sumConsumptionByFilters(invoiceFilterDto))
                .build();
    }
}
