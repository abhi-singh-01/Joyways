package in.joyway.domain.repository;

import in.joyway.domain.entity.Vehicle;
import in.joyway.domain.enums.VehicleStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import jakarta.persistence.LockModeType;
import java.util.Optional;

public interface VehicleRepository extends JpaRepository<Vehicle, Long> {

    boolean existsByRegistrationNumberIgnoreCase(String registrationNumber);

    Optional<Vehicle> findByRegistrationNumberIgnoreCase(String registrationNumber);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT v FROM Vehicle v WHERE v.id = :id")
    Optional<Vehicle> findByIdForUpdate(Long id);

    @Query("""
            SELECT v FROM Vehicle v
            WHERE (:status IS NULL OR v.status = :status)
            AND (:ownershipType IS NULL OR v.ownershipType = :ownershipType)
            AND (:vehicleType = '' OR LOWER(v.vehicleType) LIKE CONCAT('%', LOWER(CAST(:vehicleType AS string)), '%'))
            AND (:query = '' OR LOWER(v.registrationNumber) LIKE CONCAT('%', LOWER(CAST(:query AS string)), '%')
                 OR LOWER(v.vehicleType) LIKE CONCAT('%', LOWER(CAST(:query AS string)), '%'))
            """)
    Page<Vehicle> search(
            String query,
            VehicleStatus status,
            in.joyway.domain.enums.VehicleOwnershipType ownershipType,
            String vehicleType,
            Pageable pageable
    );

    long countByStatus(VehicleStatus status);
}
