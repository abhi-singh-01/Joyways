package in.joyway.mapper;

import in.joyway.domain.entity.PackageImage;
import in.joyway.domain.entity.TravelPackage;
import in.joyway.domain.enums.PackageStatus;
import in.joyway.dto.package_.PackageDetailDto;
import in.joyway.dto.package_.PackageSummaryDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PackageMapperTest {

    private PackageMapper mapper;
    private TravelPackage travelPackage;

    @BeforeEach
    void setUp() {
        mapper = new PackageMapper();

        PackageImage image = PackageImage.builder()
                .id(1L)
                .imageUrl("https://example.com/image.jpg")
                .altText("Test image")
                .sortOrder(0)
                .build();

        travelPackage = TravelPackage.builder()
                .id(1L)
                .title("Test Trip (DEMO)")
                .slug("test-trip-demo")
                .destination("Nainital")
                .shortDescription("Short desc")
                .fullDescription("Full desc")
                .departureCity("Delhi")
                .pickupPoints("CP")
                .startDate(LocalDate.now().plusDays(14))
                .endDate(LocalDate.now().plusDays(16))
                .duration("3 Days / 2 Nights")
                .pricePerPerson(new BigDecimal("4999.00"))
                .totalSeats(20)
                .availableSeats(18)
                .status(PackageStatus.PUBLISHED)
                .images(List.of(image))
                .build();

        image.setTravelPackage(travelPackage);
    }

    @Test
    void toSummaryDto_mapsFieldsCorrectly() {
        PackageSummaryDto dto = mapper.toSummaryDto(travelPackage);

        assertThat(dto.title()).isEqualTo("Test Trip (DEMO)");
        assertThat(dto.destination()).isEqualTo("Nainital");
        assertThat(dto.pricePerPerson()).isEqualByComparingTo("4999.00");
        assertThat(dto.heroImageUrl()).isEqualTo("https://example.com/image.jpg");
    }

    @Test
    void toDetailDto_marksBookableWhenPublishedWithSeats() {
        PackageDetailDto dto = mapper.toDetailDto(travelPackage);

        assertThat(dto.bookable()).isTrue();
        assertThat(dto.status()).isEqualTo("PUBLISHED");
    }

    @Test
    void toDetailDto_notBookableWhenNoSeats() {
        travelPackage.setAvailableSeats(0);
        PackageDetailDto dto = mapper.toDetailDto(travelPackage);

        assertThat(dto.bookable()).isFalse();
    }

    @Test
    void toDetailDto_notBookableWhenDatePassed() {
        travelPackage.setStartDate(LocalDate.now().minusDays(1));
        PackageDetailDto dto = mapper.toDetailDto(travelPackage);

        assertThat(dto.bookable()).isFalse();
    }
}
