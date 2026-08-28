package in.joyway.mapper;

import in.joyway.domain.entity.Booking;
import in.joyway.domain.entity.Traveller;
import in.joyway.dto.booking.BookingResponse;
import in.joyway.dto.booking.TravellerSummaryDto;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class BookingMapper {

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
                .travellers(travellers.stream()
                        .map(t -> TravellerSummaryDto.builder()
                                .fullName(t.getFullName())
                                .idDocumentType(t.getIdDocumentType())
                                .build())
                        .toList())
                .build();
    }
}
