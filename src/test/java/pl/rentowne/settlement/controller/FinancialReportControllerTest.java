package pl.rentowne.settlement.controller;


import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.AuditorAware;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.reactive.server.WebTestClient;
import pl.rentowne.IntegrationTest;
import pl.rentowne.TestSecurityConfig;
import pl.rentowne.WithMockRentowneUser;
import pl.rentowne.settlement.SettlementRepository;
import pl.rentowne.util.Clock;
import pl.rentowne.util.FixedClock;

@Sql({"classpath:sql/cleanup.sql", "classpath:sql/init_data-2.sql"})
@IntegrationTest
@Import({TestSecurityConfig.class, FinancialReportControllerTest.TestClockConfig.class})
class FinancialReportControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private SettlementRepository settlementRepository;

    @MockitoBean
    private AuditorAware<String> auditorAware;


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
    void shouldGenerateFinancialReportSuccessfully() {
        // when & then
        webTestClient.get()
                .uri("/api/financial-reports/monthly")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.months").isArray()
                .jsonPath("$.months.length()").isEqualTo(3)

                // Sprawdzenie grudnia 2024
                .jsonPath("$.months[0].month").isEqualTo("2024-12-01")
                .jsonPath("$.months[0].totalRevenue").isEqualTo(6300.00) // 3500 + 2800
                .jsonPath("$.months[0].totalCosts").isEqualTo(437.33) // 139.83 + 297.50
                .jsonPath("$.months[0].netIncome").isEqualTo(5862.67)
                .jsonPath("$.months[0].netIncomeSum").isEqualTo(5862.67)

                // Sprawdzenie stycznia 2025
                .jsonPath("$.months[1].month").isEqualTo("2025-01-01")
                .jsonPath("$.months[1].totalRevenue").isEqualTo(6800.00) // 3800 + 3000
                .jsonPath("$.months[1].totalCosts").isEqualTo(337.83) // 14.83 (faktura) + 323.00 (8.5% z 3800 odstępnego)
                .jsonPath("$.months[1].netIncome").isEqualTo(6462.17)
                .jsonPath("$.months[1].netIncomeSum").isEqualTo(12324.84) // suma narastająca

                // Sprawdzenie lutego 2025
                .jsonPath("$.months[2].month").isEqualTo("2025-02-01")
                .jsonPath("$.months[2].totalRevenue").isEqualTo(4000.00)
                .jsonPath("$.months[2].totalCosts").isEqualTo(204.00) // 8.5% z 2400 odstępnego
                .jsonPath("$.months[2].netIncome").isEqualTo(3796.00)
                .jsonPath("$.months[2].netIncomeSum").isEqualTo(16120.84)
                .jsonPath("$.yearlyTotalRevenue").isEqualTo(17100.00)
                .jsonPath("$.yearlyTotalCosts").isEqualTo(979.16); // suma narastająca
    }

    @Test
    @WithMockRentowneUser
    void shouldReturnInvoiceMonthsWhenSettlementsAreDeleted() {
        // given - czyszczenie danych
        webTestClient.delete()
                .uri("/api/settlements/{id}", 1)
                .exchange()
                .expectStatus().isOk();

        webTestClient.delete()
                .uri("/api/settlements/{id}", 2)
                .exchange()
                .expectStatus().isOk();

        webTestClient.delete()
                .uri("/api/settlements/{id}", 3)
                .exchange()
                .expectStatus().isOk();

        webTestClient.delete()
                .uri("/api/settlements/{id}", 4)
                .exchange()
                .expectStatus().isOk();

        webTestClient.delete()
                .uri("/api/settlements/{id}", 5)
                .exchange()
                .expectStatus().isOk();

        // when & then
        webTestClient.get()
                .uri("/api/financial-reports/monthly")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.months").isArray()
                .jsonPath("$.months.length()").isEqualTo(2); // Nadal 2 miesiące z faktur
    }

    @Test
    @WithMockRentowneUser
    void shouldCalculateCorrectTaxOnCompensation() {
        // when & then - weryfikacja podatku 8.5% od odstępnego
        webTestClient.get()
                .uri("/api/financial-reports/monthly")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                // Grudzień 2024: compensationAmount = 2000 + 1500 = 3500
                // Podatek: 3500 * 0.085 = 297.50
                .jsonPath("$.months[0].totalCosts").isEqualTo(437.33)

                // Styczeń 2025: compensationAmount = 2200 + 1600 = 3800
                // Podatek: 3800 * 0.085 = 323.00
                .jsonPath("$.months[1].totalCosts").isEqualTo(337.83)

                // Luty 2025: compensationAmount = 2400
                // Podatek: 2400 * 0.085 = 204.00
                .jsonPath("$.months[2].totalCosts").isEqualTo(204.00);
    }

    @Test
    @WithMockRentowneUser
    void shouldCalculateCumulativeNetIncome() {
        // when & then - sprawdzenie sumy narastającej dochodu
        webTestClient.get()
                .uri("/api/financial-reports/monthly")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.months[0].netIncomeSum").isEqualTo(5862.67)
                .jsonPath("$.months[1].netIncomeSum").isEqualTo(12324.84)
                .jsonPath("$.months[2].netIncomeSum").isEqualTo(16120.84);
    }

    @Test
    @WithMockRentowneUser
    void shouldIncludeAllMonthsFromSettlementsAndInvoices() {
        // when & then - sprawdzenie, że uwzględnione są wszystkie miesiące
        webTestClient.get()
                .uri("/api/financial-reports/monthly")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.months").isArray()
                .jsonPath("$.months.length()").isEqualTo(3)
                .jsonPath("$.months[*].month").exists();
    }

    @Test
    @WithMockRentowneUser
    void shouldReturnMonthsInChronologicalOrder() {
        // when & then - sprawdzenie kolejności chronologicznej
        webTestClient.get()
                .uri("/api/financial-reports/monthly")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.months[0].month").isEqualTo("2024-12-01")
                .jsonPath("$.months[1].month").isEqualTo("2025-01-01")
                .jsonPath("$.months[2].month").isEqualTo("2025-02-01");
    }

    @Test
    @WithMockRentowneUser
    void shouldHandleZeroRevenueMonth() {
        settlementRepository.deleteAllInBatch();
        // Test dla miesiąca bez przychodów - tylko faktury
        webTestClient.get()
                .uri("/api/financial-reports/monthly")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.months.length()").isEqualTo(2)
                .jsonPath("$.months[0].totalRevenue").isEqualTo(0)
                .jsonPath("$.months[0].totalCosts").isEqualTo(139.83)
                .jsonPath("$.months[0].netIncome").isEqualTo(-139.83)
                .jsonPath("$.months[1].totalRevenue").isEqualTo(0)
                .jsonPath("$.months[1].totalCosts").isEqualTo(14.83)
                .jsonPath("$.months[1].netIncome").isEqualTo(-14.83);
    }

    @Test
    @WithMockRentowneUser
    void shouldCalculateNetIncomeCorrectly() {
        // when & then - weryfikacja: dochód = przychód - koszty
        webTestClient.get()
                .uri("/api/financial-reports/monthly")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.months[0].totalRevenue").isEqualTo(6300.00)
                .jsonPath("$.months[0].totalCosts").isEqualTo(437.33)
                .jsonPath("$.months[0].netIncome").isEqualTo(5862.67);
    }

    @Test
    @WithMockRentowneUser
    void shouldFilterByYear2024() {
        // when & then
        webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/financial-reports/monthly")
                        .queryParam("year", 2024)
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.months").isArray()
                .jsonPath("$.months.length()").isEqualTo(1)
                .jsonPath("$.months[0].month").isEqualTo("2024-12-01")
                .jsonPath("$.months[0].totalRevenue").isEqualTo(6300.00);
    }

    @Test
    @WithMockRentowneUser
    void shouldFilterByYear2025() {
        // when & then
        webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/financial-reports/monthly")
                        .queryParam("year", 2025)
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.months").isArray()
                .jsonPath("$.months.length()").isEqualTo(2)
                .jsonPath("$.months[0].month").isEqualTo("2025-01-01")
                .jsonPath("$.months[1].month").isEqualTo("2025-02-01");
    }

    @Test
    @WithMockRentowneUser
    void shouldReturnEmptyArrayForYearWithNoData() {
        // when & then
        webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/financial-reports/monthly")
                        .queryParam("year", 2023)
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.months").isArray()
                .jsonPath("$.months.length()").isEqualTo(0)
                .jsonPath("$.yearlyTotalRevenue").isEqualTo(0)
                .jsonPath("$.yearlyTotalCosts").isEqualTo(0);
    }

    @Test
    @WithMockRentowneUser
    void shouldCalculateCumulativeNetIncomeForFilteredYear() {
        // when & then - dla roku 2025 suma narastająca powinna zaczynać się od zera
        webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/financial-reports/monthly")
                        .queryParam("year", 2025)
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.months[0].netIncomeSum").isNumber()
                .jsonPath("$.months[1].netIncomeSum").isNumber()
                .jsonPath("$.months[0].netIncomeSum").isEqualTo(6462.17)
                .jsonPath("$.months[1].netIncomeSum").isEqualTo(10258.17); // 6462.17 + 3796.00
    }

    @Test
    @WithMockRentowneUser
    void shouldReturnAllMonthsWhenYearNotProvided() {
        // when & then
        webTestClient.get()
                .uri("/api/financial-reports/monthly")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.months").isArray()
                .jsonPath("$.months.length()").isEqualTo(3);
    }

    @Test
    @WithMockRentowneUser
    void shouldFilterYear2024WithCorrectCalculations() {
        // when & then
        webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/financial-reports/monthly")
                        .queryParam("year", 2024)
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.months").isArray()
                .jsonPath("$.months.length()").isEqualTo(1)
                .jsonPath("$.months[0].month").isEqualTo("2024-12-01")
                .jsonPath("$.months[0].totalRevenue").isEqualTo(6300.00)
                .jsonPath("$.months[0].totalCosts").isEqualTo(437.33)
                .jsonPath("$.months[0].netIncome").isEqualTo(5862.67)
                .jsonPath("$.months[0].netIncomeSum").isEqualTo(5862.67); // dla jednego miesiąca = netIncome
    }

    @Test
    @WithMockRentowneUser
    void shouldFilterYear2025WithTwoMonths() {
        // when & then
        webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/financial-reports/monthly")
                        .queryParam("year", 2025)
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.months[0].month").isEqualTo("2025-01-01")
                .jsonPath("$.months[0].totalRevenue").isEqualTo(6800.00)
                .jsonPath("$.months[1].month").isEqualTo("2025-02-01")
                .jsonPath("$.months[1].totalRevenue").isEqualTo(4000.00);
    }
}

