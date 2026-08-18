---
schema: 2
id: TASK-041108
title: The name surface shows the name, or offers to set one and says what that costs
type: task
status: done
parent: STORY-0411
module: web-client
estimate: S
tier: haiku
review: standard
files_touched: 2
labels: [client, profile, ui, identity]
depends_on: [TASK-041107]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test 2>&1 | grep -qE 'Tests +400 passed \(400\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'shows the name the server sent, and offers no way to change it'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'offers the form to a player who has no name'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'says the choice is permanent before anything is sent'
  - cd web-client && npm run check
---

## Goal

`NameSurface` exists as a prop-driven component: it prints the name a player holds and offers
nothing else, or — for a player who holds none — offers one field and one button under the sentence
that says the choice cannot be undone.

## Files

| File | Action |
| --- | --- |
| `web-client/src/profile/NameSurface.tsx` | create |
| `web-client/src/profile/NameSurface.test.tsx` | create |

Read, not edited: `web-client/src/profile/name-text.ts`, `web-client/src/profile/set-name.ts`
(`SetNameOutcome`), `web-client/src/profile/ProfileStrip.tsx` (the panel classes a section is built
from here), `web-client/src/profile/profile-fixture.ts`.

## Scope

```tsx
export function NameSurface(props: {
  readonly profile: PlayerProfile;
  readonly setName: (name: string) => Promise<SetNameOutcome>;
}): ReactElement;
```

- **Props only for the profile.** No hook reads it, nothing fetches, nothing reads a global. The
  component holds local state for what the player typed and nothing else in this ticket.
- `profile.displayName !== null` renders the name inside
  `<section aria-label="your display name">` and **no input and no button**. A set name is permanent
  (`ADR-0029` §4); a field that still held it would invite an edit that can never land.
- `profile.displayName === null` renders, in this order: `PERMANENCE_LINE`, a `<label>`ed text input
  and a submit button reading *Set my name*. The line comes **before** the field, because
  `STORY-0411` requires the screen to say so at the moment the player can still avoid it.
- **The form does not send anything in this ticket.** `onSubmit` calls `preventDefault` and nothing
  else; `TASK-041110` gives it the request. `setName` stays in the props from here so that the two
  tickets do not fight over the signature.
- **No `<h1>`…`<h6>` anywhere.** `App.test.tsx` calls `getByRole("heading")`, which throws when a
  second heading exists, and this component mounts inside that tree in `TASK-041113`. The section's
  `aria-label` names the region without a heading — the rule `TASK-031107` already established for
  the strip.
- Classes come from the theme only; `styles/color-literals.test.ts` fails `npm run check` on a
  colour literal outside the token layer. Compose the classes `ProfileStrip.tsx` already uses.

## Out of scope

- The removal notice — `TASK-041109`, in this same file.
- Sending, and anything a server answers — `TASK-041110` and `TASK-041111`.
- Mounting it anywhere — `TASK-041113`.
- Designing it. **A refusal, not an omission:** `EPIC-06` has authored no screen for this surface
  and `STORY-0411` puts every colour and type decision there. Plain, token-composed, quiet. No token
  is edited, so nothing needs copying from `design/tokens/tokens.css` into
  `web-client/src/styles/`.
- Any placeholder text standing in for a name — `DEC-051`. A player who holds no name sees the offer
  here, not a word in place of a name.

## Tests

`web-client/src/profile/NameSurface.test.tsx`, describe block `"the name surface"`, rendered with
testing-library as `ProfileStrip.test.tsx` does. `setName` is a spy that records its calls and
returns a promise nothing awaits in this ticket.

| Test | Proves |
| --- | --- |
| `shows the name the server sent, and offers no way to change it` | **Two profiles in one render** — `aProfile({ displayName: "Ada" })` beside `aProfile({ displayName: "Grace" })` — put both names on screen, and `queryAllByRole("textbox")` and `queryAllByRole("button")` are both empty. Fails against a component that prints a constant, against one that keeps the form beside a set name, and against one that prints `playerId`; one name could not tell a rendered field from a hardcoded string |
| `offers the form to a player who has no name` | `aProfile({ displayName: null })` renders exactly one textbox and one submit button, and the textbox starts empty. Fails against a surface that shows the name state to a nameless player, and against one that pre-fills the field with anything |
| `says the choice is permanent before anything is sent` | The rendered text contains `PERMANENCE_LINE`'s exact sentence — typed out as a literal in the test — and the permanence line's node precedes the textbox in document order (`compareDocumentPosition`), and `setName` was not called. Fails against copy shown only after a failed send, against a line placed under the button, and against a component that fires a request on mount |

Three tests added to 397, so the suite reports **400**.

## Acceptance criteria

- [ ] `the name surface > shows the name the server sent, and offers no way to change it` passes,
      from two profiles in one render
- [ ] `the name surface > offers the form to a player who has no name` passes
- [ ] `the name surface > says the choice is permanent before anything is sent` passes, asserting
      document order and that `setName` was never called
- [ ] `NameSurface.tsx` contains no `<h1>`…`<h6>`
- [ ] `NameSurface.tsx` imports no `fetch`, reads no `localStorage`, and calls no hook other than
      `useState`
- [ ] `grep -c 'playerId' web-client/src/profile/NameSurface.tsx` returns `0`
- [ ] `npm run check` passes, so no colour literal entered the client
- [ ] No file outside `web-client/src/profile/` differs
- [ ] `npm run --silent test` reports `Tests  400 passed (400)`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
