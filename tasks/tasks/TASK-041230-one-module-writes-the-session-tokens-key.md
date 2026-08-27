---
schema: 2
id: TASK-041230
title: One module writes the session token's key, and a scan is what says so
type: task
status: done
parent: STORY-0412
module: web-client
estimate: S
tier: sonnet
review: standard
files_touched: 1
labels: [client, auth, storage, invariant, test]
depends_on: [TASK-041229]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test -- src/protocol/one-module-owns-each-storage-key.test.ts 2>&1 | grep -qE 'Tests +2 passed \(2\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'only the session-token module writes the session token key'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'the scan tells two keys apart'
  - grep -qF 'assembled from constants' web-client/src/protocol/one-module-owns-each-storage-key.test.ts
  - grep -qF 'file-name set' web-client/src/protocol/one-module-owns-each-storage-key.test.ts
  - cd web-client && npm run check
---

## Goal

A second module gaining the literal `"pd.sessionToken"` fails the client build, so `TASK-041205`'s
*one key* stops resting on convention.

## Why this exists

`TASK-041205`'s coder stated the limit unprompted rather than leaving it to be found: the module's
`writes under the one key the module names` catches a bug **inside** `session-token.ts`, but a second
module writing the same key would shadow this one's value and no test would fail. Two writers means
two sessions for one player, and `ADR-0030` §8's *sign-out clears the token and only the token*
becomes unprovable the moment a second file can clear it.

The precedent is merged and is what this copies. `TASK-040709`'s
`ProfileCreationIsOneStatementTest` scans main source text for `INSERT INTO player` and asserts the
set of files containing it equals `{PostgresPlayerDirectory.kt}`, so a second write path anywhere
fails the build until someone extends the set with a reason. Its own `## Notes` names the vacuity
guard that makes it work: **two different search strings with two different expected answers**,
because one fixture default cannot tell a working scan from a helper returning a constant.

## Files

| File | Action |
| --- | --- |
| `web-client/src/protocol/one-module-owns-each-storage-key.test.ts` | create |

Read, and do not edit: `web-client/src/protocol/session-token.ts` and
`web-client/src/protocol/device-id.ts` (the two key constants, and nothing else);
`web-client/src/App.test.tsx` lines 1–30 — the `dirname(fileURLToPath(import.meta.url))` idiom this
file follows;
[`ADR-0030`](../../docs/adr/ADR-0030-a-claim-adds-a-credential-and-moves-nothing.md) §8.

## Scope

- One new test file with one private helper:

  ```ts
  function productionSourcesContaining(literal: string): string[]
  ```

  It resolves `src` as `resolve(dirname(fileURLToPath(import.meta.url)), "..")` — the depth is fixed
  and the client's other source-reading test already uses that idiom, so no upward search is needed
  — walks it recursively, keeps files ending `.ts` or `.tsx` whose name does **not** end `.test.ts`
  or `.test.tsx`, and returns the **sorted file names** of those whose text includes `literal`.
- **It throws, naming the absolute path it resolved, when that directory does not exist.** A helper
  that answers `[]` for a missing tree turns both tests into assertions about nothing.
- **File names, not paths, and a sorted array, not a count.** A count is a magic number stale on the
  next refactor; a path breaks when a module moves for an unrelated reason.
- **Test files are excluded, and the exclusion is the point rather than a convenience.**
  `connection.test.ts`, `session-token.test.ts`, `authorized-fetch.test.ts`, `index.test.ts` and
  `device-id.test.ts` each legitimately hold a key literal — a test asserting the golden string is
  what `TASK-041205` and `TASK-030304` were required to write. The defect this guards is a second
  **writer**, and a writer lives in production source.
- **Two honest limits, in the file's leading comment**, each containing the exact phrase in bold
  because `verify:` greps for it — a limit a reader cannot find is the same as a limit nobody wrote:
  1. It reads source text. A key **assembled from constants**, or split across a line break, escapes
     it. Every storage key in this client is one string literal on one line today, and this test is
     the reason to keep it that way.
  2. It is a **file-name set** assertion. A *second* write inside `session-token.ts` escapes it, and
     that is deliberate: owning the key is that module's job, and the defect guarded against is a
     writer somewhere else.

## Out of scope

- **Changing `session-token.ts`, `device-id.ts` or `room-memory.ts`.** Each is already the only
  production file holding its own key; this ticket makes that a fact a gate holds. The *Files* table
  has one row.
- **`"pd.roomCode"`.** A room code is not a credential — losing one costs a player a rejoin, and
  `room-memory.ts` is already covered by the same shape if anyone wants it. Two needles are what the
  vacuity guard needs; a third buys nothing and adds a file to keep in step.
- **Scanning for `localStorage` or `sessionStorage` usage.** A module may legitimately take an
  injected `Storage`; what must not spread is the **key**. `DEC-032` is why the storage is injected
  and it is a different rule with a different gate.
- **A rule that every key literal appears in exactly one file.** That is a generalisation over a set
  nobody has enumerated, and it would fail the day a key is renamed in two steps. Two named keys,
  two named owners.

## Tests

`web-client/src/protocol/one-module-owns-each-storage-key.test.ts`, describe block
`"one module owns each storage key"`.

| Test | Proves |
| --- | --- |
| `only the session-token module writes the session token key` | `productionSourcesContaining("pd.sessionToken")` equals `["session-token.ts"]`. A second production file gaining that literal fails the build until either it is removed or this array is extended with a comment saying why two writers are correct |
| `the scan tells two keys apart` | `productionSourcesContaining("pd.deviceId")` equals `["device-id.ts"]`. **Two inputs, two different expected answers** — and the guard the test above cannot do without: a scan matching nothing satisfies an empty-versus-empty comparison, and one matching everything fails here |

Two tests in a new file: `npm run test -- src/protocol/one-module-owns-each-storage-key.test.ts`
reports **2**.

Both expected answers were **measured** on `develop` at `299ea851`, before this ticket was written:
`grep -rln 'pd.sessionToken' web-client/src/` answers four files, of which three end `.test.ts`;
`grep -rln 'pd.deviceId'` answers four, of which three end `.test.ts`. **If either test reddens on
first run, stop and report it** rather than extending the expected array — it means a ticket merged
between that measurement and this one put a key literal in a second production file, which is the
finding this ticket exists to produce.

## Acceptance criteria

- [ ] `one module owns each storage key > only the session-token module writes the session token key`
      passes, asserting the array `["session-token.ts"]`
- [ ] `one module owns each storage key > the scan tells two keys apart` passes, asserting the array
      `["device-id.ts"]`
- [ ] Both tests call the same `productionSourcesContaining` helper with two **different** arguments,
      and neither test hard-codes a file **count**
- [ ] The two literals are written out in the test — `"pd.sessionToken"` and `"pd.deviceId"` — and
      neither test imports `SESSION_TOKEN_STORAGE_KEY` or `DEVICE_ID_STORAGE_KEY`. A scan driven by
      the constant it is checking cannot see a second file that spells the string out
- [ ] The helper throws, naming the resolved path, when `src` is not found, rather than returning `[]`
- [ ] The file's leading comment contains the literal phrases `assembled from constants` and
      `file-name set`, one per limit — the two `grep -qF` commands in `verify:` are the check
- [ ] `npm run test -- src/protocol/one-module-owns-each-storage-key.test.ts` reports
      `Tests  2 passed (2)`
- [ ] The diff touches exactly one file, and it is the one in the *Files* table
- [ ] Every command in `verify:` exits 0

## Proof

1. Add a second writer where none belongs: in `web-client/src/protocol/room-memory.ts`, add
   `const SHADOW = "pd.sessionToken";`.
   **`only the session-token module writes the session token key` reddens alone**, with
   *expected ["session-token.ts"], received ["room-memory.ts", "session-token.ts"]*.
   `the scan tells two keys apart` searches a different literal and is unaffected. `npm run check`
   may also report the constant as unused, which is expected of a throwaway mutation and is not a
   second finding. Revert.
2. Make `productionSourcesContaining` ignore its argument and return `["session-token.ts"]`.
   **`the scan tells two keys apart` reddens alone**, *expected ["device-id.ts"]*. This is the
   mutation two needles exist for, and it is the one a single-test version of this file could never
   catch. Revert.
3. Drop the `.test.ts` exclusion.
   **Both tests redden**, each with three extra file names. Run it and read the list: those three per
   key are golden-string assertions the two storage tickets were required to write, which is why the
   exclusion is a rule with a reason rather than a filter that happened to be convenient. Revert.
4. Split the literal in `session-token.ts` — `export const SESSION_TOKEN_STORAGE_KEY = "pd." +
   "sessionToken";`.
   **`only the session-token module writes the session token key` reddens**, *expected
   ["session-token.ts"], received []*. The client still behaves identically and every test in
   `session-token.test.ts` still passes. This is limit 1 made visible, and running it is how the
   comment gets written truthfully rather than defensively. Revert.
5. Add a second `storage.setItem(SESSION_TOKEN_STORAGE_KEY, …)` **inside** `session-token.ts`.
   **Nothing reddens.** Recorded as inert on purpose: it is limit 2, it is deliberate, and a reader
   who mistakes this test for a guard against *any* second write would be wrong in a direction that
   matters. Revert.

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket.
