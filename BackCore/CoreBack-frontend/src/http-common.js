import axios from "axios";
import keycloak from "./services/keycloak";

const api = axios.create({
  baseURL: "http://localhost:8080",
  headers: { "Content-type": "application/json" },
});


api.interceptors.request.use(async (config) => {
  if (keycloak?.token) {
    await keycloak.updateToken(30).catch(() => keycloak.login());
    config.headers.Authorization = `Bearer ${keycloak.token}`;
  }
  return config;
});

export default api;
