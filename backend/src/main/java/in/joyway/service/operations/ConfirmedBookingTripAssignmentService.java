package in.joyway.service.operations;

import in.joyway.domain.entity.Booking;
import in.joyway.domain.entity.Trip;
import in.joyway.domain.entity.TripBooking;
import in.joyway.domain.entity.TripVehicle;
import in.joyway.domain.enums.TripStatus;
import in.joyway.domain.repository.TripBookingRepository;
import in.joyway.domain.repository.TripRepository;
import in.joyway.domain.repository.TripVehicleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConfirmedBookingTripAssignmentService {

    private static final Set<TripStatus> ASSIGNABLE_STATUSES = EnumSet.of(
            TripStatus.PLANNED,
            TripStatus.ASSIGNED,
            TripStatus.READY
    );

    private final TripRepository tripRepository;
    private final TripBookingRepository tripBookingRepository;
    private final TripVehicleRepository tripVehicleRepository;

    /**
     * When exactly one operational trip exists for the package and travel date,
     * attach the confirmed booking so ops does not have to assign it by hand.
     */
    @Transactional
    public void assignIfSingleMatchingTrip(Booking booking) {
        if (booking.getId() == null || tripBookingRepository.existsByBookingId(booking.getId())) {
            return;
        }
        if (booking.getTravelPackage() == null || booking.getTravelPackage().getStartDate() == null) {
            return;
        }

        List<Trip> matches = tripRepository.findAssignableForPackageAndDate(
                booking.getTravelPackage().getId(),
                booking.getTravelPackage().getStartDate(),
                ASSIGNABLE_STATUSES
        );
        if (matches.size() != 1) {
            return;
        }

        Trip trip = matches.get(0);
        int currentTravellers = tripBookingRepository.sumTravellersByTripId(trip.getId());
        int newTotal = currentTravellers + booking.getNumberOfTravellers();
        Optional<TripVehicle> primaryVehicle = tripVehicleRepository.findPrimaryVehicle(trip.getId());
        if (primaryVehicle.isPresent()
                && newTotal > primaryVehicle.get().getVehicle().getSeatingCapacity()) {
            log.info("event=trip.auto_assign.skipped reason=capacity tripId={} bookingId={}",
                    trip.getId(), booking.getId());
            return;
        }

        TripBooking tripBooking = TripBooking.builder()
                .trip(trip)
                .booking(booking)
                .build();
        tripBookingRepository.save(tripBooking);
        log.info("event=trip.auto_assigned tripReference={} bookingReference={}",
                trip.getTripReference(), booking.getBookingReference());
    }
}
