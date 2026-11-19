# ToolRent — Frontend

React 19 single-page application built with Vite. See the [main README](../README.md) for the full project.

## Scripts

| Command           | Description                                             |
|-------------------|---------------------------------------------------------|
| `npm install`     | Install dependencies                                    |
| `npm run dev`     | Dev server on http://localhost:5173 (proxies `/api` to http://localhost:8080) |
| `npm run build`   | Production build into `dist/`                           |
| `npm run lint`    | ESLint                                                  |

## Configuration (build time)

| Variable                  | Default                 |
|---------------------------|-------------------------|
| `VITE_API_URL`            | `/api`                  |
| `VITE_KEYCLOAK_URL`       | `http://localhost:8090` |
| `VITE_KEYCLOAK_REALM`     | `toolrent-realm`        |
| `VITE_KEYCLOAK_CLIENT_ID` | `toolrent-frontend`     |

Vite inlines these values when building, so they must be set before `npm run build` (not at container runtime).

## Structure

```
src/
├── components/   pages and forms (tools, loans, clients, tariffs, reports, kardex)
├── services/     one HTTP client per backend resource + Keycloak instance
├── utils/        apiError.js: turns backend errors into readable messages
├── http-common.js  axios instance; attaches and refreshes the Keycloak token
└── App.jsx       routes and role-based menu
```
