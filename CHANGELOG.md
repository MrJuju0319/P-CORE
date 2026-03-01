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
