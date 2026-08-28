package in.joyway.service.storage;

/**
 * Validates object keys before they are used for read, write or delete.
 * Prevents path traversal regardless of whether storage is local or Azure Blob.
 */
public final class StorageKeyValidator {

    private StorageKeyValidator() {
    }

    public static void validate(String storageKey) {
        if (storageKey == null || storageKey.isBlank()) {
            throw new IllegalArgumentException("Invalid storage key.");
        }
        if (storageKey.contains("..") || storageKey.startsWith("/") || storageKey.startsWith("\\")) {
            throw new IllegalArgumentException("Invalid storage key.");
        }
        if (storageKey.contains("\\")) {
            throw new IllegalArgumentException("Invalid storage key.");
        }
    }

    public static void validateCategory(String category) {
        if (category == null || category.isBlank() || category.contains("..")
                || category.contains("/") || category.contains("\\")) {
            throw new IllegalArgumentException("Invalid storage category.");
        }
    }
}
