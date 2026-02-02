package pl.rentowne.tenant.model.dto;

import org.springframework.lang.NonNull;

/**
 * DTO żądania ustawienia hasła PDF dla najemcy.
 * Hasła są przesyłane zakodowane w Base64.
 *
 * @param password        hasło zakodowane w Base64 (min 12 znaków, max 512 znaków po dekodowaniu)
 * @param confirmPassword potwierdzenie hasła zakodowane w Base64 (musi być identyczne z password)
 */
public record SetPasswordRequest(
        @NonNull String password,
        @NonNull String confirmPassword
) {
}
