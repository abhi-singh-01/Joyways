package in.joyway.service.storage;

import org.springframework.http.MediaType;

import java.util.Locale;
import java.util.Map;

public final class StorageContentTypeResolver {

    private static final Map<String, String> EXTENSION_TO_MEDIA_TYPE = Map.of(
            ".jpg", MediaType.IMAGE_JPEG_VALUE,
            ".jpeg", MediaType.IMAGE_JPEG_VALUE,
            ".png", MediaType.IMAGE_PNG_VALUE,
            ".webp", "image/webp",
            ".pdf", MediaType.APPLICATION_PDF_VALUE
    );

    private StorageContentTypeResolver() {
    }

    public static String fromStorageKey(String storageKey) {
        if (storageKey == null) {
            return MediaType.APPLICATION_OCTET_STREAM_VALUE;
        }
        int dot = storageKey.lastIndexOf('.');
        if (dot < 0) {
            return MediaType.APPLICATION_OCTET_STREAM_VALUE;
        }
        String ext = storageKey.substring(dot).toLowerCase(Locale.ROOT);
        return EXTENSION_TO_MEDIA_TYPE.getOrDefault(ext, MediaType.APPLICATION_OCTET_STREAM_VALUE);
    }
}
