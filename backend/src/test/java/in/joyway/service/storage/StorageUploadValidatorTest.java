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
    void validate_rejectsOversizedTravellerDocument() {
        byte[] jpeg = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
        MockMultipartFile file = new MockMultipartFile(
                "file", "photo.jpg", "image/jpeg", jpeg) {
            @Override
            public long getSize() {
                return 3 * 1024 * 1024;
            }
        };

        assertThrows(IllegalArgumentException.class, () -> StorageUploadValidator.validate(file));
    }

    @Test
    void validateTravellerDocument_rejectsPngAndWebp() {
        byte[] png = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
        assertThrows(IllegalArgumentException.class, () -> StorageUploadValidator.validateTravellerDocument(
                new MockMultipartFile("file", "id.png", "image/png", png)));

        byte[] webp = new byte[]{
                0x52, 0x49, 0x46, 0x46, 0, 0, 0, 0, 0x57, 0x45, 0x42, 0x50
        };
        assertThrows(IllegalArgumentException.class, () -> StorageUploadValidator.validateTravellerDocument(
                new MockMultipartFile("file", "id.webp", "image/webp", webp)));
    }

    @Test
    void validatePackageImage_acceptsJpegAndWebp() {
        byte[] jpeg = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x00};
        StorageUploadValidator.validatePackageImage(new MockMultipartFile(
                "file", "hero.jpg", "image/jpeg", jpeg));

        byte[] webp = new byte[]{
                0x52, 0x49, 0x46, 0x46, 0, 0, 0, 0, 0x57, 0x45, 0x42, 0x50
        };
        StorageUploadValidator.validatePackageImage(new MockMultipartFile(
                "file", "hero.webp", "image/webp", webp));
    }

    @Test
    void validatePackageImage_acceptsImageJpgContentType() {
        byte[] jpeg = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x00};
        StorageUploadValidator.validatePackageImage(new MockMultipartFile(
                "file", "hero.jpg", "image/jpg", jpeg));
    }

    @Test
    void validatePackageImage_rejectsPngAndPdf() {
        byte[] png = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
        assertThrows(IllegalArgumentException.class, () -> StorageUploadValidator.validatePackageImage(
                new MockMultipartFile("file", "hero.png", "image/png", png)));

        byte[] pdf = new byte[]{0x25, 0x50, 0x44, 0x46, 0x2D};
        assertThrows(IllegalArgumentException.class, () -> StorageUploadValidator.validatePackageImage(
                new MockMultipartFile("file", "hero.pdf", "application/pdf", pdf)));
    }
}
