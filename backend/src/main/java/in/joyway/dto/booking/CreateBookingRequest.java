package in.joyway.dto.booking;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.util.List;

@Data
public class CreateBookingRequest {

    @NotNull(message = "Package ID is required")
    private Long packageId;

    @NotNull(message = "Number of travellers is required")
    @Min(value = 1, message = "At least one traveller is required")
    @Max(value = 20, message = "Maximum 20 travellers per booking")
    private Integer numberOfTravellers;

    @NotNull(message = "Customer details are required")
    @Valid
    private CustomerRequest customer;

    @NotNull(message = "Traveller details are required")
    @Size(min = 1, message = "At least one traveller is required")
    @Valid
    private List<TravellerRequest> travellers;
}
