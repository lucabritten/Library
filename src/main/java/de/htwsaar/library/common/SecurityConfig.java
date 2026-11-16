package de.htwsaar.library.common;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain api(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .cors(Customizer.withDefaults())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health/**", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/members").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/books/**", "/api/v1/authors/**", "/api/v1/copies/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/token").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/books/**", "/api/v1/authors/**").permitAll()
                        .requestMatchers("/api/v1/books/**", "/api/v1/authors/**", "/api/v1/copies/**").hasAuthority("SCOPE_catalog:write")
                        .requestMatchers("/actuator/**").hasAuthority("SCOPE_admin")
                        .anyRequest().authenticated())
                .oauth2ResourceServer(o -> o.jwt(Customizer.withDefaults()))
                .build();
    }

    @Bean
    PasswordEncoder encoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }
}
