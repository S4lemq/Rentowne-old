package pl.rentowne.security.encryption;

import org.springframework.lang.NonNull;

/**
 * Serwis do szyfrowania i deszyfrowania danych wrażliwych.
 * Używa algorytmu AES-256-GCM (Galois/Counter Mode) który zapewnia:
 * - Szyfrowanie (poufność danych)
 * - Uwierzytelnianie (integralność danych i autentyczność)
 */
public interface EncryptionService {

    /**
     * Szyfruje podany tekst jawny.
     *
     * @param plainText tekst do zaszyfrowania
     * @return zaszyfrowany tekst zakodowany w Base64 (zawiera IV + ciphertext + auth tag)
     * @throws EncryptionException gdy szyfrowanie się nie powiedzie
     */
    @NonNull
    String encrypt(@NonNull String plainText);

    /**
     * Deszyfruje podany zaszyfrowany tekst.
     *
     * @param encryptedText zaszyfrowany tekst zakodowany w Base64
     * @return odszyfrowany tekst jawny
     * @throws EncryptionException gdy deszyfrowanie się nie powiedzie
     */
    @NonNull
    String decrypt(@NonNull String encryptedText);
}
