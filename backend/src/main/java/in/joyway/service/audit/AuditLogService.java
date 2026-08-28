package in.joyway.service.audit;

import in.joyway.domain.entity.AuditLog;
import in.joyway.domain.entity.User;
import in.joyway.domain.repository.AuditLogRepository;
import in.joyway.security.JoywayUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    @Transactional
    public void log(String action, String entityType, Long entityId, String summary) {
        User actor = resolveCurrentUser();
        auditLogRepository.save(AuditLog.builder()
                .actorUser(actor)
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .summary(summary)
                .build());
    }

    private User resolveCurrentUser() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof JoywayUserDetails userDetails) {
            return userDetails.getUser();
        }
        return null;
    }
}
