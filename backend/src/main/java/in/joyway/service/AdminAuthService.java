package in.joyway.service;

import in.joyway.config.JwtProperties;
import in.joyway.domain.entity.User;
import in.joyway.domain.repository.UserRepository;
import in.joyway.dto.admin.AdminProfileDto;
import in.joyway.dto.admin.LoginRequest;
import in.joyway.mapper.AdminMapper;
import in.joyway.security.JwtAuthenticationFilter;
import in.joyway.security.JwtTokenProvider;
import in.joyway.security.JoywayUserDetails;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminAuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtProperties jwtProperties;
    private final UserRepository userRepository;
    private final AdminMapper adminMapper;

    @Transactional(readOnly = true)
    public AdminProfileDto login(LoginRequest request, HttpServletResponse response) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );

            JoywayUserDetails userDetails = (JoywayUserDetails) authentication.getPrincipal();
            User user = userDetails.getUser();

            boolean isAdmin = user.getRoles().stream()
                    .anyMatch(role -> "ROLE_ADMIN".equals(role.getName()));
            if (!isAdmin) {
                throw new BadCredentialsException("Invalid credentials.");
            }

            List<String> roles = user.getRoles().stream().map(r -> r.getName()).toList();
            String token = jwtTokenProvider.generateToken(user.getEmail(), roles);

            ResponseCookie cookie = ResponseCookie.from(JwtAuthenticationFilter.JWT_COOKIE_NAME, token)
                    .httpOnly(true)
                    .secure(jwtProperties.isCookieSecure())
                    .path("/")
                    .sameSite("Lax")
                    .maxAge(Duration.ofMillis(jwtProperties.getExpirationMs()))
                    .build();
            response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

            return adminMapper.toProfileDto(user);
        } catch (BadCredentialsException ex) {
            throw new BadCredentialsException("Invalid email or password.");
        }
    }

    public void logout(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from(JwtAuthenticationFilter.JWT_COOKIE_NAME, "")
                .httpOnly(true)
                .secure(jwtProperties.isCookieSecure())
                .path("/")
                .sameSite("Lax")
                .maxAge(0)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        SecurityContextHolder.clearContext();
    }

    @Transactional(readOnly = true)
    public AdminProfileDto getCurrentAdmin() {
        String email = getAuthenticatedEmail();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadCredentialsException("Session expired."));
        return adminMapper.toProfileDto(user);
    }

    private String getAuthenticatedEmail() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getPrincipal() == null) {
            throw new BadCredentialsException("Not authenticated.");
        }
        return auth.getName();
    }
}
