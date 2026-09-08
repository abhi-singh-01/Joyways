package in.joyway.dto.admin;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class AdminRefundRequest {

    @DecimalMin(value = "1.00", message = "Refund amount must be at least ₹1.00.")
    private BigDecimal amount;

    @Size(max = 500, message = "Reason must be 500 characters or fewer.")
    private String reason;
}
