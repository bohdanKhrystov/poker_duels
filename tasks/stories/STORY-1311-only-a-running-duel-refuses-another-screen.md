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
| `P1` | `NO-REPAIR-OWED`. Two honest `bare` drives disagree, as this row must record: one saw the held `FINISHED` room's result screen SURVIVE a genuine reload; an earlier partial reading of the same path instead saw the lobby's "No duel room has that code." settle after it. `STORY-1310`'s own redrive at `c655859b` repeated the genuine reload twice more against the same held result screen and reproduced SURVIVES cleanly both times; it did not reproduce the other reading, and `STORY-1310`'s own "Every finding, given an owner" table already dispositions that disagreement — "None filed — a single unreproduced reading is not a confirmed defect" — recorded, not actioned. The behaviour that replicates is the one `ADR-0112` requires, so no ticket in this story answers `P1`, and `ADR-0117` §7 confirms `P1` does not re-drive to satisfy it either. |
| `P2` | Two different wrong screens showed on `delayed 300ms`, answered two different ways; `bare` showed neither and confirms `ADR-0102` §5 cleanly. Before any frame arrived, `open`'s own first paint was the plain create/join front door — `ADR-0118` §2's "wrong-because-false" flash, closed by `TASK-131112` rendering nothing at `/` until the server has spoken. The next frame, still before the true state, was a stale "Waiting for your rival" naming the by-then-`FINISHED` room — `ADR-0118` §3's accepted-because-late stale wait, pinned by `TASK-131113`, so `NO-REPAIR-OWED` for that half. `ADR-0117` §7 separately holds that this `delayed` finding's own weight as evidence against `ADR-0112` §6 — it turns on an 11.8 s first paint dominated by the dev server's own module cost — does not stand until re-read on `built`, a re-read owed to a later story and not yet ticketed. |
| `P3` | `NO-REPAIR-OWED`. `STORY-1310` found this path `BLOCKED`, not a product reading: cutting the relay also cuts Vite dev server's own HMR socket sharing the port, reloading the page before `reconnecting.ts`'s in-place recovery could ever be observed. That was the one decision the path was waiting on, `DEC-087`, now answered by the merged `ADR-0117`: the proofs of record move to a built bundle on `vite preview`'s `:4173`, off the HMR-bearing dev server, and its §7 names `TASK-131006` as the ticket the answer unblocks. Unblocking is not the same as redriving: `STORY-1310`'s own finding table already called the redrive itself "a ticket to file, the planner's," and no ticket anywhere — least of all this story, which `ADR-0089` §2b bars from any browser drive — has performed it, so `P3` owes a re-drive, not a repair. `TASK-131006` exists and `ADR-0117` §7 names it as the ticket that answer unblocks — but it is `status: blocked`, and its body is written entirely against the dev-server-and-relay instrument that could not produce the reading, so a built-bundle re-drive needs that ticket rewritten or a new one filed. Neither has happened. |
| `P4` | `NO-REPAIR-OWED`. At `delayed 300ms`/`1000ms`, a resumed client painted the pre-join "Waiting for your rival" screen over a room that was actually still `PLAYING`, before the true table settled; at `delayed 0ms` nothing was seen, an open inference rather than a proven negative. `ADR-0117` §7 says `P4` "finishes on the mode it started on" and owes no re-drive — its absolute durations are the dev server's own module cost and do not survive a built bundle, but the sequence itself, stale wait then true state, does, being driven by frame arrival rather than module count. That sequence is exactly `ADR-0118` §3's accepted-because-late stale waiting screen: accepted product behaviour, no interstitial, pinned by `TASK-131113` so no later ticket answers `DEC-127` the other way. |
| `P5` | `TASK-131110`. Pressing the result screen's `AccountOffer` accept matched neither of `ADR-0112` §5's named outcomes — not the account screen, not the derived bounce — but a third: the offer is visibly spent and nothing happens for at least 15 s, identically on `bare` and `delayed 300ms`. That was this path's own named `DEC` trigger, `DEC-125`, answered by the merged `ADR-0116` (its §1 "arrives as a side effect of `ADR-0114`'s branch order landing" here) and closed concretely by `TASK-131110`, whose regression test is red at `1a09fb46`, the commit `P5` itself drove. `Delayed`'s secondary finding — a lobby-shaped first paint on a later reload of the same screen, before settling on `Victory` — carries the same dev-server-latency caveat `ADR-0117` §7 names for this row: it does not stand as evidence against `ADR-0112` §6 until re-read on `built`, owed to a later story and not yet ticketed. |
| `P6a` | `TASK-131102`/`TASK-131106` (with `TASK-131107`). On `bare` the mailed `verify` screen never rendered at all, `WAITING` room or not — the shipped `Lobby.tsx` effect blanked any non-`first` fragment whenever a room was held, so a real token survived only by that accident; `TASK-131102`'s `rulingOn` and `TASK-131106`'s move of the two mailed screens above the room branches now honour a non-running room's ask instead, per `ADR-0112` §3. On `delayed 300ms` the mount effect fired and completed ("expired or has already been used") within about 0.2 s of the first frame confirming `WAITING` — a reading consistent with `WAITING` being honoured, though, like `P6a`'s own row, not traced through `Lobby.tsx`'s source to prove that is why rather than the effect simply firing on any first frame regardless of state. `STORY-1310`'s own finding table reads this reading together with `P6b`'s as evidence that the shipped effect "does not gate `verify`/`reset` on room state at all"; `P6b`'s reading is the one with teeth, since firing unconditionally is what let it spend a token mid-duel, but `TASK-131107`'s hold on `roomStanding` closes the shared cause for both paths, `P6a` included, even though nothing here was visibly harmed by it. |
| `P6b` | `TASK-131107`. Both layouts confirmed the room genuinely `PLAYING` before the mailed link was opened; on `bare` the screen never rendered at all, the same pre-existing blanket erase as `P6a`, so the token survived by the same accident. On `delayed 300ms` the mount effect fired and completed ("expired or has already been used") within about 0.2 s of the first frame, over a hand genuinely in progress — `ADR-0112` §5's silent, permanent failure, and the more damaging half of this pair, since a live token would have been spent mid-duel; `STORY-1310`'s own finding table names `ADR-0114` §5's `hold`/`roomStanding` as the fix and `TASK-131107` builds it, closed by `ADR-0114` §7's "Not spent" test — zero `verifyEmail` calls fed `RoomJoined`+`Snapshot`, exactly one fed `RoomJoined`+`DuelFinished` — which is `STORY-1311`'s own acceptance criterion. The frames that followed also replayed a stale "Waiting for your rival" over the still-`PLAYING` room before the true table arrived, independently reproducing `P4`'s own finding by a different route — `ADR-0118` §3's accepted-because-late stale wait, `NO-REPAIR-OWED` and pinned by `TASK-131113`, not a second defect here. |

## A measured gap: the token-effect coupling is unprotected

`TASK-131105` re-keys `Lobby`'s token effect to `shown` in the same diff as the `useLayoutEffect`
swap. Both the split's ticket text and this PR's reviewer predicted that breaking that coupling —
re-keying back to `screen` — would redden the merged test `lets a frame that seats this tab outrank
a mailed link`, because the token effect would fire after the restore and write `#/verify` back over
the `/`.

**It was measured on 2026-09-05 and the prediction is false.** Re-keying to `screen` leaves **all 80
tests in `Lobby.test.tsx` green.** No assertion in the file distinguishes the two keys, because no
test exercises a case where `shown` differs from `screen` *while a mailed token is pending* — which
is the only shape in which the dependency array matters.

So the coupling is correct, required by `ADR-0114`, implemented, and **held by nothing but a
comment**. A later ticket touching `Lobby.tsx` can undo it silently with every gate green.

This is the second thing in `TASK-131105` that is real in production and invisible to this suite;
the first is the `useLayoutEffect` ordering itself, which the coder measured the same way — swapping
it back to `useEffect` leaves the new tests green too, because RTL's `act()` flushes passive effects
synchronously. That one at least has a count gate on `useLayoutEffect`. The token-effect key has no
gate at all.

**Confirmed still open after `TASK-131106`.** That ticket's coder reported the move had closed it —
that re-keying to `screen` now reddened the merged test. Two further runs, the reviewer's and the
driver's in the primary checkout, both found all 81 tests still green under that mutation, so the
claim was withdrawn before merge. The likely reason it cannot be tested: line 100's *condition* and
line 104's *dep array* both read `shown`, so re-keying only the array changes **when** the effect
runs and not what it does. Three predictions have now been wrong about this one coupling.

**A second gap, found at `TASK-131109`'s review.** `ADR-0112` §3 also promises that **frames keep
applying** while a player looks away. `TASK-131109` gated the section's other two promises — nothing
about the room moves, and a frame that seats a running duel overrules the chosen screen — but nothing
asserts that a frame which does *not* seat a duel still lands while the ladder or the record is on
screen. The ticket's Goal named only the two, so this is out of its scope rather than missed by it.

**Owner:** `TASK-131114`, which reads this record against the repair, must either carry the missing
assertion — a render where the ruling moves `shown` off `screen` with a token pending — or say in as
many words that it is being left unguarded and why. Every ticket from `TASK-131106` on is briefed
that `Lobby.test.tsx` will not catch an accidental re-key.

**Disposition (`TASK-131114`).** This ticket's one editable file is this table and this prose, not
`Lobby.test.tsx` or any other suite, so neither measured gap gets its missing assertion here; both
stay open in writing rather than by omission. (1) The token-effect coupling: closing it needs a
render in `Lobby.test.tsx` that boots at a mailed fragment with a room remembered, drives a ruling
under which `shown` settles away from the raw `screen` while the token is still pending, and asserts
the call count both with the effect keyed on `shown` and with it deliberately re-keyed to `screen` —
the one shape absent from the suite today, which is why three independent predictions that a re-key
would already fail were each wrong. No such test exists after this ticket, and nothing gates a future
re-key. (2) `ADR-0112` §3's "frames keep applying": closing it needs a render, in whichever suite
already covers `Lobby` or the ladder/record components, that holds `shown` on a non-seating screen —
`duels` or `account`, over a held `WAITING` or `FINISHED` room — delivers a further frame that updates
the store without seating a duel, and asserts the update reaches the screen rather than being
swallowed. No such test exists after this ticket either. Both are documentation gaps closed here by
naming exactly what is missing; neither is a code gap this one-file ticket may touch.

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
