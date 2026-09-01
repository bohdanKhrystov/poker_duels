---
id: STORY-1213
title: Round 1 (audit) — three criteria unmet, and a table that names the wrong winner
type: story
status: ready
parent: EPIC-12
labels: [process, qa, audit]
depends_on: []
---

## The round

**Round 1** of the first `/qa-cycle audit smoke` invocation, and the first round this cycle has run
under the **audit** focus. It is the round `STORY-1212` reserved `STORY-1213` for.

| | |
| --- | --- |
| Round | **1** of at most 3 (`EPIC-12` §Termination rule 5) |
| Focus | **`audit`** — the frozen rubric of `ADR-0096` §2, answered beat by beat (`ADR-0096` §1) |
| Scope | `smoke` — the eight beats of `docs/vision.md`'s first success condition |
| Date | 2026-09-01 |
| Commit walked | `e5e97f2a` — confirmed at triage as `origin/develop`'s head, not a working tree |
| Stack | `up` — db, server, web |
| Shapes | **phone 390 × 664**, all 8 beats, both browsers; **laptop 720 × 900**, `R2`/`R3` at beats 2/3, 4, 5, 6 (`ADR-0096` §4) |
| Rubric in force | `ADR-0096` §2 as merged at `e5e97f2a` — **five criteria**, `R1`–`R5`, frozen for the invocation (`ADR-0096` §3) |
| Criteria answered | **5 of 5**, at every beat — nothing `BLOCKED` |
| `A(1)` | **3** — `R1`, `R2`, `R4` |
| `A(0)` | **n/a** — round 1 has no round 0, exactly as it has no `B(0)` |
| Verdict | **`PROCEED`** — unqualified; no criterion went unanswered at any beat |
| Fix set | **3** — `TASK-121301`, `TASK-121302`, `TASK-121303`, in the rubric's own order |
| Also filed | **1** — `TASK-121304`, a functional defect, **outside the fix set and outside `A(1)`** |
| Proposed criteria | **2**, routed as `DEC-103` and `DEC-104`; **neither reaches the rubric before the next invocation** |

## What this record is not

`ADR-0089` §2c and `ADR-0093` §2, restated because a new focus is the easiest place for an old
misreading to re-enter:

> No coverage claim. The product of a run is a **dated round record**. Neither it nor
> `docs/test-plan.md` may be cited as coverage in an epic's `Metrics`, a Definition of done, or a
> ticket's `verify:`.

`ADR-0096` §5 adds the audit's own version, and it binds the *other* verdict too: a `PASS` under
this focus would mean **only** that every criterion in the frozen rubric was met, at every beat, at
one commit, on one machine, at the two shapes §4 names. This round is not a `PASS`. It is a
`PROCEED` with three unmet criteria and one functional defect, and the honest summary is that a
player on a phone cannot see the buttons they are being asked to press, cannot see the board come
out after an all-in, and — in a second duel — is told the wrong player won.

## Per-criterion — five answered, three `not met`

Criteria are cited by id and by the section that states them (`ADR-0099`): `R1`–`R5`,
`ADR-0096` §2. **No criterion text is transcribed here** — `ADR-0099` forbids a second register,
and a round record that quoted the rubric would be one.

| id | verdict | beats answered | shapes | where the observation is |
| --- | --- | --- | --- | --- |
| `R1` | **not met** | 1–8 | phone | §*`R1`* — beat 5 only: the all-in runout |
| `R2` | **not met** | 1–8 | phone, laptop | §*`R2`* — beats 2/3, 4, 5, 6 at phone; met at laptop, and one bar means the criterion is not met |
| `R3` | met | 1–8 | phone, laptop | — |
| `R4` | **not met** | 1–8 | phone | §*`R4`* — beat 1 only: the front door's three doors |
| `R5` | met | 1–8 | phone | — |

**No cell reads `BLOCKED`**, so the verdict line carries no qualification. The observer's own
`BLOCKED:` section reads *None*, and it is confirmed rather than assumed: all eight beats were
walked on both browsers, and `R2`/`R3` were re-answered at laptop shape at every beat presenting a
betting decision.

**`R3` and `R5` are `met` and carry no observation, and that is correct rather than thin.**
`ADR-0096` §2 requires a quoted observation for a `not met` and asks nothing of a `met`. A round
that invented evidence for a pass would be doing the thing §2's *"never `this feels wrong`"*
forbids, in the other direction.

## Step 2 — every `not met` reproduced, and every one by a **different mechanism**

`ADR-0089` §4 carries into this focus with nothing subtracted (`ADR-0096` §2): an unmet criterion a
looking human cannot reproduce is a **harness defect**, filed against `EPIC-12`, repaired in
`scripts/qa/`, and excluded from every count. So each of the three was re-derived at triage.

**The reproductions vary the mechanism, not the operator.** Re-running the observer's own reads
would prove nothing about the observer's reads: a hand-check that shares the broken step inherits
the fault. Every reproduction below is a **source reading**, made without a browser.

### `R1` — the runout is unperceivable because nothing in the client can perceive it

The observer's evidence is a `record`/`frames` log showing frame *N* at `Preflop`, no board, and
frame *N+1* at `Hand complete` with five cards and a winner named — the same jump on both tabs.
The independent reading is a two-step one, and it is stronger than the frame log because it shows
that **no** instrument could have caught an intermediate frame:

1. **The server sends one snapshot per transition.**
   `poker-server/…/duel/Addressed.kt`'s `broadcast` builds, per seat, one `Events` frame of the
   new events and then *"Always emit Snapshot frame, which is the authoritative last word on
   state"* — one `Snapshot`, from the post-transition `GameState`. `DuelTurn.kt`'s `framesFor` is
   the only caller. An all-in call is **one** transition: `StreetProgression.kt`'s `runOutBoard`
   deals flop, turn and river inside it, so the five board cards and the award are all in the one
   post-transition state.
2. **The client has no path from a street event to the screen.**
   `web-client/src/table/DuelTable.tsx` renders `<BoardCards cards={view.board.cards} />` — the
   board is read off the latest `PlayerView` and nothing else. `grep -rn "StreetDealt"` over
   `web-client/src/table`, `web-client/src/store` and `web-client/src/lobby` returns **nothing**:
   no component, no reducer branch, no selector reads the event.

So the engine emits each street as its own event *"so the log reads like the deal it was"*
(`ADR-0008`, cited by `R1`), and the two layers above it collapse the sequence into one paint. The
finding **reproduces**, by construction, and it is a **product defect**.

### `R2` — the client took the card's width and not the card's height

The observer's evidence is measured geometry at four decision beats: `scrollHeight` 885 / 868 / 866
against `clientHeight` 664, and a `getBoundingClientRect()` `bottom` of **820.578** on the Fold and
All-in buttons against a **664** viewport. The independent reading is a source diff between the
merged card and the client:

- `design/screens/duel-table.html` draws the table as
  `.table { max-width: 560px; min-height: 100vh; min-height: 100dvh; margin: 0 auto; … }`.
- `web-client/src/lobby/Lobby.tsx:166` renders
  `<div className="mx-auto flex max-w-[560px] flex-col gap-5">` — the **width** and the centring,
  and **not** the height.
- `grep -n "dvh\|100vh\|sticky\|fixed bottom\|overflow"` over `Lobby.tsx`, `App.tsx` and every
  `web-client/src/table/*.tsx` returns **nothing**.

The column therefore has no viewport-height budget anywhere, so its height is the sum of its
content, and the action bar is whatever is left over. That is exactly the 885-against-664 the
observer measured, and it explains why the same layout passes at 720 × 900: at that shape the sum
happens to fit. The finding **reproduces**, and it is a **product defect**.

**The observer's own laptop reads are the control that rules out a broken instrument.** The same
`eval` and the same `getBoundingClientRect()` returned 900/900 and every control visible at laptop
shape, at the same four beats, in the same session. An instrument that reports overflow at one
width and none at another, on the same page, is measuring the page.

**One bar, checked twice — the phone answer decides.** `ADR-0096` §2 says so in as many words:
*"A criterion is `met` only if it is met at every shape it was answered at"*, and *"a product that
must scroll to show the amount to call is `R2` `not met`, whether that happens at 390 px or at
720"*. Reading the laptop pass as a partial credit would be inventing the relaxed phone bar §2
forbids, and it is named here so no later round tries.

### `R4` — three inline buttons with nothing between them

The observer's evidence is a 4× zoom crop of the front door's nav row rendering as
`Your duelsLeaderboardAccount`, and it pre-empts the obvious objection itself: the same screen's
`Room codeJoin the duel` also runs together in a text dump and is *visibly two controls* in the
same screenshot, so the crop is showing a rendered fact and not a dump artefact.

The independent reading is the markup. `web-client/src/lobby/Lobby.tsx` renders the front door as
a bare `<section>` — the **only** branch of that file whose `<section>` carries no class at all;
its six siblings (the `duels`, `leaderboard`, `account`, `sign-in`, `verify` and `reset` branches)
each carry `className="mx-auto flex w-full max-w-[380px] flex-col items-center gap-4"`. Inside it
the three doors are three adjacent `<button>` elements:

```tsx
<button type="button" onClick={() => open("duels")}>{HISTORY_HEADING}</button>
<button type="button" onClick={() => open("leaderboard")}>{LADDER_HEADING}</button>
<button type="button" onClick={() => open("account")}>{ACCOUNT_HEADING}</button>
```

JSX elides whitespace between elements when it contains a newline, so **no text node is emitted
between them**; a `<button>` is inline-level by the user-agent default and Tailwind's preflight
does not change it; and the parent has no flex, no gap and no layout of any kind. Three inline
boxes with no whitespace and no gap abut. The finding **reproduces**, and it is a **product
defect**.

**`ADR-0098` is the merged precedent for reading this as an `R4` failure rather than as a taste
call.** Its settlement — the wordmark's `aria-label`, because *"the card's markup has no text node
between the two spans, so a screen reader's own concatenation reads `PokerDuels`"* — is the same
defect one register over, decided nine days ago in the product's favour.

## `A(1)` = 3, and what the count deliberately leaves out

`A(N)` counts **criteria answered `not met`**, never observations (`ADR-0096` §5). `R2` failed at
four beats and `R1` at one; that is two unmet criteria, not five, and each ticket names every beat
its criterion failed at. So `A(1) = 3`, and the ceiling was **5** — the rubric's size — before the
round started.

| class | count | in `A(1)`? | why |
| --- | --- | --- | --- |
| criteria `not met` | **3** | yes | `R1`, `R2`, `R4` |
| criteria `met` | 2 | no | `R3`, `R5` |
| **harness defects** | **0** | no — excluded | `ADR-0089` §4 / rule 6, carried into this focus by `ADR-0096` §2. All three unmet criteria reproduced from source, without a browser. Stated with a zero because a rule recorded only when it bites is one the next round forgets |
| **functional defects** | **1** | **no — excluded** | `ADR-0096` §5: *"an audit round reports `A(N)` and no `B(N)`"*. `TASK-121304` is a product defect the walk stumbled on, not an answer to a criterion. Counting it would make `A(1)` measure two things at once |
| **proposed criteria** | **2** | no | `ADR-0096` §3: a proposal is not a finding, it is a routed question. Neither is in the rubric this round judged against, and neither may be (`ADR-0096` §3 — the rubric is frozen for the invocation) |
| repeats | **0** | no | see §*Dedupe* |
| regressions | **0** | no | see §*Dedupe* |

**Nothing was deferred, and rule 3's cap never came near binding.** Three criterion tickets plus one
functional ticket is four; the cap is eight. The rubric-order tiebreak `ADR-0096` §5 defines is
therefore stated but not exercised: had the cap bound, repair would run `R1`, then `R2`, then `R4`,
top to bottom, with no judgment in it. The fix set is written in that order anyway, so the ordering
rule is visible in the ledger the first time it could have applied.

**And a deferral could not have shrunk the number even if one had been made.** Under this focus a
deferred finding *stays* an unmet criterion and is counted again next round: filing does not reduce
`A(N)`, only repair does (`ADR-0096` §5). The `qa`/`uat` cheat rule 4 forbids — defer to shrink the
count — is not merely forbidden here, it is inoperative.

**There is no severity in this section and none anywhere in the fix set.** `EPIC-12` §Termination
rule 2 is scoped by `ADR-0096` §5 to the `qa` and `uat` focuses; under this focus a criterion is
`met` or `not met` and there is nothing to argue about. The three criterion tickets carry no
severity label. `TASK-121304` does carry one — it is a `qa`-focus defect and rule 2 governs it word
for word — and §*The functional defect* says why that is a distinction and not an inconsistency.

## Dedupe — one ledger, three focuses, zero repeats

`ADR-0092` §6 and `qa-manager` §Step 1: matched on **behaviour**, not on wording and not on which
focus saw it. Searched: every story under `tasks/stories/` and every `TASK-12NNNN` under
`tasks/tasks/` — eight stories under `EPIC-12` and their 68 tickets.

**Every `EPIC-12` ticket is `done` or `dropped`. There are no `status: backlog` tickets left in this
repository at all** — `grep -rl "status: backlog" tasks/` matches `BOARD.md`, `README.md`, three
templates, `EPIC-12` itself, `STORY-1104` and `TASK-121205`, and **no ticket's front matter**. So
`ADR-0096` §5's promotion mechanism — *"where an unmet criterion's repair is already a
`status: backlog` ticket from a `qa` or `uat` round, move that ticket into the audit round's fix set
rather than filing a second"* — was **searched and came back empty**, and all four tickets below are
new. The seventeen rows that ADR was written about were repaired between 2026-08-30 and 2026-08-31;
the mechanism is not exercised in the first round that could have exercised it, and that is worth
one sentence in the trail rather than silence.

**Repeats: 0. Regressions: 0.** The two candidates were checked rather than assumed, because a
regression is never below `high` and grading one wrong is the fastest route to a wrong verdict:

- **`R1` against `TASK-121101`/`TASK-121109` (both `done`) — not a regression, and not a repeat.**
  Those two shipped `ADR-0095`'s hand-result banner: *the table says who won the hand it just
  finished*. `R1`'s finding is about the **runout** — flop, turn and river leaving no perceivable
  trace — which is a different behaviour on a different set of events (`StreetDealt`, not
  `PotAwarded`) and was never in either ticket's scope. Neither ticket's gate came back red.
- **`R4` against `DEC-094` (open, the product owner's) — not a repeat, and it does not block.**
  `DEC-094` asks whether the same three doors should **wear the client's control vocabulary**.
  `R4`'s finding is that they are not **separated**. A door can be bare and legible or dressed and
  illegible; the two are independent, and separating them settles nothing `DEC-094` asks. See
  §*Verdict* on why this is not `STOP_BLOCKED`, and `TASK-121303`'s *Out of scope* on how the
  repair is kept from pre-empting the answer.

## The functional defect — the table names the wrong winner, and it is `high`

**This is the most consequential thing in the report and it does not touch `A(1)`.** Both halves of
that sentence are load-bearing, so both are argued.

### What it does

`FUNCTIONAL:` reports three hands of one duel — the **second** duel of room `55MF2W6G`, started via
Rematch — in which the hand-result announcement disagreed with the actual result while the chips
themselves moved correctly:

| hand | what actually happened | what both tables said |
| --- | --- | --- |
| 1 | seat A's pair of nines beat seat B's ace-high; A took the whole 200 | *Split pot — you win 100* |
| 2 | A's ace-high beat B's queen-high; A took 200 | A's own screen: *Your rival wins 200*. B's own screen: *You win 200* |
| 3 | B raised, A folded; B took 300 | both screens: *wins 19,800* |

The winner is told they lost, the loser is told they won, and an amount from an unrelated earlier
hand is printed as this hand's pot.

### The mechanism, derived from source and not from the browser

Two merged facts compose into it:

1. **`web-client/src/store/duel-state.ts:161` appends to `narration` and nothing ever clears it.**
   `case "Events": return { ...state, narration: [...state.narration, ...message.events] }` is the
   only write outside `initialState()`. `DuelFinished` clears `outcome`, `pendingTurn`,
   `rejection`, `refusal`, `rematchOffers` and `serverAction`, and leaves `narration` standing — a
   merged test, `duel-state.test.ts > leaves the view and narration untouched`, pins that. So a
   rematch in the same room accumulates **both duels'** events in one array, while the field's own
   type comment says *"every event of the whole duel"*.
2. **`web-client/src/table/PotStrip.tsx:30` finds the window with `findIndex`** — the **first**
   `HandStarted` whose `handNumber` matches the view's. In the second duel, hand *N*'s window is
   therefore the **first** duel's hand *N*.

That predicts all three rows of the table above, exactly:

- duel 1's hand 1 was a split — two `PotAwarded` of 100 — so both viewers read
  *Split pot — you win 100*;
- duel 1's hand 2 was one award of 200 to seat B, so A reads *Your rival wins 200* and B reads
  *You win 200*, which is what each was shown;
- duel 1's hand 3 was the all-in, and the report's **own `R1` observation** independently quotes
  that frame — `Your rival wins 19,800 | Blinds 50/100 · Hand 3 · Hand complete`. The 19,800 in the
  functional section is that hand's pot, read out of the previous duel.

Three for three, from two source lines, with the two halves of the report cross-checking each other
without either knowing it. The defect **reproduces** and needs no seed, exactly as the observer
says.

### Severity: `high`, and the reason is `EPIC-12`'s own word

`EPIC-12`'s `high` row is *"a core vision promise is broken — hole cards leak, **wrong winner**,
coins wrong, rematch dead"*. A table that tells the player who won that their rival won is the
plainest reading *wrong winner* has. It is filed `high` and it is **not** `blocker`: the duel
completes, the pot is credited correctly, the stacks and coins are right, and the player can keep
playing — so *"the product cannot be used for its purpose; data loss; an unescapable hang"* is not
met. `medium`'s test — *a real defect with a workaround* — does not fit either: the workaround
would be *do arithmetic on your own stack rather than believing the screen*, which is the product
asserting a falsehood and the player catching it.

**It is worse than the silence it replaced.** Round 3 of the UAT invocation graded the *absence* of
this banner `medium`, on the argument that the outcome still reached the player three other ways.
That argument held for a blank; it does not hold for a wrong sentence, and this record says so
rather than inheriting the grade.

### Where it is filed, and why it is not in `A(1)`

`ADR-0096` §5: *"A functional defect an audit round stumbles on is filed to the one ledger and
enters the next `qa` round's `B(N)`, never the audit's count — … each count must measure one
thing."* So:

- **Filed to the one ledger**, as `TASK-121304` under this round story. One ledger, one ticket,
  whichever focus saw it (`ADR-0092` §6).
- **Excluded from `A(1)`.** `A(1)` counts criteria; this is not a criterion. Folding it in would
  make the number mean *criteria unmet, plus defects*, and `A(N) ≤ |rubric|` would stop being true.
- **Not a member of the audit fix set.** That set is the criterion repairs, and `ADR-0096` §5
  orders it *"by the rubric's own order"* — an ordering a ticket with no rubric position cannot
  take.
- **Scheduled in this round's repairs anyway**, `status: ready`. The sentence about `B(N)` is an
  **accounting** rule, not a scheduling one: it says which count measures the defect, not when it
  may be fixed. `EPIC-12` §Termination rule 1's eligibility test is *"defects in round *N*'s QA
  report"*, and this is in round 1's report; rule 3's cap is eight and the round has four. Holding
  a *wrong winner* back for a hypothetical later `qa` round, in order to have something for a
  count to measure, would be the cycle serving its own arithmetic. If it is repaired here, **no
  `B(N)` ever counts it** — which is the outcome the rule wants, not an evasion of it.

## Proposed criteria — two, routed, and neither reaches this rubric

`ADR-0096` §3 allows at most three per round, each a **general standard** rather than an
observation, recorded in the round story and routed exactly as `ADR-0092` §5 routes a question. The
observer proposed **two**; a third is not invented to fill the budget.

**Neither is in the rubric and neither may be.** `ADR-0096` §3: *"No round may add a criterion to
itself, or to a later round of the same invocation. A criterion merged mid-invocation applies to
the next invocation."* And by `ADR-0099`, a criterion is **born merged** — an amending ADR that
states it in `ADR-0096` §2's own three-column form, restates the resulting priority order as ids
only, and annotates `ADR-0096`'s index row *rubric grown to 6* in the same PR. `R6` is the next
free id. Nothing in this story is that ADR, and this triage adds nothing to the rubric.

### `DEC-103` — the product owner's — the compound label that wraps mid-phrase

> *May a compound status label break across a line so that a bare numeral or symbol is stranded
> away from the word or unit it belongs to?*

Observed at phone width: `Blinds 50/100 · Hand 1 · Preflop` wraps so that a bare `1` sits alone on
its own line. **Routed to the product owner**, because `docs/vision.md` settles it: *Positioning*'s
*"Dark, quiet, fast, minimal"* and the Lichess/Chess.com benchmark the human qualified as *"not a
licence to be less finished"* are the same source `R3` and `R4` are already licensed by, and this
proposal is their sibling — it asks about the **integrity of a rendered phrase**, which is the
territory `R4` already occupies at the level of spacing.

Shape: it is a general standard, stated as one, with an observation cited as evidence rather than
as the claim. That is the form `ADR-0096` §3 asks for.

### `DEC-104` — the product owner's — what the number labelled *Pot* counts

> *Should the pot the table shows a player include the chips committed on the current street, or
> only the chips already swept in from completed streets?*

Observed: `Pot 0` stands for a whole betting street after both seats have committed chips, and moves
only when the next street is dealt. **Routed to the product owner**, because `docs/vision.md`
settles it twice over: *What it is*' first line — *"Heads-up Texas Hold'em"* — is the licence
`ADR-0101` already used to rule that *a control's word is the game's word*, and *On variance*'s
*"showing a player… is more interesting than hiding the maths"* is what `R1` is licensed by.

**Two things the answerer must be handed with it, because neither is obvious from the proposal:**

1. **The product already computes both numbers and shows different ones in different places.**
   `Lobby.tsx` passes `potIncludingStreet` to `ActionBar` — `ADR-0101` §*base* is
   `view.pot + both seats' committedThisStreet` — while `PotStrip` prints `view.pot`. So a *yes*
   would make two numbers on one screen agree, and is not a new quantity.
2. **A *yes* is not a client change.** `view.pot` is a server-authoritative field; a client that
   summed it would be asserting a game fact, which `CLAUDE.md`'s non-negotiables and `ADR-0002`
   forbid and `web-client/src/table/no-derivation.test.tsx` actively gates. The *what* is the
   product owner's; the *how* would be the architect's afterwards, and the answer should say so
   rather than specify an implementation.

Shape: general in its claim, observation-shaped in its evidence. It is recorded and routed as
written rather than reworded, and the shape is named so the answerer can push back on it if the
proposal is really a defect report about one number.

**Neither is a finding, and neither becomes one at this triage.** Under this focus a finding
contradicts a criterion in the frozen rubric, and neither observation does — `R3` asks that a
number be legible and labelled, and `Pot 0` is both; `R4` asks about spacing within player-facing
text, and a line break at a legal break point is not a missing space. The route from a proposal to
a ticket runs through a merged ADR and nowhere else.

## The fix set — three tickets, in the rubric's own order

`ADR-0096` §5 orders audit repair **top to bottom by the rubric**. That is the order below, and it
is used even though the eight-cap did not bind, so that the tiebreak is visible in the ledger the
first time it applies rather than the first time it is forced.

| # | ID | criterion | beats named |
| --- | --- | --- | --- |
| 1 | [TASK-121301](../tasks/TASK-121301-the-runout-arrives-street-by-street-on-the-screen-too.md) | `R1` | 5 |
| 2 | [TASK-121302](../tasks/TASK-121302-the-decision-fits-a-390-by-664-screen.md) | `R2` | 2/3, 4, 5, 6 |
| 3 | [TASK-121303](../tasks/TASK-121303-the-front-doors-three-doors-are-three-doors.md) | `R4` | 1 |

Filed alongside, and **not** a member of the set:

| | ID | what |
| --- | --- | --- |
| — | [TASK-121304](../tasks/TASK-121304-the-table-reads-this-duels-award-and-not-the-last-ones.md) | the functional defect — `high`, outside `A(1)`, `status: ready` |

### Three of the four `verify:` blocks are honest manual steps, and here is why

**`ADR-0089` §2b forbids the gate that would be natural here.** *"No dependency. **No gate.** No
coverage claim"* are the three conditions that license a browser-driving harness at all, and **b**
reads *"No pull request, `verify:` block or ticket waits on a QA case"*. `R2`'s defect is a measured
geometry and `R4`'s is a rendered adjacency; both are browser facts, and a browser fact may not be a
`verify:` line in this repository. `R1`'s is worse than ungateable — half of what it must build is
undecided (§*What `R1` needs decided first*).

So `TASK-121301`, `TASK-121302` and `TASK-121303` carry `labels: [… manual-verify]`, state the
manual reproduction as the acceptance criterion, and say inside the ticket why no command can
express the failure. `TASK-121302` and `TASK-121303` additionally run `npm run check`, and each
says in as many words that **`check` gates the diff and cannot fail on the defect** — it is there so
a repair cannot merge a broken client, not to look like a gate.

**A gate that cannot fail is worse than an honest manual step**, and this repository has been bitten
by exactly that. `TASK-121102` is the merged precedent for the shape: `verify:` carrying the linter
alone, `manual-verify` in the labels, and the reason written out.

**`TASK-121304` is the exception and it carries a real gate**, because its defect is a pure
data-selection bug reachable from a unit test. Its new case builds a narration holding two
`HandStarted` events with the **same** `handNumber` — the rematch shape — and asserts the banner
reads this duel's award. Today that case renders the observer's exact string,
`Split pot — you win 100`; after the repair it reads `You win 200`. The gate fails today, for the
reason the defect exists, and passes only when it is gone.

### What `R1` needs decided first

`ADR-0096` §2 anticipates this ticket by name: *"`R1` requires that a runout be perceivable; it
fixes no duration, no animation and no transition. How a beat is paced is settled by the ticket that
repairs it — with a card where a still can hold it (`ADR-0091` §3), with the architect where it
cannot."*

**A still cannot hold it.** The frames themselves need no drawing — a board with three cards and a
board with four are the card's existing anatomy at two fills. What is undecided is *temporal and
structural*: whether the server sends a snapshot per street during a runout, or the client queues
the snapshot it has and reveals the board in steps, and how long a step lasts. No card can carry
either answer.

So `TASK-121301`'s **first acceptance criterion is to register the `DEC` and route it to the
architect, before any diff exists** — `ADR-0096` §2's own routing, and the shape `TASK-120907` and
`TASK-121101` both merged with. **It is not registered from this triage**: `STORY-1211`'s reasoning
governs — *a `DEC` nobody is working is noise in the open table* — and `ADR-0096` §2 places the
registration in the repairing ticket rather than in the round record, in as many words.

**It does not make the verdict `STOP_BLOCKED`.** That fires only for an **unanswered human-only
decision** gating a member of the fix set. This one is the architect's, and it does not exist yet.

## Verdict: `PROCEED`

**Unqualified.** No criterion went unanswered at any beat, and no cell of the per-criterion table
reads `BLOCKED`.

The audit table (`ADR-0096` §5 / `EPIC-12` §Termination rules 4 and 5), walked in order:

1. **Not `PASS`.** `A(1) = 3`, not 0. Three criteria of the five are `not met`.
2. **`PROCEED`.** `A(1) > 0`, `N < 3`, and the `A(N) < A(N−1)` clause is inapplicable: **round 1 has
   no `A(0)`**, exactly as it has no `B(0)`. That is stated here so nobody reads the missing
   comparison as an exemption somebody granted — it is an absence, not a waiver, and round 2 gets
   no such absence.
3. **Not `STOP_DIVERGING`.** Rule 4 needs `N > 1`. It will apply in full at round 2, and the bar is
   explicit now so nobody has to reconstruct it: **round 2 must come in at `A(2) ≤ 2`**. At
   `A(2) ≥ 3` the invocation ends `STOP_DIVERGING`.
4. **Not `STOP_BUDGET`.** `N == 1`; rule 5 permits three.
5. **Not `STOP_BLOCKED`.** It fires only when an **unanswered human-only** decision gates a member
   of the current fix set. Checked one at a time against the open table: `DEC-002` (unowned,
   `STORY-0103`), `DEC-060`, `DEC-088`, `DEC-089`, `DEC-090`, `DEC-091`, `DEC-094`, `DEC-102` — all
   **the product owner's**; `DEC-087`, `DEC-093` — **the architect's**. **None is human-only**, so
   the condition cannot be met on the current table whatever any of them gated. `DEC-103` and
   `DEC-104`, registered by this triage, are the product owner's and gate nothing in the fix set —
   `ADR-0096` §3 puts their answers in a **later invocation** by construction. `notify.py blocked`
   carries them; the cycle continues.

**There is no baseline determination in this record and no `BASELINE:` line in the report.**
`ADR-0096` §5 lists this focus's termination rules and a baseline exemption is not among them; the
rule (`ADR-0092` §6, `EPIC-12` rule 4) is defined by a **screen** becoming conformance-judgeable on
a card merged in the previous round's repairs, and this focus judges **beats against criteria** and
has no per-screen conformance check to unlock. `DEC-093` — whether that exemption extends beyond a
card — is open, the architect's, and gates nothing here. Stated with a reason rather than omitted,
because a missing line in a report is indistinguishable from a forgotten one.

## What I would look at first

**`TASK-121304`.** It is the only ticket here whose defect makes the product state something false
to a player, it is the only one with a gate that fails today, and it is the smallest diff of the
four. Everything else on this list is a player unable to see something; this one is a player told
the wrong thing.

Then `TASK-121302`, because it is the criterion the human named twice — *"scrolling is required to
see the whole picture"* and *"we have to support phone size"* — and because it is the one defect
here that touches every betting decision in every hand at the shape the product was just told to
support.

## Owed to a later round, and not smuggled into this one

`EPIC-12` §Termination rule 1 freezes the round's set at triage, so something this triage noticed
and the report did not name is **not** this round's:

- **`DuelState.narration` grows without bound for the life of a room**, and its own type comment —
  *"every event of the whole duel"* — has been false since the first rematch. `TASK-121304` repairs
  the **banner** without touching it, deliberately: clearing the log at `DuelFinished` would
  contradict the merged `duel-state.test.ts > leaves the view and narration untouched`, which makes
  it a decision about what `narration` means rather than a one-line repair. Named in
  `TASK-121304`'s *Out of scope*; not ticketed.
- **The front door is the only branch of `Lobby.tsx` whose `<section>` carries no class**, while its
  six siblings all carry the same recipe. `R4`'s ticket repairs the consequence the criterion
  names — the three doors abutting — and nothing else on that screen. Whether the rest of the front
  door wants the sibling recipe is not a criterion answer and is not filed as one.
