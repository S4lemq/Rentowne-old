package pl.rentowne.tenant.service;

import org.springframework.lang.NonNull;

/**
 * Serwis do generowania numerów CIF (Customer Identification Number).
 * CIF to 10-cyfrowy unikalny numer identyfikacyjny klienta.
 */
public interface CifGeneratorService {

    /**
     * Generuje unikalny 10-cyfrowy numer CIF.
     *
     * @return unikalny numer CIF (10 cyfr)
     */
    @NonNull
    String generateCif();
}
