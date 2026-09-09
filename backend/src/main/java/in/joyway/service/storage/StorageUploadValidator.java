package in.joyway.service.storage;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Shared upload validation for all storage providers.
 * Never trusts browser-supplied Content-Type or filename alone.
 */
public final class StorageUploadValidator {

    public static final String PACKAGE_IMAGES_CATEGORY = "package-images";
    public static final String TRAVELLER_DOCUMENTS_CATEGORY = "traveller-documents";

    private static final String MIME_JPEG = "image/jpeg";
    private static final String MIME_PNG = "image/png";
    private static final String MIME_WEBP = "image/webp";
    private static final String MIME_PDF = "application/pdf";

    private static final Set<String> TRAVELLER_DOCUMENT_CONTENT_TYPES = Set.of(
            MIME_JPEG, MIME_PDF
    );
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            MIME_JPEG, MIME_PNG, MIME_WEBP, MIME_PDF
    );
    private static final Set<String> PACKAGE_IMAGE_CONTENT_TYPES = Set.of(
            MIME_JPEG, MIME_WEBP
    );
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            ".jpg", ".jpeg", ".png", ".webp", ".pdf"
    );
    private static final Map<String, String> CONTENT_TYPE_TO_EXTENSION = Map.of(
            MIME_JPEG, ".jpg",
            MIME_PNG, ".png",
            MIME_WEBP, ".webp",
            MIME_PDF, ".pdf"
    );
    private static final long MAX_TRAVELLER_DOCUMENT_BYTES = 2L * 1024 * 1024;
    private static final long MAX_PACKAGE_IMAGE_BYTES = 2L * 1024 * 1024;

    private StorageUploadValidator() {
    }

    /** Validates traveller ID documents (2MB max per file). */
    public static void validate(MultipartFile file) {
        validateTravellerDocument(file);
    }

    public static void validateTravellerDocument(MultipartFile file) {
        validateFile(
                file,
                MAX_TRAVELLER_DOCUMENT_BYTES,
                "2MB",
                TRAVELLER_DOCUMENT_CONTENT_TYPES,
                "File type not allowed. Use JPEG or PDF only."
        );
    }

    /**
     * Package photos are public marketing assets. Only JPEG/JPG/WebP under 2MB are accepted;
     * remote URL attachments are rejected separately in the admin service.
     */
    public static void validatePackageImage(MultipartFile file) {
        validateFile(file, MAX_PACKAGE_IMAGE_BYTES, "2MB", ALLOWED_CONTENT_TYPES);
        String contentType = normalizeContentType(file.getContentType());
        if (contentType == null || !PACKAGE_IMAGE_CONTENT_TYPES.contains(contentType)) {
            throw new IllegalArgumentException(
                    "Package images must be JPEG, JPG, or WebP and under 2MB.");
        }
    }

    public static void validateForCategory(MultipartFile file, String category) {
        if (PACKAGE_IMAGES_CATEGORY.equals(category)) {
            validatePackageImage(file);
        } else {
            validateTravellerDocument(file);
        }
    }

    public static String resolveSafeExtension(MultipartFile file) {
        String fromName = extractExtension(file.getOriginalFilename());
        if (fromName != null && ALLOWED_EXTENSIONS.contains(fromName)) {
            return fromName;
        }
        String contentType = normalizeContentType(file.getContentType());
        if (contentType != null) {
            return CONTENT_TYPE_TO_EXTENSION.getOrDefault(contentType, ".bin");
        }
        throw new IllegalArgumentException("File extension not allowed.");
    }

    private static void validateFile(
            MultipartFile file,
            long maxBytes,
            String maxLabel,
            Set<String> allowedContentTypes
    ) {
        validateFile(file, maxBytes, maxLabel, allowedContentTypes,
                "File type not allowed. Use JPEG, PNG, WebP, or PDF.");
    }

    private static void validateFile(
            MultipartFile file,
            long maxBytes,
            String maxLabel,
            Set<String> allowedContentTypes,
            String typeErrorMessage
    ) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is required.");
        }
        if (file.getSize() > maxBytes) {
            throw new IllegalArgumentException("File size exceeds maximum allowed (" + maxLabel + ").");
        }
        String contentType = normalizeContentType(file.getContentType());
        if (contentType == null || !allowedContentTypes.contains(contentType)) {
            throw new IllegalArgumentException(typeErrorMessage);
        }
        validateMagicBytes(file, contentType);
        UploadMalwareScanner.scan(file);
    }

    private static void validateMagicBytes(MultipartFile file, String contentType) {
        try {
            byte[] header = file.getInputStream().readNBytes(12);
            if (!matchesContentType(header, contentType)) {
                throw new IllegalArgumentException("File content does not match declared type.");
            }
        } catch (IOException ex) {
            throw new IllegalArgumentException("Unable to read uploaded file.");
        }
    }

    private static boolean matchesContentType(byte[] header, String contentType) {
        return switch (contentType.toLowerCase(Locale.ROOT)) {
            case MIME_JPEG -> header.length >= 3
                    && (header[0] & 0xFF) == 0xFF
                    && (header[1] & 0xFF) == 0xD8
                    && (header[2] & 0xFF) == 0xFF;
            case MIME_PNG -> header.length >= 8
                    && header[0] == (byte) 0x89
                    && header[1] == 0x50
                    && header[2] == 0x4E
                    && header[3] == 0x47;
            case MIME_PDF -> header.length >= 4
                    && header[0] == 0x25
                    && header[1] == 0x50
                    && header[2] == 0x44
                    && header[3] == 0x46;
            case MIME_WEBP -> header.length >= 12
                    && header[0] == 0x52
                    && header[1] == 0x49
                    && header[2] == 0x46
                    && header[3] == 0x46
                    && header[8] == 0x57
                    && header[9] == 0x45
                    && header[10] == 0x42
                    && header[11] == 0x50;
            default -> false;
        };
    }

    private static String extractExtension(String filename) {
        if (filename == null || filename.isBlank()) {
            return null;
        }
        String sanitized = Path.of(filename).getFileName().toString();
        int dotIndex = sanitized.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == sanitized.length() - 1) {
            return null;
        }
        return sanitized.substring(dotIndex).toLowerCase(Locale.ROOT);
    }

    private static String normalizeContentType(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            return null;
        }
        String lower = contentType.toLowerCase(Locale.ROOT);
        if ("image/jpg".equals(lower) || "image/pjpeg".equals(lower)) {
            return MIME_JPEG;
        }
        return lower;
    }
}
