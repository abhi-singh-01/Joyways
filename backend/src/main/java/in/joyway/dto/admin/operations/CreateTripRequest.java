package in.joyway.dto.admin.operations;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;

@Getter
@Setter
public class CreateTripRequest {

    @NotNull(message = "Package ID is required.")
    private Long packageId;

    @NotNull(message = "Travel date is required.")
    private LocalDate travelDate;

    @NotNull(message = "Start time is required.")
    private Instant startAt;

    @NotNull(message = "End time is required.")
    private Instant endAt;

    private String pickupLocation;
    private String dropLocation;
    private String notes;
}
