package pl.rentowne.security.encryption;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Implementacja szyfrowania AES-256-GCM.
 * <p>
 * AES-GCM (Galois/Counter Mode) to tryb szyfrowania AEAD (Authenticated Encryption with Associated Data),
 * który zapewnia zarówno poufność jak i integralność danych.
 * </p>
 * <p>
 * Format zaszyfrowanych danych: [IV (12 bajtów)][Ciphertext + Auth Tag]
 * </p>
 */
@Slf4j
@Service
class AesGcmEncryptionService implements EncryptionService {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;
    private static final int REQUIRED_KEY_LENGTH = 32;

    private final SecretKey secretKey;
    private final SecureRandom secureRandom;

    AesGcmEncryptionService(@Value("${application.security.encryption.key}") @NonNull final String encryptionKey) {
        final byte[] keyBytes = decodeKey(encryptionKey);
        if (keyBytes.length != REQUIRED_KEY_LENGTH) {
            log.error("Nieprawidłowa długość klucza szyfrowania: {} bajtów, wymagane: {} bajtów",
                    keyBytes.length, REQUIRED_KEY_LENGTH);
            throw new EncryptionException("Nieprawidłowa konfiguracja szyfrowania");
        }
        this.secretKey = new SecretKeySpec(keyBytes, "AES");
        this.secureRandom = new SecureRandom();
    }

    @Override
    @NonNull
    public String encrypt(@NonNull final String plainText) {
        if (plainText.isEmpty()) {
            log.error("Próba zaszyfrowania pustego tekstu");
            throw new EncryptionException("Błąd szyfrowania danych");
        }

        try {
            final byte[] iv = new byte[GCM_IV_LENGTH];
            secureRandom.nextBytes(iv);

            final Cipher cipher = Cipher.getInstance(ALGORITHM);
            final GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmSpec);

            final byte[] plainTextBytes = plainText.getBytes(StandardCharsets.UTF_8);
            final byte[] cipherText = cipher.doFinal(plainTextBytes);

            final ByteBuffer byteBuffer = ByteBuffer.allocate(iv.length + cipherText.length);
            byteBuffer.put(iv);
            byteBuffer.put(cipherText);

            return Base64.getEncoder().encodeToString(byteBuffer.array());
        } catch (final Exception e) {
            log.error("Błąd podczas szyfrowania danych: {}", e.getMessage(), e);
            throw new EncryptionException("Błąd szyfrowania danych");
        }
    }

    @Override
    @NonNull
    public String decrypt(@NonNull final String encryptedText) {
        if (encryptedText.isEmpty()) {
            log.error("Próba odszyfrowania pustego tekstu");
            throw new EncryptionException("Błąd deszyfrowania danych");
        }

        try {
            final byte[] encryptedBytes = Base64.getDecoder().decode(encryptedText);

            if (encryptedBytes.length < GCM_IV_LENGTH) {
                log.error("Nieprawidłowy format zaszyfrowanych danych - za krótkie: {} bajtów", encryptedBytes.length);
                throw new EncryptionException("Błąd deszyfrowania danych");
            }

            final ByteBuffer byteBuffer = ByteBuffer.wrap(encryptedBytes);
            final byte[] iv = new byte[GCM_IV_LENGTH];
            byteBuffer.get(iv);

            final byte[] cipherText = new byte[byteBuffer.remaining()];
            byteBuffer.get(cipherText);

            final Cipher cipher = Cipher.getInstance(ALGORITHM);
            final GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec);

            final byte[] plainTextBytes = cipher.doFinal(cipherText);
            return new String(plainTextBytes, StandardCharsets.UTF_8);
        } catch (final EncryptionException e) {
            throw e;
        } catch (final Exception e) {
            log.error("Błąd podczas deszyfrowania danych: {}", e.getMessage(), e);
            throw new EncryptionException("Błąd deszyfrowania danych");
        }
    }

    private byte[] decodeKey(@NonNull final String key) {
        if (key.length() == 64 && key.matches("[0-9a-fA-F]+")) {
            return hexToBytes(key);
        }
        if (key.length() == 44 && key.matches("[A-Za-z0-9+/=]+")) {
            return Base64.getDecoder().decode(key);
        }
        return key.getBytes(StandardCharsets.UTF_8);
    }

    private byte[] hexToBytes(@NonNull final String hex) {
        final int len = hex.length();
        final byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                    + Character.digit(hex.charAt(i + 1), 16));
        }
        return data;
    }
}
