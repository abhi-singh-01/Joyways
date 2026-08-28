package in.joyway.dto.package_;

import lombok.Builder;

import java.util.List;

@Builder
public record PackageImageDto(
        Long id,
        String imageUrl,
        String altText,
        Integer sortOrder
) {}
