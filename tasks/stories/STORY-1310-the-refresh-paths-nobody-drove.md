---
id: STORY-1310
title: The refresh paths nobody drove, driven and written down
type: story
status: ready
parent: EPIC-13
module: web-client
labels: [client, qa, refresh]
depends_on: [STORY-1301]
---

## Goal

Each of the six refresh and navigation paths `ADR-0112` §6 names is driven against a running stack
and its result written down, so `EPIC-13` closes on evidence rather than on a symptom nobody looked
for again.

## Why

**The human reported something and it did not reproduce.** `EPIC-13` measured five paths on
2026-09-02 with `scripts/qa/drive.mjs` and `location.reload()`: a host in a live duel on a bare `/`,
a rival on `?room=CODE`, a host on the waiting screen, and `#/leaderboard` on a room-free browser all
**survive**. What reproduced was the inverse — a browser **holding a room** having its fragment
erased.

**The epic refuses to close on that.** Its Definition of done: *"The reported refresh symptom is
either reproduced and fixed, or recorded as not reproducible with the paths that were tried written
down — `EPIC-13` does not close on a symptom nobody looked for again."* `ADR-0112` §6 turns that into
a list and adds the sentence this story exists to satisfy: ***"A dismissal without the attempt does
not satisfy the DoD row."***

**Its findings feed `STORY-1311`**, and one of them —
[`ADR-0086`](../../docs/adr/ADR-0086-the-offers-answer-is-one-key-owned-beside-the-predicate-it-feeds.md)
§6's accept path — is a collision `ADR-0112` derived rather than observed, and which it resolved in
`ADR-0086`'s favour. A derivation is not a measurement.

**It runs early, before `STORY-1302`**, because two of the six paths are driven in a **waiting**
state and `STORY-1302` retires the waiting screen. Measuring the product the human reported on is the
point; `STORY-1311` covers whatever `STORY-1302` changes.

## Design notes

- **The six paths, verbatim from `ADR-0112` §6**, each owed a written result:
  1. a refresh **on the result screen** — a held `FINISHED` room, `outcome` standing;
  2. a refresh **during a runout** — `ADR-0102` §5 says the reload jumps to the end; confirm no lobby
     shows on the way;
  3. a **genuinely dropped socket** — a reconnect through `reconnecting.ts`, **not** a reload;
  4. **real latency**, where the rejoin round-trip is visible — a lobby flash localhost sampling
     could not see;
  5. the **`AccountOffer` accept path** of `ADR-0112` §5, whose failure is so far derived rather than
     observed;
  6. a **mailed link opened while a room is held**, in both a waiting and a playing state.
- **A browser drives this client for a QA round, never for a gate** —
  [`ADR-0089`](../../docs/adr/ADR-0089-a-browser-drives-this-client-for-a-qa-round-never-for-a-gate.md)
  §2's three standing conditions: **no dependency, no gate, no coverage claim.** So **no ticket here
  may put a browser drive in a `verify:` block**, and no result here may be cited as coverage. The
  deliverable is a record; the gates are the ordinary suites plus
  `python3 .github/scripts/lint_tickets.py`, and the readings are pasted into the PR body as text —
  the shape `STORY-1215` used and `ADR-0106` §4 licensed.
- **The record lives in this story and its tickets**, as a path-by-path table with the observed
  result, the commit driven, and the stack it ran against. A path that cannot be driven says so and
  says why — that is a result, not a gap.
- **A defect found is a ticket, not a widening.** If a path reproduces a lobby flash, a lost screen
  or a spent token, that becomes its own ticket under this story or `STORY-1311`, with a **non-browser
  gate** — a unit or integration test that fails on the defect and passes on the repair. If only a
  browser can see it, it is filed the way `EPIC-12` files a defect and is repaired against a
  reproduction by hand (`ADR-0089` §4).
- **What is already measured is not re-measured.** The five paths in `EPIC-13`'s *What is already
  true* stand; this story adds the six that were not covered.
- **A mailed link refused mid-duel must not spend its token** (`ADR-0112` §5) — path 6 checks the
  token is still usable after the duel, because that is the one path whose failure is silent and
  permanent.

## The record

**This is the deliverable.** One row per path, filled in by the ticket that drives it, and nothing
here is a gate: `ADR-0089` §2b forbids a browser standing between a pull request and `develop`, so
what a `verify:` block can check is that the row exists, that it is no longer a placeholder, and
that the ordinary suites are still green. Whether the sentence in it is *true* is a human's verdict
on the readings pasted into the PR body — the shape `STORY-1215` used and `ADR-0106` §4 licensed.

The placeholder is the word standing in the `result` cells below. It appears in this file only
inside this table, so a count of it is a count of paths still owed.

| id | path | result | commit | stack |
| --- | --- | --- | --- | --- |
| `P1` | a refresh **on the result screen** — a held `FINISHED` room, `outcome` standing | OBSERVED (not inferred), at `bare`: the result screen SURVIVES. A genuine `Page.navigate` reload (confirmed by a fresh `performance` navigation entry of type `navigate`) put A back on `Defeat / −1 duel coin / 1 hand · You 0 · Your rival 20,000` immediately — `open`'s first paint already showed it, and the single `record`/`frames` frame afterward never mutated; `location.hash` read empty both before and after. This contradicts this row's own prior partial reading, which found the lobby's *"No duel room has that code."* settled after the same reload — two honest drives disagree and neither explains why. `delayed 300ms` could NOT be driven: `kill` and `pkill` were denied in this session (contradicting the ticket's brief that kill is available outside a worktree), so the leftover bare Vite on `[::1]:5173` could not be freed for the relay; `curl` then confirmed the exact trap the ticket warned of — `localhost:5173` still resolved to `::1:5173` (the stale Vite) even with the relay correctly bound on `127.0.0.1:5173` — so no delayed reading was taken, because it would not have been trustworthy. | `e364f4c6` | `bare` only — `delayed 300ms` attempted, blocked by the `kill` denial above |
| `P2` | a refresh **during a runout** — `ADR-0102` §5 says the reload jumps to the end; no lobby on the way | OBSERVED (not inferred), both layouts, same method throughout — preflop `All in`/`Call` (this ticket's own shortest route), the longest runout available (4 steps × 600 ms = 2.4 s), which on this equal-stack duel also ends the whole match in one hand. `bare` (profiles A/B, CDP 9226/9227): undisturbed baseline — `record` armed before the call, then the call, then `frames` — 7 frames, correctly staged (Preflop → Flop 3 cards → Turn 4 → River 5 → *Hand complete* + award line → outcome `Defeat`), verbatim in the PR body. Reload run (`Rematch`, roles rotated): the triggering `Call 10,000` timestamped 843.644–843.710s; a `text` check on the shover's own tab, immediately after, returning at 843.770s, read street label `Flop` — confirming by direct reading, not assumption, that the page was still mid-runout. `open` fired 2 ms later (843.772–844.099s): its own stdout, the first paint, read `Victory / +1 duel coin / 1 hand · Your rival 0 · You 20,000 / Rematch …` directly — no lobby. `record` armed immediately after, `frames` 3 s later: exactly one frame, byte-identical to `open`'s own text — no transition, ever. `location.href`/`location.hash`: `http://localhost:5173/` / `` throughout. `delayed 300ms` (fresh profiles, CDP 9230/9231; re-verified first with a real browser load — `performance` navigation `type: "navigate"`, relayed body byte-identical to the direct `:5273` fetch): triggering `Call 10,000` at 754.572–754.648s; a `text` check on the shover's own tab, after a scripted 1 s pause, returned at 755.663–755.740s (~1.09 s after the call began) and read street label `Flop` — the same direct-reading confirmation as `bare`, well inside the 2.4 s-plus-latency window. `open` fired 2 ms later (755.742s) and did **not return until 767.579s — 11.8 s later** — and its own stdout, the first paint, was the **lobby**: `Poker / Duels / Create a duel room / Room codeJoin the duel / Your duels / Leaderboard / Account`. `record` armed immediately after (frame 1, unchanged); `frames`, read once settled, holds four: (1) that lobby, (2) the lobby again with the profile loaded (`No name / 2 Duel coins / Won +1 …` ×2), (3) **`Waiting for your rival / NF6KCQPA / Invite link …`** — a waiting-room screen for the room whose duel had already finished — (4) `Victory / +1 duel coin / 1 hand · Your rival 0 · You 20,000 / Rematch …`, the correct settled state, matching `text` read afterward; `location.href`/`location.hash` were `http://localhost:5173/` / `` once settled, same as `bare`. No recovery duration is claimed: the gap between arming `record` and reading `frames` ran long on this drive for reasons on this end (time between shell invocations), not the app, so only the sequence and the endpoint are asserted, not a timing. **Answering `ADR-0112` §6 and `ADR-0102` §5 separately, per layout, as this ticket's Scope requires**: on `bare`, no lobby showed on the way, in the first paint or after, and the board came back finished with nothing in between. On `delayed`, a lobby **did** show on the way — in `open`'s own first-paint stdout, not merely caught by a `record` armed afterwards — followed by a stale waiting-room screen for the already-finished room, before the client corrected itself; the board still came back finished in the end, so `ADR-0102` §5 holds on both layouts (nothing wrong is ever asserted, only late), but `ADR-0112` §6's "no lobby on the way" holds only on `bare` and fails on `delayed`. **Bears on `P1`, not settling it**: `P1`'s two honest `bare` drives disagree — one saw the result screen survive with nothing on the way, the other saw a lobby's *"No duel room has that code."* settle after the reload. This reading does not say which of those was right, but it shows a mechanism that could produce either: under real latency this same client visibly paints a lobby-shaped screen, twice over, before correcting itself, so a drive whose timing happened to land in that window would report a lobby and one that didn't would not. | `abd9420f` | `bare` (CDP 9226/9227) and `delayed 300ms` (CDP 9230/9231) — both driven to a finished duel, fresh profiles each, same commit |
| `P3` | a **genuinely dropped socket** — a reconnect through `reconnecting.ts`, not a reload | BLOCKED, not read cleanly at either delay — attempted per Scope: two fresh `mktemp -d` profiles through the relay, a room, a `PLAYING` duel driven to the flop (board dealt, two stacks on screen) on both `delayed 0ms` and `delayed 300ms`; `X record` and `Y record` armed before every cut, per Scope's ordering. `node scripts/qa/delay.mjs cut 6173` and `cut 6174` each answered `cut 4` on every invocation (three cuts total) — four live pairs each time (two tabs × an HMR socket and an app socket), proving a live pair was severed, not an already-dead one. **The relay's own named risk is settled, OBSERVED directly**: a fresh `curl` to the relay's port immediately after each cut returned `http=200` with the same body size and the same latency as before the cut, on both relays — neither went deaf. **A second, deeper instrument fault was found instead, and reproduced 4/4 (both seats, both delays)**: the cut also severs Vite's dev-mode HMR socket, multiplexed through the same port, and the page performs an actual reload — OBSERVED, not inferred: `performance.getEntriesByType('navigation')[0].type` reads `reload` after the cut (it read `navigate` at the initial `open`); `document.readyState` cycles `complete` → `interactive` → `complete`; a JS-realm marker planted immediately before the cut, polled inside one shell invocation so the elapsed time is trustworthy, survived to `t+366ms` but was gone by `t+710ms` at `delayed 300ms`, and was already gone at the first poll, `t+25ms`, at `delayed 0ms` — inside `retryDelayMillis`'s own first-attempt window, so the reload pre-empts any chance to see `reconnecting.ts` recover in place before the page it runs in is gone. `window.__pdFrames` reads `undefined` afterward on all four tabs driven — the armed `MutationObserver` does not survive a reload, so `frames` answers `record was never armed on this page` every time; no transcript can span the cut and the return, because there is no single page instance left to hold one. **Ruled out as `reconnecting.ts`'s own doing**: the file was read in full — its `onclose` only calls `scheduleRetry()` → `setTimeout(attach, delay)`, which opens a new `WebSocket` in place; there is no navigation call anywhere in it. **Neutralizing the reload without touching source was attempted and failed**: `eval` redefining `location.reload`/`assign`/`replace` to no-ops throws `TypeError: Cannot redefine property` on all three — Chrome enforces them `[LegacyUnforgeable]`, so no page script, this one included, can suppress the navigation, which independently confirms it as a browser-level event outside `reconnecting.ts`'s own reach either way. The rival seat is not a clean control either: the same cut severs its pair too, so `Y` reloads in lockstep with `X` (same `reload` navigation type, same wiped `__pdFrames`) — "the rival's screen keeps running with a clock" could not be read, because the rival's page is gone as well. What both seats show once settled (12–15s later, by `document.readyState`) is the server's authoritative state resumed correctly — a later hand under way, stacks moved, `The server checked for you`/`…for your rival` messages from the disconnected window — reassuring on its own, but it is the reload-and-rejoin path this ticket exists to exclude, not `reconnecting.ts`'s. **Left out, not verified**: a first-paint capture of the reload itself — there is no `open`-equivalent stdout for a reload the page triggers on its own, so whatever showed between `interactive` and `complete` was not caught; a direct trace naming Vite's HMR client as the reloading agent by name — `drive.mjs` has no console-dump verb, and any page-global buffer set up to catch one would itself be wiped by the same reload before it could be read back, inside the `366–710ms` (`delayed 300ms`) or `<25ms` (`delayed 0ms`) window available. **This needs a decision no merged source settles** — whether `P3` is driven against a build without Vite's HMR client, or the relay is made to spare that socket, or some other instrument change — so a `DEC` belongs in `docs/adr/README.md`'s `## Open decisions`; not registered here, outside this file's table. | `a003d4cb` | `delayed 0ms` (CDP 9633/9733; a second relay, `5174`→`5273`/`6174`, since the standing one was already running at `300ms` and could not be restarted) and `delayed 300ms` (CDP 9433/9533, the standing relay) — both driven, fresh profiles each, same commit; neither produced a valid `P3` reading |
| `P4` | **real latency** — the rejoin round trip made wide enough to see | OBSERVED (not inferred) at `delayed 300ms` and `delayed 1000ms`, on both the host's bare `/` reload and — at `1000ms` only — the rival's `?room=CODE` reload: the pre-join **waiting-room** screen (`Invite link`, the room code, `Back to the lobby`) renders over a room that is actually `PLAYING`, before the live table settles, and the window this holds grows with the delay — `open` itself returns that still-lobby-shaped first paint after ~11.8s at `300ms` (total reload-to-settled ~13.5s), and at `1000ms` exceeds `open`'s own 20s ceiling on an empty `#root` before the same waiting-room screen and then the table arrive (total reload-to-settled ~44.3s host, ~49.5s rival, the latter confounded by the host's own timebank expiring mid-observation, so that reading is a real reload but not a clean one); at `delayed 0ms`, neither `open`'s own 354ms-resolution capture nor a `record` armed 75ms later ever saw anything but the already-settled table, and this ticket can no more rule out a flash faster than that than `EPIC-13` could, so it stays an open INFERENCE, not an observed negative; commit `c9877d88`; the finding is handed to `STORY-1311`, `ADR-0114` §5 the merged mechanism nearest it. | `c9877d88` | `delayed 0ms` (CDP `9800` host / `9801` rival-to-seat, relay `5174`→`5273`/`6174`; host driven) and `delayed 300ms` (CDP `9802` host / `9803` rival-to-seat, relay `5173`→`5273`/`6173`, the standing relay; host driven) and `delayed 1000ms` (CDP `9804` host / `9805` rival, relay `5175`→`5273`/`6175`; both driven, rival via `?room=CODE`) — three separate rooms/duels, fresh `mktemp -d` profiles each, same commit |
| `P5` | the **`AccountOffer` accept path** of `ADR-0112` §5, so far derived rather than observed | OBSERVED (not inferred), identically on `bare` and `delayed 300ms`: pressing the accept (`X click "Keep them with a password"` on the winner's result screen) matches **neither** named outcome — not §5's landing on the account screen, not §Context's derived bounce. For at least 15s after the click on each layout, three independent signals never move: `location.href`/`location.hash` stay `http://localhost:5173/` and empty, the single `record`/`frames` capture of `#root` never mutates past its first entry, and the CDP target list keeps exactly one `page` (no second tab opened). A later `open` reload of that same screen shows the offer gone on both layouts — settled by the press, though no no-click control was run to rule out "any reload drops it regardless," so that attribution is the one inference in this row, flagged as such rather than stated as fact. On `delayed` only, that reload's first paint (via `open`, not a polled sample) was the lobby, reproduced on two separate reloads, each recovering to `Victory` within `X wait "Victory" 8000` (exit 0 both times) — a flash `bare`'s equivalent reload never showed, landing on `Victory` directly both times it was checked. The un-clicked-offer-plus-reload scenario never arose inside this script's own order (the click always precedes any reload here) and is left open rather than manufactured. Winner noted for every reading, both layouts: the room's host/button, the seat that shoved all-in preflop — the offer showed only on the winner's screen, never the loser's. This is the ticket's own named `DEC` trigger — the press spends the offer and lands nowhere, and no merged source says what should happen next — so a `DEC` belongs in `docs/adr/README.md`'s `## Open decisions`; not registered here, outside this file's table. | `1a09fb46` | `bare` (profiles on CDP 9222/9223) and `delayed 300ms` (CDP 9224/9225) — both driven to a winner, fresh profiles each, same commit |
| `P6a` | a **mailed link opened while a room is held** — the room **waiting** | NOT-YET-DRIVEN | — | — |
| `P6b` | a **mailed link opened while a room is held** — the room **playing** | NOT-YET-DRIVEN | — | — |

**The `stack` cell names which of two the reading came from**, because on one of them a green
reading means nothing:

- `bare` — Vite on `5173`, the server on `8080`, no relay. The product as it ships, and the reading
  that counts when something **is** seen.
- `delayed <n>ms` — Vite moved to `5273` and `scripts/qa/delay.mjs` listening on **`5173`** in front
  of it, so the browser's origin, the invite link and every hard-coded `5173` inside `drive.mjs`
  are unchanged. The reading that counts when **nothing** is seen.

**Why the second reading is not optional.** `drive.mjs`'s `wait` and `absent` sample `#root` every
250 ms, and `record` cannot be armed across a page load — its `MutationObserver` dies with the
document. On localhost the whole rejoin round trip is shorter than one sample, so *"no lobby
appeared"* on a bare stack is a statement about the instrument and not about the product. `EPIC-13`
already recorded exactly that: *"No lobby flash was observable at the sampling resolution
`drive.mjs` allows."* So a path whose finding is a **negative** is driven twice, and its row says
which reading it is.

**What a reload can be observed with, given that.** `open` prints `#root`'s text the moment it first
has content — the first paint, and the only pre-frame observation available across a navigation;
`record` armed immediately after `open`, then `frames`, catches every transition from there on.
Both belong in the PR body verbatim.

## Tasks

Split on 2026-09-04 into **nine** tickets, one chain. The first two build the instrument, because
five of the seven readings are races a bare localhost stack cannot resolve, and one of them — a
socket that drops without the page dying — has no driver verb at all today.

**A tenth was inserted on 2026-09-04**, after `TASK-131003`'s coder failed at the drive's *first*
command and changed no files. `scripts/qa/stack.sh:23` writes the postgres container's name down as
`poker_duels-postgres-1`, but compose names containers after the project, which defaults to the
checkout directory's basename — so in a worktree the container is `agent-<id>-postgres-1` and
`db-up`'s readiness check polls a name that does not exist, sixty times, before dying with a message
naming the wrong cause. **`db-up` cannot succeed in any worktree**, which is where every drive in
this story runs; `TASK-131003`'s own acceptance says the measurement is the implementer's, so the
script has to work where the implementer is. `TASK-131010` fixes it and everything from `TASK-131003`
down now hangs off it.

| ID | Title | Status |
| --- | --- | --- |
| [TASK-131001](../tasks/TASK-131001-a-loopback-relay-puts-milliseconds-in-front-of-the-stack.md) | A loopback relay puts milliseconds in front of the stack | ready |
| [TASK-131002](../tasks/TASK-131002-the-relay-learns-to-cut.md) | The relay learns to cut, so a socket drops without the page dying | backlog |
| [TASK-131010](../tasks/TASK-131010-the-stack-asks-compose-what-it-named-the-container.md) | The stack asks compose what it named the container | ready |
| [TASK-131003](../tasks/TASK-131003-p1-a-refresh-on-the-result-screen.md) | `P1` — a refresh on the result screen | backlog |
| [TASK-131004](../tasks/TASK-131004-p5-the-account-offers-accept-is-observed.md) | `P5` — the account offer's accept, observed rather than derived | backlog |
| [TASK-131005](../tasks/TASK-131005-p2-a-refresh-during-a-runout.md) | `P2` — a refresh during a runout | backlog |
| [TASK-131006](../tasks/TASK-131006-p3-a-genuinely-dropped-socket.md) | `P3` — a genuinely dropped socket | backlog |
| [TASK-131007](../tasks/TASK-131007-p4-the-rejoin-round-trip-made-visible.md) | `P4` — the rejoin round trip made visible | backlog |
| [TASK-131008](../tasks/TASK-131008-p6-a-mailed-link-over-a-held-room.md) | `P6a`/`P6b` — a mailed link over a held room | backlog |
| [TASK-131009](../tasks/TASK-131009-the-record-read-whole.md) | The record read whole, and every finding given an owner | backlog |

## Acceptance criteria

- [ ] All six of `ADR-0112` §6's paths are driven, or a path is recorded as undrivable with the
      reason — a table in this story with one row per path, the observed result, and the commit
- [ ] The `AccountOffer` accept path's behaviour is **observed**, not derived, and the observation is
      compared against `ADR-0112` §5's resolution in `ADR-0086`'s favour
- [ ] The mailed-link path records whether the token is still usable afterwards
- [ ] No `verify:` block in any ticket of this story runs a browser (`ADR-0089` §2)
- [ ] Every defect the drive finds is filed as its own ticket naming a non-browser gate, or recorded
      as browser-only with `ADR-0089` §4's by-hand reproduction requirement written into it
- [ ] `python3 .github/scripts/lint_tickets.py` exits 0

## Out of scope

- **The repair itself.** What the client does about a held room and a chosen screen is `ADR-0112`'s
  answer, its mechanism is `DEC-123`, and both land in `STORY-1311`.
- **A QA round.** This story runs no `/qa-cycle`, reports no `A(N)` or `B(N)`, and moves no verdict
  table. It is a targeted reproduction attempt an ADR asked for by name.
- **Any coverage claim.** `ADR-0089` §2c: a drive is a statement about one run, on one machine, at
  one commit.
- **The engine and the server.** Nothing here opens either.
