package pl.rentowne.owner;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.*;
import lombok.Builder;

import java.math.BigInteger;
import java.util.Locale;

@Builder
public record OwnerDto(
        @Positive Long id,
        @PositiveOrZero Long version,
        @NotBlank(message = "Podaj imię właściciela.") @Size(max = 50) String firstName,
        @NotBlank(message = "Podaj nazwisko właściciela.") @Size(max = 50) String lastName,
        @NotBlank(message = "Podaj email właściciela.") @Email(message = "Podaj poprawny email właściciela.") @Size(max = 254) String email,
        @Size(max = 100) String streetName,
        @NotBlank(message = "Podaj numer budynku właściciela.") @Size(max = 10) String buildingNumber,
        @Size(max = 10) String apartmentNumber,
        @NotBlank(message = "Podaj kod pocztowy właściciela.") @Pattern(regexp = "\\d{2}-\\d{3}", message = "Kod pocztowy właściciela musi mieć format 00-000.") String zipCode,
        @NotBlank(message = "Podaj miejscowość właściciela.") @Size(max = 60) String cityName,
        @Size(max = 120) String representativeName,
        @Email(message = "Podaj poprawny email pełnomocnika.") @Size(max = 254) String representativeEmail,
        @NotBlank(message = "Podaj numer konta do wpłat.") @Pattern(regexp = "\\d{26}", message = "Podaj polski numer konta: 26 cyfr lub PL i 26 cyfr.") String accountNumber,
        @NotNull(message = "Wybierz właściciela konta do wpłat.") PaymentAccountHolder paymentAccountHolder
) {
    public OwnerDto {
        firstName = clean(firstName);
        lastName = clean(lastName);
        email = clean(email);
        streetName = clean(streetName);
        buildingNumber = clean(buildingNumber);
        apartmentNumber = clean(apartmentNumber);
        zipCode = clean(zipCode);
        cityName = clean(cityName);
        representativeName = clean(representativeName);
        representativeEmail = clean(representativeEmail);
        accountNumber = normalizeAccountNumber(accountNumber);
    }

    @JsonIgnore
    @AssertTrue(message = "Podaj imię i nazwisko oraz email pełnomocnika albo usuń oba pola.")
    public boolean isRepresentativeComplete() {
        return (representativeName == null) == (representativeEmail == null);
    }

    @JsonIgnore
    @AssertTrue(message = "Konto pełnomocnika wymaga podania jego imienia i nazwiska oraz emaila.")
    public boolean isAccountHolderValid() {
        return paymentAccountHolder != PaymentAccountHolder.REPRESENTATIVE
                || representativeName != null && representativeEmail != null;
    }

    @JsonIgnore
    @AssertTrue(message = "Numer konta ma nieprawidłową sumę kontrolną.")
    public boolean isAccountNumberValid() {
        if (accountNumber == null || !accountNumber.matches("\\d{26}")) return true;
        String ibanDigits = accountNumber.substring(2) + "2521" + accountNumber.substring(0, 2);
        return new BigInteger(ibanDigits).mod(BigInteger.valueOf(97)).equals(BigInteger.ONE);
    }

    private static String clean(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }

    private static String normalizeAccountNumber(String value) {
        if (value == null) return null;
        String normalized = value.replaceAll("[\\s\\p{Z}]", "").toUpperCase(Locale.ROOT);
        return normalized.startsWith("PL") ? normalized.substring(2) : normalized;
    }
}
