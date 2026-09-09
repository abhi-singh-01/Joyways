package in.joyway.notification;

import in.joyway.config.MailProperties;
import in.joyway.domain.entity.EmailLog;
import in.joyway.domain.enums.EmailCategory;
import in.joyway.domain.enums.EmailStatus;
import in.joyway.domain.repository.EmailLogRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;
    @Mock
    private MailProperties mailProperties;
    @Mock
    private EmailLogRepository emailLogRepository;
    @Mock
    private EmailTemplateService emailTemplateService;
    @Mock
    private EmailSenderResolver emailSenderResolver;

    @InjectMocks
    private EmailService emailService;

    @Test
    void sendTemplateEmail_skipsDuplicateSentEmail() {
        when(mailProperties.isEnabled()).thenReturn(true);
        when(emailLogRepository.existsByTemplateAndRecipientAndRelatedEntityTypeAndRelatedEntityIdAndStatus(
                EmailTemplates.BOOKING_CREATED,
                "customer@example.com",
                "BOOKING",
                1L,
                EmailStatus.SENT
        )).thenReturn(true);

        emailService.sendTemplateEmail(
                EmailTemplates.BOOKING_CREATED,
                "customer@example.com",
                "Subject",
                Map.of("customerName", "Test"),
                "BOOKING",
                1L
        );

        verify(emailLogRepository, never()).save(any());
        verify(mailSender, never()).createMimeMessage();
    }

    @Test
    void sendTemplateEmail_recordsFailureWithoutThrowing() {
        when(mailProperties.isEnabled()).thenReturn(true);
        when(mailProperties.getFrom()).thenReturn("hello@joyway.in");
        when(mailProperties.getFromName()).thenReturn("JOYWAY");
        when(emailSenderResolver.resolve(org.mockito.ArgumentMatchers.any()))
                .thenReturn(new EmailSenderProfile("bookings@joyway.in", "JOYWAY Bookings", "bookings@joyway.in"));
        when(emailLogRepository.existsByTemplateAndRecipientAndRelatedEntityTypeAndRelatedEntityIdAndStatus(
                any(), any(), any(), any(), eq(EmailStatus.SENT)
        )).thenReturn(false);
        when(emailLogRepository.save(any(EmailLog.class))).thenAnswer(inv -> {
            EmailLog log = inv.getArgument(0);
            log.setId(1L);
            return log;
        });
        when(emailTemplateService.render(any(), any())).thenReturn("<p>Hi</p>");
        when(mailSender.createMimeMessage()).thenThrow(new IllegalStateException("SMTP down"));

        emailService.sendTemplateEmail(
                EmailTemplates.PAYMENT_SUCCESS,
                "customer@example.com",
                "Subject",
                Map.of(),
                "BOOKING",
                2L
        );

        ArgumentCaptor<EmailLog> captor = ArgumentCaptor.forClass(EmailLog.class);
        verify(emailLogRepository, org.mockito.Mockito.atLeastOnce()).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(EmailStatus.FAILED);
    }

    @Test
    void sendTemplateEmail_usesCategoryOverrideWhenDelivering() {
        when(mailProperties.isEnabled()).thenReturn(true);
        when(emailSenderResolver.resolve(EmailCategory.SUPPORT))
                .thenReturn(new EmailSenderProfile("support@joyway.in", "JOYWAY Support", "support@joyway.in"));
        when(emailLogRepository.existsByTemplateAndRecipientAndRelatedEntityTypeAndRelatedEntityIdAndStatus(
                any(), any(), any(), any(), eq(EmailStatus.SENT)
        )).thenReturn(false);
        when(emailLogRepository.save(any(EmailLog.class))).thenAnswer(inv -> {
            EmailLog log = inv.getArgument(0);
            log.setId(1L);
            return log;
        });
        when(emailTemplateService.render(any(), any())).thenReturn("<p>Hi</p>");
        when(mailSender.createMimeMessage()).thenThrow(new IllegalStateException("SMTP down"));

        emailService.sendTemplateEmail(
                EmailTemplates.WELCOME,
                "customer@example.com",
                "Subject",
                Map.of(),
                "CUSTOMER",
                3L,
                EmailCategory.SUPPORT
        );

        verify(emailSenderResolver).resolve(EmailCategory.SUPPORT);
    }

    @Test
    void sendTemplateEmail_respectsDisabledFlag() {
        when(mailProperties.isEnabled()).thenReturn(false);

        emailService.sendTemplateEmail(
                EmailTemplates.WELCOME,
                "customer@example.com",
                "Welcome",
                Map.of(),
                "CUSTOMER",
                null
        );

        verify(emailLogRepository, never()).save(any());
    }

    @Test
    void sendOverloads_skipWhenMailDisabled() {
        when(mailProperties.isEnabled()).thenReturn(false);

        emailService.sendTemplateEmailAsync(
                EmailTemplates.WELCOME, "customer@example.com", "Welcome", Map.of(), "CUSTOMER", null);
        emailService.sendTemplateEmailWithReplyTo(
                EmailTemplates.CONTACT_ENQUIRY, "hello@joyway.in", "Enquiry", Map.of(),
                "CONTACT", 1L, "guest@example.com");
        emailService.sendTemplateEmail(
                EmailTemplates.BOOKING_CREATED, "customer@example.com", "Created", Map.of(),
                "BOOKING", 1L, EmailCategory.BOOKING);

        verify(emailLogRepository, never()).save(any());
    }
}
