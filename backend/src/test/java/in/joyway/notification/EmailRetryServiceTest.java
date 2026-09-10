package in.joyway.notification;

import in.joyway.config.MailProperties;
import in.joyway.domain.entity.Booking;
import in.joyway.domain.entity.EmailLog;
import in.joyway.domain.entity.Payment;
import in.joyway.domain.enums.EmailStatus;
import in.joyway.domain.repository.BookingRepository;
import in.joyway.domain.repository.EmailLogRepository;
import in.joyway.domain.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailRetryServiceTest {

    @Mock
    private EmailLogRepository emailLogRepository;
    @Mock
    private EmailService emailService;
    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private BookingNotificationService bookingNotificationService;
    @Mock
    private in.joyway.domain.repository.WaitlistEntryRepository waitlistEntryRepository;

    private EmailRetryService emailRetryService;
    private Booking booking;
    private Payment payment;

    @BeforeEach
    void setUp() {
        MailProperties mail = new MailProperties();
        mail.setMaxRetryAttempts(3);
        mail.setRetryDelayMs(1_000);
        emailRetryService = new EmailRetryService(
                emailLogRepository, emailService, bookingRepository, paymentRepository,
                bookingNotificationService, waitlistEntryRepository, mail);
        booking = Booking.builder().id(9L).build();
        payment = Payment.builder().id(1L).build();
    }

    @Test
    void retryFailedEmails_retriesDueLogsAndSkipsUnresolved() {
        EmailLog dueCreated = log(1L, EmailTemplates.BOOKING_CREATED, EmailStatus.FAILED, null);
        EmailLog dueUnknown = log(2L, "unknown-template", EmailStatus.FAILED, null);
        EmailLog notBooking = log(3L, EmailTemplates.BOOKING_CREATED, EmailStatus.FAILED, null);
        notBooking.setRelatedEntityType("CUSTOMER");
        EmailLog deferred = log(4L, EmailTemplates.BOOKING_CREATED, EmailStatus.PENDING, Instant.now());
        EmailLog sent = log(5L, EmailTemplates.BOOKING_CREATED, EmailStatus.SENT, Instant.now());
        EmailLog missingBooking = log(6L, EmailTemplates.PAYMENT_SUCCESS, EmailStatus.FAILED, null);
        missingBooking.setRelatedEntityId(99L);

        when(emailLogRepository.findByStatusInAndAttemptCountLessThanOrderByCreatedAtAsc(any(), anyInt()))
                .thenReturn(List.of(dueCreated, dueUnknown, notBooking, deferred, sent, missingBooking));
        when(bookingRepository.findByIdWithDetails(9L)).thenReturn(Optional.of(booking));
        when(bookingRepository.findByIdWithDetails(99L)).thenReturn(Optional.empty());
        when(paymentRepository.findTopByBookingIdOrderByCreatedAtDesc(9L)).thenReturn(Optional.of(payment));
        when(bookingNotificationService.buildBookingCreatedVars(booking)).thenReturn(Map.of("k", "v"));

        emailRetryService.retryFailedEmails();

        verify(emailService).retryEmail(dueCreated, Map.of("k", "v"));
        verify(emailService, never()).retryEmail(org.mockito.ArgumentMatchers.eq(dueUnknown), anyMap());
        verify(emailService, never()).retryEmail(org.mockito.ArgumentMatchers.eq(notBooking), anyMap());
        verify(emailService, never()).retryEmail(org.mockito.ArgumentMatchers.eq(deferred), anyMap());
        verify(emailService, never()).retryEmail(org.mockito.ArgumentMatchers.eq(sent), anyMap());
        verify(emailService, never()).retryEmail(org.mockito.ArgumentMatchers.eq(missingBooking), anyMap());
    }

    @Test
    void retryFailedEmails_resolvesEveryBookingTemplate() {
        List<String> templates = List.of(
                EmailTemplates.BOOKING_CREATED,
                EmailTemplates.PAYMENT_SUCCESS,
                EmailTemplates.PAYMENT_FAILED,
                EmailTemplates.BOOKING_CONFIRMED,
                EmailTemplates.BOOKING_CANCELLED,
                EmailTemplates.BOOKING_RECEIPT,
                EmailTemplates.TRIP_REMINDER_7,
                EmailTemplates.TRIP_REMINDER_1,
                EmailTemplates.PAYMENT_EXPIRY_REMINDER,
                EmailTemplates.REVIEW_REQUEST,
                EmailTemplates.ADMIN_NEW_BOOKING,
                EmailTemplates.ADMIN_PAYMENT_SUCCESS,
                EmailTemplates.ADMIN_PAYMENT_FAILED,
                EmailTemplates.ADMIN_BOOKING_CANCELLED
        );
        List<EmailLog> logs = templates.stream()
                .map(template -> log(1L, template, EmailStatus.FAILED, null))
                .toList();
        when(emailLogRepository.findByStatusInAndAttemptCountLessThanOrderByCreatedAtAsc(any(), anyInt()))
                .thenReturn(logs);
        when(bookingRepository.findByIdWithDetails(9L)).thenReturn(Optional.of(booking));
        when(paymentRepository.findTopByBookingIdOrderByCreatedAtDesc(9L)).thenReturn(Optional.empty());
        when(bookingNotificationService.buildBookingCreatedVars(booking)).thenReturn(Map.of("k", "v"));
        when(bookingNotificationService.buildPaymentSuccessVars(booking, null)).thenReturn(Map.of("k", "v"));
        when(bookingNotificationService.buildPaymentFailedVars(booking)).thenReturn(Map.of("k", "v"));
        when(bookingNotificationService.buildBookingConfirmedVars(booking)).thenReturn(Map.of("k", "v"));
        when(bookingNotificationService.buildBookingCancelledVars(booking)).thenReturn(Map.of("k", "v"));
        when(bookingNotificationService.buildReceiptVars(booking, null)).thenReturn(Map.of("k", "v"));
        when(bookingNotificationService.buildTripReminderVars(booking, 7)).thenReturn(Map.of("k", "v"));
        when(bookingNotificationService.buildTripReminderVars(booking, 1)).thenReturn(Map.of("k", "v"));
        when(bookingNotificationService.buildPaymentExpiryReminderVars(booking)).thenReturn(Map.of("k", "v"));
        when(bookingNotificationService.buildReviewRequestVars(booking)).thenReturn(Map.of("k", "v"));

        emailRetryService.retryFailedEmails();

        verify(emailService, times(templates.size())).retryEmail(any(EmailLog.class), anyMap());
    }

    @Test
    void retryFailedEmails_resolvesWaitlistTemplate() {
        EmailLog waitlist = log(9L, EmailTemplates.WAITLIST_SEAT_AVAILABLE, EmailStatus.FAILED, null);
        waitlist.setRelatedEntityType("WAITLIST");
        EmailLog waitlistWrongTemplate = log(9L, EmailTemplates.BOOKING_CREATED, EmailStatus.FAILED, null);
        waitlistWrongTemplate.setRelatedEntityType("WAITLIST");
        EmailLog waitlistMissing = log(8L, EmailTemplates.WAITLIST_SEAT_AVAILABLE, EmailStatus.FAILED, null);
        waitlistMissing.setRelatedEntityType("WAITLIST");
        waitlistMissing.setRelatedEntityId(8L);

        in.joyway.domain.entity.WaitlistEntry entry = in.joyway.domain.entity.WaitlistEntry.builder()
                .id(9L)
                .fullName("Priya")
                .email("priya@example.com")
                .travelPackage(in.joyway.domain.entity.TravelPackage.builder()
                        .title("Nainital")
                        .slug("nainital")
                        .build())
                .build();
        when(emailLogRepository.findByStatusInAndAttemptCountLessThanOrderByCreatedAtAsc(any(), anyInt()))
                .thenReturn(List.of(waitlist, waitlistWrongTemplate, waitlistMissing));
        when(waitlistEntryRepository.findByIdWithPackage(9L)).thenReturn(Optional.of(entry));
        when(waitlistEntryRepository.findByIdWithPackage(8L)).thenReturn(Optional.empty());
        when(bookingNotificationService.buildWaitlistSeatAvailableVars(entry.getTravelPackage(), entry))
                .thenReturn(Map.of("k", "v"));

        emailRetryService.retryFailedEmails();

        verify(emailService).retryEmail(waitlist, Map.of("k", "v"));
        verify(emailService, never()).retryEmail(org.mockito.ArgumentMatchers.eq(waitlistWrongTemplate), anyMap());
        verify(emailService, never()).retryEmail(org.mockito.ArgumentMatchers.eq(waitlistMissing), anyMap());
    }

    private static EmailLog log(Long relatedId, String template, EmailStatus status, Instant lastAttempt) {
        return EmailLog.builder()
                .id(relatedId)
                .recipient("customer@example.com")
                .subject("s")
                .template(template)
                .status(status)
                .relatedEntityType("BOOKING")
                .relatedEntityId(relatedId == null ? null : 9L)
                .attemptCount(1)
                .lastAttemptedAt(lastAttempt)
                .build();
    }
}
