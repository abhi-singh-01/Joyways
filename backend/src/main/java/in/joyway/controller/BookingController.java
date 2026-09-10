package in.joyway.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import in.joyway.dto.ApiResponse;
import in.joyway.dto.booking.BookingResponse;
import in.joyway.dto.booking.BookingReviewDto;
import in.joyway.dto.booking.BookingAccessRequest;
import in.joyway.dto.booking.CreateBookingRequest;
import in.joyway.dto.review.PackageReviewItemDto;
import in.joyway.dto.review.SubmitPackageReviewRequest;
import in.joyway.exception.RequestValidationException;
import in.joyway.service.BookingQueryService;
import in.joyway.service.BookingService;
import in.joyway.service.PackageReviewService;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
    private final PackageReviewService packageReviewService;
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

    @PostMapping("/lookup")
    public ApiResponse<in.joyway.dto.booking.BookingDetailResponse> lookupBooking(
            @RequestBody BookingAccessRequest request
    ) {
        validateAccessRequest(request);
        return ApiResponse.success(
                bookingQueryService.getBookingForCustomer(request.getReference(), request.getEmail()));
    }

    @PostMapping("/receipt")
    public ApiResponse<in.joyway.dto.booking.BookingReceiptDto> lookupReceipt(
            @RequestBody BookingAccessRequest request
    ) {
        validateAccessRequest(request);
        return ApiResponse.success(
                bookingQueryService.getReceiptForCustomer(request.getReference(), request.getEmail()));
    }

    @PostMapping("/receipt.pdf")
    public ResponseEntity<byte[]> downloadReceiptPdf(@RequestBody BookingAccessRequest request) {
        validateAccessRequest(request);
        byte[] pdf = bookingQueryService.getReceiptPdfForCustomer(request.getReference(), request.getEmail());
        String filename = "joyway-receipt-" + request.getReference().trim().toUpperCase() + ".pdf";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @PostMapping("/cancel")
    public ApiResponse<BookingResponse> cancelBookingPost(
            @RequestBody BookingAccessRequest request
    ) {
        validateAccessRequest(request);
        BookingResponse response = bookingService.cancelBookingByCustomer(
                request.getReference(), request.getEmail());
        return ApiResponse.success(response, "Booking cancelled.");
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

    @PostMapping("/reviews")
    public ApiResponse<PackageReviewItemDto> submitReview(
            @RequestBody SubmitPackageReviewRequest request
    ) {
        validateReviewRequest(request);
        PackageReviewItemDto review = packageReviewService.submitReview(request);
        return ApiResponse.success(review, "Thank you. Your review is now on the trip page.");
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

    private void validateAccessRequest(BookingAccessRequest request) {
        Set<ConstraintViolation<BookingAccessRequest>> violations = validator.validate(request);
        if (!violations.isEmpty()) {
            Map<String, String> errors = new HashMap<>();
            for (ConstraintViolation<BookingAccessRequest> v : violations) {
                errors.put(v.getPropertyPath().toString(), v.getMessage());
            }
            throw new RequestValidationException(errors);
        }
    }

    private void validateReviewRequest(SubmitPackageReviewRequest request) {
        Set<ConstraintViolation<SubmitPackageReviewRequest>> violations = validator.validate(request);
        if (!violations.isEmpty()) {
            Map<String, String> errors = new HashMap<>();
            for (ConstraintViolation<SubmitPackageReviewRequest> v : violations) {
                errors.put(v.getPropertyPath().toString(), v.getMessage());
            }
            throw new RequestValidationException(errors);
        }
    }
}
