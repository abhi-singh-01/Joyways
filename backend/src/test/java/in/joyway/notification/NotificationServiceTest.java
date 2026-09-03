package in.joyway.notification;

import in.joyway.config.MailProperties;
import in.joyway.domain.entity.Notification;
import in.joyway.domain.entity.User;
import in.joyway.domain.enums.NotificationType;
import in.joyway.domain.repository.NotificationRepository;
import in.joyway.domain.repository.UserRepository;
import in.joyway.exception.ResourceNotFoundException;
import in.joyway.security.JoywayUserDetails;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private MailProperties mailProperties;

    @InjectMocks
    private NotificationService notificationService;

    private User admin;

    @BeforeEach
    void setUp() {
        admin = User.builder().id(1L).email("admin@joyway.in").name("Admin").enabled(true).build();
        JoywayUserDetails userDetails = new JoywayUserDetails(admin);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities())
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void unreadCount_resolvesAdminFromJwtEmailPrincipal() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("admin@joyway.in", null, List.of())
        );
        when(userRepository.findByEmail("admin@joyway.in")).thenReturn(Optional.of(admin));
        when(notificationRepository.countByRecipientUserIdAndReadFalse(1L)).thenReturn(3L);

        long count = notificationService.unreadCountForCurrentAdmin();

        org.assertj.core.api.Assertions.assertThat(count).isEqualTo(3L);
    }

    @Test
    void markRead_rejectsNotificationForAnotherAdmin() {
        User otherAdmin = User.builder().id(2L).email("other@joyway.in").name("Other").build();
        Notification notification = Notification.builder()
                .id(10L)
                .recipientUser(otherAdmin)
                .type(NotificationType.NEW_BOOKING)
                .title("New booking")
                .read(false)
                .build();

        when(notificationRepository.findById(10L)).thenReturn(Optional.of(notification));

        assertThatThrownBy(() -> notificationService.markRead(10L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void notifyAdmins_createsNotificationForEachAdmin() {
        when(userRepository.findActiveAdmins()).thenReturn(List.of(admin));
        when(notificationRepository.existsByRecipientUserIdAndTypeAndRelatedEntityTypeAndRelatedEntityId(
                1L, NotificationType.NEW_BOOKING, "BOOKING", 5L
        )).thenReturn(false);

        notificationService.notifyAdmins(
                NotificationType.NEW_BOOKING,
                "New booking received",
                "JW-2026-00001",
                "BOOKING",
                5L
        );

        verify(notificationRepository).save(org.mockito.ArgumentMatchers.any(Notification.class));
    }
}
