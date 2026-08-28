package in.joyway.dto.admin.operations;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AssignVehicleRequest {

    @NotNull(message = "Vehicle ID is required.")
    private Long vehicleId;
}
