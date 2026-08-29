# Test plan

The catalogue the [`qa`](../.claude/agents/qa.md) agent runs. One case per row of behaviour, each
with an **observation that can fail** — a case whose failure condition cannot be evaluated has not
been performed, which is `ADR-0088` §2's rule and the reason that ADR is worth more than the
sentence it replaced.

This document is pointed at by [`qa-cycle`](../.claude/skills/qa-cycle/SKILL.md) and by the `qa`
agent's definition. A document nothing points at is a document nothing runs.

## How a case is written

| Field | Means |
| --- | --- |
| **id** | stable, referenced by bug tickets forever — never renumbered |
| **do** | the driver verbs, in order |
| **expect** | the observation, stated so it can be false |
| **fails if** | the concrete failure — what makes this case red |

The driver is `node scripts/qa/drive.mjs <port> <verb>`; `A` is port 9232 and `B` is 9233. Verbs:
`open`, `text`, `click`, `wait`, `absent`, `type`, `link`, `device`, `forget-room`, `eval`.

**Every round gets fresh Chrome profiles.** Clearing `pd.roomCode` is not isolation: the server
re-seats a returning device by `pd.deviceId` (`ADR-0018`), so a reused profile rejoins its old room
with client storage cleared. Measured 2026-08-29.

---

## SMOKE — is the product alive?

Six cases. Run on every scope, always. If any fails, the run is `blocker` and the remaining suites
are pointless.

| id | do | expect | fails if |
| --- | --- | --- | --- |
| `SMK-01` | `stack.sh status` | all three of db, server, web report `up` | any reports `down` |
| `SMK-02` | `A open` | `#root` renders and the text contains `Create a duel room` | the page is blank — this is `ADR-0088` gap 1, which fails green in every other gate |
| `SMK-03` | `A device` and `B device` | two non-empty ids, and they differ | they are equal, or empty — two tabs are one player (`ADR-0018`) |
| `SMK-04` | `A click "Create a duel room"`, `A wait "Waiting for your rival"` | a room code and an invite link containing `?room=` | no link, or a link with no `room=` |
| `SMK-05` | `B open <link>`, `B wait "Blinds"` | B is seated at the table without typing a code | B lands on the lobby or shows *No duel room has that code* |
| `SMK-06` | `A text` | A's screen offers actions or shows the rival's turn | neither screen ever offers an action |

`SMK-02` and `SMK-03` exist because `ADR-0088` named them as covered by nothing: the root render is
executed by no test, and two storage partitions are outside every jsdom and JVM test's notion.

---

## CORE — the v0.1 spine

The vision's success condition, decomposed: *"Send a link. She opens it in a browser. We play a
full heads-up match. Someone wins. We hit Rematch."*

### Seating and identity

| id | do | expect | fails if |
| --- | --- | --- | --- |
| `CORE-01` | A creates; read both screens | exactly one seat shows `YOUR TURN` | both do, or neither does for 30s |
| `CORE-02` | B joins by **link** | B seated, nobody typed a code | B must type anything |
| `CORE-03` | fresh profile C, `open /?room=<code>` on a **full** room | a refusal naming the room, not a crash or a third seat | a third player is seated — the vision says *"Two players. Never three."* |
| `CORE-04` | `open /?room=NOSUCH0` | *No duel room has that code* | a blank screen, a hang, or a seat |
| `CORE-05` | A creates, B joins **by code** typed into the lobby field | same result as `CORE-02` | the code path and the link path disagree |

### The hand

| id | do | expect | fails if |
| --- | --- | --- | --- |
| `CORE-06` | read both screens each turn | the two screens never disagree about the board, the pot or either stack | any of the three differ at the same moment |
| `CORE-07` | act out of turn: `click "Fold"` on the waiting screen | the control is absent or disabled; nothing is sent | an out-of-turn action is accepted |
| `CORE-08` | play to a showdown | at least one hand reaches a showdown and both screens agree who won it | they name different winners |
| `CORE-09` | one player folds | the hand ends, the pot moves, **and the folded cards are shown to nobody** | a mucked card appears anywhere |

### Secrecy — the property the engine exists for

`CORE-10` is the highest-value case in this document. `ADR-0088` step 9 is its manual form.

| id | do | expect | fails if |
| --- | --- | --- | --- |
| `CORE-10` | capture A's two hole cards via `A eval`, then `B absent "<rank><suit>"` for the whole hand | A's cards appear nowhere in B's rendered DOM before the frame that reveals them | either card is legible on B at any moment before its reveal |
| `CORE-11` | a hand won **without** a showdown | the winner's cards are never revealed, on either screen | they are shown at all |

**Write `CORE-10` against the specific card, never a bare rank.** A naive `absent "J"` matches
stack digits, hand numbers and the word *Join*; a case that is red for the wrong reason teaches the
loop to ignore it. Read the pair from the DOM first, then assert on that exact pair.

### Result, coins and rematch

| id | do | expect | fails if |
| --- | --- | --- | --- |
| `CORE-12` | play to a winner | both result screens name the **same** winner and the same hand count | they disagree |
| `CORE-13` | `psql` the `duel_result` and `player` rows | winner `coin_delta=+1`, loser `-1`, balances agree with both screens | any of the three disagrees with the other two |
| `CORE-14` | a player whose only duel was a loss | balance reads `−1`, **unclamped** | it reads `0` — `ADR-0014`/`ADR-0015`, and `EPIC-03`'s own definition of done |
| `CORE-15` | both press `Rematch` | a new duel starts in the **same** room, stacks reset | either browser is left waiting on a screen the other has left |
| `CORE-16` | one presses `Rematch`, the other does not | the presser waits; no duel starts; the offer is restated after a reconnect | a duel starts one-sided, or the offer is lost |

### Reconnect

| id | do | expect | fails if |
| --- | --- | --- | --- |
| `CORE-17` | mid-duel, `A open` (reload) | A returns to the **same seat**, same stacks, same board | A loses its seat, or is dealt in fresh |
| `CORE-18` | during A's absence | B sees the away/absent marking (`ADR-0046`) | B is told nothing, or is told the rival *left* |
| `CORE-19` | A returns | B sees *Your rival is back.* | the mark never clears |

### Lobby

| id | do | expect | fails if |
| --- | --- | --- | --- |
| `CORE-20` | on the waiting screen, `click "Back to the lobby"` | A returns to the lobby and **the room stays open** — the link still works | the room closes, or the link stops working (`ADR-0073`) |

---

## Per-epic suites

Filled in when an epic is first tested, not before. A suite written from ticket titles rather than
from exercising the screens is a suite that tests the titles.

### Template

```
## EPIC-NN — <title>

Sources: the epic's Definition of done, and the ADRs its Open decisions table names.

| id | do | expect | fails if |
| --- | --- | --- | --- |
| NN-01 | <driver verbs> | <observation> | <the concrete failure> |
```

Three rules for writing one:

1. **One case per promise the epic made**, taken from its Definition of done. Not one per ticket —
   tickets are already gated by their own `verify:` blocks and re-testing them here is waste.
2. **The `fails if` column is the case.** If you cannot state what makes it red, the case is not
   ready. `ADR-0088` §2 is the model: eleven steps, each with a named failure.
3. **Cite the ADR** a case derives from, so a disagreement between the catalogue and a merged
   decision is visible as a disagreement rather than silently resolved by whoever wrote the case.

### Not yet written

| Epic | Status |
| --- | --- |
| `EPIC-01` poker engine | not written — largely covered by the engine's own suite; a browser cannot see it |
| `EPIC-02` duel server | not written — covered by `poker-server/.../e2e/` against real Postgres |
| `EPIC-03` web client | **the CORE suite above is it** |
| `EPIC-04` identity and profiles | not written — sign-up, sign-in, recovery, the name rules, history |
| `EPIC-05` ranking and leaderboard | not written — the ladder screen, standings order, rank after a duel |
| `EPIC-06` design system | not written — and mostly not testable this way; `qa` is told not to report styling |

---

## What this catalogue does not cover

Stated so nobody reads a `PASS` as more than it is:

- **The built bundle.** Every case runs against `npm run dev`. `dist/` is loaded by nothing here,
  which is `ADR-0088` gap 3 and survives this document exactly as it survived that ADR.
- **Performance, load and security.** Out of `EPIC-12`'s scope.
- **Anything `EPIC-06` owns** — placement, colour, type. `qa` is instructed not to report it.
- **A real network.** Everything is `localhost`; latency, packet loss and a proxy in front of the
  socket are untested.
