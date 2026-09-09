package in.joyway.service;

import in.joyway.config.JwtProperties;
import in.joyway.domain.entity.Role;
import in.joyway.domain.entity.User;
import in.joyway.domain.repository.UserRepository;
import in.joyway.dto.admin.AdminProfileDto;
import in.joyway.dto.admin.LoginRequest;
import in.joyway.mapper.AdminMapper;
import in.joyway.security.JwtAuthenticationFilter;
import in.joyway.security.JwtTokenProvider;
import in.joyway.security.JoywayUserDetails;
import in.joyway.util.CookieDomainNormalizer;
import in.joyway.util.EmailUtils;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
public class AdminAuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtProperties jwtProperties;
    private final UserRepository userRepository;
    private final AdminMapper adminMapper;

    @Transactional(readOnly = true)
    public AdminProfileDto login(LoginRequest request, HttpServletResponse response) {
        String normalizedEmail = EmailUtils.normalize(request.getEmail());

        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(normalizedEmail, request.getPassword())
            );
        } catch (BadCredentialsException ex) {
            log.warn("event=auth.login.failed email={} stage=authenticate", normalizedEmail);
            throw new BadCredentialsException("Invalid email or password.");
        }

        JoywayUserDetails userDetails = (JoywayUserDetails) authentication.getPrincipal();
        User user = userDetails.getUser();

        boolean isAdmin = user.getRoles().stream()
                .anyMatch(role -> "ROLE_ADMIN".equals(role.getName()));
        if (!isAdmin) {
            log.warn("event=auth.login.failed email={} stage=role_check", normalizedEmail);
            throw new BadCredentialsException("Invalid email or password.");
        }

        List<String> roles = user.getRoles().stream().map(Role::getName).toList();
        String token = jwtTokenProvider.generateToken(user.getEmail(), roles);

        ResponseCookie cookie = buildTokenCookie(token, Duration.ofMillis(jwtProperties.getExpirationMs()));
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        return adminMapper.toProfileDto(user);
    }

    public void logout(HttpServletResponse response) {
        ResponseCookie cookie = buildTokenCookie("", Duration.ZERO);
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        SecurityContextHolder.clearContext();
    }

    private ResponseCookie buildTokenCookie(String token, Duration maxAge) {
        String domain = CookieDomainNormalizer.forResponseCookie(jwtProperties.getCookieDomain());
        return ResponseCookie.from(JwtAuthenticationFilter.JWT_COOKIE_NAME, token)
                .httpOnly(true)
                .secure(jwtProperties.isCookieSecure())
                .domain(domain != null && !domain.isBlank() ? domain : null)
                .path("/")
                .sameSite("Lax")
                .maxAge(maxAge)
                .build();
    }

    @Transactional(readOnly = true)
    public AdminProfileDto getCurrentAdmin() {
        String email = getAuthenticatedEmail();
        User user = userRepository.findByEmail(EmailUtils.normalize(email))
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
