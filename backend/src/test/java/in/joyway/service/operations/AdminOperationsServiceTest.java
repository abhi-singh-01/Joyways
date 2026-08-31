package in.joyway.service.operations;

import in.joyway.domain.entity.*;
import in.joyway.domain.enums.*;
import in.joyway.domain.repository.*;
import in.joyway.dto.admin.operations.*;
import in.joyway.exception.OperationConflictException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AdminOperationsServiceTest {

    private static final ZoneId IST = ZoneId.of("Asia/Kolkata");

    @Autowired private AdminVehicleService vehicleService;
    @Autowired private AdminDriverService driverService;
    @Autowired private AdminTripService tripService;
    @Autowired private VehicleRepository vehicleRepository;
    @Autowired private DriverRepository driverRepository;
    @Autowired private TripRepository tripRepository;
    @Autowired private TravelPackageRepository packageRepository;
    @Autowired private BookingRepository bookingRepository;
    @Autowired private CustomerRepository customerRepository;

    private TravelPackage travelPackage;
    private Booking confirmedBooking;

    @BeforeEach
    void setUp() {
        travelPackage = packageRepository.save(TravelPackage.builder()
                .title("Delhi to Nainital")
                .slug("delhi-nainital")
                .destination("Nainital")
                .departureCity("Delhi")
                .startDate(LocalDate.now(IST).plusDays(30))
                .endDate(LocalDate.now(IST).plusDays(32))
                .duration("3D/2N")
                .pricePerPerson(new BigDecimal("4999"))
                .totalSeats(40)
                .availableSeats(40)
                .status(PackageStatus.PUBLISHED)
                .build());

        Customer customer = customerRepository.save(Customer.builder()
                .fullName("Test Customer")
                .email("customer@example.com")
                .mobile("9876543210")
                .build());

        confirmedBooking = bookingRepository.save(Booking.builder()
                .bookingReference("JW-2026-TEST1")
                .travelPackage(travelPackage)
                .customer(customer)
                .numberOfTravellers(4)
                .totalAmount(new BigDecimal("19996"))
                .status(BookingStatus.CONFIRMED)
                .paymentStatus(PaymentStatus.PAID)
                .build());
    }

    @Test
    void vehicleCrud_andDuplicateRegistrationRejected() {
        CreateVehicleRequest request = new CreateVehicleRequest();
        request.setRegistrationNumber("dl01ab1234");
        request.setVehicleType("Tempo Traveller");
        request.setSeatingCapacity(17);
        request.setOwnershipType("OWNED");

        VehicleDetailDto created = vehicleService.createVehicle(request);
        assertThat(created.registrationNumber()).isEqualTo("DL01AB1234");
        assertThat(created.status()).isEqualTo("AVAILABLE");

        assertThatThrownBy(() -> vehicleService.createVehicle(request))
                .isInstanceOf(OperationConflictException.class);
    }

    @Test
    void driverCrud_expiredLicenseBlocksAssignment() {
        CreateDriverRequest request = new CreateDriverRequest();
        request.setName("Raj Kumar");
        request.setPhone("9999888877");
        request.setLicenseNumber("DL-1234567890");
        request.setLicenseExpiryDate(LocalDate.now(IST).minusDays(1));

        DriverDetailDto driver = driverService.createDriver(request);
        assertThat(driver.licenseStatus()).isEqualTo("LICENSE EXPIRED");

        TripDetailDto trip = createTrip();
        VehicleDetailDto vehicle = createVehicle("DL02CD5678", 17);

        tripService.assignVehicle(trip.id(), new AssignVehicleRequest() {{
            setVehicleId(vehicle.id());
        }});

        AssignDriverRequest assignDriver = new AssignDriverRequest();
        assignDriver.setDriverId(driver.id());

        assertThatThrownBy(() -> tripService.assignDriver(trip.id(), assignDriver))
                .isInstanceOf(OperationConflictException.class)
                .hasMessageContaining("license has expired");
    }

    @Test
    void tripAssignment_capacityAndConflictValidation() {
        VehicleDetailDto vehicle = createVehicle("DL03EF9012", 17);
        DriverDetailDto driver = createDriver("Suresh", LocalDate.now(IST).plusYears(1));
        TripDetailDto tripA = createTripAt(LocalDate.now(IST).plusDays(10), 8, 20);
        TripDetailDto tripB = createTripAt(LocalDate.now(IST).plusDays(10), 10, 18);

        assignBooking(tripA.id(), confirmedBooking.getId());
        tripService.assignVehicle(tripA.id(), vehicleRequest(vehicle.id()));
        tripService.assignDriver(tripA.id(), driverRequest(driver.id()));

        assertThat(tripService.getTrip(tripA.id()).status()).isEqualTo("ASSIGNED");

        assertThatThrownBy(() -> tripService.assignVehicle(tripB.id(), vehicleRequest(vehicle.id())))
                .isInstanceOf(OperationConflictException.class);

        assertThatThrownBy(() -> tripService.assignDriver(tripB.id(), driverRequest(driver.id())))
                .isInstanceOf(OperationConflictException.class);

        Booking mediumBooking = bookingRepository.save(Booking.builder()
                .bookingReference("JW-2026-TEST2")
                .travelPackage(travelPackage)
                .customer(confirmedBooking.getCustomer())
                .numberOfTravellers(13)
                .totalAmount(new BigDecimal("64987"))
                .status(BookingStatus.CONFIRMED)
                .paymentStatus(PaymentStatus.PAID)
                .build());

        assignBooking(tripA.id(), mediumBooking.getId());
        assertThat(tripService.getTrip(tripA.id()).totalTravellers()).isEqualTo(17);
        assertThat(tripService.getTrip(tripA.id()).remainingCapacity()).isEqualTo(0);

        Booking overflow = bookingRepository.save(Booking.builder()
                .bookingReference("JW-2026-TEST3")
                .travelPackage(travelPackage)
                .customer(confirmedBooking.getCustomer())
                .numberOfTravellers(1)
                .totalAmount(new BigDecimal("4999"))
                .status(BookingStatus.CONFIRMED)
                .paymentStatus(PaymentStatus.PAID)
                .build());

        AssignBookingToTripRequest addBooking = new AssignBookingToTripRequest();
        addBooking.setBookingId(overflow.getId());
        assertThatThrownBy(() -> tripService.addBooking(tripA.id(), addBooking))
                .isInstanceOf(OperationConflictException.class);
    }

    @Test
    void tripStatusTransitions_releaseResourcesOnComplete() {
        VehicleDetailDto vehicle = createVehicle("DL04GH3456", 17);
        DriverDetailDto driver = createDriver("Amit", LocalDate.now(IST).plusYears(1));
        TripDetailDto trip = createTrip();

        assignBooking(trip.id(), confirmedBooking.getId());
        tripService.assignVehicle(trip.id(), vehicleRequest(vehicle.id()));
        tripService.assignDriver(trip.id(), driverRequest(driver.id()));

        UpdateTripStatusRequest ready = new UpdateTripStatusRequest();
        ready.setStatus("READY");
        tripService.updateStatus(trip.id(), ready);

        UpdateTripStatusRequest inProgress = new UpdateTripStatusRequest();
        inProgress.setStatus("IN_PROGRESS");
        tripService.updateStatus(trip.id(), inProgress);

        assertThat(vehicleRepository.findById(vehicle.id()).orElseThrow().getStatus())
                .isEqualTo(VehicleStatus.ON_TRIP);

        UpdateTripStatusRequest completed = new UpdateTripStatusRequest();
        completed.setStatus("COMPLETED");
        tripService.updateStatus(trip.id(), completed);

        assertThat(vehicleRepository.findById(vehicle.id()).orElseThrow().getStatus())
                .isEqualTo(VehicleStatus.AVAILABLE);
        assertThat(driverRepository.findById(driver.id()).orElseThrow().getStatus())
                .isEqualTo(DriverStatus.AVAILABLE);
        assertThat(tripRepository.findById(trip.id()).orElseThrow().getStatus())
                .isEqualTo(TripStatus.COMPLETED);
    }

    @Test
    void tripStatusTransition_invalidTransitionRejected() {
        TripDetailDto trip = createTrip();
        UpdateTripStatusRequest completed = new UpdateTripStatusRequest();
        completed.setStatus("COMPLETED");

        assertThatThrownBy(() -> tripService.updateStatus(trip.id(), completed))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void listEndpoints_acceptEmptySearchWithoutError() {
        createVehicle("DL05JK7890", 12);
        createDriver("Neha", LocalDate.now(IST).plusYears(1));
        createTrip();

        assertThat(vehicleService.listVehicles(null, null, null, null, 0, 20).content()).isNotEmpty();
        assertThat(driverService.listDrivers("", null, null, 0, 20).content()).isNotEmpty();
        assertThat(tripService.listTrips(null, null, null, null, null, 0, 20).content()).isNotEmpty();
        assertThat(vehicleService.listVehicles("DL05", null, null, null, 0, 20).content()).isNotEmpty();
    }

    private TripDetailDto createTrip() {
        return createTripAt(LocalDate.now(IST).plusDays(15), 8, 20);
    }

    private TripDetailDto createTripAt(LocalDate travelDate, int startHour, int endHour) {
        CreateTripRequest request = new CreateTripRequest();
        request.setPackageId(travelPackage.getId());
        request.setTravelDate(travelDate);
        request.setStartAt(travelDate.atTime(startHour, 0).atZone(IST).toInstant());
        request.setEndAt(travelDate.atTime(endHour, 0).atZone(IST).toInstant());
        request.setPickupLocation("Kashmere Gate, Delhi");
        request.setDropLocation("Nainital Mall Road");
        return tripService.createTrip(request);
    }

    private VehicleDetailDto createVehicle(String registration, int capacity) {
        CreateVehicleRequest request = new CreateVehicleRequest();
        request.setRegistrationNumber(registration);
        request.setVehicleType("Tempo Traveller");
        request.setSeatingCapacity(capacity);
        request.setOwnershipType("RENTED");
        return vehicleService.createVehicle(request);
    }

    private DriverDetailDto createDriver(String name, LocalDate licenseExpiry) {
        CreateDriverRequest request = new CreateDriverRequest();
        request.setName(name);
        request.setPhone("8888777766");
        request.setLicenseNumber("DL-" + name.toUpperCase());
        request.setLicenseExpiryDate(licenseExpiry);
        return driverService.createDriver(request);
    }

    private void assignBooking(Long tripId, Long bookingId) {
        AssignBookingToTripRequest request = new AssignBookingToTripRequest();
        request.setBookingId(bookingId);
        tripService.addBooking(tripId, request);
    }

    private AssignVehicleRequest vehicleRequest(Long vehicleId) {
        AssignVehicleRequest request = new AssignVehicleRequest();
        request.setVehicleId(vehicleId);
        return request;
    }

    private AssignDriverRequest driverRequest(Long driverId) {
        AssignDriverRequest request = new AssignDriverRequest();
        request.setDriverId(driverId);
        return request;
    }
}
