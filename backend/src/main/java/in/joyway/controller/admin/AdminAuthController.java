package in.joyway.controller.admin;

import in.joyway.dto.ApiResponse;
import in.joyway.dto.admin.AdminProfileDto;
import in.joyway.dto.admin.LoginRequest;
import in.joyway.service.AdminAuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/auth")
@RequiredArgsConstructor
public class AdminAuthController {

    private final AdminAuthService adminAuthService;
    private final CsrfTokenRepository csrfTokenRepository;

    @PostMapping("/login")
    public ApiResponse<AdminProfileDto> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse response
    ) {
        AdminProfileDto profile = adminAuthService.login(request, response);
        CsrfToken csrfToken = csrfTokenRepository.generateToken(httpRequest);
        csrfTokenRepository.saveToken(csrfToken, httpRequest, response);
        return ApiResponse.success(profile, "Login successful.");
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(HttpServletResponse response) {
        adminAuthService.logout(response);
        return ApiResponse.success(null, "Logged out successfully.");
    }

    @GetMapping("/me")
    public ApiResponse<AdminProfileDto> me(
            HttpServletRequest httpRequest,
            HttpServletResponse response
    ) {
        CsrfToken csrfToken = csrfTokenRepository.loadToken(httpRequest);
        if (csrfToken == null) {
            csrfToken = csrfTokenRepository.generateToken(httpRequest);
        }
        csrfTokenRepository.saveToken(csrfToken, httpRequest, response);
        return ApiResponse.success(adminAuthService.getCurrentAdmin());
    }
}
