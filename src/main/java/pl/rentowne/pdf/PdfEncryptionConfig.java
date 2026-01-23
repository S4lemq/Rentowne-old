package pl.rentowne.pdf;

import org.springframework.lang.NonNull;

/**
 * Konfiguracja szyfrowania dokumentu PDF dla aplikacji rządowych.
 * <p>
 * Dokument może być otwarty dwoma różnymi hasłami:
 * <ul>
 *   <li>{@code userPassword} - otwiera dokument z ograniczonymi uprawnieniami (tylko drukowanie)</li>
 *   <li>{@code ownerPassword} - otwiera dokument z pełnymi uprawnieniami (może zmieniać zabezpieczenia)</li>
 * </ul>
 * Szyfrowanie AES-256 z restrykcyjnymi uprawnieniami.
 * </p>
 *
 * <h3>Użycie:</h3>
 * <pre>{@code
 * builder.setEncryption(PdfEncryptionConfig.forGovernment("haslo1", "haslo2"))
 * }</pre>
 *
 * @param userPassword  hasło do otwarcia dokumentu (z ograniczonymi uprawnieniami)
 * @param ownerPassword hasło do otwarcia dokumentu (z pełnymi uprawnieniami)
 * @param keyLength     długość klucza szyfrowania (256 bitów AES)
 * @param permissions   uprawnienia dla użytkownika otwierającego z userPassword
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
     * Tworzy konfigurację szyfrowania dla dokumentów rządowych z maksymalnym bezpieczeństwem.
     * Oba hasła pozwalają otworzyć dokument. Uprawnienia są bardzo restrykcyjne.
     *
     * @param userPassword  pierwsze hasło do otwarcia dokumentu
     * @param ownerPassword drugie hasło do otwarcia dokumentu (również pozwala na zmianę uprawnień)
     * @return konfiguracja szyfrowania
     */
    @NonNull
    public static PdfEncryptionConfig forGovernment(@NonNull final String userPassword,
                                                    @NonNull final String ownerPassword) {
        return new PdfEncryptionConfig(userPassword, ownerPassword, DEFAULT_KEY_LENGTH, PdfPermissions.GOVERNMENT_SECURE);
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
         * Uprawnienia dla dokumentów rządowych - maksymalne bezpieczeństwo.
         * Dozwolone tylko: drukowanie.
         * Zabronione: modyfikacja, kopiowanie tekstu, adnotacje, wypełnianie formularzy, składanie dokumentu.
         */
        public static final PdfPermissions GOVERNMENT_SECURE = new PdfPermissions(
                true,   // drukowanie - dozwolone (użytkownik może wydrukować do celów urzędowych)
                false,  // modyfikacja - zabroniona
                false,  // kopiowanie tekstu - zabronione
                false,  // adnotacje - zabronione
                false,  // wypełnianie formularzy - zabronione
                true,   // dostępność (czytniki ekranu) - dozwolone (wymóg WCAG)
                false   // składanie dokumentu - zabronione
        );
    }
}
