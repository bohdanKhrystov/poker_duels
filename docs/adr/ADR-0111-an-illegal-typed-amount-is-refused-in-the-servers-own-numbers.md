# ADR-0111 — An illegal typed amount is refused in the server's own numbers

- **Status:** Accepted
- **Date:** 2026-09-02
- **Resolves:** `DEC-118` — **the product owner's** — when a player **types** a bet amount that is
  not a legal raise, what does the table do: refuse the press, clamp to the nearest legal amount,
  or send it and let the server's rejection land? Raised 2026-09-02 by the human — *"player shoud
  be able to make bet using raw text input"* — and registered by
  [`EPIC-13`](../../tasks/epics/EPIC-13-the-living-table.md), whose item 7 it gates. **That the
  field exists is the human's instruction and is not re-litigated here**; this ADR decides the
  illegal case only. Derived from `docs/vision.md` — *On variance*: *"showing a player that they
  lost the match but made the better decisions is more interesting than hiding the maths"*, the
  sentence this repository already reads as *show the numbers rather than smooth them over*
  (`DEC-104`'s routing), and *Positioning*: *"The reference points are **Lichess** and Chess.com,
  not PokerStars."* Applies
  [`ADR-0100`](ADR-0100-the-driver-reaches-an-amount-by-pressing-what-a-player-presses.md) §6
  (*"A dialled total is a proposal, and it is the player's"*) and extends
  [`ADR-0101`](ADR-0101-pot-means-a-pot-sized-raise-and-the-fractions-share-its-base.md) §3
  (*"never clamped"*) to the row's third control. Amends neither.
- **Registers nothing.** No wire message moves, no `PROTOCOL_VERSION`, no engine, server or
  projection change — the mechanism is ordinary client implementation under this behaviour, so
  there is no technical half to split off.
- **Where the numbers came from:** read at `develop` `360bcacf` off
  `poker-engine/src/main/kotlin/duels/poker/engine/game/ActionValidation.kt` and
  `BettingRules.kt`, and `web-client/src/table/ActionBar.tsx`, `rejection-text.ts` and
  `act-frame.ts`.

## Context

The sizing row has never been able to say something illegal. Every chip computes a legal quantity
or is not offered (`ADR-0101` §3), the bar opens at the server's own floor, and `ActionBar.tsx`
renders `<button>` alone — so the server's two amount rejections have been unreachable from this
client's UI since the slider left. The typed field the human ordered is the first control that can
express an amount the rules refuse, and it can express exactly three kinds of illegal: a number
below the floor, a number above the stack, and something that is not a number at all.

The rule the three options differ on is already in the client's hands. `ActionValidation.kt`'s
whole amount check, once an action's type is allowed, is two comparisons — `to < minimum`
(`minBetTo` for a bet, `minRaiseTo` for a raise) is `AmountTooSmall`, and `to > allInTo` is
`AmountTooLarge` — and every one of those bounds is a literal field of the `LegalActions` the
server hands this client in `YourTurn`. `duel-rules.md` §Betting is no-limit, so every whole
number inside the interval is legal; `legalActions` caps both floors at `allInTo`, so the interval
is never empty while a bet or a raise is on offer. When the player types 500 against a stated
minimum of 800, the client is not weighing a rule — it is holding the server's own sentence about
this very turn.

What pulls apart:

- **Clamp** is the big poker clients' convention and is frictionless — but it rewrites the
  proposal. Down from an over-stack typo it sends the gravest act on the table, an all-in the
  player never sized; up from an under-floor typo it commits chips the player never typed.
- **Send it** is the purest deference to the one validator, and `rejection-text.ts` already
  prints the server's answer. But it is not one answer: a non-number cannot take the wire — `Bet`
  and `Raise` carry `to: number` — so the client would first have to invent a number, which is
  the clamp wearing the third option's clothes. And the recovery is expensive: a `Rejected` frame
  re-mounts the bar (`ActionBar.tsx` keys `Live` on the rejection count), the dialled amount
  returns to the server's minimum, and the round trip ends by erasing the very text the player
  typed.
- **Refuse** keeps the proposal the player's and teaches the bound at the moment it matters — but
  it puts a bound-reading in the client, and the bar's own merged law cuts both ways until it is
  read closely: the bar *"works out no amount the server did not send"*. Whether comparing the
  player's number against the server's stated bounds is *working out an amount* is the question
  under the question.

Two shipped precedents face each other. The sizing chips are **withheld, never clamped**
(`ADR-0101` §3 — *"hiding costs a shortcut and never a legal amount"*). The room-code input
**sends and lets the server refuse** — but there the client holds no standing word about which
codes exist (`ADR-0022`'s no-oracle rule), while here it holds the bounds in as many words. The
line that reconciles them is `ADR-0100` §6's: a proposal is the player's and the server validates
it; a statement about the game is the server's and the client only repeats it.

## Decision

### 1. A press with an illegal entry sends nothing, locks nothing, and rewrites nothing

A typed entry that spells a legal total is dialled like any chip's total, and the press sends it —
nothing else about the bar changes here. A typed entry the server's standing word refuses sends
**no frame**: the press takes no sent-lock, the bar stays the player's, and the entry stands
exactly as typed — no reset, no substitution, no nudge toward the nearest legal amount. Pressing
again with the same entry does the same thing; the refusal is safely repeatable (`ADR-0105` §3's
shape).

### 2. The refusal says why, in the server's own numbers, and the control stays live

For a number outside the stated interval the table says which bound was violated, in the
sentences already merged in `rejection-text.ts` — `500 is under the minimum of 800.`,
`5,000 is over the maximum of 4,000.` — with the bound read off this turn's `LegalActions` and
formatted by `formatChips` as everywhere else. The words are merged; whether the module is
literally shared is the implementing ticket's. The sentence stands on the bar **at latest when
the press happens** and may stand earlier — a card may say it live as the entry goes illegal —
but a press is never answered by silence, and the acting control is never a dead button with no
stated reason: the card may mark the state, it may not replace the saying.

### 3. Two kinds of illegal, one rule

The two numeric cases are **one case**: an amount outside the interval the server stated, refused
with the violated bound quoted (§2). A non-number is **a different kind**, not a third bound:
there is no amount to compare and none the wire could carry, so it is refused as

> `That is not an amount.`

— never coerced to zero, to the floor, or to anything else. The sentence explains no rule, names
no blame, and wears no casino furniture: not *error*, not *invalid input*, no exclamation mark.
The empty field is this kind; a negative is not a quantity of chips and is this kind; a plain `0`
is a number and takes §2's minimum sentence.

The one rule over both kinds, and the whole decision in one sentence: **nothing the player typed
is ever rewritten, and nothing the server's standing word already refused is ever sent.**

What counts as spelling a number is the implementing ticket's, inside two bounds: the field may
*read* the product's own printed chip format — digits and `formatChips`' grouping — because
reading is not rewriting; and when the reading is in doubt it refuses as not an amount, because a
wrong refusal costs a retype while a wrong reading costs chips.

### 4. The client's check is a reading, and it may never grow finer than the statement it reads

The check is exactly this and nothing more: the entry is sendable iff it spells a whole number of
chips within `[floor, allInTo]`, where `floor` is `minRaiseTo` when the server allowed `RAISE`
and `minBetTo` when it allowed `BET` — the bar's merged `amountFloor`, unchanged. Those are
literal fields of `YourTurn`. The client derives nothing, so the bar's law — *"works out no
amount the server did not send"* — is intact: the only amounts in the comparison are the player's
and the server's.

If the engine's amount rule ever grows finer than the interval (a later duel discipline with
structured sizes, say), **the client's check does not grow with it**. It keeps refusing only what
`LegalActions` refuses in as many words, and the finer refusals land through the untouched server
path: the client can only ever *refuse* on its reading, never admit past the server, so in any
disagreement the server wins by construction. `rejection-text.ts` is not retired — it stays
load-bearing for everything the reading cannot know (`NotYourTurn`, `HandComplete`, a race, a
finer rule, a bug) and for every client that is not this one, which the roadmap's bot API will
supply.

### 5. No act conversion, ever

A typed `callTo` is not turned into a `Call`; a typed amount at or above the stack is not turned
into an `AllIn`; a refused raise is not downgraded into anything. The player has a `Call` button
and an `all-in` chip; the table does not press them on the player's behalf. The frame that leaves
the bar is always the act the player chose at the size the player set.

### 6. `ADR-0100` §5 stands in full: the driver still presses, never types

The typed field is a real player control and is welcome — it is the opposite of §5's refused
test-only doors. But the guarantee `ADR-0100` bought is untouched: `actThroughTheBar` gains **no
typing branch and sets no field's value**; scripted duels keep reaching every amount by pressing
the sizing row and reading the action button before it clicks, and if a script one day records an
interior amount the repair remains `ADR-0100` §8's stepping search — the field is not the
driver's door to it. The field's own unit tests type into it the way a player would; that is
testing the control, not a driver reaching past the UI.

### 7. The card draws the refusal before the ticket is startable

`EPIC-13`'s design-first rule applies as written: the card that draws the field draws its illegal
states — at least *outside the interval* and *not an amount* — before item 7's implementing
ticket is startable. One constraint carries from `ADR-0100` §2: while the entry is illegal the
action button may print the player's proposal or print no amount, but **never a different
amount** — a corrected total on the button is the clamp coming back through the paint. On every
press-reached state the button's printed total remains exact, so the driver's read-before-click
contract is untouched on every path the driver walks.

## Consequences

**What it buys.** No chips ever move at a size the player did not set — the two clamp accidents
(the unsized all-in, the up-committed typo) are structurally impossible. The bound is taught at
the table, at the moment it matters, in the server's numbers — the vision's *showing the maths*
applied at the exact spot a casino client smooths over. The wire never carries a frame the client
could read as refused, the refusal arrives at typing speed rather than round-trip speed, and one
voice covers every amount refusal, said locally or by the server.

**What it costs, plainly.** The interval is now read in two places — `ActionValidation.kt` judges
it, the bar reads it — and §4's discipline is what keeps the second place a reading rather than a
rule. The sharp edge is the narrow direction: if the bar's reading is ever *stricter* than the
server's rule, a legal amount is wrongly refused and **no server message will ever say so**,
because nothing is sent; nothing in the type system polices this, so it is a place the field's
tests and a QA round must aim at. The server's `AmountTooSmall` and `AmountTooLarge` become
nearly unreachable from this client — from this UI they now signal a client bug, not a player
act — while staying fully load-bearing for other clients and the roadmap's bots. And the friction
is chosen with eyes open: a typo costs a retype where a clamping client would have acted; the
player who wants the table to just fix it does not get that table.

**What it forecloses.** Auto-correction on blur, keystroke masking, and every act-conversion
courtesy — reachable again only by a superseding ADR, not by a card or a ticket. And §2
forecloses the silent dead button: any state in which an act is refusable must say why.

**What it shifts without answering.** `DEC-102` — the stepper's step — was partly *"which legal
totals can the bar reach at all"*; the field makes every legal total reachable in one control, so
a stepper is no longer the only route to interior sizes. The ground under `DEC-102` has moved and
this ADR says so; the question stays open and stays the product owner's.

**The bar gains a state it has never had** — an entry with no sendable amount — and with it a
test surface: a refused press must be shown to send nothing, lock nothing and clear nothing,
which is an invariant `ActionBar.test.tsx` has never needed. The card debt is bounded and named
(§7).

**Reversal is cheap.** Everything here is client-local behaviour behind an unchanged wire; a
future ADR could move to clamp or to send-through by editing one control and its copy, with no
frame, fixture or schema to migrate. The decision is taken on the vision's stance, not because it
is hard to undo.

## Alternatives considered

**Clamp to the nearest legal amount.** *Strongest case:* it is the convention of the major poker
clients, so it meets arriving intuitions; it is frictionless — every press acts, and no error
state exists to draw or to test; and the clamp targets are the server's own
`minRaiseTo`/`allInTo`, so no number is invented. **Why it lost:** it rewrites the proposal, and
`ADR-0100` §6 put the proposal on the player's side of the line — a clamped press sends an amount
the client chose. Its two directions are both wrong in chips: up commits chips the player never
typed; down turns an over-stack typo into the table's gravest act. The sizing row would then
clamp in one control while `ADR-0101` §3 withholds in another — two laws on one row. And the
convention argument points the other way in this product: the reference client answers an illegal
move by putting the piece back, never by playing the nearest legal move for you. Casino clients
clamp because their economics want the bet placed; this product's vision says show the maths
instead.

**Send it and let the server's rejection land.** *Strongest case:* it is the purest form of *the
server is authoritative* — the client ships the proposal untouched, the one validator validates,
`rejection-text.ts` already prints the answer in the server's numbers, and no bound-reading
enters the client at all, so the duplication cost §4 disciplines never exists. **Why it lost:**
it is not implementable as one answer — a non-number cannot take a `to: number` wire without the
client inventing a number first, so this option owes a local refusal anyway and ships two
regimes. It spends a round trip to be told what the client is holding in the very `YourTurn` it
is answering: the server already stated the bounds, and ignoring a standing statement is not
deference. The recovery erases the typed text — a `Rejected` re-mounts the bar and resets the
amount to the floor — so the player retypes anyway, slower and noisier, against a vision that
says *fast* and *quiet*. And the client's own merged philosophy already points here —
*"retrying is how a client turns one refusal into two"* — a client that can read the refusal off
the turn does not manufacture it on the wire.

**Refuse by disablement alone — the action button goes dead while the entry is illegal.**
*Strongest case:* nothing illegal is even pressable, no refusal copy is needed, and it is how
most forms behave. **Why it lost:** a dead control explains nothing — the player who typed 25
learns the bound never — and a table that is right and mute is the exact gap `EPIC-13` opened to
close. The saying is the decision's content; §2 lets a card mark the state and forbids replacing
the sentence with silence.

**Mask the keystrokes — the field refuses to hold an illegal string.** *Strongest case:*
prevention beats correction; the illegal state never exists at all. **Why it lost:** it is
incoherent against a range — `9` en route to `900` is under any floor, so honest masking blocks
legal amounts mid-word — and where it is coherent it is the clamp applied one keystroke at a
time, rewriting what the player is still expressing.
