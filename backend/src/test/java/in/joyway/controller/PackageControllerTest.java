package in.joyway.controller;

import in.joyway.dto.package_.PackageDetailDto;
import in.joyway.dto.package_.PackageSummaryDto;
import in.joyway.exception.GlobalExceptionHandler;
import in.joyway.exception.ResourceNotFoundException;
import in.joyway.service.PackageService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import in.joyway.security.JwtAuthenticationFilter;
import in.joyway.security.JwtTokenProvider;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PackageController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class PackageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PackageService packageService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
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
}
