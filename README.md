# ToolRent

**Tool rental management system** for a store that lends tools for construction, repairs and home projects.
It replaces spreadsheets and paper records with a web application that controls the inventory in real time,
applies the rental rules automatically (limits, late fees, damage charges), keeps an auditable record of every
stock movement and produces management reports.

Full-stack web application: **React** frontend, **Spring Boot** REST API with a layered architecture,
**PostgreSQL**, **Keycloak** for identity and access management, deployed with **Docker Compose** behind an
**Nginx** load balancer and built by a **Jenkins** CI/CD pipeline.

---

## Contents

- [Features](#features)
- [Architecture](#architecture)
- [Tech stack](#tech-stack)
- [Getting started](#getting-started)
- [Local development](#local-development)
- [Testing and quality](#testing-and-quality)
- [CI/CD pipeline](#cicd-pipeline)
- [REST API](#rest-api)
- [Configuration](#configuration)
- [Project structure](#project-structure)
- [Known limitations](#known-limitations)

---

## Features

Two roles, managed in Keycloak: **Administrator** (full access) and **Employee** (loans, returns and reports).

| Module | What it does | Role |
|---|---|---|
| **Inventory** | Register tools (name, category, stock, replacement value); adjust stock; decommission tools; evaluate damaged units (*repaired* back to stock, or *irreparable* and written off). The status is derived from unit counters: available, loaned, under repair, decommissioned. | Admin |
| **Loans and returns** | Register a loan with start and due date after validating every business rule; register returns in good condition, with light damage or with irreparable damage; compute the amount due automatically; mark debts as paid. | Admin, Employee |
| **Clients** | Register clients (validated Chilean RUT, phone, email), edit their data, restrict them or reactivate them once their debts are settled. | Admin (employees can read) |
| **Tariffs** | Configure the daily rental fee, the daily late fee and the light-damage repair fee. | Admin |
| **Kardex** | Automatic audit trail of every inventory movement (income, loan, return, repair, repaired, decommission, manual decrease) with date, user and quantity; query by tool or by date range. | Admin |
| **Reports** | Active and late loans, clients with late loans, ranking of the most loaned tools; optional date range filter. | Admin, Employee |
| **Users and roles** | Login with username and password through Keycloak (OIDC + PKCE); authorization by role in both the API and the UI. | — |

### Business rules enforced by the backend

- A loan requires an **active** client without late loans, overdue loans or unpaid debts, and an available tool with stock.
- A client can have at most **5 open loans** and cannot have the same tool twice at the same time.
- The due date cannot be before the start date; a loan cannot be modified once registered.
- **Amount due** = rental days × daily fee (minimum 1 day, no refunds for early returns)
  \+ days late × daily late fee
  \+ repair fee (light damage) **or** replacement value (irreparable damage).
- An hourly job marks overdue loans as **late** and restricts their clients; a returned loan keeps the client
  restricted until the debt is paid.
- Every loan and return records the **responsible user and timestamp**; every stock change creates a Kardex movement.

---

## Architecture

```mermaid
flowchart LR
    user([Browser])

    subgraph compose[Docker Compose]
        nginx[Nginx<br/>load balancer :80]
        subgraph fe[Frontend x3]
            f1[React SPA<br/>served by Nginx]
        end
        subgraph be[Backend x3]
            b1[Spring Boot<br/>REST API]
        end
        db[(PostgreSQL)]
        kc[Keycloak<br/>:8090]
    end

    user -- "/" --> nginx
    user -- "/api/*" --> nginx
    nginx -- round robin --> fe
    nginx -- round robin --> be
    be --> db
    user -- login (OIDC + PKCE) --> kc
    be -. JWT signing keys .-> kc
```

- The browser logs in against **Keycloak** and sends the access token (JWT) with every API call.
- **Nginx** is the single entry point: `/` goes to one of the three frontend replicas and `/api/*` to one of the
  three backend replicas (the `X-Upstream` response header shows which one answered).
- Each backend replica is a stateless **OAuth2 resource server**: it validates the JWT signature with Keycloak's
  public keys and maps the realm roles (`ADMIN`, `EMPLOYEE`) to authorities.
- `backend-1` creates the schema and loads the seed data; the other replicas start once it is healthy.

### Backend layers

```
controllers  →  services  →  repositories  →  entities
(HTTP, DTOs)    (business     (Spring Data      (JPA)
                 rules)        JPA)
```

Cross-cutting concerns live in `app/`: security configuration, Keycloak role mapping, the global exception
handler (consistent JSON errors: `{status, error, message, fieldErrors}`) and the resolution of the
authenticated user.

---

## Tech stack

| Layer | Technology |
|---|---|
| Frontend | React 19, Vite 7, React Router 7, Axios, keycloak-js 26 |
| Backend | Java 21, Spring Boot 4.1 (Spring MVC, Spring Data JPA / Hibernate 7, Bean Validation, Spring Security 7 OAuth2 Resource Server, Actuator), Lombok |
| Database | PostgreSQL 15 |
| Identity and access | Keycloak 24 (realm imported on startup) |
| Infrastructure | Docker, Docker Compose, Nginx |
| CI/CD | Jenkins (pipeline as code), Docker Hub |
| Testing | JUnit 6, Mockito 5, Spring MockMvc, Spring Security Test, JaCoCo |

---

## Getting started

**Requirements:** Docker Desktop (or Docker Engine with Compose v2). Ports **80**, **5433** and **8090** must be free.

```bash
git clone https://github.com/HIN0/Tingeso_Entrega_1.git
cd Tingeso_Entrega_1
docker compose pull
docker compose up -d
```

The first start takes about a minute (Keycloak and the backend replicas start in order). Then open:

| URL | Service |
|---|---|
| http://localhost | ToolRent |
| http://localhost:8090 | Keycloak admin console (`admin` / `admin`) |

**Demo users** (defined in [`keycloak-data/realm-export.json`](keycloak-data/realm-export.json)):

| User | Password | Role |
|---|---|---|
| `diego` | `diego123` | Administrator |
| `juan` | `juan123` | Employee |

> These credentials exist only for local demos. Change them, and the database and Keycloak admin passwords in
> `compose.yml`, before exposing the application anywhere.

The database is seeded with 20 clients, 30 tools and sample loans (active, late, and returned with pending payment). To start again from a clean
database: `docker compose down -v && docker compose up -d`.

---

## Local development

Start only the infrastructure in Docker and run the applications from the IDE:

```bash
docker compose up -d db keycloak
```

**Backend** (http://localhost:8080):

```bash
cd BackCore
# The compose database is published on port 5433
DB_URL=jdbc:postgresql://localhost:5433/toolrent ./mvnw spring-boot:run
```

On Windows PowerShell: `$env:DB_URL="jdbc:postgresql://localhost:5433/toolrent"; .\mvnw.cmd spring-boot:run`

**Frontend** (http://localhost:5173, proxies `/api` to the backend):

```bash
cd CoreBack-frontend
npm install
npm run dev
```

---

## Testing and quality

```bash
cd BackCore
./mvnw test
```

**166 tests**, three kinds:

| Kind | Classes | What they cover |
|---|---|---|
| Unit (Mockito) | `*ServiceTest`, `RutValidatorTest` | Business rules of every service: loan validations, fee calculation, stock and status transitions, reactivation, reports, RUT check digit |
| Web layer (`@WebMvcTest`) | `WebLayerTest` | Real controllers, security and exception handler with simulated JWT users: 401/403 per role, 400 with field errors, 404/405, no internal details on 500, no mass assignment, password never serialized |
| Service validation | `ServiceValidationTest` | Services behind Spring's method-validation proxy, which unit tests bypass |

**Quality gate:** JaCoCo fails the build if line coverage of the business layer (`services`) drops below **90%**
(currently **98.6%**). Report: `BackCore/target/site/jacoco/index.html`.

Frontend: `npm run lint` (ESLint, 0 problems) and `npm audit` (0 known vulnerabilities).

---

## CI/CD pipeline

The [`Jenkinsfile`](Jenkinsfile) defines the pipeline:

```
Checkout (GitHub) → Test backend (JUnit + JaCoCo gate) → Build & push backend image → Build & push frontend image
```

Images are published to Docker Hub as `izkybin/toolrent-backend` and `izkybin/toolrent-frontend`, tagged with the
**build number** (traceable, allows rollback) and **`latest`** (used by `compose.yml`). A failing test or a coverage
drop stops the pipeline before anything is published.

A ready-to-use Jenkins (JDK 21, Docker CLI and the required plugins) runs locally with
`docker compose -f jenkins/compose.yml up -d --build`; setup steps in [`jenkins/README.md`](jenkins/README.md).

---

## REST API

All endpoints are under `/api` when accessed through Nginx and require a Keycloak access token
(`Authorization: Bearer <token>`), except `GET /actuator/health`.

| Resource | Endpoints | Roles |
|---|---|---|
| Tools | `GET /tools`, `GET /tools/{id}` | Admin, Employee |
| | `POST /tools`, `PUT /tools/{id}`, `PATCH /tools/{id}/stock`, `PATCH /tools/{id}/repair/complete`, `PATCH /tools/{id}/repair/write-off`, `PUT /tools/{id}/decommission` | Admin |
| Clients | `GET /clients`, `GET /clients/{id}` | Admin, Employee |
| | `POST /clients`, `PUT /clients/{id}`, `PATCH /clients/{id}/status`, `PATCH /clients/{id}/activate` | Admin |
| Loans | `GET /loans` (open loans), `GET /loans/{id}`, `GET /loans/client/{clientId}/unpaid`, `POST /loans`, `PUT /loans/{id}/return` | Admin, Employee |
| | `PATCH /loans/{id}/pay` | Admin |
| Tariffs | `GET /tariffs`, `PUT /tariffs` | Admin |
| Kardex | `GET /kardex/tool/{toolId}`, `GET /kardex/date?start=&end=` | Admin |
| Reports | `GET /reports/loans?status=&from=&to=`, `GET /reports/clients/late?from=&to=`, `GET /reports/tools/top?from=&to=` | Admin, Employee |
| Users | `GET /users/{username}`, `POST /users` | Admin |

Errors always use the same JSON shape, for example:

```json
{ "status": 400, "error": "Bad Request", "message": "Validation failed. Check 'fieldErrors'.",
  "fieldErrors": { "email": "Invalid email format" } }
```

---

## Configuration

Backend environment variables (defaults target a local run against the compose infrastructure):

| Variable | Default | Purpose |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/toolrent` | JDBC URL |
| `DB_USERNAME` / `DB_PASSWORD` | `postgres` / `postgres` | Database credentials |
| `DDL_AUTO` | `update` | Hibernate schema management |
| `SQL_INIT_MODE` | `always` | Load `data.sql` (it only inserts into empty tables) |
| `KEYCLOAK_ISSUER_URI` | `http://localhost:8090/realms/toolrent-realm` | Expected `iss` claim (URL used by the browser) |
| `KEYCLOAK_JWK_SET_URI` | `http://localhost:8090/realms/toolrent-realm/protocol/openid-connect/certs` | Signing keys (URL reachable by the backend) |

Frontend build-time variables are documented in [`CoreBack-frontend/README.md`](CoreBack-frontend/README.md).

---

## Project structure

```
.
├── BackCore/                Spring Boot REST API
│   └── src/main/java/
│       ├── app/             bootstrap, security config, Keycloak role converter, error handler
│       ├── controllers/     REST endpoints
│       ├── services/        business rules
│       ├── repositories/    Spring Data JPA repositories
│       ├── entities/        JPA entities and enums
│       └── dtos/            request bodies with validation
├── CoreBack-frontend/       React SPA (Vite)
├── keycloak-data/           Keycloak realm: roles, client and demo users
├── nginx/                   load balancer configuration
├── jenkins/                 local Jenkins image and setup guide
├── compose.yml              full deployment (9 containers)
└── Jenkinsfile              CI/CD pipeline
```

---

## Known limitations

- **Local deployment only:** HTTP without TLS, demo credentials in the repository, Keycloak in development mode
  (`start-dev`) and Jenkins running as root to use the Docker socket.
- **Keycloak 24** is behind the current release and should be upgraded together with a production setup
  (`start` mode, its own database, HTTPS).
- Tools are tracked as **types with unit counters**, not as individual serial-numbered units, so the history of a
  single physical unit is not available.
- The backend packages use short top-level names (`app`, `services`, ...) instead of a reverse-domain base package.
- No end-to-end browser tests; the web layer is covered with MockMvc.

---

*Academic project for the course* Métodos y Técnicas de Ingeniería de Software.
