package com.msedcl.billing.shared.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Configuration
public class CorsConfig {

    @Value("${cors.allowed.origins:}")
    private String allowedOrigins;

    // Optional: wildcard patterns such as https://*.vercel.app for preview deployments
    @Value("${cors.allowed.origin-patterns:}")
    private String allowedOriginPatterns;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // Support both exact origins and wildcard origin patterns
        List<String> originList = splitAndTrim(allowedOrigins);
        List<String> patternList = splitAndTrim(allowedOriginPatterns);

        if (!patternList.isEmpty()) {
            configuration.setAllowedOriginPatterns(patternList);
        }
        if (!originList.isEmpty()) {
            configuration.setAllowedOrigins(originList);
        }
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    private List<String> splitAndTrim(String csv) {
        if (csv == null || csv.trim().isEmpty()) {
            return List.of();
        }
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }
}
