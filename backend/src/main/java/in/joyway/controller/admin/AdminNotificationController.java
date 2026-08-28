package in.joyway.controller.admin;

import in.joyway.dto.ApiResponse;
import in.joyway.dto.admin.NotificationDto;
import in.joyway.dto.admin.UnreadCountDto;
import in.joyway.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/notifications")
@RequiredArgsConstructor
public class AdminNotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ApiResponse<List<NotificationDto>> listNotifications() {
        return ApiResponse.success(notificationService.listForCurrentAdmin());
    }

    @GetMapping("/unread-count")
    public ApiResponse<UnreadCountDto> unreadCount() {
        return ApiResponse.success(new UnreadCountDto(notificationService.unreadCountForCurrentAdmin()));
    }

    @PatchMapping("/{id}/read")
    public ApiResponse<Void> markRead(@PathVariable Long id) {
        notificationService.markRead(id);
        return ApiResponse.success(null, "Notification marked as read.");
    }

    @PatchMapping("/read-all")
    public ApiResponse<Void> markAllRead() {
        notificationService.markAllRead();
        return ApiResponse.success(null, "All notifications marked as read.");
    }
}
