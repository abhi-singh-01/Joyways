package in.joyway.service;

import in.joyway.domain.entity.TravelPackage;
import in.joyway.domain.enums.PackageStatus;
import in.joyway.domain.repository.TravelPackageRepository;
import in.joyway.dto.admin.UpdatePackageRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AdminPackageServiceTest {

    @Autowired
    private AdminPackageService packageService;

    @Autowired
    private TravelPackageRepository packageRepository;

    @Test
    void updatePackage_recomputesAvailableSeatsWhenClientSendsStaleAvailability() {
        TravelPackage pkg = packageRepository.save(TravelPackage.builder()
                .title("Haridwar Spiritual Journey")
                .slug("haridwar-update-seats")
                .destination("Haridwar")
                .departureCity("Delhi")
                .startDate(LocalDate.now().plusDays(20))
                .endDate(LocalDate.now().plusDays(21))
                .duration("2 Days / 1 Night")
                .pricePerPerson(new BigDecimal("3499"))
                .totalSeats(20)
                .availableSeats(20)
                .status(PackageStatus.PUBLISHED)
                .build());

        UpdatePackageRequest request = new UpdatePackageRequest();
        request.setTitle(pkg.getTitle());
        request.setTotalSeats(15);
        request.setAvailableSeats(20);

        var updated = packageService.updatePackage(pkg.getId(), request);

        assertThat(updated.totalSeats()).isEqualTo(15);
        assertThat(updated.availableSeats()).isEqualTo(15);
    }

    @Test
    void updatePackage_rejectsTotalSeatsBelowBooked() {
        TravelPackage pkg = packageRepository.save(TravelPackage.builder()
                .title("Haridwar Booked")
                .slug("haridwar-booked-seats")
                .destination("Haridwar")
                .departureCity("Delhi")
                .startDate(LocalDate.now().plusDays(20))
                .endDate(LocalDate.now().plusDays(21))
                .duration("2 Days / 1 Night")
                .pricePerPerson(new BigDecimal("3499"))
                .totalSeats(20)
                .availableSeats(12)
                .status(PackageStatus.PUBLISHED)
                .build());

        UpdatePackageRequest request = new UpdatePackageRequest();
        request.setTotalSeats(5);

        assertThatThrownBy(() -> packageService.updatePackage(pkg.getId(), request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already booked seats");
    }
}
