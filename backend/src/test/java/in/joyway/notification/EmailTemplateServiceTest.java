package in.joyway.notification;

import in.joyway.config.MailProperties;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class EmailTemplateServiceTest {

    @Test
    void render_mergesVariablesIntoLayout() {
        MailProperties mailProperties = new MailProperties();
        mailProperties.setFrom("hello@joyway.in");
        mailProperties.setSupportFrom("support@joyway.in");
        mailProperties.setBookingsFrom("bookings@joyway.in");
        EmailTemplateService service = new EmailTemplateService(mailProperties);

        Map<String, String> vars = new HashMap<>();
        vars.put("customerName", "Asha");
        vars.put("bookingReference", "JW-2026-00001");
        vars.put("packageTitle", "Nainital Escape");
        vars.put("destination", "Nainital");
        vars.put("travelDate", "15 Sep 2026");
        vars.put("endDate", "17 Sep 2026");
        vars.put("travellerCount", "2");
        vars.put("amount", "12000.00");
        vars.put("currency", "INR");
        vars.put("paymentStatusLabel", "PENDING");
        vars.put("bookingStatusLabel", "PENDING PAYMENT");
        vars.put("paymentPendingNote", "Payment is pending.");
        vars.put("continuePaymentUrl", "http://localhost:3000/bookings/lookup");
        vars.put("supportEmail", "support@joyway.in");
        vars.put("bookingsEmail", "bookings@joyway.in");
        vars.put("frontendUrl", "http://localhost:3000");

        String html = service.render(EmailTemplates.BOOKING_CREATED, vars);

        assertThat(html).contains("JOYWAY");
        assertThat(html).contains("JW-2026-00001");
        assertThat(html).contains("Asha");
        assertThat(html).contains("support@joyway.in");
    }
}
