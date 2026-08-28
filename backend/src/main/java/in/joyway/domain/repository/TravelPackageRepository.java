package in.joyway.domain.repository;

import in.joyway.domain.entity.TravelPackage;
import in.joyway.domain.enums.PackageStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TravelPackageRepository extends JpaRepository<TravelPackage, Long> {

    Optional<TravelPackage> findBySlug(String slug);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM TravelPackage p WHERE p.id = :id")
    Optional<TravelPackage> findByIdForUpdate(@Param("id") Long id);

    List<TravelPackage> findByStatusOrderByStartDateAsc(PackageStatus status);

    @Query("SELECT p FROM TravelPackage p LEFT JOIN FETCH p.images WHERE p.status = :status ORDER BY p.startDate ASC")
    List<TravelPackage> findPublishedWithImages(PackageStatus status);

    @Query("SELECT p FROM TravelPackage p LEFT JOIN FETCH p.images WHERE p.slug = :slug")
    Optional<TravelPackage> findBySlugWithImages(String slug);

    boolean existsBySlug(String slug);

    @Query("SELECT p FROM TravelPackage p LEFT JOIN FETCH p.images ORDER BY p.updatedAt DESC")
    List<TravelPackage> findAllWithImages();

    @Query("SELECT p FROM TravelPackage p LEFT JOIN FETCH p.images WHERE p.id = :id")
    Optional<TravelPackage> findByIdWithImages(Long id);

    long countByStatus(PackageStatus status);

    @Query("SELECT COUNT(p) FROM TravelPackage p WHERE p.status = 'PUBLISHED' AND p.startDate >= CURRENT_DATE")
    long countUpcomingPublished();

    @Query("SELECT COALESCE(SUM(p.availableSeats), 0) FROM TravelPackage p WHERE p.status = 'PUBLISHED'")
    long sumAvailableSeatsPublished();
}
