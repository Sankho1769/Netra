package org.netra.core.observability;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class CorrelationIdFilterTest {

    private CorrelationIdFilter filter;

    @BeforeEach
    void setUp() {
        filter = new CorrelationIdFilter();
        MDC.clear();
    }

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    void shouldAcceptValidIncomingCorrelationId() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(CorrelationIdFilter.CORRELATION_ID_HEADER, "corr-abc-12345678");
        MockHttpServletResponse response = new MockHttpServletResponse();

        AtomicReference<String> mdcDuringExecution = new AtomicReference<>();
        FilterChain chain = (req, res) -> {
            mdcDuringExecution.set(MDC.get(CorrelationIdFilter.MDC_KEY));
        };

        filter.doFilter(request, response, chain);

        assertThat(response.getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER)).isEqualTo("corr-abc-12345678");
        assertThat(mdcDuringExecution.get()).isEqualTo("corr-abc-12345678");
        assertThat(MDC.get(CorrelationIdFilter.MDC_KEY)).isNull();
    }

    @Test
    void shouldAcceptValidIncomingRequestIdAsFallback() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(CorrelationIdFilter.REQUEST_ID_HEADER, "req-fallback-87654321");
        MockHttpServletResponse response = new MockHttpServletResponse();

        AtomicReference<String> mdcDuringExecution = new AtomicReference<>();
        FilterChain chain = (req, res) -> {
            mdcDuringExecution.set(MDC.get(CorrelationIdFilter.MDC_KEY));
        };

        filter.doFilter(request, response, chain);

        assertThat(response.getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER)).isEqualTo("req-fallback-87654321");
        assertThat(mdcDuringExecution.get()).isEqualTo("req-fallback-87654321");
        assertThat(MDC.get(CorrelationIdFilter.MDC_KEY)).isNull();
    }

    @Test
    void shouldGenerateNewCorrelationIdWhenHeaderMissing() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        AtomicReference<String> mdcDuringExecution = new AtomicReference<>();
        FilterChain chain = (req, res) -> {
            mdcDuringExecution.set(MDC.get(CorrelationIdFilter.MDC_KEY));
        };

        filter.doFilter(request, response, chain);

        String generatedId = response.getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER);
        assertThat(generatedId).isNotNull().matches("^[0-9a-fA-F-]{36}$");
        assertThat(mdcDuringExecution.get()).isEqualTo(generatedId);
        assertThat(MDC.get(CorrelationIdFilter.MDC_KEY)).isNull();
    }

    @Test
    void shouldDiscardMalformedCorrelationIdAndGenerateSafeUuid() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(CorrelationIdFilter.CORRELATION_ID_HEADER, "<script>alert(1)</script>");
        MockHttpServletResponse response = new MockHttpServletResponse();

        AtomicReference<String> mdcDuringExecution = new AtomicReference<>();
        FilterChain chain = (req, res) -> {
            mdcDuringExecution.set(MDC.get(CorrelationIdFilter.MDC_KEY));
        };

        filter.doFilter(request, response, chain);

        String safeId = response.getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER);
        assertThat(safeId).isNotNull()
                .doesNotContain("<script>")
                .matches("^[0-9a-fA-F-]{36}$");
        assertThat(mdcDuringExecution.get()).isEqualTo(safeId);
        assertThat(MDC.get(CorrelationIdFilter.MDC_KEY)).isNull();
    }

    @Test
    void shouldClearMdcEvenWhenChainThrowsException() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        FilterChain chain = (req, res) -> {
            throw new RuntimeException("Simulated filter failure");
        };

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> filter.doFilter(request, response, chain))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Simulated filter failure");

        assertThat(MDC.get(CorrelationIdFilter.MDC_KEY)).isNull();
    }
}
