package in.joyway.service.storage;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class StorageUploadValidatorTest {

    @Test
    void validate_acceptsJpegWithMatchingMagicBytes() {
        byte[] jpeg = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x00, 0x01};
        MockMultipartFile file = new MockMultipartFile(
                "file", "photo.jpg", "image/jpeg", jpeg);

        StorageUploadValidator.validate(file);
        assertEquals(".jpg", StorageUploadValidator.resolveSafeExtension(file));
    }

    @Test
    void validate_rejectsMismatchedContentType() {
        byte[] png = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
        MockMultipartFile file = new MockMultipartFile(
                "file", "photo.jpg", "image/jpeg", png);

        assertThrows(IllegalArgumentException.class, () -> StorageUploadValidator.validate(file));
    }

    @Test
    void validate_rejectsOversizedFile() {
        byte[] jpeg = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
        MockMultipartFile file = new MockMultipartFile(
                "file", "photo.jpg", "image/jpeg", jpeg) {
            @Override
            public long getSize() {
                return 6 * 1024 * 1024;
            }
        };

        assertThrows(IllegalArgumentException.class, () -> StorageUploadValidator.validate(file));
    }
}
