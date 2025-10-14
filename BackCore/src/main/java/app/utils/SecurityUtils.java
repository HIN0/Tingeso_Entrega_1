package app.utils;

import entities.UserEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;

public class SecurityUtils {

    /**
     * Extrae el nombre de usuario (claim 'preferred_username') del JWT para
     * crear un UserEntity simple que será usado en los servicios de negocio
     * para el registro de auditoría (Kardex).
     */
    public static UserEntity getUserFromAuthentication(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new IllegalStateException("Usuario no autenticado. El contexto de seguridad es nulo."); 
        }

        if (!(authentication.getPrincipal() instanceof Jwt jwt)) {
            throw new IllegalArgumentException("El Principal esperado no es un JWT.");
        }
        
        // Keycloak usa 'preferred_username' para el nombre de usuario
        String username = jwt.getClaimAsString("preferred_username");

        if (username == null) {
             throw new IllegalStateException("La claim 'preferred_username' no se encontró en el JWT.");
        }

        // Retorna un UserEntity con solo el username para la trazabilidad (Kardex)
        return UserEntity.builder()
                .username(username)
                .build();
    }
}