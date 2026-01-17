package pl.rentowne.payment_card.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.rentowne.payment_card.model.PaymentCard;

/**
 * Repozytorium karty płatniczej
 */
public interface PaymentCardRepository extends JpaRepository<PaymentCard, Long> {
}
