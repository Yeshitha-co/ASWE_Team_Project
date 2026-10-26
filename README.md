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

`mvn clean test` compiles the project and runs the unit tests. `mvn spring-boot:run` starts the application. Spring Boot’s default port is 8080.

## Tests and coverage

```bash
mvn test      # unit and API tests on in-memory H2
mvn verify    # tests, then the JaCoCo coverage report and the coverage check
```

`mvn verify` writes the coverage report to `target/site/jacoco/index.html` and fails the build if branch coverage across the project is below 55%. The report for the tagged release is committed with that release.

API tests use REST Assured on top of Module E's `ApiTestBase`, which starts the application on a random port and registers a client with its own API key; for example, `AssetApiTest` covers every asset endpoint and error code over HTTP.

`AssetPersistenceRestartTest` checks that data survives an application restart on the `postgres` profile. It runs only when `DB_URL` is set, so CI without PostgreSQL skips it. To run it, start PostgreSQL as described in [Run with PostgreSQL](#run-with-postgresql), set `DB_URL`, `DB_USER` and `DB_PASSWORD`, then:

```bash
mvn test -Dtest=AssetPersistenceRestartTest
```

## Style checks

Checkstyle runs through the Maven Checkstyle Plugin 3.6.0 with the ruleset in `config/checkstyle/checkstyle.xml`: Google Java Style, adapted to this project's 4-space indentation, with a 100-character line limit. It checks production and test code. Violations are reported as warnings and do not fail the build.

```bash
mvn checkstyle:check        # print violations to the console
mvn checkstyle:checkstyle   # HTML report in target/reports/checkstyle.html, XML in target/checkstyle-result.xml
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

### Schema

The schema is in [`docs/schema.sql`](docs/schema.sql): the six tables, one per module (`license_policies`, `assets`, `derivation_edge` and `derivation_edge_conflict`, `decision_audit`, `api_clients`), with their keys and unique constraints. It is generated from the entities and documents the database; the application does not run it. It can also create an empty database by hand:

```bash
psql -h localhost -U licensegraph -d licensegraph -f docs/schema.sql
```

Versioned Flyway migrations are planned for Iteration 2. Flyway is already a dependency but is switched off.

### Adding tables (all modules)

Hibernate builds the tables from the `@Entity` classes at startup, so a module only writes its entity and repository, with no SQL:

- Put the entity anywhere under `com.licensegraph` and Spring finds it automatically.
- Store the owning client in a `clientId` column and include it in every query, so another client's records return 404.
- H2 uses `ddl-auto: create-drop` (clean tables on every run). PostgreSQL uses `ddl-auto: update` (adds missing tables and columns but never renames or removes them). After renaming or removing a field, reset your local database as shown above.

After changing an entity, regenerate `docs/schema.sql` in the same pull request so it stays accurate.

## Current status

Done:

- E1: Maven Spring Boot skeleton, package layout, application config, and the shared contracts other modules code against.
- E2: `POST /api/clients` creates an API key from a secure random source, stores only its SHA-256 hash, and returns the raw key once.
- E3: `X-API-Key` filter. A missing key is `401 MISSING_API_KEY`. An unknown key is `401 INVALID_API_KEY`. `ClientContext` exposes the caller. Request bodies are not trusted for `clientId`.
- E5: `GlobalExceptionHandler` maps shared exceptions to the error body below. Unexpected failures return `500` with code `INTERNAL_ERROR` and a fixed message. That code is not one of the Section 10 business codes.
- E6: `ApiTestBase` starts the application, registers a client, and attaches `X-API-Key` for REST Assured API tests.
- E8: unit and API tests for client registration, hashed keys, missing and invalid keys, caller identity, and error mapping.
- B6: shared persistence setup: JPA, an in-memory H2 `test` profile (the default), a PostgreSQL `postgres` profile, and an optional Docker Compose file for local PostgreSQL (see [Database](#database)).
- B1 through B5: asset entity and client-scoped repository, `POST /api/assets` and `GET /api/assets/{assetId}/versions/{version}` with validation (`VALIDATION_ERROR`, `INVALID_ASSET_TYPE`, `DUPLICATE_ASSET_VERSION`, `UNKNOWN_POLICY_REFERENCE`, `ASSET_NOT_FOUND`), and the `AssetLookup` implementation, with unit tests.
- B7: `DerivationConflictChecker`, which reports upstream `derivatives` and `aiTraining` conflicts for new derivations, with unit tests.
- B8: JaCoCo coverage report and 55% branch-coverage check (see [Tests and coverage](#tests-and-coverage)).
- B10: REST Assured API tests for the asset endpoints (`AssetApiTest`): a success case, every error code, cross-client 404, and upstream conflicts reported through `POST /api/derivations`.
- B9: README assembled from every module's section, with the third-party code list.
- Schema documented in [`docs/schema.sql`](docs/schema.sql).
- Module A: versioned policy registry, intended-use rules and obligations (see [Module A: Policies](#module-a-policies)).
- Module C: derivation recording with cycle and duplicate checks, and ancestor and descendant queries (see [Module C: Lineage](#module-c-lineage)).
- Module D: intended-use decisions with blocking paths and an insert-only decision history (see [Module D: Use decisions](#module-d-use-decisions)).
- Cross-client access returns 404 for policies, assets, lineage queries and decisions.

Still in progress:

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

## API routes

All T2 routes are implemented. Every route requires an `X-API-Key` header except client registration.

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

Each module documents its endpoints below: inputs, outputs, side effects, status and error codes, call order, and correct and bad-usage examples. Client registration (Module E) is described in [Client registration](#client-registration).

The usual call order for a client is: register (`POST /api/clients`), register policies, register assets that point to those policies, record derivations between assets, then check intended use.

### Module A: Policies

Stores versioned license policies and decides what one policy allows. Every policy belongs to the client that registered it, and the pair (`policyId`, `version`) is unique per client: a new version never overwrites an earlier one, so decisions stay tied to the terms in force when they were made. Other modules read policies only through `PolicyLookup` and `PolicyRules`, never the database.

#### `POST /api/policies`

Registers a new policy version for the caller.

**Call order:** first, before any asset that uses the policy. Assets name an exact policy version (`POST /api/assets`).

**Request body.** Every field is required.

| Field | Rules |
| --- | --- |
| `policyId` | letters, digits, `.`, `_`, `-` |
| `version` | integer, at least 1 |
| `commercialUse` | `true` or `false`: may the asset be used commercially |
| `redistribution` | `true` or `false`: may the asset be redistributed |
| `derivatives` | `true` or `false`: may derivative works be made |
| `aiTraining` | `true` or `false`: may the asset be used as AI training data |
| `attributionRequired` | `true` or `false`: does use require attribution |
| `shareAlike` | `true` or `false`: must derivatives carry the same terms; needs `derivatives: true` |

The owning client comes from the API key.

**Side effects:** stores one new policy version. Policies cannot be edited or deleted.

**Success:** `201 Created` with the stored policy.

```http
POST /api/policies
X-API-Key: <your key>
Content-Type: application/json

{ "policyId": "license-A", "version": 1, "commercialUse": false, "redistribution": true,
  "derivatives": true, "aiTraining": true, "attributionRequired": true, "shareAlike": false }
```

```json
{
  "policyId": "license-A",
  "version": 1,
  "commercialUse": false,
  "redistribution": true,
  "derivatives": true,
  "aiTraining": true,
  "attributionRequired": true,
  "shareAlike": false
}
```

**Errors**

| Status | Code | When |
| --- | --- | --- |
| 400 | `VALIDATION_ERROR` | a field is missing, `policyId` has characters outside the allowed set, or `version` is below 1; the message names each field |
| 400 | `INVALID_POLICY_COMBINATION` | `shareAlike` is `true` but `derivatives` is `false` |
| 409 | `DUPLICATE_POLICY_VERSION` | the caller already registered this `policyId` and `version` |

**Bad usage:** share-alike without derivatives.

```json
{
  "timestamp": "2026-10-26T16:27:58Z",
  "status": 400,
  "code": "INVALID_POLICY_COMBINATION",
  "message": "A policy requiring share-alike must allow derivatives",
  "path": "/api/policies"
}
```

**Bad usage:** registering the same version twice.

```json
{
  "timestamp": "2026-10-26T16:27:57Z",
  "status": 409,
  "code": "DUPLICATE_POLICY_VERSION",
  "message": "Policy version already exists: license-A:v1",
  "path": "/api/policies"
}
```

#### `GET /api/policies/{policyId}/versions`

Lists every version of one of the caller's policies, oldest first.

**Side effects:** none.

**Success:** `200 OK` with an array of policies in the same shape as registration.

```http
GET /api/policies/license-A/versions
X-API-Key: <your key>
```

**Errors**

| Status | Code | When |
| --- | --- | --- |
| 404 | `POLICY_NOT_FOUND` | the caller has no policy with this id (including another client's policy) |

#### `GET /api/policies/{policyId}/versions/{version}`

Reads one version of one of the caller's policies.

**Side effects:** none.

**Success:** `200 OK` with the policy.

**Errors**

| Status | Code | When |
| --- | --- | --- |
| 404 | `POLICY_NOT_FOUND` | the version does not exist or belongs to another client |

**Bad usage:** reading a version that was never registered.

```json
{
  "timestamp": "2026-10-26T16:27:58Z",
  "status": 404,
  "code": "POLICY_NOT_FOUND",
  "message": "Policy not found: license-A:v9",
  "path": "/api/policies/license-A/versions/9"
}
```

#### Use rules and obligations

`PolicyRules` decides what one policy allows. Module D applies it to the requested asset and every ancestor. Each intended use maps to one policy field:

| Intended use | Field checked |
| --- | --- |
| `commercial` | `commercialUse` |
| `redistribution` | `redistribution` |
| `derivative` | `derivatives` |
| `ai-training` | `aiTraining` |
| `internal` | none, always allowed |

A check returns whether the use is allowed and, when it is not, the blocking field (for example `commercialUse`).

Policies also add obligations, which never change ALLOW or DENY:

- **attribution**, when `attributionRequired` is `true`
- **share-alike**, when `shareAlike` is `true`

Each obligation names its type, the asset it comes from and that asset's policy version. When obligations from several assets are merged, each type is kept once per source asset, sorted by asset and then type.

#### Interfaces for other modules

| Interface | Used by | Purpose |
| --- | --- | --- |
| `PolicyLookup.findPolicy(clientId, ref)` | B, D | the policy version's fields, or empty if it is missing or another client's |
| `PolicyRules.check(policy, use)` | D | allowed or blocked for one intended use, with the blocking field |
| `PolicyRules.obligations(source, policy)` | D | the obligations one policy adds for one asset |
| `PolicyRules.mergeObligations(all)` | D | obligations from many assets, without duplicates and in a fixed order |

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
  "path": "/api/assets"
}
```

**Bad usage:** pointing at a policy version that does not exist (`"policyVersion": 9`).

```json
{
  "timestamp": "2026-10-22T15:04:07Z",
  "status": 422,
  "code": "UNKNOWN_POLICY_REFERENCE",
  "message": "Unknown policy reference: license-A:v9",
  "path": "/api/assets"
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
  "path": "/api/assets/dataset-A/versions/2"
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

### Module C: Lineage

Records which asset versions were derived from which, keeps the graph free of cycles, and answers ancestry and descendant questions. Edges are stored from input to output and belong to the client that recorded them.

#### `POST /api/derivations`

Records that one output asset was produced from one or more input assets.

**Call order:** register every asset involved first (`POST /api/assets`).

**Request body**

| Field | Required | Rules |
| --- | --- | --- |
| `output` | yes | `{ "assetId": ..., "version": ... }`, a positive version |
| `inputs` | yes | a non-empty list of `{ "assetId", "version", "relation" }` |
| `inputs[].relation` | yes | `trained-on`, `fine-tuned-from`, `merged-from`, `filtered-from` or `derived-from` |

**What it does,** in this order:

1. Checks that the output and every input exist for the caller.
2. Rejects any input that would make an asset its own ancestor (including an asset derived from itself).
3. Rejects an edge that is already stored or repeated in the same request.
4. Stores every edge, then asks Module B's conflict checker for upstream policy conflicts, stores them on the edges and returns them. Conflicts never reject the request.

Steps 1 to 3 and the insert run in one transaction under a per-client lock, so if any check fails nothing from the request is saved, and two simultaneous requests cannot form a cycle together.

**Success:** `201 Created` with the created edges, any conflicts, and how many ancestors the output now has.

```http
POST /api/derivations
X-API-Key: <your key>
Content-Type: application/json

{ "output": { "assetId": "model-B", "version": 1 },
  "inputs": [ { "assetId": "dataset-A", "version": 1, "relation": "trained-on" } ] }
```

```json
{
  "output": "model-B:v1",
  "created": [ { "input": "dataset-A:v1", "output": "model-B:v1", "relation": "trained-on" } ],
  "conflicts": [],
  "ancestorCount": 1
}
```

Each conflict looks like `{ "source": "dataset-A:v1", "policy": "license-A:v1", "field": "derivatives" }` (see [Conflict checks on new derivations](#conflict-checks-on-new-derivations)).

**Errors**

| Status | Code | When |
| --- | --- | --- |
| 400 | `VALIDATION_ERROR` | a field is missing, `inputs` is empty, a version is not positive, or `relation` is not one of the five values |
| 409 | `CYCLE_DETECTED` | an input already descends from the output; `details.cyclePath` lists the loop |
| 409 | `DUPLICATE_EDGE` | the same input, output and relation is already stored or repeated in the request |
| 422 | `UNKNOWN_INPUT_ASSET` | the output or an input does not exist for the caller |

**Bad usage:** with Model-B trained on Dataset-A and Model-C fine-tuned from Model-B, recording Dataset-A as derived from Model-C.

```json
{
  "timestamp": "2026-10-26T16:53:11Z",
  "status": 409,
  "code": "CYCLE_DETECTED",
  "message": "Edge model-B:v1 -> dataset-A:v1 would create a cycle",
  "path": "/api/derivations",
  "details": { "cyclePath": ["dataset-A:v1", "model-C:v1", "model-B:v1", "dataset-A:v1"] }
}
```

**Bad usage:** an input that was never registered.

```json
{
  "timestamp": "2026-10-26T16:42:53Z",
  "status": 422,
  "code": "UNKNOWN_INPUT_ASSET",
  "message": "Unknown asset in request: ghost:v1",
  "path": "/api/derivations"
}
```

#### `GET /api/assets/{assetId}/versions/{version}/ancestors` and `.../descendants`

Return everything an asset was built from (ancestors) or everything built from it (descendants), as a subgraph: the asset itself, every reachable asset, and the edges between them with their relations. In a diamond, a shared ancestor appears once.

**Side effects:** none.

**Success:** `200 OK`.

```http
GET /api/assets/model-C/versions/1/ancestors
X-API-Key: <your key>
```

```json
{
  "root": "model-C:v1",
  "nodes": ["dataset-A:v1", "model-B:v1", "model-C:v1"],
  "edges": [
    { "input": "model-B:v1", "output": "model-C:v1", "relation": "fine-tuned-from" },
    { "input": "dataset-A:v1", "output": "model-B:v1", "relation": "trained-on" }
  ]
}
```

**Errors**

| Status | Code | When |
| --- | --- | --- |
| 404 | `ASSET_NOT_FOUND` | the asset version does not exist, belongs to another client, or is not a positive integer |

#### Interfaces for other modules

| Interface | Used by | Purpose |
| --- | --- | --- |
| `LineageQueryService.findAncestors(clientId, target)` | D | the asset and all of its ancestors, with edges |
| `LineageQueryService.findDescendants(clientId, source)` | Iteration 2 impact analysis | the asset and everything derived from it |

### Module D: Use decisions

Decides whether an asset version may be used for a purpose by checking the policy of the asset and of every ancestor, explains each block with a lineage path, and keeps an insert-only history of decisions.

#### `POST /api/use-decisions`

Evaluates one intended use and records the decision.

**Call order:** after the asset, its ancestors and their policies are registered and the derivations are recorded.

**Request body**

| Field | Required | Rules |
| --- | --- | --- |
| `assetId` | yes | the asset to evaluate |
| `version` | yes | positive integer |
| `intendedUse` | yes | `commercial`, `redistribution`, `derivative`, `ai-training` or `internal` |

**What it does:** loads the asset and its ancestors (Module C), reads each one's exact policy version (Module A), and checks the intended use against every policy with Module A's rules. If any policy blocks the use, the decision is `DENY`, with one shortest path from the requested asset back to each blocking asset. Obligations from every policy are merged; they never change the decision. `internal` is always allowed.

**Side effects:** stores the decision, with the policy versions it was based on and a timestamp. Stored decisions are never changed.

**Success:** `201 Created`.

```http
POST /api/use-decisions
X-API-Key: <your key>
Content-Type: application/json

{ "assetId": "model-C", "version": 1, "intendedUse": "commercial" }
```

```json
{
  "decisionId": "4e28c554-4793-43d9-9c60-d6a2f2f88233",
  "evaluatedAt": "2026-10-26T16:53:11.901571Z",
  "result": {
    "target": "model-C:v1",
    "use": "commercial",
    "decision": "DENY",
    "blockingPaths": [
      { "path": ["model-C:v1", "model-B:v1", "dataset-A:v1"],
        "blockingPolicy": "license-A:v1", "blockingField": "commercialUse" }
    ],
    "evaluatedPolicies": ["license-A:v1", "license-B:v1", "license-C:v1"],
    "obligations": [ { "type": "attribution", "source": "dataset-A:v1", "policy": "license-A:v1" } ]
  }
}
```

Model-B and Model-C allow commercial use on their own; the decision is `DENY` because of Dataset-A two generations up. An allowed use returns `"decision": "ALLOW"` with an empty `blockingPaths`.

**Errors**

| Status | Code | When |
| --- | --- | --- |
| 400 | `VALIDATION_ERROR` | `assetId` or `intendedUse` is missing, or `version` is not positive |
| 400 | `INVALID_INTENDED_USE` | `intendedUse` is not one of the five values; `details.field` is `intendedUse` |
| 404 | `ASSET_NOT_FOUND` | the asset version does not exist for the caller |

**Bad usage:** an intended use that does not exist.

```json
{
  "timestamp": "2026-10-26T16:53:11Z",
  "status": 400,
  "code": "INVALID_INTENDED_USE",
  "message": "Unknown intended use: resell (expected commercial, redistribution, derivative, ai-training or internal)",
  "path": "/api/use-decisions",
  "details": { "field": "intendedUse" }
}
```

#### `GET /api/use-decisions/{decisionId}`

Reads one stored decision, in the same shape as the `POST` response.

**Side effects:** none. Policies are not re-evaluated.

**Errors**

| Status | Code | When |
| --- | --- | --- |
| 404 | `DECISION_NOT_FOUND` | no decision with this id, or it belongs to another client |

```json
{
  "timestamp": "2026-10-26T16:53:12Z",
  "status": 404,
  "code": "DECISION_NOT_FOUND",
  "message": "Decision not found: nope",
  "path": "/api/use-decisions/nope"
}
```

#### `GET /api/use-decisions?assetId=&version=`

Lists every stored decision for one of the caller's asset versions, as an array of decisions. An asset with no decisions returns an empty array.

**Side effects:** none.

**Errors**

| Status | Code | When |
| --- | --- | --- |
| 400 | `VALIDATION_ERROR` | `assetId` or `version` is missing, or `version` is not a positive integer; `details.field` names it |

#### Interfaces for other modules

| Interface | Used by | Purpose |
| --- | --- | --- |
| `UseDecisionService.evaluate(clientId, target, use)` | Iteration 2 impact analysis | the decision without saving it |
| `UseDecisionService.decideAndRecord(clientId, target, use)` | D's controller | the decision, saved to the history |
| `DecisionAuditStore.findById` / `findByTarget` | D's controller | stored decisions, scoped to the client |

Every error uses the same body. `details` is included only when there is something to add, such as the field that failed validation or the path of a cycle:

```json
{
  "timestamp": "2026-10-23T17:12:30Z",
  "status": 409,
  "code": "CYCLE_DETECTED",
  "message": "Edge model-B:v1 -> dataset-A:v1 would create a cycle",
  "path": "/api/derivations",
  "details": { "cyclePath": ["dataset-A:v1", "model-C:v1", "model-B:v1", "dataset-A:v1"] }
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

## Third-party code

LicenseGraph does not copy any third-party source code into the repository. It uses the following libraries, which Maven downloads from Maven Central as declared in `pom.xml`:

| Library | Used for | License |
| --- | --- | --- |
| Spring Boot 3.4.5 (web, validation, data JPA, test starters) | Web server, REST controllers, dependency injection, configuration, test support | Apache 2.0 |
| Spring Data JPA and Hibernate ORM 6.6.13 | Mapping entities to database tables and generating queries | Apache 2.0 (Spring Data), LGPL 2.1 (Hibernate) |
| Jackson 2.18 | JSON serialization of requests and responses | Apache 2.0 |
| H2 Database 2.3.232 | In-memory database for the `test` profile | MPL 2.0 or EPL 1.0 |
| PostgreSQL JDBC Driver 42.7.5 | Connecting to PostgreSQL in the `postgres` profile | BSD 2-Clause |
| Flyway 10.20 | Schema migrations, planned for Iteration 2 (dependency present, switched off) | Apache 2.0 |
| JUnit 5.11 | Unit and integration tests | EPL 2.0 |
| Mockito 5.14 | Test doubles for other modules' interfaces | MIT |
| AssertJ 3.26 | Test assertions | Apache 2.0 |
| JaCoCo 0.8.15 | Test coverage report and branch-coverage check | EPL 2.0 |
| Spring Security 6.4.5 | API-key authentication filter | Apache 2.0 |
| REST Assured 5.5.1 | API tests over HTTP | Apache 2.0 |
| Checkstyle 9.3 (Maven Checkstyle Plugin 3.6.0) | Style checks | LGPL 2.1 |

Code written with AI assistance is marked with an `AI-assisted:` comment in the source files, as described in [`AI_Documentation.md`](AI_Documentation.md).
