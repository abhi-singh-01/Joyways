package in.joyway.service.payment;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class RazorpayGatewayImplTest {

    @Test
    void toPaise_convertsInrToPaise() {
        assertThat(RazorpayGatewayImpl.toPaise(new BigDecimal("10000.00"))).isEqualTo(1000000);
        assertThat(RazorpayGatewayImpl.toPaise(new BigDecimal("99.50"))).isEqualTo(9950);
    }

    @Test
    void toPaise_enforcesMinimumOneRupee() {
        assertThat(RazorpayGatewayImpl.toPaise(new BigDecimal("1.00"))).isEqualTo(100);
    }
}
