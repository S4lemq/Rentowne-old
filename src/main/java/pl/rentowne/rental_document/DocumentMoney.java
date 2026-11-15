package pl.rentowne.rental_document;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class DocumentMoney {
    private static final String[] ONES = {"", "jeden", "dwa", "trzy", "cztery", "pięć", "sześć", "siedem", "osiem", "dziewięć"};
    private static final String[] TEENS = {"dziesięć", "jedenaście", "dwanaście", "trzynaście", "czternaście", "piętnaście", "szesnaście", "siedemnaście", "osiemnaście", "dziewiętnaście"};
    private static final String[] TENS = {"", "", "dwadzieścia", "trzydzieści", "czterdzieści", "pięćdziesiąt", "sześćdziesiąt", "siedemdziesiąt", "osiemdziesiąt", "dziewięćdziesiąt"};
    private static final String[] HUNDREDS = {"", "sto", "dwieście", "trzysta", "czterysta", "pięćset", "sześćset", "siedemset", "osiemset", "dziewięćset"};
    private static final String[][] SCALES = {{"", "", ""}, {"tysiąc", "tysiące", "tysięcy"},
            {"milion", "miliony", "milionów"}, {"miliard", "miliardy", "miliardów"}, {"bilion", "biliony", "bilionów"}};

    private DocumentMoney() {}

    public static BigDecimal amount(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(2, RoundingMode.HALF_UP);
    }

    public static String format(BigDecimal value) {
        NumberFormat format = NumberFormat.getNumberInstance(Locale.forLanguageTag("pl-PL"));
        format.setMinimumFractionDigits(2);
        format.setMaximumFractionDigits(2);
        return format.format(amount(value)).replace('\u00a0', ' ') + " zł";
    }

    public static String words(BigDecimal value) {
        BigDecimal money = amount(value).abs();
        long whole = money.setScale(0, RoundingMode.DOWN).longValueExact();
        int cents = money.remainder(BigDecimal.ONE).movePointRight(2).intValueExact();
        return wordsForWhole(whole) + " " + plural(whole, "złoty", "złote", "złotych")
                + String.format(Locale.ROOT, " %02d/100", cents);
    }

    private static String wordsForWhole(long value) {
        if (value == 0) return "zero";
        List<String> groups = new ArrayList<>();
        for (int scale = 0; value > 0; scale++, value /= 1000) {
            int group = (int) (value % 1000);
            if (group == 0) continue;
            String text = hundreds(group);
            if (scale > 0) text = (group == 1 ? "" : text + " ")
                    + plural(group, SCALES[scale][0], SCALES[scale][1], SCALES[scale][2]);
            groups.addFirst(text);
        }
        return String.join(" ", groups);
    }

    private static String hundreds(int value) {
        List<String> parts = new ArrayList<>();
        if (value >= 100) parts.add(HUNDREDS[value / 100]);
        int rest = value % 100;
        if (rest >= 10 && rest < 20) parts.add(TEENS[rest - 10]);
        else {
            if (rest >= 20) parts.add(TENS[rest / 10]);
            if (rest % 10 > 0) parts.add(ONES[rest % 10]);
        }
        return String.join(" ", parts);
    }

    private static String plural(long number, String one, String few, String many) {
        if (number == 1) return one;
        return number % 10 >= 2 && number % 10 <= 4 && (number % 100 < 12 || number % 100 > 14) ? few : many;
    }

    public static String account(String account) {
        if (account == null) return "";
        String value = account.replaceAll("[\\s\\p{Z}]", "").replaceFirst("(?i)^PL", "");
        if (value.length() != 26) return account;
        return value.substring(0, 2) + " " + String.join(" ", value.substring(2).split("(?<=\\G.{4})"));
    }
}
