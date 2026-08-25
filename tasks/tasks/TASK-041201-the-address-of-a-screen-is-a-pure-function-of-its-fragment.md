---
schema: 2
id: TASK-041201
title: The address of a screen, as a pure function of its fragment
type: task
status: ready
parent: STORY-0412
module: web-client
estimate: XS
tier: haiku
review: light
files_touched: 2
labels: [client, routing]
depends_on: []
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test -- src/routing/screen.test.ts 2>&1 | grep -qE 'Tests +4 passed \(4\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'reads each address back to the screen it names'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'names the first segment and ignores whatever follows it'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'writes an address for every screen, and the first screen carries no fragment'
  - cd web-client && npm run check
---

## Goal

The client has an address space: one framework-free module turns a URL fragment into the screen it
names and back again, with no `window`, no React and no dependency.

## Files

| File | Action |
| --- | --- |
| `web-client/src/routing/screen.ts` | create |
| `web-client/src/routing/screen.test.ts` | create |

Read, and do not edit:
[`ADR-0076`](../../docs/adr/ADR-0076-a-screen-the-player-chose-has-an-address.md) §1, §4, §5 and §7;
[`ADR-0081`](../../docs/adr/ADR-0081-a-mailed-link-is-a-fragment-route-and-the-token-is-the-segment-behind-the-slug.md)
§1 (the first-segment rule); `web-client/src/lobby/room-link.ts` (the pure, `window`-free tradition
to follow).

## Scope

- Exactly these three exports, which are `ADR-0076` §5's signature verbatim:

  ```ts
  export type Screen = "first" | "duels" | "leaderboard";
  export function screenFromHash(hash: string): Screen;
  export function hashForScreen(screen: Screen): string;
  ```

- `screenFromHash` matches on the **first fragment segment** (`ADR-0081` §1), so `#/duels` and
  `#/duels/2026` both name the record. Anything else — `""`, `"#"`, `"#/"`, `"#duels"`, `"#/nope"`,
  a different case — is `"first"`. There is no *not found*: a fragment is not a request, so there is
  nothing to refuse (`ADR-0076` §7).
- The two slugs are **literals in this file**, `"duels"` and `"leaderboard"`, and are not imported
  from `HISTORY_HEADING` or `LADDER_HEADING`. `ADR-0076` §1 requires it: a URL that changed when
  `EPIC-06` restyled a heading would break every link that ever worked. Carry that reason as a
  comment, because the duplication looks like a mistake and is not.
- `hashForScreen("first")` is `"/"` — the first screen's address carries **no fragment**, and this
  is the string a replace writes (`ADR-0076` §3, §7). The other two are `"#/duels"` and
  `"#/leaderboard"`.
- `#/duels`, never `#duels`: a bare fragment is an element identifier and the browser would scroll to
  `id="duels"` (`ADR-0076` §4). The leading `/` is what makes that collision impossible, so a bare
  fragment must resolve to `"first"` rather than to the screen it looks like.
- No `window`, no `location`, no `history`, no React import. This module takes strings and returns
  strings, so its tests need no DOM.
- KDoc on all three, naming `ADR-0076` §1 for the slug rule and `ADR-0081` §1 for the segment rule.

## Out of scope

- **`tokenFromHash`.** `ADR-0081` §1 puts it in this file, and it belongs to `STORY-0417` with the
  `reset` and `verify` slugs it reads. Adding it here would ship a parser for a screen that does not
  exist. **A refusal, not an omission** — the acceptance criteria gate it by name.
- **The account screens' slugs.** They join this union in `TASK-041222` (`account`) and
  `TASK-041225` (the sign-in screen, behind `DEC-077`). The set is deliberately open (`ADR-0076`
  §1), and this ticket ships the three addresses that ADR fixed.
- Anything that touches `window`, subscribes to an event, or renders. `TASK-041202` owns all three.

## Tests

`web-client/src/routing/screen.test.ts`, describe block `"the address of a screen"`.

| Test | Proves |
| --- | --- |
| `reads each address back to the screen it names` | `screenFromHash` over four inputs in one test: `""` and `"#"` are `"first"`, `"#/duels"` is `"duels"`, `"#/leaderboard"` is `"leaderboard"`. Four inputs, because a function that returned one constant would pass any single-input test |
| `renders the first screen for a fragment it does not know` | `"#/nope"`, `"#duels"` (the bare identifier `ADR-0076` §4 refuses), `"#/"` and `"#/LEADERBOARD"` are each `"first"`, asserted one by one so the failure names which. The upper-case row is what pins the slug as lower-case ASCII rather than a case-insensitive match |
| `names the first segment and ignores whatever follows it` | `screenFromHash("#/duels/2026")` is `"duels"` and `screenFromHash("#/leaderboard/anything")` is `"leaderboard"` — `ADR-0081` §1's rule, and the widening of `ADR-0076` §7 that ADR recorded by name |
| `writes an address for every screen, and the first screen carries no fragment` | `hashForScreen` asserted against the three literals `"/"`, `"#/duels"`, `"#/leaderboard"`, **and** `screenFromHash(hashForScreen(s)) === s` for all three. The literals are asserted as literals: a round trip alone passes for `hashForScreen("first") === ""` too |

Four tests in a new file: `npm run test -- src/routing/screen.test.ts` reports **4**.

## Acceptance criteria

- [ ] `the address of a screen > reads each address back to the screen it names` passes, asserting
      all four inputs
- [ ] `the address of a screen > renders the first screen for a fragment it does not know` passes,
      asserting all four inputs
- [ ] `the address of a screen > names the first segment and ignores whatever follows it` passes
- [ ] `the address of a screen > writes an address for every screen, and the first screen carries no
      fragment` passes, asserting the three literals **and** the three round trips
- [ ] `grep -cE 'window|location|history|react' web-client/src/routing/screen.ts` returns `0`
- [ ] `grep -c 'tokenFromHash' web-client/src/routing/screen.ts` returns `0`
- [ ] `grep -cE 'HISTORY_HEADING|LADDER_HEADING' web-client/src/routing/screen.ts` returns `0`
- [ ] `npm run test -- src/routing/screen.test.ts` reports `Tests  4 passed (4)`
- [ ] No file outside the two listed differs
- [ ] Every command in `verify:` exits 0

## Proof

1. Make `screenFromHash` return `"duels"` for every input.
   **`reads each address back to the screen it names` and `renders the first screen for a fragment it
   does not know` both redden**, and the round-trip half of the fourth test reddens for `first` and
   `leaderboard`. Revert.
2. Match the **whole** fragment rather than its first segment — `hash === "#/duels"`.
   **`names the first segment and ignores whatever follows it` reddens alone**; the other three still
   pass. This is the mutation worth running, because whole-fragment matching is the obvious
   implementation and looks complete until `STORY-0417` mails a link with a token behind the slug.
   Revert.
3. Lower-case the fragment before matching it.
   **`renders the first screen for a fragment it does not know` reddens on `"#/LEADERBOARD"` alone**,
   and nothing else moves. Run it: a case-insensitive match is a kindness that makes a permanent
   address have two spellings.
4. Change `hashForScreen("first")` from `"/"` to `""`.
   **`writes an address for every screen…` reddens on the literal assertion only** — the round trip
   still passes, because `screenFromHash("")` is `"first"` either way. That is precisely why the
   criterion demands the literal, and running this variant is the cheapest way to see that a round
   trip alone gates nothing here.
5. Delete the leading-`/` requirement so `"#duels"` resolves to `"duels"`.
   **`renders the first screen for a fragment it does not know` reddens on that one input.** It is
   the `ADR-0076` §4 collision, and no other test in this file can see it.

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket.
