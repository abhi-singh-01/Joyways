package in.joyway.notification;

import in.joyway.config.MailProperties;
import in.joyway.domain.entity.Notification;
import in.joyway.domain.entity.User;
import in.joyway.domain.enums.NotificationType;
import in.joyway.domain.repository.NotificationRepository;
import in.joyway.domain.repository.UserRepository;
import in.joyway.dto.admin.NotificationDto;
import in.joyway.exception.ResourceNotFoundException;
import in.joyway.security.JoywayUserDetails;
import in.joyway.util.EmailUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final MailProperties mailProperties;

    public void notifyAdmins(
            NotificationType type,
            String title,
            String message,
            String relatedEntityType,
            Long relatedEntityId
    ) {
        List<User> admins = userRepository.findActiveAdmins();
        if (admins.isEmpty()) {
            userRepository.findByEmail(mailProperties.getAdminNotificationEmail())
                    .ifPresent(admin -> createIfAbsent(admin, type, title, message, relatedEntityType, relatedEntityId));
            return;
        }
        for (User admin : admins) {
            createIfAbsent(admin, type, title, message, relatedEntityType, relatedEntityId);
        }
    }

    private void createIfAbsent(
            User admin,
            NotificationType type,
            String title,
            String message,
            String relatedEntityType,
            Long relatedEntityId
    ) {
        if (notificationRepository.existsByRecipientUserIdAndTypeAndRelatedEntityTypeAndRelatedEntityId(
                admin.getId(), type, relatedEntityType, relatedEntityId)) {
            return;
        }
        notificationRepository.save(Notification.builder()
                .recipientUser(admin)
                .type(type)
                .title(title)
                .message(message)
                .relatedEntityType(relatedEntityType)
                .relatedEntityId(relatedEntityId)
                .read(false)
                .build());
    }

    @Transactional(readOnly = true)
    public List<NotificationDto> listForCurrentAdmin() {
        User admin = getCurrentAdminUser();
        return notificationRepository.findByRecipientUserIdOrderByCreatedAtDesc(admin.getId()).stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public long unreadCountForCurrentAdmin() {
        User admin = getCurrentAdminUser();
        return notificationRepository.countByRecipientUserIdAndReadFalse(admin.getId());
    }

    public void markRead(Long notificationId) {
        User admin = getCurrentAdminUser();
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found."));
        if (!notification.getRecipientUser().getId().equals(admin.getId())) {
            throw new ResourceNotFoundException("Notification not found.");
        }
        notification.setRead(true);
        notification.setReadAt(Instant.now());
    }

    public void markAllRead() {
        User admin = getCurrentAdminUser();
        notificationRepository.findByRecipientUserIdOrderByCreatedAtDesc(admin.getId()).stream()
                .filter(n -> !Boolean.TRUE.equals(n.getRead()))
                .forEach(n -> {
                    n.setRead(true);
                    n.setReadAt(Instant.now());
                });
    }

    private User getCurrentAdminUser() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getPrincipal() == null) {
            throw new ResourceNotFoundException("Admin not found.");
        }
        if (auth.getPrincipal() instanceof JoywayUserDetails userDetails) {
            return userDetails.getUser();
        }
        return userRepository.findByEmail(EmailUtils.normalize(auth.getName()))
                .orElseThrow(() -> new ResourceNotFoundException("Admin not found."));
    }

    private NotificationDto toDto(Notification notification) {
        return NotificationDto.builder()
                .id(notification.getId())
                .type(notification.getType().name())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .relatedEntityType(notification.getRelatedEntityType())
                .relatedEntityId(notification.getRelatedEntityId())
                .read(Boolean.TRUE.equals(notification.getRead()))
                .createdAt(notification.getCreatedAt())
                .readAt(notification.getReadAt())
                .build();
    }
}
