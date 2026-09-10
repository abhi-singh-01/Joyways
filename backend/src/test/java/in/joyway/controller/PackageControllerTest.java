package in.joyway.controller;

import in.joyway.dto.package_.PackageDetailDto;
import in.joyway.dto.package_.PackageSummaryDto;
import in.joyway.dto.review.PackageReviewsResponse;
import in.joyway.dto.waitlist.WaitlistJoinRequest;
import in.joyway.dto.waitlist.WaitlistJoinResponse;
import in.joyway.exception.GlobalExceptionHandler;
import in.joyway.exception.ResourceNotFoundException;
import in.joyway.service.PackageReviewService;
import in.joyway.service.PackageService;
import in.joyway.service.WaitlistService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import in.joyway.security.JwtAuthenticationFilter;
import in.joyway.security.JwtTokenProvider;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PackageController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class PackageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PackageService packageService;

    @MockitoBean
    private WaitlistService waitlistService;

    @MockitoBean
    private PackageReviewService packageReviewService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @Test
    void listPackages_returnsPublishedPackages() throws Exception {
        PackageSummaryDto summary = PackageSummaryDto.builder()
                .id(1L)
                .title("Nainital Weekend (DEMO)")
                .slug("nainital-weekend-demo")
                .destination("Nainital")
                .duration("3 Days / 2 Nights")
                .departureCity("Delhi")
                .startDate(LocalDate.now().plusDays(14))
                .endDate(LocalDate.now().plusDays(16))
                .pricePerPerson(new BigDecimal("4999.00"))
                .availableSeats(18)
                .heroImageUrl("https://example.com/nainital.jpg")
                .heroImageAlt("Nainital")
                .build();

        when(packageService.getPublishedPackages()).thenReturn(List.of(summary));

        mockMvc.perform(get("/api/packages"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].destination").value("Nainital"));
    }

    @Test
    void getPackage_returnsPackageDetail() throws Exception {
        PackageDetailDto detail = PackageDetailDto.builder()
                .id(1L)
                .title("Nainital Weekend (DEMO)")
                .slug("nainital-weekend-demo")
                .destination("Nainital")
                .duration("3 Days / 2 Nights")
                .departureCity("Delhi")
                .startDate(LocalDate.now().plusDays(14))
                .endDate(LocalDate.now().plusDays(16))
                .pricePerPerson(new BigDecimal("4999.00"))
                .totalSeats(20)
                .availableSeats(18)
                .status("PUBLISHED")
                .images(List.of())
                .bookable(true)
                .build();

        when(packageService.getPublishedPackageBySlug("nainital-weekend-demo")).thenReturn(detail);

        mockMvc.perform(get("/api/packages/nainital-weekend-demo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.slug").value("nainital-weekend-demo"));
    }

    @Test
    void getPackage_notFound_returns404() throws Exception {
        when(packageService.getPublishedPackageBySlug("unknown"))
                .thenThrow(new ResourceNotFoundException("Trip not found or unavailable."));

        mockMvc.perform(get("/api/packages/unknown"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void getPackageById_returnsPackageDetail() throws Exception {
        PackageDetailDto detail = PackageDetailDto.builder()
                .id(1L)
                .title("Nainital Weekend (DEMO)")
                .slug("nainital-weekend-demo")
                .build();
        when(packageService.getPublishedPackageById(1L)).thenReturn(detail);

        mockMvc.perform(get("/api/packages/id/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    void listReviews_returnsPublishedReviews() throws Exception {
        when(packageReviewService.listPublishedReviews("nainital-weekend-demo"))
                .thenReturn(PackageReviewsResponse.builder()
                        .averageRating(new BigDecimal("4.5"))
                        .reviewCount(2)
                        .reviews(List.of())
                        .build());

        mockMvc.perform(get("/api/packages/nainital-weekend-demo/reviews"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.reviewCount").value(2))
                .andExpect(jsonPath("$.data.averageRating").value(4.5));
    }

    @Test
    void joinWaitlist_returnsJoinedMessage() throws Exception {
        when(waitlistService.join(eq("nainital-weekend-demo"), any(WaitlistJoinRequest.class)))
                .thenReturn(WaitlistJoinResponse.builder()
                        .alreadyJoined(false)
                        .email("priya@example.com")
                        .build());

        mockMvc.perform(post("/api/packages/nainital-weekend-demo/waitlist")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fullName": "Priya Sharma",
                                  "email": "priya@example.com",
                                  "mobile": "9876543210",
                                  "seatsRequested": 2
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.alreadyJoined").value(false))
                .andExpect(jsonPath("$.message")
                        .value("You are on the waitlist. We will email you if a seat opens."));
    }

    @Test
    void joinWaitlist_returnsAlreadyJoinedMessage() throws Exception {
        when(waitlistService.join(eq("nainital-weekend-demo"), any(WaitlistJoinRequest.class)))
                .thenReturn(WaitlistJoinResponse.builder()
                        .alreadyJoined(true)
                        .email("priya@example.com")
                        .build());

        mockMvc.perform(post("/api/packages/nainital-weekend-demo/waitlist")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fullName": "Priya Sharma",
                                  "email": "priya@example.com",
                                  "seatsRequested": 1
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message")
                        .value("You are already on the waitlist for this trip."));
    }
}
