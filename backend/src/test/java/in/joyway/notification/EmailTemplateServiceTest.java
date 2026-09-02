package in.joyway.notification;

import in.joyway.config.MailProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class EmailTemplateServiceTest {

    private EmailTemplateService service;

    @BeforeEach
    void setUp() {
        MailProperties mailProperties = new MailProperties();
        mailProperties.setFrom("hello@joyway.in");
        mailProperties.setSupportFrom("support@joyway.in");
        mailProperties.setBookingsFrom("bookings@joyway.in");
        service = new EmailTemplateService(mailProperties);
    }

    @Test
    void render_mergesVariablesIntoLayout() {
        Map<String, String> vars = bookingCreatedVariables("Asha", "JW-2026-00001");

        String html = service.render(EmailTemplates.BOOKING_CREATED, vars);

        assertThat(html).contains("JOYWAY");
        assertThat(html).contains("JW-2026-00001");
        assertThat(html).contains("Asha");
        assertThat(html).contains("support@joyway.in");
    }

    @ParameterizedTest
    @MethodSource("maliciousHtmlValues")
    void render_escapesMaliciousUserInput(String field, String maliciousValue) {
        Map<String, String> vars = bookingCreatedVariables("Safe Name", "JW-2026-00001");
        vars.put(field, maliciousValue);

        String html = service.render(EmailTemplates.BOOKING_CREATED, vars);

        assertThat(html).doesNotContain("<script>");
        assertThat(html).doesNotContain("<img src=x");
        assertThat(html).doesNotContain("<a href=\"https://evil.example\">");
        assertThat(html).contains(EmailTemplateService.escapeHtml(maliciousValue));
    }

    @Test
    void render_preservesUnicodeNames() {
        Map<String, String> vars = bookingCreatedVariables("अभिनव शर्मा", "JW-2026-00001");

        String html = service.render(EmailTemplates.BOOKING_CREATED, vars);

        assertThat(html).contains("अभिनव शर्मा");
    }

    @Test
    void render_escapesQuotesAndAmpersands() {
        Map<String, String> vars = bookingCreatedVariables("Tom & \"Jerry\"", "JW-2026-00001");

        String html = service.render(EmailTemplates.BOOKING_CREATED, vars);

        assertThat(html).contains("Tom &amp; &quot;Jerry&quot;");
        assertThat(html).doesNotContain("Tom & \"Jerry\"");
    }

    @Test
    void escapeHtml_escapesScriptTag() {
        assertThat(EmailTemplateService.escapeHtml("<script>alert(1)</script>"))
                .isEqualTo("&lt;script&gt;alert(1)&lt;/script&gt;");
    }

    private static Stream<Arguments> maliciousHtmlValues() {
        return Stream.of(
                Arguments.of("customerName", "<script>alert(1)</script>"),
                Arguments.of("packageTitle", "<img src=x onerror=alert(1)>"),
                Arguments.of("destination", "<a href=\"https://evil.example\">Click</a>"),
                Arguments.of("paymentPendingNote", "<script>alert(1)</script>")
        );
    }

    private static Map<String, String> bookingCreatedVariables(String customerName, String bookingReference) {
        Map<String, String> vars = new HashMap<>();
        vars.put("customerName", customerName);
        vars.put("bookingReference", bookingReference);
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
        return vars;
    }
}
