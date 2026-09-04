---
id: STORY-1311
title: Only a running duel refuses another screen, and the refusal restores the address
type: story
status: ready
parent: EPIC-13
module: web-client
labels: [client, routing, lobby, recovery]
depends_on: [STORY-1310, STORY-1302]
---

## Goal

A player holding a `WAITING`, `FINISHED` or `ABANDONED` room can ask for `duels`, `leaderboard`,
`account` or a mailed screen and get it, with the address naming it; a player in a **running** duel
asks and nothing moves, silently, with `/` restored. And a browser that is still *asking* the server
about a room shows nothing at `/` until it is answered.

## Why

`EPIC-13` item 8. The reported symptom did not reproduce; **what reproduced is the inverse** — one
effect in `Lobby.tsx` calls `leave()` whenever `outcome`, `view` or `roomCode` is non-null and the
screen is not `first`, so `location.hash = '#/leaderboard'` reads back empty within 2 s and those
three screens are unreachable while any room is held.

**Two merged promises already contradict that.** `ADR-0073` §3 tells a waiting host on screen that
they may walk away — and today refuses them a glance at the ladder inside the product. `ADR-0086`
§6's accept is `<a href="/#/account">`, and that page load rejoins the remembered `FINISHED` room,
re-seats the tree, and the effect erases `#/account` so the account screen never shows.

[`ADR-0112`](../../docs/adr/ADR-0112-only-a-running-duel-refuses-another-screen.md) resolves both:
the shipped behaviour on a **playing** browser is confirmed as intent, and the two states a player
actually sits in between hands of nothing — waiting and finished — stop being a confinement.

## The four merged ADRs this story serves

`DEC-123` is answered and this story is no longer blocked. Everything below is merged; nothing here
is a design choice a ticket may revisit.

- [`ADR-0114`](../../docs/adr/ADR-0114-one-predicate-answers-every-ask-and-a-mailed-screen-waits.md)
  — **the mechanism**, answering all four parts of `DEC-123`. One pure module,
  `web-client/src/routing/room-standing.ts`, holds `roomStanding` and `rulingOn`; the six
  chosen-screen branches move **above** the three store branches and test `shown`, never `screen`;
  the restore is a `useLayoutEffect` keyed on `ruling`, which is what makes `ADR-0112` §2's *"one
  act, not two"* true; and a screen that spends a secret on arrival **holds** until the first frame,
  which is how a mailed token survives unspent.
- [`ADR-0118`](../../docs/adr/ADR-0118-a-recovering-browser-shows-nothing-it-was-not-told.md) —
  **the shape of the recovery**, answering `DEC-127` as **two answers, not one**. Before any frame
  the client painted the front door, a screen the server stated nothing to support: **wrong-because-
  false**, so `/` now renders **no element at all** until the server speaks. After `RoomJoined` and
  before the frame that states the duel, the stale *Waiting for your rival* is
  **acceptable-because-late**: accepted product behaviour, no interstitial anywhere, and **no ticket
  repairs it**. Its §6 fixes what this story's tickets must draw, including a test that pins the
  accepted half so no later ticket answers `DEC-127` the other way.
- [`ADR-0116`](../../docs/adr/ADR-0116-the-accept-is-a-door-and-a-door-that-does-not-open-spends-nothing.md)
  — **the account offer's accept**, answering `DEC-125`. It constrains this story and nothing else,
  and it fixes nothing by itself: *"§1's behaviour arrives as a side effect of `ADR-0114`'s branch
  order landing in `STORY-1311`."* What is owed here is the branch order and one regression test.
- [`ADR-0117`](../../docs/adr/ADR-0117-the-proofs-of-record-load-the-built-bundle.md) — **not this
  story's**, and named so no ticket assumes otherwise: the proofs of record now load `dist/` served
  by `vite preview` on `4173`. No ticket here may write a `verify:` block that starts a server or a
  browser (`ADR-0089` §2b), and every ticket carries the gate that checks it.

## Design notes

Everything below is `ADR-0112`, merged, and the mechanism must serve it.

- **The address names the screen the player chose, and never the room** (§1). While a room is held,
  the address is `/` until a screen is chosen; the table, the wait and the result have **no address
  and get none**, and **no `Screen` member is added for any of them** — `ADR-0076` §§1–2 reaffirmed.
  `?room=CODE` stays an instruction consumed at boot, not a route.
- **While the held room's duel is running, an ask moves nothing** (§2). *Running* means `PLAYING`,
  **grace window included** — `ADR-0105` §2's definition borrowed whole, and read off frames as
  `view !== null && outcome === null` (`ADR-0114` §1). **The ask is refused and the address is
  restored to `/`, and these are one act, not two.** The refusal is **silent**: no notice, no
  dialog, **no new string**.
- **While the held room holds no running duel, the ask is honored** (§3). `WAITING`, `FINISHED` or
  `ABANDONED`: the chosen screen shows and the address names it — `duels`, `leaderboard`, `account`
  and the door-gated `sign-in` behind it. **Nothing about the room moves**: the seat is kept, the
  tab's memory is kept, the socket stays open so the rival never sees an absence, and frames keep
  applying, so `/` is always the room's screen **as it now stands**. The way back is each chosen
  screen's own *Back*, or the browser's. **No new control exists and none is drawn.**
- **`ABANDONED` is not a client standing, and no ticket invents one.** `RoomState` is a server-side
  enum in `Room.kt`; no frame carries it and `ADR-0112` §7 forbids moving the wire on its account.
  A client holding an abandoned room reads `waiting` or `finished` like any other, so §3's promise
  is kept for it by the same two branches and there is nothing extra to build or test.
- **A frame that seats a running duel overrules any chosen screen** (§4), `ADR-0076` §3 as written: a
  waiting host mid-scroll is pulled to the table and the fragment is replaced with `/`. A standing
  rematch **offer** seats nothing and overrules nothing.
- **The account is reachable by the same rule** (§5): mid-duel no, from a `WAITING` or `FINISHED`
  room yes. `ADR-0086` §6's accept lands on the account screen it names. **A mailed link refused
  mid-duel must not spend its token** — the same mail must work after the duel.
- **Phrased in room states, never in screens** (§3), so `STORY-1302`'s answer — wherever it puts the
  waiting host — changes nothing here. This story nonetheless **depends on `STORY-1302`** because
  both edit `Lobby.tsx`'s held-room branch and the run is sequential.
- **`STORY-1310`'s record is this story's input.** Any path that reproduced a defect is repaired
  here or in its own ticket; any path that did not is cited by name so the epic's Definition of done
  closes on written evidence. The table below is where that is written down.
- **No new surface, so no card.** `ADR-0112` names it: *"no new string, no new control"*, and the
  chosen screens stay ignorant of navigation and are not redesigned (`ADR-0060` §4). `ADR-0118`
  §2's silence is a **withholding**, not a surface, so `ADR-0091` §2 owes no card for it either.
  **If a ticket finds it wants one, that is a stop — `ADR-0105` §4's discipline: a screen that needs
  a new sentence for this has outgrown this decision and owes a new ADR.**
- **Costs that are accepted and are not defects** (§Consequences and `ADR-0118` §Consequences), so
  no ticket repairs them: mid-duel confinement is total and silent; the look-away hides the room's
  own surfaces (a waiting host on the ladder does not see their invite link, a finished player does
  not see a rematch offer arrive) and drops any state held in those components rather than in the
  store; a waiting host can be yanked to the table by §4 and no card draws the transition; a mailed
  link clicked mid-duel visibly does nothing; a recovering browser is a **black rectangle** for as
  long as the round trip takes, and permanently so against a server that answers neither frame; the
  stale waiting screen ships with a **destructive control** on it.

## `STORY-1310`'s record, read against this story's repair

[`STORY-1310`](STORY-1310-the-refresh-paths-nobody-drove.md) drove `ADR-0112` §6's six paths in
seven rows. One row here for each: the ticket that repairs it, or why no repair is owed.
`TASK-131114` fills them and is the last ticket in the chain.

| Path | Answered by |
| --- | --- |
| `P1` | NOT-YET-READ |
| `P2` | NOT-YET-READ |
| `P3` | NOT-YET-READ |
| `P4` | NOT-YET-READ |
| `P5` | NOT-YET-READ |
| `P6a` | NOT-YET-READ |
| `P6b` | NOT-YET-READ |

## Tasks

| ID | Title | Status |
| --- | --- | --- |
| TASK-131101 | `roomStanding` reads the room off the frames the server sent | ready |
| TASK-131102 | `rulingOn` answers every ask, and the two mailed screens wait | backlog |
| TASK-131103 | Boot says whether this tab is awaiting a room | backlog |
| TASK-131104 | The provider carries `roomAwaited`, and `main.tsx` hands it over | backlog |
| TASK-131105 | The lobby rules on the ask, and the refusal restores the address in one act | backlog |
| TASK-131106 | The two mailed screens move above the room and read `shown` | backlog |
| TASK-131107 | A mailed token is not spent before the server has answered | backlog |
| TASK-131108 | The record and the ladder move above the room | backlog |
| TASK-131109 | A look-away takes nothing with it, and a frame that seats a duel overrules it | backlog |
| TASK-131110 | The account moves above the room, and the offer's accept has somewhere to land | backlog |
| TASK-131111 | The sign-in screen moves above the room, and the six branches are one block | backlog |
| TASK-131112 | The address `/` renders nothing until the server has spoken | backlog |
| TASK-131113 | The late waiting screen is pinned, and the recovery lands where the frames say | backlog |
| TASK-131114 | The record is read against the repair | backlog |

## Acceptance criteria

- [ ] Holding a `WAITING` room, asking for `duels`, `leaderboard` and `account` each shows that
      screen and leaves the address naming it — one named test per screen
- [ ] The same from a `FINISHED` room. An `ABANDONED` room needs no test of its own: no frame names
      that state, so the client reads it as `waiting` or `finished` and the same two branches serve
      it
- [ ] Holding a `PLAYING` room, the same three asks each leave the table on screen and restore `/` —
      silently, proved by the rendered text being identical before and after the ask
- [ ] The grace window counts as running: `roomStanding` answers `running` for
      `view !== null && outcome === null`, and for a runout still painting
- [ ] A look-away leaves the seat and the tab's room memory untouched and calls no `forgetRoom`, and
      a frame arriving while the player is on another screen still applies
- [ ] A frame that seats a running duel pulls the player to the table and replaces the fragment with
      `/`; a standing rematch **offer** does not
- [ ] `ADR-0086` §6's accept from the result screen lands on the account screen — a regression test,
      red at `1a09fb46`, which is the path `STORY-1310`'s `P5` drove
- [ ] A mailed `verify` link opened over an unknown room leaves its token **unspent** — zero calls
      before any frame, zero once the frames say `running`, exactly one once they say `finished`
- [ ] `/` renders no element and no text while the room is unknown; the same tree with no room
      remembered renders the front door at once; the silence ends on `RoomJoined` **and** on
      `Failure(UNKNOWN_ROOM)` carrying its sentence
- [ ] `RoomJoined` alone still renders the whole waiting screen, promise sentence and all — the
      accepted half of `DEC-127`, pinned
- [ ] No `Screen` member is added for the table, the wait or the result; `PROTOCOL_VERSION` is
      unchanged and no wire type moves
- [ ] No `verify:` block anywhere under this story names `drive.mjs`, `stack.sh` or `vite preview`
- [ ] The seven-row table above is filled, so the epic's refresh Definition-of-done row closes on
      written evidence

## Out of scope

- **Honouring a navigation mid-duel.** `ADR-0112` §Alternatives refuses it while no clock bounds a
  duel; it becomes arguable the day `STORY-1308` lands, and it is a small ADR then — **not this
  story's to take.**
- **An address for the table** (`ADR-0076` §2 forecloses it permanently), and a `Screen` member for
  the wait or the result.
- **Leaving the room on a navigation.** `ADR-0112` §Alternatives refuses the destructive glance.
- **A notice explaining the refusal**, an interstitial during a recovery, and any new control for
  the way back.
- **A client that cannot reach the server at all**, leaving `/` empty indefinitely. `ADR-0118` §5
  names it, gives it no threshold and no reopening condition, and states that it is **deliberately
  not registered** as a `DEC` and does **not** block this story — `ADR-0105` §6's route, *a `DEC`
  nobody is working is noise in the open table*. No ticket here adds a timeout, a fallback or a
  line for it.
- **Closing `ADR-0114` §6's one-frame gap**, and the destructive *Back to the lobby* sitting on the
  screen inside it. Both need the server to name the room's state in the frame that answers the
  join — a wire change, the architect's, and forbidden on `ADR-0112`'s account.
- **`ADR-0118` §4's one-second reading.** Whether `/` stays empty for longer than about a second on
  a built bundle is what would reopen the interstitial question, and it is a **browser drive on
  `vite preview`** — `ADR-0089` §2b forbids it reaching any `verify:` block, and its subject does
  not exist until this story merges. It belongs with `ADR-0117` §7's owed re-read of `P2`'s and
  `P5`'s `delayed` findings, in a story of its own after this one.
- **`DEC-110`'s missing seat check.** That the flashed front door could create a second room is
  evidence *for* `ADR-0118` §2 and is cited as a force; the server's absent guard stays `DEC-110`'s.
- **Redesigning the chosen screens.** `ADR-0060` §4.
- **The engine and the server.** Nothing here opens either.
