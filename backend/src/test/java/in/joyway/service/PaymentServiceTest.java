package in.joyway.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import in.joyway.config.RazorpayProperties;
import in.joyway.domain.entity.Booking;
import in.joyway.domain.entity.Customer;
import in.joyway.domain.entity.Payment;
import in.joyway.domain.entity.TravelPackage;
import in.joyway.domain.enums.BookingStatus;
import in.joyway.domain.enums.PaymentRecordStatus;
import in.joyway.domain.enums.PaymentStatus;
import in.joyway.domain.repository.BookingRepository;
import in.joyway.domain.repository.PaymentRepository;
import in.joyway.domain.repository.PaymentWebhookEventRepository;
import in.joyway.dto.payment.CreatePaymentOrderRequest;
import in.joyway.dto.payment.VerifyPaymentRequest;
import in.joyway.exception.AccessDeniedBookingException;
import in.joyway.exception.PaymentVerificationException;
import in.joyway.notification.BookingNotificationService;
import in.joyway.service.payment.RazorpayGateway;
import in.joyway.service.payment.RazorpayOrderResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private PaymentWebhookEventRepository webhookEventRepository;
    @Mock
    private BookingQueryService bookingQueryService;
    @Mock
    private RazorpayGateway razorpayGateway;
    @Mock
    private RazorpayProperties razorpayProperties;
    @Mock
    private ReceiptService receiptService;
    @Mock
    private BookingNotificationService bookingNotificationService;

    @InjectMocks
    private PaymentService paymentService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private Booking booking;
    private Customer customer;
    private TravelPackage travelPackage;

    @BeforeEach
    void setUp() {
        paymentService = new PaymentService(
                bookingRepository,
                paymentRepository,
                webhookEventRepository,
                bookingQueryService,
                razorpayGateway,
                razorpayProperties,
                receiptService,
                objectMapper,
                bookingNotificationService
        );

        customer = Customer.builder()
                .id(1L)
                .fullName("Test Customer")
                .email("customer@example.com")
                .mobile("9876543210")
                .build();

        travelPackage = TravelPackage.builder()
                .id(10L)
                .title("Nainital Escape")
                .destination("Nainital")
                .build();

        booking = Booking.builder()
                .id(100L)
                .bookingReference("JW-2026-00001")
                .customer(customer)
                .travelPackage(travelPackage)
                .numberOfTravellers(2)
                .totalAmount(new BigDecimal("10000.00"))
                .currency("INR")
                .status(BookingStatus.PENDING_PAYMENT)
                .paymentStatus(PaymentStatus.PENDING)
                .build();
    }

    @Test
    void createOrder_createsRazorpayOrderAndPaymentRecord() {
        CreatePaymentOrderRequest request = new CreatePaymentOrderRequest();
        request.setBookingReference("JW-2026-00001");
        request.setCustomerEmail("customer@example.com");

        when(bookingQueryService.findBookingWithAccessCheck("JW-2026-00001", "customer@example.com"))
                .thenReturn(booking);
        when(bookingRepository.findByBookingReferenceForUpdate("JW-2026-00001"))
                .thenReturn(Optional.of(booking));
        when(paymentRepository.findTopByBookingIdOrderByCreatedAtDesc(100L))
                .thenReturn(Optional.empty());
        when(razorpayGateway.createOrder("JW-2026-00001", new BigDecimal("10000.00"), "INR"))
                .thenReturn(RazorpayOrderResult.builder()
                        .orderId("order_test123")
                        .amount(new BigDecimal("10000.00"))
                        .currency("INR")
                        .build());
        when(razorpayProperties.getKeyId()).thenReturn("rzp_test_key");
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        var response = paymentService.createOrder(request);

        assertThat(response.razorpayOrderId()).isEqualTo("order_test123");
        assertThat(response.razorpayKeyId()).isEqualTo("rzp_test_key");
        assertThat(response.amount()).isEqualByComparingTo("10000.00");
        verify(paymentRepository).save(any(Payment.class));
    }

    @Test
    void createOrder_rejectsAlreadyPaidBooking() {
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setPaymentStatus(PaymentStatus.PAID);

        CreatePaymentOrderRequest request = new CreatePaymentOrderRequest();
        request.setBookingReference("JW-2026-00001");
        request.setCustomerEmail("customer@example.com");

        when(bookingQueryService.findBookingWithAccessCheck("JW-2026-00001", "customer@example.com"))
                .thenReturn(booking);
        when(bookingRepository.findByBookingReferenceForUpdate("JW-2026-00001"))
                .thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> paymentService.createOrder(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already paid");
    }

    @Test
    void createOrder_rejectsCancelledBooking() {
        booking.setStatus(BookingStatus.CANCELLED);

        CreatePaymentOrderRequest request = new CreatePaymentOrderRequest();
        request.setBookingReference("JW-2026-00001");
        request.setCustomerEmail("customer@example.com");

        when(bookingQueryService.findBookingWithAccessCheck("JW-2026-00001", "customer@example.com"))
                .thenReturn(booking);
        when(bookingRepository.findByBookingReferenceForUpdate("JW-2026-00001"))
                .thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> paymentService.createOrder(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not eligible");
        verify(razorpayGateway, never()).createOrder(any(), any(), any());
    }

    @Test
    void verifyPayment_rejectsCancelledBookingAndLeavesItCancelled() {
        booking.setStatus(BookingStatus.CANCELLED);

        Payment payment = Payment.builder()
                .id(1L)
                .booking(booking)
                .razorpayOrderId("order_test123")
                .amount(new BigDecimal("10000.00"))
                .currency("INR")
                .status(PaymentRecordStatus.CREATED)
                .build();

        VerifyPaymentRequest request = new VerifyPaymentRequest();
        request.setBookingReference("JW-2026-00001");
        request.setCustomerEmail("customer@example.com");
        request.setRazorpayOrderId("order_test123");
        request.setRazorpayPaymentId("pay_test456");
        request.setRazorpaySignature("valid_signature");

        when(bookingRepository.findByBookingReferenceForUpdate("JW-2026-00001"))
                .thenReturn(Optional.of(booking));
        when(bookingQueryService.findBookingWithAccessCheck("JW-2026-00001", "customer@example.com"))
                .thenReturn(booking);
        when(paymentRepository.findByRazorpayOrderId("order_test123"))
                .thenReturn(Optional.of(payment));

        assertThatThrownBy(() -> paymentService.verifyPayment(request))
                .isInstanceOf(PaymentVerificationException.class);

        // Seats were already released on cancellation, so the booking must not be resold.
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CANCELLED);
        assertThat(booking.getPaymentStatus()).isEqualTo(PaymentStatus.PENDING);
        verify(bookingRepository, never()).save(any(Booking.class));
    }

    @Test
    void webhook_capturesPaymentButDoesNotConfirmCancelledBooking() {
        booking.setStatus(BookingStatus.CANCELLED);

        Payment payment = Payment.builder()
                .id(1L)
                .booking(booking)
                .razorpayOrderId("order_test123")
                .amount(new BigDecimal("10000.00"))
                .currency("INR")
                .status(PaymentRecordStatus.CREATED)
                .build();

        String payload = """
                {"event":"payment.captured","payload":{"payment":{"entity":
                {"id":"pay_test456","order_id":"order_test123"}}}}""";

        when(razorpayGateway.verifyWebhookSignature(payload, "sig")).thenReturn(true);
        when(webhookEventRepository.existsByEventId("evt_2")).thenReturn(false);
        when(webhookEventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(paymentRepository.findByRazorpayPaymentId("pay_test456")).thenReturn(Optional.empty());
        when(paymentRepository.findByRazorpayOrderId("order_test123")).thenReturn(Optional.of(payment));
        when(bookingRepository.findByBookingReferenceForUpdate("JW-2026-00001"))
                .thenReturn(Optional.of(booking));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        paymentService.processWebhook(payload, "sig", "evt_2");

        // Money was captured, so the payment record is retained for reconciliation...
        assertThat(payment.getStatus()).isEqualTo(PaymentRecordStatus.CAPTURED);
        assertThat(payment.getRazorpayPaymentId()).isEqualTo("pay_test456");
        // ...but the cancelled booking must not be revived.
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CANCELLED);
        assertThat(booking.getPaymentStatus()).isEqualTo(PaymentStatus.PENDING);
        verify(bookingRepository, never()).save(any(Booking.class));
    }

    @Test
    void webhook_confirmsPendingBooking() {
        Payment payment = Payment.builder()
                .id(1L)
                .booking(booking)
                .razorpayOrderId("order_test123")
                .amount(new BigDecimal("10000.00"))
                .currency("INR")
                .status(PaymentRecordStatus.CREATED)
                .build();

        String payload = """
                {"event":"payment.captured","payload":{"payment":{"entity":
                {"id":"pay_test456","order_id":"order_test123"}}}}""";

        when(razorpayGateway.verifyWebhookSignature(payload, "sig")).thenReturn(true);
        when(webhookEventRepository.existsByEventId("evt_3")).thenReturn(false);
        when(webhookEventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(paymentRepository.findByRazorpayPaymentId("pay_test456")).thenReturn(Optional.empty());
        when(paymentRepository.findByRazorpayOrderId("order_test123")).thenReturn(Optional.of(payment));
        when(bookingRepository.findByBookingReferenceForUpdate("JW-2026-00001"))
                .thenReturn(Optional.of(booking));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        paymentService.processWebhook(payload, "sig", "evt_3");

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(booking.getPaymentStatus()).isEqualTo(PaymentStatus.PAID);
    }

    @Test
    void webhook_rejectsInvalidSignature() {
        when(razorpayGateway.verifyWebhookSignature("{}", "bad")).thenReturn(false);

        assertThatThrownBy(() -> paymentService.processWebhook("{}", "bad", "evt_4"))
                .isInstanceOf(PaymentVerificationException.class);

        verify(webhookEventRepository, never()).save(any());
        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    void verifyPayment_confirmsBookingOnValidSignature() {
        Payment payment = Payment.builder()
                .id(1L)
                .booking(booking)
                .razorpayOrderId("order_test123")
                .amount(new BigDecimal("10000.00"))
                .currency("INR")
                .status(PaymentRecordStatus.CREATED)
                .build();

        VerifyPaymentRequest request = new VerifyPaymentRequest();
        request.setBookingReference("JW-2026-00001");
        request.setCustomerEmail("customer@example.com");
        request.setRazorpayOrderId("order_test123");
        request.setRazorpayPaymentId("pay_test456");
        request.setRazorpaySignature("valid_signature");

        when(bookingRepository.findByBookingReferenceForUpdate("JW-2026-00001"))
                .thenReturn(Optional.of(booking));
        when(bookingQueryService.findBookingWithAccessCheck("JW-2026-00001", "customer@example.com"))
                .thenReturn(booking);
        when(paymentRepository.findByRazorpayOrderId("order_test123"))
                .thenReturn(Optional.of(payment));
        when(paymentRepository.findByRazorpayPaymentId("pay_test456"))
                .thenReturn(Optional.empty());
        when(razorpayGateway.verifyPaymentSignature("order_test123", "pay_test456", "valid_signature"))
                .thenReturn(true);
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));
        when(receiptService.buildReceipt(any(), any())).thenReturn(null);

        var response = paymentService.verifyPayment(request);

        assertThat(response.status()).isEqualTo("CONFIRMED");
        assertThat(response.paymentStatus()).isEqualTo("PAID");
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(booking.getPaymentStatus()).isEqualTo(PaymentStatus.PAID);
    }

    @Test
    void verifyPayment_rejectsInvalidSignature() {
        Payment payment = Payment.builder()
                .id(1L)
                .booking(booking)
                .razorpayOrderId("order_test123")
                .amount(new BigDecimal("10000.00"))
                .currency("INR")
                .status(PaymentRecordStatus.CREATED)
                .build();

        VerifyPaymentRequest request = new VerifyPaymentRequest();
        request.setBookingReference("JW-2026-00001");
        request.setCustomerEmail("customer@example.com");
        request.setRazorpayOrderId("order_test123");
        request.setRazorpayPaymentId("pay_test456");
        request.setRazorpaySignature("bad_signature");

        when(bookingRepository.findByBookingReferenceForUpdate("JW-2026-00001"))
                .thenReturn(Optional.of(booking));
        when(bookingQueryService.findBookingWithAccessCheck("JW-2026-00001", "customer@example.com"))
                .thenReturn(booking);
        when(paymentRepository.findByRazorpayOrderId("order_test123"))
                .thenReturn(Optional.of(payment));
        when(paymentRepository.findByRazorpayPaymentId("pay_test456"))
                .thenReturn(Optional.empty());
        when(razorpayGateway.verifyPaymentSignature("order_test123", "pay_test456", "bad_signature"))
                .thenReturn(false);
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        assertThatThrownBy(() -> paymentService.verifyPayment(request))
                .isInstanceOf(PaymentVerificationException.class);
        assertThat(booking.getPaymentStatus()).isEqualTo(PaymentStatus.FAILED);
    }

    @Test
    void processWebhook_ignoresDuplicateEventId() {
        when(razorpayGateway.verifyWebhookSignature("{}", "sig")).thenReturn(true);
        when(webhookEventRepository.existsByEventId("evt_1")).thenReturn(true);

        paymentService.processWebhook("{}", "sig", "evt_1");

        verify(webhookEventRepository, never()).save(any());
    }
}
