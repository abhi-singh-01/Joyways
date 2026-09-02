package in.joyway.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class ClientAddressResolverTest {

    private final ClientAddressResolver resolver = new ClientAddressResolver();

    @Test
    void usesRemoteAddrWhenForwardedHeaderMissing() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("203.0.113.10");

        assertThat(resolver.resolve(request)).isEqualTo("203.0.113.10");
    }

    @Test
    void usesRightMostForwardedHop() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.1");
        request.addHeader("X-Forwarded-For", "8.8.8.8, 203.0.113.20");

        assertThat(resolver.resolve(request)).isEqualTo("203.0.113.20");
    }

    @Test
    void ignoresSpoofedLeftMostForwardedHop() {
        MockHttpServletRequest first = new MockHttpServletRequest("POST", "/api/admin/auth/login");
        first.setRemoteAddr("10.0.0.2");
        first.addHeader("X-Forwarded-For", "1.1.1.1, 203.0.113.30");
        String sharedClient = resolver.resolve(first);

        MockHttpServletRequest second = new MockHttpServletRequest("POST", "/api/admin/auth/login");
        second.setRemoteAddr("10.0.0.2");
        second.addHeader("X-Forwarded-For", "9.9.9.9, 203.0.113.30");

        assertThat(resolver.resolve(second)).isEqualTo(sharedClient);
    }

    @Test
    void normalizesIpv4() {
        assertThat(resolver.normalize("203.0.113.10")).isEqualTo("203.0.113.10");
    }

    @Test
    void normalizesIpv6() {
        assertThat(resolver.normalize("2001:db8::1")).isEqualTo("2001:db8:0:0:0:0:0:1");
    }

    @Test
    void normalizesBracketedIpv6() {
        assertThat(resolver.normalize("[2001:db8::1]")).isEqualTo("2001:db8:0:0:0:0:0:1");
    }

    @Test
    void keepsMalformedAddressAsStableKey() {
        assertThat(resolver.normalize("not-an-ip")).isEqualTo("not-an-ip");
    }

    @Test
    void returnsUnknownWhenAddressMissing() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("");
        assertThat(resolver.resolve(request)).isEqualTo(ClientAddressResolver.UNKNOWN_CLIENT);
    }
}
