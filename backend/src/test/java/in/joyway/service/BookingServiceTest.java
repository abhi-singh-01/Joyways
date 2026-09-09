package in.joyway.service;

import in.joyway.config.BookingProperties;
import in.joyway.domain.entity.Booking;
import in.joyway.domain.entity.Customer;
import in.joyway.domain.entity.TravelPackage;
import in.joyway.domain.enums.BookingStatus;
import in.joyway.domain.enums.PackageStatus;
import in.joyway.domain.enums.PaymentStatus;
import in.joyway.domain.repository.BookingRepository;
import in.joyway.domain.repository.CustomerRepository;
import in.joyway.domain.repository.TravellerRepository;
import in.joyway.domain.repository.TravelPackageRepository;
import in.joyway.dto.booking.BookingResponse;
import in.joyway.dto.booking.CreateBookingRequest;
import in.joyway.dto.booking.CustomerRequest;
import in.joyway.dto.booking.TravellerRequest;
import in.joyway.exception.InsufficientSeatsException;
import in.joyway.mapper.BookingMapper;
import in.joyway.notification.BookingNotificationService;
import in.joyway.service.operations.OperationsNotificationService;
import in.joyway.service.storage.DocumentStorageService;
import in.joyway.util.BookingReferenceGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock private TravelPackageRepository packageRepository;
    @Mock private CustomerRepository customerRepository;
    @Mock private BookingRepository bookingRepository;
    @Mock private TravellerRepository travellerRepository;
    @Mock private BookingReferenceGenerator referenceGenerator;
    @Mock private BookingMapper bookingMapper;
    @Mock private DocumentStorageService documentStorageService;
    @Mock private BookingNotificationService bookingNotificationService;
    @Mock private BookingProperties bookingProperties;
    @Mock private OperationsNotificationService operationsNotificationService;

    @InjectMocks
    private BookingService bookingService;

    private TravelPackage pkg;
    private CreateBookingRequest request;

    @BeforeEach
    void setUp() {
        pkg = TravelPackage.builder()
                .id(1L)
                .title("Test Trip")
                .slug("test-trip")
                .destination("Nainital")
                .startDate(LocalDate.now().plusDays(14))
                .endDate(LocalDate.now().plusDays(16))
                .duration("3D/2N")
                .pricePerPerson(new BigDecimal("4999.00"))
                .totalSeats(20)
                .availableSeats(2)
                .status(PackageStatus.PUBLISHED)
                .build();

        CustomerRequest customer = new CustomerRequest();
        customer.setFullName("John Doe");
        customer.setEmail("john@example.com");
        customer.setMobile("9876543210");

        TravellerRequest traveller = new TravellerRequest();
        traveller.setFullName("John Doe");
        traveller.setDateOfBirth(LocalDate.of(1990, 1, 1));
        traveller.setGender("Male");
        traveller.setMobile("9876543210");
        traveller.setEmail("john@example.com");
        traveller.setEmergencyContact("9999999999");
        traveller.setIdDocumentType("AADHAAR");
        traveller.setIdDocumentReference("XXXX1234");

        request = new CreateBookingRequest();
        request.setPackageId(1L);
        request.setNumberOfTravellers(1);
        request.setCustomer(customer);
        request.setTravellers(List.of(traveller));
        request.setConsentAccepted(true);

        lenient().when(bookingProperties.getPaymentWindowMinutes()).thenReturn(30);
    }

    @Test
    void previewBooking_throwsWhenNotEnoughSeats() {
        request.setNumberOfTravellers(5);
        List<TravellerRequest> five = new java.util.ArrayList<>();
        for (int i = 0; i < 5; i++) {
            TravellerRequest t = new TravellerRequest();
            t.setFullName("Traveller " + i);
            t.setDateOfBirth(LocalDate.of(1990, 1, 1));
            t.setGender("Male");
            t.setMobile("9876543210");
            t.setEmail("t" + i + "@example.com");
            t.setEmergencyContact("9999999999");
            t.setIdDocumentType("AADHAAR");
            t.setIdDocumentReference("1234");
            five.add(t);
        }
        request.setTravellers(five);

        when(packageRepository.findById(1L)).thenReturn(Optional.of(pkg));

        assertThatThrownBy(() -> bookingService.previewBooking(request))
                .isInstanceOf(InsufficientSeatsException.class);
    }

    @Test
    void cancelBookingByCustomer_rejectsPaidBooking() {
        Customer customer = Customer.builder()
                .id(1L)
                .fullName("John Doe")
                .email("john@example.com")
                .mobile("9876543210")
                .build();
        Booking booking = Booking.builder()
                .id(50L)
                .bookingReference("JW-2026-00050")
                .customer(customer)
                .travelPackage(pkg)
                .numberOfTravellers(1)
                .status(BookingStatus.CONFIRMED)
                .paymentStatus(PaymentStatus.PAID)
                .build();

        when(bookingRepository.findByBookingReferenceForUpdate("JW-2026-00050"))
                .thenReturn(Optional.of(booking));

        assertThatThrownBy(() ->
                bookingService.cancelBookingByCustomer("JW-2026-00050", "john@example.com"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Paid bookings cannot be cancelled");
    }

    @Test
    void cancelBookingByAdmin_cancelsUnpaidBooking() {
        Customer customer = Customer.builder()
                .id(1L)
                .fullName("John Doe")
                .email("john@example.com")
                .mobile("9876543210")
                .build();
        Booking booking = Booking.builder()
                .id(50L)
                .bookingReference("JW-2026-00050")
                .customer(customer)
                .travelPackage(pkg)
                .numberOfTravellers(1)
                .status(BookingStatus.PENDING_PAYMENT)
                .paymentStatus(PaymentStatus.PENDING)
                .build();
        when(bookingRepository.findById(50L)).thenReturn(Optional.of(booking));
        when(bookingRepository.findByBookingReferenceForUpdate("JW-2026-00050"))
                .thenReturn(Optional.of(booking));
        when(packageRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(pkg));
        when(travellerRepository.findByBookingId(50L)).thenReturn(List.of());
        BookingResponse response = mock(BookingResponse.class);
        when(bookingMapper.toResponse(eq(booking), any())).thenReturn(response);

        bookingService.cancelBookingByAdmin(50L);

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CANCELLED);
    }

    @Test
    void expireStalePendingPaymentBookings_expiresUnpaidAndReleasesSeats() {
        Booking stale = Booking.builder()
                .id(10L)
                .bookingReference("JW-2026-00010")
                .travelPackage(pkg)
                .numberOfTravellers(2)
                .status(BookingStatus.PENDING_PAYMENT)
                .paymentStatus(PaymentStatus.PENDING)
                .build();
        when(bookingRepository.findStalePendingPaymentBookings(any())).thenReturn(List.of(stale));
        when(bookingRepository.findByBookingReferenceForUpdate("JW-2026-00010"))
                .thenReturn(Optional.of(stale));
        when(packageRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(pkg));

        int expired = bookingService.expireStalePendingPaymentBookings();

        assertThat(expired).isEqualTo(1);
        assertThat(stale.getStatus()).isEqualTo(BookingStatus.EXPIRED);
        assertThat(pkg.getAvailableSeats()).isEqualTo(4);
    }

    @Test
    void expireStalePendingPaymentBookings_skipsWhenNoLongerPending() {
        Booking paid = Booking.builder()
                .id(11L)
                .bookingReference("JW-2026-00011")
                .travelPackage(pkg)
                .numberOfTravellers(2)
                .status(BookingStatus.CONFIRMED)
                .paymentStatus(PaymentStatus.PAID)
                .build();
        when(bookingRepository.findStalePendingPaymentBookings(any())).thenReturn(List.of(paid));
        when(bookingRepository.findByBookingReferenceForUpdate("JW-2026-00011"))
                .thenReturn(Optional.of(paid));

        int expired = bookingService.expireStalePendingPaymentBookings();

        assertThat(expired).isEqualTo(0);
        assertThat(pkg.getAvailableSeats()).isEqualTo(2);
        assertThat(paid.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
    }
}
