package com.parknet.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.env.Environment;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import java.io.IOException;
import java.util.Arrays;

@Configuration
public class SecurityConfig {

    private final Environment environment;

    public SecurityConfig(Environment environment) {
        this.environment = environment;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        boolean devProfile = isDevProfile();

        http
                .authorizeHttpRequests(auth -> {
                    auth.requestMatchers("/css/**", "/js/**", "/images/**", "/vendor/**", "/uploads/**", "/favicon.ico", "/error").permitAll();
                    if (devProfile) {
                        auth.requestMatchers("/h2-console/**").permitAll();
                    }
                    auth.requestMatchers(HttpMethod.GET, "/listings/new", "/listings/*/edit", "/my-listings", "/my-reservations", "/reservation-requests", "/owner/reservations").authenticated();
                    auth.requestMatchers(HttpMethod.POST, "/listings", "/listings/*/edit", "/listings/*/delete", "/listings/*/reserve", "/api/listings/map", "/api/listings/*/geometry", "/reservations/*/confirm", "/reservations/*/cancel").authenticated();
                    auth.requestMatchers(HttpMethod.GET, "/", "/listings", "/listings/*", "/api/listings/map", "/login", "/register").permitAll();
                    auth.requestMatchers(HttpMethod.POST, "/register").permitAll();
                    auth.anyRequest().authenticated();
                })
                .formLogin(login -> login
                        .loginPage("/login")
                        .usernameParameter("email")
                        .passwordParameter("password")
                        .defaultSuccessUrl("/", true)
                        .permitAll()
                )
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, authException) -> {
                            if (isApiRequest(request)) {
                                writeJsonError(
                                        response,
                                        HttpServletResponse.SC_UNAUTHORIZED,
                                        "Влезте в профила си, за да продължите."
                                );
                            } else {
                                response.sendRedirect(request.getContextPath() + "/login");
                            }
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            if (isApiRequest(request)) {
                                writeJsonError(
                                        response,
                                        HttpServletResponse.SC_FORBIDDEN,
                                        "Нямате права за това действие."
                                );
                            } else {
                                response.sendError(HttpServletResponse.SC_FORBIDDEN);
                            }
                        })
                )
                .logout(logout -> logout
                        .logoutSuccessUrl("/")
                        .permitAll()
                );

        if (devProfile) {
            http
                    .csrf(csrf -> csrf.ignoringRequestMatchers("/h2-console/**"))
                    .headers(headers -> headers.frameOptions(frameOptions -> frameOptions.sameOrigin()));
        }

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    private boolean isDevProfile() {
        return Arrays.asList(environment.getActiveProfiles()).contains("dev");
    }

    private boolean isApiRequest(HttpServletRequest request) {
        return request.getRequestURI() != null && request.getRequestURI().startsWith("/api/");
    }

    private void writeJsonError(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json");
        response.getWriter().write("{\"message\":\"" + message + "\"}");
    }
}
