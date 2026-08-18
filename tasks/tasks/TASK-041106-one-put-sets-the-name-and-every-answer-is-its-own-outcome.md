---
schema: 2
id: TASK-041106
title: One PUT sets the name, and every answer is its own outcome
type: task
status: backlog
parent: STORY-0411
module: web-client
estimate: S
tier: sonnet
review: standard
files_touched: 3
labels: [client, profile, identity, write-path]
depends_on: [TASK-041105]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test 2>&1 | grep -qE 'Tests +392 passed \(392\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'sends one PUT to /api/me/name carrying the device id and the name as typed'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'sends a body whose only key is the name'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'answers with the profile the server returned, not the name that was typed'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'gives each status its own outcome'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'answers unavailable when the fetch rejects, and sends nothing more'
  - cd web-client && npm run check
---

## Goal

`setDisplayName` sends one `PUT /api/me/name` carrying the string the player typed, and turns each
of the server's six answers into its own outcome — the `200` parsed by the same function
`GET /api/me` uses.

## Files

| File | Action |
| --- | --- |
| `web-client/src/profile/api.ts` | modify — two optional fields on the init type |
| `web-client/src/profile/set-name.ts` | create |
| `web-client/src/profile/set-name.test.ts` | create |

Read, not edited: `docs/protocol.md` (*Set display name* — the request body and all five status
rows), `web-client/src/profile/profile.ts` (`profileFromBody`),
`web-client/src/protocol/device-id.ts` (`readDeviceId`).

## Scope

```ts
export type SetNameOutcome =
  | { readonly kind: "named"; readonly profile: PlayerProfile } // 200
  | { readonly kind: "rejected" }    // 400 — the rules refuse this name
  | { readonly kind: "permanent" }   // 403 — this player already has one
  | { readonly kind: "conflict" }    // 409 — the name is not available
  | { readonly kind: "no-profile" }  // 401 — absent, blank or unknown device id
  | { readonly kind: "unavailable" };// anything else, or a fetch that rejected

export async function setDisplayName(request: {
  readonly fetch: ApiFetch;
  readonly storage: Storage;
  readonly name: string;
}): Promise<SetNameOutcome>;
```

- **`ApiFetch`'s init gains `method?: string` and `body?: string`, both optional.** Checked with
  `npm run typecheck`: every existing fake fetch and `main.tsx`'s real one still satisfy the widened
  type, so no other file changes. Do not add a second fetch type.
- **The name is sent exactly as typed** — not trimmed, not lower-cased, not normalised. `ADR-0029`
  §2 makes canonicalisation the server's, and §5 returns the whole profile so the client is *told*
  what it now owns. A client that trimmed first would be guessing at a rule it does not enforce.
- The request is `PUT`, path `/api/me/name`, header `X-Device-Id`, body
  `JSON.stringify({ name })` — **one key**. `docs/protocol.md` refuses an unrecognised field with
  `400`, and `STORY-0411` requires that no request body contain a player id.
- **No device id means no request.** `readDeviceId` answering `null` returns `{ kind: "no-profile" }`
  without calling `fetch`, exactly as `readFromApi` already does — the answer is known and the round
  trip is not free.
- The `200` body goes through `profileFromBody`; a body that is not a profile is `unavailable`, not
  a half-built success. A `200` whose body is unreadable is a server this client cannot follow.
- The `Storage` is a **parameter**, never a global. Under Vitest, Node's own `localStorage` global
  shadows jsdom's and is inert unless the process was started with `--localstorage-file`, so
  `typeof localStorage === "undefined"` while `sessionStorage` works (`DEC-032`). Every test hands
  in its own in-memory `Storage`, as the profile tests already do.

## Out of scope

- Any React. This module is a function; the surface that calls it is `TASK-041108` onwards.
- The words a player reads for any of these outcomes — `TASK-041107`. **A refusal, not an
  omission:** the outcome names are internal and one of them must never reach a screen as-is. The
  `409` copy may not say *taken* (`ADR-0052` §7), and keeping the sentence out of this module is
  what stops the outcome's name leaking into it.
- Retrying anything. `ADR-0029` §5 makes the identical name idempotent, but a client that re-sent on
  the player's behalf would be choosing for them at the one moment the choice is permanent.
- Re-reading `GET /api/me` afterwards. The `200` carries the profile, which is why `ADR-0029` §5
  answers `200` rather than `204`.

## Tests

`web-client/src/profile/set-name.test.ts`, describe block `"setting a display name"`. Reuse the
`inMemoryStorage`, `answering` and `ok` helpers' shape from `profile-no-derivation.test.tsx`.

| Test | Proves |
| --- | --- |
| `sends one PUT to /api/me/name carrying the device id and the name as typed` | **Two typed strings in one test** — `"  Ada  "` and `"Grace"` — each producing exactly one call, with `method` `"PUT"`, path `/api/me/name`, the `X-Device-Id` header of the storage it was given, and a body whose `name` is the string byte for byte. Fails against a client that trims, that sends the canonical form, or that sends twice; two strings are what stops a hardcoded body passing |
| `sends a body whose only key is the name` | `Object.keys(JSON.parse(call.body))` is exactly `["name"]`. Fails against a body carrying `playerId`, `deviceId` or anything else derived — the request half of `STORY-0411`'s no-derivation rule |
| `answers with the profile the server returned, not the name that was typed` | Typing `"  ada  "` against a `200` whose body is `meBody({ displayName: "Ada" })` answers `{ kind: "named" }` with `displayName === "Ada"`, and the outcome contains the typed string nowhere. Fails against a client that echoes its input, which is the whole reason `ADR-0029` §5 returns a profile |
| `gives each status its own outcome` | `400 → rejected`, `403 → permanent`, `409 → conflict`, `401 → no-profile`, `500 → unavailable`, and an empty storage → `no-profile` **with no call made**. Five statuses in one test; fails against any two statuses folded together, and against a `403` treated as a conflict, which is the mistake `ADR-0029` §5 chose the status codes to prevent |
| `answers unavailable when the fetch rejects, and sends nothing more` | A fetch that throws answers `unavailable` and the call count stays `1`. Fails against a client that retries, and against one that lets the rejection escape into the caller |

Five tests added to 387, so the suite reports **392**.

## Acceptance criteria

- [ ] All five tests above pass under `describe("setting a display name")`
- [ ] `setDisplayName` sends exactly one request per call, and none at all without a device id
- [ ] `grep -c 'trim\|toLowerCase\|normalize' web-client/src/profile/set-name.ts` returns `0`
- [ ] `set-name.ts` imports `profileFromBody` from `./profile` and does not re-implement it
- [ ] `api.ts`'s init type gains only optional fields, and `api.test.ts` is unmodified
- [ ] No file outside the three listed differs
- [ ] `npm run --silent test` reports `Tests  392 passed (392)`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
