---
schema: 2
id: TASK-130911
title: The duel is paused. leaves the line that named it, and the sentence before it stays
type: task
status: backlog
parent: STORY-1309
module: web-client
estimate: S
tier: haiku
review: standard
files_touched: 3
labels: [client, table, copy]
depends_on: [TASK-130910]
verify:
  - cd web-client && npm ci && FORCE_COLOR=0 NO_COLOR=1 npm run --silent check
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/table/presence-text.test.ts 2>&1 | awk '/^ *Tests +[0-9]+ passed \([0-9]+\)$/ { n = $2 } END { exit !(n >= 5) }'
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/lobby/presence-copy.test.tsx 2>&1 | awk '/^ *Tests +[0-9]+ passed \([0-9]+\)$/ { n = $2 } END { exit !(n >= 3) }'
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/lobby/Lobby.test.tsx 2>&1 | awk '/^ *Tests +[0-9]+ passed \([0-9]+\)$/ { n = $2 } END { exit !(n >= 77) }'
  - sh -c '! grep -rqF "The duel is paused" web-client/src'
  - sh -c 'grep -qF "\"Your rival is away.\"" web-client/src/table/presence-text.ts'
  - sh -c 'grep -qF "Your rival did not come back. The duel continues, and the server acts for them." web-client/src/table/presence-text.ts'
  - sh -c '! grep -rqiF "forfeit" web-client/src/table/presence-text.ts'
  - python3 .github/scripts/lint_tickets.py
---

## Goal

The line under the table says *Your rival is away.* and stops there — the second sentence goes with
the pause it named, and no new string is minted to stand in its place.

## The derivation, written down so it can be argued with

`ADR-0108` §4 retires the pause: *"The duel never pauses … no action of the present player is
refused because a rival's socket dropped."* `ADR-0113` §7 deletes `Room.isPaused` and the
`DUEL_PAUSED` enum entry, and `TASK-130805` landed both. So the client's *The duel is paused.* is
now a client asserting a game fact the server cannot state — the one thing `CLAUDE.md` forbids
outright.

**What replaces it is derived under `ADR-0046`'s register, not invented.** `ADR-0108` §5 assigns
the derivation to this story and bounds it: *"No new strings are chosen here … what stands in its
place is derived under `ADR-0046`'s register by the story that lands it."* The register is §2's
table, and its `AWAY` row is two sentences with two jobs — the first states the fact, the second
*"answers what does this mean for me"*. Only the second lost its occasion. `ADR-0108` §4 says what
now answers that question, and it is not a sentence: *"The present player's answer to how long will
I wait? is the rival's clock — one countdown, one meaning."* That clock is on screen as of
`TASK-130910`.

So the line keeps its first sentence and loses its second. Three things this is deliberately not:

- **Not a deletion of the whole row.** §2's table distinguishes *a line* from *nothing at all* — its
  last row is explicitly the second — and the `AWAY` row is not that row. The redundancy with §1's
  `Away` status word is merged and deliberate: both ship today, and §6 leaves resolving that kind of
  redundancy to the design, not to a copy ticket.
- **Not a new sentence.** *Your rival is away.* is `ADR-0046` §2's own text, minus a clause. Had the
  register needed a sentence it cannot produce, this ticket would be a stop and an ADR
  (`ADR-0108` §5); it does not.
- **Not an answer to `DEC-108`.** *"When the table says* The duel is paused.*, may the action bar
  stay enabled?"* — open, the product owner's. This ticket removes the sentence the question is
  asked about and records nothing about the bar. Whether landing it moots the question is the
  product owner's to say when they answer.

**`forfeit` appears nowhere**, in copy or in prose (`ADR-0046` §5, kept false by `ADR-0108` §3), and
a gate counts it in the copy module.

## Files

| File | Action |
| --- | --- |
| `web-client/src/table/presence-text.ts` | modify |
| `web-client/src/table/presence-text.test.ts` | modify |
| `web-client/src/lobby/presence-copy.test.tsx` | modify |
| `docs/adr/ADR-0046-the-table-says-away-timed-out-and-back.md` | read — §§2, 5 |

## Scope

- **`presence-text.ts`:** the `AWAY` case returns `"Your rival is away."`. The `ABSENT` and
  `PRESENT` cases, the `returned` bookkeeping and the default are untouched — a gate greps the
  `ABSENT` sentence unchanged, because *"The duel continues, and the server acts for them."* is
  still true and still needed.
- **The KDoc gains one sentence** saying the second clause left with the pause (`ADR-0108` §4) and
  that the rival's clock is what answers the question it used to. Cite `ADR-0046` §2 and
  `ADR-0108` §§4–5. Do not write the retired sentence even to quote it — the gate greps the
  repository for it as a fixed string and cannot tell prose from copy.
- **`presence-text.test.ts`: two assertions move and nothing else.** `says the duel is paused while
  the rival is away` is renamed to `says the rival is away, and says nothing about a pause` and
  expects `"Your rival is away."`; `lets the state the server sent outrank the return` keeps both of
  its assertions and updates the `AWAY` one. The file's count does not change — no test is added and
  none is removed, and no assertion is weakened: each still pins a whole string, exactly.
- **`presence-copy.test.tsx`: one constant moves.** `AWAY_LINE` becomes `"Your rival is away."`.
  `THE_THREE_LINES`, `FOUR_STATES`, the fifteen refused-word patterns and all three tests are
  otherwise untouched, and the file's count does not change. The comment above `AWAY_LINE` still
  reads *`ADR-0046` §2, quoted verbatim* and stays right, because it is.
- **Measured, not assumed:** returning `"Your rival is away."` from that one branch reddens exactly
  **three** tests in exactly these two test files — `presence-copy.test.tsx`'s *says exactly one of
  the four lines*, and `presence-text.test.ts`'s two — out of the 1 062 in the suite. `Lobby.test.tsx`
  and `PresenceNotice.test.tsx` stay green, and both are pinned to prove it.

## Out of scope

- **`PresenceNotice.test.tsx`.** It stays green through this change — its `/paused\.\d/` assertion
  is about a digit that left in `TASK-130805` — but it is left testing a sentence that no longer
  exists. `TASK-130912` retires it, in its own diff, so this one is not four files.
- **`seat-status.ts`.** `Away` and `Timed out` keep their seats (`ADR-0046` §1), and its comment's
  use of the word *pause* is `ADR-0046` §1's own reasoning about a seat nobody is sitting at —
  legitimate prose, and the reason the gate greps the sentence rather than the word.
- **`ActionBar.tsx`'s comment** recording that the paused-duel refusal no longer exists. History,
  correctly kept.
- **`docs/test-plan.md`.** Retired in `TASK-130812`, merged.
- **`DEC-108`.**

## Tests

No test is added and none is removed; **two assertions and one constant move**, each named above.

`presence-text.test.ts` (5) and `presence-copy.test.tsx` (3) keep their counts. `Lobby.test.tsx`
(77) is pinned unmoved.

| Test | What moves, and what does not |
| --- | --- |
| `presence-text.test.ts` · `says the rival is away, and says nothing about a pause` | renamed from `says the duel is paused while the rival is away`; expects `"Your rival is away."` exactly. Still a whole-string equality |
| `presence-text.test.ts` · `lets the state the server sent outrank the return` | its `AWAY` assertion expects the new sentence; its `ABSENT` assertion is unchanged, character for character |
| `presence-text.test.ts` · the other three | unchanged |
| `presence-copy.test.tsx` · all three | unchanged; only the `AWAY_LINE` literal they read moves |

## Acceptance criteria

- [ ] `presence-text.test.ts` reports at least **5** passing tests and none failing
- [ ] `presence-copy.test.tsx` reports at least **3** passing tests and none failing
- [ ] `Lobby.test.tsx` still reports at least **77** passing and none failing
- [ ] No file under `web-client/src` contains the string `The duel is paused`
- [ ] `presence-text.ts` returns `"Your rival is away."` and still returns the `ABSENT` sentence
      unchanged
- [ ] `presence-text.ts` contains no `forfeit`, in any case
- [ ] `cd web-client && npm run check` exits 0
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
