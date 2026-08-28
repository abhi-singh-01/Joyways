package in.joyway.notification;

import in.joyway.domain.entity.Booking;
import in.joyway.domain.entity.EmailLog;
import in.joyway.domain.entity.Payment;
import in.joyway.domain.enums.EmailStatus;
import in.joyway.domain.repository.BookingRepository;
import in.joyway.domain.repository.EmailLogRepository;
import in.joyway.domain.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailRetryService {

    private final EmailLogRepository emailLogRepository;
    private final EmailService emailService;
    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;
    private final BookingNotificationService bookingNotificationService;
    private final in.joyway.config.MailProperties mailProperties;

    @Transactional
    public void retryFailedEmails() {
        List<EmailLog> pending = emailLogRepository.findByStatusInAndAttemptCountLessThanOrderByCreatedAtAsc(
                List.of(EmailStatus.FAILED, EmailStatus.PENDING),
                mailProperties.getMaxRetryAttempts()
        );
        Instant now = Instant.now();
        Duration baseDelay = Duration.ofMillis(Math.max(1_000, mailProperties.getRetryDelayMs()));
        int retried = 0;
        int deferred = 0;
        for (EmailLog emailLog : pending) {
            if (emailLog.getStatus() == EmailStatus.SENT) {
                continue;
            }
            if (!EmailRetryBackoff.isDue(
                    emailLog.getLastAttemptedAt(),
                    emailLog.getAttemptCount(),
                    baseDelay,
                    now)) {
                deferred++;
                continue;
            }
            Map<String, String> variables = resolveVariables(emailLog);
            if (variables == null) {
                log.warn("event=email.retry.skipped reason=unresolved_variables logId={}", emailLog.getId());
                continue;
            }
            emailService.retryEmail(emailLog, variables);
            retried++;
        }
        if (retried > 0 || deferred > 0) {
            log.info("event=email.retry.cycle retried={} deferred={}", retried, deferred);
        }
    }

    private Map<String, String> resolveVariables(EmailLog emailLog) {
        if (!"BOOKING".equals(emailLog.getRelatedEntityType()) || emailLog.getRelatedEntityId() == null) {
            return null;
        }
        Booking booking = bookingRepository.findByIdWithDetails(emailLog.getRelatedEntityId()).orElse(null);
        if (booking == null) {
            return null;
        }
        Payment payment = paymentRepository.findTopByBookingIdOrderByCreatedAtDesc(booking.getId()).orElse(null);
        String template = emailLog.getTemplate();
        return switch (template) {
            case EmailTemplates.BOOKING_CREATED -> bookingNotificationService.buildBookingCreatedVars(booking);
            case EmailTemplates.PAYMENT_SUCCESS -> bookingNotificationService.buildPaymentSuccessVars(booking, payment);
            case EmailTemplates.PAYMENT_FAILED -> bookingNotificationService.buildPaymentFailedVars(booking);
            case EmailTemplates.BOOKING_CONFIRMED -> bookingNotificationService.buildBookingConfirmedVars(booking);
            case EmailTemplates.BOOKING_CANCELLED -> bookingNotificationService.buildBookingCancelledVars(booking);
            case EmailTemplates.BOOKING_RECEIPT -> bookingNotificationService.buildReceiptVars(booking, payment);
            case EmailTemplates.TRIP_REMINDER_7 -> bookingNotificationService.buildTripReminderVars(booking, 7);
            case EmailTemplates.TRIP_REMINDER_1 -> bookingNotificationService.buildTripReminderVars(booking, 1);
            case EmailTemplates.ADMIN_NEW_BOOKING,
                 EmailTemplates.ADMIN_PAYMENT_SUCCESS,
                 EmailTemplates.ADMIN_PAYMENT_FAILED,
                 EmailTemplates.ADMIN_BOOKING_CANCELLED -> bookingNotificationService.buildBookingCreatedVars(booking);
            default -> null;
        };
    }
}
