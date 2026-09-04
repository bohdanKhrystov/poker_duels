# ADR-0118 — A recovering browser shows nothing it was not told, and a late screen is not a false one

- **Status:** Accepted
- **Date:** 2026-09-04
- **Resolves:** `DEC-127` — **the product owner's** — a browser recovering a room paints a confident
  *Waiting for your rival* screen, room code and invite link and all, over a room that is actually
  `PLAYING` or already finished, and then corrects itself. Is a self-correcting false screen
  **acceptable-because-late**, or **wrong-because-false** and owed a neutral interstitial?
  Registered 2026-09-04 by `TASK-131009` reading
  [`STORY-1310`](../../tasks/stories/STORY-1310-the-refresh-paths-nobody-drove.md) whole, on three
  independent observations by three routes — `P2`, `P4`, `P6b`.
- **Where the answer came from:** derived, and the two halves are licensed by different sentences.
  **The removal** rests on `docs/vision.md`'s success condition, the one everything else is
  downstream of — ***"Send a link. She opens it in a browser. We play a full heads-up match. Someone
  wins. We hit Rematch."*** A screen that offers a player in the middle of that match two controls
  that take them out of it is against that sentence whatever the screen is called, and the front
  door offered during a recovery is exactly that screen (Context). **The refusal of an interstitial**
  rests on *Positioning* — ***"Dark, quiet, fast, minimal."*** — applied through
  [`ADR-0105`](ADR-0105-one-duel-at-a-time-and-the-refusal-hands-back-the-duel.md) §4's merged
  discipline, quoted by [`ADR-0112`](ADR-0112-only-a-running-duel-refuses-another-screen.md) §2: *a
  screen that needs a new sentence for this has outgrown this decision and owes a new ADR, not an
  invented line.*
- **Applies, and reopens none of:**
  [`ADR-0114`](ADR-0114-one-predicate-answers-every-ask-and-a-mailed-screen-waits.md) §5 —
  `roomAwaited` and the `unknown` standing it makes distinguishable; this ADR **spends** that
  distinction and does not redefine it — and §§1, 3, 4 (the predicate, the layout-effect restore,
  what the restore writes); `ADR-0112` §§1–5; `ADR-0102` §5 (a reload lands on the true state, late);
  [`ADR-0110`](ADR-0110-creating-a-duel-seats-the-host-at-the-table.md) §§2–5 (the waiting screen's
  own composition is untouched);
  [`ADR-0073`](ADR-0073-the-waiting-screen-says-back-to-the-lobby-and-the-room-stays-open.md) §3 (the
  promise sentence and *Back to the lobby* are not redrawn);
  [`ADR-0091`](ADR-0091-design-gets-no-agent-a-new-screen-owes-a-card.md) §2 — no `Screen` member is
  added and **no element is drawn**, so no card is owed.
- **Qualifies** `ADR-0114` §2 by one condition, in the one row that ADR left as *"the existing
  fall-through"*: the first screen is reached on the `none` standing and **not** on `unknown`. Rows 1
  and 2 of that table — the chosen screens above, the result/table/room ladder below — are untouched,
  and so is `rulingOn`.
- **Constrains:** `STORY-1311`'s split, and nothing else. No wire change, no `PROTOCOL_VERSION` move,
  no server file, no schema, no stored key, **no new player-facing string**, no new control, no new
  `Screen` member.
- **Names, and does not register** (`ADR-0105` §6's route, the one `EPIC-13` follows — *a `DEC`
  nobody is working is noise in the open table*): what a player is shown when the client cannot reach
  the server at all, and when a recovery is long enough that silence reads as breakage (§5).

## Context

**Three drives, three routes, one sequence.** `STORY-1310` drove `ADR-0112` §6's six paths and hit
the same shape three independent times: `P2`, reloading mid-runout, painted the lobby, then the lobby
with the profile strip loaded, then *Waiting for your rival* naming a room whose duel had **already
finished**, then the true `Victory`. `P4`, reloading a `PLAYING` duel at `delayed 300ms`, painted a
bare lobby shell, then the full waiting-room screen — invite link, room code, *Back to the lobby* —
over a live duel. `P6b` reached the same stale waiting screen over a genuinely `PLAYING` room
**through a mailed link, with no reload at all**. Three routes agreeing is what took this from a
quirk to a decision.

**It is two windows, not one, and they are different in kind.** Read against `ADR-0114`'s own
vocabulary and against the source:

- **Before any frame** — `roomStanding` answers `unknown`: `outcome`, `view` and `roomCode` are all
  null and `roomAwaited` is true. `ADR-0114` §2's branch order falls past the three store branches to
  *"the existing fall-through"*, which is the front door. **The server has stated nothing, and the
  client paints a screen anyway.**
- **After `RoomJoined`, before the frame that says what the room is doing** — `roomStanding` answers
  `waiting`, and `Lobby.tsx` renders `WaitingTable`. **The server did state this**: `RoomJoined` says
  *you hold room ABCD, seat N*, and the waiting screen is the only room screen that statement
  supports. `ADR-0114` §6 names this window and its width in advance.

**The front door is not claimless, and that is the fact this decision turns on.** It offers *Create a
duel room* and *Join the duel*, and the server does not refuse either from a player who already holds
a seat: `replyToCreateRoom` in `DuelSocket.kt` calls `rooms.create`, repoints this connection's
`RoomMembership.code` to the new room and answers `RoomJoined` — **with no seat check anywhere in
it**. `DEC-110`, the architect's, is open precisely because that guard does not exist yet. `boot.ts`
writes the code from every `RoomJoined` to storage, so a press inside that window overwrites the
remembered room. A recovering player's duel is still theirs on the server, and their tab no longer
knows its code; from there `ADR-0113`'s sweep plays the seat they left. The lobby is not a harmless
flash. It is an armed one, on every recovery, for every player.

**The waiting screen's window cannot be closed from the client, and the source says why.**
`replyToJoinRoom` has two shapes. A **resumed** seat gets `RoomJoined` and then
`deliver(resumed.outbound, …)` — more frames, immediately, as separate socket writes; that is the gap
the drives caught. A host rejoining a **still-`WAITING`** room falls past `resume` into `join`, lands
in the `ALREADY_SEATED` branch, and is sent `RoomJoined` **and nothing else, ever**. So *wait for a
second frame before drawing the waiting screen* would hang a genuinely waiting host on an empty page
for as long as they wait for their rival — a path `STORY-1310` drove and found working. `ADR-0114`
§6's conclusion is confirmed by reading the server: closing this window exactly requires the server
to name the room's state in the frame that answers the join, which is a wire change and the
architect's.

**The instrument inflates the durations and does not invent the sequence.**
[`ADR-0117`](ADR-0117-the-proofs-of-record-load-the-built-bundle.md), merged the same day, computes
the dev server's module waterfall at roughly ten seconds for `web-client/src`'s 102 modules over six
sockets — about ten of `P2`'s 11.8 s first paint and most of `P4`'s ~13.5 s. Under `dist/` the same
sequence is bundle, socket handshake, rejoin round trip. **The window shrinks; it does not vanish** —
`P6b` reached the stale screen with no page load in it at all, and `P4` recorded that the window
grows with latency rather than with module count.

**The tension.** `ADR-0102` §5's standard — a reload lands on the true state — holds on all three
routes; nothing wrong is ever asserted, only late, which is why this reached the product owner and
not the architect. `ADR-0112` §6's *no lobby on the way* fails on all three. An interstitial would
make the sequence honest and would be a **new surface** in a product whose Positioning sentence is
four words long and whose merged discipline treats a new sentence as evidence that a decision has
outgrown itself. And silence has its own bill: the lobby is the only frame in the observed sequence
that keeps the page visibly alive — `P2`'s second frame is the profile strip finishing its own HTTP
read.

**The deadline.** `STORY-1311` is split next, and it rewrites `Lobby.tsx`'s branch order once
(`ADR-0114` §2). Deciding what the fall-through shows is free while that order is being rewritten and
is a second story afterwards. The `DEC-127` row says so: *before `STORY-1311` draws its held state*.

## Decision

### 1. The rule: the client shows what it was told, and before it is told it shows nothing

A screen the server **stated**, arriving late, is acceptable. A screen the server stated **nothing**
to support is not, and the client does not paint one to fill a wait.

That is one sentence and it decides both windows, in opposite directions. It is not a new principle:
it is `CLAUDE.md`'s *the server is authoritative* read as a rule about **rendering** rather than
about asserting, and it is `ADR-0114` §5's own reason for `roomAwaited` — *"it is used only to
withhold, never to assert"* — applied past the two mailed screens to the screen a recovering player
actually lands on.

### 2. While a held room is unknown, `/` renders nothing

**Who this is about:** a browser that is asking the server about a room — a remembered code or a
`?room=CODE` — and has not been answered. `ADR-0114` §5's `unknown` standing, exactly. A browser
holding no room is unaffected and shows the front door at once, as it does today.

**What it shows:** no element. `App.tsx`'s shell stands — dark, full height — and is empty. **No
wordmark, no *Create a duel room*, no *Join the duel*, no profile strip, no doors, no room code, no
invite panel, no spinner, no mark, no sentence.** Nothing is added to draw this; something is
withheld.

**What ends it** is `ADR-0114` §5's sentence, unchanged: the first frame that answers the ask.
`RoomJoined` — and the room's own screen paints. `Failure` — and the front door paints **with the
refusal it carries**, so a reaped room lands on *"No duel room has that code."* rather than on
silence.

**What it does not touch:** the chosen screens. `rulingOn` already honours the record, the ladder,
the account and sign-in on `unknown` and holds `verify` and `reset`; a player who asked for one gets
what `ADR-0114` §5 gives them. This decision is about `/` alone.

In `ADR-0114` §2's table this is the third row's condition — `standing === "none"` rather than the
bare fall-through. That reading is **offered, not designed**: how the branch is written is the
implementing ticket's, in the tradition `ADR-0112` §7 used.

### 3. The waiting screen over a resumed room is acceptable-because-late, and that is said out loud

Once `RoomJoined` has landed, the waiting screen may stand until the next frame arrives — invite
panel, promise sentence and all — **including when the room is really `PLAYING` or already
finished**. The `DEC-127` row says *acceptable* is a complete answer and needs saying rather than
falling out, so it is said here: **this is accepted product behaviour, not an unrepaired defect, and
no ticket repairs it.**

Three reasons, in order of weight:

1. **It is a state the server stated.** `RoomJoined` is the client's whole knowledge at that instant,
   and the waiting screen is the only room screen it supports. The client invents nothing —
   `ADR-0102` §5's standard, met.
2. **The client cannot do better, and the source says so.** A rejoin into a `WAITING` room is
   answered with `RoomJoined` and nothing after it (Context), so any rule that waits for a second
   frame hangs the host who is genuinely waiting. Closing this window needs the server to name the
   room's state — a wire change, the architect's, and not bought here.
3. **The exposure is a gap between two frames of one delivery**, not a screen with a duration of its
   own, and it shrinks with the artifact (`ADR-0117`) rather than growing with it.

**Its cost is named and accepted, not hidden.** *Back to the lobby* on that screen calls
`forgetRoom`, and over a running duel that is destructive: the tab forgets the code, the page load
drops the socket, and `ADR-0113`'s sweep plays the seat from there. Accepting §3 is accepting that
hazard for the width of one frame gap. §4 names what would make it worth paying to close.

### 4. No interstitial today, and what would reopen that

**Nothing new is drawn, and no string is minted.** Not *Restoring your duel*, not *Reconnecting*, not
a spinner, not a progress mark. `ADR-0105` §4's discipline is the operative rule and it is merged: a
screen that needs a new sentence for this has outgrown its decision and owes a new ADR. This one does
not need a sentence — §2 is a withholding, not a surface.

**This answer depends on the window being short, and the dependency is stated so it can be checked.**
Under `dist/` the prediction is bundle, socket handshake, one rejoin round trip — a few hundred
milliseconds on a connection a player would actually use, at which length nobody perceives an
interruption and a mark that appears and vanishes inside it is noise. The measurement that tests the
prediction is **already owed by a merged ADR**: `ADR-0117` §6 requires a `dev` finding whose
conclusion turns on load latency to be re-read on `built` before it is filed, and names `P2`'s and
`P5`'s `delayed` findings against `ADR-0112` §6 as a separate ticket. No new obligation is created
here and none may be read into a `verify:` block (`ADR-0089` §2b).

**The reopening condition, stated as a number so it is checkable rather than a matter of taste:** if
that built-bundle reading shows `/` empty for **longer than about a second** on a normal connection,
the interstitial question is live again and is a new ADR — a second's wait is where a person stops
assuming a machine is working. Below that, silence is the answer. This ADR is one branch condition;
reversing it costs one branch condition and a card.

### 5. What this does not close

- **What a player sees when the client cannot reach the server at all.** §2's silence ends on
  `RoomJoined` or on a `Failure`; a server that answers neither leaves the page empty indefinitely.
  Falling back to the front door there is not obviously better — its controls cannot work either, so
  it is a second false screen rather than a rescue — and there is no evidence to choose on. Named
  here, deliberately not registered, and not blocking `STORY-1311`.
- **The one-frame gap itself** (§3), whose exact closure is a wire change and the architect's if the
  evidence ever justifies it.
- **`DEC-110`'s missing guard.** That the flashed front door can create a second room is evidence
  *for* §2 and is cited as a force; this ADR removes the **screen**, not the server's absent seat
  check. The guard stays `DEC-110`'s.
- **`P1`'s unexplained reading.** `STORY-1310` narrowed but did not settle why one drive saw *"No
  duel room has that code."* survive a reload. §2 makes that sentence the correct end of a silence on
  a reaped room; whether it explains that drive is not claimed here.

### 6. What `STORY-1311`'s tickets must draw

Component tests, no browser, no `data-testid` and no test-only prop (`ADR-0100` §5, `ADR-0089` §2b):

- **The silence.** A tree booted with a room in the tab's memory and **no frames applied** renders
  none of *Create a duel room*, *Join the duel*, the wordmark, the profile strip or the three doors —
  and no text at all under `/`.
- **The two inputs that stop the fix being "never render the front door".** The same tree booted with
  **no** room in the tab's memory renders the front door immediately. One fixture cannot tell a rule
  from a deletion.
- **The silence ends on either frame.** `RoomJoined` → the waiting screen. `Failure(UNKNOWN_ROOM)` →
  the front door **carrying** *"No duel room has that code."*
- **§3 is pinned, not merely permitted.** `RoomJoined` alone still renders the whole waiting screen —
  the rival's plate, the invite panel and the promise sentence — so that no later ticket "repairs"
  the accepted half of this decision by withholding it.
- **The recovery sequence end to end.** `RoomJoined` then `Snapshot` lands on the table; `RoomJoined`
  then `DuelFinished` lands on the result screen — the two shapes `P4` and `P2` drove.
- **Nothing was minted.** No new `Screen` member, no new player-facing string, `PROTOCOL_VERSION`
  unchanged.

## Consequences

**What it buys.** The one screen in the recovery sequence the server never stated stops being
painted, and with it the two controls on it that can take a player out of the duel they are in while
`DEC-110`'s guard does not exist. `ADR-0112` §6's *no lobby on the way* becomes a property instead of
an aspiration, on the three routes that falsified it. The client's rule becomes one sentence a reader
can hold — it shows what it was told, and before it is told it shows nothing — and `roomAwaited`
stops being a special case for two mailed screens and becomes the general fact it always was. And the
half that is **not** repaired is now intent on the record, so the next drive that sees a stale
waiting screen files nothing and a coder cannot quietly answer `DEC-127` the other way inside a
ticket.

**What it costs.**

- **A recovering browser is a black rectangle, and on a slow connection it is one for as long as the
  round trip takes.** This is strictly worse than today on that path: the front door is visibly alive
  — `P2`'s own second frame is the profile strip finishing its HTTP read — and an empty shell offers
  no wordmark, nothing to look at, and no clue that anything is happening. The product accepts
  looking briefly dead in exchange for never briefly lying.
- **Against an unreachable server that rectangle is permanent** (§5). The chosen screens stay
  reachable by address, and nothing on screen says so, and no player will discover it.
- **The stale waiting screen ships, with a destructive control on it** (§3). *Back to the lobby* over
  a running duel forgets the room and hands the seat to the sweep. The window is a frame gap and no
  player has ever hit it — the product has none yet — and that is the whole of the argument for
  accepting it.
- **A third qualification of `ADR-0076` §3 now lives in a third file.** `ADR-0114` §2's enforcement —
  a chosen screen cannot render without consulting `rulingOn` — does not extend to the fall-through
  this ADR conditions. Nothing mechanical stops a future branch from painting the front door on
  `unknown` again; only §6's test does.
- **`STORY-1311` grows.** It was a branch order and a hold; it is now those plus a silence and the
  tests that pin both halves of this decision.

**What it forecloses.** Nothing permanently. The front door is foreclosed as a recovery screen and a
progress surface is foreclosed *today, on today's evidence*, both at the price of one branch
condition and one card to reverse. Nothing about the wire is foreclosed: the server naming the room's
state stays available to the architect, and §4 says what would make it worth its `PROTOCOL_VERSION`
move.

## Alternatives considered

**Acceptable for both windows — change nothing.** The strongest case in the set, and the one the
`DEC` row invites. Every frame in the observed sequence is either a state the server did state or a
front door that makes no claim about the room; `ADR-0102` §5's standard is met on all three routes,
every time; ten of the observed thirteen seconds are the dev server's module waterfall and vanish
under `dist/` (`ADR-0117`); and the front door is the only frame that keeps the page visibly alive
while the rest of the recovery happens. Rejected on one fact read out of the source rather than
argued: **the front door is not claimless.** `replyToCreateRoom` performs no seat check — `DEC-110`
is open precisely because that guard does not exist — so a `CreateRoom` sent from it is honoured,
repoints this connection's membership, and `boot.ts` overwrites the remembered room code with the new
one. The screen the server never stated is the one carrying that control, it is on screen for the
whole of every recovery rather than between two frames of one delivery, and it is removable today at
the cost of one condition.

**A neutral interstitial — a quiet line or mark while the client asks.** Its case is real: it is the
only option that is both true and alive, it tells the player the machine is working rather than
leaving them to guess, and on the slow connection where any of this is visible it is strictly better
than an empty page. Rejected because it buys a permanent surface to cover a window the artifact a
player actually receives makes a round trip long — a mark that appears and vanishes inside a few
hundred milliseconds is noise, which is what *"Dark, quiet, fast, minimal"* refuses — and because it
needs a sentence, which `ADR-0105` §4 treats as the signal that a decision has outgrown itself and
owes its own ADR with its own card. The evidence offered for it is a duration measured on
`npm run dev`, most of which `ADR-0117` attributes to the instrument. Not refused forever: §4 names
the reading that would reopen it and the number that would decide it.

**Withhold the waiting screen too, until a second frame states the room.** The symmetrical answer,
and it would close the case the drives actually named rather than the one beside it. Rejected on the
server's own source: `replyToJoinRoom`'s `ALREADY_SEATED` branch answers a host rejoining a
still-`WAITING` room with `RoomJoined` and nothing after it, so this rule would hang that host on an
empty page for the whole of their wait. It converts a frame-gap flicker into a permanent blank on a
path `STORY-1310` drove and found working — the worst trade in the set.

**Have the server name the room's state in the frame that answers the join.** The only option that
closes the ambiguity exactly instead of by inference, and `ADR-0114` §6 already identified it as the
precise remedy. Rejected as not this decision's to take and not yet worth its price: it is a wire
change and a `PROTOCOL_VERSION` move, it belongs to the architect, and it would be bought for a
between-frames flicker with no observed victim. §4's reopening condition is what would make it
arguable on evidence, and it is a small ADR then.

**Render the last screen this tab was on, remembered across the reload.** Its case: no blank, no
front door, and the player sees continuity through the recovery. Rejected outright and not on
balance — a table drawn from a remembered view is the client asserting a game fact, which
`CLAUDE.md`'s third non-negotiable and [`ADR-0002`](ADR-0002-server-authoritative.md) forbid, and it
would paint stale hole cards, stacks and a pot from before the reload. It is the one option here that
is not merely wrong but unavailable.
