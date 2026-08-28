package in.joyway.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import in.joyway.dto.ApiResponse;
import in.joyway.dto.booking.BookingResponse;
import in.joyway.dto.booking.BookingReviewDto;
import in.joyway.dto.booking.CreateBookingRequest;
import in.joyway.exception.RequestValidationException;
import in.joyway.service.BookingQueryService;
import in.joyway.service.BookingService;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;
    private final BookingQueryService bookingQueryService;
    private final ObjectMapper objectMapper;
    private final Validator validator;

    @PostMapping("/preview")
    public ApiResponse<BookingReviewDto> previewBooking(
            @RequestBody CreateBookingRequest request
    ) {
        validate(request);
        return ApiResponse.success(bookingService.previewBooking(request));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<BookingResponse> createBooking(
            @RequestPart("request") String requestJson,
            @RequestPart("documents") List<MultipartFile> documents
    ) throws IOException {
        CreateBookingRequest request = objectMapper.readValue(requestJson, CreateBookingRequest.class);
        validate(request);
        BookingResponse response = bookingService.createBooking(request, documents);
        return ApiResponse.success(response, "Booking created. Proceed to payment.");
    }

    @GetMapping("/{reference}")
    public ApiResponse<in.joyway.dto.booking.BookingDetailResponse> getBooking(
            @PathVariable String reference,
            @RequestParam String email
    ) {
        return ApiResponse.success(bookingQueryService.getBookingForCustomer(reference, email));
    }

    @GetMapping("/{reference}/receipt")
    public ApiResponse<in.joyway.dto.booking.BookingReceiptDto> getReceipt(
            @PathVariable String reference,
            @RequestParam String email
    ) {
        return ApiResponse.success(bookingQueryService.getReceiptForCustomer(reference, email));
    }

    @PostMapping("/{reference}/cancel")
    public ApiResponse<BookingResponse> cancelBooking(
            @PathVariable String reference,
            @RequestParam String email
    ) {
        BookingResponse response = bookingService.cancelBookingByCustomer(reference, email);
        return ApiResponse.success(response, "Booking cancelled.");
    }

    private void validate(CreateBookingRequest request) {
        Set<ConstraintViolation<CreateBookingRequest>> violations = validator.validate(request);
        if (!violations.isEmpty()) {
            Map<String, String> errors = new HashMap<>();
            for (ConstraintViolation<CreateBookingRequest> v : violations) {
                errors.put(v.getPropertyPath().toString(), v.getMessage());
            }
            throw new RequestValidationException(errors);
        }
    }
}
