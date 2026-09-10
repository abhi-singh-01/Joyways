package in.joyway.service;

import in.joyway.domain.entity.Booking;
import in.joyway.domain.entity.PackageReview;
import in.joyway.domain.entity.TravelPackage;
import in.joyway.domain.enums.BookingStatus;
import in.joyway.domain.enums.PackageStatus;
import in.joyway.domain.repository.PackageReviewRepository;
import in.joyway.domain.repository.TravelPackageRepository;
import in.joyway.dto.review.PackageReviewItemDto;
import in.joyway.dto.review.PackageReviewsResponse;
import in.joyway.dto.review.SubmitPackageReviewRequest;
import in.joyway.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PackageReviewService {

    private final TravelPackageRepository packageRepository;
    private final PackageReviewRepository packageReviewRepository;
    private final BookingQueryService bookingQueryService;

    @Transactional(readOnly = true)
    public PackageReviewsResponse listPublishedReviews(String slug) {
        TravelPackage pkg = publishedPackageBySlug(slug);
        List<PackageReview> reviews = packageReviewRepository
                .findByTravelPackageIdOrderByCreatedAtDesc(pkg.getId());
        long count = reviews.size();
        BigDecimal average = count == 0
                ? null
                : BigDecimal.valueOf(reviews.stream().mapToInt(PackageReview::getRating).average().orElse(0))
                .setScale(1, RoundingMode.HALF_UP);
        return PackageReviewsResponse.builder()
                .averageRating(average)
                .reviewCount(count)
                .reviews(reviews.stream().map(this::toItem).toList())
                .build();
    }

    @Transactional
    public PackageReviewItemDto submitReview(SubmitPackageReviewRequest request) {
        Booking booking = bookingQueryService.findBookingWithAccessCheck(
                request.reference().trim().toUpperCase(), request.email());
        if (booking.getStatus() != BookingStatus.COMPLETED) {
            throw new IllegalArgumentException("Reviews open after the trip is completed.");
        }
        if (packageReviewRepository.existsByBookingId(booking.getId())) {
            throw new IllegalArgumentException("You have already reviewed this trip.");
        }
        String comment = request.comment() == null || request.comment().isBlank()
                ? null
                : request.comment().trim();
        PackageReview review = PackageReview.builder()
                .booking(booking)
                .travelPackage(booking.getTravelPackage())
                .customerName(booking.getCustomer().getFullName())
                .rating(request.rating())
                .comment(comment)
                .build();
        return toItem(packageReviewRepository.save(review));
    }

    private TravelPackage publishedPackageBySlug(String slug) {
        return packageRepository.findBySlug(slug)
                .filter(p -> p.getStatus() == PackageStatus.PUBLISHED)
                .orElseThrow(() -> new ResourceNotFoundException("Trip not found or unavailable."));
    }

    private PackageReviewItemDto toItem(PackageReview review) {
        return PackageReviewItemDto.builder()
                .customerName(review.getCustomerName())
                .rating(review.getRating())
                .comment(review.getComment())
                .createdAt(review.getCreatedAt())
                .build();
    }
}
