package in.joyway.dto.booking;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class BookingAccessRequest {

    @NotBlank(message = "Booking reference is required")
    @Pattern(regexp = "^JW-\\d{4}-\\d{5}$", message = "Enter a valid booking reference")
    private String reference;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;
}
