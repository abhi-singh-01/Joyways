package in.joyway.dto.admin;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class CreatePackageRequest {

    @NotBlank(message = "Title is required")
    @Size(max = 255)
    private String title;

    private String slug;

    @NotBlank(message = "Destination is required")
    private String destination;

    private String shortDescription;
    private String fullDescription;

    @NotBlank(message = "Departure city is required")
    private String departureCity = "Delhi";

    private String pickupPoints;

    @NotNull(message = "Start date is required")
    private LocalDate startDate;

    @NotNull(message = "End date is required")
    private LocalDate endDate;

    @NotBlank(message = "Duration is required")
    private String duration;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.01", message = "Price must be greater than zero")
    private BigDecimal pricePerPerson;

    @NotNull(message = "Total seats is required")
    @Min(value = 1, message = "Total seats must be at least 1")
    private Integer totalSeats;

    private String hotelName;
    private String hotelDetails;
    private String vehicleType;
    private String itinerary;
    private String inclusions;
    private String exclusions;
    private String cancellationPolicy;
}
