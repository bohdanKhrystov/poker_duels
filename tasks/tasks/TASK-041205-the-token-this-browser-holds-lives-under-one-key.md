---
schema: 2
id: TASK-041205
title: The session token this browser holds lives under one key, and clearing it clears nothing else
type: task
status: done
parent: STORY-0412
module: web-client
estimate: XS
tier: haiku
review: standard
files_touched: 2
labels: [client, auth, storage]
depends_on: [TASK-041204]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test -- src/protocol/session-token.test.ts 2>&1 | grep -qE 'Tests +5 passed \(5\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'forgetting the token leaves the device id and the room code exactly where they were'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'reads back the token it was given, byte for byte'
  - cd web-client && npm run check
---

## Goal

There is one place a session token is written, read and forgotten, and forgetting it touches nothing
else this browser holds — which is `ADR-0030` §8's *sign-out clears the token and only the token*,
made mechanical.

## Files

| File | Action |
| --- | --- |
| `web-client/src/protocol/session-token.ts` | create |
| `web-client/src/protocol/session-token.test.ts` | create |

Read, and do not edit: `web-client/src/protocol/device-id.ts` (the shape to follow, key rule
included); `web-client/src/protocol/room-memory.ts`;
[`ADR-0030`](../../docs/adr/ADR-0030-a-claim-adds-a-credential-and-moves-nothing.md) §8;
[`ADR-0027`](../../docs/adr/ADR-0027-the-session-outranks-the-device-id.md) §2.

## Scope

- Exactly these four exports, mirroring `device-id.ts` member for member:

  ```ts
  export const SESSION_TOKEN_STORAGE_KEY = "pd.sessionToken";
  export function readSessionToken(storage: Storage): string | null;
  export function writeSessionToken(storage: Storage, token: string): void;
  export function forgetSessionToken(storage: Storage): void;
  ```

- `readSessionToken` returns `null` for an absent value **and** for one that is blank after a trim,
  and otherwise returns the stored string **verbatim** — the same rule and the same comment
  `readDeviceId` carries. A token is a bearer credential and must not be re-spelled on the way out.
- The storage is **injected**, never `localStorage` read from the module. `DEC-032` records that Node
  24+ defines an inert `localStorage` global that shadows jsdom's under Vitest, so a module that
  reaches for the global is a module whose tests do not test the browser.
- `main.tsx` passes `localStorage`, because `ADR-0027` §2 gives a session thirty absolute days and a
  thirty-day session that dies when the tab closes is not one. Say so in the KDoc; do **not** wire
  `main.tsx` here.
- One key, stated in the KDoc for `device-id.ts`'s reason: two keys would mean two sessions for one
  player.

## Out of scope

- **Reading or clearing the device id.** `ADR-0030` §8 makes the stored device id **write-once** —
  never cleared, never overwritten, not on sign-in, not on sign-out. This module must not import
  `writeDeviceId` and must not name `DEVICE_ID_STORAGE_KEY`. **A refusal, not an omission**: there
  is a test below and a criterion that greps for it.
- Sending the token anywhere. `TASK-041206` puts it in `Hello` and `TASK-041209` puts it in a header.
- Expiry, refresh, or any opinion about the thirty days. The server owns the lifetime; this browser
  holds a string.

## Tests

`web-client/src/protocol/session-token.test.ts`, describe block `"the session token this browser
holds"`. Use a small in-memory `Storage` double, as `device-id.test.ts` does.

| Test | Proves |
| --- | --- |
| `reads back the token it was given, byte for byte` | Write a token with leading and trailing content that a trim would eat — e.g. `"  tok-en  "` written, `"  tok-en  "` read back — plus a plain token. **Two values**, so the test cannot pass against a function that returns a constant, and the padded one is what pins *verbatim* |
| `answers with nothing when this browser holds none` | An empty storage reads `null` |
| `answers with nothing for a blank token` | `""` and `"   "` each read `null`, asserted separately so the failure names which |
| `forgetting the token leaves the device id and the room code exactly where they were` | A storage holding all three keys: `forgetSessionToken` removes exactly one entry, and `readDeviceId` and `readRoomCode` still answer what they did. `ADR-0030` §8's rule, asserted rather than promised |
| `writes under the one key the module names` | `writeSessionToken` puts the value under `SESSION_TOKEN_STORAGE_KEY`, asserted against the **literal** `"pd.sessionToken"` — a constant compared to itself is a tautology |

Five tests in a new file: `npm run test -- src/protocol/session-token.test.ts` reports **5**.

## Acceptance criteria

- [ ] `the session token this browser holds > reads back the token it was given, byte for byte`
      passes, asserting **two** values one of which has surrounding whitespace
- [ ] `the session token this browser holds > answers with nothing when this browser holds none`
      passes
- [ ] `the session token this browser holds > answers with nothing for a blank token` passes for both
      `""` and `"   "`
- [ ] `the session token this browser holds > forgetting the token leaves the device id and the room
      code exactly where they were` passes, asserting both survivors
- [ ] `the session token this browser holds > writes under the one key the module names` passes,
      asserting the literal `"pd.sessionToken"`
- [ ] `grep -cE 'writeDeviceId|DEVICE_ID_STORAGE_KEY|localStorage' web-client/src/protocol/session-token.ts`
      returns `0`
- [ ] `npm run test -- src/protocol/session-token.test.ts` reports `Tests  5 passed (5)`
- [ ] No file outside the two listed differs
- [ ] Every command in `verify:` exits 0

## Proof

1. Make `readSessionToken` return `value.trim()` rather than `value`.
   **`reads back the token it was given, byte for byte` reddens on the padded value alone.** The
   plain value passes, which is why that test carries two: a single unpadded fixture cannot tell a
   verbatim read from a trimmed one. Revert.
2. Make `forgetSessionToken` call `storage.clear()`.
   **`forgetting the token leaves the device id and the room code exactly where they were` reddens**,
   on both survivors, and nothing else in the file moves. This is the mutation that matters most:
   `storage.clear()` is a one-word implementation that passes every other test here and abandons the
   player's profile forever, which is `ADR-0012`'s named harm.
3. Change `SESSION_TOKEN_STORAGE_KEY` to `"pd.deviceId"`.
   **`writes under the one key the module names` reddens**, and so does `forgetting the token
   leaves…`. Run the variant where the key test asserts the constant instead of the literal: it
   **passes** under the mutation, which is what the criterion is defending against.
4. Drop the blank check so `"   "` reads back as itself.
   **`answers with nothing for a blank token` reddens on the whitespace input only.** `""` still
   answers `null` under most implementations, so a fixture using only `""` would miss it.

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket.

## Notes

**The module proves it writes one key; nothing proves nothing else writes that key.** The coder
stated this limit unprompted rather than leaving it to be discovered: the literal-key test catches a
bug *inside* this module, but a second module writing `SESSION_TOKEN_STORAGE_KEY` would shadow this
one's value and no test would fail. The "one key" invariant rests on convention, not enforcement.

**That gap has a solved precedent in this codebase and should become its own ticket.**
`TASK-040709`'s `ProfileCreationIsOneStatementTest` scans main source text for `INSERT INTO player`
and asserts the set of files containing it equals `{PostgresPlayerDirectory.kt}`, so a second write
path anywhere fails the build until someone extends the set with a reason. The reviewer judged the
equivalent feasible here — scan `web-client/src/**/*.ts` for files mentioning the key and assert the
set is exactly `{session-token.ts}`. Note the precedent's own vacuity guard: it searches **two**
different strings with two different expected answers, because one fixture default cannot tell a
working scan from a helper returning a constant.

**The golden key is asserted as a literal, not through the constant.** `writes under the one key the
module names` compares against the string `"pd.sessionToken"` rather than referencing
`SESSION_TOKEN_STORAGE_KEY` — a test reading the same constant it checks is a tautology. Changing the
constant reddens this test *and* the forget test, which is what makes the pair meaningful.

**Storage is read at call time, never at import.** Under Vitest in Node here, `localStorage` is
`undefined` where `sessionStorage` works, and an import-time read fails at module load naming no line
of this file. The module takes an injected `Storage` and touches it only inside its functions, so the
trap is structurally unreachable rather than merely avoided.

**Two inputs where one would have passed vacuously.** The byte-for-byte test stores a **padded** token
and a plain one, so mutating the read to `value.trim()` reddens on the padded case alone; a fixture
with no padding could not detect it. The blank-token test asserts `""` and `"   "` separately, since
an empty-string case alone establishes nothing about whitespace — which is exactly what Proof step 4
turns on.
