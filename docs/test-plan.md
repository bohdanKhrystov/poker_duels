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
| **source** | where the expectation comes from: the module holding any player-facing string the case quotes (`ADR-0089` §5), otherwise an ADR section or a `docs/duel-rules.md` heading (`ADR-0090` §4). One column, not two — it *is* the `owner` field generalised |

**A case that quotes player-facing text cites the module that owns the literal** — the shape
`web-client/src/account/recovery-text.ts`. It is a reference, not a gate: whoever changes the words
finds by `grep` that a case depends on them. It is the cheapest thing that turns silent rot into
findable rot, and rot is the failure mode `ADR-0088` §Alternatives 2 predicted for exactly this
kind of harness.

The driver is `node scripts/qa/drive.mjs <port> <verb>`; `A` is port 9232, `B` is 9233 and `C` is 9234. Verbs:
`open`, `text`, `click`, `wait`, `absent`, `type`, `link`, `device`, `forget-room`, `eval`, `close`, `shot`.

**Every round gets fresh Chrome profiles.** Clearing `pd.roomCode` is not isolation: the server
re-seats a returning device by `pd.deviceId` (`ADR-0018`), so a reused profile rejoins its old room
with client storage cleared. Measured 2026-08-29. A case reading `localStorage` must navigate its profile to the app first — the stored id lives under the app's origin, and a fresh profile sits on `about:blank`.

---

## SMOKE — is the product alive?

Six cases. Run on every scope, always. If any fails, the run is `blocker` and the remaining suites
are pointless.

| id | do | expect | fails if |
| --- | --- | --- | --- |
| `SMK-01` | `stack.sh status` | all three of db, server, web report `up` | any reports `down` |
| `SMK-02` | `A open` | `#root` renders and the text contains `Create a duel room` | the page is blank — this is `ADR-0088` gap 1, which fails green in every other gate |
| `SMK-03` | `A open`, `B open`, then `A device` and `B device` | two non-empty ids, and they differ | they are equal, or empty — two tabs are one player (`ADR-0018`) |
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
| `CORE-03` | C open `/?room=<code>` on a **full** room | a refusal naming the room, not a crash or a third seat | a third player is seated — the vision says *"Two players. Never three."* |
| `CORE-04` | `open /?room=NOSUCH0` | *No duel room has that code* | a blank screen, a hang, or a seat |
| `CORE-05` | A creates, B joins **by code** typed into the lobby field | same result as `CORE-02` | the code path and the link path disagree |

### The hand

| id | do | expect | fails if |
| --- | --- | --- | --- |
| `CORE-06` | read both screens each turn | the two screens never disagree about the board, the pot, either stack, or who is seated and present | any of the four differ at the same moment |
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

A navigation **is** a disconnect on this browser: `open` closes the socket and the server starts the other seat's grace window within milliseconds, measured 2026-09-01. It differs from `close` only in that the client resumes immediately and the window clears again.

| id | do | expect | fails if |
| --- | --- | --- | --- |
| `CORE-17` | mid-duel, `A open` (reload) | A returns to the **same seat**, same stacks, same board | A loses its seat, or is dealt in fresh |
| `CORE-18` | A close | B sees the away/absent marking (`ADR-0046`) | B is told nothing, or is told the rival *left* |
| `CORE-19` | A open <link> | B sees *Your rival is back.* | the mark never clears |

### Presence

`CORE-18` and `CORE-19` check the away marking appears when a player closes and clears when they return. Nothing checked that it stays away when nobody has left. A case which only asserts a thing appears is passed by a product that shows it always, so the following cases test that the mark does not appear absent a departure, that a duel remains playable after an idle grace window, and that the server enforces the paused state a client displays.

| id | do | expect | fails if |
| --- | --- | --- | --- |
| `CORE-21` | both seated, nobody closes and nobody navigates; `absent "is away"` and `absent "Timed out"` on **both** screens for 75s | neither screen ever marks the other away or timed out | either screen marks a rival who never left |
| `CORE-22` | neither seat acts for 75s — longer than `RoomTimeouts.DEFAULT_DISCONNECT_GRACE_MILLIS` — then the seat to act acts | the action is accepted and the hand advances; the duel is still playable after an idle grace window | the action is refused, or a seat was folded while its player stayed connected |
| `CORE-23` | while a screen carries `The duel is paused.`, that seat clicks its own action | the action is **refused** — `ADR-0028` §6's `DUEL_PAUSED` — because a duel the table calls paused is paused on the server | the action is accepted, proving the screen said paused while the server was not |

### Lobby

| id | do | expect | fails if |
| --- | --- | --- | --- |
| `CORE-20` | on the waiting screen, `click "Back to the lobby"` | A returns to the lobby and **the room stays open** — the link still works | the room closes, or the link stops working (`ADR-0073`) |

---

## EPIC-04 — Identity and profiles

Sources: the epic's Definition of done, and the ADRs its Open decisions table names.

Five of its twelve promises reach a browser; the other seven are under §*What this catalogue does
not cover*. The cases run in order and each leaves state the next uses. **W** is the browser whose
result screen named it the winner of `04-02`'s duel and **L** is the other — decided by reading the
screens, never fixed to a port. No case may assume a device with no finished duel: scope order
decides what has already been played before a suite runs, and this suite uses profiles A and B
throughout.

**Read the strip after a reload, never on the first load of a fresh profile.** `pd.deviceId` is
written when the socket's `Welcome` lands (`web-client/src/store/boot.ts`) while the profile read
runs once at mount (`web-client/src/profile/profile-provider.tsx`), and HTTP refuses a device the
socket has not yet minted. A fresh profile's first render is therefore `No profile yet.`, and the
strip is never re-read. A second `open` is deterministic. This is `SMK-03`'s mistake, avoided.

**Three `wait` targets would prove nothing.** `Your duels`, `Leaderboard` and `Account` label the
first screen's own doors as well as the screens behind them, so a wait on one is satisfied before
the swap. Wait on a string that exists only past the door.

| id | do | expect | fails if | source |
| --- | --- | --- | --- | --- |
| `04-01` | fresh `A`: `A open`, `A open`, `A wait "Duel coins"`, `A click "Your duels"`, `A wait "Opponent name"` | the strip states a balance as `<n> Duel coins`, and the duels screen renders its own frame — the `Opponent name` header appears — whatever the device has already played; neither read refuses a device that never made an account | the strip still reads `No profile yet.` after the reload, or the screen reads `Your duels did not load. Reload the page to try again.` — `GET /api/me` or `GET /api/me/duels` answered `401` to an account-less device | `web-client/src/profile/ProfileStrip.tsx`; `web-client/src/history/history-text.ts` |
| `04-02` | play a duel to a winner (`CORE-12`'s sequence); on the loser **L**, read the balance, claim the profile with a handle and a password, reload, read it again — steps below the table | both reads return exactly the same string, character for character — same digits, same sign — because giving the browser a password neither rewrites nor clamps a balance it already holds (`ADR-0014`) | the two strings differ in any character — the claim rewrote the balance, or clamped it toward zero (`ADR-0014`) | `ADR-0014`; `web-client/src/profile/profile-text.ts` (`coinBalanceText`) |
| `04-03` | on **L**: set a display name that is not L's handle, then try to sign in with that display name and L's own password — steps below the table | the sign-in is refused with `That handle and password do not match an account.` | it signs in, or the account screen afterwards reads `Your password signs in to this account.` — something resolved a player from a display name | `ADR-0031` §1; `web-client/src/account/account-text.ts` (`SIGN_IN_REFUSED`) |
| `04-04` | on **L**, sign in twice from the sign-in screen — once with L's own handle and a wrong password, once with a handle no account holds — capturing `L text` after each | both render `That handle and password do not match an account.`, and the two captures are identical | the two differ in any character, or either names a field, a handle or an account — the wire's indistinguishability was undone in words | `ADR-0027` §6; `web-client/src/account/account-text.ts` (`SIGN_IN_REFUSED`) |
| `04-05` | on **W**: set a display name, then claim the profile with a second handle and password. On **L**: sign in with W's handle and password — steps below the table. **Runs last** | L lands on the account screen reading `Your password signs in to this account.`, and behind *Back* it shows W's display name, W's balance and W's duel — on a browser whose `device` differs from W's | any of the three differs from what W's own screen shows, or L is left on the sign-in screen | `ADR-0083` §5; `web-client/src/profile/ProfileStrip.tsx` |

**`04-02`, `04-03` and `04-05` in full.** A duel leaves both browsers in a room (`ADR-0072`), so
each begins `forget-room` then `open`. On the first screen the room-code box is input `0` and the
name box is input `1`; on the account screen the sign-up handle is `0` and its password is `1`; on
the sign-in screen the handle is `0` and the password is `1`.

- `04-02` — `L forget-room`, `L open`, `L text` (read `… Duel coins`), `L click "Account"`,
  `L wait "Give this profile a password"`, `L type 0 <handle>`, `L type 1 <password>`,
  `L click "Give this profile a password"`, `L wait "This profile now has a password."`,
  `L open`, `L text` (read it again).
- `04-03` — `L type 1 <a display name with a space in it, so it cannot be a handle>`,
  `L click "Set my name"`, `L click "Account"`, `L click "Sign in"`,
  `L wait "Forgot your password?"`, `L type 0 <that display name>`, `L type 1 <L's password>`,
  `L click "Sign in"`, `L wait "That handle and password do not match an account."`.
- `04-05` — the same two flows on **W** with a second handle and a second display name, then on
  **L**: `L click "Account"`, `L click "Sign in"`, `L wait "Forgot your password?"`,
  `L type 0 <W's handle>`, `L type 1 <W's password>`, `L click "Sign in"`,
  `L wait "Your password signs in to this account."`, `L click "Back"`, `L text`, and
  `L device` against `W device`.

**A handle is 3–32 of `[a-z0-9._-]` starting with `[a-z0-9]`, and a password is 8–128 characters**
(`web-client/src/account/account-text.ts`, `HANDLE_REFUSED` and `PASSWORD_REFUSED`). A display
name containing a space cannot be a handle, which is what makes `04-03` unambiguous.

**`04-05` runs last because it ends L's own identity**: signing in binds that browser to W's
player, so every case after it would read W's profile.

---

## EPIC-05 — Ranking, duel coins and leaderboard

Sources: the epic's Definition of done, and the ADRs its Open decisions table names.

Five of its nine promises reach a browser; the other four are under §*What this catalogue does not
cover*.

**No case asserts an absolute rank.** The database persists between rounds, so every rank on the
ladder depends on every duel ever played on that machine. What is deterministic is that a **fresh**
profile's first finished duel moves its standing by exactly one (`ADR-0014`, `ADR-0061` §4), and
`ADR-0089` §3 already requires a fresh Chrome profile per round. A case pinning `rank 1` would be
red for the machine's history rather than for a defect. The same reasoning rules out a device with
no finished duel at all: scope order decides what has already been played before this suite runs,
and this suite works with profiles A and B.

**The season line is compared against the response, never merely read.** A client that worked the
season out from the browser's clock would print the right month on the day a round runs — so *"the
screen shows a month"* is an assertion that passes on the defect `ADR-0061` §6 forbids. `05-05`
reads `GET /api/standings` and compares.

**A read that is identity-scoped is anonymous unless the case sends what the app sends** — an
anonymous read of `/api/standings` answers `self: null` for everyone.

| id | do | expect | fails if | source |
| --- | --- | --- | --- | --- |
| `05-01` | fresh `A`: `A open`, `A open`, `A click "Leaderboard"`, `A wait <the season line>` (`05-05`'s target — a month name, present whatever this device has already played), `A click "Back"`, `A wait "Create a duel room"` | a browser that never signed up opens the ladder from the first screen and leaves it again | the `Leaderboard` control is absent, the screen reads `The leaderboard did not load. Reload the page to try again.`, or *Back* does not return to `Create a duel room` | `web-client/src/ladder/ladder-text.ts` (`LADDER_FAILED`); `web-client/src/lobby/Lobby.tsx` |
| `05-02` | on that same ladder screen: `A eval "(async()=>{const h={'X-Device-Id':localStorage['pd.deviceId']};const t=localStorage['pd.sessionToken'];if(t)h.Authorization='Bearer '+t;return (await (await fetch('/api/standings',{headers:h})).json()).self})()"` to learn whether this device already has a place, then read the self line with `A text` | the self line agrees with the response either way — `You have no place on this season's leaderboard.` if the response carries no entry for this device, or a `You are rank ` line naming the same rank the response gives it | the self line disagrees with the response in either direction — a rank shown where the response has no entry, or no-place shown where the response has one — a player who finished no duel was given a place, or the reverse | `ADR-0065` §4; `web-client/src/ladder/ladder-text.ts` (`selfLine`, `NO_PLACE_THIS_SEASON`) |
| `05-03` | read both self lines and note each standing, whatever either already reads; play a duel to a winner (`CORE-12`'s sequence); `forget-room`, reload and open the ladder on each; read both self lines again; then the `duel_result` read below the table | each player's second standing is exactly one more than their first for the winner (**W**) and one fewer for the loser (**L**) — the change one duel makes, never either absolute value — and each player's season sum in `duel_result` agrees with the standing their own second reading named | either standing moves by anything other than exactly one, in either direction, or a rendered standing disagrees with its `duel_result` sum — a coin was minted or destroyed between the row and the screen | `ADR-0014`; `ADR-0061` §4; `web-client/src/ladder/ladder-text.ts` (`selfLine`) |
| `05-04` | on **L**'s ladder, `L eval` reads `GET /api/standings`'s `nextCursor` and the rendered row lines; while `nextCursor` is non-null, `L click "Show more"` and read both again — a one-page season runs the loop zero times and the case still runs | every row read down the walk keeps `rank = 1 + the number standing strictly higher`, and the standings never increase from an earlier row to a later one — the negative sits below every larger one; a `−1` row is among them; and `Show more` is visible on a page exactly when that page's own `nextCursor` came back non-null, proof that another page is waiting, never merely that the read behind it failed | no `−1` row appears at all, or it reads `0`, or its minus is an ASCII hyphen, or a standing later in the walk is greater than an earlier one, or `Show more` is reachable on a page whose `nextCursor` read `null`, or hidden on one whose `nextCursor` read non-null — a page that failed to load proves neither, and is never scored as either | `ADR-0061` §4; `ADR-0064` §1; `web-client/src/profile/profile-text.ts` (`coinBalanceText`) |
| `05-05` | on the ladder: `A eval "(async()=>(await (await fetch('/api/standings')).json()).season)()"`, then read the season line with `A text` | the screen prints the month and the year in English for exactly the season the response carried — `August 2026` for `2026-08` | the screen prints `2026-08`, prints no season line, or prints a month the response did not carry — the client worked the season out from the browser's clock (`ADR-0002`) | `ADR-0061` §6; `web-client/src/ladder/ladder-text.ts` (`seasonName`) |

**`05-03` reads the database, and only as a second witness.** `ADR-0089` §3 names the database in
what the harness may read, and this is `CORE-13`'s shape: the ladder's rendered number is still the
observation, the row is what it is checked against, and the case is red when they disagree. A case
whose *only* observation was a row would be a test of the server — which
`poker-server/.../e2e/` already covers against real Postgres — and a `PASS` over it would read as
the coverage claim `ADR-0089` §2c forbids. It writes nothing.

A season is a calendar month in UTC and a duel belongs to the season its **finish** falls in
(`ADR-0061` §§1, 2), so the read is bounded the same way:

    docker exec poker_duels-postgres-1 psql -U poker -d poker_duels -At -c \
      "SELECT b.device_id, SUM(dr.coin_delta)
         FROM device_binding b
         JOIN duel_result dr ON dr.player_id = b.player_id
         JOIN duel d ON d.id = dr.duel_id
        WHERE b.device_id IN ('<A device>', '<B device>')
          AND b.revoked_at IS NULL
          AND d.finished_at >= date_trunc('month', now() AT TIME ZONE 'UTC') AT TIME ZONE 'UTC'
        GROUP BY b.device_id"

The device ids come from `A device` and `B device`; a live binding is unique per device by the
partial index `device_binding_live_device` in `V7__device_binding.sql`, so each browser resolves to
one row without the driver ever learning a `player_id`. When a query here stops running, check
`poker-server/src/main/resources/db/migration/` for the migration that changed the table or column
it reads, rather than waiting for the next round to fail.

---

## Per-epic suites

Two paths in, and they produce different things
([`ADR-0090`](adr/ADR-0090-a-skill-may-write-the-catalogue-or-run-it-never-both.md) §5, which
amended the single sentence that used to stand here).

**Authored, then tested.** The `qa-cases` skill fills a suite in from **merged sources** — the
epic's Definition of done, the ADRs it names, `docs/duel-rules.md`, and the modules holding the
literals — *before* the epic is tested, because `ADR-0090` §3 gives it no browser and no stack. What
it produces is **provisional**, and that word is doing real work: a merged decision proves what was
*decided*, not what *shipped*, so it cannot show that the screen exists, that the control is
reachable, or that a literal has not moved since. A provisional suite carries this line, and the
round record that first runs it is what deletes the line and names the cases that round corrected:

> **provisional** — authored YYYY-MM-DD from merged sources, not yet run (`ADR-0090` §5).

While it stands, a failing case in that suite is as much a claim about the catalogue as about the
product: `ADR-0089` §4's reproduce-by-hand step is the whole point for it, and a failure that does
not reproduce is a **harness** defect — repaired here, excluded from `B(N)`, no production code
changed for it. Corrections in that first round are expected rather than surprising.

**Written while testing.** A suite filled in *during* an epic's first round is not provisional. Its
cases were observed as they were written, so it carries no line and needs no round to strike one.

**Neither path writes from ticket titles.** A suite written from titles rather than from a merged
decision or from exercising the screens is a suite that tests the titles.

### Template

```
## EPIC-NN — <title>

Sources: the epic's Definition of done, and the ADRs its Open decisions table names.

| id | do | expect | fails if | source |
| --- | --- | --- | --- | --- |
| NN-01 | <driver verbs> | <observation> | <the concrete failure> | <ADR §, rules heading, or module> |
```

Three rules for writing one:

1. **One case per promise the epic made**, taken from its Definition of done. Not one per ticket —
   tickets are already gated by their own `verify:` blocks and re-testing them here is waste.
2. **The `fails if` column is the case.** If you cannot state what makes it red, the case is not
   ready. `ADR-0088` §2 is the model: eleven steps, each with a named failure.
3. **The `source` column is where the expectation comes from**, so a disagreement between the
   catalogue and a merged decision is visible as a disagreement rather than silently resolved by
   whoever wrote the case (`ADR-0090` §4). A case whose expectation has **no** merged source is not
   written: it is a `DEC` for the product owner, because an invented expectation is a product claim
   that the cycle's repair step would change production code to satisfy. `SMOKE` and `CORE` predate
   the column and are not retrofitted.

### Not yet written

| Epic | Status |
| --- | --- |
| `EPIC-01` poker engine | not written — largely covered by the engine's own suite; a browser cannot see it |
| `EPIC-02` duel server | not written — covered by `poker-server/.../e2e/` against real Postgres |
| `EPIC-03` web client | **the CORE suite above is it** |
| `EPIC-04` identity and profiles | **the `EPIC-04` suite above is it** — authored 2026-08-29 from merged sources, run in rounds 1 and 2 (corrections tracked under `STORY-1205` and `STORY-1206`) |
| `EPIC-05` ranking and leaderboard | **the `EPIC-05` suite above is it** — authored 2026-08-29 from merged sources, run in rounds 1 and 2 (corrections tracked under `STORY-1205` and `STORY-1206`) |
| `EPIC-06` design system | not written — `qa` is told not to report styling, and the `## UAT` section's focus reports exactly that |

---

## UAT — the screens a round walks, and the questions it asks

Not a suite: this section adds no case, changes no case and touches no row above it. The existing
cases' `do` columns are already the routes to every screen-state the product has, and their
`expect`/`fails if` columns stay exactly as functional as they were — no case here is regraded on
UX (`ADR-0092` §7).

A frame that auto-advances between polls is read by arming `record` on the browser about to see
it, then reading `frames` once the transition has passed — never by polling faster, since `wait`
and `absent` sample every 250ms and a frame that lives for less time than that is invisible to any
poll. `record` installs a `MutationObserver` on `#root`; `frames` prints what it caught.

### The screen inventory

| screen | state | card | walk | routes |
| --- | --- | --- | --- | --- |
| `first` | hosting — the room code, the invite link, and the way back to the lobby | `design/screens/create-duel.html` | walked | `SMK-04`, `CORE-20` |
| `first` | joining by a shared invite link — seated with no code ever typed | `design/screens/duel-table.html` | walked | `SMK-05`, `CORE-02` |
| `first` | joining by typing a room code into the lobby's field | `design/screens/enter-code.html` | walked | `CORE-05` |
| `first` | the table once a hand is under way, both screens agreeing on it | `design/screens/duel-table.html` | walked | `SMK-05`, `SMK-06`, `CORE-06` |
| `first` | the table across its turn, waiting, away and back states | `design/screens/duel-table-states.html` | walked | `CORE-01`, `CORE-07`, `CORE-18`, `CORE-19` |
| `first` | the result screen once a duel concludes | `design/screens/duel-end.html` | walked | `CORE-12` |
| `first` | the rematch offer, accepted by both and pending on one | `design/screens/rematch-states.html` | walked | `CORE-15`, `CORE-16` |
| `duels` | the duel history list, headed `Opponent name` | `design/screens/duels.html` | walked | `04-01` |
| `leaderboard` | the season standings, with the viewer's own rank line | `design/screens/leaderboard.html` | walked | `05-01`, `05-03` |
| `account` | claiming a profile with a password, or — once signed in — that profile's own page | `design/screens/account.html` | walked | `04-02`, `04-03`, `04-05` |
| `sign-in` | the sign-in form, reached from account, with a `Forgot your password?` link | `design/screens/sign-in.html` | walked | `04-03`, `04-04`, `04-05` |
| `verify` | confirming a mailed verification link | — | not walked | no mailed link ever arrives — `ADR-0031` §7 |
| `reset` | setting a new password from a mailed link | — | not walked | no mailed link ever arrives — `ADR-0031` §7 |

No missing-card finding is ever filed for `verify` or `reset`: `ADR-0092` §4 files a `high` for a
screen in scope with no merged card, and a screen no route reaches is not in a round's scope, so
their per-screen cells read `out of scope` rather than `BLOCKED — no card` (§6). Their cards are
still owed — `ADR-0091` §5 registers all six cardless screens as debt, and `ADR-0092` §4 narrows
only the vehicle that collects it: a UAT round files the cards for the screens its scope reaches,
and the `EPIC-06` retrofit story covers whatever remains cardless, which for `verify` and `reset`
is the whole of it. And once composed, neither card will ever have a conformance check behind it:
it is accepted by the human at the pane alone, exactly as every card was before this section
existed, and `ADR-0089` §2c already forbids reading any round as the thing that validated one.

### The standing questions

| id | question | source |
| --- | --- | --- |
| `UAT-Q1` | is the main info properly highlighted? | ADR-0092 §Context |
| `UAT-Q2` | is it clear to user what is going on? | ADR-0092 §Context |
| `UAT-Q3` | are all options accessible? | ADR-0092 §Context |
| `UAT-Q4` | is the client matching the design? | ADR-0092 §Context |

An observation may be filed as a finding **only when it contradicts a merged source** — a card, `design/tokens/tokens.css`, an owned literal, an ADR section, a `docs/duel-rules.md` heading, a `docs/vision.md` sentence. An observation with no merged source to contradict — *this could be clearer*, *the emphasis feels wrong* — is a **question**, and the `uat` agent's `QUESTIONS` section is its only route.

### Settled, and not a finding

An entry here is a closed question: re-raising it would itself contradict a merged source, so it is neither a finding nor a question eligible for the `uat` agent's `QUESTIONS` section.

| observation | merged sources | round |
| --- | --- | --- |
| Duel-history timestamps render in the reader's locale — a Ukrainian browser sees `27 кв. 26`, not `2026-04` — and this is not a defect. | `web-client/src/profile/profile-text.ts` (`finishedAtText`); `ADR-0061` §*What it costs* | `STORY-1205`, `STORY-1209`, `STORY-1210` |

---

## What this catalogue does not cover

Stated so nobody reads a `PASS` as more than it is:

- **The built bundle.** Every case runs against `npm run dev`. `dist/` is loaded by nothing here,
  which is `ADR-0088` gap 3 and survives this document exactly as it survived that ADR.
- **Performance, load and security.** Out of `EPIC-12`'s scope.
- **Anything `EPIC-06` owns** — placement, colour, type. `qa` is instructed not to report it.
- **What UAT does not cover.** A UAT round makes three checks per screen (conformance, reachability, and copy against merged sources), but it does not answer taste questions: a judgment with no merged source to contradict is a question, never a finding, and only the `product-owner` answers one (`ADR-0092` §§3, 5). The human answers only what would change the vision. Recovery (`#/verify`, `#/reset`) is excluded for the reason this section already gives above about unreachable mail routes.
- **Seven of `EPIC-04`'s twelve Definition-of-done promises**, each for a stated reason rather than
  by omission. *Every story is `done` or `dropped`*, *`V1` and `V2` are byte-unchanged*,
  *`poker-engine` declares no dependency outside the `ADR-0010` allowlist* and *`verifyProtocolTypes`
  still passes* are facts about the repository, answered by the board and by Gradle; no browser can
  see one. *No response body, log line or `ServerMessage` contains a password or a password hash* is
  promised **"asserted structurally rather than by inspection"** — the epic names its own method, and
  it is not a browser: `drive.mjs` reads `#root.innerText`, which never contains an input's value, so
  a DOM assertion for it would be green on any product. *History paging is total and disjoint* needs
  more finished duels than a round can play — `DEFAULT_DUEL_LIMIT` is 10, so a second page needs
  eleven — and its concurrent-insert half needs a duel to finish between two page reads, which two
  browsers cannot do while one of them is paging. *Checked by hand, once, and recorded* asks for a
  receipt rather than a repeatable case, and its recovery half is unreachable: a machine with no mail
  transport binds `NoRecoveryMailer` (`ADR-0031` §7), the verification and reset tokens are stored
  only as `BYTEA` hashes (`V8__recovery_email.sql`), and no mailed link ever arrives for a driver to
  follow. **The whole of recovery — `#/verify`, `#/reset`, and *Forgot your password?* past its
  acknowledgement — is outside this catalogue for that reason.**
- **Four of `EPIC-05`'s nine Definition-of-done promises**, and one clause of a fifth. *Every story
  is `done` or `dropped`*, *`docs/protocol.md` contracts every endpoint this epic adds* and
  *`poker-engine` is untouched by every commit in the epic* are facts about the repository and the
  documents; no browser can see one. *`ADR-0012`'s gate is discharged in writing* was discharged by
  [`ADR-0063`](adr/ADR-0063-nothing-gates-a-place-and-the-farm-is-accepted-until-the-ladder-is-public.md)
  as an accepted risk with a named expiry, which is a written record rather than a behaviour. And
  within the self-line promise, ***tied with a hundred others*** is the scenario `ADR-0065` §1 was
  written for and the one a round cannot build: `05-02` tests that the self line exists and what it
  says, never that it survives a tie.
- **A real network.** Everything is `localhost`; latency, packet loss and a proxy in front of the
  socket are untested.
