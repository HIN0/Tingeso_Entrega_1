package app.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.*;
import java.util.stream.Collectors;

/** Maps the Keycloak realm roles of the token (realm_access.roles) to ROLE_ authorities. */
public class KeycloakRealmRoleConverter implements Converter<Jwt, Collection<GrantedAuthority>> {
    // No @NonNull needed: since Spring 7 the Converter API is null-marked (JSpecify), so jwt is non-null by contract
    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        Map<String, Object> realmAccess = jwt.getClaim("realm_access");
        if (realmAccess == null) return Collections.emptySet();

        @SuppressWarnings("unchecked")
        Collection<String> roles = (Collection<String>) realmAccess.get("roles");
        if (roles == null) return Collections.emptySet();

        return roles.stream()
                // Realm roles are ADMIN / EMPLOYEE; avoid "ROLE_ROLE_X" if a role already has the prefix
                .map(role -> new SimpleGrantedAuthority(role.startsWith("ROLE_") ? role : "ROLE_" + role))
                .collect(Collectors.toSet());
    }
}
