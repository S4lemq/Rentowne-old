package pl.rentowne.tenant.model.dto;

import org.springframework.lang.NonNull;

/**
 * DTO odpowiedzi z odszyfrowanym hasłem PDF najemcy.
 * Używane przez administratora do odzyskiwania hasła dla klienta.
 *
 * @param password         odszyfrowane hasło (własne lub domyślne)
 * @param isCustomPassword czy to hasło własne czy domyślne
 */
public record DecryptedPasswordResponse(
        @NonNull String password,
        boolean isCustomPassword
) {
}
