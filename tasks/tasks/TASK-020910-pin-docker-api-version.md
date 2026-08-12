---
schema: 2
id: TASK-020910
title: The test JVM speaks a Docker API version modern daemons still accept
type: task
status: done
parent: STORY-0209
module: poker-server
estimate: XS
tier: haiku
review: light
files_touched: 2
labels: [server, build, test-harness, docker]
depends_on: [TASK-020901]
verify:
  - ./gradlew :poker-server:test --tests '*DockerApiVersionTest'
  - ./gradlew :poker-server:check
---

## Goal

`docker-java` defaults to Docker API version **1.32**. Docker Engine 29 raised its minimum
supported API version to **1.40** and rejects anything older:

```
Status 400: {"message":"client version 1.32 is too old.
             Minimum supported API version is 1.40, please upgrade your client"}
```

Testcontainers catches that rejection while probing for a daemon and reports it as
`Could not find a valid Docker environment`, which is actively misleading: the daemon was found,
was reachable, and answered. Only the version was wrong. Every database test in `STORY-0209`
fails this way on a current Docker, and the error sends you looking at sockets instead.

Found while bringing Docker up on a developer machine running Engine 29.5.2.

## Files

| File | Action |
| --- | --- |
| `poker-server/build.gradle.kts` | modify |
| `poker-server/src/test/kotlin/duels/poker/server/db/DockerApiVersionTest.kt` | create |

## Scope

- In the existing `tasks.withType<Test>` block — the one that already forwards
  `poker.requireDocker` — forward a system property `api.version` with the value **`1.41`**, so
  the forked test JVM gets it. A `-D` on the Gradle command line reaches the daemon, **not** the
  test JVM, which is why this has to be task configuration.
- `1.41` and not something newer: it is the lowest version at or above Engine 29's 1.40 minimum,
  so it stays compatible with the older daemons on CI runners. Requesting a version *newer* than
  a daemon supports fails just as hard in the other direction.
- Say **why** in a comment — the failure this prevents is unrecognisable from the symptom, and
  the next person to see "could not find a valid Docker environment" needs the pointer.

Do not touch `PostgresTestSupport`, the Testcontainers version, or the workflow file. This ticket
changes build configuration and nothing else.

## Tests

`DockerApiVersionTest` needs no Docker and must pass on any machine — it asserts the build wiring,
not the daemon.

| Name | Asserts |
| --- | --- |
| `theTestJvmIsToldWhichDockerApiVersionToSpeak` | `System.getProperty("api.version")` is not null — the property actually reaches the forked JVM, which is the thing that silently was not happening |
| `theConfiguredApiVersionIsAtLeastTheModernMinimum` | it parses as `major.minor` and is **≥ 1.40**, so lowering it back under the floor fails here rather than in a confusing Testcontainers probe |

Parse the value rather than comparing the string to `"1.41"`: the point is the floor, not the
literal, and a string equality test would have to be edited every time the pin moves.

## Done

Both `verify:` commands exit 0, and `DockerApiVersionTest` passes with no Docker running.
