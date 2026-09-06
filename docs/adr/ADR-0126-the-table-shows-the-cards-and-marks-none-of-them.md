# ADR-0126 — The table shows the cards and marks none of them

- **Status:** Accepted
- **Date:** 2026-09-06
- **Resolves:** `DEC-134` — **the product owner's** — may the table **mark the five cards that won**,
  in the winner's hand and on the board? Raised 2026-09-06 by
  [`EPIC-14`](../../tasks/epics/EPIC-14-the-name-the-showdown-and-the-fit.md) item 2b, on the human's
  annotation over `edits3.png` after playing the product on a laptop and an iPhone: *"showdown
  animation: chips move to the winner; villan card shown if required; **winning combination
  higlited, winning cards + board card that make combination**"*. **Answered no.**
- **Where the answer came from:** **derived from the vision; the human did not state this call.**
  Their annotation is the report, not the ruling — the reading
  [`ADR-0120`](ADR-0120-a-showdown-shows-the-hands-the-rules-showed-and-the-beat-that-shows-them-stands.md)
  took the same day, of the same sentence, on the same drawing. The licensing sentence is
  [`docs/vision.md`](../vision.md)'s *What it is* — *"Replay and honest feedback. Every hand is
  stored as an event log, so a match can be replayed and **analysed afterwards**."* — read exactly as
  [`ADR-0095`](ADR-0095-the-table-states-who-took-the-pot-and-never-names-a-hand.md) already read it
  when it refused to name a hand at this table: **the vision puts the explaining surface
  *afterwards***, and the *Roadmap* dates it — the replay viewer is **v0.4**, while **v0.1** is
  *"Two browsers, one room link, one complete duel, rematch."* Seconded by *Positioning* — *"Dark,
  quiet, fast, minimal"*. Nothing in *What it is* or *What it is not* moves in either direction, so
  this is answered rather than escalated.
- **Applies, and amends nothing:** `ADR-0095` §3 (*no hand is ever named, anywhere on this table*) —
  **applied** to a statement made without words, not reopened — and §6 (the trigger that reverses it,
  to which this ADR holds itself); `ADR-0120` §1 (which hands a showdown shows) and §4's first
  bullet, which named this decision as the one it deliberately left open — *"This one decides what is
  **shown**, never what is **marked**"*; [`ADR-0002`](ADR-0002-server-authoritative.md) (a client may
  never assert a game fact, which is why a *yes* could never have been client-side);
  [`ADR-0115`](ADR-0115-motion-never-carries-a-fact-and-reduced-motion-stills-every-surface.md) §1
  (no fact lives only in motion — and under §1 below no fact lives in a still emphasis either, so
  there is nothing here for reduced motion to still);
  [`ADR-0008`](ADR-0008-loser-mucks-at-showdown.md) (untouched in both directions: nothing here
  changes what the engine publishes).
- **Registers nothing, and `DEC-148` is not opened.** The routing that reserved `DEC-148` reserved it
  for the wire question a *yes* would raise. **A no has no wire question**: no field, no
  `PROTOCOL_VERSION` step, no `atomic:` ticket, no claim on
  [`ADR-0047`](ADR-0047-a-protocol-version-is-claimed-in-a-ledger.md)'s one-bumping-branch lock.
  `ADR-0095`'s precedent, verbatim — *"It is answered **no**, so no `DEC` is registered for it"* — and
  [`ADR-0105`](ADR-0105-one-duel-at-a-time-and-the-refusal-hands-back-the-duel.md) §6, *a `DEC`
  nobody is working is noise in the open table*. §6 below states the shape that question takes on the
  day this ADR is superseded.
- **Constrains:** `STORY-1412`, which loses its wire move, its `atomic:` label and `poker-server`
  altogether, and becomes one gate and one line on `STORY-1411`'s showdown card (§4); `STORY-1413`,
  whose only stated dependency was `STORY-1412`'s hold on the version lock. **No wire change, no
  `PROTOCOL_VERSION` move, no server file, no engine file, no new string, no new control.**
  `poker-engine` is not opened — `EPIC-14`'s *Out of scope* forbids it and §3 prices the one comment
  this leaves stale.

## Context

### What was asked, and what the table does today

The annotation is one arrow with three clauses on it. Two are settled: *chips move to the winner* is
item 2c, a card under `ADR-0115` and `ADR-0102`; *villan card shown if required* is `DEC-133`,
answered by `ADR-0120` — the table shows the winner's hand, both hands on a split, no hole card on a
fold, and the last beat that turned a hand face up now stands **2,000 ms**. The third clause is this
decision: mark the two hole cards and the board cards that together make the winning hand.

Measured on `develop` at `7c39fd3d`:

- **Nothing on the table can be marked, and nothing knows which five to mark.** `PlayingCard.tsx`
  draws every face-up card through one `SHELL` constant; the only thing that varies between two
  drawn cards is the rank character, the suit glyph, the suit's colour, and the `aria-label`
  `card-text.ts` builds as `"${rank} of ${suit.name}"`. `BoardCards.tsx` draws five places and
  `Hand.tsx` two, both by position.
- **No wire field names the five.** `bestHand`, `winningCards` and any spelling of them appear
  nowhere in `protocol.gen.ts` or in `poker-server`. The view carries `holeCards` and the board and
  says nothing about what they made.
- **The engine holds them and only the engine does.** `BestHand.kt:11` —
  `public data class BestHand(val rank: HandRank, val cards: List<Card>)` — and its KDoc says why the
  `cards` field is there: *"exists because the client highlights the winning five at showdown, and
  recomputing them there would duplicate this logic in a place that could disagree with it."*
  **Nothing outside `poker-engine` references `BestHand`.** The sentence has described a consumer
  that does not exist for the whole life of the client.

### What is actually in tension

**A mark is a statement about the cards, and this table has already refused to make one.**
`ADR-0095` §3 forbids naming a hand *"not at a showdown, not at a fold, not in the amount slot, not
in the facts line, not in an `aria-label`, not in a `title`, not in a tooltip"*. A highlight prints
no string, so the merged `HAND_TALK` matcher could never see it — but the **fact** it states is of the
same kind: a reading of seven public cards, performed by the product, at the table, live. The
distance from *these five played* to *two pair, kings and jacks* is one string.

**What the human asked for is real, and the confusion it addresses is real.**
`docs/duel-rules.md` §Showdown: *"Best five cards of the seven available. **Both hole cards need not
be used; neither need be.**"* That is the single hardest sentence in Hold'em for someone meeting the
game at hand one, and the vision's first success condition names that person — *"Send a link. **She**
opens it in a browser."* A mark answers it in the one place the question is asked, without a word.

**It cannot be built without spending the irreversible half.** Which five cards won is a game fact;
`ADR-0002` and `CLAUDE.md`'s non-negotiables forbid a client asserting one, and
`web-client/src/table/no-derivation.test.tsx` already gates card reading. So a *yes* is a server-sent
field, a `PROTOCOL_VERSION` step under `ADR-0047`'s lock, and a second place outside the engine that
holds a statement about a hand. `ADR-0095` declined that same field for the name and said exactly
why: *"a wire field outlives every string in this product, and there are no players yet whose
difficulty could tell us the field is needed."*

**The shape asked for helps least where it is needed most.** By `ADR-0120` §1 you always see the hand
that beat you and never see what you beat, and a fold shows no hole card at all — so a mark exists
only at a showdown, and the losing player reads it on the **winner's** hand and on the board while
nothing marks their own five. Completing it means publishing a second reading, of a hand nobody was
required to show. The complete version of the ask is bigger than the ask.

**The mark and the name are one decision arriving twice.** Both need the same thing — the server
saying something about the made hand — both cost the same version step, and both answer the same
product question: *does this table explain a hand while it is being played?* Answering half of it six
days after the other half was refused, on identical evidence, is settlement by drift rather than by
decision.

**And the evidence has not moved.** `ADR-0095` §6 set this repository's standard for reversing a
refusal of this kind — *the first duels played by people who are not the author* — and `ADR-0120`
re-applied it hours ago on the neighbouring question: *"That standard is not met today, in either
direction."* One session, by the author, on a build nobody else has played, is the same evidence base
both earlier ADRs were written against.

### The deadline

`STORY-1411`'s first ticket is the showdown's design card (`ADR-0091` §2, `ADR-0120`), and a card
cannot draw a showdown without knowing whether the winning five is picked out — drawn without this,
it is drawn twice. The wire field is also cheapest today: `PROTOCOL_VERSION` has no deployed client
to migrate and never will again. That is a reason to decide **now**; it is not a reason to decide
either way.

## Decision

### 1. No card on this table is marked

Every card the table draws is drawn like every other card of its kind. At a showdown, at a fold, at
every street, in either seat, on the board, and for **both** winners of a split pot, **the five cards
that made the winning hand are drawn exactly as the two that did not.**

A mark is anything that distinguishes one drawn card from another by **what it did** rather than by
**what it is**: a ring, border, glow, tint, shadow, lift, scale, opacity, reordering, badge or
connector; a class name a stylesheet can key on; a `data-*` attribute; a `title`; or anything in an
`aria-label` beyond the rank and suit `card-text.ts` already produces. **Dimming, fading or greying
the cards that did not play is the same statement and the same refusal** — the fact travels equally
well either way round.

What a card **is** stays exactly as it is drawn today: face up or face down by `ADR-0120` §1's table,
suit colour by `card-text.ts`, a dashed slot for a board place not yet dealt.

### 2. The winning five stays inside `poker-engine`

No wire field carries it. `PlayerView`, `Snapshot` and every event are unchanged, `PROTOCOL_VERSION`
does not move, no serial descriptor changes, and `poker-server` leaves item 2b exactly as `ADR-0120`
left it out of item 2a.

The client does not compute it either — that was never available (`ADR-0002`), and this ADR grants no
licence to weaken any gate. `no-derivation.test.tsx` stands **byte-unchanged** and stays green
without being consulted, because §1 renders no text at all; a coder who finds themselves editing that
file for this story has built something §1 forbids.

### 3. `BestHand.cards` keeps its value and loses its stated reason

The field is **not deleted**. It is pure, deterministic and free; it is already pinned by
`TASK-010309`'s brute-force suite and `TASK-010311`'s hundred-thousand-hand equivalence check, both
of which compare whole `BestHand` values; and the surface it is for is the one the vision puts
*afterwards* — the replay viewer (v0.4) and `ADR-0005`'s analysis interface, reading stored hands,
which is where `ADR-0095` already said a hand may be named *"from stored data with no wire commitment
at all"*.

Its KDoc sentence — *"exists because the client highlights the winning five at showdown"* — states a
product commitment this decision declines, and **this ADR is the correction of record.** The one-line
KDoc repair is **not `EPIC-14`'s**: that epic's *Out of scope* says *"Nothing here opens
`poker-engine`"*, and this ADR does not open it either. It is worth exactly one standalone ticket and
no more, and until one lands the comment is stale rather than dangerous — the value it justifies is
correct and used.

### 4. What `STORY-1412` becomes

It is no longer a wire story. What is left is small and worth keeping, because **nothing in the
repository today would notice a mark appearing**:

- **One gate**: *the drawing of a card does not depend on whether it played.* The property to pin is
  that any two face-up cards rendered at a completed showdown differ only in rank, suit and suit
  colour — no other attribute, class or label differs between them. The property is stated here; the
  matcher is the ticket's, and it is deliberately not a word list, because §1's subject is pictures.
- **One line on `STORY-1411`'s showdown card** (`ADR-0091` §2): the card draws the showdown with no
  card picked out, and says so in its margin, so the drawing is not re-decided at the pane.

Whether that is a story of its own or two tickets inside `STORY-1411` is the planner's. **`STORY-1413`
no longer waits on it** — what it was waiting for was the version lock.

### 5. What this decides nothing about

- **The hand's name.** `ADR-0095` §3 stands, unamended and unqualified. It is applied here, not
  reopened, and this ADR licenses no move in either direction on it.
- **Marking or naming the winning five *afterwards*** — in the replay viewer or `EPIC-08`'s analysis
  board, over stored hands. Neither licensed nor foreclosed here; that is where §3 points
  `BestHand.cards`.
- `ADR-0120` §3's 2 s hold, the pot's travel (item 2c), the rival hand's width on a phone
  (`DEC-136`), and the **all-in-and-called** reveal `ADR-0120` §5 names without registering.
- **Whether this product ever teaches the game.** `docs/duel-rules.md` holds the ranking; whether any
  screen ever puts it in front of a player is not asked by anybody today and is not answered here.

### 6. What would reverse this, stated so the reversal is a decision rather than a drift

The trigger is `ADR-0095` §6's, unchanged and now carried by two ADRs: **the first duels played by
people who are not the author, reporting that they cannot tell what won a hand.** Or the human, who
may rule it in one sentence and needs no trigger — this is a call the vision licensed rather than one
the vision fixes, and their annotation asked for it.

When it reverses, it reverses **as one decision.** The mark and the name are the same server-sent
statement about the made hand, and a superseding ADR should settle both together — one field, one
`PROTOCOL_VERSION` step, one turn of `ADR-0047`'s lock — rather than spending that lock twice on two
halves of one question. The mechanism question that then arises is **the architect's**, named here
and deliberately not registered (`ADR-0105` §6): which wire type carries it and on which frame,
whether `poker-engine` publishes `BestHand` or the server renders a projection of it, what that costs
`docs/architecture.md`'s engine contract and the projection layer's single filtering point, and what
`PROTOCOL_VERSION` step it takes.

## Consequences

**What it buys.** `EPIC-14`'s item 2 becomes entirely client and design: `STORY-1412` stops being the
epic's only wire move, `ADR-0047`'s lock stays free for `DEC-137`'s answer if that one needs a field,
and `STORY-1413` unblocks. The table keeps exactly one rule about cards, the one `ADR-0095` gave it —
**it draws what the server sent and it never reads the cards** — with no street, ending or seat
behaving differently; and that rule is now stated for **pictures** as well as for words, which is the
half `HAND_TALK` could never have caught. The showdown card can be drawn once.

**What it costs.**

1. **The player who cannot read seven cards is still not helped, and this is the second ADR to charge
   them.** `ADR-0095` charged them the name; this charges them the mark; `ADR-0120` gave them two
   seconds to work it out unaided. That is the whole of what this product does for the author's
   sister at hand one, and it is said here in one place rather than left spread across three
   documents where nobody adds it up.
2. **It declines something the human asked for in writing.** One clause of the three on that arrow is
   refused while the other two are being built. §6 makes the overrule one sentence, and it is theirs
   to make at any time.
3. **A deferred wire field is a dearer wire field** — `ADR-0095`'s own strongest argument against
   itself, spent a second time. The version step is free today and will not be after v0.1 reaches a
   player.
4. **`BestHand.cards` stays unreferenced outside the engine for at least another milestone**, behind
   a comment that has been wrong since August and stays wrong until somebody spends a ticket on §3.
5. **A gate has to be written to hold a nothing.** A refusal that leaves nothing behind is undone by
   the next coder with a spare CSS class, so §4 buys a test that asserts an absence — the most
   fragile kind, and the reason §4 states a property rather than a matcher.

**What it forecloses.** A live, at-the-table reading of the cards — in pictures now as well as in
words — until §6's trigger, and with it any surface where this table teaches the game while the game
is being played. It forecloses **nothing afterwards**: the log stores every card that was shown, so
replay (v0.4) and `ADR-0005`'s analysis inherit the whole question with no commitment made here, and
§3 keeps the engine value they will read.

## Alternatives considered

**1. Mark the five, from a new server-sent field — what was asked for.** The strongest case in the
set, and it is strong. The human asked for it, in writing, pointing at the screen, after playing the
product on two devices. Every online poker client in existence does it, so it is the shape a player
arrives expecting. It is the only thing on this table that can answer *"both hole cards need not be
used; neither need be"* without a word, for exactly the person the vision's success condition is
written about. `ADR-0120` has just bought two seconds of showdown for it to land in, and two seconds
of a hand a player cannot parse is worth much less than two seconds of one they can. `BestHand.cards`
was built for it in August and the engine already computes it to settle the pot, so the product would
be withholding something it holds. The mark leaks nothing — every card it touches is already face up
— so `ADR-0008` does not object. And the field is cheapest today, with no deployed client to migrate.
**Rejected on four counts, in order of weight.** It is `ADR-0095` §3's refusal restated in pictures,
and reversing a merged refusal on the same evidence that refusal was made against is drift, not
decision. The vision puts the explaining surface *afterwards* and the roadmap dates it v0.4, while
v0.1 is one duel and a rematch. The mark and the name are one wire question and deserve one answer,
made once, together — §6. And it spends the one irreversible thing in the area on a single session by
the author, against a standard two merged ADRs have already applied.

**2. Mark both fives — the winner's, and the losing viewer's own.** The strongest case is that it
repairs alternative 1's asymmetry, which is a real defect in what was asked for: the player who needs
the explanation is the one who lost, and a mark on the winner's hand tells them about somebody else's
cards. It publishes nothing secret, because a player always sees their own hole cards, so the second
reading crosses no line `ADR-0008` drew. Rejected because it is strictly more than was asked; because
it needs a per-seat field where the projection layer already filters per recipient, putting a second
statement about a hand into a place whose whole discipline is having one; and because it loses to the
same objection as 1 — the wire, on this evidence, today.

**3. The client marks them — it holds every card at a showdown.** Not negligible: zero protocol cost,
zero server work, and at a showdown the client genuinely has the board plus the revealed hand, so it
would derive nothing secret and leak nothing. Rejected on a non-negotiable, exactly as `ADR-0095`
rejected the same shape for the name: `CLAUDE.md` and `ADR-0002` make the server the only thing that
asserts a game fact, and *"the client happened to have enough information this time"* is precisely
the reasoning that rule exists to refuse. It is also not a transcription but a **second evaluator**,
in another language, where a disagreement with `HandEvaluator` would be silent — the duplication
`BestHand`'s own KDoc says the field exists to prevent.

**4. Mark nothing, but put the hand ranking somewhere a player can reach — a rules link at the
table.** Its case: it helps the same beginner, costs no wire field and states no fact about *this*
hand; `docs/duel-rules.md` already holds the ranking, so it turns `ADR-0095`'s consolation — *"the
ranking is written down"* — from true into reachable. Rejected as **not this decision**: it is a new
surface and a new control on a screen whose positioning sentence is *minimal*, nobody has asked for
it, and folding a second question into this run is the failure this role exists to prevent. It stays
available and unregistered (`ADR-0105` §6).

**5. Escalate to the human, because the human asked for it in writing.** The strongest case: of the
six items in `EPIC-14`, this annotation is the least ambiguous — it names the mechanism and asks for
a thing the product does not have — so declining it looks like overruling the person whose product
this is. Rejected because the boundary does not run there. `docs/workflow.md` gives *what a player
sees* to the product owner; `docs/vision.md`'s *What it is* and *What it is not* are untouched by
either answer; no money, no roadmap milestone and no new kind of thing is involved. `ADR-0120` read
the same annotation the same way on the same day. Escalating would stall two story seams and a design
card for a call the vision contains — and §6 makes the human's overrule a sentence rather than a
merge conflict.
