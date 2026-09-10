package in.joyway.mapper;

import in.joyway.domain.entity.PackageImage;
import in.joyway.domain.entity.TravelPackage;
import in.joyway.domain.enums.PackageStatus;
import in.joyway.dto.package_.PackageDetailDto;
import in.joyway.dto.package_.PackageImageDto;
import in.joyway.dto.package_.PackageSummaryDto;
import in.joyway.service.PackageImageUrlService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@Component
@RequiredArgsConstructor
public class PackageMapper {

    private final PackageImageUrlService packageImageUrlService;

    public PackageSummaryDto toSummaryDto(TravelPackage pkg) {
        PackageImage hero = getHeroImage(pkg);
        return PackageSummaryDto.builder()
                .id(pkg.getId())
                .title(pkg.getTitle())
                .slug(pkg.getSlug())
                .destination(pkg.getDestination())
                .shortDescription(pkg.getShortDescription())
                .duration(pkg.getDuration())
                .departureCity(pkg.getDepartureCity())
                .startDate(pkg.getStartDate())
                .endDate(pkg.getEndDate())
                .pricePerPerson(pkg.getPricePerPerson())
                .availableSeats(pkg.getAvailableSeats())
                .heroImageUrl(hero != null ? packageImageUrlService.resolvePublicUrl(hero) : null)
                .heroImageAlt(hero != null ? hero.getAltText() : pkg.getDestination())
                .waitlistable(isWaitlistable(pkg))
                .build();
    }

    public PackageDetailDto toDetailDto(TravelPackage pkg) {
        return PackageDetailDto.builder()
                .id(pkg.getId())
                .title(pkg.getTitle())
                .slug(pkg.getSlug())
                .destination(pkg.getDestination())
                .shortDescription(pkg.getShortDescription())
                .fullDescription(pkg.getFullDescription())
                .departureCity(pkg.getDepartureCity())
                .pickupPoints(pkg.getPickupPoints())
                .startDate(pkg.getStartDate())
                .endDate(pkg.getEndDate())
                .duration(pkg.getDuration())
                .pricePerPerson(pkg.getPricePerPerson())
                .totalSeats(pkg.getTotalSeats())
                .availableSeats(pkg.getAvailableSeats())
                .hotelName(pkg.getHotelName())
                .hotelDetails(pkg.getHotelDetails())
                .vehicleType(pkg.getVehicleType())
                .itinerary(pkg.getItinerary())
                .inclusions(pkg.getInclusions())
                .exclusions(pkg.getExclusions())
                .cancellationPolicy(pkg.getCancellationPolicy())
                .status(pkg.getStatus().name())
                .images(toImageDtos(pkg.getImages()))
                .bookable(isBookable(pkg))
                .waitlistable(isWaitlistable(pkg))
                .build();
    }

    public List<PackageImageDto> toImageDtos(List<PackageImage> images) {
        if (images == null) {
            return List.of();
        }
        return images.stream()
                .map(img -> PackageImageDto.builder()
                        .id(img.getId())
                        .imageUrl(packageImageUrlService.resolvePublicUrl(img))
                        .altText(img.getAltText())
                        .sortOrder(img.getSortOrder())
                        .build())
                .toList();
    }

    private PackageImage getHeroImage(TravelPackage pkg) {
        if (pkg.getImages() == null || pkg.getImages().isEmpty()) {
            return null;
        }
        return pkg.getImages().getFirst();
    }

    private boolean isUpcoming(TravelPackage pkg) {
        return !pkg.getStartDate().isBefore(LocalDate.now(ZoneId.of("Asia/Kolkata")));
    }

    private boolean isBookable(TravelPackage pkg) {
        return pkg.getStatus() == PackageStatus.PUBLISHED
                && pkg.getAvailableSeats() > 0
                && isUpcoming(pkg);
    }

    private boolean isWaitlistable(TravelPackage pkg) {
        return pkg.getStatus() == PackageStatus.PUBLISHED
                && pkg.getAvailableSeats() <= 0
                && isUpcoming(pkg);
    }
}
