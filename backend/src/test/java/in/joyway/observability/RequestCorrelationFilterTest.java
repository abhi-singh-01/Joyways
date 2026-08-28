package in.joyway.observability;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class RequestCorrelationFilterTest {

    private final RequestCorrelationFilter filter = new RequestCorrelationFilter();

    @Test
    void generatesRequestIdWhenHeaderAbsent() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/health");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getHeader(RequestCorrelationFilter.REQUEST_ID_HEADER)).isNotBlank();
    }

    @Test
    void honoursSafeClientSuppliedRequestId() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/health");
        request.addHeader(RequestCorrelationFilter.REQUEST_ID_HEADER, "trace-abc_123.4");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getHeader(RequestCorrelationFilter.REQUEST_ID_HEADER))
                .isEqualTo("trace-abc_123.4");
    }

    @Test
    void rejectsRequestIdContainingLogInjectionCharacters() {
        String forged = "abc\nERROR fake log line";

        String resolved = RequestCorrelationFilter.resolveRequestId(forged);

        assertThat(resolved).doesNotContain("\n").isNotEqualTo(forged);
    }

    @Test
    void rejectsOverlongRequestId() {
        String resolved = RequestCorrelationFilter.resolveRequestId("a".repeat(200));

        assertThat(resolved).hasSizeLessThanOrEqualTo(64);
    }

    @Test
    void clearsMdcAfterRequest() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/health");
        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        assertThat(RequestContext.currentRequestId()).isNull();
    }
}
