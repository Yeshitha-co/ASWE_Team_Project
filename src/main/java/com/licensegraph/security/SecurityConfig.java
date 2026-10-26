package com.licensegraph.security;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * AI-assisted: Spring Security configuration.
 *
 * <p>Stateless API security. The API-key filter enforces authentication and writes
 * the Section 10 error body. Authorization stays open so a missing key is 401 from that
 * filter, not 403.
 */
@Configuration
public class SecurityConfig {

    @Bean
    ApiKeyFilter apiKeyFilter(ApiClientRepository clients, ApiErrorResponseWriter errors) {
        return new ApiKeyFilter(clients, errors);
    }

    /**
     * Keeps the filter inside the security chain only. A {@code Filter} bean would also be
     * registered with the servlet container and run twice.
     */
    @Bean
    FilterRegistrationBean<ApiKeyFilter> apiKeyFilterRegistration(ApiKeyFilter apiKeyFilter) {
        FilterRegistrationBean<ApiKeyFilter> registration =
                new FilterRegistrationBean<>(apiKeyFilter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, ApiKeyFilter apiKeyFilter)
            throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .addFilterBefore(apiKeyFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
