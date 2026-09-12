package in.joyway.dto.admin;

import in.joyway.dto.booking.CreateBookingRequest;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class AdminCreateBookingRequest extends CreateBookingRequest {

    @NotNull(message = "Payment mode is required")
    private AdminBookingPaymentMode paymentMode;

    @Size(max = 100, message = "Payment reference is too long")
    private String offlinePaymentReference;

    @Size(max = 500, message = "Notes must be 500 characters or fewer")
    private String notes;
}
