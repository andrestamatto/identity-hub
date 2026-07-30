package br.dev.andrestamatto.identityhub.bootstrap.observability;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.util.IdGenerator;

class CorrelationIdFilterTest {

    private static final UUID GENERATED_ID =
            UUID.fromString("f7cb9347-20ed-4b98-b42f-80ad52af27af");

    private final IdGenerator idGenerator = () -> GENERATED_ID;
    private final CorrelationIdFilter filter = new CorrelationIdFilter(idGenerator);

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    void propagatesValidCorrelationIdAndClearsMdc() throws Exception {
        var request = new MockHttpServletRequest();
        request.addHeader(CorrelationIdFilter.HEADER_NAME, "consumer-request_123");
        var response = new MockHttpServletResponse();

        filter.doFilter(request, response, (ignoredRequest, ignoredResponse) ->
                assertThat(request.getAttribute(CorrelationIdFilter.REQUEST_ATTRIBUTE))
                        .isEqualTo("consumer-request_123"));

        assertThat(response.getHeader(CorrelationIdFilter.HEADER_NAME))
                .isEqualTo("consumer-request_123");
        assertThat(MDC.get(CorrelationIdFilter.MDC_KEY)).isNull();
    }

    @Test
    void replacesInvalidCorrelationId() throws Exception {
        var request = new MockHttpServletRequest();
        request.addHeader(CorrelationIdFilter.HEADER_NAME, "invalid correlation id");
        var response = new MockHttpServletResponse();

        filter.doFilter(request, response, (ignoredRequest, ignoredResponse) ->
                assertThat(MDC.get(CorrelationIdFilter.MDC_KEY))
                        .isEqualTo(GENERATED_ID.toString()));

        assertThat(response.getHeader(CorrelationIdFilter.HEADER_NAME))
                .isEqualTo(GENERATED_ID.toString());
        assertThat(MDC.get(CorrelationIdFilter.MDC_KEY)).isNull();
    }
}
