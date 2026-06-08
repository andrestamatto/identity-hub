# Changelog

All notable changes to this project will be documented in this file.

## [Unreleased]
### Added
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
  - response mapping via `UserResponseMapper` and `RegisteredUserResponse`
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

### Changed
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

### Fixed
- Normalized password validation error message in `RawPassword`.
- Fixed Spring context bootstrap issue for configuration properties binding.
- Fixed `RegisterUserUseCaseTest` setup for deterministic token generation after introducing `VerificationTokenGenerator`.

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
