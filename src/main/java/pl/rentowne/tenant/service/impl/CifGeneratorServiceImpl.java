package pl.rentowne.tenant.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import pl.rentowne.tenant.repository.TenantRepository;
import pl.rentowne.tenant.service.CifGeneratorService;

import java.security.SecureRandom;

/**
 * Implementacja generatora numerów CIF.
 * Generuje unikalny 10-cyfrowy numer z wykorzystaniem SecureRandom.
 */
@Slf4j
@Service
@RequiredArgsConstructor
class CifGeneratorServiceImpl implements CifGeneratorService {

    private static final int CIF_LENGTH = 10;
    private static final int MAX_ATTEMPTS = 100;

    private final TenantRepository tenantRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    @NonNull
    public String generateCif() {
        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            final String cif = generateRandomCif();
            if (tenantRepository.findByCif(cif).isEmpty()) {
                return cif;
            }
        }
        log.error("Nie udało się wygenerować unikalnego numeru CIF po {} próbach", MAX_ATTEMPTS);
        throw new IllegalStateException("Nie udało się wygenerować unikalnego numeru CIF");
    }

    @NonNull
    private String generateRandomCif() {
        final StringBuilder sb = new StringBuilder(CIF_LENGTH);
        sb.append(1 + secureRandom.nextInt(9));
        for (int i = 1; i < CIF_LENGTH; i++) {
            sb.append(secureRandom.nextInt(10));
        }
        return sb.toString();
    }
}
