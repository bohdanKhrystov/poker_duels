---
id: STORY-1209
title: Round 1 (UAT) — the front door was never dressed, and four screens have no card
type: story
status: ready
parent: EPIC-12
labels: [process, qa, uat]
depends_on: []
---

## The round

**Round 1** of a `/qa-cycle uat regression` invocation — the **first round under the `uat` focus**
`ADR-0092` created. The round number lives here rather than in the id, per `EPIC-12`'s Stories
table; the id continues the round sequence after `STORY-1206`, `STORY-1207` and `STORY-1208`
having taken numbers without being rounds.

| | |
| --- | --- |
| Round | **1** |
| Focus | **`uat`** — conformance, reachability, copy against merged sources (`ADR-0092` §3) |
| Scope | `regression` — all 11 in-scope screen-states of `docs/test-plan.md` §UAT |
| Date | 2026-08-30 |
| Commit | `c05ee695` |
| Stack | `up` — db, server, web; browser profiles **not fresh** (both had played duels, both were named, one held a password) |
| Screens | 11 in scope, 11 walked; 2 (`verify`, `reset`) out of scope — no mailed link ever arrives |
| Findings | 15 reported |
| `B(1)` | **1** — `blocker` 0 + `high` 1, after dedupe and after the three exclusions |
| `B(0)` | **n/a** — a new invocation, so rule 4's comparison cannot apply |
| Baseline round | **no** — see §*Baseline* |
| Verdict | **`PROCEED (conformance unjudged on 4 of 11 screens)`** |

## What this record is not

`ADR-0089` §2c, restated for the third round running, and with `ADR-0092` §2c's UAT corollary
beside it because this is the focus that most invites the inflated reading:

> No coverage claim. The product of a run is a **dated round record**. Neither it nor
> `docs/test-plan.md` may be cited as coverage in an epic's `Metrics`, a Definition of done, or a
> ticket's `verify:`.

> **No round, and no `PASS`, may be cited as the thing that made the product ready.** Readiness is
> a judgment made while reading the record, and its written bar — if it ever has one — is
> `ADR-0093`'s two facts, neither of which any round supplies.

**This is a statement about one walk, on one machine, at commit `c05ee695`, on 2026-08-30.** Eleven
screen-states were looked at; four of them could not be judged for conformance at all, because they
have no card. `dist/` is still loaded by nothing — the walk ran against `npm run dev`, so
`ADR-0088` gap 3 survives this round exactly as it survived `STORY-1202`, `STORY-1205` and
`STORY-1206`.

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
| `duels` | the duel history list | **BLOCKED — no card** | judged | judged |
| `leaderboard` | the season standings | **BLOCKED — no card** | judged | judged |
| `account` | claiming, or the signed-in page | **BLOCKED — no card** | judged | judged |
| `sign-in` | the sign-in form | **BLOCKED — no card** | judged | judged |
| `verify` | confirming a mailed link | out of scope | out of scope | out of scope |
| `reset` | setting a new password from a mailed link | out of scope | out of scope | out of scope |

Four of the eleven in-scope screens are unjudged on conformance, which is why **every** statement
of this round's verdict carries that qualification inline and verbatim, here and in the terminal
report: **`PROCEED (conformance unjudged on 4 of 11 screens)`**. `verify` and `reset` are out of
scope, not blocked — no route reaches them (`ADR-0031` §7), so `ADR-0092` §4 files no missing-card
finding for either, exactly as `docs/test-plan.md` §UAT says.

## Baseline

**Round 1 is not a baseline round.** A baseline round is one in which a screen becomes
conformance-judgeable *for the first time*, its card merged in round *N−1*'s repairs
(`ADR-0092` §6). There is no round 0 in this invocation and no card merged into `design/screens/`
by any previous round's repairs, so no screen changed judgeability here. The determination is
recorded because it is the one rule this cycle's own machinery was recently repaired for
(`STORY-1208`), and because it is load-bearing **next** round: the four cards this round files are
merged in round 1's repairs, so **round 2 will be a baseline round** — `duels`, `leaderboard`,
`account` and `sign-in` become conformance-judgeable there for the first time, and rule 4 must not
compare `B(2)` against `B(1) = 1`. Rule 5's three-round budget binds regardless.

## Dedupe — and it spans both focuses

Searched: every story under `tasks/stories/` and every `TASK-12NNNN` under `tasks/tasks/` — five
round-or-not stories under `EPIC-12` (`STORY-1202`, `STORY-1205`–`STORY-1208`) and their 23
tickets. One ledger, both focuses: a UAT walk stumbles on functional defects it does not hunt, and
a defect seen under both focuses is one ticket or `B(N)` double-counts it.

- **Repeats: 2**, neither refiled.
- **Regressions: 0.** Nothing filed and marked `done` came back. The four round-1 repairs and the
  round-2 harness tickets are all in files this round's findings do not touch.
- **New: 13 findings**, mapped to 12 tickets.
- **Harness defects: 0** — see §*Rule 6*, which is where a harness defect was expected and did not
  turn up.

### Repeat 1 — the account screen offers the claim form to a profile that has a password

The `account` check-**c** finding is `TASK-120601`, open, and it is **not filed again**. This is the
rule that stops the backlog growing every round out of re-reports alone.

The behaviour matches, not the wording: `main.tsx` derives `signedIn` from a session token,
`AccountScreen.tsx` derives `showSignUp` from its converse, and a browser with no session is not a
browser whose player has no password. The observer reached it through *Sign out* after a
cross-device sign-in; `STORY-1206` reached it through *Sign out* too, and `TASK-120601`'s
*Out of scope* names that exact path in as many words:

> A browser that signed out — or whose session expired — still cannot tell a claimed profile from
> an unclaimed one, and still gets the form.

**Severity: `medium`, down from the observer's `high`, and the reason is not the arithmetic.** The
same defect was judged `medium` in `STORY-1206` on written reasons that still hold — no vision
promise is broken, the `credential` row is never touched, the server refuses the re-claim on its
own authority, and the product offers the workaround one control below the form. Re-grading a
deduped repeat `high` because a second focus found it would make severity a function of how many
agents looked. It is a repeat, so it does not enter `B(1)` at any severity.

**What is still owed, and it is not this round's to file.** Closing that half needs the account
screen to know whether a credential exists, and `ADR-0050` §4 says *"no `ProfileResponse` field"*.
Overturning that is a decision for the `architect`, registered by whoever picks the half up
(`CLAUDE.md` rule 5) — recorded here for the second round running rather than done quietly. It is
**not** `STOP_BLOCKED`: that state needs a **human-only** decision **gating this round's fix set**,
and this is neither.

### Repeat 2 — duel-history dates render in the reader's locale

The `duels` check-**c** Ukrainian-date finding is `STORY-1205`'s, adjudicated there and **filed as
nothing again**, for the same merged reason. The human asked for this one to be judged on the
merged source the observer named, so it is judged out loud:

- The observer named `CLAUDE.md`'s *"English everywhere"*. That rule names **code, tickets, docs,
  commits**. A reader's date format is none of the four.
- The behaviour has its own merged source pointing the other way.
  `web-client/src/profile/profile-text.ts`'s `finishedAtText` is documented — *"When the duel
  finished, **in the reader's locale**"* — and implemented as
  `new Intl.DateTimeFormat(options?.locales, …)`. `ADR-0061` §Costs names that behaviour and
  **accepts** it by name: *"`finishedAtText` renders instants 'in the reader's locale', so a player
  far enough east or west can read a duel as finishing on 1 September and find it counted in
  August."*
- So an observation that the dates are localised does not contradict a merged source; it agrees
  with two. Under `ADR-0092` §3 it is therefore **not a finding**. And it is not a live question
  either: a merged source that **blesses what shipped closes the question permanently**, so
  re-raising it would itself contradict a merged source. It is not promoted, and no promotion slot
  is spent on it.

**One correction to the report's particulars**, in the same spirit `STORY-1205` corrected `qa`'s.
The finding names `web-client/src/history/history-text.ts` as a second source. That module does not
format dates at all: `HistoryScreen.tsx:215` and `ProfileStrip.tsx:64` both call the same
`finishedAtText`. There is one behaviour from one module, not two — which is also why the observer
correctly saw it identically on the strip and on the full history row.

**If English-only dates are wanted, that is a product request and it starts as a request.** Filing
it as a defect would have `build-epic` change production code to satisfy a rule that does not cover
it — `ADR-0089` §4's failure class arriving from the other direction.

## Rule 6 — the by-hand reproduction, which found a product defect where a harness defect was expected

`EPIC-12` §Termination rule 6 and `ADR-0089` §4: before a `blocker` or a `high` may be filed, the
failing case must reproduce **by hand**. The finding this was aimed at is the `first — hosting`
check-**b** silent no-op on *Create a duel room*, which the observer deliberately reported without
a diagnosis. The stack was still up, so it was run rather than argued.

**The mechanism was varied, not just the operator.** A hand-check that reuses the harness's own
broken step inherits its fault, so the reproduction did not use `drive.mjs click` — which dispatches
an in-page `.click()`. It used a **trusted CDP mouse press and release** at the control's own
measured coordinates, which is as close to a player's hand as this machine has, driven from a
throwaway script outside the repository.

    Page.navigate → poll until the button has a rect → Input.dispatchMouseEvent (press, release)

Four runs on port 9232, at `c05ee695`:

| run | button painted | click dispatched | outcome |
| --- | --- | --- | --- |
| 1 | +24 ms | +39 ms | *Waiting for your rival* at +142 ms |
| 2 | +18 ms | +22 ms | **nothing, ever** — screen unchanged after 10 s |
| 3 | +17 ms | +19 ms | **nothing, ever** |
| 4 | +17 ms | +18 ms | **nothing, ever** |

**It reproduces, three times in four.** So it is a **product defect**, not a harness defect: it
counts toward `B(1)`, it is repaired in production code, and `EPIC-12`'s Definition-of-done box for
telling the two apart on the record stays unticked — this round did not supply that proof, because
the case it was aimed at turned out to be real.

**And the root cause is in production code, read from the console rather than guessed.** With
`Runtime`/`Log` enabled, the racing click prints:

    InvalidStateError: Failed to execute 'send' on 'WebSocket': Still in CONNECTING state.

`web-client/src/protocol/reconnecting.ts:63–84`, `attach()`, sets `live = true` on the line **after**
the socket is constructed and **before** it has opened. Its own `send` guard —

```
send(message: ClientMessage): void {
  // An action taken while disconnected is not an action.
  if (!live) return;
  current.send(message);
},
```

— therefore passes during the entire handshake, `connection.send` reaches
`options.socket.send(...)`, and the browser throws. React swallows the exception inside the
`onClick`, so the player gets no room, no error, no spinner and no recovery: the click is simply
lost. `Lobby.tsx:300` is `onClick={() => send({ type: "CreateRoom" })}` with no pending or disabled
state, so nothing on the screen changes to say anything happened.

**Severity: `medium`, down from the observer's `high`.** The reason, written out because it changes
which ticket gets scheduled:

- It breaks no promise in `EPIC-12`'s `high` row. Hole cards stay secret, the winner is right, the
  coins are right, the rematch works. `STORY-1206` fixed the reading that row gets — *a named list
  of product-integrity properties plus regressions, not a synonym for serious-feeling* — and that
  reading is not being inverted a round later because a different focus found the defect.
- It is not a regression. Nothing closed came back; this path has never been filed.
- **Nothing is lost but the click**, and the product's own control resolves it: press it again. That
  is the definition of `medium`.
- **The measured window is 4–20 ms wide on this machine** (socket open between +22 ms and +39 ms,
  button painted at ~+18 ms). No claim is made about its width in the offered deployment, because
  no such measurement was taken here and `ADR-0093` §— the readiness bar — is not something a round
  may assert against. What can be said honestly is that the window is the WebSocket handshake, and
  a handshake is longer over a network than over a loopback.
- **The severity did not move with the arithmetic, and it moved the arithmetic the expensive way.**
  Round 1 is exempt from rule 4, so `B(1) > 0` yields `PROCEED` and `B(1) = 0` yields `PASS`: grading
  this `high` would have *continued* the cycle and grading it `medium` *ends* it, if it were the
  only candidate. It is not the only candidate — `TASK-120901` is `high` on its own reasons — so the
  verdict is `PROCEED` either way and the incentive here is genuinely neutral. Said out loud
  because a downgrade that happens to shrink a count deserves the arithmetic in the open.

It is filed as `TASK-120906` with the reproduction, the console line and the mechanism attached. It
is the best-diagnosed ticket this round leaves behind, and being `medium` does not hide it —
`PROCEED` schedules a fix set; it does not close a backlog.

## Which artefact is the defect — the adjudication this round could not avoid

Seven of the eight conformance findings compare a screen against a card drawn on 2026-08-14/15
against decisions merged **after** it. So each one needed the same question answered before a
severity could be set: *does the client contradict the newest governing merged source, or does the
card?*

- **The client contradicts the card, and nothing later blesses the client** → a **product** defect.
  Filed at its judged severity; counts toward `B(1)`.
- **The card contradicts a decision merged after it was drawn, and the client implements that
  decision correctly** → a **card** defect. The product contradicts nothing, so it contributes
  nothing to `B(1)` — **not by a fourth exclusion**, which this round does not invent, but because
  `B(N)` counts product defects and a card in arrears is not one. It is still filed; the repair is
  the card. This is exactly the move `STORY-1205` made on the Ukrainian dates: a merged source that
  blesses what shipped ends the matter.

Three findings resolved the second way, and the human flagged one of them by name:

| finding | the card says | the client says | what governs |
| --- | --- | --- | --- |
| `enter-code` refusal copy | *This code doesn't open a duel.* | *No duel room has that code.* | **the client.** `Lobby.tsx:347` owns the literal; `ADR-0072` and `ADR-0073` both name it as the shipped correction; `docs/test-plan.md` `CORE-04` transcribes it as the expected wire text. **The card drifted** — the human's reading, confirmed |
| `hosting` waiting frame | no way out, no room-stays-open line | *Back to the lobby* + *The room stays open. That link still works for your rival, and it brings you back.* | **the client.** `ADR-0073` decided both strings and says in as many words that *"`design/screens/create-duel.html`'s waiting frame **gains** the control and the line verbatim"*, as `EPIC-06`'s work. That work never happened; the card is in arrears of an ADR that named it |
| `duel-end` *Back to lobby* / the offer section | *Back to lobby*; nothing after *Rematch* | *Back to the lobby*; an account-claim section | **the client.** `ADR-0073` fixes *Back to the lobby* as byte-identical across both components, and the offer is `STORY-0415`'s, merged 2026-08-28 — thirteen days after the card. `ADR-0091` §5 already registers *"carded-screen accretions … the account offer first among them"* as debt |

Those three are `TASK-120911`, a `medium` design ticket. **Two further `duel-end` divergences are
deliberately not repaired there**: the card's meta line asks for *"17 hands · 12 minutes · you took
the whole stack"*, and the wire carries no duration at all — `DuelResult.tsx`'s merged `metaLine`
KDoc explains why the line states stacks instead. Deciding what that line should say is a product
question, not a card correction, and it is named in the ticket's *Out of scope* rather than guessed.

## The two items the observer marked uncertain, adjudicated here

`ADR-0092`'s classifier puts a borderline item under `FINDINGS` so it reaches triage rather than
being lost with the round. Both arrived that way and **neither placement was inherited**.

**1. The away banner's countdown — the observer was unsure between check (a) and check (c).** It is
check **(c)**, and it stays a finding. Check (a) cannot hold it: `duel-table-states.html` draws no
away frame at all, so there is no card content to contradict. Its real source is
[`ADR-0046`](../../docs/adr/ADR-0046-the-table-says-away-timed-out-and-back.md) §3, which legislates
the numeral: *"The design fixes the numeral's shape (`0:45`, `45s`)."* `PresenceNotice.tsx:37–45`
renders `{presenceLine(…)}` immediately followed by `<span>{secondsRemaining(…)}</span>` with no
separator and no unit, so a player reads `The duel is paused.57`. That contradicts a merged ADR
section — a finding, `medium`, `TASK-120909`. The card's missing away/back frames are a separate,
smaller thing: card composition, named in `TASK-120911`'s *Out of scope* and owed to `ADR-0091` §5's
retrofit story.

**2. The screenshot's overlapping waiting frame.** The report's evidence for the hosting finding
includes a capture in which the code, the raw URL and *Copy the link* run into one line partly
hidden behind *Back to the lobby*. **No ticket rests on that capture.** `ADR-0092` §2 makes a
finding a looking human cannot see a harness defect and names clipped headless captures as the
example. The viewport measured here is 756 × 469, above the ~500 px floor where captures clip, so
the overlap is probably real — but *probably* is not evidence, and `TASK-120901` is grounded on two
things measured directly instead: computed styles, and a rect.

## The one `high` that counts, measured rather than transcribed

`TASK-120901`. The front door and the waiting frame — `design/screens/create-duel.html`, the card
for the first screen every player sees — were never dressed. Read from the running client at
`c05ee695`, not taken from the report:

```
[{"tag":"BUTTON","cls":null,"txt":"Create a duel room","bg":"rgba(0, 0, 0, 0)","pad":"0px","radius":"0px","border":"0px"},
 {"tag":"INPUT", "cls":null,"txt":"",                  "bg":"rgba(0, 0, 0, 0)","pad":"0px","radius":"0px","border":"0px"},
 {"tag":"BUTTON","cls":null,"txt":"Join the duel",     "bg":"rgba(0, 0, 0, 0)","pad":"0px","radius":"0px","border":"0px"}, …]
```

Every primary control is an unclassed native element with no background, no padding, no radius and
no border. `Lobby.tsx` carries eleven `className` attributes in the whole file, and the ones it has
arrived with later work — the `ADR-0073` way-out link is dressed; the controls the card draws are
not.

**The sharpest consequence is a control a player cannot see.** The room-code field:

```
{"rect":{"x":101.5,"y":79.5,"w":167,"h":22.5},"bg":"rgba(0, 0, 0, 0)","border":"0px solid rgb(236, 233, 227)",
 "outline":"rgb(236, 233, 227) none 3px","placeholder":null,"bodyBg":"rgb(19, 18, 17)"}
```

A 167 × 22.5 px region with a transparent background, a zero-width border, no outline and no
placeholder, on a `rgb(19,18,17)` body. There is nothing on the screen to see. `drive.mjs` finds it
because it has a rect; a player looking at the door does not, which is what check **b** is for and
what `ADR-0092` §2's *observable by a human looking* test asks. It was looked at, in a capture, and
the front door reads as three lines of plain text over an empty space.

**Severity `high`, unchanged from the observer, and on a stated line rather than a feeling.** The
line this round applied uniformly:

- **`high`** — a card's vocabulary is absent wholesale, or a control the card draws cannot be seen
  by a player's eye.
- **`medium`** — the card is transcribed and a specific element diverges.
- **`low`** — a cosmetic detail inside a transcribed screen.

The top row is anchored, not invented: `ADR-0092` §4 makes a screen with **no** card a `high`, and a
merged, accepted card that was never transcribed at all is not a smaller problem than a card that
was never drawn. Grading the absence `high` and the total non-transcription `medium` would be
incoherent, and the incoherence would fall on the product's front door.

## `B(1)` = 1

`blocker` 0 + `high` 1, after dedupe and after all three exclusions. **Every exclusion is stated
with its reason, because a manager that forgets one flips a verdict.**

| class | count | in `B(1)`? | why |
| --- | --- | --- | --- |
| product `high` | **1** | **yes** | `TASK-120901` — the front door's card was never transcribed |
| product `blocker` | 0 | yes | none found |
| **1. harness defects** | **0** | no — excluded | `ADR-0089` §4 / rule 6. The one candidate reproduced by hand under a varied mechanism and is a product defect. A stale catalogue counted here would read as a product getting worse |
| **2. missing cards** | **4** | no — excluded | `ADR-0092` §4. `duels`, `leaderboard`, `account`, `sign-in` are `ADR-0091` §5's **registered debt being collected**, not the product decaying. Counted, they would set round 2 the bar of *beat five* over a queue of design authoring — rule 4 governing the wrong quantity |
| **3. decision-born tickets** | **0** | no — excluded | `ADR-0092` §5. No ticket this round comes from a `product-owner` answer. The three `DEC`s promoted below are answered on their own clock; anything they yield enters the **earliest subsequent** triage, never this one (rule 1) |
| card in arrears | 1 ticket | no | not a fourth exclusion — the product contradicts nothing, so there is no product defect to count. See §*Which artefact is the defect* |
| `medium` | 6 | no | rule 4 counts `blocker` and `high` only |
| `low` | 1 | no | as above |
| repeats | 2 | no | removed by dedupe before the count |

**Nothing was deferred to shrink the number.** The fix set holds five tickets against a cap of
eight, so rule 3 never bound and nothing qualifying was pushed out. A deferral would have counted
in `B(1)` anyway, filed or not.

**Two severities were lowered and both reasons are above, neither of them arithmetic**: the
account-screen repeat (`high` → `medium`, on `STORY-1206`'s standing reasons) and the silent no-op
(`high` → `medium`, on the `EPIC-12` severity table, with the note that the downgrade costs the
cycle a round rather than saving one). Four missing-card findings kept the observer's `high` and are
excluded by rule, not by grade — the distinction matters, because `ADR-0092` §4 puts them in the fix
set at that severity.

## Verdict: `PROCEED (conformance unjudged on 4 of 11 screens)`

`B(1) = 1 > 0`, so not `PASS`. `N = 1 < 3`, so not `STOP_BUDGET`. Round 1 has no round 0, so rule
4's comparison does not apply and `STOP_DIVERGING` cannot fire — and this is **not** a baseline
round, which is recorded because round 2 will be one. No unanswered human-only decision gates a
member of the fix set, so not `STOP_BLOCKED`: the three `DEC`s promoted below are the product
owner's, they gate nothing in the fix set, and `ADR-0092` §5 keeps the cycle warm while they are
answered.

**The qualification is part of the verdict, not a footnote.** Four of eleven in-scope screens have
no card, so this round judged conformance on seven. Under the human's run-now call, round 1 was
always going to be largely design authoring, and the one line anyone reads has to say so.

## The three questions promoted (`ADR-0092` §5)

The observer asked exactly three, on three distinct screens, so the cap binds without forcing a
choice and `DEC-088` — *which screens' questions take the slots when more than three clear the
bar* — does not bite this round. All three clear both halves of the bar: each names a concrete
choice answerable in one sentence, and each bears on a player's ability to tell what is going on or
what they may do. All three are the **product owner's**, registered in `docs/adr/README.md` and
`tasks/BOARD.md`.

| DEC | screen | the question | why it clears the bar |
| --- | --- | --- | --- |
| `DEC-089` | `result` | Should a post-verdict account-claim nudge share the verdict's own type weight, or should the verdict stay the single largest thing on the screen? | Concrete and one-sentence-answerable. `AccountOffer.tsx:25` renders the nudge headline at `text-display font-bold`, the same weight as *Victory* itself — so it bears directly on what the player reads first after a duel, which is the verdict |
| `DEC-090` | `account` | Should *Attach a recovery address* — which asks for a *Current password* — appear on a device that has no password yet, or only once one exists? | Concrete and one-sentence-answerable. An option offered that cannot succeed is *what they may do* stated falsely |
| `DEC-091` | `sign-in` | Should *Back* on the sign-in screen return to the account screen it was opened from, or to the lobby? | Concrete and one-sentence-answerable. It is where the player ends up, which is *what is going on* at its plainest |

**Nothing else was promoted, and nothing was invented to fill a slot.** Two observations that could
have been dressed as questions were not: the Ukrainian dates (closed by a merged source, §*Repeat
2*) and the *Copy the link* / *Copy link* wording, which names a concrete choice but does **not**
bear on a player's ability to tell what is going on — below the bar, recorded here unanswered, and
not re-recorded while the screen is unchanged. It is folded into `TASK-120911` in the
better-sourced direction instead: `ADR-0073` is the newest merged signal on that frame's phrasing
and it chose the definite article for the control beside it.

**An answer changes nothing about this round.** `EPIC-12` §Termination rule 1 freezes the fix set at
this triage; a ticket any of these three yields enters the earliest **subsequent** round's triage,
or the ordinary backlog once the cycle ends.

## The observer's two notes, treated as observations

Carried verbatim in the report, and neither is filed.

1. **The near-tautology caution, answered in the useful direction.** `ADR-0092` §Consequences
   predicted that round 1's conformance would be close to a tautology, because the missing cards
   would be drawn from what shipped. That risk is real for round 2 — the four cards this round
   files will be composed by looking at four shipped screens — but it did **not** apply to the seven
   screens that already had cards: six of the seven diverge substantively, which is the opposite
   failure and the reason `B(1)` is not zero. Only `rematch-states.html` matched cleanly, verified
   through `getComputedStyle`/`className` rather than by eye. Worth recording against the day
   someone reads a future `PASS`: the cards that predate the client have proved to be a real test,
   and the cards that follow it have not been tested yet.
2. **Two sub-states the harness could not catch.** A natural multi-street showdown reveal and the
   rematch screen's transitional *"Both — it begins / dealing hand 1…"* copy both resolved faster
   than a fresh `drive.mjs` process could poll. **This is not filed as a harness defect**, and the
   distinction is deliberate: `ADR-0089` §4 defines a harness defect as a case that **fails**
   without reproducing, and nothing failed here — no case asserts either sub-state, so there is no
   red to explain. It is a **gap in reach**, the text-read analogue of `ADR-0092` §2's
   *geometry read taken mid-transition*. Filing it would put a ticket against `EPIC-12` for a case
   that does not exist; writing that case is `qa-cases`' work and `ADR-0090` §1 makes authoring a
   separate command from running. Named here so the next `/qa-cases` pass can pick it up.

## State this triage changed, disclosed

Not none, unlike `STORY-1206`, so it is itemised.

- **Two `Back to the lobby` clicks** (9232 and 9233), each calling `forgetRoom()` — the single
  storage write `ADR-0089` §3 licenses, `pd.roomCode`.
- **Seven `Create a duel room` presses** across the reproduction runs, three by `drive.mjs`, four by
  trusted CDP input. Each successful one opened a `WAITING` room on the server, which `ADR-0022`
  reaps after ten minutes. No duel was played, no coin moved, no row was written by hand.
- **Reads only, otherwise**: `text`, `eval`, `shot`, and `Page.navigate`. No database query, no
  application state seeded, no socket frame injected. `ADR-0089` §3 held throughout.
- **One throwaway script under `/tmp`**, outside the repository, for the trusted-input reproduction.
  Nothing under `scripts/qa/` was changed by this triage — a manager that edited the harness while
  judging whether the harness was at fault would be grading its own work.

## Tasks

**The fix set is five of a possible eight** — one product `high` first, then four card tickets, in
`ADR-0092` §4's order: `blocker`s, then the `high`s that count in `B(N)`, then cards.

| ID | Title | Status |
| --- | --- | --- |
| [TASK-120901](../tasks/TASK-120901-the-front-door-wears-the-clients-tokens.md) | The front door and the waiting frame wear the client's tokens, and the room-code field can be seen — *product, `high`, **counts in `B(1)`*** | ready |
| [TASK-120902](../tasks/TASK-120902-a-card-for-the-duels-screen.md) | A card for the `duels` screen — *missing card, `high`, **excluded from `B(1)`*** | ready |
| [TASK-120903](../tasks/TASK-120903-a-card-for-the-leaderboard-screen.md) | A card for the `leaderboard` screen — *missing card, `high`, **excluded from `B(1)`*** | ready |
| [TASK-120904](../tasks/TASK-120904-a-card-for-the-account-screen.md) | A card for the `account` screen — *missing card, `high`, **excluded from `B(1)`*** | ready |
| [TASK-120905](../tasks/TASK-120905-a-card-for-the-sign-in-screen.md) | A card for the `sign-in` screen — *missing card, `high`, **excluded from `B(1)`*** | ready |
| [TASK-120906](../tasks/TASK-120906-the-client-never-sends-on-a-socket-that-has-not-opened.md) | The client never sends on a socket that has not opened — *product, `medium`, never scheduled by this cycle* | backlog |
| [TASK-120907](../tasks/TASK-120907-the-join-path-ships-neither-screen-its-cards-draw.md) | The join path ships neither of the two screens its cards draw — *product, `medium`, backlog* | backlog |
| [TASK-120908](../tasks/TASK-120908-the-tables-sizing-control-is-the-cards-presets.md) | The table's sizing control is the card's presets, not a range slider — *product, `medium`, backlog; its coder shipped nothing and **routed twice on 2026-08-31** under `CLAUDE.md` rule 5 — [`ADR-0100`](../../docs/adr/ADR-0100-the-driver-reaches-an-amount-by-pressing-what-a-player-presses.md) answered `DEC-100` (how a scripted duel reaches an amount once the slider goes) and registered `DEC-101` for the product owner (what `pot`, `⅓` and `½` each compute). The ticket is **rewritten, not amended**, over the six files that ADR's §7 names, and stays blocked on `DEC-101`* | backlog |
| [TASK-120909](../tasks/TASK-120909-the-away-countdown-takes-the-shape-adr-0046-names.md) | The away countdown takes the shape `ADR-0046` §3 names — *product, `medium`, backlog* | backlog |
| [TASK-120910](../tasks/TASK-120910-the-profile-strip-shows-the-name-it-was-just-given.md) | The profile strip shows the display name it was just given — *product, `medium`, backlog* | backlog |
| [TASK-120911](../tasks/TASK-120911-three-cards-carry-the-strings-their-adrs-settled.md) | Three cards carry the strings the ADRs that superseded them settled — *design; card in arrears, `medium`, **not a product defect*** | backlog |
| [TASK-120912](../tasks/TASK-120912-not-now-is-dressed-like-the-control-beside-it.md) | The result screen's *Not now* is dressed like the control beside it — *product, `low`, backlog* | backlog |

## Acceptance criteria

- [ ] Every finding was deduped against both focuses' round stories and tickets before triage, and
      the two repeats are recorded rather than refiled.
- [ ] The check-**b** silent no-op was reproduced **by hand with a varied mechanism** (trusted CDP
      input, not `drive.mjs`'s in-page click), and the result — product, not harness — is written out
      with its console line and its measured window.
- [ ] `B(1)` is computed and stated: **1**, with all three exclusions named, counted and justified.
- [ ] The per-screen table marks checks **a**/**b**/**c** for all thirteen inventory rows, and every
      statement of the verdict carries its qualification inline and verbatim.
- [ ] Whether round 1 is a baseline round is determined and recorded — **no** — together with the
      determination it forces next round.
- [ ] Every severity change is written down with a reason that is not the arithmetic.
- [ ] Exactly three `DEC`s are promoted, one per screen, each with the two halves of the bar shown,
      and each registered in both registers.
- [ ] `python3 .github/scripts/lint_tickets.py` exits 0 with this story and its twelve tickets on
      the board.

## Out of scope

- **Repairing anything.** Repair is `build-epic`'s over the five fix-set tickets; the seven backlog
  tickets are not this cycle's to run (rule 2).
- **Answering any of the three `DEC`s.** The product owner answers by deriving from
  `docs/vision.md`; a ticket an answer yields enters the **earliest subsequent** triage (rule 1).
- **The second half of `TASK-120601`.** It needs `ADR-0050` §4 overturned, which is the
  `architect`'s decision and still unregistered. Recorded, not filed.
- **English-only dates.** Designed behaviour with `ADR-0061` §Costs behind it. A product request if
  it is wanted, never a defect.
- **The `duel-end` meta line, and the account offer's own card.** Both need a product decision, both
  are named in `TASK-120911`'s *Out of scope*, and neither is guessed here.
- **Adding catalogue cases** for the showdown and rematch sub-states the harness could not reach.
  `ADR-0090` §1 makes authoring and running two commands; the next `/qa-cases` pass owns it.
- **`STORY-1205`'s and `STORY-1206`'s statuses.** Closing a previous round's ledger is that round's
  business, not this story's.
