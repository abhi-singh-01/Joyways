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

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp", "application/pdf"
    );
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            ".jpg", ".jpeg", ".png", ".webp", ".pdf"
    );
    private static final Map<String, String> CONTENT_TYPE_TO_EXTENSION = Map.of(
            "image/jpeg", ".jpg",
            "image/png", ".png",
            "image/webp", ".webp",
            "application/pdf", ".pdf"
    );
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;

    private StorageUploadValidator() {
    }

    public static void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is required.");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("File size exceeds maximum allowed (5MB).");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new IllegalArgumentException("File type not allowed. Use JPEG, PNG, WebP, or PDF.");
        }
        validateMagicBytes(file, contentType);
    }

    public static String resolveSafeExtension(MultipartFile file) {
        String fromName = extractExtension(file.getOriginalFilename());
        if (fromName != null && ALLOWED_EXTENSIONS.contains(fromName)) {
            return fromName;
        }
        String contentType = file.getContentType();
        if (contentType != null) {
            return CONTENT_TYPE_TO_EXTENSION.getOrDefault(contentType.toLowerCase(Locale.ROOT), ".bin");
        }
        throw new IllegalArgumentException("File extension not allowed.");
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
            case "image/jpeg" -> header.length >= 3
                    && (header[0] & 0xFF) == 0xFF
                    && (header[1] & 0xFF) == 0xD8
                    && (header[2] & 0xFF) == 0xFF;
            case "image/png" -> header.length >= 8
                    && header[0] == (byte) 0x89
                    && header[1] == 0x50
                    && header[2] == 0x4E
                    && header[3] == 0x47;
            case "application/pdf" -> header.length >= 4
                    && header[0] == 0x25
                    && header[1] == 0x50
                    && header[2] == 0x44
                    && header[3] == 0x46;
            case "image/webp" -> header.length >= 12
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
}
