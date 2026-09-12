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
import in.joyway.dto.admin.AdminBookingPaymentMode;
import in.joyway.dto.admin.AdminCreateBookingRequest;
import in.joyway.dto.booking.BookingResponse;
import in.joyway.dto.booking.CreateBookingRequest;
import in.joyway.dto.booking.CustomerRequest;
import in.joyway.dto.booking.TravellerRequest;
import in.joyway.exception.AccessDeniedBookingException;
import in.joyway.exception.InsufficientSeatsException;
import in.joyway.exception.ResourceNotFoundException;
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
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

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
    @Mock private WaitlistService waitlistService;
    @Mock private PaymentService paymentService;
    @Mock private in.joyway.service.audit.AuditLogService auditLogService;

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
        lenient().when(bookingProperties.effectiveReminderMinutes()).thenReturn(10);
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
        verify(waitlistService).notifyIfSeatsAvailable(1L);
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

        assertThat(expired).isZero();
        assertThat(pkg.getAvailableSeats()).isEqualTo(2);
        assertThat(paid.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
    }

    @Test
    void remindExpiringPendingPaymentBookings_notifiesDueBookings() {
        Instant createdAt = Instant.now().minusSeconds(21 * 60);
        Booking due = Booking.builder()
                .id(12L)
                .bookingReference("JW-2026-00012")
                .travelPackage(pkg)
                .numberOfTravellers(1)
                .status(BookingStatus.PENDING_PAYMENT)
                .paymentStatus(PaymentStatus.PENDING)
                .createdAt(createdAt)
                .build();
        when(bookingRepository.findPendingPaymentBookingsNearingExpiry(any(), any()))
                .thenReturn(List.of(due));
        when(bookingProperties.paymentExpiresAt(createdAt)).thenReturn(createdAt.plusSeconds(30 * 60));

        int sent = bookingService.remindExpiringPendingPaymentBookings();

        assertThat(sent).isEqualTo(1);
        verify(bookingNotificationService).onPaymentExpiryReminder(eq(due), anyLong());
    }

    @Test
    void previewBooking_requiresListedPickupPoint() {
        pkg.setPickupPoints("Kashmere Gate, CP");
        when(packageRepository.findById(1L)).thenReturn(Optional.of(pkg));

        assertThatThrownBy(() -> bookingService.previewBooking(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("pickup point");

        request.setPickupPoint("CP");
        var preview = bookingService.previewBooking(request);
        assertThat(preview.pickupPoint()).isEqualTo("CP");
    }

    @Test
    void completeEndedConfirmedBookings_marksPaidConfirmedTripsCompleted() {
        Booking due = Booking.builder()
                .id(20L)
                .bookingReference("JW-2026-00020")
                .customer(Customer.builder().fullName("Asha").email("asha@example.com").build())
                .travelPackage(pkg)
                .numberOfTravellers(1)
                .status(BookingStatus.CONFIRMED)
                .paymentStatus(PaymentStatus.PAID)
                .build();
        when(bookingRepository.findConfirmedPaidTripsEndedBefore(any())).thenReturn(List.of(due));

        int completed = bookingService.completeEndedConfirmedBookings();

        assertThat(completed).isEqualTo(1);
        assertThat(due.getStatus()).isEqualTo(BookingStatus.COMPLETED);
        verify(bookingNotificationService).onTripCompleted(due);
    }

    @Test
    void createBooking_storesPickupPointAndDocument() throws Exception {
        request.setPickupPoint("CP");
        pkg.setPickupPoints("Kashmere Gate, CP");
        MockMultipartFile document = new MockMultipartFile(
                "documents", "id.pdf", "application/pdf", "pdf".getBytes());
        when(packageRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(pkg));
        when(customerRepository.save(any())).thenAnswer(inv -> {
            Customer customer = inv.getArgument(0);
            customer.setId(7L);
            return customer;
        });
        when(referenceGenerator.generateNext()).thenReturn("JW-2026-00099");
        when(bookingRepository.save(any())).thenAnswer(inv -> {
            Booking booking = inv.getArgument(0);
            booking.setId(99L);
            return booking;
        });
        when(documentStorageService.store(document, "traveller-documents")).thenReturn("docs/id.pdf");
        when(travellerRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        BookingResponse response = mock(BookingResponse.class);
        when(bookingMapper.toResponse(any(), any())).thenReturn(response);

        assertThat(bookingService.createBooking(request, List.of(document))).isSameAs(response);
        assertThat(pkg.getAvailableSeats()).isEqualTo(1);
        verify(documentStorageService).store(document, "traveller-documents");
        verify(operationsNotificationService).notifyDocumentUploaded("JW-2026-00099", 99L);
        verify(bookingNotificationService).onBookingCreated(any());
    }

    @Test
    void createBooking_swallowsNotificationFailure() throws Exception {
        MockMultipartFile document = new MockMultipartFile(
                "documents", "id.pdf", "application/pdf", "pdf".getBytes());
        when(packageRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(pkg));
        when(customerRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(referenceGenerator.generateNext()).thenReturn("JW-2026-00100");
        when(bookingRepository.save(any())).thenAnswer(inv -> {
            Booking booking = inv.getArgument(0);
            booking.setId(100L);
            return booking;
        });
        when(documentStorageService.store(any(), any())).thenReturn("docs/id.pdf");
        when(travellerRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        BookingResponse response = mock(BookingResponse.class);
        when(bookingMapper.toResponse(any(), any())).thenReturn(response);
        doThrow(new RuntimeException("smtp")).when(bookingNotificationService).onBookingCreated(any());

        bookingService.createBooking(request, List.of(document));

        verify(bookingMapper).toResponse(any(), any());
    }

    @Test
    void createBooking_rejectsWhenLockedSeatsAreGone() {
        pkg.setAvailableSeats(0);
        MockMultipartFile document = new MockMultipartFile(
                "documents", "id.pdf", "application/pdf", "pdf".getBytes());
        when(packageRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(pkg));
        List<MultipartFile> documents = List.of(document);

        assertThatThrownBy(() -> bookingService.createBooking(request, documents))
                .isInstanceOf(InsufficientSeatsException.class);
    }

    @Test
    void createBooking_rejectsUnpublishedPackage() {
        pkg.setStatus(PackageStatus.DRAFT);
        MockMultipartFile document = new MockMultipartFile(
                "documents", "id.pdf", "application/pdf", "pdf".getBytes());
        when(packageRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(pkg));
        List<MultipartFile> documents = List.of(document);

        assertThatThrownBy(() -> bookingService.createBooking(request, documents))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not available");
    }

    @Test
    void previewBooking_rejectsUnpublishedOrPastTrips() {
        pkg.setStatus(PackageStatus.DRAFT);
        when(packageRepository.findById(1L)).thenReturn(Optional.of(pkg));
        assertThatThrownBy(() -> bookingService.previewBooking(request))
                .isInstanceOf(ResourceNotFoundException.class);

        pkg.setStatus(PackageStatus.PUBLISHED);
        pkg.setStartDate(LocalDate.now().minusDays(1));
        when(packageRepository.findById(1L)).thenReturn(Optional.of(pkg));
        assertThatThrownBy(() -> bookingService.previewBooking(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("passed");
    }

    @Test
    void createBooking_rejectsMismatchedTravellersDocumentsAndIdType() {
        request.setNumberOfTravellers(2);
        assertThatThrownBy(() -> bookingService.createBooking(request, List.of()))
                .hasMessageContaining("must match the traveller details");

        request.setNumberOfTravellers(1);
        request.getTravellers().getFirst().setIdDocumentType("PAN");
        assertThatThrownBy(() -> bookingService.createBooking(request, List.of()))
                .hasMessageContaining("Invalid ID document type");

        request.getTravellers().getFirst().setIdDocumentType("AADHAAR");
        MockMultipartFile document = new MockMultipartFile(
                "documents", "id.pdf", "application/pdf", "pdf".getBytes());
        List<MultipartFile> extraDocuments = List.of(document, document);
        assertThatThrownBy(() -> bookingService.createBooking(request, extraDocuments))
                .hasMessageContaining("One identity document file is required");
    }

    @Test
    void createBooking_rejectsMissingConsentAndEmptyDocuments() {
        request.setConsentAccepted(false);
        assertThatThrownBy(() -> bookingService.createBooking(request, List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("accept the terms");

        request.setConsentAccepted(true);
        MockMultipartFile empty = new MockMultipartFile("documents", "id.pdf", "application/pdf", new byte[0]);
        List<MultipartFile> emptyDocuments = List.of(empty);
        assertThatThrownBy(() -> bookingService.createBooking(request, emptyDocuments))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Identity document is required");
        assertThatThrownBy(() -> bookingService.createBooking(request, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Identity documents are required");
    }

    @Test
    void remindExpiringPendingPaymentBookings_skipsWhenWindowInvalidOrExpiresAtMissing() {
        when(bookingProperties.effectiveReminderMinutes()).thenReturn(0);
        assertThat(bookingService.remindExpiringPendingPaymentBookings()).isZero();

        when(bookingProperties.effectiveReminderMinutes()).thenReturn(10);
        Booking due = Booking.builder()
                .id(12L)
                .bookingReference("JW-2026-00012")
                .createdAt(Instant.now())
                .build();
        when(bookingRepository.findPendingPaymentBookingsNearingExpiry(any(), any()))
                .thenReturn(List.of(due));
        when(bookingProperties.paymentExpiresAt(due.getCreatedAt())).thenReturn(null);

        assertThat(bookingService.remindExpiringPendingPaymentBookings()).isZero();
        verify(bookingNotificationService, never()).onPaymentExpiryReminder(any(), anyLong());
    }

    @Test
    void remindExpiringPendingPaymentBookings_continuesWhenNotificationFails() {
        Instant createdAt = Instant.now().minusSeconds(21 * 60);
        Booking due = Booking.builder()
                .id(12L)
                .bookingReference("JW-2026-00012")
                .createdAt(createdAt)
                .build();
        when(bookingRepository.findPendingPaymentBookingsNearingExpiry(any(), any()))
                .thenReturn(List.of(due));
        when(bookingProperties.paymentExpiresAt(createdAt)).thenReturn(createdAt.plusSeconds(30 * 60));
        doThrow(new RuntimeException("smtp")).when(bookingNotificationService)
                .onPaymentExpiryReminder(eq(due), anyLong());

        assertThat(bookingService.remindExpiringPendingPaymentBookings()).isZero();
    }

    @Test
    void completeConfirmedPaidBookingsForPackage_skipsUnpaidAndSwallowsNotifyFailure() {
        Booking unpaid = Booking.builder()
                .id(21L)
                .bookingReference("JW-2026-00021")
                .status(BookingStatus.PENDING_PAYMENT)
                .paymentStatus(PaymentStatus.PENDING)
                .build();
        Booking paid = Booking.builder()
                .id(22L)
                .bookingReference("JW-2026-00022")
                .status(BookingStatus.CONFIRMED)
                .paymentStatus(PaymentStatus.PAID)
                .build();
        when(bookingRepository.findConfirmedPaidByPackageId(1L)).thenReturn(List.of(unpaid, paid));
        doThrow(new RuntimeException("smtp")).when(bookingNotificationService).onTripCompleted(paid);

        assertThat(bookingService.completeConfirmedPaidBookingsForPackage(1L)).isEqualTo(1);
        assertThat(paid.getStatus()).isEqualTo(BookingStatus.COMPLETED);
        assertThat(unpaid.getStatus()).isEqualTo(BookingStatus.PENDING_PAYMENT);
    }

    @Test
    void expireStalePendingPaymentBookings_skipsMissingAndFailedCandidates() {
        Booking missing = Booking.builder().bookingReference("JW-2026-00030").build();
        Booking failing = Booking.builder()
                .bookingReference("JW-2026-00031")
                .travelPackage(pkg)
                .status(BookingStatus.PENDING_PAYMENT)
                .paymentStatus(PaymentStatus.PENDING)
                .build();
        when(bookingRepository.findStalePendingPaymentBookings(any())).thenReturn(List.of(missing, failing));
        when(bookingRepository.findByBookingReferenceForUpdate("JW-2026-00030")).thenReturn(Optional.empty());
        when(bookingRepository.findByBookingReferenceForUpdate("JW-2026-00031")).thenReturn(Optional.of(failing));
        when(packageRepository.findByIdForUpdate(1L)).thenReturn(Optional.empty());

        assertThat(bookingService.expireStalePendingPaymentBookings()).isZero();
    }

    @Test
    void cancelBookingByCustomer_cancelsUnpaidAndNotifiesWaitlist() {
        Customer customer = Customer.builder()
                .fullName("John Doe")
                .email("john@example.com")
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
        when(bookingRepository.findByBookingReferenceForUpdate("JW-2026-00050"))
                .thenReturn(Optional.of(booking));
        when(packageRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(pkg));
        when(travellerRepository.findByBookingId(50L)).thenReturn(List.of());
        BookingResponse response = mock(BookingResponse.class);
        when(bookingMapper.toResponse(eq(booking), any())).thenReturn(response);
        doThrow(new RuntimeException("smtp")).when(bookingNotificationService).onBookingCancelled(booking);

        assertThat(bookingService.cancelBookingByCustomer("JW-2026-00050", "john@example.com"))
                .isSameAs(response);
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CANCELLED);
        verify(waitlistService).notifyIfSeatsAvailable(1L);
        verify(bookingNotificationService).onBookingCancelled(booking);
    }

    @Test
    void cancelBookingByCustomer_rejectsWrongEmailAndTerminalStatuses() {
        Customer customer = Customer.builder().email("john@example.com").build();
        Booking booking = Booking.builder()
                .bookingReference("JW-2026-00050")
                .customer(customer)
                .status(BookingStatus.PENDING_PAYMENT)
                .paymentStatus(PaymentStatus.PENDING)
                .build();
        when(bookingRepository.findByBookingReferenceForUpdate("JW-2026-00050"))
                .thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> bookingService.cancelBookingByCustomer("JW-2026-00050", "other@example.com"))
                .isInstanceOf(AccessDeniedBookingException.class);

        booking.setStatus(BookingStatus.CANCELLED);
        assertThatThrownBy(() -> bookingService.cancelBookingByCustomer("JW-2026-00050", "john@example.com"))
                .hasMessageContaining("already cancelled");

        booking.setStatus(BookingStatus.EXPIRED);
        assertThatThrownBy(() -> bookingService.cancelBookingByCustomer("JW-2026-00050", "john@example.com"))
                .hasMessageContaining("expired");

        booking.setStatus(BookingStatus.COMPLETED);
        assertThatThrownBy(() -> bookingService.cancelBookingByCustomer("JW-2026-00050", "john@example.com"))
                .hasMessageContaining("Completed bookings");
    }

    @Test
    void createBookingByAdmin_pendingOnline_skipsConsentAndDocuments() throws Exception {
        request.setConsentAccepted(false);
        AdminCreateBookingRequest admin = adminRequest(AdminBookingPaymentMode.PENDING_ONLINE);
        stubCreatePath("JW-2026-00200", 200L);
        BookingResponse response = mock(BookingResponse.class);
        when(bookingMapper.toResponse(any(), any())).thenReturn(response);

        assertThat(bookingService.createBookingByAdmin(admin)).isSameAs(response);
        assertThat(pkg.getAvailableSeats()).isEqualTo(1);
        org.mockito.ArgumentCaptor<Booking> bookingCaptor =
                org.mockito.ArgumentCaptor.forClass(Booking.class);
        verify(bookingRepository).save(bookingCaptor.capture());
        assertThat(bookingCaptor.getValue().isCreatedByAdmin()).isTrue();
        verify(paymentService, never()).recordOfflinePayment(any(), any(), any());
        verify(bookingNotificationService).onBookingCreated(any());
        verify(auditLogService).log(eq("ADMIN_BOOKING_CREATED"), eq("BOOKING"), eq(200L), any());
        verify(documentStorageService, never()).store(any(), any());
    }

    @Test
    void createBookingByAdmin_offlinePaid_recordsPayment() throws Exception {
        AdminCreateBookingRequest admin = adminRequest(AdminBookingPaymentMode.PAID_OFFLINE);
        admin.setOfflinePaymentReference("UPI-8899");
        admin.setNotes("Paid at office");
        stubCreatePath("JW-2026-00201", 201L);
        BookingResponse response = mock(BookingResponse.class);
        when(bookingMapper.toResponse(any(), any())).thenReturn(response);

        assertThat(bookingService.createBookingByAdmin(admin)).isSameAs(response);
        verify(paymentService).recordOfflinePayment(any(), eq("UPI-8899"), eq("Paid at office"));
        verify(bookingNotificationService, never()).onBookingCreated(any());
    }

    @Test
    void createBookingByAdmin_rejectsMissingOfflineReferenceAndUnpublished() {
        AdminCreateBookingRequest admin = adminRequest(AdminBookingPaymentMode.PAID_OFFLINE);
        assertThatThrownBy(() -> bookingService.createBookingByAdmin(admin))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("offline payment reference");
        verify(packageRepository, never()).findByIdForUpdate(anyLong());

        admin.setOfflinePaymentReference("CASH-1");
        pkg.setStatus(PackageStatus.DRAFT);
        when(packageRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(pkg));
        assertThatThrownBy(() -> bookingService.createBookingByAdmin(admin))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not available");
    }

    private AdminCreateBookingRequest adminRequest(AdminBookingPaymentMode mode) {
        AdminCreateBookingRequest admin = new AdminCreateBookingRequest();
        admin.setPackageId(request.getPackageId());
        admin.setNumberOfTravellers(request.getNumberOfTravellers());
        admin.setCustomer(request.getCustomer());
        admin.setTravellers(request.getTravellers());
        admin.setPickupPoint(request.getPickupPoint());
        admin.setPaymentMode(mode);
        return admin;
    }

    private void stubCreatePath(String reference, long bookingId) {
        when(packageRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(pkg));
        when(customerRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(referenceGenerator.generateNext()).thenReturn(reference);
        when(bookingRepository.save(any())).thenAnswer(inv -> {
            Booking booking = inv.getArgument(0);
            booking.setId(bookingId);
            return booking;
        });
        when(travellerRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }
}
