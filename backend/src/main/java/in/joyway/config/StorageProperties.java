package in.joyway.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "storage")
@Getter
@Setter
public class StorageProperties {

    /** {@code local} for development, {@code azure} for production. */
    private String provider = "local";

    /**
     * Azure Blob service endpoint ({@code STORAGE_ENDPOINT}), e.g.
     * {@code https://<storage-account-name>.blob.core.windows.net}.
     * Required when {@link #provider} is {@code azure}.
     */
    private String endpoint;

    /**
     * Legacy ({@code STORAGE_ACCESS_KEY}): storage <em>account name</em> for shared-key auth.
     * Not required when {@link #useManagedIdentity} is true (account name may be parsed from
     * {@link #endpoint}). Prefer Managed Identity in production.
     */
    private String accessKey;

    /**
     * Legacy ({@code STORAGE_SECRET_KEY}): storage account access key for shared-key auth only.
     * Not used when {@link #useManagedIdentity} is true.
     */
    private String secretKey;

    /**
     * Legacy ({@code STORAGE_BUCKET}): container name alias. Prefer {@link #container} /
     * {@code STORAGE_CONTAINER}.
     */
    private String bucket = "joyway-documents";

    /** Blob container name ({@code STORAGE_CONTAINER}). Defaults to {@link #bucket}. */
    private String container;

    /**
     * Local filesystem root ({@code STORAGE_LOCAL_PATH}). Used when {@link #provider} is
     * {@code local}, and as the migration source path for the optional {@code storage-migration}
     * profile.
     */
    private String localPath = "./storage";

    /** When true, authenticate with {@code DefaultAzureCredential} (Managed Identity in Azure). */
    private boolean useManagedIdentity;

    /** Lifetime of read-only SAS URLs when explicitly requested. */
    private int sasExpiryMinutes = 15;

    public String getContainerName() {
        if (container != null && !container.isBlank()) {
            return container.trim();
        }
        return bucket != null && !bucket.isBlank() ? bucket.trim() : "joyway-documents";
    }

    public boolean isAzureProvider() {
        return "azure".equalsIgnoreCase(provider);
    }

    public boolean isLocalProvider() {
        return provider == null || provider.isBlank() || "local".equalsIgnoreCase(provider);
    }
}
