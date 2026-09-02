package in.joyway.domain.repository;

import in.joyway.domain.entity.PaymentWebhookEvent;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PaymentWebhookEventRepository extends JpaRepository<PaymentWebhookEvent, Long> {
    Optional<PaymentWebhookEvent> findByEventId(String eventId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT e FROM PaymentWebhookEvent e WHERE e.eventId = :eventId")
    Optional<PaymentWebhookEvent> findByEventIdForUpdate(@Param("eventId") String eventId);
}
