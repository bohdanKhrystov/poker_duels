# ADR-0122 — `Call` names the price, and `Raise to` still names the total

- **Status:** Accepted
- **Date:** 2026-09-06
- **Resolves:** `DEC-137` — **the product owner's** — does the `Call` button name the **total**
  (`callTo`, what ships) or **what the player must add**? Raised 2026-09-06 by the human after
  playing the product on a laptop and a phone — *"'call' bug - when i have call options it says
  full bet size(like i bet 100 -> opp raise 300 -> i see call 300). in this case call bnt shoud
  show the diff i need to add"* — and registered by
  [`EPIC-14`](../../tasks/epics/EPIC-14-the-name-the-showdown-and-the-fit.md), whose item 4 it
  gates. It is a decision and not a defect because **there is no arithmetic to be wrong**: the
  button prints a field the server sent, under a KDoc that refuses the netting by name.
- **Where the answer came from.** Derived, not stated. The human's words are the report, not the
  specification. The licence is the first line of [`docs/vision.md`](../vision.md)'s *What it is* —
  **"Heads-up Texas Hold'em. Two players. Never three."** — read exactly the way
  [`ADR-0101`](ADR-0101-pot-means-a-pot-sized-raise-and-the-fractions-share-its-base.md) and
  [`ADR-0107`](ADR-0107-pot-names-every-chip-committed-to-the-hand.md) already read it: the product
  *is* Texas Hold'em, so a word the game owns means, on this product's surfaces, what the game's
  audience means by it. And for this word the repository has already written down which number a
  player *faces*, in a merged sentence of [`docs/duel-rules.md`](../duel-rules.md) (§Betting,
  decided as `DEC-003`): *"A big blind all-in for 60 at 50/100 leaves the amount to match at 60, so
  the small blind **owes 10 more to call** rather than 50 … because **the bar a player faces should
  be a number that is genuinely at stake**."* The vision's *On variance* closes it — *"showing a
  player that they lost the match but made the better decisions is more interesting than hiding the
  maths"* — the sentence this repository already reads as *show the numbers rather than smooth them
  over* ([`ADR-0111`](ADR-0111-an-illegal-typed-amount-is-refused-in-the-servers-own-numbers.md)).
- **Applies, and does not touch:**
  [`ADR-0101`](ADR-0101-pot-means-a-pot-sized-raise-and-the-fractions-share-its-base.md) §1, which
  already names this quantity and its formula (`toCall = callTo − committedThisStreet`) and says in
  as many words that it *"is what the call still **costs**, which is not `callTo`"* — §§1–3 of that
  ADR are unchanged, and every chip still sets a street total.
  [`ADR-0100`](ADR-0100-the-driver-reaches-an-amount-by-pressing-what-a-player-presses.md) §6 — a
  label naming a game quantity is a statement and it is the server's; §5 below says why this figure
  is on the statement side of that line. `ADR-0111` §5 (*no act conversion*) and §7 (*never a
  different amount*) — both untouched, and §6 below says why.
  [`ADR-0107`](ADR-0107-pot-names-every-chip-committed-to-the-hand.md) §5 is the shape this ADR
  copies: the never-derives rule admits **one further named quantity** and nothing else.
  [`ADR-0002`](ADR-0002-server-authoritative.md) — the printed figure is the engine's own
  `GameState.toCall`, recovered exactly, and the client asserts no game fact.
- **Amends [`ADR-0109`](ADR-0109-the-table-marks-the-last-act-and-the-next-deal-clears-it.md) §2 in
  exactly one clause**: the last-act mark's figure list loses `Call`. §2's rule — *"the mark says
  what the actor's own button said, no more and no less"* — is what forces the amendment and is
  what survives it. §§1, 3, 4 and 6 stand byte-unchanged; §5's card owes one changed drawing and
  still owes six states.
- **Touches no module, and moves no wire.** No code ships with this ADR. No `PROTOCOL_VERSION`
  step, no engine, server or projection change, no new string: every term is already on the view,
  already threaded to the bar as a prop, and **already subtracted in shipped client code** at
  `web-client/src/table/ActionBar.tsx:255`. The implementing work is `EPIC-14` item 4's, the
  planner's to split.
- **Registers no `DEC`.** The mechanism half `EPIC-14` forecast for this answer — *a field the
  server sends beside `callTo`, or the one subtraction the never-derives gates are told to admit* —
  turns out to be neither: the subtraction already ships under `ADR-0101` §1, so what is admitted
  is **printing** a quantity the client already computes. A `DEC` nobody is working is noise in the
  open table ([`ADR-0105`](ADR-0105-one-duel-at-a-time-and-the-refusal-hands-back-the-duel.md) §6),
  and §4 names the one wire-shaped residual as an accepted cost rather than a blocked question.
- **Where the numbers came from.** Read on 2026-09-06 at `develop` `7c39fd3d`:
  `poker-engine/.../game/BettingRules.kt` (`callTo = committed + state.toCall(seatToAct)`, and
  `CALL` offered only when `callTo != committed`), `game/GameState.kt:106-109`
  (`toCall = (betToMatch − committedThisStreet).coerceIn(0, stack)`),
  `web-client/src/table/action-text.ts`, `ActionBar.tsx`, `act-frame.ts`, `DuelTable.tsx:81`,
  `lobby/Lobby.tsx:295-313`, `bar-no-derivation.test.tsx`, `ActionBar.test.tsx`,
  `design/screens/duel-table.html`, `design/screens/duel-table-states.html:298` and
  `scripts/qa/drive.mjs:77-84`.

## Context

`actionText` returns `{ verb: "Call", amount: actions.callTo }` (`action-text.ts:48-49`) under a
KDoc that refuses the netting **by name** — *"Every figure here is the server's or the player's
own: `callTo` and `allInTo` came off the wire, and `to` is what the player set on the amount
control. **Nothing is priced, netted or worked out.**"* So the human reported a bug against a
screen where nothing miscalculates, because nothing calculates. The question is which of two true
numbers the button is allowed to name.

**The player cannot get the other number off the screen.** `DuelTable.tsx` mounts exactly one
`BetLine`, and it is the rival's (`:81`). The hero's own street commitment is drawn nowhere — it
has left the stack numeral and has not reached the pot numeral. This is the same hole `ADR-0107`
named while deciding the pot, closed there for the *pot* and still open on the hero's side. It is
what makes `callTo` more than a convention a player could translate: at 100 committed against a
raise to 300, the button prints 300, the player's own 100 appears on no surface, and the 200 they
must find is therefore not computable from anything drawn.

**Two cases where the printed total is not merely unhelpful.** `duel-rules.md`'s own worked
example: a big blind all-in for 60 at 50/100 leaves the small blind owing **10**, and today's
button prints **60** — a third number, neither what is owed nor the blind. And facing a bet larger
than the stack, `callTo` is capped at `allInTo` (`BettingRules.kt`), so a player with 2,950 behind
and 200 committed reads `Call 3,150` — a figure larger than the stack printed on their own plate,
for the act `duel-rules.md` calls *"`call` (for your remaining stack)"*.

**The audience's arithmetic runs on the price.** Pot odds is the calculation Hold'em asks at the
moment this button is on screen, and it is the price over the pot-after-the-call. `ADR-0107`
already promised the screen would support it — its *Consequences* say *"the pot after the call is
the printed `Pot` plus **the printed call cost**"* — a sentence written about a call cost the
screen does not print.

**And a real force the other way.** `callTo` is what the server sent; every other figure on this
screen is a total (`minRaiseTo`, `allInTo`, the sizing chips, the typed field, the `Raise to`
button); `ADR-0109`'s last-act mark prints the same act's `to` total one tick later; the shipped
behaviour is guarded and costs nothing to keep; and a priced `Call` puts two kinds of figure in one
bar, which is exactly the *"two names for one act on one screen"* `ADR-0109`'s *Context* warned
about.

**One button is not in tension at all, and it was measured rather than assumed.** `BettingRules.kt`
offers `BET` only when `state.betToMatch == 0`, and `betToMatch` is the largest street commitment,
so when `Bet` is on offer **neither seat has committed anything this street** and its total and its
price are the same number, always.

## Decision

### 1. The `Call` button names the price: what the press takes from the stack

```
price = legalActions.callTo − seats[legalActions.seat].committedThisStreet
```

This is `ADR-0101` §1's `toCall`, character for character, and it is the **engine's own quantity**:
`BettingRules.kt` builds `callTo = committed + state.toCall(seatToAct)`, so the subtraction does not
manufacture a number — it recovers the term the server added. The label stays `Call`, the figure is
formatted as the bar already formats chips, and the button carries one figure and no parenthetical.

At the human's own frame — 100 in, rival raises to 300 — the button reads `Call 200`.

### 2. Two properties hold by construction, and a test may assert them

- **The figure is never zero or negative.** `BettingRules.kt` adds `CALL` to the legal set only in
  the `else` of `if (callTo == committed)`, so wherever the button exists the price is at least one
  chip. `Call 0` is unreachable.
- **The figure never exceeds the stack on the player's own plate**, because `GameState.toCall`
  coerces into `0..stack`. The short call therefore prints the remaining stack exactly, which is
  what `duel-rules.md` says that act is.

### 3. `Bet`, `Raise to` and `All in` keep their totals

Nothing else on the bar changes its number. `Raise to` prints the dialled street total, which is
the proposal the player set and the figure the `Act` frame carries (`ADR-0101` §2, `ADR-0100` §6);
`Bet` prints the same total, which §Context measured to be the price as well; `All in` prints
`allInTo`, unchanged.

**What tells a player which kind of figure they are looking at is the preposition, and it is the
game's own.** A live table announces *"raise to 1,500"* and never *"call to 600"*: the raise is
announced as a destination and the call is announced as a cost, so `Raise to 1,500` beside
`Call 400` is not two conventions a player must learn — it is the two announcements they already
make. The bare verb takes the price; the verb with `to` takes the total.

**`All in` is the one place that reading does not come off the label, and it is left where it is.**
It is bare and prints a total. It is not moved because nobody has asked, because its figure is the
only one on the bar that says what the rival must answer, and because its price is already on the
screen — an all-in costs the stack, and the stack numeral is on the player's own plate. This ADR
therefore covers three of the four figures the bar prints and **says so** rather than implying a
rule that governs the fourth. Moving it later costs one line in `actionText`.

### 4. The last-act mark says `Call`, bare

`ADR-0109` §2's rule is *"the mark says what the actor's own button said, no more and no less"*,
and its mechanism is *"the act event's own `to` total"*. Under §1 those two sentences no longer
name the same number for one act, and **the rule survives while the mechanism yields**: the mark
may not print a figure the button would not.

The mark cannot print the price. `PlayerCalled` carries `sequence`, `seat` and `to` and nothing
else, and after the act the caller's `committedThisStreet` *is* `to`, so the term the subtraction
needs is gone by the time the mark exists. So for a call the mark prints **no figure at all** and
joins `Fold` and `Check` as bare. `Bet`, `Raise to` and `All in` marks are untouched and agree with
their buttons figure for figure.

This is also closer to what was asked for in the first place: the human's request that produced the
mark said *"for raise/bet icon plus size info"*, and `ADR-0109` §2 added the call's figure *"for
the same reason the buttons do"*. That reason has lapsed, and the figure goes with it. `ADR-0109`
alternative 7 is **not** overturned: its second ground — the client would have to compute a number
the server never sent — is what decides the mark here, and it still holds, because §1's subtraction
is available *before* an act and not after one.

### 5. The never-derives guards admit exactly one further named quantity

`no-derivation.test.tsx` and `bar-no-derivation.test.tsx` keep proving that the table and the bar
show no number the server did not state, with **one** addition to the pot sum `ADR-0107` §5 already
admitted: the acting seat's call price, composed of two server-stated fields by the subtraction
`ADR-0101` §1 defined and `ActionBar.tsx:255` already performs for the sizing base. Everything else
stands: no hand named, no winner declared, no bound the server did not send, no second derived
figure, no ceiling reaching the player before they press for it.

It sits on the statement side of `ADR-0100` §6's line rather than being carved out of it: the
printed figure is `GameState.toCall`, a quantity the engine computes and names, reconstructed
exactly from two fields the projection sends. Whether `actionText` gains an argument or is handed
the price by its caller is the implementing ticket's shape (`ADR-0101` §7's precedent), and the
guard stays red for any second derived figure.

### 6. Nothing about the amount control, the frame or the refusals moves

The sizing chips still set street totals, the typed field still takes a street total, and
`ADR-0111` §5 stands: a typed `callTo` is not a `Call`, and a typed *price* is not one either — the
table presses no button on the player's behalf. `ADR-0111` §7's *"never a different amount"* is
untouched because it is about the acts whose frame carries an amount: `actFrame` sends
`{ type: "Call", seat }` with **no amount at all**, so the call's figure is a statement about what
the act costs and never a proposal that could disagree with what is sent. `ADR-0100` §3's driver
contract is unaffected — `scripts/qa/drive.mjs:77-84` finds a control by the text it *starts with*,
precisely because *"'Call 100' … contains digits that change every hand"*, so **no recorded drive
is re-recorded by this answer**, the same claim `ADR-0101` §3 made for the sizing row.

### 7. The cards, measured

`design/screens/duel-table.html` draws `Call 400` in three frames in which the hero has committed
nothing (the frame `ADR-0101` §4 worked through), so 400 is the price as well and **no node moves**.
`design/screens/duel-table-states.html:298` draws the mark `Call 800` on the rival's plate in the
showdown frame; under §4 that node becomes `Call`, bare. One node, one card in arrears, carried by
item 4's story rather than by this ADR — `ADR-0107` §6's shape. No new surface and no new state is
drawn, so `ADR-0091` §2 owes no new card here.

## Consequences

**What it buys.** The reported case is gone: the button names the chips the press will take, in
every case, including the two where the total was not merely unhelpful — the short blind's *owes 10
more* and the call that is capped at a stack. Pot odds become an arithmetic over two printed
numbers, and `ADR-0107`'s consequence about *"the printed call cost"* — written about a figure the
screen did not print — becomes true rather than approximate: the cost of the `pot` chip is now the
printed call cost plus the pot-after-the-call, both on screen. No wire moves, so item 4 does **not**
share `ADR-0047`'s bumping lock with the showdown's wire work, and `EPIC-14`'s ordering note about
that lock resolves in the free direction.

**What it costs.**

- **One screen, two kinds of figure.** A price on one button and totals on three, distinguished by
  a preposition that `All in` does not have. A player who reads `Call 400` as a total and
  `Raise to 1,500` as a cost has the two exactly backwards, and nothing on the screen corrects
  them. Accepted as the cheaper confusion: the total was the one that was reported.
- **The mark loses a figure it prints today.** A player who looks away and returns learns that
  their rival called, not for how much. Bounded — a call matches a figure already stated, and by
  the tick the street closes the pot figure carries it — and it is the shape §4 forecloses: giving
  the mark a figure again means a wire field on `PlayerCalled` and a `PROTOCOL_VERSION` step under
  `ADR-0047`'s lock. Named here, deliberately not registered (`ADR-0105` §6).
- **A second exception in a guard whose value was having none.** `no-derivation` now proves the
  client derives *exactly two* named quantities. Each admitted quantity makes the next argument
  easier, and this ADR is evidence that the pattern is real rather than hypothetical.
- **The price is composed of two fields that arrive on different frames** — `callTo` in `YourTurn`,
  `committedThisStreet` in `Snapshot` — so if they are ever out of step the button misprices by the
  staleness. Inherited rather than new: every sizing chip has run this exact risk since `ADR-0101`
  §1, and §2's two properties are what a test can hold it to.
- **No existing test can tell the two numbers apart**, measured rather than assumed:
  `ActionBar.test.tsx` passes `committedThisStreet={0}` in seventeen places under a `?? 0` default,
  and its single non-zero fixture (200 against `callTo` 600, at line 599) presses the sizing row and
  never asserts the `Call` label; `action-text.test.ts:90` is titled *"says Call with the call's own
  total"*. So this change lands green against today's suite, and the ticket that implements it owes
  a fixture in which the acting seat has already committed — which is what `EPIC-14`'s Definition of
  done asks to be read off a screen, in a hand where the two seats' commitments differ.

**What it forecloses.** `Call` names the price on every surface, forever: the replay viewer (v0.4),
match history, and anything that ever prints the word. And it fixes the mark's shape — a call is
marked by its verb alone until a wire change says otherwise.

## Alternatives considered

**1. Keep `callTo`, and close the ledger by drawing the hero's own bet-line.** *Its strongest
case:* the printed number is the server's own field with nothing between it and the wire; one
convention covers every figure on the screen; `ADR-0109`'s mark agrees with the button by
construction; the never-derives guard keeps its single exception; it is shipped and guarded, so it
costs nothing; and adding the hero's bet-line — the one honest gap in the drawing — would at least
make the price computable by eye, exactly as this alternative's twin proposed for the pot. *Why it
lost:* it answers *the player cannot see the term* with *give them a subtraction to do on a
thirty-second clock*, where the vision's own line is that showing the maths beats hiding it; it
leaves the short-call button printing a number bigger than the stack beside it; `duel-rules.md`'s
own worked example speaks in what a player **owes**; and it is the reading the product's only real
player read as a bug on the first evening he played it on two devices.

**2. Relabel the button `Call to 600`.** *Its strongest case:* it is the cheapest possible answer —
one string, no arithmetic, the guard untouched at full strength, the mark and the button still
agreeing — and it makes the shipped number **unambiguous** rather than merely conventional, in the
same grammar as `Raise to`. *Why it lost:* it cures the ambiguity and not the complaint. The player
still cannot obtain the price, because the term it is subtracted from is on no surface; and *call
to* is not a phrase the game's audience uses, while *raise to* is the announcement they make out
loud — so it buys clarity by minting vocabulary, which is the opposite of `action-text.ts`'s merged
contract that no action gains a word the server did not name.

**3. Print both — `Call 400 (600)`, or a second line.** *Its strongest case:* nobody has to choose;
several commercial rooms show both; and the derived figure could be scoped to the parenthetical so
the guard keeps its purity. *Why it lost:* *"Dark, quiet, fast, minimal"* — `ADR-0107` rejected
this same shape for the pot, and a second figure on a **button** is worse than on a strip, because
`ADR-0103` §1's phone fit is the tightest place on the screen and a press has to be unambiguous.

**4. Send the price as a second field beside `callTo`.** *Its strongest case:* the never-derives
guard survives with no new exception; every printed number stays the server's; and it is the only
option under which the **mark** can print the price too, so `ADR-0109` §2 would need no amendment
and the table would carry one figure per act everywhere. *Why it lost as the decision:* it is a
mechanism, not a meaning. `ADR-0101` §7 already recorded that every term the bar needs is on the
wire, and `ActionBar.tsx:255` already performs this exact subtraction for the sizing base — so the
field would buy a number the client is holding, at the price of a `PROTOCOL_VERSION` step under
`ADR-0047`'s one-branch-at-a-time lock, which would serialise a one-label change against the
showdown's wire work in the same epic. It is named in §Consequences as the reversal path if the
mark's bare `Call` proves worse in the hand than it reads on paper.

**5. Price every button — `Raise by 1,300`, `All in 13,200` — so that every figure on the bar is
money leaving the stack.** *Its strongest case:* one rule, no preposition to notice, and the
player's whole bar answers one question. *Why it lost:* it is not available. `Raise to`'s figure is
the proposal the player dialled and the total the `Act` frame carries (`act-frame.ts`'s `to` is
*"the total committed on this street … not the amount added"*), the sizing chips set that same
total (`ADR-0101` §2), and `ADR-0111` §7 requires the printed total to be exact for the driver's
read-before-click contract — so pricing the raise would make the button disagree with the row above
it and with the frame below it. Since uniformity is unavailable, the screen must distinguish two
kinds of figure, and §3 puts the distinction where the game already puts it.

**6. Leave the mark printing the event's `to`.** *Its strongest case:* no merged ADR is amended,
the mark keeps a number it prints today, and the figure is the server's own — the purest possible
reading of `ADR-0109` §2's mechanism. *Why it lost:* the same act would carry two different figures
one tick apart on one screen — press `Call 200`, then read `Call 300` on your own plate. That is
precisely the *"two true statements reading as one false hand"* that `ADR-0109` §1 exists to
prevent, and the first thing it would break is the mis-press confirmation that ADR gives as the
reason a player's **own** act is marked at all.
