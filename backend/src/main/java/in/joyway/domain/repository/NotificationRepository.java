package in.joyway.domain.repository;

import in.joyway.domain.entity.Notification;
import in.joyway.domain.enums.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByRecipientUserIdOrderByCreatedAtDesc(Long recipientUserId);

    long countByRecipientUserIdAndReadFalse(Long recipientUserId);

    boolean existsByRecipientUserIdAndTypeAndRelatedEntityTypeAndRelatedEntityId(
            Long recipientUserId,
            NotificationType type,
            String relatedEntityType,
            Long relatedEntityId
    );
}
