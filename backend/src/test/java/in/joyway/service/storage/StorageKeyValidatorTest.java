package in.joyway.service.storage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class StorageKeyValidatorTest {

    @Test
    void validate_acceptsValidKey() {
        assertDoesNotThrow(() -> StorageKeyValidator.validate("package-images/abc.jpg"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "",
            " ",
            "../secrets",
            "package-images/../etc/passwd",
            "/absolute/path",
            "\\windows\\path",
            "bad\\slash"
    })
    void validate_rejectsUnsafeKeys(String key) {
        assertThrows(IllegalArgumentException.class, () -> StorageKeyValidator.validate(key));
    }

    @Test
    void validateCategory_acceptsSimpleCategory() {
        assertDoesNotThrow(() -> StorageKeyValidator.validateCategory("traveller-documents"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "..", "a/b", "a\\b"})
    void validateCategory_rejectsUnsafeCategories(String category) {
        assertThrows(IllegalArgumentException.class, () -> StorageKeyValidator.validateCategory(category));
    }
}
