package in.joyway.util;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

public final class SlugUtils {

    private static final Pattern NON_LATIN = Pattern.compile("[^\\w-]");
    private static final Pattern WHITESPACE = Pattern.compile("[\\s]");
    private static final Pattern MULTIPLE_DASHES = Pattern.compile("-{2,}");

    private SlugUtils() {}

    public static String toSlug(String input) {
        if (input == null || input.isBlank()) {
            return "package";
        }
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);
        normalized = normalized.replaceAll("\\p{M}", "");
        normalized = WHITESPACE.matcher(normalized).replaceAll("-");
        normalized = NON_LATIN.matcher(normalized).replaceAll("");
        normalized = MULTIPLE_DASHES.matcher(normalized).replaceAll("-");
        normalized = normalized.replaceAll("(^-)|(-$)", "");
        return normalized.toLowerCase(Locale.ENGLISH);
    }

    public static String ensureUnique(String baseSlug, java.util.function.Predicate<String> existsChecker) {
        String slug = baseSlug;
        int counter = 1;
        while (existsChecker.test(slug)) {
            slug = baseSlug + "-" + counter++;
        }
        return slug;
    }
}
