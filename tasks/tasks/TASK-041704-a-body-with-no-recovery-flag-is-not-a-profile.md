---
schema: 2
id: TASK-041704
title: A body with no recovery flag is not a profile, and the flag is that player's
type: task
status: backlog
parent: STORY-0417
module: web-client
estimate: XS
tier: haiku
review: standard
files_touched: 1
labels: [client, profile, recovery, test]
depends_on: [TASK-041703]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test -- src/profile/profile.test.ts 2>&1 | grep -qE 'Test Files +1 passed \(1\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- src/profile/profile.test.ts 2>&1 | grep -qE 'Tests +13 passed \(13\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'refuses a body with no recovery flag, and one whose flag is not a boolean'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'reads the recovery flag the server sent, in both of its states'
  - cd web-client && npm run check
---

## Goal

Two tests in `profile.test.ts` hold `hasRecoveryEmail` to the rule `profile.ts` states: required on
the wire with no default, and read from the body rather than defaulted, copied or guessed.

## Files

| File | Action |
| --- | --- |
| `web-client/src/profile/profile.test.ts` | modify |

Read, and do not edit:

- `web-client/src/profile/profile.ts` — `PlayerProfile`, `profileFromBody` and `readProfile` as
  `TASK-041703` left them.
- `web-client/src/profile/profile-fixture.ts` — `meBody(overrides)`, which takes a
  `Record<string, unknown>` so a test can pass a wrong-typed field or delete one.
- `docs/protocol.md` *Profile endpoint* — the `hasRecoveryEmail` row.

## Scope

- **Two tests, appended to the merged `describe`**, in the idiom the eleven already there use. No
  production file changes and no helper is added: `meBody` already builds a bendable body.
- **The refusal test drives `readProfile`**, not `profileFromBody` alone, so the assertion is that
  the read answers `unavailable` — the observable outcome — rather than that a private parser
  answered `null`.
- **The both-directions test uses two different bodies in one test**, `true` and `false`, because one
  body cannot tell a copy from a constant.

## Out of scope

- **Any production change.** If a test here reddens against `TASK-041703`'s merged code, that is the
  finding: report it rather than editing `profile.ts`, which this ticket's `Files` table excludes.
- **Anything on a screen.** `TASK-041712`.
- **`deviceRouteLive`.** Its own tests are merged and are not touched; this diff appends and edits
  nothing.

## Tests

`web-client/src/profile/profile.test.ts`, appended inside the merged `describe("the profile read")`.
**11 merged tests become 13.**

| Test | Proves |
| --- | --- |
| `refuses a body with no recovery flag, and one whose flag is not a boolean` | Three reads in one test, each over a `200` body. First: `meBody()` with `hasRecoveryEmail` **deleted** answers `{ kind: "unavailable" }`. Second: `meBody({ hasRecoveryEmail: "true" })` — the string, not the boolean — also answers `unavailable`. Third, the presence half: an unmodified `meBody()` answers `{ kind: "profile" }`, so the two refusals are refusals and not a fixture that never parses at all. `profile.ts` documents *no defaults*, and without this a parser that defaulted the field to `false` would pass every other test in this file |
| `reads the recovery flag the server sent, in both of its states` | Two reads in one test, over `meBody({ hasRecoveryEmail: true })` and `meBody({ hasRecoveryEmail: false })`, asserting `profile.hasRecoveryEmail` is `true` and then `false`. **Both bodies also set `deviceRouteLive` to the opposite value** — `false` with `true`, then `true` with `false` — so a parser that copied `deviceRouteLive` into `hasRecoveryEmail` reddens here. One fixture default cannot tell a copy from a constant, and neither can two fixtures that agree |

**No `try` anywhere in the added code, and no `expect()` inside one** — a failing assertion is itself
a throw, and a `try` around one turns a red test green.

## Acceptance criteria

- [ ] `cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'refuses a body with no recovery flag, and one whose flag is not a boolean'`
      — passes, over all three bodies including the one that must succeed
- [ ] `cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'reads the recovery flag the server sent, in both of its states'`
      — passes, over two bodies whose two booleans **disagree** with each other
- [ ] `cd web-client && NO_COLOR=1 npm run --silent test -- src/profile/profile.test.ts 2>&1 | grep -qE 'Tests +13 passed \(13\)'`
      and `… | grep -qE 'Test Files +1 passed \(1\)'` — **exactly thirteen**: the eleven merged plus
      these two. The merged eleven are pinned by this **count**, never by their names. Both lines,
      because a collection error prints a *passing* `Tests` count with no failure line at all
- [ ] `cd web-client && npm run check` exits 0
- [ ] Every merged test in `profile.test.ts` passes unchanged. No assertion moves and none is weakened
- [ ] No file outside the one listed differs
- [ ] Every command in `verify:` exits 0

## Proof

Run each step, record what you measured, and revert it. **These mutations are experiments, not
changes**, and they are the reason this ticket exists at all: `TASK-041703`'s probe reached green
with `profile.test.ts` untouched, which is the proof that no gate held this field before now. Each
step edits `web-client/src/profile/profile.ts`, which is **outside this ticket's `Files` table** —
mutate, measure, revert, and do not leave a line of it changed. `git status` must be clean of it
before you open the PR.

1. **Delete the `typeof … hasRecoveryEmail === "boolean"` clause** from `profileFromBody`, keeping
   the copy into the returned object. Predict: `refuses a body with no recovery flag, and one whose
   flag is not a boolean` reddens **alone**. Record the count.
2. **Default it**: `hasRecoveryEmail: body.hasRecoveryEmail === true` with no `typeof` guard.
   Predict: the same test reddens — an absent field becomes `false` rather than a refusal. Record
   whether the second test moves too; if it does not, that is expected and is why they are separate.
3. **Copy the neighbour**: return `deviceRouteLive` in the `hasRecoveryEmail` slot. Predict: `reads
   the recovery flag the server sent, in both of its states` reddens **alone**, because that test's
   two bodies deliberately disagree on the two booleans. If it stays green, the two bodies agree
   somewhere and the test is vacuous — fix the fixture, not the assertion.
4. **Constant `false`** in the returned object. Predict: the second test reddens on its `true` half.
5. **Constant `true`.** Predict: the second test reddens on its `false` half. Steps 4 and 5 are run
   as a pair because a single-direction assertion cannot tell a constant from a read.

> **A red run names a prefix, not a set.** Vitest stops reporting past its first hard failure.
>
> **Expect `The 'NO_COLOR' env is ignored due to the 'FORCE_COLOR' env being set.` on stderr.** It is
> wrong — measured, 0 escape bytes with it and 74 without — so every `grep -qE` needs it.

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket.

## Notes

**Why this is not part of `TASK-041703`.** That ticket is `atomic:`, and an `atomic:` item may only
name a merged gate that **fails** on the smaller commit (`ADR-0068`). Its probe ran the whole
`client` job green with `profile.test.ts` untouched, so no such gate exists for this file — the same
reasoning that made `TASK-041641` a ticket of its own rather than a seventh row on `TASK-041616`, and
`ADR-0070` §4's propagation exception excludes *adds a test* in as many words.
