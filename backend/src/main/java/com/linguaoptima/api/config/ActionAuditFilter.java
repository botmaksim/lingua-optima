/**
 * @file ActionAuditFilter.java
 * @brief High-performance HTTP servlet filter providing structured action audit logging and MDC correlation tracing.
 */
package com.linguaoptima.api.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.linguaoptima.api.domain.User;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * @brief High-performance HTTP servlet filter providing structured action audit logging and MDC correlation tracing.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
public class ActionAuditFilter extends OncePerRequestFilter {

    private static final Logger AUDIT_LOG = LoggerFactory.getLogger("com.linguaoptima.audit");
    private static final Pattern UUID_PATTERN = Pattern.compile("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}");
    private static final Pattern NUMERIC_ID_PATTERN = Pattern.compile("/\\d+");

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void doFilterInternal(
        @NonNull HttpServletRequest request,
        @NonNull HttpServletResponse response,
        @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        final String uri = request.getRequestURI();

        // Bypass audit logging for noisy internal health/metrics checks
        if (uri.startsWith("/actuator/health") || uri.startsWith("/actuator/metrics") || uri.endsWith("/favicon.ico")) {
            filterChain.doFilter(request, response);
            return;
        }

        final long startTime = System.currentTimeMillis();

        // 1. Establish or propagate distributed request tracing ID
        String requestId = request.getHeader("X-Request-Id");
        if (requestId == null || requestId.isBlank()) {
            requestId = UUID.randomUUID().toString();
        }
        response.setHeader("X-Request-Id", requestId);

        // 2. Resolve client networking details
        final String clientIp = resolveClientIp(request);
        final String deviceId = request.getHeader("X-Device-Id") != null ? request.getHeader("X-Device-Id") : "unknown";
        final String userAgent = request.getHeader("User-Agent") != null ? request.getHeader("User-Agent") : "unknown";
        final String method = request.getMethod();

        // 3. Populate SLF4J MDC for unified contextual logging
        MDC.put("requestId", requestId);
        MDC.put("clientIp", clientIp);
        MDC.put("deviceId", deviceId);
        MDC.put("method", method);
        MDC.put("uri", uri);

        try {
            filterChain.doFilter(request, response);
        } finally {
            final long durationMs = System.currentTimeMillis() - startTime;
            final int status = response.getStatus();

            // 4. Extract user identity established by downstream security filters
            String userId = "anonymous";
            String userEmail = "anonymous";
            String userRole = "ANONYMOUS";

            final Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated()) {
                final Object principal = auth.getPrincipal();
                if (principal instanceof User user) {
                    userId = user.getId() != null ? user.getId().toString() : "anonymous";
                    userEmail = user.getEmail() != null ? user.getEmail() : "anonymous";
                    userRole = user.getRole() != null ? user.getRole().name() : "USER";
                } else if (principal instanceof String principalName && !"anonymousUser".equals(principalName)) {
                    userEmail = principalName;
                    userRole = auth.getAuthorities().stream().findFirst().map(a -> a.getAuthority().replace("ROLE_", "")).orElse("USER");
                }
            }

            final String action = resolveActionName(method, uri);

            // 5. Emit structured JSON audit log event
            try {
                Map<String, Object> logPayload = new LinkedHashMap<>();
                logPayload.put("timestamp", Instant.now().toString());
                logPayload.put("type", "ACTION_AUDIT");
                logPayload.put("requestId", requestId);
                logPayload.put("action", action);
                logPayload.put("method", method);
                logPayload.put("uri", uri);
                logPayload.put("status", status);
                logPayload.put("durationMs", durationMs);
                logPayload.put("userId", userId);
                logPayload.put("userEmail", userEmail);
                logPayload.put("role", userRole);
                logPayload.put("clientIp", clientIp);
                logPayload.put("deviceId", deviceId);
                logPayload.put("userAgent", userAgent);

                String jsonAuditRecord = objectMapper.writeValueAsString(logPayload);
                AUDIT_LOG.info(jsonAuditRecord);
            } catch (Exception e) {
                AUDIT_LOG.warn("[AUDIT_FALLBACK] req={} action={} user={} status={} duration={}ms",
                    requestId, action, userEmail, status, durationMs);
            } finally {
                MDC.clear();
            }
        }
    }

    /**
     * @brief Extracts genuine client IP addressing reverse proxy layers (Cloudflare, Nginx).
     */
    public String resolveClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            int commaIdx = xForwardedFor.indexOf(',');
            return (commaIdx > 0 ? xForwardedFor.substring(0, commaIdx) : xForwardedFor).trim();
        }
        String cfConnectingIp = request.getHeader("CF-Connecting-IP");
        if (cfConnectingIp != null && !cfConnectingIp.isBlank()) {
            return cfConnectingIp.trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isBlank()) {
            return xRealIp.trim();
        }
        return request.getRemoteAddr() != null ? request.getRemoteAddr() : "127.0.0.1";
    }

    /**
     * @brief Normalizes URI and classifies request into human-readable action domain taxonomy.
     */
    public String resolveActionName(String method, String uri) {
        String normalized = UUID_PATTERN.matcher(uri).replaceAll("{id}");
        normalized = NUMERIC_ID_PATTERN.matcher(normalized).replaceAll("/{id}");

        if (normalized.equals("/api/auth/login")) return "AUTH_LOGIN";
        if (normalized.equals("/api/auth/register")) return "AUTH_REGISTER";
        if (normalized.equals("/api/auth/send-verification-code")) return "AUTH_SEND_CODE";
        if (normalized.equals("/api/auth/refresh")) return "AUTH_REFRESH";
        if (normalized.equals("/api/auth/forgot-password")) return "AUTH_FORGOT_PASSWORD";
        if (normalized.equals("/api/auth/reset-password")) return "AUTH_RESET_PASSWORD";
        if (normalized.equals("/api/auth/google")) return "AUTH_GOOGLE_OAUTH";

        if (normalized.equals("/api/tasks/generate")) return "TASK_GENERATE";
        if (normalized.equals("/api/tasks/takeover")) return "GENERATION_TAKEOVER";
        if (normalized.equals("/api/tasks/template")) return "TASK_CREATE_TEMPLATE";
        if (normalized.equals("/api/tasks/{id}/assign")) return "TASK_ASSIGN_COHORT";
        if (normalized.equals("/api/tasks/custom")) return "TASK_CREATE_CUSTOM";

        if (normalized.equals("/api/submissions/text")) return "SUBMISSION_TEXT_EVAL";
        if (normalized.equals("/api/submissions/image")) return "SUBMISSION_IMAGE_EVAL";
        if (normalized.equals("/api/submissions/{id}/override")) return "SUBMISSION_TEACHER_OVERRIDE";

        if (normalized.equals("/api/groups") && "POST".equalsIgnoreCase(method)) return "GROUP_CREATE";
        if (normalized.equals("/api/groups/{id}/students") && "POST".equalsIgnoreCase(method)) return "GROUP_INVITE_STUDENT";
        if (normalized.contains("/invitation/accept")) return "GROUP_INVITATION_ACCEPT";
        if (normalized.contains("/invitation/decline")) return "GROUP_INVITATION_DECLINE";

        if (normalized.startsWith("/api/export/")) return "REPORT_EXPORT";
        if (normalized.startsWith("/api/progress/")) return "PROGRESS_INSPECT";
        if (normalized.startsWith("/api/curriculum/custom")) return "CUSTOM_CURRICULUM_MUTATE";
        if (normalized.startsWith("/api/api-keys")) return "BYOK_KEY_MUTATE";
        if (normalized.startsWith("/api/ai/proxy")) return "AI_VENDOR_PROXY";

        // Dynamic normalized fallback (e.g. GET_API_USERS_ME)
        String clean = normalized.replaceFirst("^/+", "").replaceAll("[^a-zA-Z0-9]+", "_").toUpperCase();
        return method.toUpperCase() + "_" + clean;
    }
}
