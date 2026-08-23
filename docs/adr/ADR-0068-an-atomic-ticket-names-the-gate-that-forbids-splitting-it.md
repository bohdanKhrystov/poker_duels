# ADR-0068 — An atomic ticket names the gate that forbids splitting it

- **Status:** Accepted
- **Date:** 2026-08-23
- **Resolves:** `DEC-063`
- **Amends** [`ADR-0007`](ADR-0007-token-lean-agent-workflow.md) — *"Files touched ≤ 3"* gains one
  narrow, declared exemption. The default is unchanged and the context budget is not reversed.
- **Amends** [`ADR-0047`](ADR-0047-a-protocol-version-is-claimed-in-a-ledger.md) §6, whose count of
  what a bump commit carries is wrong, and replaces the count with a procedure. The ledger, its
  gate and its atomicity are untouched.
- **Constrains:** [`TASK-021301`](../../tasks/tasks/TASK-021301-the-wire-gains-a-rematch-and-the-version-takes-its-step.md),
  `STORY-0214`, `STORY-0405`, and every future ticket that declares more than three files

## Context

Two rules this project already made collide head-on for the first time, and both of them are right.

**The cap.** `ADR-0007` cut a ticket from *"files touched ≤ 10"* to *"≤ 3"* on measured evidence,
and gave the reason in one line: *"The named file list **is** the context budget."* The economics of
the whole agent workflow — a cheap coder with a cold context per ticket — rest on that number. It is
enforced: `.github/scripts/lint_tickets.py` line 33, `MAX_FILES_TOUCHED = 3`.

**The gate.** `ADR-0047` made a `PROTOCOL_VERSION` bump atomic *on purpose*. Version equality is the
only compatibility mechanism this protocol has, so one integer must name exactly one wire shape
(`ADR-0028` §8), and `ProtocolVersionLedgerTest` fails on any tree where the constant, the ledger row
and the live descriptors disagree. `ProtocolDocumentationTest` closes the same trap from both sides:
a live message type with no `docs/protocol.md` row fails
`everyClientMessageHasARowSayingClientToServer`, and a row naming no live type fails
`theDocumentNamesNoMessageThatDoesNotExist` — so the document can move neither before the Kotlin nor
after it. That atomicity is a **correctness property of the wire**, not a stylistic preference.

`STORY-0213`'s split is where they meet. `TASK-021301` is twelve files (§5 enumerates them and names
the gate holding each one), it cannot be eleven, and the largest number its frontmatter is allowed
to contain is three.

### Four things worth knowing before choosing, each checked rather than assumed

**The cap is a control on *declarations*, not on *changes*.** Before this decision `files_touched`
occurred in `.github/` exactly three times, all inside `lint_tickets.py`'s range check; no workflow
compares it to a diff, a PR, or anything else. Its only mechanical effect is to refuse an integer in a markdown file's
frontmatter. It cannot stop a coder from touching twelve files. It can only stop a planner from
saying so.

**Which is precisely what has happened, ten times.** Of the 683 schema-2 tickets carrying
`files_touched`, ten declare a smaller number than their own *Files* table's create/modify rows, and
**nine of those ten are `done`**:

| Ticket | Declared | Files table | Status |
| --- | --- | --- | --- |
| `TASK-021301` | 3 | 12 | blocked — this decision |
| `TASK-020717` | 3 | 8 | done |
| `TASK-021014`, `TASK-021114`, `TASK-041018` | 3 | 5 | done |
| `TASK-040108`, `TASK-040115`, `TASK-040807`, `TASK-040907`, `TASK-040913` | 3 | 4 | done |

Not one of them hid anything. `TASK-040807`'s *Files* section opens *"Four, and the linter caps the
field at three… The frontmatter says `3`; the fourth is `AuthRouteDoubles.kt`, named here so the
count is honest rather than hidden."* `TASK-021014`'s says *"`files_touched` should have been 5"*.
Every one named its extra files in the open and wrote a false integer above them, because the true
integer would not lint. **The defect is not dishonesty. It is unrepresentability** — and the trail is
Product B, so ten merged tickets carrying a field known to be false is a defect in a deliverable.

**Nine of the ten share one mechanism, and the tenth does not.** In nine, a change to a single
declaration is propagated mechanically into companion files by something that refuses the
intermediate state: an interface signature dragging both of its test doubles (`TASK-040807`,
`TASK-040907`), a new `NOT NULL` column breaking every fixture that inserts a row directly
(`TASK-021014`), an exhaustive `when`, a byte-comparing verify task. The agent chooses nothing in
those files; a compiler or a test names them one at a time. The tenth, `TASK-020717`, is a different
animal: its scope **grew** during implementation because its original file list made its own Done
criterion unreachable. That distinction is load bearing. **A blanket raise of the cap would license
the tenth exactly as readily as the nine, and the tenth is the failure the cap exists to catch.**

**The cap has already changed a technical outcome outside ticketing.** `ADR-0062` deferred renaming
`ServerClock` in part because *"the type and method names occur in eleven Kotlin files and a rename
must land atomically to compile, which exceeds the enforced `files_touched: 1..3` cap on a ticket"*.
So this is the second time a budgeting rule has bent a technical decision, and the first time it has
stopped one dead.

### What raised this was a stale count, and that is a force of its own

`ADR-0047` §6 states that a bump commit carries **five** artifacts. `docs/protocol.md` repeats it.
Both were true when written and are now wrong by seven, because the set has silently grown every
time a gate was added: `ADR-0045` §4 said four, `ADR-0047` §6 said five, `ADR-0044` §9 — written two
days before the ledger existed — foresaw neither `web-client/src/protocol/frames.ts` nor
`web-client/src/e2e/scripted-duel.gen.json`. A planner who trusted any of those numbers would have
sized this ticket at five and discovered the rest during implementation. **Whatever replaces the
five must not be another number**, or this recurs on the gate after next.

### The deadline

Real, and it does not argue for a particular answer. `ADR-0045` §3 allows **one** bumping branch open
at a time, so `STORY-0213`, `STORY-0214` and `STORY-0405` are strictly serial behind this ticket:
three epics, and nothing in any of them moves. Separately, every day the true number stays
unrepresentable, the next atomic ticket adds another false row to a trail that is not rewritten
afterwards.

## Decision

### 1. When a gate and a budget collide, the budget is what is wrong

**A budgeting rule never rewrites a correctness gate.** `ProtocolVersionLedgerTest`,
`ProtocolDocumentationTest`, `:poker-server:verifyProtocolTypes`, `:poker-server:verifyDuelScript`,
Kotlin's exhaustiveness checking and TypeScript's `satisfies` do not move, are not weakened, are not
made skippable, and gain no escape hatch. This forecloses the whole "split the gates so the change
lands in pieces" family, permanently: those gates exist so that a wire shape and the number naming it
can never be separated by a commit boundary, and a lint rule about markdown frontmatter is not a
reason to give that up.

### 2. `files_touched` is a fact about the ticket, and it must be true

`files_touched` is the number of rows in the ticket's *Files* table whose action is
`create`, `modify`, `regenerate`, `delete` or `rename`. Rows marked `read` never count — that is
already the convention several tickets state explicitly, and it is now the rule.

**A ticket may not declare a number smaller than its own *Files* table.** The field is a fact, not a
budget, and a false one is worse than an absent one.

### 3. The exemption: `atomic:`

Schema 2 gains **one optional frontmatter key**: `atomic:`, a block sequence naming **one merged
gate per line** — the same shape `verify:` already uses, which is what the ticket frontmatter parser
reads (it is deliberately not a YAML parser, so a multi-line scalar is not available).

```yaml
files_touched: 12
atomic:
  - ProtocolVersionLedgerTest — a wire shape whose fingerprint no ledger row claims fails it
  - ProtocolDocumentationTest — a live type with no row, and a row with no live type, both fail
  - the Kotlin compiler — two exhaustive when expressions in DuelSocket
  - verifyProtocolTypes and verifyDuelScript — byte comparisons run on every check
  - tsc TS1360 — the satisfies table in frames.ts, and the ProtocolVersion alias in version.ts
```

One gate per line rather than one sentence, because a ticket held by five gates that names one has
told the reviewer almost nothing, and a single line is where a list goes to be truncated.

`lint_tickets.py` enforces exactly three rules, and nothing else changes:

| `atomic:` | `files_touched` |
| --- | --- |
| absent | `1..3` — unchanged, and the case for ~98.5% of tickets |
| a non-empty sequence of non-empty lines | `4..12` |
| present but empty, or not a sequence | rejected |

`atomic:` below four files is rejected too, so the key's presence means exactly one thing: *this
ticket claims the exemption*. A ticket that shrinks during implementation drops the key.

**Twelve is a tripwire, not a budget.** It is the size of the largest atomic unit this repository has
actually demonstrated, measured today. A ticket needing thirteen stops and becomes a decision rather
than escalating quietly, which is the property the number is there for.

### 4. What earns `atomic:`, and what does not

Each line names a **merged gate** that makes a smaller commit fail. A gate is something that fails
an exit code:

- a compiler rule — an exhaustive `when` over a sealed hierarchy or enum, an interface signature its
  implementers must follow, a `satisfies` table (TS1360);
- a merged test — `ProtocolVersionLedgerTest`, `ProtocolDocumentationTest`, a schema-constraint test;
- a Gradle verify task that byte-compares a generated artifact;
- a database constraint that breaks existing fixtures.

These do **not** earn it, and a reviewer rejects a ticket claiming them: *"these files belong
together"*, *"it is one feature"*, *"the reviewer will want the context"*, *"it is simpler in one
commit"*, and — the one this is aimed at — **a scope that grew after the ticket was written**. That
is `TASK-020717`, and it is still a split, still today.

When `atomic:` is set, every row of the *Files* table carries a *why it cannot be fewer* reason
naming its gate. The linter cannot judge prose; this column is what a reviewer checks the claim
against, and the ADR says so rather than implying the check is mechanical.

### 5. A protocol bump is one ticket, and its size is computed rather than remembered

**The set of files a `PROTOCOL_VERSION` bump carries is `PROTOCOL_VERSION` itself plus every file a
merged gate derives from it or from the wire shape it names.** That set is not a constant — it has
been four, then five, and is now twelve — so it is **never written down as a number again**. It is
enumerated by running, on the tree being changed:

```
./gradlew :poker-server:check          # the Kotlin compiler, ProtocolDocumentationTest,
                                       # ProtocolVersionLedgerTest, verifyProtocolTypes, verifyDuelScript
cd web-client && npm run check         # tsc
```

Each failure names the next file by path, and `ProtocolVersionLedgerTest` prints the ledger row to
paste (`ADR-0047` §5). A planner sizes a bump by that procedure, not by a remembered figure.

**As of 2026-08-23 the set is twelve**, listed here for a planner's convenience and dated so a stale
list is recognisable as one:

| # | File | Held by |
| --- | --- | --- |
| 1 | `protocol/ClientMessage.kt` | the new client type |
| 2 | `protocol/ServerMessage.kt` | the new server type |
| 3 | `protocol/ProtocolError.kt` | a new error value — it is a `TypeScriptDeclaration` in the walk, so it moves the fingerprint and cannot land unbumped |
| 4 | `protocol/Protocol.kt` | `ProtocolVersionLedgerTest` rules 3 and 4 |
| 5 | `DuelSocket.kt` | two exhaustive `when` expressions stop compiling the moment either hierarchy gains a variant |
| 6 | `protocol/ProtocolJsonTest.kt` | `assertEquals(2, PROTOCOL_VERSION)` — a literal |
| 7 | `docs/protocol.md` | `ProtocolDocumentationTest`, from both directions at once |
| 8 | `docs/protocol-versions.md` | `ADR-0047` §1 — one ledger row |
| 9 | `web-client/src/protocol/protocol.gen.ts` | `verifyProtocolTypes`, byte comparison |
| 10 | `web-client/src/protocol/version.ts` | typed against the generated `ProtocolVersion` alias |
| 11 | `web-client/src/protocol/frames.ts` | `satisfies Record<ServerMessage["type"], true>` — a missing key is TS1360 |
| 12 | `web-client/src/e2e/scripted-duel.gen.json` | `verifyDuelScript` — the scripted `Welcome` embeds `protocolVersion` |

Rows 9 and 12 are **regenerated by a named Gradle task and never hand-edited**; they still count,
because a file in the commit is a file in the blast radius.

`TASK-021301` becomes `files_touched: 12`, `atomic:` set, `status: ready`. `STORY-0214` and
`STORY-0405` are written the same way, each recomputing its own set.

### 6. `ADR-0047` §6 and `docs/protocol.md` are corrected

`ADR-0047` §6's *"five artifacts"* is wrong and §5 above replaces it. `ADR-0047` is **not rewritten**
— an ADR records what was decided when it was decided, and its header now carries *Amended by 0068*.
`docs/protocol.md` is a living document and **is** corrected in place: its sentence stops naming a
count and states §5's procedure instead, so the next gate added cannot make it stale in the same way.

### 7. What this does not decide

Whether `files_touched` is ever checked against the actual diff. It is not built here, deliberately:
the defect measured above is a *planning-time* one — a planner unable to write a true number — and a
merge-time diff check arrives after the plan is frozen and would be the first ticket gate that cannot
run in an agent worktree. The cheaper residual, if the honesty rule fails again, is a linter that
counts the *Files* table's edit rows itself; that is measured as feasible — it would flag exactly
those ten tickets out of 683 today — and is left unbuilt because §3 removes the *motive* for the
false number rather than policing it. Also undecided: whether `ADR-0062`'s eleven-file
`ServerClock` rename now becomes ticketable. It is newly *expressible*, and its deferral rested on
two other reasons this ADR does not touch.

## Consequences

**What it buys.**

- `TASK-021301` becomes startable, and with it `STORY-0213`, `STORY-0214` and `STORY-0405` — three
  epics off a hard stop.
- The frontmatter field becomes trustworthy going forward. A planner who needs four files writes
  four, and a reader can believe it.
- The one number worth defending is defended. The cap still binds ~673 of 683 tickets exactly as
  before, and the exemption costs a sentence naming a gate that either exists or does not.
- `ADR-0047` §6 stops being a trap. The next gate added to the protocol changes the *answer* to §5's
  procedure without making any document wrong.
- The distinction the cap actually cares about is now written down: a ticket that is *irreducible*
  and a ticket that *grew* are different failures, and only the first has an exemption.

**What it costs.**

- **The linter still cannot tell a true `atomic:` from a false one.** It checks presence and a range;
  it cannot read *"ProtocolVersionLedgerTest forbids it"* and know whether that is so. The honesty
  rule is enforced by review, which is the same place it lived before — what changed is the motive,
  not the policing. If a planner learns that `atomic: <anything>` unlocks twelve files, the cap is
  gone for anyone willing to type a sentence, and nothing here will catch it.
- **Twelve has zero headroom for the ticket that motivated it.** `TASK-021301` is exactly twelve. If
  implementation finds a thirteenth file, the ticket blocks and comes back here. That is the tripwire
  working as designed, and it is also a real risk of a second stall on the same critical path.
- **The nine merged tickets are not corrected.** They stay as they are, false field and all, because
  a merged ticket is trail and rewriting it to look tidy is worse than leaving a defect visible. The
  repository therefore permanently contains nine tickets whose `files_touched` is wrong, and a reader
  who samples the backlog will find them.
- **Schema 2 gains a key**, and five places must learn it: `lint_tickets.py`, `tasks/README.md`, the
  `planner` agent, the `plan-story` skill and the `coder` agent. Each is one more thing that can go
  stale — the exact failure mode this ADR was raised by.
- **`files_touched` now means two things at once** — a fact about the ticket and, when under four, a
  budget. The exemption makes the field's meaning conditional on another field's presence, which is
  strictly harder to explain than *"1 to 3"*.
- **A bump ticket is now a legitimately large unit of work for a cheap model.** Twelve files in one
  cold context is exactly what `ADR-0007` priced out. `TASK-021301` is `tier: sonnet`,
  `review: standard`, and this ADR does nothing to make that cheaper — it makes a previously
  impossible ticket merely expensive.

**What it forecloses.** Splitting a protocol bump, permanently and by design (§1) — including any
future scheme that would let the wire move in one commit and its version in another. Raising
`MAX_FILES_TOUCHED` globally, which now requires arguing against §4's distinction rather than just
editing a constant. And `files_touched` as a pure budget: it is a fact now, and a future gate that
wants a budget needs a different field.

**Why this shape when the evidence is thin.** It is the cheapest of the candidates to reverse.
Deleting one linter branch and one line of `tasks/README.md` restores today exactly; the `atomic:`
keys left behind in a handful of tickets are inert text that no tool reads. Weakening a protocol gate
is reversible only by someone who reconstructs why it was weakened, and a raised global cap is
reversible only after every ticket written under it has been re-sized.

## Alternatives considered

**Raise `MAX_FILES_TOUCHED` from 3 to 12 for everything.** The strongest case by far, and the
cheapest: one integer, no new key, no new rule, nothing for five documents to learn, and it fixes all
ten tickets at a stroke. `estimate` already caps a ticket at 120 lines, so size is not unbounded, and
the cap was never a real control anyway — nothing checks it against a diff. Rejected because the ten
tickets are two different things and this cannot tell them apart. Nine were irreducible; `TASK-020717`
grew past its own ticket, which `ADR-0007` and `tasks/README.md` both name as *the* failure mode this
system exists to prevent (*"the failure mode of this project is tickets that grew, never tickets that
were too small"*). A cap of twelve makes the ticket that grew lint clean, and the ceiling stops being
a signal that anything is unusual. The cost of the narrower rule is one optional key; the cost of this
one is that the workflow can no longer see a ticket outgrowing itself.

**Exempt protocol bumps specifically** — by label, by a `version-bump` marker, or by ticket id.
Genuinely appealing: it is the narrowest possible change, it names the real case, and no other ticket
kind is affected. Rejected because the exemption would be invisible where it matters. A label is
free-form and unreviewed, and it answers *this* ticket while leaving `TASK-040807`'s four files, the
`NOT NULL` fixtures and `ADR-0062`'s eleven-file rename exactly as unrepresentable as before — the
same wall, three more times, each needing its own exemption. `DEC-063` explicitly asks for a rule that
governs `STORY-0214` and `STORY-0405` too, and the honest generalisation of *"a protocol bump is
atomic"* is *"a gate forbids splitting this"*.

**Split the gates so the bump lands in pieces** — for instance, let `ProtocolDocumentationTest`
tolerate a documented type that does not exist yet, or let the ledger row land ahead of the constant.
Its case is not empty: it would keep every ticket at three files, need no schema change, and the
intermediate states are short-lived on a branch nobody deploys from. Rejected outright. That
tolerance *is* the defect `ADR-0047` was built to make impossible — a wire shape with no row and a row
with no shape are the two halves of one integer naming two wire shapes, and the protocol has no
negotiation, no capability list and no forward compatibility to absorb the mistake. Trading a
correctness property of the wire for a markdown lint rule inverts the two things' importance. §1
forecloses it in so many words.

**Redefine `files_touched` to exclude generated artifacts.** A real distinction, and the ticket's own
table already marks `protocol.gen.ts` and `scripted-duel.gen.json` as *regenerate, never hand-edited*
— their context cost is a Gradle command. Rejected because it does not reach: it takes twelve to ten,
still over the cap, so the cap must move anyway and this would be a second rule buying nothing.
Pushed to its logical end — count only files requiring judgement, excluding the two generated files,
the two moved literals, the pasted ledger row and the one `frames.ts` key — it still lands at six.
There is no counting convention under which an honest protocol bump fits in three. It also makes the
field stop describing the commit, which is where its remaining value is.

**Correct `ADR-0047` §6's count and nothing else.** The minimal reading of `DEC-063`: the count is
demonstrably wrong, correcting it is required either way, and perhaps five-should-have-been-twelve was
the whole problem. Rejected because it answers the symptom. `TASK-021301` would still be unwritable at
twelve, `lint_tickets.py` would still refuse the number, and the planner would still have to choose
between a false field and a blocked story. Correcting the count is §6 of this decision, not an
alternative to it — and correcting it to *twelve* rather than to a procedure would guarantee this
recurs on the thirteenth gate.

**Do nothing: keep declaring `3` and naming the truth in the *Files* table.** The honest case, and it
has nine merged precedents: it costs nothing, it blocks nothing, the *Files* table is what the coder
actually reads, and every one of the nine landed green. Rejected because it makes the workflow's one
enforced size rule into a rule everyone knows to write past, and because the field is then noise —
`3` means *"three, or any number at all"*, so nothing can ever be built on it. For a project whose
second deliverable is the documented trail, shipping a ticket schema with a field that is false
whenever it matters is a defect in the product, not a shortcut around one.
