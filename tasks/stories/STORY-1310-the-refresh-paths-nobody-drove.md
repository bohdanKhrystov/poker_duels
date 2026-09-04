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
| `P6a` | a **mailed link opened while a room is held** — the room **waiting** | OBSERVED (not inferred), both layouts — but only after a methodology defect was caught and fixed before any reading below was taken: a direct `X open` to a URL differing from the already-loaded page only by its fragment does **not** reload the document — `performance.getEntriesByType('navigation')` held exactly one `navigate` entry, named without the fragment, after the naive call on both profiles. That is `Page.navigate`'s own version of this ticket's own named trap, "assigning `location.hash` in a tab that is already running captures no token and measures nothing," and every reading taken under it (discarded, none reported here) would have measured nothing too. Fixed, and verified fresh on every run below: the same profile hops through `X open "about:blank"` first (which times out `open`'s own #root wait — expected and harmless, #root never exists on a blank page, and this call is not itself a reading), then `X open`s the mailed address; `performance.timeOrigin` changed and a fresh `navigate` entry named with the `#/verify/...` fragment appeared every time, proving a genuine reload rather than a hash write. **`bare`** (CDP `9900`, room `P20PZ2H6`, `pd.roomCode` read as `P20PZ2H6` immediately before the open): `open` returned in **0.657s**, and its first paint, verbatim, was already "Waiting for your rival / P20PZ2H6 / Invite link / Copy the link / You / Back to the lobby / The room stays open. That link still works for your rival, and it brings you back." — `Finish verifying an address` never appeared. `record` armed 0.20s later; after a 10.24s watch, `frames` holds exactly **one** entry, byte-identical to the first paint; `text` matches; `location.hash` read empty both times. Neither `recovery-text.ts` sentence appeared at any point. **`delayed 300ms`** (CDP `9902`, room `SRF5YDM7`, `pd.roomCode` read as `SRF5YDM7` immediately before): `open` did not return for **12.175s** — the same order of magnitude as `P4`'s own ~11.8s for a comparable boot race at this delay, independent corroboration this was a genuine reboot — and its first paint read only "Finish verifying an address" plus a `Back` control, `VERIFY_HEADING` with no outcome sentence yet. `record` armed 0.196s later, and its **first captured frame already read** "Finish verifying an address … That link has expired or has already been used. Ask for a new one from the account screen. … Back" — `recovery-text.ts`'s `VERIFY_LINK_DEAD` (`ADR-0089` §5) — so the mount effect ran and the call completed inside that ~0.2s gap. After a further 30.23s watch, `frames` holds exactly **two** entries: that heading-plus-dead-link frame, then "Waiting for your rival / SRF5YDM7 / Invite link …" — the mailed screen was **replaced**, at an unknown point inside the 30s window (no intermediate `frames` poll was taken, so this reading does not narrow it further). `text` and `location.hash` (empty) match the settled second frame. **Reading the three-way test, in the ticket's own terms**: `bare` is the first case — the screen never appears, so nothing is submitted and a real token survives this path today; `delayed 300ms` is the second — the heading appears, the call completes, and the screen is taken away, `ADR-0112` §5's silent permanent failure and `ADR-0114` §5's reason for `hold`. Neither run produced the third case (heading appears and **stays**). **INFERRED, not observed in this ticket**: why `bare` stays silent — `ADR-0112`'s own Context names the mechanism already shipped, a `Lobby.tsx` effect that erases any non-`first` fragment whenever a room is held, waiting or playing alike, with no awareness of `#/verify` specifically; on `bare` this evidently runs before any render can show the mailed screen at all, since `open`'s own first paint already carries the room screen. This is not `ADR-0114` §5's `hold`, which is not built (this ticket's Out of scope); `Lobby.tsx` itself was not opened in this ticket to confirm the mechanism line by line. **The instrument's blind spot, stated rather than assumed**: nothing is known about the interval from navigation start to `open`'s own first paint (0.657s `bare`, 12.175s `delayed`) beyond `open`'s own 250ms-resolution wait loop, and a further ~0.2s elapses before `record`'s `MutationObserver` is truly armed on both layouts — a transition faster than that gap remains unruled-out on `bare`, though `open`'s own first-paint capture, taken inside that same gap, already shows the held-room screen with no trace of either sentence, the strongest evidence available at this resolution. **Whether the token survives afterward — undrivable on this stack, and why**: a machine with no mail transport binds `NoRecoveryMailer` (`ADR-0031` §7), whose two members are empty bodies, so no real mailed link ever arrives to re-follow; the verification token is stored only as a `BYTEA` hash (`V8__recovery_email.sql`), so no real token can be read back out of the database either; and seeding one would be `ADR-0089` §3's forbidden write. Not attempted. | `32af8fc5` | `bare` (CDP `9900` driven; room `P20PZ2H6`, created after `forget-room` cleared an earlier room that only ever saw the invalid same-document attempts described above — `9901` is this pair's rival profile, held in reserve and not used until `P6b` progresses this same room to `PLAYING`) and `delayed 300ms` (CDP `9902` driven; room `SRF5YDM7`, relay `5173`→`5273`/`6173`, the standing relay — `9903` likewise held for `P6b`) — fresh `mktemp -d` profiles for all four CDP ports; `bare` driven directly against Vite's own listener on `5273`, since `5173` is this run's standing relay and no longer bare Vite |
| `P6b` | a **mailed link opened while a room is held** — the room **playing** | OBSERVED (not inferred), both layouts, same corrected method as `P6a` — verified fresh via `performance.timeOrigin` on every run here too; see `P6a`'s row for the same-document-navigation trap this avoids. Both rooms were confirmed `PLAYING` **before** the mailed link was opened, by reading the rival profile's own screen after it joined by invite link: an active hand on screen (hole cards, pot, blinds), not the waiting-room text. **`bare`** (CDP `9900`, room `P20PZ2H6`, `pd.roomCode` read as `P20PZ2H6` immediately before the open): `open` returned in **0.653s**, and its first paint, verbatim, was already the live table — "Your rival", a countdown ("3"), two dealt hole cards, "Pot 150", "YOUR TURN", and the action row (`Fold`, `Call 100`, `Raise to 200`, `All in 10,000`) — `Finish verifying an address` never appeared. `record` armed 0.19s later; after a 10.24s watch, `frames` holds **11** entries, every one the same table with only the turn countdown and then the timebank changing (3, 2, 1, then `Timebank 3:00` down through `2:53`) — continuous `MutationObserver` coverage catching every one-second tick, with no trace of either `recovery-text.ts` sentence anywhere in the transcript. `text` and `location.hash` (empty) match. **`delayed 300ms`** (CDP `9902`, room `SRF5YDM7`, `pd.roomCode` read as `SRF5YDM7` immediately before): `open` did not return for **12.150s**, and its first paint read only "Finish verifying an address" plus `Back` — no outcome sentence yet, over a room that, at that exact moment, was genuinely mid-hand. `record` armed 0.195s later, and its **first captured frame already read** "Finish verifying an address … That link has expired or has already been used. Ask for a new one from the account screen. … Back" — the call completed inside that ~0.2s gap, over a duel actively running underneath. After a further 30.23s watch, `frames` holds **33** entries: the heading-plus-dead-link frame; then, startlingly, "Waiting for your rival / SRF5YDM7 / Invite link …" — the **stale pre-join waiting screen**, re-appearing over a room that was genuinely `PLAYING`, independently reproducing `P4`'s own named finding ("the pre-join waiting-room screen … renders over a room that is actually `PLAYING`") by an entirely different path than `P4`'s plain reload; then a partial table (cards and pot, no timebank or action row yet); then the full table with its action row; then **29** further frames, the timebank ticking one second at a time from `3:00` down to `2:32`. Since those 29 of the 33 frames are consecutive one-second ticks of an otherwise uninterrupted timebank, and the total watch was 30.23s, the whole transition — dead-link shown, stale waiting screen, partial table, full table — is INFERRED, not directly timed, to be bounded inside roughly the first one to two seconds of the watch: a materially tighter bound than `P6a`'s "somewhere in 30s," because `P6a` only ever produced two frames with no intermediate marker to count against. `text` read the table at `2:31`, a fraction of a second past the last captured frame; `location.hash` read empty. **Reading the three-way test**: both layouts answer identically to `P6a` — `bare` never shows the mailed screen at all, nothing submitted, a real token survives this path today, on this reading; `delayed 300ms` shows the heading, the call completing, and the screen taken away — `ADR-0112` §5's silent permanent failure and `ADR-0114` §5's reason for `hold` — here over a hand genuinely in progress rather than a waiting room, the more damaging half of this pair of rows. Neither run produced the third case (heading appears and stays). **INFERRED, not observed in this ticket**: same attribution as `P6a` — the `bare` silence is `ADR-0112`'s own already-shipped `Lobby.tsx` blanket effect, not `ADR-0114` §5's not-yet-built `hold`; `Lobby.tsx` itself was not opened in this ticket to confirm it line by line. **The instrument's blind spot**: the same shape as `P6a` — nothing is known before `open`'s own first paint (0.653s `bare`, 12.150s `delayed`), and a further ~0.2s elapses before `record` is truly armed; a transition faster than that remains unruled-out on `bare`, though `open`'s own first paint, taken inside that gap, already shows the live table with no trace of either sentence. **Whether the token survives afterward — undrivable on this stack, and why**: identical reason to `P6a` — `NoRecoveryMailer` (`ADR-0031` §7) binds when no mail transport exists, so no real link ever arrives; the token is stored only as a `BYTEA` hash (`V8__recovery_email.sql`), unreadable even from the database; and seeding one is `ADR-0089` §3's forbidden write. Not attempted. | `32af8fc5` | same four profiles, rooms and commit as `P6a` above, now `PLAYING`: `bare` (`9900` held the room throughout; `9901` joined `P20PZ2H6` by invite link) and `delayed 300ms` (`9902` held the room throughout; `9903` joined `SRF5YDM7` by invite link) |

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

## What this found

Read together, the seven rows say the human's reported symptom — a refresh bounces to the lobby
and stays there — did not reproduce on any of the six named paths, on any layout: `P1`'s redrive
below settles its own disagreement in `bare`'s favour, `P2` shows no lobby on `bare`, and `P5`,
`P6a` and `P6b` all restore the address correctly on `bare`. Two defects nobody named going in did
reproduce, and one is now well-corroborated. First, by three independent routes — `P2` mid-runout,
`P4` a plain reload, `P6b` a mailed link — a resumed client under real latency paints a stale
`Waiting for your rival` screen naming a room that is not actually waiting: finished, in `P2`'s
case, still `PLAYING` in `P4`'s and `P6b`'s. Three routes agreeing is materially stronger evidence
than any one of them alone, and it is `ADR-0112` §6's own "no lobby on the way" failing, not
`ADR-0102` §5 — every one of the three still lands on the true state in the end. Second, `P5` found
the `AccountOffer` accept press matches neither of `ADR-0112` §5's named outcomes — not the account
screen, not the derived bounce — but a third: the offer is visibly spent and nothing happens for at
least 15s, identically on both layouts.

This ticket drove `P1`'s `bare` reload again rather than arguing about it, at `c655859b`, against
the running stack's own bare layout (`http://localhost:5273/` — `5173` is this session's standing
`300ms` relay, not bare Vite): two fresh profiles through a held room, an all-in/call preflop
finish, `record` armed on the shoving host's own tab before the call, then, once the host's held
result screen read `Defeat / −1 duel coin / 1 hand · You 0 · Your rival 20,000 / Rematch / Back to
the lobby`, two separate genuine `Page.navigate` reloads of that same address — each confirmed
genuine by a changed `performance.timeOrigin` and a fresh `navigate` entry, not a same-document hash
write. Both times `open`'s own first paint already showed the result screen intact; `record`/
`frames` afterward held one unchanging frame each time; `location.hash` read empty both times; and
an explicit `absent` check held `No duel room has that code.` off screen for 4s after each reload.
That reproduces this row's `SURVIVES` reading cleanly, twice. It does **not** reproduce the row's
other reading, and it does not explain it either: the stale-room mechanism `P2`/`P4`/`P6b` found
produces a `Waiting for your rival` screen, not the `No duel room has that code.` sentence the other
`P1` drive reported — which reads like an unknown-room answer, not a slow first frame, so the two
are not obviously the same mechanism. **The disagreement is narrowed, not settled**: `bare`
demonstrably and repeatably survives; why the other drive saw that specific sentence remains open,
recorded honestly rather than resolved by a tidy story.

`P3` could not be driven at all, and could not have been on this stack: cutting the relay also
severs Vite dev server's own HMR socket sharing the port, reloading the page before
`reconnecting.ts`'s in-place recovery can ever be observed. That is the instrument fault its own row
named, and the decision it was waiting on is now answered: `ADR-0117` (merged) moves the proof of
record to a built bundle on `vite preview`'s `:4173`, off the dev server whose HMR client caused the
fault — so `P3`'s owner is a re-drive once that ships, not a further decision. The mailed-link
token's survival past a refusal (`P6a`, `P6b`) is undrivable for a structural reason instead — no
mail transport binds on this stack, the token is stored only as a hash, and seeding one is a
forbidden write (`ADR-0089` §3) — so what is driven, at `delayed 300ms`, is that the **client
submits**: the `VERIFY_LINK_DEAD` mount effect completes over a room that is genuinely running.
Whether a **live** token would be spent by the server is inferred from that submission, not tested,
and cannot be tested here. `P4`'s own durations (~11.8s/~13.5s at `300ms`; the `1000ms` readings)
are read the same way: `ADR-0117` computes the dev server's own module-waterfall cost at roughly 10s
for `web-client/src`'s 102 modules, so those numbers do not survive the move to a built bundle even
though the sequence they describe — lobby, then stale wait, then true state — does, being driven by
frame arrival rather than module count.

`ADR-0114` §6's one-render `waiting` residual — a mailed link landing in the single gap between a
resumed `RoomJoined` and its `Snapshot` — was not observed by any of the seven readings, and could
not have been: the `roomStanding`/`hold` mechanism that gap belongs to is `STORY-1311`'s and is not
built yet; `P6a`'s and `P6b`'s own stale-screen findings are today's shipped `Lobby.tsx` blanket
effect, a different mechanism, and both rows say so themselves. No `DEC` is registered here on that
account. **What this story does not claim:** these are seven statements about one run, on one
machine, at the commit named in each row — `c655859b` for this ticket's own `P1` redrive — and
`ADR-0089` §2c holds: no coverage, and none of it may be cited in a Definition of done as a check
that passed.

### Every finding, given an owner

| finding | owner | the gate it would need |
| --- | --- | --- |
| The wait screen renders for a room that is not actually waiting under real latency — `P2` (finished, mid-runout reload), `P4` (still `PLAYING`, a plain reload) and `P6b` (still `PLAYING`, a mailed link) — three independent routes to the same defect. | A `DEC` for the product owner — is a self-correcting false *waiting* screen over a live or finished duel acceptable, or does it need a neutral interstitial? `ADR-0114` §2 leaves the wait/table/result branch order untouched, so `STORY-1311` does not fix this on its own; name the next free `DEC` in `docs/adr/README.md`'s `## Open decisions`. | Depends on the `DEC`'s answer — until then, `ADR-0089` §4's by-hand reproduction is what stands, this ticket's own redrive plus `P2`/`P4`/`P6b` being the reproduction on record. |
| Pressing the result screen's `AccountOffer` accept (`P5`) spends the offer and lands nowhere — neither `ADR-0112` §5's account screen nor the derived bounce — for at least 15s on both layouts. | `STORY-1311`, not a new `DEC` as `P5`'s own row concludes: `ADR-0114` §6 already names this exact path ("what makes `ADR-0086` §6's accept land"), and `STORY-1311`'s own seventh acceptance criterion is this path by name. | A component test feeding `DuelFinished` then the same ask `ADR-0114` §7's "Honoured" case uses — `location.hash` becomes `#/account` and the account screen renders, where today it would not. |
| At `delayed 300ms` (`P6a`, `P6b`) the mailed screen's mount effect completes over a duel that is running or was refused, which `ADR-0112` §5 forbids — today's shipped guard does not gate `verify`/`reset` on room state at all. | `STORY-1311` — `ADR-0114` §5's `hold`/`roomStanding` is built to close exactly this. | `ADR-0114` §7's "Not spent" test: a counting `verifyEmail` in `accountCalls`, booted at `#/verify/<token>` with a room remembered — zero calls fed `RoomJoined` + `Snapshot` (running, refused), exactly one fed `RoomJoined` + `DuelFinished` (honoured). |
| Every cut reloads the page via Vite dev server's own HMR socket sharing the relayed port (`P3`), so `reconnecting.ts`'s in-place recovery was never observed — an instrument fault, not a product reading. | A ticket to file, the planner's — re-drive `P3` once `ADR-0117` ships (`vite preview` on `:4173`, off the HMR-bearing dev server); the decision itself, `DEC-087`, is already answered. | Until re-driven, `ADR-0089` §4's by-hand reproduction; `reconnecting.ts`'s own retry logic (`scheduleRetry` → a fresh `WebSocket`, no navigation call anywhere in the file, read in full by `P3`) looks unit-testable without a browser at all, worth naming when that ticket is filed. |
| `drive.mjs`'s `record` arms a `MutationObserver` in a process invocation separate from, and after, the `open`/reload that starts a transition, so any transition faster than that gap is structurally invisible to every row leaning on `record`/`frames` — named as a limit on what every row above could have seen, not as a failure of any one of them. | A ticket to file, `drive.mjs`'s own — a verb arming the observer via CDP's `Page.addScriptToEvaluateOnNewDocument`, which runs before a page's own scripts on every future document and survives a reload; an instrument ticket, not a product one. | N/A — an instrument fix, not a product behaviour; `delay.mjs`'s own `--selftest`/`--selftest-cut` convention is the shape the new verb should prove itself against before a future row trusts it. |
| One earlier `bare` drive of `P1` saw the lobby's `No duel room has that code.` settle after a reload of the held result screen; this ticket's two fresh redrives (above) did not reproduce it. | None filed — a single unreproduced reading is not a confirmed defect, and this ticket's own discipline is against re-driving further without new evidence; recorded, not actioned. | N/A — nothing to reproduce against today; a third occurrence, on any path, would be the evidence a ticket needs. |

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

- [x] All six of `ADR-0112` §6's paths are driven, or a path is recorded as undrivable with the
      reason — a table in this story with one row per path, the observed result, and the commit.
      **Answered:** yes — `P1` through `P6b` each carry a row with an observed result and a commit;
      only `P3` is recorded undrivable, with the reason (Vite dev server's own HMR socket sharing the
      cut port) written into its row, and that reason is now resolved by `ADR-0117`, owed a re-drive
      rather than a further decision ("What this found", above).
- [x] The `AccountOffer` accept path's behaviour is **observed**, not derived, and the observation is
      compared against `ADR-0112` §5's resolution in `ADR-0086`'s favour.
      **Answered:** observed on both layouts (`P5`) — and it matches **neither** `ADR-0112` §5's
      resolution nor the derived bounce it was compared against; the press spends the offer and the
      account screen never shows, a third outcome `STORY-1311` now owns ("What this found", above).
- [x] The mailed-link path records whether the token is still usable afterwards.
      **Answered**, with its undrivable half inline: on `bare` the token survives, because the mailed
      screen never renders over a held room (`P6a`, `P6b`). At `delayed 300ms` the client's own mount
      effect completes over a running or refused duel — `ADR-0112` §5 forbids that — but **whether
      the server would spend a live token is inferred from the client's submission, not tested**: no
      mail transport binds on this stack, the token is stored only as a hash, and seeding one is
      `ADR-0089` §3's forbidden write, so that half stays undrivable here by construction, not by
      omission.
- [x] No `verify:` block in any ticket of this story runs a browser (`ADR-0089` §2).
      **Answered:** checked mechanically, not asserted — this ticket's own sixth `verify:` command
      greps every `verify:` block under `tasks/tasks/TASK-1310*.md` for `drive.mjs` or `stack.sh` and
      finds none, exit 0.
- [x] Every defect the drive finds is filed as its own ticket naming a non-browser gate, or recorded
      as browser-only with `ADR-0089` §4's by-hand reproduction requirement written into it.
      **Answered:** every finding below has a named owner and either a non-browser gate or, where
      only a browser can see it today, `ADR-0089` §4's by-hand line — see "Every finding, given an
      owner". None is filed as a ticket by this ticket itself, by design (Scope).
- [x] `python3 .github/scripts/lint_tickets.py` exits 0.
      **Answered:** green — `backlog ok`, this ticket's own seventh `verify:` command.

## Out of scope

- **The repair itself.** What the client does about a held room and a chosen screen is `ADR-0112`'s
  answer, its mechanism is `DEC-123`, and both land in `STORY-1311`.
- **A QA round.** This story runs no `/qa-cycle`, reports no `A(N)` or `B(N)`, and moves no verdict
  table. It is a targeted reproduction attempt an ADR asked for by name.
- **Any coverage claim.** `ADR-0089` §2c: a drive is a statement about one run, on one machine, at
  one commit.
- **The engine and the server.** Nothing here opens either.
