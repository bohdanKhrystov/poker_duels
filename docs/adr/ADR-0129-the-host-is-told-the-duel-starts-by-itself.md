# ADR-0129 — The host waiting alone is told the duel starts by itself, and the arrival stays silent

- **Status:** Accepted
- **Date:** 2026-09-06
- **Resolves:** `DEC-141` — **does the host-alone table say, in words, that the duel starts by
  itself when the rival joins — and in whose words?** Raised 2026-09-06 while splitting
  [`EPIC-14`](../../tasks/epics/EPIC-14-the-name-the-showdown-and-the-fit.md) item 3b into
  [`STORY-1404`](../../tasks/stories/STORY-1404-the-waiting-table-copies-its-link-and-says-the-duel-starts-itself.md),
  from the human's annotation beside the `Waiting for your rival` bar after playing the product on
  a laptop and an iPhone: *"dual will start automatically when rival joins"*.
- **Where the answer came from.** The annotation is the **source, not the specification** —
  [`ADR-0110`](ADR-0110-creating-a-duel-seats-the-host-at-the-table.md)'s own framing of this
  human's feedback about this same surface — so both halves are derived, and marked separately:
  - **That the sentence exists is derived** from [`docs/vision.md`](../vision.md)'s first success
    condition — ***"Send a link. She opens it in a browser. We play a full heads-up match. Someone
    wins. We hit Rematch."*** There is no step in that sentence between sending the link and
    playing, and a host who does not know the duel self-starts is sitting in one: waiting for a
    press that never comes. Weighed against *Positioning* — *"Dark, quiet, fast, minimal."* —
    which is a discipline against accretion, not a vow of silence where silence misinforms.
  - **That it is written in the product's words rather than the human's is derived** from the same
    *Positioning* sentence's vocabulary list — *"challenge, **duel**, rematch, **rival**, streak,
    season"* — applied exactly as `ADR-0110` §2 applied it to this annotation's neighbour, on this
    surface, four days ago.
- **Amends `ADR-0110` §6 by exactly one string**, through §6's own named route: *"A state that
  needs one more string has outgrown this decision, and the answer is a new ADR, not an invented
  sentence."* Nothing else in §6 moves — the enumeration stays exhaustive at its new size, and the
  way out of it is still a new ADR.
- **Upholds, and reopens none of:** `ADR-0110` §7 — **the arrival is the `Snapshot` and it is
  silent** (§6 below) — and §§1–5 and §8; `ADR-0073` §§1–3's two promises, byte-unchanged;
  [`ADR-0022`](ADR-0022-the-room-code-is-the-invite.md) (nothing is said *about* the code);
  [`ADR-0072`](ADR-0072-a-tab-remembers-its-room-until-the-player-leaves-it.md) §6 (no client clock
  against a server window); [`ADR-0002`](ADR-0002-server-authoritative.md) with `ADR-0110` §3 (no
  game fact before the `Snapshot`);
  [`ADR-0094`](ADR-0094-opening-the-invite-is-taking-the-seat.md) §1 (the rival never sees this
  state, so this sentence is spoken to exactly one person).
- **Applies:** [`ADR-0024`](ADR-0024-design-follows-the-code-workflow.md) §3 and
  [`ADR-0091`](ADR-0091-design-gets-no-agent-a-new-screen-owes-a-card.md) §2 — the sentence's exact
  glyphs and its place on the table are the card's and the human's, and the card comes first.
- **Constrains:** `STORY-1404`'s design ticket and every ticket under it;
  `web-client/src/table/WaitingTable.tsx`; and the two merged string-set tests that pin this state
  (`web-client/src/lobby/Lobby.test.tsx:1296`, `web-client/src/table/null-view.test.tsx:203`). It
  constrains **no Kotlin, no wire type, no protocol version and nothing stored**.
- **Registers no new decision, and decides nothing about:** `DEC-140` (the copy control in a
  browser with no Clipboard API); the `You` on the host's own plate, which `ADR-0110` §§2–3 already
  make optional and `STORY-1404` treats as decision-free; `DEC-111` and `DEC-139` (how many rooms a
  host may hold, and what a second press of *play again* does); and the room's `WAITING` lifetime,
  which §3 forbids this sentence to mention in either direction.

## Context

**The state is made of things the host does, and nothing on it says the host is done.** Measured on
`develop` in `WaitingTable.tsx` and `InvitePanel.tsx`, the host alone at the table sees:
`Waiting for your rival` at the empty seat; the bare room code; `Invite link` and the selectable
box; `Copy the link` where a clipboard exists; `Back to the lobby`; `ADR-0073` §3's promise; and
`You`. Three of those are acts the host performs, one is an act that leaves, one states the
situation, one states what happens if they walk away. **None of them says what ends the wait, or
that ending it needs nothing from the host.** The only control on this screen that does anything is
the one that abandons it.

**The absence of a control is not a statement.** `ADR-0110` §7 chose silence at the *arrival* on the
sound argument that facts state themselves better than copy does — the seat fills, the cards are
dealt. That argument holds because the arrival is an **event a person watches happen**. The
self-start is a **future behaviour on a still screen**: there is no event to state it, and a missing
`Start` control is indistinguishable from one that has not rendered yet, one that appears when the
rival is there, or a page that is broken. Silence about an act says nothing; it leaves the reader to
guess, and the guess every other online card product trains is *press something*.

**`ADR-0110` predicted this friction, named it as a cost, and named this route.** Its
*Consequences*: *"An empty table can read as a broken one… §2's line and the invite are the whole
explanation, and §6 forbids adding a soothing sentence without a new ADR. That friction is bought on
purpose — it is what keeps this surface from accreting copy — but it is friction, and it will be
felt the first time someone wants the table to say more."* It was felt four days later, by the only
person who has ever played this product, on two real devices. This ADR is that prediction coming due
through the door §6 left open, not a discovery.

**The claim would be true, and it is worth having checked.** `Room.kt:212–223`: a `join` on a
`WAITING` room returns `Seated` with `state = RoomState.PLAYING` and
`match = MatchState.start(format, openingButtonSeat)` in one expression. `RoomRegistry.join` then
draws the opening hand's seed, attaches the runner and clocks the opening decision **inside the same
critical section**, and the frames that hand produced travel out on `JoinResult.Seated.outbound`.
There is no host input anywhere on that path, and no state between `WAITING` and a dealt hand. A
sentence saying the duel starts by itself describes the shipped server exactly.

**The vision pulls both ways, and one of the two pulls harder here.** *Quiet* and *minimal* are why
this state has ten strings and not thirty, and every string is permanent furniture. But the first
success condition is a sentence about there being **nothing** between the link and the match, and a
host hunting for a button is something between.
[`ADR-0119`](ADR-0119-the-name-is-asked-at-the-first-press-and-skipping-plays.md), merged earlier
today, accepted **one** interstitial in a product whose positioning sentence says *fast*, and named
that as a cost rather than hiding it; the same trade is available here at a much smaller price,
because a sentence is not a screen.

**The words are already a settled kind of question.** The annotation reads *"dual will start
automatically when rival joins"*. `dual` is not a word this product uses; *duel* is its own name,
fixed in the vision's vocabulary list. `ADR-0110` §2 already normalised the same human's *"waiting
for oppent"* to `Waiting for your rival` on this exact surface, for this exact reason, and recorded
what being overruled would cost.

**The evidence is one person, on one evening.** That is an argument for the shape that is cheapest to
undo — one string, in one component, in one state — not an argument for leaving the question open.

### The deadline

`STORY-1404` is blocked, and its first ticket is the design card (`ADR-0091` §2, `ADR-0110` §8): the
four host-alone frames at `design/screens/duel-table.html:491, 520, 548, 576` are redrawn **once**,
carrying whatever this decision and `DEC-140` answer. Deciding after the card is drawn costs the
drawing twice — `ADR-0110` §8's own argument, in miniature. Nothing else is urgent: no wire moves, no
schema moves, and the state already ships.

## Decision

### 1. The host-alone table says it, in one sentence

The state `state.roomCode !== null && state.view === null` renders **one** sentence stating that the
duel begins by itself when the rival arrives. It renders **once**, in that state only.

### 2. What the sentence must convey — two facts, and a test for whether it does

1. **The rival's arrival is what starts the duel.** The wait ends when they join, and that is the
   whole of the trigger.
2. **Nothing is required of the host.** No press starts the duel, and no control to start it is
   coming.

The test a candidate string has to pass, and the test the card is reviewed against: a host reading
this surface for the first time can answer *"is there anything I still have to do?"* with *no*, from
this string alone. A sentence that only restates that the host is waiting fails it —
`Waiting for your rival` already says that, and saying it is what produced this question.

### 3. What the sentence may not carry

A closed list, each item a merged rule rather than a matter of taste:

- **No duration, countdown, deadline or expiry**, in any wording — and equally no implication that
  the room waits *forever*, or *however long it takes*. The `WAITING` lifetime is the server's and
  `DEC-139`'s; the client owns no clock against it (`ADR-0072` §6).
- **Nothing about the code** — no validity, liveness or correctness claim (`ADR-0022`'s no-oracle
  rule). `ADR-0073` §3's shipped line remains the only thing said about the room's state, in its own
  words.
- **No game fact** — no stack, blind, card, pot, dealer button, hand number or amount
  (`ADR-0110` §3, `ADR-0002`).
- **No control named, present or absent.** This product has no start control; naming one in order to
  say it is not needed puts a name on a thing that does not exist, and invites a reader to look for
  it.
- **Nothing about what the rival sees, does or is told.** The rival never reaches this state
  (`ADR-0094` §1).
- **No instruction.** It states what the product does. It does not tell the host to wait, to stay, or
  to keep the tab open — the last of which `ADR-0073` §3 already contradicts by promising the
  opposite.

### 4. It is written in the product's voice; the exact glyphs and the place are the card's

The sentence is not the annotation transcribed. It calls the second player what this surface and the
vision already call them — **rival** — and it calls the thing that starts a **duel**; never
*opponent*, never *game*, and never a gambling noun. That is `ADR-0110` §2's normalisation applied a
second time, to the same annotation, for the same reason.

Its exact string is fixed by `STORY-1404`'s design card, drawn first and approved by the human
(`ADR-0024` §3 — taste is the human's, given by looking at the rendered card). Where it sits on the
table is the card's too. The human's annotation put it on the `Waiting for your rival` bar; a card
that puts it elsewhere is departing from the report and owes its reason on the card. The two merged
string-set tests take their literal **from the merged card**, not from this ADR.

### 5. `ADR-0110` §6's set gains exactly one member and stays closed

The enumeration is amended by this sentence and by nothing else. It remains exhaustive, and its rule
is unchanged and unweakened: a state that needs one more string has outgrown the decision, and the
answer is a new ADR, not an invented sentence. This ADR is that route being used once. Using it is
not evidence that the fence has moved.

The set's **total** is deliberately not restated here, because `STORY-1404` also removes `You` —
which `ADR-0110` §§2–3 permit without an ADR, since the host's seat *may* carry its shipped name.
This decision adds one string and removes none.

### 6. The arrival stays silent, and the sentence leaves with the state

`ADR-0110` §7 stands in full. When the rival joins, the seat fills and the hand arrives, and **no
announcement string is added**. This sentence goes out with the state that carried it, exactly as the
invite, the empty seat's line and the way back do: it is not replaced by a second string, it does not
become a status that changes, and it never renders on the live table. What it promised is confirmed
by the thing happening — the form of confirmation §7 preferred to a sentence, and this ADR does not
disturb it.

## Consequences

**What it buys.** The host knows the wait ends without them, on the one screen where they cannot work
it out from anything present. The change is one string in one component: no wire, no Kotlin, no
schema, no protocol version, nothing stored. Undoing it is deleting the string, restoring two test
arrays and redrawing four frames — the cheapest shape this answer has, which is the right side to err
on when the evidence for it is one person, once.

**What it costs.**

- **The zero-new-strings property of this state is spent.** `ADR-0110` §6 was a fence built four days
  ago, and this is the first thing over it. The next request for a sentence on this surface will cite
  this ADR as evidence that the fence moves. §5 restates the rule, but a rule that has just been
  amended is weaker than one that never has, and pretending otherwise would be the dishonest half of
  this decision.
- **This is the first string on this surface that explains rather than states.** Every other one is a
  fact, a label or a control. The line between explaining and soothing now has to be held by whoever
  reviews the card, and there is no instrument for it. §3's closed list is what stands in for one,
  and a closed list is a weaker guard than an exhaustive enumeration was.
- **The sentence is a promise about behaviour that nothing enforces.** It is true because
  `Room.join` starts the match on the guest's seating and `RoomRegistry.join` deals in the same
  critical section. If a later decision ever puts anything between the rival's arrival and the first
  deal — a ready step, a format choice, an agreement of any kind — this string becomes false in a
  file nobody would think to open, and no test couples the two. That is the same unenforced coupling
  `ADR-0073` and `ADR-0110` §4 already carry on this surface; it now carries a third string.
- **Two merged tests move and the card is redrawn.** `Lobby.test.tsx:1296`'s six strings become seven
  and `null-view.test.tsx:203`'s `BASELINE` gains a member; both move deliberately, with no assertion
  weakened, as `STORY-1404` already requires. The four host-alone frames are redrawn once, carrying
  whatever `DEC-140` also lands.
- **This ADR pins no literal, so the card is a hard dependency.** `ADR-0110` §2 could fix a string
  byte-for-byte because it was **relocating** a shipped one; this one is new, and its length,
  register and line breaks are inseparable from where it is drawn. Until the card merges, no ticket
  under `STORY-1404` can write the assertion. That serialisation is accepted because the card is the
  story's first ticket in any case.

**What it forecloses.** No host-side start control is created here, and none is asked for: a press
that begins the duel would make the rival wait at a table that is not running, which `ADR-0094` §1
refuses by name, so it is a new ADR against merged behaviour rather than a card revision. The arrival
stays silent until an ADR says otherwise, and this one deliberately does not. And the sentence may
never grow a clause about time — if this product ever wants to say how long a room lives, that is
`DEC-139`'s ground and `ADR-0072` §6's, not this string's.

## Alternatives considered

**Silence: keep the state's string set exactly as `ADR-0110` §6 closed it.** Its strongest case: the
enumeration was deliberate, argued and four days old; §7's reasoning — that facts state themselves
better than copy — is a good argument that plainly generalises; the host has just been handed an
invite and a link, which is a strong hint that sending it is the whole job; every string is furniture
forever, and the cheapest one is the one not written. It is also the only option needing no card and
no test churn. Rejected because the inference it relies on is not available from the surface. The
absence of a start control is not a statement — it is an absence, and an absence is what a
half-rendered page, a not-yet-enabled button and a deliberate design all look like. §7's silence
works because the arrival is a visible event; here the fact is about the future, on a screen where
nothing is moving. And the one person who has played this product read it exactly that way and wrote
the sentence down.

**Fold it into the seat's own line: reword `Waiting for your rival` to carry the explanation, adding
no eleventh string.** Its strongest case: it lands the information precisely where the human drew it,
it leaves §6's count where it is, and one line reads faster than two — which is the *minimal* half of
the positioning sentence honoured literally. Rejected on three counts. The phrase is not this
surface's alone: `ActionBar.tsx:290` renders `Waiting for your rival…` on the live table, so
rewording here would give the product two versions of what a player reads as one line, on two
surfaces, one of which is itself under an open question (`DEC-135`). It is not cheaper: it is a
different amendment to the same §6, replacing a string `ADR-0110` §2 fixed byte-identically on
purpose. And it welds a statement about now to a promise about next, in a plate the arriving
`Your rival` plate replaces — two strings, each saying one thing, are cheaper to read and cheaper to
change.

**Give the host a `Start the duel` control and start on the press.** Its strongest case: it removes
the ambiguity without any explanatory copy at all, it is the most familiar shape in every game
product, and a host who wandered off would not lose the opening blinds to a duel that began without
them. Rejected because it contradicts merged behaviour rather than extending it: the duel already
starts on the seating (`Room.kt:212–223`), and a press would make the **rival** wait at a table that
is not running — the exact thing `ADR-0094` §1 refuses, having made the invite path a seat and not a
lobby. It is also more furniture, not less, on the surface the vision calls minimal, and it would
need a new intent on the wire, which is not a product decision at all. The wandering host is already
covered: `ADR-0108`'s turn clock plays an absent seat.

**Say it in the human's words verbatim — *dual will start automatically when rival joins*.** Its
strongest case: transcribing a report is the only way to be certain it was honoured, it costs no
interpretation, and it cannot be accused of softening what was asked. Rejected because `dual` is a
misspelling of this product's own central noun, and *automatically* is machine-register in a product
whose vocabulary sentence is a list of duelling words. `ADR-0110` §2 already normalised this same
annotation's neighbour for this same reason and recorded the reversal as cheap; the same recording
applies here — if the human wants their own sentence, that is one card frame and one string.

**Say it at the arrival instead: reopen `ADR-0110` §7 and announce the rival's joining.** Its
strongest case: it states the fact at the moment the fact is true rather than promising the future,
which is the more honest register, and it is one string either way. Rejected because it answers a
different question. The host's uncertainty exists **while they wait**; a message that arrives with
the rival arrives after the wait it was needed for, and by then the filling seat and the dealt cards
have already said it. §7's argument is untouched by anything here, and §6 above upholds it.

**Fix the exact string in this ADR, as `ADR-0110` §2 did for the seat.** Its strongest case: it is
the house precedent, the two merged string-set tests want a literal, and an ADR that names the string
leaves the implementing ticket nothing to invent — which is the failure mode this whole register
exists to prevent. Rejected because §2's string was a **relocation** of a shipped one, where this is
a new sentence whose length and register cannot be judged apart from the frame it sits in, and taste
is the human's, given by looking at the rendered card (`ADR-0024` §3). §§2–4 above bound the choice
tightly enough — two facts, six refusals and a fixed vocabulary — that the card is checkable against
this ADR rather than free. The cost, that no literal is pinned until the card merges, is named in
*Consequences* rather than hidden.
