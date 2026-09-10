# ADR-0148 — A module names every card that draws it, and every card is registered

- **Status:** Accepted
- **Date:** 2026-09-10
- **Resolves:** `DEC-158` — **the architect's** — [`ADR-0142`](ADR-0142-a-text-module-is-checked-against-the-rendered-card.md)
  §7's named reopening trigger has fired: does the **card text ⇒ module** direction now get a
  register, and of what shape? Registered 2026-09-09 while splitting
  [`STORY-1415`](../../tasks/stories/STORY-1415-the-rematch-offer-finds-the-rival-wherever-they-are.md).
- **Answer, in one sentence:** **no** — `ADR-0142` §7's third bullet stands unamended, because the
  drift that fired its trigger was not an instance of the hole that bullet names. What gets a
  register is the **card side of the pairing**, which is where the drift actually lived.
- **Amends:** [`ADR-0142`](ADR-0142-a-text-module-is-checked-against-the-rendered-card.md) §4 — *"A
  module names exactly one card"* is replaced. §5 and §6 are **extended**, not replaced; §1, §2, §3
  and §7 are untouched.
- **Applies:** [`ADR-0091`](ADR-0091-design-gets-no-agent-a-new-screen-owes-a-card.md) §2 and
  [`ADR-0145`](ADR-0145-a-cards-scaffolding-is-composing-and-the-transcriber-names-the-mint.md) §2.
- **Constrains:** `web-client/src/design/card-text.test.ts`, and every future ticket that adds a card
  under `design/screens/` or a `*-text.ts` module under `web-client/src/`.

## Context

**The trigger fired, and it fired on a different wall than the one §7 was watching.** `ADR-0142` §7
names three sentences of doctrine and one trigger. The doctrine: export names are checked both ways
(§5a, §5b); a carded value is checked against the card (§5c); a **card sentence no module carries is
invisible**, and the reason is that a card is mostly not product copy. The trigger: *"the first time
a stale sentence is found on a card."* On 2026-09-09 that happened — and when the three findings are
sorted by which hole each fell through, only one of them is §7's.

Measured on `develop` at `f20d07ed`, the head `DEC-158` was registered against:

| Finding on `rematch-states.html` | Which hole |
| --- | --- |
| `ImKate offers a rematch`, where the client says `Your rival offers a rematch` | The card is **paired with nothing**. That value is `RIVAL_OFFERS`, and `result/rematch-text.ts` names `rematch-panel.html` |
| No frame at all for `That duel room is gone.`, shipped since `TASK-030909` | Same. That value is `ROOM_GONE` |
| `Rematch offered — waiting for ImKate`, where the client says *your rival* | **§7's hole.** That sentence is a bare JSX literal at `RematchControl.tsx:71` and is no module's export |

The first two are not direction failures. They are **cardinality** failures: `ADR-0142` §4 gives a
module exactly one card, `result/rematch-text.ts` spends its one on `rematch-panel.html`, and the
same five sentences are drawn on `rematch-states.html` with nothing watching. Replay the register
that exists today against the card as it stood at `f20d07ed` — the five values
[`TASK-141503`](../../tasks/tasks/TASK-141503-one-module-holds-the-words-both-rematch-surfaces-say.md)
later moved out of `RematchControl.tsx` verbatim, character for character by its own review — and
**three of five are found and two are not**: `REMATCH_LABEL`, `DEALING_LEAD` and `DEALING_TAIL` pass,
`RIVAL_OFFERS` and `ROOM_GONE` fail. `ADR-0142`'s existing forward direction, aimed at the card it
was not allowed to name, catches two of the three findings on its own.

**The cardinality bug is not hypothetical elsewhere either — the merged register already carries a
reason that is false.** `account-text.ts` declares `SIGN_IN_REFUSED` **`notCarded`** with the reason
*"no frame on this card draws a refused sign-in"*. That is true of `account.html`. It is false of the
product: `design/screens/sign-in.html`'s second frame draws *"That handle and password do not match
an account."* — the string, exactly — and its own note says so in as many words: *"`SIGN_IN_REFUSED`
(`account-text.ts`) is the one sentence `SignInForm.tsx` shows…"*. Four more of that module's carded
strings — `HANDLE_LABEL`, `PASSWORD_LABEL`, `SIGN_IN_HEADING`, `SIGN_IN_LABEL` — are drawn on
`sign-in.html` too. The register cannot say any of this, because the module has spent its one card.
So the shape §4 chose is not merely missing a case; it is **producing a passing entry that states
something untrue about the design**, and it will do so again for every module whose screen is drawn
on more than one card.

**And the price of the direction §7 actually names is now measurable, not estimated.** Applying §2's
normalisation to all twelve tracked cards yields **746 text units**. A card text ⇒ module register in
its honest form — every unit either claimed by an export or declared not-copy with a reason — is 746
declared lines, against `ADR-0142`'s bootstrap of twenty-six. Narrowed to only the cards `PAIRS`
names today it is still **153** (`account.html` 54, `rematch-panel.html` 62, `name-ask.html` 37). And
**386 of the 746 are on `duel-table-states.html` and `duel-table.html`**, whose modules —
`table/presence-text.ts`, `table/absent-action-text.ts`, `table/action-text.ts`,
`table/card-text.ts`, `table/rejection-text.ts`, `result/outcome-text.ts` — export **zero** string
constants between them. Every sentence on those two cards is produced by a function, so for more than
half the corpus a card text ⇒ module register has **no export name to point at**: it would be 386
lines of *computed by a function* and *furniture*, checked by nothing.

**A pairing cannot be inferred from the text, so it has to be declared.** The obvious cheap version
of the card side — *a card carrying a module's carded string is that module's card* — is dead on the
first measurement. `rematch-panel.html` draws a **stand-in of the account screen** behind the panel:
`<h3>Account</h3>` and `<div class="row tail">Sign out</div>`, which are `ACCOUNT_HEADING` and
`SIGN_OUT_LABEL` exactly. That is `ADR-0145` §2 scaffolding — *"nothing transcribes it"* — and
inferring a pair from it would oblige `account-text.ts` to put all fifteen of its carded strings on a
rematch card, thirteen of which are not there. The pair is a **statement about authorship**, and the
tree does not contain it.

**`ADR-0145` §2 supplies the split §7 said was missing, and stops one step short of a mechanism.**
`ADR-0142` §7's reason for refusing the reverse direction was that *"separating product copy from
card furniture requires marking it."* Since then, `ADR-0145` §2 has drawn exactly that line —
**subject**, which a coder transcribes, against **scaffolding**, which nothing does — with a stated
test: *name the file that will transcribe it*. So the classification is no longer undefined. It is
still **unmarked**: §2's test is a planner's judgement made per drawing at split time and written
nowhere the file itself can be read for. Carrying it into the tree is the marker register `ADR-0142`
§3 deleted, at 746 units instead of three. `ADR-0145` names the same gap from its own side, in its
costs: *"scaffolding is licensed to reproduce shipped strings and `ADR-0142` §4's one-module-one-card
pairing is what contains that."* That containment is what this ADR is about.

**The measurements this decision is built on**, taken on the worktree branch at `cee03146`, with
`f20d07ed` where noted:

| Fact | Measured |
| --- | --- |
| Tracked cards under `design/screens/` | 12 |
| Text units across all twelve, by `ADR-0142` §2's normalisation | **746**; 153 on the three cards `PAIRS` names; 386 on `duel-table-states.html` + `duel-table.html` |
| String constants exported by the six `table/` and `result/outcome` text modules | **0** |
| Carded values in the merged register | 26, and **all 26 match their card as a whole text unit** — not one relies on §2's substring relaxation |
| `account-text.ts` `notCarded` values found on `account.html` | **0 of 13** — every reason is true of the card it was written against |
| `SIGN_IN_REFUSED` on `sign-in.html` | **present**, as a whole unit, while registered `notCarded` |
| `account-text.ts` string exports | **28** (`ADR-0142` measured 27; one has landed since) |
| Carded values appearing on a card their module does **not** name | **11** — five of `rematch-text.ts` on `rematch-states.html`, four of `account-text.ts` on `sign-in.html`, `REMATCH_LABEL` on `duel-end.html`, and `ACCOUNT_HEADING` + `SIGN_OUT_LABEL` on `rematch-panel.html`'s account stand-in |
| Of those, on a card no row would name after §2 below | **0** |
| `rematch-states.html` at `f20d07ed` against the five values | 3 pass, **2 fail** (`RIVAL_OFFERS`, `ROOM_GONE`) |
| `rematch-states.html` today against the same five | **5 pass** — `TASK-141511` repaired them |

## Decision

### 1. The `card text ⇒ module` direction gets no register. `ADR-0142` §7's third bullet stands

A sentence on a card that no module carries stays invisible to this gate. Nothing below marks card
text, classifies a text unit, or requires a card to declare what its words are. The reason is the one
`ADR-0142` §7 gave, now with a number on it: the register is 746 declared lines, 386 of them against modules
that export no string to name, and its yield at the moment the trigger fired was **one finding of
three**.

This clause is the answer to `DEC-158` as asked. §§2–5 answer what the trigger actually found.

### 2. A `PAIRS` row is one (module, card) pair, and a module may hold more than one row

`ADR-0142` §4's *"A module names exactly one card; several modules may name the same card"* is
replaced by: **a row pairs one module with one card; a module may appear in as many rows as there are
cards that draw its words, and a card may appear in as many rows as there are modules drawn on it.**
§4's other two clauses — *not the frame*, *not the marked block* — are untouched and their reasons
are unaffected.

Nothing else in `ADR-0142` §5 changes shape: a row carries its own `carded` and `notCarded` lists
over the whole module, and that ADR's §5a, §5b and §5c apply **per row**, exactly as written. A module drawn on three cards is
checked three times.

The register becomes six rows:

| Module | Card | `carded` | `notCarded` |
| --- | --- | --- | --- |
| `profile/name-ask-text.ts` | `name-ask.html` | all 6 | — |
| `account/account-text.ts` | `account.html` | the merged 15 | the merged 13, listed |
| `account/account-text.ts` | **`sign-in.html`** | `HANDLE_LABEL`, `PASSWORD_LABEL`, `SIGN_IN_HEADING`, `SIGN_IN_LABEL`, **`SIGN_IN_REFUSED`** | the other 23, defaulted per §3 |
| `result/rematch-text.ts` | `rematch-panel.html` | the merged 5 | `OFFER_STANDING` (§5) |
| `result/rematch-text.ts` | **`rematch-states.html`** | all 6 | — |
| `result/rematch-text.ts` | **`duel-end.html`** | `REMATCH_LABEL` | the other 5, defaulted |

Every cell above is measured green on this branch, and the `sign-in.html` row is the repair for the
false `notCarded` reason §Context names: `SIGN_IN_REFUSED` stays `notCarded` on `account.html`, where
the reason is true, and becomes `carded` on `sign-in.html`, where the sentence is drawn.

### 3. `notCarded` becomes a checked claim, and may be given once for a whole row

Two changes, and they are the pair that makes §2 affordable.

**a. A row may declare `notCardedByDefault: string` — one reason covering every string export not in
`carded` and not in `notCarded`.** `ADR-0142` §5a's partition still holds: every export is
classified. What the
default gives up is a *distinct* reason per export, and it is offered because the alternative on the
`sign-in.html` row is twenty-three copies of one sentence. A row without `notCardedByDefault`
behaves exactly as `ADR-0142` §5a does today, and the two `notCarded` lists that exist keep their
per-export reasons.

**b. Every export classified `notCarded` — listed or defaulted — is asserted absent from that row's
card.** The test asserts that **no text unit of the card equals the export's value**, after
`ADR-0142` §2's normalisation on the card side and the same whitespace collapse its §5c applies to
the value.

**The absence assertion is whole-unit equality, and the presence assertion in `ADR-0142` §5c stays
a substring.** The asymmetry is deliberate and measured: a positive claim should be generous — a
sentence is on the card however it is framed — while a negative claim should fire only when the card
unmistakably draws that exact string, so that `CANCEL` is not reported present by a card saying
*Cancel this duel*. It costs nothing today: all 26 carded values match as whole units, so the strict
relation and the generous one currently coincide.

**b is what makes a defaulted reason safe.** Without it, an export added later and drawn on the
defaulted card would slip under the default silently — the same class of hole one level down. With
it, a defaulted export that appears on the card fails the gate and must be moved to `carded`. This is
also the first assertion the register has ever made about a reason rather than about its presence:
`account-text.ts`'s thirteen merged reasons stop being prose a reviewer must believe.

### 4. Coverage is total over cards, and an unpaired card carries no carded string

The mirror of `ADR-0142` §6, on the other side of the pairing.

**a.** The test globs `design/screens/*.html` and asserts every card is either named by a `PAIRS` row
or listed in a `NO_MODULE` register with a one-line reason. Six cards enter `NO_MODULE`:
`create-duel.html`, `duel-table-states.html`, `duel-table.html`, `duels.html`, `enter-code.html`,
`leaderboard.html`.

**b.** For every card in `NO_MODULE`, **no text unit of it equals any `carded` value of any row** —
whole-unit equality, as §3b. This is what stops `NO_MODULE` from being an excuse list: a card that
draws a shipped sentence cannot be waved off, it must be paired.

**b is what would have caught `DEC-158`'s drift without any judgement at all.** `rematch-states.html`
carries `REMATCH_LABEL`, `DEALING_LEAD` and `DEALING_TAIL` as whole units at `f20d07ed` and today, so
it could never have entered `NO_MODULE`; pairing it is then forced, and `ADR-0142` §5c does the rest. It also
finds `duel-end.html` — which draws the real `Rematch` button — with no prompting and no list of
screens maintained anywhere. Measured: across the seven cards no row would have named, **b** fires
exactly once, on `duel-end.html`, and that hit is a true one.

### 5. `Rematch offered — waiting for your rival` becomes an export, and is carded

`RematchControl.tsx:71`'s bare literal moves into `result/rematch-text.ts` as
`export const OFFER_STANDING = "Rematch offered — waiting for your rival";`, and the component
imports it. It is `carded` on the `rematch-states.html` row and `notCarded` on the other two, where it
is measured absent.

This is a decision about one string, **not a rule**. `TASK-141503` left it inline on a stated ground
— `ADR-0138` §4's criterion is *the same fact stated twice*, and this sentence is the result screen's
alone — and that ground is not overturned: `ADR-0138` §4 lists what both surfaces state and never
said the module may hold nothing else. What is added is a **second, independent** reason for a string
to live in a `*-text.ts` module: `ADR-0142` §6 makes that module the unit the register is kept over,
so a sentence a card draws has somewhere to be checked only if it is an export.

The general form of that sentence — *product copy a card draws belongs in a text module* — is **not
decided here, because it has no mechanism.** Nothing reads JSX, and a rule with no gate is what left
`ADR-0142` §7's bullet reopening in the first place.

### 6. The trigger is re-armed, and re-aimed

`ADR-0142` §7's trigger is spent: it fired, and produced this. The trigger for reopening
`card text ⇒ module` is now narrower and matches the hole it is actually about:

> **A stale or missing sentence found on a card that a `PAIRS` row already names, where the sentence
> is no module's exported string constant.**

The first two of `DEC-158`'s three findings could not fire it — they are this ADR's §2 and §4 now. The
third could have, and §5 removes it rather than counting it; §Consequences says what that costs.
Two occurrences make the register a decision. One more instance of a **module** whose words are drawn
on a card no row names — after §4b, that requires the string to have been reworded on the card as
well — reopens §4b's precision instead, not the direction.

## Consequences

**What it buys.** The drift that fired the trigger is caught by machinery that already existed,
pointed at the card it was not allowed to name: replayed at `f20d07ed`, two of `DEC-158`'s three
findings turn red. A second live instance of the same class — `account-text.ts` against
`sign-in.html` — is closed at the same time, along with a merged `notCarded` reason that says
something untrue about the design and passes. `notCarded` stops being an unchecked excuse: thirteen
merged reasons become thirteen assertions. A new card cannot enter `design/screens/` unregistered,
and one that redraws a shipped sentence cannot be waved into `NO_MODULE`. And `ADR-0142` §5c's *silent pass*
surface shrinks in the direction that matters — a sentence is now checked against every card that
draws it, not against the first card someone paired.

**What it costs.**

- **The register roughly doubles, and the second half is duller than the first.** Three new `PAIRS`
  rows and about twenty-one new declared lines against `ADR-0142`'s bootstrap of twenty-six — and
  where that bootstrap's lines were each a distinct judgement, `notCardedByDefault` makes the new
  ones one sentence covering twenty-three exports. A reviewer reading *"this card draws only the
  sign-in form"* is being asked to accept a claim about twenty-three strings at once, and §3b checks
  that claim mechanically but cannot check that it is the *right* claim. This is the real price and
  it is the one to watch.
- **Every module drawn on N cards costs N partitions, forever.** `rematch-text.ts` has five exports
  and is now checked in three places; adding a sixth export edits three rows. `ADR-0142` charged one
  classification per export; this charges one per export per card, and the multiplier is set by the
  design tree, not by the code.
- **Two whole-unit equality assertions will produce false positives as the design tree grows, and the
  short strings are where.** `Rematch`, `Cancel`, `Sign in`, `Account`, `Handle`, `Password` are all
  carded values one word long. `ADR-0145` §2 licenses scaffolding to redraw shipped strings, so the
  first stand-in of the lobby drawn on an unpaired card will trip §4b, and the escape is to pair the
  card and card the one string — two lines, and an obligation on a drawing `ADR-0145` says nothing
  transcribes. The gate cannot tell a stand-in from the thing it stands in for, and this ADR does not
  give it a way to.
- **§5 removes the one live instance of the hole it declines to close.** `Rematch offered — waiting
  for your rival` was, until this ADR, exactly the sentence §6's re-armed trigger describes; moving
  it into a module means the count starts at zero again. A reader may fairly say that if the reverse
  direction is real, this ADR just delayed finding out — and that is true. The trade is one live
  uncovered sentence closed now against evidence deferred, and it is made deliberately, because a
  sentence left uncovered to serve as evidence is a sentence nothing is watching.
- **Six cards enter `NO_MODULE` with reasons nobody has to defend beyond §4b.** `duel-table.html` and
  `duel-table-states.html` between them hold 386 text units and no string export to pair against;
  their reason will read *"its modules export functions only"*, and it will still read that way when
  the two biggest cards in the tree drift.
- **`ADR-0142` gains a second document.** Anyone reading `ADR-0142` §4 now reads a clause that has
  been replaced, and its §5 describes a register with two fields it does not mention.

**What it forecloses.** Very little, and that is the point of taking it now. §1 forecloses nothing —
the direction it declines is additive, and `ADR-0142` §2's normalisation, which any such register
would be built on, is the part left untouched. §3a is the one clause that gives something up
permanently in practice: a row that adopts `notCardedByDefault` will not be walked back to twenty-three
individual reasons by anyone, so the distinct-reason discipline is spent on that row for good.

**Why this shape, on thin evidence.** One drift instance is a small sample. So the answer is
deliberately the one that spends nothing on the expensive hypothesis: the cardinality repair (§2) and
the card sweep (§4) are re-uses of machinery `ADR-0142` already merged and paid for, and every clause
lives in one test file — the same one-commit reversal `ADR-0142` §Consequences claims for itself.
Nothing here is written into a card, a workflow, a build file or a component's markup, except §5's
single import.

**The deadline is real and runs one way, on §4 alone.** `NO_MODULE`'s bootstrap is sized by the
cards that exist when it is written: six today. `EPIC-14` and its successors ship a card per screen,
and each one that lands before this clause is a card outside the register by default, entering it
later with nobody left who knows whether its words were ever checked. §2's cardinality repair has no
such deadline — it gets cheaper to make, not dearer — but it is also the clause with a **live**
defect behind it, in a reason that reads as settled and is false.

## Alternatives considered

**Build the `card text ⇒ module` register, scoped to paired cards only.** The strongest case: it is
what `DEC-158` literally asks, it is the only option that catches all three of the findings that
fired the trigger, and it is the only one that would ever notice a sentence *added* to a card that
the product never learned to say — which is a real class and the one nobody is watching. Scoped to
the three cards paired today it is 153 declarations, not 746, which is a defensible number. Rejected
on what those declarations would be. Each unit needs a verdict of *copy* or *furniture*, and that
verdict is `ADR-0145` §2's — *name the file that will transcribe it* — which is a judgement about a
future ticket, not a property of the file. Writing 153 of them into a test is the marker register
`ADR-0142` §3 deleted, moved across the fence and multiplied by fifty, and every one of them decays
the way `TASK-140810`'s three did: a duplicate of a judgement, sitting where nothing can check it.
Two of the three findings do not need it, and the third is closed by §5 for two lines.

**Build the full register over all twelve cards.** The strongest case is honesty: a rule that covers
three of twelve cards will be read as covering cards. Rejected at 746 lines, and more decisively on
what 386 of them would say. `duel-table.html` and `duel-table-states.html` are more than half the
corpus, and the modules that render them export no string constant at all — every unit would be
classified against a function, by name, in prose. `design/check-frame-cards.sh` already covers
`duel-table-states.html`'s frames with a measured expectation table, which is the right instrument
for computed text and is not this one.

**Infer the pairing: a card carrying a module's carded string is that module's card.** The strongest
case is that it needs no declaration at all, no `NO_MODULE` list, and no judgement — the tree states
the pairing and the test reads it, which is the cheapest possible version of §2 and §4 together, and
it would have caught `rematch-states.html` with nobody deciding anything. Rejected on the first
measurement: `rematch-panel.html` draws `Account` and `Sign out` as a **stand-in of the account
screen** behind the panel, so inference pairs `account-text.ts` to a rematch card and then demands
all fifteen of its carded strings be on it — thirteen fail, on a drawing `ADR-0145` §2 classifies as
scaffolding that nothing transcribes. A pairing is a claim about who wrote the words, and the text
does not carry it. §4b keeps the useful half of the idea — inference is good enough to *accuse*, and
that is exactly what a `NO_MODULE` guard needs.

**Keep §4's one card per module, and simply repoint `result/rematch-text.ts` at `rematch-states.html`.**
The strongest case: it is a one-word diff, it turns `DEC-158`'s two cardinality findings red at
`f20d07ed`, and it costs no register lines at all. Rejected because it moves the hole rather than
closing it — `rematch-panel.html` then becomes the unwatched card, and `duel-end.html` stays
unwatched either way. It also leaves the `account-text.ts` / `sign-in.html` case with no expressible
answer, which is the instance that shows one card per module is wrong rather than merely tight.

**Take §2's multi-card rows without §3's defaulted reason.** The strongest case is `ADR-0142`'s own,
argued in its last alternative: a hand-list with no completeness assertion covers what someone
remembered on the day, and the twenty-six bootstrap lines were *"the price of the gate being able to
state what it does not cover."* A default is a hand-list with a single excuse at the top. Rejected
because the property that argument defends is preserved by §3b and not by the reasons: an export that
enters the module and appears on a defaulted card **fails**, mechanically, which is more than a
per-export reason ever gave. The reasons were never checked; now the classification is. The
distinct-reason discipline is kept where it is already paid for — the thirteen merged `account.html`
entries stay listed — and spent only where it would otherwise be twenty-three copies of one sentence.

**Use whole-unit equality for `carded` too, and retire §2's substring rule.** The strongest case is
measured: all 26 carded values match as whole units today, so nothing depends on the relaxation, and
one relation is simpler than two. It would also make `ADR-0142`'s named cost — *"a carded sentence
may never span an inline element"* — explicit rather than latent. Rejected because it converts a
cost `ADR-0142` accepted knowingly into a stricter one it did not, on evidence that is only the
absence of a counter-example, and because tightening a *positive* assertion turns the first card that
puts a sentence inside a longer element into a build failure with no honest repair. The asymmetry in
§3 is the point: generous where the claim is *this is on the card*, strict where it is *this is not*.

**Do nothing; §7's third bullet already says the reverse is not enforced, and the sentences are
repaired.** The strongest case: `TASK-141511` has landed, all five values match `rematch-states.html`
today, no defect is live in the words, and `ADR-0142` §7's counter-argument is unanswered — one
instance in twelve cards over four months may simply not pay for a register. Rejected because the
repair is `TASK-141511`'s `verify:` block, which is precisely the thing `DEC-155` was registered
about and `ADR-0142` §1 exists to outlive; because the same class is live *right now* in
`account-text.ts`'s `SIGN_IN_REFUSED`, which passes while saying something false; and because the
cheap half of the answer re-uses a merged gate rather than building one. This alternative is the
right answer to §1's question and the wrong answer to §§2–4's, which is why the decision is split.
