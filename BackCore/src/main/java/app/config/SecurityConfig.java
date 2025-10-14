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
                
                // CORRECCIÓN CLAVE: Permite a USER y ADMIN leer las herramientas (GET)
                .requestMatchers(HttpMethod.GET, "/tools/**").hasAnyRole("ADMIN", "USER")
                .requestMatchers(HttpMethod.GET, "/loans/**").hasAnyRole("ADMIN", "USER") 
                
                // Las demás operaciones de /tools (POST/PUT/DELETE) siguen siendo solo para ADMIN
                .requestMatchers("/tools/**").hasRole("ADMIN") 

                // Rutas que requieren solo ADMIN
                .requestMatchers("/tariffs/**").hasRole("ADMIN")
                .requestMatchers("/clients/**").hasRole("ADMIN") 
                
                // Rutas que requieren ADMIN o USER (Préstamos/Reportes)
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