package pl.rentowne.invoice.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.AuditorAware;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.reactive.server.WebTestClient;
import pl.rentowne.IntegrationTest;
import pl.rentowne.config.TestSecurityConfig;
import pl.rentowne.config.WithMockRentowneUser;
import pl.rentowne.invoice.model.Invoice;
import pl.rentowne.invoice.model.dto.RecurringInvoiceDto;
import pl.rentowne.invoice.repository.InvoiceRepository;
import pl.rentowne.util.Clock;
import pl.rentowne.util.FixedClock;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@Sql("classpath:sql/init_data.sql")
@IntegrationTest
@Import({TestSecurityConfig.class, InvoiceControllerTest.TestClockConfig.class})
class InvoiceControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private AuditorAware<String> auditorAware;

    @Autowired
    private InvoiceRepository invoiceRepository;

    @TestConfiguration
    static class TestClockConfig {
        @Bean
        public Clock clock() {
            // Ustawiam datę na styczeń 2025
            return FixedClock.of(2025, 1);
        }
    }

    @Test
    @WithMockRentowneUser
    void shouldGenerateRecurringInvoicesSuccessfully() {
        // given
        when(auditorAware.getCurrentAuditor()).thenReturn(Optional.of("peter.lubowicz@gmail.com"));
        int initialInvoiceCount = invoiceRepository.findAll().size();

        // when
        List<RecurringInvoiceDto> response = webTestClient.post()
                .uri("/api/invoices/generate-recurring")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(RecurringInvoiceDto.class)
                .returnResult()
                .getResponseBody();

        // then
        assertThat(response).isNotNull();
        int finalInvoiceCount = invoiceRepository.findAll().size();

        // Sprawdzam że wygenerowano nowe faktury (więcej niż było na początku)
        assertThat(finalInvoiceCount).isGreaterThan(initialInvoiceCount);

        // Sprawdzam że response zawiera wygenerowane faktury
        assertThat(response).isNotEmpty();
    }

    @Test
    @WithMockRentowneUser
    void shouldGenerateInvoiceForProviderWithOldPayDayMonth() {
        // given - mamy fakturę Orange (ID=21) dla mieszkania 1 z payDayMonth = grudzień 2024
        when(auditorAware.getCurrentAuditor()).thenReturn(Optional.of("peter.lubowicz@gmail.com"));

        // when
        List<RecurringInvoiceDto> response = webTestClient.post()
                .uri("/api/invoices/generate-recurring")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(RecurringInvoiceDto.class)
                .returnResult()
                .getResponseBody();

        // then - powinna zostać wygenerowana nowa faktura dla Orange i mieszkania 1
        assertThat(response).isNotNull();
        assertThat(response).anyMatch(invoice ->
            invoice.getSeller().equals("Orange") &&
            invoice.getApartment().equals("Zwierzyniecka 18") &&
            invoice.getSettlementMonth().equals(LocalDate.of(2025, 1, 1))
        );

        // Sprawdzam że faktura została zapisana w bazie
        List<Invoice> orangeInvoices = invoiceRepository.findAll().stream()
            .filter(inv -> inv.getHousingProvider().getId() == 21L && inv.getApartment().getId() == 1L)
            .toList();

        // Powinny być 2 faktury: stara z grudnia i nowa ze stycznia
        assertThat(orangeInvoices).hasSizeGreaterThanOrEqualTo(2);
    }

    @Test
    @WithMockRentowneUser
    void shouldNotGenerateInvoiceWhenAlreadyExistsForCurrentMonth() {
        // given - mamy fakturę Sobótka woda (ID=25) dla mieszkania 2 z payDayMonth = styczeń 2025
        when(auditorAware.getCurrentAuditor()).thenReturn(Optional.of("peter.lubowicz@gmail.com"));

        // when
        List<RecurringInvoiceDto> response = webTestClient.post()
                .uri("/api/invoices/generate-recurring")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(RecurringInvoiceDto.class)
                .returnResult()
                .getResponseBody();

        // then - NIE powinna zostać wygenerowana nowa faktura dla Sobótka woda i mieszkania 2
        assertThat(response).isNotNull();
        assertThat(response).noneMatch(invoice ->
            invoice.getSeller().equals("Sobótka woda") &&
            invoice.getApartment().equals("Zwierzyniecka 4")
        );
    }

    @Test
    @WithMockRentowneUser
    void shouldGenerateInvoicesForMultipleProviderTypes() {
        // given
        when(auditorAware.getCurrentAuditor()).thenReturn(Optional.of("peter.lubowicz@gmail.com"));

        // when
        List<RecurringInvoiceDto> response = webTestClient.post()
                .uri("/api/invoices/generate-recurring")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(RecurringInvoiceDto.class)
                .returnResult()
                .getResponseBody();

        // then - powinny być faktury dla różnych typów dostawców
        assertThat(response).isNotNull();

        // Sprawdzam czy są faktury typu INTERNET
        assertThat(response).anyMatch(invoice -> invoice.getProviderType().equals("INTERNET"));

        // Sprawdzam czy są faktury typu ADMINISTRATIVE_FEE
        assertThat(response).anyMatch(invoice -> invoice.getProviderType().equals("ADMINISTRATIVE_FEE"));

        // Sprawdzam czy są faktury typu WATER
        assertThat(response).anyMatch(invoice -> invoice.getProviderType().equals("WATER"));
    }

    @Test
    @WithMockRentowneUser
    void shouldCalculateCorrectAmountFromProviderFields() {
        // given
        when(auditorAware.getCurrentAuditor()).thenReturn(Optional.of("peter.lubowicz@gmail.com"));

        // when
        List<RecurringInvoiceDto> response = webTestClient.post()
                .uri("/api/invoices/generate-recurring")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(RecurringInvoiceDto.class)
                .returnResult()
                .getResponseBody();

        // then - sprawdzam czy kwoty są poprawnie obliczone
        assertThat(response).isNotNull();

        // Orange - jedno pole z ceną 125.00
        assertThat(response)
            .filteredOn(invoice -> invoice.getSeller().equals("Orange"))
            .allMatch(invoice -> invoice.getAmount().compareTo(new BigDecimal("125.00")) == 0);

        // Czynsz administracyjny - dwa pola: 450.00 + 150.00 = 600.00
        assertThat(response)
            .filteredOn(invoice -> invoice.getSeller().equals("Czynsz administracyjny"))
            .allMatch(invoice -> invoice.getAmount().compareTo(new BigDecimal("600.00")) == 0);

        // Sobótka woda - jedno pole z ceną 14.83
        assertThat(response)
            .filteredOn(invoice -> invoice.getSeller().equals("Sobótka woda"))
            .allMatch(invoice -> invoice.getAmount().compareTo(new BigDecimal("14.83")) == 0);
    }

    @Test
    @WithMockRentowneUser
    void shouldGenerateInvoiceWithCorrectInvoiceNumber() {
        // given
        when(auditorAware.getCurrentAuditor()).thenReturn(Optional.of("peter.lubowicz@gmail.com"));

        // when
        List<RecurringInvoiceDto> response = webTestClient.post()
                .uri("/api/invoices/generate-recurring")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(RecurringInvoiceDto.class)
                .returnResult()
                .getResponseBody();

        // then - sprawdzam format numeru faktury: FV/RRRRMM/ID_DOSTAWCY/ID_MIESZKANIA
        assertThat(response).isNotNull();
        assertThat(response).allMatch(invoice -> {
            String invoiceNumber = invoice.getInvoiceNumber();
            return invoiceNumber.matches("FV/\\d{6}/\\d+/\\d+");
        });

        // Sprawdzam czy rok-miesiąc to 202501 (styczeń 2025)
        assertThat(response).allMatch(invoice ->
            invoice.getInvoiceNumber().contains("202501")
        );
    }

    @Test
    @WithMockRentowneUser
    void shouldGenerateInvoicesForBothApartments() {
        // given
        when(auditorAware.getCurrentAuditor()).thenReturn(Optional.of("peter.lubowicz@gmail.com"));

        // when
        List<RecurringInvoiceDto> response = webTestClient.post()
                .uri("/api/invoices/generate-recurring")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(RecurringInvoiceDto.class)
                .returnResult()
                .getResponseBody();

        // then - faktury dla obu mieszkań
        assertThat(response).isNotNull();

        // Faktury dla Zwierzyniecka 18
        assertThat(response).anyMatch(invoice ->
            invoice.getApartment().equals("Zwierzyniecka 18")
        );

        // Faktury dla Zwierzyniecka 4
        assertThat(response).anyMatch(invoice ->
            invoice.getApartment().equals("Zwierzyniecka 4")
        );
    }

    @Test
    @WithMockRentowneUser
    void shouldNotGenerateDuplicateInvoicesOnSecondCall() {
        // given
        when(auditorAware.getCurrentAuditor()).thenReturn(Optional.of("peter.lubowicz@gmail.com"));

        // when - pierwsze wywołanie
        List<RecurringInvoiceDto> firstResponse = webTestClient.post()
                .uri("/api/invoices/generate-recurring")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(RecurringInvoiceDto.class)
                .returnResult()
                .getResponseBody();

        // when - drugie wywołanie
        List<RecurringInvoiceDto> secondResponse = webTestClient.post()
                .uri("/api/invoices/generate-recurring")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(RecurringInvoiceDto.class)
                .returnResult()
                .getResponseBody();

        // then - drugie wywołanie nie powinno wygenerować żadnych faktur
        assertThat(firstResponse).isNotNull().isNotEmpty();
        assertThat(secondResponse).isNotNull().isEmpty();
    }

    @Test
    @WithMockRentowneUser
    void shouldSetCorrectSettlementMonthAndPayDay() {
        // given
        when(auditorAware.getCurrentAuditor()).thenReturn(Optional.of("peter.lubowicz@gmail.com"));

        // when
        List<RecurringInvoiceDto> response = webTestClient.post()
                .uri("/api/invoices/generate-recurring")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(RecurringInvoiceDto.class)
                .returnResult()
                .getResponseBody();

        // then - miesiąc rozliczenia to styczeń 2025
        assertThat(response).isNotNull();
        assertThat(response).allMatch(invoice ->
            invoice.getSettlementMonth().equals(LocalDate.of(2025, 1, 1))
        );

        // Sprawdzam faktury w bazie
        List<Invoice> newInvoices = invoiceRepository.findAll().stream()
            .filter(inv -> inv.getPayDayMonth() != null &&
                          inv.getPayDayMonth().equals(LocalDate.of(2025, 1, 1)))
            .toList();

        // Termin płatności to 15 dzień miesiąca
        assertThat(newInvoices).allMatch(invoice ->
            invoice.getPayDay() != null &&
            invoice.getPayDay().getDayOfMonth() == 15
        );

        // Data od: 1 dzień miesiąca
        assertThat(newInvoices).allMatch(invoice ->
            invoice.getDateFrom() != null &&
            invoice.getDateFrom().getDayOfMonth() == 1
        );

        // Data do: ostatni dzień miesiąca (31 stycznia)
        assertThat(newInvoices).allMatch(invoice ->
            invoice.getDateTo() != null &&
            invoice.getDateTo().getDayOfMonth() == 31
        );
    }

    @Test
    @WithMockRentowneUser
    void shouldOnlyGenerateForRecurringPaymentProviders() {
        // given
        when(auditorAware.getCurrentAuditor()).thenReturn(Optional.of("peter.lubowicz@gmail.com"));

        // when
        List<RecurringInvoiceDto> response = webTestClient.post()
                .uri("/api/invoices/generate-recurring")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(RecurringInvoiceDto.class)
                .returnResult()
                .getResponseBody();

        // then - NIE powinno być faktur dla dostawców z recurring_payment = false
        assertThat(response).isNotNull();

        // Enea ma recurring_payment = false - nie powinno być faktur
        assertThat(response).noneMatch(invoice -> invoice.getSeller().equals("Enea"));

        // PGNiG ma recurring_payment = false - nie powinno być faktur
        assertThat(response).noneMatch(invoice -> invoice.getSeller().equals("PGNiG"));

        // Vectra ma recurring_payment = false - nie powinno być faktur
        assertThat(response).noneMatch(invoice -> invoice.getSeller().equals("Vectra"));

        // Orange ma recurring_payment = true - powinny być faktury
        assertThat(response).anyMatch(invoice -> invoice.getSeller().equals("Orange"));
    }
}

