package in.joyway.controller.admin;

import in.joyway.dto.PageResponse;
import in.joyway.dto.admin.AdminBookingDetailDto;
import in.joyway.dto.booking.BookingResponse;
import in.joyway.exception.GlobalExceptionHandler;
import in.joyway.service.AdminBookingService;
import in.joyway.service.BookingService;
import in.joyway.service.PaymentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminBookingController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class AdminBookingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminBookingService adminBookingService;
    @MockitoBean
    private BookingService bookingService;
    @MockitoBean
    private PaymentService paymentService;
    @MockitoBean
    private in.joyway.security.JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockitoBean
    private in.joyway.security.JwtTokenProvider jwtTokenProvider;

    @Test
    void listAndGetBooking_returnAdminPayload() throws Exception {
        when(adminBookingService.listBookings("priya", "CONFIRMED", "PAID", "Nainital", 0, 20))
                .thenReturn(PageResponse.of(org.springframework.data.domain.Page.empty()));
        when(adminBookingService.getBooking(8L)).thenReturn(
                AdminBookingDetailDto.builder()
                        .bookingId(8L)
                        .bookingReference("JW-2026-00018")
                        .pickupPoint("CP")
                        .travellers(List.of())
                        .build());

        mockMvc.perform(get("/api/admin/bookings")
                        .param("query", "priya")
                        .param("bookingStatus", "CONFIRMED")
                        .param("paymentStatus", "PAID")
                        .param("destination", "Nainital"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/admin/bookings/8"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pickupPoint").value("CP"));
    }

    @Test
    void cancelBooking_delegatesToBookingService() throws Exception {
        when(bookingService.cancelBookingByAdmin(8L)).thenReturn(
                BookingResponse.builder().bookingReference("JW-2026-00018").status("CANCELLED").build());

        mockMvc.perform(post("/api/admin/bookings/8/cancel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Booking cancelled."));
    }
}
