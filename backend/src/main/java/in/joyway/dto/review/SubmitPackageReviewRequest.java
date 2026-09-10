package in.joyway.dto.review;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SubmitPackageReviewRequest(
        @NotBlank(message = "Booking reference is required")
        @Pattern(regexp = "^JW-\\d{4}-\\d{5}$", message = "Enter a valid booking reference")
        String reference,

        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        String email,

        @Min(value = 1, message = "Rating must be between 1 and 5")
        @Max(value = 5, message = "Rating must be between 1 and 5")
        int rating,

        @Size(max = 1000, message = "Comment must be at most 1000 characters")
        String comment
) {
}
