package in.joyway.service;

import in.joyway.domain.entity.Booking;
import in.joyway.domain.entity.Payment;
import in.joyway.domain.enums.PaymentRecordStatus;
import in.joyway.domain.enums.PaymentStatus;
import in.joyway.dto.booking.BookingReceiptDto;
import org.springframework.stereotype.Service;

@Service
public class ReceiptService {

    public BookingReceiptDto buildReceipt(Booking booking, Payment payment) {
        return BookingReceiptDto.builder()
                .brandName("JOYWAY")
                .title("Travel Booking Receipt")
                .bookingReference(booking.getBookingReference())
                .customerName(booking.getCustomer().getFullName())
                .packageTitle(booking.getTravelPackage().getTitle())
                .destination(booking.getTravelPackage().getDestination())
                .travelDate(booking.getTravelPackage().getStartDate())
                .endDate(booking.getTravelPackage().getEndDate())
                .numberOfTravellers(booking.getNumberOfTravellers())
                .packageAmount(booking.getTotalAmount())
                .currency(booking.getCurrency())
                .paymentStatus(booking.getPaymentStatus().name())
                .razorpayPaymentId(payment != null ? payment.getRazorpayPaymentId() : null)
                .bookingDate(booking.getCreatedAt())
                .paymentDate(payment != null && payment.getStatus() == PaymentRecordStatus.CAPTURED
                        ? payment.getUpdatedAt()
                        : null)
                .build();
    }
}
