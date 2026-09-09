package in.joyway.notification;

import in.joyway.config.MailProperties;
import in.joyway.domain.entity.Booking;
import in.joyway.domain.entity.Payment;
import in.joyway.domain.enums.NotificationType;
import in.joyway.domain.enums.PaymentStatus;
import in.joyway.service.ReceiptService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class BookingNotificationService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH);
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Kolkata");
    private static final String ENTITY_BOOKING = "BOOKING";
    private static final String VAR_PAYMENT_STATUS_LABEL = "paymentStatusLabel";
    private static final String VAR_BOOKING_STATUS_LABEL = "bookingStatusLabel";
    private static final String VAR_CONTINUE_PAYMENT_URL = "continuePaymentUrl";
    private static final String VAR_PAYMENT_ID = "paymentId";

    private final EmailService emailService;
    private final NotificationService notificationService;
    private final MailProperties mailProperties;
    private final ReceiptService receiptService;

    public void onWelcome(String customerName, String customerEmail) {
        Map<String, String> vars = baseVars();
        vars.put("customerName", customerName);
        vars.put("customerEmail", customerEmail);
        vars.put("loginUrl", mailProperties.getFrontendBaseUrl() + "/bookings/lookup");
        emailService.sendTemplateEmailAsync(
                EmailTemplates.WELCOME,
                customerEmail,
                "Welcome to JOYWAY",
                vars,
                "CUSTOMER",
                null
        );
    }

    public void onBookingCreated(Booking booking) {
        Map<String, String> vars = bookingVars(booking);
        vars.put(VAR_PAYMENT_STATUS_LABEL, booking.getPaymentStatus().name().replace('_', ' '));
        vars.put(VAR_BOOKING_STATUS_LABEL, booking.getStatus().name().replace('_', ' '));
        vars.put("paymentPendingNote", booking.getPaymentStatus() == PaymentStatus.PENDING
                ? "Payment is pending. Please complete payment to confirm your seats."
                : "");
        vars.put(VAR_CONTINUE_PAYMENT_URL, lookupUrl(booking));

        emailService.sendTemplateEmailAsync(
                EmailTemplates.BOOKING_CREATED,
                booking.getCustomer().getEmail(),
                "JOYWAY Booking Created — " + booking.getBookingReference(),
                vars,
                ENTITY_BOOKING,
                booking.getId()
        );

        notificationService.notifyAdmins(
                NotificationType.NEW_BOOKING,
                "New booking received",
                booking.getBookingReference() + " — " + booking.getTravelPackage().getTitle(),
                ENTITY_BOOKING,
                booking.getId()
        );
        notificationService.notifyAdmins(
                NotificationType.NEW_CUSTOMER,
                "New customer booking",
                booking.getCustomer().getFullName() + " (" + booking.getCustomer().getEmail() + ")",
                ENTITY_BOOKING,
                booking.getId()
        );

        emailService.sendTemplateEmailAsync(
                EmailTemplates.ADMIN_NEW_BOOKING,
                mailProperties.getAdminNotificationEmail(),
                "New JOYWAY Booking — " + booking.getBookingReference(),
                vars,
                ENTITY_BOOKING,
                booking.getId()
        );
    }

    public void onPaymentSuccess(Booking booking, Payment payment) {
        Map<String, String> vars = bookingVars(booking);
        vars.put(VAR_PAYMENT_ID, payment.getRazorpayPaymentId() != null ? payment.getRazorpayPaymentId() : "—");
        vars.put("paymentDate", payment.getUpdatedAt() != null
                ? DATE_FMT.format(payment.getUpdatedAt().atZone(BUSINESS_ZONE)) : "—");
        vars.put("amountPaid", booking.getTotalAmount().toPlainString());

        emailService.sendTemplateEmailAsync(
                EmailTemplates.PAYMENT_SUCCESS,
                booking.getCustomer().getEmail(),
                "Payment Successful — JOYWAY " + booking.getBookingReference(),
                vars,
                ENTITY_BOOKING,
                booking.getId()
        );

        emailService.sendTemplateEmailAsync(
                EmailTemplates.BOOKING_CONFIRMED,
                booking.getCustomer().getEmail(),
                "Booking Confirmed — JOYWAY " + booking.getBookingReference(),
                confirmedVars(booking),
                ENTITY_BOOKING,
                booking.getId()
        );

        var receipt = receiptService.buildReceipt(booking, payment);
        Map<String, String> receiptVars = bookingVars(booking);
        receiptVars.put("receiptTitle", receipt.title());
        receiptVars.put(VAR_PAYMENT_ID, receipt.razorpayPaymentId() != null ? receipt.razorpayPaymentId() : "—");
        emailService.sendTemplateEmailAsync(
                EmailTemplates.BOOKING_RECEIPT,
                booking.getCustomer().getEmail(),
                "Your JOYWAY Booking Receipt — " + booking.getBookingReference(),
                receiptVars,
                ENTITY_BOOKING,
                booking.getId()
        );

        notificationService.notifyAdmins(
                NotificationType.PAYMENT_SUCCESS,
                "Payment received",
                booking.getBookingReference() + " — " + booking.getTravelPackage().getTitle(),
                ENTITY_BOOKING,
                booking.getId()
        );

        Map<String, String> adminVars = bookingVars(booking);
        adminVars.put(VAR_PAYMENT_ID, payment.getRazorpayPaymentId() != null ? payment.getRazorpayPaymentId() : "—");
        emailService.sendTemplateEmailAsync(
                EmailTemplates.ADMIN_PAYMENT_SUCCESS,
                mailProperties.getAdminNotificationEmail(),
                "Payment Received — " + booking.getBookingReference(),
                adminVars,
                ENTITY_BOOKING,
                booking.getId()
        );
    }

    /**
     * Raised when Razorpay captured money for a booking that is no longer active
     * (cancelled or expired). The booking must not be confirmed, so the team is
     * alerted to process a refund through the normal refund workflow.
     */
    public void onPaymentReceivedForInactiveBooking(Booking booking) {
        notificationService.notifyAdmins(
                NotificationType.PAYMENT_SUCCESS,
                "Refund review required",
                "Payment received for " + booking.getBookingReference()
                        + " which is " + booking.getStatus().name().replace('_', ' ')
                        + ". Open the booking in admin and issue a Razorpay refund.",
                ENTITY_BOOKING,
                booking.getId()
        );

        Map<String, String> vars = bookingVars(booking);
        emailService.sendTemplateEmailAsync(
                EmailTemplates.ADMIN_PAYMENT_SUCCESS,
                mailProperties.getAdminNotificationEmail(),
                "Refund Review Required — " + booking.getBookingReference(),
                vars,
                ENTITY_BOOKING,
                booking.getId()
        );
    }

    public void onRefundProcessed(Booking booking, Payment payment, java.math.BigDecimal refundAmount) {
        Map<String, String> vars = bookingVars(booking);
        vars.put("refundAmount", refundAmount != null ? refundAmount.toPlainString() : booking.getTotalAmount().toPlainString());
        vars.put("refundId", payment.getRazorpayRefundId() != null ? payment.getRazorpayRefundId() : "—");
        vars.put(VAR_PAYMENT_STATUS_LABEL, booking.getPaymentStatus().name().replace('_', ' '));
        vars.put(VAR_BOOKING_STATUS_LABEL, booking.getStatus().name().replace('_', ' '));

        emailService.sendTemplateEmailAsync(
                EmailTemplates.BOOKING_REFUNDED,
                booking.getCustomer().getEmail(),
                "Refund processed — JOYWAY " + booking.getBookingReference(),
                vars,
                ENTITY_BOOKING,
                booking.getId()
        );

        notificationService.notifyAdmins(
                NotificationType.REFUND_PROCESSED,
                "Refund processed",
                booking.getBookingReference() + " — ₹" + vars.get("refundAmount"),
                ENTITY_BOOKING,
                booking.getId()
        );

        emailService.sendTemplateEmailAsync(
                EmailTemplates.ADMIN_BOOKING_REFUNDED,
                mailProperties.getAdminNotificationEmail(),
                "Refund processed — " + booking.getBookingReference(),
                vars,
                ENTITY_BOOKING,
                booking.getId()
        );
    }

    public void onPaymentFailed(Booking booking) {
        Map<String, String> vars = bookingVars(booking);
        vars.put(VAR_CONTINUE_PAYMENT_URL, lookupUrl(booking));
        vars.put(VAR_PAYMENT_STATUS_LABEL, "FAILED");

        emailService.sendTemplateEmailAsync(
                EmailTemplates.PAYMENT_FAILED,
                booking.getCustomer().getEmail(),
                "Payment Failed — JOYWAY " + booking.getBookingReference(),
                vars,
                ENTITY_BOOKING,
                booking.getId()
        );

        notificationService.notifyAdmins(
                NotificationType.PAYMENT_FAILED,
                "Payment failed",
                booking.getBookingReference() + " — " + booking.getTravelPackage().getTitle(),
                ENTITY_BOOKING,
                booking.getId()
        );

        emailService.sendTemplateEmailAsync(
                EmailTemplates.ADMIN_PAYMENT_FAILED,
                mailProperties.getAdminNotificationEmail(),
                "Payment Failed — " + booking.getBookingReference(),
                vars,
                ENTITY_BOOKING,
                booking.getId()
        );
    }

    public void onBookingCancelled(Booking booking) {
        Map<String, String> vars = bookingVars(booking);
        vars.put("cancellationDate", DATE_FMT.format(LocalDate.now(BUSINESS_ZONE)));
        vars.put("refundNote", "If a refund applies, our team will process it separately. Contact "
                + mailProperties.getSupportFrom() + " for refund assistance.");

        emailService.sendTemplateEmailAsync(
                EmailTemplates.BOOKING_CANCELLED,
                booking.getCustomer().getEmail(),
                "Booking Cancelled — JOYWAY " + booking.getBookingReference(),
                vars,
                ENTITY_BOOKING,
                booking.getId()
        );

        notificationService.notifyAdmins(
                NotificationType.BOOKING_CANCELLED,
                "Booking cancelled",
                booking.getBookingReference() + " — " + booking.getTravelPackage().getTitle(),
                ENTITY_BOOKING,
                booking.getId()
        );

        emailService.sendTemplateEmailAsync(
                EmailTemplates.ADMIN_BOOKING_CANCELLED,
                mailProperties.getAdminNotificationEmail(),
                "Booking Cancelled — " + booking.getBookingReference(),
                vars,
                ENTITY_BOOKING,
                booking.getId()
        );
    }

    public void onTripReminder(Booking booking, int daysBefore) {
        Map<String, String> vars = bookingVars(booking);
        vars.put("daysBefore", String.valueOf(daysBefore));
        vars.put("reminderMessage", daysBefore <= 1
                ? "Your JOYWAY trip is tomorrow."
                : "Your JOYWAY trip is coming up in " + daysBefore + " days.");

        String template = daysBefore <= 1 ? EmailTemplates.TRIP_REMINDER_1 : EmailTemplates.TRIP_REMINDER_7;
        emailService.sendTemplateEmailAsync(
                template,
                booking.getCustomer().getEmail(),
                "Your JOYWAY trip is coming up — " + booking.getBookingReference(),
                vars,
                ENTITY_BOOKING,
                booking.getId()
        );
    }

    public Map<String, String> buildBookingCreatedVars(Booking booking) {
        Map<String, String> vars = bookingVars(booking);
        vars.put(VAR_PAYMENT_STATUS_LABEL, booking.getPaymentStatus().name().replace('_', ' '));
        vars.put(VAR_BOOKING_STATUS_LABEL, booking.getStatus().name().replace('_', ' '));
        vars.put("paymentPendingNote", booking.getPaymentStatus() == PaymentStatus.PENDING
                ? "Payment is pending. Please complete payment to confirm your seats."
                : "");
        vars.put(VAR_CONTINUE_PAYMENT_URL, lookupUrl(booking));
        return vars;
    }

    public Map<String, String> buildPaymentSuccessVars(Booking booking, Payment payment) {
        Map<String, String> vars = bookingVars(booking);
        vars.put(VAR_PAYMENT_ID, payment != null && payment.getRazorpayPaymentId() != null ? payment.getRazorpayPaymentId() : "—");
        vars.put("paymentDate", payment != null && payment.getUpdatedAt() != null
                ? DATE_FMT.format(payment.getUpdatedAt().atZone(BUSINESS_ZONE)) : "—");
        vars.put("amountPaid", booking.getTotalAmount().toPlainString());
        return vars;
    }

    public Map<String, String> buildPaymentFailedVars(Booking booking) {
        Map<String, String> vars = bookingVars(booking);
        vars.put(VAR_CONTINUE_PAYMENT_URL, lookupUrl(booking));
        vars.put(VAR_PAYMENT_STATUS_LABEL, "FAILED");
        return vars;
    }

    public Map<String, String> buildBookingConfirmedVars(Booking booking) {
        return confirmedVars(booking);
    }

    public Map<String, String> buildBookingCancelledVars(Booking booking) {
        Map<String, String> vars = bookingVars(booking);
        vars.put("cancellationDate", DATE_FMT.format(LocalDate.now(BUSINESS_ZONE)));
        vars.put("refundNote", "If a refund applies, our team will process it separately. Contact "
                + mailProperties.getSupportFrom() + " for refund assistance.");
        return vars;
    }

    public Map<String, String> buildReceiptVars(Booking booking, Payment payment) {
        var receipt = receiptService.buildReceipt(booking, payment);
        Map<String, String> receiptVars = bookingVars(booking);
        receiptVars.put("receiptTitle", receipt.title());
        receiptVars.put(VAR_PAYMENT_ID, receipt.razorpayPaymentId() != null ? receipt.razorpayPaymentId() : "—");
        return receiptVars;
    }

    public Map<String, String> buildTripReminderVars(Booking booking, int daysBefore) {
        Map<String, String> vars = bookingVars(booking);
        vars.put("daysBefore", String.valueOf(daysBefore));
        vars.put("reminderMessage", daysBefore <= 1
                ? "Your JOYWAY trip is tomorrow."
                : "Your JOYWAY trip is coming up in " + daysBefore + " days.");
        return vars;
    }

    private Map<String, String> confirmedVars(Booking booking) {
        Map<String, String> vars = bookingVars(booking);
        vars.put("pickup", booking.getTravelPackage().getPickupPoints() != null
                ? booking.getTravelPackage().getPickupPoints() : "Details will be shared before departure.");
        vars.put("duration", booking.getTravelPackage().getDuration());
        vars.put("bookingUrl", lookupUrl(booking));
        return vars;
    }

    private Map<String, String> bookingVars(Booking booking) {
        Map<String, String> vars = baseVars();
        vars.put("customerName", booking.getCustomer().getFullName());
        vars.put("bookingReference", booking.getBookingReference());
        vars.put("packageTitle", booking.getTravelPackage().getTitle());
        vars.put("destination", booking.getTravelPackage().getDestination());
        vars.put("travelDate", DATE_FMT.format(booking.getTravelPackage().getStartDate()));
        vars.put("endDate", DATE_FMT.format(booking.getTravelPackage().getEndDate()));
        vars.put("travellerCount", String.valueOf(booking.getNumberOfTravellers()));
        vars.put("amount", booking.getTotalAmount().toPlainString());
        vars.put("currency", booking.getCurrency());
        vars.put(VAR_BOOKING_STATUS_LABEL, booking.getStatus().name().replace('_', ' '));
        vars.put(VAR_PAYMENT_STATUS_LABEL, booking.getPaymentStatus().name().replace('_', ' '));
        return vars;
    }

    private Map<String, String> baseVars() {
        Map<String, String> vars = new HashMap<>();
        vars.put("supportEmail", mailProperties.getSupportFrom());
        vars.put("bookingsEmail", mailProperties.getBookingsFrom());
        vars.put("generalEmail", mailProperties.getGeneralFrom());
        vars.put("frontendUrl", mailProperties.getFrontendBaseUrl());
        return vars;
    }

    private String lookupUrl(Booking booking) {
        return mailProperties.getFrontendBaseUrl()
                + "/bookings/lookup?reference="
                + booking.getBookingReference();
    }
}
