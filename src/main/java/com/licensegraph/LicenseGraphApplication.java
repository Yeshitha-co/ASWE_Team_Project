package com.licensegraph;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;

/**
 * LicenseGraph Spring Boot entry point.
 *
 * <p>The generated in-memory user is disabled. Callers authenticate with an API key.
 */
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class LicenseGraphApplication {

    public static void main(String[] args) {
        SpringApplication.run(LicenseGraphApplication.class, args);
    }
}
