package in.joyway.controller;

import in.joyway.dto.ApiResponse;
import in.joyway.dto.health.HealthResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Liveness and readiness probes.
 *
 * <p>Responses deliberately contain only coarse status values. Connection strings,
 * credentials, driver messages and stack traces are never surfaced here because the
 * endpoints are publicly reachable.
 */
@RestController
@RequestMapping("/api/health")
@RequiredArgsConstructor
@Slf4j
public class HealthController {

    private static final String SERVICE_NAME = "joyway-backend";
    private static final int DB_VALIDATION_TIMEOUT_SECONDS = 2;

    private final DataSource dataSource;

    @Value("${joyway.version:0.0.1-SNAPSHOT}")
    private String version;

    /**
     * Liveness: answers whether the process is running. Deliberately does not touch the
     * database, so a database outage never causes the container to be killed and restarted.
     */
    @GetMapping
    public ApiResponse<HealthResponse> health() {
        return ApiResponse.success(HealthResponse.builder()
                .status("UP")
                .service(SERVICE_NAME)
                .version(version)
                .build());
    }

    /**
     * Readiness: answers whether the instance can serve traffic. Returns 503 when the
     * database is unreachable so load balancers stop routing to this instance.
     */
    @GetMapping("/ready")
    public ResponseEntity<ApiResponse<HealthResponse>> readiness() {
        boolean databaseUp = isDatabaseReachable();
        HealthResponse body = HealthResponse.builder()
                .status(databaseUp ? "UP" : "DOWN")
                .service(SERVICE_NAME)
                .version(version)
                .database(databaseUp ? "UP" : "DOWN")
                .build();

        if (databaseUp) {
            return ResponseEntity.ok(ApiResponse.success(body));
        }
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ApiResponse.<HealthResponse>builder()
                        .success(false)
                        .data(body)
                        .message("Service is not ready.")
                        .errorCode("NOT_READY")
                        .build());
    }

    private boolean isDatabaseReachable() {
        try (Connection connection = dataSource.getConnection()) {
            return connection.isValid(DB_VALIDATION_TIMEOUT_SECONDS);
        } catch (SQLException ex) {
            // Log the type only: driver messages can contain host and credential details.
            log.error("event=health.database.unreachable reason={}", ex.getClass().getSimpleName());
            return false;
        }
    }
}
