---
schema: 2
id: TASK-050304
title: The ladder read — the device id is optional here, and there is no 401 to fear
type: task
status: done
parent: STORY-0503
module: web-client
estimate: S
tier: haiku
review: standard
files_touched: 2
labels: [client, leaderboard, http]
depends_on: [TASK-050303]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'asks for the first page with no cursor, and for the next with the one it was given'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'sends the device id when the browser holds one'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'asks anyway when the browser holds no device id, and sends no device header'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'answers unavailable for a refusal, a server error, and a fetch that throws'
  - cd web-client && npm run check
---

## Goal

One page of `GET /api/standings` reaches the client over an injected `fetch`, with the reader named
by `X-Device-Id` when there is one — and **with the request still made when there is not**.

## The trap this ticket exists to avoid

`duel-page.ts` is the module a coder will copy, and copying it wholesale is wrong here in one exact
place: it answers `no-profile` **without asking** when the browser holds no device id, because
`GET /api/me/duels` would answer `401`. This endpoint has **no `401`** (`docs/protocol.md`:
*"an absent, blank, or unknown device id is not a refusal"*), and the ladder must render for a
browser with no profile at all — `ADR-0065` §4 makes the page *"identical in all three states"*,
and `ADR-0036` keeps every screen reachable anonymously. An early return here would make the
ladder blank for every first-time visitor, which is precisely the reader most likely to open it.

## Files

| File | Action |
| --- | --- |
| `web-client/src/ladder/ladder-read.ts` | create |
| `web-client/src/ladder/ladder-read.test.ts` | create |

Read, not edited: `web-client/src/profile/duel-page.ts` (the shape, and the one difference above),
`web-client/src/profile/api.ts` (`ApiFetch`, `ApiResponse`),
`web-client/src/protocol/device-id.ts` (`readDeviceId`).

## Scope

- Exports:

  ```ts
  export type LadderRead =
    | { readonly kind: "page"; readonly page: LadderPage }
    | { readonly kind: "unavailable" };

  export function ladderPath(after: string | null): string;

  export function readLadderPage(request: {
    readonly fetch: ApiFetch;
    readonly storage: Storage;
    readonly after: string | null;
  }): Promise<LadderRead>;
  ```

- `ladderPath(null)` is `"/api/standings"`. `ladderPath(c)` is
  `` `/api/standings?after=${encodeURIComponent(c)}` `` — the cursor is passed through untouched,
  never parsed, never incremented, never derived from a row count.
- **No `limit` parameter.** The server's default of `10` is the page size for v0.3
  (`docs/protocol.md`), and a client that names one is a client choosing a number nothing asked it
  to choose.
- `readDeviceId(request.storage)` is read from the **`Storage` passed in**, never from a global.
  Node's own `localStorage` shadows jsdom's under Vitest and is `undefined`; a module reaching for
  the global is untestable here and behaves differently in a browser.
  - device id present → one header, `X-Device-Id`.
  - device id `null` → **the same request, with no `X-Device-Id` header at all** and no other
    difference. Not a second path, not an early return.
- **The `localStorage` grep below reads the whole file, comments included.** The KDoc explaining why
  the `Storage` is a parameter must say *the browser's storage* and *the inert global Node defines*
  rather than naming `localStorage`. A comment that spells it out fails a criterion, and the failure
  will look like a bug in the guard.
- Status mapping: `200` → `parseLadderPage(body)`, and `null` from the parse becomes
  `{ kind: "unavailable" }`; **every other status**, and any `fetch` or `json()` that rejects,
  becomes `{ kind: "unavailable" }`. Caught, never rethrown.

## Out of scope

- **Restarting a walk whose cursor the server refused.** `ADR-0066` §7 says the remedy for a `400`
  is *"drop the cursor and ask for the first page"*, and `TASK-041304` built that for the history
  walk — but `STORY-0503` asks for none of it, so a `400` here is `unavailable` and the screen says
  so. If the mid-walk month boundary is ever worth handling silently, it is an ordinary ticket
  against this module, not a detail to fill in now.
- **Retrying anything**, on any status, on the player's behalf.
- **A `playerId` query parameter.** `ADR-0065` §3 refuses one, and no request in this file carries
  one.
- **Telling the player anything.** This module answers a value; the sentences are
  `TASK-050309`'s.

## Tests

`web-client/src/ladder/ladder-read.test.ts`, `describe("reading one page of the ladder")`.
Use the in-memory `Storage` and the recording fake `fetch` from
`web-client/src/profile/profile-no-derivation.test.tsx` as the pattern — copied into this file,
not imported from a test.

| Test | Proves |
| --- | --- |
| `asks for the first page with no cursor, and for the next with the one it was given` | Two reads, one with `after: null` and one with `after: "cur/1+2"`: the recorded paths are `"/api/standings"` and `"/api/standings?after=cur%2F1%2B2"`. Two inputs, because one cannot tell a built path from a constant |
| `sends the device id when the browser holds one` | With `writeDeviceId(storage, "dev-1")`, the recorded call carries `X-Device-Id: "dev-1"` |
| `asks anyway when the browser holds no device id, and sends no device header` | With an **empty** storage: `fetch` is called **exactly once**, the recorded headers have no `X-Device-Id` key, and the answer is a `page` — not `unavailable` and not any no-profile state. This is the test the copied early return reddens |
| `answers unavailable for a refusal, a server error, and a fetch that throws` | A `400`, a `500`, a `fetch` that rejects, and a `200` whose body `parseLadderPage` refuses: four inputs, all `{ kind: "unavailable" }`, and none of them throws out of `readLadderPage` |

## Acceptance criteria

- [ ] `asks for the first page with no cursor, and for the next with the one it was given` passes,
      asserting both paths — dropping `encodeURIComponent` reddens it
- [ ] `sends the device id when the browser holds one` passes
- [ ] `asks anyway when the browser holds no device id, and sends no device header` passes —
      copying `duel-page.ts`'s `if (deviceId === null) return { kind: "no-profile" }` reddens it on
      the call count **and** on the answer
- [ ] `answers unavailable for a refusal, a server error, and a fetch that throws` passes for all
      four inputs
- [ ] `grep -c 'no-profile' web-client/src/ladder/ladder-read.ts` returns `0`
- [ ] `grep -c 'limit' web-client/src/ladder/ladder-read.ts` returns `0`
- [ ] `grep -cE 'localStorage|window\.fetch' web-client/src/ladder/ladder-read.ts` returns `0`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
