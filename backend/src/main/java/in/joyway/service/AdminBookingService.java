package in.joyway.service;

import in.joyway.domain.entity.Booking;
import in.joyway.domain.entity.Payment;
import in.joyway.domain.entity.Traveller;
import in.joyway.domain.enums.BookingStatus;
import in.joyway.domain.enums.PaymentRecordStatus;
import in.joyway.domain.enums.PaymentStatus;
import in.joyway.domain.repository.BookingRepository;
import in.joyway.domain.repository.PaymentRepository;
import in.joyway.domain.repository.TravellerRepository;
import in.joyway.dto.PageResponse;
import in.joyway.dto.admin.AdminBookingDetailDto;
import in.joyway.dto.admin.AdminBookingSummaryDto;
import in.joyway.dto.booking.TravellerSummaryDto;
import in.joyway.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminBookingService {

    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;
    private final TravellerRepository travellerRepository;

    public PageResponse<AdminBookingSummaryDto> listBookings(
            String query,
            String bookingStatus,
            String paymentStatus,
            String destination,
            int page,
            int size
    ) {
        Page<Booking> result = bookingRepository.search(
                blankToEmpty(query),
                parseBookingStatus(bookingStatus),
                parsePaymentStatus(paymentStatus),
                blankToEmpty(destination),
                pageable(page, size)
        );
        Map<Long, Payment> latestPayments = latestPaymentsByBookingId(result.getContent());
        List<AdminBookingSummaryDto> content = result.getContent().stream()
                .map(booking -> toSummary(booking, latestPayments.get(booking.getId())))
                .toList();
        return PageResponse.of(new PageImpl<>(content, result.getPageable(), result.getTotalElements()));
    }

    public AdminBookingDetailDto getBooking(Long bookingId) {
        Booking booking = bookingRepository.findByIdWithDetails(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found."));
        return toDetail(booking);
    }

    private Map<Long, Payment> latestPaymentsByBookingId(List<Booking> bookings) {
        List<Long> bookingIds = bookings.stream().map(Booking::getId).toList();
        Map<Long, Payment> latest = new LinkedHashMap<>();
        if (bookingIds.isEmpty()) {
            return latest;
        }
        for (Payment payment : paymentRepository.findByBookingIdIn(bookingIds)) {
            latest.putIfAbsent(payment.getBooking().getId(), payment);
        }
        return latest;
    }

    private AdminBookingSummaryDto toSummary(Booking booking, Payment payment) {
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
                .pickupPoint(booking.getPickupPoint())
                .createdByAdmin(booking.isCreatedByAdmin())
                .adminNotes(booking.getAdminNotes())
                .offlinePaymentReference(booking.getOfflinePaymentReference())
                .build();
    }

    private static boolean isCapturedOrRefunded(PaymentRecordStatus status) {
        return status == PaymentRecordStatus.CAPTURED
                || status == PaymentRecordStatus.PARTIALLY_REFUNDED
                || status == PaymentRecordStatus.REFUNDED;
    }

    private static String blankToEmpty(String value) {
        return value == null || value.isBlank() ? "" : value.trim();
    }

    private static BookingStatus parseBookingStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        try {
            return BookingStatus.valueOf(status.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Invalid booking status.", ex);
        }
    }

    private static PaymentStatus parsePaymentStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        try {
            return PaymentStatus.valueOf(status.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Invalid payment status.", ex);
        }
    }

    private static Pageable pageable(int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.clamp(size, 1, 100);
        return PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"));
    }
}
