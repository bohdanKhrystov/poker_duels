---
schema: 2
id: TASK-040623
title: An unknown device, alone, is refused too
type: task
status: done
parent: STORY-0406
module: poker-server
estimate: XS
tier: haiku
review: standard
files_touched: 1
labels: [server, auth, device]
depends_on: [TASK-040609]
verify:
  - ./gradlew :poker-server:test --tests 'duels.poker.server.http.DeviceRouteTest'
  - ./gradlew :poker-server:detekt
---

## Goal

`DELETE /api/me/device` must refuse a request carrying **only an unresolvable device id** — the
`Identity.UnknownDevice` branch — and a test must reach it.

## Why this exists

`TASK-040609` built the route with an exhaustive five-branch `when` over `Identity`. Five of the six
tests it shipped reach `Session`, `Refused`, `Device` and `Anonymous`. **None reaches
`UnknownDevice`**, and both the coder and the reviewer confirmed it independently.

The near miss is instructive. `aValidTokenBesideAnUnknownDeviceStillRevokes` *does* present an
unresolvable device id — but it pairs it with a **valid token**, so `IdentityResolver.resolve`
returns on the token branch before the device is ever looked up. The fixture looks like it covers the
case and cannot.

Reaching the branch needs both halves at once: **no token, and a device id absent from the
directory.** Today that branch could return `204`, or throw, and every test in the file would stay
green.

`TASK-040609` was right not to add a seventh test — its Tests table names exactly six, and a finding
outside a ticket becomes a new ticket. This is that ticket.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/test/kotlin/duels/poker/server/http/DeviceRouteTest.kt` | modify |

## Scope

One test. A `DELETE /api/me/device` carrying an `X-Device-Id` that is **not** in the directory and
**no** `Authorization` header at all. Assert the same refusal the other device-side tests assert —
`401`, an empty body, and `revokeCalls` empty.

The point of the empty `revokeCalls` assertion is that a route which revokes for an unknown device
would be revoking for a player that does not exist; a status-only assertion would not see it.

## Out of scope

- Changing `DeviceRoutes.kt`. The branch already exists and its behaviour is already correct; this
  ticket makes that a fact a gate holds.
- The six tests `TASK-040609` added. Leave them exactly as they are — in particular
  `aValidTokenBesideAnUnknownDeviceStillRevokes`, which reaches a different branch on purpose.
- A malformed-`Authorization` test. `TASK-040609` forbids it and its acceptance criteria say why: at
  this route a malformed header refuses identically whether it parses as an invalid token or as
  absent, so such a test cannot distinguish the bug it would exist to catch.

## Tests

| Test | Proves |
| --- | --- |
| `anUnknownDeviceWithNoSessionIsRefused` | a request with an unresolvable device id and no `Authorization` header answers `401` with an empty body, and `revokeCalls` is empty |

## Acceptance criteria

- [ ] The new test exists and passes.
- [ ] The six tests `TASK-040609` added are unchanged.
- [ ] `DeviceRoutes.kt` is not edited.
- [ ] Every command in `verify:` exits 0.

## Proof

Make the `Identity.UnknownDevice` arm answer `204` instead of refusing — the new test goes red while
all six from `TASK-040609` stay green. Revert. **Before this ticket that mutation turns nothing red**,
which is the whole reason it exists.

**Run it.** Ten `## Proof` sections in this run were wrong or incomplete when actually executed,
including one in `TASK-040609` itself, whose prediction of a clean `204` was actually an
`IllegalStateException` reddening two tests rather than one.

## Notes

**The near miss is why this ticket exists.** `aValidTokenBesideAnUnknownDeviceStillRevokes` presents
an unresolvable device id — but pairs it with a **valid token**, so `IdentityResolver.resolve` returns
on the token branch before the device is looked up. The fixture reads as covering `UnknownDevice` and
structurally cannot. Reaching that branch needs both halves at once: no token **and** an absent
device.

**The coder's mutation and the ticket's were not the same, and the reviewer ran the ticket's.**
Returning a `PlayerId` from the `UnknownDevice` arm hits `checkNotNull(token)` and throws, so the test
reddened via an exception. The ticket specifies answering `204` — a route quietly succeeding for a
device nobody knows — which is the defect that matters. Applied, it fails the new test on the status
assertion with all nine others green. The gate is sound; the substitute was a different route to the
same red.

**`revokeCalls.isEmpty()` is the assertion that carries it.** A route revoking for an unknown device
would be revoking for a player that does not exist, and a status-only check would not see it.

