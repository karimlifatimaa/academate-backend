package com.example.academatebackend.security;

import com.example.academatebackend.common.exception.ApiError;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private record BucketEntry(Bucket bucket, Instant lastAccess) {}

    private record PathRule(String prefix, int capacity, Duration period) {}

    private final Map<String, BucketEntry> buckets = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;

    @Value("${app.rate-limit.requests-per-minute:60}")
    private int defaultRequestsPerMinute;

    @Value("${app.rate-limit.login-per-minute:5}")
    private int loginPerMinute;

    @Value("${app.rate-limit.register-per-hour:3}")
    private int registerPerHour;

    @Value("${app.rate-limit.forgot-password-per-hour:3}")
    private int forgotPasswordPerHour;

    public RateLimitFilter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    private List<PathRule> pathRules() {
        return List.of(
            new PathRule("/api/v1/auth/login", loginPerMinute, Duration.ofMinutes(1)),
            new PathRule("/api/v1/auth/register", registerPerHour, Duration.ofHours(1)),
            new PathRule("/api/v1/auth/forgot-password", forgotPasswordPerHour, Duration.ofHours(1))
        );
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        evictStaleBuckets();

        String ip = request.getRemoteAddr();
        String path = request.getRequestURI();
        String bucketKey = resolveBucketKey(ip, path);
        PathRule rule = resolveRule(path);

        Bucket bucket = buckets.computeIfAbsent(bucketKey, k -> new BucketEntry(newBucket(rule), Instant.now())).bucket();
        buckets.computeIfPresent(bucketKey, (k, e) -> new BucketEntry(e.bucket(), Instant.now()));

        if (bucket.tryConsume(1)) {
            chain.doFilter(request, response);
        } else {
            log.warn("Rate limit exceeded for ip={} path={} {}", ip, request.getMethod(), path);
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            ApiError error = ApiError.builder()
                    .status(HttpStatus.TOO_MANY_REQUESTS.value())
                    .title("Too Many Requests")
                    .detail("Rate limit exceeded. Please slow down.")
                    .traceId(UUID.randomUUID().toString())
                    .timestamp(Instant.now())
                    .build();
            objectMapper.writeValue(response.getWriter(), error);
        }
    }

    private String resolveBucketKey(String ip, String path) {
        for (PathRule rule : pathRules()) {
            if (path.startsWith(rule.prefix())) {
                return rule.prefix() + ":" + ip;
            }
        }
        return "default:" + ip;
    }

    private PathRule resolveRule(String path) {
        for (PathRule rule : pathRules()) {
            if (path.startsWith(rule.prefix())) return rule;
        }
        return new PathRule("default", defaultRequestsPerMinute, Duration.ofMinutes(1));
    }

    private Bucket newBucket(PathRule rule) {
        Bandwidth limit = Bandwidth.builder()
                .capacity(rule.capacity())
                .refillGreedy(rule.capacity(), rule.period())
                .build();
        return Bucket.builder().addLimit(limit).build();
    }

    private void evictStaleBuckets() {
        if (buckets.size() < 500) return;
        Instant cutoff = Instant.now().minus(Duration.ofHours(2));
        buckets.entrySet().removeIf(e -> e.getValue().lastAccess().isBefore(cutoff));
    }
}
