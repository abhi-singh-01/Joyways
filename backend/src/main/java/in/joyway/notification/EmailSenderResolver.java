package in.joyway.notification;

import in.joyway.config.MailProperties;
import in.joyway.domain.enums.EmailCategory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EmailSenderResolver {

    private final MailProperties mailProperties;

    public EmailSenderProfile resolve(EmailCategory category) {
        return switch (category) {
            case BOOKING -> profile(
                    mailProperties.getBookingsFrom(),
                    mailProperties.getBookingsFromName(),
                    mailProperties.getBookingsReplyTo()
            );
            case SUPPORT -> profile(
                    mailProperties.getSupportFrom(),
                    mailProperties.getSupportFromName(),
                    mailProperties.getSupportReplyTo()
            );
            case NOREPLY -> profile(
                    mailProperties.getNoreplyFrom(),
                    mailProperties.getNoreplyFromName(),
                    mailProperties.getNoreplyReplyTo()
            );
            case GENERAL -> profile(
                    mailProperties.getGeneralFrom(),
                    mailProperties.getGeneralFromName(),
                    mailProperties.getGeneralReplyTo()
            );
        };
    }

    public EmailSenderProfile resolveForTemplate(String template) {
        return resolve(EmailTemplateCategoryResolver.resolve(template));
    }

    private static EmailSenderProfile profile(String from, String fromName, String replyTo) {
        return new EmailSenderProfile(from, fromName, replyTo);
    }
}
