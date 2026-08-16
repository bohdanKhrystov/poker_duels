---
id: STORY-0311
title: The profile strip — my coins and my recent duels
type: story
status: ready
parent: EPIC-03
module: web-client
labels: [client, http, profile]
depends_on: [STORY-0302, STORY-0303]
---

## Goal

The lobby shows who you are to the server: your duel-coin balance and your last few results, read
over plain HTTP with the device id the socket already owns.

## Why

`STORY-0211` shipped the read path on the argument that *a value nobody can ask for is a value that
does not exist*. It is asked for here or the coin remains invisible — and a coin nobody sees is not
the reward `docs/vision.md` says the game is played for.

## Design notes

- `GET /api/me` answers `{playerId, coinBalance}`. `GET /api/me/duels?limit=` answers
  `{duels: [...]}`, each `{duelId, opponentPlayerId, outcome, coinDelta, handsPlayed, finishedAt}`
  with `outcome` one of `"WON"`, `"LOST"`, `"DREW"`. `limit` defaults to 10 and is capped at 50;
  zero, negative and non-numeric are `400`, so the client sends a constant it knows is valid or no
  parameter at all.
- Both authenticate with the **`X-Device-Id` header** — the same id `STORY-0303` persists, read from
  the key that module exports. Two storage keys would be two identities for one player.
- **`coinBalance` is signed and may be negative** (`ADR-0014`): `−1` is a correct answer and is
  rendered as `−1`. Never clamped at zero, never shown as `0`, never hidden when negative.
- `401` means the stored id is absent or unknown to the server. That is the ordinary state of a
  first visit, so it renders as *no profile yet*, not as an error — the socket handshake will mint
  an id, after which the read succeeds. The endpoint answers `401` identically for absent, blank and
  unknown on purpose; the client must not try to tell them apart.
- An empty `duels` array is a legitimate answer for a new player (not `404`) and renders an empty
  state.
- **No opponent name is rendered.** The server returns none today; `ADR-0021` adds a display name
  and `DEC-017` decides its product rules. Printing a raw `opponentPlayerId` would pre-empt both and
  put a UUID in front of a player, so a result line shows the outcome, the coin delta, the hand
  count and when it finished — and nothing about who.
- `finishedAt` is an ISO-8601 UTC instant; format it in the browser's locale for display and keep
  the raw value out of the DOM's visible text.
- **These are not `ServerMessage`s.** They carry no `type` discriminator and are not in
  `protocol.gen.ts`; `docs/protocol.md` is their contract. Their shapes are declared **once**, in
  this HTTP module — the single hand-written wire shape the client is allowed, named here so a
  reviewer reads it as the stated exception rather than a violation.

## Where the fetch lives

The client has no HTTP layer today: `src/protocol/` owns the socket and nothing else. Decided here,
because the existing structure answers it and because each part is one file to replace:

- **`web-client/src/profile/`** — a new directory beside `src/lobby/`, `src/table/` and
  `src/result/`, holding the two response shapes, the two reads, the words and the component. It is
  *the HTTP module* `EPIC-03`'s non-negotiables already name. Not `src/protocol/`: that module is
  defined by the frame it decodes and is exempt from the boundary guard by path, so putting
  hand-written shapes inside it would widen the one place in this client that answers to nobody.
- **Everything is injected** — the `fetch` and the `Storage` are parameters, exactly as
  `openConnection({ socket, storage, onMessage })` takes its socket. No module here names a global,
  so no test reaches the network and `DEC-032`'s inert `localStorage` never comes up.
- **The read runs above the tree, in a provider whose context defaults to `null`**
  (`TASK-031109`). `ADR-0032` settled the half that matters — profile data *"is not a frame and does
  not enter this store"* — and left *"how HTTP profile data reaches screens"* to this story. The
  duel store is therefore out; so is `bootDuelClient`, which exists because a socket must be exactly
  one per tab and outlive every remount, and a read-only `GET` needs neither. A context that answers
  `null` rather than throwing is what keeps `Lobby.test.tsx` and `App.test.tsx` rendering exactly
  what they render today: no provider, no strip, no request.
- **What is *not* decided here:** whether the client grows a shared HTTP data layer — a cache, a
  second store, refresh-on-focus. `EPIC-04` is a whole epic of HTTP and owns that question. This
  story's seam is one file and is meant to be replaced by it.

`DEC-032` (the architect's, due *before* this story) is not blocking: it asks whether anything ever
exercises a **real** `Storage` and whether a developer Node version is pinned. Every ticket here
injects a `Storage`, which is the way out `TASK-030304` already took, so nothing in this story waits
on it.

## Hazards these tickets are written against

Named because each has cost this epic a round already:

- **Two distinct inputs, always.** A value asserted only at a fixture's default cannot be told from a
  hardcoded constant. Every read test uses two ids, two paths, two balances.
- **A universal name is a promise to enumerate.** *Every outcome*, *the delta is signed*, *a bad row
  is refused* — each is written out in full (`TASK-031104`), not sampled.
- **A guard is proven by planting a real violation.** Each ticket names the edit that turns it red
  and asks for the failure in the PR body.
- **The minus is U+2212**, as `result/outcome-text.ts` already prints it, asserted as an escape so
  an ASCII hyphen cannot pass by looking right.

## Tasks

| ID | Title | Status |
| --- | --- | --- |
| [TASK-031101](../tasks/TASK-031101-one-get-carrying-the-device-id-the-server-reads.md) | One GET, carrying the device id, with three answers | backlog |
| [TASK-031102](../tasks/TASK-031102-the-profile-read-states-the-balance-the-server-sent.md) | The profile read states the balance the server sent, sign and all | backlog |
| [TASK-031103](../tasks/TASK-031103-the-recent-duels-read-drops-the-opponent.md) | The recent-duels read keeps every field but the opponent's identifier | backlog |
| [TASK-031104](../tasks/TASK-031104-every-outcome-every-sign-and-what-the-parse-refuses.md) | Every outcome, every sign, and what the duel parse refuses | backlog |
| [TASK-031105](../tasks/TASK-031105-the-words-a-duel-line-is-made-of.md) | The words a profile line is made of | backlog |
| [TASK-031106](../tasks/TASK-031106-one-read-answers-the-whole-strip.md) | One read answers the whole strip, or none of it | backlog |
| [TASK-031107](../tasks/TASK-031107-the-strip-states-the-balance-or-says-there-is-none.md) | The strip states the balance, or says there is no profile yet | backlog |
| [TASK-031108](../tasks/TASK-031108-one-line-per-recent-duel-and-a-word-when-there-are-none.md) | One line per recent duel, and a word when there are none | backlog |
| [TASK-031109](../tasks/TASK-031109-the-read-runs-once-above-the-tree-and-nowhere-else.md) | The strip's read runs once above the tree | backlog |
| [TASK-031110](../tasks/TASK-031110-the-lobby-shows-the-strip-and-the-duel-does-not.md) | The lobby shows the strip, and a duel in progress does not | backlog |
| [TASK-031111](../tasks/TASK-031111-the-strip-names-no-opponent-and-counts-no-coin.md) | The strip names no opponent and counts no coin | backlog |

The chain is strictly linear: `031101 → … → 031111`, each one startable when the one before it
merges. Cumulative test counts run **326 → 358** from a measured baseline of 316, with two
`STORY-0310` follow-ups (`TASK-031014`, `TASK-031015`) landing first and taking it to 320.

## Acceptance criteria

Each one closes with a named test, so none of them is a judgement call:

- [ ] A balance of `−1` renders as `−1` — `TASK-031102` (`takes the balance the server sent, sign
      and all`) and `TASK-031107` (`states the balance the server sent`).
- [ ] `401` renders the no-profile state and no error alert — `TASK-031106` (`answers no-profile
      when either half says so`) and `TASK-031107` (`says there is no profile yet, and raises no
      alarm`).
- [ ] An empty `duels` array renders the empty state, and a populated one renders outcome, coin
      delta, hand count and time per row — `TASK-031108`, both tests.
- [ ] Both requests carry `X-Device-Id` taken from the same storage key the socket module owns —
      asserted, not assumed: `TASK-031102` (`asks /api/me with the device id from the key the socket
      module owns`) and `TASK-031103` (`asks /api/me/duels with no limit of its own`), over
      `TASK-031101`'s `sends the device id it was given`.
- [ ] No result row contains an opponent identifier — `TASK-031103` (dropped at the parse) and
      `TASK-031111` (`puts no opponent identifier anywhere on the screen`).
- [ ] The strip is on the lobby and nowhere else — `TASK-031110`.

## Out of scope

- Paging, filtering, search, a full duel history — `EPIC-04`.
- Leaderboard, rating, season standing — `EPIC-05`.
- Display names — `ADR-0021` and `DEC-017`, and no name is on the wire yet.
- Replay of a listed duel — `EPIC-08`.
- Any socket traffic: this story speaks HTTP only.
