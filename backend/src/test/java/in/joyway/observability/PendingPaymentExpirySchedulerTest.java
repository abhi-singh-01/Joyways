package in.joyway.observability;

import in.joyway.service.BookingService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PendingPaymentExpirySchedulerTest {

    @Mock
    private BookingService bookingService;

    @InjectMocks
    private PendingPaymentExpiryScheduler scheduler;

    @Test
    void expireStalePendingPayments_delegatesToBookingService() {
        when(bookingService.expireStalePendingPaymentBookings()).thenReturn(2);

        scheduler.expireStalePendingPayments();

        verify(bookingService).expireStalePendingPaymentBookings();
    }
}
