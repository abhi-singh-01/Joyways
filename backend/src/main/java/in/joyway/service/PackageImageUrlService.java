package in.joyway.service;

import in.joyway.domain.entity.PackageImage;
import in.joyway.service.storage.StorageKeyValidator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Resolves public package image URLs at read time from Azure/local blob storage keys.
 * The database stores {@code private_storage_key} only — not full URLs.
 */
@Service
public class PackageImageUrlService {

    @Value("${joyway.public-api-base-url:http://localhost:8080}")
    private String publicApiBaseUrl;

    public String resolvePublicUrl(PackageImage image) {
        if (image == null) {
            return null;
        }
        String storageKey = image.getPrivateStorageKey();
        if (storageKey == null || storageKey.isBlank()) {
            return null;
        }
        try {
            StorageKeyValidator.validate(storageKey);
        } catch (IllegalArgumentException ex) {
            return null;
        }
        if (!storageKey.startsWith("package-images/")) {
            return null;
        }
        String base = publicApiBaseUrl.endsWith("/")
                ? publicApiBaseUrl.substring(0, publicApiBaseUrl.length() - 1)
                : publicApiBaseUrl;
        return base + "/api/files/" + storageKey;
    }
}
