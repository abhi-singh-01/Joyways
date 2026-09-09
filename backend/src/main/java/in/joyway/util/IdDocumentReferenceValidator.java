package in.joyway.util;

import java.util.regex.Pattern;

/**
 * Ensures identity references are masked — never full Aadhaar or other raw ID numbers.
 */
public final class IdDocumentReferenceValidator {

    private static final Pattern FULL_AADHAAR = Pattern.compile("^\\d{12}$");
    private static final Pattern AADHAAR_MASKED = Pattern.compile("^(?:[*Xx]{1,4})?\\d{4}$");

    private IdDocumentReferenceValidator() {
    }

    public static void validate(String documentType, String reference) {
        if (reference == null || reference.isBlank()) {
            throw new IllegalArgumentException("ID document reference is required.");
        }

        String trimmed = reference.trim();
        String normalized = trimmed.replaceAll("\\s", "");

        if (FULL_AADHAAR.matcher(normalized).matches()) {
            throw new IllegalArgumentException(
                    "Provide only the last 4 digits or a masked reference, not the full ID number.");
        }

        if ("AADHAAR".equalsIgnoreCase(documentType)) {
            if (normalized.matches("\\d{5,}")) {
                throw new IllegalArgumentException(
                        "For Aadhaar, enter only the last 4 digits (e.g. 1234 or ****1234).");
            }
            if (!AADHAAR_MASKED.matcher(normalized).matches()) {
                throw new IllegalArgumentException(
                        "Enter a valid masked Aadhaar reference (last 4 digits, e.g. ****1234).");
            }
        }
    }
}
