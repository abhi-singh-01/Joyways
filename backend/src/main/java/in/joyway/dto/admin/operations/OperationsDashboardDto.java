package in.joyway.dto.admin.operations;

import lombok.Builder;

import java.util.List;

@Builder
public record OperationsDashboardDto(
        long todaysTrips,
        long upcomingTrips,
        long availableVehicles,
        long assignedVehicles,
        long availableDrivers,
        long assignedDrivers,
        long tripsInProgress,
        long tripsCompletedToday,
        List<TripSummaryDto> upcomingTripList
) {}
