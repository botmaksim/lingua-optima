package com.linguaoptima.api.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CorsConfigurationSource corsConfigurationSource;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configurationSource(corsConfigurationSource))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Public auth endpoints
                .requestMatchers(
                    "/api/auth/register",
                    "/api/auth/login",
                    "/api/auth/refresh",
                    "/api/auth/forgot-password",
                    "/actuator/**"
                ).permitAll()

                // Teacher restricted endpoints
                .requestMatchers(HttpMethod.POST, "/api/tasks/{id}/assign").hasAnyRole("TEACHER", "ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/tasks/template").hasAnyRole("TEACHER", "ADMIN")
                .requestMatchers("/api/groups/**").hasAnyRole("TEACHER", "ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/submissions/{id}/override").hasAnyRole("TEACHER", "ADMIN")
                .requestMatchers("/api/exports/**").hasAnyRole("TEACHER", "ADMIN")
                .requestMatchers("/api/progress/student/**", "/api/progress/group/**").hasAnyRole("TEACHER", "ADMIN")

                // Student restricted endpoints
                .requestMatchers("/api/sessions/**").hasAnyRole("STUDENT", "ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/submissions/text", "/api/submissions/image").hasAnyRole("STUDENT", "ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/submissions/my").hasAnyRole("STUDENT", "ADMIN")

                // All other endpoints require authentication
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
