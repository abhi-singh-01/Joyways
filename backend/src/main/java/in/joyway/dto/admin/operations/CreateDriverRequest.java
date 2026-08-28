package in.joyway.dto.admin.operations;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class CreateDriverRequest {

    @NotBlank(message = "Driver name is required.")
    private String name;

    @NotBlank(message = "Phone is required.")
    private String phone;

    @Email(message = "Invalid email address.")
    private String email;

    @NotBlank(message = "License number is required.")
    private String licenseNumber;

    @NotNull(message = "License expiry date is required.")
    private LocalDate licenseExpiryDate;

    private String notes;
}
