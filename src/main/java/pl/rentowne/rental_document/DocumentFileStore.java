package pl.rentowne.rental_document;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;

@Component
@RequiredArgsConstructor
class DocumentFileStore {
    private final DocumentProperties properties;

    String write(UUID documentId, byte[] content) throws IOException {
        Path directory = Path.of(properties.directory()).toAbsolutePath().normalize();
        Files.createDirectories(directory);
        String filename = documentId + ".pdf";
        Path temporary = Files.createTempFile(directory, documentId + "-", ".tmp");
        try {
            Files.write(temporary, content);
            try { Files.move(temporary, directory.resolve(filename), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
            catch (AtomicMoveNotSupportedException e) { Files.move(temporary, directory.resolve(filename), StandardCopyOption.REPLACE_EXISTING); }
        } finally { Files.deleteIfExists(temporary); }
        return filename;
    }

    byte[] read(String filename, String checksum) throws IOException {
        if (filename == null || !filename.matches("[a-f0-9-]{36}\\.pdf")) throw new IOException("Niepoprawna nazwa pliku dokumentu");
        byte[] bytes = Files.readAllBytes(Path.of(properties.directory()).toAbsolutePath().normalize().resolve(filename));
        if (!hash(bytes).equals(checksum)) throw new IOException("Plik dokumentu nie odpowiada zapisanej sumie kontrolnej");
        return bytes;
    }

    static String hash(byte[] content) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content)); }
        catch (NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
}
