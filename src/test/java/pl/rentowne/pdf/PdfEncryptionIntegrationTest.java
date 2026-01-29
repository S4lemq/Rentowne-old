package pl.rentowne.pdf;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PdfEncryptionIntegrationTest {

    private static final String CLIENT_PASSWORD = "klient123";
    private static final String ADMIN_PASSWORD = "admin456";

    private PdfDocumentFactory pdfDocumentFactory;

    @BeforeEach
    void setUp() {
        PdfFontProvider fontProvider = new PdfFontProvider();
        pdfDocumentFactory = new PdfDocumentFactory(fontProvider);
    }

    @Test
    void encryptedDocument_shouldRequirePasswordToOpen() {
        // given
        byte[] pdfBytes = createEncryptedPdf();

        // when & then
        assertThrows(InvalidPasswordException.class, () -> {
            try (PDDocument ignored = Loader.loadPDF(pdfBytes)) {
                // nie powinno się udać bez hasła
            }
        });
    }

    @Test
    void encryptedDocument_shouldOpenWithClientPassword() throws IOException {
        // given
        byte[] pdfBytes = createEncryptedPdf();

        // when & then
        assertDoesNotThrow(() -> {
            try (PDDocument doc = Loader.loadPDF(pdfBytes, CLIENT_PASSWORD)) {
                assertNotNull(doc);
                assertEquals(1, doc.getNumberOfPages());
            }
        });
    }

    @Test
    void encryptedDocument_shouldOpenWithAdminPassword() throws IOException {
        // given
        byte[] pdfBytes = createEncryptedPdf();

        // when & then
        assertDoesNotThrow(() -> {
            try (PDDocument doc = Loader.loadPDF(pdfBytes, ADMIN_PASSWORD)) {
                assertNotNull(doc);
                assertEquals(1, doc.getNumberOfPages());
            }
        });
    }

    @Test
    void encryptedDocument_shouldNotOpenWithWrongPassword() {
        // given
        byte[] pdfBytes = createEncryptedPdf();

        // when & then
        assertThrows(InvalidPasswordException.class, () -> {
            try (PDDocument ignored = Loader.loadPDF(pdfBytes, "wrongPassword")) {
                // nie powinno się udać
            }
        });
    }

    @Test
    void encryptedDocument_clientPassword_shouldAllowPrinting() throws IOException {
        // given
        byte[] pdfBytes = createEncryptedPdf();

        // when
        try (PDDocument doc = Loader.loadPDF(pdfBytes, CLIENT_PASSWORD)) {
            AccessPermission permissions = doc.getCurrentAccessPermission();

            // then
            assertTrue(permissions.canPrint());
        }
    }

    @Test
    void encryptedDocument_clientPassword_shouldDenyModification() throws IOException {
        // given
        byte[] pdfBytes = createEncryptedPdf();

        // when
        try (PDDocument doc = Loader.loadPDF(pdfBytes, CLIENT_PASSWORD)) {
            AccessPermission permissions = doc.getCurrentAccessPermission();

            // then
            assertFalse(permissions.canModify());
        }
    }

    @Test
    void encryptedDocument_clientPassword_shouldDenyContentExtraction() throws IOException {
        // given
        byte[] pdfBytes = createEncryptedPdf();

        // when
        try (PDDocument doc = Loader.loadPDF(pdfBytes, CLIENT_PASSWORD)) {
            AccessPermission permissions = doc.getCurrentAccessPermission();

            // then
            assertFalse(permissions.canExtractContent());
        }
    }

    @Test
    void encryptedDocument_clientPassword_shouldAllowAccessibility() throws IOException {
        // given
        byte[] pdfBytes = createEncryptedPdf();

        // when
        try (PDDocument doc = Loader.loadPDF(pdfBytes, CLIENT_PASSWORD)) {
            AccessPermission permissions = doc.getCurrentAccessPermission();

            // then
            assertTrue(permissions.canExtractForAccessibility());
        }
    }

    @Test
    void encryptedDocument_adminPassword_shouldHaveFullPermissions() throws IOException {
        // given
        byte[] pdfBytes = createEncryptedPdf();

        // when
        try (PDDocument doc = Loader.loadPDF(pdfBytes, ADMIN_PASSWORD)) {
            AccessPermission permissions = doc.getCurrentAccessPermission();

            // then - admin ma pełne uprawnienia (owner password)
            assertTrue(permissions.isOwnerPermission());
        }
    }

    private byte[] createEncryptedPdf() {
        return pdfDocumentFactory.createDocument(builder -> builder
                .setEncryption(PdfEncryptionConfig.forClientAndAdmin(CLIENT_PASSWORD, ADMIN_PASSWORD))
                .addPage()
                .createPrimaryHeader("Test zaszyfrowanego dokumentu")
                .createLeftAlignedParagraph("To jest testowy dokument z szyfrowaniem.")
        );
    }
}
