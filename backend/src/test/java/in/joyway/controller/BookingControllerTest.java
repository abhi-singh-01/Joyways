package in.joyway.controller;

import in.joyway.dto.booking.BookingDetailResponse;
import in.joyway.dto.booking.BookingReceiptDto;
import in.joyway.dto.booking.BookingResponse;
import in.joyway.dto.booking.BookingReviewDto;
import in.joyway.dto.review.PackageReviewItemDto;
import in.joyway.exception.GlobalExceptionHandler;
import in.joyway.service.BookingQueryService;
import in.joyway.service.BookingService;
import in.joyway.service.PackageReviewService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BookingController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class BookingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BookingService bookingService;

    @MockitoBean
    private BookingQueryService bookingQueryService;

    @MockitoBean
    private PackageReviewService packageReviewService;

    @MockitoBean
    private in.joyway.security.JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean
    private in.joyway.security.JwtTokenProvider jwtTokenProvider;

    @Test
    void previewBooking_returnsReview() throws Exception {
        when(bookingService.previewBooking(any())).thenReturn(
                BookingReviewDto.builder()
                        .packageId(1L)
                        .packageTitle("Test Trip")
                        .destination("Nainital")
                        .startDate(LocalDate.now().plusDays(14))
                        .endDate(LocalDate.now().plusDays(16))
                        .duration("3D/2N")
                        .numberOfTravellers(1)
                        .pricePerPerson(new BigDecimal("4999"))
                        .subtotal(new BigDecimal("4999"))
                        .taxesAndFees(BigDecimal.ZERO)
                        .totalAmount(new BigDecimal("4999"))
                        .currency("INR")
                        .availableSeats(10)
                        .bookable(true)
                        .build()
        );

        String body = """
                {
                  "packageId": 1,
                  "numberOfTravellers": 1,
                  "customer": {
                    "fullName": "John Doe",
                    "email": "john@example.com",
                    "mobile": "9876543210"
                  },
                  "travellers": [{
                    "fullName": "John Doe",
                    "dateOfBirth": "1990-01-01",
                    "gender": "Male",
                    "mobile": "9876543210",
                    "email": "john@example.com",
                    "emergencyContact": "9999999999",
                    "idDocumentType": "AADHAAR",
                    "idDocumentReference": "XXXX1234"
                  }]
                }
                """;

        mockMvc.perform(post("/api/bookings/preview")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalAmount").value(4999));
    }

    @Test
    void previewBooking_returnsFieldErrorsForInvalidPayload() throws Exception {
        mockMvc.perform(post("/api/bookings/preview")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors.packageId").exists());
    }

    @Test
    void previewBooking_returnsBadRequestForMalformedJson() throws Exception {
        mockMvc.perform(post("/api/bookings/preview")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ not json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("MALFORMED_REQUEST"));
    }

    @Test
    void getBooking_returnsBadRequestWhenEmailMissing() throws Exception {
        mockMvc.perform(get("/api/bookings/JW-2026-00001"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("MISSING_PARAMETER"));
    }

    @Test
    void createBooking_returnsBadRequestWhenDocumentsPartMissing() throws Exception {
        MockMultipartFile requestPart = new MockMultipartFile(
                "request", "", MediaType.APPLICATION_JSON_VALUE, "{}".getBytes());

        mockMvc.perform(multipart("/api/bookings").file(requestPart))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("MISSING_REQUEST_PART"));
    }

    @Test
    void cancelBooking_returnsMethodNotAllowedForWrongVerb() throws Exception {
        mockMvc.perform(get("/api/bookings/JW-2026-00001/cancel").param("email", "a@b.com"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.errorCode").value("METHOD_NOT_ALLOWED"));
    }

    @Test
    void lookupBooking_returnsDetail() throws Exception {
        when(bookingQueryService.getBookingForCustomer("JW-2026-00018", "priya@example.com"))
                .thenReturn(BookingDetailResponse.builder()
                        .bookingReference("JW-2026-00018")
                        .packageTitle("Nainital")
                        .pickupPoint("CP")
                        .reviewEligible(true)
                        .reviewSubmitted(false)
                        .build());

        mockMvc.perform(post("/api/bookings/lookup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"reference":"JW-2026-00018","email":"priya@example.com"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pickupPoint").value("CP"))
                .andExpect(jsonPath("$.data.reviewEligible").value(true));
    }

    @Test
    void lookupBooking_rejectsInvalidAccessPayload() throws Exception {
        mockMvc.perform(post("/api/bookings/lookup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"));
    }

    @Test
    void lookupReceipt_returnsReceipt() throws Exception {
        when(bookingQueryService.getReceiptForCustomer("JW-2026-00018", "priya@example.com"))
                .thenReturn(BookingReceiptDto.builder().title("Receipt").build());

        mockMvc.perform(post("/api/bookings/receipt")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"reference":"JW-2026-00018","email":"priya@example.com"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Receipt"));
    }

    @Test
    void downloadReceiptPdf_returnsPdfBytes() throws Exception {
        when(bookingQueryService.getReceiptPdfForCustomer("JW-2026-00018", "priya@example.com"))
                .thenReturn(new byte[]{1, 2});

        mockMvc.perform(post("/api/bookings/receipt.pdf")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"reference":"JW-2026-00018","email":"priya@example.com"}
                                """))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string("Content-Disposition",
                        "attachment; filename=\"joyway-receipt-JW-2026-00018.pdf\""));
    }

    @Test
    void cancelBookingPost_cancelsForCustomer() throws Exception {
        when(bookingService.cancelBookingByCustomer("JW-2026-00018", "priya@example.com"))
                .thenReturn(BookingResponse.builder()
                        .bookingReference("JW-2026-00018")
                        .status("CANCELLED")
                        .build());

        mockMvc.perform(post("/api/bookings/cancel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"reference":"JW-2026-00018","email":"priya@example.com"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Booking cancelled."));
    }

    @Test
    void getBookingAndReceipt_byReference() throws Exception {
        when(bookingQueryService.getBookingForCustomer("JW-2026-00018", "priya@example.com"))
                .thenReturn(BookingDetailResponse.builder().bookingReference("JW-2026-00018").build());
        when(bookingQueryService.getReceiptForCustomer("JW-2026-00018", "priya@example.com"))
                .thenReturn(BookingReceiptDto.builder().title("Receipt").build());

        mockMvc.perform(get("/api/bookings/JW-2026-00018").param("email", "priya@example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.bookingReference").value("JW-2026-00018"));
        mockMvc.perform(get("/api/bookings/JW-2026-00018/receipt").param("email", "priya@example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Receipt"));
    }

    @Test
    void cancelBookingByReference_cancels() throws Exception {
        when(bookingService.cancelBookingByCustomer("JW-2026-00018", "priya@example.com"))
                .thenReturn(BookingResponse.builder().bookingReference("JW-2026-00018").build());

        mockMvc.perform(post("/api/bookings/JW-2026-00018/cancel").param("email", "priya@example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void submitReview_returnsThankYou() throws Exception {
        when(packageReviewService.submitReview(any())).thenReturn(
                PackageReviewItemDto.builder()
                        .customerName("Priya")
                        .rating(5)
                        .comment("Great")
                        .createdAt(Instant.parse("2026-09-10T10:00:00Z"))
                        .build());

        mockMvc.perform(post("/api/bookings/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "reference": "JW-2026-00018",
                                  "email": "priya@example.com",
                                  "rating": 5,
                                  "comment": "Great"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.rating").value(5))
                .andExpect(jsonPath("$.message").value("Thank you. Your review is now on the trip page."));
    }

    @Test
    void submitReview_rejectsInvalidPayload() throws Exception {
        mockMvc.perform(post("/api/bookings/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"));
    }

    @Test
    void createBooking_acceptsMultipartPayload() throws Exception {
        MockMultipartFile requestPart = new MockMultipartFile(
                "request", "", MediaType.APPLICATION_JSON_VALUE, """
                {
                  "packageId": 1,
                  "numberOfTravellers": 1,
                  "consentAccepted": true,
                  "pickupPoint": "CP",
                  "customer": {
                    "fullName": "John Doe",
                    "email": "john@example.com",
                    "mobile": "9876543210"
                  },
                  "travellers": [{
                    "fullName": "John Doe",
                    "dateOfBirth": "1990-01-01",
                    "gender": "Male",
                    "mobile": "9876543210",
                    "email": "john@example.com",
                    "emergencyContact": "9999999999",
                    "idDocumentType": "AADHAAR",
                    "idDocumentReference": "XXXX1234"
                  }]
                }
                """.getBytes());
        MockMultipartFile document = new MockMultipartFile(
                "documents", "id.pdf", MediaType.APPLICATION_PDF_VALUE, "pdf".getBytes());
        when(bookingService.createBooking(any(), any())).thenReturn(
                BookingResponse.builder().bookingReference("JW-2026-00018").pickupPoint("CP").build());

        mockMvc.perform(multipart("/api/bookings").file(requestPart).file(document))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pickupPoint").value("CP"));
    }
}
