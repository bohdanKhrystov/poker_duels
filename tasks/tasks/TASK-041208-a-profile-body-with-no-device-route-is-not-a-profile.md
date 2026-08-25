---
schema: 2
id: TASK-041208
title: A profile body with no device route is not a profile, and the client asserts neither value
type: task
status: backlog
parent: STORY-0412
module: web-client
estimate: XS
tier: haiku
review: standard
files_touched: 1
labels: [client, profile, auth]
depends_on: [TASK-041207]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'reads the device route the server sent, in both of its states'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'refuses a body that says nothing about the device route'
  - cd web-client && npm run check
---

## Goal

`profileFromBody` is proved to *read* `deviceRouteLive` rather than to fill it, so the screen that
tells a player whether their device still signs in is reading a server fact and not a client
opinion.

## Files

| File | Action |
| --- | --- |
| `web-client/src/profile/profile.test.ts` | modify |

Read, and do not edit: `web-client/src/profile/profile.ts`;
`web-client/src/profile/profile-fixture.ts`;
[`ADR-0002`](../../docs/adr/ADR-0002-server-authoritative.md).

## Scope

- Three tests added to the existing describe block, in the shape the file's other parse tests use,
  built from `meBody({ … })` so the wire body is never spelled out by hand.
- No production file changes. `TASK-041207` shipped the parse; this ticket is the assertion that no
  gate there could carry, and it exists as its own ticket because **no merged gate names this file**
  — the `atomic:` probe reached green without it, which is the proof there is none (`ADR-0068`).
- No existing test in the file changes, and no assertion is weakened.

## Out of scope

- Anything about `hasRecoveryEmail` — `STORY-0417`.
- Rendering. `TASK-041217` puts a sentence on a screen; this is about the parse.
- The `no-profile` and `unavailable` outcomes of `readProfile`, which the file already covers.

## Tests

`web-client/src/profile/profile.test.ts`, in the existing describe block.

| Test | Proves |
| --- | --- |
| `reads the device route the server sent, in both of its states` | `profileFromBody(meBody({ deviceRouteLive: true }))` gives `true` **and** `profileFromBody(meBody({ deviceRouteLive: false }))` gives `false`, in one test. Two inputs, because the fixture's default is `true` and a parser that hard-coded `true` would pass any single-input test — which is exactly the mutation `TASK-041207`'s Proof showed nothing catches |
| `refuses a body that says nothing about the device route` | A body built by deleting the key — `const body = meBody(); delete body.deviceRouteLive;` — parses to `null`, not to a profile with `false`. An absent field must not become *your device does not sign in* |
| `refuses a device route that is not a boolean` | `meBody({ deviceRouteLive: "true" })` and `meBody({ deviceRouteLive: 1 })` each parse to `null`, asserted separately. The string is the one a lax server or a hand-rolled stub actually sends |

## Acceptance criteria

- [ ] `reads the device route the server sent, in both of its states` passes, asserting **both**
      `true` and `false` in the one test
- [ ] `refuses a body that says nothing about the device route` passes, with the key deleted rather
      than set to `undefined`
- [ ] `refuses a device route that is not a boolean` passes for both `"true"` and `1`
- [ ] Every pre-existing test in `profile.test.ts` passes unchanged
- [ ] `git diff --stat web-client/src/profile/profile.ts` is empty
- [ ] No file outside the one listed differs
- [ ] Every command in `verify:` exits 0

## Proof

1. In `profile.ts`, replace the parsed value with the literal `true`.
   **`reads the device route the server sent, in both of its states` reddens on the `false` half
   alone.** Nothing else moves. This is the mutation `TASK-041207` shipped ungated, and closing it is
   the whole reason this ticket is filed separately.
2. Drop the `typeof … === "boolean"` clause from the guard and keep the read.
   **`refuses a body that says nothing about the device route` reddens** — the parse succeeds and
   returns `undefined` where a boolean belongs — and `refuses a device route that is not a boolean`
   reddens with it. Two tests, which is what tells a missing guard from a wrong default.
3. Change the guard to `body.deviceRouteLive !== undefined`.
   **`refuses a device route that is not a boolean` reddens alone**, on both inputs; the delete test
   still passes. Run it: a truthiness or presence check is the cheap version of this guard and it
   admits the string `"false"` as `true`.
4. Set the fixture's default to `false` and leave the parser correct.
   **Nothing reddens**, because every test above names the value it wants. Record it — a fixture
   default at the value under test is the trap that makes a parse test vacuous, and this file is now
   immune to it.

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket.
