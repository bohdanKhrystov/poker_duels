---
schema: 2
id: TASK-030313
title: The fake socket is a no-op when nothing is listening
type: task
status: backlog
parent: STORY-0303
module: web-client
estimate: XS
tier: haiku
review: light
files_touched: 1
labels: [client, protocol, testing]
depends_on: [TASK-030311]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test 2>&1 | grep -qE 'Tests +61 passed \(61\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'opens without a handler and does nothing'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'receives without a handler and does nothing'
  - cd web-client && npm run check
---

## Goal

`FakeSocket.open()` and `receive()` are proven to do nothing when no handler is set, so the guard
that makes them safe cannot be removed without a test going red.

## Files

`files_touched` counts the create/modify rows only.

| File | Action |
| --- | --- |
| `web-client/src/protocol/fake-socket.test.ts` | modify |

## Why this exists

`TASK-030306`'s Scope required that `open()` and `receive()` are no-ops when no handler is set — a
test that receives before the client is listening should observe nothing rather than throw. It
shipped with the guards in place and **no assertion covering them**. Measured on `8e754ff`:
replacing `if (this.onopen) { this.onopen(); }` with `this.onopen!()` leaves the suite at
`Tests 37 passed (37)`, entirely green.

That is the failure this story is most exposed to, in the one file every other handshake test
stands on. It was found while landing `TASK-030306` and deliberately **not** fixed there: adding
tests mid-story would shift the cumulative counts that `TASK-030307` onward hardcode in their
`verify` blocks. This ticket sits after the last count-changing ticket (`TASK-030311`, 59) instead.

## Scope

- Add exactly two `it` blocks to the existing `"the fake socket"` describe block.
- Each constructs a `FakeSocket`, leaves every `on*` property `null`, and asserts the call does not
  throw and changes nothing observable.
- `expect(() => …).not.toThrow()` is the assertion. Do not add a spy, and do not assert on
  `onmessage` being `null` — the point is the *call*, not the field.
- Nothing in `fake-socket.ts` changes. If it needs to change, the guards were already wrong and
  that is a defect to report, not to fix here.

## Out of scope

- `close()` with no `onclose`. It sets `closed` first, so its observable effect is already covered
  by `marks itself closed and tells the close handler`; adding a third case earns nothing.
- `readyState`, close codes, `addEventListener` — still `STORY-0310`'s if reconnect needs them.

## Tests

`web-client/src/protocol/fake-socket.test.ts`, existing describe block `"the fake socket"`. Two new
`it` blocks:

| Test | Proves |
| --- | --- |
| `opens without a handler and does nothing` | with `onopen` left `null`, `open()` does not throw |
| `receives without a handler and does nothing` | with `onmessage` left `null`, `receive("{}")` does not throw, and `sent` stays empty |

Two tests. Fifty-nine exist after `TASK-030311`, so the suite reports **61**.

## Proof

| Command | Proves |
| --- | --- |
| `Tests 61 passed (61)` | both tests ran and nothing earlier was displaced |
| the two `--reporter=verbose` greps | each case exists by name |

**Name the edit that makes each assertion red:**

1. Replace the `open()` guard with `this.onopen!();` → `opens without a handler and does nothing`
   fails with a `TypeError`. Revert.
2. Replace the `receive()` guard with `this.onmessage!({ data });` →
   `receives without a handler and does nothing` fails the same way. Revert.

Quote both in the PR. Unlike most assertions in this story these are falsifiable on day one: the
guards exist, so deleting one goes red immediately.

## Acceptance criteria

- [ ] `the fake socket > opens without a handler and does nothing` passes
- [ ] `the fake socket > receives without a handler and does nothing` passes
- [ ] `npm run --silent test` reports `Tests  61 passed (61)`
- [ ] `web-client/src/protocol/fake-socket.ts` is unchanged
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
