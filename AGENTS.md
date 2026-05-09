# AGENTS.md

Guidance for coding agents working in this repository.

## Project Goal

This repository is being refactored from the legacy MassiveCraft Factions codebase into
PvPIndex Factions: a modern Paper factions plugin under the `com.gyvex.pvpindex.factions`
namespace, backed by Jaloquent persistence and optional TeamsAPI integration.

Treat the active PvPIndex implementation as the source of truth. Preserve familiar
Factions gameplay where it is intentionally carried forward, but do not reintroduce
MassiveCore/MassiveCraft framework dependencies.

## Active Code vs Legacy Code

- Active plugin code lives in `src/main/java/com/gyvex/pvpindex/factions`.
- Active tests live in `src/test/java/com/gyvex/pvpindex/factions`.
- Active runtime resources live in `src/main/resources`.
- The old MassiveCraft source tree under `src/com/massivecraft/factions` is legacy
  reference material. Read it only to understand behavior being ported.
- The root-level `plugin.yml` is legacy. Maven packages `src/main/resources/plugin.yml`.

When adding or changing functionality, implement it in the PvPIndex tree. Do not add new
classes under `src/com/massivecraft`.

## Architecture

- `PvPIndexFactions` is the Bukkit/Paper entry point and delegates lifecycle work to
  `Bootstrap`.
- `Bootstrap` owns startup and shutdown order:
  config, database, Vault, services, TeamsAPI adapters, engines, commands, PlaceholderAPI.
- Registries are simple holders populated during startup:
  `InfraRegistry`, `ServiceRegistry`, `EngineRegistry`, and `CommandRegistry`.
- Data access goes through `DatabaseManager`, `Repositories`, and repository classes in
  `data/repository`.
- Domain state is represented by Jaloquent model classes in `data/model`.
- Core behavior belongs in services or engines:
  services handle faction/member/invite/warp business operations, engines handle Bukkit
  listeners and scheduled behavior.
- Commands are thin adapters in `command/sub`; they should validate command input,
  call services/engines, and format user-facing messages.
- TeamsAPI classes in `api` are adapters around internal services. Internal behavior must
  not require TeamsAPI to be present.

## Refactor Rules

- Use package `com.gyvex.pvpindex.factions` for all new Java code.
- Remove MassiveCraft concepts as they are replaced: `MPlugin`, `MConf`, `MStore`,
  `Coll`, `Entity`, Massive command classes, and direct `com.massivecraft.*` imports.
- Keep soft integrations optional. The plugin should run standalone without TeamsAPI,
  Vault, WorldGuard, WorldEdit, PlaceholderAPI, dynmap, or EzEconomy.
- Prefer existing PvPIndex service/repository patterns over copying legacy architecture.
- Keep old gameplay semantics only when they fit the new service model. If behavior is
  intentionally different, make the new behavior explicit in config, messages, tests, or
  command output.
- Update `src/main/resources/plugin.yml`, `config.yml`, `database.yml`, and
  `messages.yml` when user-facing commands, permissions, settings, or text change.
- Avoid broad rewrites of unrelated legacy code. Port one behavior slice at a time and
  leave a clear tested path in the active tree.

## Build and Verification

This is a Maven project.

- Compile and run tests: `mvn test`
- Build shaded plugin jar: `mvn package`
- Run tests plus Checkstyle: `mvn verify`

The build targets Java 21 bytecode for Paper 1.21.4. The Maven `java.version` property may
reference a newer local JDK, but source compatibility should remain suitable for Paper's
Java 21 baseline.

Checkstyle runs during `verify` using `checkstyle.xml`. Keep Java files UTF-8, with no
trailing whitespace, no star imports, and lines at or below 130 characters unless the
Checkstyle exclusions apply.

## Testing Expectations

- Add or update JUnit 5 tests for service, repository, command, and model behavior touched
  by a change.
- Use Mockito consistently with the existing tests in `src/test/java`.
- For command changes, extend the relevant `Cmd*Test` or `CommandTestBase` pattern.
- For persistence changes, cover both model behavior and repository/database integration
  where feasible.
- If a change depends on a Bukkit server runtime and cannot be fully unit-tested, isolate
  the logic behind a service/repository boundary and test that boundary.

## Data and Persistence

- Jaloquent and query-builder dependencies are shaded into the plugin jar.
- H2 is the default embedded database and is opened in MySQL compatibility mode.
- MySQL/MariaDB is optional and configured through `database.yml`.
- Do not depend on a remote database in unit tests unless the user explicitly asks for it.
- Keep schema/table/column changes coordinated across models, repositories, and tests.
- Database files should live under the plugin data folder at runtime, not in the repo.

## Commands, Permissions, and Messages

- Public command aliases are defined in `src/main/resources/plugin.yml`.
- Permission nodes use the current `factions.cmd.*` and `factions.admin` style unless a
  compatibility task explicitly requires legacy nodes.
- User-facing text should come from `messages.yml` where practical.
- Messages use MiniMessage formatting. Preserve placeholder names when editing existing
  messages, and add tests around formatting-sensitive command output when possible.

## Dependencies and Integrations

- Paper API, TeamsAPI, Vault, WorldEdit, WorldGuard, and PlaceholderAPI are provided
  dependencies.
- Jaloquent, H2, HikariCP, and MySQL Connector/J are shaded and relocated under
  `com.gyvex.pvpindex.lib`.
- Vault, PlaceholderAPI, and TeamsAPI hooks must fail gracefully when the provider plugin
  is absent.
- Avoid static hard dependencies on optional plugins outside adapter/bootstrap code.

## Git and Workspace Safety

- This repository may contain user work in progress. Do not revert unrelated changes.
- Prefer small, focused patches.
- Do not delete legacy MassiveCraft files unless the task explicitly asks for cleanup or
  the active PvPIndex replacement is complete and verified.
- Keep generated build output in `target/` out of commits.

