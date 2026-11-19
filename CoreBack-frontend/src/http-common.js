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

// Request interceptor: attach the Keycloak JWT
apiClient.interceptors.request.use(async (config) => {
  // 1. Only when the user is authenticated
  if (keycloak.authenticated) {
    // 2. Refresh the token if it is about to expire
    try {
      await keycloak.updateToken(5); // refresh if it expires within 5 seconds
      
      // 3. Attach the (possibly refreshed) token
      config.headers.Authorization = `Bearer ${keycloak.token}`;
    } catch (error) {
      console.error('Error refreshing the Keycloak token:', error);
      // Optional: redirect to login if the refresh fails
      // keycloak.logout(); 
    }
  }
  return config;
}, (error) => {
  return Promise.reject(error);
});

export default apiClient;