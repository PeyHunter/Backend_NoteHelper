package com.example.notehelper.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.List;

@Configuration
public class WebConfig {

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();

        // Tillader cookies og headers at blive sendt med
        config.setAllowCredentials(true);

        // Tilføj både dit live-domæne og din lokale test-adresse
        config.setAllowedOrigins(List.of(
                "https://ai.peytonhunter.com",
                "http://localhost:3000"
        ));

        // Hvilke typer kald må man lave
        config.setAllowedMethods(List.of("GET", "POST", "OPTIONS", "PUT", "DELETE"));

        // Hvilke headers må browseren sende (f.eks. Content-Type ved upload)
        config.setAllowedHeaders(List.of("*"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        // Gælder for alle dine endpoints (upload, check-key, etc.)
        source.registerCorsConfiguration("/**", config);

        return new CorsFilter(source);
    }
}