package in.joyway.service.operations;

import in.joyway.domain.entity.Driver;
import in.joyway.domain.enums.DriverStatus;
import in.joyway.domain.repository.DriverRepository;
import in.joyway.domain.repository.TripRepository;
import in.joyway.dto.PageResponse;
import in.joyway.dto.admin.operations.*;
import in.joyway.exception.OperationConflictException;
import in.joyway.exception.ResourceNotFoundException;
import in.joyway.service.audit.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Transactional
public class AdminDriverService {

    private static final String ENTITY_DRIVER = "DRIVER";
    private static final String DRIVER_LABEL = "Driver ";

    private final DriverRepository driverRepository;
    private final TripRepository tripRepository;
    private final AuditLogService auditLogService;
    private final OperationsNotificationService operationsNotificationService;

    @Transactional(readOnly = true)
    public PageResponse<DriverSummaryDto> listDrivers(
            String query,
            String status,
            String licenseFilter,
            int page,
            int size
    ) {
        Page<Driver> result = driverRepository.search(
                blankToEmpty(query),
                parseStatus(status),
                resolveExpiringBefore(licenseFilter),
                pageable(page, size)
        );
        return PageResponse.of(result.map(this::toSummary));
    }

    @Transactional(readOnly = true)
    public DriverDetailDto getDriver(Long id) {
        Driver driver = findDriver(id);
        return toDetail(driver);
    }

    public DriverDetailDto createDriver(CreateDriverRequest request) {
        String licenseNumber = normalizeLicense(request.getLicenseNumber());
        if (driverRepository.existsByLicenseNumberIgnoreCase(licenseNumber)) {
            throw new OperationConflictException("A driver with this license number already exists.");
        }

        Driver driver = Driver.builder()
                .name(request.getName().trim())
                .phone(request.getPhone().trim())
                .email(trimToNull(request.getEmail()))
                .licenseNumber(licenseNumber)
                .licenseExpiryDate(request.getLicenseExpiryDate())
                .status(DriverStatus.AVAILABLE)
                .notes(trimToNull(request.getNotes()))
                .build();

        driver = driverRepository.save(driver);
        auditLogService.log("DRIVER_CREATED", ENTITY_DRIVER, driver.getId(),
                DRIVER_LABEL + driver.getName() + " created.");
        notifyIfLicenseExpiring(driver);
        return toDetail(driver);
    }

    public DriverDetailDto updateDriver(Long id, UpdateDriverRequest request) {
        Driver driver = findDriver(id);
        String licenseNumber = normalizeLicense(request.getLicenseNumber());

        if (!driver.getLicenseNumber().equalsIgnoreCase(licenseNumber)
                && driverRepository.existsByLicenseNumberIgnoreCase(licenseNumber)) {
            throw new OperationConflictException("A driver with this license number already exists.");
        }

        driver.setName(request.getName().trim());
        driver.setPhone(request.getPhone().trim());
        driver.setEmail(trimToNull(request.getEmail()));
        driver.setLicenseNumber(licenseNumber);
        driver.setLicenseExpiryDate(request.getLicenseExpiryDate());
        driver.setNotes(trimToNull(request.getNotes()));

        driver = driverRepository.save(driver);
        auditLogService.log("DRIVER_UPDATED", ENTITY_DRIVER, driver.getId(),
                DRIVER_LABEL + driver.getName() + " updated.");
        notifyIfLicenseExpiring(driver);
        return toDetail(driver);
    }

    public DriverDetailDto updateStatus(Long id, UpdateDriverStatusRequest request) {
        Driver driver = findDriver(id);
        DriverStatus newStatus = parseStatusRequired(request.getStatus());
        DriverStatus oldStatus = driver.getStatus();

        if ((oldStatus == DriverStatus.ASSIGNED || oldStatus == DriverStatus.ON_TRIP)
                && newStatus == DriverStatus.INACTIVE) {
            throw new OperationConflictException(
                    "Cannot deactivate a driver that is assigned to an active trip.");
        }

        driver.setStatus(newStatus);
        driver = driverRepository.save(driver);
        auditLogService.log("DRIVER_STATUS_CHANGED", ENTITY_DRIVER, driver.getId(),
                DRIVER_LABEL + driver.getName() + " status changed from "
                        + oldStatus + " to " + newStatus + ".");
        return toDetail(driver);
    }

    private void notifyIfLicenseExpiring(Driver driver) {
        String licenseStatus = LicenseStatusHelper.resolve(driver.getLicenseExpiryDate());
        if ("LICENSE EXPIRING SOON".equals(licenseStatus)) {
            operationsNotificationService.notifyDriverLicenseExpiring(driver.getName(), driver.getId());
        }
    }

    private Driver findDriver(Long id) {
        return driverRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Driver not found."));
    }

    private DriverSummaryDto toSummary(Driver driver) {
        return DriverSummaryDto.builder()
                .id(driver.getId())
                .name(driver.getName())
                .phone(driver.getPhone())
                .licenseNumber(driver.getLicenseNumber())
                .licenseExpiryDate(driver.getLicenseExpiryDate())
                .licenseStatus(LicenseStatusHelper.resolve(driver.getLicenseExpiryDate()))
                .status(driver.getStatus().name())
                .currentTripReference(findCurrentTripReference(driver.getId()))
                .createdAt(driver.getCreatedAt())
                .build();
    }

    private DriverDetailDto toDetail(Driver driver) {
        return DriverDetailDto.builder()
                .id(driver.getId())
                .name(driver.getName())
                .phone(driver.getPhone())
                .email(driver.getEmail())
                .licenseNumber(driver.getLicenseNumber())
                .licenseExpiryDate(driver.getLicenseExpiryDate())
                .licenseStatus(LicenseStatusHelper.resolve(driver.getLicenseExpiryDate()))
                .status(driver.getStatus().name())
                .notes(driver.getNotes())
                .currentTripReference(findCurrentTripReference(driver.getId()))
                .createdAt(driver.getCreatedAt())
                .updatedAt(driver.getUpdatedAt())
                .build();
    }

    private String findCurrentTripReference(Long driverId) {
        List<String> refs = tripRepository.findActiveTripReferencesForDriver(driverId, PageRequest.of(0, 1));
        return refs.isEmpty() ? null : refs.get(0);
    }

    private static LocalDate resolveExpiringBefore(String licenseFilter) {
        if (licenseFilter == null || licenseFilter.isBlank()) {
            return null;
        }
        return switch (licenseFilter.trim().toUpperCase(Locale.ROOT)) {
            case "EXPIRED" -> LicenseStatusHelper.businessToday().minusDays(1);
            case "EXPIRING_SOON" -> LicenseStatusHelper.businessToday().plusDays(30);
            default -> null;
        };
    }

    private static String normalizeLicense(String licenseNumber) {
        return licenseNumber.trim().toUpperCase(Locale.ROOT);
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

    private static DriverStatus parseStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        return DriverStatus.valueOf(status.trim().toUpperCase());
    }

    private static DriverStatus parseStatusRequired(String status) {
        try {
            return DriverStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Invalid driver status.");
        }
    }

    private static Pageable pageable(int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.clamp(size, 1, 100);
        return PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"));
    }
}
