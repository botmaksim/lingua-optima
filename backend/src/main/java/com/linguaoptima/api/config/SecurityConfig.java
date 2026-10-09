/**
 * @file SecurityConfig.java
 * @brief Spring Security configuration for HTTP security, JWT filters, stateless sessions, and role authorization.
 */
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

/**
 * @brief Spring Security configuration for HTTP security, JWT filters, stateless sessions, and role authorization.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    /** @brief Field representing jwt authentication filter in SecurityConfig. */
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    /** @brief Field representing cors configuration source in SecurityConfig. */
    private final CorsConfigurationSource corsConfigurationSource;

    /**
     * @brief Configures the application security filter chain and authorization rules.
     * @param http HttpSecurity configuration object.
     * @return Configured SecurityFilterChain bean.
     * @throws Exception if security building fails.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configurationSource(corsConfigurationSource))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/api/auth/register",
                    "/api/auth/send-verification-code",
                    "/api/auth/login",
                    "/api/auth/google",
                    "/api/auth/refresh",
                    "/api/auth/forgot-password",
                    "/actuator/**"
                ).permitAll()
                .requestMatchers(HttpMethod.POST, "/api/tasks/{id}/assign").hasAnyRole("TEACHER", "ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/tasks/template").hasAnyRole("TEACHER", "ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/groups").hasAnyRole("STUDENT", "TEACHER", "ADMIN")
                .requestMatchers("/api/groups/**").hasAnyRole("TEACHER", "ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/submissions/{id}/override").hasAnyRole("TEACHER", "ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/submissions/{id}/override").hasAnyRole("TEACHER", "ADMIN")
                .requestMatchers("/api/exports/**", "/api/export/**").hasAnyRole("TEACHER", "ADMIN")
                .requestMatchers("/api/progress/student/**", "/api/progress/group/**").hasAnyRole("TEACHER", "ADMIN")
                .requestMatchers("/api/sessions/**").hasAnyRole("STUDENT", "ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/submissions/text", "/api/submissions/image").hasAnyRole("STUDENT", "ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/submissions/my", "/api/submissions/me").hasAnyRole("STUDENT", "TEACHER", "ADMIN")
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * @brief Configures BCrypt password encoder with work factor 12.
     * @return BCryptPasswordEncoder instance.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    /**
     * @brief Exposes Spring Security AuthenticationManager bean.
     * @param config Authentication configuration.
     * @return AuthenticationManager instance.
     * @throws Exception if retrieval fails.
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
