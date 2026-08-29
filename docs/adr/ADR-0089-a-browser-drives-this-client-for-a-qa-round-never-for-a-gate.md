# ADR-0089 — A browser drives this client for a QA round, never for a gate

- **Status:** Accepted
- **Date:** 2026-08-29

## Context

`ADR-0088` was merged on 2026-08-28. Its §1 carries the heading **"No browser drives this client,
here or in CI"**, and `EPIC-12` — opened the next day on the human's instruction — proposes a QA
cycle whose first step is an agent driving two headless Chrome profiles through a duel.
`DEC-082` asks whether that is inside §1's refusal or outside it.

**It is inside §1's words, and this ADR does not pretend otherwise.** "Here or in CI" says *here*.
A driver under `scripts/qa/` is a browser runner living in this repository whatever invokes it, and
reading the heading as though it said "in CI" would be deciding the question by rewriting it. So
the only legal route is the one `docs/adr/README.md` names and `ADR-0088` §5 prices: a superseding
record that amends the clause in the open. What follows is that record, and the whole of the
argument is whether the amendment is right — not whether it is needed.

**§1's reasoning was a ratio, and it is stated in the ADR rather than inferred.** §Alternatives 1
rejects the browser test *"on the ratio, not on the principle"*, and the ratio has three cost terms
and one yield term:

| §1's cost | Incurred by `EPIC-12`? |
| --- | --- |
| *"a permanent third CI job that is the union of the other two"* — JVM plus Docker plus Postgres plus Node plus a browser binary | **No.** `.github/workflows/build.yml` is untouched and still has two jobs, `check` and `client` |
| *"the flakiest class of test there is"*, on *"every pull request in a repository whose pull requests are mostly markdown"* | **No.** No pull request waits on it; no `verify:` block invokes it; no flake reaches anyone |
| a Playwright, Puppeteer, Selenium, WebDriver or Cypress dependency in `web-client/package.json` | **No.** The draft's `scripts/qa/drive.mjs` has no `import` and no `require` in 249 lines; it speaks the DevTools protocol over Node's built-in `WebSocket` and `fetch`, and no `package.json` is in its diff |
| yield *"confined to one composition file, one constructor call, a four-line proxy table and a static bundle"* | **Changed.** A 26-case catalogue across seating, secrecy, showdown, coins, reconnect and rematch is not a patch for four seams; it is the product's acceptance check |

**And §2 already has a browser driving this client.** `ADR-0088`'s own decision is eleven numbered
steps in which a person opens two browser profiles, creates a room, joins by link, plays to a
winner and presses Rematch. So the document does not refuse the *act*. What separates §2 from
§Alternatives 1 is not the browser — it is the **position**: a hand-check that produces a dated
receipt, against a gate that stands between a diff and `develop`. The question `DEC-082` really
asks is which of those two positions an agent-driven harness occupies.

**The force pulling the other way is §Alternatives 2, and the CI ratio does not touch it.** That
alternative refused an `npm run e2e` — the coverage without the CI bill, exactly this shape —
because *"an un-run test is worse than no test: it rots against the client it tests, goes red for
reasons nobody owns, and the first person to find it broken deletes it."* That argument reaches
this harness with nothing subtracted. `drive.mjs` finds controls by the text they start with;
`docs/test-plan.md` asserts `Create a duel room`, `Waiting for your rival`, `Blinds`, *No duel room
has that code*. Every one of those literals is a string the client is free to move, and none of
them is referenced by anything that would fail when it moves. A catalogue left alone for three
months returns a wall of red that is all rot and no defect.

**And this harness has a property `npm run e2e` never had: it files tickets and repairs.** The
cycle's step 4 runs `build-epic` over what `qa-manager` filed, and its convergence rule stops the
loop when `B(N) >= B(N-1)`. Neither `EPIC-12` §Termination nor either agent definition distinguishes
a defect in the product from a defect in the catalogue. Left that way, a stale string can cause
production code to change to satisfy it, and a rotten catalogue reads as a product getting worse.
That is a sharper risk than the one §Alternatives 2 described, and it is the reason this ADR
carries §4 rather than a bare *yes*.

**What the 2026-08-29 run established, stated no higher than it is.** The draft was exercised
against the running stack: two fresh Chrome profiles produced two distinct `pd.deviceId`s, a room
was created and joined by link, a duel was played to a winner with `+1`/`−1` unclamped in Postgres,
and Rematch started a fresh duel in the same room. Three of the four things `ADR-0088`
§Consequences named as failing green were **executed for the first time** — the root render, a real
socket from a real browser to the duel server, and two storage partitions — and the fourth,
`dist/`, was not, because every case runs against `npm run dev`. But the run found **no defect**.
So it is evidence of *executability*, not of *yield*: it proves the harness can reach the three
uncovered seams, not that it catches anything sitting in them. §5's *"the evidence that a browser
runner would catch anything is thin"* is still true. What changed is the other side of the ratio.

**There is no deadline in either direction**, and nothing about this is cheaper today than in six
months. What forces the decision now is only that `EPIC-12` is blocked entirely behind it and a
branch exists whose behaviour contradicts a merged ADR that nothing struck. That is a reason to
decide, not a reason to decide a particular way.

## Decision

### 1. `ADR-0088` §1's heading is amended; its body stands byte-unchanged

The heading **"No browser drives this client, here or in CI"** is narrowed to: **no browser drives
this client in CI, and no browser stands between a pull request and `develop`.** That is the only
clause of `ADR-0088` this ADR touches.

Everything else in §1 stands exactly as written and is restated here as still binding:

- **No Playwright, Puppeteer, Selenium, WebDriver or Cypress dependency enters
  `web-client/package.json`.** Byte-unchanged.
- **`.github/workflows/build.yml` keeps its two jobs.** Byte-unchanged.
- **`EPIC-03` ships no automated two-browser end-to-end test and no fifteenth story**, and
  `DEC-024`'s answer stands. This ADR does not reopen a closed epic.
- **The automated ceiling stays where `STORY-0312` and `poker-server/.../e2e/` put it**, and
  `ADR-0032` §4's *"still jsdom, still no network"* **stands as written** — it is a statement about
  the client's *test suites*, and the harness is in none of them: not a Vitest file, not a Gradle
  task, not a `package.json` script, not run by `npm test`.

`ADR-0088` §§2, 3, 4 and 5 are untouched. In particular **§2's hand-check remains the proof of
record**, §3's receipt is still the one line a merge cannot write, and a QA round does not
substitute for either.

### 2. Three conditions, and the permission is exactly their conjunction

A browser-driving harness may live in this repository **while all three hold**. They are the terms
that made §1's cost zero, and they are written as conditions rather than preferences so that the
next reader can check them mechanically:

- **a. No dependency.** Nothing enters any module's dependency set to drive a browser. The driver
  uses only the Node runtime's built-ins; the browser is a machine-local binary this repository
  does not vendor, install, pin or ship.
- **b. No gate.** `build.yml` keeps its two jobs. No pull request, `verify:` block or ticket waits
  on a QA case, and a cycle is started by a **human's command** — not a merge, not a cron, not
  another skill invoking it as a step.
- **c. No coverage claim.** The harness's product is a **dated round record**. Neither it nor
  `docs/test-plan.md` may be cited as coverage in an epic's `Metrics`, a Definition of done, or a
  ticket's `verify:`. A `PASS` is a statement about one run on one machine at one commit.

If any of the three stops holding, this ADR stops licensing the harness and the question returns as
a new `DEC-NNN`. Putting `scripts/qa` behind a CI job is condition **b** failing, not a refinement
of it.

### 3. The harness acts with a player's hands and reads with anything

The driver may **click, type, navigate, reload, and clear browser storage** — what a player's hands
reach. It may **read** anything: the DOM, `localStorage`, the database, the server's log.

It may **not write application state**: no dispatch into the client store, no synthesised socket
frame, no seeded row to reach a screen the product would not otherwise have shown. A case that
reaches its precondition by writing state is a client asserting a game fact, which `ADR-0002`
forbids of a client, and the case would then prove a fiction. `drive.mjs`'s `eval` verb is a
**read** escape hatch and is used as one.

The single licensed storage write is **`forget-room`**, which deletes `pd.roomCode` because
`ADR-0072` otherwise returns a profile to its old room and every later case fails for the wrong
reason. It only *forgets*; it may never be used to make the client believe something. Note that it
is not sufficient isolation on its own — `ADR-0018` re-seats a returning device by `pd.deviceId`,
so a **fresh Chrome profile per round** is mandatory and `forget-room` is only the intra-round
convenience.

### 4. A failure that does not reproduce by hand is a harness defect, and never enters `B(N)`

Before `qa-manager` may file a `blocker` or `high` bug ticket, the failing case must be
**reproducible by hand**: by the corresponding step of `ADR-0088` §2 where one exists, or by a
stated sequence of player actions where it does not.

- **Reproduces** → a product defect. It is filed, triaged and repaired as `EPIC-12` §Termination
  already describes, and it counts toward `B(N)`.
- **Does not reproduce** → a **harness defect**. It is filed against `EPIC-12` itself, repaired in
  `scripts/qa/` or `docs/test-plan.md`, and **excluded from `B(N)`**. No production code may be
  changed to make such a case pass.

The exclusion is the load-bearing half. `EPIC-12`'s convergence rule stops the cycle when
`B(N) >= B(N-1)`; if rot counted, a catalogue that had gone stale would read as a product getting
worse and the run would end `STOP_DIVERGING` on a healthy product — or worse, step 4 would merge a
diff to satisfy a moved string. Excluding harness defects is what keeps `B(N)` a measurement of the
product rather than of the catalogue.

This is the answer to `ADR-0088` §Alternatives 2. It does not prevent rot; it prevents rot from
being mistaken for a defect, and it makes the labour of distinguishing them explicit.

### 5. A case that asserts a player-facing string names the module that owns it

Every row of `docs/test-plan.md` whose `expect` or `do` quotes player-facing text cites the module
holding that literal — the shape `web-client/src/account/recovery-text.ts` and the
one-module-owns-each-key gate already established. Not a gate: a reference, so that whoever changes
the words finds by grep that a case depends on them. It is the cheapest thing that turns silent rot
into a findable one.

### 6. Reversing this is one `git rm` and a superseding ADR

Nothing imports `scripts/qa/`, no build file names it, no CI job invokes it, no module depends on
it, and no test asserts its output. Reversal is deleting `scripts/qa/`,
`.claude/skills/qa-cycle/`, `.claude/agents/qa.md`, `.claude/agents/qa-manager.md` and
`docs/test-plan.md`, plus the ADR that says why — and the repository is byte-identical to
`ADR-0088`'s world.

**That is why this direction is the one to try, and it is the same reason `ADR-0088` gave for
choosing the other.** With the yield still unevidenced, the cheapest option to undo wins; a
harness with no gate is cheaper to undo than a CI job, because a CI job becomes load-bearing the
moment people trust it and this cannot be trusted by construction (§2c).

## Consequences

**A machine-local dependency surface enters the repository, unpinned and unchecked.**
`scripts/qa/stack.sh` hard-codes `/Applications/Google Chrome.app/Contents/MacOS/Google Chrome`,
the container name `poker_duels-postgres-1`, and the ports `5173` and `8080`. §2a holds literally —
no `package.json` gains a line — and the *substance* of a dependency is still real: a browser
binary at a version nobody records, on one operating system. The first person on Linux gets
`Chrome not found`. This is accepted rather than solved because nothing gates on it: an un-runnable
harness costs a QA round, not a merge, and `STOP_INFRA` is already a named successful exit.

**The catalogue will rot, and §4 converts rot from a false defect into manual labour.** Every case
that fails must be reproduced by hand before it may be filed, which is the same work `ADR-0088` §2
imposed — now paid per failing case instead of per release. A round against a stale catalogue is
*slower* than the hand-check it was meant to relieve, and there is no mechanism here that stops the
catalogue drifting, only one that stops the drift being scored as a product defect.

**§4 is a rule an agent follows, not an exit code.** Every other gate in this repository is
`verify:` and a process exit status. This one is prose in an ADR read by `qa-manager`, and it
stands between a stale assertion and a merged diff. The mitigation is that a bug ticket still
passes through the ordinary review gate before it merges, so the failure mode is a wasted round and
a bad ticket rather than a silent change — but it is weaker than everything around it, and saying
so is the point of this paragraph.

**`ADR-0088` §Consequences' precedent is weakened, one day after it was set.** That ADR wrote:
*"It sets a precedent beyond this epic… a later epic wanting a browser runner argues against a
precedent rather than into a vacuum."* `EPIC-12` is that later epic and it wins on its first
attempt. A reader is entitled to take from this that a merged refusal here lasts as long as it
takes to build the thing it refused. The defence is on the record above — every cost term §1's
reasoning rested on is preserved as a condition in §2, and §2 of that ADR already has a browser
driving this client — and a reader who finds that thin is entitled to that view too. This is the
real price of the decision and it is not hidden in the alternatives.

**`dist/` is still unproven.** Gap 3 survives this ADR exactly as it survived `ADR-0088`. Every
case runs against `npm run dev`, so a `PASS` says nothing about the artifact a player would load,
and `docs/test-plan.md` §*What this catalogue does not cover* is where that stays written.

**The claim `ADR-0088` made about TCP is now precise rather than false.** *"No test in this
repository has ever opened a TCP connection to the duel server"* remains **true** — no test does,
and §1 above keeps it that way. A QA round does, through a real browser, and it is not a test. The
distinction matters because it is exactly the distinction this whole ADR turns on.

**What it buys.** `ADR-0088` §Consequences' *"the regression window is a release, not a pull
request… however long a human leaves it"* becomes a scheduled act with an owner, five budgets and
named exit states. Three of the four gaps that had never been executed by anything can now be
executed on demand. `EPIC-12` unblocks, and `STORY-1201` can be split.

**What it forecloses.** Nothing structural — §6 is one `git rm`. But it forecloses the clean
version of this repository's position: it can no longer say it refuses browser automation. It
refuses browser automation **as a gate**, which is narrower, more arguable, and will have to be
defended again the first time someone proposes a nightly. §2b is the sentence to point at when
that happens.

## Alternatives considered

**1. Refuse. Uphold §1 as written, and make `EPIC-12` a hand-check on a cadence.** The strongest
case, and it is strong. §1's words are unconditional, they were merged **one day** before this
question was asked, and a repository that reinterprets a merged decision the day after it lands —
in the direction of the very thing it refused — has a decision process that means nothing; the
second deliverable of this project is that process. Worse for the permitting side,
§Alternatives 2's argument survives every point made above: this *is* executable browser-driving
code that runs only when someone remembers, which is the object that alternative refused by name,
and it now has teeth that `npm run e2e` never had. `EPIC-12` would still have a shape: a scheduled
instruction to run §2's eleven steps, a receipt per run, and `qa-manager` triaging a human's
written findings into ordinary tickets — the whole termination apparatus intact, with no harness at
all and not one word of §1 disturbed. Rejected because it answers by the heading rather than by the
argument the heading summarised: §1's cost case is written down, it is three terms, and §2 of that
ADR **already has a person driving two browsers through this client**. Refusing here would mean the
sentence forbids a thing the same document mandates, differing only in whose hands are on the
mouse. And the cadence version is not free — it costs a human the better part of an hour per round,
which is why `ADR-0088` itself predicted *"the eighth person to run it skips step 9"*.

**2. Permit it, but not here — a sibling repository, or an untracked local directory.** Its case:
§1 stays literally intact, no ADR is amended, and the rot lives outside the trail Product B is
documenting. Rejected on two grounds. First, the harness is inert when detached: its cases cite
ADRs by number, `qa-manager` reads `tasks/epics/` and writes tickets under `tasks/`, and
`build-epic` repairs them — every one of those is a path in *this* repository, and a harness that
cannot file the ticket it exists to file is not the thing being asked for. Second and worse, an
untracked directory is invisible to the process this project documents: the trail would record that
the product was hand-checked while a script on one workstation actually did it. That is precisely
the dishonest green `ADR-0088` §Alternatives 3 refused, relocated.

**3. Permit it and schedule it — a nightly or label-triggered CI job.** Its case is the one thing
§4 cannot do: rot is found the night it happens rather than at the next round, and a nightly is not
*"when someone remembers"*, which removes §Alternatives 2's objection at the root rather than
managing it. Rejected because it re-incurs, exactly, the cost §1 priced and refused — a job that is
the union of the other two, plus a browser binary in the runner image — and because a red nightly
that nobody owns is a broken window that teaches people to ignore the board. If several rounds show
the harness earns its keep, this is the natural next decision and it deserves its own ADR with
those rounds as evidence, rather than being smuggled in with the first one. That is why §2b is
written as a condition on this permission and not as a preference.

**4. Say yes and add no clauses.** Its case: `EPIC-12`'s own *Out of scope* already forbids a CI
change and a `package.json` dependency, the draft already honours both, and an architect writing
five sections of rules for a thing that has run once is inventing governance ahead of evidence.
Rejected for two reasons. `EPIC-12`'s scope list binds `EPIC-12`; `DEC-082` asks about the
**repository**, and the next epic that wants to add a browser step to a `verify:` block will read
the ADR rather than an old epic's scope list — §2 exists to be that sentence. And the one clause
genuinely missing from the proposal is §4: nothing in §Termination separates a defect in the
product from a defect in the catalogue, while step 4 of the loop merges code for whatever was
filed. A cycle that can quietly change the product to satisfy a stale string is a worse failure
than the infinite loop §Termination was written against, and closing that is most of what this ADR
is worth.
