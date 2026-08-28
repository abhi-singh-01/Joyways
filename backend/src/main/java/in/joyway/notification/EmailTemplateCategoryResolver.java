package in.joyway.notification;

import in.joyway.domain.enums.EmailCategory;

public final class EmailTemplateCategoryResolver {

    private EmailTemplateCategoryResolver() {
    }

    public static EmailCategory resolve(String template) {
        return switch (template) {
            case EmailTemplates.BOOKING_CREATED,
                 EmailTemplates.PAYMENT_SUCCESS,
                 EmailTemplates.PAYMENT_FAILED,
                 EmailTemplates.BOOKING_CONFIRMED,
                 EmailTemplates.BOOKING_CANCELLED,
                 EmailTemplates.BOOKING_RECEIPT,
                 EmailTemplates.TRIP_REMINDER,
                 EmailTemplates.TRIP_REMINDER_7,
                 EmailTemplates.TRIP_REMINDER_1 -> EmailCategory.BOOKING;
            case EmailTemplates.ADMIN_NEW_BOOKING,
                 EmailTemplates.ADMIN_PAYMENT_SUCCESS,
                 EmailTemplates.ADMIN_PAYMENT_FAILED,
                 EmailTemplates.ADMIN_BOOKING_CANCELLED -> EmailCategory.NOREPLY;
            case EmailTemplates.WELCOME -> EmailCategory.GENERAL;
            default -> EmailCategory.GENERAL;
        };
    }
}
