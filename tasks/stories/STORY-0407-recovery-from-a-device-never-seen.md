---
id: STORY-0407
title: Recovery — signing in from a device that has never been seen
type: story
status: done
parent: EPIC-04
module: poker-server
labels: [server, auth, e2e]
depends_on: [STORY-0406]
---

## Goal

A browser that has never touched this site signs in with a handle and a password and is the same
player: the same coin balance, the same display name, the same duels. It is issued no device id and
leaves no orphan profile behind.

## Why

The claim is only worth building because of this. `ADR-0012` recorded the debt as *"a lost device is
a lost profile"*, and every story before this one is machinery; this is the story where the machinery
is shown to have paid it off. It is also the shape of the epic's definition of done.

## Design notes

- **`ADR-0027` path 1 short-circuits before path 3's minting**, so a recovery sign-in on a fresh
  browser creates no `player` row and issues no device id, and `Welcome.deviceId` is `null`
  ([`ADR-0030`](../../docs/adr/ADR-0030-a-claim-adds-a-credential-and-moves-nothing.md) §2). The
  assertion is a row count taken before and after, not an absence somebody eyeballed.
- **This is a scenario story.** Its weight is one test that drives the real server and the real
  database through the whole arc — anonymous duel, coin, name, sign-up, second client with a
  *different* device id and no shared storage, sign-in, read back — and asserts every fact it
  claims, rather than a suite of unit tests for machinery that already exists.
- **Sign-out on that device returns it to nothing**, which is correct: sign-out restores whatever
  the device had, and a never-seen device had no profile. The test asserts that too, because it is
  the case a reader assumes is broken.
- **P1 and P2 hold across the whole arc** — `STORY-0406`'s helper is reused, not reimplemented.
- The two-client half that runs through the *browser* is `STORY-0414`; this one is server-side, over
  HTTP and the socket, and can be proven without a client.

## Tasks

Nine, in a single dependency chain: each is startable when the one above it merges, and no two
startable tickets touch the same file. Seven of them build one scenario class, `RecoveryOnAFreshBrowserTest`,
step by step — the story's own note that its weight is *one* test that drives the whole arc. None is
`atomic:`; every ticket touches one file, except the first, whose two are forced by an overload
ambiguity Kotlin raises between an `internal` declaration and a `private` one of the same name.

| ID | Title | Status |
| --- | --- | --- |
| `TASK-040701` | The `device_binding` snapshot comes from one place | ready |
| `TASK-040702` | A duel the recovered account will remember | backlog |
| `TASK-040703` | The name and the password that make the account recoverable | backlog |
| `TASK-040704` | A browser never seen signs in, and the socket names no device | backlog |
| `TASK-040705` | The fresh browser reads back the same profile and the same duels | backlog |
| `TASK-040706` | The recovery leaves no row in either table a profile occupies | backlog |
| `TASK-040707` | A wrong password from the fresh browser issues no session | backlog |
| `TASK-040708` | Signing out returns the fresh browser to nothing, and the original device to itself | backlog |
| `TASK-040709` | One statement in the whole server creates a profile | backlog |

**The negative is gated twice, deliberately.** `TASK-040706` asserts it across the operations this
scenario performs — both tables a profile occupies after `ADR-0049`, byte-identical, plus a live-binding
count with two inputs. `TASK-040709` asserts it for write paths nobody has written yet, by reading
source text, because the coin invariant is structurally blind to an orphan profile (balance `0`, no
deltas: P1 holds, both P2 sums unmoved) and the endpoint enumeration sees a new route rather than a
new statement.

## Acceptance criteria

- [ ] A second client, bearing a different device id and no session, signs in and reads back the
      same `playerId`, the same balance, the same display name and the same duel list as the first.
- [ ] The `player` row count is unchanged by that sign-in.
- [ ] `Welcome.deviceId` is `null` on a socket whose identity came from a session.
- [ ] A sign-in with the wrong password from the new device is refused, and still creates nothing.
- [ ] Signing out on the new device leaves it with no profile, and the original device is unaffected.
- [ ] P1 and P2 hold at every step.

## Out of scope

- The password reset by email — `STORY-0416`. Recovery here means *I know my password and I am on a
  new device*.
- Anything a browser renders — `STORY-0412` and `STORY-0414`.
