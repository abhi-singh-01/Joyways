package in.joyway.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;

class OperationConflictExceptionTest {

    @Test
    void getStatus_isConflict() {
        assertThat(new OperationConflictException("taken").getStatus()).isEqualTo(HttpStatus.CONFLICT);
    }
}
