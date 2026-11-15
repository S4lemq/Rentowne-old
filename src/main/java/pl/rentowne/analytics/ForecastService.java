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

import static java.math.BigDecimal.ZERO;
import static pl.rentowne.analytics.AnalyticsDto.*;
import static pl.rentowne.analytics.FinancialAnalyticsRepository.*;
import static pl.rentowne.analytics.FinancialAnalyticsService.money;

@Service
@RequiredArgsConstructor
@Transactional(readOnly=true, isolation=Isolation.REPEATABLE_READ)
class ForecastService {
    private final FinancialAnalyticsRepository repository;
    private final FinancialAnalyticsService analytics;
    private final UserService users;
    private final Clock clock;

    public Forecast forecast(int horizon, Long apartmentId, boolean continuation, BigDecimal costChange) {
        if (!List.of(3,6,12).contains(horizon) || costChange.compareTo(new BigDecimal("-50"))<0
                || costChange.compareTo(new BigDecimal("100"))>0 || costChange.scale()>2) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Sprawdź horyzont i zmianę kosztów prognozy.");
        }
        long owner=users.getLoggedUser().id();
        var apartments=analytics.ownedApartments(owner,apartmentId);
        LocalDate today=LocalDate.now(clock), current=today.withDayOfMonth(1), start=current.plusMonths(1), until=start.plusMonths(horizon);
        var leases=repository.leases(owner,apartmentId);
        var settlements=repository.settlementMonths(owner,apartmentId,current.minusMonths(6),until);
        var invoices=repository.invoiceMonths(owner,apartmentId,current.minusMonths(6),until);
        var providers=repository.providers(owner,apartmentId);
        Map<Long,List<Lease>> byObject=leases.stream().collect(Collectors.groupingBy(Lease::objectId));
        Map<Long,Map<LocalDate,BigDecimal>> utilities=new HashMap<>();
        Map<TenancyMonth,SettlementMonth> recorded=new HashMap<>();
        for(var row:settlements) {
            if(row.month().isBefore(current)) utilities.computeIfAbsent(row.objectId(),k->new TreeMap<>()).merge(row.month(),row.utilities(),BigDecimal::add);
            else if(!row.month().isBefore(start)) recorded.put(new TenancyMonth(row.tenantId(),row.objectId(),row.month()),row);
        }
        Map<Pair,Map<LocalDate,BigDecimal>> costs=new HashMap<>();
        for(var row:invoices) costs.computeIfAbsent(new Pair(row.apartmentId(),row.providerId()),k->new TreeMap<>()).merge(row.month(),row.amount(),BigDecimal::add);
        Map<Pair,Provider> configured=new HashMap<>();
        providers.forEach(p->configured.put(new Pair(p.apartmentId(),p.id()),p));
        Set<Pair> pairs=new HashSet<>(costs.keySet()); pairs.addAll(configured.keySet());
        Map<Long,String> names=apartments.stream().collect(Collectors.toMap(Apartment::id,Apartment::name));
        List<String> assumptions=new ArrayList<>(List.of(
                "Stałe opłaty wynikają z zapisanych umów; niepełne miesiące liczymy proporcjonalnie do dni.",
                "Prąd i woda: średnia dla obiektu z ostatnich 6 zakończonych miesięcy, przy co najmniej 3 miesiącach z rozliczeniami.",
                "Koszty cykliczne według konfiguracji dostawców; pozostałe według historii faktur z ostatnich 6 zakończonych miesięcy.",
                "W średnich uwzględniamy miesiące bez zapisów od pierwszego zapisu w analizowanym oknie. Brak zapisu nie potwierdza braku kosztu.",
                "Wprowadzone przyszłe rozliczenie lub faktura zastępuje szacunek dla tej samej umowy lub dostawcy i miesiąca.",
                "Kaucje i jednorazowe naliczenia nie są powielane. Podatek to osobny szacunek 8,5% od odstępnego.",
                continuation ? "Symulacja: kontynuacja najmu na dotychczasowych warunkach; zapisana kolejna umowa ma pierwszeństwo." : "Uwzględniamy wyłącznie okresy zapisanych umów, bez automatycznego przedłużania.",
                "Zmiana kosztów o " + costChange.stripTrailingZeros().toPlainString() + "% dotyczy tylko szacunków, nie zapisanych faktur."
        ));
        List<ForecastMonth> points=new ArrayList<>();
        BigDecimal costFactor=BigDecimal.ONE.add(costChange.divide(new BigDecimal("100"),6,RoundingMode.HALF_UP));
        for(LocalDate month=start;month.isBefore(until);month=month.plusMonths(1)) {
            final LocalDate selected=month;
            Set<String> missing=new TreeSet<>();
            Map<TenancyMonth,Projection> projected=new HashMap<>();
            for(var object:byObject.entrySet()) {
                // Pick at most one lease per unit/day. A later recorded contract supersedes a simulated extension.
                Map<Lease,Integer> days=new HashMap<>();
                for(LocalDate day=month;day.isBefore(month.plusMonths(1));day=day.plusDays(1)) {
                    Lease lease=leaseOn(object.getValue(),day,continuation);
                    if(lease!=null) days.merge(lease,1,Integer::sum);
                }
                for(var entry:days.entrySet()) {
                    Lease lease=entry.getKey();
                    var key=new TenancyMonth(lease.tenantId(),lease.objectId(),month);
                    if(recorded.containsKey(key)) continue;
                    BigDecimal share=BigDecimal.valueOf(entry.getValue()).divide(BigDecimal.valueOf(month.lengthOfMonth()),10,RoundingMode.HALF_UP);
                    BigDecimal media=average(utilities.getOrDefault(lease.objectId(),Map.of()),current);
                    if(media==null) missing.add(lease.apartmentName()+" · "+lease.objectName()+": brak 3 miesięcy historii prądu i wody");
                    projected.put(key,new Projection(lease.apartmentId(),money(lease.fixed().add(media==null?ZERO:media).multiply(share)),money(lease.compensation().multiply(share))));
                }
            }
            recorded.forEach((key,row)->{ if(key.month().equals(selected)) projected.put(key,new Projection(row.apartmentId(),row.amount(),row.compensation())); });
            BigDecimal revenue=projected.values().stream().map(Projection::amount).reduce(ZERO,BigDecimal::add);
            Map<Long,BigDecimal> compensation=new HashMap<>();
            projected.values().forEach(p->compensation.merge(p.apartmentId(),p.compensation(),BigDecimal::add));
            BigDecimal tax=compensation.values().stream().map(v->money(v.multiply(FinancialAnalyticsService.TAX_RATE))).reduce(ZERO,BigDecimal::add);
            BigDecimal expenses=ZERO;
            for(Pair pair:pairs) {
                var history=costs.getOrDefault(pair,Map.of());
                BigDecimal value=history.get(month);
                if(value==null) {
                    Provider provider=configured.get(pair);
                    value=provider!=null && provider.recurring() ? provider.amount() : average(history,current);
                    if(value==null) missing.add(names.get(pair.apartmentId())+": brak 3 miesięcy kosztów dla "+(provider==null?"dostawcy bez aktualnej konfiguracji":provider.name()));
                    else value=money(value.multiply(costFactor));
                }
                if(value!=null) expenses=expenses.add(value);
            }
            for(var apartment:apartments) if(pairs.stream().noneMatch(p->p.apartmentId()==apartment.id())) {
                missing.add(apartment.name()+": brak historii kosztów i konfiguracji dostawców");
            }
            boolean complete=missing.isEmpty();
            BigDecimal result=complete?money(revenue.subtract(expenses)):null;
            points.add(new ForecastMonth(month,money(revenue),money(expenses),result,money(tax),complete?money(result.subtract(tax)):null,complete,List.copyOf(missing)));
        }
        var ending=leases.stream().filter(l->!effectiveEnd(l).isBefore(today) && effectiveEnd(l).isBefore(until))
                .sorted(Comparator.comparing(ForecastService::effectiveEnd)).map(l->new EndingLease(l.tenantName(),l.apartmentName(),l.objectName(),effectiveEnd(l),l.fixed())).toList();
        var history=analytics.overview(current.minusMonths(5),current,apartmentId,Basis.ACCRUAL).months();
        return new Forecast(today,horizon,continuation,costChange,history,points,ending,assumptions,points.stream().allMatch(ForecastMonth::complete));
    }

    private static Lease leaseOn(List<Lease> leases, LocalDate day, boolean continuation) {
        Comparator<Lease> order=Comparator.comparing(Lease::start).thenComparingLong(Lease::tenantId);
        var actual=leases.stream().filter(l->!day.isBefore(l.start()) && !day.isAfter(effectiveEnd(l))).max(order);
        if(actual.isPresent() || !continuation) return actual.orElse(null);
        return leases.stream().filter(l->l.termination()==null && !day.isBefore(l.start()) && day.isAfter(l.end())).max(order).orElse(null);
    }
    private static LocalDate effectiveEnd(Lease lease) { return lease.termination()!=null && lease.termination().isBefore(lease.end()) ? lease.termination() : lease.end(); }
    private static BigDecimal average(Map<LocalDate,BigDecimal> history,LocalDate current) {
        var rows=history.entrySet().stream().filter(e->e.getKey().isBefore(current) && !e.getKey().isBefore(current.minusMonths(6))).toList();
        if(rows.size()<3) return null;
        LocalDate first=rows.stream().map(Map.Entry::getKey).min(LocalDate::compareTo).orElseThrow();
        return rows.stream().map(Map.Entry::getValue).reduce(ZERO,BigDecimal::add).divide(BigDecimal.valueOf(ChronoUnit.MONTHS.between(first,current)),2,RoundingMode.HALF_UP);
    }
    private record Pair(long apartmentId,long providerId) {}
    private record TenancyMonth(long tenantId,long objectId,LocalDate month) {}
    private record Projection(long apartmentId,BigDecimal amount,BigDecimal compensation) {}
}
