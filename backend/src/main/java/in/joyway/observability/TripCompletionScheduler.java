package in.joyway.observability;

import in.joyway.service.BookingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class TripCompletionScheduler {

    private final BookingService bookingService;

    @Scheduled(cron = "${joyway.booking.trip-completion-cron:0 30 20 * * *}", zone = "Asia/Kolkata")
    public void completeEndedTrips() {
        int completed = bookingService.completeEndedConfirmedBookings();
        if (completed > 0) {
            log.info("event=booking.trip_completion processed={}", completed);
        }
    }
}
