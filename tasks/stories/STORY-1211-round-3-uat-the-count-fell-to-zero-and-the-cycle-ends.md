---
id: STORY-1211
title: Round 3 (UAT) — the count fell to zero, and what is still wrong is written down
type: story
status: done
parent: EPIC-12
labels: [process, qa, uat]
depends_on: []
---

## The round

**Round 3** of the same `/qa-cycle uat regression` invocation `STORY-1209` opened and `STORY-1210`
continued. It is the **last round the budget permits** (`EPIC-12` §Termination rule 5), so this
story's verdict is the invocation's exit state.

| | |
| --- | --- |
| Round | **3** of at most 3 — the last (`EPIC-12` §Termination rule 5) |
| Focus | **`uat`** — conformance, reachability, copy against merged sources (`ADR-0092` §3) |
| Scope | `regression` — all 11 in-scope screen-states of `docs/test-plan.md` §UAT |
| Date | 2026-08-30 |
| Commit walked | `90c48862` — **not** the `06aca205` the observer reported; see §*The commit the observer read* |
| Stack | `up` — db, server, web; browser profiles **not fresh** (both had played duels, both named, both bound to accounts) |
| Screens | 11 in scope, 11 walked, **11 judged on all three checks** — no `BLOCKED` cell |
| Findings | 13 reported, 2 questions, 1 `BLOCKED` **state** |
| `B(3)` | **0** — `blocker` 0 + `high` 0, after dedupe and after the three exclusions |
| `B(2)` | **3** |
| Baseline round | **no** — and see §*Baseline*, which rules on a second candidate the driver raised |
| Verdict | **`PASS`** — unqualified; every check on every in-scope screen was judged |
| Fix set | **empty** — nothing reached `blocker` or `high` |
| Filed | 8 tickets, all `backlog`; 1 `DEC` promoted |

## What this record is not

`ADR-0089` §2c and `ADR-0092` §2c, restated for the fourth round running, and restated hardest here
because **this is the round that ends `PASS`** and a `PASS` is the record most likely to be misread:

> No coverage claim. The product of a run is a **dated round record**. Neither it nor
> `docs/test-plan.md` may be cited as coverage in an epic's `Metrics`, a Definition of done, or a
> ticket's `verify:`.

> **No round, and no `PASS`, may be cited as the thing that made the product ready.** Readiness is a
> judgment made while reading the record, and its written bar — `ADR-0093` — is two facts about the
> **shipped artifact**, neither of which any round of this cycle can establish. Every case here ran
> against `npm run dev`; `dist/` is loaded by nothing.

**`PASS` here means one thing and nothing more: no `blocker` and no `high` survived triage in round
3.** Eight tickets say what is still wrong, and eleven tickets from rounds 1 and 2 are still open in
the backlog. A reader who wants to know the product's state should read those nineteen rows, not
this word.

## The commit the observer read, and why the round record stands

The driver recorded a process fault rather than a clean suppression, and it is ruled on here because
`EPIC-12`'s second deliverable is the honest trail.

**What happened.** The main working tree was left checked out on a local branch,
`decision/DEC-092-join-path`, at `06aca205`. The observer read that tree, saw
`docs/adr/ADR-0094-…md` present, and treated `ADR-0094` as a merged source. At that moment it was an
**open pull request** — `CLAUDE.md` rule 5: *an ADR is an answer once it is merged*. On the strength
of it the observer **withheld two would-be `high` conformance findings**: the absent join
confirmation screen and the inline room-code field.

**The ruling: the round record stands, and the two withheld findings stay withheld — but by a
different authority than the one that withheld them.**

1. **The product walked is the product briefed.** `origin/develop` stood at `90c48862` for the whole
   walk. The branch touched only `docs/adr/`, `tasks/BOARD.md` and `tasks/epics/`;
   `git diff --name-only 90c48862..origin/develop` over `web-client/` was empty. The observer read
   the wrong *tree* for one classification decision; it drove the right *product* for all thirteen
   findings. Every finding about the running product is sound and is triaged here on its merits.
2. **The withholding was right by luck, not by rule.** Had `ADR-0094`'s PR been closed rather than
   merged, two `high`s would have been lost from this round with no trace — and this is the last
   round, so they would have been lost from the cycle. That is the failure mode `CLAUDE.md` rule 5's
   *merged* exists to prevent, and it very nearly bit.
3. **The authority is `cf3ccc71`, not the mid-round read.** `ADR-0094` merged with three green gates
   before this triage. So at the moment of triage the join path *is* settled by a merged source, and
   `ADR-0092` §5's *an answered question becomes a merged source* closes the pair mechanically — a
   fourth round re-raising it would itself contradict something merged. The two findings are
   recorded here as **settled by `cf3ccc71`**, and the per-screen table's two qualified cells are
   re-read the same way.
4. **What it cost, stated so it is not free.** Nothing, this round. What it would have cost is two
   `high`s — and at two `high`s `B(3)` would have been 2, still below `B(2) = 3`, so still not
   `STOP_DIVERGING`, but not `PASS` either. The verdict was one merge away from being decided by a
   stale working tree. A round that reads the repository should read `origin/develop`, and that is a
   harness observation owed to a later pass, recorded in §*Owed to a later round*.

## Per-screen — 11 walked, 11 judged, no `BLOCKED` cell

Checks are `ADR-0092` §3's: **a** conformance against the merged card, **b** reachability, **c** copy
against merged sources.

| screen | state | a | b | c |
| --- | --- | --- | --- | --- |
| `first` | hosting — the room code, the invite link, the way back | judged | judged | judged |
| `first` | joining by a shared invite link | judged — against `ADR-0094` (`cf3ccc71`), which supersedes `join-duel.html` | judged | judged |
| `first` | joining by typing a room code into the lobby's field | judged — against `ADR-0094` (`cf3ccc71`); the field is inline by decision | judged | judged |
| `first` | the table once a hand is under way | judged | judged | judged |
| `first` | the table across its turn, waiting, away and back states | judged — **first round in which the two banner frames were readable at all** | judged | judged |
| `first` | the result screen once a duel concludes | judged | judged | judged |
| `first` | the rematch offer, accepted by both and pending on one | judged — **first round in which the *it begins* frame was readable at all** | judged | judged |
| `duels` | the duel history list | judged | judged | judged |
| `leaderboard` | the season standings | judged — one **state** of one line unwalked; see §*The unwalked state* | judged | judged |
| `account` | claiming a profile, or that profile's own page | judged | judged | judged |
| `sign-in` | the sign-in form | judged | judged | judged |
| `verify` | confirming a mailed verification link | out of scope | out of scope | out of scope |
| `reset` | setting a new password from a mailed link | out of scope | out of scope | out of scope |

**No cell reads `BLOCKED`**, so the verdict line is not qualified. §*The unwalked state* rules on the
report's `BLOCKED:` entry and explains why it does not change that.

`verify` and `reset` read `out of scope`, not `BLOCKED — no card`: `ADR-0092` §4 files a missing-card
`high` for a screen **in scope**, and no route reaches these two (`ADR-0031` §7 — no mailed link ever
arrives). **No missing-card finding is filed this round, on any screen**: all eleven in-scope screens
have merged cards, four of them merged in round 1's repairs.

## Dedupe — one ledger, both focuses, three rounds

Searched: every story under `tasks/stories/` and every `TASK-12NNNN` under `tasks/tasks/` — seven
round-or-not stories under `EPIC-12` and their 44 tickets. A defect seen under both focuses is one
ticket or `B(N)` double-counts it, and a defect seen in three rounds is still one ticket.

- **Repeats: 6 findings**, none refiled.
- **Regressions: 0.** Nothing filed and marked `done` came back — see §*Not a regression*, which
  checks the two screens round 2 repaired rather than assuming.
- **Not findings: 2** — the lobby's forward navigation (§*The lobby's three doors*) and the pot
  strip's street segment (§*The card is the outlier, again*).
- **New: 7 items**, mapped to 8 tickets — one ticket more than items, because the `account` and
  `sign-in` findings each split into a repeat half and a new half, and the `BLOCKED` state yields a
  ticket of its own.

### The repeat table

Behaviour, not wording, and not which focus or which round saw it.

| round-3 finding | reported | the open ticket it repeats | severity that governs |
| --- | --- | --- | --- |
| hosting: the waiting frame renders no seat plates | `high` | `TASK-120907` *Out of scope*, verbatim — *"not a second ticket until the answer arrives"*; `TASK-120901` *Out of scope* names it too | **`medium`** |
| table states: the away countdown runs into the sentence | `low` | `TASK-120909` | **`medium`** |
| duel end: the meta line states stacks, not a duration; Defeat's coin line is numeric | `medium` | `TASK-120911` *Out of scope* (meta) and `TASK-121007` (the coin line) | **`medium`** |
| duel end: *Not now* is a bare, unclassed button | `high` | `TASK-120912` | **`low`** — round 1's grade, held again |
| duel end: *Back to the lobby* vs the card's *Back to lobby* | `low` | `TASK-120911` | **`low`** |
| account: the two form submits use the small bordered recipe | `high` (half) | `TASK-121006` | **`medium`** |
| sign-in: the submit's recipe, and *Forgot your password?* unclassed | `high` (half) | `TASK-121005` — since its 2026-08-31 split, the submit half; the route out is `TASK-121010` | **`medium`** |

Seven rows, six distinct open tickets, both counts as at this round. **None is refiled.** Four of the seven arrived at `high`;
refiling them would put `B(3)` at 4 and end the cycle `STOP_DIVERGING` on a backlog read aloud. That
is precisely the illusion `qa-manager` §Step 1 calls load-bearing.

**The seat plates are still owed to an unanswered decision, and `ADR-0094` did not answer it.** The
ADR's own front matter says it *"Leaves open: … `create-duel.html`'s front-door and waiting frames"*.
So `TASK-120907`'s *Out of scope* sentence still governs and the plates are still not a second
ticket. Round 3 is the third round to see them and the third to leave them where they are.

### *Not now*, held at `low` for the second time

It arrives at `high` for the second round running, and round 2's written reasoning governs again,
unchanged: nothing about the control changed — the same `className: ""` on the same button — and a
repeat is graded on the reasons written when it was first judged, or severity becomes a function of
how many agents looked. Its consequence is still the loss of a dismissal, not of a capability. The
incentive is genuinely absent: a repeat enters `B(3)` at no severity, so the number is 0 either way.

## Not a regression

Round 2 repaired three screens and round 3 reports two of them again, so the regression question is
answered rather than assumed — a regression is never below `high`, and grading one wrong is the
single fastest way to a wrong verdict.

**`duels` (`TASK-121001`) — not a regression.** Its `Scope` was *"dress the filter fieldset, its four
radio labels, the opponent-search field and Search"*, *"the opponent-search field must be visibly a
field"* and *"split the row into the card's parts … the outcome word carries the card's win/loss
colour"*. Read from the running client, all of that holds: the controls carry classes, the row is
split into separate spans, and the outcome word carries `text-loss`. What round 3 found is what the
ticket **never promised** — the checked radio's own distinction, the date's faint step, and the
outcome word's weight. New findings at their own severity, filed as `TASK-121103`.

**`leaderboard` (`TASK-121002`) — not a regression.** Its `Scope` was the row split, the self line's
accent box and *Show more*'s fill. The observer confirms the self line and *Show more* fixed by name.
The figures' mono/tabular treatment was never in scope. `TASK-121104`.

**`account` (`TASK-121003`) — not a regression, and confirmed fixed.** *Sign in* and *Sign out* are
dressed; the observer says so explicitly. Read live at triage:
`{"text":"Sign out","c":"rounded-medium border border-hairline px-5 py-4 leading-tight font-medium text-text"}`.

**This is the same shape round 2 recorded about `TASK-120901`**, and it is the third round it has
held: a narrow ticket lands, its gate goes green, and the next round finds what the ticket did not
promise. It is not decay and it is not a coder's failure — it is what happens when a repair is scoped
to what a gate can assert. Recorded once more so the pattern is visible across the whole invocation
rather than once per story.

## Severity — five changes from what `qa` reported, each with its reason

`qa` gives a first opinion and this triage is not bound by it. **Every change is downward and that is
uncomfortable in the round that decides the verdict**, so each one is argued from the table and from
a precedent already in this ledger, and each is stated with what it would have cost had it gone the
other way. `EPIC-12` §Termination forbids two specific cheats — downgrading to make the count fall,
and deferring a qualifying defect to shrink `B(N)` — and the second is not available here at all,
because a deferral counts in `B(N)` whether or not it is filed into the fix set.

### 1. The hand-completion banner: `high` → `medium`

**This is the closest call in the round, and on its own it decides `PASS`.** Said first, plainly, so
nobody has to reconstruct it: at `high` this finding makes `B(3) = 1`, the verdict becomes
`STOP_BUDGET`, and the cycle ends anyway — one round later than nothing, with one ticket in a fix set
that could not be finished. The stakes of the call are smaller than they look, and it is still argued
on the merits.

**The finding is real and it is filed** (`TASK-121101`). `duel-table-states.html` draws the banner as
the state's own structure — `You win 4,850` over `Two pair, aces and sevens` — and the client renders
no such thing at any tick.

**Why `medium`.** `EPIC-12`'s `high` row is *a core vision promise is broken — hole cards leak, wrong
winner, coins wrong, rematch dead — or any regression*. All four exemplars are **correctness**
failures; this is a presentation gap. No card leaks, the winner is right, the coins are right, the
rematch works. `docs/vision.md`'s success sentence — *"We play a full heads-up match. Someone wins.
We hit Rematch"* — holds end to end, and `docs/duel-rules.md` §Showdown is engine rules and says
nothing about announcing a result on screen. The outcome reaches the player three ways the observer
itself names: the winner's hole cards appear, the folder's seat reads *Folded*, and both stacks move.
**That is the literal `medium` test — a real defect with a workaround.**

**And a second, independent reason it must not be `high`: half of it cannot be built.** `PotAwarded`
carries `seat` and `amount`, so *You win 4,850* is transcribable today — but **no wire field names a
made hand**, so *Two pair, aces and sevens* would be the client asserting a game fact, which
`CLAUDE.md`'s non-negotiables and `ADR-0002` forbid and which `no-derivation.test.tsx`'s `HAND_TALK`
regex actively gates. A `high` is a thing `build-epic` starts on immediately. Grading this `high`
would put a coder on a screen the protocol cannot feed — **the exact sentence `STORY-1209` wrote when
it held `TASK-120907` at `medium`**, and it is applied here rather than invented.

The undecided half is routed the way `TASK-120907` routed its own: the ticket's first acceptance
criterion is *register the `DEC` and route it, before any diff exists*, and its `verify:` carries only
the linter with the reason written out. It is **not** registered from this triage, because a `DEC`
nobody is working is noise in the open table, and because `TASK-120907` is the merged precedent for
exactly this shape.

### 2. The rematch *it begins* frame: `high` → `low`

`EPIC-12`'s `high` row names *rematch dead*. Rematch is not dead: the click is taken, hand 1 is
dealt, and the observer's own evidence shows the destination carrying `You | YOUR TURN | D | 9,950` —
the `D` being the button-changed-sides fact the missing frame would have announced. What is absent is
a momentary interstitial whose information the destination already carries. Nothing is lost but the
beat, and a defect that loses nothing is cosmetic. `TASK-121102`.

### 3 and 4. The `account` and `sign-in` field labels: `high` → `low`

Both findings bundle a **repeat half** (the submit buttons, and on `sign-in` the *Forgot your
password?* link) with a **new half** (the labels). The repeat half is `TASK-121006` and `TASK-121005`,
both open, both graded `medium` by this role a round ago; round 2's own rule — *a repeat is graded on
the reasons written when it was first judged* — means the bundle cannot be re-graded upward on its
repeat half. So only the new half takes a grade.

**The labels are `low`, and the anchor is the sibling ticket.** Round 2 graded a *primary submit
rendered as the wrong component* `medium`. A `<label>` that is centred instead of left-aligned and
full-bright instead of muted is strictly less consequential than that: it is still a legible label
naming a field the player can fill. Grading the label `high` while the button beside it is `medium`
would invert the ordering of this screen's own defects.

**They are their own tickets, not extensions.** The driver asked, and the answer is a rule rather than
a preference: `TASK-121005` and `TASK-121006` were filed and **frozen at round 2's triage**
(`EPIC-12` §Termination rule 1 — *the round's bug set is frozen at triage*). Editing a merged, open
ticket's scope a round later rewrites the trail instead of extending it, and it would make round 2's
own record wrong about what round 2 found. `TASK-121105` and `TASK-121106` carry `depends_on` edges
onto them instead — the round-2 convention that stops two coders holding one file.

### 5. The lobby's forward navigation: `high` → **not a finding**

Its own section, below, because the driver asked for the reasoning to be judged rather than accepted.

**Nothing was downgraded to reach a number, and the arithmetic shows it.** At `qa`'s grades
throughout — including the four `high` repeats — `B(3)` would be 9. At `qa`'s grades on new findings
only, `B(3)` would be 5. The dedupe alone (a rule with no severity judgment in it) takes it to 5, and
the five severity calls take it to 0. Each is argued above; the four repeats need no argument at all.

## The lobby's three doors — the reasoning, judged

The observer filed *Your duels*, *Leaderboard* and *Account* as a `high` against
`design/tokens/tokens.css`, and **explicitly declined** to extend round 2's `Back` exemption to them,
reasoning that they are *forward navigation into carded screens rather than a Back control*.

**The observation is exactly right and was re-confirmed at triage.** Read live from browser B:

```
{"t":"Your duels","c":""},{"t":"Leaderboard","c":""},{"t":"Account","c":""}
```

sitting immediately after `Create a duel room`, the room-code input, `Join the duel` and `Set my
name`, every one of which carries the full recipe.

**The reasoning does not hold, and the finding is not a finding.** Round 2's exemption was never
*"Back controls are exempt"*. It was *no merged source draws this control, so silence is not a
contradiction* — and the direction a control travels has no bearing on that test. Three checks, all
made here rather than inherited:

1. **`design/tokens/tokens.css` cannot be contradicted by an unclassed button.** It is a `:root`
   sheet of custom properties with **no selectors at all** — `grep` for any rule head returns
   nothing. It defines values; it does not require that any element consume them. The cited source
   says nothing about these buttons, or about any button.
2. **The one card that draws the front door refuses to draw them.** `create-duel.html`'s front-door
   frame draws *Create a duel* and *I have a code* and carries the note *"nothing else on the door —
   no lobby noise, no tables list"*. The card is not silent about these three controls; it is
   **against** them. A conformance finding that the card should have dressed something it argues
   should not exist is not available.
3. **The merged ADR that does settle them settles word and element, not treatment.** `ADR-0060` §2
   fixes the word (`HISTORY_HEADING`, so one literal renames the destination), fixes that it is a
   `<button>` and not an `<a>`, and fixes its placement beneath the strip and outside it. §3 fixes
   that it does not depend on the profile read. Nothing anywhere fixes how it looks. `Leaderboard`
   and `Account` inherit *the first screen is the door* by the same ADR's own note, with the same
   silence.

**So it is a question, and it is this round's one promotion** — see §*The promotion gate*. That is
also the honest way to make it stop recurring: the question has now cost three rounds a finding
between the four `Back`s and the three doors, and only a merged answer closes it mechanically, the
way `ADR-0094` closed the join path.

## The card is the outlier, again — the pot strip's street

The observer filed the shipped meta line's third segment — `Blinds 50/100 · Hand 1 · Flop` — as a
`low` divergence from the cards' two-segment `Blinds 75/150 · Hand 14`. **The client is right and the
cards are stale**, so it is **not a product finding** and enters no count at any severity.

`web-client/src/table/PotStrip.test.tsx > names the street the view names` is a merged test that
renders all six streets and asserts each word in turn, and `PotStrip.tsx`'s `STREET_NAMES` is a side
table so that `tsc` fails the day the wire grows a street with no word. Two merged artefacts
disagree; the one with a passing gate behind it is the product. This is the third time this
invocation has made that call — *Back to the lobby* twice, the street once — and it is filed as
`TASK-121108`, `module: design`, `low`, the same class as `TASK-120911` and `TASK-121007`.

## The unwalked state — the leaderboard's *no place this season* self line

The report's `BLOCKED:` entry is the observer's own sequencing mistake, honestly named: both devices
had to finish a duel to reach the table, duel-end and rematch states, so by the time the leaderboard
was reached both had a place. There is no third device, and `ADR-0089` §3 forbids writing state to
fabricate one. The divergence round 3 was asked to confirm is **unconfirmed by any walk**.

**Two things are true at once and both are honoured.**

- **A finding may not be filed on an unwalked state.** A UAT finding is an observed contradiction. If
  this triage could file defects on states nobody reached, the loop could file from argument alone,
  and there would be no fixed point at all.
- **A known divergence may not vanish because a walk was mis-sequenced.** `TASK-121002` shipped it
  knowingly.

**The resolution: it is filed, and it is filed on a static contradiction rather than on the walk.**
The two artefacts can be read side by side and neither reading needs a browser:

- `leaderboard.html` draws `.self.muted { background: none; border-color: var(--pd-hairline); color:
  var(--pd-text-muted) }` for the *Ready — you have no place yet* frame, with **no** `.coin` inside
  it.
- `LadderScreen.tsx:101-104` renders the slot with **no branch**: `border-accent bg-accent-subtle`
  and a `<CoinMark />`, around `selfLine(state.self)`, which returns `NO_PLACE_THIS_SEASON` when
  `rank` is null.

So the divergence holds by construction, exactly as the missing banner does. It is filed as
`TASK-121107` at **`low`** — the sentence, its place and `ADR-0065` §4's *the page is identical across
every self-line state* all hold; what diverges is a tint, a border colour and a coin mark on a line
saying the player has no coins. Its acceptance criteria carry **the walk round 3 could not make** as a
manual step, so the confirmation is owed by whoever repairs it rather than dropped.

**And this does not qualify the verdict line.** The qualification `ADR-0092` §6 defines is triggered
by a **cell** of the per-screen table reading `BLOCKED`, and its purpose is to stop a verdict being
read as coverage of a check that was never made. `leaderboard` check **a** *was* made: the rows, the
self line and *Show more* were all read and judged, and two of those three were confirmed fixed. One
**frame** of one card went unreached inside a judged check. Marking the verdict as if a check were
unjudged would make the marker mean two different things and devalue it on the rounds where a check
genuinely was not made — round 1's `(conformance unjudged on 4 of 11 screens)` meant four screens had
no card at all. Round 2 made the same call in the same direction and nothing is invented here to look
cautious.

## Baseline — round 3 is **not** a baseline round, and the second candidate is refused

**The straightforward half.** No screen became conformance-judgeable for the first time in round 3.
All eleven in-scope screens had merged cards before it started; round 2's repairs merged no card, and
`ADR-0092` §6's rule is written about a card merging in round *N-1*'s repairs. Round 2 said this
would be so in as many words: *"round 3 … gets no baseline exemption unless a further screen becomes
conformance-judgeable for the first time, and none is queued to."* So rule 4's comparison applies in
full against `B(2) = 3`.

**The second candidate, which the driver asked to be ruled on because the ruling will outlive the
round.** Two findings — the hand-completion banner and the rematch *it begins* frame — were
**unreadable in rounds 1 and 2 and readable in round 3**, because `TASK-121008` gave the driver
`record`/`frames` and it merged in round 2's repairs. Round 2 stated the gap precisely: *"check (a)
on two of `duel-table-states.html`'s three frames is unreachable by any round with the verbs
`drive.mjs` has."* Is a round in which a **new harness verb** first makes a frame judgeable a baseline
round?

**Ruling: no. It counts normally, and the gap is registered rather than improvised.**

1. **The rule as written does not reach it.** `ADR-0092` §6 and `EPIC-12` §Termination rule 4 both
   define the exemption by *a screen becoming conformance-judgeable for the first time, its card
   merged in the previous round's repairs*. A verb is not a card. Reading it in is an amendment to a
   merged decision, and `CLAUDE.md` rule 5 forbids guessing one.
2. **The purpose genuinely does reach it, and that is the argument for a merged answer, not for
   improvising.** The stated reason for the exemption is that *"the two rounds measured
   differently-sized judgeable sets"*, and that is exactly true here: rounds 1 and 2 could not judge
   two frames that round 3 could. A rule whose purpose applies and whose text does not is a rule with
   a gap, and the repair for a gap is a merged sentence — which is precisely what `STORY-1208` did
   when this same machinery was found to fire `STOP_DIVERGING` wrongly. `STORY-1208` fixed it by
   writing the rule down through a reviewed PR, **not** by a manager reading it generously at triage.
3. **And the manager who widens an exemption in the round it would save is indistinguishable from
   one who is cheating.** This is the decisive reason. Round 3 is the round where this exemption
   could change a verdict; a triage that invents it here has no way to prove it would have invented
   it anywhere else. `EPIC-12` §Termination names two cheats explicitly and both share that shape —
   a judgment made in the direction the number needs. Refusing the extension costs this round
   nothing (`B(3) = 0`, so rule 4's comparison cannot fire at all — `0 >= 3` is false) and costs a
   future round only a `STOP`, which this epic calls a successful run.
4. **So it is registered**: `DEC-093`, the **architect's**, on whether `ADR-0092` §6's baseline rule
   extends to a judgeable-set unlock by any merged instrument or only by a card. It gates nothing
   here — there is no fix set — so it is not `STOP_BLOCKED`, and it is not one of `ADR-0092` §5's
   three promotion slots, which are the product owner's. It is `CLAUDE.md` rule 5 routing, and the
   distinction is stated so nobody reads the cap as having been spent twice.

**Stated for the record, since a baseline determination must be:** round 3 is **not** a baseline
round; **no** screens made it one; round 2's repairs merged **no** cards. Rule 5's three-round budget
binds regardless, and round 3 is the third.

## `B(3)` = 0

`blocker` 0 + `high` 0, after dedupe and after all three exclusions. **Every exclusion is stated with
its reason, including the ones that are zero, because a manager that forgets one flips a verdict and
a rule that is only written down when it bites is a rule that dies.**

| class | count | in `B(3)`? | why |
| --- | --- | --- | --- |
| product `blocker` | **0** | yes | none found |
| product `high` | **0** | yes | none survived triage — five severity calls, each argued in §*Severity*, none of them a deferral |
| **1. harness defects** | **0** | no — excluded | `ADR-0089` §4 / rule 6. **No case failed at all this round**, so there is nothing to exclude. Counting a rotted case here would read a stale catalogue as a product getting worse and end the run `STOP_DIVERGING` on a healthy product, or send step 5 to change production code to satisfy a string the client moved. The one process fault this round had was the driver's working tree (§*The commit the observer read*), which broke no case |
| **2. missing cards** | **0** | no — excluded | `ADR-0092` §4. All eleven in-scope screens have merged cards; `verify` and `reset` are not in scope. Stated with a zero because a rule recorded only when it bites is one the next round forgets |
| **3. decision-born tickets** | **0** | no — excluded | `ADR-0092` §5. No ticket here comes from a `product-owner` answer. `DEC-089`–`DEC-091` are round 1's and are answered on their own clock; `ADR-0094` merged mid-round and produced no ticket for this triage — `TASK-120907`'s rewrite enters the ordinary backlog now the cycle has ended, never the round that asked (rule 1) |
| repeats | 7 findings / 6 tickets | no | removed by dedupe before the count. Four arrived at `high` |
| not findings | 2 | no | no merged source contradicted — §*The lobby's three doors*, §*The card is the outlier, again* |
| `medium` | 3 new | no | rule 4 counts `blocker` and `high` only. `TASK-121101`, `TASK-121103`, `TASK-121104` |
| `low` | 4 new | no | as above. `TASK-121102`, `TASK-121105`, `TASK-121106`, `TASK-121107` |
| cards in arrears | 1 ticket | no | not a fourth exclusion — the product contradicts nothing, so there is no product defect to count. `TASK-121108` |

**Nothing was deferred to shrink the number, and nothing could have been.** The fix set is empty
because nothing qualified for it, not because anything qualifying was pushed out; rule 3's cap of
eight never came near binding. A deferral would have counted in `B(3)` anyway, filed or not.

**The comparison rule 4 makes**: `B(3) = 0`, `B(2) = 3`. `0 >= 3` is false, so rule 4 does not trip,
and it would not trip at 1 or 2 either. For the record and not for the rule: `B(1) = 1` over 7
judgeable screens, `B(2) = 3` over 11, `B(3) = 0` over 11 — the first two measured different sets and
`STORY-1210` already warned against reconstructing a per-screen rate as a metric.

## Verdict: `PASS`

**Unqualified.** No cell of the per-screen table reads `BLOCKED`; §*The unwalked state* rules on why
the report's `BLOCKED:` entry does not change that.

The table, walked in order:

1. **Baseline round first**, per `STORY-1208`'s repair to this cycle's own machinery. Round 3 is
   **not** one — §*Baseline*, which also refuses the harness-verb candidate and registers `DEC-093`
   for it.
2. **`PASS`.** `B(3) = 0`: zero `blocker`, zero `high`, after dedupe and after all three exclusions.
3. **Not `STOP_DIVERGING`.** Rule 4 applies in full this round and does not trip: `0 >= 3` is false.
4. **`STOP_BUDGET`'s condition also holds** — `N == 3`, and rule 5 binds regardless of everything
   above — **and it changes nothing.** Round 3 was the last round at any `B(3)`, so the budget stopped
   nothing that the count had not already stopped. `PASS` is the stronger true statement and it is
   the one emitted; the budget is named here so no reader thinks it was overlooked.
5. **Not `STOP_BLOCKED`.** It fires only when an unanswered human-only decision **gates a member of
   the current fix set**, and the fix set is empty. `DEC-093` is the architect's and `DEC-089`–`091`
   and this round's promotion are the product owner's — none is human-only, and none gates anything
   that is scheduled. `notify.py blocked` carries the promotion; the cycle does not stop for it.

**What `PASS` does not mean**, restated at the point of maximum temptation: not coverage
(`ADR-0089` §2c), not readiness (`ADR-0093` — two facts about the shipped artifact, and every case
here ran against `npm run dev`), and not an empty backlog. Nineteen open tickets across three rounds
say what is still wrong, and eight of them were written today.

## The promotion gate — one promoted of three, and nothing invented to fill the rest

`ADR-0092` §5 allows at most three `DEC`s per round, **at most one per screen**, each a concrete
choice answerable in one sentence **and** bearing on a player's ability to tell what is going on or
what they may do. The report asked two questions and this triage downgraded one finding into a third.
**One is promoted.**

**Promoted — the lobby's three doors** (screen: `first`, hosting; the doors sit on the front door).

> **`DEC-094`, the product owner's.** *Should a control that no card draws — the lobby's `Your duels`,
> `Leaderboard` and `Account` doors, and the `Back` on each secondary screen — wear the client's
> control vocabulary, or is a bare control the intended treatment for navigation the cards do not
> draw?*

One sentence, one choice, and it bears squarely on whether a player can tell that three words sitting
under a form are things they may activate. It is the only promotion because it is the only question
that clears both halves of the bar, and it is worth the slot for a second reason: **three rounds have
now spent a finding on it** — the four `Back`s in round 2, these three doors in round 3 — and only a
merged answer closes it mechanically. An answer either way becomes a merged source: a *yes* yields a
ticket at the ordinary backlog now the cycle has ended (`EPIC-12` §Termination rule 1 — never the
round that asked), and a *no* blesses what shipped and earns a row in `docs/test-plan.md`
§*Settled, and not a finding*, so a fourth round re-raising it would itself contradict a merged
source.

**Not promoted — the result screen's nudge weight.** The report asks whether the post-duel *Your duel
coins are only in this browser* nudge should be subordinate to the Victory/Defeat verdict, given it
reuses the verdict's own heading treatment. **This is `DEC-089`, promoted at round 1's triage and
still open**, on the same screen, in the same words. It is not re-promoted and no second slot is
spent on it. Recorded as a repeat, exactly as a finding would be.

**Not promoted — the `duels` heading's alignment.** *Should the "Your duels" heading align with the
left-aligned filter, search and rows beneath it, rather than appearing centred above them?* It clears
the first half of the bar — one concrete choice, one sentence — and **fails the second**: a centred
heading over left-aligned content does not change what a player can tell or what they may do. It is
recorded here unanswered, is not a ticket, and is not to be re-recorded while the screen is
unchanged.

**No slot was filled for the sake of filling it.** Round 1 promoted three because three cleared the
bar; round 2 promoted none; round 3 promotes one. A gate that always spends its budget is not a gate.

## The harness — zero defects, and one caveat retired by hand rather than by a verb

**Zero harness defects.** No case failed, so `ADR-0089` §4 has nothing to exclude and this round adds
nothing to `scripts/qa/`. Round 2's two capability tickets both merged and both paid off immediately:
`TASK-121008`'s `record`/`frames` produced the two readings two rounds could not get, and
`TASK-121009` put the locale ruling in `docs/test-plan.md` §*Settled, and not a finding*, where round
3 read it and spent no finding on dates — the first round of this invocation not to.

**The observer's own caveat, and how rule 6 was satisfied.** Both new findings were read through a
`MutationObserver`, and the observer named the residual risk itself: *"a same-DOM-tick collapse is the
only mechanism that could still hide a real intermediate frame from this method."* That caveat is
exactly the trap where a hand-check inherits the harness fault — reproducing through the same
mechanism proves nothing about the mechanism. So the reproduction **varied the mechanism** rather
than the operator, and did not use the browser at all:

- **The banner.** `DuelTable.tsx` renders `<PotStrip view={view} />` and no other pot-adjacent
  element; `PotStrip.tsx` has exactly one `return`, rendering `Pot {view.pot}` and `Blinds N/N ·
  Hand N · {street}` with **no branch on `COMPLETE`**. No banner can render at any tick, whatever a
  poller or an observer would have seen.
- **The rematch frame.** `grep -rn "it begins\|dealing hand\|changes sides" web-client/src` is empty.
  The strings do not exist in the client.

Both **reproduce**, so both are product defects and both would have counted in `B(3)` at
`blocker`/`high`. They are filed at `medium` and `low` for the reasons in §*Severity*, not for want of
a reproduction.

**`EPIC-12`'s Definition-of-done box for telling a harness defect from a product defect stays
unticked**, for the third round running, and this round does not tick it by relabelling anything. The
box wants a failing case that does **not** reproduce by hand; round 3 produced the opposite — two
cases that reproduced under a varied mechanism.

## Owed to a later round, and not smuggled into this one

`EPIC-12` §Termination rule 1 freezes a round's set at triage, so a defect this triage noticed and
the report did not name is **not** this round's, however tempting. Recorded so it is not lost:

- **The `duels` screen's own `Show more` is classless.** `HistoryScreen.tsx:236-238` renders
  `<button type="button" onClick={…}>{MORE}</button>` with no className, while the leaderboard's
  `Show more` was dressed by `TASK-121002`. Round 3's report did not name it; it is named in
  `TASK-121103`'s *Out of scope* and belongs to the next round or to the ordinary backlog.
- **A round should read `origin/develop`, not the working tree.** §*The commit the observer read*
  cost nothing this time and came within one merge of deciding the verdict. A harness observation,
  not a defect — no case failed — and cheap to fix in `docs/test-plan.md` or in the `uat` agent's own
  first step.
- **`ADR-0050` §4's overturning**, recorded for the fourth round running rather than done quietly: the
  client cannot tell *has a credential* from *holds a session*, which is `TASK-120601`'s root cause
  and the reason `account-offer.ts`'s KDoc and `offerAccount`'s implementation state different facts.
  It is the **architect's** and it is still unregistered. It gates nothing here.

## Tickets

**Eight, and the fix set is empty.** Rule 2 admits only `blocker` and `high` to a fix set and neither
occurred, so every ticket below is `backlog` and **none is scheduled by this cycle**. Rule 3's cap of
eight applies to a fix set, not to backlog filings, and it never bound.

| ID | Title | Status |
| --- | --- | --- |
| [TASK-121101](../tasks/TASK-121101-the-table-says-who-won-the-hand-it-just-finished.md) | The table says who won the hand it just finished — *product, `medium`, blocked on a `DEC` the ticket must register* | backlog |
| [TASK-121102](../tasks/TASK-121102-accepting-a-rematch-draws-the-cards-dealing-frame.md) | Accepting a rematch draws the card's dealing frame — *product, `low`, `manual-verify`* | backlog |
| [TASK-121103](../tasks/TASK-121103-the-duels-rows-and-filter-carry-the-cards-remaining-cues.md) | The `duels` screen's checked filter, faint date and outcome weight are the card's — *product, `medium`* | backlog |
| [TASK-121104](../tasks/TASK-121104-a-leaderboard-rank-and-coin-figure-is-a-mono-figure.md) | A leaderboard rank and coin figure is the card's mono figure — *product, `medium`* | backlog |
| [TASK-121105](../tasks/TASK-121105-the-account-forms-labels-are-the-cards-labels.md) | The `account` screen's field labels are the card's left-aligned muted labels — *product, `low`* | backlog |
| [TASK-121106](../tasks/TASK-121106-the-sign-in-forms-labels-are-the-cards-labels.md) | The `sign-in` screen's field labels are the card's left-aligned muted labels — *product, `low`* | backlog |
| [TASK-121107](../tasks/TASK-121107-a-player-with-no-place-reads-the-cards-muted-line.md) | A player with no place this season reads the card's muted line, not the accent box — *product, `low`, carried from the `BLOCKED` state* | backlog |
| [TASK-121108](../tasks/TASK-121108-two-table-cards-name-the-street-their-pot-strip-prints.md) | Two table cards name the street their pot strip prints — *design; card in arrears, `low`, **not a product defect*** | backlog |

**Two `verify:` blocks carry only the linter, and both say why in the ticket rather than faking a
gate.** `TASK-121101` cannot be gated because half of what it must build is undecided; `TASK-121102`
cannot be gated because the store state its frame renders in does not exist yet, so any grep written
today is satisfied by adding the literal and any test name pinned today is satisfied by writing the
test. Both are `manual-verify` with the manual reproduction as the acceptance criterion. **A gate that
cannot fail is worse than an honest manual step**, and this repository has been bitten by exactly
that.

**Four `depends_on` edges**, each onto the ticket that touches the same file first, so two coders can
never hold one file: `TASK-121105` → `TASK-121006` (`account`), `TASK-121106` → `TASK-121005`
(`sign-in`), `TASK-121107` → `TASK-121104` (`LadderScreen.tsx`).

## What the invocation looked like, end to end

| round | `B(N)` | baseline | verdict | fix set | filed |
| --- | --- | --- | --- | --- | --- |
| 1 | 1 | no | `PROCEED (conformance unjudged on 4 of 11 screens)` | 5 | 12 |
| 2 | 3 | **yes** — 4 screens, cards merged in round 1's repairs | `PROCEED` | 3 + 2 harness | 9 |
| 3 | **0** | no | **`PASS`** | **0** | 8 |

Three rounds, 29 tickets, 12 merged, 19 open. The count went 1 → 3 → 0, and the middle number is the
one the baseline rule exists to explain: round 2 measured four screens rounds 1 could not.

**What I would look at first**, since a `PASS` deserves a next step rather than a full stop:
`TASK-121101` — it is the only open ticket whose defect a player meets in every single hand, and it
is blocked on a decision nobody has taken. Register its `DEC` before anything else in this backlog is
scheduled.

## Definition of done

- [x] Every finding in round 3's report is deduped against rounds 1 and 2 and dispositioned
- [x] Every severity change from `qa`'s report is written down with its reason
- [x] All three `B(N)` exclusions are stated with their reasons, including the zeroes
- [x] The baseline determination is stated, and the harness-verb candidate is ruled on
- [x] `B(3)` is computed and one verdict is emitted
- [x] Every ticket has a board row and `python3 .github/scripts/lint_tickets.py` exits 0
