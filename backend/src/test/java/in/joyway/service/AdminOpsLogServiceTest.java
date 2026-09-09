package in.joyway.service;

import in.joyway.domain.entity.AuditLog;
import in.joyway.domain.entity.EmailLog;
import in.joyway.domain.enums.EmailStatus;
import in.joyway.domain.repository.AuditLogRepository;
import in.joyway.domain.repository.EmailLogRepository;
import in.joyway.dto.admin.AdminEmailLogDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminOpsLogServiceTest {

    @Mock
    private AuditLogRepository auditLogRepository;
    @Mock
    private EmailLogRepository emailLogRepository;

    @InjectMocks
    private AdminOpsLogService adminOpsLogService;

    @Test
    void listLogs_clampsPagingAndMapsRows() {
        AuditLog audit = AuditLog.builder()
                .id(1L)
                .action("LOGIN")
                .entityType("USER")
                .entityId(2L)
                .summary("ok")
                .createdAt(Instant.parse("2026-09-09T12:00:00Z"))
                .build();
        EmailLog email = EmailLog.builder()
                .id(3L)
                .recipient("a@b.c")
                .subject("hi")
                .template("welcome")
                .status(EmailStatus.SENT)
                .attemptCount(1)
                .createdAt(Instant.parse("2026-09-09T12:00:00Z"))
                .build();
        when(auditLogRepository.findAllByOrderByCreatedAtDesc(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(audit)));
        when(emailLogRepository.findAllByOrderByCreatedAtDesc(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(email)));

        assertThat(adminOpsLogService.listAuditLogs(-1, 0).content()).hasSize(1);
        assertThat(adminOpsLogService.listEmailLogs(0, 500).content())
                .extracting(AdminEmailLogDto::template)
                .containsExactly("welcome");
    }
}
