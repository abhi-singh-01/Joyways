package in.joyway.dto.admin;

import lombok.Builder;

import java.util.List;

@Builder
public record AdminProfileDto(
        Long id,
        String name,
        String email,
        List<String> roles
) {}
