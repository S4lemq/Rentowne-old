package pl.rentowne.tenant_settlement.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.rentowne.tenant_settlement.model.Payment;

/**
 * Repozytorium typów płatności
 */
public interface PaymentRepository extends JpaRepository<Payment, Long> {
}
