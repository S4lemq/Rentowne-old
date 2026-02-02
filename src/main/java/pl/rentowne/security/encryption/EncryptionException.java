package pl.rentowne.security.encryption;

/**
 * Wyjątek rzucany w przypadku błędu szyfrowania lub deszyfrowania.
 * Szczegóły błędu są logowane, a użytkownikowi zwracany jest ogólny komunikat.
 */
public class EncryptionException extends RuntimeException {

    public EncryptionException(final String message) {
        super(message);
    }
}
