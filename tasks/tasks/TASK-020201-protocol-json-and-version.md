---
schema: 2
id: TASK-020201
title: Give poker-server the serialization plugin, PROTOCOL_VERSION and one shared Json
type: task
status: done
parent: STORY-0202
module: poker-server
estimate: S
tier: haiku
review: light
files_touched: 3
labels: [server, protocol, serialization, build]
depends_on: []
verify:
  - ./gradlew :poker-server:test --tests '*ProtocolJsonTest'
  - ./gradlew build
---

## Goal

`poker-server` can compile `@Serializable` types, and every protocol frame in this story will be
encoded by one `Json` instance that writes defaults — the trap that would otherwise drop
`protocolVersion` from the handshake.

## Files

| File | Action |
| --- | --- |
| `poker-server/build.gradle.kts` | modify |
| `poker-server/src/main/kotlin/duels/poker/server/protocol/Protocol.kt` | create |
| `poker-server/src/test/kotlin/duels/poker/server/protocol/ProtocolJsonTest.kt` | create |

Read, do not modify: `poker-engine/build.gradle.kts` (the plugin line to copy),
`gradle/libs.versions.toml` (the `kotlinx-serialization-json` alias),
`poker-engine/src/main/kotlin/duels/poker/engine/log/HandLogJson.kt` (the `encodeDefaults` precedent).

## Scope

- `poker-server/build.gradle.kts`: add `kotlin("plugin.serialization")` to the `plugins` block
  under the existing `kotlin("jvm")`, and add `implementation(libs.kotlinx.serialization.json)` to
  `dependencies`. The Ktor bundle already drags the runtime in transitively; declaring it directly
  is what puts it on this module's **compile** classpath and what stops a Ktor version bump from
  removing it. Nothing else in the file changes — not the Ktor pin, not the test bundle.
- `Protocol.kt`, package `duels.poker.server.protocol`, exactly two public declarations:

  ```kotlin
  /** The wire protocol this build speaks. Bumping it breaks every older client, deliberately. */
  public const val PROTOCOL_VERSION: Int = 1

  public val protocolJson: Json = Json {
      encodeDefaults = true
      classDiscriminator = "type"
      ignoreUnknownKeys = false
      prettyPrint = false
  }
  ```

- KDoc on `protocolJson` must say *why* each of the four settings is there, in one line each:
  `encodeDefaults` because kotlinx omits a default-valued property and `Hello.protocolVersion` will
  have a default, so without it a handshake reaches the server with no version to check;
  `classDiscriminator` pinned rather than inherited from the library default because `STORY-0203`
  generates TypeScript that hard-codes this key; `ignoreUnknownKeys = false` because a frame with a
  field we do not know is a frame we do not understand; `prettyPrint = false` because frames are not
  for reading.
- No message type in this ticket. `Protocol.kt` declares no class.

## Out of scope

- `ClientMessage`, `ServerMessage`, `ProtocolError`, the codec — `TASK-020204` onward.
- Installing anything in `Application.kt`, or touching `ServerConfig`. `PROTOCOL_VERSION` is a
  compile-time constant of the protocol, never a tunable.
- Registering a `SerializersModule`: both hierarchies are sealed, so kotlinx resolves them without
  one.

## Tests

`ProtocolJsonTest`, JUnit 5, package `duels.poker.server.protocol`. Declare the probe at file
level, not nested in the class:

```kotlin
@Serializable
private data class Probe(val a: Int = 7)
```

| Test | Proves |
| --- | --- |
| `theProtocolVersionIsOne` | `PROTOCOL_VERSION == 1` |
| `defaultValuesReachTheWire` | `protocolJson.encodeToString(Probe.serializer(), Probe())` equals `{"a":7}` |
| `unknownKeysAreRefused` | decoding `{"a":1,"b":2}` as `Probe` throws `SerializationException` |

That `Probe` compiles and has a generated `serializer()` is itself the proof that the compiler
plugin is applied.

## Acceptance criteria

- [ ] `ProtocolJsonTest.theProtocolVersionIsOne` passes
- [ ] `ProtocolJsonTest.defaultValuesReachTheWire` passes
- [ ] `ProtocolJsonTest.unknownKeysAreRefused` passes
- [ ] `PokerServerModuleTest`, `ServerPluginsTest` and `HealthRouteTest` are not modified and still
      pass — this ticket adds a plugin and a dependency and changes no runtime behaviour
- [ ] `./gradlew build` exits 0 — every module, ktlint, detekt and
      `:poker-engine:checkNoDependencies` included
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
