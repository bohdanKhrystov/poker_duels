# ADR-0075 — The server's mark lives as long as the absence that produced it

- **Status:** Accepted
- **Date:** 2026-08-24
- **Resolves:** `DEC-070` — how long does the most recent *action the server took* stay on a player's
  screen, and what takes it off?
  [`ADR-0046`](ADR-0046-the-table-says-away-timed-out-and-back.md) §4 settles the six sentences and
  settles **which** mark is shown — *"showing the most recent one is enough"* — and says nothing
  about its lifetime.
- **Where the answer came from:** [`docs/vision.md`](../vision.md), *Positioning* — *"The reference
  points are **Lichess** and **Chess.com**, not PokerStars. Dark, quiet, fast, minimal."* That is
  the sentence `ADR-0046` derived its whole string set from, and it is what decides between a line
  that sits still and updates itself and one that blinks, fades or has to be dismissed. `ADR-0046`
  §2 has already applied it once in this corner of this screen: *`Your rival is back.` clears on the
  next `Snapshot` and on nothing else — never on a timer, never on a fade.* This ADR applies the
  same sentence to the fourth case and adds no commitment `docs/vision.md` does not already make: it
  is one key in a reducer case, no string, no frame, no stored data.
- **Two of the four candidates were eliminated on the server's own code, not on taste.** `Snapshot`
  and `YourTurn` are both emitted in the *same delivery* as the mark they would clear — see
  *Context*. That half of the answer is a fact anybody can check in
  `poker-server/src/main/kotlin/duels/poker/server/duel/`, and it is recorded here so the next
  reader does not re-open it.
- **Builds on:** `ADR-0046` §2 (a presence line clears on a frame, never on a clock) and §4 (the
  most recent mark, and no action log);
  [`ADR-0028`](ADR-0028-the-wire-names-an-absent-opponent.md) §3 (the client acquires no clock it
  was not given) and §5 (which frame reaches whom);
  [`ADR-0043`](ADR-0043-a-rejection-closes-no-decision-point.md) §3 (the precedent this rejects, and
  why it does not transfer).
- **Constrains:** [`TASK-031314`](../../tasks/tasks/TASK-031314-the-store-keeps-the-most-recent-action-the-server-took.md)
  — the reducer keys that clear the field and the tests that pin them — and
  [`TASK-031315`](../../tasks/tasks/TASK-031315-the-duel-screen-names-the-server-as-the-actor.md),
  which gains the one screen-level assertion that the two sentences are never on screen together. It
  constrains no Kotlin, no wire type, no protocol version and no stored data.
- **Leaves open:** the store has no room or duel boundary of its own — `rivalPresence`,
  `graceRemainingMillis` and `rivalReturned` are cleared by nothing at either — and this ADR bounds
  one field without fixing the general hole (*Consequences*). That is a defect and a ticket, not a
  decision.

## Context

`ADR-0046` §4 gives the fourth case six sentences and one placement rule, and stops. Everything
about *when the sentence appears* is settled; nothing about when it goes. Six things are in tension,
and two of them are facts about the server rather than opinions about the screen.

**The mark has nothing to hang on.** `DuelState.narration` accumulates every `GameEvent` and no
component reads it, so there is no rendered action for the mark to annotate. `ADR-0046` recorded
that as a cost it was creating — *"the mark needs a home the client does not have"* — and
`TASK-031315` gives it one: a free-standing line under the table. A line that annotates nothing has
no natural end. Something has to be chosen.

**Two of the four candidates put the sentence on screen for no readable time at all.** This is the
decisive fact and it is checkable in three files:

- `AbsentSeats.kt` returns `listOf(Addressed(0, mark), Addressed(1, mark)) + next.outbound` — the
  mark is *prepended* to the frames its own action produced, exactly as `ADR-0028` §4 promised.
- `next` is what `DuelAction.act` returned, and `act` composes its outbound through `framesFor`,
  which is `broadcast(state, newEvents, handEvents) + listOfNotNull(turnFor(state, handEvents))`.
- `broadcast` carries the comment *"Always emit Snapshot frame, which is the authoritative last word
  on state"* and does exactly that, for **every** applied action, to **both** seats.

So the mark and the `Snapshot` describing the mark's own action are consecutive frames in one
delivery, and `turnFor` puts a `YourTurn` for the seat now on turn — the present player, once
`foldAbsent`'s loop has given up every absent turn — in the same delivery. *Clears on the next
`Snapshot`* and *clears on the next `YourTurn`* therefore both mean **the mark is cleared
microseconds after it is set**, and `ADR-0046` §4 would be implemented, tested, and never read by a
human being. `ADR-0043`'s precedent is real but it does not transfer: `rejection` clears on the next
frame that reports state because a rejection is answered by the `Rejected` frame *alone*, and
`ADR-0046` §2's `Your rival is back.` clears on the next `Snapshot` because a `resume` sends the
seat that stayed a presence frame and **no** `Snapshot` (`ADR-0028` §5). The mark is the one line of
the three that arrives welded to a `Snapshot`.

**While the seat is absent the mark refreshes itself.** `foldAbsent` gives up *every* turn that
reaches an absent seat, so during an absence a new mark replaces the old one at nearly every
decision point. The most recent mark is not a notice about something that happened once; it is a
status field with a bursty update rate, and it can only go stale when the updates stop.

**Two true sentences can read as one false one.** The mark is past tense — *The server folded for
your rival.* — and it never stops being true; the server did fold. The presence line is present
tense. Put *The server folded for your rival.* under *Your rival is back.* and the reader is not
told a falsehood by either sentence; they are told one by the pair. That is the failure `DEC-070`
was raised on and it is the thing the answer has to make impossible, rather than unlikely.

**The mark can name this client's own seat, and nothing on the wire says that seat came back.**
`ADR-0046` §4 writes *The server folded for you.* because `ActedForAbsent` goes to both seats and
names the seat it is about. But `OpponentPresence` is recipient-relative and carries no seat
(`ADR-0028` §1), so the wire has a frame meaning *your rival is present again* and no frame meaning
*you are*. Any lifetime expressed as "until the seat the mark names is present" is only expressible
for one of the two seats.

**"Never clears" is not bounded by the duel.** `duel-store.ts` builds the state once — `let state =
initialState()` — and nothing resets it inside a page load. A `Snapshot` after a rematch clears
`outcome` and brings the table back (`ADR-0044` §4), so a mark that nothing removes reappears on the
first hand of a duel the absence had nothing to do with. The only way out of a room today is
`DuelResult.tsx`'s `<a href="/">`, a real navigation that rebuilds the store — so the boundary that
matters is the duel's, not the room's.

### The deadline

Nothing is stalled. `TASK-031301` is startable and the other twelve presence tickets run without
this; `TASK-031314` and `TASK-031315` are the last two of fifteen in a single chain.

The reason to answer now is the same one `ADR-0046` gave for the words: **the moment
`TASK-031315` merges, the behaviour is pinned by tests.** Deciding after the rendering ships means
changing a reducer, a component and the assertions that quote them. Deciding now costs two keys in
two case bodies. Neither argues for a particular answer.

## Decision

### 1. The mark is a status, not a notice

The most recent action the server took stays on screen for as long as the server is still the thing
acting for that seat. It is the companion to the presence line, not an event of its own: the
presence line says the server *is* acting, the mark says *what it last did*. They arrive together
and they leave together.

### 2. Exactly two frames take it off

- **An `OpponentPresence` carrying `PRESENT`.** The server has stopped acting for the rival's seat,
  so the last thing it did is history.
- **A `DuelFinished`.** The duel it happened in is over, and nothing more will be acted in it.

In `web-client/src/store/duel-state.ts`, the `OpponentPresence` case `TASK-031303` writes gains one
key,

```ts
      serverAction: message.presence === "PRESENT" ? null : state.serverAction,
```

and the `DuelFinished` case gains

```ts
      serverAction: null,
```

**Every other frame leaves it exactly as it was** — `Snapshot`, `Events`, `YourTurn`, `Rejected`,
`RematchOffered`, `Failure` and `RoomJoined`. In particular a `Snapshot` does not clear it, for the
reason `Context` gives, and neither does the arrival of a new hand.

### 3. It clears on the frame, not on a transition

Unlike `ADR-0046` §2's `Your rival is back.`, this needs no bookkeeping about what the client held
before. A `PRESENT` arriving at a resuming client whose rival never left still clears the mark, and
that is correct rather than merely harmless: the mark's clearing is about a **state** — whether the
server is acting for that seat — while a *return* is a **transition**, which is why `rivalReturned`
exists and why the mark does not need it.

### 4. Nothing else takes it off — no timer, no fade, no dismiss control

`ADR-0028` §3 keeps the client off clocks it was not handed and `ADR-0046` §2 already refused a
timer and a fade for the neighbouring line. A dismiss control is refused too: it is furniture the
design has not been asked for, it makes one sentence the player's to manage, and it would need its
own rule for what happens when the next mark arrives after a dismissal.

### 5. The words do not change

`ADR-0046` §4's six sentences stand exactly as written. This ADR adds no seventh string, removes
none, and says nothing about placement, order or colour — `EPIC-06`'s, as before.

### 6. The frame stays whole in the store

`TASK-031314` keeps the whole `ActedForAbsent` rather than a rebuilt object, and this decision does
not touch that. `(handNumber, actionSequence)` is what a later action log would attach the mark by
(`ADR-0028` §4), and keeping the coordinates is what makes §5 of *Consequences* a re-shaping rather
than a re-derivation.

## Consequences

**What it buys.** The failure `DEC-070` names becomes impossible by construction rather than
unlikely: the single frame that puts `Your rival is back.` on screen is the same frame that takes
the mark off, in the same reducer call, so the two sentences cannot be co-present for one render.
The mark is legible — it survives the `Events`, `Snapshot` and `YourTurn` that its own action
produced, which is the only way `ADR-0046` §4 reaches a person at all. And during an absence the
line behaves the way the rest of this screen behaves: it sits still and changes when the server says
something, never on a clock.

**What it costs.**

- **A mark can be older than the hand on screen.** The rival is absent, the present player is on the
  button, they fold pre-flop and the hand ends without the turn ever reaching the absent seat
  (`duel-rules.md`: *the button is the small blind* and *acts first before the flop*). Three hands
  later the line still reads *The server checked for your rival.* The sentence is past tense and
  still true, and the presence line above it still says the server is acting for that seat — but
  nothing on screen ties the mark to the hand it belongs to, and a reader may take it as describing
  the hand they are looking at. **This is the cost being chosen.** It is the price of `ADR-0046`
  §4's *no action log*: a sentence with no anchor cannot say which decision point it is about, and
  the alternative that would fix it is rejected below on flicker.
- **A mark naming this client's own seat has no clearing frame but `DuelFinished`.** There is no
  *your own seat is present again* on the wire. A client can only be holding such a mark by having
  had a live writer at the instant the server acted for its own seat — the reconnect race
  `The server folded for you.` exists for — so it is already history when it first renders and it
  stays for the rest of the duel. Named here rather than left to be discovered, and deliberately
  **not** answered by asking for a new frame: a wire change for a race whose whole window is one
  delivery is not worth a protocol step, and `ADR-0028` §6 already declined the journal that would
  make the case general.
- **`serverAction` clears on `DuelFinished` for a reason that is not about the duel ending.** It is
  a boundary guard, not a statement about absence: at `DuelFinished` the result screen has replaced
  the table and the mark renders nowhere either way. It is there so that no mark can survive into a
  rematch, without that correctness depending on a three-step argument about the server always
  sending `PRESENT` before a rematch can be offered. A future reader looking for why the field is
  touched in that case will find this paragraph and nothing more satisfying.
- **The store still has no room boundary, and this fixes one field of four.** `rivalPresence`,
  `graceRemainingMillis` and `rivalReturned` are cleared by nothing at a duel or room boundary. The
  hole is unreachable today because the only way back to the lobby is a real navigation that rebuilds
  `initialState()`. The day a client-side route replaces it — `DEC-054` — every presence field
  crosses into the next room and `serverAction` will be the only one that does not. That asymmetry
  is the honest cost of answering one decision rather than four; it is one ticket against `EPIC-03`
  when `DEC-054` lands, and it is not this decision's to take.
- **Two more clearing rules to keep in step.** `ADR-0046` §2 has `Your rival is back.` clearing on a
  `Snapshot`; this has the mark clearing on a presence frame and on `DuelFinished`. Three lines in
  one corner of one screen now have three different lifetimes, each for a stated reason, and nothing
  mechanical keeps the set coherent except this paragraph and §2 of the ADR before it.

**What it forecloses.** Almost nothing, and cheaply — the whole decision is two keys in two case
bodies and a revert is a one-line change with no migration, no schema and nothing a player keeps.
What it does close off deliberately is the *shape*: a client built on "the mark is a status that
lives as long as the absence" is not a client that can grow a per-decision-point annotation by
rewording. If an action log ever ships, the mark should attach to its line by
`(handNumber, actionSequence)` and take that line's lifetime, which supersedes this ADR rather than
extending it. §6 keeps the coordinates in the store so that day is a re-shaping and not a
re-derivation.

## Alternatives considered

**1. It never clears.** The strongest case, and it is a serious one: it is the cheapest thing in the
repository — no reducer branch, no test, nothing to get wrong — and the sentence is past tense, so
it is never actually false. `ADR-0046` §2 says in as many words that *"a line that outlives the
moment costs a player nothing"*, about a line in the same corner of the same screen, so the
precedent is not merely available, it is adjacent. Rejected because that sentence was written about
`Your rival is back.`, which nothing on screen contradicts, and this line has something to
contradict. Two true sentences in different tenses about the same person read as one false one, and
that pairing is the whole of `DEC-070`. It also fails to be bounded by anything at all: `duel-store.ts`
builds state once per page load, so the mark would survive `DuelFinished` and reappear on the first
hand of a rematch whose rival was present throughout.

**2. It clears on the next `Snapshot`, as `ADR-0043` clears a rejection.** The strongest case is the
precedent, which is exact and has already shipped twice: `ADR-0043` §3 clears `rejection` on the
next frame that reports state, and `ADR-0046` §2 clears `Your rival is back.` on the next `Snapshot`
and on nothing else. One key, one test, and a mark that can never outlive the action it describes.
Rejected on the server's own code rather than on judgement — `AbsentSeats.kt` prepends the mark to
`next.outbound`, and `act` → `framesFor` → `broadcast` puts a `Snapshot` in that outbound for every
applied action, to both seats. The mark and the `Snapshot` about the mark's own action are
consecutive frames in one delivery, so this clears the mark the instant it is set. The precedent
holds for the other two lines precisely because neither arrives welded to a `Snapshot`.

**3. It clears on the next `YourTurn`.** Its strongest case is semantic sharpness: `ADR-0043` §6
holds `YourTurn` to exactly *a new decision point has opened*, so it is the cleanest available edge
for *the table has moved on*, and it is the one frame that asks the player to do something rather
than telling them something — the natural moment to stop explaining the last thing that happened.
Rejected on the same source fact, and it is worse than option 2 rather than better: `framesFor` is
`broadcast + turnFor`, so the `YourTurn` prompting the present player usually rides in the same
delivery as the mark — but not always, because when the mark's fold ends the hand and the new hand's
first turn belongs to the absent seat, `foldAbsent` loops and the present player's prompt comes
later. A lifetime that is zero on some actions and most of a hand on others is not a rule a reader
can hold in their head, and it would be pinned by tests before anybody saw it fail.

**4. It lives for the hand it happened in** — cleared when a `Snapshot` carries a `handNumber` other
than the mark's. Its case is real and it was close. `PlayerView.handNumber` is already on the wire
the client decodes, `(handNumber, actionSequence)` is exactly what `ADR-0028` §4 put in the frame so
a client could place the mark, and it is the **only** candidate that removes the residual cost this
decision accepts: a mark can never be older than the hand on screen. Rejected on two counts. It does
not answer the question that was asked — a rival returning mid-hand leaves the mark standing beside
`Your rival is back.` until the hand ends, so it would have to be added to the presence rule rather
than replace it. And alone it flickers: during a long absence the server acts in most hands, so the
line would blank at every hand boundary and return a moment later, which is the opposite of *quiet*.
If the residual turns out to matter in front of a real player, this becomes one more condition in
the same case body.

**5. A timer, a fade, or a dismiss control.** Its strongest case is that it is the only option that
bounds the sentence by something the player can feel rather than by a frame they cannot see, and it
is what every other client in this genre does — a toast that appears, is read, and goes. Rejected on
`ADR-0028` §3 and `ADR-0046` §2 together: the client acquires no clock it was not given, and the
neighbouring line was already settled as clearing *never on a timer, never on a fade*. A second line
in the same corner behaving differently would make both look arbitrary. The vision's *Positioning*
sentence closes it — an animation and a control whose only job is to make a sentence go away are
furniture, and this epic has deferred every other piece of motion on this screen.

**6. `PRESENT` alone, without `DuelFinished`.** Its case is minimality, which is not nothing: a
rematch cannot begin unless both seats offer, an absent seat offers nothing, and a rival who returns
to offer one sends this client a `PRESENT` first — so `PRESENT` alone is *already* sufficient, and
the second key is redundant. Rejected because the sufficiency is a three-link argument through
`ADR-0044`'s rematch rules and `ADR-0028` §5's emission table, and it would be re-derived by every
future reader who wonders whether a mark can cross into the next duel. One key in a case body buys
the guarantee locally, and it is the same key that bounds the *for you* mark that `PRESENT` cannot
reach at all.
