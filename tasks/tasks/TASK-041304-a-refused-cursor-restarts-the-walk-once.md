---
schema: 2
id: TASK-041304
title: A refused cursor restarts the walk, once, and never reaches the player
type: task
status: ready
parent: STORY-0413
module: web-client
estimate: S
tier: sonnet
review: standard
files_touched: 2
labels: [client, history, http, cursor]
depends_on: [TASK-041303]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'restarts the walk when the server refuses the cursor, and says it restarted'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'keeps both filter axes when it restarts, and drops only the cursor'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'restarts at most once'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'reads a 400 on a request that carried no cursor as unavailable'
  - cd web-client && npm run check
---

## Goal

`ADR-0057` §5's client obligation is code: a `400` answering a request that carried `after` restarts
the walk from the newest page, exactly once, and the caller is told the page it got is a restart
rather than a continuation.

## Files

| File | Action |
| --- | --- |
| `web-client/src/profile/duel-page.ts` | modify — a `restarted` field, the `400` branch, one retry |
| `web-client/src/profile/duel-page.test.ts` | modify — four tests added, two `toEqual`s gain a field |

Read, not edited:
[`ADR-0057`](../../docs/adr/ADR-0057-a-cursor-names-the-filter-it-was-drawn-under.md) §5 and §6,
`docs/protocol.md` line 141.

## Scope

- The `page` variant gains `readonly restarted: boolean`.
- On a `400` **to a request whose query carried a non-null `after`**: send the same query once more
  with `after: null` and nothing else changed, and answer that response with `restarted: true`. Any
  outcome of the second request is final — a second `400`, a `401`, a `500` or a throw answers as it
  would for a first request, and **no third request is made**.
- On a `400` to a request that carried **no** `after`: `unavailable`, with no retry. There is nothing
  to drop, so retrying would repeat the request that was just refused, which is how an unbounded
  loop starts.
- `ADR-0057` §5 is why the remedy is a restart and not an error: the `400` is *"the backstop that
  turns a client bug into a visible failure instead of a truncated history"*, and it is
  indistinguishable from a corrupt cursor on purpose, so there is nothing for a player to be told and
  nothing they could do.
- **This ticket owns the two `toEqual`s `TASK-041302` wrote over a `page` answer** — in `answers the
  rows and the cursor that names the next page` and in `drops the opponent id from every row it
  parses`. Each gains `restarted: false` and keeps every field it already asserted. No assertion is
  weakened, and no other test in the file changes.
- `restarted` is a field on the existing variant rather than a fourth `kind`, because a fourth kind
  would break the narrowing in `profile-strip.ts` — and the strip, which never sends `after`, can
  never see a restart.

## Out of scope

- Anything the caller does with `restarted`. `TASK-041307` makes the reducer replace its rows
  instead of appending them; until then the field is carried and unread, which is deliberate — the
  read is where the retry belongs and the reducer is where the rows live.
- Showing a player anything about a refused cursor. **A refusal, not an omission:** `ADR-0057` §5
  forbids it in as many words — *"never shown to the player as an error"*.
- Retrying a `500`, or any status other than `400`. `ADR-0057` §5 names one status and one remedy;
  a client that retried a server error would double a load that is already failing.
- Distinguishing a stale cursor from a corrupt one. `ADR-0057` §5 makes them the same `400` with the
  same empty body, on purpose, and the client's remedy is identical.

## Tests

`web-client/src/profile/duel-page.test.ts`, describe block `"the duel page read"`.

| Test | Proves |
| --- | --- |
| `restarts the walk when the server refuses the cursor, and says it restarted` | A read with `after: "stale"` against `400` then `200`: exactly **two** calls, the second path carries no `after`, and the answer is the second page with `restarted: true`. Fails against a read that surfaces the `400` as `unavailable` — which is what `TASK-041302` shipped, so this test fails before the change and passes after it |
| `keeps both filter axes when it restarts, and drops only the cursor` | The refused request carried `{ outcome: "WON", opponent: "Ada", after: "stale" }`; the retry's path is asserted to be exactly `duelsPath({ outcome: "WON", opponent: "Ada", after: null })`. Fails against a retry that sends `WHOLE_RECORD` — which would silently show a player the unfiltered newest page while their filter controls still read *Won* and *Ada* |
| `restarts at most once` | `400` then `400`: the answer is `unavailable` and `calls` has length exactly **2**. Fails against a retry loop, and against a `while` that re-reads the same refusal forever |
| `reads a 400 on a request that carried no cursor as unavailable` | A read with `after: null` against `400`: `unavailable`, and `calls` has length exactly **1**. Fails against a retry rule that keys on the status alone rather than on the status *and* an outstanding cursor — the version that would re-send the request it was just refused |

Four tests added to the six `TASK-041302` wrote.

## Acceptance criteria

- [ ] `the duel page read > restarts the walk when the server refuses the cursor, and says it
      restarted` passes, asserting exactly two calls
- [ ] `the duel page read > keeps both filter axes when it restarts, and drops only the cursor`
      passes, asserting the retry's whole path
- [ ] `the duel page read > restarts at most once` passes, asserting exactly two calls
- [ ] `the duel page read > reads a 400 on a request that carried no cursor as unavailable` passes,
      asserting exactly one call
- [ ] The six tests `TASK-041302` wrote pass, the two that assert a whole `page` answer each gaining
      `restarted: false` and losing nothing
- [ ] Every test in `web-client/src/profile/recent-duels.test.ts` passes unchanged, and that file
      does not differ
- [ ] No file outside the two listed differs
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
