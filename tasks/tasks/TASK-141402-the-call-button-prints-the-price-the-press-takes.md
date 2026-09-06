---
schema: 2
id: TASK-141402
title: The Call button prints the price the press takes from the stack
type: task
status: backlog
parent: STORY-1414
module: web-client
estimate: S
tier: haiku
review: standard
files_touched: 3
labels: [client, table]
depends_on: [TASK-141401]
verify:
  - cd web-client && npm ci && FORCE_COLOR=0 NO_COLOR=1 npm run --silent check
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/table/action-text.test.ts > "${TMPDIR:-/tmp}/action-text-after.txt" 2>&1; grep -qF 'Tests  11 passed (11)' "${TMPDIR:-/tmp}/action-text-after.txt"
  - cd web-client && cp src/table/action-text.ts "${TMPDIR:-/tmp}/action-text.fixed.ts" && perl -0pi -e 's/actions\.callTo - committedThisStreet/actions.callTo/' src/table/action-text.ts && FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/table/action-text.test.ts > "${TMPDIR:-/tmp}/action-text-before.txt" 2>&1; cp "${TMPDIR:-/tmp}/action-text.fixed.ts" src/table/action-text.ts; cmp -s "${TMPDIR:-/tmp}/action-text.fixed.ts" src/table/action-text.ts && grep -qF "expected { verb: 'Call', amount: 600 } to deeply equal { verb: 'Call', amount: 400 }" "${TMPDIR:-/tmp}/action-text-before.txt" && grep -qF 'Tests  1 failed | 10 passed (11)' "${TMPDIR:-/tmp}/action-text-before.txt"
  - awk 'index($0, "props.committedThisStreet") { n++ } END { exit (n != 3) }' web-client/src/table/ActionBar.tsx
  - python3 .github/scripts/lint_tickets.py
---

## Goal

`actionText` prices a call at `actions.callTo - committedThisStreet` — `ADR-0101` §1's `toCall`,
the term `BettingRules.kt` added to build `callTo` — and the action bar hands it the commitment it
already holds. `Bet`, `Raise to` and `All in` return the same figures they return today.

## Files

| File | Action |
| --- | --- |
| `web-client/src/table/action-text.ts` | modify |
| `web-client/src/table/ActionBar.tsx` | modify |
| `web-client/src/table/action-text.test.ts` | modify |

Read [`STORY-1414`](../stories/STORY-1414-call-says-what-it-costs.md)'s *Design notes* and
[`ADR-0122`](../../docs/adr/ADR-0122-call-names-the-price-and-raise-to-names-the-total.md) §§1, 3 and
5. `web-client/src/table/turn-fixture.ts` is 33 lines and is where `aLegalActions` lives — open it
if you need the default figures. **Nothing else is opened.**

## Scope

- **`actionText` gains a fourth parameter, required and undefaulted**, and prices the call:

  ```ts
  export function actionText(
    type: ActionType,
    actions: LegalActions,
    to: number,
    committedThisStreet: number,
  ): ActionText {
    switch (type) {
      case "CALL":
        return {
          verb: actionVerb(type),
          amount: actions.callTo - committedThisStreet,
        };
  ```

  Write `actions.callTo - committedThisStreet` **verbatim**, in that spelling: the third `verify:`
  command reintroduces the defect by matching exactly that text, and a differently spelled
  equivalent (`actions.callTo - props.committedThisStreet`, a hoisted `const price`, a reordered
  subtraction) makes that gate red. **No default value and no `?? 0`** — a defaulted commitment is
  the exact mechanism that hid this from the suite (`ADR-0122` §Consequences), and a caller that
  forgets the argument must fail to compile.
- **The `ALL_IN`, `BET`, `RAISE` and `default` arms are byte-unchanged.** `All in` still prints
  `allInTo`, `Bet` and `Raise to` still print `to`, `Fold` and `Check` still print nothing
  (`ADR-0122` §3).
- **`actionText`'s KDoc is corrected, because it currently refuses this in as many words.** The
  sentence *"Nothing is priced, netted or worked out"* is now false and may not stand. Replace the
  second paragraph with words to this effect — the sentences are yours, the four facts are not:
  `allInTo` and `to` are still the server's and the player's own; the call's figure is
  `ADR-0101` §1's `toCall`, **recovered** rather than manufactured, since `BettingRules.kt` builds
  `callTo = committed + toCall(seat)`; it is the **one** further quantity the never-derives guards
  admit (`ADR-0122` §5, `ADR-0107` §5's shape); and nothing else here is priced, netted or worked
  out.
- **`ActionBar.tsx:163` passes the prop the component already holds**, and nothing else in that
  file changes:

  ```ts
          const text = actionText(
            type,
            actions,
            dialled ?? 0,
            props.committedThisStreet,
          );
  ```

  That is prettier's own wrapping of the call; run `npm run format` and leave it where prettier puts
  it. The fourth `verify:` command counts `props.committedThisStreet` in that file at **3** — the
  handoff at `:73`, the sizing base at `:113`, and this — so passing a literal, a hoisted local or
  `0` fails the gate.
- **`action-text.test.ts` moves off zero at every `actionText` call site.** There are ten of them and
  a zero at any of them proves nothing. See *Tests*.
- Run `npm run format` before `npm run check` — prettier owns the wrapping.

## Out of scope

- **`lastActText` and its KDoc**, in this same file. The mark still prints `event.to` after this
  ticket merges and `TASK-141404` is what changes it — the two are split because the mark's own
  blast radius is three files of its own. Do not touch `lastActText`, `SeatPlate.tsx` or
  `SeatPlate.test.tsx` here; if you do, `TASK-141404`'s gates will disagree with its ticket.
- **`ActionBar.test.tsx` and `bar-no-derivation.test.tsx`.** They are `TASK-141403`'s, whole. All
  three of `ActionBar.test.tsx`'s merged `Call` assertions (lines 57, 479, 658) render at
  `committedThisStreet ?? 0`, where `callTo - 0 === callTo`, so **every one of them is green before
  and after this change and none may be edited**. If a merged assertion anywhere in `web-client/src`
  goes red, the change did something this ticket did not ask for — stop and report it rather than
  editing the assertion.
- **`sizingChips`** (`ActionBar.tsx:249-267`, its `toCall` at `:255`). It already computes this
  difference and keeps its
  body, its KDoc and its own local `toCall`. This ticket adds a second place the subtraction is
  spelled and deliberately does not refactor them into one: the sizing base is a number handed to
  five presets, the price is a number printed on a button, and `ADR-0101` §7's precedent is that the
  bar composes what it needs from the props it is given.
- **The `Act` frame.** `actFrame` sends `{ type: "Call", seat }` with no amount at all
  (`ADR-0122` §6), so nothing about what is *sent* changes and `act-frame.ts` is not opened.
- **`All in`'s figure** — `ADR-0122` §3 leaves it a bare total on purpose.

## Tests

No test is added or removed: `action-text.test.ts` holds **11** tests before and after. What changes
is the fixtures, and the change is the point — a commitment of zero cannot tell the two
implementations apart.

`action-text.test.ts` → `describe("the action text")`

| Test | Change |
| --- | --- |
| `prices a call from the server's callTo` → **`prices a call at what the press takes from the stack`** | Three assertions, replacing two. `actionText("CALL", aLegalActions({ callTo: 600 }), 9999, 200)` is `{ verb: "Call", amount: 400 }`; `actionText("CALL", aLegalActions({ callTo: 925 }), 9999, 75)` is `{ verb: "Call", amount: 850 }`; `actionText("CALL", aLegalActions(), 9999, 0)` is `{ verb: "Call", amount: 400 }`. **Two different non-zero commitments** so that a constant, a dropped argument and a swapped operand each fail, and **one zero** so that the shipped zero-commitment frame is pinned as still printing the total |
| `prices an all-in from the server's allInTo` | Same two assertions, now passing `200` and `75` as the commitment: `allInTo` 13,400 and 8,500 come back **unchanged**. This is `ADR-0122` §3 under test — the commitment must reach the call and nothing else |
| `prices a bet and a raise from the total the player dialled in` | Same four assertions, passing `200`, `200`, `75`, `75`: 3,250 and 5,000 come back unchanged |
| `puts no figure on a fold or a check` | Same two assertions, passing `200`: still `null` |

The `lastActText` tests in the second half of the file are **not touched** by this ticket.

**The change must be proved red rather than asserted red.** The third `verify:` command copies
`action-text.ts` aside, rewrites `actions.callTo - committedThisStreet` back to `actions.callTo`,
re-runs this one file, restores the file byte-for-byte (`cmp -s`), and then requires the captured
output to contain both `expected { verb: 'Call', amount: 600 } to deeply equal { verb: 'Call',
amount: 400 }` and `Tests  1 failed | 10 passed (11)`. It goes red if the fixture cannot detect the
old behaviour, if the mutation did not apply, and if the file was not restored. **Both strings were
measured by running that probe on these exact fixtures**, not computed from the change.

The second command pins the file's absolute test count at **11**, so a collection error or a
quietly renamed test cannot pass as a green run.

## Acceptance criteria

- [ ] `prices a call at what the press takes from the stack` passes, asserting 400 from
      `callTo` 600 / committed 200, 850 from 925 / 75, and 400 from the default fixture / 0
- [ ] `prices an all-in from the server's allInTo`, `prices a bet and a raise from the total the
      player dialled in` and `puts no figure on a fold or a check` pass with a **non-zero**
      commitment and unchanged expected figures
- [ ] `web-client/src/table/action-text.test.ts` holds exactly 11 tests and all 11 pass
- [ ] With `- committedThisStreet` removed from `action-text.ts`, exactly one of those 11 fails —
      asserted by the third `verify:` command, which restores the file afterwards
- [ ] `props.committedThisStreet` appears exactly 3 times in `web-client/src/table/ActionBar.tsx`
- [ ] `actionText`'s KDoc no longer contains the sentence *"Nothing is priced, netted or worked
      out"* as a claim about the call, and names `ADR-0101` §1 and `ADR-0122` §5
- [ ] `lastActText` and its KDoc are byte-identical to `develop`
- [ ] `cd web-client && npm run --silent check` exits 0 with no merged assertion edited
- [ ] Every command in `verify:` exits 0

The PR body quotes the failure line the third command captured from
`"${TMPDIR:-/tmp}/action-text-before.txt"`, not retyped from this ticket.

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
