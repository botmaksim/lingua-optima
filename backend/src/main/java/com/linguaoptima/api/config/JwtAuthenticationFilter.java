/**
 * @file JwtAuthenticationFilter.java
 * @brief HTTP servlet filter intercepting Bearer JWT tokens and establishing SecurityContext authentication.
 */
package com.linguaoptima.api.config;

import com.linguaoptima.api.domain.User;
import com.linguaoptima.api.repository.UserRepository;
import com.linguaoptima.api.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.Optional;

/**
 * @brief HTTP servlet filter intercepting Bearer JWT tokens and establishing SecurityContext authentication.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    /** @brief Field representing jwt service in JwtAuthenticationFilter. */
    private final JwtService jwtService;
    /** @brief Field representing user repository in JwtAuthenticationFilter. */
    private final UserRepository userRepository;

    /**
     * @brief Inspects incoming HTTP requests for Authorization headers, validates JWT tokens, and sets authentication.
     * @param request Incoming HTTP request.
     * @param response Outgoing HTTP response.
     * @param filterChain Target filter execution chain.
     * @throws ServletException in case of servlet processing error.
     * @throws IOException in case of I/O failure.
     */
    @Override
    protected void doFilterInternal(
        @NonNull HttpServletRequest request,
        @NonNull HttpServletResponse response,
        @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");
        final String queryToken = request.getParameter("token");
        final String jwt;
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            jwt = authHeader.substring(7);
        } else if (queryToken != null && !queryToken.isBlank()) {
            jwt = queryToken;
        } else {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            if (jwtService.isTokenValid(jwt)) {
                String tokenType = jwtService.extractClaim(jwt, claims -> claims.get("type", String.class));
                if (!"REFRESH".equals(tokenType)) {
                    String email = jwtService.extractEmail(jwt);
                    if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                        Optional<User> userOptional = userRepository.findByEmail(email);
                        if (userOptional.isPresent()) {
                            User user = userOptional.get();
                            String role = user.getRole().name();
                            SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + role);

                            UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                                user,
                                null,
                                Collections.singletonList(authority)
                            );
                            authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                            SecurityContextHolder.getContext().setAuthentication(authToken);

                            if (user.getId() != null) {
                                MDC.put("userId", user.getId().toString());
                            }
                            if (user.getEmail() != null) {
                                MDC.put("userEmail", user.getEmail());
                            }
                            MDC.put("userRole", role);
                        }
                    }
                }
            }
        } catch (Exception ignored) {
        }

        filterChain.doFilter(request, response);
    }
}
