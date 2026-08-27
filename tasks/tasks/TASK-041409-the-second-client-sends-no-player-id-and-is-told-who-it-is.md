---
schema: 2
id: TASK-041409
title: The second client sends no player id, and is told who it is
type: task
status: done
parent: STORY-0414
module: web-client
estimate: S
tier: haiku
review: standard
files_touched: 1
labels: [client, e2e, test, auth, invariant]
depends_on: [TASK-041408]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test -- src/e2e/claimed-here-recovered-there.test.tsx 2>&1 | grep -qE 'Tests +6 passed \(6\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- src/e2e/claimed-here-recovered-there.test.tsx --reporter=verbose 2>&1 | grep -qF 'no request the second client made carries a player id'
  - cd web-client && NO_COLOR=1 npm run --silent test -- src/e2e/claimed-here-recovered-there.test.tsx --reporter=verbose 2>&1 | grep -qF 'the second client learns who it is only from an answer'
  - cd web-client && npm run check
---

## Goal

Across every request browser B made in the whole arc, no path, header or body carries a player id —
and the only place `player-seat-0` ever appears is in what the server answered.

## Why this exists

`EPIC-04`'s non-negotiable is that a client presents a credential and is **told** who it is
(`ADR-0002`, `ADR-0027`). `no-secret-in-a-url.test.ts` proves it for four functions called directly,
and its own note is explicit about what it cannot see:

> **A future caller that builds its own URL.** The sweep exercises the four modules under
> `web-client/src/account/` and nothing else… Extending this file's coverage to a new caller is that
> caller's ticket, not a side effect of this one.

Browser B driven through its **screens** is exactly that caller. This ticket is the extension the
note asks for, over a request log the arc already produced.

## Files

| File | Action |
| --- | --- |
| `web-client/src/e2e/claimed-here-recovered-there.test.tsx` | modify |

Read, and do not edit: `web-client/src/account/no-secret-in-a-url.md`;
`web-client/src/account/no-secret-in-a-url.test.ts`; `web-client/src/e2e/account-server.ts`.

## Scope

- Drive the whole of B's arc — its own boot, sign-in, and the reboot under the session — against one
  `accountServer`, then sweep `server.requests`.
- For every recorded request, assert the **path in full** (a path carries its own query string in this
  client, so one check covers both) contains neither player id, and that the parsed body's **keys**
  do not include `playerId`, `player_id` or `id`. Keys through `Object.keys().includes()`, never a
  substring scan over the serialized body — the collision a short key invites, and the reason
  `TASK-041224` made the same choice.
- Assert the session token appears in **no** path and no body — it is a bearer credential and belongs
  in the `Authorization` header only.
- Separately, assert B **was told**: `player-seat-0` appears in at least one *response* B received,
  and the request that earned it carried only `Authorization` and B's own `X-Device-Id`.

## Out of scope

- The address bar and `console`. `no-secret-in-a-url.test.ts` sweeps both for the four account
  modules, and re-sweeping them here would duplicate a merged guard rather than extend it.
- The socket. `Hello` carries a device id and a session token by design (`connection.ts:43–50`), and
  `ADR-0027` §1 settles that; it is not a player id.
- Widening `no-secret-in-a-url.test.ts` or editing `no-secret-in-a-url.md`. This is a second guard
  over a different surface, in this story's own file.

## Tests

`claimed-here-recovered-there.test.tsx` — two new, on top of four.

| Test | Proves |
| --- | --- |
| `no request the second client made carries a player id` | Over the whole recorded log: no path contains `player-seat-0` or `player-seat-1`, no body has a forbidden key, and the session token is in no path or body. The log is asserted **non-empty** first, so a sweep over nothing cannot pass. |
| `the second client learns who it is only from an answer` | `player-seat-0` appears in a response body B received, and the request that produced it carried no player id anywhere — the positive half, without which the test above passes for a client that made no requests at all. |

## Acceptance criteria

- [ ] `claimed-here-recovered-there.test.tsx` `no request the second client made carries a player id` passes
- [ ] `claimed-here-recovered-there.test.tsx` `the second client learns who it is only from an answer` passes
- [ ] `NO_COLOR=1 npm run --silent test -- src/e2e/claimed-here-recovered-there.test.tsx 2>&1 | grep -qE 'Tests +6 passed \(6\)'` exits 0
- [ ] The four tests from `TASK-041407` and `TASK-041408` pass unchanged — no assertion edited or removed
- [ ] `npm run check` exits 0
- [ ] Every command in `verify:` exits 0

## Proof

1. **Plant a violation.** Append `?playerId=player-seat-0` to the path in `drive-arc.tsx`'s profile
   read, in a scratch edit, and confirm `no request the second client made carries a player id`
   reddens. Revert. A sweep that does not redden on a planted offender is proving nothing, and this
   is the only way to know.
2. **Plant a body key.** Add `id: "x"` to the sign-in body and confirm the same test reddens on the
   key check specifically. If it does not, the key check is looking at the wrong object — the body
   arrives as a JSON **string** and must be parsed first.
3. **Empty the log.** Make the sweep run before B does anything: it must redden on the non-empty
   assertion rather than passing. A green run here is the vacuity this ticket is built to avoid.
4. **Remove the positive test** and confirm nothing else covers it: mutate the server to answer B's
   read with `player-seat-1` and check that `the second client learns who it is only from an answer`
   is the test that reddens. If `TASK-041408`'s tests redden too, say so — that is fine, but it means
   the positive half is corroboration rather than the only guard, and it should be written down.

## Definition of done

Standard, per [`tasks/README.md`](../README.md).
