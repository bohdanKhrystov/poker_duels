---
schema: 2
id: TASK-030303
title: The frame codec decodes only what the generated union names
type: task
status: done
parent: STORY-0303
module: web-client
estimate: S
tier: sonnet
review: standard
files_touched: 2
labels: [client, protocol]
depends_on: [TASK-030302]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test 2>&1 | grep -qE 'Tests +27 passed \(27\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'knows exactly the variants the generated union declares'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'drops a frame whose type the union does not name'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'drops a frame that is not JSON'
  - grep -qF 'satisfies Record<ServerMessage["type"], true>' web-client/src/protocol/frames.ts
  - cd web-client && npm run check
  - ./gradlew :poker-server:verifyProtocolTypes
---

## Goal

One function turns a raw frame into a `ServerMessage` or into `null`, and the set of discriminators
it accepts is proven — at compile time and in a test — to be exactly the generated union.

## Files

`files_touched` counts the create/modify rows only.

| File | Action |
| --- | --- |
| `web-client/src/protocol/frames.ts` | create |
| `web-client/src/protocol/frames.test.ts` | create |
| `web-client/src/protocol/protocol.gen.ts` | read — lines 4–6 and 64 only: `ProtocolVersion`, `ClientMessage`, `ServerMessage`. **Never edited.** Its bytes belong to the Kotlin emitter (`ADR-0026`) |

## Scope

- `frames.ts` exports exactly three things:

  ```ts
  import type { ClientMessage, ServerMessage } from "./protocol.gen";

  // Keyed by discriminator so `satisfies` makes the compiler prove the set is the
  // union: a missing key is TS2739, an extra key is TS2353. When `ADR-0028` adds
  // `OpponentPresence` and `ActedForAbsentSeat` to `ServerMessage`, `tsc` fails
  // here until they are added — which is the cheap, reviewed edit that change wants.
  const SERVER_MESSAGE_TABLE = {
    DuelFinished: true,
    Events: true,
    Failure: true,
    Rejected: true,
    RoomJoined: true,
    Snapshot: true,
    Welcome: true,
    YourTurn: true,
  } satisfies Record<ServerMessage["type"], true>;

  /** Every discriminator this client can decode, sorted. */
  export const SERVER_MESSAGE_TYPES: readonly string[] =
    Object.keys(SERVER_MESSAGE_TABLE).sort();

  /** One outbound frame, as the server's `protocolJson` will read it. */
  export function encodeClientMessage(message: ClientMessage): string;

  /** One inbound frame, or `null` if this client cannot read it. */
  export function decodeServerMessage(data: unknown): ServerMessage | null;
  ```

- `decodeServerMessage` returns `null`, never throws, for each of: `data` that is not a string
  (a binary frame), text that is not JSON, JSON that is not an object (`"3"`, `'"Welcome"'`,
  `"[]"` — `Array.isArray` is a separate check from `typeof x === "object"`), an object with no
  `type` or a non-string `type`, and a `type` that `SERVER_MESSAGE_TYPES` does not contain.
- Narrowing is by the discriminator and nothing else: the last line is
  `return parsed as ServerMessage`. This is deliberate. The server writes every field
  (`encodeDefaults = true`, `ADR-0020`) and is authoritative, so a structural validator here would
  be a second, hand-written mirror of the schema — the exact artefact `ADR-0020` exists to prevent.
  Say so in a comment on that line.
- `encodeClientMessage` is `JSON.stringify(message)`. It exists so that no other file in the client
  ever serialises a wire message, which is what makes the boundary guard's rule meaningful.
- Run `npm run format` before committing.

## Out of scope

- Validating fields, lengths or ranges. See above.
- Any `switch` over `ServerMessage`. Nothing here dispatches; nothing here is exhaustive over
  variants at runtime. Two variants are arriving (`ADR-0028`) and a decoder written as though the
  protocol were closed would have to change shape when they do; this one changes by one line.
- Logging. The caller logs — `TASK-030308`.
- The version. `TASK-030302` owns the constant, `TASK-030310` owns the mismatch.

## Tests

`web-client/src/protocol/frames.test.ts`, describe block `"the frame codec"`. Seven `it` blocks:

| Test | Proves |
| --- | --- |
| `decodes a frame the union names` | `decodeServerMessage('{"type":"Welcome","deviceId":"d-1","protocolVersion":2}')` is an object whose `type` is `"Welcome"` |
| `drops a frame that is not JSON` | `decodeServerMessage("not json at all")` is `null` |
| `drops a frame that is not an object` | `decodeServerMessage('"Welcome"')`, `decodeServerMessage("[]")` and `decodeServerMessage("42")` are each `null` |
| `drops a frame with no type` | `decodeServerMessage('{"deviceId":"d-1"}')` is `null` |
| `drops a frame whose type the union does not name` | `decodeServerMessage('{"type":"Nonsense"}')` is `null` |
| `drops a frame that is not text` | `decodeServerMessage(new ArrayBuffer(4))` is `null` |
| `knows exactly the variants the generated union declares` | parsing `protocol.gen.ts` for `/^export type ServerMessage = (.+);$/m`, splitting on `\|` and trimming, gives exactly `SERVER_MESSAGE_TYPES` |

Seven tests. Twenty exist, so the suite reports **27**.

The last test must `throw` if the regex finds no match, so a reshaped generated file is a failure
rather than a skipped assertion. It compares *type names* to *discriminators*, which is sound
because `ADR-0020` derives both from the same `@SerialName`; say that in a comment.

## Proof

| Command | Proves |
| --- | --- |
| `Tests 27 passed (27)` | the seven tests ran and nothing earlier was displaced |
| the three `--reporter=verbose` greps | the drop rules and the union coupling exist by name |
| `grep 'satisfies Record<ServerMessage["type"], true>'` | the compile-time half of the coupling is in the source. `esbuild` strips types without checking them, so no test can observe it — only `npm run typecheck` and this grep can |
| `npm run check` | typecheck, lint, format and test all pass together |

**Name the edit that makes each assertion red:**

1. Delete the `Rejected: true` line from `SERVER_MESSAGE_TABLE` →
   `knows exactly the variants the generated union declares` fails, naming `Rejected`, **and**
   `npm run typecheck` fails with TS2739. Two independent reds from one edit. Restore.
2. Change `decodeServerMessage` to return the parsed object whenever it is an object →
   `drops a frame whose type the union does not name` and `drops a frame with no type` both fail.
   Revert.

Quote both failures in the PR.

**A note for whoever lands `ADR-0028`.** `drops a frame whose type the union does not name` uses
`"Nonsense"`, not `OpponentPresence`, on purpose: a test that pins a *future* variant as
undecodable would have to be deleted rather than merely extended. When `OpponentPresence` and
`ActedForAbsentSeat` arrive, this ticket's cost is two lines in `SERVER_MESSAGE_TABLE` and nothing
else — `tsc` will say so.

## Acceptance criteria

- [ ] `the frame codec > decodes a frame the union names` passes
- [ ] `the frame codec > drops a frame that is not JSON` passes
- [ ] `the frame codec > drops a frame that is not an object` passes
- [ ] `the frame codec > drops a frame with no type` passes
- [ ] `the frame codec > drops a frame whose type the union does not name` passes
- [ ] `the frame codec > drops a frame that is not text` passes
- [ ] `the frame codec > knows exactly the variants the generated union declares` passes
- [ ] `npm run --silent test` reports `Tests  27 passed (27)`
- [ ] `frames.ts` contains no `switch` and no `any`
- [ ] No byte is added to `src/protocol/protocol.gen.ts`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
