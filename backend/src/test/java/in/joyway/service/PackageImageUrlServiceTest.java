package in.joyway.service;

import in.joyway.domain.entity.PackageImage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class PackageImageUrlServiceTest {

    private PackageImageUrlService service;

    @BeforeEach
    void setUp() {
        service = new PackageImageUrlService();
        ReflectionTestUtils.setField(service, "publicApiBaseUrl", "https://api.joyway.in");
        ReflectionTestUtils.setField(service, "packageImageCdnBaseUrl", "");
    }

    @Test
    void resolvePublicUrl_buildsApiFileUrlFromStorageKey() {
        PackageImage image = PackageImage.builder()
                .privateStorageKey("package-images/abc123.jpg")
                .build();

        assertThat(service.resolvePublicUrl(image))
                .isEqualTo("https://api.joyway.in/api/files/package-images/abc123.jpg");
    }

    @Test
    void resolvePublicUrl_returnsNullWithoutStorageKey() {
        PackageImage image = PackageImage.builder()
                .imageUrl("https://legacy.example/image.jpg")
                .build();

        assertThat(service.resolvePublicUrl(image)).isNull();
    }

    @Test
    void resolvePublicUrl_returnsNullForNonPackageImageKeys() {
        PackageImage image = PackageImage.builder()
                .privateStorageKey("traveller-documents/secret.pdf")
                .build();

        assertThat(service.resolvePublicUrl(image)).isNull();
    }

    @Test
    void resolvePublicUrl_returnsNullForPathTraversalKey() {
        PackageImage image = PackageImage.builder()
                .privateStorageKey("package-images/../secret.jpg")
                .build();

        assertThat(service.resolvePublicUrl(image)).isNull();
    }

    @Test
    void resolvePublicUrl_prefersCdnBaseWhenConfigured() {
        ReflectionTestUtils.setField(service, "packageImageCdnBaseUrl", "https://joyway-images-prod.azurefd.net/");
        PackageImage image = PackageImage.builder()
                .privateStorageKey("package-images/abc123.jpg")
                .build();

        assertThat(service.resolvePublicUrl(image))
                .isEqualTo("https://joyway-images-prod.azurefd.net/api/files/package-images/abc123.jpg");
    }
}
