package in.joyway.controller.admin;

import in.joyway.dto.admin.AdminProfileDto;
import in.joyway.dto.admin.LoginRequest;
import in.joyway.exception.GlobalExceptionHandler;
import in.joyway.service.AdminAuthService;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminAuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class AdminAuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminAuthService adminAuthService;

    @MockitoBean
    private in.joyway.security.JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean
    private in.joyway.security.JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private CsrfTokenRepository csrfTokenRepository;

    @Test
    void login_success() throws Exception {
        CsrfToken csrfToken = mock(CsrfToken.class);
        when(csrfTokenRepository.generateToken(any())).thenReturn(csrfToken);
        when(adminAuthService.login(any(LoginRequest.class), any(HttpServletResponse.class)))
                .thenReturn(AdminProfileDto.builder()
                        .id(1L)
                        .name("Admin")
                        .email("admin@joyway.in")
                        .roles(List.of("ROLE_ADMIN"))
                        .build());

        mockMvc.perform(post("/api/admin/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"admin@joyway.in\",\"password\":\"secret123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value("admin@joyway.in"));
    }

    @Test
    void login_validationError() throws Exception {
        mockMvc.perform(post("/api/admin/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"\",\"password\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void logout_success() throws Exception {
        doNothing().when(adminAuthService).logout(any(HttpServletResponse.class));

        mockMvc.perform(post("/api/admin/auth/logout"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
