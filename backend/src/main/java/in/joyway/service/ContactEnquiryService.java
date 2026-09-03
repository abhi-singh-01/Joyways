package in.joyway.service;

import in.joyway.config.MailProperties;
import in.joyway.dto.contact.ContactEnquiryRequest;
import in.joyway.notification.EmailService;
import in.joyway.notification.EmailTemplates;
import in.joyway.util.EmailHeaderValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class ContactEnquiryService {

    private final EmailService emailService;
    private final MailProperties mailProperties;

    public void submitEnquiry(ContactEnquiryRequest request) {
        validateSafeForEmailHeaders(request);

        String recipient = mailProperties.getAdminNotificationEmail();
        String subject = "[JOYWAY Enquiry] " + request.subject().trim();

        Map<String, String> variables = Map.of(
                "name", request.name().trim(),
                "email", request.email().trim(),
                "phone", request.phone().trim(),
                "subject", request.subject().trim(),
                "message", request.message().trim()
        );

        long relatedId = ThreadLocalRandom.current().nextLong(Long.MAX_VALUE);

        emailService.sendTemplateEmailWithReplyTo(
                EmailTemplates.CONTACT_ENQUIRY,
                recipient,
                subject,
                variables,
                "CONTACT",
                relatedId,
                request.email().trim()
        );
    }

    private static void validateSafeForEmailHeaders(ContactEnquiryRequest request) {
        EmailHeaderValidator.rejectHeaderInjection("name", request.name());
        EmailHeaderValidator.rejectHeaderInjection("email", request.email());
        EmailHeaderValidator.rejectHeaderInjection("phone", request.phone());
        EmailHeaderValidator.rejectHeaderInjection("subject", request.subject());
        EmailHeaderValidator.rejectHeaderInjection("message", request.message());
    }
}
