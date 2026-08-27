---
schema: 2
id: TASK-041501
title: The words the offer says, and the one word ADR-0036 already chose
type: task
status: done
parent: STORY-0415
module: web-client
estimate: XS
tier: haiku
review: standard
files_touched: 2
labels: [client, account, copy]
depends_on: []
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test -- src/result/account-offer-text.test.ts 2>&1 | grep -qE 'Tests +1 passed \(1\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'states every sentence exactly, character for character, and names the stake'
  - test "$(grep -oE '[0-9]' web-client/src/result/account-offer-text.ts | wc -l | tr -d ' ')" = 0
  - test "$(grep -oF 'Not now' web-client/src/result/account-offer-text.ts | wc -l | tr -d ' ')" = 1
  - test "$(grep -oF 'export const' web-client/src/result/account-offer-text.ts | wc -l | tr -d ' ')" = 4
  - cd web-client && npm run check
---

## Goal

`ADR-0036`'s offer has words: four exported constants, pinned character for character by one test,
in a module nothing else in this client writes.

## Files

| File | Action |
| --- | --- |
| `web-client/src/result/account-offer-text.ts` | create |
| `web-client/src/result/account-offer-text.test.ts` | create |

Read, and do not edit:

- [`ADR-0036`](../../docs/adr/ADR-0036-an-account-is-offered-never-required.md) — §Decision is the
  whole brief for these words.
- `web-client/src/account/account-text.ts` — the house shape for a copy module, and the strings this
  ticket must **not** duplicate (`SIGN_UP_LABEL` is a submit button, `ACCOUNT_HEADING` is the
  lobby's existing door).
- `web-client/src/account/account-text.test.ts` — lines 1–40, the export-key-set assertion this
  ticket copies. It is the mechanism, not an illustration: `toBe` on each literal cannot see an
  export that was added or removed.
- `web-client/src/result/result-no-derivation.test.tsx` — `numbersOnScreen`, and why a digit in
  this copy is a defect (see `## Scope`).

## Scope

- Four exports, and exactly four:

  ```ts
  export const OFFER_HEADING = "Your duel coins are only in this browser";

  export const OFFER_BODY =
    "You have won a duel, so you have duel coins to lose. They belong to this browser and go with it. " +
    "A password keeps them, and your duels, on any browser you sign in from. You are never required to have one.";

  export const OFFER_ACCEPT = "Keep them with a password";

  export const OFFER_DISMISS = "Not now";
  ```

  These four strings were written against `ADR-0036` §Decision and run green here. Carry them over
  verbatim; a reviewer judging the wording judges these.
- **`OFFER_DISMISS` is `"Not now"` and is not this ticket's to choose.** `ADR-0036` §Decision writes
  it in terms — *"It is dismissible, and dismissal is permanent. 'Not now' means not again"* — and
  `DEC-049` restates it as *"which only 'Not now' dismisses"*. A `verify:` line pins the file at
  exactly one occurrence.
- **`OFFER_BODY` states four facts, in this order, and `ADR-0036` is where each comes from:** the
  player has coins (*"names the actual stake — coins that exist and could be lost"*), the coins are
  bound to this browser (`ADR-0012`'s device-bound profile), a password preserves them, and it is
  never required (*"An account is never required to play"*). Nothing else.
- **No digit anywhere in the file**, including in a comment. `result-no-derivation.test.tsx`'s
  `shows no number the outcome does not carry` sweeps every text node under the result panel, and
  once a later ticket renders this copy inside `DuelResult` any numeral in it lands outside the
  allowed set — measured mechanism, gated here so the defect cannot be introduced in the first
  place. A `verify:` line greps the source for `[0-9]` and requires zero matches.
- KDoc on `OFFER_HEADING` and `OFFER_DISMISS` naming `ADR-0036`, in the `account-text.ts` house
  style: comment *why*, never *what*.

## Out of scope

- **Rendering any of it.** No component, no JSX, no import of React. `TASK-041503`.
- **Reusing `SIGN_UP_LABEL` or `ACCOUNT_HEADING` for `OFFER_ACCEPT`.** Deliberate, and the reason is
  a defect rather than taste: `SIGN_UP_LABEL` ("Give this profile a password") sits on the sign-up
  form's **submit** button, which performs a network write, and `ACCOUNT_HEADING` ("Account") sits
  on the lobby's existing door. A third control carrying either string would be indistinguishable
  from it in a `getByRole(…, { name })` query, and `TASK-041503` queries this link by name.
- **Any word about what dismissal costs, or about how long the offer lasts.** `DEC-079` is open on
  exactly what *"not again"* means; copy that described it would be this ticket answering it.
- Where the flag lives, when the offer appears, and who renders it — `DEC-079`, `DEC-080`, and the
  tickets those unblock.

## Tests

`web-client/src/result/account-offer-text.test.ts` — a new file, one test.

| Test | Proves |
| --- | --- |
| `states every sentence exactly, character for character, and names the stake` | The export key set is **exactly** these four (`Object.keys(…).sort()`), each literal matches byte for byte, and `OFFER_DISMISS` is `ADR-0036`'s own word. The key-set assertion is what an added or removed export fails; the four `toBe`s are what an edited word fails |

Write it in `account-text.test.ts`'s shape:

```ts
import { describe, expect, it } from "vitest";
import * as offerText from "./account-offer-text";

describe("the offer's words", () => {
  it("states every sentence exactly, character for character, and names the stake", () => {
    // Exactly these exports, and no others — an extra or a missing one fails here
    // even if every literal below still matches, because `toBe` cannot see either.
    expect(Object.keys(offerText).sort()).toEqual(
      ["OFFER_ACCEPT", "OFFER_BODY", "OFFER_DISMISS", "OFFER_HEADING"].sort(),
    );

    expect(offerText.OFFER_HEADING).toBe(
      "Your duel coins are only in this browser",
    );
    expect(offerText.OFFER_BODY).toBe(
      "You have won a duel, so you have duel coins to lose. They belong to this browser and go with it. " +
        "A password keeps them, and your duels, on any browser you sign in from. You are never required to have one.",
    );
    expect(offerText.OFFER_ACCEPT).toBe("Keep them with a password");
    // ADR-0036 §Decision writes this word itself; it is not this file's to choose.
    expect(offerText.OFFER_DISMISS).toBe("Not now");
  });
});
```

**No `try` anywhere in this file, and no `expect()` inside one.** A failing assertion is itself a
throw, so a `try` around one discards the failure and the test passes green — `TASK-041409` shipped
exactly that and planting a forbidden key left all six of its tests passing.

## Acceptance criteria

- [ ] `cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'states every sentence exactly, character for character, and names the stake'`
      — the test exists and passes
- [ ] `cd web-client && NO_COLOR=1 npm run --silent test -- src/result/account-offer-text.test.ts 2>&1 | grep -qE 'Tests +1 passed \(1\)'`
      — exactly one test in the new file. A second test added here is a second place to edit the
      same strings
- [ ] `test "$(grep -oE '[0-9]' web-client/src/result/account-offer-text.ts | wc -l | tr -d ' ')" = 0`
      — no digit in the source, comments included
- [ ] `test "$(grep -oF 'Not now' web-client/src/result/account-offer-text.ts | wc -l | tr -d ' ')" = 1` —
      `ADR-0036`'s word appears once, on `OFFER_DISMISS`, and is not restated in a comment
- [ ] `test "$(grep -oF 'export const' web-client/src/result/account-offer-text.ts | wc -l | tr -d ' ')" = 4`
      — four exports, no fifth
- [ ] `cd web-client && npm run check` exits 0 — typecheck, lint, `prettier --check` and the whole
      suite. The suite reads **812 passed (812)** afterwards, up one from the merged 811
- [ ] No file outside the two listed differs
- [ ] Every command in `verify:` exits 0

## Proof

**Every step below was run in this worktree, against the projected diff, before this ticket was
written.** Baseline measured on `develop` at `922d57fc`: **811 tests / 103 files**, green. Record
the numbers you actually see; a mismatch with a step is a finding worth reporting, not a cell to
round off. Never record the unmutated state as a step's "actual", and never write *would*, *if
done* or *not testable*.

1. Change `OFFER_DISMISS` from `"Not now"` to `"Later"`. **`states every sentence exactly…` reddens**
   — measured, on the projected four-ticket diff, `2 failed | 819 passed (821)`; the second failure
   was `TASK-041503`'s `calls onDismiss when Not now is taken`, which does not exist yet, so on this
   ticket alone expect **one** failure at `811 passed | 1 failed (812)`. Revert.
2. Add a fifth export (`export const OFFER_LATER = "…";`). **The same test reddens**, on the key-set
   assertion, before any `toBe` runs. This is the assertion the four `toBe`s cannot make. Revert.
3. Delete `OFFER_ACCEPT`. **The same test reddens** on the key set, and the file no longer
   typechecks once `TASK-041503` imports it — but that is a later ticket, and *this* ticket's gate
   is the key set. Revert.
4. Put a digit in a comment (`// one coin`). **`npm run check` still passes and every test stays
   green** — measured: nothing in the suite sees it. The `verify:` grep is the only gate, which is
   why it is a `verify:` line and not a note to the reviewer. Revert.

> **Expect two warnings on stderr from every command above:** `The 'NO_COLOR' env is ignored due to
> the 'FORCE_COLOR' env being set.` It is harmless here and was checked rather than assumed —
> `cat -v` on the reporter's output shows test names and the `Tests  N passed (N)` summary carry
> **no escape codes** either way, so `grep -qF` on a name and `grep -qE` on the summary both match.

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket.

## Notes

`grep -c` counts matching **lines** and exits **1** when it matches nothing, so a bare
`grep -c … = 0` fails the step it is meant to pass. Every zero-expectation above is wrapped as
`test "$(… | wc -l | tr -d ' ')" = 0`, and `-F` is on every fixed needle so that `(`, `)` and `.`
stay literal.
