# ADR-0047 — A protocol version is claimed in a ledger, and the second claim conflicts

- **Status:** Accepted — §6's artifact count amended by [`ADR-0068`](ADR-0068-an-atomic-ticket-names-the-gate-that-forbids-splitting-it.md)
- **Date:** 2026-08-16
- **Resolves:** `DEC-040`
- **Extends** [`ADR-0045`](ADR-0045-presence-belongs-to-the-table.md) §4 with a fifth artifact the
  bump commit carries, and makes [`ADR-0028`](ADR-0028-the-wire-names-an-absent-opponent.md) §8's
  rule — one number names one wire shape — executable for the first time. Neither is re-argued:
  when a bump is *needed* stays `ADR-0028` §8's, and the landing order stays `ADR-0045` §3's.
- **Constrains:** [`TASK-000104`](../../tasks/tasks/TASK-000104-a-second-branch-cannot-claim-the-same-protocol-version.md),
  and every future change to `ClientMessage` or `ServerMessage` — `STORY-0213`, `STORY-0214` and
  `STORY-0405` first

## Context

Version equality is the **only** compatibility mechanism this protocol has. `protocolJson` sets
`ignoreUnknownKeys = false`, the handshake compares `protocolVersion` for exact equality, and
`VERSION_MISMATCH` is terminal for the connection. There is no negotiation, no capability list and
no forward compatibility, by design — so `ADR-0028` §8's rule that one integer names exactly one
wire shape is not a style preference. It is the assumption the handshake is built on.

Nothing enforces it. `ADR-0045` §3 wrote the hole down rather than fix it, and called the fix
*"its own decision"*. That is this one.

**The defect is not hypothetical, and it is queued three times.** `STORY-0213` (`ADR-0044`),
`STORY-0214` (`ADR-0028`, filed by `ADR-0045`) and `STORY-0405` (`ADR-0027`) each carry an unlanded
bump, and each claims *"the next number free when it lands"* — three claims on the same next
integer, in three epics' queues, held apart today by a sentence in an ADR and a rebase.

**Every gate in the repository is blind to it, and the blindness is structural.** Reproduced with
`git merge-file` against the real constant:

```
$ git merge-file -p Protocol.b.kt Protocol.base.kt Protocol.a.kt
public const val PROTOCOL_VERSION: Int = 3
exit=0
```

Both branches made the *identical* edit, so the three-way merge takes it silently; a rebase drops
the patch as already applied. `ProtocolDocumentationTest.theDocumentStatesTheCurrentProtocolVersion`
compares the document to the constant, and both moved together on both branches, so it passes on
either and on their merge. `:poker-server:verifyProtocolTypes` byte-compares `protocol.gen.ts`
against what the descriptors on **that tree** emit — it is a pure function of one tree and cannot
see a number another branch spent. The result is one integer naming two wire shapes, reached with
every check green, and the first symptom is a real client meeting a real server.

**What makes it a real decision is that the fact to be protected is not in any one branch's diff.**
Uniqueness of a claim is a property of the *union* of the branches. Every gate this project owns
reads a single tree, so a gate can only see the race if either (a) it reaches outside its tree for
another branch's state, or (b) the claim is turned into *content*, so that two claims are two
different bytes in one place and git's own three-way merge does the comparing. Those two families
have completely different failure modes, costs and homes, which is why two competent engineers
would not land in the same place.

Three further forces bound the answer:

- **GitHub does not re-run a pull request's checks when the base branch moves.** The merge ref is
  recomputed; the workflow is not re-triggered — `pull_request` fires on head pushes, not base
  pushes. A check that compared this branch against `origin/develop` and went green when `3` was
  free stays green after `3` is taken, and the merge button stays enabled.
- **`strict = false` is a recorded, measured decision.** `TASK-000102` set branch protection when
  the repository went public and deliberately did not require branches to be up to date: *"true
  would force a rebase and a full CI re-run on every PR in a sequential chain, doubling wall-clock
  for no safety gained."* Anything whose soundness depends on a stale-base re-check is a reversal of
  that, not an addition to it.
- **Every protocol gate here is a pure function of the tree.** `ProtocolDocumentationTest` reads two
  files; `verifyProtocolTypes` compares two byte arrays. They run identically in CI, in an agent
  worktree and offline. A gate needing a network fetch and a resolvable `origin/develop` would be
  the first exception, and it would be weakest exactly where this project runs most — in detached
  agent worktrees.

### The deadline

Real, and it does not argue for a particular answer. `ADR-0045` §3's *"at most one bumping branch
open at a time"* is discipline no agent can verify from inside its own ticket, and three branches
are queued behind it. The gate is free while the count of protocol versions is two and the wire is
in the tree to be fingerprinted; once two of the three bumps have landed unguarded, seeding a
record of what each number meant requires reconstructing shapes that no longer exist anywhere.

## Decision

**`docs/protocol-versions.md` is a ledger of claims: one row per protocol version, naming the wire
shape that number means. Moving `PROTOCOL_VERSION` without appending a row fails the build, and two
branches appending a row for the same number conflict textually — git refuses the second merge,
before any check runs.**

### 1. The ledger

`docs/protocol-versions.md`, a single markdown table, newest row last:

```markdown
| Version | Wire fingerprint | Claimed by | Landed |
| --- | --- | --- | --- |
| 2 | `0123456789abcdef` | STORY-0202 | 2026-08-12 |
```

Every row of the table matches ``^\| (\d+) \| `([0-9a-f]{16})` \|`` — a row that does not is a
failure, never a row silently skipped. The last two columns are for the reader and are not parsed.

The ledger starts at version **2**, and the document says why in one line: it was introduced after
version 1's shape had already been replaced, and that shape is not recoverable. A ledger that
invents a row it cannot substantiate is worth less than one that admits where it begins.

### 2. The fingerprint

The first **16 hexadecimal characters** of the SHA-256 of the protocol's declarations: the `text` of
every `TypeScriptDeclaration` returned by `protocolDeclarations()` — `ADR-0020`'s descriptor walk
over `ClientMessage` and `ServerMessage` — **sorted by declaration name** and joined with `\n`.

- The generated file's header and its `export type ProtocolVersion = N;` alias are **excluded**. The
  fingerprint names the shape, not the number; that is what lets a wrong number be recognised as a
  shape that already has one.
- Sorting by name makes it insensitive to walk order, which is not a wire fact.
- It is the same content `verifyProtocolTypes` already byte-compares, so the two gates cannot
  disagree about what the wire is. (Hashing the committed `protocol.gen.ts` would be equivalent and
  simpler, but would make a `poker-server` test read a `web-client` source file and would re-admit
  walk order. The declarations are the source; the file is their projection.)
- Truncation to 16 hex characters is deliberate — this is a collision check between two branches,
  not a security boundary, and a 64-character column is a document nobody reads.

### 3. What the gate asserts

One JUnit test, `ProtocolVersionLedgerTest`, in `poker-server`'s test source set beside
`ProtocolDocumentationTest`, locating the repository root the same way that test already does:

1. The table has at least one row, and every table row parses.
2. Versions ascend by exactly one from row to row — which makes a duplicate number impossible to
   express.
3. The **last** row's version equals `PROTOCOL_VERSION`.
4. The last row's fingerprint equals the fingerprint computed from the live descriptors.

Rules 3 and 4 are what make the mechanism sound rather than lucky: the textual conflict is the
alarm, and these are what fail **every wrong way of silencing it**. Keep both rows for `3` — rule 2
fails. Keep the other branch's row and drop yours — rule 4 fails, because the merged tree's wire is
neither branch's. Renumber to `4` and forget the constant — rule 3 fails. Move the wire with no bump
at all — rule 4 fails, and `ADR-0028` §8 is enforced by a test for the first time.

### 4. Where the gate lives, and what it does not add

In `:poker-server:test`, therefore in `:poker-server:check`, therefore in the `check` CI job, which
is a required check on `develop`. It adds **no** new CI job, **no** new Gradle task, **no** network
access and **no** production code — the running server is unchanged, and the whole mechanism is one
document plus one test. It runs offline, in a worktree, on a shallow clone, identically.

### 5. There is deliberately no task that writes the row

No `updateProtocolLedger`. A command that regenerates the ledger is a command that overwrites
another branch's claim without reading it, which is the defect wearing a fix's clothes. The row is
written **by hand**, and the test's failure message supplies exactly what to write:

```
docs/protocol-versions.md does not match the wire.
  PROTOCOL_VERSION is 4; the last ledger row claims 3.
  Append this row, then re-run:
      | 4 | `a1b2c3d4e5f60718` | STORY-XXXX | 2026-08-20 |
  If another version already claims your number, rebase on develop and take the next free
  one (ADR-0045 §4).
```

The friction is the point, and it is bounded to one paste.

### 6. What a bump costs, and what `ADR-0045` §4 now reads as

> **Amended by [`ADR-0068`](ADR-0068-an-atomic-ticket-names-the-gate-that-forbids-splitting-it.md) §5 on 2026-08-23.** The *"five artifacts"* below is wrong — the first bump after the web client and this ledger both existed carries **twelve**, and the set had already grown from `ADR-0045` §4's four. `ADR-0068` replaces the count with a procedure, so that the next gate added cannot make it stale again. The paragraph is left as written: an ADR records what was decided when it was decided.

The bump is still the last ticket of its story, still rebased on `develop` immediately before it,
still one commit. That commit now carries **five** artifacts rather than four: `PROTOCOL_VERSION`,
`docs/protocol.md`'s version line, the new message rows, a regenerated `protocol.gen.ts`, and **one
new ledger row**. `web-client/src/protocol/version.ts` moves with it as `ADR-0020` already requires.

The number is still never written down in advance, and neither is the fingerprint: no ADR, story or
ticket names either. The ledger records a claim at the moment it is made, which is the only moment
either value is true.

### 7. What this does not decide

When a bump is needed (`ADR-0028` §8), which stories bump (`ADR-0044`, `ADR-0045`), the order they
land in (`ADR-0045` §3 — still the plan; this makes breaking it loud rather than automatic), and the
integer itself. `PROTOCOL_VERSION` keeps the value `develop` has today.

## Consequences

**What it buys.**

- The second branch fails **at merge time, by git**, on every path — a rebase stops with conflict
  markers, and a GitHub merge or squash-merge is refused before a check is consulted. Verified, not
  assumed: the same three-way merge that takes the constant silently produces
  ```
  <<<<<<< ours
  | 3 | `cccccccccccccccc` | STORY-0214 | … |
  =======
  | 3 | `bbbbbbbbbbbbbbbb` | STORY-0213 | … |
  >>>>>>> theirs
  ```
  and exits 1. The conflict text *is* the diagnosis: two stories claimed 3.
- No dependence on a human noticing, on a re-run, on a setting, or on an agent having read three
  epics' queues.
- A wire change with **no** bump now fails too. That defect was never even named; it comes free.
- The protocol gains a history. `git log docs/protocol-versions.md` answers *"what did version 2
  mean?"* — which nothing in the repository could answer before.

**What it costs.**

- **Every bump costs one hand-written line and one deliberate red-then-green cycle.** The story that
  needs a bump must: rebase, move the constant, regenerate `protocol.gen.ts`, move
  `docs/protocol.md`'s version line, run the test *expecting it to fail*, paste the fingerprint from
  the failure into a new row, re-run. The fingerprint cannot be known before the run, and cannot be
  generated on purpose — see §5. That is roughly five minutes and one more file in the bump commit,
  charged to the ticket that bumps and to no other.
- **Every wire-shape change now costs a version bump, with no escape hatch.** Adding a field to a
  `ServerMessage` inside a story that was not planning a bump now fails `check` and forces one.
  That is `ADR-0028` §8 exactly, but the cost lands on stories whose authors have never read it, and
  the failure will read as an obstacle rather than as a rule. There is no exception mechanism, and
  adding one later would mean a new ADR.
- **The fingerprint is over-sensitive in one direction.** Reordering a message's fields is not a
  wire change — JSON decoding is order-insensitive — but it moves the fingerprint and so demands a
  bump. The remedy is not to reorder, or to accept a free number; it is not a silent failure, and
  version numbers are cheap while nothing is deployed.
- **Only the last row is verifiable.** Historical fingerprints cannot be recomputed, because the
  shapes they name are gone. An edited old row passes forever. The ledger's past is guarded by
  review, not by a test, and this ADR says so rather than implying otherwise.
- **The lock's resolution is the TypeScript projection.** `Int`, `Long`, `Byte` and `Double` all
  project to `number`, so a change among them is invisible to the fingerprint, and two branches
  differing only in ways `protocol.gen.ts` cannot show would write byte-identical rows and merge
  clean. Bounded, and bounded honestly: what escapes this lock is exactly what already escapes
  `verifyProtocolTypes`, which is this repository's operative definition of the wire.
- **Conflict resolution becomes a routine event on a bump branch**, and it is the one moment an
  agent can confidently do the wrong thing — the conflicting hunk is two lines, and taking one looks
  like a resolution. §3's rules 3 and 4 are the whole reason that is survivable, and they are load
  bearing rather than belt-and-braces.
- **One more artifact for a planner to remember** when writing a wire story, and one more file in
  the bump's blast radius.

**What it forecloses.** Generating the ledger, deliberately and permanently. Making
`PROTOCOL_VERSION` a value derived from the ledger without a further ADR (see the alternatives).
And silent unversioned wire changes — including any that would have been legitimate, of which none
is known today.

**Why this shape when the evidence is thin.** It is the cheapest of the three to reverse and the
only one that leaves no residue outside the repository. Deleting one document and one test restores
today exactly, with nothing to remember and nothing to un-set in a settings page; the ledger's rows
survive as history even if the gate is removed. A branch-protection change is reversible only by
someone who knows it was made, which — for a project whose second deliverable is the trail — is the
expensive kind of reversible.

## Alternatives considered

**A CI check comparing `PROTOCOL_VERSION` against `origin/develop`.** The strongest case, and the
one `ADR-0045` itself nominated: it states the rule directly — *your version is `develop`'s plus
one* — adds no checked-in artifact, needs no hand-written hash, cannot be forgotten by a story, and
costs a bump nothing at all. Rejected on two independent grounds, either of which is sufficient.
First, it is not sound without `strict = true`: GitHub does not re-run a pull request's checks when
the base moves, so branch B's check, computed when `3` was free, stays green after A takes `3` and
the merge proceeds. The mechanism therefore *is* the branch-protection alternative, with extra code.
Second, it cannot run in `./gradlew check`: an agent worktree or a shallow clone may not have
`origin/develop` at all, so the check must either fail for everyone or skip when it cannot resolve a
base — and a gate that skips silently is worse than no gate, because it is believed. It would be the
only impure gate in a set whose purity is what makes them trustworthy.

**Branch protection: require branches to be up to date (`strict = true`).** Its case is genuinely
strong: no code, no artifact, no fingerprint, and it catches every stale-base defect rather than
this one — it is precisely the mechanism GitHub built for this. Rejected because `TASK-000102`
already took this decision, with a measured reason, in the other direction: *"true would force a
rebase and a full CI re-run on every PR in a sequential chain, doubling wall-clock for no safety
gained."* This project's runs *are* sequential chains of pull requests against a moving `develop`,
so the trade is a permanent tax on every PR — the overwhelming majority of which will never touch
the wire — against one rare defect. It is also invisible: it lives on a settings page no reader of
this repository can see, `TASK-000102` records how little of GitHub's settings this project could
rely on before it went public, and the trail is Product B. The ledger charges the cost to the PRs
that bump and to nothing else.

**A `.gitattributes` merge driver that always conflicts on `Protocol.kt`.** Case: no new document,
no test, no fingerprint, and it fails at exactly the right moment. Rejected because `.gitattributes`
can *name* a custom merge driver but cannot *supply* it — the driver must be defined in each clone's
`.git/config`, so it does nothing in a fresh clone, nothing in an agent worktree that was never set
up, and nothing whatsoever in GitHub's server-side merge, which is where most of these merges
happen. It would also conflict on every unrelated edit to that file.

**A reservation table with no fingerprint** — version to story, nothing else. Case: the same textual
conflict, no hash, no red-then-green cycle, trivially readable and writable by hand. Rejected
because it holds only half the property. It makes a second *claim* conflict, but records nothing
about what the number means, so the wrong conflict resolution — keep theirs, drop mine — passes
green, and a wire change with no bump at all still passes. The fingerprint is what turns an alarm
into a proof, and the alarm without the proof is what `ADR-0045` §3 already has in prose.

**Derive `PROTOCOL_VERSION` from the ledger.** Genuinely better in principle: one source of truth
beats two kept in sync by a test, and the entire class of *"the constant moved and the ledger did
not"* disappears with rule 3. Rejected on cost and blast radius — it needs build-time code
generation and a generated Kotlin source on the server's compile path for the sake of one integer,
and `TASK-000104`'s brief is a gate that names the constant and does not change it. If the ledger
earns its keep over the three queued bumps, this is the natural successor ADR, and nothing here
forecloses it beyond requiring one.

**Do nothing: keep `ADR-0045` §3's rule and the rebase.** The honest case: the rule is written, the
three stories are ordered, no client is deployed, a wrong version costs a `VERSION_MISMATCH` in
development and nothing in production, and every mechanism above buys insurance against a defect
that discipline has so far prevented. Rejected because the defect is silent by construction — it
produces no red anywhere until a real client meets a real server — and because *"at most one
bumping branch open at a time, across three epics"* is a serialization no agent can see from inside
its own ticket, in a project that deliberately forbids surveying the repository. A rule whose
enforcement requires holding three epics in mind is not a lock; it is a hope with a citation.
