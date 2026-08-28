package in.joyway.service.storage;

import in.joyway.config.StorageProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LocalDocumentStorageServiceTest {

    @TempDir
    Path tempDir;

    private LocalDocumentStorageService service;

    @BeforeEach
    void setUp() {
        StorageProperties properties = new StorageProperties();
        properties.setLocalPath(tempDir.toString());
        service = new LocalDocumentStorageService(properties);
    }

    @Test
    void storeRetrieveDelete_roundTrip() throws Exception {
        byte[] jpeg = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x00};
        MockMultipartFile file = new MockMultipartFile(
                "file", "photo.jpg", "image/jpeg", jpeg);

        String key = service.store(file, "package-images");
        assertTrue(service.exists(key));

        Optional<InputStream> stream = service.retrieve(key);
        assertTrue(stream.isPresent());
        stream.get().close();

        service.delete(key);
        assertFalse(service.exists(key));
    }

    @Test
    void putObject_preservesExplicitKey() throws Exception {
        byte[] jpeg = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x00};
        String key = "package-images/existing-uuid.jpg";
        service.putObject(key, jpeg, "image/jpeg");

        assertTrue(service.exists(key));
        assertTrue(service.retrieve(key).isPresent());
    }
}
