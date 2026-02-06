package org.papercloud.de.pdfsecurity.config;

import lombok.RequiredArgsConstructor;
import org.papercloud.de.pdfsecurity.filter.JwtRequestFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

  private final JwtRequestFilter jwtRequestFilter;
  private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
            // 1. We’re a stateless REST API, so disable CSRF and HTTP sessions
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // 2. Define our authorization rules
            .authorizeHttpRequests(auth -> auth
                    // 2a. Allow anyone to register or login
                    .requestMatchers("/api/auth/**").permitAll()
                    // 2b. Allow anyone to hit the ping endpoint
                    .requestMatchers("/api/documents/ping").permitAll()
                    // 2c. All other /api/documents/** endpoints require a valid JWT
                    .requestMatchers("/api/documents/**").authenticated()
                    // 2d. (Catch-all) any other request also needs authentication
                    .anyRequest().authenticated()
            )

            // 3. Handle auth errors with our custom entry point (returns JSON 401)
            .exceptionHandling(ex -> ex
                    .authenticationEntryPoint(jwtAuthenticationEntryPoint)
            )

            // 4. Plug in our JWT filter before Spring’s username/password filter
            .addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class);

    return http.build();
  }


  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
    return authConfig.getAuthenticationManager();
  }
}

