---
schema: 2
id: TASK-000104
title: A second branch cannot claim the same PROTOCOL_VERSION
type: task
status: blocked
parent: STORY-0001
module: poker-server
estimate: S
tier: sonnet
review: standard
files_touched: 3
labels: [process, ci, protocol]
depends_on: []
verify:
  - ./gradlew :poker-server:test --tests '*ProtocolDocumentationTest'
  - ./gradlew :poker-server:check
  - python3 .github/scripts/lint_tickets.py
---

## Blocked on `DEC-040`

**Do not start this before the ADR answering `DEC-040` is merged.** The requirement below is
settled; the mechanism is not, and the mechanism is most of the work. The third `verify` command —
the one that runs the new gate — is written **by the ADR**, and this block is completed then.

## Goal

Two branches cannot both move `PROTOCOL_VERSION` to the same number and both merge green. The lock
`ADR-0045` §3 states in prose becomes a check that fails the build.

## Why

`ADR-0045` §3 accepted this as a known cost, in its own words:

> Three stories in three epics share one lock, and nothing in CI holds it. `STORY-0213`,
> `STORY-0214` and `STORY-0405` must land one at a time. The guard is §3's rule plus a rebase; two
> branches both claiming 3 merge clean and green, and the defect surfaces only when a real client
> meets a real server. Enforcing it mechanically — a check comparing `PROTOCOL_VERSION` against
> `origin/develop` — is addable, is not built here, and would be its own decision.

The failure is silent by construction, and every existing gate looks the other way:

- A three-way merge sees the **same** edit (`2` → `3`) on both sides and takes it without a
  conflict. A rebase sees the patch as already applied and drops it.
- `ProtocolDocumentationTest.theDocumentStatesTheCurrentProtocolVersion` compares the document to
  the constant. Both branches moved both, so it passes on either, and on the merge of the two.
- `:poker-server:verifyProtocolTypes` byte-compares `protocol.gen.ts` against what the emitter
  writes from the descriptors on **that** branch — it cannot see a version the other branch spent.
- `protocolJson` sets `ignoreUnknownKeys = false` and the handshake compares versions for exact
  equality, so version equality is the protocol's **only** compatibility mechanism.

The result is one integer naming two wire shapes, arrived at with every gate green — precisely what
`ADR-0028` §8 forbids. Three bumps are unlanded today (`STORY-0213`, `STORY-0214`, `STORY-0405`) and
each claims "the next number free", so this is live, not hypothetical.

## What is settled, and is not `DEC-040`'s to reopen

- **One number names exactly one wire shape** — `ADR-0028` §8, restated by `ADR-0045` §4.
- **The bump is the last ticket of its story**, rebased on `develop` immediately before it, moving
  the constant, `docs/protocol.md`'s version line, the message rows and `protocol.gen.ts` in one
  commit — `ADR-0045` §4. This ticket enforces that rule; it does not replace it.
- **No ADR, story or ticket names the integer.** Whatever the gate records, it is not a number
  written into a document in advance.

## What `DEC-040` decides

The mechanism, and it is a real choice with different costs:

- a check that compares `PROTOCOL_VERSION` against `origin/develop` in CI — needs a fetch, needs a
  base to compare with, and is a `pull_request` check that must be re-run after the base moves;
- a checked-in ledger mapping each version to a fingerprint of the wire, so two branches claiming
  one number **conflict textually** and a mismatch fails an ordinary test — no git, no network, in
  the shape `ProtocolDocumentationTest` and `verifyProtocolTypes` already set;
- a branch-protection setting that forces a re-check against the moved base — no code, but it lives
  outside the repository and `TASK-000102` records how little of that this project can rely on;
- something else.

They differ in where the gate lives, what a bump costs a future story, and whether the rule is
visible in the repository or in GitHub's settings. Two competent engineers would not land in the
same place, which is what makes it the architect's.

## Files

Three at most, and which three is the ADR's to say. Whatever the mechanism, it names the constant in
`poker-server/src/main/kotlin/duels/poker/server/protocol/Protocol.kt` and must not change it.

| File | Action |
| --- | --- |
| the gate itself | create |
| the gate's own test, or its CI job | create |
| `docs/protocol.md` | modify — the evolution note says how the lock is held |

## Scope

- One gate, holding one property: **a change that moves `PROTOCOL_VERSION` to a number another
  landed change already used fails the build.**
- It runs where the other protocol gates run, on every ordinary check, so no story has to remember
  it.
- It says, in its failure message, what to do: rebase on `develop` and take the next free number
  (`ADR-0045` §4).

## Out of scope

- Deciding when a bump is *needed*. `ADR-0028` §8 owns that and is not reopened.
- Serialising the three stories on the board. `ADR-0045` §3 already orders them; this makes the
  order enforceable, not automatic.
- Any change to the wire, the handshake, or `PROTOCOL_VERSION`'s current value.

## Acceptance criteria

Mechanism-independent, and each is a reproduction rather than an opinion:

- [ ] With `PROTOCOL_VERSION` moved by hand to a value the recorded wire does not name, the gate's
      command exits **non-zero**. Quote the failure in the PR.
- [ ] With the version and the wire moved together, the same command exits 0.
- [ ] The two-branch reproduction, run by hand and written into the PR body: branch A and branch B
      both cut from the same `develop`, both moving `2` → `3` with **different** wire changes; A
      merges; B then fails.
- [ ] `docs/protocol.md`'s evolution note names the gate and says what a bump costs.
- [ ] `./gradlew :poker-server:check` passes, and `PROTOCOL_VERSION` is the value `develop` had.
- [ ] Every command in `verify:` exits 0 — including the gate's own, added to that block by the
      implementer once `DEC-040` is answered.

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
