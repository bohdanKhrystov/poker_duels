---
schema: 2
id: TASK-121101
title: The table says who won the hand it just finished
type: task
status: ready
parent: STORY-1211
module: web-client
estimate: S
tier: sonnet
review: standard
files_touched: 3
labels: [qa, uat, bug, medium]
depends_on: []
verify:
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose src/table/DuelTable.test.tsx 2>&1 | grep -qF "states the viewer's own win in place of the pot line"
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose src/table/DuelTable.test.tsx 2>&1 | grep -qF "names the rival when the rival took the pot"
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose src/table/DuelTable.test.tsx 2>&1 | grep -qF "states only the viewer's share of a split pot"
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose src/table/DuelTable.test.tsx 2>&1 | grep -qF "leaves the pot line alone while the hand is still being played"
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose src/table/DuelTable.test.tsx 2>&1 | grep -qF "leaves the pot line alone when this client never saw the award"
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose src/table/DuelTable.test.tsx 2>&1 | grep -qF "reads the ended hand's award and not an earlier hand's"
  - cd web-client && NO_COLOR=1 npm run --silent test -- src/table/DuelTable.test.tsx
  - git diff --exit-code "$(git merge-base HEAD develop)" -- web-client/src/table/no-derivation.test.tsx
  - cd web-client && NO_COLOR=1 npm run --silent check
  - python3 .github/scripts/lint_tickets.py
---

## Goal

When the view's street is `COMPLETE` and this client received the ended hand's `PotAwarded`, the
duel table prints one of [`ADR-0095`](../../docs/adr/ADR-0095-the-table-states-who-took-the-pot-and-never-names-a-hand.md)
§2's three lines **where `Pot N` stands**, and names no hand anywhere.

## The defect

Round 3 of the UAT cycle found that when a hand ends the table says nothing about it. The observer's
`record`/`frames` reads show the frame array stepping from the acting state **straight to `Hand
complete`** — no banner at any tick.

That is true by construction, not only by observation, which is how round 3's hand-check reproduced
it without inheriting the `record`/`frames` blind spot: `web-client/src/table/PotStrip.tsx` has
exactly one `return`, it renders `Pot {view.pot}` and `Blinds N/N · Hand N · {street}` on every
street including `COMPLETE`, it has **no branch** that could ever swap in a banner, and
`DuelTable.tsx` renders no other pot-adjacent element.

**The half that could not be built has now been refused rather than deferred.** Round 3 graded this
`medium` partly because *You win 4,850* was transcribable and *Two pair, aces and sevens* was not —
no `GameEvent` names a made hand, so a client printing one would be asserting a game fact.
`ADR-0095` answers **yes** to the banner and **no** to the hand name, permanently and at every
street. So this is now an ordinary client ticket over facts already on the wire, and it carries a
real `verify:` block instead of the `manual-verify` label it used to.

## Files

| File | Action |
| --- | --- |
| `web-client/src/table/PotStrip.tsx` | modify |
| `web-client/src/table/DuelTable.tsx` | modify |
| `web-client/src/table/DuelTable.test.tsx` | modify |
| `docs/adr/ADR-0095-the-table-states-who-took-the-pot-and-never-names-a-hand.md` | read |
| `web-client/src/table/no-derivation.test.tsx` | read |

`no-derivation.test.tsx` is there to be **read and never edited** — a `verify:` line diffs it against
the merge base. *Out of scope* says why.

The two wire shapes this needs, quoted so `protocol.gen.ts` need not be opened. Both are exported as
types from `../protocol`, and `verbatimModuleSyntax` is on, so they are imported with `import type`:

```ts
interface PotAwarded  { type: "PotAwarded";  sequence: number; seat: number; amount: number }
interface HandStarted { type: "HandStarted"; sequence: number; handNumber: number;
                        buttonSeat: number; smallBlind: number; bigBlind: number;
                        stacks: readonly number[] }
```

`GameEvent` is the union both belong to. `DuelState.narration` is `readonly GameEvent[]` — every
event of the **whole duel**, appended in arrival order, which is why the hand window below matters.

## Scope

**1. `PotStrip.tsx` takes the narration and prints the line.** A new **optional** prop
`narration?: readonly GameEvent[]`, defaulted to `[]` at the point of use.

Optional on both components, and that is a constraint rather than a taste. `PotStrip.test.tsx`'s
five `<PotStrip view={…} />` renders and `DuelTable.test.tsx`'s thirteen `<DuelTable …/>`
renders compile only against an optional prop — and `no-derivation.test.tsx` renders `DuelTable`
six times the same way and **may not be edited at all**, so a required prop on `DuelTable` is not
available to this ticket under any design.

The banner replaces the **content of the existing amount `<span>`** and nothing else. Its classes,
the `<div>` around it and the facts `<span>` beside it are byte-identical to what they are now — the
facts line goes on reading `Blinds N/N · Hand N · Hand complete` (`ADR-0095` §1):

```tsx
<span className="font-mono text-large tabular-nums">
  {awardLine ?? <>Pot&nbsp;{formatChips(view.pot)}</>}
</span>
```

**2. The window: the awards of the hand the view describes.** `narration` spans the whole duel, so
the events of hand 3 are those **after the `HandStarted` whose `handNumber === view.handNumber`, up
to the next `HandStarted`**. Keyed to the view's hand number, not to "the last `HandStarted` seen":
the `Events` frame that starts hand 4 can arrive before the `Snapshot` that moves the view off hand
3, and a banner keyed to the last start would blink out for that tick.

`Array.prototype.findLastIndex` is **ES2023 and this project targets ES2022** — use `findIndex` plus
a forward loop that breaks on the next `HandStarted`.

**3. The three lines, and the trigger.** Return `null` — meaning the ordinary `Pot N` line — unless
`view.street === "COMPLETE"` **and** the window holds at least one `PotAwarded`. Then, exactly
(`ADR-0095` §2, and the amount always through `formatChips`):

| the window's awards | the line |
| --- | --- |
| one, to `view.viewerSeat` | `You win 4,850` |
| one, to the other seat | `Your rival wins 4,850` |
| two — a split | `Split pot — you win 2,425`, the viewer's **own** award |

The verb is `win`, present tense, in all three. No exclamation mark, nothing congratulatory or
consoling. The dash in the split line is an em dash, `—`, with a space either side. If a split
window somehow holds no award to `view.viewerSeat`, return `null` and print the ordinary pot line —
never a figure that is not the reader's.

**4. `DuelTable.tsx` forwards it.** A matching optional `narration?: readonly GameEvent[]` prop,
passed straight to `<PotStrip>`. No selection, no formatting and no branch here — this file only
carries the value one level down.

## Out of scope

- **`web-client/src/table/no-derivation.test.tsx` — do not open an editor on it.** Its `HAND_TALK`
  matcher (`/\b(pair|trips|set|straight|flush|full house|quads|high card|wins?|won|loses?|loser|winner|beats)\b/i`)
  is a **merged gate** and `ADR-0095` §5 grants no licence to weaken, street-scope or except it. Its
  fixture renders a `street: "TURN"` view, so a banner built on the trigger in *Scope* 3 leaves both
  of its assertions green. **A coder who meets that test red has built the wrong trigger — the fix
  is the condition, never the matcher.** A `verify:` line diffs the file against the merge base.
- **Naming the made hand, at any street, in any form.** Not in text, not in an `aria-label`, not in
  a `title`, not in a tooltip. `ADR-0095` §3, and it is closed permanently rather than deferred.
- **Surviving a reload between hands.** A client that arrives at a completed hand without that
  hand's award shows the plain `Pot N` line. That is `ADR-0095` §4's decision, not a bug: **no
  `PlayerView` field may be added** to make the banner survivable, and no store field is added
  either. It is gated below by *leaves the pot line alone when this client never saw the award*.
- **What the banner looks like.** The card draws only a winning viewer's amount, in mono and the win
  colour, and draws no losing frame at all, so the treatment of a loss belongs to `EPIC-06`. This
  ticket changes the span's **content** and not its classes.
- **Handing the narration to the table from the store.** `Lobby.tsx` still renders
  `<DuelTable view={state.view} rivalPresence={state.rivalPresence} />` and does not yet pass
  `narration`, so the banner is not on a player's screen until `TASK-121109` merges. That ticket is
  one line of `Lobby.tsx` and its test, and it depends on this one.
- **The facts line's street segment.** `TASK-121108` — the card is the outlier there, not the
  client.

## Tests

`DuelTable.test.tsx`, six new cases appended to the existing `describe`. They render the whole table
rather than `PotStrip` alone, deliberately: that is what gates *Scope* 4's forwarding hop in the same
ticket as the banner it feeds. `PotStrip.test.tsx` is not touched — every case in it renders without
narration and stays green on the `[]` default.

Two small factories keep them short:

```tsx
const started = (handNumber: number): GameEvent => ({
  type: "HandStarted", sequence: handNumber * 10, handNumber,
  buttonSeat: 0, smallBlind: 25, bigBlind: 50, stacks: [1500, 1500],
});
const awarded = (seat: number, amount: number): GameEvent => ({
  type: "PotAwarded", sequence: 99, seat, amount,
});
```

| Test | Proves |
| --- | --- |
| `states the viewer's own win in place of the pot line` | `viewerSeat: 0`, `handNumber: 3`, `street: "COMPLETE"`, `pot: 0`; narration `[started(3), awarded(0, 4850)]`. `getByText("You win 4,850")` **and** `queryByText(/Pot/)` is `null` — the banner *replaces*, it does not join |
| `names the rival when the rival took the pot` | `viewerSeat: 1`, award to seat `0`. `getByText("Your rival wins 4,850")` **and** `queryByText(/You win/)` is `null`. The viewer's seat is `1` here and `0` above, so an implementation that hard-codes "seat 0 is you" prints *You win* and fails |
| `states only the viewer's share of a split pot` | `viewerSeat: 1`; narration `[started(3), awarded(0, 2425), awarded(1, 2426)]` — the odd chip out of position, so the two shares differ. `getByText("Split pot — you win 2,426")` **and** `queryByText(/2,425/)` is `null`. The two amounts differ, so taking the first award, the larger, or a sum all fail |
| `leaves the pot line alone while the hand is still being played` | `street: "TURN"`, `pot: 5675`, narration `[started(3), awarded(0, 4850)]` — the award is present and the trigger must still refuse. `getByText(/Pot 5,675/)`, and `expect(container.innerHTML).not.toMatch(/win/i)`, which covers attributes as well as text because `innerHTML` carries both |
| `leaves the pot line alone when this client never saw the award` | `street: "COMPLETE"`, `pot: 0`, narration `[]` — `ADR-0095` §4's between-hands reload. `getByText(/Pot 0/)` and `expect(container.innerHTML).not.toMatch(/win/i)` |
| `reads the ended hand's award and not an earlier hand's` | narration `[started(1), awarded(0, 1200), started(2), awarded(0, 4850)]`, view at `handNumber: 2`, `street: "COMPLETE"`, `viewerSeat: 0`. `getByText("You win 4,850")` **and** `queryByText(/1,200/)` is `null` — an implementation that takes the first `PotAwarded` in the narration fails |

No test asserts that an element exists, that a class is non-empty, or that some node is present:
every row above pins a **rendered string** or the **absence** of one, and each fixture differs from
its neighbour in the one value that would let a wrong implementation pass.

## Acceptance criteria

- [ ] `DuelTable.test.tsx` — `states the viewer's own win in place of the pot line` passes
- [ ] `DuelTable.test.tsx` — `names the rival when the rival took the pot` passes
- [ ] `DuelTable.test.tsx` — `states only the viewer's share of a split pot` passes
- [ ] `DuelTable.test.tsx` — `leaves the pot line alone while the hand is still being played` passes
- [ ] `DuelTable.test.tsx` — `leaves the pot line alone when this client never saw the award` passes
- [ ] `DuelTable.test.tsx` — `reads the ended hand's award and not an earlier hand's` passes
- [ ] `web-client/src/table/no-derivation.test.tsx` is **byte-identical** to the merge base — the
      `git diff --exit-code` line in `verify:` exits 0
- [ ] `web-client/src/table/PotStrip.test.tsx` is not in the diff at all
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
