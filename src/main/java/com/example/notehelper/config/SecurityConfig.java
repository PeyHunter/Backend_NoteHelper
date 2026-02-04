package com.example.notehelper.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Meget vigtigt: Aktiverer CORS-indstillingerne fra WebConfig
                .cors(Customizer.withDefaults())
                // Deaktiverer CSRF-beskyttelse, som ellers blokerer POST-kald
                .csrf(csrf -> csrf.disable())
                // Tillader alle anmodninger uden login
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().permitAll()
                );

        return http.build();
    }
}