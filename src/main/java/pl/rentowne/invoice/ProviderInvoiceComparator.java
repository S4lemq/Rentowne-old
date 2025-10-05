package pl.rentowne.invoice;

import org.apache.commons.text.similarity.LevenshteinDistance;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.rentowne.housing_provider.model.ProviderType;
import pl.rentowne.housing_provider.repository.HousingProviderRepository;
import pl.rentowne.provider_field.model.ProviderField;
import pl.rentowne.provider_field.repository.ProviderFieldRepository;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;


@Service
public class ProviderInvoiceComparator {

    private final HousingProviderRepository providerRepo;
    private final ProviderFieldRepository fieldRepo;

    public ProviderInvoiceComparator(HousingProviderRepository providerRepo, ProviderFieldRepository fieldRepo) {
        this.providerRepo = providerRepo;
        this.fieldRepo = fieldRepo;
    }

    @Transactional(readOnly = true)
    public ComparisonResponse compare(Long providerId, List<InvoiceLine> pdfLines) {
        var provider = providerRepo.findById(providerId)
                .orElseThrow(() -> new NoSuchElementException("Nie znaleziono housing_provider: " + providerId));

        if (provider.getType() != ProviderType.ELECTRICITY) {
            throw new IllegalArgumentException("Porównanie dozwolone tylko dla ProviderType=ELECTRICITY");
        }

        var dbFields = fieldRepo.findByHousingProviderId(providerId);

        // Mapowanie po nazwie (z normalizacją)
        Map<String, ProviderField> dbByName = dbFields.stream().collect(Collectors.toMap(
                f -> normalize(f.getName()), Function.identity(), (a, b) -> a, LinkedHashMap::new));

        List<FieldDiff> diffs = new ArrayList<>();
        List<String> notes = new ArrayList<>();

        // 1) sprawdź pozycje z PDF vs DB
        for (InvoiceLine line : pdfLines) {
            String key = normalize(line.name());
            ProviderField db = dbByName.get(key);

            if (db == null) {
                // spróbuj lekkiego fuzzy na 1 najbliższym kandydacie
                var nearest = nearestByName(dbFields, line.name());
                if (nearest != null && distance(normalize(nearest.getName()), key) <= 3) {
                    // jest prawie to samo → potraktuj jako porównywalne
                    db = nearest;
                    notes.add("Dopasowano rozmycie nazwy: '%s' ~ '%s'".formatted(line.name(), nearest.getName()));
                }
            }

            if (db == null) {
                diffs.add(new FieldDiff(line.name(), null, line.price(), null, line.billingMethod(), "NAME_ONLY_IN_PDF"));
                continue;
            }

            boolean priceChanged = comparePrice(db.getPrice(), line.price());
            boolean billingChanged = db.getBillingMethod() != line.billingMethod();

            if (priceChanged || billingChanged) {
                diffs.add(new FieldDiff(
                        db.getName(),
                        db.getPrice(),
                        line.price(),
                        db.getBillingMethod(),
                        line.billingMethod(),
                        priceChanged && billingChanged ? "PRICE_CHANGED|BILLING_CHANGED" :
                                priceChanged ? "PRICE_CHANGED" : "BILLING_CHANGED"
                ));
            }
        }

        // 2) pozycje z DB nieobecne w PDF
        Set<String> pdfKeys = pdfLines.stream().map(l -> normalize(l.name())).collect(Collectors.toSet());
        for (ProviderField f : dbFields) {
            if (!pdfKeys.contains(normalize(f.getName()))) {
                diffs.add(new FieldDiff(f.getName(), f.getPrice(), null, f.getBillingMethod(), null, "NAME_ONLY_IN_DB"));
            }
        }

        return new ComparisonResponse(
                provider.getId(),
                provider.getName(),
                provider.getType(),
                pdfLines,
                diffs,
                notes
        );
    }

    private static String normalize(String s) {
        return s == null ? "" : s.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
    }

    private static int distance(String a, String b) {
        return LevenshteinDistance.getDefaultInstance().apply(a, b);
    }

    private static ProviderField nearestByName(List<ProviderField> all, String target) {
        String nt = normalize(target);
        return all.stream()
                .min(Comparator.comparingInt(f -> distance(normalize(f.getName()), nt)))
                .orElse(null);
    }

    private static boolean comparePrice(BigDecimal a, BigDecimal b) {
        if (a == null || b == null) return a != b;
        // tolerancja 0.0001
        return a.subtract(b).abs().compareTo(new BigDecimal("0.0001")) > 0;
    }
}

