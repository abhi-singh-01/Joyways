package in.joyway.service.operations;

import in.joyway.domain.enums.TripStatus;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TripStatusTransitionsTest {

    private static final ZoneId IST = ZoneId.of("Asia/Kolkata");

    @Test
    void allowsExpectedTransitions() {
        TripStatusTransitions.validate(TripStatus.PLANNED, TripStatus.ASSIGNED);
        TripStatusTransitions.validate(TripStatus.ASSIGNED, TripStatus.READY);
        TripStatusTransitions.validate(TripStatus.READY, TripStatus.IN_PROGRESS);
        TripStatusTransitions.validate(TripStatus.IN_PROGRESS, TripStatus.COMPLETED);
        TripStatusTransitions.validate(TripStatus.PLANNED, TripStatus.CANCELLED);
    }

    @Test
    void rejectsInvalidTransitions() {
        assertThatThrownBy(() -> TripStatusTransitions.validate(TripStatus.COMPLETED, TripStatus.IN_PROGRESS))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void licenseStatusHelper_detectsExpiry() {
        LocalDate today = LocalDate.now(IST);
        assertThat(LicenseStatusHelper.resolve(today.minusDays(1))).isEqualTo("LICENSE EXPIRED");
        assertThat(LicenseStatusHelper.resolve(today.plusDays(10))).isEqualTo("LICENSE EXPIRING SOON");
        assertThat(LicenseStatusHelper.resolve(today.plusDays(60))).isEqualTo("VALID");
    }
}
