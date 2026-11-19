import Keycloak from 'keycloak-js';

// Vite injects VITE_* variables at BUILD time; the defaults match compose.yml.
const keycloak = new Keycloak({
  url: import.meta.env.VITE_KEYCLOAK_URL || 'http://localhost:8090',
  realm: import.meta.env.VITE_KEYCLOAK_REALM || 'toolrent-realm',
  clientId: import.meta.env.VITE_KEYCLOAK_CLIENT_ID || 'toolrent-frontend',
});

export const hasRole = (role) => {
  return keycloak.hasRealmRole(role);
}

export default keycloak;
