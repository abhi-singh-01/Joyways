package in.joyway.dto.admin.operations;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AssignDriverRequest {

    @NotNull(message = "Driver ID is required.")
    private Long driverId;
}
