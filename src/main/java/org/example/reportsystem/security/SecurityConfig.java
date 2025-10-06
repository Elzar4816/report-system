package org.example.reportsystem.security;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;

import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

// свои классы
import org.example.reportsystem.security.JwtAuthFilter;
import org.example.reportsystem.service.AppUserDetailsService;

import java.util.List;

@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    private final JwtAuthFilter jwtFilter;

    @Bean PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http,
                                            JwtAuthFilter jwtFilter) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**").permitAll() // login/refresh/logout
                        // ↓ Если есть эндпоинты, которые ДОЛЖНЫ требовать логин:
                        .requestMatchers("/api/admin/**").authenticated()
                        // ↓ Важно: всё остальное — доступно и гостю
                        .anyRequest().permitAll()
                )
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
    // любая @Configuration
    @Bean
    public org.springframework.web.filter.CommonsRequestLoggingFilter requestLoggingFilter() {
        var f = new org.springframework.web.filter.CommonsRequestLoggingFilter();
        f.setIncludeClientInfo(true);     // ip, session, user
        f.setIncludeQueryString(true);    // ?a=1&b=2
        f.setIncludePayload(true);        // тело (осторожно с паролями)
        f.setMaxPayloadLength(4096);
        f.setIncludeHeaders(false);       // можно true, если нужно
        return f;
    }



    @Bean
    CorsConfigurationSource cors(@Value("${app.cors.allowed-origins}") String origins) {
        var cfg = new CorsConfiguration();
        cfg.setAllowedOrigins(List.of(origins.split(",")));
        cfg.setAllowedMethods(List.of("GET","POST","PUT","PATCH","DELETE","OPTIONS"));
        cfg.setAllowedHeaders(List.of("*"));
        cfg.setAllowCredentials(true);
        var src = new UrlBasedCorsConfigurationSource();
        src.registerCorsConfiguration("/**", cfg);
        return src;
    }

    @Bean
    AuthenticationProvider dao(AppUserDetailsService uds, PasswordEncoder enc) {
        var p = new DaoAuthenticationProvider();
        p.setUserDetailsService(uds);
        p.setPasswordEncoder(enc);
        return p;
    }

    @Bean
    AuthenticationManager authManager(AuthenticationConfiguration cfg) throws Exception {
        return cfg.getAuthenticationManager();
    }
}
