package pl.rentowne.pdf;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PdfEncryptionConfigTest {

    @Test
    void forClientAndAdmin_shouldSetPasswordsCorrectly() {
        // given
        String clientPassword = "klient123";
        String adminPassword = "admin456";

        // when
        PdfEncryptionConfig config = PdfEncryptionConfig.forClientAndAdmin(clientPassword, adminPassword);

        // then
        assertEquals(clientPassword, config.userPassword());
        assertEquals(adminPassword, config.ownerPassword());
    }

    @Test
    void forClientAndAdmin_shouldUseDefaultKeyLength() {
        // when
        PdfEncryptionConfig config = PdfEncryptionConfig.forClientAndAdmin("client", "admin");

        // then
        assertEquals(256, config.keyLength());
    }

    @Test
    void forClientAndAdmin_shouldUseClientAccessPermissions() {
        // when
        PdfEncryptionConfig config = PdfEncryptionConfig.forClientAndAdmin("client", "admin");

        // then
        assertEquals(PdfEncryptionConfig.PdfPermissions.CLIENT_ACCESS, config.permissions());
    }

    @Test
    void clientAccessPermissions_shouldAllowPrinting() {
        // when
        PdfEncryptionConfig.PdfPermissions permissions = PdfEncryptionConfig.PdfPermissions.CLIENT_ACCESS;

        // then
        assertTrue(permissions.canPrint());
    }

    @Test
    void clientAccessPermissions_shouldAllowAccessibility() {
        // when
        PdfEncryptionConfig.PdfPermissions permissions = PdfEncryptionConfig.PdfPermissions.CLIENT_ACCESS;

        // then
        assertTrue(permissions.canExtractForAccessibility());
    }

    @Test
    void clientAccessPermissions_shouldDenyModification() {
        // when
        PdfEncryptionConfig.PdfPermissions permissions = PdfEncryptionConfig.PdfPermissions.CLIENT_ACCESS;

        // then
        assertFalse(permissions.canModify());
    }

    @Test
    void clientAccessPermissions_shouldDenyContentExtraction() {
        // when
        PdfEncryptionConfig.PdfPermissions permissions = PdfEncryptionConfig.PdfPermissions.CLIENT_ACCESS;

        // then
        assertFalse(permissions.canExtractContent());
    }

    @Test
    void clientAccessPermissions_shouldDenyAnnotations() {
        // when
        PdfEncryptionConfig.PdfPermissions permissions = PdfEncryptionConfig.PdfPermissions.CLIENT_ACCESS;

        // then
        assertFalse(permissions.canModifyAnnotations());
    }

    @Test
    void clientAccessPermissions_shouldDenyFormFilling() {
        // when
        PdfEncryptionConfig.PdfPermissions permissions = PdfEncryptionConfig.PdfPermissions.CLIENT_ACCESS;

        // then
        assertFalse(permissions.canFillInForm());
    }

    @Test
    void clientAccessPermissions_shouldDenyDocumentAssembly() {
        // when
        PdfEncryptionConfig.PdfPermissions permissions = PdfEncryptionConfig.PdfPermissions.CLIENT_ACCESS;

        // then
        assertFalse(permissions.canAssembleDocument());
    }
}
