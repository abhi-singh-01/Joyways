package in.joyway.dto.admin;

import in.joyway.domain.enums.PackageStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdatePackageStatusRequest {

    @NotNull(message = "Status is required")
    private PackageStatus status;
}
