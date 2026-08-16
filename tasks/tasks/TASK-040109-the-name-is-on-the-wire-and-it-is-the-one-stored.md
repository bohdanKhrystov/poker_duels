---
schema: 2
id: TASK-040109
title: The name is on the wire, and it is the one stored
type: task
status: ready
parent: STORY-0401
module: poker-server
estimate: S
tier: haiku
review: standard
files_touched: 2
labels: [server, http, read-path, tests, identity]
depends_on: [TASK-040108]
verify:
  - ./gradlew :poker-server:test --tests '*ProfileDtosTest.anUnnamedProfileEncodesTheFieldAsNull'
  - ./gradlew :poker-server:test --tests '*PostgresProfileReadsTest.aProfileWithNoNameReadsBackNull' -PrequireDocker=true
  - ./gradlew :poker-server:test --tests '*ProfileDtosTest'
  - ./gradlew :poker-server:test --tests '*PostgresProfileReadsTest' -PrequireDocker=true
---

## Goal

Two claims, each proven with two distinct inputs: the field is always present on the wire — as a
string when there is a name, as `null` when there is not — and the string it carries is the one the
`player` row holds.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/test/kotlin/duels/poker/server/protocol/http/ProfileDtosTest.kt` | modify |
| `poker-server/src/test/kotlin/duels/poker/server/db/PostgresProfileReadsTest.kt` | modify |
| `poker-server/src/test/kotlin/duels/poker/server/protocol/http/ProfileDtoFixtures.kt` | read — the builder, whose `displayName` default is exactly what these tests must not rely on |

## Scope

- Four tests added, two per file. Nothing existing moves.
- **Every one passes `displayName` explicitly**, including the `null` case. A value asserted only at
  the builder's default cannot be told apart from a constant, and `null` is the default.
- The JSON assertions read the encoded text, as the neighbouring tests in `ProfileDtosTest` already
  do, so *present as `null`* is distinguishable from *absent*.
- In `PostgresProfileReadsTest`, the name is written straight into the `player` row by the existing
  fixture helper — no write path exists yet, and this ticket must not invent one.

## Out of scope

- `PUT /api/me/name` — `TASK-040115`. Nothing here writes a name through an endpoint.
- The opponent's name on a duel line — `STORY-0402`.

## Tests

`ProfileDtosTest`

| Test | Proves |
| --- | --- |
| `aProfileEncodesTheNameItWasGiven` | a profile built with `displayName = "Élodie"` encodes with `"displayName":"Élodie"` in the JSON text |
| `anUnnamedProfileEncodesTheFieldAsNull` | a profile built with an explicit `displayName = null` encodes with `"displayName":null` **present** in the text — not omitted |

`PostgresProfileReadsTest`

| Test | Proves |
| --- | --- |
| `aProfileReadsBackTheNameItsRowHolds` | a `player` row stored with `bob` reads back `displayName == "bob"` |
| `aProfileWithNoNameReadsBackNull` | a row whose `display_name` is SQL `NULL` reads back `null`, in the same class as the test above so the field is shown to vary |

## Acceptance criteria

- [ ] All four tests above pass
- [ ] Each of the four passes its `displayName` explicitly rather than accepting the builder's
      default
- [ ] `anUnnamedProfileEncodesTheFieldAsNull` asserts on the JSON **text** and fails if the property
      is omitted rather than encoded as `null`
- [ ] `aProfileReadsBackTheNameItsRowHolds` uses a name that is not the empty string and not a
      value any other test in the file uses
- [ ] Every test already in both files passes unchanged
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
