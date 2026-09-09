package in.joyway.service.operations;

import in.joyway.domain.entity.Booking;
import in.joyway.domain.entity.TravelPackage;
import in.joyway.domain.entity.Trip;
import in.joyway.domain.entity.TripBooking;
import in.joyway.domain.entity.TripVehicle;
import in.joyway.domain.entity.Vehicle;
import in.joyway.domain.repository.TripBookingRepository;
import in.joyway.domain.repository.TripRepository;
import in.joyway.domain.repository.TripVehicleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConfirmedBookingTripAssignmentServiceTest {

    @Mock
    private TripRepository tripRepository;
    @Mock
    private TripBookingRepository tripBookingRepository;
    @Mock
    private TripVehicleRepository tripVehicleRepository;

    @InjectMocks
    private ConfirmedBookingTripAssignmentService service;

    private TravelPackage travelPackage;
    private Booking booking;
    private Trip trip;

    @BeforeEach
    void setUp() {
        LocalDate startDate = LocalDate.of(2026, 10, 1);
        travelPackage = TravelPackage.builder()
                .id(1L)
                .startDate(startDate)
                .build();
        booking = Booking.builder()
                .id(10L)
                .bookingReference("JW-2026-00010")
                .travelPackage(travelPackage)
                .numberOfTravellers(2)
                .build();
        trip = Trip.builder()
                .id(9L)
                .tripReference("TR-2026-00009")
                .travelPackage(travelPackage)
                .travelDate(startDate)
                .build();
    }

    @Test
    void assign_skipsWhenBookingAlreadyAttached() {
        when(tripBookingRepository.existsByBookingId(10L)).thenReturn(true);

        service.assignIfSingleMatchingTrip(booking);

        verify(tripRepository, never()).findAssignableForPackageAndDate(any(), any(), any());
        verify(tripBookingRepository, never()).save(any());
    }

    @Test
    void assign_skipsWhenZeroOrMultipleMatchingTrips() {
        when(tripBookingRepository.existsByBookingId(10L)).thenReturn(false);
        when(tripRepository.findAssignableForPackageAndDate(eq(1L), eq(travelPackage.getStartDate()), any()))
                .thenReturn(List.of())
                .thenReturn(List.of(trip, Trip.builder().id(8L).build()));

        service.assignIfSingleMatchingTrip(booking);
        service.assignIfSingleMatchingTrip(booking);

        verify(tripBookingRepository, never()).save(any());
    }

    @Test
    void assign_attachesWhenExactlyOneMatchingTripHasCapacity() {
        when(tripBookingRepository.existsByBookingId(10L)).thenReturn(false);
        when(tripRepository.findAssignableForPackageAndDate(eq(1L), eq(travelPackage.getStartDate()), any()))
                .thenReturn(List.of(trip));
        when(tripBookingRepository.sumTravellersByTripId(9L)).thenReturn(4);
        Vehicle vehicle = Vehicle.builder().id(3L).seatingCapacity(12).build();
        when(tripVehicleRepository.findPrimaryVehicle(9L))
                .thenReturn(Optional.of(TripVehicle.builder().vehicle(vehicle).build()));

        service.assignIfSingleMatchingTrip(booking);

        ArgumentCaptor<TripBooking> captor = ArgumentCaptor.forClass(TripBooking.class);
        verify(tripBookingRepository).save(captor.capture());
        assertThat(captor.getValue().getTrip()).isEqualTo(trip);
        assertThat(captor.getValue().getBooking()).isEqualTo(booking);
    }

    @Test
    void assign_skipsWhenPrimaryVehicleWouldBeOverCapacity() {
        when(tripBookingRepository.existsByBookingId(10L)).thenReturn(false);
        when(tripRepository.findAssignableForPackageAndDate(eq(1L), eq(travelPackage.getStartDate()), any()))
                .thenReturn(List.of(trip));
        when(tripBookingRepository.sumTravellersByTripId(9L)).thenReturn(11);
        Vehicle vehicle = Vehicle.builder().id(3L).seatingCapacity(12).build();
        when(tripVehicleRepository.findPrimaryVehicle(9L))
                .thenReturn(Optional.of(TripVehicle.builder().vehicle(vehicle).build()));

        service.assignIfSingleMatchingTrip(booking);

        verify(tripBookingRepository, never()).save(any());
    }
}
