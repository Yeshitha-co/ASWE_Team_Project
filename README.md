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

`mvn clean test` compiles the project and runs the unit tests. `mvn spring-boot:run` starts the application. Spring Boot’s default port is 8080. The application starts once Module A's `PolicyLookup` and Module E's `ClientContext` are implemented; until then `mvn test` replaces them with mocks.

## Tests and coverage

```bash
mvn test      # unit and API tests on in-memory H2
mvn verify    # tests, then the JaCoCo coverage report and the coverage check
```

`mvn verify` writes the coverage report to `target/site/jacoco/index.html` and fails the build if branch coverage across the project is below 55%. The report for the tagged release is committed with that release.

`AssetPersistenceRestartTest` checks that data survives an application restart on the `postgres` profile. It runs only when `DB_URL` is set, so CI without PostgreSQL skips it. To run it, start PostgreSQL as described in [Run with PostgreSQL](#run-with-postgresql), set `DB_URL`, `DB_USER` and `DB_PASSWORD`, then:

```bash
mvn test -Dtest=AssetPersistenceRestartTest
```

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
- B6: shared persistence setup: JPA, an in-memory H2 `test` profile (the default), a PostgreSQL `postgres` profile, and an optional Docker Compose file for local PostgreSQL (see [Database](#database)).
- B1 through B5: asset entity and client-scoped repository, `POST /api/assets` and `GET /api/assets/{assetId}/versions/{version}` with validation (`VALIDATION_ERROR`, `INVALID_ASSET_TYPE`, `DUPLICATE_ASSET_VERSION`, `UNKNOWN_POLICY_REFERENCE`, `ASSET_NOT_FOUND`), and the `AssetLookup` implementation, with unit tests.
- B7: `DerivationConflictChecker`, which reports upstream `derivatives` and `aiTraining` conflicts for new derivations, with unit tests.
- B8: JaCoCo coverage report and 55% branch-coverage check (see [Tests and coverage](#tests-and-coverage)).

Still in progress:

- Module A, C, and D implementations
- B6 follow-up: freeze the schema into a Flyway migration once every module's entities are on `main`
- B9: assemble the README sections from every module (due Oct 23)
- B10: REST Assured API tests for the asset endpoints, once the shared harness (E6) is available
- E2 through E8: client registration, API-key authentication, client isolation, shared error mapping, the REST Assured harness, GitHub release setup, and Module E tests

There is no security filter or shared error handler yet. Until they land, the application does not start outside tests, and asset errors are thrown as the shared exceptions but not yet turned into the error format.

## Planned API

These routes are specified for T2. The asset routes (Module B) are implemented, see [Module B: Assets](#module-b-assets); the rest are planned. Every route will require an `X-API-Key` header except client registration.

| Method | Route | Module | Planned success |
| --- | --- | --- | --- |
| POST | `/api/clients` | E | 201, API key returned once |
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

Each module documents its endpoints below: inputs, outputs, side effects, status and error codes, call order, and correct and bad-usage examples. Sections for Modules A, C, D and E are added as their owners hand them over (due Oct 23).

The usual call order for a client is: register (`POST /api/clients`), register policies, register assets that point to those policies, record derivations between assets, then check intended use.

### Module B: Assets

Stores immutable, versioned assets (datasets, models, code and derived datasets). Each asset version is linked to one exact policy version and owned by the client that registered it. Module B also checks new derivations for upstream policy conflicts, which Module C reports in its derivation response.

#### `POST /api/assets`

Registers a new asset version for the caller.

**Call order:** register the policy version first (`POST /api/policies`). Derivations (`POST /api/derivations`) can only reference assets registered here.

**Request body**

| Field | Required | Rules |
| --- | --- | --- |
| `assetId` | yes | 1 to 255 characters: letters, digits, `.`, `_`, `-` |
| `version` | yes | positive integer |
| `type` | yes | `dataset`, `model`, `code` or `derived-dataset` |
| `policyId` | yes | same rules as `assetId`; must name a policy registered by the caller |
| `policyVersion` | yes | positive integer; that exact policy version must exist for the caller |
| `metadata` | no | free text, at most 2000 characters, for example a name or source reference |

The owning client always comes from the API key. A `clientId` in the body is ignored.

**Side effects:** stores one new asset version. Nothing is ever updated: assets cannot be edited or deleted, and linking an asset to a different policy means registering a new asset version. Earlier versions stay unchanged.

**Success:** `201 Created`, with a `Location` header pointing at the new asset and the stored asset as the body.

```http
POST /api/assets
X-API-Key: <your key>
Content-Type: application/json

{ "assetId": "dataset-A", "version": 1, "type": "dataset", "policyId": "license-A", "policyVersion": 1 }
```

```http
HTTP/1.1 201 Created
Location: /api/assets/dataset-A/versions/1

{
  "assetId": "dataset-A",
  "version": 1,
  "name": "dataset-A:v1",
  "type": "dataset",
  "policyId": "license-A",
  "policyVersion": 1,
  "policy": "license-A:v1",
  "createdAt": "2026-10-22T15:04:05.123456Z"
}
```

`metadata` appears in the response only when it was supplied.

**Errors,** checked in this order:

| Status | Code | When |
| --- | --- | --- |
| 400 | `VALIDATION_ERROR` | a required field is missing, an id has characters outside the allowed set, a version is not a positive integer, or `metadata` is too long; `details.field` names the field |
| 400 | `INVALID_ASSET_TYPE` | `type` is not one of the four types |
| 409 | `DUPLICATE_ASSET_VERSION` | the caller already registered this `assetId` and `version` |
| 422 | `UNKNOWN_POLICY_REFERENCE` | the caller has no policy with this `policyId` and `policyVersion` |

Another client may register an asset with the same id and version; each client only sees its own.

**Bad usage:** registering the same version twice.

```json
{
  "timestamp": "2026-10-22T15:04:06Z",
  "status": 409,
  "code": "DUPLICATE_ASSET_VERSION",
  "message": "Asset version already exists: dataset-A:v1",
  "path": "/api/assets",
  "details": {}
}
```

**Bad usage:** pointing at a policy version that does not exist (`"policyVersion": 9`).

```json
{
  "timestamp": "2026-10-22T15:04:07Z",
  "status": 422,
  "code": "UNKNOWN_POLICY_REFERENCE",
  "message": "Unknown policy reference: license-A:v9",
  "path": "/api/assets",
  "details": {}
}
```

#### `GET /api/assets/{assetId}/versions/{version}`

Reads one asset version owned by the caller.

**Side effects:** none.

**Success:** `200 OK` with the same body as registration.

```http
GET /api/assets/dataset-A/versions/1
X-API-Key: <your key>
```

**Errors**

| Status | Code | When |
| --- | --- | --- |
| 404 | `ASSET_NOT_FOUND` | the version does not exist, belongs to another client, or is not a positive integer |

Another client's asset returns 404, never 403, so clients cannot tell whether someone else's asset exists.

**Bad usage:** reading another client's asset, or a version that was never registered.

```json
{
  "timestamp": "2026-10-22T15:04:08Z",
  "status": 404,
  "code": "ASSET_NOT_FOUND",
  "message": "Asset not found: dataset-A:v2",
  "path": "/api/assets/dataset-A/versions/2",
  "details": {}
}
```

#### Conflict checks on new derivations

This is not an endpoint. When a derivation is recorded (`POST /api/derivations`, Module C), Module C passes each input's upstream graph (the input and all of its ancestors) to Module B's `DerivationConflictChecker`. For every asset in those graphs, the checker reads the asset's exact policy version and reports:

| Field | Reported when |
| --- | --- |
| `derivatives` | the policy has `derivatives=false`, for any relation |
| `aiTraining` | the policy has `aiTraining=false` and the input's relation is `trained-on` |

Each conflict names the asset, the policy version and the field. An asset reached through several inputs is reported once per field, and conflicts are sorted by asset and field. Conflicts are reported and stored with the derivation, but they never reject it: only structural problems such as cycles do.

Example: if `license-A:v1` had `derivatives=false`, recording Model-B as trained on Dataset-A stores the edge and returns one conflict naming `dataset-A:v1`, `license-A:v1` and `derivatives`. The same conflict is found further down the chain too: deriving Model-D from Model-C reports it again, because Dataset-A is among Model-C's ancestors.

#### Interfaces for other modules

| Interface | Used by | Purpose |
| --- | --- | --- |
| `AssetLookup.findAsset(clientId, ref)` | C, D | the asset's type and exact policy version, or empty if missing or another client's |
| `AssetLookup.findAssetsByPolicy(clientId, policy)` | Iteration 2 impact analysis | every asset version of the caller linked to an exact policy version |
| `DerivationConflictChecker.check(clientId, inputs, upstreamByInput)` | C | upstream policy conflicts for a new derivation |

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

Status codes used across the service:

| Status | Codes |
| --- | --- |
| 400 | `VALIDATION_ERROR`, `INVALID_INTENDED_USE`, `INVALID_POLICY_COMBINATION`, `INVALID_ASSET_TYPE` |
| 401 | `MISSING_API_KEY`, `INVALID_API_KEY` |
| 404 | `ASSET_NOT_FOUND`, `POLICY_NOT_FOUND`, `DECISION_NOT_FOUND` |
| 409 | `DUPLICATE_POLICY_VERSION`, `DUPLICATE_ASSET_VERSION`, `DUPLICATE_EDGE`, `CYCLE_DETECTED` |
| 422 | `UNKNOWN_POLICY_REFERENCE`, `UNKNOWN_INPUT_ASSET` |

## Third-party code

LicenseGraph does not copy any third-party source code into the repository. It uses the following libraries, which Maven downloads from Maven Central as declared in `pom.xml`:

| Library | Used for | License |
| --- | --- | --- |
| Spring Boot 3.4.5 (web, validation, data JPA, test starters) | Web server, REST controllers, dependency injection, configuration, test support | Apache 2.0 |
| Spring Data JPA and Hibernate ORM 6.6.13 | Mapping entities to database tables and generating queries | Apache 2.0 (Spring Data), LGPL 2.1 (Hibernate) |
| Jackson 2.18 | JSON serialization of requests and responses | Apache 2.0 |
| H2 Database 2.3.232 | In-memory database for the `test` profile | MPL 2.0 or EPL 1.0 |
| PostgreSQL JDBC Driver 42.7.5 | Connecting to PostgreSQL in the `postgres` profile | BSD 2-Clause |
| Flyway 10.20 | Schema migrations (switched on once the schema is frozen) | Apache 2.0 |
| JUnit 5.11 | Unit and integration tests | EPL 2.0 |
| Mockito 5.14 | Test doubles for other modules' interfaces | MIT |
| AssertJ 3.26 | Test assertions | Apache 2.0 |
| JaCoCo 0.8.15 | Test coverage report and branch-coverage check | EPL 2.0 |

Libraries added by other modules (for example REST Assured for API tests and Checkstyle for style checks) are listed here as they are merged.

Code written with AI assistance is marked in comments in the source files, as required by the team's AI policy.
