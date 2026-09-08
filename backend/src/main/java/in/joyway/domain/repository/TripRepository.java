package in.joyway.domain.repository;

import in.joyway.domain.entity.Trip;
import in.joyway.domain.enums.TripStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface TripRepository extends JpaRepository<Trip, Long> {

    Optional<Trip> findByTripReference(String tripReference);

    boolean existsByTripReference(String tripReference);

    Optional<Trip> findFirstByTripReferenceStartingWithOrderByTripReferenceDesc(String prefix);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM Trip t WHERE t.id = :id")
    Optional<Trip> findByIdForUpdate(Long id);

    @Query("""
            SELECT DISTINCT t FROM Trip t
            JOIN FETCH t.travelPackage
            LEFT JOIN FETCH t.driver
            LEFT JOIN FETCH t.tripVehicles tv
            LEFT JOIN FETCH tv.vehicle
            WHERE t.id = :id
            """)
    Optional<Trip> findByIdWithDetails(Long id);

    @Query("""
            SELECT t FROM Trip t
            JOIN t.travelPackage p
            LEFT JOIN t.driver d
            WHERE (:status IS NULL OR t.status = :status)
            AND (:travelDate IS NULL OR t.travelDate = :travelDate)
            AND (:vehicleId IS NULL OR EXISTS (
                SELECT 1 FROM TripVehicle tv WHERE tv.trip = t AND tv.vehicle.id = :vehicleId))
            AND (:driverId IS NULL OR d.id = :driverId)
            AND (:query = '' OR LOWER(t.tripReference) LIKE CONCAT('%', LOWER(CAST(:query AS string)), '%')
                 OR LOWER(t.destination) LIKE CONCAT('%', LOWER(CAST(:query AS string)), '%'))
            """)
    Page<Trip> search(
            String query,
            TripStatus status,
            LocalDate travelDate,
            Long vehicleId,
            Long driverId,
            Pageable pageable
    );

    @Query("""
            SELECT t FROM Trip t
            WHERE t.travelDate >= :fromDate AND t.travelDate <= :toDate
            AND t.status NOT IN (in.joyway.domain.enums.TripStatus.CANCELLED)
            ORDER BY t.startAt ASC
            """)
    List<Trip> findByTravelDateBetween(LocalDate fromDate, LocalDate toDate);

    @Query("""
            SELECT COUNT(t) > 0 FROM Trip t
            WHERE t.id <> :tripId
            AND t.status NOT IN (in.joyway.domain.enums.TripStatus.COMPLETED, in.joyway.domain.enums.TripStatus.CANCELLED)
            AND t.startAt < :endAt AND t.endAt > :startAt
            AND EXISTS (SELECT 1 FROM TripVehicle tv WHERE tv.trip = t AND tv.vehicle.id = :vehicleId)
            """)
    boolean existsVehicleConflict(Long tripId, Long vehicleId, Instant startAt, Instant endAt);

    @Query("""
            SELECT COUNT(t) > 0 FROM Trip t
            WHERE t.id <> :tripId
            AND t.status NOT IN (in.joyway.domain.enums.TripStatus.COMPLETED, in.joyway.domain.enums.TripStatus.CANCELLED)
            AND t.startAt < :endAt AND t.endAt > :startAt
            AND t.driver.id = :driverId
            """)
    boolean existsDriverConflict(Long tripId, Long driverId, Instant startAt, Instant endAt);

    long countByStatus(TripStatus status);

    @Query("""
            SELECT COUNT(t) FROM Trip t
            WHERE t.travelDate = :date
            AND t.status NOT IN (in.joyway.domain.enums.TripStatus.CANCELLED)
            """)
    long countTripsOnDate(LocalDate date);

    @Query("""
            SELECT t FROM Trip t
            WHERE t.travelDate >= :fromDate
            AND t.status NOT IN (in.joyway.domain.enums.TripStatus.COMPLETED, in.joyway.domain.enums.TripStatus.CANCELLED)
            ORDER BY t.startAt ASC
            """)
    List<Trip> findUpcoming(LocalDate fromDate, Pageable pageable);

    @Query("""
            SELECT COUNT(t) FROM Trip t
            WHERE t.travelDate >= :fromDate
            AND t.status NOT IN (in.joyway.domain.enums.TripStatus.COMPLETED, in.joyway.domain.enums.TripStatus.CANCELLED)
            """)
    long countUpcoming(LocalDate fromDate);

    @Query("""
            SELECT t.tripReference FROM Trip t
            JOIN t.tripVehicles tv
            WHERE tv.vehicle.id = :vehicleId
            AND t.status NOT IN (in.joyway.domain.enums.TripStatus.COMPLETED, in.joyway.domain.enums.TripStatus.CANCELLED)
            ORDER BY t.startAt DESC
            """)
    List<String> findActiveTripReferencesForVehicle(Long vehicleId, Pageable pageable);

    @Query("""
            SELECT t.tripReference FROM Trip t
            WHERE t.driver.id = :driverId
            AND t.status NOT IN (in.joyway.domain.enums.TripStatus.COMPLETED, in.joyway.domain.enums.TripStatus.CANCELLED)
            ORDER BY t.startAt DESC
            """)
    List<String> findActiveTripReferencesForDriver(Long driverId, Pageable pageable);

    @Query("""
            SELECT t FROM Trip t
            WHERE t.travelPackage.id = :packageId
            AND t.travelDate = :travelDate
            AND t.status IN :statuses
            ORDER BY t.startAt ASC
            """)
    List<Trip> findAssignableForPackageAndDate(
            Long packageId,
            LocalDate travelDate,
            Collection<TripStatus> statuses
    );

    @Query("""
            SELECT COUNT(t) FROM Trip t
            WHERE t.status = in.joyway.domain.enums.TripStatus.COMPLETED
            AND t.travelDate = :date
            """)
    long countCompletedOnDate(LocalDate date);

    @Query("""
            SELECT COUNT(t) > 0 FROM Trip t
            JOIN t.tripVehicles tv
            WHERE tv.vehicle.id = :vehicleId
            AND t.id <> :tripId
            AND t.status NOT IN (in.joyway.domain.enums.TripStatus.COMPLETED, in.joyway.domain.enums.TripStatus.CANCELLED)
            """)
    boolean hasOtherActiveTripsForVehicle(Long vehicleId, Long tripId);

    @Query("""
            SELECT COUNT(t) > 0 FROM Trip t
            WHERE t.driver.id = :driverId
            AND t.id <> :tripId
            AND t.status NOT IN (in.joyway.domain.enums.TripStatus.COMPLETED, in.joyway.domain.enums.TripStatus.CANCELLED)
            """)
    boolean hasOtherActiveTripsForDriver(Long driverId, Long tripId);
}
