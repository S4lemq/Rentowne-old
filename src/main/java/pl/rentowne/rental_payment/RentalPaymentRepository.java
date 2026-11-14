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
                   COALESCE(p.payments, 0) AS totalPayments
            FROM tenant t
            LEFT JOIN (
                SELECT s.tenant_id, s.rented_object_id, SUM(COALESCE(s.total_amount, 0)) AS charges,
                       SUM(COALESCE(c.amount, 0)) AS credit
                FROM settlement s LEFT JOIN settlement_opening_credit c ON c.settlement_id = s.settlement_id
                WHERE s.tenant_id IN (:tenantIds)
                GROUP BY s.tenant_id, s.rented_object_id
            ) s ON s.tenant_id = t.tenant_id AND s.rented_object_id = t.rented_object_id
            LEFT JOIN (
                SELECT tenant_id, rented_object_id, SUM(amount) AS payments
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
    }
}
