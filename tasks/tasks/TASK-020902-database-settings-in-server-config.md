---
schema: 2
id: TASK-020902
title: Read the database URL, credentials and pool size from ServerConfig
type: task
status: done
parent: STORY-0209
module: poker-server
estimate: S
tier: haiku
review: standard
files_touched: 3
labels: [server, persistence, config]
depends_on: []
verify:
  - ./gradlew :poker-server:test --tests '*ServerConfigTest'
  - ./gradlew :poker-server:check -PrequireDocker=true
---

## Goal

`ServerConfig` carries the database URL, user, password and pool size, each overridable by an
environment variable and defaulted to the local development database.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/config/ServerConfig.kt` | modify |
| `poker-server/src/main/resources/application.conf` | modify |
| `poker-server/src/test/kotlin/duels/poker/server/config/ServerConfigTest.kt` | modify |

## Scope

- Four new `val`s on the data class, after `port`: `databaseUrl: String`,
  `databaseUser: String`, `databasePassword: String`, `databasePoolSize: Int`.
- Twelve new companion constants, following the existing `DEFAULT_ / _KEY / _ENV` naming:

  | Constant | Value |
  | --- | --- |
  | `DEFAULT_DATABASE_URL` | `jdbc:postgresql://localhost:5432/poker_duels` |
  | `DEFAULT_DATABASE_USER` | `poker` |
  | `DEFAULT_DATABASE_PASSWORD` | `poker` |
  | `DEFAULT_DATABASE_POOL_SIZE` | `8` |
  | `DATABASE_URL_KEY` / `_USER_KEY` / `_PASSWORD_KEY` / `_POOL_SIZE_KEY` | `database.url`, `database.user`, `database.password`, `database.poolSize` |
  | `DATABASE_URL_ENV` / `_USER_ENV` / `_PASSWORD_ENV` / `_POOL_SIZE_ENV` | `DATABASE_URL`, `DATABASE_USER`, `DATABASE_PASSWORD`, `DATABASE_POOL_SIZE` |

- Keep `from` short with one private companion helper, and use it for `port` too — the precedence
  is unchanged, so the four existing port tests keep their assertions:

  ```kotlin
  private fun resolve(
      config: ApplicationConfig,
      env: (String) -> String?,
      envName: String,
      key: String,
      default: String,
  ): String = env(envName) ?: config.propertyOrNull(key)?.getString() ?: default
  ```

- `databasePoolSize` parses with `requireNotNull(raw.toIntOrNull()) { … }`, exactly like `port`: a
  non-numeric pool size is a startup error, not a silent default.
- **Do not validate the URL's shape.** A JDBC URL that PostgreSQL rejects is the driver's error to
  report at connection time, with a better message than a prefix check would give.
- The defaults, the `application.conf` block and `docker-compose.yml` (`TASK-020909`) describe the
  same local database. Add to `application.conf`:

  ```hocon
  database {
      url = "jdbc:postgresql://localhost:5432/poker_duels"
      user = "poker"
      password = "poker"
      poolSize = 8
  }
  ```

- Extend the class KDoc: these are development credentials for a container that listens on
  localhost, and production secrets arrive by environment variable in `EPIC-07`.

## Out of scope

- Opening a connection, a pool or a driver — `TASK-020907`. This ticket produces strings and an
  integer and nothing else. `ServerConfig.kt` imports nothing from `java.sql` or `com.zaxxer`.
- Running migrations — `TASK-020908`.
- Removing the plaintext development password, or reading a secret from a file — `EPIC-07`.
- Any assertion about a real database. This ticket's tests need no Docker and must not need any.

## Tests

`ServerConfigTest`, JUnit 5. Every test passes an explicit `env` lambda; no test may read the real
process environment.

**Two existing tests change, and only in their `env` argument.** `theEnvironmentVariableOverridesTheConfig`
and `theEnvironmentVariableOverridesTheShippedFile` currently pass `{ "9001" }`, a lambda that
answers *every* variable name — which now also answers `DATABASE_URL` and `DATABASE_POOL_SIZE`.
Change both to name-aware lambdas, `{ name -> if (name == ServerConfig.PORT_ENV) "9001" else null }`,
and leave their `assertEquals(9001, serverConfig.port)` assertions exactly as they are. No other
existing test changes, no assertion is removed or weakened.

New tests:

| Test | Proves |
| --- | --- |
| `readsTheDatabaseUrlFromTheConfig` | `MapApplicationConfig("database.url" to "jdbc:postgresql://db:5432/x")` with `env = { null }` ⇒ `databaseUrl` is that string |
| `theEnvironmentVariableOverridesTheDatabaseUrl` | same config, `env` answering only `DATABASE_URL` with `"jdbc:postgresql://env:5432/y"` ⇒ that value wins |
| `fallsBackToTheDefaultDatabaseSettings` | `from(MapApplicationConfig()) { null }` gives `DEFAULT_DATABASE_URL`, `DEFAULT_DATABASE_USER`, `DEFAULT_DATABASE_PASSWORD`, `DEFAULT_DATABASE_POOL_SIZE`, and does not throw |
| `rejectsAPoolSizeThatIsNotANumber` | `assertThrows<IllegalArgumentException>` for `MapApplicationConfig("database.poolSize" to "many")` |
| `loadsTheDatabaseSettingsFromTheShippedApplicationConf` | `ServerConfig.load { null }` reads the `database` block from `application.conf` and yields the four default values |

## Acceptance criteria

- [ ] `ServerConfigTest.readsTheDatabaseUrlFromTheConfig` passes
- [ ] `ServerConfigTest.theEnvironmentVariableOverridesTheDatabaseUrl` passes
- [ ] `ServerConfigTest.fallsBackToTheDefaultDatabaseSettings` passes
- [ ] `ServerConfigTest.rejectsAPoolSizeThatIsNotANumber` passes
- [ ] `ServerConfigTest.loadsTheDatabaseSettingsFromTheShippedApplicationConf` passes
- [ ] The four pre-existing port tests still assert the same values; only the two `env` lambdas
      named above changed, and no assertion was removed
- [ ] `ServerConfig.kt` contains no reference to `System.getenv` outside the default value of the
      `env` parameter, and no `java.sql`, `javax.sql`, `com.zaxxer` or `org.flywaydb` import
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
