# Changelog

All notable changes to this project will be documented in this file.

## [Unreleased]
### Added
- Initial domain modeling for authentication core:
  - `Username` and `UsernameType`
  - `RawPassword` and `EncodedPassword`
  - `LoginData`
- First unit tests for domain value objects and login composition.

### Changed
- Reorganized `LoginData` from `domain/entities` to `domain/valueobjects` to align with value-object semantics.
- Simplified `LoginDataTest` to validate `LoginData` invariants directly without false positives caused by invalid setup objects.
- Refined domain unit tests with one-scenario assertions for failure paths and added boundary coverage for `UsernameType` validations.

### Fixed
- Normalized password validation error message in `RawPassword`.

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
