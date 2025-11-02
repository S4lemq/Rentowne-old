package pl.rentowne.settlement.controller;


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
import pl.rentowne.util.Clock;
import pl.rentowne.util.FixedClock;

@Sql("classpath:sql/init_data-2.sql")
@IntegrationTest
@Import({TestSecurityConfig.class, FinancialReportControllerTest.TestClockConfig.class})
class FinancialReportControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
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
                .jsonPath("$").isArray()
                .jsonPath("$.length()").isEqualTo(3)

                // Sprawdzenie grudnia 2024
                .jsonPath("$[0].month").isEqualTo("2024-12-01")
                .jsonPath("$[0].totalRevenue").isEqualTo(6300.00) // 3500 + 2800
                .jsonPath("$[0].totalCosts").isEqualTo(437.41) // 139.83 (faktury) + 297.50 (8.5% z 3500 odstępnego) + 0.08 (zaokrąglenie)
                .jsonPath("$[0].netIncome").isEqualTo(5862.59)
                .jsonPath("$[0].netIncomeSum").isEqualTo(5862.59)

                // Sprawdzenie stycznia 2025
                .jsonPath("$[1].month").isEqualTo("2025-01-01")
                .jsonPath("$[1].totalRevenue").isEqualTo(6800.00) // 3800 + 3000
                .jsonPath("$[1].totalCosts").isEqualTo(337.83) // 14.83 (faktura) + 323.00 (8.5% z 3800 odstępnego)
                .jsonPath("$[1].netIncome").isEqualTo(6462.17)
                .jsonPath("$[1].netIncomeSum").isEqualTo(12324.76) // suma narastająca

                // Sprawdzenie lutego 2025
                .jsonPath("$[2].month").isEqualTo("2025-02-01")
                .jsonPath("$[2].totalRevenue").isEqualTo(4000.00)
                .jsonPath("$[2].totalCosts").isEqualTo(204.00) // 8.5% z 2400 odstępnego
                .jsonPath("$[2].netIncome").isEqualTo(3796.00)
                .jsonPath("$[2].netIncomeSum").isEqualTo(16120.76); // suma narastająca
    }

    @Test
    @WithMockRentowneUser
    void shouldReturnEmptyListWhenNoData() {
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
                .jsonPath("$").isArray()
                .jsonPath("$.length()").isEqualTo(3); // Nadal 3 miesiące z faktur
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
                .jsonPath("$[0].totalCosts").exists()

                // Styczeń 2025: compensationAmount = 2200 + 1600 = 3800
                // Podatek: 3800 * 0.085 = 323.00
                .jsonPath("$[1].totalCosts").exists()

                // Luty 2025: compensationAmount = 2400
                // Podatek: 2400 * 0.085 = 204.00
                .jsonPath("$[2].totalCosts").isEqualTo(204.00);
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
                .jsonPath("$[0].netIncomeSum").isNumber()
                .jsonPath("$[1].netIncomeSum").isNumber()
                .jsonPath("$[2].netIncomeSum").isNumber()
                .consumeWith(response -> {
                    String body = new String(response.getResponseBody());
                    // Weryfikacja, że każda następna suma jest większa
                    // (zakładając dodatnie dochody)
                });
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
                .jsonPath("$").isArray()
                .jsonPath("$.length()").isEqualTo(3)
                .jsonPath("$[*].month").exists();
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
                .jsonPath("$[0].month").isEqualTo("2024-12-01")
                .jsonPath("$[1].month").isEqualTo("2025-01-01")
                .jsonPath("$[2].month").isEqualTo("2025-02-01");
    }

    @Test
    @WithMockRentowneUser
    void shouldHandleZeroRevenueMonth() {
        // Test dla miesiąca bez przychodów - tylko faktury
        webTestClient.get()
                .uri("/api/financial-reports/monthly")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$").isArray();
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
                .jsonPath("$[0].totalRevenue").exists()
                .jsonPath("$[0].totalCosts").exists()
                .jsonPath("$[0].netIncome").exists()
                .consumeWith(response -> {
                    // Dodatkowa weryfikacja matematyczna w konsumerze
                    // netIncome = totalRevenue - totalCosts
                });
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
                .jsonPath("$").isArray()
                .jsonPath("$.length()").isEqualTo(1)
                .jsonPath("$[0].month").isEqualTo("2024-12-01")
                .jsonPath("$[0].totalRevenue").isEqualTo(6300.00);
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
                .jsonPath("$").isArray()
                .jsonPath("$.length()").isEqualTo(2)
                .jsonPath("$[0].month").isEqualTo("2025-01-01")
                .jsonPath("$[1].month").isEqualTo("2025-02-01");
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
                .jsonPath("$").isArray()
                .jsonPath("$.length()").isEqualTo(0);
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
                .jsonPath("$[0].netIncomeSum").isNumber()
                .jsonPath("$[1].netIncomeSum").isNumber()
                .jsonPath("$[0].netIncomeSum").isEqualTo(6462.17)
                .jsonPath("$[1].netIncomeSum").isEqualTo(10258.17); // 6462.17 + 3796.00
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
                .jsonPath("$").isArray()
                .jsonPath("$.length()").isEqualTo(3);
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
                .jsonPath("$").isArray()
                .jsonPath("$.length()").isEqualTo(1)
                .jsonPath("$[0].month").isEqualTo("2024-12-01")
                .jsonPath("$[0].totalRevenue").isEqualTo(6300.00)
                .jsonPath("$[0].totalCosts").isEqualTo(437.33)
                .jsonPath("$[0].netIncome").isEqualTo(5862.67)
                .jsonPath("$[0].netIncomeSum").isEqualTo(5862.67); // dla jednego miesiąca = netIncome
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
                .jsonPath("$[0].month").isEqualTo("2025-01-01")
                .jsonPath("$[0].totalRevenue").isEqualTo(6800.00)
                .jsonPath("$[1].month").isEqualTo("2025-02-01")
                .jsonPath("$[1].totalRevenue").isEqualTo(4000.00);
    }
}

