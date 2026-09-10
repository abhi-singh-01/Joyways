package in.joyway.notification;

import in.joyway.config.MailProperties;
import in.joyway.domain.entity.EmailLog;
import in.joyway.domain.enums.EmailCategory;
import in.joyway.domain.enums.EmailStatus;
import in.joyway.domain.repository.EmailLogRepository;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;
    private final MailProperties mailProperties;
    private final EmailLogRepository emailLogRepository;
    private final EmailTemplateService emailTemplateService;
    private final EmailSenderResolver emailSenderResolver;
    private EmailService self = this;

    @Autowired
    @Lazy
    void setSelf(EmailService self) {
        this.self = self;
    }

    @Async
    public void sendTemplateEmailAsync(
            String template,
            String recipient,
            String subject,
            Map<String, String> variables,
            String relatedEntityType,
            Long relatedEntityId
    ) {
        self.sendTemplateEmail(template, recipient, subject, variables, relatedEntityType, relatedEntityId);
    }

    /**
     * Sends a templated email with a custom reply-to address (e.g. contact form submitter).
     */
    @Transactional
    public void sendTemplateEmailWithReplyTo(
            String template,
            String recipient,
            String subject,
            Map<String, String> variables,
            String relatedEntityType,
            Long relatedEntityId,
            String replyTo
    ) {
        dispatchSend(new EmailSendRequest(
                template, recipient, subject, variables, relatedEntityType, relatedEntityId, null, replyTo));
    }

    @Transactional
    public void sendTemplateEmail(
            String template,
            String recipient,
            String subject,
            Map<String, String> variables,
            String relatedEntityType,
            Long relatedEntityId
    ) {
        dispatchSend(new EmailSendRequest(
                template, recipient, subject, variables, relatedEntityType, relatedEntityId, null, null));
    }

    @Transactional
    public void sendTemplateEmail(
            String template,
            String recipient,
            String subject,
            Map<String, String> variables,
            String relatedEntityType,
            Long relatedEntityId,
            EmailCategory categoryOverride
    ) {
        dispatchSend(new EmailSendRequest(
                template, recipient, subject, variables, relatedEntityType, relatedEntityId, categoryOverride, null));
    }

    private void dispatchSend(EmailSendRequest request) {
        if (!mailProperties.isEnabled()) {
            log.info("Email disabled; skipping template={} recipient={}",
                    request.template(), maskEmail(request.recipient()));
            return;
        }

        if (emailLogRepository.existsByTemplateAndRecipientAndRelatedEntityTypeAndRelatedEntityId(
                request.template(),
                request.recipient(),
                request.relatedEntityType(),
                request.relatedEntityId())) {
            log.info("Duplicate email skipped: template={} recipient={}",
                    request.template(), maskEmail(request.recipient()));
            return;
        }

        EmailLog emailLog = EmailLog.builder()
                .recipient(request.recipient())
                .subject(request.subject())
                .template(request.template())
                .status(EmailStatus.PENDING)
                .relatedEntityType(request.relatedEntityType())
                .relatedEntityId(request.relatedEntityId())
                .attemptCount(0)
                .build();
        emailLog = emailLogRepository.save(emailLog);

        EmailCategory category = request.categoryOverride() != null
                ? request.categoryOverride()
                : EmailTemplateCategoryResolver.resolve(request.template());
        deliver(emailLog, request.variables(), category, request.replyToOverride());
    }

    @Transactional
    public void retryEmail(EmailLog emailLog, Map<String, String> variables) {
        if (emailLog.getAttemptCount() >= mailProperties.getMaxRetryAttempts()) {
            return;
        }
        EmailCategory category = EmailTemplateCategoryResolver.resolve(emailLog.getTemplate());
        deliver(emailLog, variables, category, null);
    }

    private void deliver(
            EmailLog emailLog,
            Map<String, String> variables,
            EmailCategory category,
            String replyToOverride
    ) {
        emailLog.setAttemptCount(emailLog.getAttemptCount() + 1);
        emailLog.setLastAttemptedAt(Instant.now());
        try {
            String html = emailTemplateService.render(emailLog.getTemplate(), variables);
            EmailSenderProfile sender = emailSenderResolver.resolve(category);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(emailLog.getRecipient());
            helper.setSubject(emailLog.getSubject());
            helper.setFrom(sender.fromAddress(), sender.fromName());
            String replyTo = replyToOverride != null && !replyToOverride.isBlank()
                    ? replyToOverride
                    : sender.replyTo();
            if (replyTo != null && !replyTo.isBlank()) {
                helper.setReplyTo(replyTo);
            }
            helper.setText(html, true);
            mailSender.send(message);

            emailLog.setStatus(EmailStatus.SENT);
            emailLog.setSentAt(Instant.now());
            emailLog.setFailureReason(null);
            log.info("event=email.sent template={} category={} from={} recipient={}",
                    emailLog.getTemplate(), category, maskEmail(sender.fromAddress()), maskEmail(emailLog.getRecipient()));
        } catch (Exception ex) {
            emailLog.setStatus(EmailStatus.FAILED);
            emailLog.setFailureReason(ex.getClass().getSimpleName());
            log.warn("event=email.failed template={} category={} recipient={} reason={} attempt={}",
                    emailLog.getTemplate(), category, maskEmail(emailLog.getRecipient()),
                    ex.getClass().getSimpleName(), emailLog.getAttemptCount());
        }
        emailLogRepository.save(emailLog);
    }

    public Optional<EmailLog> findLog(Long id) {
        return emailLogRepository.findById(id);
    }

    private static String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return "***";
        }
        int at = email.indexOf('@');
        return email.charAt(0) + "***" + email.substring(at);
    }

    private record EmailSendRequest(
            String template,
            String recipient,
            String subject,
            Map<String, String> variables,
            String relatedEntityType,
            Long relatedEntityId,
            EmailCategory categoryOverride,
            String replyToOverride
    ) {
    }
}
