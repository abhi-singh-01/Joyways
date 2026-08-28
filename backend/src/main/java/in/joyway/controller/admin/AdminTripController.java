package in.joyway.controller.admin;

import in.joyway.dto.ApiResponse;
import in.joyway.dto.PageResponse;
import in.joyway.dto.admin.operations.*;
import in.joyway.service.operations.AdminTripService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/admin/trips")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminTripController {

    private final AdminTripService tripService;

    @GetMapping
    public ApiResponse<PageResponse<TripSummaryDto>> listTrips(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate travelDate,
            @RequestParam(required = false) Long vehicleId,
            @RequestParam(required = false) Long driverId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.success(tripService.listTrips(
                query, status, travelDate, vehicleId, driverId, page, size));
    }

    @GetMapping("/calendar")
    public ApiResponse<List<TripSummaryDto>> calendarTrips(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate
    ) {
        return ApiResponse.success(tripService.listCalendarTrips(fromDate, toDate));
    }

    @GetMapping("/upcoming")
    public ApiResponse<List<TripSummaryDto>> upcomingTrips(
            @RequestParam(defaultValue = "10") int limit
    ) {
        return ApiResponse.success(tripService.listUpcomingTrips(limit));
    }

    @GetMapping("/{id}")
    public ApiResponse<TripDetailDto> getTrip(@PathVariable Long id) {
        return ApiResponse.success(tripService.getTrip(id));
    }

    @PostMapping
    public ApiResponse<TripDetailDto> createTrip(
            @Valid @RequestBody CreateTripRequest request
    ) {
        return ApiResponse.success(tripService.createTrip(request), "Trip created.");
    }

    @PutMapping("/{id}")
    public ApiResponse<TripDetailDto> updateTrip(
            @PathVariable Long id,
            @Valid @RequestBody UpdateTripRequest request
    ) {
        return ApiResponse.success(tripService.updateTrip(id, request), "Trip updated.");
    }

    @PostMapping("/{id}/assign-vehicle")
    public ApiResponse<TripDetailDto> assignVehicle(
            @PathVariable Long id,
            @Valid @RequestBody AssignVehicleRequest request
    ) {
        return ApiResponse.success(tripService.assignVehicle(id, request), "Vehicle assigned.");
    }

    @PostMapping("/{id}/assign-driver")
    public ApiResponse<TripDetailDto> assignDriver(
            @PathVariable Long id,
            @Valid @RequestBody AssignDriverRequest request
    ) {
        return ApiResponse.success(tripService.assignDriver(id, request), "Driver assigned.");
    }

    @PostMapping("/{id}/bookings")
    public ApiResponse<TripDetailDto> addBooking(
            @PathVariable Long id,
            @Valid @RequestBody AssignBookingToTripRequest request
    ) {
        return ApiResponse.success(tripService.addBooking(id, request), "Booking assigned to trip.");
    }

    @DeleteMapping("/{id}/bookings/{bookingId}")
    public ApiResponse<TripDetailDto> removeBooking(
            @PathVariable Long id,
            @PathVariable Long bookingId
    ) {
        return ApiResponse.success(tripService.removeBooking(id, bookingId), "Booking removed from trip.");
    }

    @PatchMapping("/{id}/status")
    public ApiResponse<TripDetailDto> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateTripStatusRequest request
    ) {
        return ApiResponse.success(tripService.updateStatus(id, request), "Trip status updated.");
    }
}
