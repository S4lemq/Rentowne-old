package pl.rentowne.analytics;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static pl.rentowne.analytics.AnalyticsDto.*;

@Repository
@RequiredArgsConstructor
class FinancialAnalyticsRepository {
    private final NamedParameterJdbcTemplate jdbc;

    List<Apartment> apartments(long owner) {
        return jdbc.query("SELECT apartment_id, apartment_name FROM apartment WHERE user_account_id=:owner ORDER BY apartment_name, apartment_id",
                Map.of("owner", owner), (r, n) -> new Apartment(r.getLong(1), r.getString(2)));
    }

    LocalDate firstMonth(long owner, Long apartment, Basis basis, LocalDate through) {
        String revenue = basis == Basis.CASH
                ? "SELECT p.payment_date AS day FROM rental_payment p JOIN rented_object ro ON ro.rented_object_id=p.rented_object_id JOIN apartment a ON a.apartment_id=ro.apartment_id WHERE p.deleted=false"
                : "SELECT s.settlement_date AS day FROM settlement s JOIN rented_object ro ON ro.rented_object_id=s.rented_object_id JOIN apartment a ON a.apartment_id=ro.apartment_id WHERE true";
        String costDate = basis == Basis.CASH ? "payment_date" : "pay_day_month";
        return jdbc.queryForObject("SELECT MIN(day) FROM (" + revenue + scope(apartment)
                        + " UNION ALL SELECT i." + costDate + " AS day FROM invoice i JOIN apartment a ON a.apartment_id=i.apartment_id WHERE true"
                        + scope(apartment) + ") dates WHERE day < :until", params(owner, apartment).addValue("until", Date.valueOf(through.plusDays(1))),
                (r, n) -> r.getDate(1) == null ? null : r.getDate(1).toLocalDate().withDayOfMonth(1));
    }

    List<Entry> entries(long owner, Long apartment, Basis basis, LocalDate from, LocalDate until) {
        String source = basis == Basis.CASH
                ? "rental_payment s JOIN rented_object ro ON ro.rented_object_id=s.rented_object_id"
                : "settlement s JOIN rented_object ro ON ro.rented_object_id=s.rented_object_id";
        String day = basis == Basis.CASH ? "s.payment_date" : "s.settlement_date";
        String amount = basis == Basis.CASH ? "s.amount-s.deposit_amount" : "COALESCE(s.total_amount,0)-s.deposit_top_up";
        String compensation = basis == Basis.CASH ? "0" : "COALESCE(s.compensation_amount,0)";
        String date = basis == Basis.CASH ? "i.payment_date" : "i.pay_day_month";
        var values = params(owner, apartment).addValue("from", Date.valueOf(from)).addValue("until", Date.valueOf(until));
        String revenue = "SELECT a.apartment_id, CAST(date_trunc('month', " + day + ") AS date) AS month, 'REVENUE' AS category, SUM(" + amount + ") AS amount, SUM(" + compensation + ") AS compensation, COUNT(*) AS records FROM "
                + source + " JOIN apartment a ON a.apartment_id=ro.apartment_id WHERE " + day + ">=:from AND " + day + "<:until"
                + (basis == Basis.CASH ? " AND s.deleted=false" : "") + scope(apartment) + " GROUP BY a.apartment_id, date_trunc('month', " + day + ")";
        String costs = "SELECT a.apartment_id, CAST(date_trunc('month', " + date + ") AS date) AS month, COALESCE(h.type,'OTHER') AS category, COALESCE(SUM(i.amount),0) AS amount, 0 AS compensation, COUNT(*) AS records FROM invoice i JOIN apartment a ON a.apartment_id=i.apartment_id LEFT JOIN housing_provider h ON h.housing_provider_id=i.housing_provider_id WHERE "
                + date + ">=:from AND " + date + "<:until" + scope(apartment) + " GROUP BY a.apartment_id, date_trunc('month', " + date + "), COALESCE(h.type,'OTHER')";
        return jdbc.query(revenue + " UNION ALL " + costs, values, (r, n) -> new Entry(r.getLong("apartment_id"),
                r.getDate("month").toLocalDate(), r.getString("category"), r.getBigDecimal("amount"), r.getBigDecimal("compensation")));
    }

    List<Lease> leases(long owner, Long apartment) {
        return jdbc.query("""
                SELECT t.tenant_id, ro.rented_object_id, a.apartment_id, a.apartment_name, ro.rented_object_name,
                       CONCAT(t.first_name,' ',t.last_name) AS tenant_name, la.start_contract_date, la.end_contract_date,
                       la.actual_termination_date, la.compensation_amount,
                       la.compensation_amount+la.rent_amount+COALESCE(la.internet_fee,0)+COALESCE(la.gas_deposit,0) AS fixed
                FROM lease_agreement la JOIN tenant t ON t.lease_agreement_id=la.lease_agreement_id
                JOIN rented_object ro ON ro.rented_object_id=t.rented_object_id JOIN apartment a ON a.apartment_id=ro.apartment_id
                WHERE t.active=true AND la.is_contract_active=true
                """ + scope(apartment), params(owner, apartment), (r,n) -> new Lease(r.getLong("tenant_id"), r.getLong("rented_object_id"),
                r.getLong("apartment_id"), r.getString("apartment_name"), r.getString("rented_object_name"), r.getString("tenant_name"),
                r.getDate("start_contract_date").toLocalDate(), r.getDate("end_contract_date").toLocalDate(),
                r.getDate("actual_termination_date") == null ? null : r.getDate("actual_termination_date").toLocalDate(),
                r.getBigDecimal("fixed"), r.getBigDecimal("compensation_amount")));
    }

    List<SettlementMonth> settlementMonths(long owner, Long apartment, LocalDate from, LocalDate until) {
        return jdbc.query("""
                SELECT s.tenant_id, s.rented_object_id, a.apartment_id, CAST(date_trunc('month',s.settlement_date) AS date) AS month,
                       SUM(COALESCE(s.total_amount,0)-s.deposit_top_up) AS amount,
                       SUM(COALESCE(s.electricity_amount,0)+COALESCE(s.water_amount,0)) AS utilities,
                       SUM(COALESCE(s.compensation_amount,0)) AS compensation
                FROM settlement s JOIN rented_object ro ON ro.rented_object_id=s.rented_object_id JOIN apartment a ON a.apartment_id=ro.apartment_id
                WHERE s.settlement_date>=:from AND s.settlement_date<:until
                """ + scope(apartment) + " GROUP BY s.tenant_id,s.rented_object_id,a.apartment_id,date_trunc('month',s.settlement_date)",
                params(owner, apartment).addValue("from", Date.valueOf(from)).addValue("until", Date.valueOf(until)),
                (r,n) -> new SettlementMonth(r.getLong("tenant_id"),r.getLong("rented_object_id"),r.getLong("apartment_id"),r.getDate("month").toLocalDate(),
                        r.getBigDecimal("amount"),r.getBigDecimal("utilities"),r.getBigDecimal("compensation")));
    }

    List<InvoiceMonth> invoiceMonths(long owner, Long apartment, LocalDate from, LocalDate until) {
        return jdbc.query("""
                SELECT a.apartment_id, COALESCE(i.housing_provider_id,0) AS provider_id,
                       CAST(date_trunc('month',i.pay_day_month) AS date) AS month, COALESCE(SUM(i.amount),0) AS amount
                FROM invoice i JOIN apartment a ON a.apartment_id=i.apartment_id
                WHERE i.pay_day_month>=:from AND i.pay_day_month<:until
                """ + scope(apartment) + " GROUP BY a.apartment_id,i.housing_provider_id,date_trunc('month',i.pay_day_month)",
                params(owner, apartment).addValue("from", Date.valueOf(from)).addValue("until", Date.valueOf(until)),
                (r,n) -> new InvoiceMonth(r.getLong("apartment_id"),r.getLong("provider_id"),r.getDate("month").toLocalDate(),r.getBigDecimal("amount")));
    }

    List<Provider> providers(long owner, Long apartment) {
        return jdbc.query("""
                SELECT a.apartment_id, h.housing_provider_id, h.name, h.recurring_payment,
                       COALESCE((SELECT SUM(p.price) FROM provider_field p WHERE p.housing_provider_id=h.housing_provider_id),0) AS amount
                FROM apartment_housing_provider ah JOIN apartment a ON a.apartment_id=ah.apartment_id
                JOIN housing_provider h ON h.housing_provider_id=ah.housing_provider_id
                WHERE h.user_account_id=:owner
                """ + scope(apartment), params(owner, apartment), (r,n) -> new Provider(r.getLong("apartment_id"),r.getLong("housing_provider_id"),
                r.getString("name"),r.getBoolean("recurring_payment"),r.getBigDecimal("amount")));
    }

    private String scope(Long apartment) { return " AND a.user_account_id=:owner" + (apartment == null ? "" : " AND a.apartment_id=:apartment"); }
    private MapSqlParameterSource params(long owner, Long apartment) { return new MapSqlParameterSource("owner",owner).addValue("apartment",apartment); }
    record Entry(long apartmentId, LocalDate month, String category, BigDecimal amount, BigDecimal compensation) {}
    record Lease(long tenantId, long objectId, long apartmentId, String apartmentName, String objectName, String tenantName,
                 LocalDate start, LocalDate end, LocalDate termination, BigDecimal fixed, BigDecimal compensation) {}
    record SettlementMonth(long tenantId, long objectId, long apartmentId, LocalDate month, BigDecimal amount, BigDecimal utilities, BigDecimal compensation) {}
    record InvoiceMonth(long apartmentId, long providerId, LocalDate month, BigDecimal amount) {}
    record Provider(long apartmentId, long id, String name, boolean recurring, BigDecimal amount) {}
}
