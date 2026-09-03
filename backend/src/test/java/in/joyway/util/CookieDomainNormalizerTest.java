package in.joyway.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CookieDomainNormalizerTest {

    @Test
    void stripsLeadingDotForSpringResponseCookie() {
        assertThat(CookieDomainNormalizer.forResponseCookie(".joyway.in")).isEqualTo("joyway.in");
    }

    @Test
    void leavesDomainWithoutLeadingDotUnchanged() {
        assertThat(CookieDomainNormalizer.forResponseCookie("joyway.in")).isEqualTo("joyway.in");
    }

    @Test
    void blankReturnsNull() {
        assertThat(CookieDomainNormalizer.forResponseCookie("")).isNull();
        assertThat(CookieDomainNormalizer.forResponseCookie(null)).isNull();
    }
}
