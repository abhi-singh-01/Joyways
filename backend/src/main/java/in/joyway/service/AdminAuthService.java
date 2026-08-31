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
import org.springframework.security.crypto.password.PasswordEncoder;
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
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public AdminProfileDto login(LoginRequest request, HttpServletResponse response) {
        String normalizedEmail = EmailUtils.normalize(request.getEmail());
        var userLookup = userRepository.findByEmail(normalizedEmail);
        boolean userFound = userLookup.isPresent();
        int hashLength = userLookup
                .map(User::getPasswordHash)
                .map(hash -> hash != null ? hash.length() : 0)
                .orElse(0);
        boolean hashPresent = userLookup
                .map(User::getPasswordHash)
                .map(hash -> hash != null && !hash.isBlank())
                .orElse(false);
        int passwordLength = request.getPassword() != null ? request.getPassword().length() : 0;
        boolean encoderMatches = userLookup
                .filter(user -> user.getPasswordHash() != null && request.getPassword() != null)
                .map(user -> passwordEncoder.matches(request.getPassword(), user.getPasswordHash()))
                .orElse(false);
        int authorityCount = userLookup
                .map(user -> user.getRoles() != null ? user.getRoles().size() : 0)
                .orElse(0);
        Boolean enabled = userLookup.map(User::getEnabled).orElse(null);

        log.info(
                "event=auth.login.diagnostic email={} userFound={} enabled={} authorityCount={} "
                        + "hashPresent={} hashLength={} passwordLength={} encoderMatches={} "
                        + "authManagerClass={} passwordEncoderClass={}",
                normalizedEmail,
                userFound,
                enabled,
                authorityCount,
                hashPresent,
                hashLength,
                passwordLength,
                encoderMatches,
                authenticationManager.getClass().getName(),
                passwordEncoder.getClass().getName()
        );

        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(normalizedEmail, request.getPassword())
            );
        } catch (BadCredentialsException ex) {
            String causeClass = ex.getCause() != null ? ex.getCause().getClass().getName() : "none";
            log.warn(
                    "event=auth.login.failed email={} stage=authenticate exceptionClass={} causeClass={} "
                            + "userFound={} encoderMatches={} hashLength={} passwordLength={}",
                    normalizedEmail,
                    ex.getClass().getName(),
                    causeClass,
                    userFound,
                    encoderMatches,
                    hashLength,
                    passwordLength
            );
            throw new BadCredentialsException("Invalid email or password.");
        }

        JoywayUserDetails userDetails = (JoywayUserDetails) authentication.getPrincipal();
        User user = userDetails.getUser();

        boolean isAdmin = user.getRoles().stream()
                .anyMatch(role -> "ROLE_ADMIN".equals(role.getName()));
        if (!isAdmin) {
            List<String> authorityNames = user.getRoles().stream()
                    .map(role -> role.getName())
                    .toList();
            log.warn(
                    "event=auth.login.failed email={} stage=role_check enabled={} authorityCount={} authorityNames={}",
                    normalizedEmail,
                    user.getEnabled(),
                    user.getRoles().size(),
                    authorityNames
            );
            throw new BadCredentialsException("Invalid email or password.");
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
