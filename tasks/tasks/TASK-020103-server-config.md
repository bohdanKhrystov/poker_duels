---
schema: 2
id: TASK-020103
title: Read every tunable from one typed ServerConfig
type: task
status: done
parent: STORY-0201
module: poker-server
estimate: S
tier: haiku
review: standard
files_touched: 2
labels: [server, config, foundation]
depends_on: [TASK-020101]
verify:
  - ./gradlew :poker-server:test --tests '*ServerConfigTest'
  - ./gradlew :poker-server:check
---

## Goal

One typed `ServerConfig` carries the server's tunables, built from a Ktor `ApplicationConfig`
with each value overridable by an environment variable and defaulted when neither supplies it.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/config/ServerConfig.kt` | create |
| `poker-server/src/test/kotlin/duels/poker/server/config/ServerConfigTest.kt` | create |

Read, do not modify: `poker-engine/src/main/kotlin/duels/poker/engine/random/Rng.kt` (house style
for KDoc and explicit `public`), `poker-server/build.gradle.kts`.

## Scope

- Package `duels.poker.server.config`. The file holds a single top-level class, so ktlint's
  `standard:filename` rule requires the name `ServerConfig.kt` — it already matches.
- The shape, exactly:

  ```kotlin
  public data class ServerConfig(val port: Int) {
      public companion object {
          public const val DEFAULT_PORT: Int = 8080
          public const val PORT_KEY: String = "server.port"
          public const val PORT_ENV: String = "PORT"

          public fun from(
              config: ApplicationConfig,
              env: (String) -> String? = { name -> System.getenv(name) },
          ): ServerConfig
      }
  }
  ```

- Precedence inside `from`, in this order: the environment lookup `env(PORT_ENV)`, then
  `config.propertyOrNull(PORT_KEY)?.getString()`, then `DEFAULT_PORT`. An absent value falls
  through to the next source and never throws.
- A value that is present but not an integer is a startup error: use
  `requireNotNull(raw.toIntOrNull()) { ... }`, which throws `IllegalArgumentException`. Do not
  silently fall back to the default — a typo'd `PORT` that quietly serves 8080 is worse than a
  crash.
- `env` is a parameter, not a call to `System.getenv` scattered through the body. That is the
  whole point of the ticket: it makes the override testable without mutating the process
  environment, which is why the story rules out HOCON's `${?PORT}` substitution.
- KDoc on the class and on `from`, saying that this is the *only* place the server reads its
  environment, and that `ADR-0013`'s grace period and `ADR-0011`'s database URL land here as
  further `val`s with their own key, env name and default.
- Imports needed: `io.ktor.server.config.ApplicationConfig`. The test also needs
  `io.ktor.server.config.MapApplicationConfig`.

## Out of scope

- Loading `application.conf` from the classpath and a `load()` helper — `TASK-020104`.
- Any field other than `port`. The grace period is `STORY-0208`, the database URL is
  `STORY-0209`.
- Validating the port range: `0` is a legitimate Ktor port meaning "any free port", so a range
  check would be wrong here.
- Trimming or rejecting a blank environment variable — not ticketed, do not add it.
- Touching `Application.kt` or the build file; neither exists in this ticket's budget.

## Tests

`ServerConfigTest`, JUnit 5, package `duels.poker.server.config`. Build the config argument with
`MapApplicationConfig("server.port" to "…")` and pass an explicit `env` lambda every time — no
test may depend on the real process environment.

| Test | Proves |
| --- | --- |
| `readsThePortFromTheConfig` | `from(MapApplicationConfig("server.port" to "7000")) { null }.port == 7000` |
| `theEnvironmentVariableOverridesTheConfig` | same config, `env = { "9001" }` ⇒ `port == 9001` |
| `fallsBackToTheDefaultWhenNothingIsSet` | `from(MapApplicationConfig()) { null }.port == ServerConfig.DEFAULT_PORT` and the call does not throw |
| `rejectsAPortThatIsNotANumber` | `assertThrows<IllegalArgumentException>` for `MapApplicationConfig("server.port" to "eighty-eighty")` |

## Acceptance criteria

- [ ] `ServerConfigTest.readsThePortFromTheConfig` passes
- [ ] `ServerConfigTest.theEnvironmentVariableOverridesTheConfig` passes
- [ ] `ServerConfigTest.fallsBackToTheDefaultWhenNothingIsSet` passes
- [ ] `ServerConfigTest.rejectsAPortThatIsNotANumber` passes
- [ ] `ServerConfig.kt` contains no reference to `System.getenv` outside the default value of the
      `env` parameter
- [ ] `./gradlew :poker-server:check` exits 0, ktlint and detekt included
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
