package pl.rentowne.invoice.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.rentowne.apartment.Apartment;
import pl.rentowne.housing_provider.HousingProvider;
import pl.rentowne.housing_provider.HousingProviderRepository;
import pl.rentowne.invoice.model.Invoice;
import pl.rentowne.invoice.model.dto.RecurringInvoiceDto;
import pl.rentowne.invoice.repository.InvoiceRepository;
import pl.rentowne.invoice.service.RecurringInvoiceService;
import pl.rentowne.provider_field.model.ProviderField;
import pl.rentowne.util.Clock;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.util.stream.Collectors.toMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecurringInvoiceServiceImpl implements RecurringInvoiceService {

    private static final int PAY_DAY_OF_MONTH = 15;

    private final HousingProviderRepository housingProviderRepository;
    private final InvoiceRepository invoiceRepository;
    private final Clock clock;

    @Override
    @Transactional
    public List<RecurringInvoiceDto> generateRecurringInvoices() {
        final LocalDate currentMonth = clock.currentMonthFirstDay();

        final Set<HousingProvider> providers = loadRecurringProvidersWithFieldsAndApartments();
        if (providers.isEmpty()) {
            log.info("Brak dostawców z płatnościami cyklicznymi.");
            return List.of();
        }

        log.info("Znaleziono {} dostawców z płatnościami cyklicznymi", providers.size());

        final Set<Long> providerIds = providers.stream()
                .map(HousingProvider::getId)
                .collect(java.util.stream.Collectors.toSet());

        final Set<Long> apartmentIds = providers.stream()
                .flatMap(p -> p.getApartments().stream())
                .map(Apartment::getId)
                .collect(java.util.stream.Collectors.toSet());

        if (apartmentIds.isEmpty()) {
            log.info("Dostawcy cykliczni nie mają przypisanych mieszkań. Kończę.");
            return List.of();
        }

        final var lastPayDayMonthByPair = loadLastPayDayMonthMap(providerIds, apartmentIds);

        final List<Invoice> invoicesToSave = new ArrayList<>();
        final List<RecurringInvoiceDto> result = new ArrayList<>();

        for (final HousingProvider provider : providers) {
            final BigDecimal amount = calculateAmount(provider);

            for (final Apartment apartment : provider.getApartments()) {
                if (!shouldGenerate(currentMonth, lastPayDayMonthByPair, provider.getId(), apartment.getId())) {
                    continue;
                }

                final Invoice invoice = buildInvoice(provider, apartment, amount, currentMonth);
                invoicesToSave.add(invoice);

                // DTO budujemy już teraz (invoiceNumber znamy), a nie po save()
                result.add(toDto(provider, apartment, amount, currentMonth, invoice.getInvoiceNumber()));
            }
        }

        if (invoicesToSave.isEmpty()) {
            log.info("Brak faktur do wygenerowania dla miesiąca {}", currentMonth);
            return List.of();
        }

        invoiceRepository.saveAll(invoicesToSave);

        log.info("Zakończono generowanie faktur. Wygenerowano {} faktur.", result.size());
        return result;
    }

    private Set<HousingProvider> loadRecurringProvidersWithFieldsAndApartments() {
        final Set<HousingProvider> providers = housingProviderRepository.findProvidersWithRecurringPaymentAndProviderFields();
        if (providers.isEmpty()) {
            return providers;
        }
        return housingProviderRepository.fetchApartmentsForProviders(providers);
    }

    private Map<InvoiceKey, LocalDate> loadLastPayDayMonthMap(final Set<Long> providerIds, final Set<Long> apartmentIds) {
        return invoiceRepository.findLastPayDayMonthByProviderAndApartment(providerIds, apartmentIds).stream()
                .collect(toMap(
                        row -> new InvoiceKey(row.getHousingProviderId(), row.getApartmentId()),
                        InvoiceRepository.LastPayDayMonthRow::getLastPayDayMonth
                ));
    }

    private boolean shouldGenerate(final LocalDate currentMonth,
                                   final Map<InvoiceKey, LocalDate> lastPayDayMonthByPair,
                                   final Long providerId,
                                   final Long apartmentId) {
        final LocalDate last = lastPayDayMonthByPair.get(new InvoiceKey(providerId, apartmentId));

        // brak rekordu => nigdy nie było faktury => generuj
        if (last == null) {
            return true;
        }
        return last.isBefore(currentMonth);
    }

    private BigDecimal calculateAmount(final HousingProvider provider) {
        // zabezpieczenie na null listy lub null price
        if (provider.getProviderFields() == null || provider.getProviderFields().isEmpty()) {
            return BigDecimal.ZERO;
        }
        return provider.getProviderFields().stream()
                .map(ProviderField::getPrice)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private Invoice buildInvoice(final HousingProvider provider,
                                 final Apartment apartment,
                                 final BigDecimal amount,
                                 final LocalDate currentMonth) {

        final String invoiceNumber = generateInvoiceNumber(provider.getId(), apartment.getId(), currentMonth);

        final LocalDate dateTo = currentMonth.withDayOfMonth(currentMonth.lengthOfMonth());
        final LocalDate payDay = currentMonth.withDayOfMonth(PAY_DAY_OF_MONTH);

        return Invoice.builder()
                .housingProvider(provider)
                .apartment(apartment)
                .invoiceNumber(invoiceNumber)
                .month(currentMonth)
                .dateFrom(currentMonth)
                .dateTo(dateTo)
                .amount(amount)
                .payDay(payDay)
                .payDayMonth(currentMonth)
                .build();
    }

    private RecurringInvoiceDto toDto(final HousingProvider provider,
                                      final Apartment apartment,
                                      final BigDecimal amount,
                                      final LocalDate currentMonth,
                                      final String invoiceNumber) {
        return RecurringInvoiceDto.builder()
                .seller(provider.getName())
                .invoiceNumber(invoiceNumber)
                .apartment(apartment.getApartmentName())
                .settlementMonth(currentMonth)
                .providerType(provider.getType().name())
                .amount(amount)
                .build();
    }

    private String generateInvoiceNumber(final Long providerId, final Long apartmentId, final LocalDate monthStart) {
        // FV/yyyyMM/providerId/apartmentId
        final String yearMonth = java.time.YearMonth.from(monthStart).format(java.time.format.DateTimeFormatter.ofPattern("yyyyMM"));
        return "FV/%s/%d/%d".formatted(yearMonth, providerId, apartmentId);
    }

    private record InvoiceKey(Long providerId, Long apartmentId) { }
}
