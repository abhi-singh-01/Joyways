package in.joyway.dto.admin.operations;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateDriverStatusRequest {

    @NotBlank(message = "Status is required.")
    private String status;
}
