package in.joyway.dto.admin;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class UpdatePackageRequest {

    @Size(max = 255)
    private String title;

    private String slug;
    private String destination;
    private String shortDescription;
    private String fullDescription;
    private String departureCity;
    private String pickupPoints;
    private LocalDate startDate;
    private LocalDate endDate;
    private String duration;

    @DecimalMin(value = "0.01", message = "Price must be greater than zero")
    private BigDecimal pricePerPerson;

    @Min(value = 1, message = "Total seats must be at least 1")
    private Integer totalSeats;

    @Min(value = 0, message = "Available seats cannot be negative")
    private Integer availableSeats;

    private String hotelName;
    private String hotelDetails;
    private String vehicleType;
    private String itinerary;
    private String inclusions;
    private String exclusions;
    private String cancellationPolicy;
}
