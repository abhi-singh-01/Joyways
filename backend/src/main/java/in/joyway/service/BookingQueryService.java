package in.joyway.service;

import in.joyway.config.BookingProperties;
import in.joyway.domain.entity.Booking;
import in.joyway.domain.entity.Payment;
import in.joyway.domain.entity.Traveller;
import in.joyway.domain.enums.BookingStatus;
import in.joyway.domain.enums.PaymentRecordStatus;
import in.joyway.domain.repository.BookingRepository;
import in.joyway.domain.repository.PackageReviewRepository;
import in.joyway.domain.repository.PaymentRepository;
import in.joyway.domain.repository.TravellerRepository;
import in.joyway.dto.booking.BookingDetailResponse;
import in.joyway.dto.booking.BookingReceiptDto;
import in.joyway.dto.booking.TravellerSummaryDto;
import in.joyway.exception.AccessDeniedBookingException;
import in.joyway.exception.ResourceNotFoundException;
import in.joyway.service.operations.CustomerTripInfoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BookingQueryService {

    private final BookingRepository bookingRepository;
    private final TravellerRepository travellerRepository;
    private final PaymentRepository paymentRepository;
    private final ReceiptService receiptService;
    private final ReceiptPdfService receiptPdfService;
    private final CustomerTripInfoService customerTripInfoService;
    private final BookingProperties bookingProperties;
    private final PackageReviewRepository packageReviewRepository;

    public BookingDetailResponse getBookingForCustomer(String bookingReference, String customerEmail) {
        Booking booking = findBookingWithAccessCheck(bookingReference, customerEmail);
        return toDetailResponse(booking);
    }

    public BookingReceiptDto getReceiptForCustomer(String bookingReference, String customerEmail) {
        Booking booking = findBookingWithAccessCheck(bookingReference, customerEmail);
        Payment payment = paymentRepository.findTopByBookingIdOrderByCreatedAtDesc(booking.getId()).orElse(null);
        return receiptService.buildReceipt(booking, payment);
    }

    public byte[] getReceiptPdfForCustomer(String bookingReference, String customerEmail) {
        Booking booking = findBookingWithAccessCheck(bookingReference, customerEmail);
        if (booking.getPaymentStatus() != in.joyway.domain.enums.PaymentStatus.PAID
                && booking.getPaymentStatus() != in.joyway.domain.enums.PaymentStatus.REFUNDED
                && booking.getPaymentStatus() != in.joyway.domain.enums.PaymentStatus.PARTIALLY_REFUNDED) {
            throw new IllegalArgumentException("A PDF receipt is available after payment.");
        }
        return receiptPdfService.render(getReceiptForCustomer(bookingReference, customerEmail));
    }

    public Booking findBookingWithAccessCheck(String bookingReference, String customerEmail) {
        Booking booking = bookingRepository.findByBookingReference(bookingReference)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found."));

        String normalizedEmail = normalizeEmail(customerEmail);
        if (!booking.getCustomer().getEmail().equalsIgnoreCase(normalizedEmail)) {
            throw new AccessDeniedBookingException("You do not have access to this booking.");
        }
        return booking;
    }

    BookingDetailResponse toDetailResponse(Booking booking) {
        List<Traveller> travellers = travellerRepository.findByBookingId(booking.getId());
        Payment payment = paymentRepository.findTopByBookingIdOrderByCreatedAtDesc(booking.getId()).orElse(null);

        return BookingDetailResponse.builder()
                .bookingReference(booking.getBookingReference())
                .bookingId(booking.getId())
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
                .status(booking.getStatus().name())
                .paymentStatus(booking.getPaymentStatus().name())
                .razorpayOrderId(payment != null ? payment.getRazorpayOrderId() : null)
                .razorpayPaymentId(payment != null ? payment.getRazorpayPaymentId() : null)
                .paymentDate(payment != null && payment.getStatus() == PaymentRecordStatus.CAPTURED
                        ? payment.getUpdatedAt() : null)
                .createdAt(booking.getCreatedAt())
                .paymentExpiresAt(paymentExpiresAt(booking))
                .pickupPoint(booking.getPickupPoint())
                .reviewEligible(booking.getStatus() == BookingStatus.COMPLETED)
                .reviewSubmitted(packageReviewRepository.existsByBookingId(booking.getId()))
                .travellers(travellers.stream()
                        .map(t -> TravellerSummaryDto.builder()
                                .fullName(t.getFullName())
                                .idDocumentType(t.getIdDocumentType())
                                .build())
                        .toList())
                .tripInfo(customerTripInfoService.getTripInfoForBooking(booking.getId()).orElse(null))
                .build();
    }

    private Instant paymentExpiresAt(Booking booking) {
        if (booking.getStatus() != BookingStatus.PENDING_PAYMENT || booking.isCreatedByAdmin()) {
            return null;
        }
        return bookingProperties.paymentExpiresAt(booking.getCreatedAt());
    }

    static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
