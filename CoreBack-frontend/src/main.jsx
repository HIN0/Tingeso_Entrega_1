import { createRoot } from "react-dom/client";
import { ReactKeycloakProvider } from "@react-keycloak/web";
import keycloak from "./services/keycloak";
import App from "./App";
import "./index.css";

// Tokens stay in keycloak-js memory only: never copy them to localStorage,
// where any injected script (XSS) could read them.
createRoot(document.getElementById("root")).render(
  <ReactKeycloakProvider
    authClient={keycloak}
    initOptions={{ onLoad: "login-required" }}
  >
    <App />
  </ReactKeycloakProvider>
);
