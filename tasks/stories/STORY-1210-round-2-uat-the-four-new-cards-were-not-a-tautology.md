---
id: STORY-1210
title: Round 2 (UAT) — the four new cards were not a tautology, and the screens behind them are undressed
type: story
status: done
parent: EPIC-12
labels: [process, qa, uat]
depends_on: []
---

## The round

**Round 2** of the same `/qa-cycle uat regression` invocation `STORY-1209` opened. The round number
lives here rather than in the id, per `EPIC-12`'s Stories table; the id continues the round sequence
after `STORY-1209`.

| | |
| --- | --- |
| Round | **2** of at most 3 (`EPIC-12` §Termination rule 5) |
| Focus | **`uat`** — conformance, reachability, copy against merged sources (`ADR-0092` §3) |
| Scope | `regression` — all 11 in-scope screen-states of `docs/test-plan.md` §UAT |
| Date | 2026-08-30 |
| Commit | `07df9e7f` |
| Stack | `up` — db, server, web; browser profiles **not fresh** (both had played duels this round, both named, both bound to accounts) |
| Screens | 11 in scope, 11 walked, **11 judged on all three checks** — no `BLOCKED` cell |
| Findings | 20 reported |
| `B(2)` | **3** — `blocker` 0 + `high` 3, after dedupe and after the three exclusions |
| `B(1)` | **1** — but see §*Baseline*: rule 4's comparison **does not apply this round** |
| Baseline round | **yes** — `duels`, `leaderboard`, `account`, `sign-in`, on cards merged in round 1's repairs |
| Verdict | **`PROCEED`** — unqualified; every check on every in-scope screen was judged |

## What this record is not

`ADR-0089` §2c and `ADR-0092` §2c, restated because a round that judges eleven of eleven screens is
the one most likely to be read as coverage:

> No coverage claim. The product of a run is a **dated round record**. Neither it nor
> `docs/test-plan.md` may be cited as coverage in an epic's `Metrics`, a Definition of done, or a
> ticket's `verify:`.

> **No round, and no `PASS`, may be cited as the thing that made the product ready.** Readiness is
> a judgment made while reading the record, and its written bar — if it ever has one — is
> `ADR-0093`'s two facts, neither of which any round supplies.

**This is a statement about one walk, on one machine, at commit `07df9e7f`, on 2026-08-30.**
`dist/` is still loaded by nothing — the walk ran against `npm run dev`, so `ADR-0088` gap 3
survives this round exactly as it survived `STORY-1202`, `STORY-1205`, `STORY-1206` and
`STORY-1209`. **A verdict with no `BLOCKED` cell is not a verdict that everything was checked**; it
means every check the catalogue defines had a source to check against.

## Per-screen table (`ADR-0092` §6)

**a** conformance · **b** reachability · **c** copy against merged sources.

| screen | state | a | b | c |
| --- | --- | --- | --- | --- |
| `first` | hosting | judged | judged | judged |
| `first` | joining by a shared invite link | judged | judged | judged |
| `first` | joining by typing a room code | judged | judged | judged |
| `first` | the table once a hand is under way | judged | judged | judged |
| `first` | the table across turn/waiting/away/back | judged | judged | judged |
| `first` | the result screen | judged | judged | judged |
| `first` | the rematch offer | judged | judged | judged |
| `duels` | the duel history list | judged | judged | judged |
| `leaderboard` | the season standings | judged | judged | judged |
| `account` | claiming, or the signed-in page | judged | judged | judged |
| `sign-in` | the sign-in form | judged | judged | judged |
| `verify` | confirming a mailed link | out of scope | out of scope | out of scope |
| `reset` | setting a new password from a mailed link | out of scope | out of scope | out of scope |

**No cell reads `BLOCKED — no card`**, against four last round, so this round's verdict carries **no
inline qualification** — the first UAT verdict in this invocation that does not. `verify` and
`reset` stay `out of scope`, not blocked: no route reaches them (`ADR-0031` §7), so `ADR-0092` §4
files no missing-card finding for either.

**One check was not completed**, and it is reported as incomplete rather than as a pass: the
intra-duel showdown/fold winner banner (`duel-table-states.html`'s second and third frames)
auto-advances faster than the driver can poll. That does not make the cell `BLOCKED` — the card
exists, and the rest of the screen-state was judged against it — but it is the second round running
that the same two frames could not be read, and §*The harness* files it.

## Baseline — and this is the round the exemption was written for

**Round 2 is a baseline round, and rule 4's comparison is skipped.** Stated here in full rather than
left to be inferred from a `B(2)` that would otherwise read as catastrophic divergence.

`EPIC-12` §Termination rule 4 and `ADR-0092` §6 define a baseline round as one in which a screen
becomes conformance-judgeable **for the first time**, its card merged in round *N−1*'s repairs. Four
screens did:

| screen | card | merged by | commit |
| --- | --- | --- | --- |
| `duels` | `design/screens/duels.html` | `TASK-120902` | `d500f56d` |
| `leaderboard` | `design/screens/leaderboard.html` | `TASK-120903` | `17d641e8` |
| `account` | `design/screens/account.html` | `TASK-120904` | `07df9e7f` |
| `sign-in` | `design/screens/sign-in.html` | `TASK-120905` | `ebb00c49` |

All four merged into `develop` as round 1's repairs — **merged**, not merely filed;
`STORY-1209` could only say they were filed, so the fact is established here from the log rather
than inherited. Round 1 judged conformance on **7** screens; round 2 judged it on **11**. The two
rounds measured differently-sized judgeable sets, and comparing `B(2) = 3` against `B(1) = 1` would
score the unlock as decay — `STOP_DIVERGING` fired on the exact round in which round 1's repairs
first became measurable, which is the outcome `STORY-1208` repaired this cycle's machinery to
prevent.

**Rule 5's three-round budget binds regardless.** This is round 2 of at most 3. Round 3 gets **no**
exemption unless a further screen becomes judgeable for the first time in it — none is queued to —
so `B(3)` will be compared against `B(2) = 3` and `STOP_DIVERGING` is live next round.

**The exemption is not a licence for a bigger number.** Three of the four new cards produced a
finding that counts; the fourth produced one that does not, and it was graded `medium` on written
reasons in §*Severity* rather than rounded up to make the baseline look busy.

## The near-tautology, answered — and it is the round's most useful result

`ADR-0092` §Consequences predicted that round 2's conformance would be close to a tautology, because
the four cards would be composed by looking at four shipped screens. `STORY-1209` §*The observer's
two notes* carried the prediction forward as a live risk. **It did not happen, and the reason is
structural rather than lucky.**

`TASK-120902`–`TASK-120905` each carried the same *Scope* rule — *"Compose, do not mint"*, every
value from `design/tokens/tokens.css`, conventions per `design/README.md` and `ADR-0033` — and the
same *Out of scope* sentence:

> **Changing the client.** This ticket creates a reference; nothing under `web-client/` is touched.
> Where the shipped screen and the finished card disagree, that is a **finding for the next round**,
> which is the whole point of composing it.

So the cards were composed from the **settled vocabulary**, and only their *copy* was transcribed
from the running screens. That is exactly the split the report found: copy matches verbatim on all
four, and computed styling diverges sharply on all four. A card traced from a screenshot would have
produced neither.

**And the four cards are now the newest merged sources for their screens** — first commit
`2026-08-30`, against components written `2026-08-15` to `2026-08-28`. Where round 1 had to
adjudicate *which artefact is the defect* seven times and found the card in arrears three times,
this round's four new screens admit no such argument: nothing merged after the card blesses the
client. The client is in arrears, and that asymmetry is the whole content of `B(2)`.

## Dedupe — and it spans both focuses

Searched: every story under `tasks/stories/` and every `TASK-12NNNN` under `tasks/tasks/` — six
round-or-not stories under `EPIC-12` (`STORY-1202`, `STORY-1205`–`STORY-1209`) and their 35
tickets. One ledger, both focuses: a UAT walk stumbles on functional defects it does not hunt, and
a defect seen under both focuses is one ticket or `B(N)` double-counts it.

- **Repeats: 10 items**, none refiled — eight whole findings and two halves of findings that split
  (the hosting waiting frame, and the duel-end verdict panel).
- **Regressions: 0.** Nothing filed and marked `done` came back — see §*Not a regression* for the
  one case that had to be checked rather than assumed.
- **Not findings: 2** — both Ukrainian-date reports, closed by merged sources for the second round
  running. See §*The dates, ruled on twice*.
- **New: 10 items**, mapped to 7 tickets.
- **Harness: 0 defects, 2 capability tickets** — see §*The harness*, which is careful about the
  difference.

**The arithmetic reconciles at 22, not 20**, and the two extra come from splitting: `10 + 10 + 2 = 22`
over 20 reported findings, because two findings each carry a repeat half and a new half and are
counted on both sides rather than rounded to whichever was larger.

### The repeat table

Behaviour, not wording, and not which focus saw it.

| round-2 finding | reported | the open ticket it repeats | severity that governs |
| --- | --- | --- | --- |
| hosting: no *I have a code*, no two-screen code flow | `high` | `TASK-120907` — names this front-door pair in as many words | **`medium`** |
| hosting: the waiting frame has no seat plates | `high` | `TASK-120901` *Out of scope*, deferred to `TASK-120907`'s decision | **`medium`** |
| join-by-link: the offered seat never renders | `high` | `TASK-120907` finding 1 | **`medium`** |
| join-by-code: no dedicated enter-a-code screen | `high` | `TASK-120907` finding 2 | **`medium`** |
| duel table: sizing chips and a fourth action button | `high` | `TASK-120908` | **`medium`** |
| table states: the card draws no away/back frame | `low` | `TASK-120911` *Out of scope*, owed to `ADR-0091` §5's retrofit story | **`low`** |
| duel end: the meta line states stacks, not a duration | `high` | `TASK-120911` *Out of scope* — a product question, not a card correction | **`medium`** |
| duel end: *Back to the lobby* vs the card's *Back to lobby* | `low` | `TASK-120911` | **`low`** |
| duel end: the Victory frame's extra offer panel | `medium` | `ADR-0091` §5, *"carded-screen accretions … the account offer first among them"* | **`medium`** |
| duel end: *Not now* is a bare, unclassed button | **`high`** | `TASK-120912` | **`low`** — see below |

Ten rows, eight distinct open tickets. **None is refiled.** This is the rule that stops the backlog
growing every round out of re-reports alone, and it is load-bearing here: seven of the ten arrived at
`high`, so refiling them would put `B(2)` at **10** instead of 3 — and adding the two date reports,
also `high`, at **12**. A round that re-reads the backlog aloud and calls it decay is the failure
this cycle's own rules exist to prevent.

### *Not now*, and which severity governs a repeat

The human asked for this one by name. It arrives at `high` where round 1 graded it `low`.

**Round 1's `low` governs.** The reason is not the arithmetic — a repeat does not enter `B(2)` at
any severity, so the number is identical either way and the incentive here is genuinely absent.

- **Nothing about the control changed between rounds.** Round 2's evidence is the same
  `className: ""` on the same button in the same `AccountOffer.tsx`. A repeat is graded on the
  reasons written when it was first judged; re-grading it because a second walk found the same
  string would make severity a function of how many agents looked. `STORY-1209` used exactly that
  reasoning to hold the `TASK-120601` repeat at `medium`, and it is not being inverted a round later.
- **Its consequence is still small.** *Not now* dismisses an optional nudge; the sibling control
  *Keep them with a password* is dressed, and *Back to the lobby* below it is dressed. A player who
  cannot see *Not now* loses a dismissal, not a capability.
- **What is new is the cause, not the consequence**, and severity measures consequence. *Not now* is
  the **fifth site** of this round's bare-control cluster (§*The bare-control cluster*), and that is
  a fact about scheduling: whoever runs `TASK-121001`–`TASK-121003` should close `TASK-120912` in
  the same pass. It is named here so that connection is not lost, and it stays `low` and stays in
  the backlog, because rule 2 does not bend for a tidy grouping.

### Not a regression

`TASK-120901` merged in round 1's repairs and is `done`, and round 2 reports the hosting screen
again — so the regression question had to be answered rather than assumed, since a regression is
never below `high`.

**It is not a regression.** `TASK-120901`'s *Scope* was *"dress the front door's three controls …
with the client's existing token classes"* and its gate asserted a non-empty class drawn from that
vocabulary. Read from the running client at `07df9e7f`, all three still carry it, and so does the
whole waiting frame:

```
{"tag":"P",     "txt":"ZEG5WXN1",     "cls":"rounded-medium border border-hairline bg-surface px-5 py-4 text-text"}
{"tag":"INPUT", "txt":"http://…?room=","cls":"rounded-medium border border-hairline bg-surface px-5 py-4 text-text"}
{"tag":"BUTTON","txt":"Copy the link", "cls":"rounded-medium border border-hairline px-5 py-4 leading-tight font-medium text-text"}
```

Nothing that was repaired came back. What round 2 found is what `TASK-120901` **never promised**:
the card's `.btn.fill` accent treatment, the `.code` well's mono/size/tracking, and the `.mark`
wordmark. Those are new findings at their own severity (§*Severity*), filed as `TASK-121004`.

**And one of the report's particulars is corrected**, in the same spirit `STORY-1209` corrected
`qa`'s. The report says the room code *"renders as plain body text"*. It does not: the code element
carries `rounded-medium border border-hairline bg-surface px-5 py-4 text-text` and computes
`background-color: rgb(28, 26, 24)`. The divergence is real but narrower than reported — the box is
there, the card's type treatment is not.

## The dates, ruled on twice

The human asked for this one to be ruled on once more, and it is ruled on out loud, because it is
the second round to spend a finding on it — filed twice this round, both at `high`, one against the
lobby's profile strip and one against the full `duels` screen.

**The ruling stands: it is not a finding, and it is not a live question either.** Both merged
sources were re-read at `07df9e7f` rather than taken from `STORY-1209`.

- The report names `CLAUDE.md`'s *"English everywhere"*. That rule names **code, tickets, docs,
  commits**. A reader's date format is none of the four.
- `web-client/src/profile/profile-text.ts` documents `finishedAtText` as *"When the duel finished,
  **in the reader's locale**"* and implements it as
  `new Intl.DateTimeFormat(options?.locales, { dateStyle: "medium", timeStyle: "short", … })`.
- `ADR-0061` §*What it costs* names that behaviour and **accepts** it verbatim: *"`finishedAtText`
  renders instants 'in the reader's locale', so a player far enough east or west can read a duel as
  finishing on 1 September and find it counted in August."*
- So the observation does not contradict a merged source; it agrees with two. Under `ADR-0092` §3 it
  is **not a finding**. And a merged source that blesses what shipped **closes the question
  permanently** — re-raising it would itself contradict a merged source — so it is not promoted and
  no promotion slot is spent on it.

**The report's own uncertainty was honest and correctly routed.** It filed under `FINDINGS` with
`OBSERVER UNCERTAIN whether a more specific owning module exists; did not go looking, per scope`.
That is `ADR-0092`'s classifier working as designed: a wrongly filed question is lost with the
round, a borderline finding reaches triage. The placement was **not inherited** — it was read
against its named source and downgraded, and as a downgraded item it joined the ordinary question
pool and was found closed there.

**The blessing is now recorded where a third round will read it**, which is the human's question and
the answer is the catalogue, not a card. A card is a drawing of one screen; this behaviour is one
function consumed by two screens, and `docs/test-plan.md` §UAT is where a round is told what a
finding is. `TASK-121009` adds a short *settled, and not a finding* list to that section, with this
entry and its two sources. It is a **harness** ticket against `EPIC-12`, excluded from `B(2)`, and
it is the cheapest thing in this ledger: it costs one paragraph and it stops the third round
spending a finding on designed behaviour.

**If English-only dates are wanted, that is a product request and it starts as a request.** Filing
it as a defect would have `build-epic` change production code to satisfy a rule that does not cover
it.

## The bare-control cluster — one cause, four screens, and not one defect

The human asked for a ruling on whether these are one defect or several, and whether the fix set
should carry one ticket per screen or one shared ticket.

**They are one cause and four defects, and the fix set carries one ticket per screen.**

### First, what was measured, by hand, at `07df9e7f`

Not taken from the report. Two independent mechanisms, because a hand-check that reuses the
harness's own step inherits its fault: the **running DOM** through `getComputedStyle`, and the
**component source**, which must agree.

```
leaderboard  Show more   getAttribute("class") → null   bg rgba(0,0,0,0)  pad 0px  radius 0px  border 0px
account      Sign in     getAttribute("class") → null   bg rgba(0,0,0,0)  pad 0px  radius 0px  border 0px
account      Sign out    getAttribute("class") → null   bg rgba(0,0,0,0)  pad 0px  radius 0px  border 0px
duels        Search      getAttribute("class") → null   bg rgba(0,0,0,0)  pad 0px  radius 0px  border 0px
duels        fieldset    getAttribute("class") → null   ·  4 radio labels, all null
sign-in      Forgot…     getAttribute("class") → null   bg rgba(0,0,0,0)  pad 0px
```

The source agrees: `LadderScreen.tsx:116`, `AccountScreen.tsx:126`, `SignOutControl.tsx:39,42,50`,
`HistoryScreen.tsx:153–201` and `Lobby.tsx:387` render those elements with **no `className` prop at
all**. This is an empty class list, not a class present and overridden, so the report's own reading
is confirmed: these are component-code gaps, not a CSS build failure. Classes that *are* present
compute correctly on the same page.

**The sharpest single measurement is a control a player cannot see**, and it is byte-for-byte the
signature round 1 graded `high`:

```
duels, the opponent-search field
{"cls":null,"rect":{"x":326,"y":149,"w":167,"h":23},"bg":"rgba(0, 0, 0, 0)",
 "border":"0px solid rgb(236, 233, 227)","outline":"rgb(236, 233, 227) none 3px",
 "placeholder":null,"bodyBg":"rgb(19, 18, 17)"}
```

Against `design/screens/duels.html:60`, which draws that field explicitly:
`.search input[type="text"] { background: var(--pd-surface); border: 1px solid var(--pd-hairline); … }`.
A 167 × 23 px region with a transparent background, a zero-width border, no outline and no
placeholder, on a `rgb(19,18,17)` body. There is nothing on the screen to see. Round 1 measured
`167 × 22.5` on the room-code field and graded it `high`; the same measurement on a different screen
is graded the same way, or the line means nothing.

**The second sharpest is that the account screen's controls are typographically identical to its
prose.** Measured, not eyeballed:

```
Sign out (button) : 15px / 400 / rgb(236,233,227) / center / -apple-system / cursor: default
Back     (button) : 15px / 400 / rgb(236,233,227) / center / -apple-system / cursor: default
a paragraph       : 13px / 400 / rgb(236,233,227) / center / -apple-system / cursor: auto
```

Not a border, not a background, not a weight, not a colour, not even a pointer cursor separates the
two. A capture at 756 × 469 — above the ~500 px width floor where headless shots clip, so
`ADR-0092` §2's harness-defect test is not in play — shows *Sign out* and *Back* reading as two more
centred sentences under *Your password signs in to this account.* `UAT-Q3`, *are all options
accessible?*, is the standing question this fails against, and it fails against a card that draws
both as `.btn.ghost` (`design/screens/account.html:94, 112`).

### Why not one shared ticket

The cause is shared: these components were written before the token vocabulary reached them.
The **defects are not**, and three things follow.

1. **Fixing one fixes none of the others.** `HistoryScreen.tsx`, `LadderScreen.tsx` and
   `AccountScreen.tsx`/`SignOutControl.tsx` are disjoint files with disjoint test files. Behaviour,
   not cause, is what dedupe matches on, and no repair of one screen changes what a walk sees on
   another.
2. **Each screen has exactly one merged card, and the card is the acceptance criterion.** A ticket
   per screen has one source of truth and one reviewer's question. A shared ticket would carry four
   cards and a `verify:` block spanning four test files.
3. **A shared gate hides a partial repair, and this cycle has already been bitten by one.**
   `TASK-120901` shipped a scope narrower than its screen needed, its gate went green, and round 2
   found the rest. One ticket per screen gives round 3 four independent verdicts instead of one
   ambiguous green.

**And one control is deliberately not split by screen.** *Back* appears on all four screens and is
rendered **once**, by `Lobby.tsx`'s swap — giving it to four tickets would put four coders in the
same six lines of one file. It is not in any of the three fix-set tickets for a different and
better reason: **no card draws it**, so it contradicts nothing (§*What was not filed*).

## Severity — the line, stated once and applied to all four new screens

Round 1 wrote a UAT severity line and this round applies the same one rather than inventing a
second:

- **`high`** — a card's vocabulary is absent wholesale, or a control the card draws cannot be seen
  by a player's eye.
- **`medium`** — the card is transcribed and a specific element diverges.
- **`low`** — a cosmetic detail inside a transcribed screen.

| screen | how much of the card's control vocabulary is transcribed | severity |
| --- | --- | --- |
| `duels` | 0 of 7 drawn controls; rows collapsed to one flat string; **the search field is invisible** | **`high`** |
| `leaderboard` | 0 of 1 drawn button; `.self`/`.coin` absent; rows collapsed to `1 x 1` | **`high`** |
| `account` | 0 of 4 drawn buttons — two unclassed, two a different, smaller component | **`high`** |
| `sign-in` | 0 of 2 drawn controls — but the card is otherwise transcribed | **`medium`** |

**`sign-in` is the one place the report's own grade was kept rather than raised, and the reason is
written down so nobody reads this round as uniform inflation.**

- The card's three frames are transcribed: heading, both field pairs, and the refusal frame —
  `role="status"`, positioned above the fields, fields retaining typed values — which the report
  itself confirms matched *exactly*.
- The screen's **primary** control, the *Sign in* submit, **is** a visible button. It is the wrong
  component (`rounded-small … px-4 py-2 text-small` where the card draws `.btn.fill`), which is a
  divergence, not an absence.
- What is bare is *Forgot your password?*, a secondary route out, behind which no round can go
  anyway — no mail arrives (`ADR-0031` §7).

That is two specific elements diverging inside a transcribed screen, which is the `medium` row. It
goes to the backlog as `TASK-121005` and is **not** scheduled by this cycle (rule 2). One finding,
and since 2026-08-31 two tickets — one per element, for the reason §Tasks records.

**`account` is `high` on the first clause, not on a stretched reading of the second.** Every one of
the four buttons the card draws — *Give this profile a password* and *Attach a recovery address* as
`.btn.fill`, *Sign in* and *Sign out* as `.btn.ghost` — is wrong: two carry no class at all and two
carry a smaller component the card does not define. The card's `.btn` block is three of its own CSS
rules and every control it draws uses one; **none of them reached the client**. That is a vocabulary
absent wholesale. The typographic identity with the prose above is corroboration, not the argument.

**Severity changes from the report, each with its reason:**

| finding | report | ruled | reason |
| --- | --- | --- | --- |
| the two Ukrainian-date findings | `high` | **not a finding** | two merged sources bless the behaviour; §*The dates* |
| five repeats re-raised at `high` | `high` | **`medium`** | round 1's written reasons, unchanged; §*The repeat table* |
| duel end: *Not now* | `high` | **`low`** | round 1's grade governs a repeat; §*Not now* |
| duel table: *committed* vs *bets* | `medium` | **card defect** | the client's own `BetLine` KDoc and `ADR-0002`; §*Which artefact* |
| duel end: the Defeat coin line | `high` | **card defect** | `outcome-text.ts` owns the literal and postdates the card; §*Which artefact* |
| `sign-in`: the unstyled route out | `medium` | **`medium`** — kept | above |
| `duels`, `leaderboard`, `account` | `high` | **`high`** — kept | above |

**Nothing was lowered to make a number.** Every lowering here is a repeat, and a repeat does not
enter `B(2)` at any grade. The one place the arithmetic could have been touched is `sign-in`:
grading it `high` would have made `B(2) = 4`, and a **larger** `B(2)` makes round 3's rule-4
comparison *easier* to pass. The incentive ran toward inflation and the grade is 3 anyway.

**One finding is repaired by two tickets, and that is a split of the work, not of the severity.**
The `account` finding is one `high` and counts once in `B(2)`. Its repair is `TASK-121003` (the two
unclassed buttons — a player cannot tell them from prose, which is what makes the finding `high`)
and `TASK-121006` (the two form submits, visibly buttons of the wrong component, which on their own
would be `medium`). They are separate because they touch different files and have different
consequences, and because a `high` ticket that drags a `medium` in has widened scope. **The `medium`
half is not a downgrade**: no part of this finding left `B(2)`, because the finding entered it once
and at `high`. Said out loud, because a severity split inside a finding is the shape a laundered
downgrade would take.

## Which artefact is the defect — two card corrections, and a note that has come true

Round 1 established the adjudication and this round reuses it: the client contradicting the newest
governing merged source is a **product** defect; a card contradicting a decision merged after it was
drawn, where the client implements that decision, is a **card** defect — filed, repaired in the
card, and contributing nothing to `B(N)`, not by a fourth exclusion but because `B(N)` counts
product defects and a card in arrears is not one.

Two findings resolved that way, and both were checked against the log rather than argued:

| finding | the card says | the client says | what governs |
| --- | --- | --- | --- |
| `duel-table` bet line | `bets 400` (`.bet-line`, drawn `2026-08-14`) | `committed 100` (`DuelTable.tsx`, `2026-08-15`) | **the client.** Its own KDoc states the rule: *"The word is the field's, not an action's: the view says how much is committed and never says whether it got there by a blind, a call, a bet or a raise."* The projection carries a committed total; printing *bets* would be the client inferring an action, which `ADR-0002` forbids |
| `duel-end` Defeat coin line | `The coin goes to ImKate` (drawn `2026-08-14`) | `−1 duel coin` (`outcome-text.ts:52`, `2026-08-16`) | **the client.** `ADR-0089` §5 makes the owning module the source for player-facing text, and `coinLine` postdates the card by two days. The card's phrasing also needs a fact the wire does not carry: `DuelOutcome` is `{winner, handsPlayed, finalStacks}` — no display name, no duration |

**And `TASK-120905`'s card predicted a finding and got one — but not the one it predicted.** The
human asked what that means for the margin note, and the answer is that the note is now false and
has to be corrected, while the drawing it justified is vindicated.

The note reads:

> Forgot your password? is FORGOT_PASSWORD_LABEL (recovery-text.ts) — **SignInForm.tsx does not
> render it yet**, so this card draws the gap the next UAT round should catch.

The control **does** render, and it always did — from `Lobby.tsx:387–389`'s `SignInScreenBody`, not
from `SignInForm.tsx`:

```
web-client/src/lobby/Lobby.tsx:387
      <button type="button" onClick={() => setAskingForALink(true)}>
        {FORGOT_PASSWORD_LABEL}
      </button>
```

`grep -rn 'FORGOT_PASSWORD_LABEL' web-client/src` returns `Lobby.tsx` and no other component. So the
card was **right to draw it** — the control belongs on that screen and ships — and the note was
wrong about **where** it lives and therefore about **whether** it exists. Left alone, the note tells
the next reader the screen is short a control it has.

Three consequences, all recorded rather than left to inference:

1. **The prediction succeeded on its own terms.** The next UAT round *did* catch a gap on that
   control. The gap is a treatment gap, not an absence: it renders with no class where the card
   draws `class="link"` — accent-coloured, `--pd-fs-small` — so it is indistinguishable from body
   text. That is `TASK-121010`, `medium` — the half of `TASK-121005` that the 2026-08-31 split gave
   its own id (§Tasks).
2. **The note is corrected**, in `TASK-121007` alongside the two card corrections above: it should
   say the control is rendered by `Lobby.tsx`'s `SignInScreenBody` and is unstyled, which is what
   the next round should check.
3. **The pattern is worth keeping.** A card that writes down what it believes the client does not do
   is a card that can be proved wrong by a walk, and this one was — in one round, on a claim nobody
   would otherwise have checked. `ADR-0091` §3 puts the human's visual verdict after the merge;
   this is the same idea aimed at facts instead of taste.

## What was not filed, and why — the classifier doing its refusing

`ADR-0092` §3: a finding must contradict a **merged source**. Three observations in this round's
neighbourhood do not, and none is filed.

**1. The bare *Back* on all four screens.** It is genuinely unclassed and computes identically to
body text, exactly like *Sign out*. **No card draws it.** `grep -i back` over `duels.html`,
`leaderboard.html`, `account.html` and `sign-in.html` returns only `background` in their CSS
blocks — the four cards draw the screens, not the swap that hosts them. `ADR-0060` §4 settles the
*word* (*Back*, one word) and its *ownership* (rendered by whatever renders the swap, not by the
screen) and says nothing about its treatment; no other ADR does. **Silence is not a
contradiction**, and this is the same move round 1 made when it refused to hold the away-banner
finding under check (a) because `duel-table-states.html` draws no away frame. It is recorded here
and not filed, and it is a good candidate for the `ADR-0091` §5 retrofit story — the cards owe a
frame for the way out of a secondary screen.

**2. The three lobby door buttons** — *Your duels*, *Leaderboard*, *Account* — are bare for the same
reason and filed for the same non-reason: `create-duel.html`'s front-door frame draws
*Create a duel* and *I have a code* and carries the note *"nothing else on the door — no lobby
noise"*. The card does not draw these doors at all.

**3. The report's own uncertainty flags** were adjudicated, never inherited: the Ukrainian dates,
downgraded to a question and found closed (§*The dates*); the *Back to the lobby* wording, which the
report itself flagged as the card being the outlier, which round 1 had already resolved that way.

## `B(2)` = 3

`blocker` 0 + `high` 3, after dedupe and after all three exclusions. **Every exclusion is stated
with its reason, because a manager that forgets one flips a verdict.**

| class | count | in `B(2)`? | why |
| --- | --- | --- | --- |
| product `high` | **3** | **yes** | `TASK-121001`, `TASK-121002`, `TASK-121003` — `duels`, `leaderboard` and `account` do not wear the cards merged for them last round |
| product `blocker` | 0 | yes | none found |
| **1. harness defects** | **0** | no — excluded | `ADR-0089` §4 / rule 6. No case failed without reproducing. The two harness tickets this round files are **capability** tickets, not defects — §*The harness*. A stale catalogue counted here would read as a product getting worse, and step 5 would merge a diff to satisfy a moved string |
| **2. missing cards** | **0** | no — excluded | `ADR-0092` §4. There are none: round 1's four merged, which is what makes this a baseline round. The exclusion is stated with a zero because forgetting it next round is how the rule dies |
| **3. decision-born tickets** | **0** | no — excluded | `ADR-0092` §5. No ticket this round comes from a `product-owner` answer, and none could — no `DEC` was promoted (§*The promotion gate*). `DEC-089`–`DEC-091` are answered on their own clock; anything they yield enters the earliest **subsequent** triage, never this one (rule 1) |
| cards in arrears | 1 ticket | no | not a fourth exclusion — the product contradicts nothing, so there is no product defect to count. `TASK-121007` |
| `medium` | 3 new | no | rule 4 counts `blocker` and `high` only. `TASK-121004`, `TASK-121005`, `TASK-121006` — and, since 2026-08-31, `TASK-121010`, `TASK-121011` and `TASK-121012`, which are the two halves of `TASK-121005` and the two halves of `TASK-121004`'s third scope item, and not further findings (§Tasks) |
| `low` | 0 new | no | as above |
| repeats | 8 | no | removed by dedupe before the count |
| not findings | 2 | no | closed by merged sources before severity was set |

**Nothing was deferred to shrink the number.** The fix set holds three tickets against a cap of
eight, so rule 3 never bound and nothing qualifying was pushed out. A deferral would have counted in
`B(2)` anyway, filed or not.

**The comparison against `B(1)` is not made**, and that is the point of §*Baseline*. For the record
and not for the rule: `B(1) = 1` was measured over 7 conformance-judgeable screens and `B(2) = 3`
over 11. Per judgeable screen the two rounds are `0.14` and `0.27`, which is not a metric this
cycle uses and is written down only so nobody reconstructs it as one.

## Verdict: `PROCEED`

**Unqualified**, and it is the first UAT verdict in this invocation that is. Round 1's line carried
*(conformance unjudged on 4 of 11 screens)* inline and verbatim because four screens had no card;
this round has no `BLOCKED` cell, so there is nothing to qualify and nothing is invented to look
cautious.

The table, walked in order:

1. **Not `PASS`.** `B(2) = 3 > 0`.
2. **Baseline round first**, per `STORY-1208`'s repair to this cycle's own machinery. Round 2 **is**
   one — four screens became conformance-judgeable for the first time on cards merged in round 1's
   repairs — so rule 4's comparison is skipped and **`STOP_DIVERGING` cannot fire**. Read without the
   exemption, `3 >= 1` would have ended the run on the round in which round 1's repairs first became
   measurable. That is the precise outcome `ADR-0092` §6 exists to prevent, and it is stated here
   rather than left for a reader to reconstruct.
3. **Not `STOP_BUDGET`.** `N = 2 < 3`.
4. **Not `STOP_BLOCKED`.** No unanswered human-only decision gates any member of the fix set. Zero
   `DEC`s were promoted this round; `DEC-089`–`DEC-091` are round 1's, are the **product owner's**
   rather than the human's, and gate nothing here. The two decisions this round records as owed —
   `ADR-0050` §4's overturning, and `TASK-120907`'s pair — are the `architect`'s and the product
   owner's, and every ticket they touch is in the backlog, not the fix set.
5. **Therefore `PROCEED`**: repair the three fix-set tickets and the two harness tickets, then
   retest.

**What round 3 inherits, said plainly because it is the last round.** Rule 5's budget means round 3
ends the invocation whatever it finds. It gets **no** baseline exemption unless a further screen
becomes conformance-judgeable for the first time, and none is queued to — every in-scope screen now
has a card. So `B(3)` is compared against `B(2) = 3`, and the run ends `PASS` at zero,
`STOP_DIVERGING` at three or more, and `STOP_BUDGET` otherwise. **A fourth round is not available at
any `B(3)`.**

**And the fix set is what round 3 will measure.** Three tickets against a cap of eight: rule 3 never
bound, nothing qualifying was deferred, and a deferral would have counted in `B(2)` anyway. If all
three land, the three screens they dress are the three that carried this round's whole count.

## The promotion gate — nothing promoted, and nothing invented to fill a slot

`ADR-0092` §5 allows at most three `DEC`s per round, at most one per screen, each a concrete choice
answerable in one sentence **and** bearing on a player's ability to tell what is going on or what
they may do. The report asked exactly two questions. **Both are answered by merged sources, so
neither is promoted and the round promotes zero.**

**1. `leaderboard` — should tied rows have a stable, deliberate secondary order?** Answered by
`ADR-0064`, whose title is *"Tied players share one rank number, and the order rows sit in is not a
ranking"*. §2: *"the position of a row inside a page is transport, is never rendered, and is never
used to derive a rank."* §3: *"There is no secondary column, no ordering the screen explains."* §4:
*"The order tied rows are emitted in is **arbitrary, invisible, and not a measure of play**"*, and
which deterministic key the query uses is **the architect's**, at `STORY-0502`'s split, already
registered as part of `DEC-061`. The report guessed this correctly and filed it as a question
anyway, which is the classifier working: the product half is settled and the residue is an
architect decision that already exists. Promoting it would register a second `DEC` for a question
two merged sections answer.

**2. `duel end` — should the account nudge stop appearing once a device already has a password?**
Answered by `ADR-0036` §Decision: the trigger is *"the **first duel won**, not the first duel
played"*, and *"It is **dismissible, and dismissal is permanent.** 'Not now' means not again. This
is the half of the decision most likely to erode under a growth argument later, so it is stated as a
rule rather than as a default."* The product answer exists and is emphatic.

**And the report did not observe the nudge misbehaving — it asked.** That distinction is kept: this
triage may not promote a question into a finding, and the only route from a question to a ticket
runs through a merged ADR. Recorded, not filed: `web-client/src/result/account-offer.ts`'s KDoc
states the condition as *"they do not already hold a credential"* while
`offerAccount` implements `!input.signedIn`, whose own doc-comment says it is *"whether this browser
holds a session token"*. Those are different facts, and it is the **same root cause** as
`TASK-120601` — the client cannot tell *has a credential* from *holds a session*, because
`ADR-0050` §4 says *"no `ProfileResponse` field"*. Overturning that is the **architect's** and is
still unregistered, recorded here for the third round running rather than done quietly
(`CLAUDE.md` rule 5).

**No slot was filled for the sake of filling it**, and nothing below the bar was dressed up. Round 1
promoted three because three cleared the bar; round 2 promotes none because none did. A gate that
always spends its budget is not a gate.

## The harness — two capability tickets, and neither is a harness defect

The human asked for a ruling on the showdown/fold banner, which two rounds have now failed to read.

**It is not an `ADR-0089` §4 harness defect, and calling it one would be convenient rather than
true.** §4's harness defect is a case that **fails** without reproducing by hand. No case asserts
the showdown or fold banner, so nothing is red and there is nothing to exclude from `B(2)` — the
same reasoning `STORY-1209` used to refuse to file it. `EPIC-12`'s Definition-of-done box for
telling a harness defect from a product defect on the record therefore stays **unticked** for the
second round running, and this round does not tick it by relabelling a reach gap.

**But two rounds failing the same read is evidence, and the evidence is about the harness.** Round 1
called it a *gap in reach* and left it for the next `/qa-cycle` pass; the cost has now been paid
twice and is measurable: **check (a) on two of `duel-table-states.html`'s three frames is
unreachable by any round with the verbs `drive.mjs` has.** `wait` polls, and a frame that
auto-advances between polls is not slow — it is invisible to a poller, whatever the interval. That
is a capability the driver lacks, not a case that is wrong, and it is repaired in `scripts/qa/`,
which is where rule 6 puts harness work.

So it is filed as **`TASK-121008`**, against this round's story, targeting `scripts/qa/drive.mjs`,
and it is **excluded from `B(2)`** — not by rule 6, which does not reach it, but for the same reason
a card ticket is excluded: `B(N)` counts product defects and a missing driver verb is not one. That
is stated rather than filed under a rule that does not fit, because a manager who stretches an
exclusion to cover a convenient case has broken the exclusion for the round that needs it.

**`TASK-121009` is the second, and it is the catalogue's own arrears.** Two things:
`docs/test-plan.md` §UAT's screen inventory still prints `—` in the `card` column for `duels`,
`leaderboard`, `account` and `sign-in`, whose cards merged yesterday — the catalogue says four
screens have no card on the very round that proves they do. And the *settled, and not a finding*
list §*The dates* calls for. Both are `docs/test-plan.md`, which rule 6 names as harness territory,
and both are excluded from `B(2)` for the same stated reason.

**Neither harness ticket is in the fix set**, which holds `blocker` and `high` product defects
(rule 2) plus, when there are any, `ADR-0092` §4's card tickets. They are scheduled beside it and
counted against neither the eight nor `B(2)`.

## State this triage changed, disclosed

- **One `Create a duel room` press** on 9232, to read the waiting frame's computed styles for the
  regression check. It opened one `WAITING` room, which `ADR-0022` reaps after ten minutes, and the
  room was left with **one `forget-room`** — `pd.roomCode`, the single storage write `ADR-0089` §3
  licenses.
- **Navigation clicks only, otherwise**: *Account*, *Sign in*, *Your duels*, *Leaderboard*, *Back*.
  No sign-in was attempted, no sign-out performed, no name set, no duel played, no coin moved.
- **Reads**: `text`, `eval`, `shot`, plus one `window.scrollTo` to bring the account screen's
  controls into a capture. No database query, no application state seeded, no socket frame
  injected. `ADR-0089` §3 held throughout.
- **Nothing under `scripts/qa/` was changed by this triage.** A manager that edited the harness
  while judging whether the harness was at fault would be grading its own work.

## Tasks

**The fix set is three of a possible eight** — three product `high`s, in `ADR-0092` §4's order:
`blocker`s (none), then the `high`s that count in `B(2)`, then card tickets (none owed this round).
The two harness tickets are scheduled beside the fix set and counted in neither the eight nor
`B(2)`.

| ID | Title | Status |
| --- | --- | --- |
| [TASK-121001](../tasks/TASK-121001-the-duels-screen-wears-the-card-merged-for-it.md) | The `duels` screen wears the card merged for it, and its search field can be seen — *product, `high`, **counts in `B(2)`*** | ready |
| [TASK-121002](../tasks/TASK-121002-the-leaderboard-screen-wears-the-card-merged-for-it.md) | The `leaderboard` screen wears the card merged for it, and a row reads as rank, name and coins — *product, `high`, **counts in `B(2)`*** | ready |
| [TASK-121003](../tasks/TASK-121003-the-account-screens-controls-look-like-controls.md) | The `account` screen's *Sign in* and *Sign out* are the card's buttons, not sentences — *product, `high`, **counts in `B(2)`*** | ready |
| [TASK-121008](../tasks/TASK-121008-the-driver-can-read-a-screen-that-auto-advances.md) | The driver can read a screen that auto-advances between polls — *harness capability, `manual-verify`, **excluded from `B(2)`*** | ready |
| [TASK-121009](../tasks/TASK-121009-the-catalogue-records-the-cards-it-has-and-the-question-it-closed.md) | The catalogue records the cards it now has, and the question two rounds have closed — *harness, **excluded from `B(2)`*** | ready |
| [TASK-121004](../tasks/TASK-121004-the-front-door-finishes-the-card-it-started.md) | The front door finishes the card `TASK-120901` started — the fill, the code well, the wordmark — *product, `medium`, never scheduled by this cycle* | backlog |
| [TASK-121005](../tasks/TASK-121005-the-sign-in-submit-is-the-cards-fill-button.md) | The `sign-in` form's submit is the card's fill button, not a smaller one — *product, `medium`, one half of what was filed here* | backlog → **ready** (2026-08-31) |
| [TASK-121010](../tasks/TASK-121010-the-sign-in-screens-route-out-is-the-cards-link.md) | The `sign-in` screen's route out is the card's link, not body text — *product, `medium`, the other half* | backlog |
| [TASK-121006](../tasks/TASK-121006-the-account-forms-submits-are-the-cards-fill-button.md) | The `account` screen's two form submits are the card's fill button — *product, `medium`, backlog* | backlog |
| [TASK-121007](../tasks/TASK-121007-two-cards-and-a-margin-note-catch-up-with-the-client.md) | Two cards and a margin note catch up with the client that overtook them — *design; cards in arrears, `medium`, **not a product defect*** | backlog |
| [TASK-121011](../tasks/TASK-121011-the-product-name-leaves-the-shell-that-draws-it-above-every-screen.md) | The product's name leaves the shell that draws it above every screen — *product, `medium`, the first half of `TASK-121004`'s struck third scope item* | backlog |
| [TASK-121012](../tasks/TASK-121012-the-front-door-alone-wears-the-cards-wordmark.md) | The front door alone wears the card's wordmark, and it says the product's name — *product, `medium`, the second half* | backlog |

**Nine tickets at triage, three in the fix set; twelve since the two splits below.** The `medium` tickets
carry `depends_on` edges onto the ticket that touches the same file first — `TASK-121010` onto
`TASK-121004` (`Lobby.tsx`), `TASK-121006` onto `TASK-121003` (the account screen) — so that two
coders can never hold the same component at once. None of them is scheduled by this cycle; the edges
are for whoever drains the backlog later.

**Amended 2026-08-31: `TASK-121005` is two tickets.** As filed it covered both undressed controls on
the `sign-in` screen, and it could not be worked: its `verify:` block required a new test in
`SignInForm.test.tsx` while its `## Files` table named three other files, so its own definition of
done was unreachable inside its own scope. A coder took it and blocked before writing code. Adding
the fourth row is not the fix — `ADR-0068` caps a ticket at three files and grants `atomic:` only to
a change some **merged gate** refuses to let land in pieces, and no gate refuses this one: the two
controls live in different components with independent suites. So the ticket split. `TASK-121005`
keeps the *Sign in* submit, because `TASK-121006`, `TASK-121106` and `STORY-1211` already point at
that half by that id; `TASK-121010` takes *Forgot your password?* with `TASK-121005`'s `depends_on`
onto `TASK-121004`, which existed only to keep two coders off `Lobby.tsx` at once.

**This changes no finding of round 2.** One finding, still `medium`, still the same two controls on
one screen; nothing is re-scoped, re-graded, added or dropped, and neither half is scheduled by this
cycle. `EPIC-12` §Termination rule 1 freezes what a round may **repair**, and this round repaired
three `high`s and neither of these.

**Amended 2026-08-31: `TASK-121004`'s third scope item is two tickets.** That ticket's coder shipped
its fill and its code well (PR #1234) and refused to guess the third — the wordmark — because the
markup it quoted lives in `web-client/src/App.tsx` above every screen, not in the `Lobby.tsx` the
ticket declared, so dressing it in place would have put the front door's wordmark on all ten
surfaces. Routed as `DEC-099` under `CLAUDE.md` rule 5 and answered by
[`ADR-0098`](../../docs/adr/ADR-0098-the-wordmark-belongs-to-the-front-door-alone.md): **the front
door alone**, and the unconditional `<h1>` leaves `App.tsx`.

That ADR's §4 names four files, and four is one past `ADR-0068`'s cap. `atomic:` is bought only by
naming a **merged gate that fails on the smaller commit**, so the planner probed for one rather than
assuming it (`ADR-0069`): the shell half alone — `App.tsx` and `App.test.tsx` — runs the client
job's whole command, `npm run check`, at **117 files, 956 tests, exit 0**. Green, so no gate
forbids the split and under `ADR-0068` §4 it is two tickets. `TASK-121011` takes the shell;
`TASK-121012` puts the lockup on the front door and settles the one choice `ADR-0098` left to the
ticket — the lockup is an `h1` carrying `aria-label="Poker Duels"`, because the card's markup
concatenates to `PokerDuels` and, as the planner measured, a `getByRole` name query finds the
unlabelled markup too and so cannot gate the difference. The order is forced: with the shell's
`<h1>` still standing, the lockup is a second heading on the front door and
`screen.getByRole("heading")` throws in three merged `App.test.tsx` tests.

**This changes no finding of round 2 either.** The wordmark was already one third of one `medium`
finding filed as `TASK-121004`; nothing is re-scoped, re-graded, added or dropped, and neither half
is scheduled by this cycle. `TASK-121004`'s own row and status are untouched — its PR is in flight
and that row is the scheduler's.

**The `ready` beside `TASK-121005` is the ordinary backlog's, not this cycle's.** The invocation
ended at round 3's `PASS`, and rule 2's *never scheduled by this cycle* stopped binding with it;
these tickets are now the driver's to schedule like any other. `TASK-121005` is startable because
the split left it with no dependency — the edge onto `TASK-121004` went with the half that touches
`Lobby.tsx` — and `TASK-121010` stays `backlog` behind that edge, so no two coders hold one file.

## Acceptance criteria

- [ ] Every finding was deduped against both focuses' round stories and tickets before triage, and
      the eight repeats are recorded rather than refiled.
- [ ] Round 2 is determined to be a **baseline round**, the four screens and the commits that merged
      their cards are named, and the exemption from rule 4's comparison is stated explicitly rather
      than left to be inferred.
- [ ] `B(2)` is computed and stated: **3**, with all three exclusions named, counted and justified —
      including the two that are zero.
- [ ] The per-screen table marks checks **a**/**b**/**c** for all thirteen inventory rows, and the
      verdict carries no inline qualification because no cell reads `BLOCKED`.
- [ ] Every severity change is written down with a reason that is not the arithmetic, and the one
      case where the arithmetic pointed the other way is stated.
- [ ] The Ukrainian-date ruling is re-derived from both merged sources rather than cited, and the
      blessing is recorded somewhere a third round will read it.
- [ ] The showdown-banner reach gap is ruled on: **not** an `ADR-0089` §4 harness defect, filed as a
      capability ticket, and `EPIC-12`'s Definition-of-done box left unticked.
- [ ] Zero `DEC`s are promoted, and each of the report's two questions is shown closed by a named
      merged section.
- [ ] `python3 .github/scripts/lint_tickets.py` exits 0 with this story and its nine tickets on the
      board — ten since the 2026-08-31 split, which added a ticket and no finding.

## Out of scope

- **Repairing anything.** Repair is `build-epic`'s over the three fix-set tickets and the two
  harness tickets; the four backlog tickets are not this cycle's to run (rule 2).
- **`TASK-120912`.** It is round 1's ticket, still open at `low`. Naming it as the cluster's fifth
  site is not scheduling it, and folding a backlog `low` into a round-2 `high` would smuggle
  unscheduled work into the fix set.
- **The bare *Back* and the three lobby doors.** No card draws them; they contradict nothing and are
  recorded in §*What was not filed* as retrofit-story candidates, not filed.
- **English-only dates.** Designed behaviour with `ADR-0061` §Costs behind it. A product request if
  it is wanted, never a defect.
- **The `duel-end` meta line and the account offer's own card.** Both still need a product decision,
  both are named in `TASK-120911`'s *Out of scope*, and neither is guessed here.
- **The `account-offer.ts` credential-versus-session gap.** It needs `ADR-0050` §4 overturned, which
  is the `architect`'s decision and still unregistered. Recorded, not filed — for the third round
  running.
- **`STORY-1209`'s status.** Closing round 1's ledger is that round's business, not this story's.
