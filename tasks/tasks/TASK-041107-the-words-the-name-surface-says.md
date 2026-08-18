---
schema: 2
id: TASK-041107
title: The words the name surface says, and which of them leave a way back
type: task
status: ready
parent: STORY-0411
module: web-client
estimate: S
tier: haiku
review: standard
files_touched: 2
labels: [client, profile, copy, identity, moderation]
depends_on: [TASK-041106]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test 2>&1 | grep -qE 'Tests +397 passed \(397\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'says the four sentences ADR-0052 shipped, and no fifth'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'never says a name is taken, and never names a holder'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'gives each refusal a sentence of its own'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'leaves a way back from a refused name and a conflicting one, and from nothing else'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'says a name can be taken away, before anything is sent'
  - cd web-client && npm run check
---

## Goal

One module holds every sentence the name surface can say — the removal notice, the permanence line
and one sentence per refusal — and answers which refusals leave the form worth using again.

## Files

| File | Action |
| --- | --- |
| `web-client/src/profile/name-text.ts` | create |
| `web-client/src/profile/name-text.test.ts` | create |

Read, not edited: `docs/adr/ADR-0052-a-takedown-is-told-to-the-player-it-happened-to.md` §§2 and 7
(the shipped strings), `web-client/src/profile/set-name.ts` (`SetNameOutcome`),
`web-client/src/table/rejection-text.ts` (how this codebase writes a text module).

## Scope

Three constants, two functions. The three ADR-shipped strings are quoted here in full because they
are the deliverable, not a paraphrase of it:

```ts
export const NAME_REMOVED_HEADING = "Your display name was removed.";

export const NAME_REMOVED_BODY =
  "A person running Poker Duels removed it — not a bug, and not another player. " +
  "That name cannot be used again, by you or by anyone. " +
  "Choose a new one whenever you like.";

export const PERMANENCE_LINE =
  "A name is chosen once. You cannot change it later, and it can be taken away.";

export function refusalSentence(kind: Exclude<SetNameOutcome["kind"], "named">): string;
export function mayTryAgain(kind: Exclude<SetNameOutcome["kind"], "named">): boolean;
```

The five refusal sentences, verbatim:

| Outcome | Sentence | `mayTryAgain` |
| --- | --- | --- |
| `rejected` (`400`) | `That name cannot be used. Try another.` | `true` |
| `conflict` (`409`) | `That name is not available. Try another.` | `true` |
| `permanent` (`403`) | `You already have a display name. That choice is permanent and cannot be changed.` | `false` |
| `no-profile` (`401`) | `This browser has no profile. Reload the page and try again.` | `false` |
| `unavailable` | `That did not reach the server. Reload the page to see whether the name was set.` | `false` |

- **The em dash in `NAME_REMOVED_BODY` is `—` (U+2014) with a space either side**, as `ADR-0052` §2
  prints it. It is not a hyphen and not an en dash.
- **`conflict` may never say *taken*.** `ADR-0051` §2 answers `409` for a name held by somebody
  else, a blocked name and a name retired from **this very player**, and no answer says which. For
  the player most likely to see it, *taken* is a false statement about a string nobody holds and
  nobody ever can (`ADR-0052` §7).
- **`rejected` restates no rule.** *Up to 32 characters*, *no invisible characters* and the rest live
  on the server, and `ADR-0029` §3 says that rule is the one most likely to move; a client that
  printed it would go stale while looking correct.
- **`unavailable` does not invite a retry**, and that is deliberate rather than pessimistic: after a
  request whose outcome is unknown, the player cannot tell whether their one permanent name landed,
  and re-sending a *different* name is the one action that could waste it. Reloading answers the
  question from the server, which is `ADR-0002` applied to a failure.
- `mayTryAgain` is `true` for exactly two of five, which is `STORY-0411`'s *only `400` and `409`
  leave the form retryable* expressed as a function a test can call.
- No React, no JSX, no DOM. A `.ts` module of strings.

## Out of scope

- The word for a player who has **no** name. **A refusal, not an omission:** that is `DEC-051`,
  the product owner's, and it lands in this file in `TASK-041114`. Nothing here may guess it.
- Rendering any of this — `TASK-041108` and `TASK-041109`.
- Colour, weight and type. `EPIC-06` owns the language this composes; this ticket authors no class
  and edits no token. Nothing here touches `design/tokens/tokens.css`, so nothing needs copying into
  `web-client/src/styles/`.

## Tests

`web-client/src/profile/name-text.test.ts`, describe block `"the name surface's words"`.

**Every assertion types the expected sentence out as a literal.** A test that compares
`NAME_REMOVED_BODY` to `NAME_REMOVED_BODY`, or that asserts `refusalSentence("conflict")` merely
*contains* a word, proves nothing about the words that ship. These strings are golden.

| Test | Proves |
| --- | --- |
| `says the four sentences ADR-0052 shipped, and no fifth` | `NAME_REMOVED_HEADING` and `NAME_REMOVED_BODY` equal the literals above, character for character, and `NAME_REMOVED_BODY` splits on `". "` into exactly three sentences. Fails against a reworded sentence, a dropped one, a hyphen in place of the em dash, and a fifth sentence added |
| `never says a name is taken, and never names a holder` | `refusalSentence("conflict")` equals `That name is not available. Try another.`, and neither it nor any other refusal sentence contains `taken`, `someone`, `somebody`, `another player` or `already has it`. Fails against `ADR-0052`'s own example of the defect — reverting to *"That name is taken"* — and against a friendlier rewrite that names a holder |
| `gives each refusal a sentence of its own` | The five sentences are pairwise distinct (a `Set` of them has size `5`) and each equals its literal above. Fails against two branches sharing a sentence and against a `default:` clause swallowing a case |
| `leaves a way back from a refused name and a conflicting one, and from nothing else` | `mayTryAgain` is `true` for `rejected` and `conflict` and `false` for the other three, asserted for all five. Fails against `return true`, against `return kind !== "permanent"`, and against a `403` treated as retryable |
| `says a name can be taken away, before anything is sent` | `PERMANENCE_LINE` equals its literal above, including *and it can be taken away*. Fails against `ADR-0038`-era copy that promised permanence without it, which would contradict the notice a player may later read on the same screen |

Five tests added to 392, so the suite reports **397**.

## Acceptance criteria

- [ ] All five tests above pass under `describe("the name surface's words")`
- [ ] Each of the eight strings appears as a literal in `name-text.test.ts`, not as a reference to
      the module's own constant
- [ ] `grep -c 'taken' web-client/src/profile/name-text.ts` returns `1` — the permanence line's *it
      can be taken away*, and no refusal sentence
- [ ] `name-text.ts` contains no JSX and imports nothing from `react`
- [ ] `grep -c 'NO_NAME\|Anonymous\|Unnamed\|nameless' web-client/src/profile/name-text.ts` returns
      `0` — `DEC-051` is answered by `ADR-0058` (`No name`), and the string lands here in
      `TASK-041114`, not in this ticket
- [ ] `npm run --silent test` reports `Tests  397 passed (397)`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
