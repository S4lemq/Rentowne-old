package pl.rentowne.util;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.YearMonth;

/**
 * Implementacja Clock używająca rzeczywistego czasu systemowego.
 */
@Component
public class SystemClock implements Clock {

    @Override
    public YearMonth currentYearMonth() {
        return YearMonth.now();
    }

    @Override
    public LocalDate currentDate() {
        return LocalDate.now();
    }
}

