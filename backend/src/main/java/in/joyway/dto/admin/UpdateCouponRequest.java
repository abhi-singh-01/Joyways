package in.joyway.dto.admin;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class UpdateCouponRequest {

    @NotNull
    private Long packageId;

    @NotBlank
    @Pattern(regexp = "PERCENT|FIXED", message = "Discount type must be PERCENT or FIXED")
    private String discountType;

    @NotNull
    @DecimalMin(value = "0.01", message = "Discount value must be greater than zero")
    private BigDecimal discountValue;

    @NotNull
    private LocalDate expiresOn;

    @NotNull
    private Boolean active;
}
