package in.joyway.service.storage;

import in.joyway.config.StorageProperties;
import in.joyway.exception.StorageOperationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.time.Duration;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

/**
 * S3-compatible storage implementation for Cloudflare R2, Backblaze B2, MinIO, etc.
 *
 * <p>Configure with:
 * <ul>
 *   <li>{@code STORAGE_PROVIDER=s3}</li>
 *   <li>{@code STORAGE_ENDPOINT=https://<account-id>.r2.cloudflarestorage.com}</li>
 *   <li>{@code STORAGE_ACCESS_KEY=<R2 access key id>}</li>
 *   <li>{@code STORAGE_SECRET_KEY=<R2 secret access key>}</li>
 *   <li>{@code STORAGE_BUCKET=joyway-documents}</li>
 *   <li>{@code STORAGE_S3_REGION=auto} (default, correct for Cloudflare R2)</li>
 *   <li>{@code STORAGE_PUBLIC_BASE_URL=https://pub-<hash>.r2.dev} (optional — for public buckets)</li>
 * </ul>
 *
 * <p>When {@code STORAGE_PUBLIC_BASE_URL} is set, {@link #getReadAccessUrl} returns a permanent
 * public URL. Otherwise it generates a presigned URL valid for {@code STORAGE_SAS_EXPIRY_MINUTES}.
 */
@Slf4j
public class S3CompatibleStorageService implements DocumentStorageService {

    private final StorageProperties properties;
    private final S3Client s3Client;
    private final S3Presigner presigner;
    private final String bucket;

    public S3CompatibleStorageService(StorageProperties properties) {
        this.properties = properties;
        this.bucket = properties.getContainerName();

        String endpoint = requireEndpoint(properties);
        Region region = Region.of(properties.getS3Region() != null ? properties.getS3Region() : "auto");
        StaticCredentialsProvider creds = StaticCredentialsProvider.create(
                AwsBasicCredentials.create(
                        requireAccessKey(properties),
                        requireSecretKey(properties)
                )
        );

        this.s3Client = S3Client.builder()
                .endpointOverride(URI.create(endpoint))
                .credentialsProvider(creds)
                .region(region)
                .forcePathStyle(true)
                .build();

        this.presigner = S3Presigner.builder()
                .endpointOverride(URI.create(endpoint))
                .credentialsProvider(creds)
                .region(region)
                .build();

        assertBucketAccessible();
    }

    /** Package-private for unit tests with mocked clients. */
    S3CompatibleStorageService(StorageProperties properties, S3Client s3Client, S3Presigner presigner) {
        this.properties = properties;
        this.bucket = properties.getContainerName();
        this.s3Client = s3Client;
        this.presigner = presigner;
    }

    @Override
    public String store(MultipartFile file, String category) throws IOException {
        StorageKeyValidator.validateCategory(category);
        StorageUploadValidator.validateForCategory(file, category);

        String extension = StorageUploadValidator.resolveSafeExtension(file);
        String key = category + "/" + UUID.randomUUID() + extension;
        StorageKeyValidator.validate(key);

        String contentType = file.getContentType() != null
                ? file.getContentType().toLowerCase(Locale.ROOT)
                : "application/octet-stream";
        byte[] bytes = file.getBytes();

        try {
            s3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(bucket)
                            .key(key)
                            .contentType(contentType)
                            .contentLength((long) bytes.length)
                            .build(),
                    RequestBody.fromBytes(bytes)
            );
            log.info("event=storage.uploaded provider=s3 category={}", category);
        } catch (RuntimeException ex) {
            log.error("event=storage.upload.failed provider=s3 category={} reason={}",
                    category, ex.getClass().getSimpleName());
            throw new StorageOperationException("Unable to store file.", "STORAGE_UPLOAD_FAILED");
        }
        return key;
    }

    @Override
    public Optional<InputStream> retrieve(String storageKey) throws IOException {
        StorageKeyValidator.validate(storageKey);
        try {
            byte[] data = s3Client.getObjectAsBytes(
                    GetObjectRequest.builder().bucket(bucket).key(storageKey).build()
            ).asByteArray();
            return Optional.of(new ByteArrayInputStream(data));
        } catch (NoSuchKeyException ex) {
            return Optional.empty();
        } catch (RuntimeException ex) {
            log.error("event=storage.download.failed provider=s3 reason={}", ex.getClass().getSimpleName());
            throw new StorageOperationException("Unable to retrieve file.", "STORAGE_DOWNLOAD_FAILED");
        }
    }

    @Override
    public void delete(String storageKey) throws IOException {
        StorageKeyValidator.validate(storageKey);
        try {
            s3Client.deleteObject(
                    DeleteObjectRequest.builder().bucket(bucket).key(storageKey).build()
            );
            log.info("event=storage.deleted provider=s3");
        } catch (RuntimeException ex) {
            log.error("event=storage.delete.failed provider=s3 reason={}", ex.getClass().getSimpleName());
            throw new StorageOperationException("Unable to delete file.", "STORAGE_DELETE_FAILED");
        }
    }

    @Override
    public boolean exists(String storageKey) {
        StorageKeyValidator.validate(storageKey);
        try {
            s3Client.headObject(HeadObjectRequest.builder().bucket(bucket).key(storageKey).build());
            return true;
        } catch (NoSuchKeyException ex) {
            return false;
        }
    }

    @Override
    public void putObject(String storageKey, byte[] content, String contentType) throws IOException {
        StorageKeyValidator.validate(storageKey);
        String resolved = contentType != null && !contentType.isBlank()
                ? contentType
                : StorageContentTypeResolver.fromStorageKey(storageKey);
        try {
            s3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(bucket)
                            .key(storageKey)
                            .contentType(resolved)
                            .contentLength((long) content.length)
                            .build(),
                    RequestBody.fromBytes(content)
            );
            log.info("event=storage.put provider=s3");
        } catch (RuntimeException ex) {
            log.error("event=storage.put.failed provider=s3 reason={}", ex.getClass().getSimpleName());
            throw new StorageOperationException("Unable to store file.", "STORAGE_UPLOAD_FAILED");
        }
    }

    /**
     * Returns a public URL when {@code STORAGE_PUBLIC_BASE_URL} is configured, or a presigned
     * URL valid for {@code STORAGE_SAS_EXPIRY_MINUTES} otherwise.
     */
    @Override
    public Optional<String> getReadAccessUrl(String storageKey) {
        StorageKeyValidator.validate(storageKey);

        String publicBase = properties.getPublicBaseUrl();
        if (publicBase != null && !publicBase.isBlank()) {
            String base = publicBase.endsWith("/") ? publicBase : publicBase + "/";
            return Optional.of(base + storageKey);
        }

        try {
            int expiry = properties.getSasExpiryMinutes() > 0 ? properties.getSasExpiryMinutes() : 15;
            GetObjectPresignRequest req = GetObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofMinutes(expiry))
                    .getObjectRequest(r -> r.bucket(bucket).key(storageKey))
                    .build();
            return Optional.of(presigner.presignGetObject(req).url().toString());
        } catch (RuntimeException ex) {
            log.warn("event=storage.presign.failed provider=s3 reason={}", ex.getClass().getSimpleName());
            return Optional.empty();
        }
    }

    private void assertBucketAccessible() {
        try {
            s3Client.headBucket(HeadBucketRequest.builder().bucket(bucket).build());
        } catch (RuntimeException ex) {
            throw new IllegalStateException(
                    "Unable to access S3-compatible bucket '" + bucket + "'. " +
                    "Verify STORAGE_ENDPOINT, STORAGE_BUCKET, STORAGE_ACCESS_KEY, and STORAGE_SECRET_KEY.",
                    ex);
        }
    }

    private static String requireEndpoint(StorageProperties p) {
        if (p.getEndpoint() == null || p.getEndpoint().isBlank()) {
            throw new IllegalStateException("STORAGE_ENDPOINT is required for S3-compatible storage.");
        }
        return p.getEndpoint().trim();
    }

    private static String requireAccessKey(StorageProperties p) {
        if (p.getAccessKey() == null || p.getAccessKey().isBlank()) {
            throw new IllegalStateException("STORAGE_ACCESS_KEY is required for S3-compatible storage.");
        }
        return p.getAccessKey().trim();
    }

    private static String requireSecretKey(StorageProperties p) {
        if (p.getSecretKey() == null || p.getSecretKey().isBlank()) {
            throw new IllegalStateException("STORAGE_SECRET_KEY is required for S3-compatible storage.");
        }
        return p.getSecretKey().trim();
    }
}
