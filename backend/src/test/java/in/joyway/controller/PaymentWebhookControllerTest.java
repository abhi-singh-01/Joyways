package in.joyway.controller;

import in.joyway.exception.GlobalExceptionHandler;
import in.joyway.observability.WebhookMetricsRecorder;
import in.joyway.service.PaymentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PaymentWebhookController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class PaymentWebhookControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PaymentService paymentService;

    @MockitoBean
    private WebhookMetricsRecorder webhookMetricsRecorder;

    @MockitoBean
    private in.joyway.security.JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean
    private in.joyway.security.JwtTokenProvider jwtTokenProvider;

    @Test
    void missingSignature_returns400AndDoesNotCallService() throws Exception {
        mockMvc.perform(post("/api/payments/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")
                        .header("X-Razorpay-Event-Id", "evt_missing_sig"))
                .andExpect(status().isBadRequest());

        verify(webhookMetricsRecorder, times(1)).recordMissingSignature();
        verify(webhookMetricsRecorder, never()).recordMissingEventId();
        verify(paymentService, never()).processWebhook(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void blankSignature_returns400RecordsMissingSignatureMetricAndDoesNotCallService() throws Exception {
        mockMvc.perform(post("/api/payments/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")
                        .header("X-Razorpay-Signature", "   ")
                        .header("X-Razorpay-Event-Id", "evt_blank_sig"))
                .andExpect(status().isBadRequest());

        verify(webhookMetricsRecorder, times(1)).recordMissingSignature();
        verify(webhookMetricsRecorder, never()).recordMissingEventId();
        verify(paymentService, never()).processWebhook(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void missingEventId_returns400RecordsMetricAndDoesNotCallService() throws Exception {
        mockMvc.perform(post("/api/payments/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")
                        .header("X-Razorpay-Signature", "sig"))
                .andExpect(status().isBadRequest());

        verify(webhookMetricsRecorder).recordMissingEventId();
        verify(webhookMetricsRecorder, never()).recordMissingSignature();
        verify(paymentService, never()).processWebhook(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void blankEventId_returns400RecordsMetricAndDoesNotCallService() throws Exception {
        mockMvc.perform(post("/api/payments/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")
                        .header("X-Razorpay-Signature", "sig")
                        .header("X-Razorpay-Event-Id", "   "))
                .andExpect(status().isBadRequest());

        verify(webhookMetricsRecorder).recordMissingEventId();
        verify(webhookMetricsRecorder, never()).recordMissingSignature();
        verify(paymentService, never()).processWebhook(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void validHeaders_delegatesToService() throws Exception {
        mockMvc.perform(post("/api/payments/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"event\":\"payment.captured\"}")
                        .header("X-Razorpay-Signature", "sig")
                        .header("X-Razorpay-Event-Id", "evt_ok"))
                .andExpect(status().isOk());

        verify(paymentService).processWebhook("{\"event\":\"payment.captured\"}", "sig", "evt_ok");
    }
}
