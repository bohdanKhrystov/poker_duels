---
schema: 2
id: TASK-031103
title: The recent-duels read keeps every field but the opponent's identifier
type: task
status: done
parent: STORY-0311
module: web-client
estimate: S
tier: haiku
review: standard
files_touched: 2
labels: [client, http, profile]
depends_on: [TASK-031102]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test 2>&1 | grep -qE 'Tests +333 passed \(333\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'asks /api/me/duels with no limit of its own'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'keeps every field a row carries except the opponent'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'answers an empty list for a player who has never duelled'
  - cd web-client && npm run check
---

## Goal

`GET /api/me/duels` becomes a list of `RecentDuel`s the client can render — and the opponent's
player id is **dropped at the parse**, so no component downstream has anywhere to leak it from.

## Files

| File | Action |
| --- | --- |
| `web-client/src/profile/recent-duels.ts` | create |
| `web-client/src/profile/recent-duels.test.ts` | create |
| `web-client/src/profile/api.ts` | read — `readFromApi`, `ApiFetch` |
| `web-client/src/profile/profile.ts` | read — the shape this one mirrors |
| `docs/protocol.md` | read — the duel summary's six fields and the `limit` rule, lines 75–99 |

## Scope

- `recent-duels.ts` declares the second and last hand-written wire shape this client is allowed:

  ```ts
  /** The outcome of a listed duel, from the reader's side (`docs/protocol.md`). */
  export type DuelOutcomeWord = "WON" | "LOST" | "DREW";

  /**
   * One row of `GET /api/me/duels`.
   *
   * The wire carries `opponentPlayerId` and this type does not: no display name
   * exists yet (`ADR-0021`, `DEC-017`), so the only thing the client could print
   * is a raw identifier in front of a player. Dropping it here means no screen
   * can print it by accident.
   */
  export interface RecentDuel {
    readonly duelId: string;
    readonly outcome: DuelOutcomeWord;
    readonly coinDelta: number;
    readonly handsPlayed: number;
    readonly finishedAt: string;
  }

  export type RecentDuelsRead =
    | { readonly kind: "duels"; readonly duels: readonly RecentDuel[] }
    | { readonly kind: "no-profile" }
    | { readonly kind: "unavailable" };

  export async function readRecentDuels(deps: {
    readonly fetch: ApiFetch;
    readonly storage: Storage;
  }): Promise<RecentDuelsRead>;
  ```

- The path is exactly `"/api/me/duels"` — **no `limit` parameter**. The server defaults to 10 and
  rejects zero, negative and non-numeric with `400`; sending nothing is the one spelling that cannot
  be wrong, and the strip shows a *few* recent duels rather than a page of them.
- The body must be `{ duels: [...] }`; each row is built field by field from the five names above,
  with `outcome` accepted only when it is one of the three words. Anything else makes the whole read
  `unavailable` — a half-parsed ledger is worse than none.
- An empty `duels` array is an **answer**, not an error: `{ kind: "duels", duels: [] }`.
- `no-profile` and `unavailable` pass through from `readFromApi` unchanged.

## Out of scope

- Enumerating the three outcomes, the signed deltas and the rejections — `TASK-031104` owns those
  tests, against this same parse.
- Paging, filtering, a limit control, a full history — `EPIC-04` (`STORY-0311`'s out-of-scope list).
- Sorting. The server answers newest first and the client keeps the order it was given.
- Rendering anything — `TASK-031105` and after.

## Tests

`web-client/src/profile/recent-duels.test.ts`, describe block `"the recent duels read"`. Same
helpers as `profile.test.ts` — `inMemoryStorage()` and `answering()`/`ok()` — plus the four-line
convenience they share:

```ts
function storageHolding(deviceId: string): Storage {
  const storage = inMemoryStorage();
  storage.setItem(DEVICE_ID_STORAGE_KEY, deviceId);
  return storage;
}
```

| Test | Proves |
| --- | --- |
| `asks /api/me/duels with no limit of its own` | the recorded path is exactly `"/api/me/duels"` — it contains no `?`, and exactly one call is made |
| `keeps every field a row carries except the opponent` | one row carrying **all six** wire fields parses to an object deep-equal to the five-field `RecentDuel`; `Object.keys(duel)` does not contain `opponentPlayerId`; and `JSON.stringify(result)` does not contain the opponent id string the body carried |
| `answers an empty list for a player who has never duelled` | `{ duels: [] }` answers `{ kind: "duels", duels: [] }` — not `no-profile`, not `unavailable` |

```ts
it("keeps every field a row carries except the opponent", async () => {
  const { fetch } = answering(
    ok({
      duels: [
        {
          duelId: "duel-1",
          opponentPlayerId: "player-77",
          outcome: "WON",
          coinDelta: 1,
          handsPlayed: 9,
          finishedAt: "2026-08-14T21:03:05Z",
        },
      ],
    }),
  );

  const read = await readRecentDuels({ fetch, storage: storageHolding("d-1") });

  expect(read).toEqual({
    kind: "duels",
    duels: [
      {
        duelId: "duel-1",
        outcome: "WON",
        coinDelta: 1,
        handsPlayed: 9,
        finishedAt: "2026-08-14T21:03:05Z",
      },
    ],
  });
  expect(JSON.stringify(read)).not.toContain("player-77");
});
```

Three tests added. Three hundred and thirty exist, so the suite reports **333**.

## Proof

| Command | Proves |
| --- | --- |
| `Tests 333 passed (333)` | three ran and nothing else moved |
| the three `--reporter=verbose` greps | every name above exists |
| `npm run check` | typechecks under `strict`, lints, is formatted |

**Name the edit that makes each assertion red** — run each, quote two in the PR, revert:

1. Spread the row (`...row`) instead of naming its five fields → `keeps every field a row carries
   except the opponent` fails on both the `toEqual` and the `player-77` half.
2. Append `?limit=10` to the path → `asks /api/me/duels with no limit of its own` fails.
3. Answer `unavailable` for an empty array → `answers an empty list for a player who has never
   duelled` fails.

## Acceptance criteria

- [ ] `the recent duels read > asks /api/me/duels with no limit of its own` passes
- [ ] `the recent duels read > keeps every field a row carries except the opponent` passes
- [ ] `the recent duels read > answers an empty list for a player who has never duelled` passes
- [ ] `RecentDuel` declares no `opponentPlayerId`, and the parse names its five fields one by one
- [ ] `recent-duels.ts` contains no spread of a parsed row
- [ ] `web-client/src/profile/api.ts` and `profile.ts` are byte-identical to what they were
- [ ] `npm run --silent test` reports `Tests  333 passed (333)`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
