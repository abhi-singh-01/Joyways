package in.joyway.observability;

import in.joyway.service.BookingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PendingPaymentExpiryScheduler {

    private final BookingService bookingService;

    @Scheduled(cron = "${joyway.booking.payment-expiry-cron:0 * * * * *}", zone = "Asia/Kolkata")
    public void expireStalePendingPayments() {
        int expired = bookingService.expireStalePendingPaymentBookings();
        if (expired > 0) {
            log.info("event=booking.payment_expiry processed={}", expired);
        }
    }
}
