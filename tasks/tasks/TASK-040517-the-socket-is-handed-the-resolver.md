---
schema: 2
id: TASK-040517
title: The socket's dependencies carry the resolver
type: task
status: backlog
parent: STORY-0405
module: poker-server
estimate: XS
tier: haiku
review: light
files_touched: 3
labels: [server, socket, wiring, identity]
depends_on: [TASK-040516]
verify:
  - ./gradlew :poker-server:test --tests 'duels.poker.server.session.SocketFixturesTest'
  - ./gradlew :poker-server:test --tests 'duels.poker.server.ServerComponentsTest' -PrequireDocker=true
  - ./gradlew :poker-server:check -PrequireDocker=true
---

## Goal

`SocketDependencies` carries an `IdentityResolver`, the shipping server builds it from the same
`AuthSessions` the HTTP routes use, and every existing socket test keeps compiling without being
edited.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/session/SocketDependencies.kt` | modify |
| `poker-server/src/main/kotlin/duels/poker/server/ServerComponents.kt` | modify |
| `poker-server/src/test/kotlin/duels/poker/server/session/SocketFixtures.kt` | modify |

## Scope

- `SocketDependencies` gains `val identities: IdentityResolver`, with **no default** — the type's
  own KDoc already says a caller that forgets a field must fail to compile rather than inherit
  something silently.
- `serverComponents` passes the **same** `IdentityResolver` instance it already gives the routes.
  One resolver over one session store, or the socket and HTTP become two rules again.
- `testDeps` gains `identities: IdentityResolver = IdentityResolver(NoAuthSessions, directory)` —
  **defaulted, and defaulted in terms of the `directory` parameter above it**, so every one of the
  socket suites keeps its current call verbatim and every one of them gets a resolver that agrees
  with the directory it was already using. `NoAuthSessions` means *no token was ever issued*, which
  is exactly today's world.
- No behaviour changes. `DuelSocket` does not read the new field yet.

## Out of scope

- The socket using it — `TASK-040518`.
- Any socket test. If one has to change, this ticket has done something it should not have.

## Tests

`SocketFixturesTest` — one method added.

| Test | Proves |
| --- | --- |
| `testDepsDefaultsAResolverOverItsOwnDirectory` | `testDeps(directory = d).identities.resolve(null, DeviceId("x"))` answers `UnknownDevice` before `d.resolve(DeviceId("x"))` and `Device` after — so the default is wired to *that* directory and not to a fresh one it made up |

`ServerComponentsTest` and every socket suite are the gate that nothing else moved; they are in
`verify:` and are **not** edited.

## Acceptance criteria

- [ ] `SocketFixturesTest.testDepsDefaultsAResolverOverItsOwnDirectory` passes
- [ ] `git diff --name-only` names exactly three files, and none of them is a socket test
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket.
