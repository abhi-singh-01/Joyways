package in.joyway.controller.admin;

import in.joyway.dto.ApiResponse;
import in.joyway.dto.PageResponse;
import in.joyway.dto.admin.AdminBookingDetailDto;
import in.joyway.dto.admin.AdminBookingSummaryDto;
import in.joyway.dto.admin.AdminCreateBookingRequest;
import in.joyway.dto.admin.AdminRefundRequest;
import in.joyway.dto.admin.AdminRefundResponse;
import in.joyway.dto.booking.BookingResponse;
import in.joyway.service.AdminBookingService;
import in.joyway.service.BookingService;
import in.joyway.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@RequestMapping("/api/admin/bookings")
@RequiredArgsConstructor
public class AdminBookingController {

    private final AdminBookingService adminBookingService;
    private final BookingService bookingService;
    private final PaymentService paymentService;

    @GetMapping
    public ApiResponse<PageResponse<AdminBookingSummaryDto>> listBookings(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String bookingStatus,
            @RequestParam(required = false) String paymentStatus,
            @RequestParam(required = false) String destination,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.success(adminBookingService.listBookings(
                query, bookingStatus, paymentStatus, destination, page, size));
    }

    @PostMapping
    public ApiResponse<BookingResponse> createBooking(@Valid @RequestBody AdminCreateBookingRequest request)
            throws IOException {
        BookingResponse response = bookingService.createBookingByAdmin(request);
        return ApiResponse.success(response, "Booking created.");
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
