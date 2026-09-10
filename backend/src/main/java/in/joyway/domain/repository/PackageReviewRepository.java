package in.joyway.domain.repository;

import in.joyway.domain.entity.PackageReview;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PackageReviewRepository extends JpaRepository<PackageReview, Long> {

    boolean existsByBookingId(Long bookingId);

    List<PackageReview> findByTravelPackageIdOrderByCreatedAtDesc(Long packageId);
}
