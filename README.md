# Tax Administrative Fine Platform

Spring Boot microservices for managing tax administrative fines in Vietnam.

## Modules

- `api-gateway` - entry point on port 8088.
- `eureka-server` - service registry on port 8761.
- `common-library` - shared JWT security, `ApiResponse`, exception handler, utilities, Kafka topic/event config and OpenFeign defaults.
- `common-library` also provides an AOP audit logger. Each REST controller call is stored in the `audit_logs` table of the service database with service name, action, URI, username, HTTP status, duration and error information.
- `province-service` - provinces/cities and administrative units.
- `organization-service` - tax departments and enforcement units.
- `taxpayer-service` - taxpayer/business profiles.
- `fine-service` - violation records, fine decisions and Kafka events.
- `payment-service` - receives `fine.created` and tracks payment requests.
- `identity-service` - admin-only API for looking up realm users and updating their application roles through Keycloak.
- `dataplatform` - tenant-scoped source and dataset catalog APIs under `/api/v1/**`.

### Independently versioned common library

`common-library` is published as an independent Maven artifact (`vn.gov.tax:common-library`). Each consuming service pins its own version with `<common-library.version>` in its POM, so services can upgrade independently. Configure `COMMON_MAVEN_REPOSITORY_URL` and a Maven `settings.xml` server with ID `common-library`; the Jenkins credential for that settings file is named `maven-settings`.

To publish a shared-library change, increment `<version>` in `common-library/pom.xml` and run the Jenkins job with `PUBLISH_COMMON_LIBRARY=true`. Then update `<common-library.version>` only in the POMs of services that need the change. Set `BUILD_SERVICE` to the affected service to test, build, push, and deploy only that service. Use `all` for a full release. Standalone local build: `mvn -f dataplatform/pom.xml spring-boot:run`; that service resolves its pinned common artifact instead of building the common module. For common-library development inside the reactor, `mvn -pl dataplatform -am test` still builds it from source.

## Shared Kafka and Feign

Kafka topic and event contract are centralized in `common-library`:

- `common-library/.../messaging/CommonKafkaConfig.java`
- `common-library/.../messaging/KafkaTopics.java`
- `common-library/.../messaging/event/FineCreatedEvent.java`

Kafka is enabled only for `fine-service` and `payment-service` with `app.kafka.enabled=true`. `organization-service` contains a sample `ProvinceClient` that calls `province-service` through Eureka using OpenFeign. Example endpoint: `GET /api/organizations/province/{id}`.

## Run infrastructure

```bash
docker compose up -d
```

This starts local Redis on `6379`, SQL Server on `1433`, Kafka on `9092`, and Keycloak on `8080`. Eureka is a Spring Boot module and runs on `8761`. Set the same random `REDIS_PASSWORD` for Compose and the Spring services before starting them. The Compose Redis instance is for development; production should use a managed or clustered Redis deployment with TLS, ACL credentials, high availability, and persistence enabled.

The `sqlserver-init` job waits for SQL Server to become healthy and creates the shared `tax_platform` database if it does not exist. Development services then create/update their tables through Hibernate. Use the same `MSSQL_SA_PASSWORD` and `DB_PASSWORD` values in `.env`; SQL Server requires a strong password. SQL Server is published on `127.0.0.1:1433` for local access only.

Copy `.env.example` to `.env` and replace development secrets before starting Compose.

## Environments

Each business service has three profile files: `application-dev.yml`, `application-uat.yml` and `application-prod.yml`. The base `application.yml` defaults to `dev`.

Switch environment before starting a service:

```powershell
$env:SPRING_PROFILES_ACTIVE = "uat"
mvn -pl fine-service spring-boot:run
```

Use `dev` for local defaults, `uat` for shared testing and `prod` for production. UAT/production require `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `KEYCLOAK_ISSUER_URI`, `EUREKA_SERVER_URL`, `REDIS_HOST`, `REDIS_USERNAME`, and `REDIS_PASSWORD` to be provided by the deployment environment. `identity-service` also needs `KEYCLOAK_ADMIN_URL`, `KEYCLOAK_ADMIN_CLIENT_ID`, and `KEYCLOAK_ADMIN_CLIENT_SECRET`. UAT/production use `ddl-auto: validate` and require TLS to Redis.

## Internal whitelist

The common security config permits only `/internal/**` and `/api/*/internal/**` without JWT. All normal business endpoints remain authenticated. The Feign sample uses `/api/provinces/internal/{id}`. Keep these routes reachable only on a private service network; the whitelist is not a substitute for mTLS or network policy.

Keycloak is available at http://localhost:8080 (admin/admin). Realm import is in `infra/keycloak/tax-realm.json`.

## Database configuration

Each service has its own environment profile file. The default database in `dev` is `tax_platform`; connection values can be overridden without changing source code:

```powershell
$env:DB_URL = "jdbc:sqlserver://db-host:1433;databaseName=tax_platform;encrypt=true;trustServerCertificate=true"
$env:DB_USERNAME = "sa"
$env:DB_PASSWORD = "your-secret"
$env:SPRING_PROFILES_ACTIVE = "local"
```

For a different environment, create `application-<profile>.yml` in each service and start with `SPRING_PROFILES_ACTIVE=<profile>`.

Before deploying services with `ddl-auto: validate`, apply `deploy/migrations/001_create_audit_logs.sql` to the application database. The migration is idempotent and creates the shared audit table used by `common-library`; run it as a deployment migration using the same DB credentials and target database configured for the services.

## Run services

```bash
mvn clean package
mvn -pl eureka-server spring-boot:run
mvn -pl province-service spring-boot:run
```

Run Eureka before the clients, then run each service in a separate terminal. The gateway uses `lb://...` routes resolved by Eureka. Eureka dashboard: http://localhost:8761. Gateway: http://localhost:8088.

### Swagger / OpenAPI

Services using `common-library` expose Swagger UI at `/swagger-ui/index.html` and OpenAPI JSON at `/v3/api-docs` when running with the default `dev` or `uat` profile. The API gateway Swagger UI aggregates the service specs at `http://localhost:8088/swagger-ui/index.html`; Eureka has its own UI at `http://localhost:8761/swagger-ui/index.html`. Use the **Authorize** button and a bearer access token to try protected operations. Production profiles disable Swagger by default; set `SWAGGER_ENABLED=true` only when documentation access is intentionally enabled.

The data platform registers with Eureka on port `8087` and stores catalog tables in the shared `tax_platform` database. Its APIs use the Keycloak `tenant_id` claim when present, or the authenticated user's subject (`sub`) when it is absent. JWT authentication remains required; `tax-officer` and `supervisor` can read sources/datasets, and only `supervisor` can create sources.

To search source connections, call `GET /api/v1/sources` with optional `name` (case-insensitive partial match), `type` (`MSSQL`, `MYSQL`, `POSTGRESQL`, or `ORACLE`), and `status` (`ACTIVE` or `DISABLED`) filters plus `page` and `size`. Omitting `status` preserves the default active-only list. For example, `GET /api/v1/sources?type=MSSQL&name=tax&status=ACTIVE&page=0&size=20` searches the current tenant's active MSSQL sources. Select a result and call `GET /api/v1/sources/{id}` for connection host, port, database/schema, username, authentication method, and status. Disabled source details remain searchable by explicitly filtering `status=DISABLED`; disabled sources cannot be used for table discovery or execution. Passwords are never returned by list or detail APIs.

### MSSQL source management

Set `DATAPLATFORM_ENCRYPTION_KEY` in `.env` to a Base64-encoded 32-byte key. The development `.env` is git-ignored. Production must inject a different key through the deployment secret store; losing or rotating the key without re-encrypting credentials makes stored source passwords unreadable.

Run the service with `mvn -f dataplatform/pom.xml spring-boot:run` after publishing/resolving its pinned `common-library` version. During common-library development, use `mvn -pl dataplatform -am spring-boot:run` to build it from the reactor. Supported source types are `MSSQL`, `MYSQL`, `POSTGRESQL`, and `ORACLE`. Through the gateway, send `POST /api/v1/sources/connection-test` a body with `type` and the `connection` object below to test without saving. `POST /api/v1/sources` creates a source after a successful connection test; `GET /api/v1/sources` and `GET /api/v1/sources/{id}` list and view sources; `PUT /api/v1/sources/{id}` updates one; `DELETE /api/v1/sources/{id}` removes it. Read operations allow `tax-officer` and `supervisor`; connection tests and mutations require `supervisor`. Every operation is scoped by `tenant_id` when present, otherwise by the authenticated JWT subject (`sub`).

Create and update requests separate business metadata from the connection profile:

```json
{
  "metadata": {
    "type": "MSSQL",
    "name": "tax-reporting",
    "description": "SQL Server reporting source"
  },
  "connection": {
    "host": "sql.example.internal",
    "port": 1433,
    "databaseName": "reporting",
    "schemaName": "dbo",
    "username": "report_reader",
    "password": "<secret>",
    "encrypt": true,
    "trustServerCertificate": false
  }
}
```

For Oracle, `databaseName` is the service name. TLS uses TCPS and the JVM truststore; set `trustServerCertificate` to `false` and configure the trusted CA in the runtime truststore.

The password is encrypted with AES-GCM and stored separately from source metadata. It is never included in source API responses. On update, omit or leave `password` blank to keep the existing credential.

Obtain a token from Keycloak:

```bash
curl -X POST http://localhost:8080/realms/tax-platform/protocol/openid-connect/token \
  -d client_id=tax-api \
  -d client_secret=tax-api-secret \
  -d username=admin.user \
  -d password=admin123 \
  -d grant_type=password
```

Send it with `Authorization: Bearer <token>`. Replace the sample credentials in non-development environments.

JWT validation requires issuer, audience `tax-api`, expiry and Keycloak roles. The `tax-officer` and `supervisor` roles are allowed to call protected APIs. Configure browser origins with `CORS_ALLOWED_ORIGINS`, for example `http://localhost:3000,http://localhost:8088`; the setting applies to both the gateway and direct Data Platform API access. Do not use `*` with credentials enabled.

### Data platform pipelines and Airflow

`dataplatform` is the source of truth for source connection metadata, pipeline definitions, and execution records. Airflow owns DAG/task state and data-plane job execution; both sides should persist the shared `sourceId`, `pipelineId`, and `executionId` so they can reconcile the same run without maintaining conflicting copies of source configuration. Create a pipeline with `POST /api/v1/pipelines`; its `definition` uses schema version `1` and contains the source table, watermark column, keys, Bronze/Silver/Gold/ClickHouse targets, cleaning rules, and DQ rules. The source database type is read from the registered source and included in every compiled execution plan. The supported cleaning operations are `TRIM`, `NORMALIZE_WHITESPACE`, `LOWERCASE`, and `UPPERCASE`; DQ rule types are `NOT_NULL`, `UNIQUE`, `REGEX`, `MIN`, and `MAX`.

Call `POST /api/v1/pipelines/{id}/compile` to validate the configured columns against the live source schema. Invalid plans return validation errors and are not sent to Airflow. Call `POST /api/v1/pipelines/{id}/runs` to launch a run (`202 Accepted`), then poll `GET /api/v1/executions/{executionId}` for run and task states. `GET /api/v1/pipelines/{id}/runs` lists past runs.

Configure the external Airflow REST API with `AIRFLOW_BASE_URL`, `AIRFLOW_API_PREFIX` (default `/api/v1`; set `/api/v2` if required by the Airflow deployment), and `AIRFLOW_DAG_ID`. Authenticate with `AIRFLOW_API_TOKEN` or `AIRFLOW_API_USERNAME` and `AIRFLOW_API_PASSWORD`. Data Platform triggers Airflow with `POST {AIRFLOW_BASE_URL}{AIRFLOW_API_PREFIX}/dags/{AIRFLOW_DAG_ID}/dagRuns`. The Airflow request body is:

```json
{
  "dag_run_id": "manual__<unique-run-id>",
  "conf": {
    "execution_id": "<execution-uuid>",
    "plan": {
      "schemaVersion": 1,
      "pipelineId": "<pipeline-uuid>",
      "sourceId": "<source-uuid>",
      "sourceType": "MSSQL",
      "definition": {
        "schemaVersion": 1,
        "sourceTable": "dbo.orders",
        "watermarkColumn": "updated_at",
        "keyColumns": ["order_id"],
        "bronzeTable": "bronze.orders",
        "silverTable": "silver.orders",
        "goldTable": "gold.orders",
        "clickHouseTable": "analytics.orders",
        "cleaningRules": [],
        "dqRules": [
          {
            "name": "order-id-required",
            "column": "order_id",
            "type": "NOT_NULL",
            "value": null
          }
        ]
      }
    }
  }
}
```

`sourceType` is one of `MSSQL`, `MYSQL`, `POSTGRESQL`, or `ORACLE`; `definition.sourceTable` identifies the exact input table. The plan is credential-free. The Airflow worker obtains an audience-validated Keycloak service-account token for the `dataplatform-worker` client and calls `GET /api/v1/internal/executions/{executionId}/source-connection`; the response envelope's `data` contains the connection settings, including the password needed for the active run. The endpoint requires the `data-platform-worker` realm role and only serves credentials for an active execution. Keep this endpoint private and use TLS between Airflow and `dataplatform`; never persist source passwords in Airflow DAG configuration or run metadata.

Airflow owns the data plane: INS extraction, Bronze/Silver/Gold writes, quarantine, DQ, ClickHouse load, reconciliation, and watermark commit. The DAG must only advance its source watermark after DQ and reconciliation succeed and the commit/audit step completes; on failure it must preserve the previous watermark. `dataplatform` records Airflow status but never advances the watermark itself. The ClickHouse-backed Data Service / BI query endpoint remains a separate integration from this run-control API.

### Keycloak role management

The imported `tax-platform` realm defines the realm roles `tax-officer` and `supervisor`. Manage users and assign these roles in the Keycloak Admin Console at **Users → select a user → Role mapping → Assign role**. The development import gives `admin.user` the `tax-officer` role; assign `supervisor` to a separate user when needed. Do not reuse development credentials outside local development.

Business services map Keycloak realm roles and roles under the `tax-api` client to Spring authorities (`ROLE_tax-officer`, `ROLE_supervisor`). Set `app.security.client-id` if the client is named differently. Protected `/api/**` endpoints require `tax-officer` or `supervisor`; `DELETE /api/**` requires `supervisor`. The gateway checks that a JWT is present, while each business service enforces the role policy.

To let trusted administrators update roles through this project:

1. In the `tax-platform` realm, create a confidential client named `tax-admin-api`, enable service accounts, and disable direct access grants.
2. Assign the service account these `realm-management` client roles: `query-users`, `view-users`, `manage-users`, and `view-realm`. Do not assign `realm-admin`.
3. Set `KEYCLOAK_ADMIN_URL`, `KEYCLOAK_ADMIN_CLIENT_ID`, and `KEYCLOAK_ADMIN_CLIENT_SECRET` for `identity-service` using the client secret from Keycloak.
4. Assign the realm role `user-admin` to each human operator who may use the project API. The API cannot assign `user-admin` to other users; it only manages `tax-officer` and `supervisor`.
5. Start `identity-service` with Eureka and the gateway. It is reachable through the gateway at `/api/admin/**`.

Run it locally with `mvn -pl identity-service spring-boot:run` after exporting the three `KEYCLOAK_ADMIN_*` settings above.

The admin API supports user lookup with `GET /api/admin/users?search=<text>`, managed role listing with `GET /api/admin/roles`, and assigned role lookup with `GET /api/admin/users/{userId}/roles`. Replace a user's managed roles with `PUT /api/admin/users/{userId}/roles` and JSON body `{"roles":["tax-officer"]}`. Send an operator's Keycloak access token as a Bearer token. The service account credentials stay on the server and are never returned by these endpoints.

When roles are changed, `identity-service` writes a per-user issued-at cutoff to Redis before and after updating Keycloak. Every protected service checks this shared cutoff while validating JWTs, so tokens issued before the completed change stop working across services; the user must obtain a new token with the updated roles. The check fails closed if Redis is unavailable, so Redis must be treated as a security-critical dependency and monitored accordingly. Set `TOKEN_REVOCATION_TTL` longer than the maximum Keycloak access-token lifetime plus clock skew; the default is 15 minutes. For production, configure a short access-token lifetime (for example, 5 minutes) and use the same Redis endpoint, namespace, TTL, and TLS settings in every business service and `identity-service`.

Immediate invalidation applies to role changes made through `identity-service`. If an administrator changes roles directly in the Keycloak console, the existing token has no Redis cutoff and can remain usable until it expires.

The application uses Redis `GET`/`SET` and an atomic Lua script (`EVAL`/`EVALSHA`) for revocation cutoffs. Health checks also require `PING`. Restrict the Redis ACL user to these commands and the `tax-platform:security:revoked-before:*` key pattern (adjust the pattern if `TOKEN_REVOCATION_KEY_PREFIX` changes). The production profiles require `REDIS_HOST`, `REDIS_USERNAME`, and `REDIS_PASSWORD`, enable TLS, and use short connection/command timeouts with a bounded Lettuce pool. Readiness checks include Redis; liveness checks do not. Redis persistence/replication must preserve recent revocation keys across failover; otherwise old JWTs could become valid again before their natural expiration.

After changing `tax-realm.json`, recreate the development Keycloak realm/container or add the `tax-api-audience` protocol mapper, `user-admin` role, and `dataplatform-worker` client/service-account role manually, because Keycloak does not re-import an already existing realm automatically.
