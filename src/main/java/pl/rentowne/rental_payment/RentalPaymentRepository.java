package pl.rentowne.rental_payment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface RentalPaymentRepository extends JpaRepository<RentalPayment, Long> {
    Optional<RentalPayment> findByTenantIdAndRequestId(Long tenantId, UUID requestId);
    Optional<RentalPayment> findByIdAndTenantIdAndRentedObjectIdAndDeletedFalse(Long id, Long tenantId, Long rentedObjectId);
    List<RentalPayment> findByTenantIdAndRentedObjectIdAndDeletedFalseOrderByPaymentDateDescIdDesc(Long tenantId, Long rentedObjectId);

    // Aggregate independently to avoid multiplying charges by the number of receipts.
    @Query(value = """
            SELECT t.tenant_id AS tenantId,
                   COALESCE(s.charges, 0) AS totalCharges,
                   COALESCE(s.credit, 0) AS historicalCredit,
                   COALESCE(p.payments, 0) AS totalPayments,
                   COALESCE(p.deposits, 0) AS depositPayments,
                   COALESCE(s.deposit_charges, 0) AS depositCharges,
                   COALESCE(s.deposit_carry, 0) AS depositCarryIn,
                   COALESCE(s.assessed, false) AS depositAssessed,
                   la.deposit AS depositRequired,
                   la.deposit_paid AS depositPaid,
                   CASE WHEN la.deposit_balance_confirmed AND la.is_contract_active AND la.deposit_paid IS NOT NULL
                        THEN GREATEST(COALESCE(la.deposit, 0) - la.deposit_paid, 0)
                        ELSE NULL END AS depositReminder
            FROM tenant t
            LEFT JOIN lease_agreement la ON la.lease_agreement_id = t.lease_agreement_id
            LEFT JOIN (
                SELECT s.tenant_id, s.rented_object_id, SUM(COALESCE(s.total_amount, 0)) AS charges,
                       SUM(COALESCE(c.amount, 0)) AS credit,
                       SUM(s.deposit_top_up) AS deposit_charges,
                       SUM(s.deposit_carry_in) AS deposit_carry,
                       BOOL_OR(s.deposit_assessment) AS assessed
                FROM settlement s LEFT JOIN settlement_opening_credit c ON c.settlement_id = s.settlement_id
                WHERE s.tenant_id IN (:tenantIds)
                GROUP BY s.tenant_id, s.rented_object_id
            ) s ON s.tenant_id = t.tenant_id AND s.rented_object_id = t.rented_object_id
            LEFT JOIN (
                SELECT tenant_id, rented_object_id, SUM(amount) AS payments, SUM(deposit_amount) AS deposits
                FROM rental_payment WHERE deleted = false AND tenant_id IN (:tenantIds)
                GROUP BY tenant_id, rented_object_id
            ) p ON p.tenant_id = t.tenant_id AND p.rented_object_id = t.rented_object_id
            WHERE t.tenant_id IN (:tenantIds)
            """, nativeQuery = true)
    List<BalanceTotals> findBalances(@Param("tenantIds") Collection<Long> tenantIds);

    interface BalanceTotals {
        Long getTenantId();
        BigDecimal getTotalCharges();
        BigDecimal getHistoricalCredit();
        BigDecimal getTotalPayments();
        BigDecimal getDepositPayments();
        BigDecimal getDepositCharges();
        BigDecimal getDepositCarryIn();
        boolean getDepositAssessed();
        BigDecimal getDepositReminder();
        BigDecimal getDepositRequired();
        BigDecimal getDepositPaid();
    }

    @Query("select coalesce(sum(p.depositAmount), 0) from RentalPayment p where p.tenant.id = :tenantId and p.deleted = false")
    BigDecimal sumDepositPayments(@Param("tenantId") Long tenantId);
}
