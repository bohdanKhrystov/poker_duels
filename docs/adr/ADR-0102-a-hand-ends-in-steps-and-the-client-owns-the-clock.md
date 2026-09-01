# ADR-0102 — A hand ends in steps, and the client owns the clock

- **Status:** Accepted
- **Date:** 2026-09-01
- **Resolves:** `DEC-105` — **the architect's** — how is a runout's pacing produced, and what does a
  step cost? Raised 2026-09-01 by
  [`TASK-121301`](../../tasks/tasks/TASK-121301-the-runout-arrives-street-by-street-on-the-screen-too.md)
  against `R1` `not met` at beat 5 of round 1 of `/qa-cycle audit smoke`
  ([`STORY-1213`](../../tasks/stories/STORY-1213-round-1-audit-three-criteria-unmet-and-a-table-that-names-the-wrong-winner.md)),
  and routed by [`ADR-0096`](ADR-0096-the-audit-judges-a-whole-duel-against-a-frozen-rubric.md) §2:
  *"`R1` requires that a runout be perceivable; it fixes no duration, no animation and no
  transition. How a beat is paced is settled by the ticket that repairs it — with a card where a
  still can hold it (`ADR-0091` §3), with the architect where it cannot."*
- **Applies** [`ADR-0002`](ADR-0002-server-authoritative.md),
  [`ADR-0032`](ADR-0032-react-subscribes-to-a-store-it-does-not-own.md) §§1–3 and
  [`ADR-0095`](ADR-0095-the-table-states-who-took-the-pot-and-never-names-a-hand.md) §§2, 4.
  **Amends nothing**, and in particular contradicts no non-negotiable: the wire does not move,
  `poker-engine` is not opened, and no fact on the screen is one the server did not state (§6).
- **Registers no new `DEC`.** The one thing this leaves unhandsome — a runout's stacks settle at the
  first step, so the outcome is legible from the seat plates before the river lands
  (§*Consequences*) — blocks nothing and nobody is working it, and *a `DEC` nobody is working is
  noise in the open table* (`STORY-1211`, quoted by `STORY-1213`).

## Context

### What was seen, and what the source says about it

At beat 5 of the first audit round a player who called an all-in received two consecutive frames and
nothing between them: no community cards and *Preflop*, then five community cards, *Hand complete*,
and a winner named. `poker-engine`'s `StreetProgression.runOutBoard` deals the flop, the turn and
the river as three separate `StreetDealt` events — *"so the log reads like the deal it was instead
of one card dump"*, which is the sentence `R1` is licensed by — and not one of the three is
perceivable.

`TASK-121301` read both sides and established three facts. **All three are confirmed, and a fourth
that changes the answer was missed.**

1. **Confirmed.** `poker-server/…/duel/Addressed.kt`'s `broadcast` emits, per seat, one `Events`
   frame carrying every `visibleTo`-filtered new event — all three `StreetDealt` entries, still
   distinct, in deal order — and then the `Snapshot` that is *"the authoritative last word on
   state"*. Its own KDoc fixes the order: *"within each seat, events (if any) before snapshot"*.
   Nothing is dropped or coalesced on the wire.
2. **Confirmed.** `web-client/src/store/duel-state.ts`'s `"Events"` case appends them into
   `state.narration`. They land in the client's memory.
3. **Confirmed.** The same reducer's `"Snapshot"` case replaces `state.view` wholesale and never
   reads `narration`; `DuelTable.tsx` renders `<BoardCards cards={view.board.cards} />`. So
   `view.board.cards` goes 0 → 5 in the tick the `Snapshot` runs, because that `Snapshot` already
   carries the post-runout state — the whole runout is one engine transition,
   `EngineResult.accepted(showdown.newState, events)`, one state and a list.
4. **Missed, and decisive.** *The next hand is dealt in the same call, and its frames are delivered
   in the same batch.* `DuelAction.kt`'s `act` ends with `if (!result.newState.isHandOver) return …;
   val advanced = advance(played, seeds); return DuelStep(advanced.runner, outbound +
   advanced.outbound)`, and `DuelSocket.kt` hands the whole list to one `deliver`. A merged test
   states it in as many words — `DuelActionTest.afoldEndsTheHandAndOpensTheNext` asserts that one
   `act` produces snapshots for **hand 1 and hand 2** in one `outbound`. There is no pause anywhere
   on the server: the only `delay(…)` in `poker-server/src/main` is `Application.kt`'s sweep ticker.

Fact 4 is why the obvious repair does not work. A client that slices the board over 1.5 seconds is
overwritten by the next hand's `Snapshot` a millisecond into the first step. **Whatever paces a
runout must also govern when the frames behind it are applied**, or it paces nothing. The same fact
says something uncomfortable about a merged promise:
[`ADR-0095`](ADR-0095-the-table-states-who-took-the-pot-and-never-names-a-hand.md) §4's award banner
*"stands while the street is `COMPLETE` and goes when the next hand begins"* — and the next hand
begins in the same delivery.

### What is actually in tension

**One:** the client already holds every fact it needs and paints them all at once, while the server
holds no fact it has not already sent. So the question is not *what to send* — it is *who owns the
schedule*, and this repository has a non-negotiable that sounds like it answers that and does not:
*the server is authoritative; a client may never assert a game fact.* A client that reveals in steps
holds back, for a second or two, something the server has already told it. Whether that is an
assertion or a repaint is the whole decision, and §6 meets it head on rather than around.

**Two:** the engine hands the server exactly one post-transition `GameState` for a whole runout. A
server that wanted to send one `Snapshot` per street would have to manufacture the intermediate
states, and `poker-engine` is not open here — so it would rebuild them by replaying events through
`StateProjection.apply`. That is possible today and wrong in a way that is easy to miss:
`runOutBoard` carries the deck forward **outside** the projection (`StateProjection.apply(current,
dealtEvent).copy(deck = deal.deck)`), so a replayed state matches the engine's in every field a
`PlayerView` happens to read and in neither of the two it does not. It works by luck.

**Three:** the pacing is on the critical path of four merged end-to-end suites.
`whole-duel.test.tsx`, `duel-secrecy.test.tsx`, `claimed-here-recovered-there.test.tsx` and
`drive-duel.test.tsx` drive the real store through `bootDuelClient` and replay recorded frames
synchronously inside `act()`.
[`ADR-0100`](ADR-0100-the-driver-reaches-an-amount-by-pressing-what-a-player-presses.md) §3 made
*no frame is re-recorded and none of the four files is edited* the evidence that nothing those
suites prove was traded away. A clock anywhere in that path is a bill against that evidence.

### The deadline

Two, and neither is a reason to decide a particular way.

- **`R1` stays `not met` until this merges**, and `EPIC-12` §Termination rule 5's three-round budget
  is running. A criterion in the rubric's own priority order that no ticket may start on is the one
  shape that can end a cycle `STOP_BUDGET` with the repair known and unwritten.
- **Recorded frames are cheap to keep pure and expensive to clean up.** `scripted-duel.gen.json` is
  a list of frames, not a list of instants. If the pauses lived on the server they would be *in*
  every future recording, and every e2e suite would become timing-dependent at the moment it was
  recorded. Deciding client-side now costs one parameter; deciding it later costs a re-record.

## Decision

### 1. A hand's ending is painted as a sequence of steps, and the **client** is what holds them

`poker-server` is not changed. `poker-engine` is not opened. The wire does not move. The whole
mechanism is `web-client`'s.

**The store gains a step queue.** When a `Snapshot` says the hand is over — `view.street ===
"COMPLETE"` — it is not painted in one tick. It is painted as a sequence of **steps**, and every
frame that arrives while steps remain is **queued, in arrival order, and applied when the last step
has stood**. Nothing is dropped and nothing is reordered.

A `Snapshot` that does not end a hand is applied at once, exactly as today, with no step, no timer
and no queue — which is every frame in ordinary play. **Only a hand's ending is ever paced.**

### 2. What a step is, exactly

The steps of a hand-completing `Snapshot` are:

- **one step for each `StreetDealt` in the `Events` frame that immediately preceded it**, in the
  order the server sent them; then
- **one final step**, which is the whole snapshot: the full board, the street label *Hand complete*,
  and `ADR-0095`'s award line.

At non-final step *k* the table paints the snapshot's own view with exactly two fields lagged:

- **the board** is `view.board.cards.slice(0, n_k)`, where
  `n_k = view.board.cards.length − (the number of cards carried by the steps after k)`;
- **the street label** is `steps[k].street`, the `StreetDealt` event's own field.

Everything else on the screen is the snapshot's, unlagged (§*Consequences* names what that costs).

Worked, for the beat that raised this. `Events` carries `StreetDealt(FLOP, 3 cards)`,
`StreetDealt(TURN, 1)`, `StreetDealt(RIVER, 1)`; the `Snapshot` has a five-card board and
`street: COMPLETE`. Four steps: `5 − 2 = 3` cards and *Flop*; `5 − 1 = 4` and *Turn*; `5` and
*River*; then the whole snapshot, *Hand complete*, and the award line. An all-in **on the turn**
carries one `StreetDealt`, so two steps: five cards and *River*, then the award. A fold, or a hand
called down to the river, carries none: **one** step, which is what finally gives `ADR-0095` §4's
banner a moment to be read before the next hand's `Snapshot` lands.

The subtraction is deliberate and it is why no previous view is needed: every step's board is a
**prefix of the snapshot's own list**, at a length that is the snapshot's own length minus the
lengths of the server's own `StreetDealt` payloads. A hand that opens all-in — `advance`'s
`openHand` path, where the previous view belongs to a different hand entirely — walks the same three
steps with no special case.

### 3. What is withheld, and what is never withheld

**The only thing this mechanism ever withholds is the tail of the community board, and the delivery
of frames behind it.** That is the invariant, and it is narrow on purpose:

- **No card face on screen ever comes from a `StreetDealt` payload.** The identity of every card
  painted is read from `view.board.cards`; the events are read for *how many* cards a step adds and
  *which street to name*, both fields the server set. `TASK-121301`'s *Out of scope* — *"it never
  reconstructs a board from events"* — stands unamended, and §8 names the test that proves it.
- **No hole card is ever held back.** The rival's cards appear with the first step, which is what an
  all-in runout looks like in every poker client there is: the hands go face-up, then the board comes
  out. `PlayerView.of`'s *"under-revealing is a visible bug, over-revealing is a silent leak"* is
  untouched, in both directions.
- **No control is ever offered against a lagged screen, and none is ever retracted.** The
  hand-completing `Snapshot` has `seatToAct: null` and its reducer case clears `pendingTurn`; the
  next hand's `YourTurn` is *queued*, so it is shown late and never shown early. A step therefore
  never leaves a decision on screen the server has closed, and never opens one before the server
  does.
- **Nothing is computed.** `web-client/src/table/no-derivation.test.tsx` stays **byte-unchanged** and
  green, for the reason `ADR-0095` §5 gives: a step shows *fewer* of the server's own values, never
  a value the client worked out. A coder who meets that gate red has built a step on a computed
  number, and the fix is the step, never the matcher.

### 4. A step is **600 ms**, named once, at the boot seam

`REVEAL_STEP_MS = 600`. A runout from preflop therefore takes 2.4 s and an ordinary hand's ending
0.6 s. The number is named **once**, in `web-client/src/store/boot.ts`, and reaches the store as a
parameter.

**Zero means synchronous, not `setTimeout(fn, 0)`.** At a step of `0` the store releases in the same
turn and schedules nothing, which is byte-for-byte today's behaviour. That is what lets
`web-client/src/e2e/drive-duel.tsx` boot at `0` and keeps `ADR-0100` §3's evidence intact: **the four
recorded-frame suites are not edited and no frame is re-recorded.**

The parameter is not a test-only door in `ADR-0100` §5's sense, and the distinction matters. It is a
production seam — boot has to name a schedule for the store to have one at all — of exactly the kind
this repository already insists on everywhere the outcome depends on time or chance: the engine's
injected `Rng`, and `ADR-0062`'s *"a date comes only from an injected `java.time.Clock`"*. The driver
choosing `0` is choosing a value, not opening a hatch, and no component gains a prop, a flag or a
`data-testid`.

**600 is a feel number and it is the cheapest thing here to be wrong about.** It is one constant in
one file, behind a mechanism this ADR fixes; moving it changes no interface and no test that is not
about the number itself. If the product owner wants it at 400 or 900, that is a one-line answer that
amends nothing in §§1–3. What is *not* free to move is the mechanism, and that is what this section
exists to pin.

### 5. A returning or reconnecting client **jumps to the end**, and the server is why

It replays nothing, and this needs no special case, no flag and no field: `DuelResume.kt`'s
`resumeFrames` calls `framesFor(hand.state, newEvents = emptyList(), handEvents = …)`, and
`broadcast` *"only emits an `Events` frame when a seat's visible new events are non-empty"*. **A
resuming client is sent no `StreetDealt` at all**, so §2's street steps are empty by construction — a
reconnect mid-runout, and a reload, both land on the finished board at once.

A resume `Snapshot` that happens to arrive at `COMPLETE` still takes §2's single final step. That is
600 ms with nothing behind it in the queue, and it is left uniform rather than special-cased: one
rule for *what a hand-completing snapshot does* is worth more than 600 ms saved on a reload.

### 6. This asserts no game fact — met head on

The rule this decision draws, in one sentence:

> **The client may choose when to paint a fact the server sent. It may never choose what the fact
> is.**

A paced reveal is ordering, not authorship, and four properties make that checkable rather than
merely arguable:

1. **Every value painted is a value the server sent**, in the order the server sent it. The board at
   every step is a prefix of the server's own list; the street name at every step is a `StreetDealt`
   field; the award line is `PotAwarded`'s own `seat` and `amount`.
2. **The lag is monotone, bounded and self-terminating.** Steps only advance, there are at most
   four, the queue always drains, and the screen always ends on the snapshot.
3. **It can never influence what the client sends.** Legality reaches the client only through
   `YourTurn` and `LegalActions`, and §3 forbids painting either early.
4. **It is only ever a lag, never a contradiction.** A client whose screen trails the server by two
   seconds is what every client on a network already is; what would be new is a screen that says
   something the server did not.

**The repository has already ruled on the principle, and this is its second application.**
`ADR-0095` put `PotAwarded.amount` and `PotAwarded.seat` on the table straight out of `narration` —
*"every number is a `PotAwarded.amount` this client actually received, never a total the client works
out"* — which settles that **rendering a value the server stated is not asserting a game fact;
computing one is.** `ADR-0002` fixes who decides what is true (*"which cards exist and who holds
them, whose turn it is, whether an action is legal, the size of every pot, who won"*) and says
nothing about repaint order; its own summary of the client's job is *"render state, send intents"*,
and this changes neither. And `docs/protocol.md` already treats the `Snapshot` as a **repair** rather
than a paint instruction, in its own words: a frame the client cannot process is dropped and *"the
next `Snapshot` from the server re-establishes the truth."*

**The one place this could go wrong, named so it is a decision and not a drift:** the queue delays
*every* frame behind a hand's ending, `OpponentPresence` included. `ADR-0028` §2 makes
`graceRemainingMillis` *"how much of the window was left **at the instant the server built the
frame**"*, and *"the client turns it into a local deadline once"* — so a queued one anchors its
deadline up to 2.4 s late and over-states the grace left by that much. `ADR-0028` §3 already prices
that error and, usefully, in the other direction: *"the client's countdown is expected to reach zero
early by up to `sweepPeriodMillis` plus latency. That is not drift to be corrected."* This makes it
reach zero **late** instead, which is the more generous side of the same rendering aid — and §3's
governing rule is that the countdown decides nothing anyway: *"the client never changes what it does
because its countdown reached zero."* FIFO is kept, because reordering by frame type is precisely
what turns *a lag* into *a contradiction*, and exempting a type later is additive.

**And the deadline on the rule itself:** the day a duel gains a **move clock**, the frame that starts
it must not sit in this queue. There is no move clock today (`ADR-0013`'s grace period is about a
dropped connection, not a slow decision), so nothing is owed now — but a future ADR that adds one
owes this queue an exemption, stated there.

### 7. The wire does not move, and it would not have moved either way

`PROTOCOL_VERSION` **stays where `develop` has it.** No new `ServerMessage` variant, no new field, no
extra frame: `docs/protocol-versions.md` gains no row, `protocol.gen.ts` is not regenerated,
`web-client/src/protocol/version.ts` does not move, and `ProtocolVersionLedgerTest`'s fingerprint is
unchanged by construction — `ADR-0047` §2 hashes the **declarations**, and no declaration changes.
`docs/protocol.md` gains nothing; a client's repaint schedule is not a protocol fact.

Said plainly, because it removes a bad argument from this ADR's own side of the ledger: **a
server-paced answer would not have needed a bump either.** More frames of an existing type change no
declaration, so `ADR-0047`'s fingerprint would have been just as still. The server option was not
rejected on the cost of a version bump — it was rejected on §*Alternatives considered*.

### 8. What `TASK-121301` becomes, and what a test must prove

**It stays one `module: web-client` ticket, re-cut whole**, and it is **atomic** under
[`ADR-0068`](ADR-0068-an-atomic-ticket-names-the-gate-that-forbids-splitting-it.md) §3 as amended by
`ADR-0069`/`ADR-0070`: the file set is its own `Files` table, probed and run to a green gate set, and
it declares the exemption naming the gate. The planner may split it along exactly **one** seam — *the
store publishes the steps* / *the table paints them* — and only if the half that lands first leaves
the step at `0`, so nothing half-paced ever reaches `develop`; both halves land before `STORY-1213`
closes. It is **not** split any other way, because every other cut leaves a store field with no
consumer, which is the precise shape of the defect being repaired.

Its `## Files` table is replaced, not amended. The probe starts here and the ticket owns the result:
`web-client/src/store/duel-state.ts` and its test (the step state, and the pure advance the store
calls per tick), `web-client/src/store/duel-store.ts` and its test (the queue and the injected
schedule), `web-client/src/store/boot.ts` (the constant, once), `web-client/src/table/DuelTable.tsx`
and `web-client/src/table/PotStrip.tsx` with their tests (paint at the step),
`web-client/src/lobby/Lobby.tsx` (pass it down), and `web-client/src/e2e/drive-duel.tsx` (boot at
`0`). `web-client/src/table/no-derivation.test.tsx` is **read, never edited** (§3).

`verify:` stops carrying only the linter. It carries the client suite, and the `manual-verify` label
stays for the browser walk — a browser fact may never be a gate here (`ADR-0089` §2b), and a `grep`
that passes either way is worse than an honest manual step.

**What the tests must prove**, because a suite that only counts paints would pass on the wrong
mechanism:

1. **The card faces come from the snapshot, not from the event.** A `StreetDealt` whose `cards`
   differ from the snapshot's board prefix, and the assertion is that the screen shows the
   **snapshot's** cards. Two inputs that disagree, or the test cannot tell a copy from a constant.
2. **A runout paints three intermediate boards** — 3, 4 and 5 cards, with *Flop*, *Turn* and *River*
   — **and the award line appears at none of them.**
3. **The queue is FIFO and loses nothing.** With the next hand's `Events`, `Snapshot` and `YourTurn`
   arriving during the steps: nothing of hand *N+1* is on screen before the last step, all of it is
   after, and in that order.
4. **Ordinary play schedules nothing.** A mid-hand `Snapshot` with one `StreetDealt` before it is
   applied synchronously, with the injected schedule asserted **not called** — the no-regression gate
   for `R1` at the other seven beats.
5. **A resume replays nothing.** A `Snapshot` at `COMPLETE` with no `Events` before it takes one
   step, not four.
6. **A step of `0` schedules no timer at all** — the property §4 leans on to leave four e2e suites
   alone.

## Consequences

**The cost, named first because it is the one most likely to bring this ADR back: a runout's stacks
settle at the first step, so the outcome is legible from the seat plates before the river lands.**
Only the board and the street label lag (§2); the snapshot's seat views are the post-award ones, so a
player watching the numbers rather than the cards learns who won 1.8 seconds early. It was not fixed
here, deliberately: holding the plates means painting some fields from the new view and others from
the previous one, and that composite is *sound for a mid-hand all-in and badly wrong for a hand that
opens all-in*, where the previous view belongs to another hand entirely. A client that assembles a
view the server never sent as a unit is over the line §6 draws, and it is a worse thing to be wrong
about than a spoiler. The cheap repair, if it bites, is a smaller one — quiet the seat plates during
a step rather than back-date them — and it belongs to whoever files it.

**A second cost: every hand now ends 600 ms later than it does today, on every path, whether or not
anything was worth watching.** A duel of thirty hands pays eighteen seconds it did not pay before, in
a product whose vision word is *fast*. That is the price of `ADR-0095` §4's banner being readable at
all, and the argument that it is worth paying is that the banner is currently overwritten in the same
delivery that draws it.

**A third: the client's screen and the server's state now disagree by design, for up to 2.4 seconds.**
Every future reader of a bug report has to ask *"was the screen mid-step?"*, and every future frame
type has to be asked whether it may sit in a queue (§6 names the one that must not, the day it
exists). A queued `OpponentPresence` over-states the grace remaining by up to the queue's bound.

**A fourth: the two seats pace independently and can drift.** Pacing is per-tab, so a backgrounded
tab whose timers are throttled runs its steps slower than the tab beside it. The drift is bounded by
the queue and erased by the next frame either way, but *"we both saw it at the same moment"* is no
longer true of a runout, and no test can hold it.

**A fifth, and it is somebody else's: `DEC-104` gets worse before it gets better.** During a runout
the award line is suppressed (§2), so the pot slot reads `Pot {view.pot}` for the whole reveal —
which after settlement is **0**. That is the same `Pot 0` the audit already observed and already
routed, held on screen for 1.8 s instead of a tick. This decision introduces no new falsehood and
fixes none: *what the number labelled `Pot` counts* is `DEC-104`, the product owner's, and answering
it here would be guessing a product decision in an architecture costume.

**What it buys.** `R1` becomes meetable at beat 5 without touching `poker-engine`, `poker-server`,
the wire, `PROTOCOL_VERSION` or a single recorded frame; `ADR-0095` §4's banner becomes real rather
than nominal; and the repository gets a stated rule — *when to paint is the client's, what is true is
the server's* — that the next question of this shape can be decided against instead of re-argued.

**What it forecloses.** Server-side pacing, effectively: once the client owns the schedule, a server
that also paced would double every pause, and reversing this ADR means deleting a queue rather than
adding one — which is the direction to be wrong in. It also forecloses, until a further decision, any
presentation that needs an intermediate **state** rather than an intermediate **board**: a
street-by-street pot, or seat plates that back-date, cannot be built on §2 and would need the client
to hold a view the server never sent.

**What it is cheapest to reverse in.** All of it. One store field, one queue, one constant, one
parameter at the boot seam; no wire, no server, no engine, no recorded frame, no card. That is why
this answer was chosen over the two below on evidence this thin — nobody has yet watched a paced
runout, and the first person who does may want a different number or a different last step.

## Alternatives considered

**`poker-server` sends one `Snapshot` per street during a runout.** Its strongest case is the honest
one: it is the reading of *the server is authoritative* that needs no argument at all, it fixes both
browsers and any future client at once, and it puts the intermediate facts on the wire where a
recorded frame log can prove they existed — which is exactly the instrument the audit round used and
found empty. It also needs no protocol bump (§7), so it is cheaper than it looks.

Rejected on four counts, of which the last is fatal. (a) **The states do not exist.** The engine
returns one post-transition `GameState`; the server would rebuild the intermediate ones by replaying
events through `StateProjection.apply`, and `runOutBoard` carries the deck forward outside that
projection — so the rebuilt states match the engine's in every field `PlayerView` reads and in
neither of the two it does not. The server would be publishing views projected from states the engine
never produced, correct today by luck and silently wrong the day a view field reads a deck. (b) **It
puts a wall clock in the authoritative path.** Inside `RoomRegistry.act`'s mutex (`ADR-0016`) the room
is blocked for seconds, presence sweeps included; outside it, the duel's true state and the frames in
flight disagree, and a `resumeFrames` during the window sends the **true** state — so a reconnecting
client and a connected one see different games. Client pacing keeps that divergence local, bounded
and self-healing. (c) **It charges both seats the latency whether or not either is looking**, and
every server test that asserts a frame list gains a clock. (d) **It does not fix the defect.** The
next hand's frames come from `advance` in the same `act` (Context, fact 4), so the server would have
to pace those too — that is, own the client's presentation schedule outright, one hand at a time,
forever.

**Deal the next hand only when a client asks for it.** Strongest case: it removes the race at the
root rather than papering over it, it costs no clock anywhere, and it is how most poker clients
behave between hands. Rejected because it is not the architect's to decide — *does a player press
something to see the next hand?* is a change to what a duel **is**, and it would be the product
owner's before it was anyone's. It is recorded here so that a future reader knows it was seen and
routed, not overlooked.

**Split the runout into several `Events` frames and paint from them, holding the terminal `Snapshot`
behind them.** Strongest case: it is the smallest possible change to the client — a queue at the
socket seam and no new state — and it paces the runout without any board arithmetic at all. Rejected
on two counts. It requires the client to build a board out of event payloads, which is the thing
`TASK-121301` put out of scope and §3 keeps out. And holding the terminal `Snapshot` means holding a
**stale decision point**: the pre-runout view has `seatToAct` naming the player who just called, and
`pendingTurn` is cleared only by the frame being held — so the action bar would stay live against a
hand that is already over. The server would drop the click, and the screen would still have lied.
Holding the *next* hand's snapshot, which is what §1 does, is the safe direction of the same idea: a
decision shown late is never a decision shown falsely.

**Animate the board with CSS and change no state.** Strongest case: zero new client state, zero
queue, no clock in the store, and it is where a designer would start. Rejected because the props
never carry the intermediate values: the component is handed a five-card array once, so a transition
can only stagger the *entrance* of five cards that are all already true, and the next hand's snapshot
replaces the array a millisecond later and kills the animation mid-flight. `ADR-0096` §2 is explicit
that `R1` *"fixes no duration, no animation and no transition"*; the reason this one fails is not
taste, it is that there is nothing for it to interpolate between.

**Do nothing, and let `R1` be met by the event log.** Strongest case: the events *are* delivered, and
`R1`'s wording is about what a player *can tell happened*, which a written log would satisfy without
any pacing at all. Rejected because the criterion's own licence is `ADR-0008`'s *"so the log reads
like the deal it was"* read through `docs/vision.md`'s *"showing a player… is more interesting than
hiding the maths"*, and because it inverts the finding: the round did not observe a missing log, it
observed a board going from zero cards to five in one paint. A duel table that answers *what just
happened?* with a transcript is a different product from the one `ADR-0095` §3 spent a decision
keeping quiet.
