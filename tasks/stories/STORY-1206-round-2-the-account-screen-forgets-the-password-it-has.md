---
id: STORY-1206
title: Round 2 — the account screen forgets the password the profile has, and the cycle ends
type: story
status: done
parent: EPIC-12
labels: [process, qa]
depends_on: [STORY-1205]
---

## The round

**Round 2** of the `/qa-cycle regression` invocation `STORY-1205` opened. The round number lives
here rather than in the id, per `EPIC-12`'s Stories table.

| | |
| --- | --- |
| Round | **2** |
| Scope | `regression` — SMOKE (6) + CORE (20) + EPIC-04 (5) + EPIC-05 (5) |
| Date | 2026-08-29 |
| Commit | `c7b35f4b` |
| Stack | `up` — db, server, web; fresh browser profiles, dev server restarted on the repaired client |
| Cases | 36 catalogued; **34 run, 34 passed, 0 failed**, 2 blocked |
| `B(2)` | **0** — `blocker` 0 + `high` 0, after dedupe and after harness defects are excluded |
| `B(1)` | 1 |
| Verdict | **`PASS`** |

## What this record is not

`ADR-0089` §2c, restated for the same reason `STORY-1205` restated it — a round record that omits
it invites exactly the reading the condition forbids, and this one ends in `PASS`, which is the
reading most easily inflated:

> No coverage claim. The product of a run is a **dated round record**. Neither it nor
> `docs/test-plan.md` may be cited as coverage in an epic's `Metrics`, a Definition of done, or a
> ticket's `verify:`. A `PASS` is a statement about one run, on one machine, at one commit.

**This is a statement about one run, on one machine, at commit `c7b35f4b`, on 2026-08-29.** Thirty-
four cases passed, none failed, two never ran, and one defect was found — outside every case, one
click beyond `04-02`. `PASS` means *this round's report carried no `blocker` and no `high`*. It does
not mean the product has no defects; the round found one and it is filed.

`dist/` is still loaded by nothing — every case ran against `npm run dev`, so `ADR-0088` gap 3
survives this round exactly as it survived `STORY-1202` and `STORY-1205`.

## What round 1's repairs did, measured rather than assumed

All four merged before this round, and each is visible in this round's outcomes:

| ticket | what it was for | this round |
| --- | --- | --- |
| `TASK-120501` | every request declares `application/json` | `04-02`…`04-05` were **unreachable** in round 1 behind a sign-up that always `400`ed; all four ran and passed |
| `TASK-120503` | five rows made history-independent | `04-02` and `05-03` passed on a database full of finished duels, which is what would have reddened them |
| `TASK-120505` | the driver does not click what a player cannot see | `05-04` reported *"found 1 match(es) for 'Show more', all invisible"* instead of clicking a hidden control — the fix behaving exactly as designed |
| `TASK-120506` | a case can end a browser session | `CORE-18`/`CORE-19` — round 1's phantom `high` — passed, B reading the away notice and then `Your rival is back.` |

`TASK-120506` is the one worth naming twice. Round 1 filed `CORE-18` as a product `high`,
dispatched it to a coder, and then withdrew it as a harness defect. This round, with a verb that can
actually end a session, the case passes. **The withdrawal was right**, and the evidence is a passing
case rather than an argument.

## Dedupe

Searched: every story under `tasks/stories/` and every `TASK-12NNNN` under `tasks/tasks/` — ten
tasks and five stories, of which `STORY-1202` and `STORY-1205` are round ledgers.

- **Repeats: 1.** `CORE-03` is blocked for the same reason as in round 1 — a round allocates two
  browser profiles and the case needs a third. It is already `TASK-120504`, which is open, so it is
  **not filed again**. See §*The two blocked cases*.
- **Regressions: 0.** Every defect filed and marked `done` stayed fixed; the table above is the
  check. Nothing that was closed came back.
- **New: 4** — one product defect and three catalogue defects, none of which matches anything
  already filed.

**The one product defect is new, not a regression, and the distinction was checked rather than
assumed.** `AccountScreen.tsx`'s `!signedIn` guard arrived in `11281ff2` (`TASK-041227`, `#1094`) on
2026-08-26, three days before this cycle opened, and `TASK-120501`'s diff does not touch that file.
Round 1 could not have found it: no claim ever succeeded, so no browser ever reached the state.
`TASK-120501` made the defect **reachable**, not present. A defect newly reachable because a
blocking defect was repaired is a **new** defect — a regression is a closed ticket coming back, and
none did.

## The product defect: `TASK-120601`

`qa` reported it `medium`, one click beyond `04-02`'s own check, and asked for a severity rather
than a rubber stamp.

### It reproduces by hand (`ADR-0089` §4)

Run at `c7b35f4b` on the live stack with the driver, which is a player's hands (`ADR-0089` §3), on
**two** profiles that were claimed by two different paths:

    node scripts/qa/drive.mjs 9233 open              # a plain navigation to /
    node scripts/qa/drive.mjs 9233 click "Account"
    node scripts/qa/drive.mjs 9233 text 1200

renders *Handle / Password / Give this profile a password* and **no** `Your password signs in to
this account.` — for the player behind device `MjF3MdDWR_cDpsfbEQAlIw`, whose `credential` row
(`84e8e12a-…`, `password`, `winnerplayer`, secret present) was read from the database in the same
minute. The same three commands on 9232 (`UEMJWw0n0DezVTe_L0McoQ`, `loserplayer`) render the same
screen. **It is a product defect and it counts as one.**

Two things were checked rather than assumed, because round 1 was burned by a reproduction that
inherited the harness's own fault:

- **The control is visible to a player.** `hidden:false`, `offsetParent` shown, 312px wide — not a
  `TASK-120505`-style phantom the driver can see and a player cannot.
- **The browser genuinely holds no session.** `localStorage` is `{"pd.deviceId":"MjF3M…"}` — one
  key, no token — so `signedIn` is false for the honest reason, not because the harness cleared
  something.

### The mechanism, which decides the repair

`main.tsx:182` sets `signedIn = readSessionToken(…) !== null`, and `AccountScreen.tsx` derives both
statements from it — `showPasswordRoute = signedIn && …`, `showSignUp = !signedIn && …`. The comment
above them derives the first correctly: sign-in is the only endpoint that issues a token, so a
browser holding one has a password. **That runs one way only, and the second guard is its converse.**
`docs/protocol.md` already names the missing step — *"no session is issued and the client signs in
afterwards"* — and the client does not.

### Severity: `medium`, unchanged, and why

The count this feeds decided the verdict, so the reasoning is written out rather than asserted.

- **No vision promise is broken.** Hole cards stay secret, the winner is right, the coins are right,
  rematch works. `docs/vision.md` promises duels, coins, a leaderboard and replay; it says nothing
  about account screens, and `EPIC-12`'s `high` row is a named list of product-integrity properties
  plus regressions, not a synonym for *serious-feeling*.
- **Nothing is lost or corrupted.** The `credential` row is untouched throughout, the server refuses
  the re-claim on its own authority (`That handle is taken, or this profile already has a
  password.`), and no action available on the false screen can change any state.
- **There is a workaround the product itself offers, one control below the form.** *Sign in*, and
  `ADR-0083` §2 lands a successful sign-in on `#/account`, which then states the password route.
  That is the definition of `medium`: a real defect with a workaround.

**The test I applied to my own reasoning.** `medium` yields `B(2) = 0` and `PASS`; `high` yields
`B(2) = 1` and `STOP_DIVERGING`. So I asked whether I would call it `high` if `B(1)` had been five,
where `high` would still have meant `PROCEED`. **No** — the credential is intact, the server is
correct, and the product's own door resolves it. The severity does not move with the arithmetic,
which is the only defence against the two ways `EPIC-12` §Termination can be cheated.

**And it is not hidden by being `medium`.** `TASK-120601` is filed with the mechanism, the
reproduction and a `verify:` block, and it is the most valuable open ticket this cycle leaves
behind. `PASS` ends the loop; it does not close the ticket.

### What `TASK-120601` does not repair, said here rather than discovered later

The ticket repairs the reported path — a claim leaves the browser holding a session, per
`docs/protocol.md`'s own sentence. **A browser that signed out still cannot tell a claimed profile
from an unclaimed one**, which is the second path `qa` reproduced, through the product's own *Sign
out* control. Closing that needs the account screen to know whether a credential exists, and no
endpoint carries the fact: `ADR-0050` §4 says *"no `ProfileResponse` field"* and *"`deviceRouteLive`
is the whole of what the screen reads"*, and `STORY-0412` recorded the question as already settled on
a derivation this defect proves incomplete.

**That sentence is now known to be wrong, and overturning it is a decision, not a repair.** It is
technical, so it is the `architect` agent's, registered as a `DEC` by whoever picks the half up
(`CLAUDE.md` rule 5). It is **not** `STOP_BLOCKED`: that state is for a decision only the human can
answer, and this is not one.

## The two blocked cases, which are not the same kind

A blocked case is not a failure and neither is counted in `B(2)`.

- **`CORE-03` — known, filed, and deliberately still open.** It needs a third browser profile; the
  skill starts two. Already `TASK-120504`; **not filed again**, which is the rule that keeps the
  backlog from growing every round out of re-reports alone. One thing about it does need saying:
  `TASK-120504` is still `backlog` although its stated unblocking condition — *"goes `ready` when
  `TASK-120503` merges"* — was met when `TASK-120503` merged as `c1cbb9bc`. Nothing in this round
  moves another round's ticket, so it is recorded here instead: `CORE-03` stays blocked until that
  ticket is made `ready` and lands.
- **`05-04` — new, and a catalogue defect.** The season holds 8 entries on one page, so
  `nextCursor` is `null`, *Show more* is correctly hidden, and the prescribed walk cannot be
  performed. The precondition is one a round **cannot** supply: a page-2 needs more players than a
  round has profiles, and `ADR-0089` §3 forbids seeding rows to reach a screen. `TASK-120604`.

## The three catalogue defects, and why none of them counts

`ADR-0089` §4 and `EPIC-12` §Termination rule 6: a defect in the catalogue or the harness is filed
against this epic, repaired in `scripts/qa/` or `docs/test-plan.md`, **excluded from `B(2)`**, and
**no production code may change to make it pass**. The exclusion is the load-bearing half — counted,
these three would have made `B(2) = 3` and ended a healthy round `STOP_DIVERGING`.

- **`TASK-120602` — the one prescribed SQL template names a column that was dropped.** `05-03`'s
  preamble selects, filters and groups on `p.device_id`; `V7__device_binding.sql` dropped it on
  `ADR-0049` §1. Confirmed against the running database: `\d player` lists four columns and none is
  `device_id`. **One correction to the report**, which said the template is `05-03`'s *and*
  `CORE-13`'s: `CORE-13`'s row prescribes no SQL at all, so there is exactly one stale query — the
  one that preamble calls *"`CORE-13`'s shape"*, which is why a `CORE-13` tester lands on it.
- **`TASK-120603` — `05-02`'s own recipe manufactures a phantom.** It borrows `05-05`'s headerless
  `fetch('/api/standings')` for a read that **is** identity-scoped, and an anonymous read answers
  `"self": null` for a player who has a place. Measured this round, one profile, one variable
  changed: headerless → `self: null`; with `X-Device-Id` → `{"rank":4,"coins":0}`; and the screen
  read `You are rank 4 this season, on 0 duel coins.` **The tester nearly filed this as a `05-02`
  failure and caught it.** That is the second round running in which a case's own recipe produced a
  phantom, which is why it is a ticket and not a note.
- **`TASK-120604` — `05-04` asks for a walk no round can perform.** Above. The rewrite makes the
  walk conditional on `nextCursor` and gains an assertion the blocked case was silently not making:
  *Show more* is offered exactly when there is another page — the behaviour `TASK-120505`
  established as correct, which today no case covers at all.

All three land in `docs/test-plan.md` and are therefore **strictly ordered**: `TASK-120602` is
`ready`, the other two are `backlog` behind it, the same sequencing `STORY-1205` used for
`TASK-120503`/`TASK-120504` and for the same reason — a batch that started them together would
conflict by construction. `TASK-120504` belongs in that queue too.

## The observation the reporter ruled out, confirmed against a merged ADR

`qa` reported, deliberately unfiled, that at showdown the losing seat's `holeCards` arrive as `[]`
to the winner even in the terminal snapshot, and judged it consistent with the secrecy design.

**Confirmed, and it is not a judgement call.**
[`ADR-0008`](../../docs/adr/ADR-0008-loser-mucks-at-showdown.md) decides it in one sentence: *"At a
showdown, the last aggressor shows first, and the losing hand is never revealed"* — *"a mucked hand
appears in **no event**, exactly as a folded hand does"*. `docs/duel-rules.md` agrees: *"The loser
may muck."*

So the product does what a merged decision says. **No `DEC` is raised**: `CLAUDE.md` rule 5 asks for
one only where no ADR covers the question, and one does. If a showdown reveal is wanted, that is a
product request for the `product-owner` agent and it starts as a request, not as a defect — the same
resolution `STORY-1205` gave the Ukrainian dates, and for the same reason.

## The catalogue did not catch this round's only defect, and no case is added for it

`04-02` **passed**; the defect sits one click beyond it, on a screen the case never opens. That is
worth recording, and it is not something this story may fix.

`ADR-0090` §1 licenses writing the catalogue and running it as **two commands**, never one turn:
adding a case now — mid-cycle, on a finding from the round in progress — is precisely the
composition that ADR forbids, whatever the case would be worth. **It is work for the next
`/qa-cases` pass**: a case that claims a profile, reloads, opens the account screen and reads the
routes statement. Named here so it is picked up by the mechanism that is allowed to pick it up.

## `B(2)` = 0

`blocker` 0 + `high` 0, after dedupe and after the three catalogue defects are excluded.

- The one product defect is `medium`, for the reasons written out above, and `EPIC-12`
  §Termination rule 4 counts `blocker` and `high` only.
- **Nothing was deferred to shrink the number.** The fix set would take eight tickets and no
  finding qualified for it; a deferral would have been counted in `B(2)` anyway, filed or not.
- **No severity was lowered.** `qa` said `medium` and it stays `medium` — the only severity
  question this round was whether to raise it, and the answer, with its reasoning, is above.
- **Three catalogue defects were excluded, and that exclusion is the rule working as designed.**
  Counted, `B(2)` would read 3 against `B(1) = 1` and this round would end `STOP_DIVERGING` on a
  product that got strictly better — the exact inversion `ADR-0089` §4 exists to prevent.

## Verdict: `PASS`

`B(2) = 0`, so `EPIC-12` §Termination's first exit state is reached and **the cycle ends,
successfully**. It ends after two rounds of a budget of three, having repaired the defect that made
the entire identity write path dead and having left four tickets — one product, three catalogue —
in the backlog with reproductions attached.

`STOP_DIVERGING` was never reached: `B` went `1 → 0`, which is the strict decrease rule 4 requires.
`STOP_BUDGET` was not reached: two rounds of three. `STOP_BLOCKED` was not reached: the one decision
this round surfaced is the `architect`'s, not the human's.

## State this triage changed, disclosed

**None.** Every command run during triage was a read or a navigation: `open`, `click`, `text`,
`device` and `eval`, plus two `GET /api/standings` calls and four read-only `psql` queries. No row
was written, no storage key was set or cleared, and no profile was claimed. `STORY-1205` disclosed
two mutations; this round has none, and saying so is worth as much as saying so when there are.

## Tasks

| ID | Title | Status |
| --- | --- | --- |
| [TASK-120601](../tasks/TASK-120601-a-claimed-profile-is-never-offered-the-claim-form-again.md) | A claimed profile is never offered the claim form again — *product, `medium`, backlog* | backlog |
| [TASK-120602](../tasks/TASK-120602-the-catalogues-coin-query-reads-the-device-binding-table.md) | The catalogue's coin query reads the table the device id actually lives in — *harness; excluded from `B(2)`* | ready |
| [TASK-120603](../tasks/TASK-120603-05-02s-standings-read-carries-the-identity-the-app-sends.md) | `05-02`'s standings read carries the identity the app sends — *harness; excluded from `B(2)`* | backlog |
| [TASK-120604](../tasks/TASK-120604-05-04-walks-the-pages-that-exist.md) | `05-04` walks the pages that exist, and says what a hidden *Show more* proves — *harness; excluded from `B(2)`* | backlog |

**The fix set is empty**, and that is the correct outcome rather than an omission: `EPIC-12`
§Termination rule 2 admits only `blocker` and `high` to a fix set, and this round produced neither.
`TASK-120601` is a `medium` filed to the backlog and **never scheduled by this cycle**;
`TASK-120602`, `TASK-120603` and `TASK-120604` are catalogue tickets against this epic, in none of
whose `## Files` tables any production file appears.

## Acceptance criteria

- [ ] Every finding in round 2's report was deduped against the existing round stories and tickets
      before triage; the search and its result are recorded, including the one repeat that was
      **not** refiled.
- [ ] The one product defect reproduces by hand, on a path whose visibility and storage state were
      checked rather than assumed, and the reproduction is written out (`ADR-0089` §4).
- [ ] `B(2)` is computed and stated: **0**, with the three exclusions named and justified.
- [ ] The verdict is exactly one of the five named exit states: **`PASS`**.
- [ ] The severity `qa` assigned was re-judged rather than adopted, and the reasoning — including
      the test applied against the arithmetic — is written down. No severity moved.
- [ ] Every catalogue defect is filed against `EPIC-12`, repaired only in `docs/test-plan.md`, and
      excluded from `B(2)`; no production file appears in any of their `## Files` tables.
- [ ] The record states, in its own words, that it is one run on one machine at one commit and not
      a coverage claim (`ADR-0089` §2c).
- [ ] `python3 .github/scripts/lint_tickets.py` exits 0 with this story and its four tickets on the
      board.

## Out of scope

- **Repairing anything this round found.** The cycle ends at `PASS`; the four tickets are ordinary
  backlog work for whoever schedules them next, and `TASK-120601` is not this cycle's to run
  (§Termination rule 2).
- **Adding a catalogue case for the account-screen defect.** `ADR-0090` §1 makes authoring and
  running two commands; the next `/qa-cases` pass owns it. See above.
- **The second half of `TASK-120601`** — the signed-out browser. It needs `ADR-0050` §4 overturned,
  which is a `DEC` for the `architect`, and it is not ticketed until that is answered.
- **`TASK-120504`'s status.** Its unblocking condition is met and nothing here moves it; recorded
  above rather than changed, because a round story does not edit another round's tickets.
- **`web-client/src/result/account-offer.ts`.** Same root cause as `TASK-120601`, bounded by
  `pd.accountOfferSettled`, and named in that ticket's *Out of scope*. Not a second defect.
- **A showdown reveal.** Designed behaviour with a merged decision behind it (`ADR-0008`). A product
  request if it is wanted, never a defect.
- **`STORY-1205`'s status.** It is still `ready` with its own criteria unticked; closing a previous
  round's ledger is that round's business and the driver's, not this story's.
