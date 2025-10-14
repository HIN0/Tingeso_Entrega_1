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

        // Converter para mapear realm roles -> ROLE_*
        JwtAuthenticationConverter jwtAuthConverter = new JwtAuthenticationConverter();
        jwtAuthConverter.setJwtGrantedAuthoritiesConverter(new KeycloakRealmRoleConverter());

        http
            .csrf(AbstractHttpConfigurer::disable)
            // SOLUCIÓN CORS: Deshabilitamos el filtro CORS de Security, confiando en @CrossOrigin
            .cors(AbstractHttpConfigurer::disable) 
            
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                
                // CRÍTICO: Permitir OPTIONS para todas las rutas.
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll() 
                
                // Reglas de Autorización (RBAC)
                .requestMatchers("/tools/**").hasRole("ADMIN")
                .requestMatchers("/tariffs/**").hasRole("ADMIN")
                .requestMatchers("/clients/**").hasRole("ADMIN") 
                .requestMatchers("/kardex/**").hasAnyRole("ADMIN", "USER")
                .requestMatchers("/loans/**", "/returns/**").hasAnyRole("ADMIN", "USER")
                .requestMatchers("/reports/**").hasAnyRole("ADMIN", "USER")
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthConverter))
            );

        return http.build();
    }
}