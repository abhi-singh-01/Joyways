package in.joyway.service.operations;

import in.joyway.domain.entity.Driver;
import in.joyway.domain.entity.TravelPackage;
import in.joyway.domain.entity.Trip;
import in.joyway.domain.entity.TripVehicle;
import in.joyway.domain.entity.Vehicle;
import in.joyway.domain.enums.TripStatus;
import in.joyway.domain.repository.BookingRepository;
import in.joyway.domain.repository.DriverRepository;
import in.joyway.domain.repository.TravelPackageRepository;
import in.joyway.domain.repository.TripBookingRepository;
import in.joyway.domain.repository.TripRepository;
import in.joyway.domain.repository.TripVehicleRepository;
import in.joyway.domain.repository.VehicleRepository;
import in.joyway.dto.admin.operations.TripSummaryDto;
import in.joyway.service.audit.AuditLogService;
import in.joyway.util.TripReferenceGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminTripServiceTest {

    @Mock private TripRepository tripRepository;
    @Mock private TripBookingRepository tripBookingRepository;
    @Mock private TripVehicleRepository tripVehicleRepository;
    @Mock private VehicleRepository vehicleRepository;
    @Mock private DriverRepository driverRepository;
    @Mock private BookingRepository bookingRepository;
    @Mock private TravelPackageRepository travelPackageRepository;
    @Mock private TripReferenceGenerator tripReferenceGenerator;
    @Mock private AuditLogService auditLogService;
    @Mock private OperationsNotificationService operationsNotificationService;
    @Mock private in.joyway.service.BookingService bookingService;

    @InjectMocks
    private AdminTripService adminTripService;

    @Test
    void listTrips_batchesLoadSummaries() {
        Trip trip = sampleTrip();
        when(tripRepository.search(eq(""), eq(null), eq(null), eq(null), eq(null), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(trip)));
        when(tripBookingRepository.summarizeByTripIds(List.of(4L))).thenReturn(List.of(load(4L, 6, 2)));
        TripVehicle primary = TripVehicle.builder()
                .trip(trip)
                .vehicle(Vehicle.builder()
                        .registrationNumber("DL01AB1234")
                        .seatingCapacity(12)
                        .build())
                .primary(true)
                .build();
        when(tripVehicleRepository.findPrimaryVehiclesByTripIdIn(List.of(4L))).thenReturn(List.of(primary));

        TripSummaryDto row = adminTripService.listTrips(null, null, null, null, null, 0, 20)
                .content()
                .getFirst();

        assertThat(row.bookingCount()).isEqualTo(2);
        assertThat(row.totalTravellers()).isEqualTo(6);
        assertThat(row.vehicleCapacity()).isEqualTo(12);
        assertThat(row.remainingCapacity()).isEqualTo(6);
        assertThat(row.vehicleRegistration()).isEqualTo("DL01AB1234");
        assertThat(row.driverName()).isEqualTo("Ravi");
        verify(tripBookingRepository, never()).sumTravellersByTripId(any());
        verify(tripBookingRepository, never()).findByTripId(any());
    }

    @Test
    void listTrips_skipsBatchQueriesWhenEmpty() {
        when(tripRepository.search(eq(""), eq(null), eq(null), eq(null), eq(null), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        assertThat(adminTripService.listTrips(null, null, null, null, null, 0, 20).content()).isEmpty();
        verify(tripBookingRepository, never()).summarizeByTripIds(any());
        verify(tripVehicleRepository, never()).findPrimaryVehiclesByTripIdIn(any());
    }

    private static Trip sampleTrip() {
        return Trip.builder()
                .id(4L)
                .tripReference("TR-2026-00004")
                .travelPackage(TravelPackage.builder().title("Nainital Weekend").build())
                .destination("Nainital")
                .travelDate(LocalDate.of(2026, 9, 15))
                .startAt(Instant.parse("2026-09-15T01:00:00Z"))
                .endAt(Instant.parse("2026-09-17T14:00:00Z"))
                .driver(Driver.builder().name("Ravi").build())
                .status(TripStatus.ASSIGNED)
                .build();
    }

    private static TripBookingRepository.TripLoadSummary load(long tripId, long travellers, long bookings) {
        return new TripBookingRepository.TripLoadSummary() {
            @Override
            public Long getTripId() {
                return tripId;
            }

            @Override
            public Long getTravellerCount() {
                return travellers;
            }

            @Override
            public Long getBookingCount() {
                return bookings;
            }
        };
    }
}
