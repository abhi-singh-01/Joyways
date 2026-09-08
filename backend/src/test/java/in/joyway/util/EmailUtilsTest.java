package in.joyway.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class EmailUtilsTest {

    @Test
    void normalize_trimsAndLowercasesEmail() {
        assertEquals("rockabhi0977@joyway.in", EmailUtils.normalize(" RockAbhi0977@Joyway.in "));
    }

    @Test
    void normalize_returnsNullForNullInput() {
        assertNull(EmailUtils.normalize(null));
    }
}
