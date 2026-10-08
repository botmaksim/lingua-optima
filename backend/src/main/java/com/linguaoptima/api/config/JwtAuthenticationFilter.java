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
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(
        @NonNull HttpServletRequest request,
        @NonNull HttpServletResponse response,
        @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        final String jwt = authHeader.substring(7);
        try {
            if (jwtService.isTokenValid(jwt)) {
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
                    }
                }
            }
        } catch (Exception ignored) {
            // In case of invalid/tampered token, context remains unauthenticated
        }

        filterChain.doFilter(request, response);
    }
}
