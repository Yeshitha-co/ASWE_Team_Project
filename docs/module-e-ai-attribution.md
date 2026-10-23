# Module E AI-assisted code attribution

Tool: Cursor, Grok 4.7.

Token counts were not available, so none are recorded. This note does not say the code was written without AI assistance.

Source files do not contain attribution comments.

## Created or edited in this change

- `pom.xml` (Spring Security and REST Assured test dependencies)
- `README.md` (Module E status and client registration)
- `src/main/java/com/licensegraph/LicenseGraphApplication.java`
- `src/main/java/com/licensegraph/security/package-info.java`
- `src/main/java/com/licensegraph/security/ApiClient.java`
- `src/main/java/com/licensegraph/security/ApiClientRepository.java`
- `src/main/java/com/licensegraph/security/ApiErrorFactory.java`
- `src/main/java/com/licensegraph/security/ApiErrorResponseWriter.java`
- `src/main/java/com/licensegraph/security/ApiKeyFilter.java`
- `src/main/java/com/licensegraph/security/ApiKeyGenerator.java`
- `src/main/java/com/licensegraph/security/ApiKeyHasher.java`
- `src/main/java/com/licensegraph/security/ClientController.java`
- `src/main/java/com/licensegraph/security/ClientIdHolder.java`
- `src/main/java/com/licensegraph/security/ClientRegistrationResponse.java`
- `src/main/java/com/licensegraph/security/ClientRegistrationService.java`
- `src/main/java/com/licensegraph/security/GlobalExceptionHandler.java`
- `src/main/java/com/licensegraph/security/RequestClientContext.java`
- `src/main/java/com/licensegraph/security/SecurityConfig.java`
- `src/test/java/com/licensegraph/security/ApiKeyHasherTest.java`
- `src/test/java/com/licensegraph/security/ClientApiTest.java`
- `src/test/java/com/licensegraph/security/ErrorHandlingApiTest.java`
- `src/test/java/com/licensegraph/security/ModuleEWebTest.java`
- `src/test/java/com/licensegraph/support/ApiTestBase.java`

## Already on main from the E1 skeleton

These were also written with the same assistance and were committed earlier. Shared contracts under `common`, `policy`, `asset`, `lineage`, and `decision` were not changed for client registration, the API-key filter, or error handling. Persistence files from pull request #2 are teammate work.

- `.gitignore`
- `src/main/java/com/licensegraph/security/ClientContext.java`
- `src/main/resources/application.yml` (later edited by the persistence pull request)
- `src/main/java/com/licensegraph/config/ApplicationConfig.java`
- `src/test/java/com/licensegraph/LicenseGraphApplicationTests.java`
- `src/test/java/com/licensegraph/common/SharedContractsTest.java`
- the E1 interfaces and records under `com.licensegraph.common`, `policy`, `asset`, `lineage`, and `decision`
