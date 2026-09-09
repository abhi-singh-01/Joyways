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

    /**
     * Optional Azure Front Door (or other CDN) base URL for package images.
     * When empty, images are served from {@link #publicApiBaseUrl}.
     */
    @Value("${joyway.package-image-cdn-base-url:}")
    private String packageImageCdnBaseUrl;

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
        String rawBase = (packageImageCdnBaseUrl != null && !packageImageCdnBaseUrl.isBlank())
                ? packageImageCdnBaseUrl
                : publicApiBaseUrl;
        String base = rawBase.endsWith("/")
                ? rawBase.substring(0, rawBase.length() - 1)
                : rawBase;
        return base + "/api/files/" + storageKey;
    }
}
