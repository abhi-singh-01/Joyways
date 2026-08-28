package in.joyway.service.storage;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

/**
 * Storage service abstraction for documents and images (business code must not depend on
 * Azure SDK or filesystem APIs directly).
 *
 * <p>Implementations: {@link LocalDocumentStorageService} (dev) and
 * {@link AzureBlobStorageService} (production).
 */
public interface DocumentStorageService {

    String store(MultipartFile file, String category) throws IOException;

    Optional<InputStream> retrieve(String storageKey) throws IOException;

    void delete(String storageKey) throws IOException;

    boolean exists(String storageKey);

    /**
     * Writes content at an explicit storage key. Used by one-off migration tooling;
     * skips upload validation because objects already exist on disk.
     */
    void putObject(String storageKey, byte[] content, String contentType) throws IOException;

    /**
     * Optional short-lived HTTPS read URL (SAS). Implementations that cannot issue
     * SAS tokens return empty and callers should stream via {@link #retrieve(String)}.
     */
    default Optional<String> getReadAccessUrl(String storageKey) {
        return Optional.empty();
    }
}
