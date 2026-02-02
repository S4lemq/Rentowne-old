package pl.rentowne.tenant.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.rentowne.exception.RentowneBusinessException;
import pl.rentowne.exception.RentowneErrorCode;
import pl.rentowne.security.encryption.EncryptionService;
import pl.rentowne.tenant.model.Tenant;
import pl.rentowne.tenant.repository.TenantRepository;
import pl.rentowne.tenant.service.TenantPasswordService;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Implementacja serwisu do zarządzania hasłami PDF dla najemców.
 */
@Slf4j
@Service
@RequiredArgsConstructor
class TenantPasswordServiceImpl implements TenantPasswordService {

    private static final int CIF_SUFFIX_LENGTH = 6;
    private static final int DEFAULT_PASSWORD_REPEAT = 2;
    private static final int MIN_PASSWORD_LENGTH = 12;
    private static final int MAX_PASSWORD_LENGTH = 512;

    private final TenantRepository tenantRepository;
    private final EncryptionService encryptionService;

    @Value("${application.pdf.admin-password}")
    private String adminPassword;

    @Override
    @NonNull
    public String getPdfPassword(@NonNull final Long tenantId) {
        final Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new RentowneBusinessException(RentowneErrorCode.ITEM_NOT_FOUND));

        return resolvePassword(tenant);
    }

    @Override
    @NonNull
    public String getPdfPasswordBase64Encoded(@NonNull final Long tenantId) {
        final Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new RentowneBusinessException(RentowneErrorCode.ITEM_NOT_FOUND));

        return resolvePasswordBase64Encoded(tenant);
    }

    @Override
    @NonNull
    public String getPdfPasswordByCif(@NonNull final String cif) {
        final Tenant tenant = tenantRepository.findByCif(cif)
                .orElseThrow(() -> new RentowneBusinessException(RentowneErrorCode.ITEM_NOT_FOUND));

        return resolvePassword(tenant);
    }

    @Override
    @Transactional
    public void setCustomPassword(@NonNull final Long tenantId,
                                  @NonNull final String passwordBase64,
                                  @NonNull final String confirmPasswordBase64) {
        final String password = decodeBase64(passwordBase64);
        final String confirmPassword = decodeBase64(confirmPasswordBase64);

        validatePasswords(password, confirmPassword);

        final Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new RentowneBusinessException(RentowneErrorCode.ITEM_NOT_FOUND));

        final String encryptedPassword = encryptionService.encrypt(password);
        tenant.setAddtInfoValue(encryptedPassword);
        tenantRepository.save(tenant);
    }

    @Override
    @Transactional
    public void removeCustomPassword(@NonNull final Long tenantId) {
        final Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new RentowneBusinessException(RentowneErrorCode.ITEM_NOT_FOUND));

        tenant.setAddtInfoValue(null);
        tenantRepository.save(tenant);
    }

    @Override
    public boolean hasCustomPassword(@NonNull final Long tenantId) {
        final Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new RentowneBusinessException(RentowneErrorCode.ITEM_NOT_FOUND));

        return hasEncryptedPassword(tenant.getAddtInfoValue());
    }

    @Override
    @NonNull
    public String generateDefaultPassword(@NonNull final String cif) {
        if (cif.length() < CIF_SUFFIX_LENGTH) {
            throw new IllegalArgumentException("Numer CIF musi mieć co najmniej " + CIF_SUFFIX_LENGTH + " znaków");
        }

        final String suffix = cif.substring(cif.length() - CIF_SUFFIX_LENGTH);
        return suffix.repeat(DEFAULT_PASSWORD_REPEAT);
    }

    @Override
    @NonNull
    public String getAdminPassword() {
        return adminPassword;
    }

    /**
     * Waliduje hasła - sprawdza czy są identyczne i czy spełniają wymagania długości.
     *
     * @param password        hasło
     * @param confirmPassword potwierdzenie hasła
     */
    private void validatePasswords(@NonNull final String password, @NonNull final String confirmPassword) {
        if (!password.equals(confirmPassword)) {
            log.warn("Próba ustawienia hasła - hasła nie są identyczne");
            throw new RentowneBusinessException(RentowneErrorCode.PASSWORDS_DO_NOT_MATCH);
        }

        final int passwordLength = password.length();
        if (passwordLength < MIN_PASSWORD_LENGTH || passwordLength > MAX_PASSWORD_LENGTH) {
            log.warn("Próba ustawienia hasła o nieprawidłowej długości: {} znaków (wymagane: {}-{})",
                    passwordLength, MIN_PASSWORD_LENGTH, MAX_PASSWORD_LENGTH);
            throw new RentowneBusinessException(RentowneErrorCode.INVALID_PASSWORD_LENGTH);
        }
    }

    /**
     * Dekoduje tekst z Base64.
     *
     * @param base64Text tekst zakodowany w Base64
     * @return zdekodowany tekst
     */
    @NonNull
    private String decodeBase64(@NonNull final String base64Text) {
        try {
            final byte[] decodedBytes = Base64.getDecoder().decode(base64Text);
            return new String(decodedBytes, StandardCharsets.UTF_8);
        } catch (final IllegalArgumentException e) {
            log.error("Błąd dekodowania Base64 - nieprawidłowy format: {}", e.getMessage());
            throw new RentowneBusinessException(RentowneErrorCode.INVALID_BASE64_FORMAT);
        }
    }

    /**
     * Koduje tekst do Base64.
     *
     * @param text tekst do zakodowania
     * @return tekst zakodowany w Base64
     */
    @NonNull
    private String encodeBase64(@NonNull final String text) {
        final byte[] textBytes = text.getBytes(StandardCharsets.UTF_8);
        return Base64.getEncoder().encodeToString(textBytes);
    }

    @NonNull
    private String resolvePassword(@NonNull final Tenant tenant) {
        final String encryptedPassword = tenant.getAddtInfoValue();

        if (hasEncryptedPassword(encryptedPassword)) {
            return encryptionService.decrypt(encryptedPassword);
        }

        return generateDefaultPassword(tenant.getCif());
    }

    @NonNull
    private String resolvePasswordBase64Encoded(@NonNull final Tenant tenant) {
        final String password = resolvePassword(tenant);
        return encodeBase64(password);
    }

    private boolean hasEncryptedPassword(@Nullable final String encryptedPassword) {
        return encryptedPassword != null && !encryptedPassword.isEmpty();
    }
}
