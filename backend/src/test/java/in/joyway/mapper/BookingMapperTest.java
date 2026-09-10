package in.joyway.mapper;

import in.joyway.config.BookingProperties;
import in.joyway.domain.entity.Booking;
import in.joyway.domain.entity.Traveller;
import in.joyway.domain.entity.TravelPackage;
import in.joyway.domain.enums.BookingStatus;
import in.joyway.domain.enums.PaymentStatus;
import in.joyway.dto.booking.BookingResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class BookingMapperTest {

    private BookingMapper mapper;
    private Booking booking;

    @BeforeEach
    void setUp() {
        mapper = new BookingMapper(new BookingProperties());
        TravelPackage pkg = TravelPackage.builder()
                .title("Nainital Weekend")
                .destination("Nainital")
                .startDate(LocalDate.of(2026, 10, 1))
                .endDate(LocalDate.of(2026, 10, 3))
                .build();
        booking = Booking.builder()
                .id(18L)
                .bookingReference("JW-2026-00018")
                .travelPackage(pkg)
                .numberOfTravellers(2)
                .totalAmount(new BigDecimal("12000.00"))
                .currency("INR")
                .status(BookingStatus.PENDING_PAYMENT)
                .paymentStatus(PaymentStatus.PENDING)
                .pickupPoint("Kashmere Gate")
                .createdAt(Instant.parse("2026-09-10T10:00:00Z"))
                .build();
    }

    @Test
    void toResponse_includesPickupAndPaymentExpiry() {
        Traveller traveller = Traveller.builder()
                .fullName("Priya Sharma")
                .idDocumentType("AADHAAR")
                .build();

        BookingResponse response = mapper.toResponse(booking, List.of(traveller));

        assertThat(response.pickupPoint()).isEqualTo("Kashmere Gate");
        assertThat(response.paymentExpiresAt()).isEqualTo(Instant.parse("2026-09-10T10:30:00Z"));
        assertThat(response.travellers()).hasSize(1);
        assertThat(response.travellers().getFirst().fullName()).isEqualTo("Priya Sharma");
    }

    @Test
    void paymentExpiresAt_isNullOnceBookingLeavesPendingPayment() {
        booking.setStatus(BookingStatus.CONFIRMED);

        assertThat(mapper.paymentExpiresAt(booking)).isNull();
    }
}
