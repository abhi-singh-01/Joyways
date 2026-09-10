package in.joyway.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PickupPointsTest {

    @Test
    void parse_splitsCommaAndNewlineValues() {
        assertThat(PickupPoints.parse("Kashmere Gate, CP\nAnand Vihar"))
                .containsExactly("Kashmere Gate", "CP", "Anand Vihar");
    }

    @Test
    void requireSelection_allowsBlankWhenPackageHasNoPoints() {
        assertThat(PickupPoints.requireSelection(null, "  ")).isNull();
        assertThat(PickupPoints.requireSelection("", "Hotel lobby")).isEqualTo("Hotel lobby");
    }

    @Test
    void requireSelection_mustMatchListedPoint() {
        assertThat(PickupPoints.requireSelection("Kashmere Gate, CP", "cp"))
                .isEqualTo("CP");
        assertThatThrownBy(() -> PickupPoints.requireSelection("Kashmere Gate", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("pickup point");
        assertThatThrownBy(() -> PickupPoints.requireSelection("Kashmere Gate", "Airport"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
