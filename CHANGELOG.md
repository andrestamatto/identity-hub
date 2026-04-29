# Changelog

All notable changes to this project will be documented in this file.

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
