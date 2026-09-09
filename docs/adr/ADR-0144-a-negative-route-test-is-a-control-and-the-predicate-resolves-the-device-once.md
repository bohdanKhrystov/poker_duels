# ADR-0144 — A negative route test is a control, not a prohibition, and the standing predicate resolves the device once

- **Status:** Accepted
- **Date:** 2026-09-09
- **Resolves:** `DEC-159` — **three merged sources are jointly unsatisfiable and one must yield: which?**
  Registered 2026-09-09 while `TASK-141003`'s coder implemented
  [`ADR-0135`](ADR-0135-the-server-says-which-sign-out-this-is-and-the-browser-forgets-one-key.md).
- **Supersedes**, by number and quoted sentence, and nothing else:
  - `ADR-0135` §4's **mechanism** — *"It gains one function:"* with
    `public suspend fun namesPlayer(deviceId: DeviceId, playerId: PlayerId): Boolean`, *"implemented
    as `players.findOrNull(deviceId)?.id == playerId`, reusing the shipped `revoked_at IS NULL`
    lookup."* That function cannot compute the table §5 draws, and §4 is corrected here rather than
    quietly implemented around.
  - `ADR-0135` §Consequences' bullet **"`IdentityResolver` grows a second question."** — *"It stays
    the only place a device id is turned into a player, which is the point, but a class whose whole
    KDoc is about precedence now also answers a comparison, and a careless later reader could
    mistake `namesPlayer` for a second resolution path."* The class grows no second question.
  - `TASK-040611`'s *Tests* row for `theDeviceRouteIsNotInstalledOnAnyOtherVerb` — *"`GET
    /api/me/device` answers `405` or `404` — assert `status != HttpStatusCode.OK` **and** `status !=
    HttpStatusCode.Unauthorized`. Only `DELETE` was asked for (`ADR-0049` §5), and a `get(...)` typed
    beside the `delete(...)` would otherwise be invisible"* — in its **choice of verb only**. The
    reason it gives is kept and is the whole argument of §2 below.
- **Supersedes nothing in `ADR-0049` and nothing in `ADR-0068`.** §1 shows `ADR-0049` §5 never held
  the position ascribed to it; §3 shows `ADR-0068` §4 already decides the sizing question, in the
  direction taken here.
- **`ADR-0135` §5's table, §4's prose predicate, §6's failure direction and §8's *not `atomic:`* all
  stand, unamended.** They are the constraints this ADR is written inside; §5's table is what §4's
  mechanism is corrected *to*.
- **Nothing crosses the socket, nothing is written, no schema moves.** `Hello`, `Welcome`,
  `ProtocolCodec`, `protocol.gen.ts`, `PROTOCOL_VERSION` and `ADR-0047`'s ledger are byte-unchanged,
  and this ADR mints no string.

## Context

`ADR-0135` merged on 2026-09-07 and requires a route: **`GET /api/me/device`**, session-required,
answering one boolean. A test merged on **2026-08-24** — `TASK-040611`, which wired `DELETE
/api/me/device` into the composition root — asserts that the same route is **unwired**:

```kotlin
@Test
fun theDeviceRouteIsNotInstalledOnAnyOtherVerb(): Unit = testApplication {
    application { duelServer(serverComponents(config, dataSource)) }
    val response = client.get("/api/me/device")
    // Only DELETE was asked for (ADR-0049 §5); assert not OK and not Unauthorized
    assert(response.status != HttpStatusCode.OK)
    assert(response.status != HttpStatusCode.Unauthorized)
}
```

`TASK-141003` implemented correctly makes `GET /api/me/device` answer `401` to that request, which is
exactly the status the second line rejects. The ticket's own `verify:` block runs `./gradlew check
-PrequireDocker=true`, so the ticket cannot pass its own gates.

The obvious repair — edit the test inside `TASK-141003` — takes the ticket to a **fourth** file, and
`.github/scripts/lint_tickets.py` sets `MAX_FILES_TOUCHED = 3` beside `MIN_FILES_TOUCHED_ATOMIC = 4`,
so a fourth file **requires** `atomic:`. `ADR-0135` §8 forbids that in as many words: *"The
implementing ticket is a plain ticket under the `files_touched: 1..3` cap, split as the planner sees
fit; it is **not** `atomic:`, and a ticket that claims to be would be claiming a gate that does not
apply to it."*

**The second force, found by the same coder in the same hour.** `ADR-0135` §4 specifies the answer's
mechanism as one new resolver function returning one boolean. §5 draws the table that function is
supposed to reproduce:

| The caller | The presented `X-Device-Id` resolves, live, to | `signOutHandsANewProfile` |
| --- | --- | --- |
| a session | **the caller** | `true` |
| a session | **another player** | `false` |
| a session | nothing, because none was presented | `true` |
| a session | nothing, because the binding is revoked | `true` |

`namesPlayer(deviceId, playerId)` is `players.findOrNull(deviceId)?.id == playerId`. It answers
`true` on row one and `false` on rows two, three and four. So `namesPlayer` inverts rows three and
four, `!namesPlayer` — the reading `TASK-141003`'s *Scope* took — inverts rows one and two, and **no
sign repairs it**: rows two and four must differ, and a single boolean over *does this device name
this player* collapses *resolves to somebody else* and *resolves to nobody* into the same `false`.
The distinction §5 requires is structurally unavailable to the function §4 names.

**What is actually in tension.** Not three ADRs. `ADR-0049` §5 specifies `DELETE /api/me/device` and
never mentions another verb on that path — the only occurrence of the word *verb* in it is *"The
`auth` family is verbs about a session — sign-up, sign-in, sign-out — and the point of this decision
is that revocation is not one"*, a sentence about which family the route belongs to. The prohibition
lives in a **test comment**, which turned *"only DELETE was asked for"* — true, and a statement about
what an ADR had asked for in 2026-08 — into *"only DELETE may ever be installed"*, which no document
decided. The real tension is between an ADR that asks for a route and a **negative assertion whose
authority was misattributed**; and, underneath it, between a repository that is right to make
negative surface-area assertions and a three-file cap that makes every such assertion expensive to
move.

That second half is the part worth deciding carefully. A test that says *this route does not exist*
is load-bearing documentation about the surface of the server. Deleting it because it became
inconvenient would buy the route and cost the property; keeping it would refuse a merged ADR.

### The deadline

`TASK-141003`'s implementation is written and waiting. Every hour it waits, the eleven client tickets
behind it wait, and `STORY-1408`'s serialisation window against `STORY-1410` stays open. The `§4`
correction is also cheapest now: `namesPlayer` merged **today**, in `TASK-141002` and as
*"Dead code for exactly one merge"* in its own board row, and has never had a caller. It can be
removed at the cost of one function and four tests, before anything has been built on it. After
`TASK-141003` merges around it, removing it costs a caller as well.

## Decision

### 1. `ADR-0049` does not yield, because it never held the position

`ADR-0049` §5 is a specification of `DELETE /api/me/device`: its identity rule, its `409`, its `204`,
its path family, and `ProfileResponse.deviceRouteLive`. **It contains no sentence constraining any
other method on that path**, and none is quoted here because none exists. *"Only DELETE was asked
for"* is the test author's summary of §5's scope, not a clause of it — and a summary of what a
document asked for on the day it was written cannot bind a document written later.

So the citation is withdrawn and no clause of `ADR-0049` is superseded, amended or narrowed. A route
is forbidden by an ADR saying so, and by nothing else.

### 2. A negative route assertion is a control for a positive row, and it names a verb nobody has asked for

`TASK-040611` states the assertion's purpose in its own words, and the purpose is right: *"a `get(...)`
typed beside the `delete(...)` would otherwise be invisible"*. Its sibling `theDeviceRouteIsInstalled`
asserts `401` — *"A route that was never installed answers 404, which makes this falsifiable"* — and
the pair together says something no single test says: **`duelServer` wires this path per verb.**

That property is kept in full. What changes is which verb carries it:

- **`theDeviceRouteIsNotInstalledOnAnyOtherVerb` is renamed `theDeviceRouteIsNotInstalledOnUnaskedVerbs`
  and issues `client.put("/api/me/device")`.** Its two inequalities are unchanged, and unchanged for
  their original reason: Ktor answers `404` or `405` depending on how the path matched, and the
  assertion is deliberately agnostic between them.
- **`PUT` is the strongest available replacement**, not an arbitrary one. `PUT` is installed
  **elsewhere under `/api/me/`** — `PUT /api/me/name` — so the assertion also refuses a path-matching
  mistake that a verb absent from the whole server could not see. `client.put` occurs nowhere else in
  `DuelServerRoutesTest` today, which makes it greppable as a gate.

**The rule this fixes, for every such assertion the repository writes from here on.** A merged test
may assert that a path does not answer a verb **only as a control for a positive row in the same
file**, and it must name a verb **no merged ADR has asked for**. Such an assertion is never a
prohibition and is never cited as one: it records what is wired today so that a stray handler cannot
be invisible. When a later ADR asks for a verb the control names, **the control moves to another
unasked verb** — it is not deleted, not weakened, and not argued with.

### 3. `ADR-0068` does not yield either — it already decides this, and it says *split*

`ADR-0068` §4 lists what does not earn `atomic:` and names this case exactly: *"These do **not** earn
it, and a reviewer rejects a ticket claiming them: … and — the one this is aimed at — **a scope that
grew after the ticket was written**. That is `TASK-020717`, and it is still a split, still today."*

`TASK-141003` was written at three files and grew a fourth. `ADR-0068` §4 answers it directly, in the
negative, and prescribes the remedy. So `ADR-0135` §8's *not `atomic:`*, `ADR-0068`'s cap and
`lint_tickets.py`'s `MAX_FILES_TOUCHED = 3` are not in conflict at all: **they agree, and jointly they
say the test edit is a separate ticket.** Nothing in the linter moves.

`ADR-0068` §1 — *"A budgeting rule never rewrites a correctness gate."* — is worth naming, because a
careless reader of §2 could accuse this ADR of doing precisely what §1 forbids. It does not. The
control is not weakened to fit a budget; it is re-pointed because **its premise changed**, and it is
re-pointed at a verb that makes it strictly more discriminating than it was. The budget decides only
*which commit* carries the edit.

### 4. The edit lands **before** the route, and asserts only what is true when it lands

The split has one legal direction. A test claiming the route *is* installed cannot land before it
exists; that would be worse than the contradiction, because it would be a green suite asserting a
false thing for one merge.

The predecessor commit therefore asserts, in full:

- `DELETE /api/me/device` answers `401`, not `404` — **unchanged, still true**;
- `PUT /api/me/device` answers neither `200` nor `401` — **new, true at that commit and true
  forever**, since no document has asked for `PUT` on that path or ever will under §2's rule.

It gives up exactly one true-today statement — that `GET` is unwired — and that is the statement
`ADR-0135` decided on 2026-09-07 to falsify. **The `GET` row has been stale since that merge, not
since `TASK-141003`.** It was asserting a fact the repository had already resolved to make false, and
a test in that condition is not coverage; it is a countdown.

**No replacement positive row for `GET` is added to `DuelServerRoutesTest`, and this is a decision
rather than an omission.** `theDeviceRouteIsInstalled` already proves `duelServer` calls
`deviceRoutes`; `ADR-0135` §4 installs the `get` *inside that same function*, beside the `delete`;
and `DeviceRouteTest`'s six new tests install `deviceRoutes` and nothing else, so a `get` placed in
any other route file makes all six fail. The one failure mode a composition-level `GET` row would
uniquely catch — the handler installed from a file `duelServer` does not call — is already caught
twice over. What is lost is the *reading* of the file: after this, `DuelServerRoutesTest` names two
verbs on that path and is silent about the third. The re-pointed test's comment therefore states the
rule rather than a roster, so there is nothing in it to go stale, and `docs/protocol.md` — completed
for this route by `TASK-141004` — stays the place the full surface is written down.

### 5. `ADR-0135` §4's mechanism is corrected: the device is resolved, not compared

**`IdentityResolver.namesPlayer` is deleted.** It cannot compute §5's table, it has never had a
caller, and the KDoc it merged with — *"this method exists solely to check whether a device belongs to
a player, used by the session restoration endpoint per `ADR-0135` §4"* — describes a use that will
now never happen. `IdentityResolver` returns byte-for-byte to what it was before `TASK-141002`: one
public suspend function, one `players.findOrNull` site, five `Identity` cases.

**The predicate is computed from `resolve`, which already answers the three-way question.** No new
public surface is added anywhere, because the `Identity` type already models exactly the distinction
§5 requires and `namesPlayer` could not make:

| `identities.resolve(token = null, deviceId = presented)` | means | `signOutHandsANewProfile` |
| --- | --- | --- |
| `Identity.Device(playerId, _)` | the device resolves, live, to that player | `playerId == caller` |
| `Identity.UnknownDevice` | a device id was presented and names nobody — the revoked row | `true` |
| `Identity.Anonymous` | no device id was presented | `true` |

which is `ADR-0135` §5's table, row for row, and `ADR-0135` §4's own prose predicate unchanged:
*"`signOutHandsANewProfile` is `true` **exactly when the `X-Device-Id` presented on this same request
does not resolve, through a live `device_binding`, to a player other than the caller.** It is `false`
only when it does."* That sentence was always right. Only the function underneath it was wrong.

In `DeviceRoutes.kt`, after the session has been resolved and `playerId` is in hand:

```kotlin
val handsANewProfile = when (val standing = identities.resolve(token = null, deviceId = call.deviceIdOrNull())) {
    is Identity.Device -> standing.playerId == playerId
    is Identity.UnknownDevice -> true
    Identity.Anonymous -> true
    // A null token cannot produce either of these. If one ever did, keep the profile:
    // ADR-0135 §6 makes every unknown a keep, and this is an unknown.
    is Identity.Session -> false
    Identity.Refused -> false
}
```

Four things this settles, each of which a reviewer would otherwise have to work out:

- **The explicit `token = null` is the question, not an oversight.** It asks *who does this device
  name, on its own* — and it must be asked with a null token, because `ADR-0027` §4's precedence
  makes `resolve` ignore the device id entirely whenever a token is present. The route calls `resolve`
  twice for two different questions.
- **It costs nothing extra.** The second call skips `sessions` and performs the single
  `players.findOrNull` that `namesPlayer` would have performed. One database read, as before.
- **It mints nothing.** `IdentityResolver`'s KDoc — *"This resolver never creates a profile"* — and
  the merged `resolvingCreatesNoProfile`, which already calls `resolve(token = null, deviceId = …)`
  for both a known and a ghost device and asserts the directory's profile count is unchanged, cover
  the property `namesPlayerAsksTheDirectoryOnceAndNeverMints` was written to cover. The guard is not
  lost with the function.
- **The two impossible branches take the safe direction, deliberately.** `ADR-0135` §6 rules that
  everything unknown is a keep and that *"Wrong-and-unsafe is a browser that abandons a profile with
  no password"*, so an impossible case answers `false`. A `when` that fell through to `true` would put
  the unreachable branch on the dangerous side, which is the one shape §6 forbids.

### 6. What the tickets become

**`TASK-141015` — new, ordered before `TASK-141003`, three files, not `atomic:`.** *The resolver stops
answering a comparison, and the smoke test names a verb nobody asked for.* Both halves remove a merged
claim this ADR supersedes, which is one subject and not a bundle of convenience:

| File | Action |
| --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/auth/IdentityResolver.kt` | modify — `namesPlayer` and its class-KDoc paragraph deleted |
| `poker-server/src/test/kotlin/duels/poker/server/auth/IdentityResolverTest.kt` | modify — its four `namesPlayer` tests deleted, **12 → 8** |
| `poker-server/src/test/kotlin/duels/poker/server/DuelServerRoutesTest.kt` | modify — the control re-pointed to `PUT`, **9 → 9** |

`CountingPlayerDirectory` stays: a second, unrelated test uses it. `depends_on: [TASK-141002]`.

**`TASK-141003` — unchanged in size, corrected in substance.** It stays at **three** files and stays a
plain ticket, so `ADR-0135` §8 holds exactly as written. Three edits:

1. `depends_on` becomes `[TASK-141002, TASK-141015]`.
2. Its *Scope* line *"The body is `DeviceStandingResponse(signOutHandsANewProfile =
   !identities.namesPlayer(deviceId, playerId))`"* is replaced by §5's `when` above. The clause
   after it — *"Never a stored association: `ADR-0135` §4 says computing it any other way turns the
   route into an enumeration surface"* — stands, and is satisfied: the device id comes from
   `call.deviceIdOrNull()` on that same request.
3. Its `verify:` gains `sh -c '! grep -rqF "namesPlayer" poker-server/src'`, so the removed function
   cannot come back through this ticket.

Its six named tests, its `DeviceRouteTest` count of **16**, its `ProfileDtosTest` count of **27**, its
no-default gate and its `git diff --exit-code` over the wire artifacts are untouched. In particular
`aRevokedBindingIsHandedANewProfile` — the row `!namesPlayer` happened to get right and
`namesPlayer` got wrong — is now the row that discriminates the two mechanisms, and it stays.

**Nothing else in `STORY-1410` moves.** `TASK-141004` keeps its two files, its 49-test count and its
*"Server or client code"* refusal; `TASK-141005` onward are untouched.

### 7. What does not move

`lint_tickets.py`, in any constant or rule. `ADR-0068` §3's table. `ADR-0049` §5. `ADR-0135` §§1, 2,
3, 5, 6, 7, 8. `IdentityResolver.resolve` and all five `Identity` cases — which §4 promised would be
byte-unchanged and which are now more nearly so than the mechanism it specified would have left them.
`docs/protocol.md`, which `TASK-141004` still owns. And no gate anywhere is deleted, skipped, made
conditional or given an escape hatch.

## Consequences

**What it buys.** `TASK-141003` becomes implementable exactly as `ADR-0135` §8 says it should be — a
plain three-file ticket — and its waiting implementation needs one expression changed rather than a
new shape. The predicate reproduces `ADR-0135` §5's table for the first time. `IdentityResolver` keeps
its one job. The repository gains a stated rule for negative surface-area assertions, so the next one
is a control with a known move rather than a citation nobody can overrule. And the `PUT` control is
strictly more discriminating than the `GET` one it replaces, because `PUT` is live elsewhere under
`/api/me/`.

**What it costs.**

- **`STORY-1410` grows a ticket, and it is a ticket that ships no behaviour.** `TASK-141015` deletes a
  function, deletes four tests and changes a verb. Nothing a player can see is different afterwards,
  and it sits on the critical path of eleven tickets.
- **Four merged tests are deleted and are not replaced.** `aDeviceResolvingToThatPlayerNamesThem`,
  `aDeviceResolvingToAnotherPlayerDoesNotNameThem`, `aDeviceResolvingToNobodyNamesNobody` and
  `namesPlayerAsksTheDirectoryOnceAndNeverMints` go. The properties survive in
  `resolvingCreatesNoProfile` and in `DeviceRouteTest`'s four resolution rows, but they survive at the
  route rather than at the unit, one level further from the thing they describe.
- **`TASK-141002` becomes a done ticket whose `verify:` block no longer passes.** Its three structural
  gates count *two* public suspend functions and *two* `players.findOrNull` sites, and after
  `TASK-141015` both are one again; its count gate expects `tests="12"` where the file will hold 8.
  Nothing re-runs a merged ticket's gates, so no build breaks — but the trail now contains a ticket
  whose recorded proof is false against the tree, and this ADR is the only thing that explains why.
  That is the price of correcting `ADR-0135` §4 rather than building around it, and it is written
  down here rather than left for a reader to discover.
- **For one merge, `DuelServerRoutesTest` says nothing about `GET /api/me/device`**, and permanently
  it says nothing about it at the composition level. §4 argues the gap is already covered twice; the
  argument is sound and it is still an argument, not a gate.
- **The route resolves identity twice.** One extra `resolve` call per request, and a reader must see
  why `token = null` is passed on purpose. A comment carries that, and comments are not gates.
- **A two-day-old ADR is corrected by the story implementing it, for the second time in this
  epic.** That is the process working, and it is also evidence that an ADR specifying a Kotlin
  signature is specifying something it cannot check.

**What it forecloses.** Citing a test as a prohibition — after §2, a negative route assertion carries
no authority over what may be installed, and any future argument of the form *a merged test says this
route may not exist* is answered by pointing here. Raising `MAX_FILES_TOUCHED`, or claiming `atomic:`,
for a scope that grew after a ticket was written: `ADR-0068` §4 already refused it and this ADR is now
a worked example, so the next attempt has two documents against it. And a single-boolean
device-to-player question on `IdentityResolver` — the shape is deleted, and re-adding one would have
to argue against a table it cannot express.

**What it does not foreclose.** Installing further verbs on `/api/me/device`, which needs an ADR and
then §2's one-line control move. Deriving the control's verb list mechanically from `docs/protocol.md`
rather than writing it by hand — see the trigger below, deliberately not decided now.

### The reversal trigger

Reverse §2 if a stray handler is ever installed on a path whose control names a different verb and
merges undetected. That is the failure the control exists to prevent, and one sighting means naming a
single verb by hand is not enough — the list has to be derived from the documented surface rather than
chosen by an author.

### The trigger for a mechanism, named rather than registered

**Nothing in this decision detects the collision recurring.** No gate links *an ADR asks for a route*
to *a merged test denies that route*, and none is added here: `HttpEndpointDocumentationTest` reads
`docs/protocol.md` against the response DTOs and never enumerates installed routes, so there is no
existing hook to hang it on, and inventing one is a mechanism decision of its own rather than a clause
of this one. What changes is the **cost** when it recurs: after §2 the collision is not a contradiction
between an ADR and a test — it is a one-line move with a named shape, a predecessor ticket, and no
decision to make. That is a smaller thing to hit, not an absent one.

The trigger is stated so it can fire: **the second time an ADR asks for a verb that a merged negative
control names — on this path or any other — the verb list stops being written by hand and becomes a
decision**, following `ADR-0142` §7's precedent, where a named trigger fired and became `DEC-158`. One
occurrence is an incident; two is a mechanism.

## Alternatives considered

**1. Retire `theDeviceRouteIsNotInstalledOnAnyOtherVerb` outright.** The strongest case: it is one
assertion about one verb on one path, its cited source turns out not to say what it claims, the route
it denies is now required by a merged ADR, and deleting it makes `TASK-141003` a three-file ticket with
no new ticket and no ordering constraint — the cheapest answer on the board by a distance, and
`DeviceRouteTest` covers the `GET`'s behaviour thoroughly either way. Rejected because the assertion's
purpose survives its citation. `TASK-040611` wrote down what it was for — *"a `get(...)` typed beside
the `delete(...)` would otherwise be invisible"* — and that is a property about the composition root
that nothing else asserts and that has nothing to do with `ADR-0049`. Deleting it would buy the route
by spending a guard, and would leave the repository with a precedent that an inconvenient negative
assertion may be removed rather than moved.

**2. Split `TASK-141003` so the *route* lands after the *test*, with the test asserting the route is
installed.** Strongest case: it is the smallest possible predecessor, one file and one line, and it
leaves `DuelServerRoutesTest` permanently symmetrical — a positive row per installed verb, forever.
Rejected outright: a test asserting `GET /api/me/device` answers `401` cannot pass before the handler
exists, so either it lands red — which no gate permits — or it lands `@Disabled`, which is a green
suite carrying a false claim. §4's ordering is the only direction in which every intermediate commit
asserts only true things.

**3. Amend `ADR-0135` §8's prohibition and let `TASK-141003` be `atomic:` at four files.** Strongest
case: it is one ticket instead of two, the four files genuinely do belong to one change, and §8's
sentence was written about the *socket* — its whole paragraph is about `Hello`, `Welcome`,
`ProtocolCodec` and `PROTOCOL_VERSION` — so reading it as a bar on any fourth file for any reason is
arguably over-reading a sentence about protocol bumps. Rejected because `ADR-0068` §4 decides it
independently of §8 and decides it against: `atomic:` names *a merged gate that makes a smaller commit
fail*, and here a smaller commit demonstrably does not fail — `TASK-141015` compiles, passes and ships
on its own. The claim would be false on the linter's own terms, and *"a scope that grew after the
ticket was written"* is the exact case §4 says it is aimed at.

**4. Raise `MAX_FILES_TOUCHED`, or add an exemption for a test-only fourth file.** Strongest case: this
collision is a symptom, the cap is the cause, and the cap is a number somebody chose — a fourth file
that is purely a test repair adds no reviewable risk, and `ADR-0069` already deleted the *ceiling* on
`atomic:` for the honest reason that every number written down had been wrong. Rejected on scope and
on evidence. It governs every ticket in the repository — the largest change available here — to fix a
case that has arisen once, and `ADR-0068` §1's *"A budgeting rule never rewrites a correctness gate"*
cuts both ways: a budget that has caught real scope growth twice is not obviously the thing that is
wrong. If it is, that is its own decision with its own evidence, not a side effect of one route.

**5. Keep `namesPlayer`, unused, and compute the predicate from `resolve` anyway.** Strongest case:
`TASK-141015` drops to one file, `TASK-141002`'s `verify:` block stays true, four merged tests survive,
and nothing has to be deleted at all — by far the cheapest to reverse. Rejected because it leaves
merged public API whose own KDoc says it is *"used by the session restoration endpoint per `ADR-0135`
§4"* when nothing uses it, in a repository that has spent this epic correcting exactly that kind of
false statement. Cheapest-to-reverse governs when the evidence is thin; here the evidence is that the
function provably cannot compute the table and provably will never be called.

**6. Widen `namesPlayer` into `playerNamedBy(deviceId): PlayerId?` instead of deleting it.** The
strongest case of the rejected mechanisms: it is the minimal diff from what merged, it needs no
unreachable branches in the route, it reads in three lines, and — measured — it keeps every one of
`TASK-141002`'s four gates passing, including the `tests="12"` count, because the four tests are
retargeted rather than removed. Rejected on what it does to `IdentityResolver`. A function returning a
`PlayerId?` for a `DeviceId` **is** a second way to turn a device into a player, which is precisely
what `ADR-0135` §4 worried a *"careless later reader could mistake `namesPlayer` for"* — under this
option it would no longer be a mistake. `resolve` already answers the question through a type built for
it, and the class stays *"the one place the session-versus-device precedence rule is written"*.

**7. Have the route reach `PlayerDirectory` directly, bypassing `IdentityResolver`.** Strongest case:
it is the most direct expression of the question — `players.findOrNull(deviceId)?.id` is the whole
computation — and it adds nothing to any shared class. Rejected on two counts: `deviceRoutes` would
need a fourth constructor parameter, which `TASK-141003`'s *"It needs no new parameter and no wiring
change"* rules out and which would put `Application.kt` in the ticket as a fourth file; and it would
give a route file its own device-to-player lookup, which is the arrangement `ADR-0027` §4 exists to
prevent.

## What this does not settle

Whether any negative surface-area control should be derived from `docs/protocol.md` rather than
hand-written — named as a trigger above, unregistered, and deliberately left until it has happened
twice. Whether `docs/protocol.md` should document that a path answers *only* the verbs it lists:
`TASK-141004` writes the `GET` section and this ADR neither requires nor forbids such a sentence.
And `DEC-147`'s and this ADR's shared observation that `GET /api/me` and `GET /api/me/device` could
one day fold into a single account-facts read, which `ADR-0135` §Consequences already left open and
which nothing here narrows.
