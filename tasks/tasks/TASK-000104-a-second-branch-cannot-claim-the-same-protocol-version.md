---
schema: 2
id: TASK-000104
title: A second branch cannot claim the same PROTOCOL_VERSION
type: task
status: ready
parent: STORY-0001
module: poker-server
estimate: S
tier: sonnet
review: standard
files_touched: 3
labels: [process, ci, protocol]
depends_on: []
verify:
  - ./gradlew :poker-server:test --tests '*ProtocolVersionLedgerTest'
  - ./gradlew :poker-server:test --tests '*ProtocolDocumentationTest'
  - ./gradlew :poker-server:check
  - python3 .github/scripts/lint_tickets.py
---

## Goal

Two branches cannot both move `PROTOCOL_VERSION` to the same number and both merge green. The lock
[`ADR-0045`](../../docs/adr/ADR-0045-presence-belongs-to-the-table.md) §3 states in prose becomes a
conflict git refuses, backed by a test that fails every wrong way of resolving it.

The mechanism is decided:
[`ADR-0047`](../../docs/adr/ADR-0047-a-protocol-version-is-claimed-in-a-ledger.md), answering
`DEC-040`. **Read `ADR-0047` §§1–5 before starting** — this ticket implements it and adds nothing to
it.

## Why

Version equality is the protocol's only compatibility mechanism: `ignoreUnknownKeys = false`, exact
equality in the handshake, `VERSION_MISMATCH` terminal. `ADR-0028` §8's rule that one number names
one wire shape is the assumption the handshake is built on, and nothing enforces it.

The failure is silent by construction, and every existing gate looks the other way:

- A three-way merge sees the **same** edit (`2` → `3`) on both sides and takes it without a
  conflict. A rebase sees the patch as already applied and drops it.
- `ProtocolDocumentationTest.theDocumentStatesTheCurrentProtocolVersion` compares the document to
  the constant. Both branches moved both, so it passes on either, and on the merge of the two.
- `:poker-server:verifyProtocolTypes` byte-compares `protocol.gen.ts` against what the descriptors
  on **that tree** emit — it cannot see a number another branch spent.

Three bumps are unlanded today (`STORY-0213`, `STORY-0214`, `STORY-0405`) and each claims "the next
number free", so this is live, not hypothetical.

## What to build

Three files, exactly as `ADR-0047` specifies.

### 1. `docs/protocol-versions.md` — the ledger

A short preamble and a single markdown table, newest row last:

```markdown
| Version | Wire fingerprint | Claimed by | Landed |
| --- | --- | --- | --- |
| 2 | `<from the failing test>` | STORY-0202 | 2026-08-12 |
```

- The preamble says in one or two lines what the file is, that a row is written by hand, and that
  the ledger starts at **2** because it was introduced after version 1's shape was replaced and that
  shape is not recoverable. Link `ADR-0047`.
- Do **not** invent the fingerprint. Write the row with a placeholder, run the test, and paste the
  value from its failure message.

### 2. `ProtocolVersionLedgerTest` — the gate

`poker-server/src/test/kotlin/duels/poker/server/protocol/ProtocolVersionLedgerTest.kt`, beside
`ProtocolDocumentationTest`, whose repository-root walk
(`generateSequence(File("").absoluteFile) { it.parentFile }`) it copies.

The fingerprint, per `ADR-0047` §2 — computed in this test, not in `src/main`:

> the first **16 hexadecimal characters** of the SHA-256 of the `text` of every
> `TypeScriptDeclaration` returned by `protocolDeclarations()`, **sorted by declaration name** and
> joined with `\n`.

`protocolDeclarations()` is `internal` in `duels.poker.server.protocol.typescript` and visible to
this module's test source set — `ProtocolTypeScriptTest` already calls it. The generated file's
header and its `export type ProtocolVersion = N;` line are **excluded**: the fingerprint names the
shape, not the number.

Four assertions, one test each:

1. The table has at least one row, and **every** table row matches
   `` ^\| (\d+) \| `([0-9a-f]{16})` \| `` — a row that does not parse is a failure, never a row
   silently skipped.
2. Versions ascend by exactly one from row to row.
3. The last row's version equals `PROTOCOL_VERSION`.
4. The last row's fingerprint equals the computed fingerprint.

The failure message of 3 and 4 must print the exact row to append, and say what to do when the
number is taken. `ADR-0047` §5 gives the wording; match its shape:

```
docs/protocol-versions.md does not match the wire.
  PROTOCOL_VERSION is 4; the last ledger row claims 3.
  Append this row, then re-run:
      | 4 | `a1b2c3d4e5f60718` | STORY-XXXX | 2026-08-20 |
  If another version already claims your number, rebase on develop and take the next free
  one (ADR-0045 §4).
```

### 3. `docs/protocol.md` — the evolution note

Extend the **Protocol evolution** bullet: the ledger is where a version is claimed, a bump appends a
row, and a bump commit now carries **five** artifacts — `PROTOCOL_VERSION`, this document's version
line, the new message rows, a regenerated `protocol.gen.ts`, and one ledger row. Link
`docs/protocol-versions.md` and `ADR-0047`.

## Scope

- One gate, holding one property: **a change that moves `PROTOCOL_VERSION` to a number another
  landed change already used fails — at merge, by git, before any check runs.**
- It runs where the other protocol gates run, on every `:poker-server:check`, so no story has to
  remember it. No new CI job, no new Gradle task, no network, no `src/main` change.

## Out of scope

- Deciding when a bump is *needed*. `ADR-0028` §8 owns that and is not reopened.
- Serialising the three stories on the board. `ADR-0045` §3 already orders them; this makes breaking
  the order loud, not impossible.
- Any change to the wire, the handshake, or `PROTOCOL_VERSION`'s current value — it stays **2**.
- A task or script that writes the ledger row. `ADR-0047` §5 forbids one, on purpose: a command that
  regenerates the ledger is a command that overwrites another branch's claim without reading it.

## Acceptance criteria

Each is a reproduction rather than an opinion:

- [ ] With `PROTOCOL_VERSION` moved by hand to a value the ledger's last row does not name, the
      gate exits **non-zero**, and the message names the row to append. Quote it in the PR.
- [ ] With a field added to any `ServerMessage` and no version bump, the gate exits **non-zero** —
      `ADR-0028` §8 is now executable.
- [ ] With the ledger, the constant and the wire agreeing, the gate exits 0.
- [ ] A row that does not parse — a truncated fingerprint, a missing backtick — fails rather than
      being skipped.
- [ ] **The two-branch reproduction, run by hand and written into the PR body.** Branches A and B
      cut from the same base, each appending a row for the same version with a *different*
      fingerprint; A lands; B's merge or rebase is refused with both claims in the conflict markers.
      `git merge-file -p ledger.b.md ledger.base.md ledger.a.md` reproduces it in one command, and
      the same command against `Protocol.kt` exits 0 — show both.
- [ ] `docs/protocol.md`'s evolution note names the ledger and says a bump commit carries five
      artifacts.
- [ ] `./gradlew :poker-server:check` passes and `PROTOCOL_VERSION` is still `2`.
- [ ] Every command in `verify:` exits 0.

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
