# ADR-0045 — Presence belongs to the table, not to reconnect, and EPIC-02 ships the server half

- **Status:** Accepted
- **Date:** 2026-08-16
- **Resolves:** `DEC-038`
- **Files, and does not amend,** [`ADR-0028`](ADR-0028-the-wire-names-an-absent-opponent.md): every
  type, field, emission point and rule in that ADR stands exactly as written. This one decides where
  the work lives, in what order it lands, and how it takes its wire version.
- **Constrains:** [`STORY-0214`](../../tasks/stories/STORY-0214-the-wire-names-an-absent-opponent.md)
  (the server half), [`STORY-0313`](../../tasks/stories/STORY-0313-the-table-names-an-absent-opponent.md)
  (the client half), the landing order of `STORY-0213` and `STORY-0405`, and the wire, via
  `PROTOCOL_VERSION`

## Context

`ADR-0028` is `Accepted` and answers `DEC-018` — the human's product call, made verbatim as *"away +
countdown + mark timeout folds"*. It specifies `SeatPresence`, `OpponentPresence`,
`ActedForAbsentSeat` and `Room.presenceOf` down to their `init` blocks, names five emission points,
and states the rule that keeps the countdown from becoming client-side authority.

**None of it exists.** `SeatPresence`, `OpponentPresence`, `ActedForAbsentSeat` and `presenceOf`
appear in no Kotlin file, in no row of `docs/protocol.md`, and nowhere in
`web-client/src/protocol/protocol.gen.ts`. `PROTOCOL_VERSION` is still 2. What makes this a decision
rather than a chore is that the promise is written in three registers and the work is in nobody's
backlog:

**The ADR's own header points at a story that was already done.** It constrains *"the follow-on
server work for `STORY-0208`"*, and `STORY-0208` is `done` — fifteen merged tickets, closed inside
the `EPIC-02` run that ended on 2026-08-14, the same day `ADR-0028` is dated. The follow-on arrived
after the story it follows on from had shipped, and no ticket was ever filed for it.

**`EPIC-03`'s register promises the rendering and cannot build the wire.** Its out-of-scope table
redirects *"what a player is shown while the opponent is away"* to `STORY-0310`, and its
answered-decisions table repeats it. `STORY-0310` has just been split into thirteen tickets that
render none of it, correctly: the epic's own rule is *"any change to the server, the protocol, or the
rules: nowhere in this epic"*. `docs/protocol.md`'s evolution note carries the promise a third time,
naming `ADR-0028`'s bump as a thing that will happen.

**Nothing has to be invented — only emitted.** `STORY-0208` shipped every fact presence projects:
`Room.gracePeriods`, `Room.absentSeats`, `Room.isPaused`, `foldAbsent`, and `DUEL_PAUSED`. They are
tested. That is what makes this a *placement* question: the design half was settled two days ago and
is not reopened here.

**Two homes are plausible and the wrong one is the tempting one.** `EPIC-03` is where the need is
felt, where the story that would render it lives, and where a branch is already open. `EPIC-02` is
where the code lives, and it is `in-progress` again — `ADR-0044` reopened it on 2026-08-16 for
`STORY-0213`, and that ADR's consequences already name this decision as having *"the same
argument"*, while deliberately declining to file it.

**And presence is not reconnect.** Four of `ADR-0028` §5's five emission points reach the player who
*stayed* — at the table, mid-duel, watching an opponent vanish, time out and come back. Exactly one
reaches the returning client. A client half filed under *"Reconnect — the client resumes its seat"*
would put four fifths of a table feature in the story about coming back.

**A fourth force, on the wire.** `ADR-0027`'s bump (`STORY-0405`, `EPIC-04`), `ADR-0028`'s and
`ADR-0044`'s (`STORY-0213`, `EPIC-02`) are all unlanded and each claims *"the next number free"*.
Three claims live in three epics' queues. `protocolJson` sets `ignoreUnknownKeys = false` and the
handshake compares versions for exact equality, so version equality is the only compatibility
mechanism the protocol has — and nothing in CI holds that lock.

### The deadline

Two, and neither argues for a particular answer.

`DEC-038` is due **before `EPIC-03` closes**, because that epic's definition of done includes screens
that its own registers say will show the pause state. Filing the work is what turns those rows from a
promise into a plan.

The second is `ADR-0027`'s and `ADR-0028`'s, unchanged: **no client is deployed.** A new
`ServerMessage` subtype is a breaking change here — an old client cannot decode a frame whose `type`
discriminator it has never heard of — so it is free today and a compatibility window later.

## Decision

**`ADR-0028`'s server half ships from `EPIC-02` as `STORY-0214`, takes its own `PROTOCOL_VERSION`
step, and its client half is a new `EPIC-03` story, `STORY-0313` — not a reopened `STORY-0310` and
not a reopened `STORY-0208`.**

### 1. `EPIC-02` ships the server half, as `STORY-0214`

`STORY-0214 — The wire names an absent opponent`, `parent: EPIC-02`, `module: poker-server`,
`depends_on: [STORY-0208]`. Its content is `ADR-0028` §§1–5 and §10 and nothing else; this ADR adds
no type, no field and no emission point.

`EPIC-02`'s scope line reads *"Sessions, the socket lifecycle, and the disconnect grace period of
`ADR-0013`"*. The epic shipped that grace period as far as a `Room` that knows exactly who is gone
and stopped before telling anyone. The unfinished half returns to the epic that promised it and to
the module that owns the code — the same argument `ADR-0044` made for the rematch, applied to the
case that ADR explicitly identified and declined to file.

`EPIC-02` is already `in-progress`, so this costs no second reopening. Like `STORY-0213`,
`STORY-0214` sits **outside** the epic's metrics ledger, which stays as measured at the first close
on 2026-08-14; the note under the stories table names both rather than quietly re-measuring.

### 2. `STORY-0208` is not reopened, and stays `done`

The presence work is a sibling story, never new tickets under `STORY-0208`. That story's fifteen
tickets and its acceptance criteria are the record `EPIC-02`'s first close was measured against;
adding to them rewrites a shipped ledger instead of extending it, and a `TASK-0208NN` dated
2026-08-16 would interleave new work into a closed run. `ADR-0044` made the same choice one level up
— a new story under a reopened epic rather than an edit to a settled one — and the trail is only
worth keeping if it is not rewritten after the fact.

### 3. A protocol bump lands alone, and `STORY-0213` goes first

`STORY-0213` and `STORY-0214` both edit the `ClientMessage`/`ServerMessage` hierarchies,
`PROTOCOL_VERSION`, `docs/protocol.md`'s version line and the generated `protocol.gen.ts`. They are
independent in feature and inseparable in file, so:

**At most one branch carrying a `PROTOCOL_VERSION` bump is open at a time**, across all epics.
`STORY-0213` is in front, because it is `ready` and its client half (`STORY-0309`) is the last
sentence of `docs/vision.md`'s success condition. `STORY-0214` follows it; `STORY-0405` takes its
turn whenever `EPIC-04` reaches it.

This is a constraint on the wire, not a scheduling preference. The failure it prevents is silent:
two branches that both move 2 → 3 **merge without a conflict** — a three-way merge sees the same
edit on both sides — and `ProtocolDocumentationTest` still passes, because it compares the document
to the constant and both moved together. The result is one version integer naming two different wire
shapes, which is precisely what `ADR-0028` §8 forbids, arrived at with every gate green.

### 4. The version number is read from `develop`, never claimed in a document

`ADR-0028` §8's rule stands and is not re-argued: one number names exactly one wire shape, so
presence takes **its own step**. What this ADR adds is how the number is taken:

- The bump is the **last ticket** of its story.
- That ticket's branch is rebased on `develop` immediately before it, and `PROTOCOL_VERSION` becomes
  whatever `develop` says **plus one**.
- `PROTOCOL_VERSION`, `docs/protocol.md`'s version line, the new message rows and a regenerated
  `web-client/src/protocol/protocol.gen.ts` move in that one commit.
  `ProtocolDocumentationTest` fails the build on a documented message that does not exist, so the
  document cannot move first.
- **No ADR, story or ticket names the integer.** A number written down in advance is stale the
  moment another bump lands, and a stale number is a lie the handshake cannot catch.

### 5. The client half is `STORY-0313`, under `EPIC-03`

`STORY-0313 — The table names an absent opponent`, `parent: EPIC-03`, `module: web-client`,
`depends_on: [STORY-0307, STORY-0310, STORY-0214]`, `status: blocked` until `STORY-0214` merges —
because `protocol.gen.ts` does not yet name the types it would render.

Its own story rather than a reopened `STORY-0310`, for two reasons of unequal weight. The lesser one
is timing: `STORY-0310` was split into thirteen tickets hours before this decision and
`TASK-031001` is startable, so reopening it rewrites a plan being executed. The real reason is §5 of
`ADR-0028`: presence is a **table** feature that reconnect observes once. The present player is told
three times, the returning player once, and the mark goes to both seats mid-hand.

**`EPIC-03` does not widen.** The pause state was already this epic's — its out-of-scope table
claimed it and gave it the wrong address. That row is corrected to name `STORY-0313`, and the scope
list gains a presence bullet exactly as `ADR-0044` gave it a rematch bullet. No capability enters the
epic that its registers did not already carry, and no ticket under it touches Kotlin.

### 6. What `STORY-0313` may render, and what it may not

Restated as the story's constraint because it is the failure mode most likely to be coded wrong, and
because it is now a live client rather than a hypothetical one:

- Three presence states, and one countdown started once from `graceRemainingMillis` against the
  client's own elapsed-time source — **never** a per-second frame, and **never** acted upon.
  `ADR-0028` §3 is the rule: reaching zero re-enables nothing, sends nothing, marks no hand lost and
  assumes no resumption. The duel is paused until an `OpponentPresence` says otherwise.
- `AWAY` with zero remaining is a legal frame and renders as waiting, not as an event.
- `ActedForAbsentSeat` labels the event at `(handNumber, actionSequence)` — the coordinates the
  client already holds — so the label does not depend on frame order.
- No `ClientMessage` gains a timestamp, a deadline or a remaining-millis field, in this story or any
  other.
- **The words are not settled**, here or in the story. `ADR-0028` reserved every word a player reads
  — *"away"*, *"waiting"*, *"timed out"* — to the human. `STORY-0313` records that as an open input
  and its rendering tickets are not written until it is answered.

### 7. What does not change

`poker-engine` gains nothing: no clock, no absence, no networking, no event, no schema bump.
`ADR-0028`'s design is neither amended nor re-litigated — if any part of it proves wrong when built,
that is a new ADR superseding it, not an edit here. `STORY-0310` keeps its thirteen tickets, its
acceptance criteria and its out-of-scope row; `TASK-031001` stays startable. `STORY-0208` stays
`done`. `STORY-0213` is untouched except that it is now explicitly first in the version queue.

### 8. What the tests must prove

`ADR-0028` §10 is the list for `STORY-0214`, unchanged. This ADR adds two, both about the seam it
creates rather than the behaviour:

- After `STORY-0214` lands, `web-client/src/protocol/protocol.gen.ts` names `SeatPresence`,
  `OpponentPresence` and `ActedForAbsentSeat`, and `:poker-server:verifyProtocolTypes` proves it
  byte-for-byte on every `check`. The gate that makes `STORY-0313` startable is executable, not a
  judgement call.
- `web-client/src/protocol/version.ts` moves with the wire in the same change, and the client's
  typecheck fails until it does — the mechanism `docs/protocol.md` already documents for exactly
  this pair of bumps.

## Consequences

**What it buys.** An accepted ADR stops being a promise nobody owns: three registers that today
point at a story rendering nothing point at a filed, dependency-ordered pair. The Kotlin is written
in the module that owns it and reviewed by whoever reviews `poker-server`, which is the rule that
produced this decision surviving contact with the case it was written for. The client half lands
where its four-out-of-five emission points actually arrive. And the version race that three unlanded
bumps had quietly created is named and given an order, before it corrupts a number rather than after.

**What it costs.**

- **`EPIC-03` can no longer finish on its own.** `STORY-0313` is `blocked` on a `poker-server` story
  in another epic's queue, so this epic's close date is now set by work no client agent can unblock —
  and `STORY-0214` sits behind `STORY-0213` in the version queue, one more hop away. The epic gains a
  thirteenth story whose start it does not control.
- **A second story outside `EPIC-02`'s metrics ledger.** `STORY-0213` was the first; the table now
  measures an epic as it closed on 2026-08-14 and covers two of the stories that will finally sit
  under it. Product B's per-epic numbers describe less of the epic with each addition, and *"done"*
  is now a state this epic has left and not yet returned to.
- **Three stories in three epics share one lock, and nothing in CI holds it.** `STORY-0213`,
  `STORY-0214` and `STORY-0405` must land one at a time. The guard is §3's rule plus a rebase; two
  branches both claiming 3 merge clean and green, and the defect surfaces only when a real client
  meets a real server. Enforcing it mechanically — a check comparing `PROTOCOL_VERSION` against
  `origin/develop` — is addable, is not built here, and would be its own decision.
- **The countdown becomes a live invitation to client-side authority.** `ADR-0028` named this cost
  when no client existed; `STORY-0313` is the ticket that makes it real, and every future ticket
  touching the table inherits the obligation. No type prevents it — only §6's rule and the test that
  pins it.
- **`STORY-0313` cannot be split into rendering tickets when it unblocks.** Its behaviour is
  specified and its copy is not, so the planner will hit the reserved product question the moment
  `STORY-0214` merges. Filing the story does not remove that stall; it moves it somewhere visible.
- **`STORY-0214`'s first ticket deletes a passing test.** `ADR-0028` retracted the wire half of
  `ADR-0023`'s indistinguishability property, and `TASK-020806` shipped a test asserting it, along
  with a `foldAbsent` KDoc saying the function *"constructs no frame of its own"*. Both are green,
  correct as written, and must become false in the same change. A reviewer will see a deliberate
  reversal that looks like a regression, which is exactly why it is written down here.

**What it forecloses.** Nothing structural. It does close off, deliberately: working the two
`EPIC-02` wire stories in parallel; `EPIC-02` closing before both land; and `STORY-0310` gaining
presence tickets later — by the time it could, it will be `done`, and the sibling is the cheaper
route.

**Why this shape when the evidence is thin.** Every part of this is cheap to reverse. A story is a
file and two rows: if `STORY-0313` turns out to be one ticket that belonged inside `STORY-0310`, the
merge back costs a rename. The expensive branch is the one not taken — Kotlin filed under a
`web-client` epic makes the board's module column false, routes a protocol change to a client
reviewer, and would have to be unpicked across a whole epic's tickets.

## Alternatives considered

**Reopen `STORY-0208` and add the presence tickets to it.** The strongest case of the six.
`ADR-0028`'s own header names *"the follow-on server work for `STORY-0208`"*; every fact presence
projects — `gracePeriods`, `absentSeats`, `foldAbsent` — is that story's; its acceptance criteria
would gain four rows and no new file, no new dependency edge and no new story number would exist. If
the trail did not matter, this would win. Rejected because the trail is Product B: `STORY-0208` is
`done` with fifteen merged tickets, its criteria are what `EPIC-02`'s first close was measured
against, and a `TASK-0208NN` dated two days later interleaves new work into a closed run. `ADR-0044`
took the sibling over the edit one level up; taking the edit here would make the two answers
inconsistent for no reason a reader could find.

**`EPIC-03` ships the Kotlin as a recorded exception.** Its case: one epic, one branch, one review,
no cross-epic dependency, and the client is the only consumer these frames will ever have —
splitting the work costs an extra story, an extra edge and a merge order to get wrong. Rejected for
`ADR-0044`'s reason exactly: `EPIC-03`'s out-of-scope rule is *why `DEC-038` exists*, its module
column says `web-client`, and a protocol change behind a client review has the wrong reviewer. Two
adjacent decisions answering this opposite ways would leave the rule meaning nothing.

**A new epic for the wire's unfinished edges** — presence, the rematch and `ADR-0027`'s session bump
in one place. Genuinely strong: it is true that three protocol bumps in three epics share one lock,
and an epic owning all three would make that serialization visible on the board instead of hiding it
in three queues; it would also give the metrics ledger a clean unit instead of two epics with holes.
Rejected because it invents a home where the code does not live, it would take `STORY-0213` back out
of `EPIC-02` two days after `ADR-0044` put it there, and an epic is a promise about a capability
rather than a bin for leftovers. What that epic was actually wanted for is the ordering, and §3 and
§4 supply it directly.

**Share one `PROTOCOL_VERSION` step with `STORY-0213`.** Its case is real and tempting: both are
unlanded, nothing is deployed, they will land within days of each other, and one bump instead of two
removes the entire class of merge race that §3 has to guard with discipline. Rejected because
`ADR-0028` §8 already refused exactly this against `ADR-0027`, and no premise has changed — one
number must name one wire shape, `VERSION_MISMATCH` is exact equality, and a shared "3" meaning
rematch on one branch and presence on another is a number the handshake cannot check. Sharing does
not remove the race; it removes the meaning of the thing the race would corrupt.

**The client half is a reopened `STORY-0310`.** Its case: reconnect is where the absence of presence
is most visible today, the story's own out-of-scope row names the gap, and a reader asking *"what
happened to the pause state?"* looks there first — `EPIC-03`'s registers already send them there.
Rejected on substance rather than on timing: four of `ADR-0028` §5's five emission points reach the
player who stayed, at the table, mid-duel, and only one reaches the returning client. Presence is a
table feature reconnect happens to observe once, and filing it under reconnect would misname it
permanently. The timing — thirteen tickets split hours earlier, `TASK-031001` startable — only makes
the same answer urgent.

**File nothing; let a later planner rediscover it.** Its case: nothing is blocked today,
`STORY-0310` ships reconnect either way, `EPIC-03` can close eleven other stories, and a story filed
long before it is startable rots into a stale design. Rejected because the promise is already written
in three registers — `EPIC-03`'s out-of-scope table, its answered-decisions table, and
`docs/protocol.md`'s evolution note — and an accepted ADR with no ticket behind it is the exact
failure `DEC-038` was registered to catch. Leaving it costs the same discovery again in a month, by
which time the free wire break may not be free.

**Supersede `ADR-0028` and ship no presence at all** — `DUEL_PAUSED` and a spinner. Its case is
honest: it is the largest of the four options the human was shown, it was chosen before a client
existed, and the countdown is the single most dangerous thing this epic will put in a browser.
Rejected because it is not an architect's to take. `DEC-018` was the human's product call, made
verbatim, and unbuilding it is a product reversal wearing a scheduling costume. If the pause state is
to be cut, that is the product owner's or the human's, and it is a new ADR superseding `ADR-0028` —
not a decision about where to file a ticket.
