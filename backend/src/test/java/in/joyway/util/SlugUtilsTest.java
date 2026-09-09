package in.joyway.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SlugUtilsTest {

    @Test
    void toSlug_normalizesTitle() {
        assertThat(SlugUtils.toSlug("-Nainital Weekend-")).isEqualTo("nainital-weekend");
        assertThat(SlugUtils.toSlug("Nainital Weekend Escape (DEMO)"))
                .isEqualTo("nainital-weekend-escape-demo");
    }

    @Test
    void ensureUnique_appendsCounter() {
        String slug = SlugUtils.ensureUnique("nainital-trip", s -> s.equals("nainital-trip"));
        assertThat(slug).isEqualTo("nainital-trip-1");
    }
}
