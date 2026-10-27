package com.connectx.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.http.HttpStatus;

@Configuration
public class SecurityConfig {

private final JwtAuthenticationFilter jwtAuthenticationFilter;

public SecurityConfig(
        JwtAuthenticationFilter jwtAuthenticationFilter) {
    this.jwtAuthenticationFilter = jwtAuthenticationFilter;
}

@Bean
public SecurityFilterChain securityFilterChain(
        HttpSecurity http) throws Exception {

    http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session
                    .sessionCreationPolicy(
                            SessionCreationPolicy.STATELESS
                    )
            )
            .exceptionHandling(exceptions -> exceptions
                    .authenticationEntryPoint(
                            new HttpStatusEntryPoint(
                                    HttpStatus.UNAUTHORIZED
                            )
                    )
            )
            .authorizeHttpRequests(auth -> auth
                    .requestMatchers(
                            "/api/health",
                            "/api/auth/register",
                            "/api/auth/login",
                            "/h2-console",
                            "/h2-console/**"
                    ).permitAll()
                    .anyRequest().authenticated()
            )
            .headers(headers -> headers
                    .frameOptions(frame -> frame.sameOrigin())
            )
            .httpBasic(basic -> basic.disable())
            .formLogin(form -> form.disable())
            .addFilterBefore(
                    jwtAuthenticationFilter,
                    UsernamePasswordAuthenticationFilter.class
            );

    return http.build();
}


}
