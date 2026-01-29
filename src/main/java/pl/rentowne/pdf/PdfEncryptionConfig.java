package pl.rentowne.pdf;

import org.springframework.lang.NonNull;

/**
 * Konfiguracja szyfrowania dokumentu PDF.
 * <p>
 * Dokument może być otwarty dwoma różnymi hasłami:
 * <ul>
 *   <li>{@code userPassword} (hasło klienta) - otwiera dokument z ograniczonymi uprawnieniami
 *       (wyświetlanie, drukowanie)</li>
 *   <li>{@code ownerPassword} (hasło admina) - otwiera dokument z pełnymi uprawnieniami
 *       (edycja, zmiana zabezpieczeń, kopiowanie)</li>
 * </ul>
 * Szyfrowanie AES-256.
 * </p>
 *
 * <h3>Użycie:</h3>
 * <pre>{@code
 * builder.setEncryption(PdfEncryptionConfig.forClientAndAdmin("hasloKlienta", "hasloAdmina"))
 * }</pre>
 *
 * @param userPassword  hasło klienta - otwiera dokument z ograniczonymi uprawnieniami
 * @param ownerPassword hasło admina - otwiera dokument z pełnymi uprawnieniami
 * @param keyLength     długość klucza szyfrowania (256 bitów AES)
 * @param permissions   uprawnienia dla użytkownika otwierającego hasłem klienta
 */
public record PdfEncryptionConfig(
        @NonNull String userPassword,
        @NonNull String ownerPassword,
        int keyLength,
        @NonNull PdfPermissions permissions
) {

    /**
     * Domyślna długość klucza - 256 bitów (AES).
     */
    public static final int DEFAULT_KEY_LENGTH = 256;

    /**
     * Tworzy konfigurację szyfrowania z dwoma poziomami dostępu.
     * <p>
     * Klient otwierający dokument swoim hasłem może:
     * <ul>
     *   <li>wyświetlać dokument</li>
     *   <li>drukować dokument</li>
     * </ul>
     * Admin otwierający dokument swoim hasłem może wykonywać wszystkie operacje.
     * </p>
     *
     * @param clientPassword hasło dla klienta (ograniczone uprawnienia)
     * @param adminPassword  hasło dla admina (pełne uprawnienia)
     * @return konfiguracja szyfrowania
     */
    @NonNull
    public static PdfEncryptionConfig forClientAndAdmin(@NonNull final String clientPassword,
                                                        @NonNull final String adminPassword) {
        return new PdfEncryptionConfig(clientPassword, adminPassword, DEFAULT_KEY_LENGTH, PdfPermissions.CLIENT_ACCESS);
    }

    /**
     * Uprawnienia dokumentu PDF.
     */
    public record PdfPermissions(
            boolean canPrint,
            boolean canModify,
            boolean canExtractContent,
            boolean canModifyAnnotations,
            boolean canFillInForm,
            boolean canExtractForAccessibility,
            boolean canAssembleDocument
    ) {
        /**
         * Uprawnienia dla klienta - wyświetlanie i drukowanie.
         * <ul>
         *   <li>Dozwolone: drukowanie, dostępność (WCAG)</li>
         *   <li>Zabronione: modyfikacja, kopiowanie tekstu, adnotacje, wypełnianie formularzy, składanie dokumentu</li>
         * </ul>
         */
        public static final PdfPermissions CLIENT_ACCESS = new PdfPermissions(
                true,   // drukowanie - dozwolone
                false,  // modyfikacja - zabroniona
                false,  // kopiowanie tekstu - zabronione
                false,  // adnotacje - zabronione
                false,  // wypełnianie formularzy - zabronione
                true,   // dostępność (czytniki ekranu) - dozwolone (wymóg WCAG)
                false   // składanie dokumentu - zabronione
        );
    }
}
