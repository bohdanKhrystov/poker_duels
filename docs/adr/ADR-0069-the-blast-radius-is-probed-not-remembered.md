# ADR-0069 — The blast radius is probed, not remembered, and a ticket's size is its own Files table

- **Status:** Accepted
- **Date:** 2026-08-23
- **Resolves:** `DEC-064`
- **Amends** [`ADR-0068`](ADR-0068-an-atomic-ticket-names-the-gate-that-forbids-splitting-it.md)
  §3, whose twelve-file ceiling is deleted rather than raised, and §5, whose enumeration procedure
  could not be run at the time it was needed. §1 (a gate outranks a budget), §2 (`files_touched` is
  a fact) and §4 (what earns `atomic:`) stand exactly as written, and are the reason this ADR is
  short.
- **Constrains:** [`TASK-021301`](../../tasks/tasks/TASK-021301-the-wire-gains-a-rematch-and-the-version-takes-its-step.md),
  `STORY-0214`, `STORY-0405`, every future `atomic:` ticket, and every test that carries a protocol
  version

## Context

`ADR-0068` merged yesterday. Its §3 set a twelve-file ceiling on an `atomic:` ticket and called
twelve *"a tripwire, not a budget"*, recording as an accepted cost that *"twelve has zero headroom
for the ticket that motivated it… If implementation finds a thirteenth file, the ticket blocks and
comes back here."*

It found three, on the first ticket the rule governed.

**The tripwire worked, and it is the reason there is anything to decide.** `TASK-021301`'s coder
implemented all twelve declared files correctly — `compileKotlin`, `detekt`, `ktlint`,
`verifyProtocolTypes`, and the client's `tsc`, `eslint` and `prettier` all pass — then found three
more files the change forces, stopped rather than deciding, and cited §5. Nothing was invented
inside a ticket. That work is sound and is not redone.

The three, each verified on `develop`:

| # | File | Held by |
| --- | --- | --- |
| 13 | `poker-server/src/test/kotlin/duels/poker/server/e2e/SocketDuel.kt` | an exhaustive `when` over `ServerMessage` with no `else`, in **test** sources |
| 14 | `poker-server/src/test/kotlin/duels/poker/server/e2e/SocketSecrecyTest.kt` | the same shape, whose ignore-group ends `is ServerMessage.DuelFinished, -> Unit` |
| 15 | `web-client/src/protocol/connection.test.ts` | five `protocolVersion: 2` fixtures the client compares against `PROTOCOL_VERSION` at runtime, and one `protocolVersion: 3` |

### The number is the smaller half

`ADR-0068` §5 replaced `ADR-0047` §6's stale count with a procedure: *"run `./gradlew
:poker-server:check` and `npm run check` **on the tree being changed** — each failure names the next
file by path."* The procedure is correct. It was never run, and it could not have been: a planner
sizing a ticket has no tree being changed. So the twelve-file list §5 published, *"dated so a stale
list is recognisable as one"*, was produced by reading rather than by running — and reading stopped
at `poker-server/src/main/.../protocol` and `web-client/src/protocol`, which is where a protocol
change looks like it lives.

**A procedure whose only input is the finished work is a verification procedure. It cannot size
anything.** That is the defect, and it is general — nothing about it is specific to this bump.

### The set grows on its own, and always has

| Written | Said a bump carries | Wrong by |
| --- | --- | --- |
| `ADR-0044` §9 | (foresaw neither `frames.ts` nor `scripted-duel.gen.json`) | — |
| `ADR-0045` §4 | four | eleven |
| `ADR-0047` §6 | five | ten |
| `ADR-0068` §5 | twelve | three |
| today | fifteen | unknown, and that is the point |

Every step was taken by an ordinary ticket that added a gate — an exhaustive `when`, a `satisfies`
table, a byte-comparing verify task, a fixture — without knowing it was widening a protocol bump.
Nothing couples the two and nothing can: a gate is added where the behaviour is, and the bump is
somewhere else entirely. So **the set is monotone in the number of gates, and the number of gates is
not controlled by anyone.** Any number written down is a number that will be wrong, and `ADR-0068`
§5 said so in as many words — *"whatever replaces the five must not be another number"* — while its
own §3 wrote twelve into `.github/scripts/lint_tickets.py`. The two halves of one ADR contradicted
each other, and the contradiction is what stalled.

### The ceiling is not what stopped the ticket

Worth being precise about, because it changes what may safely be deleted. `TASK-021301`'s *Files*
section says: *"There is no headroom — a thirteenth file is a decision, not a bigger ticket, so stop
and raise one."* The coder stopped because file thirteen **was not in the ticket's *Files* table**.
That stop condition is true at any ceiling, including none. `MAX_FILES_TOUCHED_ATOMIC = 12`'s only
mechanical effect was to make the *correct* count unwritable in frontmatter — which is exactly the
defect `ADR-0068` §2 was written to end, reappearing one day later inside the fix for it.

### A second category, which no gate holds at all

Enumerating the version literals turned up more than the coder reported, and the extras are worse
than the ones that broke:

```
web-client/src/lobby/Lobby.test.tsx:330         protocolVersion: 2
web-client/src/store/duel-state.test.ts:72      protocolVersion: 2
web-client/src/store/duel-provider.test.tsx:10  protocolVersion: 2
web-client/src/store/duel-store.test.ts:8       protocolVersion: 2
web-client/src/protocol/frames.test.ts:13       "protocolVersion":2
```

None of these fail after a bump. `protocol.gen.ts` declares `Welcome.protocolVersion` as `number`,
not as the `ProtocolVersion` literal alias, so `tsc` has no grip on any of them; and the client
compares a version in exactly one place, `connection.ts:58`, which none of these five files reaches
(`grep -rl openConnection web-client/src` names none of them). They simply keep asserting a version
that no longer exists, forever, silently.

`connection.test.ts` is the same category caught by luck: its fixtures happen to flow through the
one comparison, so four of its tests fail loudly. And its *"refuses to trust a welcome at another
version"* case writes the wrong version as the absolute literal `3` — which the bump turns into the
**current** version, so the test that names the mismatch branch stops reaching it. The repository
already contains the idiom that survives, twice, and did not use it here:
`web-client/src/protocol/reconnecting.test.ts` writes `PROTOCOL_VERSION + 1`, and
`poker-server/.../protocol/HandshakeTest.kt` writes `PROTOCOL_VERSION - 1` and `+ 1`.

So a version literal in a fixture is one of three things, and only the first belongs in a bump: a
test whose **subject is the number** (`ProtocolJsonTest.theProtocolVersionIsTwo`); a test that needs
a version **different from** the current one; and a test that needs a `Welcome` at all and does not
care what number is in it. Two of the three need no literal, and today all three are written as
literals.

### The deadline

Unchanged from `ADR-0068` and still real: `ADR-0045` §3 allows one bumping branch open at a time, so
`STORY-0213`, `STORY-0214` and `STORY-0405` are strictly serial behind `TASK-021301` and none of
them moves. Separately, §4 below is free today and costs one more line for every fixture written
before it lands.

## Decision

### 1. The ceiling is deleted, and `files_touched` is checked against the ticket's own *Files* table

`MAX_FILES_TOUCHED_ATOMIC` is removed from `.github/scripts/lint_tickets.py`. There is no maximum
`files_touched` on a ticket declaring `atomic:`.

In its place the linter checks the thing that is actually knowable: on a ticket declaring `atomic:`,
**`files_touched` must equal the number of edit rows in that ticket's own `## Files` table**, and
must be at least four. An edit row is one whose *Action* cell reads `create`, `modify`,
`regenerate`, `delete` or `rename`; a `read` row is not counted; any other action is a lint failure,
so a typo cannot quietly undercount.

Nothing changes for a ticket without `atomic:` — the range is `1..3`, and the table is not counted.
That narrowing is deliberate and is what makes the check safe to land: `ADR-0068` measured ten
merged tickets whose declared count is smaller than their own table, nine of them `done`, and this
rule must not turn nine trail entries into a red CI job. Only a ticket written after `ADR-0068` can
carry `atomic:`, and there is exactly one.

This is the residual `ADR-0068` §7 left unbuilt — *"a linter that counts the *Files* table's edit
rows itself; that is measured as feasible"* — built now, because deleting the ceiling without it
would leave `files_touched` on an atomic ticket both unbounded and unchecked.

### 2. What stops an atomic ticket is a file its *Files* table does not name

At any count. A coder who needs a file the table does not name **stops and raises a `DEC`**, exactly
as happened here; it is never a bigger ticket, and the integer in the frontmatter is not the signal.
This was prose inside one ticket. It is now the rule, and it carries the whole of the growth
protection the ceiling was credited with — `ADR-0068` §4's *"a scope that grew after the ticket was
written"* earns nothing, unchanged.

### 3. A blast radius is **probed**, and the probe runs before the change exists

`ADR-0068` §5's *"on the tree being changed"* is replaced by: **run the gates against a probe.** A
probe is a throwaway one-line change that trips every gate the real change will trip, made only to
be read and reverted. It needs no design, no ADR and no understanding of the feature, so it runs at
planning time, and it names files by observing rather than by predicting.

For a `PROTOCOL_VERSION` bump the probe is two edits and four commands:

```
# (a) the version's own radius — every file that derives from the number
#     edit poker-server/src/main/kotlin/duels/poker/server/protocol/Protocol.kt: PROTOCOL_VERSION + 1
./gradlew :poker-server:check
cd web-client && npm run check

# (b) the wire shape's radius — every file that derives from the set of message types
#     add one throwaway variant to each sealed hierarchy the story touches
./gradlew :poker-server:compileTestKotlin
cd web-client && npm run typecheck

git checkout -- <the probe files>
```

Every path either command prints is a file a merged gate holds. That union, plus the files the
story's own content requires (the new types, the document rows, the ledger row), is the ticket's
*Files* table. **`:poker-server:check` compiles test sources and `npm run check` runs `vitest`** —
which is why the probe finds what §5's reading did not, and why (a) and (b) are separate: a version
bump alone does not break an exhaustive `when`, and a new variant alone does not break a version
fixture.

**No count appears in this ADR, in `docs/protocol.md`, or in any document, and none ever will
again.** `ADR-0068` §5's dated twelve-row table stands inside `ADR-0068` as the historical record it
is — an ADR records what was decided when it was decided — and is no longer a planning aid; a
planner who wants the list runs the probe. `docs/protocol.md` is a living document and its sentence
is corrected in place to state the probe and to name no files.

### 4. A version literal in a fixture references the constant, and exactly one test per side pins the number

Three rules, and they turn on the **subject** of the test:

1. A test whose subject **is** the version asserts a literal, because referencing the constant would
   make it a tautology. There is exactly **one** on each side and both already exist: Kotlin's
   `ProtocolJsonTest.theProtocolVersionIsTwo` (`assertEquals(2, PROTOCOL_VERSION)`), which a bump
   renames and re-values by design; and the client's `version.test.ts`, which reads the alias out of
   `protocol.gen.ts` and therefore needs no client-side literal at all. **Adding a second is a
   defect.**
2. A test that needs a version **different from** the current one writes it relative to the
   constant — `PROTOCOL_VERSION + 1`, or `- 1` — never an absolute number. The property under test
   is *"not the server's version"*, and an absolute literal stops expressing it the moment the
   server reaches that number, without failing.
3. Every other fixture carrying a version references `PROTOCOL_VERSION`. Its subject is a store, a
   render or a decoder; the number is scenery, and scenery written as a literal goes stale in
   silence.

This is not the *a golden string must stay literal* rule inverted. That rule protects a test whose
subject is the wire format from asserting the encoder against itself, and rule 1 **is** that rule,
kept and narrowed to the two places it belongs. A `Welcome` fed to a store reducer asserts nothing
about the wire, and interpolating the constant into it cannot make anything tautological, because
nothing downstream of it compares the two.

The effect is that §4 **shrinks** the blast radius rather than growing it. `connection.test.ts`
therefore leaves it permanently, and `TASK-021301` converts that file to the constant rather than
moving its literals — the same one file either way, and the last time it is in a bump.

### 5. The five silently-stale fixtures are one ticket, and not this one

`Lobby.test.tsx`, `duel-state.test.ts`, `duel-provider.test.tsx`, `duel-store.test.ts` and
`frames.test.ts` are **not** added to `TASK-021301`. No gate holds them, so their *why it cannot be
fewer* cell would have to say something false — which is the defect `ADR-0068` §4 forbids, and
`atomic:` would come to mean *"everything related"* on its second-ever use. They are one ordinary
ticket under §4, named on the board, and until it lands they carry a version that does not exist.

### 6. `TASK-021301` becomes fifteen files

`files_touched: 15`, `atomic:` gaining two lines (the Kotlin compiler over `ServerMessage` in test
sources; `vitest` over `connection.test.ts`), the three files added to its *Files* table with their
gates named, `status: ready`, and the twelve files already implemented at `c904503` unchanged.

Fifteen appears in that ticket and nowhere else. It is not a rule, a ceiling, or a fact about
protocol bumps; it is the size of one ticket, measured today.

### 7. What this does not decide

**Whether `Welcome.protocolVersion` should be generated as the `ProtocolVersion` literal alias
rather than `number`.** Argued below and declined for now, with the trigger written down: if a
fixture literal is ever found stale again *after* §4 has landed, the type is the cause and the
generator change is a ticket on that evidence.

**Whether `files_touched` is ever checked against the actual diff.** `ADR-0068` §7 stands unchanged.
§1 above builds the planning-time half of it and nothing at merge time.

## Consequences

**What it buys.**

- `TASK-021301` becomes startable again at its true size, and `STORY-0213`, `STORY-0214` and
  `STORY-0405` come off a second hard stop in two days.
- **No number governs an atomic ticket, so no number can go stale.** The sequence 4 → 5 → 12 → 15
  ends — not because fifteen is right, but because nothing asks the question any more.
- The linter checks `files_touched` for **truth** rather than for smallness, which is what
  `ADR-0068` §2 declared the field to mean and then left to review. The honesty rule stops depending
  on a reviewer noticing an integer.
- The next bump is sized by running two commands, by a planner, before any design exists — and the
  procedure finds test sources because the Kotlin compiler and `vitest` do not know or care which
  directory a file is in.
- §4 makes the blast radius **shrink** as fixtures are converted, and retires a whole class of test
  that passes while asserting something untrue.

**What it costs.**

- **The guard surface narrows from about fifteen incidental literals to two deliberate ones.** After
  §4, `ProtocolJsonTest` and `version.test.ts` are the *only* things in the repository that pin the
  version number. Today a wrong number would trip a dozen fixtures by accident; afterwards, if those
  two tests were ever deleted or weakened, nothing else would notice. This is the sharpest cost
  here, and it is a deliberate trade of accidental redundancy for fixtures that cannot lie.
- **The only mechanical brake on the size of an `atomic:` ticket is gone.** A forty-row *Files*
  table with `files_touched: 40` lints clean. What remains is `estimate` (`S` ≤ 120 changed lines),
  review, §2's stop condition and the *why it cannot be fewer* column — none of which a script can
  judge. `ADR-0068` kept a number precisely so that something mechanical existed, and this gives
  that up.
- **The probe costs two full `check` runs per bump, and it must be reverted.** A probe left in the
  tree is a wire shape with no ledger row, which `ProtocolVersionLedgerTest` catches — loudly, but
  only after someone has spent a build wondering why.
- **The probe is complete only for the gates that exist when it runs.** It observes rather than
  predicts, so a gate added tomorrow widens the set tomorrow and the probe finds it then. There is
  no list in this ADR for the same reason there is no number.
- **Five fixtures stay stale until a separate ticket lands.** After the bump, five client test files
  assert `protocolVersion: 2` against a server that speaks 3, and nothing fails. That is §5 choosing
  a visible defect over scope growth; it is a defect either way.
- **The rules move twice in two days.** `lint_tickets.py`, `tasks/README.md`, the `planner` agent,
  the `coder` agent and `plan-story/SKILL.md` learned `atomic:` yesterday and are edited again
  today; the trail now holds `ADR-0068` saying *twelve* and `ADR-0069` saying *no number*, one day
  apart, and a reader has to read both to know the rule. That is the price of leaving `ADR-0068`
  standing rather than rewriting it, and it is the cheaper of the two.
- **`TASK-021301` gets larger, and it was already the largest ticket in the repository.** Fifteen
  files in one cold `tier: sonnet` context is squarely what `ADR-0007` priced out.

**What it forecloses.** A number as an answer to *how big is a bump*, permanently: a future proposal
to write one down has to argue against a set that has been four, five, twelve and fifteen, each time
by accident. An absolute version literal in a fixture, including the *"obviously wrong version"*
idiom that produced `connection.test.ts`'s `3`. And `files_touched` as anything a planner can be
loose about on an atomic ticket, since the linter now reads the table underneath it.

**Why this shape when the evidence is thin.** It is the cheapest of the candidates to reverse.
Restoring the ceiling is one constant and one `if` branch; the probe is a documented procedure with
nothing built, so it reverses by deleting a paragraph; and §4 leaves behind ordinary references to
an exported constant, which stay correct under any rule that might replace it. The one thing that is
expensive to undo is the *deletion* of literals from fixtures — which is why §4 keeps rule 1 and
says out loud which two tests must never become derived.

## Alternatives considered

**Raise the ceiling from twelve to fifteen.** By far the strongest case, and the cheapest: one
integer in `lint_tickets.py`, `ADR-0068` survives intact, `TASK-021301` starts in five minutes, and
— unlike twelve — fifteen is *measured*, the output of a real implementation attempt against real
gates rather than of a reading. Nothing else in the workflow moves. Rejected because the measurement
does not generalise past the ticket it came from. The set is monotone in a gate count nobody
controls, it has been wrong four times in four attempts, and each step was taken by a ticket that
had no idea it was widening a bump; `STORY-0214` adds three message types and `STORY-0405` changes
`Hello`, so the next two bumps recompute this number rather than reuse it. Choosing a number that
can be shown in advance to be wrong, on the grounds that it is cheap, is choosing to hold this
decision again. `ADR-0068` §5 made this argument itself, and §3 wrote a number anyway.

**Keep a ceiling but have CI recompute it.** Appealing because it keeps a mechanical brake *and*
keeps it fresh: the linter would derive the cap from the current blast radius rather than from
memory. Rejected on mechanism. The blast radius is only observable by running the gates against a
change, so a linter that reads static files cannot compute it; it would have to run
`:poker-server:check` and `npm run check`, which is the first ticket gate that cannot run in an
agent worktree — `ADR-0068` §7's own stated objection — and it would make a markdown lint depend on
a Gradle build and an `npm ci`. A brake that turns a two-second lint into a ten-minute one is
disabled within a month.

**Drop `atomic:` entirely and let `estimate` do the bounding.** Genuinely the better metric, and it
deserves saying out loud: `estimate: S` ≤ 120 changed lines already bounds the thing that actually
costs context, and fifteen files of one line each is a smaller change than three files of forty.
Rejected because `files_touched` is not only a size — it is the coder's **contract**, the list of
files the ticket may open, and *"read only what the ticket names"* is the working agreement's second
rule. A line budget names no files, so a coder given one has nothing to stop at, and §2's stop
condition — the thing that produced this ADR instead of a silent fifteen-file diff — would have
nothing to be a condition on. It would also delete `ADR-0068` one day after it merged, on a case
nobody has measured.

**Widen §5's enumeration to say *"and grep the test sources too"*.** The minimal reading of
`DEC-064`: one sentence, no new mechanism, and it names exactly what was missed. Rejected because it
is another remembered list, one category longer. It would have found the two exhaustive `when`s and
the version literals, and would still have missed the next category — and there provably is a next
one: `ProtocolError` is an enum that an exhaustive `when` could cover tomorrow, and `frames.ts`'s
`satisfies Record<ServerMessage["type"], true>` has no sibling *today*. Both of those facts have a
date on them. Enumerating categories fails the same way as enumerating files, one level up; the
probe needs neither, because the compiler already knows.

**Add the five silently-stale fixtures to `TASK-021301`.** Its case is real: they are one-line
edits, they become wrong the instant the bump lands, and the person holding the context is the
person about to bump — three separate reasons to do it now rather than file a ticket nobody
collects. Rejected because no merged gate holds them, so the *why it cannot be fewer* column would
have to be filled with something false on the second-ever use of `atomic:`, and `ADR-0068` §4 names
that exact move — a scope that grew after the ticket was written — as the failure the whole
exemption was narrowed to exclude. A named ticket is the honest cost, and §5 says what stays wrong
until it lands.

**Generate `Welcome.protocolVersion` as the `ProtocolVersion` literal alias instead of `number`.**
The strongest technical case in this document, and it is `version.ts`'s own principle quoted back:
*"a stale version must fail the build, not the handshake"* — the alias exists precisely so that a
wrong number is TS2322 rather than a runtime surprise, and today it stops one line short of the
fixtures where the staleness actually lives. It would have made all six client literals fail loudly
at the bump. Rejected for now because it widens the wound it closes: every fixture literal becomes
gate-held, so the blast radius grows by five files permanently, at every bump, forever — and after
§4 there are no literals left for it to catch, because the fixtures reference the constant and the
constant is already typed against the alias. It also changes `ADR-0020`'s generator contract and the
`verifyProtocolTypes` byte comparison, which is a ticket of its own and not one this decision needs.
§7 records the trigger that would reopen it.

**Correct the ticket to fifteen and change no rule.** The smallest possible response, and it has one
strong argument: `ADR-0068` is one day old, nobody has yet worked under it, and rewriting a rule on
its first exercise is how a workflow becomes unreadable. Rejected because the ticket *cannot* be
corrected to fifteen — `MAX_FILES_TOUCHED_ATOMIC = 12` refuses the integer, so some rule changes
today no matter what is decided. Given that, the choice is between changing the number and changing
what the number is for, and only one of those two is still true next month.
