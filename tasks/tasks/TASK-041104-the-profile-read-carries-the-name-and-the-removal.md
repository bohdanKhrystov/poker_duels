---
schema: 2
id: TASK-041104
title: The profile read carries the name, and whether one was removed
type: task
status: backlog
parent: STORY-0411
module: web-client
estimate: S
tier: haiku
review: standard
files_touched: 3
labels: [client, profile, parse, identity]
depends_on: [TASK-041103]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test 2>&1 | grep -qE 'Tests +385 passed \(385\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'takes the name the server sent, and the null of a player who has none'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'takes the removed bit both ways, from two bodies'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'answers unavailable when the body says nothing about a removed name'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'answers unavailable when the name is neither a string nor null'
  - cd web-client && npm run check
---

## Goal

`PlayerProfile` carries `displayName: string | null` and `displayNameRemoved: boolean`, read from
`GET /api/me` and refused when either is missing or wrong-typed; and the body-to-profile parse is a
named exported function, so `TASK-041106` can reuse it for the `200` from `PUT /api/me/name`.

## Files

| File | Action |
| --- | --- |
| `web-client/src/profile/profile.ts` | modify — two fields, the parse, and one extracted export |
| `web-client/src/profile/profile.test.ts` | modify — four tests added |
| `web-client/src/profile/profile-fixture.ts` | modify — two defaults on `aProfile` and `meBody` |

Read, not edited: `docs/protocol.md` (the profile endpoint's table — two rows, `displayName` and
`displayNameRemoved`), `docs/adr/ADR-0053-the-profile-says-the-name-was-removed.md` §1.

## Scope

```ts
export interface PlayerProfile {
  readonly playerId: string;
  readonly coinBalance: number;
  /** The name the player chose, or `null` when they hold none. Never derived, never defaulted. */
  readonly displayName: string | null;
  /** `true` only when they hold no name **and** one was removed from them (`ADR-0052`). */
  readonly displayNameRemoved: boolean;
}

export function profileFromBody(body: unknown): PlayerProfile | null;
```

- **Both fields are required on the wire and the parse says so.** A body without
  `displayNameRemoved`, or with a `displayName` that is neither a string nor `null`, answers
  `unavailable` — the same refusal the parse already gives a body without `coinBalance`. This is the
  client half of `ADR-0053` §1's rule that the field carries **no default value**: the server emits
  it for every player, so a client that filled one in would be inventing the one bit that
  distinguishes *removed* from *never set*.
- `readProfile` keeps its three outcomes and its signature; the body validation moves into
  `profileFromBody`, which returns `null` for anything that is not a profile. `readProfile` maps
  `null` to `{ kind: "unavailable" }` exactly as today.
- The fixture gains `displayName: null` and `displayNameRemoved: false` on **both** `aProfile` and
  `meBody`. Those are the ordinary state of a first visit and the values almost every existing test
  wants; a test that asserts either one passes it explicitly.
- The two `null` states are recorded in the KDoc where a reader will meet them:
  `displayName === null && !displayNameRemoved` is *never set*;
  `displayName === null && displayNameRemoved` is *removed by an operator*. Both are `null` names,
  and only the player themselves can tell them apart.

## Out of scope

- Rendering any of it. **A refusal, not an omission:** what the strip prints where a name would be
  is `DEC-051`, and the strip is `TASK-041115`.
- `opponentDisplayName` — `TASK-041105`, one file over.
- Any client-side rule about what a name may contain. `ADR-0029` §3 says the character rule is the
  one most likely to move, and a client that restated it would go stale silently while looking
  correct. The server refuses; the client renders the refusal (`TASK-041111`).
- Re-reading the profile after a name is set. The surface states what the server returned
  (`TASK-041110`); nothing refreshes the strip, and nothing here should.

## Tests

`web-client/src/profile/profile.test.ts`, describe block `"the profile read"`. Four added; the four
existing tests keep their names and their assertions, and pass unchanged because the fixture now
supplies both fields.

| Test | Proves |
| --- | --- |
| `takes the name the server sent, and the null of a player who has none` | **Two bodies in one test**: `meBody({ displayName: "Ada Lovelace" })` parses to that exact string, and `meBody({ displayName: null })` parses to `null`. Fails against a parse that hardcodes `null`, against one that hardcodes a placeholder, and against one that reads `playerId` into the name — one body could not tell a copied field from a constant |
| `takes the removed bit both ways, from two bodies` | `meBody({ displayNameRemoved: true })` parses to `true` and `meBody({ displayNameRemoved: false })` to `false`. Fails against `displayNameRemoved: false` written as a literal in the parse, which is the shortcut that would make the notice unreachable for everybody |
| `answers unavailable when the body says nothing about a removed name` | A **literal** body `{ playerId: "p-1", coinBalance: 0, displayName: null }` — built by hand, because a builder that always supplies the field cannot express its absence — answers `{ kind: "unavailable" }`. Fails against `?? false` and against `typeof x === "boolean" \|\| x === undefined` |
| `answers unavailable when the name is neither a string nor null` | `meBody({ displayName: 42 })` answers `unavailable`. Fails against a parse that casts without checking, which would put `42` on a screen where a person's name belongs |

Four tests added to 381, so the suite reports **385**.

## Acceptance criteria

- [ ] `the profile read > takes the name the server sent, and the null of a player who has none`
      passes, from two bodies in the one test
- [ ] `the profile read > takes the removed bit both ways, from two bodies` passes
- [ ] `the profile read > answers unavailable when the body says nothing about a removed name`
      passes
- [ ] `the profile read > answers unavailable when the name is neither a string nor null` passes
- [ ] The four pre-existing tests in `profile.test.ts` pass with their names and assertions
      unchanged
- [ ] `profileFromBody` is exported from `profile.ts` and `readProfile` calls it
- [ ] `grep -rl 'displayName' web-client/src/` lists exactly three files: `profile/profile.ts`,
      `profile/profile.test.ts` and `profile/profile-fixture.ts` — nothing renders it yet
- [ ] `npm run --silent test` reports `Tests  385 passed (385)`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
