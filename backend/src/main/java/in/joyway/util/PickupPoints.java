package in.joyway.util;

import java.util.Arrays;
import java.util.List;

public final class PickupPoints {

    private PickupPoints() {
    }

    public static List<String> parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        return Arrays.stream(raw.split("[,\\r\\n]+"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .distinct()
                .toList();
    }

    /**
     * When the trip lists pickup points, the guest must choose one of them.
     * Otherwise any trimmed value is stored as-is (or null when blank).
     */
    public static String requireSelection(String packagePoints, String selected) {
        List<String> options = parse(packagePoints);
        String trimmed = selected == null ? "" : selected.trim();
        if (options.isEmpty()) {
            return trimmed.isEmpty() ? null : trimmed;
        }
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("Please choose a pickup point.");
        }
        return options.stream()
                .filter(option -> option.equalsIgnoreCase(trimmed))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Please choose a listed pickup point."));
    }
}
