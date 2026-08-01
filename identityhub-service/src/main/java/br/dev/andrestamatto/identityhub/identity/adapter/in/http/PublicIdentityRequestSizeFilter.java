package br.dev.andrestamatto.identityhub.identity.adapter.in.http;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.ReadListener;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.filter.OncePerRequestFilter;

@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public final class PublicIdentityRequestSizeFilter extends OncePerRequestFilter {

    private static final String VERIFICATION_PATH = "/public/v1/email-verifications";
    private static final String REGISTRATION_PREFIX = "/public/v1/applications/";
    private static final String REGISTRATION_SUFFIX = "/local-registrations";
    private static final byte[] TOO_LARGE_PROBLEM = ("{\"title\":\"Request too large\","
            + "\"status\":413,\"detail\":\"The request body exceeds the allowed size\"}")
            .getBytes(StandardCharsets.UTF_8);

    private final int maximumRequestBytes;

    public PublicIdentityRequestSizeFilter(int maximumRequestBytes) {
        if (maximumRequestBytes < 1) {
            throw new IllegalArgumentException("Maximum request size must be positive");
        }
        this.maximumRequestBytes = maximumRequestBytes;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!"POST".equals(request.getMethod())) {
            return true;
        }
        var path = request.getRequestURI();
        if (VERIFICATION_PATH.equals(path)) {
            return false;
        }
        if (!path.startsWith(REGISTRATION_PREFIX) || !path.endsWith(REGISTRATION_SUFFIX)) {
            return true;
        }
        var identifier = path.substring(
                REGISTRATION_PREFIX.length(),
                path.length() - REGISTRATION_SUFFIX.length());
        return identifier.isEmpty() || identifier.indexOf('/') >= 0;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        var body = request.getInputStream().readNBytes(maximumRequestBytes + 1);
        if (body.length > maximumRequestBytes) {
            Arrays.fill(body, (byte) 0);
            reject(response);
            return;
        }
        try {
            filterChain.doFilter(new BufferedRequest(request, body), response);
        } finally {
            Arrays.fill(body, (byte) 0);
        }
    }

    private void reject(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE);
        response.setContentType("application/problem+json");
        response.setHeader("Cache-Control", "no-store");
        response.setContentLength(TOO_LARGE_PROBLEM.length);
        response.getOutputStream().write(TOO_LARGE_PROBLEM);
    }

    private static final class BufferedRequest extends HttpServletRequestWrapper {
        private final byte[] body;

        private BufferedRequest(HttpServletRequest request, byte[] body) {
            super(request);
            this.body = body;
        }

        @Override
        public ServletInputStream getInputStream() {
            return new BufferedServletInputStream(body);
        }

        @Override
        public BufferedReader getReader() {
            return new BufferedReader(new InputStreamReader(
                    getInputStream(), StandardCharsets.UTF_8));
        }

        @Override
        public int getContentLength() {
            return body.length;
        }

        @Override
        public long getContentLengthLong() {
            return body.length;
        }
    }

    private static final class BufferedServletInputStream extends ServletInputStream {
        private final ByteArrayInputStream input;

        private BufferedServletInputStream(byte[] body) {
            input = new ByteArrayInputStream(body);
        }

        @Override
        public int read() {
            return input.read();
        }

        @Override
        public boolean isFinished() {
            return input.available() == 0;
        }

        @Override
        public boolean isReady() {
            return true;
        }

        @Override
        public void setReadListener(ReadListener readListener) {
            throw new UnsupportedOperationException("Asynchronous request reading is unsupported");
        }
    }
}
