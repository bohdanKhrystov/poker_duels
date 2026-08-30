---
schema: 2
id: TASK-121107
title: A player with no place this season reads the card's muted line, not the accent box
type: task
status: backlog
parent: STORY-1211
module: web-client
estimate: XS
tier: sonnet
review: standard
files_touched: 2
labels: [qa, uat, bug, low]
depends_on: [TASK-121104]
verify:
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose src/ladder/LadderScreen.test.tsx 2>&1 | grep -qF "a player with no place reads the card's muted line, with no coin"
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose src/ladder/LadderScreen.test.tsx 2>&1 | grep -qF "the self line is the highlighted box the card draws"
  - cd web-client && NO_COLOR=1 npm run --silent test -- src/ladder/LadderScreen.test.tsx
  - cd web-client && NO_COLOR=1 npm run --silent check
  - python3 .github/scripts/lint_tickets.py
---

## Goal

A player who has finished no duel this season reads `NO_PLACE_THIS_SEASON` in the card's muted line —
hairline border, no tint, no coin — instead of the accent box a ranked player gets.

## The defect, and how it was established without a walk

`design/screens/leaderboard.html` draws two frames of the same slot, and the second is deliberate:

```
/* No place yet: the same line's slot, worded as an absence rather than a number — */
.self.muted { background: none; border-color: var(--pd-hairline); color: var(--pd-text-muted); }
...
<p class="self muted"><span>You have no place on this season's leaderboard.</span></p>
```

No `.coin` inside it, where the ranked frame carries one.

`LadderScreen.tsx:101-104` renders the slot **unconditionally**:

```
{state.self !== null && (
  <p className="flex items-center gap-3 rounded-medium border border-accent bg-accent-subtle px-5 py-4">
    <CoinMark />
    <span>{selfLine(state.self)}</span>
```

`selfLine` returns `NO_PLACE_THIS_SEASON` when `rank` is null, so the absence sentence lands inside
the accent box with a coin mark before it. There is no branch, so the divergence holds by
construction.

**This was not walked, and the record says so.** Round 3's `BLOCKED` entry names the observer's own
sequencing mistake: both devices had finished a duel before the leaderboard was reached, so no
device could show the no-place state, and `ADR-0089` §3 forbids writing state to reach a screen. It
is filed anyway because the contradiction is between two artefacts that can be read side by side —
the card's second frame and the component's single branchless slot — and neither reading needs a
browser. `STORY-1211` §*The unwalked state* carries the full ruling.

**Why `low`.** A player with no place still reads the right sentence, in the right place, and
`ADR-0065` §4's *the page is identical across every self-line state* still holds. What diverges is a
tint, a border colour and a coin mark on a line that says a player has no coins to mark.

## Files

| File | Action |
| --- | --- |
| `web-client/src/ladder/LadderScreen.tsx` | edit |
| `web-client/src/ladder/LadderScreen.test.tsx` | edit |

## Scope

- **Branch the self line on the standing it already holds** — `self.rank === null` is the same
  condition `selfLine` uses, so the screen asks no new question of its state.
- **The no-place line takes the card's muted recipe**: `border-hairline`, no `bg-accent-subtle`, and
  `text-text-muted`.
- **No `CoinMark` in the no-place line.** The card's frame has none, and a coin before a sentence
  about having no place is the one part of this that could actually mislead.

## Out of scope

- **The sentence.** `NO_PLACE_THIS_SEASON` is `ladder-text.ts`'s and reads exactly as the card does.
  **Change no literal**, and do not add a digit — `ADR-0065` and `ladder-text.ts` both forbid `0`
  here.
- **The ranked self line.** `TASK-121002`'s merged case `the self line is the highlighted box the
  card draws` fixtures `self: { rank: 1, coins: 1 }` and must stay green **unedited** — `verify:`
  greps for it by name for exactly that reason.
- **The third state `ADR-0065` §4 names** — no self line at all, for a request with no known device.
  `state.self === null` already handles it and it is not this ticket's.
- **The row figures.** `TASK-121104`, which is this ticket's `depends_on` so two coders never hold
  `LadderScreen.tsx` at once.

## Tests

`LadderScreen.test.tsx`

| Test | Proves |
| --- | --- |
| `a player with no place reads the card's muted line, with no coin` | with `self: { rank: null, coins: null }`, the paragraph holding `NO_PLACE_THIS_SEASON` carries `border-hairline` and `text-text-muted`, carries **neither** `bg-accent-subtle` **nor** `border-accent`, and contains no `CoinMark` — the negative halves are the ones that fail today |

Two fixtures, not one: the ranked case above and this one, so a component that hard-codes either
recipe fails the other (`STORY-1210`'s own note on one-value tests).

## Acceptance criteria

- [ ] `LadderScreen.test.tsx > a player with no place reads the card's muted line, with no coin` passes
- [ ] `LadderScreen.test.tsx > the self line is the highlighted box the card draws` passes, unedited
- [ ] **Manual acceptance, the walk round 3 could not make:** on a browser profile that has finished
      no duel this season, open the leaderboard before playing anything — the self line is muted,
      hairline-bordered and carries no coin
- [ ] Every command in `verify:` exits 0
