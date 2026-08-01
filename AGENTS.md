# IdentityHub agent guide

## Mission

Build the IdentityHub incrementally from the approved product and architecture
documents. Prefer the smallest secure implementation that proves the current
vertical slice.

## Sources of truth

Read the relevant documents before changing behavior:

1. `docs/identityhub-spec.md` — verifiable MVP behavior;
2. `docs/architecture.md` — modules, boundaries and flows;
3. `docs/security-model.md` — threats and mandatory controls;
4. `docs/integration-mode.md` — consumer contract;
5. `docs/migration-strategy.md` — delivery sequence and gates;
6. `docs/autonomous-delivery.md` — autonomous mandate and stop conditions;
7. `docs/adr/` — accepted decisions.

When code and documentation disagree, stop and request human direction. Do not
silently reinterpret an accepted decision.

## Delivery workflow

- Work in a branch created from an updated `develop`.
- Define acceptance criteria before non-trivial implementation.
- Use TDD: red test, minimum implementation, refactor.
- When an autonomous mandate is active, commit, push, open and merge pull
  requests only after every gate in `docs/autonomous-delivery.md` passes.
- Without an active autonomous mandate, request human approval before committing
  or pushing a completed increment.
- Use Conventional Commits and keep unrelated contexts separate.
- Never edit another product repository, including Auto Radar, from this project.
- Record deferrable decisions in `docs/pending-decisions.md`; stop for blocking
  decisions instead of selecting a product or security policy silently.

## Architecture

- `identityhub-service` is the Spring Boot deployable.
- Organize business code by capability, not by global technical layer.
- Create a capability package only when its vertical slice starts.
- Inside a capability, use `domain`, `application`, `adapter.in` and
  `adapter.out` only when each boundary is real.
- Domain code must not depend on Spring, HTTP, JPA or Keycloak.
- Modules must not access another module's repositories or internal entities.
- Synchronous collaboration uses small public application contracts.
- Important completed facts may use domain or application events.
- Cycles between capabilities are forbidden.
- `bootstrap` owns Spring startup, configuration, security wiring, jobs and
  observability. It contains no business rules.

## Simplicity rules

- Do not create generic repositories, base services, mapper layers, DTO mirrors,
  `common` packages or extension points without a concrete need.
- Prefer records for immutable application and adapter contracts.
- Keep controllers thin and aggregates responsible for their invariants.
- Inject time through `Clock`.
- Wrap an external system behind an adapter; do not wrap pure language features.
- Do not add a dependency until the current slice uses it.

## Keycloak boundary

- Keycloak is an internal engine, never the public integration contract.
- Use documented OIDC/OAuth endpoints and Admin REST APIs.
- Types under `org.keycloak` may exist only inside a dedicated Keycloak adapter.
- Domain, application, public API and Integration Mode must remain engine-neutral.
- Never read or modify Keycloak's private database schema.

## Security

- Default to deny and least privilege.
- Never log tokens, passwords, OTP codes, cookies, authorization headers or
  provider secrets.
- Do not provide secret defaults.
- Validate issuer, audience, signature and time constraints when token work starts.
- Preserve environment isolation.
- Administrative behavior requires explicit negative authorization tests.
- Do not weaken a control merely to make a test pass.

## Testing and validation

Canonical local command on Windows:

```powershell
.\gradlew.bat clean build
```

Canonical command on Linux and CI:

```sh
./gradlew clean build
```

Before handoff:

- run the canonical build;
- run focused tests while iterating;
- verify `bootJar` when the service changes;
- update ArchUnit rules when a new capability boundary is introduced;
- scan the diff for credentials, generated files and unrelated changes;
- document any test that could not be executed.

Before autonomous merge, also require all mandatory GitHub checks to be green
and no unresolved blocking review or pending decision.

Use Testcontainers for real PostgreSQL or Keycloak integration only when the
slice introduces those dependencies. Unit tests must remain the fastest layer.

## Repository hygiene

- Do not commit `build/`, IDE files, logs, local environment files or secrets.
- Preserve `docs/archive/v0.3.0/` and the historical tag.
- Do not rewrite unrelated user changes.
- Avoid destructive Git commands.
- Database cleanup requires explicit target inspection and human authorization.
- Autonomous delivery never authorizes production deployment, paid services,
  real-secret rotation, destructive data operations or writes to another
  repository.
