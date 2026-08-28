package in.joyway.dto.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AddPackageImageUrlRequest {

    @NotBlank(message = "Image URL is required")
    @Size(max = 500)
    private String imageUrl;

    @Size(max = 255)
    private String altText;

    private Integer sortOrder;
}
