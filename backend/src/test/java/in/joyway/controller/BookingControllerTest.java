package in.joyway.controller;

import in.joyway.dto.booking.BookingReviewDto;
import in.joyway.exception.GlobalExceptionHandler;
import in.joyway.service.BookingQueryService;
import in.joyway.service.BookingService;
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
import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
}
