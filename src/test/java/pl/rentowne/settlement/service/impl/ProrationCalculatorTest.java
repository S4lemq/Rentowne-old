package pl.rentowne.settlement.service.impl;

import org.junit.jupiter.api.Test;
import pl.rentowne.settlement.model.ProrationMode;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ProrationCalculatorTest {

    @Test
    void shouldCalculateActualDaysCorrectly() {
        // Okres: 15-31 stycznia (17 dni)
        ProrationCalculator calculator = ProrationCalculator.builder()
                .periodStartDate(LocalDateTime.of(2025, 1, 15, 0, 0))
                .periodEndDate(LocalDateTime.of(2025, 1, 31, 23, 59))
                .settlementDate(LocalDateTime.of(2025, 1, 31, 23, 59))
                .build();

        assertEquals(17, calculator.getActualDays());
    }

    @Test
    void shouldCalculateTotalDaysInMonthCorrectly() {
        // Styczeń ma 31 dni
        ProrationCalculator calculator = ProrationCalculator.builder()
                .periodStartDate(LocalDateTime.of(2025, 1, 15, 0, 0))
                .periodEndDate(LocalDateTime.of(2025, 1, 31, 23, 59))
                .settlementDate(LocalDateTime.of(2025, 1, 31, 23, 59))
                .build();

        assertEquals(31, calculator.getTotalDaysInMonth());
    }

    @Test
    void shouldCalculateTotalDaysForFebruaryLeapYear() {
        // Luty 2024 (rok przestępny) ma 29 dni
        ProrationCalculator calculator = ProrationCalculator.builder()
                .periodStartDate(LocalDateTime.of(2024, 2, 1, 0, 0))
                .periodEndDate(LocalDateTime.of(2024, 2, 15, 23, 59))
                .settlementDate(LocalDateTime.of(2024, 2, 29, 23, 59))
                .build();

        assertEquals(29, calculator.getTotalDaysInMonth());
    }

    @Test
    void shouldCalculateTotalDaysForFebruaryNonLeapYear() {
        // Luty 2025 (rok nieprzestępny) ma 28 dni
        ProrationCalculator calculator = ProrationCalculator.builder()
                .periodStartDate(LocalDateTime.of(2025, 2, 1, 0, 0))
                .periodEndDate(LocalDateTime.of(2025, 2, 15, 23, 59))
                .settlementDate(LocalDateTime.of(2025, 2, 28, 23, 59))
                .build();

        assertEquals(28, calculator.getTotalDaysInMonth());
    }

    @Test
    void shouldCalculateProrationFactorCorrectly() {
        // 17 dni z 31 = 0.548387
        ProrationCalculator calculator = ProrationCalculator.builder()
                .periodStartDate(LocalDateTime.of(2025, 1, 15, 0, 0))
                .periodEndDate(LocalDateTime.of(2025, 1, 31, 23, 59))
                .settlementDate(LocalDateTime.of(2025, 1, 31, 23, 59))
                .build();

        BigDecimal expectedFactor = BigDecimal.valueOf(17)
                .divide(BigDecimal.valueOf(31), 6, RoundingMode.HALF_UP);

        assertEquals(expectedFactor, calculator.getProrationFactor());
    }

    @Test
    void shouldCalculateProratedAmountCorrectly() {
        // Czynsz 900 zł za 17 dni z 31
        ProrationCalculator calculator = ProrationCalculator.builder()
                .periodStartDate(LocalDateTime.of(2025, 1, 15, 0, 0))
                .periodEndDate(LocalDateTime.of(2025, 1, 31, 23, 59))
                .settlementDate(LocalDateTime.of(2025, 1, 31, 23, 59))
                .build();

        BigDecimal rentAmount = new BigDecimal("900.00");
        BigDecimal proratedAmount = calculator.calculateAmount(rentAmount, ProrationMode.PRORATED);

        // 900 × (17/31) = 493.548... ≈ 493.55
        assertEquals(new BigDecimal("493.55"), proratedAmount);
    }

    @Test
    void shouldReturnFullAmountWhenModeIsFull() {
        ProrationCalculator calculator = ProrationCalculator.builder()
                .periodStartDate(LocalDateTime.of(2025, 1, 15, 0, 0))
                .periodEndDate(LocalDateTime.of(2025, 1, 31, 23, 59))
                .settlementDate(LocalDateTime.of(2025, 1, 31, 23, 59))
                .build();

        BigDecimal rentAmount = new BigDecimal("900.00");
        BigDecimal fullAmount = calculator.calculateAmount(rentAmount, ProrationMode.FULL);

        assertEquals(new BigDecimal("900.00"), fullAmount);
    }

    @Test
    void shouldReturnFullAmountWhenModeIsNull() {
        ProrationCalculator calculator = ProrationCalculator.builder()
                .periodStartDate(LocalDateTime.of(2025, 1, 15, 0, 0))
                .periodEndDate(LocalDateTime.of(2025, 1, 31, 23, 59))
                .settlementDate(LocalDateTime.of(2025, 1, 31, 23, 59))
                .build();

        BigDecimal rentAmount = new BigDecimal("900.00");
        BigDecimal fullAmount = calculator.calculateAmount(rentAmount, null);

        assertEquals(new BigDecimal("900.00"), fullAmount);
    }

    @Test
    void shouldGeneratePeriodDescriptionCorrectly() {
        ProrationCalculator calculator = ProrationCalculator.builder()
                .periodStartDate(LocalDateTime.of(2025, 1, 15, 0, 0))
                .periodEndDate(LocalDateTime.of(2025, 1, 31, 23, 59))
                .settlementDate(LocalDateTime.of(2025, 1, 31, 23, 59))
                .build();

        assertEquals("15-31.01.2025", calculator.getPeriodDescription());
    }

    @Test
    void shouldGeneratePeriodDescriptionForSingleDigitDates() {
        ProrationCalculator calculator = ProrationCalculator.builder()
                .periodStartDate(LocalDateTime.of(2025, 2, 1, 0, 0))
                .periodEndDate(LocalDateTime.of(2025, 2, 9, 23, 59))
                .settlementDate(LocalDateTime.of(2025, 2, 28, 23, 59))
                .build();

        assertEquals("01-09.02.2025", calculator.getPeriodDescription());
    }

    @Test
    void shouldCalculateFullMonthCorrectly() {
        // Pełny miesiąc: 1-31 stycznia
        ProrationCalculator calculator = ProrationCalculator.builder()
                .periodStartDate(LocalDateTime.of(2025, 1, 1, 0, 0))
                .periodEndDate(LocalDateTime.of(2025, 1, 31, 23, 59))
                .settlementDate(LocalDateTime.of(2025, 1, 31, 23, 59))
                .build();

        assertEquals(31, calculator.getActualDays());
        assertEquals(31, calculator.getTotalDaysInMonth());

        BigDecimal rentAmount = new BigDecimal("900.00");
        BigDecimal proratedAmount = calculator.calculateAmount(rentAmount, ProrationMode.PRORATED);

        // 900 × (31/31) = 900.00
        assertEquals(new BigDecimal("900.00"), proratedAmount);
    }

    @Test
    void shouldCalculateWaterMetersProportion() {
        // 5 m³ wody za 17 dni z 31
        ProrationCalculator calculator = ProrationCalculator.builder()
                .periodStartDate(LocalDateTime.of(2025, 1, 15, 0, 0))
                .periodEndDate(LocalDateTime.of(2025, 1, 31, 23, 59))
                .settlementDate(LocalDateTime.of(2025, 1, 31, 23, 59))
                .build();

        BigDecimal waterMeters = new BigDecimal("5.00");
        BigDecimal proratedWater = calculator.calculateAmount(waterMeters, ProrationMode.PRORATED);

        // 5 × (17/31) = 2.741... ≈ 2.74
        assertEquals(new BigDecimal("2.74"), proratedWater);
    }
}

