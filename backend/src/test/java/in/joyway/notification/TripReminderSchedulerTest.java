package in.joyway.notification;

import in.joyway.config.NotificationProperties;
import in.joyway.domain.entity.Booking;
import in.joyway.domain.repository.BookingRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TripReminderSchedulerTest {

    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private BookingNotificationService bookingNotificationService;

    @Test
    void sendTripReminders_notifiesSevenDayAndDayBeforeBookings() {
        NotificationProperties properties = new NotificationProperties();
        properties.setTripReminderEnabled(true);
        properties.setTripReminderDaysBefore(7);
        properties.setTripReminderDayBeforeEnabled(true);
        TripReminderScheduler scheduler = new TripReminderScheduler(
                bookingRepository, bookingNotificationService, properties);

        Booking weekOut = Booking.builder().id(1L).build();
        Booking tomorrow = Booking.builder().id(2L).build();
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Kolkata"));
        when(bookingRepository.findConfirmedBookingsStartingOn(today.plusDays(7)))
                .thenReturn(List.of(weekOut));
        when(bookingRepository.findConfirmedBookingsStartingOn(today.plusDays(1)))
                .thenReturn(List.of(tomorrow));

        scheduler.sendTripReminders();

        verify(bookingNotificationService).onTripReminder(weekOut, 7);
        verify(bookingNotificationService).onTripReminder(tomorrow, 1);
    }
}
