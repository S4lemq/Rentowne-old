package pl.rentowne.dashboard;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static pl.rentowne.dashboard.DashboardDto.*;

/** Read-only projections; every query is scoped to the authenticated property owner. */
@Repository
@RequiredArgsConstructor
class DashboardRepository {
    private final NamedParameterJdbcTemplate jdbc;

    Portfolio portfolio(long ownerId) {
        return jdbc.queryForObject("""
                SELECT (SELECT COUNT(*) FROM apartment WHERE user_account_id = :ownerId) AS apartments,
                       COUNT(*) AS total_units,
                       COALESCE(SUM(CASE WHEN ro.is_rented THEN 1 ELSE 0 END), 0) AS rented_units
                FROM rented_object ro
                JOIN apartment a ON a.apartment_id = ro.apartment_id
                WHERE a.user_account_id = :ownerId
                """, Map.of("ownerId", ownerId), (rs, n) -> new Portfolio(
                rs.getLong("apartments"), rs.getLong("rented_units"), rs.getLong("total_units")));
    }

    List<Tenancy> tenancies(long ownerId) {
        // Include ended tenancies: ending a lease does not clear its outstanding balance.
        return jdbc.query("""
                SELECT t.tenant_id, ro.rented_object_id, CONCAT(t.first_name, ' ', t.last_name) AS tenant_name,
                       a.apartment_name, ro.rented_object_name, t.active
                FROM tenant t
                JOIN rented_object ro ON ro.rented_object_id = t.rented_object_id
                JOIN apartment a ON a.apartment_id = ro.apartment_id
                WHERE a.user_account_id = :ownerId
                ORDER BY t.tenant_id
                """, Map.of("ownerId", ownerId), (rs, n) -> new Tenancy(
                rs.getLong("tenant_id"), rs.getLong("rented_object_id"), rs.getString("tenant_name"),
                rs.getString("apartment_name"), rs.getString("rented_object_name"), rs.getBoolean("active")));
    }

    Map<LocalDate, BigDecimal> receipts(long ownerId, LocalDate from, LocalDate through) {
        // Opening credits are not receipts. Deposit portions are held funds, not rental income.
        return jdbc.query("""
                SELECT CAST(date_trunc('month', p.payment_date) AS date) AS month,
                       COALESCE(SUM(p.amount - p.deposit_amount), 0) AS amount
                FROM rental_payment p
                JOIN rented_object ro ON ro.rented_object_id = p.rented_object_id
                JOIN apartment a ON a.apartment_id = ro.apartment_id
                WHERE a.user_account_id = :ownerId AND p.deleted = false
                  AND p.payment_date >= :from AND p.payment_date <= :through
                GROUP BY date_trunc('month', p.payment_date)
                """, period(ownerId, from, through), (rs, n) -> new AmountMonth(
                rs.getDate("month").toLocalDate(), rs.getBigDecimal("amount")))
                .stream().collect(Collectors.toMap(AmountMonth::month, AmountMonth::amount));
    }

    Map<LocalDate, BigDecimal> paidInvoices(long ownerId, LocalDate from, LocalDate through) {
        return jdbc.query("""
                SELECT CAST(date_trunc('month', i.payment_date) AS date) AS month,
                       COALESCE(SUM(i.amount), 0) AS amount
                FROM invoice i
                JOIN apartment a ON a.apartment_id = i.apartment_id
                WHERE a.user_account_id = :ownerId
                  AND i.payment_date >= :from AND i.payment_date <= :through
                GROUP BY date_trunc('month', i.payment_date)
                """, period(ownerId, from, through), (rs, n) -> new AmountMonth(
                rs.getDate("month").toLocalDate(), rs.getBigDecimal("amount")))
                .stream().collect(Collectors.toMap(AmountMonth::month, AmountMonth::amount));
    }

    Preview<InvoiceDue> invoicesDue(long ownerId, LocalDate today) {
        var rows = jdbc.query("""
                SELECT i.invoice_id, i.invoice_number, hp.name AS provider_name, a.apartment_name,
                       COALESCE(i.amount, 0) AS amount, i.pay_day, COUNT(*) OVER() AS total_count
                FROM invoice i
                JOIN apartment a ON a.apartment_id = i.apartment_id
                LEFT JOIN housing_provider hp ON hp.housing_provider_id = i.housing_provider_id
                WHERE a.user_account_id = :ownerId AND i.payment_date IS NULL AND i.pay_day <= :through
                ORDER BY i.pay_day, i.invoice_id
                LIMIT 5
                """, new MapSqlParameterSource("ownerId", ownerId)
                .addValue("through", Date.valueOf(today.plusDays(7))), (rs, n) -> new Counted<>(
                rs.getLong("total_count"), new InvoiceDue(rs.getLong("invoice_id"),
                rs.getString("invoice_number"), rs.getString("provider_name"), rs.getString("apartment_name"),
                rs.getBigDecimal("amount"), rs.getDate("pay_day").toLocalDate(),
                rs.getDate("pay_day").toLocalDate().isBefore(today))));
        return preview(rows);
    }

    Preview<ExpiringAgreement> expiringAgreements(long ownerId, LocalDate today) {
        var rows = jdbc.query("""
                SELECT t.tenant_id, CONCAT(t.first_name, ' ', t.last_name) AS tenant_name,
                       a.apartment_name, ro.rented_object_name, la.end_contract_date,
                       COUNT(*) OVER() AS total_count
                FROM lease_agreement la
                JOIN tenant t ON t.lease_agreement_id = la.lease_agreement_id
                JOIN rented_object ro ON ro.rented_object_id = t.rented_object_id
                JOIN apartment a ON a.apartment_id = ro.apartment_id
                WHERE a.user_account_id = :ownerId AND la.is_contract_active = true AND t.active = true
                  AND la.actual_termination_date IS NULL
                  AND la.end_contract_date >= :from AND la.end_contract_date < :until
                ORDER BY la.end_contract_date, t.tenant_id
                LIMIT 5
                """, new MapSqlParameterSource("ownerId", ownerId)
                .addValue("from", Date.valueOf(today)).addValue("until", Date.valueOf(today.plusDays(31))),
                (rs, n) -> new Counted<>(rs.getLong("total_count"), new ExpiringAgreement(
                rs.getLong("tenant_id"), rs.getString("tenant_name"), rs.getString("apartment_name"),
                rs.getString("rented_object_name"), rs.getDate("end_contract_date").toLocalDate())));
        return preview(rows);
    }

    private static MapSqlParameterSource period(long ownerId, LocalDate from, LocalDate through) {
        return new MapSqlParameterSource("ownerId", ownerId)
                .addValue("from", Date.valueOf(from)).addValue("through", Date.valueOf(through));
    }

    private static <T> Preview<T> preview(List<Counted<T>> rows) {
        return new Preview<>(rows.isEmpty() ? 0 : rows.getFirst().total(),
                rows.stream().map(Counted::item).toList());
    }

    record Portfolio(long apartments, long rentedUnits, long totalUnits) {}
    record Tenancy(long tenantId, long rentedObjectId, String tenantName, String apartmentName,
                   String rentedObjectName, boolean active) {}
    private record AmountMonth(LocalDate month, BigDecimal amount) {}
    private record Counted<T>(long total, T item) {}
}
