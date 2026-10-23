# LicenseGraph

LicenseGraph records versioned license policies and assets, stores how assets were derived from one another, and decides whether an intended use is allowed. A decision considers the asset and its ancestors. If any policy in that lineage blocks the use, the result is deny, with the blocking path and any obligations.

This repository is the first iteration (T2): register a policy, register an asset, record a derivation, and check intended use. Impact analysis and the lineage compliance report are planned for a later iteration.

## Tech stack

- Java 17
- Spring Boot 3.4.5
- Maven

## Modules

| Module | Responsibility |
| --- | --- |
| A. Policy Registry and Use Rules | Store versioned license policies and evaluate what one policy allows, including obligations. |
| B. Asset Registry, Persistence and Conflict Checks | Store immutable asset versions linked to a policy version, own database setup, and report policy conflicts on new derivations. |
| C. Lineage Graph Engine | Record derivation edges, reject cycles, and answer ancestor and descendant queries. |
| D. Use Decisions and Audit Log | Combine an asset and its ancestors into allow or deny, explain blocking paths, and keep an insert-only decision history. |
| E. Platform and Security | Provide the shared skeleton and contracts, identify callers by API key, and keep each client limited to its own records. |

Modules call each other only through interfaces. The call direction is D to C to B to A.

## Package structure

```
com.licensegraph
├── common          shared records, enums, and exceptions
├── policy          Module A contracts
├── asset           Module B contracts
├── lineage         Module C contracts
├── decision        Module D contracts
├── security        Module E contracts
└── config          application configuration
```

Shared names print as `model-C:v1` and `license-A:v1`. JSON fields are camelCase. Enum values are lowercase kebab-case, such as `ai-training` and `fine-tuned-from`. `AssetRef` and `PolicyRef` serialize as objects (`assetId` / `policyId` plus `version`).

## Prerequisites

- JDK 17
- Maven 3.9 or later

Confirm the compiler before building:

```bash
java -version
mvn -version
```

Both should report Java 17. If another JDK is first on `PATH`, point `JAVA_HOME` at a JDK 17 and put it ahead of other Java installs:

```bash
export JAVA_HOME=/path/to/jdk-17
export PATH="$JAVA_HOME/bin:$PATH"
```

On this machine, Homebrew’s JDK 17 is:

```bash
export JAVA_HOME="$(brew --prefix openjdk@17)/libexec/openjdk.jdk/Contents/Home"
export PATH="$JAVA_HOME/bin:$PATH"
```

## Build and run

From the project root:

```bash
mvn clean test
mvn spring-boot:run
```

`mvn clean test` compiles the project and runs the unit tests. `mvn spring-boot:run` starts the application. Spring Boot’s default port is 8080. The only implemented route is `POST /api/clients`. Every other path requires an `X-API-Key` header.

## Database

The service runs on one of two databases, chosen by Spring profile:

| Profile | Database | Used for | Data after restart |
| --- | --- | --- | --- |
| `test` (default) | H2, in memory | Unit tests, API tests, CI, quick local runs | Lost |
| `postgres` | PostgreSQL | Demo and deployment | Kept |

If no profile is given, the `test` profile is used, so `mvn test` and CI need no database setup.

Configuration lives in `src/main/resources`:

- `application.yml`: settings shared by both profiles
- `application-test.yml`: H2 connection
- `application-postgres.yml`: PostgreSQL connection, read from the `DB_URL`, `DB_USER` and `DB_PASSWORD` environment variables. Credentials are never committed.

### Run with PostgreSQL

Only needed to run against a persistent database. Tests and everyday development use H2 and need no setup.

1. Install PostgreSQL, using the [installer](https://www.postgresql.org/download/) or `brew install postgresql@17`, and make sure the server is running. With the installer, the command-line tools are in `/Library/PostgreSQL/17/bin` on macOS. Add that folder to `PATH` if `psql` is not found.

2. Create a user and a database for LicenseGraph. Connect as the PostgreSQL superuser: `postgres` for the installer, or your macOS username for Homebrew.

   ```bash
   psql -h localhost -U postgres -c "CREATE USER licensegraph WITH PASSWORD 'choose-a-password';"
   psql -h localhost -U postgres -c "CREATE DATABASE licensegraph OWNER licensegraph;"
   ```

   **With Docker instead of steps 1 and 2:** if you have [Docker Desktop](https://www.docker.com/products/docker-desktop/), the included `docker-compose.yml` starts PostgreSQL with the user and database already created. Data is kept in a Docker volume between restarts.

   ```bash
   export DB_USER=licensegraph
   export DB_PASSWORD=choose-a-password
   docker compose up -d
   ```

   Stop it with `docker compose down`. `docker compose down -v` also deletes the data.

3. Start the application with the `postgres` profile:

   ```bash
   export DB_URL=jdbc:postgresql://localhost:5432/licensegraph
   export DB_USER=licensegraph
   export DB_PASSWORD=choose-a-password
   mvn spring-boot:run -Dspring-boot.run.profiles=postgres
   ```

4. Confirm the tables were created:

   ```bash
   psql -h localhost -U licensegraph -d licensegraph -c '\dt'
   ```

   With Docker: `docker compose exec postgres psql -U licensegraph -d licensegraph -c '\dt'`.

To start over with an empty database, drop and recreate it (with Docker, run `docker compose down -v` and then `docker compose up -d`):

```bash
psql -h localhost -U postgres -c "DROP DATABASE licensegraph;"
psql -h localhost -U postgres -c "CREATE DATABASE licensegraph OWNER licensegraph;"
```

### Adding tables (all modules)

Until integration day (Oct 22), Hibernate builds the tables from the `@Entity` classes, so a module only writes its entity and repository, with no SQL:

- Put the entity anywhere under `com.licensegraph` and Spring finds it automatically.
- Store the owning client in a `clientId` column and include it in every query, so another client's records return 404.
- H2 uses `ddl-auto: create-drop` (clean tables on every run). PostgreSQL uses `ddl-auto: update` (adds missing tables and columns but never renames or removes them). After renaming or removing a field, reset your local database as shown above.

On Oct 22 the schema is frozen: the SQL Hibernate generates is committed as a Flyway migration, and both profiles switch to Flyway with `ddl-auto: validate`. After that, every schema change is a new numbered migration file (for example `V2__add_column.sql`) in the same PR as the entity change. Flyway applies it on every database at startup.

## Current status

Done:

- E1: Maven Spring Boot skeleton, package layout, application config, and the shared contracts other modules code against.
- E2: `POST /api/clients` creates an API key from a secure random source, stores only its SHA-256 hash, and returns the raw key once.
- E3: `X-API-Key` filter. A missing key is `401 MISSING_API_KEY`. An unknown key is `401 INVALID_API_KEY`. `ClientContext` exposes the caller. Request bodies are not trusted for `clientId`.
- E5: `GlobalExceptionHandler` maps shared exceptions to the error body below. Unexpected failures return `500` with code `INTERNAL_ERROR` and a fixed message. That code is not one of the Section 10 business codes.
- E6: `ApiTestBase` starts the application, registers a client, and attaches `X-API-Key` for REST Assured API tests.
- E8: unit and API tests for client registration, hashed keys, missing and invalid keys, caller identity, and error mapping.
- B6: shared persistence setup: JPA, an in-memory H2 `test` profile (the default), a PostgreSQL `postgres` profile, and an optional Docker Compose file for local PostgreSQL (see [Database](#database)).

Still in progress:

- Module A, C, and D implementations, on their own branches and not merged here
- B1 through B5 and B7 through B10, on the Module B branch and not merged here
- E4 cross-client 404 for policies, assets, edges, and decisions, once those endpoints are merged. Their lookups already take `clientId` on their own branches.
- E7: branch protection, issue templates, and the Projects board are not set up on the repository. The release tag is not created yet.

## Client registration

`POST /api/clients` needs no API key and no body.

```bash
curl -i -X POST http://localhost:8080/api/clients
```

`201 Created`:

```json
{ "clientId": "generated-uuid", "apiKey": "shown-once" }
```

The database stores `clientId` and the SHA-256 hex digest of `apiKey`. A later request cannot read the raw key back. Send the key on later calls as `X-API-Key`. A request to any other path without that header returns `401` with `code` `MISSING_API_KEY`. A wrong key returns `401` with `code` `INVALID_API_KEY`. A `clientId` field in the registration body is ignored.

## Planned API

`POST /api/clients` is implemented. The other routes are specified for T2 and are **not implemented** on this branch. Every route except client registration requires an `X-API-Key` header.

| Method | Route | Module | Success |
| --- | --- | --- | --- |
| POST | `/api/clients` | E | 201, API key returned once (implemented) |
| POST | `/api/policies` | A | 201 |
| GET | `/api/policies/{policyId}/versions` | A | 200 |
| GET | `/api/policies/{policyId}/versions/{version}` | A | 200 |
| POST | `/api/assets` | B | 201 |
| GET | `/api/assets/{assetId}/versions/{version}` | B | 200 |
| POST | `/api/derivations` | C | 201, including any conflicts |
| GET | `/api/assets/{assetId}/versions/{version}/ancestors` | C | 200 |
| GET | `/api/assets/{assetId}/versions/{version}/descendants` | C | 200 |
| POST | `/api/use-decisions` | D | 201, including a decision id |
| GET | `/api/use-decisions/{decisionId}` | D | 200 |
| GET | `/api/use-decisions?assetId=&version=` | D | 200 |

Policies, assets, edges, and decisions are insert-only. Another client’s record is planned to return 404, not 403.

## API documentation

Request and response examples, per-endpoint error behavior, and the assembled endpoint guide will be added here as each module is implemented.

Planned error body:

```json
{
  "timestamp": "2026-10-21T14:03:11Z",
  "status": 409,
  "code": "CYCLE_DETECTED",
  "message": "Edge dataset-A:v1 -> model-C:v1 would create a cycle",
  "path": "/api/derivations",
  "details": {}
}
```

`401 MISSING_API_KEY` and `401 INVALID_API_KEY` are returned by the API-key filter. The other codes are mapped by `GlobalExceptionHandler` when a module throws the matching shared exception. Per-endpoint examples for Modules A–D will be added as those routes are merged:

| Status | Codes |
| --- | --- |
| 400 | `VALIDATION_ERROR`, `INVALID_INTENDED_USE`, `INVALID_POLICY_COMBINATION`, `INVALID_ASSET_TYPE` |
| 401 | `MISSING_API_KEY`, `INVALID_API_KEY` |
| 404 | `ASSET_NOT_FOUND`, `POLICY_NOT_FOUND`, `DECISION_NOT_FOUND` |
| 409 | `DUPLICATE_POLICY_VERSION`, `DUPLICATE_ASSET_VERSION`, `DUPLICATE_EDGE`, `CYCLE_DETECTED` |
| 422 | `UNKNOWN_POLICY_REFERENCE`, `UNKNOWN_INPUT_ASSET` |
