package com.nunclear.escritores.config;

import com.nunclear.escritores.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.http.HttpMethod;

@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**"
                        ).permitAll()
                        .requestMatchers(
                                "/auth/register",
                                "/auth/login",
                                "/auth/refresh",
                                "/auth/forgot-password",
                                "/auth/reset-password",
                                "/auth/verify-email",
                                "/users/*",
                                "/users/*/public-profile",
                                "/users/*/stories",
                                "/stories",
                                "/stories/*",
                                "/stories/slug/*",
                                "/stories/search",
                                "/stories/user/*",
                                "/chapters/*",
                                "/chapters/story/*",
                                "/chapters/story/*/published",
                                "/chapters/search",
                                "/arcs/*",
                                "/arcs/story/*",
                                "/volumes/*",
                                "/volumes/story/*",
                                "/characters/*",
                                "/characters/story/*",
                                "/characters/search",
                                "/skills/*",
                                "/skills/story/*",
                                "/skills/search",
                                "/character-skills/character/*",
                                "/character-skills/skill/*",
                                "/events/*",
                                "/events/story/*",
                                "/events/chapter/*",
                                "/events/search",
                                "/items/*",
                                "/items/story/*",
                                "/media/*",
                                "/media/chapter/*",
                                "/media/*/download",
                                "/comments/*",
                                "/comments/story/*",
                                "/comments/chapter/*",
                                "/comments/*/replies",
                                "/ratings/*",
                                "/ratings/story/*",
                                "/ratings/story/*/average",
                                "/global-notices/*",
                                "/global-notices/active",
                                "/favorites/story/*/count",
                                "/follows/user/*/followers",
                                "/follows/user/*/count",
                                "/metrics/views/story",
                                "/metrics/views/chapter",
                                "/metrics/stories/top-viewed"
                        ).permitAll()
                        .requestMatchers("/dashboard/**").authenticated()
                        .requestMatchers("/ideas/**").authenticated()
                        .requestMatchers("/favorites/**").authenticated()
                        .requestMatchers("/follows/**").authenticated()
                        .requestMatchers("/reports/**").authenticated()
                        .requestMatchers("/sanctions/**").authenticated()
                        .requestMatchers("/moderation/**").authenticated()
                        .requestMatchers("/admin/**").authenticated()
                        .anyRequest().authenticated()

                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) {
        return configuration.getAuthenticationManager();
    }
}