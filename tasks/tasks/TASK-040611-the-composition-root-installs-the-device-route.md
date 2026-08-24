---
schema: 2
id: TASK-040611
title: The composition root builds the bindings and installs the device route
type: task
status: done
parent: STORY-0406
module: poker-server
estimate: XS
tier: haiku
review: standard
files_touched: 3
labels: [server, wiring, composition-root]
depends_on: [TASK-040610]
verify:
  - ./gradlew :poker-server:test --tests 'duels.poker.server.DuelServerRoutesTest' -PrequireDocker=true
  - ./gradlew :poker-server:test --tests 'duels.poker.server.ServerComponentsTest' -PrequireDocker=true
  - ./gradlew :poker-server:check -PrequireDocker=true
---

## Goal

A server booted the way production boots it answers `DELETE /api/me/device`, so the route
`TASK-040609` and `TASK-040610` built is reachable rather than merely present.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/ServerComponents.kt` | modify |
| `poker-server/src/main/kotlin/duels/poker/server/Application.kt` | modify |
| `poker-server/src/test/kotlin/duels/poker/server/DuelServerRoutesTest.kt` | modify |

Read, and do not edit: `poker-server/src/main/kotlin/duels/poker/server/http/DeviceRoutes.kt`,
`poker-server/src/test/kotlin/duels/poker/server/ServerComponentsTest.kt`.

## Scope

- `ServerComponents` gains one property, `val bindings: DeviceBindings`, declared **last** so no
  existing named argument moves. `serverComponents(...)` builds it as
  `PostgresDeviceBindings(dataSource)` beside the other Postgres collaborators and passes it in the
  `ServerComponents(...)` call.
- No test constructs `ServerComponents` directly — every one goes through `serverComponents(...)` —
  so this property costs no call site outside this file. That is checked, not assumed:
  `grep -rn "ServerComponents(" poker-server/src` must name only `ServerComponents.kt`.
- `Application.duelServer` gains one line:
  `deviceRoutes(components.identities, components.credentials, components.bindings)`, placed
  **after** `profileRoutes(...)` and before `standingsRoutes(...)`, and the import to match.
- The KDoc on `duelServer` already lists what it installs; the device route joins that list.

## Out of scope

- Any change to `DeviceRoutes.kt`, `PostgresDeviceBindings.kt` or the `DeviceBindings` port.
- Any change to `profileRoutes`, `authRoutes` or `standingsRoutes` and their parameters.
- `docs/protocol.md` — `TASK-040612`.
- `ServerComponentsTest.kt` — **a named prohibition.** It asserts what the components *do*, and
  this ticket adds a property rather than changing one; if it fails, the ticket stops and reports.

## Tests

`DuelServerRoutesTest` — the class whose subject is *"a route the production root forgot to install
cannot slip through"*. Two methods added, in the shape the file already uses (a real database, the
whole `duelServer` composition).

| Test | Proves |
| --- | --- |
| `theDeviceRouteIsInstalled` | `DELETE /api/me/device` with **no** headers answers `401`, not `404`. `404` is what an uninstalled route answers, and it is the only outcome this test is built to reject; a Ktor application with no matching route returns `404 Not Found`, so the two are genuinely distinguishable here |
| `theDeviceRouteIsNotInstalledOnAnyOtherVerb` | `GET /api/me/device` answers `405` or `404` — assert `status != HttpStatusCode.OK` **and** `status != HttpStatusCode.Unauthorized`. Only `DELETE` was asked for (`ADR-0049` §5), and a `get(...)` typed beside the `delete(...)` would otherwise be invisible |

## Acceptance criteria

- [ ] `DuelServerRoutesTest.theDeviceRouteIsInstalled` passes
- [ ] `DuelServerRoutesTest.theDeviceRouteIsNotInstalledOnAnyOtherVerb` passes
- [ ] `grep -rn "ServerComponents(" poker-server/src` names exactly one file,
      `ServerComponents.kt`
- [ ] `Application.kt` contains exactly one call to `deviceRoutes`
- [ ] Every test already in `DuelServerRoutesTest` and `ServerComponentsTest` passes unchanged
- [ ] Every command in `verify:` exits 0

## Proof

Comment out the `deviceRoutes(...)` line in `Application.duelServer`.
`theDeviceRouteIsInstalled` reddens: the answer becomes `404` instead of `401`.
`theDeviceRouteIsNotInstalledOnAnyOtherVerb` stays **green** — `404` satisfies both of its
inequalities — which is why the first test asserts a specific status rather than merely "not OK".

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket.

## Notes

**The uninstalled-route case is caught, which is the whole point of a wiring ticket.**
`theDeviceRouteIsInstalled` asserts `401` specifically, not "not 404" — commenting out the
`deviceRoutes(...)` call makes it fail with `404`. `TASK-040517` was the same shape and nothing
observed the wiring there; here something does.

**The second test proves a narrower thing than its name suggests.** `GET /api/me/device` returning
neither OK nor Unauthorized passes for `404` *or* `405`, so it says nothing about whether the route
is installed — that is the first test's job. What it does catch is someone routing the wrong verb,
which is real if narrow.

**One `dataSource`, one `authSessions`, one `identities`.** Every Postgres collaborator receives the
same single value, so a throwaway would have to be constructed deliberately; `bindings` is exposed as
the `DeviceBindings` port rather than the concrete class, matching its siblings.

