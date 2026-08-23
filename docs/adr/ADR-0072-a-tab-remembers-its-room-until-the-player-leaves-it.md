# ADR-0072 — A tab remembers its room until the player leaves it, and the way back is what forgets

- **Status:** Accepted
- **Date:** 2026-08-23
- **Resolves:** `DEC-067`
- **Builds on:** [`ADR-0032`](ADR-0032-react-subscribes-to-a-store-it-does-not-own.md) §2 (boot owns
  the connection and every message-triggered send, outside the tree) and §3 (what a screen may
  hold, and that it sends only from event handlers);
  [`ADR-0044`](ADR-0044-a-rematch-is-one-intent-and-one-room-fact.md) §5, whose server half is
  merged and, as of today, unreachable
- **Constrains:** [`STORY-0309`](../../tasks/stories/STORY-0309-rematch.md) — the transport half of
  its fourth acceptance criterion — and reverses one branch of `TASK-031009`, which stays `done` and
  is not rewritten
- **Leaves open:** whether the *waiting for your rival* screen ever gains a way out (see
  Consequences), and `DEC-054`, which owns addresses and browser *Back* for the whole client

## Context

Two merged halves of one feature disagree, and each is defended by a green test.

**The server restates a standing offer to a returning socket.** `DuelSocket.replyToJoinRoom` calls
`RoomRegistry.resume`, which answers for a `FINISHED` room, sends `RoomJoined`, delivers the
resumed frames — for a finished duel, `DuelFinished` alone — and then:

```kotlin
for (player in resumed.room.rematchOffers) {
    val seat = resumed.room.seatOf(player) ?: continue
    send(ProtocolCodec.encode(ServerMessage.RematchOffered(seat)))
}
```

That is `ADR-0044` §5, shipped by `TASK-021307`, and §5 says why it exists: *"without this, an offer
made while the opponent was inside their disconnect grace window would be delivered to nobody and
never restated — the one way this feature could silently lose a fact the room is still holding."*

**The client forgets the room at the moment that path becomes relevant.** `boot.ts` has a branch,
merged by `TASK-031009`:

```ts
if (message.type === "DuelFinished" && options.storage) {
  forgetRoomCode(options.storage);
}
```

and a merged test that states the consequence outright — `boot.test.ts`'s *sends no JoinRoom on the
Welcome after a duel has finished*, which drives `Welcome`, `RoomJoined`, `DuelFinished`, a second
`Welcome`, and asserts `sentJoinRooms(socket)` is `[]`. A second `Welcome` is what a reopened socket
produces: `reconnecting.ts` opens a fresh `openConnection` per attempt, each of which re-`Hello`s.
So the assertion reads, in the terms the feature cares about: **after a duel finishes, no socket
this tab opens ever rejoins the room again.** `RoomMembership` on that socket stays empty, so the
player's own `OfferRematch` answers `Failure(UNKNOWN_ROOM)` — `replyToOfferRematch`'s first branch —
and the room's standing offer reaches nobody. The asymmetry is not hypothetical and not a
prediction: it is one merged test asserting that another merged code path cannot run.

Four forces decide which half moves.

1. **`TASK-031009`'s reason is still true.** `TASK-030807` made the way on from the result an
   `<a href="/">`, and argued it in the ticket: the reducer clears nothing a frame established, so
   an empty store is reached by *booting* one. If the tab still remembers the room, that reload
   re-`JoinRoom`s, `resume` answers `RoomJoined` + `DuelFinished`, and `Lobby.tsx`'s first branch
   (`state.outcome !== null`, deliberately ahead of `view` and `roomCode`) puts the same result
   screen back up. Undo the forget and nothing else, and the lobby is unreachable by the only route
   the client has to it.
2. **One storage key is answering two questions.** `pd.roomCode` is read at exactly one place —
   boot's `Welcome` reaction — where it answers *what should this new socket ask for?* `TASK-031009`
   used it to answer *which screen should this tab show?* The two answers agree everywhere except
   in the window this decision is about: after `DuelFinished`, while the room is still alive and the
   player may or may not still want it.
3. **Nothing on the wire says *leave*.** `ADR-0044` ships no `LeaveRoom` and vacates no seat; a room
   is left by being reaped (`ADR-0022`'s `finishedMillis`, five minutes by default). So *"the player
   has left"* is a client-side fact that no frame can carry, and anything that clears the memory on
   that fact has to be reachable **from a screen** — where today a component may hold the store and
   `send`, and nothing else (`ADR-0032` §3).
4. **A memory can go stale, and staleness must not strand.** Five minutes after the finish the room
   is gone. Whatever is decided has to have an answer for a tab that boots holding a code for a room
   that no longer exists, and *"shows a dead screen with no way off it"* is not one.

## Decision

**A tab remembers the room it is seated in until the player leaves it, or until the server says
that room is gone. `DuelFinished` forgets nothing. The way back to the lobby is the control that
forgets, and boot exposes the forget as the third member of `DuelClient`.**

### 1. What the memory means

`pd.roomCode` names **the room this tab is seated in** — not the screen this tab should show. It is
written on `RoomJoined` and read in boot's `Welcome` reaction, which are the only two places that
touch it. The screen remains entirely the store's business: `Lobby.tsx` decides what to render from
folded frames, exactly as it does today, and never reads storage.

### 2. `boot.ts` loses its `DuelFinished` branch

The branch quoted above is deleted, and nothing replaces it in that reaction. A finished duel is a
fact about the duel; it is not a fact about which room this tab is seated in, and the room outlives
it on the server on purpose.

### 3. Exactly two things clear the memory

- **`forgetRoom()`** — the player leaving, §4.
- **`Failure(UNKNOWN_ROOM)` answering this tab's own rejoin**, behind `TASK-031010`'s `rejoining`
  flag, **kept exactly as written**. Its reason is untouched: the same refusal reaches a player who
  mistyped a code in the lobby, and that must not throw away a room this tab is seated in.

Nothing else clears it. Not `DuelFinished`, not a timer, not a `Failure` that answered something
else, not the store.

### 4. `DuelClient` gains `forgetRoom`, and the provider passes it down

```ts
export interface DuelClient {
  readonly store: DuelStore;
  readonly send: (message: ClientMessage) => void;
  /**
   * Forgets the room this tab remembers, so no socket opened after this one
   * rejoins it. It tells the server nothing — there is no leave on the wire —
   * and the socket that is open keeps its seat: the memory is about the next
   * socket, never the current one.
   */
  readonly forgetRoom: () => void;
}
```

Boot implements it as `forgetRoomCode(options.storage)` when it was given a `Storage`, and as a
no-op when it was not — the same optionality `BootOptions.storage` already carries, for the same
reason it gives.

`DuelProvider` gains a third prop, `forgetRoom?: () => void`, and `useForgetRoom(): () => void` sits
beside `useSend()`, returning a no-op where no provider supplied one. **`main.tsx` passes
`client.forgetRoom`.** The prop is optional so that the fourteen other render sites, in seven files
that are about something else, stay byte-identical; the cost of that choice is in Consequences, and
it is real.

**The name is the mechanism, deliberately.** `leaveRoom` would be a lie — nothing is sent, no seat
is vacated, and the server is not told. `forgetRoom` says what happens, which is what a reader needs
in a client whose only authority on room membership is the server.

**`ADR-0032` §3's rule extends to it verbatim: screens call it from an event handler only — never
from render, never from an effect.** An effect that forgot on mount would fire twice under
StrictMode and, worse, would fire on the mount a rejoin has just produced, deleting the memory of a
room the tab is sitting in.

### 5. The way back forgets, then navigates

`DuelResult` gains `onLeave?: () => void` and puts it on the existing anchor's `onClick`. The anchor
stays an `<a href="/">`: no `preventDefault`, no `window.location` call. `Storage.removeItem` is
synchronous, so the memory is gone before the browser leaves the page, and the navigation stays the
browser's — which is what makes the lobby an empty store, `TASK-030807`'s argument, unchanged.

`Lobby.tsx`'s `state.outcome !== null` branch passes `onLeave={useForgetRoom()}`'s result, the same
shape `TASK-030910` already uses to hand that branch a `send`-backed `onOffer`. `DuelResult` stays a
function of its props and reads no hook and no storage, which is what `TASK-031009`'s acceptance
criterion asked of it and what this keeps.

This is the only control that forgets today.

### 6. A stale memory is corrected by the server, not by a clock

A tab that boots holding a reaped code sends `JoinRoom`, `resume` answers `null`, the ordinary join
refuses, and `Failure(UNKNOWN_ROOM)` arrives while `rejoining` is true: §3 forgets it, the reducer
records `refusal`, and `Lobby.tsx` renders the lobby with *No duel room has that code.* One round
trip, one of `ADR-0022`'s ten budgeted joins a minute, once per stale code, and no client-side
clock anywhere.

**The client runs no timer against the rematch window.** `ADR-0044` decided the wire carries no
deadline, and `ADR-0028`'s rule is that a client renders a deadline and never acts on one. A memory
that expired on a number the client invented would be a second, unsynchronised authority on a
window the server owns: too short and it deletes a rematch that was still available; too long and
it buys nothing over the refusal above, which is exact.

### 7. `UNKNOWN_ROOM` answering a rematch press forgets nothing, and that is enough

Boot cannot see which frame a `Failure` answers, and tracking an in-flight `OfferRematch` to find
out would be the client-side lock `ADR-0044` §3 says no client needs. It is not needed: the screen
`TASK-030909` builds retires the control and says *That duel room is gone.*, with `TASK-030911`'s
way back directly below it — and that control forgets (§5). The only way a code for a reaped room
survives the moment the player is told about it is the player closing the tab instead of pressing;
§6 corrects that on the next boot.

### 8. What does not change

The reducer: no field added, none cleared, `roomCode` and `outcome` still outlive the duel. The
wire: nothing. `poker-server`: nothing — `ADR-0044` §5's server half is already merged, and this is
what makes it observable. `room-memory.ts`: the key, the `Storage` it is handed, and all three
functions unchanged. `TASK-031010`'s flag and its three tests: untouched, including *keeps the room
when the refusal answered no rejoin of its own*.

### 9. Which merged assertions go, and what has to replace them

`TASK-031009`'s two tests in `boot.test.ts` assert the behaviour this reverses. They are
**replaced, not deleted**, and the replacements must be at least as strong — a suite that simply
lost them would be green for a client that had stopped remembering anything at all:

| Goes | Replaced by |
| --- | --- |
| `forgets the room once the duel has finished` | a test that `RoomJoined` + `DuelFinished` leaves the code in storage, **and** a test that `forgetRoom()` removes it |
| `sends no JoinRoom on the Welcome after a duel has finished` | a test that the same three frames plus a second `Welcome` send exactly one `JoinRoom`, **at that code**, and a test that the same run with `client.forgetRoom()` before the second `Welcome` sends none |

Both pairs are needed in both rows: the storage assertion alone cannot tell *remembered* from
*rejoined*, and the frame assertion alone cannot tell *forgotten* from *never written*. One
end-to-end assertion is also owed, because §4's prop is optional and §5's wiring is otherwise
proven only one seam at a time: from the frames a resumed socket delivers, pressing the way back
forgets the code.

`TASK-031009` itself stays `done` and is not rewritten. It records what was done and why, this ADR
records the reversal, and a reader who greps `forgetRoomCode` lands on both.

## Consequences

**What it buys.** `ADR-0044` §5 becomes reachable, which turns a merged, tested, never-executed
server path into behaviour: a player whose phone locked on the result screen comes back to a room
that still holds their rival's offer, and their own press reaches a socket that is in the room.
`STORY-0309`'s fourth acceptance criterion gains the transport half it is missing, and
`TASK-030913`'s two ordering tests stop describing a rejoin the client cannot perform. One rule
replaces a special case: the memory means one thing, is written at one frame, and is cleared by the
player leaving or by the server refusing. Nothing in the store moves, nothing on the wire moves, and
no Kotlin is written — `EPIC-03`'s standing rule holds.

**What it costs.**

- **A merged ticket is reversed, and two green tests are deleted.** `TASK-031009`'s branch is
  removed and its two assertions go. The trail now holds a `done` ticket whose Goal sentence is no
  longer the client's behaviour, left as written on `ADR-0071`'s precedent — with the cost that a
  reader who finds that ticket first learns today's rule only if they follow it here. The
  replacements in §9 are the guard against this being a weakening rather than a reversal; a PR that
  drops the two and adds fewer is not implementing this ADR.
- **The way back is no longer a plain link.** It now depends on a click handler running before the
  navigation. A modifier-click — ctrl, cmd, or the middle button — fires the handler and opens the
  lobby in a *new* tab while this one stays on the result screen having already forgotten its room.
  The player is left looking at a screen whose rematch control will answer `UNKNOWN_ROOM` on the
  next socket. Small, real, and not fixable while keeping both the link and the forget.
- **A tab closed on the result screen comes back to it.** Until the player presses the way back or
  the room is reaped, `/` is that room. Someone who closes the tab and returns two minutes later
  lands on the result of a duel they had finished with, and presses once more to reach the lobby.
  That is the price of the room staying rematchable, and it is the half of this decision a player
  can actually notice.
- **The multi-tab adoption window widens.** `localStorage` is per-origin, so a second tab opened at
  `/` rejoins the same room; `ADR-0018` has the second socket adopt the seat and close the first,
  and `reconnecting.ts` reopens the first, which adopts back. That fight already exists for a live
  duel — `TASK-031001` put the code in `localStorage` — and this extends the window it can happen in
  from the duel to the whole life of the finished room.
- **The provider grows a third member, and an optional one.** `main.tsx` is outside the test net by
  `ADR-0032`'s own admission, so `tsc` is the only thing that reads it, and an *optional* prop means
  a `main.tsx` that omitted `forgetRoom` would compile, run, and silently ship the exact defect this
  ADR exists to remove. That hole is bought deliberately, to keep fourteen unrelated render sites and
  the tickets that touch them out of this change; it is closed by review and by §9's end-to-end
  assertion, and by nothing else. If a second screen ever needs a boot capability, the answer is to
  hand `DuelProvider` the `DuelClient` whole rather than to grow a fourth prop.
- **"Event handlers only" now governs one more function, and still by discipline.** `ADR-0032` §3
  noted that no lint rule enforces it. `forgetRoom` in an effect fails quietly and only for players
  whose socket dropped — the population least likely to file a report.
- **A stale code costs one budgeted failed join per boot** until the first refusal clears it. Cheap
  and bounded, but it is traffic the old rule did not produce.
- **The *waiting for your rival* screen still has no way out, and this does not fix it.** A host who
  creates a room is remembered in it, has no control that leaves it, and reaches the lobby only when
  the room is reaped or by clearing storage — true before this decision and true after, since
  `resume` declines a `WAITING` room and the ordinary join re-seats the same player. `forgetRoom` is
  precisely what such a control would call, which makes the gap cheaper to close without closing it.
  Named rather than fixed: whether that screen offers a way out is a screen the product owns, and it
  belongs to a ticket of its own.

**What it forecloses.** Nothing on the wire and nothing in the store. It does foreclose *the client
deciding when a room has expired*: with no timer anywhere, the only authority on a dead room is
`Failure(UNKNOWN_ROOM)`, and a result screen that counted down would need `ADR-0044`'s deadline
field, which that ADR priced at one more version bump.

**Why this shape, on thin evidence.** It is the cheapest of the working options to reverse: one
branch in one file, an additive member on an interface, and one `onClick`. Nothing is persisted
that was not persisted yesterday — the same eight-character code, under the same key — and the
first refusal drops it, so a revert needs no migration and leaves no data behind. Every alternative
that looked cheaper either changed what a frame means (the reducer, `Welcome`) or added state that
would have to be unwound.

**Deadline.** Nothing outside `STORY-0309` waits on this, and it blocks none of that story's
fourteen tickets. The deadline is the story being **called done**: its fourth acceptance criterion
and `TASK-030913`'s two tests describe a rejoin onto a result screen, and signing that off while the
client cannot rejoin records a green test for an unreachable path — the expensive kind of green, and
the one that gets harder to correct the longer it sits.

## Alternatives considered

**Keep `DuelFinished`'s forget and ship the rematch as live-sockets-only, recorded as a limitation.**
Its strongest case: zero new surface, zero reversal, no discipline for anyone to remember, and
`TASK-031009`'s trap stays closed by a mechanism that needs no control to be pressed. The common
case genuinely works — two players sitting on their result screens with open sockets offer, agree,
and are dealt a new hand — and a limitation honestly written down is a legitimate ship. Rejected on
what the limitation actually is: it is not *"rematch does not survive a reload"* but *"`ADR-0044` §5
is dead code"*, and §5 exists for one case in particular — an offer made while the opponent is
inside their disconnect grace window — which is exactly the case that stays broken. It would also
leave `TASK-030913` asserting the ordering of frames that no client can receive, and a test whose
scenario the product cannot produce is worse than no test, because it reads as coverage.

**Re-remember the room on `RematchOffered` — keep it only while an offer stands.** Its strongest
case is strong: it is the narrowest memory of all the options, kept exactly as long as it is useful;
it needs no provider surface, no screen change and no discipline, because the whole rule stays in
boot's one reaction; and `TASK-031009` itself forecast it — *"a rematch that the wire could carry
would arrive as its own frame and could remember the room again."* Both sockets get the frame, so
the offerer and the receiver re-remember symmetrically. Rejected on two findings. First, it makes
`TASK-031009`'s trap **intermittent** rather than closed: once an offer stands, the code is
remembered again, so the way back reloads straight into the result screen — the lobby becomes
unreachable for exactly the players who engaged with the feature, and an intermittent trap is worse
than a permanent one. Closing that needs the way back to forget, which is this ADR, at which point
the `DuelFinished` branch buys nothing. Second, it half-covers the case it was chosen for: a player
who drops *before* `DuelFinished` never forgets and rejoins fine, but one who drops *after* it —
the common case, both players having seen the result — has already forgotten, and no frame can reach
a socket that is not in the room to tell it otherwise.

**Move the forget onto `Failure(UNKNOWN_ROOM)` unconditionally, dropping the `rejoining` guard.**
Its strongest case: one rule in one place, and the right authority — the room is forgotten exactly
when the server says it is gone, which is the only fact anyone should act on, and no new surface
appears anywhere. Rejected twice over. It re-opens a bug `TASK-031010` closed on purpose and
documented in the code: the same `UNKNOWN_ROOM` answers a player who mistyped a code in the lobby,
and forgetting on it would throw away a room this tab is seated in. And even with the guard kept it
does not answer the question asked, because a *live* finished room refuses nothing: the way back
reloads, `resume` succeeds, and the result screen returns — press again, and again. That is a state
in which the lobby is unreachable, which is the one outcome this decision may not produce.

**A timeout: remember the code for some milliseconds after `DuelFinished`.** Its strongest case: it
bounds the staleness without any new surface and roughly tracks reality, since the room really does
die `finishedMillis` after the finish, and a player returning hours later is not pushed back into a
dead room. Rejected because the wire carries no deadline by `ADR-0044`'s explicit decision, so any
number the client picks is a second authority on a window the server owns — shorter than the
server's and it silently deletes an available rematch, longer and it buys nothing over §6's
refusal, which is exact and needs no clock. It would also put a timer in the boot layer, which is
new state to test and new state to get wrong across a reload, and `ADR-0028`'s standing rule is that
the client renders a deadline and never acts on one.

**Clear at `DuelFinished` and let the server put the player back on a fresh connection.** Its
strongest case: the server is authoritative and already knows which room this session is seated in,
so it could resume on the handshake without being asked, and the client would need no memory at all
for the finished case — arguably no `pd.roomCode` at all, which would delete a whole class of
client state and the staleness question with it. Rejected: it makes `Welcome` mean *"put me back
where I was"*, which is the overload `ADR-0044` already refused for `JoinRoom` on the grounds that
one frame cannot mean two things and the client cannot see which the server picked. It is strictly
worse here than there, because a tab that is auto-seated on the handshake has **no way to say "not
that room"** — there is no request to withhold — so the lobby would be unreachable from a finished
room by any means at all, and a second tab would adopt the seat merely by opening. It is also
Kotlin, which `EPIC-03` may not write.

**The way back carries a marker in the URL that boot reads and forgets on** — `href="/?lobby=1"`,
with boot clearing the memory when it sees it. Its strongest case: the forget stays inside boot, the
provider keeps its two members, no screen touches storage, and it is a one-attribute change plus one
branch. Rejected because the query string is the invite (`ADR-0022`, `roomCodeFromSearch`), and a
client-only flag beside `?room=` is a precedent for a second and a third, in a string players
bookmark and paste to each other. It also moves the forget from the moment the player leaves to the
moment the next boot happens, which is strictly later and no more reliable, and it leaves the client
with no way to forget a room without a full navigation — so the first screen that needs one (see the
waiting-screen gap above) reopens this decision anyway.

**Move the memory to `sessionStorage`.** Its strongest case: it is per-tab, so it fixes the
multi-tab adoption fight named in Consequences by construction, survives a reload — which is the
case this decision is about — and dies with the tab, which makes every staleness question smaller.
Rejected because it is a change to a merged behaviour that nothing here requires: `STORY-0310`
persists across a tab that was closed and reopened, and `sessionStorage` would silently drop that.
It also answers a different question — *where* the memory lives, not *when* it is cleared — so the
`DuelFinished`-versus-the-lobby conflict would survive the move unchanged, and `DEC-032` records
that this client's storage seams are already delicate under Vitest. Recorded because it is the right
change to make if the two-tab fight ever shows up in practice.
