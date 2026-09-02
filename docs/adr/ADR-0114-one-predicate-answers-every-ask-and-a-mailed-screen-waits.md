# ADR-0114 — One predicate answers every ask, and a mailed screen waits for the first frame

- **Status:** Accepted
- **Date:** 2026-09-02
- **Resolves:** `DEC-123` — **the architect's** — by what mechanism is a mid-duel navigation
  refused and the address restored? Registered open 2026-09-02 by
  [`ADR-0112`](ADR-0112-only-a-running-duel-refuses-another-screen.md) §7, in four parts: the
  branch order, the room-state read, restore promptness, and how a mailed token survives unspent.
- **Serves, and reopens nothing in:** `ADR-0112` §§1–5. Everything below is *how*; the *what* is
  that ADR's and is not restated except where a mechanism needs quoting.
- **Applies:** [`ADR-0076`](ADR-0076-a-screen-the-player-chose-has-an-address.md) §3 (the store
  outranks the address) as `ADR-0112` qualified it, §5 (the `replaceState` trap and the two owned
  routing files) and §7 (an address grants nothing);
  [`ADR-0032`](ADR-0032-react-subscribes-to-a-store-it-does-not-own.md) §2 (one boot per tab,
  outside the tree, owning the connection);
  [`ADR-0060`](ADR-0060-the-record-is-its-own-screen-and-the-lobby-is-the-door.md) §4 (a screen
  knows nothing about navigation — none of them change here);
  [`ADR-0081`](ADR-0081-a-mailed-link-is-a-fragment-route-and-the-token-is-the-segment-behind-the-slug.md) §5
  (the mailed token is read once, at mount, and then leaves the address);
  [`ADR-0086`](ADR-0086-the-offers-answer-is-one-key-owned-beside-the-predicate-it-feeds.md) §2
  (the reach for `localStorage` lives in `main.tsx`, never in a component);
  [`ADR-0100`](ADR-0100-the-driver-reaches-an-amount-by-pressing-what-a-player-presses.md) §5 (no test-only prop,
  no `data-testid`: it is driven by what a player presses).
- **Constrains:** `EPIC-13` item 8's story, and every screen story after it — a new screen is now a
  row in `screen.ts` **and** an answer to §5's question about what it sends on arrival.
- **Amends nothing, and moves nothing outside the client.** No wire type, no `PROTOCOL_VERSION`, no
  server file, no schema, no stored key, no new player-facing string, no new control, and no
  `Screen` member for the table (`ADR-0112` §1).

## Context

`ADR-0112` decided what happens; the client that has to do it is one effect and a branch order,
and neither can express the answer as it stands.

**The shipped guard cannot tell an ask from an arrival, and does not try.** `Lobby.tsx` holds

```tsx
const seatedByAFrame =
  state.outcome !== null || state.view !== null || state.roomCode !== null;
useEffect(() => {
  if (seatedByAFrame && screen !== "first") leave();
}, [seatedByAFrame, screen, leave]);
```

Its comment cites `ADR-0076` §3, which is about a **frame arriving** over a chosen screen. The
predicate fires identically when a **player asks** over a held room, and it does not read the
room's state at all — `roomCode !== null` is enough. That is the shipped generalization `ADR-0112`
§2–§3 split in half, and the half that must now behave differently is not distinguishable by
anything in that expression.

**The branch order below it is the other half of the same problem.** `Lobby.tsx` tests `outcome`,
then `view`, then `roomCode`, then the chosen screens. A chosen screen therefore cannot render
while any of the three stand, which is exactly what `ADR-0112` §3 now requires for two of them.
`ADR-0076`'s own Consequences named this in advance: *"Two navigation authorities, held apart by
prose and a branch order… nothing mechanical enforces it,"* and `ADR-0112` recorded that the count
has grown to three, the room's state being the new one. Re-fixing the order is the visible half of
`DEC-123`; making the rule mechanical rather than prose is the half worth having.

**The room's state is not on the wire, and cannot be put there here.** `RoomState` —
`WAITING`, `PLAYING`, `FINISHED`, `ABANDONED` — is a server-side enum in `Room.kt`; no frame
carries it and `ADR-0112` §7 forbids moving the wire on its account. What the client has is what
the frames set: `roomCode` from `RoomJoined`, `view` from `Snapshot`, `outcome` from
`DuelFinished`, and the reducer's own rules — a `Snapshot` clears `outcome` (`ADR-0044` §4), a
`DuelFinished` leaves `view` standing. That ladder is a faithful reading of `PLAYING` while frames
have arrived. **Before they have arrived it reads nothing at all**, and that is where the fourth
part of `DEC-123` bites.

**A mailed screen spends its secret by existing.** `VerifyScreen` submits its token in a mount
effect — deliberately, `ADR-0081`'s model is that the mailed link *is* the action. A tab that boots
holding a room boots with an empty store, so for the whole rejoin round trip the ladder reads *no
room*: the verify branch renders and the token is spent hundreds of milliseconds before the frames
that would have refused it arrive. This is today's behaviour on that path and it is what
`ADR-0112` §5 forbids in the words *"must not spend its token."* Any mechanism that reads only the
store answers three of `DEC-123`'s four parts and fails the fourth on every attempt.

**Promptness is a mechanism question that the measurement cannot answer.** The recorded defect —
*the fragment reads back empty within 2 s* — is a driver poll interval, not a latency;
it cannot tell 1 ms from 1900 ms, and `ADR-0112` §7 says so. What has to be settled is what
**guarantees** that `ADR-0112` §2's *"one act, not two"* is one act, and the only guarantee
available inside a React tree is which effect phase the history write happens in.

**The deadline.** `EPIC-13` item 8 cannot be split without this. And the same timing argument
`ADR-0076` made about itself holds again: the next screen copies the pattern the first ones share,
so the resolution point is cheap to introduce now and is a rewrite of every screen branch later.

## Decision

### 1. One pure module reads the room and rules on the ask

New file `web-client/src/routing/room-standing.ts`, in the `screen.ts` tradition — no `window`, no
React, type-only imports — holding both halves of the rule, because they are one rule and a
reader who has to open two files to know what the client does is back to prose:

```ts
export type RoomStanding = "unknown" | "none" | "waiting" | "running" | "finished";

export function roomStanding(state: DuelState, roomAwaited: boolean): RoomStanding {
  if (state.outcome !== null) return "finished";
  if (state.view !== null) return "running";
  if (state.roomCode !== null) return "waiting";
  return roomAwaited && state.refusal === null ? "unknown" : "none";
}

export type Ruling = "honour" | "refuse" | "hold";

export function rulingOn(asked: Screen, standing: RoomStanding): Ruling {
  if (asked === "first") return "honour";
  if (standing === "running") return "refuse";
  if (standing === "unknown") return spendsOnArrival(asked) ? "hold" : "honour";
  return "honour";
}
```

**Every term of `roomStanding` is a fact the server sent.** `outcome`, `view`, `roomCode` and
`refusal` are set by `DuelFinished`, `Snapshot`, `RoomJoined` and `Failure` and by nothing else;
the client asserts no game fact, it reads back the last thing it was told. The order is not
arbitrary and is not a new invention: it is `Lobby.tsx`'s existing branch order, which is the order
because the reducer clears nothing a frame established — `view` and `roomCode` both outlive the
duel, so `finished` must be tested before `running` or it never fires. **`running` is
`view !== null && outcome === null`**, which is `ADR-0105` §2's *"running means `PLAYING`"* read
off frames: the grace window is inside it (a seat inside the window is a seat in a `PLAYING` room
and no frame ends the duel), and a runout is inside it too — `ADR-0102`'s paint holds the queued
`DuelFinished` behind its steps, so the standing stays `running` until the last step has stood.

**`ADR-0110` does not disturb this.** It moves the host to the table and keeps the client's waiting
state exactly where it is — its own constraint line names *"the `state.roomCode !== null &&
state.view === null` branch"* — so `waiting` is a standing, not a screen, which is the phrasing
`ADR-0112` §3 asked for.

**`unknown` is the state of knowledge before the first frame, and `roomAwaited` is what makes it
distinguishable from `none`** (§5).

### 2. The branch order: the chosen screens are asked first, and asked about the ruling

`Lobby.tsx` computes, once, above every branch:

```tsx
const standing = roomStanding(state, roomAwaited);
const ruling = rulingOn(screen, standing);
const shown = ruling === "honour" ? screen : "first";
```

and the six chosen-screen branches **move above** the three store branches and test `shown`,
never `screen`:

| Order | Branch | Condition |
| --- | --- | --- |
| 1 | the record, the ladder, the account, sign-in, verify, reset | `shown === <slug>`, each keeping the read/provider fallback it already has |
| 2 | the result, the table, the room's own screen | `outcome`, then `view`, then `roomCode` — untouched |
| 3 | the first screen | the existing fall-through |

**`shown` is `"first"` for every ruling but `honour`, and `"first"` is the store's screen** — it
always was; that is why `ADR-0076` §1 gives the first screen the address `/` and lets the three
store branches sit under it. So a refusal and a hold both render the room's own screen with no
branch of their own, and `ADR-0076` §3's frame rule needs no separate machinery: a frame that
starts a duel moves `standing` to `running`, which moves `ruling` to `refuse`, which is the same
line that refuses a player's ask. **There are not two rules. There is one predicate, re-evaluated
on every render, and the two cases differ only in which input changed** — the address, or a frame.

The order is what makes it mechanical: a chosen screen cannot render without consulting
`rulingOn`, because the only value its branch can test is the one `rulingOn` produced. That is the
enforcement `ADR-0076`'s Consequences said did not exist.

The fall-throughs keep working and now mean more: `shown === "duels"` with no history read
available falls past its branch into the store ladder, so a player whose read is unavailable lands
on the room they hold rather than on a lobby that pretends they hold nothing.

### 3. The restore is a layout effect, and that is what makes it one act

```tsx
useLayoutEffect(() => {
  if (ruling === "refuse") leave();
}, [ruling]);
```

**`useLayoutEffect`, not `useEffect`, and this is the whole of `DEC-123`'s promptness half.** A
layout effect runs synchronously after the commit that refused the ask and **before the browser
paints it**; a passive effect is scheduled and may be flushed a task later. `ADR-0112` §2's
*"these are one act, not two"* is exactly the statement that no paint separates them, and the
effect phase is the only thing in a React tree that guarantees it. Nothing here is measured in
milliseconds and no number is asserted: *the same commit* is a stronger claim than any number a
2-second poll could produce.

Four details it depends on, each already merged and each silent if got wrong:

- **`leave()` and nothing else.** `history.replaceState` fires neither `popstate` nor `hashchange`
  (`ADR-0076` §5), so `use-screen.ts` notifies its own subscribers after writing; a raw
  `replaceState` here would restore the address and leave every `useScreen()` caller rendering the
  stale screen. `use-screen.ts` and `screen.ts` are **not modified by this ADR**.
- **The dependency array is `[ruling]`.** `useScreen()` memoises none of its returned functions, so
  including `leave` would run this effect on every render; it is omitted with the same
  `eslint-disable` and the same reason the merged token effect beside it already gives. `ruling` is
  a string and compares by value, so the effect runs when the ruling changes and not otherwise —
  and it settles in one pass, because `leave()` moves `screen` to `"first"` and `rulingOn` answers
  `honour` for `"first"`.
- **`asked === "first"` short-circuits before `running` is tested** (§1), so a player already at
  `/` never has `replaceState` called on every arriving frame. That guard exists in the shipped
  effect and is kept.
- **The merged token effect beside it is re-keyed to `shown`.** `ADR-0081` §5's effect calls
  `clearToken()`, which replaces the address with `hashForScreen(screen)` — the screen it was
  given. Left keyed on `screen`, it would fire on the very commit that refused a mailed ask and
  write `#/verify` back over the `/` this effect had just restored: two writers of one address,
  with the wrong one last. Keyed on `shown` it cannot fire unless the ask was honoured, so the two
  effects are disjoint by construction instead of by declaration order, and the merged comment's
  *"declared, and so run, before the effect below"* stops being load-bearing.

### 4. What the restore writes, and what it takes with it

`leave()` replaces the current history entry with `hashForScreen("first")`, which is `/` — so
`location.hash` reads back empty, which is what the recorded measurement saw and is the behaviour
being kept, not the behaviour being replaced.

- **Replace, never push, and never `history.back()`.** `back()` is the tempting version — it would
  leave no dead entry — and it is wrong: an address that was *typed* into a fresh tab has no
  previous entry in this document, so `back()` would leave the client entirely, from a mid-duel
  ask, silently. `replaceState` cannot do that from any starting point.
- **The fragment goes whole, its token with it** (§5), which is `ADR-0081` §4's preference anyway:
  a mailed secret should not linger in an address bar.
- **A query string is not preserved**, because `/` replaces the whole address. This is the merged
  behaviour of `leave()` and is harmless: `?room=` is an instruction consumed once at boot
  (`ADR-0076` §4), and it is the tab's memory, never the address, that puts a player back
  (`ADR-0072`).

### 5. A screen that spends a secret on arrival waits for the first frame; every other screen renders at once

**The problem, stated exactly.** The store is empty until frames arrive, so at boot
`roomStanding` would answer `none` for a tab that holds a running duel — for the whole socket
handshake plus rejoin round trip. Rendering a chosen screen in that window is harmless for four of
the six, because rendering them sends nothing that cannot be sent again. It is not harmless for
`verify` and `reset`: `VerifyScreen` submits its token in a mount effect, so *rendering it is
spending it*, and `ADR-0112` §5 forbids that outright.

**`roomAwaited`** is the fact that closes the window, and it is available at boot: *this tab is
asking the server about a room*. `bootDuelClient` already decides it — `options.joinRoomCode ??
readRoomCode(storage)` — so it computes it once at construction and returns it on `DuelClient`;
`DuelProvider` carries it beside `store`, `send` and `forgetRoom`; a `useRoomAwaited()` hook reads
it, the way `useSignedIn()` reads its own boot-time boolean. The reach for `localStorage` stays in
`boot.ts` and `main.tsx` (`ADR-0086` §2), the key literal stays in `room-memory.ts` (the merged
one-module-owns-each-key gate), and nothing new is stored.

**It is used only to withhold, never to assert.** A remembered room code is a code the server
minted and sent in a `RoomJoined`; the client does not claim to be seated on the strength of it and
shows nothing because of it. It says only *do not act yet, the server has not answered* — which
`CLAUDE.md`'s third non-negotiable permits and, on this path, requires.

**So the ruling has three values, not two.** `hold` renders the room's own screen **and writes
nothing to the address** — the token stays in the fragment, unspent and unread, until the frames
say which of the other two rulings applies. Restoring during a hold would throw the token away
before knowing it was allowed to be used; refusing during a hold would refuse a mailed link that a
`FINISHED` room is entitled to open (`ADR-0112` §§3, 5). Only `refuse` restores the address, and
that is what makes `ADR-0112` §2's one act one act.

**`unknown` ends on the first frame that answers the ask** — `RoomJoined` (→ `waiting`, or the
frames behind it), or `Failure` (→ `none`; `boot.ts` forgets a room the server answered
`UNKNOWN_ROOM` for, and `refusal` in the predicate is what keeps a reaped room from holding a
mailed link forever). Under a server that never answers, a mailed screen keeps waiting — which is
the right outcome and not a degradation: `VerifyScreen` reached with an unreachable server prints
*that link is dead* about a token that is perfectly alive.

**The other four chosen screens do not wait, and the reason is not only that they are harmless.**
Rendering them immediately keeps the address and the screen in agreement at every instant,
including during the unknown window; holding them would show the first screen while the address
named `#/account`, which is the disagreement `ADR-0112` §2 exists to prevent. It is also what
makes `ADR-0086` §6's accept land: `<a href="/#/account">` is a page load into a tab that
remembers a `FINISHED` room, and the account screen shows immediately rather than after a round
trip. The hold is accepted for `verify` and `reset` alone, because there the alternative is
spending a single-use secret.

**`spendsOnArrival` is an exported, tested predicate, not a literal inside a condition**, so that
the question it asks — *does mounting this screen send something the server cannot be asked
twice?* — is one a future screen has to answer rather than one it can walk past.

### 6. What this does not close, and what would

**One frame's width remains, and it cannot be closed from the client.** A resume into a `PLAYING`
room is `RoomJoined`, then `Snapshot`, sent back to back but delivered as two socket messages and
therefore applied in two renders. In the render between them the standing reads `waiting`, which is
indistinguishable from a room that really is waiting — the client is told *you hold room ABCD* and
is not told what it is doing until the next frame. A mailed link that arrives in exactly that
window is honoured and its token spent.

This is not a defect of the shape chosen; it is the shape of an asynchronous authoritative server,
and **every** client-side gate has it. What the mechanism buys is the size: the exposure falls from
the whole rejoin round trip, where it fires every time, to the gap between two frames of one
delivery, where it essentially never does. Closing it exactly requires the server to name the
room's state in the frame that answers the join — a wire change `ADR-0112` §7 forbids on its
account. **If the drive §7 below owes observes a token spent this way, that observation is the
evidence for a new `DEC`, and it is not smuggled in here on a guess.**

**One inherited claim this ADR does not rest on.** `ADR-0112`'s Context says `ADR-0086` §6's accept
anchor currently bounces off the result screen, and flags it *derived, not driven*. Nothing above
depends on it: §2's order and §5's ruling produce the account screen from a `FINISHED` room whether
or not today's tree fails to. What a drive would settle is only which kind of test the implementing
ticket writes — a regression test for a defect, or a proof of new behaviour — and the story owes
the drive either way.

### 7. What the implementing story must prove

`ADR-0100` §5 holds in full: no `data-testid`, no test-only prop, no exported setter. Everything
below is reachable by pressing what a player presses, assigning `window.location.hash` (what a
player typing in the address bar does, and what the recorded measurement already did), or booting
at an address with a room in the tab's memory.

- **`roomStanding` and `rulingOn`, as pure functions**, over states built from `initialState()`:
  each of the five standings from the fields that produce it, and each of the three rulings. Two
  cases that are not tautologies — `finished` while `view` still stands (the reducer leaves it
  there), and `running` while a reveal is mid-paint.
- **Refused:** a tree fed `RoomJoined` + `Snapshot`, then `location.hash = "#/leaderboard"` — the
  table still renders and `location.hash` reads `""`. The same again for `#/account` and for a
  mailed address.
- **Honoured:** the same tree fed `RoomJoined` + `Snapshot` + `DuelFinished`, then the same
  assignment — the ladder renders, `location.hash` still reads `"#/leaderboard"`, and the store's
  `roomCode` is unchanged. And from `RoomJoined` alone, for the waiting standing.
- **Overruled:** honoured first, then a `Snapshot` arrives — the table renders and the address is
  restored, with no other input (`ADR-0112` §4 through the same predicate).
- **Not spent:** a tree booted at `#/verify/<token>` with a room in the tab's memory and a counting
  `verifyEmail` in `accountCalls` — **zero** calls before any frame arrives, and the address still
  holds the token. Then `RoomJoined` + `Snapshot`: still zero, and `location.hash` reads `""`.
  Then the same boot answered with `RoomJoined` + `DuelFinished` instead: exactly **one** call.
  A count, never an assertion about the argument of the last call — zero and one are the whole
  requirement, and only a count tells them apart. The drive sets the tab's memory, not the
  provider prop.
- **`EPIC-13`'s Definition of done** still binds the story to `ADR-0112` §6's six undriven paths;
  this ADR adds none and removes none.

## Consequences

**What it buys.** The rule stops being prose. `ADR-0076` §3's *"the store outranks the address"*
and `ADR-0112`'s qualification of it are one function with one input from each authority, and the
branch order is downstream of it rather than beside it — a chosen screen literally cannot render
without asking. The frame rule and the refused ask stop being two mechanisms that must be kept
consistent. The address bar tells the truth at every instant, not only at rest. And the one path
where `ADR-0112` set a hard requirement — the mailed token — is closed against the failure that
actually fires today, with the residual named and sized rather than assumed away.

**What it costs.**

- **A third state of knowledge, and it is real.** `unknown` exists because the client's picture of
  the room is a function of frames and there are none yet. It leaks into the ruling as `hold`, so
  the client now has a navigation outcome that is neither yes nor no, and every future screen must
  be classified against `spendsOnArrival` or it will be classified by default. And a hold is
  itself a small lie: for one round trip the address names `#/verify` while the first screen is on
  show — the disagreement `ADR-0112` §2 exists to prevent, accepted here for two screens and one
  round trip because the alternative spends a secret.
- **A boot-time boolean threaded through three files.** `roomAwaited` crosses `boot.ts`,
  `duel-provider.tsx` and `main.tsx` to be read in `Lobby.tsx`, and it is a snapshot: a tab that
  forgets its room mid-document keeps a stale `true`, harmless only because `refusal` and the
  standing dominate the predicate wherever it would matter. That is a correctness argument held in
  a comment, which is the class of thing this ADR is otherwise trying to reduce.
- **A look-away unmounts the room's screen.** Anything held in those components rather than in the
  store is lost and rebuilt on return — concretely `RematchControl`'s local *it begins* mark
  (`TASK-121102`): a player who offers a rematch, looks at the ladder and comes back sees the
  standing offer from the store but not that frame. `ADR-0112` named the missed *arrival* of an
  offer; this names the loss of local state, which is new and is the mechanism's own.
- **A refused ask leaves a dead history entry.** The ask pushed one (assigning `location.hash`
  does), and the restore replaces its contents rather than removing it, so the browser's *Back*
  does nothing once per refusal. `ADR-0076` already named the collision between the two Backs;
  this makes it one press deeper each time a player types an address mid-duel.
- **`useLayoutEffect` is a sharper tool than the tree otherwise uses**, and its guarantee is
  invisible: swapping it for `useEffect` breaks nothing any test would notice by name, and the
  behaviour it protects — no paint between refusing and restoring — is not something a DOM
  assertion can see. The reason lives in a comment and in this ADR.
- **The verify path keeps its shape only because nothing else spends on arrival.** The day a
  screen sends a non-idempotent request on mount for a reason other than a mailed token, the
  predicate's name will be a lie or the list will grow silently.

**What it forecloses.** Nothing permanently. It deliberately does not build a router: `App.tsx`
still renders `<Lobby />` and the screen branches stay where they are, so the day a fourth
authority appears the refactor is still available and is still the same size it is today. It also
leaves `ADR-0112`'s own reversal cheap in the way that ADR promised: when `DEC-120`'s clock bounds
every duel and mid-duel navigation becomes arguable on evidence, honouring it is the deletion of
one line of `rulingOn` — `if (standing === "running") return "refuse";` — and nothing else in the
client moves.

**On reversibility, which is why it went this way.** One new pure file, one reordered branch list,
one effect changed in phase and predicate, and a boolean carried from boot. No wire, no storage, no
server, no string, no control, nothing a player keeps. On evidence this thin — one reproduced
defect and one derived one — the cheapest thing to unwind is the right thing to build, which is
the argument `ADR-0076` made for fragment routing in the first place and the reason this is a
resolution point rather than a router.

## Alternatives considered

**Keep the guard where it is and make its predicate cleverer** — `if (roomIsRunning && screen !==
"first") leave()`, with the branch order untouched below it. The strongest case in the set: the
smallest possible diff, no new file, no new concept, and the effect keeps doing what it already
does in the place a reader already looks. Rejected because it answers only half the question: with
the store branches still first, a chosen screen still cannot render over a `WAITING` or `FINISHED`
room, so `ADR-0112` §3 — the half the ADR actually changes — does not ship. Reordering the branches
is unavoidable; and once they are reordered, the condition each of them tests has to come from
somewhere, which is the resolution point this ADR builds.

**Put the guard in `use-screen.ts`, refusing inside the `hashchange` listener, before React
renders.** Its case is promptness, argued at its strongest: nothing is more prompt than
synchronous, and the restore would happen in the same task as the ask with no commit in between.
Rejected on two counts. It buys nothing measurable — a hash assignment cannot be cancelled, so the
address holds the asked value for an instant either way, and a layout effect already lands the
restore before the paint that shows the refusal. And it costs the routing module its ignorance of
the store: `use-screen.ts` would need the room's standing, which lives behind the provider, so the
one file `ADR-0076` §5 kept free of everything but the address would gain a dependency on the
connection. Two subscription surfaces that know about each other is worse than one function that
takes both as arguments.

**Read the room's state from the wire — a `state` field on `RoomJoined`.** Its case is the
strongest correctness argument available: it closes §6's residual window exactly, it removes the
inference in `roomStanding` entirely, and it would let a mailed screen decide on the first frame
with no `unknown` and no `hold`. Rejected because `ADR-0112` §7 forbids moving the wire on its
account, and rightly: a `PROTOCOL_VERSION` step under `ADR-0047`'s lock, for a client rendering
decision, on the strength of a race nobody has yet observed, is a large irreversible payment for a
small reversible problem. Left as the named closure if the drive ever produces the evidence.

**Gate the mailed screens on the tab's memory alone — a tab that boots remembering a room never
opens a mailed link.** Its case: no `unknown` state, no `hold` ruling, no third navigation outcome,
and the token is safe by construction in every window including §6's. Rejected because it refuses
what `ADR-0112` §§3 and 5 grant: a `FINISHED` or `WAITING` room honours a mailed link like every
other ask, and a browser that has finished a duel and not yet returned to the lobby would find its
verification mail permanently inert. Narrowing a merged product rule for a mechanism's convenience
is the one move an architecture decision may not make.

**Hold every chosen screen until the standing is known, not just the mailed two.** Its case is
uniformity, which is worth a lot here: one rule, no `spendsOnArrival` list, no classification for a
future screen to get wrong, and no possibility that a screen quietly acquires a mount-time request
and nobody notices. Rejected because it makes the common case worse for no gain — every boot into
`#/leaderboard` on a browser holding a room shows the lobby for a round trip first — and because it
introduces the very disagreement `ADR-0112` §2 forbids: the address naming a screen the player is
not being shown, for as long as the network takes. Uniformity that has to show a wrong screen to
achieve itself is not uniformity worth having.

**Move the screen branches out of `Lobby.tsx` into a `<Router>` above it.** Its case is the honest
architectural one: the resolution point *is* a router, `Lobby.tsx` is over 500 lines and holds nine
branches, and a component named for the first screen deciding which of seven screens shows is the
structure that produced this defect. Rejected on cost and reversibility only — it touches every
screen's tests to change no behaviour, and `ADR-0112` bought a client-rendering change with nothing
underneath it. The resolution point introduced here is what a router would be built from, so the
refactor gets cheaper rather than harder by waiting, and the day a fourth authority appears it is
the obvious next ADR.
