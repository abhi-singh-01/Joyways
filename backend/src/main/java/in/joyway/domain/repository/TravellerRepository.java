package in.joyway.domain.repository;

import in.joyway.domain.entity.Traveller;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TravellerRepository extends JpaRepository<Traveller, Long> {
    List<Traveller> findByBookingId(Long bookingId);
}
