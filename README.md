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

This starts SQL Server on `1433`, Kafka on `9092`, and Keycloak on `8080`. Eureka is a Spring Boot module and runs on `8761`.

Copy `.env.example` to `.env` and replace development secrets before starting Compose.

## Environments

Each business service has three profile files: `application-dev.yml`, `application-uat.yml` and `application-prod.yml`. The base `application.yml` defaults to `dev`.

Switch environment before starting a service:

```powershell
$env:SPRING_PROFILES_ACTIVE = "uat"
mvn -pl fine-service spring-boot:run
```

Use `dev` for local defaults, `uat` for shared testing and `prod` for production. UAT/production require `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `KEYCLOAK_ISSUER_URI` and `EUREKA_SERVER_URL` to be provided by the deployment environment. UAT/production use `ddl-auto: validate`.

## Internal whitelist

The common security config permits only `/internal/**` and `/api/**/internal/**` without JWT. All normal business endpoints remain authenticated. The Feign sample uses `/api/provinces/internal/{id}`. Keep these routes reachable only on a private service network; the whitelist is not a substitute for mTLS or network policy.

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

JWT validation requires issuer, audience `tax-api`, expiry and Keycloak realm roles. The `tax-officer` and `supervisor` roles are allowed to call protected APIs. Configure frontend origins with `CORS_ALLOWED_ORIGINS`, for example `http://localhost:3000`; do not use `*` with credentials enabled.

After changing `tax-realm.json`, recreate the development Keycloak realm/container or add the `tax-api-audience` protocol mapper manually, because Keycloak does not re-import an already existing realm automatically.
