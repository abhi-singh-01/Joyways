package in.joyway.service.operations;

import in.joyway.domain.entity.Vehicle;
import in.joyway.domain.enums.VehicleOwnershipType;
import in.joyway.domain.enums.VehicleStatus;
import in.joyway.domain.repository.TripRepository;
import in.joyway.domain.repository.VehicleRepository;
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

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class AdminVehicleService {

    private static final String ENTITY_VEHICLE = "VEHICLE";
    private static final String VEHICLE_LABEL = "Vehicle ";

    private final VehicleRepository vehicleRepository;
    private final TripRepository tripRepository;
    private final AuditLogService auditLogService;
    private final OperationsNotificationService operationsNotificationService;

    @Transactional(readOnly = true)
    public PageResponse<VehicleSummaryDto> listVehicles(
            String query,
            String status,
            String ownershipType,
            String vehicleType,
            int page,
            int size
    ) {
        Page<Vehicle> result = vehicleRepository.search(
                blankToEmpty(query),
                parseStatus(status),
                parseOwnership(ownershipType),
                blankToEmpty(vehicleType),
                pageable(page, size)
        );
        return PageResponse.of(result.map(this::toSummary));
    }

    @Transactional(readOnly = true)
    public VehicleDetailDto getVehicle(Long id) {
        Vehicle vehicle = findVehicle(id);
        return toDetail(vehicle);
    }

    public VehicleDetailDto createVehicle(CreateVehicleRequest request) {
        String registration = normalizeRegistration(request.getRegistrationNumber());
        if (vehicleRepository.existsByRegistrationNumberIgnoreCase(registration)) {
            throw new OperationConflictException("A vehicle with this registration number already exists.");
        }

        Vehicle vehicle = Vehicle.builder()
                .registrationNumber(registration)
                .vehicleType(request.getVehicleType().trim())
                .brandModel(trimToNull(request.getBrandModel()))
                .seatingCapacity(request.getSeatingCapacity())
                .ownershipType(parseOwnershipRequired(request.getOwnershipType()))
                .status(VehicleStatus.AVAILABLE)
                .notes(trimToNull(request.getNotes()))
                .build();

        vehicle = vehicleRepository.save(vehicle);
        auditLogService.log("VEHICLE_CREATED", ENTITY_VEHICLE, vehicle.getId(),
                VEHICLE_LABEL + vehicle.getRegistrationNumber() + " created.");
        return toDetail(vehicle);
    }

    public VehicleDetailDto updateVehicle(Long id, UpdateVehicleRequest request) {
        Vehicle vehicle = findVehicle(id);
        String registration = normalizeRegistration(request.getRegistrationNumber());

        vehicleRepository.findByRegistrationNumberIgnoreCase(registration)
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new OperationConflictException("A vehicle with this registration number already exists.");
                });

        vehicle.setRegistrationNumber(registration);
        vehicle.setVehicleType(request.getVehicleType().trim());
        vehicle.setBrandModel(trimToNull(request.getBrandModel()));
        vehicle.setSeatingCapacity(request.getSeatingCapacity());
        vehicle.setOwnershipType(parseOwnershipRequired(request.getOwnershipType()));
        vehicle.setNotes(trimToNull(request.getNotes()));

        vehicle = vehicleRepository.save(vehicle);
        auditLogService.log("VEHICLE_UPDATED", ENTITY_VEHICLE, vehicle.getId(),
                VEHICLE_LABEL + vehicle.getRegistrationNumber() + " updated.");
        return toDetail(vehicle);
    }

    public VehicleDetailDto updateStatus(Long id, UpdateVehicleStatusRequest request) {
        Vehicle vehicle = findVehicle(id);
        VehicleStatus newStatus = parseStatusRequired(request.getStatus());
        VehicleStatus oldStatus = vehicle.getStatus();

        if ((oldStatus == VehicleStatus.ASSIGNED || oldStatus == VehicleStatus.ON_TRIP)
                && (newStatus == VehicleStatus.INACTIVE || newStatus == VehicleStatus.MAINTENANCE)) {
            throw new OperationConflictException(
                    "Cannot deactivate or send to maintenance a vehicle that is assigned to an active trip.");
        }

        vehicle.setStatus(newStatus);
        vehicle = vehicleRepository.save(vehicle);
        auditLogService.log("VEHICLE_STATUS_CHANGED", ENTITY_VEHICLE, vehicle.getId(),
                VEHICLE_LABEL + vehicle.getRegistrationNumber() + " status changed from "
                        + oldStatus + " to " + newStatus + ".");
        if (newStatus == VehicleStatus.MAINTENANCE || newStatus == VehicleStatus.INACTIVE) {
            operationsNotificationService.notifyVehicleUnavailable(
                    vehicle.getRegistrationNumber(), newStatus.name(), vehicle.getId());
        }
        return toDetail(vehicle);
    }

    private Vehicle findVehicle(Long id) {
        return vehicleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found."));
    }

    private VehicleSummaryDto toSummary(Vehicle vehicle) {
        return VehicleSummaryDto.builder()
                .id(vehicle.getId())
                .registrationNumber(vehicle.getRegistrationNumber())
                .vehicleType(vehicle.getVehicleType())
                .brandModel(vehicle.getBrandModel())
                .seatingCapacity(vehicle.getSeatingCapacity())
                .ownershipType(vehicle.getOwnershipType().name())
                .status(vehicle.getStatus().name())
                .currentTripReference(findCurrentTripReference(vehicle.getId()))
                .createdAt(vehicle.getCreatedAt())
                .build();
    }

    private VehicleDetailDto toDetail(Vehicle vehicle) {
        return VehicleDetailDto.builder()
                .id(vehicle.getId())
                .registrationNumber(vehicle.getRegistrationNumber())
                .vehicleType(vehicle.getVehicleType())
                .brandModel(vehicle.getBrandModel())
                .seatingCapacity(vehicle.getSeatingCapacity())
                .ownershipType(vehicle.getOwnershipType().name())
                .status(vehicle.getStatus().name())
                .notes(vehicle.getNotes())
                .currentTripReference(findCurrentTripReference(vehicle.getId()))
                .createdAt(vehicle.getCreatedAt())
                .updatedAt(vehicle.getUpdatedAt())
                .build();
    }

    private String findCurrentTripReference(Long vehicleId) {
        List<String> refs = tripRepository.findActiveTripReferencesForVehicle(vehicleId, PageRequest.of(0, 1));
        return refs.isEmpty() ? null : refs.get(0);
    }

    private static String normalizeRegistration(String registration) {
        return registration.trim().toUpperCase();
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

    private static VehicleStatus parseStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        return VehicleStatus.valueOf(status.trim().toUpperCase());
    }

    private static VehicleStatus parseStatusRequired(String status) {
        try {
            return VehicleStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Invalid vehicle status.");
        }
    }

    private static VehicleOwnershipType parseOwnership(String ownershipType) {
        if (ownershipType == null || ownershipType.isBlank()) {
            return null;
        }
        return VehicleOwnershipType.valueOf(ownershipType.trim().toUpperCase());
    }

    private static VehicleOwnershipType parseOwnershipRequired(String ownershipType) {
        try {
            return VehicleOwnershipType.valueOf(ownershipType.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Invalid ownership type.");
        }
    }

    private static Pageable pageable(int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.clamp(size, 1, 100);
        return PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"));
    }
}
