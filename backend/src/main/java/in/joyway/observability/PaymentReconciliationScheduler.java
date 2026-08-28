package in.joyway.observability;

import in.joyway.domain.entity.Booking;
import in.joyway.domain.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Periodically reports bookings whose booking status and payment status disagree.
 *
 * <p>This only observes and logs. Repairing a mismatch requires a human decision
 * (refund, manual confirmation, or reconciliation against the Razorpay dashboard),
 * so nothing is mutated automatically.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentReconciliationScheduler {

    private final BookingRepository bookingRepository;

    @Scheduled(cron = "${joyway.monitoring.reconciliation-cron:0 */15 * * * *}", zone = "Asia/Kolkata")
    @Transactional(readOnly = true)
    public void reportPaymentBookingMismatches() {
        List<Booking> paidNotConfirmed = bookingRepository.findPaidButNotConfirmed();
        List<Booking> confirmedNotPaid = bookingRepository.findConfirmedButNotPaid();

        if (paidNotConfirmed.isEmpty() && confirmedNotPaid.isEmpty()) {
            log.info("event=payment.reconciliation.ok mismatches=0");
            return;
        }

        log.warn("event=payment.reconciliation.mismatch paidNotConfirmed={} confirmedNotPaid={}",
                paidNotConfirmed.size(), confirmedNotPaid.size());

        for (Booking booking : paidNotConfirmed) {
            log.warn("event=payment.reconciliation.paid_not_confirmed bookingReference={} bookingStatus={}",
                    booking.getBookingReference(), booking.getStatus());
        }
        for (Booking booking : confirmedNotPaid) {
            log.warn("event=payment.reconciliation.confirmed_not_paid bookingReference={} paymentStatus={}",
                    booking.getBookingReference(), booking.getPaymentStatus());
        }
    }
}
