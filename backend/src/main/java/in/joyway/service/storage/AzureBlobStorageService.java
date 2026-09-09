package in.joyway.service.storage;

import com.azure.core.util.BinaryData;
import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.blob.models.BlobHttpHeaders;
import com.azure.storage.blob.models.UserDelegationKey;
import com.azure.storage.blob.sas.BlobSasPermission;
import com.azure.storage.blob.sas.BlobServiceSasSignatureValues;
import com.azure.storage.common.StorageSharedKeyCredential;
import com.azure.storage.common.sas.SasProtocol;
import in.joyway.config.StorageProperties;
import in.joyway.exception.StorageOperationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

/**
 * Azure Blob Storage implementation for production.
 */
@Slf4j
public class AzureBlobStorageService implements DocumentStorageService {

    private final StorageProperties properties;
    private final BlobContainerClient containerClient;
    private final StorageSharedKeyCredential sharedKeyCredential;
    private final BlobServiceClient blobServiceClient;

    public AzureBlobStorageService(StorageProperties properties) {
        this(properties, buildServiceClient(properties));
    }

    private AzureBlobStorageService(StorageProperties properties, BlobServiceClient serviceClient) {
        this(properties, serviceClient.getBlobContainerClient(properties.getContainerName()), serviceClient);
    }

    /** Package-private for unit tests with a mocked {@link BlobContainerClient}. */
    AzureBlobStorageService(StorageProperties properties, BlobContainerClient containerClient) {
        this(properties, containerClient, null);
    }

    AzureBlobStorageService(
            StorageProperties properties,
            BlobContainerClient containerClient,
            BlobServiceClient blobServiceClient
    ) {
        this.properties = properties;
        this.containerClient = containerClient;
        this.blobServiceClient = blobServiceClient;
        this.sharedKeyCredential = properties.isUseManagedIdentity()
                ? null
                : buildSharedKeyCredential(properties);
        assertContainerAccessible(containerClient, properties.getContainerName());
    }

    @Override
    public String store(MultipartFile file, String category) throws IOException {
        StorageKeyValidator.validateCategory(category);
        StorageUploadValidator.validateForCategory(file, category);

        String extension = StorageUploadValidator.resolveSafeExtension(file);
        String blobName = category + "/" + UUID.randomUUID() + extension;
        StorageKeyValidator.validate(blobName);

        byte[] bytes = file.getBytes();
        BlobClient blobClient = containerClient.getBlobClient(blobName);
        String contentType = file.getContentType() != null
                ? file.getContentType().toLowerCase(Locale.ROOT)
                : "application/octet-stream";

        try {
            blobClient.upload(BinaryData.fromBytes(bytes), true);
            blobClient.setHttpHeaders(new BlobHttpHeaders().setContentType(contentType));
        } catch (RuntimeException ex) {
            log.error("event=storage.upload.failed provider=azure category={} reason={}",
                    category, ex.getClass().getSimpleName());
            throw new StorageOperationException("Unable to store file.", "STORAGE_UPLOAD_FAILED");
        }

        log.info("event=storage.uploaded provider=azure category={}", category);
        return blobName;
    }

    @Override
    public Optional<InputStream> retrieve(String storageKey) throws IOException {
        StorageKeyValidator.validate(storageKey);
        BlobClient blobClient = containerClient.getBlobClient(storageKey);
        if (!Boolean.TRUE.equals(blobClient.exists())) {
            return Optional.empty();
        }
        try {
            byte[] data = blobClient.downloadContent().toBytes();
            return Optional.of(new ByteArrayInputStream(data));
        } catch (RuntimeException ex) {
            log.error("event=storage.download.failed provider=azure reason={}", ex.getClass().getSimpleName());
            throw new StorageOperationException("Unable to retrieve file.", "STORAGE_DOWNLOAD_FAILED");
        }
    }

    @Override
    public void delete(String storageKey) throws IOException {
        StorageKeyValidator.validate(storageKey);
        BlobClient blobClient = containerClient.getBlobClient(storageKey);
        try {
            blobClient.deleteIfExists();
            log.info("event=storage.deleted provider=azure");
        } catch (RuntimeException ex) {
            log.error("event=storage.delete.failed provider=azure reason={}", ex.getClass().getSimpleName());
            throw new StorageOperationException("Unable to delete file.", "STORAGE_DELETE_FAILED");
        }
    }

    @Override
    public boolean exists(String storageKey) {
        StorageKeyValidator.validate(storageKey);
        return containerClient.getBlobClient(storageKey).exists();
    }

    @Override
    public void putObject(String storageKey, byte[] content, String contentType) throws IOException {
        StorageKeyValidator.validate(storageKey);
        BlobClient blobClient = containerClient.getBlobClient(storageKey);
        String resolvedContentType = contentType != null && !contentType.isBlank()
                ? contentType
                : StorageContentTypeResolver.fromStorageKey(storageKey);
        try {
            blobClient.upload(BinaryData.fromBytes(content), true);
            blobClient.setHttpHeaders(new BlobHttpHeaders().setContentType(resolvedContentType));
            log.info("event=storage.put provider=azure");
        } catch (RuntimeException ex) {
            log.error("event=storage.put.failed provider=azure reason={}", ex.getClass().getSimpleName());
            throw new StorageOperationException("Unable to store file.", "STORAGE_UPLOAD_FAILED");
        }
    }

    @Override
    public Optional<String> getReadAccessUrl(String storageKey) {
        StorageKeyValidator.validate(storageKey);
        if (sharedKeyCredential == null && blobServiceClient == null) {
            return Optional.empty();
        }
        BlobClient blobClient = containerClient.getBlobClient(storageKey);
        if (blobClient == null || !blobClient.exists()) {
            return Optional.empty();
        }

        BlobSasPermission permission = new BlobSasPermission().setReadPermission(true);
        OffsetDateTime expiry = OffsetDateTime.now(ZoneOffset.UTC).plusMinutes(properties.getSasExpiryMinutes());
        BlobServiceSasSignatureValues values = new BlobServiceSasSignatureValues(expiry, permission)
                .setProtocol(SasProtocol.HTTPS_ONLY);

        try {
            if (sharedKeyCredential != null) {
                String sas = blobClient.generateSas(values);
                return Optional.of(blobClient.getBlobUrl() + "?" + sas);
            }
            if (blobServiceClient != null) {
                OffsetDateTime start = OffsetDateTime.now(ZoneOffset.UTC).minusMinutes(5);
                UserDelegationKey key = blobServiceClient.getUserDelegationKey(start, expiry);
                String sas = blobClient.generateUserDelegationSas(values, key);
                return Optional.of(blobClient.getBlobUrl() + "?" + sas);
            }
        } catch (RuntimeException ex) {
            log.warn("event=storage.sas.failed provider=azure reason={}", ex.getClass().getSimpleName());
            return Optional.empty();
        }
        return Optional.empty();
    }

    private static BlobServiceClient buildServiceClient(StorageProperties properties) {
        if (properties.isUseManagedIdentity()) {
            return new BlobServiceClientBuilder()
                    .endpoint(requireEndpoint(properties))
                    .credential(new DefaultAzureCredentialBuilder().build())
                    .buildClient();
        }
        StorageSharedKeyCredential credential = buildSharedKeyCredential(properties);
        return new BlobServiceClientBuilder()
                .endpoint(requireEndpoint(properties))
                .credential(credential)
                .buildClient();
    }

    private static StorageSharedKeyCredential buildSharedKeyCredential(StorageProperties properties) {
        String accountName = resolveAccountName(properties);
        if (properties.getSecretKey() == null || properties.getSecretKey().isBlank()) {
            throw new IllegalStateException(
                    "STORAGE_SECRET_KEY is required when STORAGE_USE_MANAGED_IDENTITY=false.");
        }
        return new StorageSharedKeyCredential(accountName, properties.getSecretKey());
    }

    static String requireEndpoint(StorageProperties properties) {
        if (properties.getEndpoint() == null || properties.getEndpoint().isBlank()) {
            throw new IllegalStateException("STORAGE_ENDPOINT is required for Azure storage.");
        }
        return properties.getEndpoint().trim();
    }

    static String resolveAccountName(StorageProperties properties) {
        if (properties.getAccessKey() != null && !properties.getAccessKey().isBlank()) {
            return properties.getAccessKey().trim();
        }
        String endpoint = requireEndpoint(properties);
        URI uri = URI.create(endpoint);
        String host = uri.getHost();
        if (host == null || !host.contains(".")) {
            throw new IllegalStateException("STORAGE_ENDPOINT must be a valid Azure Blob endpoint.");
        }
        return host.substring(0, host.indexOf('.'));
    }

    static void assertContainerAccessible(BlobContainerClient containerClient, String containerName) {
        try {
            if (!containerClient.exists()) {
                throw new IllegalStateException("Azure Blob container not found: " + containerName);
            }
        } catch (RuntimeException ex) {
            throw new IllegalStateException(
                    "Unable to access Azure Blob Storage. Verify STORAGE_ENDPOINT, container, and authentication.",
                    ex);
        }
    }
}
