package in.joyway.service;

import in.joyway.domain.entity.Booking;
import in.joyway.domain.entity.Customer;
import in.joyway.domain.entity.PackageReview;
import in.joyway.domain.entity.TravelPackage;
import in.joyway.domain.enums.BookingStatus;
import in.joyway.domain.enums.PackageStatus;
import in.joyway.domain.repository.PackageReviewRepository;
import in.joyway.domain.repository.TravelPackageRepository;
import in.joyway.dto.review.SubmitPackageReviewRequest;
import in.joyway.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PackageReviewServiceTest {

    @Mock private TravelPackageRepository packageRepository;
    @Mock private PackageReviewRepository packageReviewRepository;
    @Mock private BookingQueryService bookingQueryService;

    @InjectMocks
    private PackageReviewService packageReviewService;

    private Booking booking;
    private TravelPackage pkg;

    @BeforeEach
    void setUp() {
        pkg = TravelPackage.builder()
                .id(1L)
                .slug("nainital-weekend")
                .title("Nainital")
                .status(PackageStatus.PUBLISHED)
                .build();
        Customer customer = Customer.builder()
                .fullName("Priya Sharma")
                .email("priya@example.com")
                .build();
        booking = Booking.builder()
                .id(10L)
                .bookingReference("JW-2026-00010")
                .customer(customer)
                .travelPackage(pkg)
                .status(BookingStatus.COMPLETED)
                .build();
    }

    @Test
    void submitReview_rejectsBeforeCompletion() {
        booking.setStatus(BookingStatus.CONFIRMED);
        when(bookingQueryService.findBookingWithAccessCheck("JW-2026-00010", "priya@example.com"))
                .thenReturn(booking);

        SubmitPackageReviewRequest payload = request();
        assertThatThrownBy(() -> packageReviewService.submitReview(payload))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("completed");
    }

    @Test
    void submitReview_savesOncePerBooking() {
        when(bookingQueryService.findBookingWithAccessCheck("JW-2026-00010", "priya@example.com"))
                .thenReturn(booking);
        when(packageReviewRepository.existsByBookingId(10L)).thenReturn(false);
        when(packageReviewRepository.save(any())).thenAnswer(inv -> {
            PackageReview review = inv.getArgument(0);
            review.setCreatedAt(Instant.parse("2026-09-10T10:00:00Z"));
            return review;
        });

        var item = packageReviewService.submitReview(request());

        assertThat(item.rating()).isEqualTo(5);
        assertThat(item.customerName()).isEqualTo("Priya Sharma");
        assertThat(item.comment()).isEqualTo("Loved the trip");
    }

    @Test
    void submitReview_rejectsDuplicateAndBlankCommentBecomesNull() {
        when(bookingQueryService.findBookingWithAccessCheck("JW-2026-00010", "priya@example.com"))
                .thenReturn(booking);
        when(packageReviewRepository.existsByBookingId(10L)).thenReturn(true);

        SubmitPackageReviewRequest duplicate = request();
        assertThatThrownBy(() -> packageReviewService.submitReview(duplicate))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already reviewed");

        when(packageReviewRepository.existsByBookingId(10L)).thenReturn(false);
        when(packageReviewRepository.save(any())).thenAnswer(inv -> {
            PackageReview review = inv.getArgument(0);
            review.setCreatedAt(Instant.parse("2026-09-10T10:00:00Z"));
            return review;
        });

        var item = packageReviewService.submitReview(
                new SubmitPackageReviewRequest("JW-2026-00010", "priya@example.com", 4, "   "));

        assertThat(item.comment()).isNull();
        assertThat(item.rating()).isEqualTo(4);
    }

    @Test
    void listPublishedReviews_returnsNullAverageWhenEmpty() {
        when(packageRepository.findBySlug("nainital-weekend")).thenReturn(Optional.of(pkg));
        when(packageReviewRepository.findByTravelPackageIdOrderByCreatedAtDesc(1L)).thenReturn(List.of());

        var response = packageReviewService.listPublishedReviews("nainital-weekend");

        assertThat(response.reviewCount()).isZero();
        assertThat(response.averageRating()).isNull();
        assertThat(response.reviews()).isEmpty();
    }

    @Test
    void listPublishedReviews_rejectsUnpublishedPackage() {
        when(packageRepository.findBySlug("nainital-weekend")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> packageReviewService.listPublishedReviews("nainital-weekend"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void listPublishedReviews_returnsAverage() {
        when(packageRepository.findBySlug("nainital-weekend")).thenReturn(Optional.of(pkg));
        PackageReview first = PackageReview.builder()
                .customerName("Asha")
                .rating(5)
                .comment("Great")
                .createdAt(Instant.parse("2026-09-10T10:00:00Z"))
                .build();
        PackageReview second = PackageReview.builder()
                .customerName("Ravi")
                .rating(3)
                .createdAt(Instant.parse("2026-09-11T10:00:00Z"))
                .build();
        when(packageReviewRepository.findByTravelPackageIdOrderByCreatedAtDesc(1L))
                .thenReturn(List.of(first, second));

        var response = packageReviewService.listPublishedReviews("nainital-weekend");

        assertThat(response.reviewCount()).isEqualTo(2);
        assertThat(response.averageRating()).isEqualByComparingTo("4.0");
        assertThat(response.reviews()).hasSize(2);
    }

    private static SubmitPackageReviewRequest request() {
        return new SubmitPackageReviewRequest("JW-2026-00010", "priya@example.com", 5, "Loved the trip");
    }
}
