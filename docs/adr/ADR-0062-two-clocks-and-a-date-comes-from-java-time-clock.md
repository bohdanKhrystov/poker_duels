# ADR-0062 — The server has two clocks, and a date comes only from an injected `java.time.Clock`

- **Status:** Accepted
- **Date:** 2026-08-19
- **Resolves:** `DEC-062`, raised at `STORY-0501`'s split — which instrument does the server read
  **wall-clock** time from, when a piece of product behaviour is a function of the calendar? Three
  parts, all answered here: the instrument (§2), which documents are amended (§5 — three ADRs, not
  the one the decision was raised against), and what every later caller injects (§6)
- **Amends:** [`ADR-0061`](ADR-0061-a-season-is-a-calendar-month-and-the-coin-never-resets.md) §3,
  in **one clause of one sentence**: *"**Which season is it** is a function of
  `ServerClock.nowMillis()`"* names an instrument that cannot answer it, and becomes *a function of
  the instant an injected `java.time.Clock` reports*. Everything else in §3 stands untouched — a
  season is still derived and never stored, there is still no table, no column, no migration, no
  seed and no season in any wire type, and *which season was that duel in* is still a function of
  `finished_at`. Every other section of that ADR is unaffected: this decision changes **where the
  server reads the time**, not **what a season is**
- **Amends, on the same argument:**
  [`ADR-0027`](ADR-0027-the-session-outranks-the-device-id.md) §1 — *"an absolute 30 days from issue,
  computed from the injected `ServerClock`"* — and
  [`ADR-0031`](ADR-0031-an-optional-verified-recovery-email.md)'s two token lifetimes, *"24 hours,
  computed from the injected `ServerClock` at issue"* and *"one hour absolute, from `ServerClock` at
  issue"*. All three land in an `expires_at TIMESTAMPTZ` compared against SQL `now()`, so all three
  produce a row that expired in 1970. **The durations stay exactly as decided** — thirty days,
  twenty-four hours, one hour — and only the instrument that turns them into an instant changes.
  Both ADRs' **rate-limit windows keep `ServerClock` and are correct as written**: an in-memory
  rolling window is a duration, which is what that clock is for. None of this code is written yet
  (§6)
- **Builds on:** [`ADR-0025`](ADR-0025-one-ticker-coroutine-drives-both-sweeps.md), whose sweeps are
  the reason a monotonic clock exists at all and whose guarantee is preserved here in full;
  [`ADR-0013`](ADR-0013-disconnect-grace-period.md), whose grace window is the deadline that must
  not stretch when a host corrects its clock; [`ADR-0002`](ADR-0002-server-authoritative.md), which
  is why the calendar is read on the server and never in a browser
- **Constrains:** `TASK-050106`, which this **unblocks**; `STORY-0502`'s ladder route and every
  later `EPIC-05` read that needs *now* as a date; `STORY-0405`'s session rows and `STORY-0407`'s
  recovery tokens in `EPIC-04`, whose design notes are corrected in this change; and every future
  `poker-server` component that takes a clock of either kind
- **Creates three tickets and writes none of them** — see §7. One corrects a KDoc, one moves a
  default to the composition root, one turns §3's rule into a command. The planner files them
- **No wire change, no migration, no engine change.** `PROTOCOL_VERSION` does not move: nothing here
  is a socket fact. `poker-engine` takes no clock of any kind and this decision does not give it one
- **Raises no decision.** Nothing here is the product owner's: *what a season is* was settled by
  `ADR-0061` from the vision, and *which object reports the time* is not a product question under any
  reading

## Context

`poker-server` has had a clock since `EPIC-02`. `duels.poker.server.time.ServerClock` is a
`fun interface` with one method, `nowMillis(): Long`, implemented in production by `SystemClock` as
`System.nanoTime() / 1_000_000` and in tests by `MutableClock`, which only moves when a test moves
it. `RoomRegistry` reads it for room idleness and disconnect grace windows, and `ADR-0025`'s ticker
sweeps on it. It measures **elapsed time from an arbitrary epoch** and its KDoc has always said so:
*"Never use this clock to stamp a database row with a date."*

A second clock arrived later and quietly. `PostgresDuelResultSink` needs a wall-clock instant to
stamp `duel.started_at` and `finished_at`, so `TASK-021006`'s adapter takes a `java.time.Clock`
defaulting to `Clock.systemUTC()`, and its KDoc explains at length why that is not `ServerClock`.
Nothing announced that the server now had two clocks, because at the time only one class needed the
second one.

`ADR-0061` then made a piece of **product behaviour** a function of the calendar — a season is a
calendar month in UTC — and §3 named the instrument: *"Which season is it is a function of
`ServerClock.nowMillis()`."* `STORY-0501`'s design notes say the same, in the same words. Both are
merged. Both are wrong, and wrong in a way that compiles.

### The forces

1. **The named instrument cannot answer the question.** `System.nanoTime()`'s origin is arbitrary
   and unrelated to any epoch; the JDK guarantees only that differences between two readings are
   meaningful. A coder following `ADR-0061` §3 literally writes
   `seasonOf(Instant.ofEpochMilli(clock.nowMillis()))` and gets a season a few days or weeks after
   1 January 1970, varying with host uptime and JVM. Nothing rejects it: it type-checks, it runs,
   and on a freshly booted host it even looks stable.
2. **A merged precedent contradicts a merged ADR.** One of them has to give, and until this is
   settled the next author picks whichever document they read first. That is the whole reason
   `DEC-062` was registered rather than resolved inside `TASK-050106`.
3. **It is not one sentence, it is a class of sentence.** Looking for the phrase rather than the
   ticket finds it three more times in merged decisions: `ADR-0027` §1 gives a session *"an absolute
   30 days from issue, computed from the injected `ServerClock`"*, and `ADR-0031` gives its
   verification and reset tokens twenty-four hours and one hour *"from `ServerClock` at issue"* —
   while all three values land in an `expires_at TIMESTAMPTZ` compared against SQL `now()`. Every
   one of those rows would be born expired. Two live story documents repeat the instruction to the
   coders who will implement them (`STORY-0405`, `STORY-0502`). An answer that fixes only the season
   fixes one instance of a mistake this repository has now made four times.
4. **The monotonic clock exists for a reason that must survive.** A grace window measured on the
   wall clock stretches or collapses when the host corrects its time — an NTP step during a duel
   would extend or end a disconnected player's window arbitrarily. `ADR-0013`'s window and
   `ADR-0025`'s sweeps depend on a clock that only ever moves forward. "Use one clock for
   everything" is therefore not an available answer, in either direction.
5. **Names did the damage, and the file's own KDoc did not stop it.** `ServerClock` reads like
   *the* clock the server has; `SystemClock` reads like `System.currentTimeMillis()`. Neither is
   what it sounds like. The instruction in `ADR-0061` §3 was copied out of a design note by an
   author who never opened `ServerClock.kt` — where the prohibition has sat, in the KDoc, since the
   day it was written. Any answer that consists only of a better comment is answering a question
   nobody asked.
6. **Nothing shipped is wrong yet.** `RoomRegistry` uses the elapsed clock for durations, correctly.
   The sink uses the wall clock for row stamps, correctly. This is still a documentation defect, and
   `TASK-050106` is the ticket that would have turned it into a code defect. It is cheap today and
   expensive after a route, a screen and a ladder read have each picked their own answer.
7. **The blast radius only grows.** `TASK-050106` is the third caller. `STORY-0502`'s route is the
   fourth, and every `EPIC-05` read after it needs the same *now*. Whatever is chosen has to be the
   thing all of them take, decided once.

## Decision

### 1. Two kinds of time, two instruments, and neither answers the other's question

| The question | The instrument | In production | In a test |
| --- | --- | --- | --- |
| *How long since…*, *has this deadline passed?* | `duels.poker.server.time.ServerClock`, `nowMillis(): Long` | `SystemClock` — `System.nanoTime()` | `MutableClock`, advanced by the test |
| *What is the date?*, *which month is it?*, *what instant does this row carry?* | `java.time.Clock` | `Clock.systemUTC()` | `Clock.fixed(instant, ZoneOffset.UTC)` |

The two are **never converted into one another**: `Instant.ofEpochMilli(serverClock.nowMillis())` is
a defect wherever it appears.

A deadline is sorted by one question — **does it outlive the process?**

- **No** — a disconnect grace window, a room's idle timeout, a rate-limit window held in memory, a
  sweep period. It is counted on `ServerClock`, because the whole point is that it must not stretch
  or collapse when the host corrects its clock, and nothing outside this process ever reads it.
- **Yes** — `auth_session.expires_at`, a verification or reset token's expiry, anything written to a
  `TIMESTAMPTZ` column or compared against SQL `now()`. It is a **wall-clock instant**: elapsed
  milliseconds from an arbitrary epoch mean nothing to another process, to the database, or to this
  process after a restart. The *duration* is still a constant — thirty days, one hour — and it is
  added to `clock.instant()`, never to `clock.nowMillis()`.

A duration *shown to a player* is likewise the difference of two stored stamps: those are facts
about **when** something happened, recorded by the wall clock.

`ServerClock` is otherwise unchanged: same interface, same one method, same implementations, same
callers. It is not widened, wrapped, deprecated or moved.

### 2. The wall clock is `java.time.Clock` itself, and the server grows no third clock type

Every wall-clock reading in `poker-server` goes through `java.time.Clock`. There is no `WallClock`
port, no wrapper, no typealias and no new interface in `duels.poker.server.time`.

The reason is that the instrument already exists, merged and in use: `PostgresDuelResultSink` takes
one, defaults it to `Clock.systemUTC()`, and its tests fix it with `Clock.fixed`. A repo-owned port
would make **three** clock types in one module in order to fix a confusion caused by having two, and
every calendar function in `java.time` — `Instant.now(clock)`, `LocalDate.now(clock)`,
`YearMonth.now(clock)` — takes a `java.time.Clock` anyway, so the port would be unwrapped at each
call site or forced to re-expose one. See alternative 1 for what that costs us.

### 3. It is injected, it is never read statically, and the zone is written down

- **A wall clock is a parameter.** A class takes it as a constructor parameter; a function takes it
  as an argument. Nothing reads the time from a static.
- **`Instant.now()`, `System.currentTimeMillis()`, `LocalDate.now()`, `LocalDateTime.now()`,
  `ZonedDateTime.now()` and `YearMonth.now()` in their no-argument forms appear nowhere in
  `poker-server/src/main`.** The argument-taking forms are the whole point and are not affected:
  `Instant.now(clock)` is exactly right.
- **There is exactly one `Clock.systemUTC()` in the server, and it is the composition root's
  default** — `serverComponents(…, wallClock: Clock = Clock.systemUTC())`, alongside the
  `clock: ServerClock = SystemClock` parameter it already has. That is ticket (b) in §7; until it
  lands, `PostgresDuelResultSink`'s own default is the only other one and stays where it is.
- **A pure function that computes a calendar fact takes the clock without a default.**
  `public fun currentSeason(clock: Clock): Season` has no default value, because a defaulted pure
  function can be called with no clock at all — and that call compiles anywhere, in any module, with
  nothing in a diff to notice. A component wired at the composition root may keep a default, because
  the root is the one place a default is a decision rather than an accident.
- **The zone is `ZoneOffset.UTC`, written literally at the conversion.** A season boundary is UTC
  (`ADR-0061` §1), and a `java.time.Clock` carries a zone of its own that a test — or a
  `Clock.systemDefaultZone()` one autocomplete away from `Clock.systemUTC()` — can set to something
  else. The clock supplies the **instant** and nothing else: `seasonOf(clock.instant())`, with
  `TASK-050103`'s `seasonOf` doing the UTC conversion it already owns. `ZoneId.systemDefault()`
  appears nowhere.

### 4. `ServerClock` keeps its name, and stops being the general answer by construction

The name is a lie and it stays, for now, because removing it costs more than it saves — the argument
and the trigger that would reverse this are in alternative 3, which is where a future reader should
look before re-opening it. What changes instead:

- **This decision stops `ServerClock` accreting callers.** Every new caller that wants *now as a
  date* takes a `java.time.Clock` by §2, so the only code that ever names `ServerClock` again is
  code measuring a duration — one production class today.
- **Its KDoc is corrected** (ticket (a), §7). The last paragraph currently says *"use
  `System.currentTimeMillis` for wall-clock time and `java.time.Instant` for UTC dates"*, which is
  now wrong twice: it names a static read this decision forbids, and it predates the injected
  `java.time.Clock` that is the real answer. It becomes a pointer to this ADR, so the one file a
  confused reader opens says where to go instead of what not to do.
- **`docs/architecture.md` gains a short *Time in the server* section**, in this change, naming both
  instruments and linking here. That is the document an architect reads before writing an ADR about
  the server, and it is the one place `ADR-0061` §3's author would plausibly have looked.

### 5. Three ADRs are amended in one clause each, and none is superseded

`docs/adr/README.md` reserves *supersede* for a decision that is **reversed**, and records
amendments the way `ADR-0023` recorded its narrowing of `ADR-0013`: the amending ADR carries an
`**Amends:**` header, the amended ADR's **status line** carries the correction, and the index says
so in both rows. This change does exactly that, three times, and edits no other line of any of them,
because an ADR body is immutable here.

`ADR-0061`'s decision is intact. What a season is, what it does to a coin, what the ladder shows and
what a boundary does are untouched; retiring a 300-line product decision to correct the name of an
object would lose all of that to fix one noun. The corrected reading of §3's first sentence is:

> *Which season is it* is a function of the instant an injected `java.time.Clock` reports; *which
> season was that duel in* is a function of `finished_at`.

`ADR-0027` §1 and `ADR-0031` are corrected on the identical argument and nothing more. A session
still lasts **thirty days absolute with no sliding window**; a verification token still lasts
twenty-four hours and a reset token one hour, single-use. Those are policy and this decision does not
touch them. What changes is that the *instant* stamped into `auth_session.issued_at` and
`expires_at`, and into the token tables' `expires_at`, comes from an injected `java.time.Clock` —
because those columns are `TIMESTAMPTZ` compared against SQL `now()`, and a value derived from
`System.nanoTime()` puts every one of them in 1970, which means every session and every token is
expired the moment it is written. Both ADRs' rate-limit windows keep `ServerClock` and are right as
they stand.

Three ticket documents carried the same error in the same words and are corrected in this change
rather than amended — `STORY-0501`, `STORY-0405` and `STORY-0502` — because a ticket document is a
live instruction and not a record. `STORY-0505` also carries it and is deliberately left alone: it
is `dropped`, and editing a dropped story rewrites the trail rather than correcting an instruction
anybody will follow.

### 6. Every later caller injects the same thing, and the client still injects nothing

`STORY-0502`'s ladder route, `STORY-0405`'s session rows, `STORY-0407`'s recovery tokens, and any
future component that needs today's date all take a `java.time.Clock` under §3 — the same instrument,
injected the same way, defaulted in the same single place. A second answer anywhere is a defect, not
a local choice.

**None of that code exists yet**, which is why this is three status lines and three design notes
rather than a repair. `auth_session` is a merged table (`V4`) with no writer in `poker-server/src/main`, and
`STORY-0405` had not been split when this was written. The rule arrives before the first row does.

None of this reaches the browser. The season a player sees travels in the response (`ADR-0061` §6),
because a client deriving the season from its own clock is a client asserting a server fact
(`ADR-0002`) — the same failure as this one, one layer out.

### 7. The work this creates, named here and written by the planner

Three tickets. None of them is written in this change, and none of them is `TASK-050106`, which is
unblocked and whose three-file budget is unchanged:

- **(a) `ServerClock`'s KDoc names the wall clock.** Replace its final paragraph with the two-
  instrument rule of §1 and a pointer to this ADR; the file must no longer name
  `System.currentTimeMillis` as the answer to anything. One file,
  `poker-server/src/main/kotlin/duels/poker/server/time/ServerClock.kt`. Due before the next ticket
  that touches the room's timeouts, and cheap at any time.
- **(b) The composition root owns the one `Clock.systemUTC()`.** `serverComponents` gains
  `wallClock: Clock = Clock.systemUTC()` and passes it to `PostgresDuelResultSink`, whose own
  parameter loses its default so that no component can mint a clock of its own. Two files plus the
  sink's test. **Due before `STORY-0502`**, which is the first production caller that needs a wall
  clock it did not construct itself.
- **(c) A guard test that fails the build on an ambient time read, and on the conversion.** A test
  in `poker-server` that walks `src/main/kotlin` and fails on the no-argument forms listed in §3,
  plus `ZoneId.systemDefault()`, `Clock.systemDefaultZone()` and `System.nanoTime()` — **and on
  `Instant.ofEpochMilli(`**, which is the one expression that turns an elapsed reading into a date
  and appears nowhere in the module today. That last one is what catches a coder following an
  instruction this ADR did not reach. Exempt `ServerComponents.kt` and `time/ServerClock.kt` **by
  name** rather than by luck. Its model is
  `web-client/src/virtual-time.test.ts` (`TASK-031015`), which already does this for timers in the
  client and whose self-exemption discipline it should copy. One new test file; `STORY-0501` is its
  natural home, beside `TASK-050105`, which is the story's other refusal-turned-command.

## Consequences

**What it buys.**

- **`TASK-050106` can be written.** It takes `java.time.Clock`, with no default; its two fixtures are
  `Clock.fixed` at instants in months the suite does not run in, and the test that moves the clock
  across a year boundary moves one instrument rather than sleeping.
- **One answer, decided before there are four callers.** The ladder route, the screen's season label
  and anything after them take the same instrument from the same place, and the question does not get
  re-asked per ticket.
- **Three future 1970 bugs die with this one.** `EPIC-04`'s session rows and both recovery tokens
  would each have been born expired, in code nobody has written yet, from instructions that read as
  settled. Fixing the class rather than the instance cost two extra status lines in the same change.
- **The trail stops disagreeing with itself.** After this change no live document in the repository
  attributes a calendar fact to `ServerClock` — three ADRs carry their correction in their status
  lines, three stories' notes are fixed, and the four sibling tickets stop citing an open decision.
- **`ADR-0025`'s guarantee is untouched.** The sweeps still run on a clock that cannot jump backwards,
  because nothing about the elapsed clock changed. That was a hard constraint on any answer, and the
  answer costs it nothing.
- **Nothing new to learn in tests.** `Clock.fixed` is already how `PostgresDuelResultSinkTest` pins a
  timestamp, so the pattern in the season tests is the one already in the repository.

**What it costs.**

- **The misleading name survives, and the mistake stays expressible.** `ServerClock` still reads like
  the general answer to *what time is it*, and this decision does not make the error impossible — it
  documents around it, with a KDoc, an architecture section and a guard test that catches named
  spellings. **Whoever picks a type by its name alone can still pick wrong**, which is how this
  happened four times in three merged ADRs, and the honest description of §4 is that it lowers the
  odds rather than closing the hole: a wall-clock instant derived from `clock.nowMillis()` by any
  spelling the guard does not list still compiles, still runs, and is still 1970. Alternative 3 says
  what would close it and why that is not being done today.
- **Two clocks to wire and two to learn, permanently.** `serverComponents` grows a fifth parameter,
  and every future component that needs time must answer *which clock* before it can be constructed.
  A component that legitimately needs both — a deadline and a timestamp in the same class — takes two
  parameters and always will.
- **`java.time.Clock` is a wider surface than a one-method port would be.** It is a JDK abstract
  class, not a `fun interface`, so a test fake is not a lambda: a test that must **move** wall-clock
  time writes a small subclass with a settable instant (`TASK-050106` does exactly this, privately,
  in its own test file). `withZone`, `getZone` and `millis()` are all reachable, and
  `Clock.systemDefaultZone()` sits one autocomplete from `Clock.systemUTC()`. §3's UTC rule is a rule
  a person follows, not a property the type enforces.
- **Amending rather than superseding leaves the wrong sentence in three ADR bodies.** A reader who
  jumps straight to `ADR-0061` §3, or to `ADR-0027`'s session bullet, without reading the status line
  at the top of the file still reads `ServerClock`. That is the price of the repository's
  immutability convention, paid deliberately rather than by inventing a new one, and it is the reason
  each status line names the clause and gives the corrected reading rather than merely pointing here.
  Three files now open with a paragraph of correction, which is a real cost in reading weight on
  documents that were already long.
- **The guard test (c) will one day be wrong.** A scanner reading Kotlin with regexes catches
  spellings, not semantics: it will eventually fail on a legitimate use and someone will extend an
  allowlist under time pressure, which is how allowlists rot. It is worth having anyway, because the
  failure it prevents is silent and the failure it causes is loud.
- **A third clock is now harder to introduce for a real reason.** If something ever genuinely needs a
  monotonic clock with nanosecond resolution, or a multiplatform one, it argues against this ADR
  rather than merely adding a class.

**What it forecloses.**

- **A single `now()` on one type.** Nothing in `poker-server` will ever ask one object both *how long*
  and *what day*, so a future convenience wrapper over the pair needs a superseding decision rather
  than a helper.
- **`kotlinx-datetime` in the server**, without one too. The season logic is JVM-only by this
  decision, which matters the day anything about a season has to run outside the JVM — see
  alternative 5.
- **Wall-clock-driven in-process deadlines**, permanently. No grace window, room timeout or sweep
  period may be derived from `Instant` differences, so a future feature that wants *"expire this at
  midnight UTC"* has to express it as a wall-clock **comparison** made by a sweep the monotonic clock
  paces, not as a countdown — two clocks in one feature, which is more moving parts than it looks.

**The deadline, and why now rather than later.** Deciding this cost one ADR today because there is
exactly one wall-clock caller in the tree and one about to be written. After `STORY-0502` ships a
route, `STORY-0503` a screen and `STORY-0506` an end-to-end test, the same decision is a change to
every caller plus their fixtures — and the version of this bug that reaches production is a
leaderboard that is permanently empty because every duel is being attributed to a season in 1970.

## Alternatives considered

**1. A named wall-clock port beside `ServerClock` — `public fun interface WallClock { public fun now():
Instant }`, with `object SystemWallClock` and a lambda fake.** The strongest case in the set, and it
loses narrowly. It is symmetric with the clock the module already has, so the two instruments are
told apart by two repo-owned names rather than by one repo-owned name and one JDK class; it is a
single symbol to grep for when asking *who reads the calendar*; its KDoc would carry §1's whole rule
at the point of use; being a `fun interface` it fakes as `WallClock { fixedInstant }` and **moves**
as a `var` in a closure, which is strictly nicer than subclassing `java.time.Clock`; and it cannot be
handed a non-UTC zone, because it has no zone at all. Rejected because the server already merged the
other answer: `PostgresDuelResultSink` takes a `java.time.Clock` and says in its KDoc why. Adopting a
port means either three clock types in one module — the exact confusion `DEC-062` is about, plus one
— or a follow-up that rewrites merged, working, tested code to satisfy a naming preference. Every
`java.time` calendar function takes a `java.time.Clock` as well, so the port would be unwrapped at
each call site. The naming benefit is real and is bought instead, more cheaply, by §4's KDoc, the
architecture section and this ADR.

**2. Widen `ServerClock` with a second method — `nowInstant(): Instant` beside `nowMillis(): Long`.**
One clock to inject, one type to learn, one composition-root parameter, one KDoc stating both rules,
and every existing call site keeps compiling. Rejected because it puts two **incomparable
quantities** behind one type: a caller can subtract `nowMillis()` from `nowInstant().toEpochMilli()`
and get an interval since 1970 with no compiler complaint, which is a strictly worse version of the
bug being fixed. It also dissolves the guarantee that justifies the type — after the change, "this
clock never jumps backwards" is true of one method and false of the other, so `ADR-0025`'s sweeps
depend on a property the type no longer has — and `MutableClock` would have to keep two independent
notions of time in step to stay honest, which no test would remember to do.

**3. Rename it: `ServerClock` → `ElapsedClock`, `SystemClock` → `SystemElapsedClock`, `nowMillis()` →
`elapsedMillis()`.** The most direct fix to the actual cause, and the case for it is stronger than
one incident: **four** authors reached for this name for a calendar fact — `ADR-0061` §3,
`ADR-0027` §1, `ADR-0031` twice — and none of them had the file open, where the prohibition sits in
the KDoc. At that distance the name is the only thing visible, and *"an absolute 30 days from issue,
computed from the injected `ElapsedClock`"* is a sentence its own author would have flinched at. The
change is mechanical, the IDE does it, and it costs about twenty-five lines. Rejected **for now,
with a trigger**, for three reasons that are about landing it rather than about whether it is right.
The
type and method names occur in **eleven Kotlin files** and a rename must land atomically to compile,
which exceeds the enforced `files_touched: 1..3` cap on a ticket; splitting it behind a transitional
`typealias` puts two live names for one instrument into the tree across several merges, which is the
confusion being removed, temporarily doubled. **Seven merged ADRs and fourteen merged tickets** name
`ServerClock` or `nowMillis()` in prose this repository holds immutable, so afterwards the trail
names a type that does not exist — and a document naming a type that does not exist is its own kind
of defect. What makes deferring cheap is this decision itself: under §2, `ServerClock` stops
accreting callers, so the cost of renaming it does not grow with time.

**The trigger, stated so it can be checked rather than felt.** All four occurrences predate this
ADR, and all four came from documents that now say the opposite, next to an architecture section
that did not exist and a guard that did not exist. If it happens **once more — in any document
written after this one** — the documents are not the cause, the name is, and the rename becomes a
story on this argument without another ADR. The names are chosen above so that story has nothing
left to decide.

**4. Read the wall clock statically where it is needed — `Instant.now()`, as `ServerClock`'s KDoc
currently advises.** Nothing to inject, nothing to wire, no fifth parameter on the composition root,
and the value is always right in production. Rejected because a season boundary is then untestable
without waiting for one: `STORY-0501`'s whole point is that the season is asserted by **moving** a
clock rather than by sleeping, and `TASK-050106`'s fixtures are deliberately in months the suite does
not run in, which a static read fails on every day of every month. It is also the shape
`PostgresDuelResultSink` already rejected, for the same reason, in code that is merged.

**5. `kotlinx.datetime.Clock`.** Kotlin-idiomatic, `Clock.System.now()` reads well, and it is the
right answer the day season logic has to run outside the JVM — which is not an idle thought in a
repository whose engine is deliberately dependency-free. Rejected because `poker-server` is JVM-only
and persists `TIMESTAMPTZ` through JDBC, which speaks `java.time`: a second time library would be
converted at every boundary to buy nothing today. `poker-engine`, the one module with a multiplatform
reason to care, takes no clock at all and is not given one here.

**6. Leave `ADR-0061` §3 alone and note the correction only in the new ADR.** Cheapest possible
change, and defensible on the grounds that an ADR is a record of what was decided on a day rather
than a manual. Rejected because §3 is not a record of a decision that was later revised — it is an
instruction that was wrong when written, in a document `STORY-0501` and four tickets cite as their
specification. A reader who follows it produces a season in 1970. The repository's own convention
covers this exactly: the status line is where a correction goes, and `ADR-0013` has carried one since
`ADR-0023`.
