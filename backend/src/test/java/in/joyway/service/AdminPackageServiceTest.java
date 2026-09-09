package in.joyway.service;

import in.joyway.domain.entity.TravelPackage;
import in.joyway.domain.enums.PackageStatus;
import in.joyway.domain.repository.TravelPackageRepository;
import in.joyway.dto.admin.CreatePackageRequest;
import in.joyway.dto.admin.UpdatePackageRequest;
import in.joyway.dto.admin.UpdatePackageStatusRequest;
import in.joyway.exception.ResourceNotFoundException;
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
        Long packageId = pkg.getId();

        assertThatThrownBy(() -> packageService.updatePackage(packageId, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already booked seats");
    }

    @Test
    void updatePackage_rejectsAvailableSeatsAboveTotal() {
        TravelPackage pkg = packageRepository.save(TravelPackage.builder()
                .title("Seat Cap")
                .slug("seat-cap-pkg")
                .destination("Manali")
                .departureCity("Delhi")
                .startDate(LocalDate.now().plusDays(20))
                .endDate(LocalDate.now().plusDays(21))
                .duration("2 Days")
                .pricePerPerson(new BigDecimal("3499"))
                .totalSeats(10)
                .availableSeats(10)
                .status(PackageStatus.PUBLISHED)
                .build());

        UpdatePackageRequest request = new UpdatePackageRequest();
        request.setAvailableSeats(99);
        Long packageId = pkg.getId();
        assertThatThrownBy(() -> packageService.updatePackage(packageId, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot exceed total seats");
    }

    @Test
    void createAndReadPackage_usesTitleWhenSlugBlank() {
        CreatePackageRequest request = new CreatePackageRequest();
        request.setTitle("Manali Snow Week");
        request.setSlug("  ");
        request.setDestination("Manali");
        request.setDepartureCity("Delhi");
        request.setStartDate(LocalDate.now().plusDays(30));
        request.setEndDate(LocalDate.now().plusDays(33));
        request.setDuration("4 Days / 3 Nights");
        request.setPricePerPerson(new BigDecimal("9999"));
        request.setTotalSeats(12);

        var created = packageService.createPackage(request);

        assertThat(created.slug()).isEqualTo("manali-snow-week");
        assertThat(packageService.getPackageById(created.id()).title()).isEqualTo("Manali Snow Week");
        assertThat(packageService.listAllPackages()).extracting(dto -> dto.id()).contains(created.id());
    }

    @Test
    void createPackage_usesProvidedSlug() {
        CreatePackageRequest request = new CreatePackageRequest();
        request.setTitle("Shimla Hills");
        request.setSlug("custom-shimla-slug");
        request.setDestination("Shimla");
        request.setDepartureCity("Delhi");
        request.setStartDate(LocalDate.now().plusDays(40));
        request.setEndDate(LocalDate.now().plusDays(42));
        request.setDuration("3 Days");
        request.setPricePerPerson(new BigDecimal("7999"));
        request.setTotalSeats(8);

        assertThat(packageService.createPackage(request).slug()).isEqualTo("custom-shimla-slug");
    }

    @Test
    void updateStatusAndDeletePackage() {
        TravelPackage pkg = packageRepository.save(TravelPackage.builder()
                .title("Status Flow")
                .slug("status-flow-pkg")
                .destination("Rishikesh")
                .departureCity("Delhi")
                .startDate(LocalDate.now().plusDays(40))
                .endDate(LocalDate.now().plusDays(42))
                .duration("3 Days")
                .pricePerPerson(new BigDecimal("4999"))
                .totalSeats(10)
                .availableSeats(10)
                .status(PackageStatus.DRAFT)
                .build());

        UpdatePackageStatusRequest publish = new UpdatePackageStatusRequest();
        publish.setStatus(PackageStatus.PUBLISHED);
        assertThat(packageService.updateStatus(pkg.getId(), publish).status()).isEqualTo("PUBLISHED");

        UpdatePackageRequest seats = new UpdatePackageRequest();
        seats.setAvailableSeats(8);
        assertThat(packageService.updatePackage(pkg.getId(), seats).availableSeats()).isEqualTo(8);

        Long packageId = pkg.getId();
        packageService.deletePackage(packageId);
        assertThatThrownBy(() -> packageService.getPackageById(packageId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateStatus_rejectsCancelledPackage() {
        TravelPackage pkg = packageRepository.save(TravelPackage.builder()
                .title("Cancelled Flow")
                .slug("cancelled-flow-pkg")
                .destination("Rishikesh")
                .departureCity("Delhi")
                .startDate(LocalDate.now().plusDays(40))
                .endDate(LocalDate.now().plusDays(42))
                .duration("3 Days")
                .pricePerPerson(new BigDecimal("4999"))
                .totalSeats(10)
                .availableSeats(10)
                .status(PackageStatus.CANCELLED)
                .build());

        UpdatePackageStatusRequest publish = new UpdatePackageStatusRequest();
        publish.setStatus(PackageStatus.PUBLISHED);
        Long packageId = pkg.getId();
        assertThatThrownBy(() -> packageService.updateStatus(packageId, publish))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cancelled packages");
    }
}
