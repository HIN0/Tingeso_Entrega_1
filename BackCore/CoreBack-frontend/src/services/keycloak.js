import Keycloak from 'keycloak-js';

const keycloak = new Keycloak({
  url: 'http://localhost:9090',
  realm: 'sisgr-realm',
  clientId: 'sisgr-frontend',
});

// Función auxiliar para revisar roles
export const hasRole = (role) => {
  // keycloak.hasRealmRole() busca directamente en el realm
  // Si usa roles de cliente: keycloak.hasResourceRole(role, 'toolrent-app')
  return keycloak.hasRealmRole(role);
}

export default keycloak;