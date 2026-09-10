package in.joyway.service;

import in.joyway.domain.entity.Booking;
import in.joyway.domain.entity.Customer;
import in.joyway.domain.entity.Payment;
import in.joyway.domain.entity.Traveller;
import in.joyway.domain.entity.TravelPackage;
import in.joyway.domain.enums.BookingStatus;
import in.joyway.domain.enums.PaymentRecordStatus;
import in.joyway.domain.enums.PaymentStatus;
import in.joyway.domain.repository.BookingRepository;
import in.joyway.domain.repository.PaymentRepository;
import in.joyway.domain.repository.TravellerRepository;
import in.joyway.dto.admin.AdminBookingDetailDto;
import in.joyway.dto.admin.AdminBookingSummaryDto;
import in.joyway.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminBookingServiceTest {

    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private TravellerRepository travellerRepository;

    @InjectMocks
    private AdminBookingService adminBookingService;

    @Test
    void listBookings_clampsPagingAndMapsRows() {
        Booking booking = sampleBooking();
        when(bookingRepository.search(eq(""), eq(null), eq(null), eq(""), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(booking)));
        when(paymentRepository.findByBookingIdIn(List.of(8L))).thenReturn(List.of());

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        List<AdminBookingSummaryDto> content = adminBookingService
                .listBookings("  ", null, null, null, -1, 500)
                .content();

        verify(bookingRepository).search(eq(""), eq(null), eq(null), eq(""), pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getPageNumber()).isZero();
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(100);
        assertThat(content)
                .extracting(AdminBookingSummaryDto::bookingReference)
                .containsExactly("JW-2026-00018");
        assertThat(content.getFirst().customerEmail()).isEqualTo("priya@example.com");
    }

    @Test
    void listBookings_parsesFilters() {
        when(bookingRepository.search(
                eq("priya"),
                eq(BookingStatus.CONFIRMED),
                eq(PaymentStatus.PAID),
                eq("Nainital"),
                any(Pageable.class)
        )).thenReturn(new PageImpl<>(List.of()));

        assertThat(adminBookingService.listBookings(
                "priya", "confirmed", "paid", "Nainital", 0, 20).content()).isEmpty();
        verify(paymentRepository, never()).findByBookingIdIn(any());
    }

    @Test
    void listBookings_usesLatestPaymentPerBooking() {
        Booking booking = sampleBooking();
        when(bookingRepository.search(eq(""), eq(null), eq(null), eq(""), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(booking)));
        Payment older = Payment.builder()
                .booking(booking)
                .razorpayOrderId("order_old")
                .razorpayPaymentId("pay_old")
                .createdAt(Instant.parse("2026-09-09T10:00:00Z"))
                .build();
        Payment newer = Payment.builder()
                .booking(booking)
                .razorpayOrderId("order_new")
                .razorpayPaymentId("pay_new")
                .createdAt(Instant.parse("2026-09-09T11:00:00Z"))
                .build();
        when(paymentRepository.findByBookingIdIn(List.of(8L))).thenReturn(List.of(newer, older));

        AdminBookingSummaryDto row = adminBookingService.listBookings(null, null, null, null, 0, 20)
                .content()
                .getFirst();

        assertThat(row.razorpayOrderId()).isEqualTo("order_new");
        assertThat(row.razorpayPaymentId()).isEqualTo("pay_new");
    }

    @Test
    void listBookings_rejectsInvalidStatus() {
        assertThatThrownBy(() -> adminBookingService.listBookings(null, "NOPE", null, null, 0, 20))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid booking status.");
        assertThatThrownBy(() -> adminBookingService.listBookings(null, null, "NOPE", null, 0, 20))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid payment status.");
    }

    @Test
    void getBooking_mapsPickupAndPaymentDetail() {
        Booking booking = sampleBooking();
        booking.getTravelPackage().setStartDate(LocalDate.of(2026, 10, 1));
        booking.getTravelPackage().setEndDate(LocalDate.of(2026, 10, 3));
        booking.getCustomer().setMobile("9876543210");
        booking.setPickupPoint("Kashmere Gate");
        when(bookingRepository.findByIdWithDetails(8L)).thenReturn(Optional.of(booking));
        Payment payment = Payment.builder()
                .booking(booking)
                .razorpayOrderId("order_1")
                .razorpayPaymentId("pay_1")
                .razorpayRefundId("rfnd_1")
                .status(PaymentRecordStatus.CAPTURED)
                .refundedAmount(BigDecimal.ZERO)
                .signatureVerified(true)
                .updatedAt(Instant.parse("2026-09-09T13:00:00Z"))
                .build();
        when(paymentRepository.findTopByBookingIdOrderByCreatedAtDesc(8L)).thenReturn(Optional.of(payment));
        when(travellerRepository.findByBookingId(8L)).thenReturn(List.of(
                Traveller.builder().fullName("Priya Sharma").idDocumentType("AADHAAR").build()));

        AdminBookingDetailDto detail = adminBookingService.getBooking(8L);

        assertThat(detail.pickupPoint()).isEqualTo("Kashmere Gate");
        assertThat(detail.razorpayPaymentId()).isEqualTo("pay_1");
        assertThat(detail.signatureVerified()).isTrue();
        assertThat(detail.paymentDate()).isEqualTo(Instant.parse("2026-09-09T13:00:00Z"));
        assertThat(detail.travellers()).hasSize(1);
    }

    @Test
    void getBooking_rejectsUnknownId() {
        when(bookingRepository.findByIdWithDetails(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminBookingService.getBooking(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    private static Booking sampleBooking() {
        return Booking.builder()
                .id(8L)
                .bookingReference("JW-2026-00018")
                .customer(Customer.builder()
                        .fullName("Priya Sharma")
                        .email("priya@example.com")
                        .build())
                .travelPackage(TravelPackage.builder()
                        .title("Nainital Weekend")
                        .destination("Nainital")
                        .build())
                .numberOfTravellers(2)
                .totalAmount(new BigDecimal("12000.00"))
                .currency("INR")
                .status(BookingStatus.CONFIRMED)
                .paymentStatus(PaymentStatus.PAID)
                .createdAt(Instant.parse("2026-09-09T12:00:00Z"))
                .build();
    }
}
