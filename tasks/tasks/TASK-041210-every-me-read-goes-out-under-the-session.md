---
schema: 2
id: TASK-041210
title: Every read under /api/me goes out under the session, so the strip stops naming the wrong player
type: task
status: ready
parent: STORY-0412
module: web-client
estimate: S
tier: haiku
review: standard
files_touched: 2
labels: [client, auth, wiring]
depends_on: [TASK-041209]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'reads the profile, the record and the ladder under the session this browser holds'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'sets a name under the session this browser holds'
  - cd web-client && npm run check
---

## Goal

The four reads `main.tsx` binds go out through `authorizedFetch`, so a signed-in player's strip,
record, ladder row and name all answer for the account and not for the device this browser happens to
be.

## Files

| File | Action |
| --- | --- |
| `web-client/src/main.tsx` | modify |
| `web-client/src/App.test.tsx` | modify |

Read, and do not edit: `web-client/src/account/authorized-fetch.ts`;
`web-client/src/profile/profile-provider.tsx` (the stable-reference rule this must not break);
`docs/protocol.md` *Sign in*.

## Scope

- One module-scope constant beside the existing four bindings:

  ```ts
  const apiFetch = authorizedFetch(
    (path, init) => window.fetch(path, init),
    localStorage,
  );
  ```

  and `readProfile`, `setName`, `readHistory` and `readLadder` each pass `apiFetch` where they passed
  the inline `window.fetch` arrow.
- **Module scope, not inline.** `ProfileProvider` and the two screen providers take the read as their
  only effect dependency, and a reference built during render re-runs the read on every render.
  `main.tsx` already carries that comment four times; this ticket keeps it true.
- Nothing else in `main.tsx` moves: the boot, the provider nesting and `roomCodeFromSearch` are
  untouched.
- The known consequence, recorded in a comment rather than discovered: `readFromApi` still returns
  `no-profile` without a request when no **device id** is stored, so a browser that holds a token and
  has never seen a `Welcome` reads nothing. It cannot arise — the device id is written by the first
  `Welcome` this browser ever receives and is never cleared (`ADR-0030` §8) — and the comment says
  which fact makes it unreachable.

## Out of scope

- **Sign-in, and sign-up.** Neither goes through this wrapper: sign-in carries no authentication at
  all by `docs/protocol.md`, and sign-up authenticates with `X-Device-Id` it sets itself.
  `TASK-041223` binds both to the plain `window.fetch`. **A refusal, not an omission** — a criterion
  greps `main.tsx` for it once those bindings exist.
- Changing `profile.ts`, `duel-page.ts`, `ladder-read.ts` or `set-name.ts`. They each take an
  `ApiFetch` and this ticket hands them a different one; that is the whole reason the wrapper exists
  where it does.
- Refreshing a read when the token changes. Sign-in and sign-out reload the document
  (`TASK-041213`, `TASK-041214`), so every read re-runs from a fresh boot.

## Tests

`web-client/src/App.test.tsx`, in the existing `describe("App")`, beside `binds the history read to
the browser fetch and the browser storage`.

| Test | Proves |
| --- | --- |
| `reads the profile, the record and the ladder under the session this browser holds` | With `"pd.sessionToken"` in the storage before render, a stubbed `window.fetch` records `Authorization: "Bearer …"` on the request to `/api/me`, on the one to `/api/me/duels` **and** on the one to `/api/standings`. **All three asserted**, so a wrapper applied to one binding cannot pass |
| `sets a name under the session this browser holds` | The `PUT /api/me/name` raised through the rendered tree carries the bearer header too. Separate because `setName` is the one write among the four bindings and is the easiest to forget |
| `reads under no session when this browser holds no token` | With an empty storage, none of the recorded requests carries an `Authorization` key. The anonymous path, which is every player until they sign in, and which `ADR-0036` keeps fully working |

## Acceptance criteria

- [ ] `App > reads the profile, the record and the ladder under the session this browser holds`
      passes, asserting the header on **all three** paths
- [ ] `App > sets a name under the session this browser holds` passes
- [ ] `App > reads under no session when this browser holds no token` passes, asserting the absence
      of the key rather than an undefined value
- [ ] `App > binds the history read to the browser fetch and the browser storage` and `App > binds
      the ladder read to the browser fetch and the browser storage` pass unchanged
- [ ] `grep -c 'authorizedFetch' web-client/src/main.tsx` returns `1` — one wrapper, not four
- [ ] `apiFetch` is declared at module scope, and no `authorizedFetch(` call appears inside a
      component body
- [ ] No file outside the two listed differs
- [ ] Every command in `verify:` exits 0

## Proof

1. Wrap only `readProfile` and leave the other three on the inline arrow.
   **`reads the profile, the record and the ladder under the session this browser holds` reddens on
   the record and the ladder**, and `sets a name under the session this browser holds` reddens too.
   The profile assertion passes — which is exactly why that test names three paths and not one, and
   why the name has a test of its own. Revert.
2. Build the wrapper inside `HistoryProvider` rather than at module scope.
   **Nothing reddens.** Record it: the defect is a read that re-runs on every render, which this
   suite cannot see and which `main.tsx`'s four existing comments exist to prevent. Review catches
   this, and the criterion above is what makes it checkable.
3. Wrap `window.fetch` once, globally, instead of per binding — assign the wrapper to `window.fetch`.
   **Every test above passes**, and it is wrong: the day `TASK-041223` binds sign-in, a request that
   `docs/protocol.md` says carries no authentication would carry a bearer token. Run it to see that
   nothing in this ticket's suite objects, then revert — this is the shortcut to refuse now rather
   than to discover in `TASK-041223`.
4. Leave the storage argument off — `authorizedFetch(fetch)` reading `localStorage` itself.
   **`npm run typecheck` reddens** at the call site. It is the compiler, not a test, and saying so is
   more useful than pretending the suite covers it.

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket.
