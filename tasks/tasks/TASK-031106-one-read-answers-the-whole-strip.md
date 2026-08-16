---
schema: 2
id: TASK-031106
title: One read answers the whole strip, or none of it
type: task
status: backlog
parent: STORY-0311
module: web-client
estimate: S
tier: haiku
review: standard
files_touched: 2
labels: [client, http, profile]
depends_on: [TASK-031105]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test 2>&1 | grep -qE 'Tests +345 passed \(345\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'answers the profile and its duels when both reads land'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'asks both endpoints once each'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'answers no-profile when either half says so'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'answers unavailable when either half fails'
  - cd web-client && npm run check
---

## Goal

One call — `readProfileStrip` — answers the whole strip: the profile with its duels, *no profile
yet*, or nothing. A screen never has to combine two reads, and never renders half a ledger.

## Files

| File | Action |
| --- | --- |
| `web-client/src/profile/profile-strip.ts` | create |
| `web-client/src/profile/profile-strip.test.ts` | create |
| `web-client/src/profile/profile.ts` | read — `readProfile`, `PlayerProfile` |
| `web-client/src/profile/recent-duels.ts` | read — `readRecentDuels`, `RecentDuel` |
| `web-client/src/profile/api.ts` | read — `ApiFetch` |

## Scope

- One state type and one function:

  ```ts
  /** What the lobby strip has to show. */
  export type ProfileStripState =
    | {
        readonly kind: "profile";
        readonly profile: PlayerProfile;
        readonly duels: readonly RecentDuel[];
      }
    | { readonly kind: "no-profile" }
    | { readonly kind: "unavailable" };

  export async function readProfileStrip(deps: {
    readonly fetch: ApiFetch;
    readonly storage: Storage;
  }): Promise<ProfileStripState>;
  ```

- Both reads start together (`Promise.all`) — they are independent GETs and serialising them would
  double the wait for no benefit.
- The mapping, in this order:
  - either read says `no-profile` → `{ kind: "no-profile" }`. On a first visit both do; the socket
    handshake is what mints an id, after which the next mount reads a profile.
  - either read says `unavailable` → `{ kind: "unavailable" }`. **All or nothing**: a balance shown
    beside a duel list that failed to load reads as *you have no duels*, which is a lie about the
    ledger, and the player cannot tell the difference.
  - both landed → `{ kind: "profile", profile, duels }`, each verbatim.
- Nothing here counts, sums, sorts or compares. The balance and the deltas arrive from two responses
  and are put in one object.

## Out of scope

- React. This file imports none and is testable without a DOM — `TASK-031109` is the seam that runs
  it from a component tree.
- Retrying the half that failed, or falling back to a cached answer. One read per mount.
- Deciding what *unavailable* looks like. `TASK-031107` renders it, and renders nothing.

## Tests

`web-client/src/profile/profile-strip.test.ts`, describe block `"the profile strip read"`. Same
helpers as the two read tests: `inMemoryStorage()`, `storageHolding()`, `answering()`, `ok()`,
`refusedWith()`. `answering()` hands out its answers in call order, so a test that cares which
endpoint got which answer asserts on `calls` as well.

| Test | Proves |
| --- | --- |
| `answers the profile and its duels when both reads land` | a balance of `-1` and two duel rows come back as one `{ kind: "profile" }` with both parts verbatim — and a second case with a balance of `7` and **no** duels, so neither part can be a constant |
| `asks both endpoints once each` | the recorded paths, sorted, are exactly `["/api/me", "/api/me/duels"]` — two calls, no third, no repeat |
| `answers no-profile when either half says so` | both doors, written out: an empty `Storage` (and then **no call is made at all**), and a stored id both endpoints answer `401` to |
| `answers unavailable when either half fails` | both halves, written out: `/api/me` answers `500` while the duels answer `200`; then the duels answer `500` while `/api/me` answers `200`. A strip that showed the balance anyway would pass the first and fail here |

Four tests added. Three hundred and forty-one exist, so the suite reports **345**.

## Proof

| Command | Proves |
| --- | --- |
| `Tests 345 passed (345)` | four ran and nothing else moved |
| the four `--reporter=verbose` greps | every name above exists |
| `npm run check` | typechecks under `strict`, lints, is formatted |

**Name the edit that makes each assertion red** — run each, quote two in the PR, revert:

1. Return `{ kind: "profile", profile, duels: [] }` when the duels read failed → `answers
   unavailable when either half fails` fails on its second case only, which is exactly the lie the
   rule forbids.
2. Await the two reads one after the other and return after the first → `asks both endpoints once
   each` fails on the recorded paths.
3. Map `no-profile` to `unavailable` → `answers no-profile when either half says so`
   fails.

## Acceptance criteria

- [ ] `the profile strip read > answers the profile and its duels when both reads land` passes, with
      two different profiles
- [ ] `the profile strip read > asks both endpoints once each` passes
- [ ] `the profile strip read > answers no-profile when either half says so` passes
- [ ] `the profile strip read > answers unavailable when either half fails` passes, with both halves
      failing in turn
- [ ] `profile-strip.ts` imports nothing from `react`
- [ ] `profile.ts`, `recent-duels.ts` and `api.ts` are byte-identical to what they were
- [ ] `npm run --silent test` reports `Tests  345 passed (345)`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
