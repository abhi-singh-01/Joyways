package in.joyway.dto.payment;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreatePaymentOrderRequest {

    @NotBlank(message = "Booking reference is required")
    private String bookingReference;

    @NotBlank(message = "Customer email is required")
    @Email(message = "Valid customer email is required")
    private String customerEmail;
}
