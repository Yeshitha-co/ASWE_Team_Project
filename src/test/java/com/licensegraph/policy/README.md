## Module A: Policy Registry and Use Rules

### Overview

Module A manages versioned license policies and provides the interfaces used by other modules to retrieve policies, evaluate intended uses, and generate license obligations.

It is responsible for:

- Creating and validating license policies.
- Storing multiple versions of a policy without overwriting earlier versions.
- Retrieving policies by client, policy ID, and version.
- Evaluating whether a particular intended use is permitted by a policy.
- Generating and merging obligations associated with policies.
- Providing `PolicyLookup` and `PolicyRules` interfaces so other modules can use policy functionality without directly accessing the database.

### Policy Storage

License policies are represented by the `LicensePolicy` JPA entity and stored in the `license_policies` database table.

Each policy contains:

| Field | Description |
|---|---|
| `clientId` | Identifies the client that owns the policy. |
| `policyId` | Identifies the policy. |
| `version` | Identifies a particular version of the policy. |
| `commercialUse` | Specifies whether commercial use is permitted. |
| `redistribution` | Specifies whether redistribution is permitted. |
| `derivatives` | Specifies whether derivative works are permitted. |
| `aiTraining` | Specifies whether AI training is permitted. |
| `attributionRequired` | Indicates whether attribution is required. |
| `shareAlike` | Indicates whether share-alike obligations apply. |

The combination of `clientId`, `policyId`, and `version` must be unique. This allows different clients to maintain their own policies and allows a policy to have multiple versions.

### REST API

The `PolicyController` exposes the following endpoints:

| Method | Endpoint | Purpose |
|---|---|---|
| `POST` | `/api/policies` | Creates a new policy version. |
| `GET` | `/api/policies/{policyId}/versions` | Retrieves the available versions of a policy. |
| `GET` | `/api/policies/{policyId}/versions/{version}` | Retrieves a specific policy version. |

Policy creation uses `CreatePolicyRequest`, which validates the required policy ID, version, and policy-rule fields. `PolicyResponse` represents the policy data returned by the API.

### Validation and Error Handling

Policy creation and retrieval enforce the following behavior:

- Required fields are validated before a policy is created.
- Invalid combinations of policy rules are rejected.
- Creating a version that already exists for the same client and policy produces a conflict.
- Missing policies or requested versions produce a not-found error.
- Policy queries are scoped to the requesting client, preventing one client from retrieving another client's policies through the client-scoped service and lookup operations.

The module uses shared typed exceptions, including `BadRequestException`, `ConflictException`, and `NotFoundException`, together with the common API error contracts. HTTP error responses are handled through the application's shared exception-handling layer.

### Policy Lookup Interface

`PolicyLookup` provides a database-independent interface for retrieving policy data.

`PolicyLookupImpl` implements this interface using the policy service and returns a `PolicyView` containing the policy reference and its rule fields. A missing or inaccessible policy does not produce a policy view.

This interface allows other modules to retrieve policy information without depending directly on the JPA entity or repository.

### Intended-Use Rules

`PolicyRules` provides the rule-evaluation and obligation-generation operations. `PolicyRulesImpl` implements the rules independently of database access.

The supported intended uses are:

| Intended use | Policy field checked |
|---|---|
| `commercial` | `commercialUse` |
| `redistribution` | `redistribution` |
| `derivative` | `derivatives` |
| `ai-training` | `aiTraining` |
| `internal` | Always allowed |

Rule evaluation returns a `RuleOutcome` containing an `allowed` boolean and a `blockingField`. When a policy blocks an intended use, `blockingField` identifies the field responsible for the denial. For an allowed outcome, the blocking field is `null`.

### Obligations

The module generates obligations from policy requirements:

- **Attribution:** generated when `attributionRequired` is enabled.
- **Share-alike:** generated when `shareAlike` is enabled.

Each `Obligation` records its type, source, and associated policy. The `mergeObligations` operation combines obligations from multiple sources, removes duplicates for the same obligation type and source, and sorts the results deterministically by asset and obligation type.

This allows downstream modules to evaluate policies and combine their obligations consistently across multiple assets.

### Implementation Structure

| Component | Responsibility |
|---|---|
| `LicensePolicy` | JPA entity representing a versioned license policy. |
| Policy repository | Database persistence and client-scoped policy queries. |
| `PolicyService` / `PolicyServiceImpl` | Policy creation, validation, version retrieval, and lookup. |
| `PolicyController` | REST endpoints for policy creation and retrieval. |
| `CreatePolicyRequest` | Policy creation request and input validation. |
| `PolicyResponse` | API representation of policy data. |
| `PolicyLookup` / `PolicyLookupImpl` | Database-independent policy lookup for other modules. |
| `PolicyRules` / `PolicyRulesImpl` | Intended-use evaluation and obligation generation and merging. |
| `PolicyView` | Policy data contract used by the lookup and rules interfaces. |
| `RuleOutcome` | Result of intended-use evaluation. |
| `Obligation` | Representation of a policy obligation. |

### Testing

Module A includes unit and API tests for policy creation, validation, duplicate versions, policy and version retrieval, client-scoped lookup, intended-use decisions, obligation generation, and obligation merging. The tests also cover internal-use permission and preservation of earlier policy versions when a newer version is created.

### Checkstyle and Code Style

The project uses the **Maven Checkstyle Plugin 3.6.0** with the **Google Java Style** ruleset (`google_checks.xml`). Checkstyle is configured to include both production and test source files.

The plugin is configured in `pom.xml` to support style checking and HTML report generation.

Run Checkstyle validation from the project root:

```powershell
mvn checkstyle:check
```

Generate the HTML report:

```powershell
mvn checkstyle:checkstyle
```

The generated reports are available under the Maven `target` directory:

- HTML report: `target/reports/checkstyle.html`
- XML results: `target/checkstyle-result.xml`

Module A follows the configured Java style rules and uses descriptive class, method, and variable names, with comments where they help explain non-obvious behavior.