# ADR-0116 — The accept is a door, and a door that does not open spends nothing

- **Status:** Accepted
- **Date:** 2026-09-04
- **Resolves:** `DEC-125` — **the product owner's** — pressing the account offer's accept
  (*"Keep them with a password"*) on the result screen **spends the offer and navigates
  nowhere**. What should the press do, and what becomes of a player who has already spent one
  this way? Raised 2026-09-04 by the driver while landing
  [`TASK-131004`](../../tasks/tasks/TASK-131004-p5-the-account-offers-accept-is-observed.md), from
  a drive, not from planning.
- **Where the answer came from:** derived, not stated by the human. `docs/vision.md`'s
  *Positioning* — *"Dark, quiet, fast, minimal"* — the sentence
  [`ADR-0085`](ADR-0085-not-again-is-this-browser-and-an-answer-spends-the-offer.md) already used
  to settle what spends this offer, and which a control that takes something and does nothing is
  none of; and the *What it is* line the offer exists to protect — ***"One duel coin per win. Not
  chips, not currency, not a balance. A counter of duels won."*** The product half of the answer
  is [`ADR-0036`](ADR-0036-an-account-is-offered-never-required.md), which records the human's own
  call in `DEC-025`: the offer is *"an **offer, not a gate**"*, and its §Consequences makes
  [`ADR-0030`](ADR-0030-a-claim-adds-a-credential-and-moves-nothing.md)'s claim path *"the
  prompt's destination rather than a flow a player has to find on their own."*
- **Applies, and reopens none of:** `ADR-0036` §Decision (offered after a first win, never
  required, dismissal permanent, declining degrades nothing); `ADR-0085` §3's table, §4's
  survival list and §6's refusals;
  [`ADR-0086`](ADR-0086-the-offers-answer-is-one-key-owned-beside-the-predicate-it-feeds.md)
  §§1–5 (the key, its owning module, the sentinel, the failure direction, the gate row) and §4's
  *no way to clear the bit*;
  [`ADR-0112`](ADR-0112-only-a-running-duel-refuses-another-screen.md) §3 (a `FINISHED` room
  honours an ask for another screen) and §5 (the accept lands on the account screen);
  [`ADR-0114`](ADR-0114-one-predicate-answers-every-ask-and-a-mailed-screen-waits.md) in full —
  the ruling predicate, the branch order and the `useLayoutEffect` restore are the mechanism and
  are not touched here.
- **Narrows `ADR-0085` §2 on one clause**, and on nothing else: its first bullet's *"Taking the
  offer spends it"* is read as **taking is reaching the account screen, not pressing toward it**
  (§2). The rest of §2 stands byte-unchanged — *"Not now"* still spends the offer at the press,
  nothing else ever sets the bit, an unanswered offer is not spent, and nothing in the product
  ever clears it.
- **Hands `ADR-0114` §6 the one thing it left to a drive.** That section says the drive settles
  *"which kind of test the implementing ticket writes — a regression test for a defect, or a proof
  of new behaviour."* The drive happened: it is a **regression test for an observed defect** (§6).
- **Constrains:** [`STORY-1311`](../../tasks/stories/STORY-1311-only-a-running-duel-refuses-another-screen.md),
  and nothing else. No wire change, no `PROTOCOL_VERSION` move, no schema, no migration, no new
  endpoint, no new screen, no new control, no new player-facing string, and no storage key added,
  renamed or versioned.
- **Registers no new `DEC`.** The mechanism that makes the press land is already `DEC-123`'s
  answer, merged as `ADR-0114` and landed by `STORY-1311`; the offer's storage is `ADR-0086`'s and
  this ADR asks nothing new of it. *A `DEC` nobody is working is noise in the open table*
  (`STORY-1211`).

## Context

### What was observed

`STORY-1310`'s `P5`, driven 2026-09-04 at commit `1a09fb46` on both a `bare` stack and a
`delayed 300ms` one: pressing *"Keep them with a password"* on the winner's result screen matches
**neither** outcome the record names. Not `ADR-0112` §5's landing on the account screen, and not
its §Context's derived bounce. For at least 15 s after the click, on each layout, three
independent signals never move — `location.href` and `location.hash` stay `http://localhost:5173/`
and empty, the `record` capture of `#root` never mutates past its first entry, and the CDP target
list keeps exactly one `page`. A later reload of the same screen shows the offer gone.

**One inference is flagged in that row and is not treated as fact here.** No control was driven in
which the offer is left un-clicked across a reload, so *the press is what spent it* is derived from
the handler rather than isolated by measurement. §6 closes it with a gate rather than a second
drive.

### What the code says, read this run

`web-client/src/lobby/Lobby.tsx` renders `<AccountOffer onAccept={settleOfferHere} …/>` inside a
result branch that **returns before any chosen-screen branch is reached**, and an effect above it
restores `/` whenever a frame has seated the tab and the screen is not `first`.
`settleOfferHere` writes `ADR-0086` §3's sentinel synchronously; `AccountOffer`'s accept is an
`<a href="/#/account">`, which from `/` is a same-document fragment change and not the page load
`ADR-0086` §6 assumed. So the answer is recorded and the tree the player is looking at never
changes. The state that hides the offer in-tab is set only by the dismiss handler, which is why the
offer stands until the next read of storage and is gone after it.

This is a reading of the source, and it explains the observation. It is not the missing control.

### The tension, which is inside the record and not only inside the code

**The merged record can be read as saying today's behaviour is correct.** `ADR-0085` §2: *"Taking
the offer spends it."* The player pressed the accept; the bit is set; by the letter, the offer was
answered and the only defect is a navigation one. Under that reading `STORY-1311` repairs
everything by making the press land, and the rule that a press may consume the offer while
delivering nothing survives — unwritten, uncontested, and inherited by whatever renders this offer
next.

**The other half of the record says the opposite.** `ADR-0036` §Consequences makes the claim path
*the prompt's destination*. `ADR-0085` §2 says an offer *"rendered and never answered is not
spent"*. And the offer exists at all because `ADR-0036` §Alternatives refused silence: *"a player
who has never been told their coins are device-bound learns it by losing them."* A press that
takes the telling and delivers no destination arrives at the outcome the offer was built to
prevent, one press sooner — and it does it to the player who said yes.

Both readings are available from merged text. That is what makes this a decision rather than a
bug report.

### The population is empty, and that is a fact with a shelf life

Nothing is hosted: `tasks/BOARD.md` carries `EPIC-07 | Infrastructure and delivery | *not
written*`. Every browser that has pressed this control is a fresh QA profile driven by
`scripts/qa/drive.mjs` against a localhost stack. So the second half of `DEC-125` — *what becomes
of a player who has already spent one this way* — has no player in it today. It acquires one the
day something is served to somebody with this defect in it.

### The deadline

Two things here expire, and differently.

**The correction window closes on the day of first deployment, not gradually.** `ADR-0086` §4
gives the product no way to clear the settled bit, deliberately and mechanically, and §4 below
declines to add one. So a browser that presses this control before the repair lands is a browser
that is never offered again, permanently, with nothing able to undo it. While the only such
browsers are the driver's own, the whole cost is zero; after that it is unrecoverable per player.
The ordering that matters is therefore *repair before serve*, and it is cheap only right now.

**The rule is cheapest to write while exactly one surface renders the offer.** One component, one
call site, one handler. Every later surface inherits whatever is written now, including nothing.

## Decision

### 1. The accept is a door, and its whole job is to open it

Pressing *"Keep them with a password"* puts the player on the **account screen** — the same screen
the lobby's account control opens and the address `#/account` names — where a password can be set.
That is the entire meaning of the press: *take me to where I keep them*.

It promises nothing beyond arrival. `ADR-0036`'s *never required* is untouched: the player may read
that screen and leave it without setting anything, no screen gates on the result, and nothing
follows them for declining. `ADR-0112` §5 already ruled this; it is restated here so that the
sentence a reader finds under *what the accept does* is a sentence about the offer, and not a
clause inside a decision about a navigation guard.

The drive does not disturb that ruling. `ADR-0112` resolved a derived collision in `ADR-0086`'s
favour; the observed behaviour turned out to be neither candidate, and the candidate the record
chose is the one that stands.

### 2. The offer is spent where the player is delivered, and a press that delivers nothing spends nothing

**A player never loses the offer without reaching the account screen.** Where the product does not
put them there, the offer stands, and it is made again after their next win — treated exactly as an
offer nobody answered, which is `ADR-0085` §2's third bullet applied to a press that arrived
nowhere.

This narrows one clause of `ADR-0085` §2 and nothing else: *taking the offer* means reaching the
screen it names, not pressing toward it. The asymmetry with the other control is deliberate and is
the reason the narrowing is small — *"Not now"* has nowhere to arrive, so it is complete at the
press and spends the offer there, exactly as merged.

Reaching the screen **in a second tab is reaching it**: `ADR-0086` §Consequences' modified-click
case — a middle-click or cmd-click that opens the account screen in a new tab while the original
tab still shows the offer — is unchanged, and the offer is spent, because the player was delivered.

### 3. An answered offer is not shown again, including on the screen behind it

Once the player has been delivered, the offer is gone: the browser's Back, or any later return to
that result screen, shows the result and no offer. Nothing re-offers it, on that screen or any
other. This is `ADR-0085` §3's table read at the timescale of one press rather than one season, and
it exists so that an answered offer can never be pressed twice.

### 4. Nothing is given back, and nothing is built to give it back

There is no un-dismiss, no *"sorry — here it is again"*, no setting, no support path, no migration
that clears `pd.accountOfferSettled`, and no rename of that key to re-offer the browsers holding
it. `ADR-0085` §2's last bullet and `ADR-0086` §4 stand exactly as written. The standing way to an
account is the one that already exists and is already rendered: the account screen's own address,
and the control the lobby draws for it.

The repair available to a player who pressed this control is therefore §1 — landing the press
before anyone can press it — and nothing else. Today that costs nothing, because the browsers
holding a bit they got nothing for are the driver's own QA profiles and nothing is served to
anybody.

**The word, chosen rather than inherited.** The record's verb is *spend*, and a spend is an
exchange: something is handed over for it. What today's press does is not a spend and this ADR
does not call it one — the offer is **taken, and nothing is handed over**. Nor is it a *forfeit*
(`ADR-0046` §5, re-applied by `ADR-0108` §3): nothing was owed, no rule was broken and the player
did nothing wrong. Naming it correctly is most of why §2 reads as obvious.

### 5. The offer is shown only where its accept can be honoured

The offer renders on the result screen, which means a held `FINISHED` room, which `ADR-0112` §3
honours and `ADR-0114`'s `room-standing.ts` reads as `finished`. So §2 costs nothing to satisfy
today: **there is no state in which the offer is visible and the ask would be refused or held.**

That is a condition, not a coincidence. A surface that would render this offer where the ask is
refused or held may not render it at all — it shows the offer where the door opens, or it does not
show the offer.

### 6. What the repair owes, and the one inference it closes

- **The kind of test is settled by the drive**, which is the question `ADR-0114` §6 left to it: a
  **regression test for an observed defect** — red on the tree at `1a09fb46`, green on the repair —
  not a proof of new behaviour. It is non-browser (`ADR-0089` §2), because a browser may not stand
  between a pull request and `develop`.
- **Two facts, proved separately**, because the record currently has one of them by inference:
  that the press reaches the account screen, and that an offer which was **rendered and not
  pressed** is still offered afterwards. The second is already merged law — `ADR-0085` §2, *"not a
  reload"* — and what is owed is a gate that would go red if it were false, not a second drive.

Which file, which framework and which seam are the implementing story's and the architect's;
`ADR-0114` §7 already fixes how such a proof is reached without a `data-testid`, a test-only prop
or an exported setter.

## Consequences

**What it buys.** The offer's four documents now answer the question a reader actually arrives
with — *what happens when I press this?* — with a sentence about the player rather than about a
guard. The accept and the arrival become one act, so the failure mode that produced `DEC-125`
cannot be reintroduced by repairing navigation alone. The narrowest possible amendment does it:
one clause of one bullet, with the dismiss path, the storage, the key, the table of cases and every
refusal untouched. And the whole answer is reversible — it adds no field, no key, no string and no
surface, so a later ADR that disagrees changes a sentence and a test.

**What it costs.**

- **A player who accepts and never sets a password is still never offered again.** §2 moves *when*
  the offer is spent, not *what* spending costs. `ADR-0085`'s sharpest named cost is re-paid here
  in full, and §4 refuses the repair for it a second time.
- **Every browser that pressed this control before the repair keeps a bit it got nothing for**, and
  the product will not correct it. That is written as a rule and not as *"it does not matter"*,
  because the rule has to hold on the day one of those browsers turns out not to have been the
  driver's.
- **§2 has no enforcement of its own.** It is satisfied by construction (§5), so nothing in the
  client refuses a press that would not land. The day a surface breaks §5, the failure is exactly
  today's failure, and the only things standing against it are this ADR and one regression test on
  one surface.
- **§3 costs a piece of state honesty.** The offer disappearing *behind* a returning player means
  the result screen they come back to is not the one they left. Accepted, because a second press of
  an answered offer is worse than a screen that changed while the player was elsewhere.
- **A fourth document about one prompt.** `ADR-0036`, `ADR-0085`, `ADR-0086`, and now this. A
  reader who finds any one of the first three alone gets an answer that is right in its own terms
  and incomplete — the standing cost of amendment, paid again.
- **This ADR fixes nothing by itself.** §1's behaviour arrives as a side effect of `ADR-0114`'s
  branch order landing in `STORY-1311`. If that story slips, the accept keeps taking offers on
  every tree anyone drives, and this document only makes the absence legible.

**What it forecloses.**

- **Any *"you accepted and did not finish"* follow-up** — no second prompt, no badge, no reminder
  on the account screen, no mail. `ADR-0085` §6 forbade it; §4 re-refuses it in the one case where
  sympathy for the player is strongest, which is where such rules are actually tested.
- **Any repair by storage.** The key is not renamed, not versioned and not cleared, so there is
  permanently no way to tell a browser that answered from a browser this defect answered for. That
  is not free to reverse: `ADR-0086` forecloses both a second value format and a second key on its
  own terms.
- **Spending the offer on the account screen's own load**, as a way of being certain the player
  arrived. `ADR-0086` §6 refused it for a reason that still holds — the lobby's control reaches the
  same screen and would settle an offer that was never made — and §5 removes the need for it.

## Alternatives considered

**Read `ADR-0085` §2 literally: the press is the answer, wherever it lands.** The strongest case in
the file. It is what the merged sentence says, so it needs no amendment at all; it keeps the two
controls symmetrical, which is the whole shape of *an answer spends the offer*; and once
`STORY-1311` lands, the press and the arrival coincide anyway, so the distinction is invisible and
the clause is dead weight. Rejected because the invisible case is the one that just happened. Under
that reading today's behaviour is *correct*, and a reader repairing only the navigation would leave
in place the rule that produced the defect: that a press may take the offer and deliver nothing. A
rule that is only ever exercised when something else is broken is worth exactly the moments
something else is broken, and this product has just had one.

**Rename the key, so every browser holding a settled bit is offered again.** The cheapest repair
available: one string in one module and one row in `ADR-0086` §5's gate. It adds no clearing export,
so `ADR-0086` §4's mechanical guarantee survives intact, and it repairs the harm exactly, for
exactly the players who suffered it. Rejected because it cannot tell them apart from the players who
pressed *Not now*. A rename re-offers **everybody**, breaking *not again* — the half of `ADR-0036`
stated as a rule precisely because it erodes under pressure — in order to repair a population that
is empty. It would also make storage a routine instrument of repair, and the next defect would
reach for it without an ADR.

**The accept does not spend the offer until the browser holds a credential.** It repairs
`ADR-0085`'s own sharpest named cost — *"a player who accepts and abandons is never asked again"* —
it makes the offer a promise about an outcome rather than about a press, and it is what `ADR-0056`
§5's parenthetical originally said. Rejected because `ADR-0085` considered this exact rule under
the name *"Only 'Not now' spends it"* and rejected it for a reason a defect does not change: a
prompt that returns after every win, forever, to the player most likely to sign up, which is
`ADR-0036` §Decision's *"reminder badge that never goes away"* in another shape. Reversing that
needs an ADR superseding `ADR-0085` on evidence about players, and this product has no players.

**Tell the player the press failed** — an error line on the result screen when the account screen
cannot be reached, with the offer left standing. Honest, never silent, and a shape this client
already has: `ADR-0111` refuses a press in words on the table. Rejected as a surface invented for a
state that cannot exist under §5. The offer is only ever shown where the ask is honoured, so the
line would be unreachable copy in every state the product can produce — and unreachable copy on the
quietest screen the product has is exactly what *"Dark, quiet, fast, minimal"* refuses. §2 makes
the failure impossible instead of narrating it.

**Put the password form on the result screen instead of navigating** — a modal, so there is no
navigation to fail. The shortest path from *I want to keep them* to *they are kept*, and it deletes
this whole class of defect rather than repairing one instance. Rejected twice over. It reopens two
merged decisions on a defect's evidence — `ADR-0036` §Consequences' claim path *as the prompt's
destination*, and `ADR-0112` §5's landing. And it is the wrong role's answer: a modal is a new
surface owing a design card (`ADR-0091` §2), which is a story, not a clause in an ADR about what a
press means. If the product ever wants it, it will want it for a reason other than this one.

## What this does not settle

- **Whether the offer's disappearance in `P5` had any cause other than the press.** The source
  reading in §Context explains it and `ADR-0085` §2 already forbids the alternatives (*"not a
  rematch, not a reload"*), but no drive isolated it. §6 closes it with a gate rather than a second
  drive, and if that gate ever goes red for a reason other than the press, that is a new
  observation and a new `DEC`.
- **What the account screen shows a player who arrived from the offer.** Nothing new: it is the
  screen the lobby reaches, unchanged, with no new string and no memory of where the player came
  from. Named because *"you came from the offer"* is the obvious next thought and nobody has asked
  for it.
- **Whether anything ever measures the offer.** Still nothing (`ADR-0085`'s foreclosure), and this
  ADR adds no count of presses that landed nowhere — which is the one number that would have found
  this defect without a browser. Named, and refused.
- **The signed-out account holder**, exactly where `ADR-0085` §Consequences left it.
- **The five paths of `ADR-0112` §6 still undriven.** `P2`, `P3`, `P4`, `P6a` and `P6b` remain
  `STORY-1310`'s, and nothing here anticipates what they will find.
