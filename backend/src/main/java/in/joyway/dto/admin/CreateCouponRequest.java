package in.joyway.dto.admin;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class CreateCouponRequest {

    @NotBlank
    @Size(min = 3, max = 40)
    @Pattern(regexp = "^[A-Za-z0-9_-]+$", message = "Code may only contain letters, numbers, hyphen, and underscore")
    private String code;

    @NotNull
    private Long packageId;

    @NotBlank
    @Pattern(regexp = "PERCENT|FIXED", message = "Discount type must be PERCENT or FIXED")
    private String discountType;

    @NotNull
    @DecimalMin(value = "0.01", message = "Discount value must be greater than zero")
    private BigDecimal discountValue;

    @NotNull
    @FutureOrPresent(message = "Expiry date must be today or later")
    private LocalDate expiresOn;
}
