package app.utils;

import entities.UserEntity;
import entities.enums.UserRole;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import services.UserService;

import java.util.Collection;
import java.util.Map;

@Component
public class SecurityUtils {

    private final UserService userService;

    public SecurityUtils(UserService userService) {
        this.userService = userService;
    }

    /**
     * Resolves the authenticated Keycloak user (JWT) to its local UserEntity,
     * creating it on first access so every operation can be audited.
     */
    public UserEntity getUserFromAuthentication(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new IllegalStateException("User is not authenticated.");
        }
        if (!(authentication.getPrincipal() instanceof Jwt jwt)) {
            throw new IllegalArgumentException("Expected a JWT principal.");
        }

        String username = jwt.getClaimAsString("preferred_username");
        if (username == null) {
            throw new IllegalStateException("Claim 'preferred_username' not found in JWT.");
        }
        return userService.findOrCreate(username, resolveRole(jwt));
    }

    private UserRole resolveRole(Jwt jwt) {
        Map<String, Object> realmAccess = jwt.getClaim("realm_access");
        if (realmAccess != null && realmAccess.get("roles") instanceof Collection<?> roles
                && roles.contains("ADMIN")) {
            return UserRole.ADMIN;
        }
        return UserRole.EMPLOYEE;
    }
}
