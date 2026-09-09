package in.joyway.service;

import in.joyway.domain.entity.TravelPackage;
import in.joyway.domain.enums.PackageStatus;
import in.joyway.domain.repository.TravelPackageRepository;
import in.joyway.dto.admin.*;
import in.joyway.exception.ResourceNotFoundException;
import in.joyway.mapper.AdminMapper;
import in.joyway.util.SlugUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class AdminPackageService {

    private static final String PACKAGE_NOT_FOUND = "Package not found.";

    private final TravelPackageRepository packageRepository;
    private final AdminMapper adminMapper;

    @Transactional(readOnly = true)
    public List<AdminPackageSummaryDto> listAllPackages() {
        return packageRepository.findAllWithImages()
                .stream()
                .map(adminMapper::toSummaryDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public AdminPackageDetailDto getPackageById(Long id) {
        return adminMapper.toDetailDto(requirePackageWithImages(id));
    }

    public AdminPackageDetailDto createPackage(CreatePackageRequest request) {
        validateDates(request.getStartDate(), request.getEndDate());

        String slugSource = request.getSlug() != null && !request.getSlug().isBlank()
                ? request.getSlug()
                : request.getTitle();
        String slug = SlugUtils.ensureUnique(SlugUtils.toSlug(slugSource), packageRepository::existsBySlug);

        TravelPackage pkg = TravelPackage.builder()
                .title(request.getTitle())
                .slug(slug)
                .destination(request.getDestination())
                .shortDescription(request.getShortDescription())
                .fullDescription(request.getFullDescription())
                .departureCity(request.getDepartureCity())
                .pickupPoints(request.getPickupPoints())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .duration(request.getDuration())
                .pricePerPerson(request.getPricePerPerson())
                .totalSeats(request.getTotalSeats())
                .availableSeats(request.getTotalSeats())
                .hotelName(request.getHotelName())
                .hotelDetails(request.getHotelDetails())
                .vehicleType(request.getVehicleType())
                .itinerary(request.getItinerary())
                .inclusions(request.getInclusions())
                .exclusions(request.getExclusions())
                .cancellationPolicy(request.getCancellationPolicy())
                .status(PackageStatus.DRAFT)
                .build();

        pkg = packageRepository.save(pkg);
        return adminMapper.toDetailDto(pkg);
    }

    public AdminPackageDetailDto updatePackage(Long id, UpdatePackageRequest request) {
        TravelPackage pkg = requirePackageWithImages(id);
        applyIdentity(pkg, request);
        applyCopy(pkg, request);
        applyScheduleAndPricing(pkg, request);
        applyContent(pkg, request);
        return adminMapper.toDetailDto(packageRepository.save(pkg));
    }

    public AdminPackageDetailDto updateStatus(Long id, UpdatePackageStatusRequest request) {
        TravelPackage pkg = requirePackageWithImages(id);

        PackageStatus newStatus = request.getStatus();
        validateStatusTransition(pkg.getStatus(), newStatus);

        pkg.setStatus(newStatus);
        pkg = packageRepository.save(pkg);
        return adminMapper.toDetailDto(pkg);
    }

    public void deletePackage(Long id) {
        packageRepository.delete(requirePackage(id));
    }

    private TravelPackage requirePackage(Long id) {
        return packageRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(PACKAGE_NOT_FOUND));
    }

    private TravelPackage requirePackageWithImages(Long id) {
        return packageRepository.findByIdWithImages(id)
                .orElseThrow(() -> new ResourceNotFoundException(PACKAGE_NOT_FOUND));
    }

    private void applyIdentity(TravelPackage pkg, UpdatePackageRequest request) {
        if (request.getTitle() != null) {
            pkg.setTitle(request.getTitle());
        }
        if (request.getSlug() != null && !request.getSlug().isBlank()) {
            String newSlug = SlugUtils.toSlug(request.getSlug());
            if (!newSlug.equals(pkg.getSlug()) && packageRepository.existsBySlug(newSlug)) {
                throw new IllegalArgumentException("Slug already in use.");
            }
            pkg.setSlug(newSlug);
        }
        if (request.getDestination() != null) {
            pkg.setDestination(request.getDestination());
        }
    }

    private void applyCopy(TravelPackage pkg, UpdatePackageRequest request) {
        if (request.getShortDescription() != null) {
            pkg.setShortDescription(request.getShortDescription());
        }
        if (request.getFullDescription() != null) {
            pkg.setFullDescription(request.getFullDescription());
        }
        if (request.getDepartureCity() != null) {
            pkg.setDepartureCity(request.getDepartureCity());
        }
        if (request.getPickupPoints() != null) {
            pkg.setPickupPoints(request.getPickupPoints());
        }
    }

    private void applyScheduleAndPricing(TravelPackage pkg, UpdatePackageRequest request) {
        if (request.getStartDate() != null) {
            pkg.setStartDate(request.getStartDate());
        }
        if (request.getEndDate() != null) {
            pkg.setEndDate(request.getEndDate());
        }
        if (request.getStartDate() != null || request.getEndDate() != null) {
            validateDates(pkg.getStartDate(), pkg.getEndDate());
        }
        if (request.getDuration() != null) {
            pkg.setDuration(request.getDuration());
        }
        if (request.getPricePerPerson() != null) {
            pkg.setPricePerPerson(request.getPricePerPerson());
        }
        applySeatCounts(pkg, request);
    }

    private void applySeatCounts(TravelPackage pkg, UpdatePackageRequest request) {
        int booked = Math.max(0, pkg.getTotalSeats() - pkg.getAvailableSeats());
        if (request.getTotalSeats() != null) {
            if (request.getTotalSeats() < booked) {
                throw new IllegalArgumentException(
                        "Total seats cannot be less than already booked seats (" + booked + ").");
            }
            pkg.setTotalSeats(request.getTotalSeats());
            pkg.setAvailableSeats(request.getTotalSeats() - booked);
            return;
        }
        if (request.getAvailableSeats() != null) {
            if (request.getAvailableSeats() > pkg.getTotalSeats()) {
                throw new IllegalArgumentException("Available seats cannot exceed total seats.");
            }
            pkg.setAvailableSeats(request.getAvailableSeats());
        }
    }

    private void applyContent(TravelPackage pkg, UpdatePackageRequest request) {
        if (request.getHotelName() != null) {
            pkg.setHotelName(request.getHotelName());
        }
        if (request.getHotelDetails() != null) {
            pkg.setHotelDetails(request.getHotelDetails());
        }
        if (request.getVehicleType() != null) {
            pkg.setVehicleType(request.getVehicleType());
        }
        if (request.getItinerary() != null) {
            pkg.setItinerary(request.getItinerary());
        }
        if (request.getInclusions() != null) {
            pkg.setInclusions(request.getInclusions());
        }
        if (request.getExclusions() != null) {
            pkg.setExclusions(request.getExclusions());
        }
        if (request.getCancellationPolicy() != null) {
            pkg.setCancellationPolicy(request.getCancellationPolicy());
        }
    }

    private void validateDates(java.time.LocalDate start, java.time.LocalDate end) {
        if (end.isBefore(start)) {
            throw new IllegalArgumentException("End date must be on or after start date.");
        }
    }

    private void validateStatusTransition(PackageStatus current, PackageStatus next) {
        if (current == next) {
            return;
        }
        // Allow all reasonable admin transitions for MVP
        if (current == PackageStatus.CANCELLED) {
            throw new IllegalArgumentException("Cancelled packages cannot change status.");
        }
    }
}
