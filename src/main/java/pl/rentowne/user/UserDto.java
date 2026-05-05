package pl.rentowne.user;

import pl.rentowne.address.AddressDto;
import pl.rentowne.payment_card.PaymentCardDto;

public record UserDto(
        Long id,
        String firstname,
        String lastname,
        String email,
        String oldPassword,
        String password,
        String repeatPassword,
        String image,
        String phoneNumber,
        PreferredLanguage preferredLanguage,
        PaymentCardDto paymentCardDto,
        AddressDto addressDto
) {
}
