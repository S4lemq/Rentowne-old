package pl.rentowne.analytics;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import pl.rentowne.user.UserService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

import static pl.rentowne.analytics.AnalyticsDto.*;
import static java.math.BigDecimal.ZERO;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
public class FinancialAnalyticsService {
    static final BigDecimal TAX_RATE = new BigDecimal("0.085");
    private final FinancialAnalyticsRepository repository;
    private final UserService users;
    private final Clock clock;

    public Overview overview(LocalDate from, LocalDate to, Long apartmentId, Basis basis) {
        return report(from, to, apartmentId, basis, false);
    }

    public Overview report(LocalDate from, LocalDate to, Long apartmentId, Basis basis, boolean includeFuture) {
        long owner = users.getLoggedUser().id();
        var apartments = ownedApartments(owner, apartmentId);
        LocalDate today = LocalDate.now(clock), current = today.withDayOfMonth(1);
        to = to == null ? current : to.withDayOfMonth(1);
        if (!includeFuture && to.isAfter(current)) to = current;
        LocalDate first = repository.firstMonth(owner, apartmentId, basis, includeFuture ? to.plusMonths(1).minusDays(1) : today);
        from = from == null ? (first == null ? current : first) : from.withDayOfMonth(1);
        if (from.isAfter(to) || from.getYear() < 1900 || to.getYear() > 2200 || ChronoUnit.MONTHS.between(from,to) > 1200) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nieprawidłowy zakres miesięcy.");
        }
        LocalDate comparisonTo = to.isBefore(current) ? to : current.minusMonths(1);
        LocalDate comparisonFrom = from.isAfter(comparisonTo.minusMonths(11)) ? from : comparisonTo.minusMonths(11);
        LocalDate until = to.plusMonths(1);
        if (basis == Basis.CASH && until.isAfter(today.plusDays(1))) until = today.plusDays(1);
        var entries = repository.entries(owner, apartmentId, basis, from.minusYears(1), until);
        List<Property> properties = new ArrayList<>();
        for (var apartment : apartments) {
            var own = entries.stream().filter(e -> e.apartmentId() == apartment.id()).toList();
            List<Month> months = months(own, from, to, current, basis);
            var costs = categories(own, from, to);
            properties.add(new Property(apartment.id(), apartment.name(), sum(months.stream().map(Month::totals).toList(), basis), months, costs));
        }
        properties.sort(Comparator.comparing((Property p) -> p.totals().result()).reversed().thenComparing(Property::name));
        List<Month> months = new ArrayList<>();
        BigDecimal cumulative = ZERO;
        for (LocalDate month = from; !month.isAfter(to); month = month.plusMonths(1)) {
            final LocalDate selected = month;
            var rows = properties.stream().flatMap(p -> p.months().stream()).filter(m -> m.month().equals(selected)).toList();
            var totals = sum(rows.stream().map(Month::totals).toList(), basis);
            cumulative = cumulative.add(totals.result());
            months.add(new Month(month, totals, cumulative, rows.stream().anyMatch(Month::hasRecords), month.equals(current), month.isAfter(current)));
        }
        return new Overview(today,from,to,first,basis,repository.apartments(owner),sum(months.stream().map(Month::totals).toList(),basis),
                months,categories(entries,from,to),properties,comparison(entries,comparisonFrom,comparisonTo,current,basis));
    }

    List<Apartment> ownedApartments(long owner, Long id) {
        var all = repository.apartments(owner);
        if (id != null && all.stream().noneMatch(a -> a.id() == id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Nie znaleziono nieruchomości.");
        }
        return id == null ? all : all.stream().filter(a -> a.id() == id).toList();
    }

    private List<Month> months(List<FinancialAnalyticsRepository.Entry> entries, LocalDate from, LocalDate to, LocalDate current, Basis basis) {
        var grouped = entries.stream().collect(Collectors.groupingBy(FinancialAnalyticsRepository.Entry::month));
        List<Month> result = new ArrayList<>();
        BigDecimal cumulative = ZERO;
        for (LocalDate date = from; !date.isAfter(to); date = date.plusMonths(1)) {
            var rows = grouped.getOrDefault(date,List.of());
            BigDecimal revenue=ZERO,costs=ZERO,compensation=ZERO;
            for (var row : rows) {
                if (row.category().equals("REVENUE")) { revenue=revenue.add(row.amount()); compensation=compensation.add(row.compensation()); }
                else costs=costs.add(row.amount());
            }
            var value=totals(revenue,costs,basis == Basis.CASH ? null : money(compensation.multiply(TAX_RATE)));
            cumulative=cumulative.add(value.result());
            result.add(new Month(date,value,cumulative,!rows.isEmpty(),date.equals(current),date.isAfter(current)));
        }
        return result;
    }

    private List<Category> categories(List<FinancialAnalyticsRepository.Entry> entries, LocalDate from, LocalDate to) {
        Map<String,BigDecimal> sums = new TreeMap<>();
        entries.stream().filter(e -> !e.category().equals("REVENUE") && !e.month().isBefore(from) && !e.month().isAfter(to))
                .forEach(e -> sums.merge(e.category(),e.amount(),BigDecimal::add));
        BigDecimal total=sums.values().stream().reduce(ZERO,BigDecimal::add);
        return sums.entrySet().stream().map(e -> new Category(e.getKey(),money(e.getValue()),percent(e.getValue(),total)))
                .sorted(Comparator.comparing(Category::amount).reversed()).toList();
    }

    private Comparison comparison(List<FinancialAnalyticsRepository.Entry> entries, LocalDate from, LocalDate to, LocalDate current, Basis basis) {
        if (to.isBefore(from)) return new Comparison(List.of(),ZERO,ZERO,null,false);
        // Round tax at property/month level in every view. Comparisons use the before-tax result.
        var now=months(entries,from,to,current,basis);
        var previous=months(entries,from.minusYears(1),to.minusYears(1),current,basis);
        List<ComparisonMonth> rows=new ArrayList<>();
        for(int i=0;i<now.size();i++) rows.add(new ComparisonMonth(now.get(i).month(),now.get(i).totals().result(),
                previous.get(i).totals().result(),now.get(i).hasRecords(),previous.get(i).hasRecords()));
        BigDecimal nowTotal=now.stream().map(m -> m.totals().result()).reduce(ZERO,BigDecimal::add);
        BigDecimal previousTotal=previous.stream().map(m -> m.totals().result()).reduce(ZERO,BigDecimal::add);
        boolean available=previous.stream().anyMatch(Month::hasRecords) && now.stream().anyMatch(Month::hasRecords);
        return new Comparison(rows,nowTotal,previousTotal,available ? percent(nowTotal.subtract(previousTotal),previousTotal.abs()) : null,available);
    }

    public static Totals sum(List<Totals> rows, Basis basis) {
        BigDecimal revenue=ZERO,costs=ZERO,tax=ZERO;
        for(var row:rows) { revenue=revenue.add(row.revenue()); costs=costs.add(row.costs()); if(row.estimatedTax()!=null) tax=tax.add(row.estimatedTax()); }
        return totals(revenue,costs,basis==Basis.CASH ? null : tax);
    }
    public static Totals totals(BigDecimal revenue,BigDecimal costs,BigDecimal tax) {
        BigDecimal result=revenue.subtract(costs);
        return new Totals(money(revenue),money(costs),money(result),percent(result,revenue),tax==null?null:money(tax),tax==null?null:money(result.subtract(tax)));
    }
    static BigDecimal money(BigDecimal value) { return value.setScale(2,RoundingMode.HALF_UP); }
    private static BigDecimal percent(BigDecimal value,BigDecimal base) { return base.signum()>0 ? value.multiply(BigDecimal.valueOf(100)).divide(base,2,RoundingMode.HALF_UP) : null; }
}
