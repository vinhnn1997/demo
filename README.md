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

## Run services

```bash
mvn clean package
mvn -pl eureka-server spring-boot:run
mvn -pl province-service spring-boot:run
```

Run Eureka before the clients, then run each service in a separate terminal. The gateway uses `lb://...` routes resolved by Eureka. Eureka dashboard: http://localhost:8761. Gateway: http://localhost:8088.

The data platform registers with Eureka on port `8087` and stores catalog tables in the shared `tax_platform` database. Its APIs require a Keycloak `tenant_id` claim; `tax-officer` and `supervisor` can read sources/datasets, and only `supervisor` can create sources.

### MSSQL source management

Set `DATAPLATFORM_ENCRYPTION_KEY` in `.env` to a Base64-encoded 32-byte key. The development `.env` is git-ignored. Production must inject a different key through the deployment secret store; losing or rotating the key without re-encrypting credentials makes stored source passwords unreadable.

Run the service with `mvn -pl dataplatform -am spring-boot:run`. Supported source types are `MSSQL`, `MYSQL`, `POSTGRESQL`, and `ORACLE`. Through the gateway, send `POST /api/v1/sources/connection-test` a body with `type` and the `connection` object below to test without saving. `POST /api/v1/sources` creates a source after a successful connection test; `GET /api/v1/sources` and `GET /api/v1/sources/{id}` list and view sources; `PUT /api/v1/sources/{id}` updates one; `DELETE /api/v1/sources/{id}` removes it. Read operations allow `tax-officer` and `supervisor`; connection tests and mutations require `supervisor`. Every operation is tenant-scoped by the JWT `tenant_id` claim.

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

JWT validation requires issuer, audience `tax-api`, expiry and Keycloak roles. The `tax-officer` and `supervisor` roles are allowed to call protected APIs. Configure frontend origins with `CORS_ALLOWED_ORIGINS`, for example `http://localhost:3000`; do not use `*` with credentials enabled.

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

After changing `tax-realm.json`, recreate the development Keycloak realm/container or add the `tax-api-audience` protocol mapper and `user-admin` realm role manually, because Keycloak does not re-import an already existing realm automatically.
