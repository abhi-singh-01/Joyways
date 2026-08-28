package in.joyway.dto.admin;

import in.joyway.dto.admin.operations.TripSummaryDto;
import lombok.Builder;

import java.math.BigDecimal;
import java.util.List;

@Builder
public record DashboardDto(
        long totalBookings,
        long confirmedBookings,
        long pendingPayments,
        long cancelledBookings,
        BigDecimal revenue,
        long activePackages,
        long upcomingTrips,
        long availableSeats,
        long todaysTrips,
        long upcomingOperationalTrips,
        long availableVehicles,
        long assignedVehicles,
        long availableDrivers,
        long assignedDrivers,
        long tripsInProgress,
        long tripsCompletedToday,
        /** Captured payments whose booking is not CONFIRMED — needs operator review. */
        long paidButNotConfirmed,
        /** Bookings holding seats without a settled payment — needs operator review. */
        long confirmedButNotPaid,
        List<TripSummaryDto> upcomingTripList
) {}
