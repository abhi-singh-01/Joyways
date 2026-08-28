package in.joyway.controller;

import in.joyway.exception.ResourceNotFoundException;
import in.joyway.service.storage.DocumentStorageService;
import in.joyway.service.storage.StorageContentTypeResolver;
import in.joyway.service.storage.StorageKeyValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.InputStream;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class PublicFileController {

    private static final String ALLOWED_CATEGORY = "package-images";

    private final DocumentStorageService documentStorageService;

    @GetMapping("/{category}/{filename}")
    public ResponseEntity<InputStreamResource> serveFile(
            @PathVariable String category,
            @PathVariable String filename
    ) throws IOException {
        if (!ALLOWED_CATEGORY.equals(category)) {
            throw new ResourceNotFoundException("File not found.");
        }

        if (filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
            throw new ResourceNotFoundException("File not found.");
        }

        String storageKey = category + "/" + filename;
        StorageKeyValidator.validate(storageKey);

        InputStream stream = documentStorageService.retrieve(storageKey)
                .orElseThrow(() -> new ResourceNotFoundException("File not found."));

        String contentType = StorageContentTypeResolver.fromStorageKey(storageKey);
        InputStreamResource resource = new InputStreamResource(stream);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .body(resource);
    }
}
