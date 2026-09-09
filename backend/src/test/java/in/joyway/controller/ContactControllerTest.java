package in.joyway.controller;

import in.joyway.exception.GlobalExceptionHandler;
import in.joyway.service.ContactEnquiryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ContactController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class ContactControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ContactEnquiryService contactEnquiryService;

    @MockitoBean
    private in.joyway.security.JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean
    private in.joyway.security.JwtTokenProvider jwtTokenProvider;

    @Test
    void submitEnquiry_returnsSuccess() throws Exception {
        doNothing().when(contactEnquiryService).submitEnquiry(any());

        mockMvc.perform(post("/api/contact/enquiry")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Jane Doe",
                                  "email": "visitor@example.com",
                                  "phone": "9876543210",
                                  "subject": "Group trip enquiry",
                                  "message": "Please share details for a weekend trip."
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Thank you. We received your message and will respond shortly."));
    }

    @Test
    void submitEnquiry_rejectsInvalidPayload() throws Exception {
        mockMvc.perform(post("/api/contact/enquiry")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "J",
                                  "email": "not-an-email",
                                  "phone": "123",
                                  "subject": "Hi",
                                  "message": "short"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"));
    }
}
