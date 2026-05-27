package com.caicai.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.function.Supplier;

@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

    private final ProxyManager<String> proxyManager;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String uri = request.getRequestURI();

        if (!uri.startsWith("/api/")) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientIp = getClientIp(request);
        String bucketKey = "rate_limit:" + uri + ":" + clientIp;

        BucketConfiguration config = buildConfig(uri);
        Supplier<BucketConfiguration> configSupplier = () -> config;
        Bucket bucket = proxyManager.builder().build(bucketKey, configSupplier);

        if (bucket.tryConsume(1)) {
            filterChain.doFilter(request, response);
        } else {
            log.warn("Rate limit exceeded for IP: {} on URI: {}", clientIp, uri);
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            objectMapper.writeValue(response.getWriter(), Map.of(
                    "status", 429,
                    "message", "Too many requests. Please try again later."
            ));
        }
    }

    private BucketConfiguration buildConfig(String uri) {
        int capacity;
        Duration duration;

        switch (uri) {
            case "/api/auth/login"           -> { capacity = 5;   duration = Duration.ofMinutes(1); }
            case "/api/auth/register"        -> { capacity = 3;   duration = Duration.ofMinutes(1); }
            case "/api/auth/forgot-password" -> { capacity = 3;   duration = Duration.ofMinutes(10); }
            case "/api/auth/reset-password"  -> { capacity = 5;   duration = Duration.ofMinutes(10); }
            case "/api/auth/verify"          -> { capacity = 5;   duration = Duration.ofMinutes(10); }
            case "/api/auth/demo"            -> { capacity = 3;   duration = Duration.ofMinutes(10); }
            default                          -> { capacity = 100; duration = Duration.ofMinutes(1); }
        }

        return BucketConfiguration.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(capacity)
                        .refillIntervally(capacity, duration)
                        .build())
                .build();
    }

    private String getClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isEmpty()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}