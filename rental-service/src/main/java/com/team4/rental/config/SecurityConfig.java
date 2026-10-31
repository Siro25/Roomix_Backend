package com.team4.rental.config;

import com.team4.rental.exceptions.GlobalExceptionHandler;
import com.team4.rental.security.AccountStatusFilter;
import java.util.Base64;
import java.util.Set;
import java.util.UUID;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.*;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.authentication.*;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {
    @Bean
    JwtDecoder jwtDecoder(@Value("${security.jwt.secret}") String secret,
                          @Value("${security.jwt.issuer}") String issuer) {
        byte[] key = Base64.getDecoder().decode(secret);
        if (key.length < 32 || issuer.isBlank()) {
            throw new IllegalArgumentException("JWT_SECRET must contain at least 32 bytes and issuer is required");
        }
        var decoder = NimbusJwtDecoder.withSecretKey(new SecretKeySpec(key, "HmacSHA256"))
                .macAlgorithm(MacAlgorithm.HS256).build();
        OAuth2TokenValidator<Jwt> claims = jwt -> {
            try {
                UUID.fromString(jwt.getSubject());
                if (jwt.getExpiresAt() == null || jwt.getIssuedAt() == null
                        || !jwt.getAudience().contains("roomix-api")
                        || !Set.of("TENANT", "LANDLORD", "ADMIN").contains(jwt.getClaimAsString("role"))) {
                    throw new IllegalArgumentException("Invalid claims");
                }
                return OAuth2TokenValidatorResult.success();
            } catch (RuntimeException exception) {
                return OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token"));
            }
        };
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(issuer), claims));
        return decoder;
    }

    @Bean
    FilterRegistrationBean<AccountStatusFilter> disableServletRegistration(AccountStatusFilter filter) {
        var registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, AccountStatusFilter accounts,
                                            GlobalExceptionHandler errors) throws Exception {
        var authorities = new JwtGrantedAuthoritiesConverter();
        authorities.setAuthoritiesClaimName("role");
        authorities.setAuthorityPrefix("ROLE_");
        var converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authorities);
        return http.csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable).httpBasic(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/invoices/vnpay/ipn", "/api/invoices/vnpay/return").permitAll()
                        .requestMatchers("/error").permitAll()
                        .requestMatchers("/api/rentals/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/rentals/**", "/api/tenancies/**", "/api/invoices/**")
                        .hasAnyRole("TENANT", "LANDLORD")
                        .anyRequest().denyAll())
                .exceptionHandling(error -> error.authenticationEntryPoint(errors).accessDeniedHandler(errors))
                .oauth2ResourceServer(resource -> resource
                        .authenticationEntryPoint(errors).accessDeniedHandler(errors)
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(converter)))
                .addFilterAfter(accounts, BearerTokenAuthenticationFilter.class)
                .build();
    }
}
