package pl.rentowne.payment_card;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repozytorium karty płatniczej
 */
interface PaymentCardRepository extends JpaRepository<PaymentCard, Long> {
}
