---
schema: 2
id: TASK-121304
title: The table reads this duel's award, not the last duel's
type: task
status: ready
parent: STORY-1213
module: web-client
estimate: XS
tier: sonnet
review: standard
files_touched: 2
labels: [qa, audit, bug, high]
depends_on: []
verify:
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose src/table/DuelTable.test.tsx 2>&1 | grep -qF "reads this duel's award and not the previous duel's"
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose src/table/DuelTable.test.tsx 2>&1 | grep -qF "names this duel's winner even once the next hand has started"
  - cd web-client && NO_COLOR=1 npm run --silent test -- src/table/DuelTable.test.tsx
  - git diff --exit-code "$(git merge-base HEAD develop)" -- web-client/src/table/no-derivation.test.tsx
  - cd web-client && NO_COLOR=1 npm run --silent check
  - python3 .github/scripts/lint_tickets.py
---

## Goal

In the second and every later duel of a room, the hand-result banner states **this** duel's winner
and **this** hand's amount — never the same-numbered hand of a duel that has already finished.

## The defect — the product tells a player they lost when they won

Round 1 of `/qa-cycle audit smoke` reported it under `FUNCTIONAL:`, from the second duel of room
`55MF2W6G`, started via Rematch. Three hands, three wrong announcements, with the chips themselves
moving correctly every time:

| hand | what actually happened | what the tables said |
| --- | --- | --- |
| 1 | seat A's pair of nines beat seat B's ace-high; A's stack 9,900 → 10,100, B's 9,900 → 9,900 | both browsers: **`Split pot — you win 100`** |
| 2 | A's ace-high beat B's queen-high; A's stack 10,000 → 10,200, B's 9,800 → 9,800 | A's own screen: **`Your rival wins 200`**. B's own screen: **`You win 200`** |
| 3 | B raised to 200 preflop, A folded — true pot 300; B's stack 9,600 → 9,900 | both screens: **`wins 19,800`** |

The winner is told they lost, the loser is told they won, and an amount from an unrelated earlier
hand is printed as this hand's pot. **The pot itself is credited correctly, the reveal and muck
behaviour is correct, and the coins and the ladder are right** — the engine and the server are not
implicated. What is wrong is the sentence the table prints on top of them.

**Severity `high`, on `EPIC-12`'s own word.** Its `high` row reads *"a core vision promise is
broken — hole cards leak, **wrong winner**, coins wrong, rematch dead"*. Not `blocker`: the duel
completes, no data is lost, nothing hangs. Not `medium`: the only workaround is *do arithmetic on
your own stack rather than believing the screen*.

## The cause, derived from source and not from the browser

Two merged facts compose:

1. **`web-client/src/store/duel-state.ts:161` appends to `narration` and nothing ever clears it.**
   `case "Events": return { ...state, narration: [...state.narration, ...message.events] }` is its
   only write outside `initialState()`. `DuelFinished` clears `outcome`, `pendingTurn`,
   `rejection`, `refusal`, `rematchOffers` and `serverAction` and leaves `narration` standing. So a
   rematch in the same room accumulates **both duels'** events in one array.
2. **`web-client/src/table/PotStrip.tsx`'s `awardsForHand` uses `findIndex`** — the **first**
   `HandStarted` whose `handNumber` matches the view's:

   ```ts
   const start = narration.findIndex(
     (event) => event.type === "HandStarted" && event.handNumber === handNumber,
   );
   ```

   In the second duel, hand *N*'s window is therefore the **first** duel's hand *N*.

That predicts all three rows above exactly: duel 1's hand 1 was a chopped 200 (two `PotAwarded` of
100), so both viewers read the split line and their own 100; duel 1's hand 2 was one award of 200 to
seat B, so A reads *Your rival wins 200* and B reads *You win 200*; and duel 1's hand 3 was the
all-in whose pot was 19,800 — the **same report's `R1` observation independently quotes that frame**,
`Your rival wins 19,800 | Blinds 50/100 · Hand 3 · Hand complete`.

Three for three, from two source lines, with two halves of one report cross-checking each other
without either knowing it. It reproduces and needs no seed: play any second duel in a room to a
decisive result and compare the announcement against the two stacks' deltas.

## Files

| File | Action |
| --- | --- |
| `web-client/src/table/PotStrip.tsx` | modify |
| `web-client/src/table/DuelTable.test.tsx` | modify |
| `web-client/src/store/duel-state.ts` | read |
| `docs/adr/ADR-0095-the-table-states-who-took-the-pot-and-never-names-a-hand.md` | read |

## Scope

**One change, in `awardsForHand`: the window opens at the *last* `HandStarted` carrying the view's
hand number, not the first.** Everything else about the function stays — it still stops at the next
`HandStarted`, still collects only `PotAwarded`, and still returns `[]` when no start matches.

- Keying to the **view's hand number** is kept, and keying to *"the last `HandStarted` seen"* is
  still refused, for the reason the function's own KDoc gives: the `Events` frame that starts hand
  4 can arrive before the `Snapshot` that moves the view off hand 3, and a window keyed to the last
  start would blink out for that tick. *Last matching* and *last seen* are different rules; this
  ticket takes the first.
- **`Array.prototype.findLastIndex` is ES2023 and `web-client/tsconfig.json` targets ES2022**
  (`"target": "ES2022"`, `"lib": ["ES2022", "DOM", "DOM.Iterable"]`). Use a backward loop, or track
  the last matching index on a forward pass. This is the trap the KDoc already documents once.
- `ADR-0095` §2's three lines and their wording are untouched, and so is `§4`'s rule that a client
  which never saw the award prints the plain `Pot N` line.

## Out of scope

- **Clearing `narration` in the store.** It is the deeper cause and it is deliberately not repaired
  here: a merged test, `web-client/src/store/duel-state.test.ts > leaves the view and narration
  untouched`, asserts that `DuelFinished` leaves the log standing, so changing it is a decision
  about what `DuelState.narration` **means** — its type comment says *"every event of the whole
  duel"* and has been false since the first rematch — and not a one-line repair. `STORY-1213`
  §*Owed to a later round* records it. Do not widen into it; a correct `awardsForHand` is right
  whether the log is cleared or not.
- **`web-client/src/table/no-derivation.test.tsx` — do not open an editor on it.** A `verify:` line
  diffs it against the merge base. Its fixture is mid-hand, so a correct window leaves it green.
- **`PotStrip.test.tsx`.** Every case in it renders without narration and stays green on the `[]`
  default. If it reddens, the change did something this ticket did not ask for.
- **Naming a made hand.** `ADR-0095` §3, closed permanently.
- **`R1`, `R2` and `R4`.** `TASK-121301`, `TASK-121302`, `TASK-121303`.

## Tests

`DuelTable.test.tsx`, **two** cases appended to the existing `describe`, using the two factories
already in the file — `started(handNumber)` and `awarded(seat, amount)` — and the `aView` fixture.
Both render the whole table, as the six existing banner cases do.

Two cases rather than one, because either alone admits a wrong implementation that the other kills.

| Test | Narration | View | Proves |
| --- | --- | --- | --- |
| `reads this duel's award and not the previous duel's` | `[started(1), awarded(0, 100), awarded(1, 100), started(2), awarded(1, 200), started(1), awarded(0, 200)]` | `viewerSeat: 0`, `handNumber: 1`, `street: "COMPLETE"`, `pot: 0` | `getByText("You win 200")` **and** `queryByText(/Split pot/)` is `null`. Two `HandStarted` carry `handNumber: 1` — the rematch shape. Today the first one wins and the table renders the observer's exact string, `Split pot — you win 100`, so both assertions are red |
| `names this duel's winner even once the next hand has started` | `[started(2), awarded(1, 9800), started(1), awarded(0, 200), started(2), awarded(0, 200), started(3), awarded(1, 5000)]` | `viewerSeat: 0`, `handNumber: 2`, `street: "COMPLETE"`, `pot: 0` | `getByText("You win 200")`, and `queryByText(/Your rival wins/)`, `queryByText(/9,800/)` and `queryByText(/5,000/)` are all `null`. Today the first `started(2)` wins and the actual winner is told *Your rival wins 9,800* — the shape of hand 2 in the report |

**What each wrong implementation the pair forecloses:**

- *first matching start* — today's bug: fails both.
- *the last `PotAwarded` anywhere in the narration* — passes case 1, and fails case 2 by printing
  *Your rival wins 5,000*.
- *the last `HandStarted` seen, whatever its number* — fails case 2 the same way, which is the
  mis-reading the function's KDoc already warns about.
- *summing, or taking the larger award* — fails case 1, whose two stale awards are equal and whose
  correct answer is neither their sum nor either of them.

The existing merged case `reads the ended hand's award and not an earlier hand's` already forecloses
*the first `PotAwarded`* and is left byte-unchanged.

`aView`'s blinds are 10/20, so the `9,800` and `5,000` queries cannot collide with the facts line.

## Acceptance criteria

- [ ] `DuelTable.test.tsx` — `reads this duel's award and not the previous duel's` passes
- [ ] `DuelTable.test.tsx` — `names this duel's winner even once the next hand has started` passes
- [ ] The six banner cases `TASK-121101` shipped still pass, unedited
- [ ] `web-client/src/table/no-derivation.test.tsx` is **byte-identical** to the merge base — the
      `git diff --exit-code` line in `verify:` exits 0
- [ ] `web-client/src/table/PotStrip.test.tsx` is not in the diff at all
- [ ] `web-client/src/store/duel-state.ts` is not in the diff at all
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket: `verify` green,
review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into `develop`.
