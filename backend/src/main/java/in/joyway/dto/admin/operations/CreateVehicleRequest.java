package in.joyway.dto.admin.operations;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateVehicleRequest {

    @NotBlank(message = "Registration number is required.")
    private String registrationNumber;

    @NotBlank(message = "Vehicle type is required.")
    private String vehicleType;

    private String brandModel;

    @NotNull(message = "Seating capacity is required.")
    @Min(value = 1, message = "Seating capacity must be greater than zero.")
    private Integer seatingCapacity;

    @NotBlank(message = "Ownership type is required.")
    private String ownershipType;

    private String notes;
}
