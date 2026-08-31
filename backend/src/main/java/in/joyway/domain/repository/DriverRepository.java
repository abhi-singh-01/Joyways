package in.joyway.domain.repository;

import in.joyway.domain.entity.Driver;
import in.joyway.domain.enums.DriverStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.Optional;

public interface DriverRepository extends JpaRepository<Driver, Long> {

    boolean existsByLicenseNumberIgnoreCase(String licenseNumber);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT d FROM Driver d WHERE d.id = :id")
    Optional<Driver> findByIdForUpdate(Long id);

    @Query("""
            SELECT d FROM Driver d
            WHERE (:status IS NULL OR d.status = :status)
            AND (:expiringBefore IS NULL OR d.licenseExpiryDate <= :expiringBefore)
            AND (:query = '' OR LOWER(d.name) LIKE CONCAT('%', LOWER(CAST(:query AS string)), '%')
                 OR d.phone LIKE CONCAT('%', CAST(:query AS string), '%')
                 OR LOWER(d.licenseNumber) LIKE CONCAT('%', LOWER(CAST(:query AS string)), '%'))
            """)
    Page<Driver> search(
            String query,
            DriverStatus status,
            LocalDate expiringBefore,
            Pageable pageable
    );

    long countByStatus(DriverStatus status);

    @Query("SELECT COUNT(d) FROM Driver d WHERE d.licenseExpiryDate <= :date AND d.status <> in.joyway.domain.enums.DriverStatus.INACTIVE")
    long countWithLicenseExpiringBy(LocalDate date);
}
