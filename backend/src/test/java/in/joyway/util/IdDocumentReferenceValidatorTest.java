package in.joyway.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class IdDocumentReferenceValidatorTest {

    @Test
    void acceptsMaskedAadhaarReference() {
        assertDoesNotThrow(() -> IdDocumentReferenceValidator.validate("AADHAAR", "****1234"));
        assertDoesNotThrow(() -> IdDocumentReferenceValidator.validate("AADHAAR", "1234"));
    }

    @Test
    void rejectsFullAadhaarNumber() {
        assertThrows(IllegalArgumentException.class,
                () -> IdDocumentReferenceValidator.validate("AADHAAR", "123456789012"));
    }

    @Test
    void rejectsLongNumericAadhaarReference() {
        assertThrows(IllegalArgumentException.class,
                () -> IdDocumentReferenceValidator.validate("AADHAAR", "123456789"));
    }

    @Test
    void allowsPassportReference() {
        assertDoesNotThrow(() -> IdDocumentReferenceValidator.validate("PASSPORT", "P1234567"));
    }
}
