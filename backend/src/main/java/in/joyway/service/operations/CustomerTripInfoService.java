package in.joyway.service.operations;

import in.joyway.config.OperationsProperties;
import in.joyway.domain.entity.Trip;
import in.joyway.domain.enums.TripStatus;
import in.joyway.domain.repository.TripBookingRepository;
import in.joyway.domain.repository.TripVehicleRepository;
import in.joyway.dto.booking.CustomerTripInfoDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CustomerTripInfoService {

    private static final Set<TripStatus> DRIVER_CONTACT_STATUSES = EnumSet.of(
            TripStatus.READY,
            TripStatus.IN_PROGRESS,
            TripStatus.COMPLETED
    );

    private final TripBookingRepository tripBookingRepository;
    private final TripVehicleRepository tripVehicleRepository;
    private final OperationsProperties operationsProperties;

    public Optional<CustomerTripInfoDto> getTripInfoForBooking(Long bookingId) {
        return tripBookingRepository.findByBookingIdWithTripDetails(bookingId)
                .map(tb -> toCustomerTripInfo(tb.getTrip()));
    }

    private CustomerTripInfoDto toCustomerTripInfo(Trip trip) {
        String vehicleType = tripVehicleRepository.findPrimaryVehicle(trip.getId())
                .map(tv -> tv.getVehicle().getVehicleType())
                .orElse(null);

        CustomerTripInfoDto.CustomerTripInfoDtoBuilder builder = CustomerTripInfoDto.builder()
                .tripReference(trip.getTripReference())
                .status(trip.getStatus().name())
                .travelDate(trip.getTravelDate())
                .startAt(trip.getStartAt())
                .endAt(trip.getEndAt())
                .pickupLocation(trip.getPickupLocation())
                .dropLocation(trip.getDropLocation())
                .vehicleType(vehicleType);

        if (operationsProperties.isExposeDriverContactToCustomers()
                && trip.getDriver() != null
                && DRIVER_CONTACT_STATUSES.contains(trip.getStatus())) {
            builder.driverContactName(trip.getDriver().getName())
                    .driverContactPhone(trip.getDriver().getPhone());
        }

        return builder.build();
    }
}
