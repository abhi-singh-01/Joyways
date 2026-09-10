package in.joyway.controller.dev;

import in.joyway.config.MailProperties;
import in.joyway.notification.EmailTemplateService;
import in.joyway.notification.EmailTemplates;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailPreviewControllerTest {

    @Mock
    private EmailTemplateService emailTemplateService;

    private MailProperties mailProperties;
    private EmailPreviewController controller;

    @BeforeEach
    void setUp() {
        mailProperties = new MailProperties();
        mailProperties.setFrontendBaseUrl("http://localhost:3000");
        mailProperties.setFrom("hello@joyway.in");
        controller = new EmailPreviewController(emailTemplateService, mailProperties);
    }

    @Test
    void previewTemplate_returnsDisabledMessageWhenPreviewOff() {
        mailProperties.setPreviewEnabled(false);

        assertThat(controller.previewTemplate("booking-confirmed"))
                .contains("Email preview is disabled");
    }

    @Test
    void previewTemplate_rendersSampleVariablesWhenEnabled() {
        mailProperties.setPreviewEnabled(true);
        when(emailTemplateService.preview(eq("review-request"), org.mockito.ArgumentMatchers.anyMap()))
                .thenReturn("<html>preview</html>");

        assertThat(controller.previewTemplate("review-request")).isEqualTo("<html>preview</html>");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, String>> vars = ArgumentCaptor.forClass(Map.class);
        verify(emailTemplateService).preview(eq("review-request"), vars.capture());
        assertThat(vars.getValue())
                .containsEntry("customerName", "Priya Sharma")
                .containsEntry("reviewUrl", "http://localhost:3000/bookings/lookup")
                .containsEntry("continueBookingUrl", "http://localhost:3000/book/1")
                .containsEntry("availableSeats", "2")
                .containsEntry("remainingMinutes", "10");
    }

    @Test
    void listTemplates_includesWaitlistAndReviewTemplates() {
        Map<String, String> templates = controller.listTemplates().getData();

        assertThat(templates)
                .containsEntry("waitlist-seat-available", EmailTemplates.WAITLIST_SEAT_AVAILABLE)
                .containsEntry("review-request", EmailTemplates.REVIEW_REQUEST)
                .containsEntry("payment-expiry-reminder", EmailTemplates.PAYMENT_EXPIRY_REMINDER)
                .containsEntry("booking-confirmed", EmailTemplates.BOOKING_CONFIRMED);
    }
}
