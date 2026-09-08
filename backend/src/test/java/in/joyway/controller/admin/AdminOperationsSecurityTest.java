package in.joyway.controller.admin;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminOperationsSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void vehiclesEndpoint_requiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/admin/vehicles"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void driversEndpoint_requiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/admin/drivers"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tripsEndpoint_requiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/admin/trips"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void auditLogsEndpoint_requiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/admin/audit-logs"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void emailLogsEndpoint_requiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/admin/email-logs"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createVehicle_requiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/admin/vehicles")
                        .contentType("application/json")
                        .content("""
                                {"registrationNumber":"DL01AB1234","vehicleType":"Bus","seatingCapacity":17,"ownershipType":"OWNED"}
                                """))
                // Unauthenticated mutating requests are blocked by CSRF before auth (403).
                .andExpect(status().isForbidden());
    }
}
