package pl.rentowne.image;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import pl.rentowne.common.controler.AbstractController;
import pl.rentowne.user.UploadResponse;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

@RestController
@RequiredArgsConstructor
@Tag(name = "Pliki graficzne")
class ImageController extends AbstractController {

    private final ImageService imageService;

    @PostMapping("/api/upload-image")
    UploadResponse uploadImage(@RequestParam("file") MultipartFile multipartFile) {
        try (InputStream inputStream = multipartFile.getInputStream()) {
            String savedFileName = imageService.uploadImage(multipartFile.getOriginalFilename(), inputStream);
            return new UploadResponse(savedFileName);
        } catch (IOException e) {
            throw new RuntimeException("Coś poszło źle podczas wgrywania pliku", e);
        }
    }

    @GetMapping("/api/data/image/{filename}")
     ResponseEntity<Resource> serveFiles(@PathVariable String filename) throws IOException {
        Resource file = imageService.serveFiles(filename);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, Files.probeContentType(Path.of(filename)))
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=31536000")
                .body(file);
    }

}
