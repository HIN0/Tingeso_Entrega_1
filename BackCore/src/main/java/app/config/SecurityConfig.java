package app.config;

import app.security.KeycloakRealmRoleConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        JwtAuthenticationConverter jwtAuthConverter = new JwtAuthenticationConverter();
        jwtAuthConverter.setJwtGrantedAuthoritiesConverter(new KeycloakRealmRoleConverter());

        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(AbstractHttpConfigurer::disable) 
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                // Health check used by docker compose
                .requestMatchers("/actuator/health").permitAll()

                // EMPLOYEE: loans/returns and reports only. It may READ tools and clients
                // because it needs them to register a loan.
                .requestMatchers(HttpMethod.GET, "/tools/**").hasAnyRole("ADMIN", "EMPLOYEE")
                .requestMatchers(HttpMethod.GET, "/clients/**").hasAnyRole("ADMIN", "EMPLOYEE")
                .requestMatchers("/loans/**").hasAnyRole("ADMIN", "EMPLOYEE")
                .requestMatchers("/reports/**").hasAnyRole("ADMIN", "EMPLOYEE")

                // ADMIN: full access (inventory, clients, tariffs, kardex, users)
                .requestMatchers("/tools/**").hasRole("ADMIN")
                .requestMatchers("/clients/**").hasRole("ADMIN")
                .requestMatchers("/tariffs/**").hasRole("ADMIN")
                .requestMatchers("/kardex/**").hasRole("ADMIN")
                .requestMatchers("/users/**").hasRole("ADMIN")

                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthConverter))
            );

        return http.build();
    }
}