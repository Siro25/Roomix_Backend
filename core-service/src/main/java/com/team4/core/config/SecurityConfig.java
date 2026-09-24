package com.team4.core.config;

import com.team4.core.exception.GlobalExceptionHandler;
import com.team4.core.repositories.UserRepository;
import com.team4.core.security.CustomUserDetails;
import com.team4.core.security.JwtTokenProvider;
import java.util.Locale;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {
        @Bean
        PasswordEncoder passwordEncoder() {
                return new BCryptPasswordEncoder();
        }

        @Bean
        UserDetailsService userDetailsService(UserRepository users) {
                return username -> users.findByUsername(username.strip().toLowerCase(Locale.ROOT))
                                .map(CustomUserDetails::new)
                                .orElseThrow(() -> new UsernameNotFoundException("Invalid email or password"));
        }

        @Bean
        AuthenticationManager authenticationManager(UserDetailsService users, PasswordEncoder encoder) {
                var provider = new DaoAuthenticationProvider(users);
                provider.setPasswordEncoder(encoder);
                return new ProviderManager(provider);
        }

        @Bean
        SecurityFilterChain securityFilterChain(
                        HttpSecurity http,
                        JwtTokenProvider tokens,
                        GlobalExceptionHandler exceptionHandler) throws Exception {
                var authorities = new JwtGrantedAuthoritiesConverter();
                authorities.setAuthoritiesClaimName("role");
                authorities.setAuthorityPrefix("ROLE_");
                var converter = new JwtAuthenticationConverter();
                converter.setJwtGrantedAuthoritiesConverter(authorities);

                return http
                                .csrf(AbstractHttpConfigurer::disable)
                                .formLogin(AbstractHttpConfigurer::disable)
                                .httpBasic(AbstractHttpConfigurer::disable)
                                .logout(AbstractHttpConfigurer::disable)
                                .requestCache(AbstractHttpConfigurer::disable)
                                .exceptionHandling(exception -> exception
                                                .authenticationEntryPoint(exceptionHandler)
                                                .accessDeniedHandler(exceptionHandler))
                                .sessionManagement(session -> session
                                                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                                .authorizeHttpRequests(auth -> auth
                                                .requestMatchers(HttpMethod.POST, "/api/auth/register",
                                                                "/api/auth/login")
                                                .permitAll()
                                                .requestMatchers("/error").permitAll()
                                                .requestMatchers("/api/admin", "/api/admin/**").hasRole("ADMIN")
                                                .anyRequest().authenticated())
                                .oauth2ResourceServer(resource -> resource
                                                .jwt(jwt -> jwt.decoder(tokens).jwtAuthenticationConverter(converter)))
                                .build();
        }
}
