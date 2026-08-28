package in.joyway.notification;

import in.joyway.config.MailProperties;
import in.joyway.domain.enums.EmailCategory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EmailSenderResolverTest {

    private EmailSenderResolver resolver;

    @BeforeEach
    void setUp() {
        MailProperties properties = new MailProperties();
        properties.setBookingsFrom("bookings@joyway.in");
        properties.setBookingsFromName("JOYWAY Bookings");
        properties.setBookingsReplyTo("bookings@joyway.in");
        properties.setSupportFrom("support@joyway.in");
        properties.setSupportFromName("JOYWAY Support");
        properties.setSupportReplyTo("support@joyway.in");
        properties.setNoreplyFrom("noreply@joyway.in");
        properties.setNoreplyFromName("JOYWAY");
        properties.setNoreplyReplyTo("noreply@joyway.in");
        properties.setGeneralFrom("hello@joyway.in");
        properties.setGeneralFromName("JOYWAY");
        properties.setGeneralReplyTo("hello@joyway.in");
        resolver = new EmailSenderResolver(properties);
    }

    @Test
    void resolveForTemplate_usesBookingsSenderForBookingCreated() {
        EmailSenderProfile profile = resolver.resolveForTemplate(EmailTemplates.BOOKING_CREATED);

        assertThat(profile.fromAddress()).isEqualTo("bookings@joyway.in");
        assertThat(profile.fromName()).isEqualTo("JOYWAY Bookings");
        assertThat(profile.replyTo()).isEqualTo("bookings@joyway.in");
    }

    @Test
    void resolveForTemplate_usesNoreplySenderForAdminAlerts() {
        EmailSenderProfile profile = resolver.resolveForTemplate(EmailTemplates.ADMIN_NEW_BOOKING);

        assertThat(profile.fromAddress()).isEqualTo("noreply@joyway.in");
        assertThat(profile.replyTo()).isEqualTo("noreply@joyway.in");
    }

    @Test
    void resolveForTemplate_usesGeneralSenderForWelcome() {
        EmailSenderProfile profile = resolver.resolveForTemplate(EmailTemplates.WELCOME);

        assertThat(profile.fromAddress()).isEqualTo("hello@joyway.in");
        assertThat(profile.replyTo()).isEqualTo("hello@joyway.in");
    }

    @Test
    void resolve_supportCategoryUsesSupportAddresses() {
        EmailSenderProfile profile = resolver.resolve(EmailCategory.SUPPORT);

        assertThat(profile.fromAddress()).isEqualTo("support@joyway.in");
        assertThat(profile.fromName()).isEqualTo("JOYWAY Support");
        assertThat(profile.replyTo()).isEqualTo("support@joyway.in");
    }
}
