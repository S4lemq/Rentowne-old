package pl.rentowne.dashboard;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import pl.rentowne.rental_payment.RentalBalanceDto;
import pl.rentowne.rental_payment.RentalPaymentService;
import pl.rentowne.user.UserService;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;

import static pl.rentowne.dashboard.DashboardDto.*;

@Service
@RequiredArgsConstructor
class DashboardService {
    private final DashboardRepository repository;
    private final RentalPaymentService payments;
    private final UserService userService;
    private final Clock clock;

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public DashboardDto getDashboard(int months) {
        if (months != 6 && months != 12) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Zakres musi wynosić 6 lub 12 miesięcy.");
        }
        long ownerId = userService.getLoggedUser().id();
        LocalDate today = LocalDate.now(clock);
        LocalDate currentMonth = today.withDayOfMonth(1);
        LocalDate firstMonth = currentMonth.minusMonths(months - 1L);
        var portfolio = repository.portfolio(ownerId);
        var receipts = repository.receipts(ownerId, firstMonth, today);
        var invoices = repository.paidInvoices(ownerId, firstMonth, today);
        var trend = new ArrayList<Month>(months);
        for (int i = 0; i < months; i++) {
            LocalDate month = firstMonth.plusMonths(i);
            trend.add(new Month(month, receipts.getOrDefault(month, BigDecimal.ZERO),
                    invoices.getOrDefault(month, BigDecimal.ZERO)));
        }

        var tenancies = repository.tenancies(ownerId);
        var balances = payments.getBalances(tenancies.stream().map(DashboardRepository.Tenancy::tenantId).toList());
        var dueTenants = new ArrayList<TenantDue>();
        BigDecimal totalDue = BigDecimal.ZERO;
        for (var tenant : tenancies) {
            RentalBalanceDto balance = balances.get(tenant.tenantId());
            if (balance != null && balance.amountDue().signum() > 0) {
                totalDue = totalDue.add(balance.amountDue());
                dueTenants.add(new TenantDue(tenant.tenantId(), tenant.rentedObjectId(), tenant.tenantName(),
                        tenant.apartmentName(), tenant.rentedObjectName(), tenant.active(), balance.amountDue()));
            }
        }
        dueTenants.sort(Comparator.comparing(TenantDue::amountDue).reversed().thenComparingLong(TenantDue::tenantId));

        return new DashboardDto(today, months, new Summary(
                receipts.getOrDefault(currentMonth, BigDecimal.ZERO),
                invoices.getOrDefault(currentMonth, BigDecimal.ZERO), totalDue, dueTenants.size(),
                portfolio.apartments(), portfolio.rentedUnits(), portfolio.totalUnits()),
                trend, new Attention(new Preview<>(dueTenants.size(), dueTenants.stream().limit(5).toList()),
                repository.invoicesDue(ownerId, today), repository.expiringAgreements(ownerId, today)));
    }
}
