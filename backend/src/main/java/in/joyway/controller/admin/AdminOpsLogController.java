package in.joyway.controller.admin;

import in.joyway.dto.ApiResponse;
import in.joyway.dto.PageResponse;
import in.joyway.dto.admin.AdminAuditLogDto;
import in.joyway.dto.admin.AdminEmailLogDto;
import in.joyway.service.AdminOpsLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminOpsLogController {

    private final AdminOpsLogService adminOpsLogService;

    @GetMapping("/audit-logs")
    public ApiResponse<PageResponse<AdminAuditLogDto>> listAuditLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size
    ) {
        return ApiResponse.success(adminOpsLogService.listAuditLogs(page, size));
    }

    @GetMapping("/email-logs")
    public ApiResponse<PageResponse<AdminEmailLogDto>> listEmailLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size
    ) {
        return ApiResponse.success(adminOpsLogService.listEmailLogs(page, size));
    }
}
