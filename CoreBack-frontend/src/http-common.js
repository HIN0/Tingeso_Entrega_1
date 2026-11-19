import axios from "axios";
import keycloak from "./services/keycloak"; 

const apiClient = axios.create({
  // Relative path: Nginx load balancer forwards /api/* to the backend replicas
  // (in "npm run dev" the Vite proxy forwards it to localhost:8080).
  baseURL: import.meta.env.VITE_API_URL || "/api",
  headers: {
    "Content-type": "application/json",
  },
});

// Interceptor de Solicitudes: Adjuntar el token JWT
apiClient.interceptors.request.use(async (config) => {
  // 1. Asegurar que Keycloak esté inicializado y autenticado
  if (keycloak.authenticated) {
    // 2. Intentar actualizar el token si está por expirar
    try {
      await keycloak.updateToken(5); // Forzar refresh si expira en 5 segundos
      
      // 3. Adjuntar el nuevo token
      config.headers.Authorization = `Bearer ${keycloak.token}`;
    } catch (error) {
      console.error('Error al actualizar o adjuntar el token de Keycloak:', error);
      // Opcional: Redirigir a login si falla el refresh
      // keycloak.logout(); 
    }
  }
  return config;
}, (error) => {
  return Promise.reject(error);
});

export default apiClient;