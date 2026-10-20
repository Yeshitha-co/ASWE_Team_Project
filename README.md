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

`mvn clean test` compiles the project and runs the unit tests. `mvn spring-boot:run` starts the application. Spring Boot’s default port is 8080. No API routes are registered yet.

## Current status

Done:

- E1: Maven Spring Boot skeleton, package layout, application config, and the shared contracts other modules code against.

Still in progress:

- Module A, B, C, and D implementations
- E2 through E8: client registration, API-key authentication, client isolation, shared error mapping, the REST Assured harness, GitHub release setup, and Module E tests

There is no database profile, security filter, or controller yet.

## Planned API

These routes are specified for T2 and are **not implemented**. Every route will require an `X-API-Key` header except client registration.

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

Planned status codes, not yet returned by the service:

| Status | Codes |
| --- | --- |
| 400 | `VALIDATION_ERROR`, `INVALID_INTENDED_USE`, `INVALID_POLICY_COMBINATION`, `INVALID_ASSET_TYPE` |
| 401 | `MISSING_API_KEY`, `INVALID_API_KEY` |
| 404 | `ASSET_NOT_FOUND`, `POLICY_NOT_FOUND`, `DECISION_NOT_FOUND` |
| 409 | `DUPLICATE_POLICY_VERSION`, `DUPLICATE_ASSET_VERSION`, `DUPLICATE_EDGE`, `CYCLE_DETECTED` |
| 422 | `UNKNOWN_POLICY_REFERENCE`, `UNKNOWN_INPUT_ASSET` |
