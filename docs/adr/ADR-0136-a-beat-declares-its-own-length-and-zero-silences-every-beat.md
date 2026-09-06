# ADR-0136 — A beat declares its own length, and zero silences every beat

- **Status:** Accepted
- **Date:** 2026-09-07
- **Resolves:** `DEC-146` — **by what mechanism does the client's step queue give one beat a length
  different from a step?** Registered 2026-09-06 by
  [`ADR-0120`](ADR-0120-a-showdown-shows-the-hands-the-rules-showed-and-the-beat-that-shows-them-stands.md)
  §3, which fixes the product half — the last beat of a hand that turned a hand face up the viewer
  had not seen stands **2,000 ms**, every other ending keeps
  [`ADR-0102`](ADR-0102-a-hand-ends-in-steps-and-the-client-owns-the-clock.md) §4's **600 ms** — and
  designs none of the machinery under it.
- **Serves, and does not reopen:** `ADR-0120` §§1–2 (what a showdown shows, and that it is drawn in
  the seat that showed it, from the snapshot), §3 (**which** beat is long, **how long** it stands,
  and the predicate the client evaluates — *the hand-completing view carries hole cards for the
  rival's seat*), and §4 (what it decides nothing about). Every one of those is a constraint this
  ADR is written inside, and none of them is re-argued here. `ADR-0126`'s *no card is marked* is
  likewise untouched: nothing here paints anything.
- **Applies, and amends nothing:** `ADR-0102` §§1–3 (the FIFO queue, what a step paints, what is
  withheld) and §5 (a resuming client's single step) stand **exactly** as written; §4's *a step is
  600 ms, named once, at the boot seam* is **extended, not amended** — a second number joins it at
  the same seam, `REVEAL_STEP_MS` keeps its name and its value, and §4's *zero means synchronous*
  is widened from *a step* to *every beat* (§3 below).
  [`ADR-0100`](ADR-0100-the-driver-reaches-an-amount-by-pressing-what-a-player-presses.md) §3 — **no
  frame is regenerated and no e2e file is edited**;
  [`ADR-0115`](ADR-0115-motion-never-carries-a-fact-and-reduced-motion-stills-every-surface.md) §3
  (a step is not motion, §6 below);
  [`ADR-0113`](ADR-0113-the-turn-clock-is-derived-state-and-the-sweep-plays-the-seat.md) §6 (a queued
  frame anchors at arrival); [`ADR-0032`](ADR-0032-react-subscribes-to-a-store-it-does-not-own.md)
  §§1–3; [`ADR-0095`](ADR-0095-the-table-states-who-took-the-pot-and-never-names-a-hand.md) §4
  (*never on a timer, never on a fade*).
- **Nothing crosses the socket.** `PROTOCOL_VERSION` stays where `develop` has it, no
  `ServerMessage` variant and no field is added, `protocol.gen.ts` is not regenerated,
  `docs/protocol.md` and `docs/protocol-versions.md` gain nothing, and
  [`ADR-0047`](ADR-0047-a-protocol-version-is-claimed-in-a-ledger.md) §2's fingerprint is unchanged
  **by construction**, because it hashes declarations and no declaration changes. `STORY-1411`'s
  implementing ticket is therefore **not** `atomic:` under
  [`ADR-0068`](ADR-0068-an-atomic-ticket-names-the-gate-that-forbids-splitting-it.md) §3, holds no
  branch lock, and may land beside anything else in `EPIC-14`.
- **No server file, no engine file, no schema, no migration, no new player-facing string, no new
  control, no new `Screen` member** — `ADR-0120`'s *Constrains* list, honoured in full. The whole
  mechanism is one union-typed field, one predicate over one frame, one constant and one store
  option.
- **Constrains:** `STORY-1411`'s implementing ticket and its design card. It registers **no new
  `DEC`**; §8 names the one thing that follows from `ADR-0120` §3's rule and that a product owner
  might want stated differently, on `ADR-0105` §6's route — *a `DEC` nobody is working is noise in
  the open table*.

## Context

### What is merged, measured rather than recalled

The mechanism `ADR-0102` built has exactly one dial and three seams, and all three are one file
deep:

- **`web-client/src/store/duel-state.ts:157-160`** — `RevealStep` is `{ board, street }`, and
  nothing else. **`:466-482`** — `layOutReveal(view, streetDealt)` builds one step per `StreetDealt`
  plus a final step for the whole snapshot, each board a prefix of `view.board.cards`.
  **`:221-233`** — `advanceReveal` drops the standing step and, when it was the last one, folds
  every queued frame back through the reducer in arrival order. **`:244-257`** — while a `Reveal`
  stands, *every* arriving message is queued, whatever its type.
- **`web-client/src/store/duel-store.ts:48,76-80`** — `stepMillis` defaults to `0`; `armTick` calls
  `tick()` in the same turn at `0` and `schedule(tick, stepMillis)` otherwise. One number, read from
  a closure, with no reference to which step is standing.
- **`web-client/src/store/boot.ts:10,71`** — `REVEAL_STEP_MS = 600`, named once, and
  `stepMillis: options.stepMillis ?? REVEAL_STEP_MS`.

Downstream, the step is already narrower than the state that carries it:
`web-client/src/lobby/Lobby.tsx:338` passes `state.reveal?.steps[0] ?? null` into `DuelTable`, whose
prop (`DuelTable.tsx:35`) declares `{ board; street } | null` — a structural subset — and which uses
it for two things only: the board (`:50`) and the street label handed to `PotStrip` (`:91`, falling
back to `view.street` at `PotStrip.tsx:108`).

And the frame already carries everything `ADR-0120` §3's predicate needs. `PlayerView` declares
`viewerSeat` (`protocol.gen.ts:281`) — the server's own statement of whose view this is,
`PlayerView.kt:30,88` — and `SeatView` declares `holeCards` (`:306`), populated only where
`PlayerView.of`'s `showCards` said so. The predicate is a pure function of **one frame**.

### What is in tension

**Three things have to be true at once, and the obvious shapes each break one of them.**

1. **The number stays at the boot seam.** `ADR-0102` §4 is emphatic that this is the part that is
   *not* free to move: the duration is a production seam of the same class as the engine's injected
   `Rng` and `ADR-0062`'s injected `Clock`. A duration that migrates into the reducer stops being
   bootable, and `drive-duel.tsx:229` stops working.
2. **The beat's kind is a property of a frame, not of a clock.** `ADR-0120` §3 states it as a
   predicate over the hand-completing view, and states plainly that *"the two seats can answer it
   differently for the same hand"*. A store that decides at tick time has to re-derive, every tick,
   something that was settled when the step was laid out.
3. **Zero has to stay zero, for every beat.** `drive-duel.tsx:229` boots at `0` with the comment
   *"the four recorded-frame suites this driver serves may not be edited, and none may gain a
   clock"*. The four are `whole-duel.test.tsx`, `drive-duel.test.tsx`, `duel-secrecy.test.tsx` and
   `claimed-here-recovered-there.test.tsx`, and `ADR-0100` §3's evidence is precisely that **they
   need no edit**.

**Point 3 is not a preference, and the failure mode is worse than slowness.** The reveal's timer is
what *drains the queue*: while a `Reveal` stands, `applyServerMessage` queues every frame behind it.
If a read beat can arm a real `setTimeout` inside a suite that runs synchronously under `act()`,
those suites do not merely take two seconds longer per showdown — the next hand never reaches the
screen inside the `act()` that delivered it, and they fail. A `readMillis` that defaults from a
constant while `stepMillis` is `0` produces exactly that, quietly, and the symptom reads like a
flake. This is the single sharpest edge in `DEC-146`.

**And the driver's zero cuts the other way too.** At `0` no beat stands at all, so the recorded-frame
suites cannot see this decision and never could — they are evidence about *what is painted*, not
about *how long anything stands*. A mechanism that let them see it would be a mechanism that had
made the driver wait, which is the thing point 3 forbids. So the hold needs a proof of its own, in
the one place a duration is observable: the injected `schedule`.

### The deadline

`STORY-1411` is being split now, and this is the last moment the shape is free. Once the hold has a
consumer, moving it out of the reducer — or moving the classification out of the store — is a change
to merged files with merged tests. `ADR-0120` §3 also fixes **2,000** as a feel number it expects to
be wrong about; a mechanism that does not make that number a one-line move has failed at the one
thing it was asked to protect.

## Decision

### 1. A beat carries its **kind**, not its length

`RevealStep` gains one field, and `duel-state.ts` is where the classification lives:

```ts
export interface RevealStep {
  readonly board: readonly string[];
  readonly street: Street;
  /** How long this beat stands, named as a kind — the milliseconds are the boot seam's. */
  readonly hold: "step" | "read";
}
```

`layOutReveal` sets it, from the view it is already given and from nothing else. Every street step
is `"step"`. The final step is `"read"` exactly when `ADR-0120` §3's predicate holds:

```ts
const rivalShown = view.seats.some(
  (seat) => seat.index !== view.viewerSeat && seat.holeCards.length > 0,
);
steps[streetDealt.length] = {
  board: view.board.cards,
  street: view.street,
  hold: rivalShown ? "read" : "step",
};
```

**`layOutReveal`'s signature does not change**, and neither does `applyServerMessage`'s. The
predicate reads `view.viewerSeat` rather than `state.mySeat` for three reasons that are all the same
reason: the frame answers for itself, so there is no cross-frame dependency to get wrong; a
hand-ending `Snapshot` that was **queued** behind another hand's ending classifies itself correctly
when `advanceReveal` folds it back through the reducer; and `mySeat`'s `null` case — a `Snapshot`
before a `RoomJoined` — has no wrong answer to give, because it does not arise. The `.some` form is
used rather than `seats[1 - viewerSeat]` so that nothing here depends on the array's order or on
there being two seats.

The predicate is `ADR-0120` §3's sentence and not a paraphrase of it: nothing is computed, no event
is consulted, and no previous view is remembered. A fold answers `"step"` with no special case,
because the server sends no hole cards for a hand that folded (`ADR-0008`); a showdown the viewer
won answers `"step"` for that viewer and `"read"` for the other, from the same hand, which is what
`ADR-0120` §3 says it must.

### 2. The store owns both lengths, and reads the kind off the beat now standing

`DuelStoreOptions` gains `readMillis?: number`, and **absent means `stepMillis`** — so a beat is a
step unless a caller has asked for a longer read. `armTick` maps kind to milliseconds:

```ts
const readMillis = options.readMillis ?? stepMillis;

// The beat now standing is the head of the queue at the instant the tick is armed, so nothing
// here remembers which step it is on.
const armTick = (): void => {
  if (stepMillis === 0) {
    tick();
    return;
  }
  schedule(tick, state.reveal?.steps[0].hold === "read" ? readMillis : stepMillis);
};
```

`boot.ts` names the second number beside the first, once, and passes it down:

```ts
export const REVEAL_READ_MS = 2000;
// ...
readMillis: options.readMillis ?? REVEAL_READ_MS,
```

`BootOptions` gains `readMillis?: number`, documented as `REVEAL_STEP_MS`'s sibling and as a
production seam rather than a test door, in `ADR-0102` §4's own terms.

**Why the kind is in the reducer and the milliseconds are in the store:** that is `ADR-0102`'s own
seam, kept. §2 is *what a step is*, and it is pure, frame-derived and testable without a clock; §4
is *how long a step stands*, and it is the boot seam's. This ADR adds nothing to either side that
does not already belong there.

`advanceReveal` is **byte-unchanged**. It drops steps; it has no opinion about their length.

### 3. `stepMillis === 0` silences **every** beat, and the gate lives in the store

`ADR-0102` §4's *zero means synchronous, not `setTimeout(fn, 0)`* is widened, in one clause:

> **A step of `0` is a reveal with no clock in it at all.** Every beat, of every kind, releases in
> the turn that armed it, and `schedule` is never called for a reveal. `readMillis` is not consulted.

The gate is in `duel-store.ts`, **before** the kind is read — not in `boot.ts` — because that is the
one place both numbers meet and the only place that covers every caller. `createDuelStore` is
called with no options at 102 sites and with options at 11; `bootDuelClient` is called at 11. A gate
in boot would protect only the paths that go through boot, leaving every direct store construction
to a convention nobody enforces.

**What this buys, concretely:**

- **`web-client/src/e2e/drive-duel.tsx` is not edited.** Its `stepMillis: 0` (`:229`) already means
  what it has to mean, and it is deliberately **not** given a redundant `readMillis: 0` — a
  redundant zero is a second statement that can drift out of agreement with the gate that makes it
  redundant.
- **`ADR-0100` §3's evidence stands.** The four recorded-frame suites are neither edited nor
  re-recorded, `scripted-duel.gen.json` is byte-unchanged, and `ScriptedDuel.kt`'s step counts (57
  and 55) do not move.
- **Every merged caller keeps its exact behaviour with no edit**, including
  `duel-store.test.ts`'s `stepMillis: 250` cases, because `readMillis` absent is `stepMillis`.

**What the driver proves, and what it does not.** It proves the frames: what is painted, in what
order, from what the server sent. It has never proved a duration — at `0` nothing stands — and it
does not begin to. The hold is proved where a duration is observable, in §7.

### 4. Nothing on the screen states the hold, and no component learns it

`DuelTable`'s `revealStep` prop keeps its declared shape, `{ board; street } | null`. A `RevealStep`
carrying `hold` is assignable to it without an edit, because TypeScript's excess-property check
reaches object literals and `Lobby.tsx:338` passes a variable; this was checked against `tsc
--strict`, not assumed. `Lobby.tsx`, `DuelTable.tsx` and `PotStrip.tsx` are **untouched**, and
`DuelTable.test.tsx`'s literal `revealStep` props stay valid.

So: **no component gains a prop, no element gains an attribute or a `data-testid`, and the DOM is
byte-identical at 600 ms and at 2,000 ms.** `ADR-0102` §4's *"no component gains a prop, a flag or a
`data-testid`"* stays true, `web-client/src/table/no-derivation.test.tsx` stays **byte-unchanged**
and green (nothing new is painted, so there is no new number for it to reject), and the reason the
recorded-frame suites cannot see this decision is structural rather than incidental.

### 5. Nothing crosses the socket — and this is the load-bearing claim for the plan

Stated at full volume, because `STORY-1411`'s ticket shape turns on it. The predicate reads
`PlayerView.viewerSeat` and `SeatView.holeCards`. **Both already ship.** Both are already on every
`Snapshot`. Both are already filtered per recipient by the engine's projection layer — `PlayerView.of`'s
`showCards`, which is the only place a hole card is ever filtered and stays so. No declaration
changes, therefore `ADR-0047` §2's ledger fingerprint cannot move, therefore there is no
`PROTOCOL_VERSION` step, therefore **`STORY-1411`'s implementing ticket is not `atomic:` and takes no
branch lock.**

The non-negotiable is worth meeting head on rather than by silence. The client **reads** hole cards
here to choose a schedule; it does not filter them, and it can only ever see what the projection
already decided to send it. The strongest form of the argument is the failure mode: **the worst this
mechanism can do wrong is stand a beat for the wrong length.** There is no input to it that could
put a card on a screen, and no output from it that reaches the wire. A client that classifies a beat
wrongly shows exactly the same cards for 600 ms instead of 2,000, or the reverse.

### 6. Reduced motion does not reach this, by construction rather than by exemption

`ADR-0115` §3 draws the line — *"a step changes what the screen states; motion is how the change is
drawn. Reduced motion keeps every step and skips every how"* — and names `ADR-0102`'s stepped runout
as a step. The read beat is the same kind of thing: a longer interval between two facts, with no
`how` in it.

The mechanism makes that unfalsifiable rather than merely intended: **the hold has no CSS.** It is
`REVEAL_READ_MS` in `boot.ts` and a `delayMillis` argument to an injected `schedule`. No
`--pd-motion-*` token is minted, `design/tokens/tokens.css` is not edited, the sheet's one
`prefers-reduced-motion: reduce` block is not edited, and the store has no `matchMedia`, no
stylesheet and no element. There is nothing here for a media query to reach, so
`prefers-reduced-motion: reduce` **cannot** shorten, skip or collapse this beat, and no ticket has
to remember not to let it.

Two corollaries, so no card or ticket re-derives them:

- **`STORY-1411`'s design card draws a still state.** A fade-in or a scale on the revealed hand
  would spend part of the 2,000 ms drawing rather than showing, would make the beat's meaning
  partly motion against `ADR-0115` §1, and would put a fade next to `ADR-0095` §4's *never on a
  fade*. The reveal is already on screen the instant the beat begins; the beat is time, not
  entrance.
- **The hold is never implemented as a CSS transition or an animation event.** Delivery of every
  queued frame depends on this timer firing; a stylesheet-driven hold would put frame delivery
  behind layout, and a suite with no layout would never drain the queue.

### 7. What a test must prove

Named here, in `ADR-0102` §8's idiom, because a suite that only counted paints would pass on the
wrong mechanism — and because at `stepMillis: 0` a paint count cannot tell 600 from 2,000 at all.

1. **The same frame answers differently for the two seats.** One hand-completing view, laid out
   twice with `viewerSeat: 0` and `viewerSeat: 1`, where seat 1 carries `holeCards` and seat 0 does
   not: the last step is `"read"` for seat 0 and `"step"` for seat 1. **Two inputs that disagree**,
   or the test cannot tell a predicate from a constant.
2. **A fold's ending is a `"step"`** — the rival seat `hasFolded: true` with empty `holeCards` — so
   nothing passes merely because the hand is over. `view-fixture.ts`'s `aSeat` defaults
   `holeCards: []`, which is the short side, so no existing test can drift into `"read"` by
   accident and no new one can reach it without saying so.
3. **Only the last step is long.** A three-`StreetDealt` runout to a showdown: steps 0–2 are
   `"step"`, step 3 is `"read"`.
4. **The store waits both lengths.** With a `schedule` spy at `stepMillis: 600, readMillis: 2000`,
   assert the **`delayMillis` arguments** — `[600, 600, 600, 2000]` for that runout, `[600]` for a
   fold's ending. A call *count* is identical either way and proves nothing.
5. **`stepMillis: 0` schedules nothing, whatever the beat.** A showdown ending at
   `stepMillis: 0, readMillis: 2000` — `readMillis` explicitly non-zero, so the test cannot pass for
   the wrong reason — calls `schedule` **not at all** (a store test's `tickMillis` defaults to `0`,
   so no clock tick competes for the spy) and drains the queued next hand in the same turn. This is the property `drive-duel.tsx` and four merged suites rest on, and it is
   the one to break first when checking that these tests bite.
6. **Held as constraints rather than new assertions:** `no-derivation.test.tsx` and
   `scripted-duel.gen.json` are byte-unchanged, no file under `web-client/src/e2e/` is edited, and
   `DuelTable`'s props gain no member. That the four suites need no edit is the evidence that
   nothing they prove was traded away — `ADR-0100` §3's own standard, applied to this change.

### 8. What follows from the rule as written, named rather than registered

Two things fall out of `ADR-0120` §3 arithmetically. Both are stated so no ticket invents a
different answer, and neither is a new decision:

- **A showdown reached from preflop takes 3.8 s, not 4.4 s.** §3 fixes two clauses — the non-final
  steps keep 600 ms each, and the **last** beat stands 2,000 ms — so a four-step runout is
  `600 × 3 + 2000`. The last beat *is* the held one; it is not a fifth beat added after a 2.4 s
  runout.
- **The deciding hand of a duel holds too, so the result screen arrives 2 s later.** `ADR-0102` §1's
  queue is FIFO for every frame type, `DuelFinished` included, and `ADR-0120` §3's rule is stated
  over *a hand*, with no exception for the last one. Applying it as written is what two readers
  would both do; carving out the final hand would be inventing a rule. If the product owner wants
  the result screen sooner, that is one clause added to §1's predicate and **no** change to this
  mechanism — which is why it is named here on `ADR-0105` §6's route rather than registered as a
  `DEC` nobody is working.

And one thing that is already priced elsewhere and needs no change here: a player held two seconds
starts their next decision with two of their 30 s allowance spent. `ADR-0113` §6 anchors a queued
`TurnClock` to `arrivedAt` rather than to the drain, `ADR-0120`'s *Consequences* accept the cost by
name, and the store's `armClockTick` is already independent of `armTick`. **The queue gains no
exemption**, which is `ADR-0102` §6's own resolution of the move-clock deadline it named: the clock
is anchored honestly, not let past the queue.

## Consequences

**What it buys.** `ADR-0120` §3's hold becomes four small, separately-testable pieces: a union field,
a predicate over one frame, a constant, and a mapping in the one function that already owns the
clock. The 2,000 stays exactly as cheap to be wrong about as `ADR-0102` §4 promised — moving it is
one line in `boot.ts`, and moving the classification is one line in `layOutReveal`, neither of which
touches an interface. The driver, the four recorded-frame suites and 113 store call sites keep their
behaviour with **no edit at all**, so `ADR-0100` §3's *"no e2e file is edited"* evidence survives
this change intact rather than being re-established. And the reduced-motion path is safe by
construction: there is no CSS to still.

**What it costs.**

- **A second number exists at the boot seam**, which `ADR-0102` §4 deliberately kept at one.
  `ADR-0120` already booked this cost; this ADR is what spends it. There are now two dials a future
  reader must find, and a third would be a smell rather than a pattern.
- **`stepMillis: 0` is overloaded.** It no longer means *steps are instant*; it means *the reveal has
  no clock*. That is a stronger claim living on the same value, and a caller who wants instant
  streets with a paced read beat cannot express it. Nobody wants that today, and it is called out in
  the foreclosures below rather than hidden.
- **`RevealStep` is wider than what any component consumes.** `Lobby.tsx` hands `DuelTable` a value
  with a field the prop type does not mention. That is deliberate — it is the property that keeps the
  DOM identical — but it means the widening is invisible at the call site and a reader has to look at
  `duel-state.ts` to know the field is there.
- **The hold has no browser-visible proof.** Nothing in the DOM says a beat is long, so no recorded
  frame, no screenshot and no design pane can catch a hold that silently reverts to 600 ms. The only
  guard is §7.4's `delayMillis` assertion, and it is the assertion to break first when auditing this.
- **The read beat is per-viewer, so the two tabs drift by 1.4 s**, and now the *result screen* drifts
  with them on the deciding hand (§8). `ADR-0120` accepted the first; the second is its arithmetic
  consequence and is accepted here.

**What it forecloses.** A per-beat length is now a **kind**, not a duration, so a future beat that
wants an arbitrary millisecond figure — a per-hand pace, a length that scales with how much is on
the board — cannot be expressed without changing the field's type. That is the intended foreclosure:
a duration in the reducer is precisely what would unbootable the seam. It also forecloses, for as
long as §3's gate stands, any caller booting a paced read beat with unpaced street steps; the day
someone genuinely wants that, the gate moves from `stepMillis === 0` to a per-beat zero check, which
is a one-line change with one test to update.

This is the cheapest of the candidate shapes to reverse, and that is why it was chosen while the
evidence for **2,000** is one session by the author. Every part of it is deletable in the direction
of *no hold at all*: remove the field, remove the constant, and `armTick` is the merged function
again.

## Alternatives considered

**Release the read beat on the next hand's frames arriving, with no timer at all.** The strongest
alternative, and the one a coder reading `ADR-0120` §3 literally will build: §3 says *"the hold ends
when the next hand's frames are applied, which is a **fact arriving**, not a fade and not an
expiry"*. Taken as a schedule, it needs no second number, no second option, no field, and no thought
about `drive-duel`'s zero — the last step simply never expires, and the queue drains on arrival.
Rejected because it deletes `ADR-0102` §6.2's *"the lag is monotone, bounded and self-terminating"*:
a hand with nothing behind it — the deciding hand of a duel, a rival who disconnects at showdown, a
`WAITING` room after the last hand — would stand forever, and the hold's length would otherwise be
whatever interval the server happened to leave between hands, which is 40 ms on a fast local stack
and unbounded on a slow one. §3's sentence describes what **ends** the hold in the ordinary case, not
what schedules it, and the distinction is exactly why this ADR exists.

**Express the long beat as repeated identical steps.** Genuinely elegant: `layOutReveal` pushes the
final step three or four times, the store keeps its **single** parameter, `ADR-0102` §4 stays
literally true with one number named once, `drive-duel`'s zero needs no new rule, and nothing gains a
field. Rejected on arithmetic first: 2000 ÷ 600 is 3.33, so repetition can produce 1,800 or 2,400 but
never 2,000 — it would silently amend `ADR-0120` §3's number, which is the one thing this ADR was
told to keep movable. Then on behaviour: identical repeated steps make `advanceReveal` notify with no
state change, re-rendering the table for nothing three times, and the queue would drain later than
the step array's length suggests to anyone reading it. And the number to tune would be an integer
count whose effect depends on another number — the opposite of *the cheapest thing here to be wrong
about*.

**Put an absolute duration on the step: `RevealStep.standMillis`.** The store becomes trivial —
`schedule(tick, steps[0].standMillis)` — the state is fully self-describing, and a test can read the
duration straight off `state.reveal` with no `schedule` spy, which is a real testing advantage over
what was chosen. Rejected because it moves the number out of the boot seam and into the reducer,
breaking the half of `ADR-0102` §4 that is explicitly *not* free to move. Making it bootable means
threading both durations through `applyServerMessage`'s signature to every call site; not threading
them means hard-coding 600 and 2,000 in `duel-state.ts`, at which point `drive-duel.tsx:229`'s `0`
stops working entirely, because the reducer would emit 2,000 whatever boot was told, and the four
recorded-frame suites would fail. That last consequence is concrete and decisive.

**Let the store branch on what the beat paints.** `RevealStep`, `Reveal`, `Lobby.tsx` and
`DuelTable`'s prop are then all literally untouched — a strictly smaller diff — and the rule sits
next to the clock that enforces it, which is arguably where a scheduling rule belongs. Rejected
because it puts a rule about poker inside the tick: the store would have to ask both *is the rival's
seat showing cards* and *is this the last step* on every arm, re-deriving on each tick something
settled once when the step was laid out, and duplicating structure the `steps` array already holds.
It also puts the rule where no pure test can reach it — `duel-state.test.ts` could not see it at all,
and the only possible proof would be the schedule spy, so §7.1's two-seat test would have nothing to
assert against.

**Carry the flag on the `Reveal` rather than on the step** — `Reveal.readAtEnd: boolean`, with
`armTick` asking `steps.length === 1 && reveal.readAtEnd`. One field instead of one per step, and it
reads as what it is: a property of this hand's ending. Rejected as the same fact stated twice — *this
is the last step* is already `steps.length === 1`, and pairing it with a second condition is an
invariant someone can break. Per-step, `armTick` reads the head of the queue and needs to know
nothing else, and a future beat that wants its own length gets one without a new field.

**Make the hold a `--pd-motion-*` token and drive it from CSS.** The number would live in
`design/tokens/tokens.css`, where `ADR-0024` §2 says design values are born, and the human could tune
the beat in the design pane with the rest of the vocabulary — which is a real argument, since 2,000
is a feel number and feel is the human's. Rejected on three counts, any one sufficient. It makes the
hold **motion**, and `ADR-0115` §3 says it is not one. The sheet's single
`prefers-reduced-motion: reduce` block would then still it, deleting this beat for exactly the
players who asked for less motion rather than less time — the precise inversion of what they asked
for. And it would put frame delivery behind a stylesheet: a suite with no layout would never fire the
transition, so the queue would never drain.

**Give `drive-duel.tsx` an explicit `readMillis: 0`.** Explicit beats implicit, and a reader of the
driver would see both dials at zero rather than having to know a gate exists. Rejected because it is
redundant under §3 and a redundant zero can drift out of agreement with the gate that makes it
redundant — and because it fixes exactly one of the 113 call sites that pace nothing, leaving the
other 112 relying on a rule that would then be stated in a place that no longer needed it. The gate
is what makes them all correct at once; the explicit zero would make one of them correct twice.
