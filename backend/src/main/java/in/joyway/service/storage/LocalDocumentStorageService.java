package in.joyway.service.storage;

import in.joyway.config.StorageProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;
import java.util.UUID;

/**
 * Local filesystem storage for development and tests.
 */
@Slf4j
public class LocalDocumentStorageService implements DocumentStorageService {

    private final StorageProperties storageProperties;

    public LocalDocumentStorageService(StorageProperties storageProperties) {
        this.storageProperties = storageProperties;
    }

    @Override
    public String store(MultipartFile file, String category) throws IOException {
        StorageKeyValidator.validateCategory(category);
        StorageUploadValidator.validate(file);

        Path basePath = Path.of(storageProperties.getLocalPath(), category).normalize().toAbsolutePath();
        Path storageRoot = Path.of(storageProperties.getLocalPath()).normalize().toAbsolutePath();
        if (!basePath.startsWith(storageRoot)) {
            throw new IllegalArgumentException("Invalid storage category.");
        }
        Files.createDirectories(basePath);

        String extension = StorageUploadValidator.resolveSafeExtension(file);
        String safeName = UUID.randomUUID() + extension;
        Path targetPath = basePath.resolve(safeName).normalize();
        if (!targetPath.startsWith(basePath)) {
            throw new IllegalArgumentException("Invalid file name.");
        }

        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
        }

        String storageKey = category + "/" + safeName;
        log.info("event=storage.uploaded provider=local category={}", category);
        return storageKey;
    }

    @Override
    public Optional<InputStream> retrieve(String storageKey) throws IOException {
        StorageKeyValidator.validate(storageKey);
        Path filePath = resolveStoragePath(storageKey);
        if (!Files.exists(filePath)) {
            return Optional.empty();
        }
        return Optional.of(Files.newInputStream(filePath));
    }

    @Override
    public void delete(String storageKey) throws IOException {
        StorageKeyValidator.validate(storageKey);
        Files.deleteIfExists(resolveStoragePath(storageKey));
        log.info("event=storage.deleted provider=local");
    }

    @Override
    public boolean exists(String storageKey) {
        StorageKeyValidator.validate(storageKey);
        return Files.exists(resolveStoragePath(storageKey));
    }

    @Override
    public void putObject(String storageKey, byte[] content, String contentType) throws IOException {
        StorageKeyValidator.validate(storageKey);
        Path filePath = resolveStoragePath(storageKey);
        Files.createDirectories(filePath.getParent());
        Files.write(filePath, content);
        log.info("event=storage.put provider=local");
    }

    private Path resolveStoragePath(String storageKey) {
        Path storageRoot = Path.of(storageProperties.getLocalPath()).normalize().toAbsolutePath();
        Path filePath = storageRoot.resolve(storageKey).normalize().toAbsolutePath();
        if (!filePath.startsWith(storageRoot)) {
            throw new IllegalArgumentException("Invalid storage key.");
        }
        return filePath;
    }
}
