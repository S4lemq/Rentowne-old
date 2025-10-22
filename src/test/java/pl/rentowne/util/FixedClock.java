package pl.rentowne.util;

import java.time.LocalDate;
import java.time.YearMonth;

/**
 * Implementacja Clock z ustaloną datą, używana w testach.
 */
public class FixedClock implements Clock {

    private final LocalDate fixedDate;

    public FixedClock(LocalDate fixedDate) {
        this.fixedDate = fixedDate;
    }

    @Override
    public YearMonth currentYearMonth() {
        return YearMonth.from(fixedDate);
    }

    @Override
    public LocalDate currentDate() {
        return fixedDate;
    }

    /**
     * Tworzy FixedClock dla konkretnego roku i miesiąca (pierwszy dzień miesiąca)
     */
    public static FixedClock of(int year, int month) {
        return new FixedClock(LocalDate.of(year, month, 1));
    }
}

