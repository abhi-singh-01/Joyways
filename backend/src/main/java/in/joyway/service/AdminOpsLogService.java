package in.joyway.service;

import in.joyway.domain.entity.AuditLog;
import in.joyway.domain.entity.EmailLog;
import in.joyway.domain.repository.AuditLogRepository;
import in.joyway.domain.repository.EmailLogRepository;
import in.joyway.dto.PageResponse;
import in.joyway.dto.admin.AdminAuditLogDto;
import in.joyway.dto.admin.AdminEmailLogDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminOpsLogService {

    private final AuditLogRepository auditLogRepository;
    private final EmailLogRepository emailLogRepository;

    public PageResponse<AdminAuditLogDto> listAuditLogs(int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100));
        return PageResponse.of(auditLogRepository.findAllByOrderByCreatedAtDesc(pageable).map(this::toAuditDto));
    }

    public PageResponse<AdminEmailLogDto> listEmailLogs(int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100));
        return PageResponse.of(emailLogRepository.findAllByOrderByCreatedAtDesc(pageable).map(this::toEmailDto));
    }

    private AdminAuditLogDto toAuditDto(AuditLog log) {
        String actorName = log.getActorUser() != null ? log.getActorUser().getName() : "System";
        String actorEmail = log.getActorUser() != null ? log.getActorUser().getEmail() : null;
        return AdminAuditLogDto.builder()
                .id(log.getId())
                .actorName(actorName)
                .actorEmail(actorEmail)
                .action(log.getAction())
                .entityType(log.getEntityType())
                .entityId(log.getEntityId())
                .summary(log.getSummary())
                .createdAt(log.getCreatedAt())
                .build();
    }

    private AdminEmailLogDto toEmailDto(EmailLog log) {
        return AdminEmailLogDto.builder()
                .id(log.getId())
                .recipient(log.getRecipient())
                .subject(log.getSubject())
                .template(log.getTemplate())
                .status(log.getStatus().name())
                .attemptCount(log.getAttemptCount() != null ? log.getAttemptCount() : 0)
                .sentAt(log.getSentAt())
                .lastAttemptedAt(log.getLastAttemptedAt())
                .createdAt(log.getCreatedAt())
                .failureReason(log.getFailureReason())
                .build();
    }
}
