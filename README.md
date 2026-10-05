# Postroom

A full-stack social publishing workspace for drafting, scheduling, and managing posts.

## Included

- Spring Boot 3.5 / Java 17 API with PostgreSQL, Flyway schema migration, JPA, validation, and a local H2 profile.
- Stateless JWT access tokens, rotating opaque refresh tokens stored as SHA-256 hashes, HttpOnly refresh cookies, BCrypt passwords, and role-based authorization.
- Owner-scoped post CRUD, scheduled publishing, validated sorting/pagination, `JOIN FETCH`/entity-graph author loading, and an Ehcache trending feed.
- AES-256-GCM JPA converter for private user notes. The AES key and JWT secret are required environment properties outside the local-only profile.
- Standard response envelopes, centralized errors, CORS, MDC correlation IDs, request timing logs, Actuator health, and an admin-only overview.
- React 19 + TypeScript client with session restore/refresh, post composer, filters, scheduling queue, editing, deletion, and paging.
- Postman collection, JMeter read-load plan, and backend tests for crypto, token rotation, ownership, pagination, and RBAC.

## Structure

```text
FS-5_6_7/
├── backend/
│   ├── .mvn/wrapper/                 # Pinned Maven Wrapper
│   ├── mvnw.cmd
│   ├── pom.xml
│   └── src/
│       ├── main/java/com/example/social/
│       │   ├── common/{api,crypto,error,logging}/
│       │   ├── config/
│       │   ├── post/                 # CRUD, paging, scheduling, cache
│       │   ├── security/             # JWT, refresh rotation, RBAC
│       │   └── user/
│       ├── main/resources/            # Profiles, Ehcache, Flyway SQL
│       └── test/java/com/example/social/
├── frontend/                          # Vite + React + TypeScript
├── docs/
│   ├── API-TESTING.md
│   └── postman/Postroom.postman_collection.json
├── performance/jmeter/posts-read-load.jmx
└── docker-compose.yml                 # PostgreSQL 17
```

## Run Locally

Requirements: Java 17+, Node 20.19+ (or 22.12+), and npm. Maven is downloaded by the checked-in wrapper. Docker is optional if using the in-memory H2 profile.

Start the API in one PowerShell terminal:

```powershell
cd backend
$env:SPRING_PROFILES_ACTIVE = 'local-h2'
.\mvnw.cmd spring-boot:run
```

Start the frontend in a second terminal:

```powershell
cd frontend
npm install
npm run dev
```

Open `http://localhost:5173`. Create an account on the sign-in screen. The Vite server proxies `/api` to Spring on port 8080.

Run the checks from the repository root:

```powershell
Push-Location backend; .\mvnw.cmd test; Pop-Location
Push-Location frontend; npm run lint; npm run build; Pop-Location
```

## PostgreSQL Profile

Start the database with `docker compose up -d postgres`. Set `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET_BASE64`, `AES_ENCRYPTION_KEY_BASE64`, and `FRONTEND_ORIGIN` in the backend process environment. Both secrets must be independently generated random 32-byte values encoded as Base64; set `COOKIE_SECURE=true` behind HTTPS. Do not use the example local keys from `.env.example` in a shared or production environment. Flyway applies the schema at startup.

The PostgreSQL connection and secrets have no production defaults, so a missing setting fails startup instead of silently using a development credential. `application-local-h2.yml` contains disposable local-only settings and disables Flyway in favor of Hibernate's in-memory schema.

## API and Load Testing

See [docs/API-TESTING.md](docs/API-TESTING.md) for request bodies, pagination, role expectations, and JMeter instructions. Import [the Postman collection](docs/postman/Postroom.postman_collection.json) to exercise the endpoints. A regular registered account receives `ROLE_USER`; provision `ROLE_ADMIN` out of band with restricted database access before calling `/api/admin/overview`.
