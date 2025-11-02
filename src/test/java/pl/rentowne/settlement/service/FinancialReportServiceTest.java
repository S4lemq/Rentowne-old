package pl.rentowne.settlement.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;
import pl.rentowne.IntegrationTest;
import pl.rentowne.settlement.model.dto.MonthlyFinancialReportDto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Sql("classpath:sql/init_data-2.sql")
@IntegrationTest
class FinancialReportServiceTest {

    @Autowired
    private FinancialReportService financialReportService;

    @Test
    void shouldGenerateMonthlyFinancialReport() {
        // when
        List<MonthlyFinancialReportDto> report = financialReportService.getMonthlyFinancialReport();

        // then
        assertThat(report).isNotNull();
        assertThat(report).hasSize(3);
    }

    @Test
    void shouldReturnReportsInChronologicalOrder() {
        // when
        List<MonthlyFinancialReportDto> report = financialReportService.getMonthlyFinancialReport();

        // then
        assertThat(report.get(0).getMonth()).isEqualTo(LocalDate.of(2024, 12, 1));
        assertThat(report.get(1).getMonth()).isEqualTo(LocalDate.of(2025, 1, 1));
        assertThat(report.get(2).getMonth()).isEqualTo(LocalDate.of(2025, 2, 1));
    }

    @Test
    void shouldCalculateDecember2024Correctly() {
        // when
        List<MonthlyFinancialReportDto> report = financialReportService.getMonthlyFinancialReport();
        MonthlyFinancialReportDto december = report.get(0);

        // then
        // Przychód: Settlement 3500 + 2800 = 6300
        assertThat(december.getTotalRevenue()).isEqualByComparingTo(new BigDecimal("6300.00"));

        // Koszty: Invoice (125 + 14.83 = 139.83) + Podatek od odstępnego (3500 * 0.085 = 297.50)
        assertThat(december.getTotalCosts()).isEqualByComparingTo(new BigDecimal("437.33"));

        // Dochód: 6300 - 437.33 = 5862.67
        assertThat(december.getNetIncome()).isEqualByComparingTo(new BigDecimal("5862.67"));

        // Suma narastająca = dochód pierwszego miesiąca
        assertThat(december.getNetIncomeSum()).isEqualByComparingTo(new BigDecimal("5862.67"));
    }

    @Test
    void shouldCalculateJanuary2025Correctly() {
        // when
        List<MonthlyFinancialReportDto> report = financialReportService.getMonthlyFinancialReport();
        MonthlyFinancialReportDto january = report.get(1);

        // then
        // Przychód: Settlement 3800 + 3000 = 6800
        assertThat(january.getTotalRevenue()).isEqualByComparingTo(new BigDecimal("6800.00"));

        // Koszty: Invoice (14.83) + Podatek od odstępnego (3800 * 0.085 = 323.00)
        assertThat(january.getTotalCosts()).isEqualByComparingTo(new BigDecimal("337.83"));

        // Dochód: 6800 - 337.83 = 6462.17
        assertThat(january.getNetIncome()).isEqualByComparingTo(new BigDecimal("6462.17"));

        // Suma narastająca: poprzednia suma + obecny dochód
        assertThat(january.getNetIncomeSum()).isGreaterThan(new BigDecimal("12000.00"));
    }

    @Test
    void shouldCalculateFebruary2025Correctly() {
        // when
        List<MonthlyFinancialReportDto> report = financialReportService.getMonthlyFinancialReport();
        MonthlyFinancialReportDto february = report.get(2);

        // then
        // Przychód: Settlement 4000
        assertThat(february.getTotalRevenue()).isEqualByComparingTo(new BigDecimal("4000.00"));

        // Koszty: Brak faktur + Podatek od odstępnego (2400 * 0.085 = 204.00)
        assertThat(february.getTotalCosts()).isEqualByComparingTo(new BigDecimal("204.00"));

        // Dochód: 4000 - 204 = 3796
        assertThat(february.getNetIncome()).isEqualByComparingTo(new BigDecimal("3796.00"));

        // Suma narastająca: powinna być największa
        assertThat(february.getNetIncomeSum()).isGreaterThan(new BigDecimal("16000.00"));
    }

    @Test
    void shouldCalculateCompensationTaxCorrectly() {
        // when
        List<MonthlyFinancialReportDto> report = financialReportService.getMonthlyFinancialReport();

        // then - sprawdzenie podatku 8.5% dla każdego miesiąca
        // Grudzień: compensationAmount = 3500, podatek = 297.50
        MonthlyFinancialReportDto december = report.get(0);
        BigDecimal decemberInvoiceCosts = new BigDecimal("139.83");
        BigDecimal decemberTax = december.getTotalCosts().subtract(decemberInvoiceCosts);
        assertThat(decemberTax).isEqualByComparingTo(new BigDecimal("297.50"));

        // Styczeń: compensationAmount = 3800, podatek = 323.00
        MonthlyFinancialReportDto january = report.get(1);
        BigDecimal januaryInvoiceCosts = new BigDecimal("14.83");
        BigDecimal januaryTax = january.getTotalCosts().subtract(januaryInvoiceCosts);
        assertThat(januaryTax).isEqualByComparingTo(new BigDecimal("323.00"));

        // Luty: compensationAmount = 2400, podatek = 204.00
        MonthlyFinancialReportDto february = report.get(2);
        assertThat(february.getTotalCosts()).isEqualByComparingTo(new BigDecimal("204.00"));
    }

    @Test
    void shouldCalculateCumulativeNetIncomeCorrectly() {
        // when
        List<MonthlyFinancialReportDto> report = financialReportService.getMonthlyFinancialReport();

        // then - sprawdzenie sumy narastającej
        BigDecimal previousSum = BigDecimal.ZERO;
        for (MonthlyFinancialReportDto dto : report) {
            BigDecimal expectedSum = previousSum.add(dto.getNetIncome());
            assertThat(dto.getNetIncomeSum())
                    .as("Suma narastająca dla miesiąca %s", dto.getMonth())
                    .isEqualByComparingTo(expectedSum);
            previousSum = dto.getNetIncomeSum();
        }
    }

    @Test
    void shouldHandleMonthsWithOnlyInvoices() {
        // when
        List<MonthlyFinancialReportDto> report = financialReportService.getMonthlyFinancialReport();

        // then - wszystkie miesiące powinny być uwzględnione
        assertThat(report).allMatch(dto -> dto.getMonth() != null);
        assertThat(report).allMatch(dto -> dto.getTotalRevenue() != null);
        assertThat(report).allMatch(dto -> dto.getTotalCosts() != null);
        assertThat(report).allMatch(dto -> dto.getNetIncome() != null);
        assertThat(report).allMatch(dto -> dto.getNetIncomeSum() != null);
    }

    @Test
    void shouldCalculateNetIncomeAsRevenueMInusCosts() {
        // when
        List<MonthlyFinancialReportDto> report = financialReportService.getMonthlyFinancialReport();

        // then - dla każdego miesiąca: netIncome = totalRevenue - totalCosts
        for (MonthlyFinancialReportDto dto : report) {
            BigDecimal expectedNetIncome = dto.getTotalRevenue().subtract(dto.getTotalCosts());
            assertThat(dto.getNetIncome())
                    .as("Dochód netto dla miesiąca %s powinien być równy przychód minus koszty", dto.getMonth())
                    .isEqualByComparingTo(expectedNetIncome);
        }
    }

    @Test
    void shouldIncludeAllFieldsInReport() {
        // when
        List<MonthlyFinancialReportDto> report = financialReportService.getMonthlyFinancialReport();

        // then - sprawdzenie, że wszystkie pola są wypełnione
        for (MonthlyFinancialReportDto dto : report) {
            assertThat(dto.getMonth()).isNotNull();
            assertThat(dto.getTotalRevenue()).isNotNull();
            assertThat(dto.getTotalCosts()).isNotNull();
            assertThat(dto.getNetIncome()).isNotNull();
            assertThat(dto.getNetIncomeSum()).isNotNull();
        }
    }

    @Test
    void shouldHandleZeroValuesCorrectly() {
        // when
        List<MonthlyFinancialReportDto> report = financialReportService.getMonthlyFinancialReport();

        // then - żadna wartość nie powinna być null, może być zero
        for (MonthlyFinancialReportDto dto : report) {
            assertThat(dto.getTotalRevenue()).isGreaterThanOrEqualTo(BigDecimal.ZERO);
            assertThat(dto.getTotalCosts()).isGreaterThanOrEqualTo(BigDecimal.ZERO);
        }
    }

    @Test
    void shouldCalculateTotalCostsAsInvoicePlusCompensationTax() {
        // when
        List<MonthlyFinancialReportDto> report = financialReportService.getMonthlyFinancialReport();
        MonthlyFinancialReportDto february = report.get(2);

        // then - luty ma tylko podatek od odstępnego, brak faktur
        // compensationAmount = 2400, podatek = 2400 * 0.085 = 204.00
        assertThat(february.getTotalCosts()).isEqualByComparingTo(new BigDecimal("204.00"));
        assertThat(february.getTotalRevenue()).isEqualByComparingTo(new BigDecimal("4000.00"));
    }

    @Test
    void shouldFilterByYear2024() {
        // when
        List<MonthlyFinancialReportDto> report = financialReportService.getMonthlyFinancialReport(2024);

        // then - tylko grudzień 2024
        assertThat(report).hasSize(1);
        assertThat(report.get(0).getMonth()).isEqualTo(LocalDate.of(2024, 12, 1));
        assertThat(report.get(0).getTotalRevenue()).isEqualByComparingTo(new BigDecimal("6300.00"));
    }

    @Test
    void shouldFilterByYear2025() {
        // when
        List<MonthlyFinancialReportDto> report = financialReportService.getMonthlyFinancialReport(2025);

        // then - styczeń i luty 2025
        assertThat(report).hasSize(2);
        assertThat(report.get(0).getMonth()).isEqualTo(LocalDate.of(2025, 1, 1));
        assertThat(report.get(1).getMonth()).isEqualTo(LocalDate.of(2025, 2, 1));
    }

    @Test
    void shouldReturnEmptyListForYearWithNoData() {
        // when
        List<MonthlyFinancialReportDto> report = financialReportService.getMonthlyFinancialReport(2023);

        // then - brak danych dla 2023
        assertThat(report).isEmpty();
    }

    @Test
    void shouldCalculateCumulativeNetIncomeForFilteredYear() {
        // when
        List<MonthlyFinancialReportDto> report = financialReportService.getMonthlyFinancialReport(2025);

        // then - suma narastająca powinna być liczona od początku odfiltrowanego roku
        BigDecimal previousSum = BigDecimal.ZERO;
        for (MonthlyFinancialReportDto dto : report) {
            BigDecimal expectedSum = previousSum.add(dto.getNetIncome());
            assertThat(dto.getNetIncomeSum())
                    .as("Suma narastająca dla miesiąca %s", dto.getMonth())
                    .isEqualByComparingTo(expectedSum);
            previousSum = dto.getNetIncomeSum();
        }
    }

    @Test
    void shouldReturnAllDataWhenYearIsNull() {
        // when
        List<MonthlyFinancialReportDto> reportWithNull = financialReportService.getMonthlyFinancialReport(null);
        List<MonthlyFinancialReportDto> reportWithoutParam = financialReportService.getMonthlyFinancialReport();

        // then - obie metody powinny zwrócić te same dane
        assertThat(reportWithNull).hasSize(3);
        assertThat(reportWithoutParam).hasSize(3);
        assertThat(reportWithNull).hasSameSizeAs(reportWithoutParam);
    }

    @Test
    void shouldFilterYear2024OnlyDecember() {
        // when
        List<MonthlyFinancialReportDto> report = financialReportService.getMonthlyFinancialReport(2024);
        MonthlyFinancialReportDto december = report.get(0);

        // then
        assertThat(report).hasSize(1);
        assertThat(december.getMonth().getYear()).isEqualTo(2024);
        assertThat(december.getMonth().getMonthValue()).isEqualTo(12);

        // Suma narastająca dla jednego miesiąca powinna być równa dochód tego miesiąca
        assertThat(december.getNetIncomeSum()).isEqualByComparingTo(december.getNetIncome());
    }

    @Test
    void shouldFilterYear2025WithCorrectCalculations() {
        // when
        List<MonthlyFinancialReportDto> report = financialReportService.getMonthlyFinancialReport(2025);

        // then
        assertThat(report).hasSize(2);

        // Styczeń 2025
        MonthlyFinancialReportDto january = report.get(0);
        assertThat(january.getMonth()).isEqualTo(LocalDate.of(2025, 1, 1));
        assertThat(january.getTotalRevenue()).isEqualByComparingTo(new BigDecimal("6800.00"));

        // Luty 2025
        MonthlyFinancialReportDto february = report.get(1);
        assertThat(february.getMonth()).isEqualTo(LocalDate.of(2025, 2, 1));
        assertThat(february.getTotalRevenue()).isEqualByComparingTo(new BigDecimal("4000.00"));

        // Suma narastająca lutego = styczeń + luty
        BigDecimal expectedSum = january.getNetIncome().add(february.getNetIncome());
        assertThat(february.getNetIncomeSum()).isEqualByComparingTo(expectedSum);
    }
}
