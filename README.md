# BabaAI Core

Java 25 + Spring Boot service — authentication, pantry, recipes, notifications, and PostgreSQL.

## Prerequisites

- Docker, or Java 25 + Gradle 9.1+ for local dev

## Quick start (Docker)

Includes PostgreSQL.

```bash
cp .env.example .env
docker compose up --build
```

Core API: http://localhost:8081  
JWKS: http://localhost:8081/.well-known/jwks.json

On first boot, Flyway migrates the schema and seeds categories + sample recipes.

## Local dev (without Docker)

```bash
cp .env.example .env
# Set JWT_KEY_PATH=./keys and DATABASE_URL=jdbc:postgresql://localhost:5432/babaai for local Postgres
# Export variables from .env into your shell, then:
./gradlew bootRun
```

Spring Boot does not load `.env` automatically — use Docker, or export the variables before `bootRun`.

## Full stack startup order

1. **babaai-core** (this repo) — start first
2. **babaai-ai**
3. **babaai-gateway**
4. **babaai-frontend**

Set the same `AI_SERVICE_TOKEN` here and in **babaai-ai** `.env`.

## Environment

Copy `.env.example` to `.env`. **All listed variables are required** — core fails at startup if any are missing (except optional JSON path overrides).

| Variable | Description |
|----------|-------------|
| `POSTGRES_*`, `DATABASE_URL` | Database connection |
| `CORE_PORT` | Host port published by Docker (`host:8081` mapping) |
| `JWT_EXPIRE_MINUTES`, `JWT_KEY_PATH` | JWT signing (`/app/keys` in Docker, `./keys` locally) |
| `AI_SERVICE_TOKEN` | Shared secret for core ↔ ai calls |
| `AI_SERVICE_URL` | AI service URL |
| `CORS_ORIGINS` | Browser origins allowed by core |
| `DEFAULT_*` | App defaults exposed via `/api/v1/config/public` |

## Config data

Seed and reference JSON lives in `src/main/resources/config/` (overridable via `*_CONFIG_PATH` env vars):

- `sample_recipes.json` — catalog seed
- `ingredient_categories.json` — pantry categories
- `recipe_categories.json` — recipe tags

## Tests

Integration tests use PostgreSQL via Testcontainers (Docker required locally).

```bash
./gradlew test
```

**Windows + Docker Desktop:** if tests fail with `Could not find a valid Docker environment`, ensure Docker Desktop is running. The Gradle `test` task sets `DOCKER_HOST` to the `dockerDesktopLinuxEngine` pipe automatically. If you run tests from the IDE only, set environment variable `DOCKER_HOST=npipe:////./pipe/dockerDesktopLinuxEngine` in your run configuration, or remove `docker.client.strategy` from `%USERPROFILE%\.testcontainers.properties` if that file forces the wrong pipe.

### CI checks

On every push to `main` and on pull requests, GitHub Actions runs:

- `./gradlew bootJar test` — compile, unit tests, and Testcontainers integration tests for auth, ingredients, and recipes APIs
