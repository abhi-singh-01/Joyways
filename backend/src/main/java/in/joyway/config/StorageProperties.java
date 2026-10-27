package in.joyway.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "storage")
@Getter
@Setter
public class StorageProperties {

    /**
     * {@code local} for development, {@code azure} for Azure Blob Storage,
     * {@code s3} for S3-compatible providers (Cloudflare R2, Backblaze B2, MinIO).
     */
    private String provider = "local";

    /**
     * Service endpoint ({@code STORAGE_ENDPOINT}).
     * <ul>
     *   <li>Azure: {@code https://<storage-account>.blob.core.windows.net}</li>
     *   <li>Cloudflare R2: {@code https://<account-id>.r2.cloudflarestorage.com}</li>
     * </ul>
     * Required when {@link #provider} is {@code azure} or {@code s3}.
     */
    private String endpoint;

    /**
     * Access key ID ({@code STORAGE_ACCESS_KEY}).
     * <ul>
     *   <li>Azure: storage account name (only needed when {@link #useManagedIdentity} is false)</li>
     *   <li>S3-compatible: R2 / B2 access key ID</li>
     * </ul>
     */
    private String accessKey;

    /**
     * Secret key ({@code STORAGE_SECRET_KEY}).
     * <ul>
     *   <li>Azure: storage account access key (shared-key auth)</li>
     *   <li>S3-compatible: R2 / B2 secret access key</li>
     * </ul>
     * Not used when {@link #useManagedIdentity} is true.
     */
    private String secretKey;

    /**
     * Legacy ({@code STORAGE_BUCKET}): container/bucket name alias.
     * Prefer {@link #container} / {@code STORAGE_CONTAINER}.
     */
    private String bucket = "joyway-documents";

    /** Blob container / bucket name ({@code STORAGE_CONTAINER}). Defaults to {@link #bucket}. */
    private String container;

    /**
     * Local filesystem root ({@code STORAGE_LOCAL_PATH}). Used when {@link #provider} is
     * {@code local}, and as the migration source path for the optional {@code storage-migration}
     * profile.
     */
    private String localPath = "./storage";

    /** When true, authenticate with {@code DefaultAzureCredential} (Managed Identity in Azure). */
    private boolean useManagedIdentity;

    /** Lifetime of read-only SAS / presigned URLs when explicitly requested. */
    private int sasExpiryMinutes = 15;

    /**
     * S3 region ({@code STORAGE_S3_REGION}). Defaults to {@code auto} (correct for Cloudflare R2).
     * Set to your region for other providers, e.g. {@code us-east-1} for Backblaze B2.
     */
    private String s3Region = "auto";

    /**
     * Public base URL for S3-compatible buckets with public access enabled
     * ({@code STORAGE_PUBLIC_BASE_URL}). When set, {@link
     * in.joyway.service.storage.S3CompatibleStorageService#getReadAccessUrl} returns a permanent
     * public URL instead of a presigned URL. Example: {@code https://pub-abc123.r2.dev}.
     */
    private String publicBaseUrl;

    public String getContainerName() {
        if (container != null && !container.isBlank()) {
            return container.trim();
        }
        return bucket != null && !bucket.isBlank() ? bucket.trim() : "joyway-documents";
    }

    public boolean isAzureProvider() {
        return "azure".equalsIgnoreCase(provider);
    }

    public boolean isS3Provider() {
        return "s3".equalsIgnoreCase(provider);
    }

    public boolean isLocalProvider() {
        return provider == null || provider.isBlank() || "local".equalsIgnoreCase(provider);
    }
}
