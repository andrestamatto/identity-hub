# identity-hub
`identity-hub` is an authentication and authorization core built with Java + Spring Boot.

The main goal is to allow your project to focus on the business without worrying about implementing these mechanisms or duplicating security flow implementations across multiple projects.
Instead of each system creating its own login, token, and role-permission logic, `identity-hub` centralizes this behavior with a configurable model.

Current vision:
- generic identity (`identity`) instead of hardcoding only `email`;
- stateless JWT authentication;
- configurable route authorization rules, independent from business-specific role/permission names.

Fictional use case:
- Imagine a company with 5 products (`CRM`, `Support`, `Billing`, `Analytics`, `Admin Portal`).
- Each product has different routes and authorization vocabulary.
- `identity-hub` provides one shared auth core, and each product configures only what is business-specific through identityhub-conf.yml (route patterns, access rules such as ANY_ROLE/ALL_PERM, and supported identity_type values).

Long-term, this project is intended to evolve into:
- a reusable module/starter for Java applications; and
- later, a standalone identity service (Keycloak-like) with dedicated APIs.

## Current Status (Implemented)
- Authentication by `identity + password`.
- Stronger password typing in domain:
  - `RawPassword`
  - `EncodedPassword`
- JWT issuance with claims:
  - `sub`
  - `identity`
  - `identity_type`
  - `roles`
  - `permissions`
- JWT filter resolves claims into Spring authorities:
  - roles -> `ROLE_*`
  - permissions -> `PERM_*`
- Route authorization is configuration-driven (`identityhub-conf.yml`).
- Supported access rule DSL:
  - `PERMIT_ALL`
  - `DENY_ALL`
  - `AUTHENTICATED`
  - `ROLE:<VALUE>`
  - `PERM:<VALUE>`
  - `ANY_ROLE:<A,B,...>`
  - `ANY_PERM:<A,B,...>`
  - `ANY_AUTHORITY:<A,B,...>`
  - `ALL_ROLE:<A,B,...>`
  - `ALL_PERM:<A,B,...>`
  - `HAS_IP:<IP1,IP2,...>`

## Architecture (Current)
- `application`: use cases + ports.
- `domain`: core model/value objects and domain services.
- `infrastructure`: persistence, JWT, Spring Security adapters/config.
- `interfaces`: REST controllers and DTOs.

## Configuration Files
- Main app config: `src/main/resources/application.yml`
- Default identity-hub config: `src/main/resources/identityhub-conf.yml`

Default access rules are intentionally generic:
- `/auth/**` -> `PERMIT_ALL`
- `/**` -> `AUTHENTICATED`

## Local Built-in Identity Store (Purpose)
The project currently uses H2 + SQL seeds (`schema.sql`, `data.sql`) as a **built-in local identity store**.

Why this exists:
- to make clone-and-run testing possible with no external dependencies;
- to provide immediate test users/roles/permissions with default configuration;
- to speed up learning and iterative development.

Important:
- this is for local/dev validation only;
- production persistence concerns are intentionally out of scope at this stage.

## Seeded Test Users (`data.sql`)
For testing, all seeded users use the same raw password:
- `Password@123`

| Identity | Roles |
|---|---|
| user1@identityhub.dev | USER |
| user2@identityhub.dev | USER |
| user3@identityhub.dev | USER, CREATOR |
| user4@identityhub.dev | USER |
| user5@identityhub.dev | USER, BETA_TESTER |
| user6@identityhub.dev | USER |
| user7@identityhub.dev | USER, CREATOR |
| user8@identityhub.dev | USER |
| user9@identityhub.dev | USER, ANALYST |
| user10@identityhub.dev | ADMIN |
| user11@identityhub.dev | ADMIN, MANAGER |
| user12@identityhub.dev | ADMIN |
| user13@identityhub.dev | ADMIN, SECURITY |
| user14@identityhub.dev | ADMIN, SUPPORT |
| user15@identityhub.dev | ADMIN, ANALYST |
| user16@identityhub.dev | SUPPORT |
| user17@identityhub.dev | SUPPORT, USER |
| user18@identityhub.dev | SUPPORT, MANAGER |
| user19@identityhub.dev | SUPPORT |
| user20@identityhub.dev | SUPPORT, SECURITY |
| user21@identityhub.dev | AUDITOR |
| user22@identityhub.dev | AUDITOR, ANALYST |
| user23@identityhub.dev | AUDITOR |
| user24@identityhub.dev | AUDITOR, SECURITY |
| user25@identityhub.dev | AUDITOR, MANAGER |
| user26@identityhub.dev | CREATOR |
| user27@identityhub.dev | CREATOR, USER |
| user28@identityhub.dev | CREATOR, MANAGER |
| user29@identityhub.dev | CREATOR, SUPPORT |
| user30@identityhub.dev | CREATOR, ANALYST |

For full permissions per user, check `src/main/resources/data.sql`.

## Run Locally
Requirements:
- Java 21+
- Gradle Wrapper (already in repository)

Start:
```bash
./gradlew bootRun
```
On Windows PowerShell:
```powershell
.\gradlew.bat bootRun
```

### Local OAuth2 credentials setup (recommended)
Do not commit real OAuth credentials. Use local scripts:

PowerShell:
```powershell
Copy-Item .\scripts\dev-env.ps1.example .\scripts\dev-env.ps1
# edit with real values
. .\scripts\dev-env.ps1
.\gradlew.bat bootRun
```

Bash:
```bash
cp ./scripts/dev-env.sh.example ./scripts/dev-env.sh
# edit with real values
source ./scripts/dev-env.sh
./gradlew bootRun
```

Default base URL:
- `http://localhost:8080`

## Quick Validation (Manual Testing)
### 1) Login
Endpoint:
- `POST /auth/login`

Request body:
```json
{
  "identity": "user10@identityhub.dev",
  "password": "Password@123"
}
```

Compatibility note:
- `email` is accepted as an alias for `identity` in login payload.

Example:
```bash
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"identity":"user10@identityhub.dev","password":"Password@123"}'
```

### 2) Use JWT on a protected endpoint
Endpoint:
- `GET /users/me`

```bash
curl http://localhost:8080/users/me \
  -H "Authorization: Bearer <ACCESS_TOKEN>"
```

Expected response includes:
- `subject` (JWT subject)
- `authorities` (resolved `ROLE_*` and `PERM_*`)

### 3) Run test suite
```bash
./gradlew test
```
On Windows PowerShell:
```powershell
.\gradlew.bat test
```

## Integrating identity-hub Into Your Project (Current State)
At this stage, the recommended integration model is to run `identity-hub` as an auth service and consume its token flow from your application.

### Step 1: Clone and run
```bash
git clone <this-repository-url>
cd identity-hub
./gradlew bootRun
```

### Step 2: Adapt `identityhub-conf.yml` to your business rules
Edit:
- `src/main/resources/identityhub-conf.yml`

Typical changes:
- route patterns and access operators (`identity-hub.security.rules`);
- default/supported identity types (`identity-hub.jwt`).

Example:
```yml
identity-hub:
  security:
    rules:
      - pattern: /auth/**
        access: PERMIT_ALL
      - pattern: /videos/upload
        access: ANY_ROLE:CREATOR,ADMIN
      - pattern: /reports/**
        access: ALL_PERM:REPORT_READ,EXPORT_DATA
      - pattern: /internal/**
        access: HAS_IP:127.0.0.1,10.0.0.10
      - pattern: /**
        access: AUTHENTICATED
  jwt:
    default-identity-type: email
    supported-identity-types:
      - email
      - username
      - external_id
```

### Step 3: Consume login/token in your application
Your consumer app should:
- call `POST /auth/login`;
- receive JWT;
- send JWT in `Authorization: Bearer <token>` when calling protected resources.

### Step 4: Validate final authorities
Use `GET /users/me` to verify the final authorities generated from roles and permissions.

## Disabling the Built-in Local Identity Store (Consumer Guide)
By default, the built-in local identity store is enabled:
- `identity-hub.local-identity-store.enabled: true`

If your consumer project provides its own identity source, disable the built-in local identity store explicitly.

### Step-by-step
1. In your consumer configuration, set:
```yml
identity-hub:
  local-identity-store:
    enabled: false
```

2. Provide your own implementation of `LoadExternalIdentity`:
```java
@Component
public class MyExternalIdentityAdapter implements LoadExternalIdentity {
    @Override
    public Optional<User> findByIdentity(String identityValue) {
        // Load from your real data source (DB/API/LDAP/etc.)
        return Optional.empty();
    }
}
```

3. Keep your security rules in `identityhub-conf.yml` aligned with your business:
```yml
identity-hub:
  security:
    rules:
      - pattern: /auth/**
        access: PERMIT_ALL
      - pattern: /**
        access: AUTHENTICATED
```

What happens when `enabled=false`:
- identity-hub does not bootstrap built-in local persistence infrastructure (H2/JPA setup);
- your project becomes responsible for supplying `LoadExternalIdentity`.

## Roadmap (Next Versions)
- OAuth providers (Google, Microsoft, GitHub, Facebook).
- Module-first packaging for easier embedding into Java applications.
- Better identity source/type configurability.
- Standalone deployable identity service (REST and/or gRPC).
- Operational production concerns (auditing, observability, key rotation, etc.).
