package pl.rentowne.tenant_settlement;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repozytorium typów płatności
 */
interface PaymentRepository extends JpaRepository<Payment, Long> {
}
