# ADR-0093 — "Ready for real users" is said of the shipped artifact, and the bar is two facts

- **Status:** Accepted
- **Date:** 2026-08-30
- **Resolves:** `DEC-086` — what, if anything, is the written bar for *"ready for real users"*?
  Registered open by [`ADR-0092`](ADR-0092-a-uat-round-files-what-a-source-settles-and-asks-the-rest.md)
  for the product owner. **Derived from the vision and the shipped ADRs; the human did not state
  this call.** The licensing sentence is the success condition, read at its second clause: *"Send
  a link. **She opens it in a browser.** We play a full heads-up match. Someone wins. We hit
  Rematch."* What she opens is the artifact the product serves her — a real user receives the
  product, never the repository — so a readiness claim is a claim about that artifact, and today
  no proof of record describes it. The second fact stands on merged ADRs rather than on a vision
  sentence: [`ADR-0031`](ADR-0031-an-optional-verified-recovery-email.md) §7 scopes a senderless
  build's validity to *"development and tests"*, and
  [`ADR-0087`](ADR-0087-forgot-your-password-is-a-door-on-the-sign-in-screen.md) §1's
  acknowledgement promises a link. The licence to accept the five remaining absences is
  [`docs/workflow.md`](../workflow.md#who-answers-a-dec)'s product column: *which risks inside
  the software are acceptable to ship with*
- **Registers:** `DEC-087` open for the architect — by what mechanism the proofs of record load
  the built bundle (§1a names it; the two open tables carry it)
- **Builds on:** [`ADR-0088`](ADR-0088-the-two-browser-proof-is-a-written-hand-check.md), whose
  gap 3 — *"the built bundle is never loaded… nothing opens `dist/index.html`"* — is the fact
  this decision turns on, and whose §2 hand-check is a proof of record here;
  [`ADR-0089`](ADR-0089-a-browser-drives-this-client-for-a-qa-round-never-for-a-gate.md) §2c and
  [`ADR-0092`](ADR-0092-a-uat-round-files-what-a-source-settles-and-asks-the-rest.md) §2, which
  forbid the one bar everyone would otherwise reach for and are load-bearing in §2 below;
  [`ADR-0063`](ADR-0063-nothing-gates-a-place-and-the-farm-is-accepted-until-the-ladder-is-public.md)
  §5, the precedent for an acceptance written against an event, and the ADR whose event this bar
  shares; [`ADR-0077`](ADR-0077-no-sender-is-an-implementation-and-detachment-is-a-decorator.md)
  (`NoRecoveryMailer` is what an unconfigured build binds); and `docs/test-plan.md` §*What this
  catalogue does not cover*, which names every absence §4 triages
- **Amends nothing.** `ADR-0088` §2's eleven steps stand as written — step 3's `npm run dev`
  is superseded only by the architect's answer to `DEC-087`, through that ADR's own §5 route.
  `ADR-0089` §2c and `ADR-0092` §2 stand byte-unchanged and this decision depends on them
- **Constrains:** the phrase — *ready for real users* may not be written in any epic's
  Definition of done, register, report or README below the bar; `EPIC-07`, when it is written
  (§1's two facts are properties its deployment must have); and nothing that runs today — no
  ticket, round, PR or cycle waits on any of it

## Context

The human asked for a UAT workflow whose stated goal is that afterwards the *"product should look
like ready for real users."* `ADR-0092` built the workflow and refused the phrase: its §2
extends `ADR-0089` §2c by name — **no round record, a `PASS` included, may be cited as the thing
that made the product ready** — and registered the phrase's definition here as `DEC-086`, with an
escape hatch stated in the register itself: *"there is no written bar; the human judges by
reading the round records" is a complete answer.* So the decision space is narrow before it is
entered: whatever the bar is, it cannot be a test result. It is merged, checkable facts, or it is
nothing.

**What made the escape hatch stop being complete.** The human clarified that *"the product"*
means the product from a user's chair — engine determinism, the `Rng`, hand evaluation and
repository facts are a different category with their own tests, correctly outside the browser
catalogue. Re-scored against user-reachable behaviour, the records the escape-hatch human would
read have a systematic property, not an incidental one: **every layer of proof this repository
produces — the unit suites, the jsdom suites, the JVM e2e package, `ADR-0088` §2's hand-check,
the QA rounds, and the UAT rounds about to start — loads `npm run dev` on `localhost`.** The
artifact a real user would receive is `dist/`, and `docs/test-plan.md` says of it what `ADR-0088`
gap 3 said first: *"`dist/` is loaded by nothing here."* CI runs `npm run build` and *"its exit
code is the whole of the assertion."* A human reading the round records with perfect care learns
about the dev server, because that is what the records describe. The escape hatch fails not on
judgment but on evidence: reading cannot recover a fact no record contains.

**The second systematic absence: recovery.** The client ships the doors — *Forgot your
password?* on `#/sign-in` (`ADR-0087`), the attach-address form, `#/verify`, `#/reset` — and the
acknowledgement copy promises movement: *"If that address is verified on an account here, a link
is on its way."* No mail transport is bound: an unconfigured build binds `NoRecoveryMailer`
(`ADR-0077`), the tokens exist only as `BYTEA` hashes (`V8__recovery_email.sql`), and no link
ever arrives — which is why the whole flow sits outside the catalogue and outside the
hand-check's reach. `ADR-0031` §7 called that state valid **"in development and tests"** — a
scoped clause, and an offering to real users is neither — and `ADR-0031`'s own Consequences
price what the absence does to a person: a player who forgets their password loses the account,
its coins and its ladder place, totally and permanently.

**Five more absences, of a visibly different class.** Duel history paging needs eleven finished
duels against a `DEFAULT_DUEL_LIMIT` of 10; leaderboard paging (`05-04`) needs a second page a
round cannot populate; `CORE-03`'s third device waits on `TASK-120504`'s third profile; the
hundred-way tie `ADR-0065` §1 was written for cannot be built by any round; and everything runs
on `localhost`, so latency, packet loss and a proxy are unexercised. Each is a **scenario a
round cannot build over a behaviour that is already decided in a merged ADR and proved at the
layer that computes it** — paging in the JVM suites, seat adoption and the join budget in
`ADR-0018`/`ADR-0022` and the server e2e package, tie semantics in `ADR-0064`. Whether these
belong in a readiness bar is the third thing this decision must say, so that a future round
knows whether an absence it cannot close is a blocker or a known and accepted absence.

**The deadline force.** UAT rounds are about to run, and each one files a dated record made
against the dev server. Nothing about the bar is cheaper to decide today than in six months —
but every green record that lands before this ADR is one a future reader may take as readiness
evidence, and the pile should grow under a merged statement of what it can and cannot support.
That is a reason to decide now, not a reason to decide a particular way.

## Decision

### 1. There is a written bar, and it is two facts — neither of them a test result

*Ready for real users* may be said of this product only when both of the following hold. Each is
a property of instruments and configuration, checkable by reading the repository and the offered
deployment's wiring — never by citing a run.

**a. The artifact under test is the artifact shipped.** The proofs of record — `ADR-0088` §2's
hand-check and the QA/UAT rounds whose records the human reads — load **the built bundle**: the
same bytes `npm run build` emits and a real user would be served. Not the dev server. Today this
fact is false by construction — nothing in this repository serves `dist/` to anything — and **the
mechanism that makes it satisfiable is the architect's, registered by this ADR as `DEC-087`**:
what serves the bundle (the Ktor server, a static server, a `scripts/qa/stack.sh` mode), on what
origin, and what supersedes `ADR-0088` §2 step 3's `npm run dev` so the hand-check describes the
same artifact — through `ADR-0088` §5's own route, since its §2 is immutable prose. Until
`DEC-087` is answered, the bar is unmeetable. **That is correct, not a defect**: a phrase with no
referent should be unavailable, and the alternative — declaring readiness of an artifact opened
by nothing, ever — is the exact claim this decision exists to refuse. The fact is about the
**artifact**, not the environment around it: TLS, a real domain and a proxy are deployment
properties, named out of the bar in §4.

**b. Recovery is completable by a real user in the deployment offered.** A mail transport is
bound — the offered build does not run `NoRecoveryMailer` — so that every door the client shows
can reach its end: the verification mail arrives, `#/verify` can be reached from it, *Forgot
your password?* produces a link, and `#/reset` sets a password. This is `ADR-0031` §7 applied,
not amended: it called a senderless build valid *"in development and tests"*, and an offering to
real users is neither. Two merged texts make the fact product-shaped rather than operational:
`ADR-0087`'s acknowledgement tells a player a link is on its way, and a deployment in which that
sentence can never be true of anyone is a product telling real users a falsehood at the exact
moment they are locked out; and `ADR-0031`'s Consequences price the failure as **total and
permanent** — the one loss in this product a player cannot recover from and the author cannot
repair forward. Which transport, at what cost, is `EPIC-07`'s and — where it means a bill — the
human's; the bar constrains the *state*, not the *vendor*.

**Nothing else is in the bar.** Not a coverage number, not a case count, not a card count, not
any round's verdict, and not the five absences §4 names out.

### 2. Meeting the bar does not make the product ready

The bar is a **precondition on the phrase, not a certificate of it**. Above the bar, readiness
is still not a fact anything can emit: it is the human's judgment, made by reading records that
— because of §1a — now describe the artifact a user receives. `ADR-0089` §2c and `ADR-0092` §2
stand untouched and do the work they were written for: no round, no `PASS`, no receipt and no
count may be cited as the bar being met or as readiness itself, in an epic's `Metrics`, a
Definition of done, a `verify:` block or anywhere else. The registered escape hatch is therefore
not rejected here — **its kernel is kept as the second half of the answer**: the human judges by
reading. What this decision adds is only the two facts without which the reading has the wrong
object.

Two edges, stated so nobody rebuilds them wrong:

- **The bar does not require that any proof has passed.** *"A hand-check receipt exists against
  the bundle"* would cite a record as the bar — §2c's laundering, one instrument over. The bar
  requires the instruments be *about* the right artifact; whether what they then show adds up to
  readiness is the reading, and the reading is the human's.
- **The bar gates the phrase, not the act.** Offering the product to anyone, at any time, in any
  state, is the human's own act with consequences outside the software, and no ADR schedules,
  licenses or forbids it. What this ADR governs is what this repository may *say*: below the
  bar, no register, report or document writes *ready for real users*, and any register that
  needs to describe that state writes what is true instead — which facts hold, which do not.

### 3. A real user is a stranger, and the bar's event is the one `ADR-0063` §5 already named

The bar falls due at the event the `DEC-086` row's own *Due* column names — *before anything is
offered to real users* — and that event is the one a merged ADR has already located:
`ADR-0063` §5's acceptance expires *"the first time the ladder is served on a public address"*,
against a population it defines as *"people the author invited personally."* **A real user is a
player beyond that population**: someone who reaches the product without the author inviting
them and standing behind the deployment. The founding moment — *send a link, she opens it* — is
inside the founding population, is v0.1's whole point, and **is not gated by this bar**: the
author at the keyboard, running the stack for someone they invited, is the hand-check with a
second human in it, and making the vision's first success condition harder to have would be
changing the vision, which this record has no authority to do.

One consequence of sharing the event is named rather than left to be noticed: `ADR-0063` §5's
farm acceptance — and `ADR-0012`'s clause it discharges — falls due at the same moment this bar
does. It is decided there, re-opened by that ADR's own triggers, and this bar neither collects
it nor re-decides it; this paragraph exists so a reader asking *what falls due when strangers
arrive* finds both clocks in one place.

### 4. Five absences are out of the bar, and the rule that keeps them out

**The rule.** A gap enters the bar only when its failure would (i) falsify the success-condition
sentence at first contact — the product a user loads simply not working — or (ii) destroy
something a player cannot get back. §1's two facts are the only known gaps of those kinds. A gap
whose failure arrives later, for fewer players, recoverably — a wrong page, an unreachable
scenario, a degraded connection — is a **known and accepted absence**: it stays exactly where it
is registered, a round that cannot reach it reports the absence without it bearing on the bar,
and its repair is ordinary ticketed work on no deadline this ADR sets.

| Absence | In the bar? | Why |
| --- | --- | --- |
| The built bundle | **In — §1a.** | The artifact is what a real user receives; no proof describes it; failure is total and at first contact |
| The recovery flow | **In — §1b.** | The one permanent, unrepairable loss in the product (`ADR-0031` Consequences); the copy promises what the build cannot do |
| Duel history paging past 10 | Out | Decided and proved at the query layer; a paging defect shows a signed-in player a wrong second page, loses nothing, and is fixed forward |
| Leaderboard paging (`05-04`) | Out | Same class; the ladder's stake is a monthly window with no audience yet, and a wrong page 2 misprints a list — recoverable, display-only |
| Third device (`CORE-03`) | Out | `ADR-0018`/`ADR-0022` decide it and the JVM e2e package proves it; `TASK-120504` already files the harness work; a browser round seeing it changes no fact about the product |
| Hundred-way tie (`ADR-0065` §1) | Out | Tie semantics are merged (`ADR-0064`) and computed server-side; the unreachable part is a data-scale fixture; failure is a misrendered row at a scale the product does not yet have |
| Real network | Out | The product's answer to a bad network is merged behaviour — grace, absence, adoption (`ADR-0013`, `ADR-0023`, `ADR-0028`) — and tested; how a *deployment* behaves under real latency is a property of `EPIC-07`'s deployment, examinable only when one exists, and an environment question rather than an artifact one (§1a's last sentence) |

**The reopening trigger, named now**: the day a real user loses something they cannot get back
through one of the five — not a misprint, a loss — that row was misclassified under rule (ii),
and the correction is a superseding ADR moving it in, not a quiet widening of §1.

### 5. No machine enforces the bar, and none may be built

The bar is prose in a merged record, checked by a human reading two facts. Deliberately: a
`verify:` block asserting *ready* is `ADR-0024` §3's opinion-in-a-verify-block one abstraction
up, and a CI job gating on §1b would put a mail transport's configuration on the merge path of a
repository whose deployments do not exist. The costs of prose-only enforcement are §Consequences'
first entry. Reversing any of this — a third fact, a removed fact, a different event — is one
superseding ADR; nothing mechanical consumes the bar, no ticket waits on it today, and the two
registered artefacts (`DEC-087`, the phrase's unavailability) are a table row and a sentence.

## Consequences

**The cost most likely to be underestimated: this ADR tells the human their UAT pass cannot end
where they aimed it.** The request's goal was that after the pass the product *"should look like
ready for real users"*; this record says no accumulation of green rounds against `npm run dev`
can reach that phrase, because the rounds describe an artifact and a flow no real user will
receive — the bundle unserved, recovery unfinishable. The distance between the pass and the
phrase is now written down before the rounds run, instead of discovered at offering time — that
is the point — but it reads as a demotion of the cycle the human just commissioned, and the
first qualified `PASS` will sit beside a merged sentence saying it is not, and cannot become,
the thing the human asked the pass to produce.

**The bar is unmeetable today and stays unmeetable until other people's work lands.** §1a waits
on `DEC-087` (the architect's) and §1b on a bound transport (`EPIC-07`'s, and a possible bill —
the human's). This ADR thereby adds one more pointer at an epic that is not written, a pattern
this repository has already named as its own failure mode. The mitigation is the pointer's
direction: the twenty existing pointers *defer work to* `EPIC-07`; this one *blocks a phrase on*
it, and a blocking pointer fails loudly — the phrase stays unavailable — rather than silently
collecting nothing.

**§1b removes a degree of freedom from a deployment that does not exist yet.** A strangers-facing
deployment without a mail transport is now below a merged bar: the human who wants one anyway —
to trial the product with accounts whose recovery is dead — must supersede this ADR in the open
rather than shrug, and satisfying §1b may mean paying for a transport earlier than `EPIC-07`
would otherwise have forced. Inside the founding population, §3 keeps everything free: a
senderless build offered to invited people is exactly `ADR-0031` §7's valid state.

**Two facts is a judgment, and a wrong one ships to real users.** §4's rule keeps five absences
out, and if one is misclassified, the discovery is a real user's loss — the trigger is named,
but naming a trigger is not preventing the event. The converse risk is also real: a future
reader treating the two-fact bar as exhaustive of *ready* — it is a floor, §2 says so, and floors
get read as definitions anyway.

**Prose-only enforcement means the phrase can be written below the bar by anyone who has not
read this ADR.** No grep guards *ready for real users* across two hundred documents, and none is
built — a phrase-hunting lint would false-positive on this very file and on every quotation of
the human's request. The defence is the same one every judgment rule in this structure leans on:
the review gate, and it is weaker than a gate.

**What it buys.** The phrase gets a referent: two facts a reader can check, an event with a
merged definition, and a named judge for the remainder. The round records about to accumulate
get a merged statement of their evidentiary limit before the pile exists. The five absences get
pre-written statuses a future round cites instead of re-litigating. The architect gets a crisp
question (`DEC-087`) instead of an ambient unease about `dist/`. And the register's escape
hatch is answered rather than dodged: the human still judges by reading — the bar only makes
the reading be about the product users get.

**What it forecloses.** A readiness certificate of any kind, from any instrument, ever, without
first superseding `ADR-0089` §2c and `ADR-0092` §2 — restated from those ADRs, not new, and now
load-bearing from a third direction. And saying *ready for real users* of the dev server, which
was available yesterday and is not now.

## Alternatives considered

**1. No written bar — the human judges by reading the round records.** The registered escape
hatch, and the strongest alternative here: it is pre-blessed as complete in the `DEC-086` row
itself; there is one human, one product and no users, so a bar has no second reader to
coordinate; §2c already prevents any record being laundered into a gate; and every bar risks
becoming the certificate §2c forbids, where *bar met* quietly reads as *ready*. Rejected because
the hatch assumes the records are evidence about the thing being judged, and today they are
systematically not: every record describes `npm run dev` on `localhost` and a recovery flow that
cannot finish, so the most careful reading reaches a judgment about an artifact no user will
receive. The failure is in the evidence, not the judge — and the fix is exactly two facts, after
which the hatch's kernel is this decision's §2, word for word: the human judges by reading.

**2. The bar is a UAT `PASS` at the offered commit.** The plain reading of the human's request —
the pass's stated goal *is* readiness — and the cheapest bar imaginable: one line in one record.
Rejected by standing law, not by preference: `ADR-0089` §2c and `ADR-0092` §2 forbid citing any
round as coverage or readiness by name, a `PASS` is *"a statement about one run on one machine
at one commit"*, and `ADR-0092`'s qualified verdict shows what the line can look like —
`PASS (conformance unjudged on 6 of 7 screens)` — which as a readiness bar would be laundering,
not measurement. This alternative is why `DEC-086` exists rather than being answered by the
cycle that raised it.

**3. A readiness checklist over all seven gaps.** Complete-looking, nothing surprises, and every
absence a future round hits has a pre-written status — the same virtue §4 claims, extended to
everything. Rejected because five of the seven are scenario-depth absences over behaviour
already proved at the layer that computes it; gating the phrase on them manufactures fixtures —
eleven finished duels, a second ladder page, a third profile, a hundred-way tie, a WAN rig —
whose readiness yield is thin; and a long bar reads as a certificate in proportion to its
length: the more boxes, the more *all boxes ticked* sounds like *ready*, which is the exact
misreading §2 exists to prevent. The five keep their registered homes and §4's rule says why.

**4. Add a third fact: every player-facing screen has a card the human has accepted.** The card
is this repository's only record of an accepted look (`ADR-0024` §3, `ADR-0091`), a new screen
already owes one, and readiness-for-users plausibly includes *the author has looked at every
screen* — a bar item would collect that debt at its natural event. Rejected on three grounds:
the debt already has a register and a collector (`ADR-0091` §5; `ADR-0092` §4 files a missing
card as `high` with the card's path as its dedupe key), so a bar item would be the same debt in
a second register — the split-brain `ADR-0092` §Consequences' *one debt, two filers, one key*
paragraph exists to prevent; *accepted* is not a checkable fact today — `ADR-0092` records that
no repository artefact says which cards the human has judged at the pane, so the item would
either gate on an unreadable fact or water down to *a card exists*, which the loop already
forces; and the failure a missing card ships is aesthetic and forward-fixable — §4's rule (ii)
keeps it out on the same test as the other five.

**5. Defer the decision to `EPIC-07`, when a deployment exists to measure.** Not empty: both of
§1's facts are properties of an offered deployment, and deciding beside the real one means
deciding with the transport, the origin and the serving mechanism in hand rather than imagined.
Rejected on `ADR-0088` §Alternatives 4's own test: nothing becomes decidable then that is not
decidable now — the two facts and the five exclusions are the same either way, and no new
evidence arrives in the interval. And the interval is not free here: UAT rounds start
accumulating records now, and a reader between now and `EPIC-07` would find a growing pile of
green and no merged sentence about what it supports. The deferral would also be this
repository's twenty-first pointer into an unwritten epic of the *collecting* kind — the kind
that fails silently — where deciding now makes the one pointer this ADR does add a blocking one.
