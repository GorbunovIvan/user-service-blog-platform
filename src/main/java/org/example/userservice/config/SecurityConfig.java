package org.example.userservice.config;

import lombok.RequiredArgsConstructor;
import org.example.userservice.model.security.Permission;
import org.example.userservice.service.security.jwt.JwtConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtConfigurer jwtConfigurer;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(authz -> authz
                        .requestMatchers(new AntPathRequestMatcher("/auth/**")).permitAll()
                        .requestMatchers(new AntPathRequestMatcher("/api/**", "POST")).hasAuthority(Permission.CREATE.getAuthority())
                        .requestMatchers(new AntPathRequestMatcher("/api/**", "PUT")).hasAuthority(Permission.UPDATE.getAuthority())
                        .requestMatchers(new AntPathRequestMatcher("/api/**", "DELETE")).hasAuthority(Permission.DELETE.getAuthority())
                        .requestMatchers(new AntPathRequestMatcher("/api/**", "GET")).hasAuthority(Permission.READ.getAuthority())
                        .anyRequest().authenticated()
                )
                .with(jwtConfigurer, c -> c.setBuilder(http));

        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfiguration) throws Exception {
        return authConfiguration.getAuthenticationManager();
    }
}
