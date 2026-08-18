---
schema: 2
id: TASK-041110
title: The surface sends once, and shows the name that came back
type: task
status: ready
parent: STORY-0411
module: web-client
estimate: S
tier: sonnet
review: standard
files_touched: 2
labels: [client, profile, ui, identity]
depends_on: [TASK-041109]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test 2>&1 | grep -qE 'Tests +404 passed \(404\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'sends what the player typed, once, however many times the button is pressed'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'shows the name the server canonicalised, never the one that was typed'
  - cd web-client && npm run check
---

## Goal

Submitting the form calls `setName` once with the string in the field, and what the player sees
afterwards is the name the **server** returned.

## Files

| File | Action |
| --- | --- |
| `web-client/src/profile/NameSurface.tsx` | modify — the submit handler and the named state |
| `web-client/src/profile/NameSurface.test.tsx` | modify — two tests added |

Read, not edited: `web-client/src/profile/set-name.ts` (`SetNameOutcome`),
`web-client/src/store/reconnect.test.tsx` (how an async component test is written here).

## Scope

- `onSubmit` calls `preventDefault`, then `setName(typed)` with the field's value **exactly as
  typed** — no trim, no case change. `ADR-0029` §2 puts canonicalisation on the server and §5
  returns the whole profile so the client is told, not left to assume.
- **While a request is in flight the button is disabled and a second submit sends nothing.** A name
  is permanent and the answer to a double-send is a `200` for the winner and a `403` for the loser
  of a race the player never asked for.
- A `{ kind: "named" }` outcome puts `outcome.profile.displayName` into local state and the surface
  renders its *has a name* state from it — the form goes, as it does for a player who arrived with a
  name. The `profile` prop is stale by then and is not consulted for the name again; nothing
  re-reads `GET /api/me`.
- **The field is repopulated from the response and never from the input.** That is the whole reason
  `ADR-0029` §5 answers `200` with a profile rather than `204`.
- Every other outcome is `TASK-041111`'s; until then they may leave the form as it was.

## Out of scope

- The refusal sentences and what each leaves on screen — `TASK-041111`, in this same file.
- Refreshing the profile strip beside it. **A refusal, not an omission:** the strip renders the read
  the provider ran at mount, so a name set in this session shows there on the next load. Making the
  strip live needs a shared, refreshable profile and that is not this story's — nothing is ticketed
  for it.
- Any client-side validation of the typed string. An empty field may be submitted and the server
  answers `400`; a client that pre-refused would be enforcing a rule `ADR-0029` §3 expects to move.

## Tests

`web-client/src/profile/NameSurface.test.tsx`, describe block `"the name surface"`. Use
`fireEvent`/`findBy*` and await the surface's own change; **no test sleeps on a real clock**.

| Test | Proves |
| --- | --- |
| `sends what the player typed, once, however many times the button is pressed` | **Two typed strings across two renders** — `"  Ada  "` then `"Grace"` — each producing exactly one `setName` call whose argument is the string byte for byte, and pressing the button twice before the promise settles still leaves the call count at `1`. Fails against a client that trims, against one that sends the canonical form, and against a handler with no in-flight guard |
| `shows the name the server canonicalised, never the one that was typed` | Typing `"  ada  "` against `{ kind: "named", profile: aProfile({ displayName: "Ada" }) }` puts `Ada` on screen, leaves no textbox, and the rendered text contains neither `"  ada  "` nor a lower-case `ada` standing alone. The input is one the server would change, as `STORY-0411` requires. Fails against a surface that echoes the field, which is the exact defect `ADR-0029` §5 exists to prevent |

Two tests added to 402, so the suite reports **404**.

## Acceptance criteria

- [ ] `the name surface > sends what the player typed, once, however many times the button is
      pressed` passes, with two typed strings
- [ ] `the name surface > shows the name the server canonicalised, never the one that was typed`
      passes
- [ ] The five tests from `TASK-041108` and `TASK-041109` pass unchanged
- [ ] `grep -c 'trim\|toLowerCase\|normalize' web-client/src/profile/NameSurface.tsx` returns `0`
- [ ] No test in `NameSurface.test.tsx` calls `setTimeout`, `vi.advanceTimersByTime` or any real
      clock
- [ ] `npm run --silent test` reports `Tests  404 passed (404)`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
