# ADR-0152 — The finished room's five minutes stand, because they never measured the holder's screen

- **Status:** Accepted
- **Date:** 2026-09-10
- **Resolves:** `DEC-164` — **does the finished room's five minutes still cover the window
  [`ADR-0150`](ADR-0150-back-to-the-lobby-keeps-the-room.md) §1 opens, now that the room's holder may
  spend the whole of it on another screen?** **Yes, and nothing changes.** Registered 2026-09-10 by
  that ADR §7, which named *nothing changes* a complete answer. No production line moves: the reaping
  rule, `RoomTimeouts.finishedMillis`, its default and its deployment override all stand exactly as
  merged, and this ADR mints no ticket but a characterisation test (§6).
- **The measurement, on `develop` at `d5ef7df8`** — the commit that merged `ADR-0150`. A `FINISHED`
  room's reaping deadline is a function of `Room.lastActivityAt` and of nothing else: not of which
  screen its holder is on, not of whether a socket is open, not of whether an offer stands. Exactly
  **two** things move that field on a `FINISHED` room — `Room.finish(now)` (`Room.kt:238`), which
  stamps it as the duel ends, and `RoomRegistry.resume`'s `room.reconnect(seat).touch(now)`
  (`RoomRegistry.kt:390`), which a returning socket triggers. **A wander is neither, and neither was
  sitting on the result screen.** The interval the room must survive is `[finish, finish +
  finishedMillis)` before `ADR-0150` and the same interval after it.
- **What `ADR-0150` §1 changed is how much of that interval a presser can reach** — from none of it
  (`forgetRoom()` left the tab holding no room, so no frame about one was addressed to it) to all of
  it, which is what a player who never pressed always had. §1 **spends** room life that was already
  allocated; it does not ask for more. That is the whole answer to `DEC-164`, and everything below is
  the evidence for it and the cost of it.
- **Confirms, and does not amend,** [`ADR-0150`](ADR-0150-back-to-the-lobby-keeps-the-room.md) §4's
  first bullet — *"The room's own idle reaping — `RoomTimeouts.finishedMillis`, five minutes by
  default"* — and §1's *"still reaped on `lastActivityAt` idleness after `RoomTimeouts.finishedMillis`
  (`RoomRegistry.kt:841`)"*. Both are measured true here.
- **Supersedes nothing and amends nothing.**
  [`ADR-0025`](ADR-0025-one-ticker-coroutine-drives-both-sweeps.md)'s one ticker,
  [`ADR-0044`](ADR-0044-a-rematch-is-one-intent-and-one-room-fact.md) §6's silence,
  [`ADR-0016`](ADR-0016-a-room-is-serialised-by-its-own-mutex.md)'s per-room mutex and
  [`ADR-0032`](ADR-0032-react-subscribes-to-a-store-it-does-not-own.md)'s one-socket-per-tab boot are
  all read as evidence here and left byte-unchanged.
- **Moves no code, no wire, no `PROTOCOL_VERSION`, no schema, no configuration default, no stored key
  and no word on any screen.**
- **Does not answer `DEC-163`**, which is a different decision registered by the same ADR and belongs
  to whoever holds it.
- **Registers, and does not answer:** `DEC-165` — **the product owner's** — does a rematch offer owe
  its recipient a minimum time to answer, and if so how long? (§5.)

## Context

**`ADR-0150` handed this over as a suspicion, in the human's own words.** Its Consequences: *"rooms now
outlive the result screen, so the three-to-five-minute room lifetime has to cover a player who has
wandered off to another screen rather than only one sitting on the result."* Read literally that says
the lifetime is now under-provisioned, and the two obvious repairs are a bigger number or a clock that
refreshes while somebody is holding the room. `ADR-0150` refused both on the spot — *"nobody has
measured what the current number covers"* — and registered `DEC-164` so that somebody would measure.
This is that measurement.

**What the deadline is actually a function of.** `RoomRegistry.isReapable` (`RoomRegistry.kt:836-844`)
is four lines: a room whose result is still being recorded is never reapable; a `WAITING` room dies at
`now - lastActivityAt >= timeouts.waitingMillis`; a `FINISHED` or `ABANDONED` room at
`now - lastActivityAt >= timeouts.finishedMillis`; a `PLAYING` room never. The defaults are ten minutes
and five (`RoomTimeouts.DEFAULT_WAITING_MILLIS`, `DEFAULT_FINISHED_MILLIS`), each overridable per
deployment (`ROOM_FINISHED_TIMEOUT_MILLIS` / `room.finishedTimeoutMillis`, `ServerConfig.kt:123-125`).
The sweep that calls `reap()` runs on a fixed **one-second** delay (`DEFAULT_SWEEP_PERIOD_MILLIS`,
`Application.kt:146-152`), so a room dies within a second of its deadline and the sweep's granularity
is not a force in this decision.

**Nothing in that expression can see a screen.** The complete set of writes to a `FINISHED` room's
`lastActivityAt`, read off the whole server source rather than inferred:

- `Room.finish(now)` (`Room.kt:238`) — the stamp the clock runs from.
- `RoomRegistry.resume` → `room.reconnect(seat).touch(now)` (`RoomRegistry.kt:390`) — the **only**
  `touch(` call site in the entire main source. It fires when a seated player's socket sends
  `JoinRoom`, which `replyToJoinRoom` routes to `resume` before it ever tries `join`
  (`DuelSocket.kt:577`).

And the things that conspicuously do **not** write it:

- **A standing offer.** `Room.offerRematch`'s `Offered` branch returns `copy(rematchOffers = offers)`
  (`Room.kt:330`) — the field is not in that copy, and `RoomRegistry.mutate` adds no implicit stamp: it
  writes back exactly what the block returns (`RoomRegistry.kt:861-877`). A rival's offer at
  `finish + 4:59` leaves the deadline where it was. Only an **agreed** rematch restamps, and that room
  is `PLAYING` and off the reaper's list entirely.
- **A socket dropping.** `Room.disconnect(seat)` copies `awaySeats` and `absentSeats` and nothing else
  (`Room.kt:489`), and the socket's `finally` calls only `rooms.disconnect` (`DuelSocket.kt:210-228`).
- **The turn-clock sweep.** `expireTurnClocks` will take a `FINISHED` room's lock while a seat is away,
  and `Room.giveUpTurn` answers `null` on its first line for any room that is not `PLAYING`, so the
  pass writes nothing. Nothing in production calls `RoomRegistry.abandon` at all: a `FINISHED` room
  whose players are both gone stays `FINISHED`, on its original stamp.

**And nothing a browsing player does reaches any of them.** The client's whole message set is
`Act | CreateRoom | Hello | JoinRoom | OfferRematch` (`protocol.gen.ts:6`) — there is no ping, no
keepalive and no *leave*. Screens are the URL **hash** (`use-screen.ts:58-75`), and the tab's one
socket is booted once outside the React tree (`main.tsx:172-178`, `ADR-0032`), so a screen change opens no
socket, sends no frame, and cannot restamp anything. A player reading the ladder generates exactly as
much room activity as a player staring at the verdict: none.

**So the two windows, stated exactly.** Write `F` for `finishedMillis`, `t0` for the finish and `tp`
for the press.

| | Before `ADR-0150` | After `ADR-0150` |
| --- | --- | --- |
| The room lives | `[t0, t0 + F)` | `[t0, t0 + F)` |
| A player who stays on the result screen can be offered to | `[t0, t0 + F)` | `[t0, t0 + F)` |
| A player who presses *Back to the lobby* at `tp` can be offered to | `[t0, tp)` | `[t0, t0 + F)` |

The bottom row is the whole of `ADR-0150` §1. Nothing in it extends the top row, and the window §1
opens is bounded above by a window that already existed and was already being thrown away. **The
lifetime does not have to grow to cover the wander: the wander fits inside what the sitter always
had.**

**What genuinely is new, and it is not a duration.** Before, a rival offering to a presser was offering
into nothing — `ADR-0044` §6's silence, nobody disappointed. Now the offer arrives wherever the player
is, and if it arrives late the panel's `Rematch` press answers `Failure(UNKNOWN_ROOM)` —
`ADR-0044`'s own *"the button can lie"*, told for the first time to a player the product deliberately
invited to press it. `ADR-0150` predicted exactly this. The measurement adds the sharp edge: because a
standing offer does not restamp, the time a recipient has to answer is `F - (toffer - t0)`, **whose
floor is zero**. A room can be reaped a millisecond after the panel appears.

**The evidence for changing anything is still thin, and the deadline is soft.** No counter exists for
how late in a room's life offers are made; nothing measures `rooms.size`; no player has reported a dead
room; and no string on any screen states the lifetime, so nothing has been promised that could be
broken by leaving it alone — `grep` for *minute* across `web-client/src` finds the turn clock and two
comments. Nor is this a decision that is free today and impossible later: every alternative below is a
handful of lines against code that already has a test file, and none of them becomes harder by waiting.
That is the case for the cheapest answer available, and for saying that is why it was taken.

## Decision

### 1. The reaping rule is unchanged, and `DEC-164` mints no production change

`RoomRegistry.isReapable` keeps its `FINISHED, ABANDONED -> now - room.lastActivityAt >=
timeouts.finishedMillis`. `RoomTimeouts.finishedMillis` keeps its meaning, `DEFAULT_FINISHED_MILLIS`
keeps its five minutes, and `ROOM_FINISHED_TIMEOUT_MILLIS` keeps its job — a deployment that wants a
different number already has the lever, and this ADR neither pulls it nor recommends pulling it.

### 2. The answer to `DEC-164` is that §1 opened no new interval

The finished room's five minutes cover the window `ADR-0150` §1 opens **because that window is a subset
of the one the room already served**. The reaping deadline is `finish + finishedMillis` and has never
been a function of the holder's screen, socket or attention; `ADR-0150` moved the screen, not the
clock. Any future argument that the number is wrong must therefore rest on the number having always
been wrong — for the sitter as much as for the wanderer — and not on `ADR-0150` having consumed
anything.

### 3. The set of writes to a `FINISHED` room's `lastActivityAt` is closed at two, and a ticket may not add to it

`Room.finish` and `RoomRegistry.resume`'s `touch`. Adding a third — on an offer, on a presence change,
on a frame, on a screen ask — is a change to how long a room lives, which is a change to how long a
player may wait, and it is not a repair a coder makes inside a ticket that happens to be nearby. Any
such addition needs an ADR, and if it moves what a player experiences as a duration it needs `DEC-165`
answered first (§5).

### 4. A standing offer goes on not restamping the clock

`Room.offerRematch`'s `Offered` branch stays `copy(rematchOffers = offers)`. It reads like an oversight
and is hereby a decision: today a room's life is *five minutes after the duel*, one sentence, the same
for both players and independent of what either of them does. **That branch is also exactly where any
minimum answering window would land** — one extra field in one pure copy, with `RoomReapTest` and
`RoomRematchTest` already standing — so the cheap door stays open without being walked through.

### 5. Whether a rematch offer owes a minimum answering time is registered as `DEC-165`, for the product owner

The mechanism is the architect's and it is one field; the floor is not. *How long a player has to answer
a rematch offer* is a duration a player waits on and therefore a promise, and this ADR will not pick a
number, nor smuggle one in by making an offer restamp a clock. The constraint the measurement hands the
product owner is exact: **the floor today is zero**, the ceiling is `finishedMillis`, what a recipient
actually gets is `finishedMillis - (toffer - finish)`, and no screen states any of it. `DEC-165` blocks
nothing — the window as it stands is what ships.

### 6. One characterisation test follows, and no production ticket

So that §§3–4 are not sentences only, `RoomReapTest` gains a case pinning that a standing offer leaves
the deadline where the finish put it: a `FINISHED` room that one seat has offered a rematch on is still
reaped at `finish + finishedMillis`, on the same clock, against the same room. It must fail if the
`Offered` branch starts stamping `lastActivityAt` — that is the whole point of it — and if `DEC-165` is
one day answered with a floor, this test is the thing deliberately rewritten rather than the thing that
silently goes on passing.

## Consequences

**What it buys.** The cheapest possible answer to a question whose other answers were both expensive:
no code, no number, no new state, no migration, nothing to reverse. It replaces `ADR-0150`'s suspicion
with an argument a future reader can check against four lines of `isReapable`, and it takes the
specific fear — *the wander eats the room's life* — out of the register instead of leaving it to be
re-triaged every QA round. It also writes down two facts that were previously true only by accident:
that a screen change touches nothing, and that an offer does not restamp.

**What it costs.**

- **The product now ships a control that can lie to a player it invited to press it, and this ADR
  chooses to leave it that way.** A rival who offers at `finish + 4:59` puts a `Rematch` control in
  front of a wandering player that has under a second to work; the press answers
  `Failure(UNKNOWN_ROOM)` and a sentence. Before `ADR-0150` that player could not be offered to at all,
  so the failure could not be seen; `ADR-0150` made it visible, and this ADR declines to make it
  impossible. That is the cost carried by not changing anything, and it is carried until `DEC-165` is
  answered — which may be with *nothing changes* again, at which point it is carried permanently and
  deliberately.
- **The answering window has no floor, and the player least likely to be watching is the one who gets
  it.** `finishedMillis - (toffer - finish)` can be a millisecond. The wanderer is by construction
  reading something else, so the gap between the panel appearing and the player noticing it is now
  seconds rather than zero — measured against a remainder nobody controls.
- **Finished rooms accumulate a little more than they used to, and their five minutes are
  refreshable.** Every wandering holder's tab still holds the room, and every reload of any screen by
  such a tab sends `JoinRoom` → `resume` → `touch`, buying that room another `finishedMillis`. Before,
  a presser's tab had forgotten the room and its reloads cost nothing. `rooms` is an unbounded
  `ConcurrentHashMap` (`RoomRegistry.kt:53`) with no cap and no instrumentation, so a finished room has
  no absolute lifetime at all — only an idleness one a holder can keep pushing out. Small per room,
  unbounded in principle, and this ADR knowingly does not bound it.
- **The answer rests on structure, not on evidence, because the instrument does not exist and this ADR
  does not build it.** Nobody knows what fraction of offers land in a room's last thirty seconds. The
  reasoning here — that the interval did not change — holds whatever that fraction is, but the
  *number's* fitness is exactly what it cannot speak to, so the trigger for revisiting it is a QA
  report or a player's complaint rather than a metric.
- **`DEC-164` closes having changed nothing, which is the kind of answer that reads as though the
  question was not taken seriously.** The mitigation is §§3–4 and §6's test: what the register keeps is
  a measurement, not a shrug.

**What it forecloses.** Nothing structurally. Every alternative below stays one small diff away: no
timer is built, no field is added, no configuration is spent, no wire message is minted, and no test is
written that would have to be deleted to change course. The one thing it does foreclose is a coder
adding a restamp inside an unrelated ticket (§3), and that foreclosure is the point.

**What it deliberately leaves open.** Whether five minutes is the right promise at all. `DEC-165` takes
the offer-side half of that question to the product owner; the whole-room-lifetime half remains
`ROOM_FINISHED_TIMEOUT_MILLIS`'s, and nobody has asked for it to move.

## Alternatives considered

**Restamp the clock on a standing offer — `copy(rematchOffers = offers, lastActivityAt = now)`.** The
strongest of the set by a distance, and the closest to being right. It is a single field in a pure
function; it guarantees the thing the product plainly wants, that an offer a player can see is an offer
they can answer; it needs no new number, no configuration, no wire and no client change; and the
extension it grants is **bounded** rather than open-ended — a second offer from the same player refuses
`ALREADY_OFFERED` (`Room.kt:315`), so there is at most one restamp per seat and a worst case of
`finish + 3 × finishedMillis`. Rejected here because it changes what a player waits on: after it, *the
room ends five minutes after the duel* becomes *five minutes after the last offer*, and that is a
promise, not a mechanism — precisely the line `DEC-164` drew when it said a different number comes back
to the product owner. It is not free, either: it hands a rival a small lever over a stranger's room, one
press that extends someone else's memory footprint by five minutes. Registered as `DEC-165` rather than
taken, with §4 naming this exact branch as where it lands if the answer is yes.

**Refresh on the holder's presence — a room whose holder still has a socket does not die.** `DEC-164`
named this a complete answer, and its case is the most faithful reading of the cost `ADR-0150` recorded:
the room dies under a player who is still right there, and the server already knows who is connected
(`ConnectionDirectory`, `Room.awaySeats`). Rejected on three counts. It makes a room's life a function
of a browser tab being open, which is unbounded — a forgotten tab holds a room forever, and `ADR-0018`'s
seat adoption means the tab need not even be the current one. It changes what reaping *means*: `reap()`
collects rooms that are idle, and a room nobody is playing in is idle no matter who is watching it —
presence is `ADR-0045`'s table fact, not a liveness signal about a room. And it needs a write path from
the connection layer into `RoomRegistry` on every heartbeat, against a design whose whole virtue is that
a room is judged by its own timestamp under its own mutex (`ADR-0016`).

**Raise `DEFAULT_FINISHED_MILLIS` now — five minutes becomes ten.** Its case: one constant, no code, no
test beyond the ones that already read the field, and it buys headroom directly for the case the human
named. Rejected twice over. It is not the architect's — a duration a player waits on is a promise, and
`DEC-164` said in as many words that a different number comes back to the product owner. And the
measurement removes its premise: the wander consumed nothing, so a number raised for it would be a
number raised against an imagined failure, at the price of every finished room on the server living
twice as long. `ADR-0150` rejected the same option on the same reasoning one day earlier; this ADR
confirms that rejection with the measurement it was waiting for.

**Cap a finished room's absolute life — reap at `finish + N` whatever the touches say.** Its case is the
third cost above: today a finished room has no ceiling, only a refreshable idleness window, and a cap
would make the population predictable and close the reload lever. It is two lines in `isReapable` given
a second timestamp on `Room`. Rejected because it needs a **second** number, hence a second promise, and
because it would break the one refresh the product genuinely depends on: a player who reloads while
looking at their result must not lose the room they are still holding. Nothing has been observed
accumulating, and the field it would need does not exist. If accumulation ever becomes real, this is the
shape to reach for.

**Instrument first: count how late in a room's life offers arrive, then answer.** Its case is the honest
one — this ADR admits its evidence is thin, and a decision about a duration taken without telemetry is a
decision taken on taste. Rejected as disproportionate to what was actually asked: `DEC-164` asks whether
`ADR-0150` changed the fit, and that is answerable structurally and exactly — the interval did not
change. Telemetry would inform the **number**, which is not the architect's to pick, so the instrument's
real customer is whoever answers `DEC-165`, and they can ask for it. Named here so that they know it does
not exist, and that no counter in this server can tell them how often the button lies.

**Give the player a way to end the room deliberately, so a wanderer is not held for five minutes.** Its
case: it addresses the mirror-image complaint — not *the room died too soon* but *the room held me too
long* — and a duelling product arguably owes a decline. Rejected as outside this decision and already
answered next door: `ADR-0044` §6 chose silence over a decline, and `ADR-0150` names this gap in its own
*deliberately left open*. Inventing it here, out of a question about a reaping deadline, is how a
decision becomes a feature.
