package com.agent.baki.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Arrays;

/**
 * CORS configuration for allowing React frontend to access the API and H2 console
 */
@Configuration
public class CorsConfig {

   @Bean
    public CorsFilter corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();
        
        config.setAllowCredentials(true);
        config.addAllowedOrigin("http://localhost:3000");
        config.addAllowedHeader("*");
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        
        // Register CORS configuration for API endpoints
        source.registerCorsConfiguration("/api/**", config);
        
        // Register CORS configuration for H2 console
        CorsConfiguration h2Config = new CorsConfiguration();
        h2Config.setAllowCredentials(true);
        h2Config.addAllowedOriginPattern("*");
        h2Config.addAllowedHeader("*");
        h2Config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        source.registerCorsConfiguration("/h2-console/**", h2Config);
        
        return new CorsFilter(source);
    }
}

// Made with Bob
