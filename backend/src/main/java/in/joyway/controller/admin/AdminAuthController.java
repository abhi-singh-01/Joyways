package in.joyway.controller.admin;

import in.joyway.dto.ApiResponse;
import in.joyway.dto.admin.AdminProfileDto;
import in.joyway.dto.admin.LoginRequest;
import in.joyway.service.AdminAuthService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/auth")
@RequiredArgsConstructor
public class AdminAuthController {

    private final AdminAuthService adminAuthService;

    @PostMapping("/login")
    public ApiResponse<AdminProfileDto> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletResponse response
    ) {
        AdminProfileDto profile = adminAuthService.login(request, response);
        return ApiResponse.success(profile, "Login successful.");
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(HttpServletResponse response) {
        adminAuthService.logout(response);
        return ApiResponse.success(null, "Logged out successfully.");
    }

    @GetMapping("/me")
    public ApiResponse<AdminProfileDto> me() {
        return ApiResponse.success(adminAuthService.getCurrentAdmin());
    }
}
