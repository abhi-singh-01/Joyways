package in.joyway.service;

import in.joyway.domain.entity.Booking;
import in.joyway.domain.entity.Customer;
import in.joyway.domain.entity.TravelPackage;
import in.joyway.domain.repository.BookingRepository;
import in.joyway.domain.repository.PaymentRepository;
import in.joyway.domain.repository.TravellerRepository;
import in.joyway.exception.AccessDeniedBookingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingQueryServiceTest {

    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private TravellerRepository travellerRepository;
    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private ReceiptService receiptService;

    @InjectMocks
    private BookingQueryService bookingQueryService;

    private Booking booking;

    @BeforeEach
    void setUp() {
        Customer customer = Customer.builder()
                .fullName("Alice")
                .email("alice@example.com")
                .mobile("9876543210")
                .build();
        TravelPackage pkg = TravelPackage.builder()
                .title("Nainital")
                .destination("Nainital")
                .build();
        booking = Booking.builder()
                .bookingReference("JW-2026-00001")
                .customer(customer)
                .travelPackage(pkg)
                .numberOfTravellers(2)
                .totalAmount(new BigDecimal("5000"))
                .currency("INR")
                .build();
    }

    @Test
    void getBookingForCustomer_rejectsWrongEmail() {
        when(bookingRepository.findByBookingReference("JW-2026-00001"))
                .thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> bookingQueryService.getBookingForCustomer(
                "JW-2026-00001", "bob@example.com"))
                .isInstanceOf(AccessDeniedBookingException.class);
    }
}
