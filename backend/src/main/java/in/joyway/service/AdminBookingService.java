package in.joyway.service;

import in.joyway.domain.entity.Booking;
import in.joyway.domain.entity.Payment;
import in.joyway.domain.entity.Traveller;
import in.joyway.domain.enums.PaymentRecordStatus;
import in.joyway.domain.repository.BookingRepository;
import in.joyway.domain.repository.PaymentRepository;
import in.joyway.domain.repository.TravellerRepository;
import in.joyway.dto.admin.AdminBookingDetailDto;
import in.joyway.dto.admin.AdminBookingSummaryDto;
import in.joyway.dto.booking.TravellerSummaryDto;
import in.joyway.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminBookingService {

    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;
    private final TravellerRepository travellerRepository;

    public List<AdminBookingSummaryDto> listBookings() {
        return bookingRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toSummary)
                .toList();
    }

    public AdminBookingDetailDto getBooking(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found."));
        return toDetail(booking);
    }

    private AdminBookingSummaryDto toSummary(Booking booking) {
        Payment payment = paymentRepository.findTopByBookingIdOrderByCreatedAtDesc(booking.getId()).orElse(null);
        return AdminBookingSummaryDto.builder()
                .bookingId(booking.getId())
                .bookingReference(booking.getBookingReference())
                .customerName(booking.getCustomer().getFullName())
                .customerEmail(booking.getCustomer().getEmail())
                .packageTitle(booking.getTravelPackage().getTitle())
                .destination(booking.getTravelPackage().getDestination())
                .numberOfTravellers(booking.getNumberOfTravellers())
                .totalAmount(booking.getTotalAmount())
                .currency(booking.getCurrency())
                .bookingStatus(booking.getStatus().name())
                .paymentStatus(booking.getPaymentStatus().name())
                .razorpayOrderId(payment != null ? payment.getRazorpayOrderId() : null)
                .razorpayPaymentId(payment != null ? payment.getRazorpayPaymentId() : null)
                .createdAt(booking.getCreatedAt())
                .build();
    }

    private AdminBookingDetailDto toDetail(Booking booking) {
        Payment payment = paymentRepository.findTopByBookingIdOrderByCreatedAtDesc(booking.getId()).orElse(null);
        List<Traveller> travellers = travellerRepository.findByBookingId(booking.getId());

        return AdminBookingDetailDto.builder()
                .bookingId(booking.getId())
                .bookingReference(booking.getBookingReference())
                .customerName(booking.getCustomer().getFullName())
                .customerEmail(booking.getCustomer().getEmail())
                .customerMobile(booking.getCustomer().getMobile())
                .packageTitle(booking.getTravelPackage().getTitle())
                .destination(booking.getTravelPackage().getDestination())
                .startDate(booking.getTravelPackage().getStartDate())
                .endDate(booking.getTravelPackage().getEndDate())
                .numberOfTravellers(booking.getNumberOfTravellers())
                .totalAmount(booking.getTotalAmount())
                .currency(booking.getCurrency())
                .bookingStatus(booking.getStatus().name())
                .paymentStatus(booking.getPaymentStatus().name())
                .razorpayOrderId(payment != null ? payment.getRazorpayOrderId() : null)
                .razorpayPaymentId(payment != null ? payment.getRazorpayPaymentId() : null)
                .razorpayRefundId(payment != null ? payment.getRazorpayRefundId() : null)
                .paymentRecordStatus(payment != null ? payment.getStatus().name() : null)
                .refundedAmount(payment != null && payment.getRefundedAmount() != null
                        ? payment.getRefundedAmount() : java.math.BigDecimal.ZERO)
                .signatureVerified(payment != null && Boolean.TRUE.equals(payment.getSignatureVerified()))
                .paymentDate(payment != null && isCapturedOrRefunded(payment.getStatus())
                        ? payment.getUpdatedAt() : null)
                .createdAt(booking.getCreatedAt())
                .travellers(travellers.stream()
                        .map(t -> TravellerSummaryDto.builder()
                                .fullName(t.getFullName())
                                .idDocumentType(t.getIdDocumentType())
                                .build())
                        .toList())
                .build();
    }

    private static boolean isCapturedOrRefunded(PaymentRecordStatus status) {
        return status == PaymentRecordStatus.CAPTURED
                || status == PaymentRecordStatus.PARTIALLY_REFUNDED
                || status == PaymentRecordStatus.REFUNDED;
    }
}
