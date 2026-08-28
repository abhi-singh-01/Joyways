package in.joyway.dto.health;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

/**
 * Health payload. Intentionally limited to non-sensitive status values so the
 * endpoint can stay publicly reachable for container and Azure probes.
 */
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record HealthResponse(
        String status,
        String service,
        String version,
        String database
) {}
