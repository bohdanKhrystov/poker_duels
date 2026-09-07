# ADR-0139 — The read beat is spent once per hand, and the store already remembers

- **Status:** Accepted
- **Date:** 2026-09-07
- **Resolves:** `DEC-153` — **how does the client tell a *first* showing of the rival's hand from a
  repeat of the same hand, and is it worth remembering anything to do it?** Registered open
  2026-09-07 by the planner while splitting `STORY-1411`, against a shipped predicate that answers
  `"read"` on **every** delivery carrying the rival's hole cards, first or fiftieth.
- **Amends [`ADR-0136`](ADR-0136-a-beat-declares-its-own-length-and-zero-silences-every-beat.md) §1,
  in three sentences and in those three only** — *"`layOutReveal`'s signature does not change"*,
  *"from the view it is already given and from nothing else"*, and *"no previous view is
  remembered"*. Everything else in §1 stands verbatim, including the two reasons it gave for reading
  `view.viewerSeat` rather than `state.mySeat`, and §§2–8 are untouched. `ADR-0136` §1 also claims
  *"The predicate is `ADR-0120` §3's sentence and not a paraphrase of it"*; that sentence is
  **withdrawn**, because the predicate it shipped is the paraphrase, and the paraphrase drops a word.
- **Serves, and does not reopen:**
  [`ADR-0120`](ADR-0120-a-showdown-shows-the-hands-the-rules-showed-and-the-beat-that-shows-them-stands.md)
  §§1–2 (what a showdown shows and where it is drawn), §3 (**which** beat is long, **how long** it
  stands, and that the two seats may answer differently for the same hand), §4 (what it decides
  nothing about) and §5 (what would reverse §1). Every clause of what a player sees is that ADR's.
  This one changes **no** number, **no** string and **no** card — only *when the rule it already
  wrote is satisfied*.
- **Applies, and amends nothing:**
  [`ADR-0102`](ADR-0102-a-hand-ends-in-steps-and-the-client-owns-the-clock.md) §§1–4 (the FIFO
  queue, what a step paints, what is withheld, the boot seam's number) and §5's **decision** — a
  resuming client jumps to the end, and a hand-completing snapshot is governed by one rule with no
  special case for a resume (§8 below reconciles §5's *stated* 600 ms, which `ADR-0136` moved and
  neither ADR noticed); `ADR-0136` §§2–8; `ADR-0100` §3 (**no recorded frame is regenerated and no
  e2e file is edited**); `ADR-0115` §3 (the hold has no CSS and mints no token);
  [`ADR-0008`](ADR-0008-loser-mucks-at-showdown.md) (the loser mucks, and a mucked hand appears in no
  event); [`ADR-0002`](ADR-0002-server-authoritative.md) (the client asserts no game fact — §4);
  [`ADR-0032`](ADR-0032-react-subscribes-to-a-store-it-does-not-own.md) §§1–3.
- **Nothing crosses the socket, and no Kotlin file is opened.** No wire type, no
  `PROTOCOL_VERSION`, no `docs/protocol.md` edit, no schema, no migration, no server or engine file,
  and no new player-facing string — so `ADR-0047` §2's fingerprint cannot move and
  **`TASK-141108` is not `atomic:`**. `EPIC-14`'s *Out of scope* forbidding `poker-engine` and
  `poker-server` is honoured by construction: the whole diff is two files under `web-client/src/store`.
- **Registers no decision.** §9 explains the one thing it deliberately does *not* register, and why
  registering it would be worse than not.

## Context

### What is merged, measured rather than recalled, on `develop` at `5247ed07`

**The rule has the word *before* in it.** `ADR-0120` §3's operative sentence: *"A hand that ends
with a hand face up the viewer **has not been shown before** holds its **last** step for 2,000 ms."*
The next paragraph restates it *"as the client can evaluate it, so that no ticket invents a
different test"* — *"the hand-completing view carries hole cards for the **rival's** seat"* — and
**that restatement drops the word `before`.** `ADR-0136` §1 implemented the restatement, calling it
*"`ADR-0120` §3's sentence and not a paraphrase of it"*, and `TASK-141104` merged it. So today
`duel-state.ts:490`'s `rivalShown` answers `"read"` for any delivery carrying the rival's cards,
however many times that hand has already been on the screen.

**The frame cannot answer the question by itself, and three merged files say so.**
`PlayerView.kt:99` builds each `SeatView` with
`showCards = it.index == seat || it.index in revealed`, where `revealed` is
`revealedSeats(handEvents)` (`EventRedaction.kt:61`, called at `Addressed.kt:52`) — every seat a
`HandRevealed` has fired for **anywhere in the hand's log**. Its own KDoc says the log is the only
record of what has been shown; nothing marks a projection as the first one. `duel-state.ts:316`
lays out a reveal on **every** `view.street === "COMPLETE"`. `duel-store.ts:58` builds
`let state = initialState()` once and keeps it across sockets.

**The symptom is unreachable today, and that was measured rather than argued.** A throwaway
`poker-server` probe over 60 duels (since reverted) found **604** hand-completing snapshots, **115**
of them carrying the rival's cards, **2,220** `resumeFrames` frames — and **0** `Snapshot`s at
`COMPLETE` in a resume, **0** seats sent one hand's completing view twice. The mechanism is a chain
of four merged facts: `act` calls `advance` **in the same call** when a hand ends
(`DuelAction.kt:59`); `advance` loops until a hand is not over (`DuelProgress.kt`); `isHandOver`
**is** `street == Street.COMPLETE` (`GameState.kt:94`); and `resumeFrames` projects
`runner.hand.state` or, for a finished duel, `finishedFrames`, which carries no `Snapshot` at all
(`DuelResume.kt`). **The probe was falsified rather than trusted**: asked the same question of a
runner as it would stand if `act` did not call `advance`, the same run found **602**. The detector
works; the path is closed.

**That invariant is written in no ADR and enforced by no `require`.** `DuelRunner`'s `init` rejects
the three ways its fields can disagree and **deliberately permits a hand that is over**, because
that value is `advance`'s own argument type — `act` constructs exactly one at `DuelAction.kt:53`
before handing it on. So the client's correctness rests on a call-ordering property of a function in
a module `EPIC-14` forbids opening, which nobody working on the client can see, let alone check.

**And the record already believes the path is open.** `ADR-0102` §5, verbatim: *"A resume `Snapshot`
that happens to arrive at `COMPLETE` still takes §2's single final step."* `duel-state.test.ts`
carries a merged test of exactly that frame — *a snapshot at COMPLETE with no events before it takes
one step, not four*. Three ADRs have now been written on top of a case the server does not produce,
and none of them said so.

### What is in tension

1. **The predicate is wrong against the rule it cites, and right against the server it runs on.**
   Both halves are true at once. It is not a bug in the ordinary sense — no user can reach it — and
   it is not merely cosmetic either, because the sentence justifying it (*"not a paraphrase"*) is
   false and was reviewed as true.
2. **The severity ceiling is genuinely low.** `ADR-0136` §5 already bounds it: *"the worst this
   mechanism can do wrong is stand a beat for the wrong length."* If the path opened, a reconnecting
   player would sit through 2,000 ms instead of 600 ms, once, on a hand whose cards are correct,
   filtered by `PlayerView.of` exactly as always. Nothing is misstated, nothing leaks, nothing
   stalls — the hold ends when the next frames apply (`ADR-0120` §3). **1.4 s, once, on a
   reconnect** is the whole of it.
3. **`ADR-0136` §1 chose frame-locality for three stated reasons, and any guard breaks the letter of
   the first.** The reasons were: no cross-frame dependency to get wrong; a hand-ending `Snapshot`
   **queued** behind another classifies itself correctly when `advanceReveal` folds it back; and
   `state.mySeat`'s `null` case has no wrong answer to give. Whether those reasons survive a guard
   is the real question, and it is not answered by pointing at the three sentences.
4. **A guard is not free of its own risk.** A classification that reads state carried across frames
   can be wrong in a direction the current one cannot: it can silence a read beat that should have
   stood. That is the failure the player would actually notice, and it is worse than the one being
   repaired.
5. **Writing the invariant down is a real second answer, and its weakness is where it would live.**
   The only honest pin is a `poker-server` test, and `EPIC-14` forbids `poker-server`. A test
   deferred to a story nobody has written is a test nobody writes; ten ADRs in this repository
   already point at an `EPIC-07` that never collected them.

### The deadline

**`layOutReveal` has exactly one call site** (`duel-state.ts:318`). `STORY-1411`'s remaining tickets
are merged or startable, and `TASK-141108` is the last one; every consumer that lands after it makes
the signature more expensive to move. `ADR-0136` is one day old with one implementing ticket merged,
so narrowing three of its sentences costs one supersession clause today and a reopened story later.
This is the cheap moment, which is a reason to decide **now** — not a reason to decide a particular
way.

## Decision

### 1. The rule is the sentence with *before* in it

`ADR-0120` §3's rule is *"a hand face up the viewer **has not been shown before**"*. Its restatement
*"as the client can evaluate it"* is a **sufficient test under an invariant**, not the rule itself,
and where the two disagree the rule governs. This ADR chooses no product behaviour: `ADR-0120` §3
already decided which beat is long and how long it stands, including the word this decision is
about.

### 2. `layOutReveal` is handed the view the store already holds

```ts
function layOutReveal(
  view: PlayerView,
  streetDealt: readonly StreetDealt[],
  held: PlayerView | null,
): Reveal {
```

The `Snapshot` case passes `state.view` — which, on that path, is still the **previous** view,
because the reveal is computed from `state` while `view:` is being set from `message`:

```ts
? layOutReveal(message.view, state.pendingStreetDealt, state.view)
```

`TASK-141104`'s inline expression is lifted out **unchanged**, so it can be asked of either view:

```ts
function rivalCardsShown(view: PlayerView): boolean {
  return view.seats.some(
    (seat) => seat.index !== view.viewerSeat && seat.holeCards.length > 0,
  );
}
```

and the final step asks whether the cards are *newly* shown:

```ts
const alreadyShown =
  held !== null &&
  held.handNumber === view.handNumber &&
  rivalCardsShown(held);
steps[streetDealt.length] = {
  board: view.board.cards,
  street: view.street,
  hold: rivalCardsShown(view) && !alreadyShown ? "read" : "step",
};
```

`.some` and `view.viewerSeat` are kept for `ADR-0136` §1's own reasons: nothing depends on the
array's order or on there being two seats, and each view answers for its own viewer — which is why
`rivalCardsShown` takes a view rather than a seat, and why asking it of `held` reads `held`'s
`viewerSeat`.

### 3. Nothing new is remembered, and that is what makes the amendment narrow

`DuelState` gains no field. `state.view` already exists (`duel-state.ts:37`), is set to `null` once
in `initialState` and is written **only** by the `Snapshot` case (`duel-state.ts:302`) — nothing
clears it, and no other message touches it. So `ADR-0136` §1's *"no previous view is remembered"* is
superseded **as a statement about `layOutReveal`'s argument list, and about nothing else**: the
store's memory does not grow by one byte, and no history, no set of seen hands and no counter is
introduced. The classification stays a pure function of its arguments; it stops being a pure
function of *one frame*.

`held` names a view that was **painted**, not merely received. A `COMPLETE` view can only reach
`state.view` by being applied, applying a `COMPLETE` view always lays out a reveal, and a frame that
arrives while a reveal stands is **queued rather than applied** (`ADR-0102` §1,
`duel-state.ts:246`). There is no path by which `state.view` holds a hand-completing view the player
never saw.

### 4. The guard lives in the reducer, and that is what keeps `ADR-0136` §1's second reason

`advanceReveal` folds every queued frame back through `applyServerMessage`, in arrival order, with
the state whose `view` the first delivery already set. Placing the guard in the reducer is therefore
the *only* placement that reaches the queued-drain path; a guard in `duel-store.ts` would classify
the direct delivery and miss the replay. `ADR-0136` §1's second reason — *a hand-ending `Snapshot`
queued behind another classifies itself correctly on drain* — is not merely preserved, it is the
argument for where this goes. `advanceReveal` is **byte-unchanged** and `applyServerMessage`'s
signature does not change. Nothing here asserts a game fact: the client still paints only what a
snapshot says, and still reads a hole card **to choose a schedule and never a face** (`ADR-0136`
§5).

### 5. Two ways the guard could be wrong, closed by merged facts rather than by care

- **It cannot silence a first showing, because rival cards cannot appear before the hand is over.**
  `HandRevealed` is emitted in exactly one place — `reachShowdownAndSettle`
  (`StreetProgression.kt:131`) — which settles the hand in the same engine step. So a view carrying
  the rival's cards is a view at `COMPLETE`, and `rivalCardsShown(held)` being true for hand *N*
  means hand *N*'s completing view was already on the screen. A *mid-hand* view of hand *N* sitting
  in `state.view` answers `false` and the read beat stands — which is why `rivalCardsShown(held)` is
  load-bearing on its own and not implied by the hand comparison.
- **It cannot silence the next hand.** `held.handNumber === view.handNumber` is the whole of it, and
  without it one showdown would silence every showdown after it. No duel identifier is compared
  because none exists on the wire and none is needed: a new duel's opening hand cannot be at
  `COMPLETE` (`DuelStart.kt`'s `check(hand.state.seatToAct != null)`), so its opening `Snapshot`
  overwrites `state.view` first.

**The first bullet is this decision's one load-bearing engine fact, and it is named rather than
assumed.** If `ADR-0120` §5's all-in-and-called widening ever lands, a mid-hand view *will* carry
rival cards, and `alreadyShown` would need `held.street === "COMPLETE"` beside the hand comparison.
That clause is **not** added now: it would guard an unreachable case inside a guard for an
unreachable case, no test could make it bite, and `ADR-0120` §5 says that widening **supersedes**
`ADR-0120` rather than amending it — so §3's predicate is rewritten at that point anyway, and this
one with it.

### 6. What `ADR-0136` §1 keeps

`RevealStep.hold: "step" | "read"` is unchanged and gains nothing. The predicate still reads
`view.viewerSeat` and **not** `state.mySeat`, so §1's third reason — `mySeat`'s `null` case has no
wrong answer to give — stands untouched, and the same hand still answers `"read"` for one seat and
`"step"` for the other. A fold still answers `"step"` with no special case, because the server sends
no hole cards for a hand that folded (`ADR-0008`). Every street step is still `"step"`, and the
boards are still reached backward from the snapshot's own length, which needs no previous view at
all.

### 7. What *before* means here, stated as a limit rather than a promise

It means **before, in this store's lifetime**. A browser that reloads has forgotten, and would
answer `"read"` for a hand it already read. That limit is accepted rather than closed, for three
reasons that are not preferences: nothing on the wire records what a browser *painted*, and the
field that would is the engine change `ADR-0008`, `ADR-0120` and `EPIC-14` all refuse; `ADR-0102`
§5 already treats a reload as a jump to the end with no special case; and the case is unreachable
for the same reason everything else in this ADR is. It is a **limit**, not an open question, and it
is deliberately not registered: two competent engineers with these constraints reach the same
place, and a `DEC` for an unreachable case is noise in a register that has to stay readable.

### 8. `stepMillis === 0` is untouched, and `ADR-0102` §5 is reconciled rather than assumed

`ADR-0136` §3's gate is not reached by anything here: the four recorded-frame suites drain
synchronously at `stepMillis: 0`, no beat stands, and this change alters a `hold` value they never
read. `drive-duel.tsx` is **not** edited, `ADR-0100` §3's suites are neither edited nor
re-recorded, and the DOM is byte-identical at every length.

`ADR-0102` §5 says *"A resume `Snapshot` that happens to arrive at `COMPLETE` still takes §2's
single final step. That is 600 ms with nothing behind it in the queue, and it is left uniform rather
than special-cased: one rule for what a hand-completing snapshot does is worth more than 600 ms
saved on a reload."* `ADR-0136` moved the length of that step without noticing the sentence, and its
own front matter claims §5 *"stands exactly as written"*. Under the predicate as shipped, a resume
`Snapshot` at `COMPLETE` carrying the rival's cards stands **2,000 ms** — three times the wait §5
weighed and accepted. Under §2 above it stands **600 ms** when the store has already shown that
hand, and 2,000 ms only for a store that has not. **§5's decision is what stands: one rule, no
special case for a resume** — there is still exactly one rule, and it is `ADR-0120` §3's. Its
*stated* 600 ms is a description of a step's length before there were two, and is not re-asserted
here.

### 9. The server invariant is written down, and deliberately not pinned by a test

For the record, and as a **fact rather than a contract**: today `poker-server` delivers a
hand-completing `Snapshot` to a seat **at most once**, because `act` calls `advance` in the same
call, `advance` loops while `isHandOver`, and `resumeFrames` projects only a live hand's state or a
finished duel's outcome. `DuelRunner`'s `init` does not enforce it and cannot cheaply be made to,
because a runner holding a finished hand is `advance`'s argument type.

**No server test is registered for it and no story is created to carry one.** After §2 nothing in
the client depends on the invariant, so a test would pin a property with no consumer — and a ticket
deferred to a story outside this epic is a ticket nothing fails without. If a future change to
`DuelAction` or `DuelProgress` opens the path — the most likely being a change motivated by this
very feature, holding a settled hand before dealing the next — §2 is already correct and nothing has
to happen. That is the point of putting the guard in the client rather than a promise in the server.

### 10. What a test must prove, and `TASK-141108` stands as written

`TASK-141108` is **unblocked and implemented exactly as it is written** — two files, `92 → 95` in
`duel-state.test.ts`, both mutations, every literal in its `Scope` block reproduced verbatim. Its
three test names are what this ADR asks for:

1. `a hand-ending view delivered a second time is a step, not a second read` — two deliveries, or
   the test cannot see a repeat at all.
2. `a repeat queued behind the beat it repeats is a step when the queue drains` — **the load-bearing
   one**, because it is the delivery path §4 chose the reducer for; a guard in `duel-store.ts`
   passes test 1 and fails this.
3. `the next hand's showdown is a read, however recently the last one was` — what stops *spend the
   read once* being read as *once per duel*.

Both mutations must additionally prove with `! cmp -s` that the literal they mutate was present:
each matches **nothing** on unfixed source, so without that check both gates pass vacuously on code
that never had the guard.

## Consequences

**What it buys.** The client evaluates the rule `ADR-0120` §3 actually wrote, including the word
*before*. The classification stops depending on a call-ordering property of a module this epic
forbids opening, stated in no ADR and pinned by no test — so a future server change cannot silently
misfire the beat, and nobody has to remember a cross-module invariant to review a client ticket.
`ADR-0102` §5's outcome for a resume at `COMPLETE` becomes true again. And the fix costs no new
state: the memory it consults was already in the store.

**What it costs.**

- **`layOutReveal` is no longer a pure function of one frame.** It has one cross-frame input, and
  the property `ADR-0136` §1 was protecting is gone in letter even though its three reasons survive.
  A future change that clears `state.view` on some path — a `RoomJoined` for a different room, a
  `DuelFinished` — would silently change a hold. Nothing clears it today; that is a fact about the
  current reducer, not a rule, and this ADR does not make it one.
- **A merged, one-day-old ADR is amended by the story that implements it.** The trail now reads:
  `ADR-0120` §3 wrote the rule, `ADR-0136` §1 implemented a restatement that lost a word and called
  it the sentence itself, a reviewer passed it, and `ADR-0139` narrowed it. That is the honest
  sequence and it is worth keeping, but it costs a reader three ADRs to learn one predicate.
- **The change is unfalsifiable in the product.** No browser can reach the state it corrects, so the
  only evidence it works is three unit tests over synthetic frames — and if the guard is *wrong*,
  nothing in a running duel will say so either. A symptom no path reaches is also a symptom no path
  disproves. This is the sharpest cost here and it is not mitigated.
- **The repository now carries a client guard against a server behaviour that does not occur**,
  which a future reader may reasonably read as evidence that it does. §9 exists to stop that, and
  §9 is prose with no gate behind it.
- **One more parameter, and a KDoc that has to say two different things about the same phrase**:
  *"no previous view is needed"* is true of the boards and false of the hold.
- **A reload forgets** (§7). *Before* is scoped to a store's lifetime, and no cheaper honest scope
  exists.
- **The read beat now depends on an engine fact that `ADR-0120` §5 names as the most likely thing to
  change** (§5). It is named, not guarded.

**What it forecloses.** It forecloses the server-side shape — a `PlayerView` field, a `Snapshot`
flag, or any wire mark saying *this is the first send*. That is the only construction under which
*before* survives a reload, and it is now settled as unnecessary rather than merely unbuilt; anyone
reopening it is reversing this ADR, not filling a gap. It also forecloses making the invariant in §9
structural (a `require` in `DuelRunner.init`), which would need `advance` to take something other
than a `DuelRunner` — a server refactor with no remaining consumer once §2 lands.

## Alternatives considered

**Leave the predicate and write the invariant down instead — pin the server sequencing with a test,
so a future change to `DuelAction` cannot silently open the path.** Its strongest case: the symptom
is *measured* unreachable, 0 in 2,220, with a falsified detector; the harm ceiling if it ever became
reachable is 1.4 s once on a reconnect; the shipped predicate is genuinely correct on the server it
runs on; and this route keeps `ADR-0136` §1's frame-locality — a real property, chosen for real
reasons, in the one reducer where cross-frame state is easiest to get wrong. It is also strictly the
cheaper of the two to reverse, because it changes no code. **Why it lost:** the pin has nowhere to
live. `EPIC-14` forbids `poker-server`, so the test defers to a story nobody has written, and this
repository already has ten ADRs pointing at an epic that never collected them — leaving prose with
no gate, which is precisely the failure this decision was raised to name. And it leaves the client
correct-by-luck: correctness that depends on a module the client's own epic may not open cannot be
checked by anyone reviewing a client ticket, and `ADR-0102` §5 shows the record already forgetting
which way round the dependency runs. The deciding asymmetry is that the guard costs **no new
state** — `state.view` is already there — so the thing §1 was protecting is not actually spent.

**Both: the guard *and* a server test.** Strongest case: defence in depth, and the invariant is worth
knowing regardless of who relies on it. Rejected because once §2 lands nothing depends on the
invariant, so the test pins a property with no consumer, and it would still have to be deferred
outside `EPIC-14`. A deferral that nothing fails without is worse than an honest note, which is what
§9 is.

**Put the guard in `duel-store.ts` and leave the reducer frame-local.** Strongest case: it keeps
`ADR-0136` §1 literally intact — `layOutReveal`'s signature would not change, and the store is
already the layer that owns lifetimes across sockets. Rejected on a mechanism, not a preference:
`advanceReveal` folds queued frames back through `applyServerMessage`, so a hand-completing
`Snapshot` **queued behind the beat it repeats** never passes through the store's classification at
all. `TASK-141108`'s second test is the one that says so.

**Remember a set of hands already read, rather than consulting `state.view`.** Strongest case: it
says exactly what it means, needs no reasoning about what `state.view` happens to hold, and is
immune to §5's engine dependency. Rejected because it is the only option here that actually grows
the store's memory — a set that must be cleared on a new duel, with no duel identifier on the wire
to key it (`TASK-141108` names this) — and because it buys nothing over one hand number: a client
never sees hand *N* again after hand *N+1* begins.

**Mark the first send on the wire — a `PlayerView` or `Snapshot` field saying *these cards are newly
shown*.** Strongest case: it is the only construction under which *before* is true across a reload,
and it puts the fact where the authoritative party is. Rejected on three merged constraints, none of
them mine to overturn: `EPIC-14` forbids opening `poker-engine`; `ADR-0120` moved no wire and took
`poker-server` out of item 2a; and the server knows what it **sent**, never what a browser
**painted**, so the field would answer a different question than the rule asks. If it were ever
wanted it would be a decision for the product owner about what a player is promised, not a mechanism
choice.

**Change `ADR-0120` §3's rule to match the shipped predicate — drop *before* from the rule instead
of adding it to the code.** Strongest case: it is the smallest possible diff, it makes a merged
predicate correct by definition, and *every* showing of a rival's hand holding for 2 s is a
defensible product rule in its own right. Rejected because it is **not this agent's to make**: what
a player is shown, and for how long, is the product owner's, `ADR-0120` §3 decided it deliberately,
and no technical argument produces that answer. Reaching for it would have been the cheapest way to
close `DEC-153` and the most expensive thing this run could have produced.
