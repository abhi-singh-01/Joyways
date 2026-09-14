package in.joyway.service.operations;

import in.joyway.domain.entity.*;
import in.joyway.domain.enums.*;
import in.joyway.domain.repository.*;
import in.joyway.dto.PageResponse;
import in.joyway.dto.admin.operations.*;
import in.joyway.exception.OperationConflictException;
import in.joyway.exception.ResourceNotFoundException;
import in.joyway.service.BookingService;
import in.joyway.service.audit.AuditLogService;
import in.joyway.util.TripReferenceGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class AdminTripService {

    private static final Set<TripStatus> ACTIVE_TRIP_STATUSES = EnumSet.of(
            TripStatus.PLANNED,
            TripStatus.ASSIGNED,
            TripStatus.READY,
            TripStatus.IN_PROGRESS
    );

    private static final Set<TripStatus> EDITABLE_STATUSES = EnumSet.of(
            TripStatus.PLANNED,
            TripStatus.ASSIGNED,
            TripStatus.READY
    );
    private static final String TRIP_LABEL = "Trip ";
    private static final String ASSIGNED_TO = " assigned to ";

    private static final Pageable SINGLE_RESULT = PageRequest.of(0, 1);

    private final TripRepository tripRepository;
    private final TripBookingRepository tripBookingRepository;
    private final TripVehicleRepository tripVehicleRepository;
    private final VehicleRepository vehicleRepository;
    private final DriverRepository driverRepository;
    private final BookingRepository bookingRepository;
    private final TravelPackageRepository travelPackageRepository;
    private final TripReferenceGenerator tripReferenceGenerator;
    private final AuditLogService auditLogService;
    private final OperationsNotificationService operationsNotificationService;
    private final BookingService bookingService;

    @Transactional(readOnly = true)
    public PageResponse<TripSummaryDto> listTrips(
            String query,
            String status,
            LocalDate travelDate,
            Long vehicleId,
            Long driverId,
            int page,
            int size
    ) {
        Page<Trip> result = tripRepository.search(
                blankToEmpty(query),
                parseTripStatus(status),
                travelDate,
                vehicleId,
                driverId,
                pageable(page, size)
        );
        return PageResponse.of(new PageImpl<>(
                toSummaries(result.getContent()), result.getPageable(), result.getTotalElements()));
    }

    @Transactional(readOnly = true)
    public List<TripSummaryDto> listCalendarTrips(LocalDate fromDate, LocalDate toDate) {
        if (fromDate == null || toDate == null) {
            throw new IllegalArgumentException("Date range is required.");
        }
        if (toDate.isBefore(fromDate)) {
            throw new IllegalArgumentException("End date must be on or after start date.");
        }
        long days = toDate.toEpochDay() - fromDate.toEpochDay();
        if (days > 93) {
            throw new IllegalArgumentException("Date range cannot exceed 93 days.");
        }
        return toSummaries(tripRepository.findByTravelDateBetween(fromDate, toDate));
    }

    @Transactional(readOnly = true)
    public List<TripSummaryDto> listUpcomingTrips(int limit) {
        int safeLimit = Math.clamp(limit, 1, 20);
        LocalDate today = LicenseStatusHelper.businessToday();
        return toSummaries(tripRepository.findUpcoming(today, PageRequest.of(0, safeLimit)));
    }

    @Transactional(readOnly = true)
    public TripDetailDto getTrip(Long id) {
        Trip trip = findTripWithDetails(id);
        return toDetail(trip);
    }

    public TripDetailDto createTrip(CreateTripRequest request) {
        validateTimes(request.getStartAt(), request.getEndAt());

        TravelPackage travelPackage = travelPackageRepository.findById(request.getPackageId())
                .orElseThrow(() -> new ResourceNotFoundException("Package not found."));

        Trip trip = Trip.builder()
                .tripReference(tripReferenceGenerator.generateNext())
                .travelPackage(travelPackage)
                .travelDate(request.getTravelDate())
                .startAt(request.getStartAt())
                .endAt(request.getEndAt())
                .pickupLocation(trimToNull(request.getPickupLocation()))
                .dropLocation(trimToNull(request.getDropLocation()))
                .destination(travelPackage.getDestination())
                .status(TripStatus.PLANNED)
                .notes(trimToNull(request.getNotes()))
                .build();

        trip = tripRepository.save(trip);
        alignPackageScheduleToTrip(travelPackage, request.getTravelDate());
        auditLogService.log("TRIP_CREATED", "TRIP", trip.getId(),
                TRIP_LABEL + trip.getTripReference() + " created for " + trip.getDestination() + ".");
        operationsNotificationService.notifyTripCreated(
                trip.getTripReference(), trip.getDestination(), trip.getId());
        return toDetail(findTripWithDetails(trip.getId()));
    }

    public TripDetailDto updateTrip(Long id, UpdateTripRequest request) {
        Trip trip = findTripForUpdate(id);
        if (!EDITABLE_STATUSES.contains(trip.getStatus())) {
            throw new OperationConflictException("Trip cannot be edited in its current status.");
        }

        validateTimes(request.getStartAt(), request.getEndAt());

        TravelPackage travelPackage = travelPackageRepository.findById(request.getPackageId())
                .orElseThrow(() -> new ResourceNotFoundException("Package not found."));

        trip.setTravelPackage(travelPackage);
        trip.setTravelDate(request.getTravelDate());
        trip.setStartAt(request.getStartAt());
        trip.setEndAt(request.getEndAt());
        trip.setPickupLocation(trimToNull(request.getPickupLocation()));
        trip.setDropLocation(trimToNull(request.getDropLocation()));
        trip.setDestination(travelPackage.getDestination());
        trip.setNotes(trimToNull(request.getNotes()));

        trip = tripRepository.save(trip);
        alignPackageScheduleToTrip(travelPackage, request.getTravelDate());
        auditLogService.log("TRIP_UPDATED", "TRIP", trip.getId(),
                TRIP_LABEL + trip.getTripReference() + " updated.");
        return toDetail(findTripWithDetails(trip.getId()));
    }

    public TripDetailDto assignVehicle(Long tripId, AssignVehicleRequest request) {
        Trip trip = findTripForUpdate(tripId);
        if (trip.getStatus() == TripStatus.COMPLETED || trip.getStatus() == TripStatus.CANCELLED) {
            throw new OperationConflictException("Cannot assign a vehicle to a completed or cancelled trip.");
        }

        Vehicle vehicle = vehicleRepository.findByIdForUpdate(request.getVehicleId())
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found."));

        validateVehicleAssignable(vehicle, trip);

        if (tripRepository.existsVehicleConflict(trip.getId(), vehicle.getId(), trip.getStartAt(), trip.getEndAt())) {
            throw new OperationConflictException("Vehicle is already assigned to an overlapping trip.");
        }

        int travellers = tripBookingRepository.sumTravellersByTripId(trip.getId());
        if (travellers > vehicle.getSeatingCapacity()) {
            throw new OperationConflictException(
                    "Assigned travellers (" + travellers + ") exceed vehicle capacity ("
                            + vehicle.getSeatingCapacity() + ").");
        }

        final Trip workingTrip = trip;
        final Long currentTripId = workingTrip.getId();
        tripVehicleRepository.findPrimaryVehicle(currentTripId).ifPresent(existing -> {
            Vehicle previous = vehicleRepository.findByIdForUpdate(existing.getVehicle().getId()).orElseThrow();
            if (!tripRepository.hasOtherActiveTripsForVehicle(previous.getId(), currentTripId)) {
                previous.setStatus(VehicleStatus.AVAILABLE);
                vehicleRepository.save(previous);
            }
            workingTrip.getTripVehicles().remove(existing);
            tripVehicleRepository.delete(existing);
        });

        TripVehicle tripVehicle = TripVehicle.builder()
                .trip(workingTrip)
                .vehicle(vehicle)
                .primary(true)
                .build();
        workingTrip.getTripVehicles().add(tripVehicle);
        tripVehicleRepository.save(tripVehicle);

        if (vehicle.getStatus() == VehicleStatus.AVAILABLE) {
            vehicle.setStatus(VehicleStatus.ASSIGNED);
            vehicleRepository.save(vehicle);
        }

        maybeAdvanceToAssigned(trip);
        trip = tripRepository.save(trip);

        auditLogService.log("VEHICLE_ASSIGNED", "TRIP", trip.getId(),
                "Vehicle " + vehicle.getRegistrationNumber() + ASSIGNED_TO + trip.getTripReference() + ".");
        operationsNotificationService.notifyVehicleAssigned(
                trip.getTripReference(), vehicle.getRegistrationNumber(), trip.getId());
        return toDetail(findTripWithDetails(trip.getId()));
    }

    public TripDetailDto assignDriver(Long tripId, AssignDriverRequest request) {
        Trip trip = findTripForUpdate(tripId);
        if (trip.getStatus() == TripStatus.COMPLETED || trip.getStatus() == TripStatus.CANCELLED) {
            throw new OperationConflictException("Cannot assign a driver to a completed or cancelled trip.");
        }

        Driver driver = driverRepository.findByIdForUpdate(request.getDriverId())
                .orElseThrow(() -> new ResourceNotFoundException("Driver not found."));

        validateDriverAssignable(driver, trip);

        if (tripRepository.existsDriverConflict(trip.getId(), driver.getId(), trip.getStartAt(), trip.getEndAt())) {
            throw new OperationConflictException("Driver is already assigned to an overlapping trip.");
        }

        if (trip.getDriver() != null && !trip.getDriver().getId().equals(driver.getId())) {
            Driver previous = driverRepository.findByIdForUpdate(trip.getDriver().getId()).orElseThrow();
            if (!tripRepository.hasOtherActiveTripsForDriver(previous.getId(), trip.getId())) {
                previous.setStatus(DriverStatus.AVAILABLE);
                driverRepository.save(previous);
            }
        }

        trip.setDriver(driver);
        if (driver.getStatus() == DriverStatus.AVAILABLE) {
            driver.setStatus(DriverStatus.ASSIGNED);
            driverRepository.save(driver);
        }

        maybeAdvanceToAssigned(trip);
        trip = tripRepository.save(trip);

        auditLogService.log("DRIVER_ASSIGNED", "TRIP", trip.getId(),
                "Driver " + driver.getName() + ASSIGNED_TO + trip.getTripReference() + ".");
        operationsNotificationService.notifyDriverAssigned(
                trip.getTripReference(), driver.getName(), trip.getId());
        return toDetail(findTripWithDetails(trip.getId()));
    }

    public TripDetailDto addBooking(Long tripId, AssignBookingToTripRequest request) {
        Trip trip = findTripForUpdate(tripId);
        if (!EDITABLE_STATUSES.contains(trip.getStatus())) {
            throw new OperationConflictException("Bookings cannot be added in the current trip status.");
        }

        if (tripBookingRepository.existsByBookingId(request.getBookingId())) {
            throw new OperationConflictException("Booking is already assigned to a trip.");
        }

        Booking booking = bookingRepository.findByIdWithDetails(request.getBookingId())
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found."));

        validateBookingAssignable(booking, trip);

        int currentTravellers = tripBookingRepository.sumTravellersByTripId(trip.getId());
        int newTotal = currentTravellers + booking.getNumberOfTravellers();

        Optional<TripVehicle> primaryVehicle = tripVehicleRepository.findPrimaryVehicle(trip.getId());
        if (primaryVehicle.isPresent()) {
            int capacity = primaryVehicle.get().getVehicle().getSeatingCapacity();
            if (newTotal > capacity) {
                throw new OperationConflictException(
                        "Adding this booking would exceed vehicle capacity (" + capacity + ").");
            }
        }

        TripBooking tripBooking = TripBooking.builder()
                .trip(trip)
                .booking(booking)
                .build();
        trip.getTripBookings().add(tripBooking);
        tripBookingRepository.save(tripBooking);

        auditLogService.log("BOOKING_ASSIGNED", "TRIP", trip.getId(),
                "Booking " + booking.getBookingReference() + ASSIGNED_TO + trip.getTripReference() + ".");
        return toDetail(findTripWithDetails(trip.getId()));
    }

    public TripDetailDto removeBooking(Long tripId, Long bookingId) {
        Trip trip = findTripForUpdate(tripId);
        if (trip.getStatus() == TripStatus.IN_PROGRESS
                || trip.getStatus() == TripStatus.COMPLETED
                || trip.getStatus() == TripStatus.CANCELLED) {
            throw new OperationConflictException("Booking cannot be removed in the current trip status.");
        }

        TripBooking tripBooking = tripBookingRepository.findByTripId(trip.getId()).stream()
                .filter(tb -> tb.getBooking().getId().equals(bookingId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Booking is not assigned to this trip."));

        String bookingReference = tripBooking.getBooking().getBookingReference();
        trip.getTripBookings().remove(tripBooking);
        tripBookingRepository.delete(tripBooking);

        auditLogService.log("BOOKING_REMOVED", "TRIP", trip.getId(),
                "Booking " + bookingReference + " removed from " + trip.getTripReference() + ".");
        return toDetail(findTripWithDetails(trip.getId()));
    }

    public TripDetailDto updateStatus(Long tripId, UpdateTripStatusRequest request) {
        Trip trip = findTripForUpdate(tripId);
        TripStatus newStatus = parseTripStatusRequired(request.getStatus());
        TripStatus oldStatus = trip.getStatus();

        TripStatusTransitions.validate(oldStatus, newStatus);

        if (newStatus == TripStatus.ASSIGNED) {
            ensureFullyAssigned(trip);
        }
        if (newStatus == TripStatus.READY) {
            ensureFullyAssigned(trip);
        }
        if (newStatus == TripStatus.IN_PROGRESS) {
            ensureFullyAssigned(trip);
            markResourcesOnTrip(trip);
        }
        if (newStatus == TripStatus.COMPLETED) {
            releaseTripResources(trip);
            bookingService.completeConfirmedPaidBookingsForPackage(trip.getTravelPackage().getId());
        }
        if (newStatus == TripStatus.CANCELLED) {
            releaseTripResources(trip);
            operationsNotificationService.notifyTripCancelled(trip.getTripReference(), trip.getId());
        }

        trip.setStatus(newStatus);
        trip = tripRepository.save(trip);

        if (newStatus == TripStatus.READY) {
            operationsNotificationService.notifyTripReady(trip.getTripReference(), trip.getId());
        }

        auditLogService.log("TRIP_STATUS_CHANGED", "TRIP", trip.getId(),
                TRIP_LABEL + trip.getTripReference() + " status changed from "
                        + oldStatus + " to " + newStatus + ".");
        return toDetail(findTripWithDetails(trip.getId()));
    }

    private void validateVehicleAssignable(Vehicle vehicle, Trip trip) {
        if (vehicle.getStatus() == VehicleStatus.INACTIVE || vehicle.getStatus() == VehicleStatus.MAINTENANCE) {
            throw new OperationConflictException("Inactive or maintenance vehicles cannot be assigned.");
        }
        if (vehicle.getStatus() == VehicleStatus.ASSIGNED || vehicle.getStatus() == VehicleStatus.ON_TRIP) {
            boolean assignedToSameTrip = trip.getTripVehicles().stream()
                    .anyMatch(tv -> tv.getVehicle().getId().equals(vehicle.getId()));
            if (!assignedToSameTrip) {
                throw new OperationConflictException("Vehicle is not available for assignment.");
            }
        }
    }

    private void validateDriverAssignable(Driver driver, Trip trip) {
        if (driver.getStatus() == DriverStatus.INACTIVE) {
            throw new OperationConflictException("Inactive drivers cannot be assigned.");
        }
        if (LicenseStatusHelper.isExpired(driver.getLicenseExpiryDate())) {
            throw new OperationConflictException("Driver license has expired.");
        }
        if (driver.getStatus() == DriverStatus.ASSIGNED || driver.getStatus() == DriverStatus.ON_TRIP) {
            boolean assignedToSameTrip = trip.getDriver() != null
                    && trip.getDriver().getId().equals(driver.getId());
            if (!assignedToSameTrip) {
                throw new OperationConflictException("Driver is not available for assignment.");
            }
        }
    }

    private void validateBookingAssignable(Booking booking, Trip trip) {
        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new OperationConflictException("Only confirmed bookings can be assigned to a trip.");
        }
        if (booking.getPaymentStatus() != PaymentStatus.PAID) {
            throw new OperationConflictException("Only paid bookings can be assigned to a trip.");
        }
        if (!booking.getTravelPackage().getId().equals(trip.getTravelPackage().getId())) {
            throw new OperationConflictException("Booking package does not match the trip package.");
        }
    }

    private void maybeAdvanceToAssigned(Trip trip) {
        if (trip.getStatus() == TripStatus.PLANNED
                && hasPrimaryVehicle(trip)
                && trip.getDriver() != null) {
            trip.setStatus(TripStatus.ASSIGNED);
        }
    }

    private void ensureFullyAssigned(Trip trip) {
        if (!hasPrimaryVehicle(trip) || trip.getDriver() == null) {
            throw new OperationConflictException("Trip requires both a vehicle and driver before this status change.");
        }
    }

    private boolean hasPrimaryVehicle(Trip trip) {
        return tripVehicleRepository.findPrimaryVehicle(trip.getId()).isPresent()
                || trip.getTripVehicles().stream().anyMatch(TripVehicle::getPrimary);
    }

    private void markResourcesOnTrip(Trip trip) {
        tripVehicleRepository.findPrimaryVehicle(trip.getId()).ifPresent(tv -> {
            Vehicle vehicle = vehicleRepository.findByIdForUpdate(tv.getVehicle().getId()).orElseThrow();
            vehicle.setStatus(VehicleStatus.ON_TRIP);
            vehicleRepository.save(vehicle);
        });
        if (trip.getDriver() != null) {
            Driver driver = driverRepository.findByIdForUpdate(trip.getDriver().getId()).orElseThrow();
            driver.setStatus(DriverStatus.ON_TRIP);
            driverRepository.save(driver);
        }
    }

    private void releaseTripResources(Trip trip) {
        tripVehicleRepository.findPrimaryVehicle(trip.getId()).ifPresent(tv -> {
            Vehicle vehicle = vehicleRepository.findByIdForUpdate(tv.getVehicle().getId()).orElseThrow();
            if (!tripRepository.hasOtherActiveTripsForVehicle(vehicle.getId(), trip.getId())) {
                vehicle.setStatus(VehicleStatus.AVAILABLE);
                vehicleRepository.save(vehicle);
            }
            trip.getTripVehicles().remove(tv);
            tripVehicleRepository.delete(tv);
        });

        if (trip.getDriver() != null) {
            Driver driver = driverRepository.findByIdForUpdate(trip.getDriver().getId()).orElseThrow();
            if (!tripRepository.hasOtherActiveTripsForDriver(driver.getId(), trip.getId())) {
                driver.setStatus(DriverStatus.AVAILABLE);
                driverRepository.save(driver);
            }
            trip.setDriver(null);
        }
    }

    private Trip findTripWithDetails(Long id) {
        return tripRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Trip not found."));
    }

    private Trip findTripForUpdate(Long id) {
        return tripRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Trip not found."));
    }

    private List<TripSummaryDto> toSummaries(List<Trip> trips) {
        if (trips.isEmpty()) {
            return List.of();
        }
        List<Long> tripIds = trips.stream().map(Trip::getId).toList();
        Map<Long, TripBookingRepository.TripLoadSummary> loads = new HashMap<>();
        for (TripBookingRepository.TripLoadSummary row : tripBookingRepository.summarizeByTripIds(tripIds)) {
            loads.put(row.getTripId(), row);
        }
        Map<Long, TripVehicle> primaries = new HashMap<>();
        for (TripVehicle vehicle : tripVehicleRepository.findPrimaryVehiclesByTripIdIn(tripIds)) {
            primaries.put(vehicle.getTrip().getId(), vehicle);
        }
        return trips.stream()
                .map(trip -> toSummary(trip, loads.get(trip.getId()), primaries.get(trip.getId())))
                .toList();
    }

    private TripSummaryDto toSummary(
            Trip trip,
            TripBookingRepository.TripLoadSummary load,
            TripVehicle primaryVehicle
    ) {
        int travellers = load == null || load.getTravellerCount() == null ? 0 : load.getTravellerCount().intValue();
        int bookingCount = load == null || load.getBookingCount() == null ? 0 : load.getBookingCount().intValue();
        Integer capacity = primaryVehicle != null ? primaryVehicle.getVehicle().getSeatingCapacity() : null;
        Integer remaining = capacity != null ? Math.max(capacity - travellers, 0) : null;

        return TripSummaryDto.builder()
                .id(trip.getId())
                .tripReference(trip.getTripReference())
                .packageTitle(trip.getTravelPackage().getTitle())
                .destination(trip.getDestination())
                .travelDate(trip.getTravelDate())
                .startAt(trip.getStartAt())
                .endAt(trip.getEndAt())
                .bookingCount(bookingCount)
                .totalTravellers(travellers)
                .vehicleCapacity(capacity)
                .remainingCapacity(remaining)
                .vehicleRegistration(primaryVehicle != null
                        ? primaryVehicle.getVehicle().getRegistrationNumber() : null)
                .driverName(trip.getDriver() != null ? trip.getDriver().getName() : null)
                .status(trip.getStatus().name())
                .build();
    }

    private TripDetailDto toDetail(Trip trip) {
        int travellers = tripBookingRepository.sumTravellersByTripId(trip.getId());
        Optional<TripVehicle> primaryVehicle = tripVehicleRepository.findPrimaryVehicle(trip.getId());
        Integer capacity = primaryVehicle.map(tv -> tv.getVehicle().getSeatingCapacity()).orElse(null);
        Integer remaining = capacity != null ? Math.max(capacity - travellers, 0) : null;

        List<TripBookingSummaryDto> bookings = tripBookingRepository.findByTripIdWithDetails(trip.getId()).stream()
                .map(tb -> TripBookingSummaryDto.builder()
                        .bookingId(tb.getBooking().getId())
                        .bookingReference(tb.getBooking().getBookingReference())
                        .customerName(tb.getBooking().getCustomer().getFullName())
                        .numberOfTravellers(tb.getBooking().getNumberOfTravellers())
                        .bookingStatus(tb.getBooking().getStatus().name())
                        .paymentStatus(tb.getBooking().getPaymentStatus().name())
                        .build())
                .toList();

        VehicleSummaryDto vehicleDto = primaryVehicle
                .map(tv -> VehicleSummaryDto.builder()
                        .id(tv.getVehicle().getId())
                        .registrationNumber(tv.getVehicle().getRegistrationNumber())
                        .vehicleType(tv.getVehicle().getVehicleType())
                        .brandModel(tv.getVehicle().getBrandModel())
                        .seatingCapacity(tv.getVehicle().getSeatingCapacity())
                        .ownershipType(tv.getVehicle().getOwnershipType().name())
                        .status(tv.getVehicle().getStatus().name())
                        .currentTripReference(trip.getTripReference())
                        .createdAt(tv.getVehicle().getCreatedAt())
                        .build())
                .orElse(null);

        DriverSummaryDto driverDto = trip.getDriver() != null
                ? DriverSummaryDto.builder()
                .id(trip.getDriver().getId())
                .name(trip.getDriver().getName())
                .phone(trip.getDriver().getPhone())
                .licenseNumber(trip.getDriver().getLicenseNumber())
                .licenseExpiryDate(trip.getDriver().getLicenseExpiryDate())
                .licenseStatus(LicenseStatusHelper.resolve(trip.getDriver().getLicenseExpiryDate()))
                .status(trip.getDriver().getStatus().name())
                .currentTripReference(trip.getTripReference())
                .createdAt(trip.getDriver().getCreatedAt())
                .build()
                : null;

        return TripDetailDto.builder()
                .id(trip.getId())
                .tripReference(trip.getTripReference())
                .packageId(trip.getTravelPackage().getId())
                .packageTitle(trip.getTravelPackage().getTitle())
                .destination(trip.getDestination())
                .travelDate(trip.getTravelDate())
                .startAt(trip.getStartAt())
                .endAt(trip.getEndAt())
                .pickupLocation(trip.getPickupLocation())
                .dropLocation(trip.getDropLocation())
                .status(trip.getStatus().name())
                .notes(trip.getNotes())
                .totalTravellers(travellers)
                .vehicleCapacity(capacity)
                .remainingCapacity(remaining)
                .vehicle(vehicleDto)
                .driver(driverDto)
                .bookings(bookings)
                .createdAt(trip.getCreatedAt())
                .updatedAt(trip.getUpdatedAt())
                .build();
    }

    private static void validateTimes(Instant startAt, Instant endAt) {
        if (!endAt.isAfter(startAt)) {
            throw new IllegalArgumentException("End time must be after start time.");
        }
    }

    /**
     * Public booking uses package startDate, not the ops trip travelDate.
     * When an admin schedules a trip, keep the sellable package dates aligned
     * so guests are not blocked while seats remain.
     */
    private void alignPackageScheduleToTrip(TravelPackage travelPackage, LocalDate travelDate) {
        if (travelDate == null) {
            throw new IllegalArgumentException("Travel date is required.");
        }
        LocalDate today = LicenseStatusHelper.businessToday();
        if (travelDate.isBefore(today)) {
            throw new IllegalArgumentException("Travel date cannot be in the past.");
        }

        LocalDate oldStart = travelPackage.getStartDate();
        LocalDate oldEnd = travelPackage.getEndDate() != null ? travelPackage.getEndDate() : oldStart;
        long spanDays = Math.max(0, oldEnd.toEpochDay() - oldStart.toEpochDay());
        LocalDate newEnd = travelDate.plusDays(spanDays);

        if (oldStart.equals(travelDate) && oldEnd.equals(newEnd)) {
            return;
        }

        travelPackage.setStartDate(travelDate);
        travelPackage.setEndDate(newEnd);
        travelPackageRepository.save(travelPackage);
        auditLogService.log(
                "PACKAGE_DATES_ALIGNED",
                "PACKAGE",
                travelPackage.getId(),
                "Package dates set to " + travelDate + " – " + newEnd
                        + " to match trip schedule."
        );
    }

    private static String blankToEmpty(String value) {
        return value == null || value.isBlank() ? "" : value.trim();
    }

    private static String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private static TripStatus parseTripStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        return TripStatus.valueOf(status.trim().toUpperCase());
    }

    private static TripStatus parseTripStatusRequired(String status) {
        try {
            return TripStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Invalid trip status.");
        }
    }

    private static Pageable pageable(int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.clamp(size, 1, 100);
        return PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "startAt"));
    }
}
