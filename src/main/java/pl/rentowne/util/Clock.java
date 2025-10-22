package pl.rentowne.util;

import java.time.LocalDate;
import java.time.YearMonth;

/**
 * Interfejs służący do dostarczania bieżącej daty.
 * Umożliwia mockowanie czasu w testach.
 */
public interface Clock {

    /**
     * Zwraca bieżący miesiąc i rok
     */
    YearMonth currentYearMonth();

    /**
     * Zwraca bieżącą datę
     */
    LocalDate currentDate();

    /**
     * Zwraca pierwszy dzień bieżącego miesiąca
     */
    default LocalDate currentMonthFirstDay() {
        return currentYearMonth().atDay(1);
    }
}

