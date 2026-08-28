package in.joyway.service.storage;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StorageContentTypeResolverTest {

    @Test
    void fromStorageKey_mapsKnownExtensions() {
        assertEquals(MediaType.IMAGE_JPEG_VALUE,
                StorageContentTypeResolver.fromStorageKey("package-images/uuid.jpg"));
        assertEquals(MediaType.IMAGE_PNG_VALUE,
                StorageContentTypeResolver.fromStorageKey("traveller-documents/uuid.png"));
        assertEquals(MediaType.APPLICATION_PDF_VALUE,
                StorageContentTypeResolver.fromStorageKey("traveller-documents/uuid.pdf"));
    }

    @Test
    void fromStorageKey_defaultsToOctetStream() {
        assertEquals(MediaType.APPLICATION_OCTET_STREAM_VALUE,
                StorageContentTypeResolver.fromStorageKey("package-images/uuid"));
    }
}
