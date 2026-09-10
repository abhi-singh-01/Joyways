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
import in.joyway.dto.booking.BookingDetailResponse;
import in.joyway.dto.booking.BookingReceiptDto;
import in.joyway.exception.AccessDeniedBookingException;
import in.joyway.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingQueryServiceTest {

    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private TravellerRepository travellerRepository;
    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private ReceiptService receiptService;
    @Mock
    private ReceiptPdfService receiptPdfService;
    @Mock
    private in.joyway.service.operations.CustomerTripInfoService customerTripInfoService;
    @Mock
    private in.joyway.config.BookingProperties bookingProperties;
    @Mock
    private in.joyway.domain.repository.PackageReviewRepository packageReviewRepository;

    @InjectMocks
    private BookingQueryService bookingQueryService;

    private Booking booking;

    @BeforeEach
    void setUp() {
        Customer customer = Customer.builder()
                .fullName("Alice")
                .email("alice@example.com")
                .mobile("9876543210")
                .build();
        TravelPackage pkg = TravelPackage.builder()
                .title("Nainital")
                .destination("Nainital")
                .startDate(LocalDate.of(2026, 9, 8))
                .endDate(LocalDate.of(2026, 9, 10))
                .build();
        booking = Booking.builder()
                .id(1L)
                .bookingReference("JW-2026-00001")
                .customer(customer)
                .travelPackage(pkg)
                .numberOfTravellers(2)
                .totalAmount(new BigDecimal("5000"))
                .currency("INR")
                .status(BookingStatus.COMPLETED)
                .paymentStatus(PaymentStatus.PAID)
                .pickupPoint("Kashmere Gate")
                .createdAt(Instant.parse("2026-09-01T10:00:00Z"))
                .build();
    }

    @Test
    void getBookingForCustomer_rejectsWrongEmail() {
        when(bookingRepository.findByBookingReference("JW-2026-00001"))
                .thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> bookingQueryService.getBookingForCustomer(
                "JW-2026-00001", "bob@example.com"))
                .isInstanceOf(AccessDeniedBookingException.class);
    }

    @Test
    void getBookingForCustomer_includesPickupAndReviewFlags() {
        when(bookingRepository.findByBookingReference("JW-2026-00001"))
                .thenReturn(Optional.of(booking));
        when(travellerRepository.findByBookingId(1L)).thenReturn(List.of(
                Traveller.builder().fullName("Alice").idDocumentType("AADHAAR").build()));
        Payment payment = Payment.builder()
                .razorpayOrderId("order_1")
                .razorpayPaymentId("pay_1")
                .status(PaymentRecordStatus.CAPTURED)
                .updatedAt(Instant.parse("2026-09-02T10:00:00Z"))
                .build();
        when(paymentRepository.findTopByBookingIdOrderByCreatedAtDesc(1L)).thenReturn(Optional.of(payment));
        when(packageReviewRepository.existsByBookingId(1L)).thenReturn(false);
        when(customerTripInfoService.getTripInfoForBooking(1L)).thenReturn(Optional.empty());

        BookingDetailResponse detail = bookingQueryService.getBookingForCustomer(
                "JW-2026-00001", "alice@example.com");

        assertThat(detail.pickupPoint()).isEqualTo("Kashmere Gate");
        assertThat(detail.reviewEligible()).isTrue();
        assertThat(detail.reviewSubmitted()).isFalse();
        assertThat(detail.razorpayPaymentId()).isEqualTo("pay_1");
        assertThat(detail.paymentDate()).isEqualTo(Instant.parse("2026-09-02T10:00:00Z"));
        assertThat(detail.paymentExpiresAt()).isNull();
        assertThat(detail.travellers()).hasSize(1);
    }

    @Test
    void getBookingForCustomer_includesPaymentExpiryWhilePending() {
        booking.setStatus(BookingStatus.PENDING_PAYMENT);
        booking.setPaymentStatus(PaymentStatus.PENDING);
        Instant expires = Instant.parse("2026-09-01T10:30:00Z");
        when(bookingRepository.findByBookingReference("JW-2026-00001"))
                .thenReturn(Optional.of(booking));
        when(travellerRepository.findByBookingId(1L)).thenReturn(List.of());
        when(paymentRepository.findTopByBookingIdOrderByCreatedAtDesc(1L)).thenReturn(Optional.empty());
        when(packageReviewRepository.existsByBookingId(1L)).thenReturn(true);
        when(customerTripInfoService.getTripInfoForBooking(1L)).thenReturn(Optional.empty());
        when(bookingProperties.paymentExpiresAt(booking.getCreatedAt())).thenReturn(expires);

        BookingDetailResponse detail = bookingQueryService.getBookingForCustomer(
                "JW-2026-00001", "alice@example.com");

        assertThat(detail.reviewEligible()).isFalse();
        assertThat(detail.reviewSubmitted()).isTrue();
        assertThat(detail.paymentExpiresAt()).isEqualTo(expires);
        assertThat(detail.razorpayOrderId()).isNull();
    }

    @Test
    void getReceiptForCustomer_buildsReceiptFromLatestPayment() {
        when(bookingRepository.findByBookingReference("JW-2026-00001"))
                .thenReturn(Optional.of(booking));
        Payment payment = Payment.builder().razorpayPaymentId("pay_1").build();
        when(paymentRepository.findTopByBookingIdOrderByCreatedAtDesc(1L)).thenReturn(Optional.of(payment));
        BookingReceiptDto receipt = BookingReceiptDto.builder().title("Receipt").build();
        when(receiptService.buildReceipt(booking, payment)).thenReturn(receipt);

        assertThat(bookingQueryService.getReceiptForCustomer("JW-2026-00001", "alice@example.com"))
                .isSameAs(receipt);
    }

    @Test
    void getReceiptPdfForCustomer_rejectsUnpaidBookings() {
        booking.setPaymentStatus(PaymentStatus.PENDING);
        when(bookingRepository.findByBookingReference("JW-2026-00001"))
                .thenReturn(Optional.of(booking));

        assertThatThrownBy(() ->
                bookingQueryService.getReceiptPdfForCustomer("JW-2026-00001", "alice@example.com"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("after payment");
    }

    @Test
    void getReceiptPdfForCustomer_rendersPaidReceipt() {
        when(bookingRepository.findByBookingReference("JW-2026-00001"))
                .thenReturn(Optional.of(booking));
        when(paymentRepository.findTopByBookingIdOrderByCreatedAtDesc(1L)).thenReturn(Optional.empty());
        BookingReceiptDto receipt = BookingReceiptDto.builder().title("Receipt").build();
        when(receiptService.buildReceipt(booking, null)).thenReturn(receipt);
        when(receiptPdfService.render(receipt)).thenReturn(new byte[]{1, 2, 3});

        assertThat(bookingQueryService.getReceiptPdfForCustomer("JW-2026-00001", "alice@example.com"))
                .containsExactly(1, 2, 3);
    }

    @Test
    void findBookingWithAccessCheck_rejectsUnknownReference() {
        when(bookingRepository.findByBookingReference("JW-2026-00099")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookingQueryService.findBookingWithAccessCheck(
                "JW-2026-00099", "alice@example.com"))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
