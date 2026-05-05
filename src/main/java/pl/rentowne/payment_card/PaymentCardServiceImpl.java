package pl.rentowne.payment_card;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pl.rentowne.user.User;

@Service
@RequiredArgsConstructor
class PaymentCardServiceImpl implements PaymentCardService {

    private final PaymentCardRepository paymentCardRepository;

    @Override
    public void save(PaymentCardDto paymentCardDto, User user) {
        PaymentCard paymentCard = new PaymentCard(
                paymentCardDto.id(),
                paymentCardDto.number(),
                paymentCardDto.name(),
                paymentCardDto.month(),
                paymentCardDto.year(),
                paymentCardDto.cvv(),
                user
        );
        paymentCardRepository.save(paymentCard);
    }
}
