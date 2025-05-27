package com.ps.securityctf.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.annotation.web.configurers.LogoutConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;

    public SecurityConfig(CustomUserDetailsService userDetailsService) {
        this.userDetailsService = userDetailsService;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/**").permitAll()  // Allow access to Actuator endpoints
                        .requestMatchers("/swagger-ui.html").permitAll()  // Allow access to Swagger UI
                        .requestMatchers("/swagger-ui/**").permitAll()  // Allow access to Swagger UI resources
                        .requestMatchers("/api-docs/**").permitAll()  // Allow access to OpenAPI docs
                        .requestMatchers("/v3/api-docs/**").permitAll()  // Allow access to OpenAPI docs
                        .requestMatchers("/api/auth/**").permitAll()  // Allow access to JWT auth endpoints
                        .requestMatchers("/api/injection/**").permitAll()  // Allow access to injection endpoints (vulnerable)
                        .requestMatchers("/*.html").permitAll()  // Allow access to all HTML pages
                        .requestMatchers("/").permitAll()  // Allow access to root path
                        .requestMatchers("/css/**").permitAll()  // Allow access to CSS files
                        .requestMatchers("/js/**").permitAll()  // Allow access to JavaScript files
                        .requestMatchers("/api/users/**").authenticated()  // Require authentication for user endpoints
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .defaultSuccessUrl("/api/users/me", true)
                        .permitAll()
                )
                .logout(LogoutConfigurer::permitAll
                )
                .csrf(csrf -> csrf.disable())  // Disable CSRF for API endpoints
                .headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin));

        return http.build();
    }

    @Bean
    public AuthenticationManager authManager(HttpSecurity http) throws Exception {
        AuthenticationManagerBuilder authenticationManagerBuilder =
                http.getSharedObject(AuthenticationManagerBuilder.class);
        authenticationManagerBuilder.userDetailsService(userDetailsService)
                .passwordEncoder(passwordEncoder());
        return authenticationManagerBuilder.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
