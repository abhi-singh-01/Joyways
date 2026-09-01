package in.joyway.domain.repository;

import in.joyway.domain.entity.PaymentWebhookEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentWebhookEventRepository extends JpaRepository<PaymentWebhookEvent, Long> {
    boolean existsByEventId(String eventId);

    boolean existsByPayloadHash(String payloadHash);

    Optional<PaymentWebhookEvent> findByEventId(String eventId);
}
