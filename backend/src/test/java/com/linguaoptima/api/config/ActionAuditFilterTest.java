/**
 * @file ActionAuditFilterTest.java
 * @brief Unit tests for ActionAuditFilter client IP resolution, route taxonomy classification, and filter execution.
 */
package com.linguaoptima.api.config;

import com.linguaoptima.api.domain.enums.Role;
import com.linguaoptima.api.domain.User;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.util.Collections;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ActionAuditFilterTest {

    private ActionAuditFilter filter;

    @Mock
    private FilterChain filterChain;

    @BeforeEach
    void setUp() {
        filter = new ActionAuditFilter();
        SecurityContextHolder.clearContext();
        MDC.clear();
    }

    @Test
    @DisplayName("resolveClientIp extracts first address from X-Forwarded-For")
    void testResolveClientIp_XForwardedFor() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Forwarded-For", "203.0.113.195, 70.41.3.18, 150.172.238.178");

        assertThat(filter.resolveClientIp(request)).isEqualTo("203.0.113.195");
    }

    @Test
    @DisplayName("resolveClientIp falls back to CF-Connecting-IP and X-Real-IP")
    void testResolveClientIp_CloudflareAndRealIp() {
        MockHttpServletRequest cfRequest = new MockHttpServletRequest();
        cfRequest.addHeader("CF-Connecting-IP", "198.51.100.42");
        assertThat(filter.resolveClientIp(cfRequest)).isEqualTo("198.51.100.42");

        MockHttpServletRequest realIpRequest = new MockHttpServletRequest();
        realIpRequest.addHeader("X-Real-IP", "192.0.2.88");
        assertThat(filter.resolveClientIp(realIpRequest)).isEqualTo("192.0.2.88");
    }

    @Test
    @DisplayName("resolveActionName correctly maps key platform domains and sanitizes UUIDs")
    void testResolveActionName() {
        assertThat(filter.resolveActionName("POST", "/api/auth/login")).isEqualTo("AUTH_LOGIN");
        assertThat(filter.resolveActionName("POST", "/api/tasks/generate")).isEqualTo("TASK_GENERATE");
        assertThat(filter.resolveActionName("POST", "/api/tasks/550e8400-e29b-41d4-a716-446655440000/assign"))
            .isEqualTo("TASK_ASSIGN_COHORT");
        assertThat(filter.resolveActionName("POST", "/api/submissions/text")).isEqualTo("SUBMISSION_TEXT_EVAL");
        assertThat(filter.resolveActionName("GET", "/api/export/group/550e8400-e29b-41d4-a716-446655440000"))
            .isEqualTo("REPORT_EXPORT");
        assertThat(filter.resolveActionName("POST", "/api/groups")).isEqualTo("GROUP_CREATE");
    }

    @Test
    @DisplayName("doFilterInternal bypasses health check without error")
    void testDoFilterInternal_BypassesActuator() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/actuator/health");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(response.getHeader("X-Request-Id")).isNull();
    }

    @Test
    @DisplayName("doFilterInternal injects X-Request-Id and captures authenticated user details")
    void testDoFilterInternal_AuthenticatedRequest() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/tasks/generate");
        request.addHeader("X-Device-Id", "device-browser-123");
        MockHttpServletResponse response = new MockHttpServletResponse();

        UUID userId = UUID.randomUUID();
        User user = User.builder()
            .id(userId)
            .email("teacher@school.org")
            .fullName("Jane Teacher")
            .role(Role.TEACHER)
            .build();

        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(user, null, Collections.singletonList(new SimpleGrantedAuthority("ROLE_TEACHER")))
        );

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(response.getHeader("X-Request-Id")).isNotBlank();
        // MDC must be cleared in finally
        assertThat(MDC.get("requestId")).isNull();
    }
}
