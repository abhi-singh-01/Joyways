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
class TripCompletionSchedulerTest {

    @Mock
    private BookingService bookingService;

    @InjectMocks
    private TripCompletionScheduler scheduler;

    @Test
    void completeEndedTrips_delegatesToBookingService() {
        when(bookingService.completeEndedConfirmedBookings()).thenReturn(2);

        scheduler.completeEndedTrips();

        verify(bookingService).completeEndedConfirmedBookings();
    }
}
