package in.joyway.security;

import in.joyway.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date; // NOSONAR java:S2143 — JJWT 0.12 issuedAt/expiration still require Date
import java.util.List;

@Component
@RequiredArgsConstructor
@SuppressWarnings("java:S2143") // JJWT 0.12 issuedAt/expiration still require java.util.Date
public class JwtTokenProvider {

    private final JwtProperties jwtProperties;

    public String generateToken(String email, List<String> roles) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(email)
                .claim("roles", roles)
                .issuedAt(toJwtDate(now))
                .expiration(toJwtDate(now.plusMillis(jwtProperties.getExpirationMs())))
                .signWith(getSigningKey())
                .compact();
    }

    public String getEmailFromToken(String token) {
        return getClaims(token).getSubject();
    }

    @SuppressWarnings("unchecked")
    public List<String> getRolesFromToken(String token) {
        return getClaims(token).get("roles", List.class);
    }

    public boolean validateToken(String token) {
        try {
            getClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getSigningKey() {
        byte[] keyBytes = jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    @SuppressWarnings("java:S2143")
    private static Date toJwtDate(Instant instant) {
        return Date.from(instant); // NOSONAR — JJWT ClaimsMutator only accepts Date
    }
}
