---
schema: 2
id: TASK-130103
title: The strip prints every chip committed to the hand
type: task
status: ready
parent: STORY-1301
module: web-client
estimate: S
tier: haiku
review: standard
files_touched: 2
labels: [client, table]
depends_on: [TASK-130102]
verify:
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npm run --silent test -- src/table/PotStrip.test.tsx 2>&1 | grep -qE "Tests +7 passed \(7\)"
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npm run --silent test -- --reporter=verbose src/table/PotStrip.test.tsx 2>&1 | grep -qF "opens the hand at the blinds, never at nothing"
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npm run --silent test -- --reporter=verbose src/table/PotStrip.test.tsx 2>&1 | grep -qF "adds what both seats have out this street to the collected pot"
  - awk 'index($0, "takes the pot from the view and not from what the seats put in") { n++ } END { exit (n != 0) }' web-client/src/table/PotStrip.test.tsx
  - awk 'index($0, "not a sum of what the seats put in") { n++ } END { exit (n != 0) }' web-client/src/table/PotStrip.tsx
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npm run --silent test -- src/table/no-derivation.test.tsx 2>&1 | grep -qE "Tests +7 passed \(7\)"
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npm run --silent test -- src/table/DuelTable.test.tsx 2>&1 | grep -qE "Tests +22 passed \(22\)"
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npm run --silent test -- src/lobby/Lobby.test.tsx 2>&1 | grep -qE "Tests +80 passed \(80\)"
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npm run --silent test -- src/store/reconnect.test.tsx 2>&1 | grep -qE "Tests +8 passed \(8\)"
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npm run --silent check
  - cd web-client && FORCE_COLOR=0 npm run --silent build
  - python3 .github/scripts/lint_tickets.py
---

## Goal

The duel table's strip prints `view.pot` plus **both** seats' `committedThisStreet`
([`ADR-0107`](../../docs/adr/ADR-0107-pot-names-every-chip-committed-to-the-hand.md) §1), so at
50/100 the first decision of a hand reads `Pot 150` where it read `Pot 0`, and one word on the screen
finally names one quantity.

## The quantity, and the shape

```
Pot = view.pot + seats[0].committedThisStreet + seats[1].committedThisStreet
```

Identically `web-client/src/lobby/Lobby.tsx:154`'s `potIncludingStreet` and `ADR-0101` §1's `P` —
the number the sizing row has priced against since `ADR-0100` §6 landed. **The strip sums the view
itself** rather than taking a prop: `ADR-0107` §5 leaves the shape to this ticket, `ADR-0101` §7 is
the precedent, and every term is already on the `view` this component receives, so a prop would
thread the sum through `Lobby.tsx` and `DuelTable.tsx`, which do not otherwise need it, for no
gain. **Do not re-litigate this** — it is the ticket's shape, not a decision.

`ADR-0107` §2 and §3 close the two nearby temptations: the label stays the single word `Pot`, with
no *Total pot*, no second figure and no parenthetical; and an uncalled raise stays **inside** the
number even though a fold would hand it back, because `Pot` states what is committed, not what the
winner nets.

## What the whole client suite says about this change

Measured 2026-09-02 by applying the sum and running every test the pull-request gate set runs:

- `PotStrip.test.tsx` **5 → all pass**, `DuelTable.test.tsx` **22 pass**, `Lobby.test.tsx` **80
  pass**, `reconnect.test.tsx` **8 pass**. **Not one merged `Pot` pin moves**, because every one of
  those fixtures has `committedThisStreet: 0` on both seats — `DuelTable.test.tsx`'s `Pot 5,675` is
  a default-seat fixture, and the `5,675` beside commitments of 125 and 825 that `ADR-0107`'s
  *Consequences* refers to lives in `no-derivation.test.tsx`, whose amendment is `TASK-130102`.
  Four `verify:` commands pin those four counts so the coder cannot quietly edit a file outside this
  budget, and so a reviewer can see the claim was checked.
- The one file that reddens is `no-derivation.test.tsx`, and `TASK-130102` has already amended it —
  which is why this ticket depends on it.

Because nothing existing moves, **the whole burden of proving the change is on the two new tests
below.** They are specified value by value for that reason.

## Files

| File | Action |
| --- | --- |
| `web-client/src/table/PotStrip.tsx` | modify |
| `web-client/src/table/PotStrip.test.tsx` | modify |
| `docs/adr/ADR-0107-pot-names-every-chip-committed-to-the-hand.md` | read |

## Scope

- In `PotStrip.tsx`, add a file-local named function above the component and render its result:

  ```tsx
  function potCommittedToTheHand(view: PlayerView): number {
    return view.seats.reduce(
      (sum, seat) => sum + seat.committedThisStreet,
      view.pot,
    );
  }
  ```

  and `{awardLine ?? <>Pot&nbsp;{formatChips(potCommittedToTheHand(view))}</>}`. `PlayerView` is
  already imported.

- **Invert the component's docstring, which currently asserts the opposite.** The sentence *"the pot
  is `view.pot` and not a sum of what the seats put in"* becomes a sentence naming `ADR-0107` §1's
  quantity. A `verify:` command refuses the old wording. Nothing else in that docstring moves — the
  paragraphs about `props.street`, the runout step and `ADR-0095`'s award line are all still true.

- In `PotStrip.test.tsx`, **rename** `takes the pot from the view and not from what the seats put
  in` to `adds this street and never the whole hand, which would count a swept street twice`, and
  update its one comment. **Its fixture and its expected `Pot 4,850` do not move** — it uses
  `committedThisHand`, not `committedThisStreet`, and `ADR-0107`'s *Consequences* says the sum it
  guards against stays wrong under the new semantics too, so its successor guards the same mistake
  under a truthful name.

- Add the two tests written out under *Tests* below, verbatim in their fixtures and their expected
  strings.

- Leave `writes the pot the view carries, grouped` alone. Its fixture holds no street commitments,
  so `Pot 2,450` is still the right answer — and it is the assertion that stops a component summing
  only the commitments and forgetting `view.pot`.

## Out of scope

- **`no-derivation.test.tsx`.** `TASK-130102` owns it and has already landed the narrowing. A
  `verify:` command pins it at 7 passing; if it is red, stop and report rather than editing it.
- **`DuelTable.test.tsx`, `Lobby.test.tsx`, `reconnect.test.tsx`.** Measured green under this change
  and pinned by count. Do not open them.
- **`DuelTable.tsx`, `Lobby.tsx`.** No prop is threaded; see *The quantity, and the shape*.
- **The rival's `committed` bet-line at `DuelTable.tsx:58`, and a hero bet-line.** `ADR-0107` §4
  leaves both to `STORY-1306`'s card. The rival's street chips now appearing twice on screen is a
  cost the ADR accepts by name; it is not a defect to repair here.
- **Netting an uncalled raise out of the figure.** `ADR-0107` §3 forbids it in as many words.
- **A second figure, or relabelling `Pot`.** `ADR-0107` §2 forecloses both; a ticket that wants one
  owes a new ADR.
- **The sizing row, and `DEC-102`.** `ADR-0101`'s presets and base are unchanged.

## Tests

`the pot strip` — `web-client/src/table/PotStrip.test.tsx`

| Test | Proves |
| --- | --- |
| `opens the hand at the blinds, never at nothing` | With `pot: 0`, blinds 50/100 and `committedThisStreet` of **50** and **100**, the strip prints `Pot 150` and no `Pot 0` — the reported case, gone |
| `adds what both seats have out this street to the collected pot` | With `pot: 2450` and `committedThisStreet` of **125** and **825**, the strip prints `Pot 3,400`, and prints neither `Pot 2,450` (the collected pot alone) nor `Pot 2,575` / `Pot 3,275` (one seat's chips only) |
| `adds this street and never the whole hand, which would count a swept street twice` *(renamed)* | Fixture unchanged: `pot: 4850` with `committedThisHand: 10` on both seats still prints `Pot 4,850`, so the sum reads `committedThisStreet` and never `committedThisHand` |
| `writes the pot the view carries, grouped` *(untouched)* | `pot: 2450` with no street commitments still prints `Pot 2,450` — a component that summed only the commitments would print `Pot 0` here |

Every expected value above is **read off its own fixture** (`ADR-0107`, *Consequences*):
`0 + 50 + 100 = 150`, `2450 + 125 + 825 = 3400`, and the two negatives are the two single-seat sums
`2450 + 125` and `2450 + 825`. The two fixtures use **different** non-zero commitments on the two
seats and one has a non-zero `pot` while the other has zero, so no single wrong formula passes both.

Write them as:

```tsx
it("opens the hand at the blinds, never at nothing", () => {
  render(
    <PotStrip
      view={aView({
        pot: 0,
        smallBlind: 50,
        bigBlind: 100,
        seats: [
          aSeat({ index: 0, committedThisStreet: 50 }),
          aSeat({ index: 1, committedThisStreet: 100 }),
        ],
      })}
    />,
  );

  expect(screen.getByText(/Pot 150/)).toBeTruthy();
  expect(screen.queryByText(/Pot 0/)).toBeNull();
});

it("adds what both seats have out this street to the collected pot", () => {
  render(
    <PotStrip
      view={aView({
        pot: 2450,
        seats: [
          aSeat({ index: 0, committedThisStreet: 125 }),
          aSeat({ index: 1, committedThisStreet: 825 }),
        ],
      })}
    />,
  );

  expect(screen.getByText(/Pot 3,400/)).toBeTruthy();
  expect(screen.queryByText(/Pot 2,450/)).toBeNull();
  expect(screen.queryByText(/Pot 2,575/)).toBeNull();
  expect(screen.queryByText(/Pot 3,275/)).toBeNull();
});
```

**Both are red on today's behaviour and green on the new one** — measured, not assumed: against the
unchanged `PotStrip.tsx` the file reports `Tests 2 failed | 5 passed (7)`, both failures being
*"Unable to find an element with the text"* for `/Pot 150/` and `/Pot 3,400/`.

**The count in `verify:` is 7 and was measured, not computed**: `PotStrip.test.tsx` runs 5 tests
today, this ticket renames one and adds two, and the finished file was run at 7 passing before this
ticket was written.

## Acceptance criteria

- [ ] `opens the hand at the blinds, never at nothing` passes
- [ ] `adds what both seats have out this street to the collected pot` passes
- [ ] `adds this street and never the whole hand, which would count a swept street twice` passes,
      and the string `takes the pot from the view and not from what the seats put in` appears
      nowhere in `PotStrip.test.tsx`
- [ ] `PotStrip.test.tsx` reports exactly `Tests 7 passed (7)`
- [ ] The string `not a sum of what the seats put in` appears nowhere in `PotStrip.tsx`
- [ ] `no-derivation.test.tsx` reports exactly `Tests 7 passed (7)`, `DuelTable.test.tsx` exactly
      `22 passed (22)`, `Lobby.test.tsx` exactly `80 passed (80)` and `reconnect.test.tsx` exactly
      `8 passed (8)` — no assertion in any of them was moved, weakened or added
- [ ] `npm run check` and `npm run build` exit 0 in `web-client`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
