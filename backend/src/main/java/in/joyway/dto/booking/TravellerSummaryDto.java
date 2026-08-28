package in.joyway.dto.booking;

import lombok.Builder;

@Builder
public record TravellerSummaryDto(
        String fullName,
        String idDocumentType
) {}
