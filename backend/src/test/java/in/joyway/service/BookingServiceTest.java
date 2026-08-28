package in.joyway.service;

import in.joyway.domain.entity.TravelPackage;
import in.joyway.domain.enums.PackageStatus;
import in.joyway.domain.repository.BookingRepository;
import in.joyway.domain.repository.CustomerRepository;
import in.joyway.domain.repository.TravellerRepository;
import in.joyway.domain.repository.TravelPackageRepository;
import in.joyway.dto.booking.CreateBookingRequest;
import in.joyway.dto.booking.CustomerRequest;
import in.joyway.dto.booking.TravellerRequest;
import in.joyway.exception.InsufficientSeatsException;
import in.joyway.mapper.BookingMapper;
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

import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
            t.setIdDocumentReference("XXXX" + i);
            five.add(t);
        }
        request.setTravellers(five);

        when(packageRepository.findById(1L)).thenReturn(Optional.of(pkg));

        assertThatThrownBy(() -> bookingService.previewBooking(request))
                .isInstanceOf(InsufficientSeatsException.class);
    }
}
