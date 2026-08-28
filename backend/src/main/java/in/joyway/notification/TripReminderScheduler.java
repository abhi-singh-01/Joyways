package in.joyway.notification;

import in.joyway.config.NotificationProperties;
import in.joyway.domain.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
@RequiredArgsConstructor
@Slf4j
public class TripReminderScheduler {

    private final BookingRepository bookingRepository;
    private final BookingNotificationService bookingNotificationService;
    private final NotificationProperties notificationProperties;

    @Scheduled(cron = "${joyway.notifications.trip-reminder-cron:0 0 8 * * *}", zone = "Asia/Kolkata")
    public void sendTripReminders() {
        if (!notificationProperties.isTripReminderEnabled()) {
            return;
        }

        LocalDate today = LocalDate.now();
        LocalDate sevenDaysAhead = today.plusDays(notificationProperties.getTripReminderDaysBefore());
        bookingRepository.findConfirmedBookingsStartingOn(sevenDaysAhead).forEach(booking ->
                bookingNotificationService.onTripReminder(booking, notificationProperties.getTripReminderDaysBefore())
        );

        if (notificationProperties.isTripReminderDayBeforeEnabled()) {
            LocalDate tomorrow = today.plusDays(1);
            bookingRepository.findConfirmedBookingsStartingOn(tomorrow).forEach(booking ->
                    bookingNotificationService.onTripReminder(booking, 1)
            );
        }
    }
}
