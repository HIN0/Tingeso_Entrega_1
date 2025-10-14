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
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource; 
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

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
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsConfigurationSource())) // Usa el Bean que definiremos abajo
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                
                // Permitir OPTIONS explícitamente (aunque el corsConfigurationSource debería bastar)
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll() 
                
                // Reglas de Autorización (Roles)
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
    
    // === Nueva definición de CORS como Bean de Seguridad ===
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // Orígenes permitidos (su frontend Vite)
        configuration.setAllowedOrigins(List.of("http://localhost:5173"));
        // Métodos permitidos
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        // Encabezados permitidos (permitimos todos para JWT/Authorization)
        configuration.setAllowedHeaders(List.of("*"));
        // Permitir credenciales (importante para CORS)
        configuration.setAllowCredentials(true);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration); // Aplica a todas las rutas
        return source;
    }
}