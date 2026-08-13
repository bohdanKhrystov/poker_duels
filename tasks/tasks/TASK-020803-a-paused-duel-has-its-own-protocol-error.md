---
schema: 2
id: TASK-020803
title: A paused duel has its own protocol error, and the document lists it
type: task
status: done
parent: STORY-0208
module: poker-server
estimate: XS
tier: haiku
review: light
files_touched: 3
labels: [server, protocol, docs]
depends_on: [TASK-020802]
verify:
  - ./gradlew :poker-server:test --tests '*ProtocolDocumentationTest'
  - ./gradlew :poker-server:test --tests '*ProtocolCodecTest'
  - grep -q 'DUEL_PAUSED' poker-server/src/main/kotlin/duels/poker/server/protocol/ProtocolError.kt
  - grep -q '`DUEL_PAUSED`' docs/protocol.md
---

## Goal

`ProtocolError` names the one thing the server will shortly need to say and cannot say today: the
duel is paused because the other seat dropped, so this action was not applied.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/protocol/ProtocolError.kt` | modify |
| `docs/protocol.md` | modify |
| `poker-server/src/test/kotlin/duels/poker/server/protocol/ServerMessageHandshakeTest.kt` | modify |

## Scope

- One entry appended to the `ProtocolError` enum, with a KDoc line in the style of its neighbours:

  ```kotlin
  /** The duel is paused while the other seat is inside `ADR-0013`'s grace period. */
  DUEL_PAUSED,
  ```

  Append it after `NOT_IN_DUEL` and before `FRAME_LIMIT_EXCEEDED`, or at the end — but do not
  reorder the values that are already there.
- One bullet in the `## Protocol Errors` list in `docs/protocol.md`, in the same
  `` - `NAME`: sentence. `` shape as the eight already there. `ProtocolDocumentationTest`
  `.theDocumentListsEveryProtocolError` fails without it, which is why the document is in this
  ticket's budget rather than a later one's.
- The sentence a client reads should say what a client should do: the action was not applied, the
  duel resumes on its own when the opponent returns or when their window runs out, and nothing
  needs re-sending until then.

## Out of scope

- Sending it. `Room.act` answers a paused room with this error in `TASK-020807`; nothing constructs
  a `DUEL_PAUSED` before then.
- Any new `ClientMessage` or `ServerMessage`. Reconnect reuses `JoinRoom` (`TASK-020814`), so the
  message hierarchies do not move in this story and the protocol version does not change.
- Telling the *opponent* that their opponent dropped — that is a frame nobody has specified;
  `DEC-018`.

## Tests

No new test file. Two tests that already exist become the gate, and both are falsifiable here
because they read the enum and the document rather than a fixture:

`ProtocolDocumentationTest`

| Test | Proves |
| --- | --- |
| `theDocumentListsEveryProtocolError` | iterates `ProtocolError.entries` and fails unless `docs/protocol.md` names each one — so it goes red the moment the enum gains a value the document does not list |
| `theDocumentStatesTheCurrentProtocolVersion` | still green: adding an error value is not a wire-version change |

## Acceptance criteria

- [ ] `ProtocolDocumentationTest.theDocumentListsEveryProtocolError` passes
- [ ] `ProtocolDocumentationTest.theDocumentStatesTheCurrentProtocolVersion` passes, with
      `PROTOCOL_VERSION` unchanged
- [ ] `ProtocolCodecTest` passes unchanged
- [ ] Both greps in `verify:` find `DUEL_PAUSED`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.

---

## Widened by one file, found in CI

`ServerMessageHandshakeTest.theErrorSetIsTheDeclaredEight` hard-codes the whole list of
`ProtocolError` names and asserts it equals `ProtocolError.entries`. Adding a ninth value turns it
red. The ticket's own `verify:` block never ran it, so only CI's full `check` caught it — the same
shape of miss as `TASK-020719`, where three tests pinned a value the survey's grep could not see.

It is **not** deleted. `ProtocolDocumentationTest.theDocumentListsEveryProtocolError` proves only
that every declared error is documented; nothing proves the reverse for errors, so this test is the
only thing pinning the exact set and its order. For an enum that goes on the wire, that pin is worth
keeping: it catches an accidental addition and an accidental reordering, both of which are silent.

What comes out is the **count in the name**. `theErrorSetIsTheDeclaredEight` becomes
`theErrorSetIsExactlyWhatIsDeclared`, so the tenth error costs a list entry rather than a rename.
