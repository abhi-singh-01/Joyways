package in.joyway.dto.admin;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdatePackageImageRequest {

    @Size(max = 255)
    private String altText;

    private Integer sortOrder;
}
