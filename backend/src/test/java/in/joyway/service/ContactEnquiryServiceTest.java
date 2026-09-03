package in.joyway.service;

import in.joyway.config.MailProperties;
import in.joyway.dto.contact.ContactEnquiryRequest;
import in.joyway.exception.RequestValidationException;
import in.joyway.notification.EmailService;
import in.joyway.notification.EmailTemplates;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ContactEnquiryServiceTest {

    @Mock
    private EmailService emailService;

    private MailProperties mailProperties;
    private ContactEnquiryService service;

    @BeforeEach
    void setUp() {
        mailProperties = new MailProperties();
        mailProperties.setAdminNotificationEmail("hello@joyway.in");
        service = new ContactEnquiryService(emailService, mailProperties);
    }

    @Test
    void submitEnquiry_sendsSupportEmailWithSubmitterReplyTo() {
        ContactEnquiryRequest request = validRequest();

        service.submitEnquiry(request);

        ArgumentCaptor<Long> relatedIdCaptor = ArgumentCaptor.forClass(Long.class);
        verify(emailService).sendTemplateEmailWithReplyTo(
                eq(EmailTemplates.CONTACT_ENQUIRY),
                eq("hello@joyway.in"),
                eq("[JOYWAY Enquiry] Group trip enquiry"),
                any(),
                eq("CONTACT"),
                relatedIdCaptor.capture(),
                eq("visitor@example.com")
        );
        assertThat(relatedIdCaptor.getValue()).isNotNull();
    }

    @Test
    void submitEnquiry_rejectsHeaderInjectionInSubject() {
        ContactEnquiryRequest request = new ContactEnquiryRequest(
                "Jane Doe",
                "visitor@example.com",
                "9876543210",
                "Hello\r\nBcc: attacker@evil.com",
                "I would like to know more about your Nainital packages."
        );

        assertThatThrownBy(() -> service.submitEnquiry(request))
                .isInstanceOf(RequestValidationException.class);
    }

    private static ContactEnquiryRequest validRequest() {
        return new ContactEnquiryRequest(
                "Jane Doe",
                "visitor@example.com",
                "9876543210",
                "Group trip enquiry",
                "I would like to know more about your Nainital packages."
        );
    }
}
