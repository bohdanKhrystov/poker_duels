---
schema: 2
id: TASK-030302
title: The protocol version the client sends is typed against the generated alias
type: task
status: done
parent: STORY-0303
module: web-client
estimate: XS
tier: haiku
review: light
files_touched: 2
labels: [client, protocol]
depends_on: [TASK-030301]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test 2>&1 | grep -qE 'Tests +20 passed \(20\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'is the version the generated alias declares'
  - grep -qF 'ProtocolVersion' web-client/src/protocol/version.ts
  - cd web-client && npm run check
  - ./gradlew :poker-server:verifyProtocolTypes
---

## Goal

The client has one `PROTOCOL_VERSION` constant, typed `ProtocolVersion`, so a server-side version
bump fails `tsc` here instead of failing a handshake in someone's browser.

## Files

`files_touched` counts the create/modify rows only.

| File | Action |
| --- | --- |
| `web-client/src/protocol/version.ts` | create |
| `web-client/src/protocol/version.test.ts` | create |

## Scope

- `version.ts` is exactly this, plus KDoc-style comment:

  ```ts
  import type { ProtocolVersion } from "./protocol.gen";

  /**
   * The wire version this client speaks, sent on every `Hello`.
   *
   * Typed against the generated alias on purpose: when the server bumps
   * `PROTOCOL_VERSION`, the alias becomes a different literal and this line stops
   * compiling. That is the whole reason `ADR-0020` emits the alias — a stale
   * version must fail the build, not the handshake.
   */
  export const PROTOCOL_VERSION: ProtocolVersion = 2;
  ```

- The type import must be `import type`, per `verbatimModuleSyntax`.
- Nothing else lives in this file. It is imported by `TASK-030307`'s handshake.

## Out of scope

- Sending it. `TASK-030307` builds the `Hello` frame.
- Reacting to a mismatch. `TASK-030310` owns `VERSION_MISMATCH`.
- Anticipating the bump. `ADR-0027` (`STORY-0405`) and `ADR-0028` both move the version, and
  whichever lands first takes 3 while the second takes 4. Neither has landed; the client tracks the
  generated alias and moves when it moves. Do not write `2 | 3`, do not widen the type, do not add a
  compatibility branch.

## Tests

`web-client/src/protocol/version.test.ts`, describe block `"the protocol version"`. One `it` block:

| Test | Proves |
| --- | --- |
| `is the version the generated alias declares` | reading `protocol.gen.ts` and matching `/^export type ProtocolVersion = (\d+);$/m` yields a number equal to `PROTOCOL_VERSION` |

Throw explicitly if the regex does not match, so a rename of the alias is a failure and not a
silently skipped assertion.

One test. Nineteen exist, so the suite reports **20**.

## Proof

| Command | Proves |
| --- | --- |
| `Tests 20 passed (20)` | the test ran and nothing earlier was displaced |
| the `--reporter=verbose` grep | the test exists by the name the acceptance criterion uses |
| `grep 'ProtocolVersion' version.ts` | the constant is typed against the generated alias rather than being a bare `number` |

**Name the edit that makes it red:** change `2` to `3` in `version.ts`. Two things then go wrong and
both must be reported in the PR:

1. `npm run typecheck` fails with `Type '3' is not assignable to type 'ProtocolVersion'`. Quote the
   exact message.
2. `is the version the generated alias declares` fails, saying `expected 3 to be 2`.

Revert. The first is the guarantee this ticket exists for; the second is what makes it legible in a
test report. Note that (1) is only observable by experiment — `tsc` cannot be made to fail on a file
that is correct, so the permanent assertion is (2).

## Acceptance criteria

- [ ] `the protocol version > is the version the generated alias declares` passes
- [ ] `npm run --silent test` reports `Tests  20 passed (20)`
- [ ] `version.ts` declares `PROTOCOL_VERSION` with the type annotation `ProtocolVersion`
- [ ] The PR quotes both failure messages from the Proof section
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
