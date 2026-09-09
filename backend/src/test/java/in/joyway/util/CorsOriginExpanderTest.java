package in.joyway.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CorsOriginExpanderTest {

    @Test
    void expand_addsWwwTwinForApexOrigin() {
        assertThat(CorsOriginExpander.expand("https://joyway.in"))
                .containsExactly("https://joyway.in", "https://www.joyway.in");
    }

    @Test
    void expand_addsApexTwinForWwwOrigin() {
        assertThat(CorsOriginExpander.expand("https://www.joyway.in"))
                .containsExactly("https://www.joyway.in", "https://joyway.in");
    }

    @Test
    void expand_doesNotInventWwwForApiSubdomain() {
        assertThat(CorsOriginExpander.expand("https://api.joyway.in"))
                .containsExactly("https://api.joyway.in");
    }

    @Test
    void expand_leavesLocalhostUnchanged() {
        assertThat(CorsOriginExpander.expand("http://localhost:3000"))
                .containsExactly("http://localhost:3000");
    }

    @Test
    void expand_dedupesWhenBothAlreadyListed() {
        assertThat(CorsOriginExpander.expand("https://joyway.in, https://www.joyway.in"))
                .containsExactly("https://joyway.in", "https://www.joyway.in");
    }

    @Test
    void expand_emptyWhenBlank() {
        assertThat(CorsOriginExpander.expand("")).isEmpty();
        assertThat(CorsOriginExpander.expand(null)).isEmpty();
    }

    @Test
    void wwwTwin_returnsNullWhenHostOrSchemeMissing() {
        assertThat(CorsOriginExpander.wwwTwin("mailto:ops@joyway.in")).isNull();
        assertThat(CorsOriginExpander.wwwTwin("//www.joyway.in")).isNull();
        assertThat(CorsOriginExpander.wwwTwin("urn:example:cors")).isNull();
    }

    @Test
    void wwwTwin_returnsNullForIllegalUri() {
        assertThat(CorsOriginExpander.wwwTwin("http://[invalid")).isNull();
    }
}
