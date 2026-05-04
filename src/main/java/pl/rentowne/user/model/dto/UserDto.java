package pl.rentowne.user.model.dto;

import lombok.Builder;
import lombok.Getter;
import pl.rentowne.address.AddressDto;
import pl.rentowne.payment_card.PaymentCardDto;
import pl.rentowne.user.model.PreferredLanguage;

@Getter
@Builder
public class UserDto {
    private Long id;
    private String firstname;
    private String lastname;
    private String email;
    private String oldPassword;
    private String password;
    private String repeatPassword;
    private String image;
    private String phoneNumber;
    private PreferredLanguage preferredLanguage;
    private PaymentCardDto paymentCardDto;
    private AddressDto addressDto;
}
