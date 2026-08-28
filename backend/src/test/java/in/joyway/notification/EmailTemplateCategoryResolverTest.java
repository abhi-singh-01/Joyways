package in.joyway.notification;

import in.joyway.domain.enums.EmailCategory;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EmailTemplateCategoryResolverTest {

    @Test
    void bookingTemplatesUseBookingCategory() {
        assertThat(EmailTemplateCategoryResolver.resolve(EmailTemplates.BOOKING_CREATED))
                .isEqualTo(EmailCategory.BOOKING);
        assertThat(EmailTemplateCategoryResolver.resolve(EmailTemplates.PAYMENT_SUCCESS))
                .isEqualTo(EmailCategory.BOOKING);
        assertThat(EmailTemplateCategoryResolver.resolve(EmailTemplates.BOOKING_CANCELLED))
                .isEqualTo(EmailCategory.BOOKING);
    }

    @Test
    void adminTemplatesUseNoreplyCategory() {
        assertThat(EmailTemplateCategoryResolver.resolve(EmailTemplates.ADMIN_PAYMENT_SUCCESS))
                .isEqualTo(EmailCategory.NOREPLY);
    }

    @Test
    void welcomeUsesGeneralCategory() {
        assertThat(EmailTemplateCategoryResolver.resolve(EmailTemplates.WELCOME))
                .isEqualTo(EmailCategory.GENERAL);
    }
}
