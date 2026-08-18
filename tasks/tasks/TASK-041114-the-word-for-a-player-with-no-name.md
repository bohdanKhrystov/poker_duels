---
schema: 2
id: TASK-041114
title: The word for a player who has no name
type: task
status: ready
parent: STORY-0411
module: web-client
estimate: XS
tier: haiku
review: standard
files_touched: 2
labels: [client, profile, copy, identity]
depends_on: [TASK-041113]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test 2>&1 | grep -qE 'Tests +413 passed \(413\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'says the same thing about a player with no name wherever it is asked'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'says nothing about why a name is missing'
  - cd web-client && npm run check
---

## `DEC-051` is answered — the string is `No name`

[`ADR-0058`](../../docs/adr/ADR-0058-where-a-name-would-be-the-client-prints-no-name.md) §1 fixes
it: where a display name would be printed and the player holds none, the client prints **`No
name`** — sentence case, one `U+0020` between the words, no full stop, no brackets, no dash. Type
that literal out; do not compose it, abbreviate it or reword it.

§2 fixes that it is **one treatment on every surface, for every viewer** — a player's own strip
prints `No name`, not *You* — which is why there is one function and no second-person variant. §3
fixes that it says nothing about why: a player who never set a name and one whose name was removed
produce the same two words, because `ADR-0052` §5's invisibility survives only if two surfaces
cannot decide separately.

The answer is a **single string**, which is the shape this ticket was written for, so the split
stands: no re-split, and `TASK-041115` and `TASK-041116` are unchanged.

## Goal

`name-text.ts` answers what to print where a name would be for a player who holds none, in one place
that every surface calls.

## Files

| File | Action |
| --- | --- |
| `web-client/src/profile/name-text.ts` | modify — one export |
| `web-client/src/profile/name-text.test.ts` | modify — two tests added |

Read, not edited: `docs/adr/ADR-0058-where-a-name-would-be-the-client-prints-no-name.md` §§1–3,
and `docs/adr/ADR-0052-a-takedown-is-told-to-the-player-it-happened-to.md` §5.

## Scope

```ts
/** What stands where a display name would be, for a player who holds none. */
export function nameOrNone(displayName: string | null): string;
```

- One function, used by both surfaces: a player's own strip and an opponent on a duel line. It takes
  the name and answers it, or answers the treatment. Callers never branch on `null` themselves —
  one `if` in one file is what keeps the two surfaces from drifting apart.
- The treatment string is **exactly** the answering ADR's, quoted in the module's KDoc with the ADR
  number beside it.
- It says nothing about *why* the name is missing, and there is no second function and no parameter
  for the removed case. `ADR-0052` §5: to everybody except the player themselves, the two `null`
  states are indistinguishable, and a second treatment would be the mark that ADR refuses.

## Out of scope

- The removal notice, which is the player's own screen and already exists (`TASK-041109`). It is the
  one place the two `null` states differ, and it differs there because `ADR-0052` §1 says so.
- Rendering — `TASK-041115` (the strip) and `TASK-041116` (a duel line).
- Colour or type. `EPIC-06` owns the language; this ticket ships a string, not a style, and edits no
  token.

## Tests

`web-client/src/profile/name-text.test.ts`, describe block `"the name surface's words"`.

| Test | Proves |
| --- | --- |
| `says the same thing about a player with no name wherever it is asked` | `nameOrNone("Ada")` is `"Ada"` and `nameOrNone("Grace")` is `"Grace"` — two names, so a passthrough cannot be a constant — and `nameOrNone(null)` equals the answering ADR's literal, typed out in the test. Fails against a function that returns the treatment for everybody, against one that returns the empty string for `null`, and against a reworded treatment |
| `says nothing about why a name is missing` | `nameOrNone(null)` contains none of `removed`, `taken`, `banned`, `deleted`, `moderat` or `former`, in any case. Fails against a treatment that leaks the takedown onto a stranger's screen — the one thing `ADR-0052` §5 forbids by name, and the reason this string is a product decision rather than a coder's |

Two tests added to 411, so the suite reports **413**.

## Acceptance criteria

- [ ] The treatment string is `No name`, exactly as `ADR-0058` §1 fixes it — sentence case, one
      space, no full stop — and the module's KDoc quotes it with `ADR-0058` beside it
- [ ] `the name surface's words > says the same thing about a player with no name wherever it is
      asked` passes, with two names and the null case
- [ ] `the name surface's words > says nothing about why a name is missing` passes
- [ ] The five tests from `TASK-041107` pass unchanged
- [ ] `nameOrNone` is the only exported function in the client that branches on a `null` display
      name: `grep -rc 'displayName === null' web-client/src/` reports it in `name-text.ts` and
      nowhere under `web-client/src/profile/*.tsx` except `NameSurface.tsx`, which branches on the
      player's own state and not on a label
- [ ] `npm run --silent test` reports `Tests  413 passed (413)`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
