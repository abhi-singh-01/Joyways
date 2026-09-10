package in.joyway.controller.dev;

import in.joyway.config.MailProperties;
import in.joyway.dto.ApiResponse;
import in.joyway.notification.EmailTemplateService;
import in.joyway.notification.EmailTemplates;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/dev/email")
@Profile("dev")
@RequiredArgsConstructor
public class EmailPreviewController {

    private final EmailTemplateService emailTemplateService;
    private final MailProperties mailProperties;

    @GetMapping(value = "/preview/{template}", produces = "text/html")
    public String previewTemplate(@PathVariable String template) {
        if (!mailProperties.isPreviewEnabled()) {
            return "<p>Email preview is disabled. Set MAIL_PREVIEW_ENABLED=true</p>";
        }
        return emailTemplateService.preview(template, sampleVariables());
    }

    @GetMapping("/templates")
    public ApiResponse<Map<String, String>> listTemplates() {
        Map<String, String> templates = new HashMap<>();
        templates.put("welcome", EmailTemplates.WELCOME);
        templates.put("booking-created", EmailTemplates.BOOKING_CREATED);
        templates.put("payment-success", EmailTemplates.PAYMENT_SUCCESS);
        templates.put("payment-failed", EmailTemplates.PAYMENT_FAILED);
        templates.put("booking-confirmed", EmailTemplates.BOOKING_CONFIRMED);
        templates.put("booking-cancelled", EmailTemplates.BOOKING_CANCELLED);
        templates.put("booking-refunded", EmailTemplates.BOOKING_REFUNDED);
        templates.put("payment-expiry-reminder", EmailTemplates.PAYMENT_EXPIRY_REMINDER);
        templates.put("waitlist-seat-available", EmailTemplates.WAITLIST_SEAT_AVAILABLE);
        templates.put("review-request", EmailTemplates.REVIEW_REQUEST);
        templates.put("trip-reminder-7", EmailTemplates.TRIP_REMINDER_7);
        templates.put("trip-reminder-1", EmailTemplates.TRIP_REMINDER_1);
        return ApiResponse.success(templates);
    }

    private static final String BOOKING_LOOKUP_PATH = "/bookings/lookup";
    private static final String SAMPLE_AMOUNT = "12000.00";

    private Map<String, String> sampleVariables() {
        String lookupUrl = mailProperties.getFrontendBaseUrl() + BOOKING_LOOKUP_PATH;
        Map<String, String> vars = new HashMap<>();
        vars.put("customerName", "Priya Sharma");
        vars.put("customerEmail", "priya@example.com");
        vars.put("loginUrl", lookupUrl);
        vars.put("bookingReference", "JW-2026-00018");
        vars.put("packageTitle", "Nainital Weekend Escape");
        vars.put("destination", "Nainital");
        vars.put("travelDate", "15 Sep 2026");
        vars.put("endDate", "17 Sep 2026");
        vars.put("travellerCount", "2");
        vars.put("amount", SAMPLE_AMOUNT);
        vars.put("amountPaid", SAMPLE_AMOUNT);
        vars.put("currency", "INR");
        vars.put("paymentStatusLabel", "PAID");
        vars.put("bookingStatusLabel", "CONFIRMED");
        vars.put("paymentPendingNote", "Payment is pending. Please complete payment to confirm your seats.");
        vars.put("continuePaymentUrl", lookupUrl);
        vars.put("remainingMinutes", "10");
        vars.put("whatsappUrl", "https://wa.me/917050971956?text=Hi%20JOYWAY");
        vars.put("paymentId", "pay_sample123");
        vars.put("paymentDate", "27 Aug 2026");
        vars.put("duration", "2 Nights / 3 Days");
        vars.put("pickup", "Kashmere Gate, Delhi");
        vars.put("bookingUrl", lookupUrl);
        vars.put("cancellationDate", "27 Aug 2026");
        vars.put("refundNote", "If a refund applies, our team will process it separately and confirm by email.");
        vars.put("refundAmount", SAMPLE_AMOUNT);
        vars.put("refundId", "rfnd_sample123");
        vars.put("reminderMessage", "Your JOYWAY trip is coming up in 7 days.");
        vars.put("daysBefore", "7");
        vars.put("receiptTitle", "Travel Booking Receipt");
        vars.put("frontendUrl", mailProperties.getFrontendBaseUrl());
        vars.put("supportEmail", mailProperties.getFrom());
        vars.put("availableSeats", "2");
        vars.put("continueBookingUrl", mailProperties.getFrontendBaseUrl() + "/book/1");
        vars.put("tripUrl", mailProperties.getFrontendBaseUrl() + "/trips/nainital-weekend");
        vars.put("reviewUrl", lookupUrl);
        return vars;
    }
}
