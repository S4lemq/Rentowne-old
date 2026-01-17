package pl.rentowne.invoice.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.rentowne.apartment.Apartment;
import pl.rentowne.housing_provider.model.HousingProvider;
import pl.rentowne.housing_provider.repository.HousingProviderRepository;
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
import java.util.Optional;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecurringInvoiceServiceImpl implements RecurringInvoiceService {

    private final HousingProviderRepository housingProviderRepository;
    private final InvoiceRepository invoiceRepository;
    private final Clock clock;

    @Override
    @Transactional
    public List<RecurringInvoiceDto> generateRecurringInvoices() {

        List<RecurringInvoiceDto> generatedInvoices = new ArrayList<>();
        LocalDate currentMonth = clock.currentMonthFirstDay();

        // Pobierz wszystkich dostawców z recurringPayment = true wraz z polami
        Set<HousingProvider> providers = housingProviderRepository.findProvidersWithRecurringPaymentAndProviderFields();

        // Doładuj mieszkania dla tych dostawców w osobnym zapytaniu
        if (!providers.isEmpty()) {
            providers = housingProviderRepository.fetchApartmentsForProviders(providers);
        }

        log.info("Znaleziono {} dostawców z płatnościami cyklicznymi", providers.size());

        for (HousingProvider provider : providers) {
            log.debug("Przetwarzam dostawcę: {} (ID: {})", provider.getName(), provider.getId());

            // Oblicz kwotę faktury - suma wszystkich price z ProviderField
            BigDecimal amount = provider.getProviderFields().stream()
                    .map(ProviderField::getPrice)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // Iteruj po wszystkich mieszkaniach przypisanych do dostawcy
            for (Apartment apartment : provider.getApartments()) {
                try {
                    // Sprawdź czy istnieje już faktura dla bieżącego miesiąca
                    boolean invoiceExists = invoiceRepository.existsByApartmentAndProviderForCurrentMonth(
                            apartment.getId(), provider.getId(), currentMonth
                    );

                    if (invoiceExists) {
                        log.debug("Faktura dla mieszkania {} i dostawcy {} już istnieje dla bieżącego miesiąca",
                                apartment.getApartmentName(), provider.getName());
                        continue;
                    }

                    // Sprawdź czy są jakieś faktury dla tego mieszkania i dostawcy
                    Optional<Invoice> latestInvoiceOpt = invoiceRepository.findLatestByApartmentAndProvider(
                            apartment.getId(), provider.getId()
                    );

                    boolean shouldGenerate = false;

                    if (latestInvoiceOpt.isEmpty()) {
                        // Brak faktur w systemie - generuj nową
                        log.debug("Brak faktur dla mieszkania {} i dostawcy {} - generuję nową",
                                apartment.getApartmentName(), provider.getName());
                        shouldGenerate = true;
                    } else {
                        // Sprawdź czy payDayMonth jest starszy niż aktualny miesiąc
                        Invoice latestInvoice = latestInvoiceOpt.get();
                        if (latestInvoice.getPayDayMonth() != null &&
                                latestInvoice.getPayDayMonth().isBefore(currentMonth)) {
                            log.debug("Ostatnia faktura dla mieszkania {} i dostawcy {} ma payDayMonth starszy niż bieżący miesiąc - generuję nową",
                                    apartment.getApartmentName(), provider.getName());
                            shouldGenerate = true;
                        }
                    }

                    if (shouldGenerate) {
                        RecurringInvoiceDto invoiceDto = generateInvoice(provider, apartment, amount, currentMonth);
                        generatedInvoices.add(invoiceDto);
                        log.info("Wygenerowano fakturę: {} dla mieszkania: {}",
                                invoiceDto.getInvoiceNumber(), apartment.getApartmentName());
                    }

                } catch (Exception e) {
                    log.error("Błąd podczas generowania faktury dla mieszkania {} i dostawcy {}",
                            apartment.getApartmentName(), provider.getName(), e);
                }
            }
        }

        log.info("Zakończono generowanie faktur. Wygenerowano {} faktur", generatedInvoices.size());
        return generatedInvoices;
    }

    private RecurringInvoiceDto generateInvoice(HousingProvider provider, Apartment apartment,
                                                 BigDecimal amount, LocalDate currentMonth) {
        // Generuj numer faktury (format: FV/RRRRMM/ID_DOSTAWCY/ID_MIESZKANIA)
        String invoiceNumber = generateInvoiceNumber(provider.getId(), apartment.getId(), currentMonth);

        // Ustaw daty
        LocalDate dateFrom = currentMonth.withDayOfMonth(1);
        LocalDate dateTo = currentMonth.withDayOfMonth(currentMonth.lengthOfMonth());
        LocalDate payDay = currentMonth.withDayOfMonth(15); // 15 dzień miesiąca

        // Zapisz fakturę do bazy
        Invoice invoice = Invoice.builder()
                .housingProvider(provider)
                .apartment(apartment)
                .invoiceNumber(invoiceNumber)
                .month(currentMonth)
                .dateFrom(dateFrom)
                .dateTo(dateTo)
                .amount(amount)
                .payDay(payDay)
                .payDayMonth(currentMonth)
                .build();

        Invoice savedInvoice = invoiceRepository.save(invoice);

        // Zwróć DTO z informacjami o fakturze
        return RecurringInvoiceDto.builder()
                .seller(provider.getName())
                .invoiceNumber(savedInvoice.getInvoiceNumber())
                .apartment(apartment.getApartmentName())
                .settlementMonth(currentMonth)
                .providerType(provider.getType().name())
                .amount(amount)
                .build();
    }

    private String generateInvoiceNumber(Long providerId, Long apartmentId, LocalDate month) {
        // Format: FV/RRRRMM/ID_DOSTAWCY/ID_MIESZKANIA
        String yearMonth = String.format("%d%02d", month.getYear(), month.getMonthValue());
        return String.format("FV/%s/%d/%d", yearMonth, providerId, apartmentId);
    }
}
