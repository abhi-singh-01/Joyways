package in.joyway.controller.admin;

import in.joyway.dto.ApiResponse;
import in.joyway.dto.booking.BookingResponse;
import in.joyway.dto.admin.AdminBookingDetailDto;
import in.joyway.dto.admin.AdminBookingSummaryDto;
import in.joyway.service.AdminBookingService;
import in.joyway.service.BookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/bookings")
@RequiredArgsConstructor
public class AdminBookingController {

    private final AdminBookingService adminBookingService;
    private final BookingService bookingService;

    @GetMapping
    public ApiResponse<List<AdminBookingSummaryDto>> listBookings() {
        return ApiResponse.success(adminBookingService.listBookings());
    }

    @GetMapping("/{id}")
    public ApiResponse<AdminBookingDetailDto> getBooking(@PathVariable Long id) {
        return ApiResponse.success(adminBookingService.getBooking(id));
    }

    @PostMapping("/{id}/cancel")
    public ApiResponse<BookingResponse> cancelBooking(@PathVariable Long id) {
        BookingResponse response = bookingService.cancelBookingByAdmin(id);
        return ApiResponse.success(response, "Booking cancelled.");
    }
}
