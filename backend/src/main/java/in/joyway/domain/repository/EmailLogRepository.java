package in.joyway.domain.repository;

import in.joyway.domain.entity.EmailLog;
import in.joyway.domain.enums.EmailStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EmailLogRepository extends JpaRepository<EmailLog, Long> {

    boolean existsByTemplateAndRecipientAndRelatedEntityTypeAndRelatedEntityIdAndStatus(
            String template,
            String recipient,
            String relatedEntityType,
            Long relatedEntityId,
            EmailStatus status
    );

    List<EmailLog> findByStatusInAndAttemptCountLessThanOrderByCreatedAtAsc(
            List<EmailStatus> statuses,
            int maxAttempts
    );

    Page<EmailLog> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
