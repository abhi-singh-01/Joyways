package in.joyway.controller.admin;

import in.joyway.dto.ApiResponse;
import in.joyway.dto.admin.AdminBookingDetailDto;
import in.joyway.dto.admin.AdminBookingSummaryDto;
import in.joyway.dto.admin.AdminRefundRequest;
import in.joyway.dto.admin.AdminRefundResponse;
import in.joyway.dto.booking.BookingResponse;
import in.joyway.service.AdminBookingService;
import in.joyway.service.BookingService;
import in.joyway.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/bookings")
@RequiredArgsConstructor
public class AdminBookingController {

    private final AdminBookingService adminBookingService;
    private final BookingService bookingService;
    private final PaymentService paymentService;

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

    @PostMapping("/{id}/refund")
    public ApiResponse<AdminRefundResponse> refundBooking(
            @PathVariable Long id,
            @RequestBody(required = false) AdminRefundRequest request
    ) {
        AdminRefundResponse response = paymentService.refundBooking(
                id, request != null ? request : new AdminRefundRequest());
        return ApiResponse.success(response, "Refund submitted.");
    }
}
