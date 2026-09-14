package in.joyway.service;

import in.joyway.config.BookingProperties;
import in.joyway.domain.entity.*;
import in.joyway.domain.enums.BookingStatus;
import in.joyway.domain.enums.PackageStatus;
import in.joyway.domain.enums.PaymentStatus;
import in.joyway.domain.repository.BookingRepository;
import in.joyway.domain.repository.CustomerRepository;
import in.joyway.domain.repository.TravellerRepository;
import in.joyway.domain.repository.TravelPackageRepository;
import in.joyway.dto.admin.AdminBookingPaymentMode;
import in.joyway.dto.admin.AdminCreateBookingRequest;
import in.joyway.dto.booking.*;
import in.joyway.exception.AccessDeniedBookingException;
import in.joyway.exception.InsufficientSeatsException;
import in.joyway.exception.ResourceNotFoundException;
import in.joyway.mapper.BookingMapper;
import in.joyway.notification.BookingNotificationService;
import in.joyway.service.audit.AuditLogService;
import in.joyway.service.operations.OperationsNotificationService;
import in.joyway.service.storage.DocumentStorageService;
import in.joyway.util.BookingReferenceGenerator;
import in.joyway.util.IdDocumentReferenceValidator;
import in.joyway.util.PickupPoints;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingService {

    private static final Set<String> ALLOWED_DOCUMENT_TYPES = Set.of(
            "AADHAAR", "PASSPORT", "DRIVING_LICENCE", "OTHER"
    );
    private static final String TRAVELLER_DOC_CATEGORY = "traveller-documents";
    private static final String BOOKING_NOT_FOUND = "Booking not found.";

    private final TravelPackageRepository packageRepository;
    private final CustomerRepository customerRepository;
    private final BookingRepository bookingRepository;
    private final TravellerRepository travellerRepository;
    private final BookingReferenceGenerator referenceGenerator;
    private final BookingMapper bookingMapper;
    private final DocumentStorageService documentStorageService;
    private final BookingNotificationService bookingNotificationService;
    private final BookingProperties bookingProperties;
    private final OperationsNotificationService operationsNotificationService;
    private final WaitlistService waitlistService;
    private final PaymentService paymentService;
    private final AuditLogService auditLogService;
    private final CouponService couponService;

    @Transactional(readOnly = true)
    public BookingReviewDto previewBooking(CreateBookingRequest request) {
        validateBookingRequest(request);
        TravelPackage pkg = findPublishedPackage(request.getPackageId());
        validateBookable(pkg, request.getNumberOfTravellers());
        String pickupPoint = PickupPoints.requireSelection(pkg.getPickupPoints(), request.getPickupPoint());

        BigDecimal subtotal = calculateSubtotal(pkg, request.getNumberOfTravellers());
        BigDecimal taxes = BigDecimal.ZERO;
        CouponService.AppliedCoupon applied = couponService.applyForBooking(
                request.getCouponCode(),
                pkg,
                subtotal,
                request.getCustomer().getEmail(),
                request.getCustomer().getMobile()
        );
        BigDecimal discount = applied != null ? applied.discountAmount() : BigDecimal.ZERO;
        BigDecimal total = applied != null ? applied.totalAmount() : subtotal.add(taxes);

        return BookingReviewDto.builder()
                .packageId(pkg.getId())
                .packageTitle(pkg.getTitle())
                .destination(pkg.getDestination())
                .startDate(pkg.getStartDate())
                .endDate(pkg.getEndDate())
                .duration(pkg.getDuration())
                .numberOfTravellers(request.getNumberOfTravellers())
                .pricePerPerson(pkg.getPricePerPerson())
                .subtotal(subtotal)
                .taxesAndFees(taxes)
                .discountAmount(discount)
                .couponCode(applied != null ? applied.coupon().getCode() : null)
                .totalAmount(total)
                .currency("INR")
                .availableSeats(pkg.getAvailableSeats())
                .bookable(true)
                .pickupPoint(pickupPoint)
                .build();
    }

    @Transactional(rollbackFor = Exception.class)
    public BookingResponse createBooking(CreateBookingRequest request, List<MultipartFile> documents)
            throws IOException {
        if (!request.isConsentAccepted()) {
            throw new IllegalArgumentException(
                    "You must accept the terms, privacy policy, and ID document processing notice.");
        }
        validateBookingRequest(request);
        validateDocuments(request, documents);

        CreatedBooking created = persistNewBooking(request, documents, false, null, null);
        notifyBookingCreatedSafely(created.booking());
        return bookingMapper.toResponse(created.booking(), created.travellers());
    }

    /**
     * Creates a booking from the admin panel (phone / WhatsApp / walk-in).
     * Consent and identity-document files are not required. Seats are still locked
     * against published, upcoming, in-capacity packages.
     */
    @Transactional(rollbackFor = Exception.class)
    public BookingResponse createBookingByAdmin(AdminCreateBookingRequest request) throws IOException {
        if (request.getPaymentMode() == null) {
            throw new IllegalArgumentException("Payment mode is required.");
        }
        if (request.getPaymentMode() == AdminBookingPaymentMode.PAID_OFFLINE
                && isBlank(request.getOfflinePaymentReference())) {
            throw new IllegalArgumentException(
                    "Enter an offline payment reference (UPI, cash receipt, or bank transfer).");
        }
        validateBookingRequest(request);

        String notes = isBlank(request.getNotes()) ? null : request.getNotes().trim();
        String offlineRef = request.getPaymentMode() == AdminBookingPaymentMode.PAID_OFFLINE
                ? request.getOfflinePaymentReference().trim()
                : null;
        CreatedBooking created = persistNewBooking(request, null, true, notes, offlineRef);
        Booking booking = created.booking();
        auditManualBooking(booking, request);

        if (request.getPaymentMode() == AdminBookingPaymentMode.PAID_OFFLINE) {
            paymentService.recordOfflinePayment(
                    booking,
                    request.getOfflinePaymentReference().trim(),
                    request.getNotes());
        } else {
            notifyBookingCreatedSafely(booking);
        }

        return bookingMapper.toResponse(booking, created.travellers());
    }

    /**
     * Expires unpaid bookings past the payment window and releases reserved seats.
     */
    @Transactional
    public int expireStalePendingPaymentBookings() {
        Instant cutoff = Instant.now().minus(
                bookingProperties.getPaymentWindowMinutes(),
                ChronoUnit.MINUTES);
        List<Booking> stale = bookingRepository.findStalePendingPaymentBookings(cutoff);
        int expired = 0;
        for (Booking candidate : stale) {
            try {
                if (expireBookingInternal(candidate.getBookingReference())) {
                    expired++;
                }
            } catch (Exception ex) {
                log.warn("Payment expiry failed for reference={}: {}",
                        candidate.getBookingReference(), ex.getClass().getSimpleName());
            }
        }
        return expired;
    }

    /**
     * Emails unpaid bookings that are still inside the payment window but close to expiry.
     * Duplicate sends are skipped by the email log (template + booking id).
     */
    @Transactional
    public int remindExpiringPendingPaymentBookings() {
        Instant now = Instant.now();
        Instant expiryCutoff = now.minus(bookingProperties.getPaymentWindowMinutes(), ChronoUnit.MINUTES);
        Instant reminderBefore = now.minus(
                bookingProperties.getPaymentWindowMinutes() - bookingProperties.effectiveReminderMinutes(),
                ChronoUnit.MINUTES);
        if (!reminderBefore.isAfter(expiryCutoff)) {
            return 0;
        }
        List<Booking> due = bookingRepository.findPendingPaymentBookingsNearingExpiry(
                expiryCutoff, reminderBefore);
        int sent = 0;
        for (Booking booking : due) {
            try {
                Instant expiresAt = bookingProperties.paymentExpiresAt(booking.getCreatedAt());
                if (expiresAt == null) {
                    continue;
                }
                long remaining = ChronoUnit.MINUTES.between(now, expiresAt);
                bookingNotificationService.onPaymentExpiryReminder(booking, Math.max(1, remaining));
                sent++;
            } catch (RuntimeException ex) {
                log.warn("Payment expiry reminder failed for reference={}: {}",
                        booking.getBookingReference(), ex.getClass().getSimpleName());
            }
        }
        return sent;
    }

    @Transactional
    public int completeEndedConfirmedBookings() {
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Kolkata"));
        return completeConfirmedPaidBookings(bookingRepository.findConfirmedPaidTripsEndedBefore(today));
    }

    @Transactional
    public int completeConfirmedPaidBookingsForPackage(Long packageId) {
        return completeConfirmedPaidBookings(bookingRepository.findConfirmedPaidByPackageId(packageId));
    }

    private int completeConfirmedPaidBookings(List<Booking> bookings) {
        int completed = 0;
        for (Booking booking : bookings) {
            if (booking.getStatus() != BookingStatus.CONFIRMED
                    || booking.getPaymentStatus() != PaymentStatus.PAID) {
                continue;
            }
            booking.setStatus(BookingStatus.COMPLETED);
            bookingRepository.save(booking);
            completed++;
            try {
                bookingNotificationService.onTripCompleted(booking);
            } catch (RuntimeException ex) {
                log.warn("Trip completion notification failed for reference={}: {}",
                        booking.getBookingReference(), ex.getClass().getSimpleName());
            }
            log.info("event=booking.completed bookingReference={}", booking.getBookingReference());
        }
        return completed;
    }

    private boolean expireBookingInternal(String bookingReference) {
        Booking booking = bookingRepository.findByBookingReferenceForUpdate(bookingReference)
                .orElse(null);
        if (booking == null) {
            return false;
        }
        if (booking.getStatus() != BookingStatus.PENDING_PAYMENT
                || booking.getPaymentStatus() != PaymentStatus.PENDING) {
            return false;
        }

        TravelPackage pkg = packageRepository.findByIdForUpdate(booking.getTravelPackage().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Trip not found."));
        pkg.setAvailableSeats(pkg.getAvailableSeats() + booking.getNumberOfTravellers());
        packageRepository.save(pkg);

        booking.setStatus(BookingStatus.EXPIRED);
        bookingRepository.save(booking);

        waitlistService.notifyIfSeatsAvailable(pkg.getId());

        log.info("event=booking.expired bookingReference={}", booking.getBookingReference());
        return true;
    }

    @Transactional
    public BookingResponse cancelBookingByCustomer(String bookingReference, String customerEmail) {
        Booking booking = bookingRepository.findByBookingReferenceForUpdate(bookingReference)
                .orElseThrow(() -> new ResourceNotFoundException(BOOKING_NOT_FOUND));

        String normalizedEmail = customerEmail.trim().toLowerCase();
        if (!booking.getCustomer().getEmail().equalsIgnoreCase(normalizedEmail)) {
            throw new AccessDeniedBookingException("You do not have access to this booking.");
        }

        cancelBookingInternal(booking);
        List<Traveller> travellers = travellerRepository.findByBookingId(booking.getId());
        return bookingMapper.toResponse(booking, travellers);
    }

    @Transactional
    public BookingResponse cancelBookingByAdmin(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException(BOOKING_NOT_FOUND));
        booking = bookingRepository.findByBookingReferenceForUpdate(booking.getBookingReference())
                .orElseThrow(() -> new ResourceNotFoundException(BOOKING_NOT_FOUND));
        cancelBookingInternal(booking);
        List<Traveller> travellers = travellerRepository.findByBookingId(booking.getId());
        return bookingMapper.toResponse(booking, travellers);
    }

    private void cancelBookingInternal(Booking booking) {
        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new IllegalArgumentException("This booking is already cancelled.");
        }
        if (booking.getStatus() == BookingStatus.EXPIRED) {
            throw new IllegalArgumentException("This booking has expired.");
        }
        if (booking.getStatus() == BookingStatus.COMPLETED) {
            throw new IllegalArgumentException("Completed bookings cannot be cancelled.");
        }
        if (booking.getPaymentStatus() == PaymentStatus.PAID
                || booking.getStatus() == BookingStatus.CONFIRMED) {
            throw new IllegalArgumentException(
                    "Paid bookings cannot be cancelled here. Issue a refund from this booking instead.");
        }

        TravelPackage pkg = packageRepository.findByIdForUpdate(booking.getTravelPackage().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Trip not found."));
        pkg.setAvailableSeats(pkg.getAvailableSeats() + booking.getNumberOfTravellers());
        packageRepository.save(pkg);

        booking.setStatus(BookingStatus.CANCELLED);
        bookingRepository.save(booking);

        waitlistService.notifyIfSeatsAvailable(pkg.getId());

        log.info("event=booking.cancelled bookingReference={} paymentStatus={}",
                booking.getBookingReference(), booking.getPaymentStatus());

        try {
            bookingNotificationService.onBookingCancelled(booking);
        } catch (Exception ex) {
            log.warn("Cancellation notification failed for reference={}: {}",
                    booking.getBookingReference(), ex.getClass().getSimpleName());
        }
    }

    private CreatedBooking persistNewBooking(
            CreateBookingRequest request,
            List<MultipartFile> documents,
            boolean createdByAdmin,
            String adminNotes,
            String offlinePaymentReference
    ) throws IOException {
        TravelPackage pkg = packageRepository.findByIdForUpdate(request.getPackageId())
                .orElseThrow(() -> new ResourceNotFoundException("Trip not found or unavailable."));

        validateBookable(pkg, request.getNumberOfTravellers());
        String pickupPoint = PickupPoints.requireSelection(pkg.getPickupPoints(), request.getPickupPoint());

        if (pkg.getAvailableSeats() < request.getNumberOfTravellers()) {
            throw new InsufficientSeatsException("No seats are available for this trip.");
        }

        BigDecimal subtotal = calculateSubtotal(pkg, request.getNumberOfTravellers());
        CouponService.AppliedCoupon applied = couponService.applyForBooking(
                request.getCouponCode(),
                pkg,
                subtotal,
                request.getCustomer().getEmail(),
                request.getCustomer().getMobile()
        );
        BigDecimal discount = applied != null ? applied.discountAmount() : BigDecimal.ZERO;
        BigDecimal totalAmount = applied != null ? applied.totalAmount() : subtotal;

        Customer customer = Customer.builder()
                .fullName(request.getCustomer().getFullName())
                .email(request.getCustomer().getEmail().trim().toLowerCase())
                .mobile(request.getCustomer().getMobile())
                .build();
        customer = customerRepository.save(customer);

        String reference = referenceGenerator.generateNext();

        boolean fullyCoveredByCoupon = totalAmount.compareTo(BigDecimal.ZERO) <= 0;
        Booking booking = Booking.builder()
                .bookingReference(reference)
                .travelPackage(pkg)
                .customer(customer)
                .numberOfTravellers(request.getNumberOfTravellers())
                .pickupPoint(pickupPoint)
                .totalAmount(totalAmount)
                .discountAmount(discount)
                .coupon(applied != null ? applied.coupon() : null)
                .couponCode(applied != null ? applied.coupon().getCode() : null)
                .currency("INR")
                .status(fullyCoveredByCoupon ? BookingStatus.CONFIRMED : BookingStatus.PENDING_PAYMENT)
                .paymentStatus(fullyCoveredByCoupon ? PaymentStatus.PAID : PaymentStatus.PENDING)
                .createdByAdmin(createdByAdmin)
                .adminNotes(adminNotes)
                .offlinePaymentReference(offlinePaymentReference)
                .build();
        booking = bookingRepository.save(booking);

        if (applied != null) {
            couponService.redeem(applied, booking, customer.getEmail(), customer.getMobile());
        }

        pkg.setAvailableSeats(pkg.getAvailableSeats() - request.getNumberOfTravellers());
        packageRepository.save(pkg);

        List<Traveller> savedTravellers = saveTravellers(booking, request, documents);

        log.info("event=booking.created bookingReference={} packageId={} travellers={}",
                reference, pkg.getId(), request.getNumberOfTravellers());

        boolean uploadedDocuments = savedTravellers.stream()
                .anyMatch(t -> t.getPrivateDocumentStorageKey() != null);
        if (uploadedDocuments) {
            operationsNotificationService.notifyDocumentUploaded(reference, booking.getId());
        }

        return new CreatedBooking(booking, savedTravellers);
    }

    private List<Traveller> saveTravellers(
            Booking booking,
            CreateBookingRequest request,
            List<MultipartFile> documents
    ) throws IOException {
        List<Traveller> savedTravellers = new ArrayList<>();
        for (int i = 0; i < request.getTravellers().size(); i++) {
            TravellerRequest tr = request.getTravellers().get(i);
            String storageKey = null;

            if (documents != null && i < documents.size() && documents.get(i) != null
                    && !documents.get(i).isEmpty()) {
                storageKey = documentStorageService.store(documents.get(i), TRAVELLER_DOC_CATEGORY);
            }

            Traveller traveller = Traveller.builder()
                    .booking(booking)
                    .fullName(tr.getFullName())
                    .dateOfBirth(tr.getDateOfBirth())
                    .gender(tr.getGender())
                    .mobile(tr.getMobile())
                    .email(tr.getEmail().trim().toLowerCase())
                    .emergencyContact(tr.getEmergencyContact())
                    .idDocumentType(tr.getIdDocumentType())
                    .idDocumentReference(tr.getIdDocumentReference())
                    .privateDocumentStorageKey(storageKey)
                    .build();
            savedTravellers.add(travellerRepository.save(traveller));
        }
        return savedTravellers;
    }

    private void notifyBookingCreatedSafely(Booking booking) {
        try {
            bookingNotificationService.onBookingCreated(booking);
        } catch (Exception ex) {
            log.warn("Booking notification failed for reference={}: {}",
                    booking.getBookingReference(), ex.getClass().getSimpleName());
        }
    }

    private void auditManualBooking(Booking booking, AdminCreateBookingRequest request) {
        StringBuilder summary = new StringBuilder("Manual booking ")
                .append(booking.getBookingReference())
                .append(" (")
                .append(request.getPaymentMode())
                .append(')');
        if (!isBlank(request.getNotes())) {
            summary.append(". ").append(request.getNotes().trim());
        }
        auditLogService.log("ADMIN_BOOKING_CREATED", "BOOKING", booking.getId(), summary.toString());
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private record CreatedBooking(Booking booking, List<Traveller> travellers) {
    }

    private void validateBookingRequest(CreateBookingRequest request) {
        if (!request.getNumberOfTravellers().equals(request.getTravellers().size())) {
            throw new IllegalArgumentException(
                    "Number of travellers must match the traveller details provided.");
        }

        for (TravellerRequest traveller : request.getTravellers()) {
            if (!ALLOWED_DOCUMENT_TYPES.contains(traveller.getIdDocumentType().toUpperCase())) {
                throw new IllegalArgumentException("Invalid ID document type: " + traveller.getIdDocumentType());
            }
            IdDocumentReferenceValidator.validate(
                    traveller.getIdDocumentType(),
                    traveller.getIdDocumentReference());
        }
    }

    private void validateDocuments(CreateBookingRequest request, List<MultipartFile> documents) {
        if (documents == null) {
            throw new IllegalArgumentException("Identity documents are required for all travellers.");
        }
        if (documents.size() != request.getTravellers().size()) {
            throw new IllegalArgumentException(
                    "One identity document file is required for each traveller.");
        }
        for (int i = 0; i < documents.size(); i++) {
            MultipartFile file = documents.get(i);
            if (file == null || file.isEmpty()) {
                throw new IllegalArgumentException(
                        "Identity document is required for traveller " + (i + 1) + ".");
            }
        }
    }

    private TravelPackage findPublishedPackage(Long packageId) {
        TravelPackage pkg = packageRepository.findById(packageId)
                .orElseThrow(() -> new ResourceNotFoundException("Trip not found or unavailable."));
        if (pkg.getStatus() != PackageStatus.PUBLISHED) {
            throw new ResourceNotFoundException("Trip not found or unavailable.");
        }
        return pkg;
    }

    private void validateBookable(TravelPackage pkg, int travellers) {
        if (pkg.getStatus() != PackageStatus.PUBLISHED) {
            throw new IllegalArgumentException("This trip is not available for booking.");
        }
        if (pkg.getStartDate().isBefore(LocalDate.now(ZoneId.of("Asia/Kolkata")))) {
            throw new IllegalArgumentException("This trip date has passed.");
        }
        if (pkg.getAvailableSeats() < travellers) {
            throw new InsufficientSeatsException("No seats are available for this trip.");
        }
    }

    private BigDecimal calculateSubtotal(TravelPackage pkg, int numberOfTravellers) {
        return pkg.getPricePerPerson()
                .multiply(BigDecimal.valueOf(numberOfTravellers))
                .setScale(2, RoundingMode.HALF_UP);
    }
}
