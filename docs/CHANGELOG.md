# Changelog

All notable changes to this project will be documented in this file.

## [Unreleased]
### Changed
- API resource-server projections now use their stable public audience as the
  Keycloak `client_id`, allowing native audience resolution without exposing a
  custom Keycloak claim contract.
- Platform-role assignments now use Keycloak as their MVP source of truth while
  IdentityHub retains the administrative contracts, authorization policies and
  audit boundary, avoiding an unsafe authoritative replica.
- Acquisition correlation now uses standard OIDC Authorization Code with PKCE
  in the consumer backend or BFF; the proprietary onboarding session, proof,
  endpoint and machine scope were removed from runtime and public contracts.
- Delivery governance now supports a revocable autonomous mode with mandatory
  PR checks, explicit stop conditions and a versioned pending-decision ledger.

### Added
- First Integration Mode runtime: the `identityhub-spring-boot-starter` Servlet
  auto-configures a stateless default-deny Resource Server, validates only
  `RS256` JWTs through issuer discovery/JWKS with issuer, audience, time and
  public-claim checks, verifies JWKS availability before a default decoder
  starts with bounded 2-second connect and 5-second read calls, maps only public
  scope/roles authorities, and backs off for an explicit consumer security chain.
  Unit, HTTP, architecture and real Keycloak 26.7 Testcontainers evidence cover
  the secure path.
- Membership-conditioned public token projection using private API roles and
  per-application Keycloak role scopes. Browser tokens receive only
  same-application API audiences, `roles=[]` until business roles exist, and no
  native role, group or PII claims; real Keycloak/PostgreSQL evidence covers
  cross-application isolation and managed-scope drift reconciliation.
- Durable membership projection through a transactional PostgreSQL outbox,
  private per-application Keycloak markers, fail-closed activation, scoped
  operation status and explicit reconciliation, with outcome and duration
  metrics that contain no application or user identifiers.
- Authenticated membership-grant intent API with a stable integration audience,
  explicit `membership:write` machine scope, target application derived from
  validated `azp`, idempotent PostgreSQL persistence in `PENDING`, strict
  payload isolation, and real Keycloak/PostgreSQL evidence.
- Durable global-account disablement with recent TOTP administration,
  idempotency, fail-closed last-administrator protection based on effective
  Keycloak roles, session revocation, PostgreSQL evidence and safe retry.
- Public password-recovery completion with a 15–64 character/common-password
  policy, single-use proof consumption, fail-secure Keycloak identity
  revalidation, session revocation before credential reset, durable
  password-change notification, sanitized administrative events, and real
  PostgreSQL/Keycloak evidence.
- Public password-recovery request foundation with account-enumeration-resistant
  responses, eligible local-credential lookup through supported Keycloak Admin
  REST APIs, hashed 15-minute proofs, per-destination and per-source limits, and
  transactional durable email delivery that erases the sensitive link.
- Hosted local login proof using OIDC Authorization Code with PKCE, generic
  authentication failures, email-only user profiles, five-failure progressive
  brute-force protection, retained authentication events, and no business API
  authorization without Membership.
- Public local-registration and email-verification edge with generic
  anti-enumeration responses, stable application identifiers, strict JSON and
  2 KiB body limits, per-source rate limiting, bounded response timing,
  default-deny routes, non-cacheable output, and verification proofs kept out of
  HTTP request URLs.
- Durable email-verification core with 30-minute single-use challenges, hashed
  256-bit secrets, resend and attempt limits, atomic email outbox scheduling,
  address-bound Keycloak activation, and erasure of delivered sensitive links.
- Internal pending-local-identity foundation with normalized email, 15–64
  character password baseline, fail-closed per-application policy, disabled and
  unverified Keycloak users, idempotent replay, opaque account references, and
  mutually isolated least-privilege service accounts.
- Per-application self-registration policy with fail-closed default,
  idempotent administrative configuration, PostgreSQL isolation, and local
  harness coverage.
- Durable transactional email outbox for password-change notifications, with
  PostgreSQL leasing, controlled retry and terminal failure diagnostics,
  sanitized admin/auditor access, SMTP adapter, and real Mailpit evidence.
- Machine client configuration and Keycloak projection with Service Accounts,
  confidential one-response secret issuance, and no browser flows or secret
  persistence.
- Confidential BFF client projection with exact browser redirects, Authorization
  Code plus PKCE `S256`, one-response Keycloak secret issuance, non-cacheable
  output, and no IdentityHub secret persistence.
- Public SPA client configuration with environment-aware exact redirect/origin
  validation, transactional PostgreSQL persistence, and Keycloak Authorization
  Code projection requiring PKCE `S256` without a client secret.
- Local development harness for SLICE-001 with loopback-only PostgreSQL and
  Keycloak containers, idempotent realm bootstrap, hosted device login with TOTP,
  protected token storage, and an administrative API smoke test.
- Approved SLICE-001 with the `ClientApplication` aggregate,
  idempotent administrative registration and lookup, PostgreSQL/Flyway ownership,
  admin/auditor authorization, safe HTTP errors, correlation, auditing, metrics,
  and real Keycloak/PostgreSQL Testcontainers evidence.
- Approved SLICE-000 administrative boundary with real Keycloak 26.7 hosted TOTP login, isolated PostgreSQL 17 databases, exact environment and audience validation, role-based access, readiness, correlation, Flyway-owned append-only access auditing, and Testcontainers contract evidence.
- Approved MIG-003 controlled reset assessment with TDD evidence, negative guardrail proofs, residue verification, historical secret baseline, and rollback strategy.
- Initial `identityhub-service` Spring Boot 4 foundation with typed runtime environment, injectable UTC clock, fail-closed stateless security, minimal health probes, and sanitized request correlation.
- Repository engineering guardrails with project-specific `AGENTS.md`, ArchUnit, Checkstyle, JaCoCo, pinned GitHub Actions, Gradle Wrapper validation, Gitleaks, and dependency review.
- Approved MIG-002 platform compatibility assessment with supported toolchain evidence, production-mode Keycloak and PostgreSQL probes, Testcontainers validation, least-privilege administration, hosted TOTP verification, and the Auto Radar integration decision.
- ADR accepting Java 21, Spring Boot 4.1, Gradle 9.6.1, Keycloak 26.7, PostgreSQL 17, managed framework dependencies, and a single Spring Boot 4.1 line for the MVP Integration Mode.
- Explicit Gradle test-count gate for the 91-test legacy baseline, making accidental omissions visible in the canonical build.
- Approved migration strategy defining controlled legacy replacement, preparatory increments, vertical MVP slices, testing and security gates, data handling, Keycloak boundaries, outbox entry, rollback, and human approval workflow.
- ADR establishing `communication` as the canonical capability name and partially superseding the former `notification` module name.
- Approved pre-refactor technical baseline covering the current build, tests, architecture, security, event reliability, guardrails, target gaps, and migration classification.
- Product marketing context distinguishing approved positioning, target audiences, pain points, differentiation hypotheses, objections, voice, proof gaps, and stage-based goals.
- ADR catalog recording the accepted engine, identity, distribution, realm, modularity, outbox, token claims, local console, branding, and data-ownership decisions.
- Outcome-driven roadmap covering the MVP delivery sequence, product horizons, future security, B2B, ecosystem, commercialization, messaging, and evidence-based scaling.
- Integration Mode contract covering the Spring Boot starter, runtime and declarative configuration, token validation, typed client, local console, diagnostics, compatibility, and tests.
- Security model covering trust boundaries, threats, OAuth/OIDC profiles, tokens, sessions, credentials, MFA, browser and infrastructure controls, verification, release gates, and incident response.
- Target architecture defining IdentityHub distribution modes, runtime planes, modular monolith boundaries, data ownership, integration flows, consistency strategy, deployment, observability, and testing.
- MVP behavioral specification covering application isolation, identity lifecycle, authentication, authorization flows, tokens, sessions, platform administration, branding, integration, notifications, auditing, and acceptance scenarios.
- Initial product vision defining the IdentityHub purpose, audiences, value proposition, product principles, MVP direction, and intended outcomes.
- Versioned archive under `docs/archive/v0.3.0/` for the superseded specification, ADRs, and implementation guides, preserving the abandoned baseline while the new product documentation is defined.
- Initial domain modeling for authentication core:
  - `Username` and `UsernameType`
  - `RawPassword` and `EncodedPassword`
  - `Credentials` (replacing `LoginData`)
- First unit tests for domain value objects and login composition.
- Product and architecture documentation:
  - `identityhub-spec.md` with feature IDs (`IH-001` ... `IH-005`)
  - ADRs for deployment modes, source of truth, and token/session model
- `IH-001` application flow:
  - `RegisterUserUseCase`
  - `RegisterUser` use case contract
  - `RegisterUserCommand`
  - `UserRepository` port
  - `PasswordHasher` port
  - `UserRegistrationPolicy` port
  - `UserAlreadyExistsException`
- REST layer for `IH-001`:
  - `UserController` (`POST /users/register`)
  - `GlobalExceptionHandler` with `ApiErrorResponse`
  - response mapping via `UserResponseMapper` and `UserResponse`
- Optional persistence adapters:
  - in-memory repository fallback
  - optional JPA adapter (`JpaUserRepositoryAdapter`) with mapper/entity/repository port
- Security infrastructure for password hashing:
  - `BCryptPasswordHasher`
  - `SecurityProperties` + configuration binding
  - support configuration for `Clock`
- Test support and application tests for `IH-001`:
  - `RegisterUserUseCaseTest`
  - fixtures in `support/*`
- `IH-002` initial confirmation flow:
  - `ConfirmUserUseCase` + `ConfirmUser` contract
  - `ConfirmUserCommand`
  - `VerificationToken` and `NotificationMethod`
  - registration/confirmation events:
    - `UserRegisteredPendingVerificationEvent`
    - `UserConfirmedEvent`
  - notification port/listeners:
    - `UserNotifier`
    - `UserNotificationListeners`
- Token generation abstraction:
  - `VerificationTokenGenerator` port
  - `RandomVerificationTokenGenerator` adapter
- Confirmation flow tests (`IH-002`):
  - success path (activate + publish event)
  - invalid code
  - expired token
  - invalid user status
  - user not found
  - assert for saved user final state (`ACTIVE` + null token)
  - assert for published event payload (`UserConfirmedEvent.username`)
  - register flow assertions for token generation invocation rules
  - controller-level confirmation endpoint tests (`/users/confirm`) for success and not-found error contract
- Notification messaging contracts:
  - `NotificationMessage`
  - `NotificationChannel` and `NotificationChannels`
  - `EmailSender`, `SmsSender`, and `UserNotifier` ports
  - `EmailRenderer`, `RenderedEmail`, and `EmailDelivery` separation
  - email/SMS template identifiers
- Email notification infrastructure:
  - `DefaultEmailSender`
  - `TemplatedEmailRenderer`
  - `UserVerificationCodeEmailTemplate`
  - SMTP delivery adapter placeholder (`SmtpEmailDelivery`)
  - HTML verification-code email template
- Real SMTP email delivery support:
  - Spring Mail integration
  - configurable SMTP host, port, credentials, TLS, and timeouts
  - asynchronous notification execution with virtual threads
  - public REST error mapping for email delivery failures
- Welcome email notification after successful user confirmation:
  - `UserWelcomeEmailTemplate`
  - HTML welcome email template
- Mailpit setup guide for local SMTP verification flows.
- Minimal application logging for registration, confirmation, notification dispatch, SMTP delivery, and REST exception handling.
- Configurable SMTP retry attempts and retry backoff for transient email delivery failures.
- SMS notification foundation with renderer, template, sender, delivery port, and local logging delivery adapter.
- Reorganized messaging infrastructure packages by responsibility (`sender`, `delivery`, and `template`) and channel.
- Real Twilio SMS delivery support:
  - `TwilioSmsDelivery` adapter
  - Twilio SDK dependency
  - configurable SMS provider credentials via environment variables
- Username resolution port and infrastructure adapter:
  - `UsernameResolver` application output port
  - `LibPhoneNumberUsernameResolver` adapter for email detection and E.164 phone normalization
  - `libphonenumber` dependency isolated in infrastructure
- Environment property binding tests for local secrets and provider credentials:
  - `IDENTITY_HUB_API_SECRET`
  - Twilio SMS and WhatsApp provider variables
- WhatsApp notification rendering foundation:
  - `WhatsappDelivery` output port
  - `WhatsappRenderer` output port
  - `RenderedWhatsapp` rendered message value
  - `WhatsappMessageTemplate` template identifiers
  - `TemplatedWhatsappRenderer`
  - `UserVerificationCodeWhatsappTemplate`
  - WhatsApp template image asset
- Explicit WhatsApp channel selection support through `NotificationMethod.WHATSAPP` and `NotificationChannels.whatsapp()`.
- PostgreSQL-backed JPA persistence setup:
  - PostgreSQL JDBC driver
  - Flyway PostgreSQL support
  - default `jpa` profile configuration
  - initial `users` table migration
- Real WhatsApp verification-code delivery foundation:
  - OpenFeign client for WhatsApp API integration
  - `WhatsappSender` output port
  - `DefaultWhatsappSender`
  - `DefaultWhatsappDelivery`
  - configurable WhatsApp API URL
  - configurable media base URL for WhatsApp message assets
- Unit test coverage for WhatsApp verification-code notification rendering, routing, sender orchestration, and delivery endpoint selection.
- Transactional use case decorators for registration and confirmation flows so domain events can be published inside an active transaction.
- WhatsApp provider request DTO to isolate the external HTTP contract from the internal rendered message model.

### Changed
- Established `0.4.0-SNAPSHOT` as the post-abandonment development line and upgraded the wrapper to Gradle 9.6.1.
- Reorganized the build as a multi-project foundation with `identityhub-service` as the first deployable unit.
- Corrected the pre-refactor test assessment: all 30 suites and 91 tests were already executed; eight Windows-shortened XML report names were excluded by the original `TEST-*.xml` measurement.
- Renamed `LoginData` to `Credentials` and aligned semantics for authentication input.
- Simplified `UsernameType` support to `EMAIL`, `PHONE`, and `EXTERNAL_ID` (removed regional document-specific types from core).
- Refactored `IH-001` orchestration:
  - duplicate check before hashing
  - `Clock` injection for deterministic timestamps
  - domain creation delegated to `User.register(...)`
  - initial status delegated to configurable `UserRegistrationPolicy`
- Refined domain construction APIs:
  - `Username.create(...)`
  - `RawPassword.create(...)`
- Kept `User` builder internal/private and moved registration defaults to domain method.
- Refined architecture package consistency:
  - moved output ports to `application.ports.output`
  - added explicit use case wiring via `infrastructure.usecase.UseCaseConfiguration`
- Replaced `HelloController` bootstrap endpoint with `UserController` for feature-driven delivery.
- Simplified and aligned unit tests to current command flow (`username` + `rawPassword`).
- Refined domain unit tests with one-scenario assertions for failure paths and added boundary coverage for `UsernameType` validations.
- `RegisterUserUseCase` now:
  - publishes event when user is pending verification with active token
  - delegates token generation to `VerificationTokenGenerator` instead of creating time/random values directly in VO
- `User.register(...)` now receives pre-generated `VerificationToken` from application layer.
- `UserController` now includes confirmation endpoint (`GET /users/confirm`) with command orchestration.
- `GET /users/confirm` now returns the activated `UserResponse` with `200 OK` instead of an empty `201 Created` response.
- Test suite updated for deterministic clock handling in confirmation scenarios.
- REST error mapping updated to include `UserNotFoundException` as `404 Not Found` with `ApiErrorResponse`.
- Notification delivery flow now separates:
  - message intent (`NotificationMessage`)
  - logical channels (`NotificationChannels`)
  - template rendering (`EmailRenderer`)
  - rendered email value (`RenderedEmail`)
  - provider delivery (`EmailDelivery`)
- Notification providers are resolved by infrastructure configuration instead of being carried by notification messages.
- Messaging packages were reorganized to clarify ports, senders, renderers, templates, delivery, and Spring configuration.
- Moved `SmtpEmailDelivery` to `infrastructure.messaging.delivery` to align infrastructure delivery adapters with the output delivery port.
- Verification notification emails now receive formatted expiration timestamps from the infrastructure listener instead of formatting dates inside the domain token.
- Application use cases now publish domain/application events through a `DomainEventPublisher` output port instead of depending directly on Spring.
- Domain exceptions for verification-token validation and registration-confirmation status moved to the domain layer.
- Renamed REST user response model from `RegisteredUserResponse` to `UserResponse` so registration and confirmation can share the same response contract.
- Updated `identityhub-spec.md` to reflect the implemented IH-002 scope and document `IH-006` as the future outbox/retry notification feature.
- Simplified `UsernameType` to represent only real persisted username types (`EMAIL` and `PHONE`).
- `RegisterUserUseCase` and `ConfirmUserUseCase` now resolve raw usernames through `UsernameResolver` before repository interaction.
- Welcome notifications now derive the delivery method from the resolved username type.
- `Username` no longer depends on external phone parsing libraries and now remains a pure domain value object.
- Renamed the infrastructure username resolver to `PhoneEmailUsernameResolver` to describe its responsibility instead of its implementation detail.
- Updated the project specification to include SMS confirmation delivery when the resolved username type is `PHONE`.
- `MessageTemplates` now carries email, SMS, and WhatsApp template identifiers while preserving the email/SMS constructor for existing callers.
- Renamed the multi-channel notification option from `BOTH` to `ALL`.
- Moved rendered email and SMS message values under `application.ports.output.messaging.renderers.*`.
- Kept `NotificationChannels.all()` limited to currently wired email and SMS delivery; WhatsApp remains an explicit channel until the sender/delivery adapter is implemented.
- `RenderedWhatsapp` now carries structured provider-ready content (`recipientNumber`, media type, media URL, and caption) instead of a raw JSON string.
- WhatsApp media type moved to the application messaging model so output ports no longer depend on infrastructure template classes.
- JPA user persistence mapping now uses `UserEntity` and the `users` table naming expected by the PostgreSQL migration.
- Flyway migration location now points to the shared `classpath:db/migration` folder.
- Phone-based registration now generates WhatsApp verification tokens for the current verification-code delivery flow.

### Fixed
- Normalized `ClientApplication.registeredAt` to microsecond precision so the
  creation response and PostgreSQL round-trip expose the same timestamp.
- Normalized password validation error message in `RawPassword`.
- Fixed Spring context bootstrap issue for configuration properties binding.
- Fixed `RegisterUserUseCaseTest` setup for deterministic token generation after introducing `VerificationTokenGenerator`.
- Fixed Thymeleaf verification email template resolution by keeping template names independent from the `.html` suffix.
- Fixed JPA user mapping to preserve verification-token state during persistence and rehydration.
- Fixed local SMTP development config to use explicit IPv4 loopback (`127.0.0.1`) for SSH tunnel compatibility.
- Prevented duplicate stacktraces when asynchronous notification delivery fails.
- Fixed SMS provider bean wiring by binding nested notification provider properties through `NotificationProperties`.
- Removed hardcoded Twilio recipient and credential defaults from SMS delivery/configuration.
- Removed the default fallback value for `IDENTITY_HUB_API_SECRET`, requiring it to be supplied explicitly by the environment.
- Fixed application context tests to provide an explicit fake API secret during test bootstrap.
- Fixed stale/corrupted notification text that had replaced connective words with `recipientNumber` in comments, messages, and tests.
- Fixed WhatsApp renderer template resolution error message to report the missing WhatsApp template.
- Fixed test compatibility after adding WhatsApp template selection to `MessageTemplates`.
- Fixed Flyway dependency alignment for Spring Boot 3.3.11 and PostgreSQL 17 compatibility.
- Fixed application context bootstrap for WhatsApp Feign client tests by providing a test API URL.
- Fixed WhatsApp notification architecture so application output ports do not depend on infrastructure-owned media types.
- Fixed WhatsApp media message validation to require media URLs for non-text messages.
- Fixed transactional event listener execution by moving transaction boundaries from `@Bean` factory methods to use case execution decorators.
- Fixed WhatsApp media request serialization to send `number`, lowercase `mediaType`, `mediaUrl`, and `caption` as expected by the external API.

### Removed
- Removed the abandoned active implementation, including proprietary registration and confirmation, local credential hashing, legacy user persistence, messaging providers, templates, profiles, migrations, and their exclusive tests. The historical state remains available through Git history, tag `v0.3.0`, and `docs/archive/v0.3.0/`.

## [0.1.0] - 2026-05-12
### Added
- Minimal Spring Boot bootstrap baseline for a fresh restart:
  - Gradle wrapper and Java 21 toolchain
  - `IdentityHubApplication`
  - `HelloController` (`GET /`)
  - base `application.yml`
  - context-load test
- Dependency reset to minimal stack for TDD-first rebuild:
  - `spring-boot-starter-web`
  - `spring-boot-starter-test`
