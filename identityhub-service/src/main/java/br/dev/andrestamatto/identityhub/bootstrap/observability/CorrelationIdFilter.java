package br.dev.andrestamatto.identityhub.bootstrap.observability;

import java.io.IOException;
import java.util.regex.Pattern;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.IdGenerator;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
final class CorrelationIdFilter extends OncePerRequestFilter {

    static final String HEADER_NAME = "X-Correlation-ID";
    static final String MDC_KEY = "correlationId";

    private static final Pattern ALLOWED_VALUE = Pattern.compile("[A-Za-z0-9._-]{1,64}");

    private final IdGenerator idGenerator;

    CorrelationIdFilter(IdGenerator idGenerator) {
        this.idGenerator = idGenerator;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        var correlationId = resolveCorrelationId(request.getHeader(HEADER_NAME));
        var previousCorrelationId = MDC.get(MDC_KEY);

        MDC.put(MDC_KEY, correlationId);
        response.setHeader(HEADER_NAME, correlationId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            restoreMdc(previousCorrelationId);
        }
    }

    private String resolveCorrelationId(String candidate) {
        if (candidate != null && ALLOWED_VALUE.matcher(candidate).matches()) {
            return candidate;
        }
        return idGenerator.generateId().toString();
    }

    private void restoreMdc(String previousCorrelationId) {
        if (previousCorrelationId == null) {
            MDC.remove(MDC_KEY);
            return;
        }
        MDC.put(MDC_KEY, previousCorrelationId);
    }
}
