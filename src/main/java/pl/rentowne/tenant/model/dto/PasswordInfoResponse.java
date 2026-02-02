package pl.rentowne.tenant.model.dto;

import org.springframework.lang.NonNull;

/**
 * DTO odpowiedzi z informacją o haśle PDF najemcy.
 *
 * @param hasCustomPassword    czy najemca ma ustawione własne hasło
 * @param usingDefaultPassword czy używane jest hasło domyślne (2x ostatnie 6 cyfr CIF)
 */
public record PasswordInfoResponse(
        boolean hasCustomPassword,
        boolean usingDefaultPassword
) {
    @NonNull
    public static PasswordInfoResponse custom() {
        return new PasswordInfoResponse(true, false);
    }

    @NonNull
    public static PasswordInfoResponse defaultPassword() {
        return new PasswordInfoResponse(false, true);
    }
}
