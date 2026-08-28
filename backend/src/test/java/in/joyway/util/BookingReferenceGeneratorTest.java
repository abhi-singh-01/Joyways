package in.joyway.util;

import in.joyway.domain.repository.BookingRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingReferenceGeneratorTest {

    @Mock
    private BookingRepository bookingRepository;

    @InjectMocks
    private BookingReferenceGenerator generator;

    @Test
    void generateNext_startsAtOneWhenNoExisting() {
        when(bookingRepository.findFirstByBookingReferenceStartingWithOrderByBookingReferenceDesc(anyString()))
                .thenReturn(Optional.empty());
        when(bookingRepository.existsByBookingReference(anyString())).thenReturn(false);

        String ref = generator.generateNext();
        assertThat(ref).matches("JW-\\d{4}-00001");
    }
}
