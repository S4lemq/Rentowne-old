package pl.rentowne.tenant.service;

import org.springframework.lang.NonNull;

/**
 * Serwis do zarządzania hasłami PDF dla najemców.
 * <p>
 * Hasło klienta do dokumentów PDF może być:
 * <ul>
 *   <li>Domyślne - 2x ostatnie 6 cyfr numeru CIF (np. CIF=1234567890 → hasło=567890567890)</li>
 *   <li>Własne - zdefiniowane przez klienta (min 12 znaków, max 512 znaków)</li>
 * </ul>
 * Hasło własne jest przechowywane w formie zaszyfrowanej (AES-256-GCM).
 * </p>
 * <p>
 * Hasła przesyłane przez API są kodowane/dekodowane w Base64.
 * </p>
 */
public interface TenantPasswordService {

    /**
     * Pobiera hasło PDF dla najemcy.
     * Jeśli najemca ma ustawione własne hasło, zwraca je (odszyfrowane).
     * W przeciwnym razie zwraca hasło domyślne (2x ostatnie 6 cyfr CIF).
     *
     * @param tenantId identyfikator najemcy
     * @return hasło do dokumentów PDF
     */
    @NonNull
    String getPdfPassword(@NonNull Long tenantId);

    /**
     * Pobiera hasło PDF dla najemcy zakodowane w Base64.
     *
     * @param tenantId identyfikator najemcy
     * @return hasło do dokumentów PDF zakodowane w Base64
     */
    @NonNull
    String getPdfPasswordBase64Encoded(@NonNull Long tenantId);

    /**
     * Pobiera hasło PDF dla najemcy na podstawie numeru CIF.
     *
     * @param cif numer CIF najemcy
     * @return hasło do dokumentów PDF
     */
    @NonNull
    String getPdfPasswordByCif(@NonNull String cif);

    /**
     * Ustawia własne hasło PDF dla najemcy.
     * Hasła muszą być zakodowane w Base64.
     * Hasło jest walidowane i szyfrowane przed zapisem do bazy danych.
     *
     * @param tenantId           identyfikator najemcy
     * @param passwordBase64     hasło zakodowane w Base64
     * @param confirmPasswordBase64 potwierdzenie hasła zakodowane w Base64
     */
    void setCustomPassword(@NonNull Long tenantId, @NonNull String passwordBase64, @NonNull String confirmPasswordBase64);

    /**
     * Usuwa własne hasło PDF dla najemcy.
     * Po usunięciu najemca będzie używał hasła domyślnego.
     *
     * @param tenantId identyfikator najemcy
     */
    void removeCustomPassword(@NonNull Long tenantId);

    /**
     * Sprawdza czy najemca ma ustawione własne hasło PDF.
     *
     * @param tenantId identyfikator najemcy
     * @return true jeśli ma własne hasło, false jeśli używa domyślnego
     */
    boolean hasCustomPassword(@NonNull Long tenantId);

    /**
     * Generuje domyślne hasło na podstawie numeru CIF.
     * Hasło domyślne to 2x ostatnie 6 cyfr numeru CIF.
     *
     * @param cif numer CIF najemcy (10 znaków)
     * @return domyślne hasło (12 znaków)
     */
    @NonNull
    String generateDefaultPassword(@NonNull String cif);

    /**
     * Pobiera hasło administratora do dokumentów PDF.
     *
     * @return hasło administratora
     */
    @NonNull
    String getAdminPassword();
}
