package in.joyway.util;

import in.joyway.exception.RequestValidationException;

import java.util.Map;

public final class EmailHeaderValidator {

    private EmailHeaderValidator() {
    }

    public static void rejectHeaderInjection(String fieldName, String value) {
        if (value == null) {
            return;
        }
        if (value.indexOf('\r') >= 0 || value.indexOf('\n') >= 0) {
            throw new RequestValidationException(Map.of(fieldName, "Invalid characters in " + fieldName + "."));
        }
    }
}
