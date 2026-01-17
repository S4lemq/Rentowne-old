package pl.rentowne.util;

import pl.rentowne.exception.RentowneBusinessException;
import pl.rentowne.exception.RentowneErrorCode;
import pl.rentowne.meter.model.MeterType;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;


/**
 * Klasa pomocnicza dla metod ułatwiających obsługę dat
 */
public class DateUtils {
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMMM yyyy", new Locale("pl", "PL"));
    private static final Map<String, String> monthReplacements = Map.ofEntries(
            Map.entry("stycznia", "styczeń"),
            Map.entry("lutego", "luty"),
            Map.entry("marca", "marzec"),
            Map.entry("kwietnia", "kwiecień"),
            Map.entry("maja", "maj"),
            Map.entry("czerwca", "czerwiec"),
            Map.entry("lipca", "lipiec"),
            Map.entry("sierpnia", "sierpień"),
            Map.entry("września", "wrzesień"),
            Map.entry("października", "październik"),
            Map.entry("listopada", "listopad"),
            Map.entry("grudnia", "grudzień")
    );

    private DateUtils() {}

    public static void compareDates(LocalDateTime readingDate, LocalDateTime previousReadingDate, boolean isTenant, MeterType meterType) throws RentowneBusinessException {
        if (MeterType.WATER_COLD.equals(meterType)) {
            if (readingDate.isBefore(previousReadingDate) || readingDate.isEqual(previousReadingDate)) {
                throw new RentowneBusinessException(RentowneErrorCode.BAD_METER_READING_DATE);
            }
            return;
        }

        if (readingDate.isBefore(previousReadingDate) || readingDate.isEqual(previousReadingDate)) {
            if (isTenant) {
                throw new RentowneBusinessException(RentowneErrorCode.BAD_METER_READING_DATE_TENANT);
            } else {
                throw new RentowneBusinessException(RentowneErrorCode.BAD_METER_READING_DATE);
            }
        }
    }

    public static String formatPolishDate(LocalDateTime date) {
        if (date == null) {
            return null;
        }
        String formattedDate = date.format(formatter);
        for (Map.Entry<String, String> entry : monthReplacements.entrySet()) {
            if (formattedDate.contains(entry.getKey())) {
                formattedDate = formattedDate.replace(entry.getKey(), entry.getValue());
                break;
            }
        }
        return formattedDate;
    }

    public static String getPolishMonthName(final int month) {
        return switch (month) {
            case 1 -> "stycznia";
            case 2 -> "lutego";
            case 3 -> "marca";
            case 4 -> "kwietnia";
            case 5 -> "maja";
            case 6 -> "czerwca";
            case 7 -> "lipca";
            case 8 -> "sierpnia";
            case 9 -> "września";
            case 10 -> "października";
            case 11 -> "listopada";
            case 12 -> "grudnia";
            default -> "";
        };
    }
}
