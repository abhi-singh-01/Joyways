package in.joyway.mapper;

import in.joyway.config.BookingProperties;
import in.joyway.domain.entity.Booking;
import in.joyway.domain.entity.Traveller;
import in.joyway.domain.enums.BookingStatus;
import in.joyway.dto.booking.BookingResponse;
import in.joyway.dto.booking.TravellerSummaryDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Component
@RequiredArgsConstructor
public class BookingMapper {

    private final BookingProperties bookingProperties;

    public BookingResponse toResponse(Booking booking, List<Traveller> travellers) {
        return BookingResponse.builder()
                .bookingReference(booking.getBookingReference())
                .bookingId(booking.getId())
                .packageTitle(booking.getTravelPackage().getTitle())
                .destination(booking.getTravelPackage().getDestination())
                .startDate(booking.getTravelPackage().getStartDate())
                .endDate(booking.getTravelPackage().getEndDate())
                .numberOfTravellers(booking.getNumberOfTravellers())
                .totalAmount(booking.getTotalAmount())
                .currency(booking.getCurrency())
                .status(booking.getStatus().name())
                .paymentStatus(booking.getPaymentStatus().name())
                .createdAt(booking.getCreatedAt())
                .paymentExpiresAt(paymentExpiresAt(booking))
                .pickupPoint(booking.getPickupPoint())
                .travellers(travellers.stream()
                        .map(t -> TravellerSummaryDto.builder()
                                .fullName(t.getFullName())
                                .idDocumentType(t.getIdDocumentType())
                                .build())
                        .toList())
                .build();
    }

    public Instant paymentExpiresAt(Booking booking) {
        if (booking.getStatus() != BookingStatus.PENDING_PAYMENT) {
            return null;
        }
        return bookingProperties.paymentExpiresAt(booking.getCreatedAt());
    }
}
