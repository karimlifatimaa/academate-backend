package com.example.academatebackend.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Logs every HTTP request: method, path, status, duration, client IP.
 * Pushes a `traceId` into SLF4J MDC so all logs produced while handling
 * a single request share the same id (the log pattern already includes
 * `%X{traceId}` — see application.yml).
 *
 * Static asset and Swagger paths are skipped to avoid noise.
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final String TRACE_ID_KEY = "traceId";
    private static final String TRACE_ID_HEADER = "X-Request-Id";

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/swagger-ui")
                || path.startsWith("/v3/api-docs")
                || path.startsWith("/actuator")
                || path.equals("/favicon.ico");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String incoming = request.getHeader(TRACE_ID_HEADER);
        String traceId = (incoming != null && !incoming.isBlank())
                ? incoming
                : UUID.randomUUID().toString().substring(0, 8);

        MDC.put(TRACE_ID_KEY, traceId);
        response.setHeader(TRACE_ID_HEADER, traceId);

        long start = System.currentTimeMillis();
        String method = request.getMethod();
        String uri = request.getRequestURI();
        String query = request.getQueryString();
        String fullPath = query == null ? uri : uri + "?" + query;
        String clientIp = clientIp(request);

        log.info("→ {} {} from {}", method, fullPath, clientIp);

        try {
            chain.doFilter(request, response);
        } finally {
            long elapsed = System.currentTimeMillis() - start;
            int status = response.getStatus();
            if (status >= 500) {
                log.error("← {} {} {} ({}ms)", method, fullPath, status, elapsed);
            } else if (status >= 400) {
                log.warn("← {} {} {} ({}ms)", method, fullPath, status, elapsed);
            } else {
                log.info("← {} {} {} ({}ms)", method, fullPath, status, elapsed);
            }
            MDC.remove(TRACE_ID_KEY);
        }
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
