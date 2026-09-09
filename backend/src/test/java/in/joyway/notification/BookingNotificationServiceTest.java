package in.joyway.notification;

import in.joyway.config.MailProperties;
import in.joyway.domain.entity.Booking;
import in.joyway.domain.entity.Customer;
import in.joyway.domain.entity.Payment;
import in.joyway.domain.entity.TravelPackage;
import in.joyway.domain.enums.BookingStatus;
import in.joyway.domain.enums.PaymentStatus;
import in.joyway.dto.booking.BookingReceiptDto;
import in.joyway.service.ReceiptService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingNotificationServiceTest {

    @Mock
    private EmailService emailService;
    @Mock
    private NotificationService notificationService;
    @Mock
    private ReceiptService receiptService;

    private BookingNotificationService service;
    private Booking booking;
    private Payment payment;

    @BeforeEach
    void setUp() {
        MailProperties mail = new MailProperties();
        mail.setFrontendBaseUrl("http://localhost:3000");
        mail.setAdminNotificationEmail("admin@joyway.in");
        mail.setSupportFrom("support@joyway.in");
        service = new BookingNotificationService(emailService, notificationService, mail, receiptService);

        Customer customer = Customer.builder()
                .id(1L)
                .fullName("Test Customer")
                .email("customer@example.com")
                .build();
        TravelPackage travelPackage = TravelPackage.builder()
                .id(10L)
                .title("Nainital Escape")
                .destination("Nainital")
                .duration("3 Days")
                .pickupPoints("Kashmere Gate")
                .startDate(LocalDate.of(2026, 10, 1))
                .endDate(LocalDate.of(2026, 10, 3))
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
        payment = Payment.builder()
                .id(1L)
                .booking(booking)
                .razorpayPaymentId("pay_1")
                .razorpayRefundId("rfnd_1")
                .amount(new BigDecimal("10000.00"))
                .updatedAt(Instant.parse("2026-09-09T12:00:00Z"))
                .build();
    }

    @Test
    void onBookingCreated_includesPendingPaymentCopy() {
        service.onBookingCreated(booking);
        verify(emailService).sendTemplateEmailAsync(
                org.mockito.ArgumentMatchers.eq(EmailTemplates.BOOKING_CREATED),
                org.mockito.ArgumentMatchers.eq("customer@example.com"),
                org.mockito.ArgumentMatchers.contains("JW-2026-00001"),
                org.mockito.ArgumentMatchers.anyMap(),
                org.mockito.ArgumentMatchers.eq("BOOKING"),
                org.mockito.ArgumentMatchers.eq(100L)
        );
        Map<String, String> pending = service.buildBookingCreatedVars(booking);
        assertThat(pending.get("paymentPendingNote")).contains("Payment is pending");

        booking.setPaymentStatus(PaymentStatus.PAID);
        assertThat(service.buildBookingCreatedVars(booking)).containsEntry("paymentPendingNote", "");
    }

    @Test
    void onPaymentSuccess_andReceiptVarsHandleMissingPaymentId() {
        when(receiptService.buildReceipt(any(), any())).thenReturn(
                BookingReceiptDto.builder().title("Receipt").razorpayPaymentId(null).build());
        payment.setRazorpayPaymentId(null);
        payment.setUpdatedAt(null);

        service.onPaymentSuccess(booking, payment);

        Map<String, String> vars = service.buildPaymentSuccessVars(booking, null);
        assertThat(vars)
                .containsEntry("paymentId", "—")
                .containsEntry("paymentDate", "—");
        assertThat(service.buildReceiptVars(booking, payment)).containsEntry("paymentId", "—");
    }

    @Test
    void remainingNotificationFlowsPopulateTemplateVars() {
        service.onWelcome("Test Customer", "customer@example.com");
        service.onPaymentReceivedForInactiveBooking(booking);
        service.onRefundProcessed(booking, payment, null);
        service.onRefundProcessed(booking, payment, new BigDecimal("500.00"));
        service.onPaymentFailed(booking);
        service.onBookingCancelled(booking);
        service.onTripReminder(booking, 7);
        service.onTripReminder(booking, 1);

        assertThat(service.buildPaymentFailedVars(booking)).containsKey("continuePaymentUrl");
        assertThat(service.buildBookingConfirmedVars(booking)).containsEntry("pickup", "Kashmere Gate");
        assertThat(service.buildBookingCancelledVars(booking)).containsKey("cancellationDate");
        assertThat(service.buildTripReminderVars(booking, 1))
                .containsEntry("reminderMessage", "Your JOYWAY trip is tomorrow.");
        assertThat(service.buildTripReminderVars(booking, 7))
                .containsEntry("reminderMessage", "Your JOYWAY trip is coming up in 7 days.");
        verify(notificationService, org.mockito.Mockito.atLeastOnce())
                .notifyAdmins(any(), any(), any(), any(), any());
    }
}
