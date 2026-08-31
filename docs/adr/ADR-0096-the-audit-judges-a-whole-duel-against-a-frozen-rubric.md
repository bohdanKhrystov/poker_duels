# ADR-0096 — The audit judges a whole duel against a frozen rubric, and no round may grow it

- **Status:** Accepted
- **Date:** 2026-08-31
- **Resolves:** `DEC-096` — what process catches a product that is *raw*, and what authority does
  it need? Raised 2026-08-31 by the human, after playing the product in two browsers immediately
  following the `/qa-cycle uat regression` that ended `PASS` with `B(3) = 0` on 2026-08-30.
  Registered and answered in the same PR (the `DEC-039` path — it never appeared in an open
  table). Registers `DEC-097` open for the architect.
- **Where the answer came from.** Four things were **stated by the human and are recorded here
  verbatim, not chosen by this ADR**: *"the audit MAY file findings that contradict no merged
  source"*; *"the benchmark is category quality with the vision's aesthetics… Lichess and
  Chess.com, not PokerStars governs tone and features — it is not a licence to be less
  finished"*; *"audit findings are repaired regardless of severity… `EPIC-12` rule 2 stays as
  written for the functional QA cycle."*; and — answering the one question this ADR's first draft
  escalated — **"we have to support phone size."** That fourth call is a **supported surface**, the
  human's own column, and §4 is rewritten around it rather than reasoned toward it. Everything
  else is derived from `docs/vision.md`'s first
  success condition — **"Send a link. She opens it in a browser. We play a full heads-up match.
  Someone wins. We hit Rematch."**, with *"Everything else is downstream of that moment"* — which
  is a sentence about **one continuous act**, and from *Positioning*'s *"Dark, quiet, fast,
  minimal"* and *On variance*'s *"showing a player that they lost the match but made the better
  decisions is more interesting than hiding the maths."*
- **Amends:** [`ADR-0092`](ADR-0092-a-uat-round-files-what-a-source-settles-and-asks-the-rest.md)
  §3's classifier — **for the audit focus only**, and by relocating the merged source rather than
  removing it (§2 below). §3 stands byte-unchanged for the `qa` and `uat` focuses. Also scopes
  `EPIC-12` §Termination rule 2 to those two focuses (§5), on the human's call.
- **Applies:** `ADR-0092` §6 (one ledger, one manager) and §8 (no second skill, no second
  manager) — both merged, both reached unchanged; [`ADR-0093`](ADR-0093-ready-for-real-users-is-said-of-the-shipped-artifact.md)
  §2 (a bar is a precondition on a phrase, never a certificate); `ADR-0089` §§2c, 3, 4 and §2b as
  amended by `ADR-0090` §1 (all byte-unchanged); `EPIC-12` §Termination rules 1, 3 and 5

## Context

On 2026-08-30 `/qa-cycle uat regression` ended `PASS` with `B(3) = 0` and an empty fix set. On
2026-08-31 the human played the product in two browsers and reported: the round **ends immediately
after an all-in**, with no runout and no beat to see what happened; *"a lot of times it is hard to
understand what is going on"*; the pot and the bet amounts are not legible; **scrolling** is
required to see the whole picture, including the opponent's actions; weird copy, text without
spacing; *"and a lot of different issues… and more and more."*

Not one of those could have been reported by the cycle that had just passed. Three merged rules,
each correct on its own, compose into a process with no aperture for any of them.

**1. The classifier has no source of the right kind.** `ADR-0092` §3 permits a finding **only when
it contradicts something merged** — a card, a token, an owned literal, an ADR section, a
`duel-rules` heading, a vision sentence — and makes *"a judgment with no merged source"* a
question instead, capped at three per screen in the observer and three promoted per round by the
manager, one per screen. Round 3 promoted **zero**, and did so correctly: the manager answered
both candidates from existing ADRs. Nothing merged says a pot must be legible, or that an all-in
must show a runout, so no observation of either **could** be filed. Every merged source in that
list is a description of a *particular screen*. There is no merged source of the general kind — a
standard the product is held to — because none was ever written.

**2. The severity band that carries unfinishedness is the band the cycle refuses.** `EPIC-12`
§Termination rule 2: `blocker`/`high` means *the product cannot be used for its purpose* or *a
core vision promise is broken*; a real defect with a workaround is `medium`, cosmetic is `low`,
and both are *"filed to the backlog and never scheduled by this cycle."* Almost everything that
makes a product feel unfinished lands there by definition — the product **can** be used, it is
just bad to use. Counted today: **seventeen** `status: backlog` tickets, every one of them from an
`EPIC-12` round, several of them the human's own items (`TASK-120908` — the table's sizing
control; `TASK-121108` — two table cards name the street their pot strip prints; `TASK-120912` —
*Not now* is dressed like the control beside it).

**3. Coverage is per-screen, and a duel is not a screen.** `docs/test-plan.md`'s UAT inventory
walks eleven screen-states, each in isolation, each judged by three checks. Nothing plays a duel
from the link to the rematch and asks whether it was any good, so **pacing has nowhere to be
reported at all** — it is a property of a sequence, and the inventory has no row for a sequence.
The observer reads `#root.innerText` and computed styles and never checks a viewport, so *"you
have to scroll"* is invisible to it by construction.

**The all-in item is the sharpest evidence, because the beats already exist and only the process
cannot see them.** `poker-engine`'s `StreetProgression.runOutBoard` deals each remaining street as
its own `StreetDealt`, and its KDoc says why in as many words: *"carrying `GameState.deck` forward
by hand exactly as `closeRound`'s single-street path does, so the log reads like the deal it was
instead of one card dump."* The engine emits the flop, the turn and the river as three separate
events **for the reader**. Whether anything downstream shows them to a player is a question no
merged source asks and no round has ever walked: `docs/test-plan.md` contains **zero** cases
naming an all-in.

**The force pulling the other way, and it is the whole difficulty.** `EPIC-12` exists because of
one instruction: *"we do not want to run infinitely or get stuck (each time report more and more
bugs)."* Five rules answer it, and all five are built on `B(N)`, a count of severities. An audit
empowered to judge quality directly has **no natural fixed point** — everything can always be
better — so an unbounded version of the human's first half would put the loop exactly where
§Termination was written to keep it out of, and would do it through the door the human just
opened. Any answer that does not produce a fixed point is a worse outcome than the `PASS` that
prompted the question.

**The deadline is real but mild.** Nothing here gets more expensive with time except the backlog:
each round the process runs adds `medium`/`low` rows nobody schedules, and a queue that only grows
is one nobody reads. Seventeen is already past the number a person will re-read.

## Decision

### 1. There is a product audit, and its unit of observation is the **beat**, not the screen

`/qa-cycle audit <scope>` — a third focus of the one cycle, on the human's own message, the first
act of the turn it starts, exactly as `ADR-0089` §2b (amended by `ADR-0090` §1) requires of the
other two. **One manager, one ledger, one copy of the stopping rules**: `ADR-0092` §6 makes the
single ledger load-bearing for dedupe across focuses, and §8 already priced and refused a second
manager or a second skill — *"two copies of a rule drift"* — and this is the same shape arriving a
second time, so the holding is applied, not re-argued.

**What the mechanism costs in file terms is the architect's**, and this ADR does not spend it:
`ADR-0090` §2's declared-file set is four, an observer agent that says the cycle owns the stack
lifecycle would be a fifth, and whether that set is amended again (the `ADR-0092` §2 precedent) or
the focus runs without a new agent file is registered here as **`DEC-097`**, with the mechanism
that produces §4's **two** viewports inside one round. This ADR fixes *what kind of thing the audit is*; `DEC-097` fixes *what text
changes in which files to make it legal*.

A **beat** is a state the product passes through in the course of a duel. The walk is
`docs/vision.md`'s first success condition read one clause at a time, plus the hands a duel
actually contains:

| # | beat | from |
| --- | --- | --- |
| 1 | the link is opened and the seat is taken | *"Send a link. She opens it in a browser."* |
| 2 | the hand starts — blinds posted, hole cards dealt, the button known | `docs/duel-rules.md` |
| 3 | a betting decision on each street, with the flop, turn and river dealt between them | `docs/duel-rules.md` |
| 4 | a hand that ends in a fold — the pot awarded with no showdown | `ADR-0008` |
| 5 | **a hand that goes all-in and runs the board out to showdown** | `StreetProgression.runOutBoard`; reachable with a player's hands alone — *"A player may always go all-in for their remaining stack"* — so `ADR-0089` §3 is untouched and nothing is seeded |
| 6 | a hand that reaches showdown with both seats acting, and the loser mucks | `ADR-0008` |
| 7 | the duel ends and someone wins | *"We play a full heads-up match. Someone wins."* |
| 8 | rematch | *"We hit Rematch."* |

**Both browsers are observed at every beat.** Half of *"scrolling is required to see… the
opponent's actions"* exists only on the other screen, and an audit that watches one seat cannot
see what the other seat was shown.

### 2. A finding contradicts a **criterion**, and the criterion is a merged source

`ADR-0092` §3's rule is not removed. Its merged source is **relocated**: under the audit focus, an
observation is filed as a finding when it contradicts a criterion in the frozen rubric, and it
needs no other merged source. That is the human's *"MAY file findings that contradict no merged
source"* honoured exactly — no card, no token, no owned literal is required — while the principle
§3 was written to protect stands, because **the rubric is merged, closed and general**. What was
missing was never the merged-source rule; it was a merged source of the *general* kind.

**The founding rubric, in priority order.** It is small because five criteria is what the evidence
supports; it is ordered because §5 repairs top-down.

| id | criterion | licensed by |
| --- | --- | --- |
| `R1` | **Every event a player receives is perceivable.** For each `GameEvent` the projection delivers to a seat, that seat's player can tell it happened: the board coming out street by street, the opponent's action and its amount, the pot moving, the hand ending. An event that arrives and leaves no trace a player can perceive is `not met`. | *On variance* — *"showing a player… is more interesting than hiding the maths"*; `ADR-0008`; `StreetProgression.runOutBoard`, which deals each street as its own event *"so the log reads like the deal it was"* |
| `R2` | **The decision fits the screen.** At **every viewport the round walks**, the action the player is being asked to take and every number that decision depends on — their stack, the rival's stack, the pot, the amount to call — are visible at once, without scrolling. | *Positioning* — *"Dark, quiet, fast, minimal"*; `design/screens/duel-table.html`'s `max-width: 560px; min-height: 100dvh` single column |
| `R3` | **Every number is legible and labelled.** Every amount the product shows renders at or above the body type size, is not clipped or truncated, and says what it is. | *Positioning*, the Lichess/Chess.com reference, with the human's call that it *"is not a licence to be less finished"* |
| `R4` | **Every sentence renders as a sentence.** No missing or doubled space, no raw identifier, no unfilled placeholder, no untrimmed concatenation, in any player-facing text. | *Positioning*; `ADR-0089` §5's module-owns-the-literal |
| `R5` | **The screen says whose turn it is and what just happened**, at every beat, without the player consulting anything outside the screen. | `docs/test-plan.md` §*The standing questions*, `UAT-Q1` and `UAT-Q2` |

**One bar, checked more than once — never two bars.** `R2` and `R3` are the two criteria whose
answer can differ between a wide and a narrow screen, and the human's *"we have to support phone
size"* is exactly what makes that possible. Only their **quantifier** moves from *the* viewport to
*every* viewport the round walks: the enumerated list in `R2` is the same list at every shape, and
`R3`'s test — at or above the body size, not clipped, labelled — is the same test. **A criterion is
`met` only if it is met at every shape it was answered at.** Nothing here defines a relaxed phone
bar, and no round may invent one; a product that must scroll to show the amount to call is `R2`
`not met`, whether that happens at 390 px or at 720.

**That the same bar is meetable at both shapes is a merged fact, not an aspiration.**
`design/screens/duel-table.html` draws the table as one column at `max-width: 560px` with
`min-height: 100dvh` — a shape that is the whole width of a phone and a centred column with gutters
on a laptop. The product has **one layout**, and the two shapes are that layout at two widths. That
is also the reason §4 can walk two shapes without doubling the rubric.

**The acceptance test the human set, answered item by item.** *The round ends immediately after
an all-in* — `R1` at beat 5, the beat no case has ever walked. *The pot and the bet amounts are
not legible* — `R3`, and `R1` for the amount of a bet that is never shown at all. *Scrolling is
required to see the whole picture, including the opponent's actions* — `R2` at both shapes §4
walks, on both browsers, and it is the criterion the phone shape will fire hardest on. *Weird copy; text without spacing* — `R4`. *A lot of times it is hard
to understand what is going on* — `R5`, plus `R1`: this criterion **operationalises** the
complaint into a list of events and beats, and it will not catch a version of *confusing* that is
neither an unperceived event nor an unanswered *whose turn is it*. *"And a lot of different
issues… and more and more"* — **not caught, and no closed rubric can catch it.** That clause is
the human's eye finding a seventh thing, and §§3 and 6 are the whole of this ADR's answer to it:
the seventh thing becomes a criterion, and the process's job is to never lose it again.

**A criterion is `met` or `not met`, and `not met` carries a quoted observation** — a rendered
string, a measured geometry, a recorded frame list — never *"this feels wrong"*. `ADR-0089` §4
carries over with nothing subtracted: an unmet criterion a looking human cannot reproduce is a
**harness defect**, filed against `EPIC-12`, repaired in `scripts/qa/`, and it never enters the
count.

**What the rubric does not say is deliberate.** `R1` requires that a runout be perceivable; it
fixes no duration, no animation and no transition. How a beat is paced is settled by the ticket
that repairs it — with a card where a still can hold it (`ADR-0091` §3), with the architect where
it cannot. Settling the shape and leaving the rest open is the point.

### 3. Three things stop this becoming unbounded taste

- **The auditor answers a closed list and may file nothing else.** An observation that is not an
  answer to a criterion is not a finding. It is a **proposed criterion**, at most three per round,
  recorded in the round story and routed exactly as `ADR-0092` §5 routes a question: a `DEC` for
  the product owner where `docs/vision.md` settles it, the human where it does not, and a merged
  PR either way.
- **The rubric is frozen for the invocation.** No round may add a criterion to itself, or to a
  later round of the same invocation. A criterion merged mid-invocation applies to the **next**
  invocation. This is `EPIC-12` §Termination rule 1's frozen fix set, one level up, and it is the
  rule that makes `A(N)` and `A(N−1)` comparable at all. `STORY-1211` is the precedent for the
  discipline: a round that refused to widen `ADR-0092` §6 at the triage the widening would have
  saved.
- **Each criterion is binary, and its failure names its observation.** There is no severity to
  argue about and no scale to slide down.

### 4. Two shapes — the whole walk on a phone, and `R2`/`R3` again on a laptop

The human's call is *"we have to support phone size"*, and it is taken as **in addition to** the
laptop, not instead of it: they were playing in two browsers on a laptop when they raised every
item in §Context, so both surfaces are live and a walk that dropped the laptop would stop seeing
the screen the complaints came from.

An audit round walks **exactly two shapes**, both stated in the round record:

| shape | viewport | why this number |
| --- | --- | --- |
| `phone` | **390 × 664** | the iPhone 14/15 portrait width with the browser's chrome expanded — the **smallest `100dvh`** the card's own column is ever asked to fill. A taller value would let `R2` pass for a player who does not have it, which is the one way this number can lie |
| `laptop` | **720 × 900** | half of a 1440 × 900 laptop screen, which is what *"two browsers"* means on the machine this product is played on, and comfortably above the ~500 px width at which headless captures clip rather than overflow |

**The whole walk runs at `phone`; `laptop` re-answers `R2` and `R3` only, at the beats where a
player is asked to act.** The narrow column is where fitting and truncation fail, so it gets the
full eight beats and all five criteria. The other three criteria are not re-asked at `laptop`
because `R1`, `R4` and `R5` ask whether a thing is **shown at all** — an event with no trace, a
sentence with no space, a screen that never says whose turn it is — and that is a property of what
the client renders, not of how wide the window is. **The case this closure misses is named rather
than hidden**: a failure of `R1`, `R4` or `R5` that appears *only* when the window is wide will not
be seen by this walk. The cheapest reversal is one word in this paragraph.

**Adding the shape multiplies the labour and not the count.** `A(N)` still counts criteria, so it
is still bounded by the rubric's size — a criterion failing at both shapes and six beats is one
unmet criterion whose ticket names all of them (§5). This is what makes a second shape safe to
add: the walk got longer, the fixed point did not move.

**No tablet, and this is a reasoned refusal rather than an omission.** The card is one column
capped at 560 px, so a tablet renders exactly the `laptop` case — the column centred with wider
gutters — and reveals nothing that shape does not. A third row in the table above is the whole cost
of changing this.

**Landscape on a phone is not walked, and is not settled here.** Rotating is something a player's
hands do, and 664 × 390 is a shape in which `R2` may be unmeetable for a table with a board, two
seats and an action bar. Whether the product supports it, refuses it, or asks the player to rotate
back is a question this ADR leaves open and names in §Consequences; it is not silently answered by
walking portrait.

**This does not amend `docs/vision.md`, and the reason is the vision's own words.** *Why this
exists* says *"She opens it in a browser"*; the roadmap says *"Two browsers, one room link"*.
Both name **a browser** and **no device**. Supporting a phone therefore does not contradict a
sentence of the vision — it **resolves a silence in the direction the words already permit**, and
a link sent to a person is opened on a phone more often than not. Nothing in *What it is* or *What
it is not* becomes false, no milestone is added or reordered, and the commitment is carried here,
in a merged ADR that every later reader of the vision is already obliged to read.

**What would change that answer, stated so the next reader can check it rather than re-argue it:**
the day the product does something *different* on a phone — a second layout, a reduced feature set,
a separate application — it has acquired a second surface and that belongs in `docs/vision.md`, not
in an ADR. Today it does not: the card is one column, and §2's *one bar, checked more than once* is
only defensible because of that. If the human reads it the other way, the change is one clause in
the roadmap's v0.1 row and it is theirs to make, not this ADR's.

**How a round produces two viewports is the architect's**, and is the sharper half of `DEC-097`
now that there are two of them — §1's second paragraph, restated there.

### 5. Termination — a round ends because the list ends, and the loop ends on the count

- **`A(N)` is the number of criteria answered `not met` in round *N*.** Not observations: a
  criterion failing at six beats is **one** unmet criterion, and its ticket names all six. So
  `A(N) ≤ |rubric|` — **five today** — a ceiling known before the round starts. *"Each time report
  more and more bugs"* is not a shape this quantity can take.
- **A round ends when every criterion has been answered at every beat.** The auditor has no
  discretion to keep looking, because there is nothing else on the list to look at.
- **The invocation ends `PASS` when `A(N) = 0`**, and `PASS` means one thing only: every criterion
  in the frozen rubric was met, at every beat, at one commit, on one machine, at the shapes §4
  names and no others. It
  does **not** mean the product is finished, and no register, report or Definition of done may
  cite it as meaning that — `ADR-0089` §2c and `ADR-0093` §2 stand untouched and are what forbid
  it.
- **`A(N) ≥ A(N−1)` ends it `STOP_DIVERGING`; a third round ends it `STOP_BUDGET`.** Both are
  successful runs, and both say *the product is still raw and here is the list of how* — the
  sentence the current process cannot produce at any severity.
- **There is no severity under this focus, and there is no audit backlog.** `EPIC-12` §Termination
  rule 2 is scoped to the `qa` and `uat` focuses and stays byte-unchanged there, on the human's
  call. Under the audit focus a finding deferred by rule 3's eight-ticket cap **stays an unmet
  criterion and is counted again in the next round**: filing does not reduce `A(N)`, only repair
  does. That is the direct repair of the seventeen tickets, and it is why the cap orders
  repair **by the rubric's own order**, top to bottom — a deterministic tiebreak with no judgment
  in it. (This is the audit's *repair* ordering. `DEC-088`'s question about which screens take the
  UAT *promotion* gate's three slots is a different gate and is untouched.)
- **An audit round reports `A(N)` and no `B(N)`.** A functional defect an audit round stumbles on
  is filed to the one ledger and enters the next `qa` round's `B(N)`, never the audit's count —
  the same reason `ADR-0089` §4 and `ADR-0092` §5 keep three other classes out of `B(N)`: each
  count must measure one thing.
- **The audit promotes, it does not duplicate.** Where an unmet criterion's repair is already a
  `status: backlog` ticket from a `qa` or `uat` round, `qa-manager` moves that ticket into the
  audit round's fix set rather than filing a second — one ledger, `ADR-0092` §6.

### 6. How the process knows the product is finished rather than merely out of budget

**It does not, and the honest proxy is written down rather than implied.** `PASS` is a statement
about a list; the list is the accumulated memory of what a human noticed while playing. So the
number that answers the human's question is **criteria added per invocation**, recorded as a row
in `EPIC-12` §Metrics. Every time the human plays and finds something the rubric did not have, the
rubric gains a criterion and the number is positive. **The day the human plays and adds nothing,
the list has caught up with their eye** — and that convergence, not any round's verdict, is the
only evidence this process can produce that the product is no longer raw.

This is `ADR-0093` §2 applied one instrument over: the bar is a precondition on a phrase, never a
certificate of it, and the judgment stays with the human reading. What this decision adds is that
the human's reading now **accumulates** — into a file a round must answer, instead of into an
evening that is forgotten by the next `PASS`.

### 7. Reversing this is one superseding ADR and three deletions

Delete the rubric, the audit focus from `qa-cycle`'s `SKILL.md`, and whatever `DEC-097` decides
carries the observer, plus the ADR that says why — and `ADR-0092` §3 is back to one reading, rule
2 is back to unscoped, and the repository is byte-identical to the world that produced the `PASS`
of 2026-08-30. Nothing imports it and nothing gates on it, which is `ADR-0089` §6's own grade of
reversibility and the same reason this direction is the one to try while the yield is unevidenced.

## Consequences

**The rubric only knows what a human has already noticed, and that is the price of bounding
taste.** All five founding criteria are back-derived from one evening's play. The seventh thing
the human sees next month is not on the list, and no round will find it — this process *retains*
attention, it does not *originate* it. §6 makes that visible rather than hiding it, but the cost
is paid on every invocation and there is no version of a closed rubric that does not pay it.

**A criterion that is one line to state can be very expensive to meet, and nothing here can tell
the difference in advance.** `R1` is a table row and may mean pacing every runout in the client,
work with no design card behind it. Rule 3's eight-ticket cap and rule 5's three-round budget will
both be reached, and **`STOP_BUDGET` will be the ordinary exit for a while** — an invocation that
spends three rounds and hands back a still-raw product. That is the honest report, and it is worse
reading than `PASS (conformance unjudged on 6 of 7 screens)` was.

**The second shape is paid in round length, and `STOP_BUDGET` gets likelier again.** The walk was
eight beats on two browsers; it is now that at `phone` plus a `R2`/`R3` pass at `laptop`, and the
criterion most likely to be `not met` — fitting a decision on a 390 px column — is also the one
whose repair is the least bounded, because it can mean re-laying-out the table rather than adding a
line. An audit that spends three rounds on `R2` alone is a plausible first invocation, and the
report will say so honestly rather than passing.

**`ADR-0092`'s classifier now has two readings, one per focus, and one file holds both.** That is
precisely the drift `ADR-0092` §8 spent a section refusing for the stopping rules. It is accepted
here because the alternative — a second manager — breaks dedupe across three focuses, and the
seventeen-ticket queue is what a second register looks like after three rounds. The mitigation is
that the audit's half of the classifier is a **file** rather than a judgment; the mitigation is
not that the risk is small.

**A closed rubric is gamed by construction, not by malice.** A ticket repairing `R2` at 390 × 664
and 720 × 900 satisfies `R2` at 390 × 664 and 720 × 900; the next round reports `met` for a product
that still breaks at 1200 wide, at 320, or on a phone held sideways, and the report will be true.
**Two shapes is better than one and is still not a range** — declared viewports are what make the
criterion checkable and they are also what make the check narrow. Stated rather than solved.

**Landscape on a phone is genuinely unresolved, and this ADR does not close it.** §4 walks portrait;
a player who rotates gets a 664 × 390 window in which `R2`'s enumerated list may not fit at all, and
nothing here says whether the product then reflows, refuses, or asks them to rotate back. It is a
one-sentence question and it is named in the report rather than answered by the walk's silence.

**This forecloses a per-screen quality bar, permanently.** Once quality is judged per-beat against
a general rubric, nothing in this process ever again asks *"is this screen good?"* — that stays
where `ADR-0091` and `ADR-0024` §3 put it, with the card and the human at the pane, and the audit
deliberately declines to take it.

**The seventeen backlog tickets are not drained by this.** §5 promotes the ones a criterion
reaches; what no criterion reaches stays `status: backlog` for the roadmap to schedule, and this
ADR does not pretend otherwise. If that queue is the thing that most needs emptying, this is the
wrong instrument and rule 2 is the right one — see §Alternatives 1, which is rejected as a
*complete* answer, not as a useless one.

**Nothing here says anything about sound**, about latency on a real network, or about the built
bundle. The audit walks `npm run dev` like everything else until `DEC-087` is answered, so every
criterion is met of an artifact no user receives.

## Alternatives considered

**1. Raise `EPIC-12` rule 2: repair `medium` and `low` in the functional cycle.** Its case is
strong and mostly practical — it is one sentence in one file, it needs no new agent, no rubric and
no new arithmetic, it keeps a single classifier and a single count, and it would immediately
schedule seventeen tickets of which several are on the human's own list. **It loses because it
repairs the second of three failures and neither of the others.** The classifier still forbids
filing anything with no per-screen merged source, so the pot's legibility and the all-in runout
remain unfilable *at any severity*; coverage is still per-screen, so nothing ever plays a duel. It
would drain a queue and leave the product raw. And with the classifier unchanged, `medium`/`low`
has no fixed point of its own, which puts the loop back where §Termination was written against.

**2. Let the `uat` agent file taste findings, bounded only by the eight-ticket cap and the
three-round budget.** This is the smallest possible change and the most literal reading of the
human's first half; the budgets do genuinely bound the *work* per round. **It loses on the fixed
point.** `A(N)` would be whatever the agent happened to notice that day, so two rounds are not
comparable, rule 4's convergence check means nothing, and `PASS` would mean *"the agent ran out of
things to say"* — a property of the agent, not of the product. It recreates the exact loop
`EPIC-12` exists to prevent, arriving through the door the human just opened.

**3. A separate `/product-audit` skill with its own manager and its own stopping rules.** Its case
is real separation: the audit's arithmetic never has to coexist with `B(N)` in one file,
`qa-manager.md` keeps one classifier, and `ADR-0090` §2's declared-file grep is untouched because
a new skill need never name `qa-cycle`. **It loses to a merged holding.** `ADR-0092` §8 priced
this shape and refused it — a second manager is a second copy of the stopping rules and two copies
drift — and two filers break dedupe, so a defect both focuses see becomes two tickets against one
product.

**4. Write the missing per-screen sources instead: draw a card for every beat and leave
`ADR-0092` §3 exactly as it is.** This needs no new authority at all. It is the process working as
designed, `ADR-0091` already makes the card the carrier of taste with the human accepting it at
the pane, and every one of the human's six items would become an ordinary conformance finding.
**It loses because the beats are not screens.** A runout is a *sequence*; a card is a still, and
`ADR-0024` §2 with `ADR-0033` make it a fixed-width preview artefact. *The board coming out over
two seconds* is not a thing the card format holds. It also front-loads unbounded design authoring
before any audit can run, which is the branch the human declined once already (`ADR-0092`
§Context, the run-now call). It survives as the **repair** for whatever a criterion reaches that a
still can hold.

**5. Make the human the audit: schedule an `ADR-0088` §2 hand-check per release and file what it
finds.** It is the only instrument that has caught any of this — it caught all six items in one
evening, against a cycle that had just passed — and it costs no agent, no rubric and no
arithmetic. **It loses because it is a person, not a process**: it does not scale and, more
importantly, it does not *remember*, so the same six items are available to be re-discovered after
every `PASS`. This ADR keeps the person exactly where they are decisive — §3 and §6 make the
rubric grow only from their play — and moves the remembering into a file.
