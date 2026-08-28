package in.joyway.dto.admin.operations;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AssignBookingToTripRequest {

    @NotNull(message = "Booking ID is required.")
    private Long bookingId;
}
