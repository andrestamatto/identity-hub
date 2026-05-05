# Changelog

All notable changes to this project will be documented in this file.

## [Unreleased]
### Added
- Social OAuth2 integration pipeline with provider abstraction:
  - `OAuth2ProviderClient` contract
  - `OAuth2SocialIdentityLoaderAdapter`
  - Google adapter/client implementation (`GoogleOAuth2ProviderClientAdapter`, token and userinfo clients)
  - Provider DTOs (`GoogleTokenResponse`, `GoogleUserInfoResponse`)
- Application policy boundary for social provider settings:
  - `SocialProviderPolicyPort`
  - `SocialProviderPolicy` DTO
  - `PropertiesSocialProviderPolicyAdapter`
- Dedicated configuration modules:
  - `PasswordAuthConfiguration`
  - `SocialAuthConfiguration`
  - `AuthWarningsConfiguration`

### Changed
- Refactored token contract to application boundary:
  - moved `TokenService` to `TokenServicePort`
  - security components now depend on port abstractions
- Renamed JWT implementation to explicit adapter role:
  - `JwtService` -> `JwtTokenServiceAdapter`
- Reorganized use case input boundaries for clarity:
  - `PasswordLogin` -> `PasswordLoginUseCasePort`
  - `SocialLogin` -> `SocialLoginUseCasePort`
  - `SocialLoginInput` -> `SocialLoginCommand`
- Standardized port naming in application layer:
  - `LoadExternalIdentityPort`
  - `LoadSocialIdentityPort`
  - `ResolveSocialUserPort`
- Updated social login properties API:
  - `filterProviderProperties(...)` -> `getProviderProperties(...)`
- Build/dependency cleanup:
  - removed duplicated Lombok declarations
  - removed unnecessary `spring-boot-starter-oauth2-client`
  - added `jsr305` for annotation metadata compatibility warnings

### Fixed
- Social login disabled flow now fails fast with consistent `503` behavior instead of falling through provider-resolution errors.
- Spring test context ambiguity for token service bean resolved after security-port refactor.
- OAuth2 provider test naming aligned with implementation (`GoogleOAuth2ProviderClientAdapterTest`).

### Removed
- Deprecated aggregate auth configuration class (`AuthenticatableConfig`) in favor of focused module configurations.
- Redundant `TokenConfiguration` bean factory after direct adapter-to-port wiring.

## [0.1.0] - 2026-04-29
### Added
- JWT authentication flow with claims: `sub`, `identity`, `identity_type`, `roles`, `permissions`.
- Configurable route authorization DSL:
  - `PERMIT_ALL`, `DENY_ALL`, `AUTHENTICATED`
  - `ROLE`, `PERM`, `ANY_ROLE`, `ANY_PERM`, `ANY_AUTHORITY`, `ALL_ROLE`, `ALL_PERM`, `HAS_IP`
- Built-in local identity store toggle via `identity-hub.local-identity-store.enabled`.
- Graceful startup behavior when no `LoadExternalIdentity` is available:
  - application still starts
  - `/auth/login` returns `503`
  - startup warning is logged.
- Unit and integration test coverage for security/configuration/authentication core paths.

### Changed
- Identity model generalized from email-centric to `identity`.
- Password model strengthened with `RawPassword` and `EncodedPassword`.
- Authorization rule parsing and application refactored into dedicated types (`ConfiguredAccessRule`, `AccessType`).
- Terminology/documentation changed from “fake persistence” to “built-in local identity store”.

### Removed
- Redundant `ExternalUser` + `UserMapper` layer; identity loading now returns `User` directly.
