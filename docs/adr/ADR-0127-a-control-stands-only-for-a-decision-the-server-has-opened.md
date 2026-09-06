# ADR-0127 — A control stands only for a decision the server has opened

- **Status:** Accepted
- **Date:** 2026-09-06
- **Resolves:** `DEC-135` — *when it is not the player's turn, does the action bar stand as
  **disabled controls** rather than the sentence `Waiting for your rival…`?* **No.** Derived from
  `docs/vision.md`'s *Positioning* — ***"The reference points are Lichess and Chess.com, not
  PokerStars. Dark, quiet, fast, minimal."*** — applied through `CLAUDE.md`'s non-negotiable *the
  server is authoritative; a client may never assert a game fact*, because **which actions are
  legal is a game fact and it arrives with the turn**. Not derived from a preference about
  greyness.
- **Also resolves:** `DEC-108` — *when the table says* The duel is paused.*, may the action bar
  stay enabled?* — whose subject left the screen with
  [`ADR-0113`](ADR-0113-the-turn-clock-is-derived-state-and-the-sweep-plays-the-seat.md) §7 and
  whose principle is §4 below. Struck in this PR from `docs/adr/README.md` and `tasks/BOARD.md`,
  and annotated on `STORY-1214`'s own decision row, which is where it was born.
- **The human's annotation is recorded and is not followed.** On 2026-09-06, on `edits3.png`, the
  human boxed the panel reading `Waiting for your rival…` and wrote ***"show disabled
  controllers"***. [`EPIC-14`](../../tasks/epics/EPIC-14-the-name-the-showdown-and-the-fit.md) item
  3c registered that as `DEC-135` **for the product owner** rather than as a specification, and
  this ADR answers it *no*. §7 states exactly what a one-sentence overrule would cost, so that
  reversing this is cheap and visible rather than a rewrite.

## Context

### What is on the screen, measured on `develop` at `7c39fd3d`

`ActionBar` branches on one thing: `turn === null` (`web-client/src/table/ActionBar.tsx:55`). When
it is null the bar renders `Waiting()` — a reserved sizing row and the single line
`Waiting for your rival…` (`ActionBar.tsx:285-294`, the string on `:290`). When it is not null the
bar renders `Live`: the sizing chips, the typed total, and one button per action the server named.

`turn` is `PendingTurn`, and `PendingTurn` carries `legalActions` (`store/duel-state.ts:140-144`).
It is written by exactly one frame — `case "YourTurn"` (`duel-state.ts:282-292`) — and cleared by
the next `Snapshot` and by `DuelFinished` (`duel-state.ts:301`, `:356`). **So the client holds a
`LegalActions` exactly while a decision stands for this seat, and holds none at any other
moment.** There is no second source: `LegalActions` rides on `YourTurn` and on nothing else.

The bar's own KDoc already states the rule this decision is about, and states it as a promise:

> It offers exactly the actions the server named in `YourTurn` and no others — it **hides none it
> thinks bad, adds none it thinks legal**, and works out no amount the server did not send.

### What the merged design card already draws

`design/components/action-bar.html` draws **three** states, not two, and gives each a caption:

| State | Caption on the card | What it shows |
| --- | --- | --- |
| live | *Your move* | chips, typed total, one button per allowed action |
| `.disabled` | ***Sent — waiting on the server*** | the same two rows, everything faint, **no spinner** |
| `.off` | ***Not your move*** | the sizing row hidden for wrap parity, and `Waiting for ImKate…` |

So *disabled controls* is a state this product already has, already ships (the `sent` lock —
`disabled={sent}` at `ActionBar.tsx:135`, `:150`, `:179`), and already **means something
specific**: your press is in flight.
[`ADR-0103`](ADR-0103-the-table-fits-the-phone-and-the-cards-give-before-the-numbers.md) quotes the
same card for the height — *"the bar reserves both rows in its `off` state"* — which is why the
height is not what `DEC-135` is about.

### What the table already says while the rival acts

The fact *it is the rival's turn* is stated three times, on the plate it belongs to, before the bar
is reached at all: `seatStatus` returns `"Their turn"` (`seat-status.ts:24`), `SeatPlate` prints it
in accent uppercase and marks the plate with an accent left border (`SeatPlate.tsx:42-60`), and the
countdown runs beside it (`ADR-0113` §6). The bar's sentence is the **third** statement of a fact
the table already carries. That is the strongest thing that can be said against the sentence, and
it is a reason to consider replacing it — not a reason to accept any particular replacement.

### What is actually in tension

Two things both worth having, and only one of them is available:

1. **A bar that is one object rather than two.** A player who can see the controls while they wait
   learns the interface once, and their turn opening becomes a state change rather than a
   substitution. This is what the human asked for and it is a real gain.
2. **A bar that never names an action the server has not offered.** Heads-up, every decision is one
   of two shapes — `{CHECK, BET}` when nothing is owed, `{FOLD, CALL, RAISE}` when something is —
   and *folding does not exist until something is owed* (the card's own lede). The client cannot
   know which shape is coming, because the shape arrives with the turn.

Wanting (1) is what makes this a decision. (2) is why it cannot be had honestly: a faint
`Fold · Call · Raise` standing under a hand whose next decision will be `Check · Bet` is the client
adding actions it thinks legal, in grey. The amounts are worse still — `ADR-0111`'s field and
`bar-no-derivation.test.tsx`'s *"shows no number the turn does not carry"* exist because this line
has been crossed before, and a greyed `Call 400` for a decision the server has not opened is a
number for a turn that does not exist.

### The neighbour, and why it can be closed here

`DEC-108` asked whether the bar may stay **enabled** while the table said *The duel is paused.* It
was raised because `STORY-1214`'s human read that sentence, obeyed it, and waited out a duel the
server would have accepted actions in.
[`ADR-0046`](ADR-0046-the-table-says-away-timed-out-and-back.md) §6 had declined the question by
name; `ADR-0113` §7 then deleted `isPaused`, the `DUEL_PAUSED` enum entry and the sentence itself,
and its *Consequences* recorded that *"whoever reaches it will find its ground gone"*. Verified
here: `DUEL_PAUSED` appears in no `.kt`, `.ts` or `.tsx` file in this repository, and
`ProtocolError` (`protocol/protocol.gen.ts:221`) no longer lists it.

The **fact** is gone. What was never written down is the **rule** — whether *wait* and *you may
act* may be on one screen at one moment at all — and that is the same rule `DEC-135` turns on. It
is written down in §4.

### The deadline

Nothing here is expensive today and nothing gets more expensive later: this is one branch of one
component and one state of one design card. No schema, no wire field, no name, no migration. The
reason to answer now is that `STORY-1406` cannot be split until it is answered and `EPIC-14` item
3c's other two seams are already moving. **Reversibility is not what chose the answer**, and this
ADR does not pretend it was: both directions are equally cheap to reverse, so the argument had to
be made on what the bar is allowed to say.

## Decision

### 1. A control is drawn only for a decision the server has opened for this seat

The action bar draws a pressable or unpressable control **only** while this client holds a
`LegalActions` for a decision point the server opened for its own seat and has not closed. Outside
that window it draws no button, no sizing chip and no amount field — not live, not faint, not
outlined, not as a placeholder.

This is `ADR-0002` and `CLAUDE.md`'s non-negotiable applied to the question *what may the screen
say*, not merely *what may the client send*: **which actions are legal is a game fact**, and a
control is the screen's way of stating that its action is one of them. The client has that fact
only when `YourTurn` has given it, so it may only draw controls then.

### 2. When no decision stands, the bar is the reserved box and one sentence

The `off` state keeps its box, keeps both rows reserved — `ADR-0103`'s quoted promise and
`duel-table-states.html`'s *"Every slot … exists in every state, so nothing appears, disappears, or
moves"* are unchanged — and holds **one sentence and nothing else**. Today that sentence is
`Waiting for your rival…`, and this ADR does not change it, does not shorten it and does not move
it.

The sentence is a **still form** in `ADR-0115` §1's sense: the `…` is one literal ellipsis
character in the string, the fact is legible in a screenshot, and no animation carries any part of
it. Specifically forbidden as the way to say *wait*: animated dots, a spinner, a pulse, a shimmer,
a progress bar, and any skeleton that fades. The card already says it for the neighbouring state —
*"everything faint, **no spinner**"* — and it is now said for this one.

### 3. A faint control means exactly one thing: your press is in flight

`.disabled` — both rows present, everything faint — is reserved to the state the card already
names: **Sent — waiting on the server.** A decision is open, the player has pressed, and the bar
refuses a second press until the server answers. The amounts it shows are the amounts of the
decision that is still open, so §1 holds.

This is why *not your turn* may not borrow the same treatment even if it could be made honest: two
different facts would be drawn identically, and the one meaning *your press is in flight* is the
one a player needs to read at a glance. One treatment, one meaning.

### 4. *Wait* and *you may act* are never sayable at the same moment — and the notice gives way, not the bar (`DEC-108`)

**They may not.** A screen that tells a player to wait while the server would accept their press is
a defect whatever the bar's `disabled` attribute says, and `STORY-1214` measured what it costs: a
person read the sentence, obeyed it, and the duel stopped. A script clicking through would have
seen nothing.

When the two conflict, **the bar is right and the sentence is wrong.** The bar is drawn from the
server's own `LegalActions` for an open decision (§1); a notice that contradicts it is copy about a
state that no longer holds. So the resolution is always to delete or correct the notice, never to
disable a control the server would honour. That is also, as it happens, the route the product
already took: `ADR-0113` §7 deleted the pause rather than dimming the bar under it, and
`TASK-130911` removed the line that named it.

`DEC-108` is therefore answered on both halves — *may they coexist?* no; *which gives way?* the
notice — and struck. Nothing in the product exhibits the case today, so no ticket falls out of
this; it is a rule that makes the next such notice a defect on the day it is written instead of a
question to be re-raised.

### 5. The edge this rule does not claim to close

A control is offered because the server opened this decision point **and has not, as far as this
client has been told, closed it**. That is a statement about frames received, not about the
server's present instant, and there is one window where the two differ: the turn clock's sweep.
`ADR-0113` §5 keeps no deadline check on the act path, so a late press is usually honoured, and
when the sweep wins the race the press comes back `Rejected` and the notice says so (`ADR-0043`: a
rejection closes no decision point).

That window is **not** what §4 forbids. §4 forbids a *standing, known* state in which the screen
offers a press the product has already decided to refuse — the pause was exactly that, for as long
as it lasted. A race at the boundary of a decision that no screen can be ahead of is a different
thing, it is already told to the player after the fact, and this ADR neither closes it nor pretends
to.

### 6. What this does not decide

- **The sentence's words.** Whether the bar says `Waiting for your rival…` or names the rival — the
  card draws `Waiting for ImKate…` and the code says *your rival* — follows from the naming work
  `ADR-0119` began, not from this. Nothing here blesses either string; §2 fixes only that the slot
  holds one still sentence.
- **Pre-actions.** *Check/fold in advance* is a feature, not a treatment, nobody has asked for it,
  and it is not opened here.
- **Item 3c's other two seams.** The timebank figure is a defect with a known cause
  (`turn-clock.ts:105`) and the pot's chip pile is `ADR-0115`'s and `ADR-0102`'s. Neither is
  touched.
- **The waiting-room screen.** `WaitingTable` prints *"Waiting for your rival"* on the rival's
  **empty seat plate** (`WaitingTable.tsx:22`), not in an action bar — `ActionBar` renders only
  where a `view` exists (`Lobby.tsx:360`). That surface is `EPIC-14` item 3b's and is unaffected.

### 7. What a reversal would cost, if the human overrules this

Stated so that the overrule is one sentence and one superseding ADR rather than an argument: a
*yes* would have to say **which** of the two heads-up action shapes the faint bar draws when the
client knows neither, and **what stands where an amount goes** on a control for a decision that
does not exist. Those two sentences are the whole of the disagreement; everything else — height,
box, tokens, the card's third state — already exists.

## Consequences

**What it buys.** The bar has one rule, stated in one line, that covers every state it has ever
been asked about: *a control stands only for a decision the server has opened.* `STORY-1406` stops
being a design question. The rule that `STORY-1214`'s human ran into is written down, so the next
notice contradicting a live bar is a defect on the day it is written rather than a `DEC` that sits
open while its subject is deleted underneath it — which is what happened to `DEC-108`: the question
was declined by name on 2026-08-16 (`ADR-0046` §6), registered as a `DEC` on 2026-09-01, and had
its subject deleted on 2026-09-02 (`ADR-0113` §7) without ever being answered. It leaves the open
table here.

**What it costs.**

- **The human asked for something and is being told no.** That is the whole cost and it is not
  small: the annotation was made looking at the real screen, and the answer here is reasoned from
  merged sources rather than from having played the hand. §7 exists because of that.
- **`STORY-1406` has no work in it.** Its title — *The bar stands disabled where it printed a
  sentence* — states the opposite of this decision, so the story is retired rather than split, and
  `EPIC-14` item 3c shrinks to its two remaining seams. An epic loses a story to an answer, which
  is the honest outcome but not a free one.
- **The bar's sentence stays the third statement of a fact the table already makes twice.** This
  ADR measured that redundancy (`seat-status.ts:24`, the accent border, the countdown) and keeps it
  anyway, because the alternative on offer was worse — not because the redundancy is good. Anyone
  who wants the slot to earn its space has to propose something honest to put in it, and §2 says a
  spinner is not it.
- **A player still cannot see, while they wait, what they will be able to do.** The gain the
  annotation was reaching for is genuinely forgone, not redesigned away. It is available only to a
  product where the server tells a client about a decision it has not opened, and that is a wire
  and a permission this one does not have.

**What it forecloses.**

- **A bar that previews the next decision.** Even if the wire someday carried the shape of a
  decision not yet open, §1 forbids drawing controls for it. Reversing that needs a superseding
  ADR, and it would have to answer §7's two questions.
- **A second meaning for the faint treatment.** `.disabled` is *your press is in flight*,
  product-wide, and any future state wanting faint controls has to displace that meaning
  explicitly.
- **A skeleton vocabulary in the action bar.** Grey placeholder shapes standing where controls will
  be are refused here, so a loading idiom for the bar would now be a reversal rather than an
  addition. Nothing else in the product has one either.

## Alternatives considered

**1. Disabled controls carrying real action names and amounts.** The strongest form of what was
asked for, and the strongest case for it: the bar becomes one object, its geometry is identical in
both states so nothing at all changes when a turn opens, the player reads the interface once, and
every poker client the human has used works this way. Rejected because the client holds no
`LegalActions` outside a turn (`duel-state.ts:282-301`), so every name and every number on such a
bar would be invented or stale — the client adding actions it thinks legal, which its own KDoc
forbids, and stating amounts, which `ADR-0111` and `bar-no-derivation.test.tsx` gate. The strongest
version of this option is the one that most clearly asserts game facts.

**2. Disabled controls with verbs only, no amounts.** Answers the `no-derivation` objection cleanly
— no number is shown, so no number is invented — and still teaches the interface. Rejected because
the verbs are the bigger claim, not the smaller one: heads-up, the coming decision is
`{CHECK, BET}` or `{FOLD, CALL, RAISE}`, the client cannot know which, and a bar showing `Fold`
where nothing is owed teaches the interface **wrong**. A control's word is the game's word
(`ADR-0101`'s ruling for the sizing row); it is not decoration that happens to be shaped like a
button.

**3. Three faint empty slots — a skeleton.** Honest: it names nothing and claims nothing, while
still showing that controls live here. Rejected on two grounds. It is not what was asked for — a
skeleton is not *"controllers"*, and it shows a player less than the sentence does, since it says
neither what is happening nor who is being waited on. And it would introduce a placeholder idiom
this product has nowhere else, in the one component whose job is to be unambiguous.

**4. Both: the faint controls *and* the sentence.** Would have satisfied the annotation without
losing the explanation. Rejected because it doubles the objection rather than answering it — the
invented verbs are still invented — and because it makes the bar say two things at once in a
product whose positioning sentence is *quiet, minimal*.

**5. Show nothing at all: the reserved box, empty.** Maximally minimal, states no fact, invents
nothing, and leans on the plate's `Their turn` and countdown to carry the meaning — which *Context*
measured as already sufficient. Rejected as strictly worse than one sentence at equal cost: an
empty bordered box is the one thing on that screen that would read as a rendering failure, and a
player whose rival has gone away benefits from a line naming who is being waited on. It is also the
option most easily reached later if the sentence ever stops earning its space — nothing here blocks
it.

**6. Refuse the question and send it to the human.** Defensible on the ground that the human wrote
the annotation and could settle it in a word. Rejected because `docs/vision.md` plus the merged
sources do contain the answer — *Lichess, not PokerStars*, and the client asserts no game fact —
and because a `DEC` returned unanswered to the person who is not at the keyboard is the exact stall
this role exists to prevent. The right treatment for the disagreement is to answer it, record the
annotation verbatim, and make the overrule cheap: §7.
