---
schema: 2
id: TASK-041701
title: Two mailed addresses, and the opaque segment behind the slug
type: task
status: done
parent: STORY-0417
module: web-client
estimate: S
tier: sonnet
review: standard
files_touched: 2
labels: [client, routing, recovery]
depends_on: []
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test -- src/routing/screen.test.ts 2>&1 | grep -qE 'Test Files +1 passed \(1\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- src/routing/screen.test.ts 2>&1 | grep -qE 'Tests +7 passed \(7\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'names a screen from a slug followed by a secret nothing else reads'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'hands back the second segment, and nothing when there is none'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'keeps a token that would end a path segment or start a query whole'
  - test "$(grep -oF 'tokenFromHash' web-client/src/routing/screen.ts | wc -l | tr -d ' ')" = 1
  - test "$(grep -oF 'split' web-client/src/routing/screen.test.ts | wc -l | tr -d ' ')" = 0
  - cd web-client && npm run check
---

## Goal

`screenFromHash` names `"verify"` and `"reset"` from `#/verify/<token>` and `#/reset/<token>`,
`tokenFromHash` hands back that token, and both are asserted against segments that are **not** words
the switch already knows — the debt `TASK-041201` recorded and `STORY-0417` states as a requirement
on its own split.

## Files

| File | Action |
| --- | --- |
| `web-client/src/routing/screen.ts` | modify |
| `web-client/src/routing/screen.test.ts` | modify |

Read, and do not edit:

- [`ADR-0081`](../../docs/adr/ADR-0081-a-mailed-link-is-a-fragment-route-and-the-token-is-the-segment-behind-the-slug.md)
  §1 (the two links character for character), §5 (the signature this ticket lands, and the
  first-segment rule), §6 (a missing token is not an error address), §7 (the address grants nothing).
- [`STORY-0417`](../stories/STORY-0417-the-recovery-screens.md) §*A test this story must carry* — the
  four bullets below are that section, and a reviewer checks this ticket against it.
- `web-client/src/routing/use-screen.ts` — the caller. It is `TASK-041702`'s and is **not** edited
  here; `getSnapshot` keeps calling `screenFromHash` and nothing else.

## Scope

- **`Screen` gains `"verify"` and `"reset"`**, and nothing else. `ADR-0081` §5 fixes the union
  exactly; a third member for any screen this story turns out to have is another ticket's.
- **`screenFromHash` gains two `case` arms.** The first-segment split it already performs is
  unchanged — this ticket adds recognised words, it does not touch how the fragment is cut.
- **`hashForScreen` gains `"#/verify"` and `"#/reset"`**, as literals, beside the comment already in
  that function saying why a slug is a literal and not a heading read at runtime (`ADR-0076` §1).
  `ADR-0081` §5: **`hashForScreen` never emits a token.**
- **`tokenFromHash(hash: string): string | null` is new**, exported from the same module, framework
  free, touching no `window`:

  ```ts
  export function tokenFromHash(hash: string): string | null;
  ```

  It returns the **second** segment of the fragment, or `null` when the fragment has no second
  segment or that segment is empty. It is not told which screen it is on and does not check: a
  fragment is `#/<slug>` optionally followed by `/<token>`, and the token is a position.
- **KDoc on `tokenFromHash`** naming `ADR-0081` §4 for *why the token is in the fragment at all* —
  it crosses no wire, reaches no access log and no `Referer` — and §5 for *read once, at mount*. The
  comment says why, never what.

## Out of scope

- **Reading the token from `window.location`, replacing the fragment, or any `history` call.**
  `TASK-041702` adds the replace to `use-screen.ts`, which is the only module here allowed to touch
  `window` (`ADR-0076` §5).
- **Any screen component, any request, any copy.** Later tickets.
- **A slug for the *forgot password* flow.** Held on `DEC-081`; `ADR-0081` §3 fixes only these two,
  because a server writes them into a mail.
- **Changing the four merged tests in `screen.test.ts`.** They pass untouched — this diff appends
  three and edits none.
- **Widening `screenFromHash` past the first segment**, and any use of `URLSearchParams`. `ADR-0081`
  §2: a recovery link contains no `?` at all, and the fragment has one grammar.

## Tests

`web-client/src/routing/screen.test.ts`, appended inside the existing
`describe("the address of a screen")`. **4 merged tests become 7.**

Two token values are fixed here and must be used verbatim, because the whole point is that they
resemble nothing the switch knows:

```ts
const MAILED_TOKEN = "Xk93qQz7aa4bbCC1ddEE8ff2gg";
const AWKWARD_TOKEN = "ab?c=d";
```

| Test | Proves |
| --- | --- |
| `names a screen from a slug followed by a secret nothing else reads` | `screenFromHash("#/verify/" + MAILED_TOKEN)` is `"verify"` **and** `screenFromHash("#/reset/" + MAILED_TOKEN)` is `"reset"`. **Two** slugs, because one cannot tell a general rule from a special case for `verify`; and a token that is not a word, not a year and not a slug, because `#/duels/2026` cannot tell a first-segment match from a whole-fragment match that happens to know `2026`. Also `screenFromHash("#/verify")` is `"verify"` and `screenFromHash("#/reset")` is `"reset"` — `ADR-0081` §6's *a missing token is not an error address* |
| `hands back the second segment, and nothing when there is none` | `tokenFromHash("#/verify/" + MAILED_TOKEN)` and `tokenFromHash("#/reset/" + MAILED_TOKEN)` are both exactly `MAILED_TOKEN`; `tokenFromHash("#/reset")` is `null`; `tokenFromHash("#/reset/")` is `null`; `tokenFromHash("#/duels")` is `null`. The last is what stops the function being *the tail of any fragment* |
| `keeps a token that would end a path segment or start a query whole` | `tokenFromHash("#/reset/" + AWKWARD_TOKEN)` is exactly `"ab?c=d"`, and `screenFromHash("#/reset/" + AWKWARD_TOKEN)` is still `"reset"`. The guard against a reader that splits on the wrong thing: a `URLSearchParams` or a `split("?")` in either function returns `"ab"` here and fails. **Measured in this worktree**: jsdom leaves `window.location.hash` as `"#/reset/ab?c=d"`, so this input is one a real address bar produces |
| *(merged, unchanged)* `writes an address for every screen, and the first screen carries no fragment` | Extended in place with `hashForScreen("verify")` = `"#/verify"`, `hashForScreen("reset")` = `"#/reset"`, and the round trip back through `screenFromHash` for both. This is the one merged test this diff edits, and it gains lines only |

**No `try` anywhere in the added code, and no `expect()` inside one** — a failing assertion is itself
a throw, and a `try` around one turns a red test green.

## Acceptance criteria

- [ ] `cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'names a screen from a slug followed by a secret nothing else reads'`
      — passes, over **both** slugs, with `MAILED_TOKEN` as the second segment
- [ ] `cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'hands back the second segment, and nothing when there is none'`
      — passes, including the two `null` cases and `#/duels`
- [ ] `cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'keeps a token that would end a path segment or start a query whole'`
      — passes, asserting `"ab?c=d"` character for character
- [ ] `cd web-client && NO_COLOR=1 npm run --silent test -- src/routing/screen.test.ts 2>&1 | grep -qE 'Tests +7 passed \(7\)'`
      and `… | grep -qE 'Test Files +1 passed \(1\)'` — **exactly seven**: the four merged plus these
      three. The merged four are pinned by this **count**, never by their names. Both lines, because
      a collection error prints a *passing* `Tests` count with no failure line at all
- [ ] `test "$(grep -oF 'tokenFromHash' web-client/src/routing/screen.ts | wc -l | tr -d ' ')" = 1`
      — declared once, and called from nowhere inside this module. This reads the whole file,
      comments and KDoc included, so the name may not appear in prose here
- [ ] `test "$(grep -oF 'split' web-client/src/routing/screen.test.ts | wc -l | tr -d ' ')" = 0`
      — the test never re-implements the cut it is checking. It compares against literals
- [ ] `cd web-client && npm run check` exits 0. The suite reads **839 passed (839)** over **107**
      files: 836 merged plus these three
- [ ] Every merged test in `screen.test.ts` passes; only `writes an address for every screen…`
      changes, and it gains assertions rather than moving any
- [ ] No file outside the two listed differs
- [ ] Every command in `verify:` exits 0

## Proof

Run each step, record what you measured, and revert it. **A mutation here is an experiment, not a
change** — the property being probed lives in `screen.ts`, which is inside this ticket's budget, so
nothing here asks you to edit an unbudgeted file.

1. **Match the whole fragment instead of the first segment** — replace the `segments[0]` read in
   `screenFromHash` with `fragment.slice(1)`. Predict: `names a screen from a slug followed by a
   secret nothing else reads` reddens, **and so does** the merged `names the first segment and
   ignores whatever follows it`. Record both counts. This is the mutation `TASK-041201` could not
   reach, and the whole reason this ticket exists.
2. **Split the token on `?`** — return `second.split("?")[0]` from `tokenFromHash`. Predict:
   `keeps a token that would end a path segment or start a query whole` reddens **alone**. If
   anything else moves, say so.
3. **Return the last segment instead of the second** — `segments[segments.length - 1]`. Predict:
   `hands back the second segment, and nothing when there is none` reddens on `#/reset`, which has
   one segment and would answer `"reset"`. Record the message.
4. **Answer the empty string rather than `null`** for a missing second segment. Predict: the same
   test reddens. This is the vacuity check on the two `null` cases: without it a `toBeFalsy()`-shaped
   assertion would pass for either.
5. **Delete `case "reset"` from `screenFromHash`.** Predict: `names a screen from a slug followed by
   a secret nothing else reads` reddens on the `reset` half alone, and the `verify` half stays green.
   The two slugs are asserted separately for exactly this reason.

> **A red run names a prefix, not a set.** Vitest stops reporting past its first hard failure: a
> syntax error in a client source file was measured on this repo failing **twelve** test files at
> collection and printing `667 passed` with **no failure count at all**. If a step's output looks
> unrelated to the mutation, check for a collection error before concluding anything.
>
> **Expect `The 'NO_COLOR' env is ignored due to the 'FORCE_COLOR' env being set.` on stderr.** It is
> wrong: measured on this repo, `NO_COLOR=1` produces 0 escape bytes and its absence produces 74, so
> every `grep -qE` in `verify:` needs it.

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket.

## Notes

**Why the token values are fixed in the ticket rather than left to the coder.** `TASK-041201`'s two
inputs were `#/duels/2026` and `#/leaderboard/anything`, and both put a slug the switch already knows
in front of a segment nothing reads — so a `screenFromHash` matching the *whole* fragment would have
had to know `2026` to pass, which is implausible enough that the test reads as a proof and is not
one. A token that resembles nothing removes that escape.

**`ab?c=d` was measured, not imagined.** In this repository's jsdom, setting
`window.location.hash = "#/reset/ab?c=d"` leaves `window.location.hash` as exactly
`"#/reset/ab?c=d"` — the `?` stays inside the fragment rather than starting a query. So a token
containing one really does arrive at this function, and a reader that split on `?` really would lose
half of it.

`grep -c` counts matching **lines** and exits **1** on zero matches, so the zero-expectation above is
wrapped as `test "$(… | wc -l | tr -d ' ')" = 0`. `-F` keeps `?` and `(` literal.
