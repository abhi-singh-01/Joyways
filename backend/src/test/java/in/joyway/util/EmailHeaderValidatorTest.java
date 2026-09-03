package in.joyway.util;

import in.joyway.exception.RequestValidationException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EmailHeaderValidatorTest {

    @Test
    void acceptsSafeValues() {
        assertThatCode(() -> EmailHeaderValidator.rejectHeaderInjection("subject", "Trip enquiry"))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsCarriageReturn() {
        assertThatThrownBy(() -> EmailHeaderValidator.rejectHeaderInjection("subject", "Hello\rBcc: x"))
                .isInstanceOf(RequestValidationException.class);
    }

    @Test
    void rejectsNewline() {
        assertThatThrownBy(() -> EmailHeaderValidator.rejectHeaderInjection("subject", "Hello\nBcc: x"))
                .isInstanceOf(RequestValidationException.class);
    }
}
