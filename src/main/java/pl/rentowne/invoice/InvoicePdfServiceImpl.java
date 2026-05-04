package pl.rentowne.invoice;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import pl.rentowne.util.ExistingFileRenameUtils;
import pl.rentowne.util.UploadedFilesNameUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
@Service
class InvoicePdfServiceImpl implements InvoicePdfService {

    @Value("${application.invoicePdfDir:data/invoicePdfs/}")
    private String invoicePdfDir;

    @Override
    public String uploadInvoicePdf(String filename, InputStream inputStream) {
        createDirectoryIfNotExists(invoicePdfDir);

        String newFileName = UploadedFilesNameUtils.slugifyFileName(filename);
        newFileName = ExistingFileRenameUtils.renameIfExists(Path.of(invoicePdfDir), newFileName);
        Path filePath = Paths.get(invoicePdfDir).resolve(newFileName);

        try (OutputStream outputStream = Files.newOutputStream(filePath)) {
            inputStream.transferTo(outputStream);
            log.info("Zapisano plik PDF faktury: {}", newFileName);
        } catch (IOException e) {
            log.error("Błąd podczas zapisywania pliku PDF faktury: {}", filename, e);
            throw new RuntimeException("Nie mogę zapisać pliku PDF faktury", e);
        }
        return newFileName;
    }

    @Override
    public Resource getInvoicePdf(String filename) {
        FileSystemResourceLoader fileSystemResourceLoader = new FileSystemResourceLoader();
        return fileSystemResourceLoader.getResource(invoicePdfDir + filename);
    }

    @Override
    public void deleteInvoicePdf(String filename) {
        if (filename == null || filename.isEmpty()) {
            return;
        }

        Path filePath = Paths.get(invoicePdfDir).resolve(filename);
        try {
            if (Files.exists(filePath)) {
                Files.delete(filePath);
                log.info("Usunięto plik PDF faktury: {}", filename);
            }
        } catch (IOException e) {
            log.error("Błąd podczas usuwania pliku PDF faktury: {}", filename, e);
            throw new RuntimeException("Nie mogę usunąć pliku PDF faktury", e);
        }
    }

    private void createDirectoryIfNotExists(String directoryPath) {
        Path path = Paths.get(directoryPath);
        if (!Files.exists(path)) {
            try {
                Files.createDirectories(path);
                log.info("Utworzono katalog dla PDF-ów faktur: {}", directoryPath);
            } catch (IOException e) {
                log.error("Nie udało się stworzyć katalogu: {}", directoryPath, e);
                throw new RuntimeException("Nie udało się stworzyć katalogu: " + directoryPath, e);
            }
        }
    }
}

