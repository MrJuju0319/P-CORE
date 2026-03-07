# Changelog

## [0.1.0] - 2026-03-01
### Added
- Initial Maven-based `p-core` plugin scaffold for Paper/Folia.
- Shared async MariaDB service with HikariCP (`DbService`).
- Shared Redis service with Lettuce (`RedisService`) including get/set/incr/pubsub.
- Central `PcoreApi` service registration via Bukkit ServiceManager.
- Server identity service (`serverId`, `group`, `tags`) with Redis heartbeat support.
- Security service using HMAC-SHA256 + anti-replay nonce cache.
- Config registry service for plugin-specific config + namespace mapping.
- Default `config.yml` and `plugin.yml`.
- Unit tests for security signature/replay checks and namespace mapping.
- Full README documentation (installation, configuration, variables, API, commands).


## [0.1.1] - 2026-03-01
### Changed
- Expanded README with a detailed API integration guide for client plugins: managed/standalone bootstrap, async/Folia rules, and concrete usage examples for DbService, RedisService, IdentityService, SecurityService, and ConfigService.
- Extended README operational content with additional configuration guidance, variable explanations, and migration best practices for plugin developers.

## [0.1.2] - 2026-03-01
### Fixed
- Fixed MariaDB driver bootstrap by explicitly setting/loading `org.mariadb.jdbc.Driver` before creating the Hikari datasource.
- Improved plugin startup robustness: initialization errors are logged clearly and plugin is disabled cleanly instead of hard-crashing startup flow.
- Added `db.minIdle` configuration to prevent fixed-size pool warning and make `idleTimeout` effective when desired.

### Changed
- Enabled shaded release packaging (dependencies embedded) and relocated internal Hikari/Lettuce classes to reduce inter-plugin classpath conflicts.
- Expanded README with troubleshooting steps for `No suitable driver` and Hikari fixed-size warning, plus updated build/config guidance.

## [0.1.3] - 2026-03-01
### Fixed
- Fixed `ClassCastException` for downstream plugins casting DB query rows to `Map<String, Object>` by making `Row` implement `Map` semantics (extends `HashMap`) while preserving the p-core API contract.

### Changed
- Updated README DB API section with explicit row compatibility notes (`Row` + `Map`).

## [0.1.4] - 2026-03-01
### Changed
- Removed `server.identity.group` and `server.identity.tags` from the default `config.yml` example as requested; they remain optional and supported by the code with defaults.
- Updated README configuration sample and variable descriptions to reflect optional `group/tags`.
