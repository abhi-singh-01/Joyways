package in.joyway.domain.repository;

import in.joyway.domain.entity.TripVehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface TripVehicleRepository extends JpaRepository<TripVehicle, Long> {

    List<TripVehicle> findByTripId(Long tripId);

    Optional<TripVehicle> findByTripIdAndPrimaryTrue(Long tripId);

    @Query("""
            SELECT tv FROM TripVehicle tv
            JOIN FETCH tv.vehicle
            WHERE tv.trip.id = :tripId AND tv.primary = true
            """)
    Optional<TripVehicle> findPrimaryVehicle(Long tripId);

    @Query("""
            SELECT tv FROM TripVehicle tv
            JOIN FETCH tv.vehicle
            WHERE tv.primary = true AND tv.trip.id IN :tripIds
            """)
    List<TripVehicle> findPrimaryVehiclesByTripIdIn(Collection<Long> tripIds);

    void deleteByTripId(Long tripId);
}
