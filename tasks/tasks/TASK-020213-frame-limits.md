---
schema: 2
id: TASK-020213
title: A frame that is too large or too deeply nested is refused before it is parsed
type: task
status: backlog
parent: STORY-0202
module: poker-server
estimate: S
tier: sonnet
review: deep
files_touched: 3
labels: [server, protocol, robustness, security]
depends_on: [TASK-020208]
verify:
  - ./gradlew :poker-server:test --tests '*FrameLimitTest'
  - ./gradlew :poker-server:test --tests '*ProtocolCodecTest'
  - ./gradlew :poker-server:check
---

## Goal

`decodeClient` turns every malformed frame into a `Refused` value rather than an exception — except
one. A frame of deeply nested JSON (thousands of `[`) makes `parseToJsonElement` recurse until it
throws `StackOverflowError`, which escapes the codec's deliberately narrow
`catch (IllegalArgumentException)`.

Found by fuzzing during the `TASK-020208` review.

## Why it is not a codec bug

The codec is right to catch narrowly. A `StackOverflowError` is an `Error`, not a protocol problem,
and widening the catch to `Throwable` would mean swallowing genuine bugs — an `OutOfMemoryError`
reported as "bad frame" is worse than a crash, because it is silent.

The defect is that **nothing rejects the frame before it reaches the parser.** A limit is a
connection-layer concern, which is why this belongs to `STORY-0205` rather than to the codec.

It matters because the server will face the internet, and this is a trivially cheap denial of
service: a few kilobytes of `[` costs a client nothing and takes down whatever thread parses it.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/config/ServerConfig.kt` | modify |
| `poker-server/src/main/kotlin/duels/poker/server/protocol/ProtocolCodec.kt` | modify |
| `poker-server/src/test/kotlin/duels/poker/server/protocol/FrameLimitTest.kt` | create |

Read `docs/adr/ADR-0013-disconnect-grace-period.md` for the config precedent. Do not widen
`decodeClient`'s existing catch.

## Scope

- Two tunables on `ServerConfig`, in the typed home with everything else — a maximum frame length in
  bytes and a maximum nesting depth. Both need defaults and both must be overridable by environment
  variable, following the existing precedence (env → `application.conf` → default).
- Refuse an over-long frame **before** parsing, by length. Cheapest possible check, and it catches
  the overwhelming majority.
- Refuse an over-deep frame before recursion can exhaust the stack. Scanning the raw text for
  bracket depth is acceptable and avoids parsing untrusted input to find out whether it is safe to
  parse; say in a comment why the check precedes the parse.
- A refused frame produces the same `Refused` value shape as any other bad frame, with a
  `ProtocolError` code. Add a code if none of the seven fits, rather than reusing a misleading one.

## Tests

| Name | Asserts |
| --- | --- |
| `aFrameLongerThanTheLimitIsRefused` | refused by length, without parsing |
| `aFrameDeeperThanTheLimitIsRefused` | refused by depth |
| `theNestingBombNoLongerEscapes` | the exact input that produced `StackOverflowError` now returns `Refused` — **this is the regression, pin it** |
| `aFrameAtExactlyTheLimitIsAccepted` | the boundary is inclusive as documented, so the limit is not off by one |
| `anOrdinaryFrameIsUnaffected` | `ProtocolCodecTest`'s existing cases still decode |

The nesting-bomb test must construct the input programmatically and state the depth, so it stays
meaningful if the limit changes.

## Done

All three `verify:` commands exit 0, `ProtocolCodecTest` passes unedited, and a nesting bomb that
previously threw now returns a `Refused`.
