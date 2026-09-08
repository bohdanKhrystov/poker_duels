# ADR-0143 — Irreversibility is said last, and never coloured

- **Status:** Accepted
- **Date:** 2026-09-09
- **Resolves:** `DEC-156` — **the product owner's** — on a screen carrying both reversible and
  irreversible acts, does the irreversible one get **weight** the others do not? Raised 2026-09-09
  by the coder that had just drawn `design/screens/account.html`'s name form, reading back its own
  work: the screen now carries the anonymous block, the recovery address, a password form and a
  name form, and only the name form spends something forever. **Answered: yes — but the weight is
  words and a step, never a treatment.**
- **Where the answer came from:** **derived from the vision; the human did not state this call.**
  The licensing sentence is [`docs/vision.md`](../vision.md)'s *Positioning* — *"The reference
  points are **Lichess** and **Chess.com**, not PokerStars. **Dark, quiet, fast, minimal.**"* — and
  the boundary is [`docs/workflow.md`](../workflow.md)'s row assigning *"what a player sees"* to
  this role. Nothing in *What it is* or *What it is not* moves in either direction; no money, no
  roadmap milestone, no new kind of thing. §1 **mints nothing**, which is the only part of a visual
  question [`ADR-0091`](ADR-0091-design-gets-no-agent-a-new-screen-owes-a-card.md) §3 reserves to
  the human.
- **Supersedes nothing.** No sentence of any merged ADR is deleted or replaced.
- **Narrows one grant, by exactly one relation.**
  [`ADR-0130`](ADR-0130-a-name-can-be-changed-and-the-name-it-leaves-is-spent.md) §5's closing
  sentence — ***"The words, the heading, the control labels and the layout are the design card's
  (`ADR-0091` §2), within those two."*** — keeps every word. §2.2 below fixes **one** relation it
  had left to the card: where the irreversibility claim sits **relative to the control that
  performs the act**. The words, the heading, the control labels and every other question of
  layout stay the card's, unchanged, and §5's two obligations are untouched.
- **Applies, and amends nothing:** `ADR-0130` §1's ***"with no quota, no cooling-off, no
  confirmation step and no *changes remaining* counter"*** and §7's ***"No quota, no counter, no
  cooling-off, no confirmation press."*** — **applied** in §3, which is why the name form is the one
  irreversible act in this product whose whole guard is a sentence; `ADR-0091` §2 (the card draws
  it) and §3 (minting is the human's, interactively);
  [`ADR-0024`](ADR-0024-design-follows-the-code-workflow.md) §3 (the visual verdict stays the
  human's); [`ADR-0126`](ADR-0126-the-table-shows-the-cards-and-marks-none-of-them.md) §1, whose
  subject is **cards on the table** and which is *not* the authority for §1 here — the family
  resemblance is in the reasoning, not in the scope;
  [`ADR-0115`](ADR-0115-motion-never-carries-a-fact-and-reduced-motion-stills-every-surface.md) §1
  (no fact lives only in motion — §1 adds no motion, so there is nothing here for reduced motion to
  still);
  [`ADR-0050`](ADR-0050-revoking-the-device-signs-out-everywhere-but-here.md) §3's three revoke
  facts and `ADR-0052` §1's removal notice, both untouched in their words and their triggers.
- **Registers:** **`DEC-157`**, the product owner's — whether *give this profile a password*, which
  claims a handle nothing in the product gives back, owes an irreversibility sentence of its own.
  §4 measures it and deliberately does not answer it.
- **Constrains:** `design/screens/account.html`, which owes **one margin sentence and no change to
  any frame** (§5); `TASK-140910`–`TASK-140912`, which may keep transcribing into plain
  `text-small`; every future screen of controls. **No wire change, no `PROTOCOL_VERSION` move, no
  server file, no engine file, no new string, no new token, no new control.**

## Context

### What is actually on that screen, measured

On `develop` at `d54ba165`, and in `TASK-140909`'s card as drawn, the account surface carries
**three** acts a player cannot undo, not one:

- **Set or change a display name.** `ADR-0130` §2 retires the string given up — *"Nobody else may
  take it, ever"*, and *"The player who gave it up may not take it back either."* The form says so:
  *"But the name you give up is gone for good — you cannot take it back, and nobody else can take it
  either."*
- **Stop this device signing in.** `account-text.ts`'s `REVOKE_PERMANENT` is *"This device will
  never sign in to this account again. **This cannot be undone.**"* — the **other** sentence in this
  product claiming an act cannot be undone, and the **earlier** one: it has shipped since
  `TASK-041220`, under `EPIC-04`.
  `AccountScreen.tsx`'s own KDoc records that *"The revoke control (`RevokeControl`,
  `TASK-041220`) still exists but is not placed on this screen yet"*, which is why the card does not
  draw it.
- **Sign out.** `SIGN_OUT_WARNING` is *"Signing out leaves any duel room this browser is in, and a
  duel left this way can be lost. This browser goes back to the profile it had before."* A duel lost
  is a coin to the rival and a row on a ladder, and no path takes either back.

So the premise the question was raised on — *the name form is the only act on this screen that
spends something forever* — is true of **the card** and false of **the screen**. That correction is
what makes this decidable rather than a matter of taste, because the product has already answered
this question twice, in code, and its answer can be read off.

### The product's existing grammar for an irreversible act is a step, not a colour

`RevokeControl.tsx`: *"Pressing `REVOKE_LABEL` only asks: it shows `ADR-0050` §3's three facts and a
confirming control, and `props.revoke` is called from nowhere else. **An in-page step, never a
native dialog** — three facts do not fit one."* `SignOutControl.tsx` does the same and says why in
the same words: *"An in-page step, never a native dialog, exactly as `RevokeControl` does — **one
shape for both confirmations on this screen.**"*

Neither carries a colour, a border, a box, an icon or a size step. `REVOKE_PERMANENT` renders inside
a bare `<p>`; `SIGN_OUT_WARNING` renders inside a bare `<p>`. **The weight is entirely in the second
press.**

### And the client has never coloured a risk

Measured across `web-client/src`, non-test files only:

- **Colour marks a fact the server sent.** `text-win`/`text-loss` on a duel's outcome
  (`DuelResult.tsx:78,80`, `HistoryScreen.tsx:257,259`); `text-warn` on a clock running out
  (`SeatPlate.tsx:17`); `text-accent` and `border-l-accent` on whose turn it is and on a timebank
  (`SeatPlate.tsx:18,47,55`); `border-accent bg-accent-subtle` on your own ranked row
  (`LadderScreen.tsx:111`) and on a standing rematch offer (`RematchControl.tsx:64,73`).
- **Or colour marks a control as a control.** `bg-accent-fill` on a screen's primary action;
  `text-accent` on the one link-shaped control the client has, *Forgot password*
  (`Lobby.tsx:585`).
- **Colour has never marked a consequence.** All **eight** `role="status"` refusal sentences, across
  **seven** components — `NameAsk`, `NameSurface`, `SignInForm`, `ForgotPasswordForm`, `SignUpForm`
  (twice), `RecoveryEmailForm`, `RevokeControl` — are plain: seven carry `text-small` and one
  carries no class at all. There is **no `role="alert"` anywhere in this client.**

The token sheet says the same thing about itself, in a comment that is the design system's own
reservation: `design/tokens/tokens.css` lines 36–37 — *"Semantic — outcomes and urgency, all
desaturated: results are stated, not lit up. **Amber exists only for the turn clock running
down.**"* `design/screens/duels.html:82` restates it — *"`--pd-warn` is reserved for the turn
clock"* — and `SeatPlate.tsx:17` is its only consumer. There is no danger hue in the sheet, and the
one urgency hue is spoken for.

### What is in tension

**The report is real, and it is not a wording complaint.** The coder did not find a missing
sentence; `ADR-0130` §5 already requires two, before the send. It found **four panels that look
alike**, one of which spends something forever, and no signal of any kind that says which. A player
who skims rather than reads is not helped by a sentence that is drawn exactly like the three
sentences beside it.

**`ADR-0130` removed every brake but the sentence.** Its §1 is *"with no quota, no cooling-off, no
confirmation step and no *changes remaining* counter"*, on the human's recorded *"at any time"*. The
argument for weight is strongest precisely here: when every other guard has been deliberately
removed, the one remaining should be the most visible thing on the screen, not the least. And
`ADR-0130` priced the outcome itself — *"The most common outcome of §2 will not be an impersonation
defeated; it will be a player who changed their mind and cannot change it back."*

**But the product's own answer to irreversibility is unavailable to this form.** The step
`RevokeControl` and `SignOutControl` use is refused for the name by `ADR-0130` §7 — *"No quota, no
counter, no cooling-off, no confirmation press."* That refusal derives from the human's own phrase,
and reopening it is not this decision's to make.

**A colour is not a small thing to add.** There is no danger hue to reach for, so a visual grammar
means **minting**, and `ADR-0091` §3 puts minting with the human, interactively — *"because taste
does not survive a verify block"*. It cannot be dispatched and it cannot be decided in an ADR.

**And the evidence is one agent reading a card it had just drawn.** `ADR-0095` §6 and `ADR-0126` §6
both fix this repository's standard for adding an emphasis to a surface on thin evidence: *the first
duels played by people who are not the author.* Nothing has moved that standard since 2026-09-06.

### The deadline

Two clocks, and only one of them is the card's.

**The card is being drawn now.** `TASK-140909` draws the name form and `TASK-140910`–`TASK-140912`
transcribe from it. `ADR-0091` §3 lets the human's visual verdict **trail the merge**, and its
Consequences name the price of that — *"A screen can ship wearing a look the human later rejects…
its price is rework — the card **and** the implementation behind it."* An undecided flatness gets
graded at the pane as an oversight; a decided flatness gets graded as a choice. §5 is one sentence
and it is due before that pane, not after it.

**A register is cheap to add and dear to remove.** Adding weight later is one class on one panel and
nothing is lost. Removing a danger register after four screens have joined it changes what all four
of them mean, silently, for the players who learned that loud means careful. Nothing has been minted
yet, so the cheap direction is still open — which is a reason to decide **now**, and, unusually,
also most of the reason the answer is the one that spends nothing.

## Decision

### 1. The product mints no danger register

**Nothing in this product marks an act as irreversible by how it is drawn.** No accent colour, no
tint, no fill, no border, no box, no rule, no icon, no badge, no size step, no weight step, no
capitalisation, no italic, no reordering that lifts it out of its group, and no motion. Not on the
account screen, not on any screen, now or later.

It is refused in the assistive register too, because the statement travels equally well there:
**no `role="alert"`, no `aria-live` urgency, and no visually-hidden prefix such as *Warning:*.** The
claim reads to a screen reader exactly as the sentences beside it read.

The reason is measured, not asserted: in this client colour says **what a thing is** — a result, a
clock, a turn, your own row, a standing offer, a control — and has never said **how careful to be**.
Eight refusal sentences ship plain. *"Dark, quiet, fast, minimal"* is what a fourth semantic use of
colour would have to answer to, and it does not.

### 2. What an irreversible act is owed instead — three obligations, and none of them is a treatment

These bind **a screen of controls**: a surface whose purpose is acts, carrying at least one act that
cannot be undone alongside at least one that can. Today that is the account screen. The duel table
is out — see §6.

1. **It says, before the act, what the act costs and that the cost does not come back**, in the
   product's own plain words. The claim is made **at the act's own strength** — unconditional where
   the loss is unconditional (`REVOKE_PERMANENT`'s *"This cannot be undone."*, and `ADR-0130` §5's
   obligation 2 for the name, already merged and unchanged), conditional where the loss is
   (`SIGN_OUT_WARNING`'s *"a duel left this way **can be** lost"*, which is true only while a duel
   is live, and the hedge is what makes it truthful). The obligation is the **claim**, never a fixed
   phrase. This is `STORY-0411`'s reasoning, quoted by `ADR-0052` and by `ADR-0130` §5: *a player is
   entitled to know that at the moment they can still avoid it.*
2. **That claim is the last thing a player reads before the control that performs the act.** It sits
   in the same group as the control, and **no other sentence sits between them** — the act's own
   input may, nothing else. It is never in a lede, a heading, a page note, a tooltip, a `title`, or
   a different panel. This is the third obligation, beside `ADR-0130` §5's two; it is a **relation**
   and not a drawing, and how the group is drawn stays the card's (`ADR-0091` §2).
3. **A second press, wherever no merged ADR has refused one.** `RevokeControl`'s and
   `SignOutControl`'s in-page two-step is the shape — *never a native dialog*, one shape for every
   confirmation in the product. This is applied from what ships, not invented here, and it is the
   only place weight is permitted to cost a player anything.

**Weight, then, is real and it is ordinal: words, position, and a press.** What it is never is
emphasis.

### 3. The name form takes 1 and 2 and cannot take 3

`ADR-0130` §1 — *"with no quota, no cooling-off, no confirmation step and no *changes remaining*
counter"* — and §7 — *"No quota, no counter, no cooling-off, no confirmation press."* — are
**applied here, not reopened, not qualified and not superseded.** Both derive from the human's
recorded *"at any time"*.

The consequence is stated plainly rather than smoothed over: **the display name is the one
irreversible act in this product whose entire guard is a sentence**, and this ADR declines to add a
second guard of any kind. That is a merged decision being obeyed, and the place to change it is a
superseding ADR or the human — not this one.

### 4. Every irreversible act in the product today, and the one that is unguarded

| Act | Says what it costs, before the act | Last before the control | Second press |
| --- | --- | --- | --- |
| Set or change a display name | yes — `ADR-0130` §5 obligation 2, unconditional | yes, as `TASK-140909` draws it | **refused** by `ADR-0130` §1, §7 |
| Stop this device signing in | yes — `REVOKE_PERMANENT`, unconditional | yes — `RevokeControl.tsx` | yes |
| Sign out | yes — `SIGN_OUT_WARNING`, conditional and truthfully so | yes — `SignOutControl.tsx` | yes |
| Give this profile a password | **no — nothing is said at all** | — | no |

The first three conform today and **nothing about them changes**. `RevokeControl` and
`SignOutControl` are not touched, in their words or in their steps.

The fourth is a gap this ADR found by measuring and **does not close**. Signing up claims a handle
into `credential`'s `UNIQUE (kind, identifier)` (`V4__credential_and_auth_session.sql:17`), and
[`ADR-0039`](ADR-0039-v01-offers-no-account-deletion.md) refuses account deletion in v0.1, so
**no path a player has gives a handle back** — while `SIGN_UP_LABEL`, `HANDLE_LABEL` and
`PASSWORD_LABEL` say nothing about it at all. Whether that act owes a sentence is an obligation
question of `ADR-0130` §5's kind, about a different act, and answering it inside this run would be
settling two decisions because they looked related. **Registered as `DEC-157`, the product owner's.
It blocks nothing.**

So the answer is *only the name form gets no second press* — and that is not special pleading,
because it is not a judgment about the name at all. It is `ADR-0130` §7 applied. Every other
irreversible act in the product gets the press, and the one act that is missing a sentence is named
rather than excused.

### 5. What the account card owes: one margin sentence, and no change to any frame

`TASK-140909`'s two name-form frames **already satisfy §2.2** — the *gone for good* line is the last
sentence in the panel, with only the field between it and `Set my name`. Checked against the drawn
card; **no frame changes and no ticket repairs one.**

What the card owes is `ADR-0126` §4's shape, and nothing more: **one line in the name-form frame's
margin note saying the panel carries no accent and none is owed, under this ADR** — so the flatness
is graded at the pane as a decision rather than re-litigated as an oversight, and so the next card
drawn against this one inherits the rule. One sentence, in one existing note. The planner cuts the
ticket; this ADR writes none.

**No gate is bought.** `ADR-0126` §Consequences 5 already priced this exact shape — *"A refusal that
leaves nothing behind is undone by the next coder with a spare CSS class"* — and it is true here
too. The property, stated so that a later ticket can pin it without re-deriving it: *no element in a
group carrying an irreversibility claim differs by class from the corresponding element of a group
that carries none.* Whether that earns a test is the planner's; the honest note is that it would be
**green from birth**, because nothing in the client carries such a class today, so its whole value
is against the next edit — exactly what `ADR-0142` said of itself.

### 6. What this decides nothing about

- **The words.** `ADR-0130` §5's two obligations, and the card's choice of words, heading and
  control labels within them (`ADR-0091` §2). Untouched, in either direction.
- **Whether *give this profile a password* owes an irreversibility sentence** — `DEC-157`, §4.
- **The duel table.** A fold, a check that loses a hand, a duel left by closing a tab: those are
  moves in a game whose rules are `docs/duel-rules.md`'s, on a surface whose register is already
  settled by `ADR-0095` and `ADR-0126`, and nobody has asked. §2's obligations bind a screen of
  controls and stop at the table's edge.
- **Whether the name form ever gets a confirming press.** `ADR-0130` §1 and §7's, and only a
  superseding ADR or the human moves it.
- **Whether `RevokeControl` is ever placed on the account screen.** That is `TASK-041220`'s standing
  absence, recorded in `AccountScreen.tsx`'s KDoc, and this ADR neither places it nor forbids it.
- **`DEC-094`, which is a different question and stays open.** It asks whether an *unclassed*
  navigation control — the lobby's doors and each `Back`, rendering with `className: ""` — is the
  intended treatment. That is about controls the cards do not draw. §1 says nothing about how any
  control is drawn; it says only that **being irreversible** may not be what decides it.
- **Anything about motion**, which `ADR-0115` §1 already governs and which §1 adds none of.

### 7. What would reverse this, stated so the reversal is a decision rather than a drift

**The product cannot count skimmers**, and this ADR builds no instrument that could. A player who
read the sentence and meant it and a player who skimmed past it produce the identical write. So the
trigger is a **reported regret**, not a rate:

1. **One player asking for a name back.** Under `ADR-0130` §2 and §7 there is no un-retire, no
   reclaim and no operator rename, so a player who regrets a change cannot be served by any
   mechanism in the product and has to ask a person. **That request needs no instrument, and one of
   them is the observation.** It is also the only signal that separates a sentence that worked from
   a sentence that was skipped.
2. **The human at the pane.** `ADR-0024` §3's visual verdict, which `ADR-0091` §3 lets trail the
   merge. One sentence reverses this, with no trigger and no player, and it is theirs to give at any
   time.

**What is not evidence:** another agent reading a card back and reporting that the panels look
alike. That is the report this ADR answers; repeating it is not a second observation. And a
reversal, when it comes, has to answer `ADR-0091` §3 first — a danger hue is **minting**, worked
with the human, not chosen in a ticket.

## Consequences

**What it buys.** The account card's name-form frames are correct as drawn, so `TASK-140909` needs
no repair and `TASK-140910`–`TASK-140912` transcribe into plain `text-small` without inventing a
treatment between them. The product's one rule about colour — *it names what a thing is, never how
careful to be* — is now written down, which it was not; the next screen of controls inherits an
answer instead of re-deriving one at Haiku temperature. `RevokeControl`'s and `SignOutControl`'s
two-step stops being a local habit and becomes the product's shape for irreversibility, so a future
irreversible act gets the strongest guard available to it rather than the one the ticket happened to
think of. And the flatness of the account screen becomes a graded choice at the pane instead of an
oversight discovered there.

**What it costs.**

1. **The skimmer is not helped, and this ADR knows exactly who they are.** The vision's first
   success condition names a person meeting this product for the first time — *"Send a link. **She**
   opens it in a browser."* — and a first-timer is the skimmer. `ADR-0130` has already named the
   likely outcome: *"a player who changed their mind and cannot change it back."* §7's trigger fires
   **after** that has happened to somebody. **The price of this decision is denominated in one
   player's name**, and no reading of it avoids that.
2. **It leaves the least-guarded act carrying the largest permanent loss.** `ADR-0130` removed the
   quota, the cooling-off and the confirming press; this ADR declines to add emphasis. The display
   name is the whole of a player's identity here —
   [`ADR-0067`](ADR-0067-a-leaderboard-row-is-text-and-no-id-turns-into-a-profile.md) made a
   leaderboard row text that no id turns into a profile — and it is now the only irreversible act in
   the product with a single guard, while revoking a device, which loses far less, has two.
3. **A refusal that leaves nothing behind, and no gate bought for it.** §1 is held by an ADR and by
   review. One `text-warn` on one panel undoes it silently, and §5 declines to buy the absence-test
   that would catch it — `ADR-0126` incurred this same cost knowingly and it is incurred again here,
   with the reason stated rather than hidden.
4. **A product-wide rule from a base of three acts.** Two conforming examples and one that cannot
   take the third obligation is thin. If a fourth kind of irreversible act arrives that a sentence
   and a press genuinely cannot carry — one whose consequence lands on somebody other than the
   player pressing — §1 has to be superseded rather than qualified, because it is written as an
   absolute.
5. **A found gap is registered rather than closed.** The account screen ships knowingly inconsistent
   with its own new rule: *give this profile a password* spends a handle and says nothing, and
   `DEC-157` may sit open for a while. Naming it is better than not finding it and worse than
   fixing it.

**What it forecloses.** A danger register, and with it warning boxes, red buttons, alert icons and
every treatment that would let one screen be louder than another — until §7's trigger, or the
human's sentence. **Nothing irreversible is spent**: reversing this costs one token binding and one
class on one panel, no wire field, no migration, no string that has to be un-said and no promise
that has to be withdrawn. That is not incidental — with no players yet and one agent's reading as
the whole of the evidence, being the cheapest answer to reverse is a substantial part of why it is
the answer.

## Alternatives considered

**1. Give the irreversible act a visual grammar — an accent border, a tinted panel, or the amber the
sheet already holds.** The strongest case in the set. The defect is real and it was found by exactly
the review this repository trusts: a coder reading back its own work, unprompted. The display name
is the whole of a player's identity here —
[`ADR-0067`](ADR-0067-a-leaderboard-row-is-text-and-no-id-turns-into-a-profile.md) made a
leaderboard row text that no id turns into a profile, so the string is all a player is — and
`ADR-0130` deliberately stripped every other brake (*"no quota, no cooling-off, no confirmation
step"*), which means the sentence is now the only thing
standing between a player and a permanent loss; when every guard but one has been removed, that one
should be the most visible thing on the screen rather than the least. The sheet already spends
colour on meaning three times over (`--pd-win`, `--pd-loss`, `--pd-warn`), so a fourth semantic use
is not casino chrome by itself. And the vision's own success condition names a first-timer who will
not read four panels of prose before pressing. **Rejected on three counts, in order of weight.**
There is no hue to take: the sheet's own comment reserves the only urgency colour — *"Amber exists
only for the turn clock running down."* — so this needs a **new** one, and minting is worked
interactively with the human (`ADR-0091` §3), which an ADR cannot do and a ticket cannot be
dispatched to do. Every existing semantic colour marks **what a thing is** — a result, a clock, a
turn, your own row, an offer, a control — and this would be the first colour in the product meaning
*be careful*, arriving on the quietest screen it has. And it is bought against no evidence, on one
session by one agent, against the standard two merged ADRs already set (`ADR-0095` §6, `ADR-0126`
§6) and neither has since relaxed.

**2. Give the name form a confirming second press, matching `RevokeControl` and `SignOutControl`.**
Its case is the strongest on the merits and it nearly did not need arguing: this is the product's
**own** answer to irreversibility, shipped twice, needing no colour, no token, no minting session
and no new vocabulary — the cheapest conforming answer available. It would also work better than
any treatment, because a skimmer who skims a sentence still has to press twice, which is the one
guard a skim cannot defeat. **Rejected because it is not available.** `ADR-0130` §1 — *"with no
quota, no cooling-off, no confirmation step and no *changes remaining* counter"* — and §7 — *"No
quota, no counter, no cooling-off, no confirmation press."* Taking it means superseding a clause
that ADR derived from the human's own recorded *"at any time"*, which is a reopening this decision
was told not to make and could not make on its own authority. §2.3 keeps the press for every act
`ADR-0130` does not speak for, which is the whole of what was left to take.

**3. A sentence is enough, and nothing further is owed — the flat *no*.** Its case: it is the
shortest true answer, it changes nothing, it costs nothing, and `ADR-0130` §5 already discharged
`STORY-0411`'s entitlement by requiring the claim *before the send*. Piling obligations onto a
merged one is how a product accretes rules nobody asked for. **Rejected because it answers the
wording question and not the one asked.** The report was not *a sentence is missing*; it was *four
panels look alike*. `ADR-0130` §5's *before the send* is a statement about **time**, and a screen can
satisfy it while putting the claim in a lede, a footnote or a note beside the form — which is
exactly where a claim drifts when nothing forbids it. §2.2 closes that at the cost of one relation
and no colour, and it is the only part of this decision that responds to what was actually reported.

**4. Move the irreversible act to a screen of its own, where it cannot be skimmed past.** A genuine
case: separation is the one shape a skim cannot defeat, it needs neither colour nor a second
press, and there is precedent in this product — `ADR-0130` §6 records that `ADR-0119` §1's ask
*"stays a screen in place of the front door at the player's own first press"*, so a name surface
standing alone has shipped. **Rejected**: `ADR-0130` §6 fixed *"One surface does both acts"* on
the account screen, from the human's *"in accounts settings"*, and hiving the change off is a
second surface for the same
act — precisely the duplication §6 was written to end. It is also strictly **more** friction than
the confirming press of alternative 2, which was refused as too much, so it cannot be the smaller
answer to the same problem.

**5. Escalate to the human, because a rule about warning and emphasis is the visual language.** Its
case: `EPIC-06` and `ADR-0024` §3 put the visual verdict with the human at the rendered card, and a
decision that fixes the product's whole relationship to *loud* reads like a change to the vocabulary
rather than an application of it. **Rejected because the boundary does not run there.**
`docs/workflow.md` gives *"what a player sees"* to this role; nothing in *What it is* or *What it is
not* moves in either direction; there is no money, no roadmap milestone, no new kind of thing, and
no risk outside the software. Decisively, **§1 mints nothing** — the only branch that would have
needed the human is alternative 1's new hue, and that is refused here partly for exactly that
reason. §7 makes the overrule one sentence, and the pane it is given at is already scheduled. Same
reading `ADR-0126` §Alternatives 5 made on the neighbouring question three days ago.
