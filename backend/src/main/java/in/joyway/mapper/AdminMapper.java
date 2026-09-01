package in.joyway.mapper;

import in.joyway.domain.entity.PackageImage;
import in.joyway.domain.entity.TravelPackage;
import in.joyway.domain.entity.User;
import in.joyway.dto.admin.AdminPackageDetailDto;
import in.joyway.dto.admin.AdminPackageSummaryDto;
import in.joyway.dto.admin.AdminProfileDto;
import in.joyway.dto.package_.PackageImageDto;
import in.joyway.service.PackageImageUrlService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class AdminMapper {

    private final PackageImageUrlService packageImageUrlService;

    public AdminProfileDto toProfileDto(User user) {
        return AdminProfileDto.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .roles(user.getRoles().stream().map(r -> r.getName()).toList())
                .build();
    }

    public AdminPackageSummaryDto toSummaryDto(TravelPackage pkg) {
        PackageImage hero = getHeroImage(pkg);
        return AdminPackageSummaryDto.builder()
                .id(pkg.getId())
                .title(pkg.getTitle())
                .slug(pkg.getSlug())
                .destination(pkg.getDestination())
                .startDate(pkg.getStartDate())
                .endDate(pkg.getEndDate())
                .duration(pkg.getDuration())
                .pricePerPerson(pkg.getPricePerPerson())
                .totalSeats(pkg.getTotalSeats())
                .availableSeats(pkg.getAvailableSeats())
                .status(pkg.getStatus().name())
                .heroImageUrl(hero != null ? packageImageUrlService.resolvePublicUrl(hero) : null)
                .imageCount(pkg.getImages() != null ? pkg.getImages().size() : 0)
                .build();
    }

    public AdminPackageDetailDto toDetailDto(TravelPackage pkg) {
        return AdminPackageDetailDto.builder()
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
}
