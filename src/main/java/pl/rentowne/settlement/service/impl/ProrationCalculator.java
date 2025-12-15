package pl.rentowne.settlement.service.impl;

import lombok.Builder;
import lombok.Getter;
import pl.rentowne.settlement.model.ProrationMode;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;

/**
 * Klasa pomocnicza do obliczania proporcji dla rozliczenia niepełnego miesiąca
 */
@Getter
@Builder
public class ProrationCalculator {
    private LocalDateTime periodStartDate;
    private LocalDateTime periodEndDate;
    private LocalDateTime settlementDate;

    /**
     * Oblicza liczbę dni w okresie rozliczeniowym (włącznie)
     */
    public long getActualDays() {
        return ChronoUnit.DAYS.between(periodStartDate.toLocalDate(), periodEndDate.toLocalDate()) + 1;
    }

    /**
     * Oblicza liczbę dni w całym miesiącu rozliczeniowym
     */
    public int getTotalDaysInMonth() {
        YearMonth yearMonth = YearMonth.from(settlementDate);
        return yearMonth.lengthOfMonth();
    }

    /**
     * Oblicza współczynnik proporcji (dni faktyczne / dni w miesiącu)
     */
    public BigDecimal getProrationFactor() {
        long actualDays = getActualDays();
        int totalDays = getTotalDaysInMonth();
        return BigDecimal.valueOf(actualDays).divide(BigDecimal.valueOf(totalDays), 6, RoundingMode.HALF_UP);
    }

    /**
     * Oblicza kwotę proporcjonalną lub pełną w zależności od trybu
     */
    public BigDecimal calculateAmount(BigDecimal fullAmount, ProrationMode mode) {
        if (mode == null || mode == ProrationMode.FULL) {
            return fullAmount;
        }

        BigDecimal prorationFactor = getProrationFactor();
        return fullAmount.multiply(prorationFactor).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Generuje opis okresu rozliczeniowego (np. "15-31.01.2025")
     */
    public String getPeriodDescription() {
        int startDay = periodStartDate.getDayOfMonth();
        int endDay = periodEndDate.getDayOfMonth();
        int month = settlementDate.getMonthValue();
        int year = settlementDate.getYear();

        return String.format("%02d-%02d.%02d.%d", startDay, endDay, month, year);
    }
}

