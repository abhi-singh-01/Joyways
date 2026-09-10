package in.joyway.domain.repository;

import in.joyway.domain.entity.TripBooking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface TripBookingRepository extends JpaRepository<TripBooking, Long> {

    List<TripBooking> findByTripId(Long tripId);

    @Query("""
            SELECT tb FROM TripBooking tb
            JOIN FETCH tb.booking b
            JOIN FETCH b.customer
            WHERE tb.trip.id = :tripId
            ORDER BY tb.assignedAt ASC
            """)
    List<TripBooking> findByTripIdWithDetails(Long tripId);

    @Query("""
            SELECT tb FROM TripBooking tb
            JOIN FETCH tb.trip t
            LEFT JOIN FETCH t.driver
            LEFT JOIN FETCH t.tripVehicles tv
            LEFT JOIN FETCH tv.vehicle
            WHERE tb.booking.id = :bookingId
            """)
    Optional<TripBooking> findByBookingIdWithTripDetails(Long bookingId);

    boolean existsByBookingId(Long bookingId);

    @Query("""
            SELECT COALESCE(SUM(tb.booking.numberOfTravellers), 0) FROM TripBooking tb
            WHERE tb.trip.id = :tripId
            """)
    int sumTravellersByTripId(Long tripId);

    @Query("""
            SELECT tb.trip.id AS tripId,
                   COALESCE(SUM(b.numberOfTravellers), 0) AS travellerCount,
                   COUNT(tb.id) AS bookingCount
            FROM TripBooking tb
            JOIN tb.booking b
            WHERE tb.trip.id IN :tripIds
            GROUP BY tb.trip.id
            """)
    List<TripLoadSummary> summarizeByTripIds(Collection<Long> tripIds);

    void deleteByTripIdAndBookingId(Long tripId, Long bookingId);

    interface TripLoadSummary {
        Long getTripId();
        Long getTravellerCount();
        Long getBookingCount();
    }
}
