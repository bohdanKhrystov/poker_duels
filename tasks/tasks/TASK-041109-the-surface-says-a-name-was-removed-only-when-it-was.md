---
schema: 2
id: TASK-041109
title: The surface says a name was removed, only to the player it happened to
type: task
status: ready
parent: STORY-0411
module: web-client
estimate: XS
tier: haiku
review: standard
files_touched: 2
labels: [client, profile, ui, identity, moderation]
depends_on: [TASK-041108]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test 2>&1 | grep -qE 'Tests +402 passed \(402\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'tells the player their name was removed, and tells the player beside them nothing'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'keeps the notice out of the way of choosing again'
  - cd web-client && npm run check
---

## Goal

The name surface shows `ADR-0052` §2's notice when — and only when — the player holds no name and
one was removed from them, above the same form that offers them another.

## Files

| File | Action |
| --- | --- |
| `web-client/src/profile/NameSurface.tsx` | modify — one branch and one block |
| `web-client/src/profile/NameSurface.test.tsx` | modify — two tests added |

Read, not edited: `docs/adr/ADR-0052-a-takedown-is-told-to-the-player-it-happened-to.md` §1 and §2,
`web-client/src/profile/name-text.ts`.

## Scope

- The notice renders when `profile.displayName === null && profile.displayNameRemoved`, and never
  otherwise. Both halves are load-bearing (`ADR-0052` §1): a player who has since chosen a new name
  sees nothing, and a player who never set one sees nothing.
- It is `NAME_REMOVED_HEADING` as emphasised text and `NAME_REMOVED_BODY` beneath it, **above** the
  form from `TASK-041108`, inside the same `<section>`. Not a modal, not a toast, not a banner, and
  nothing to dismiss: `ADR-0052` §1 makes the telling a function of state that ends when the player
  sets a name, so there is nothing to acknowledge and nothing to remember.
- **The form is unchanged by it.** `ADR-0052` §4 adds no hold and no cooling-off: the same input and
  the same button, offered on the same terms.
- **No heading element**, for `TASK-041108`'s reason: the notice's first line is emphasis, not an
  `<h*>`.
- No `role="alert"`, and no colour that says *error*. The state is a fact about the player's
  account, not a failure of the screen, and `ADR-0052` §2 refuses to accuse.

## Out of scope

- Any reason, appeal, contact route or *why*. **A refusal, not an omission:** `ADR-0052` §3 records
  that nothing is stored — there is no actor, no reason and no log — so a client that offered a
  *find out more* would be promising a screen that cannot exist.
- Repeating the removed name. `ADR-0052`'s *Alternatives considered* rejects echoing it: the wire
  carries one boolean and no string, so the client could not print it if it wanted to.
- Marking anybody else's duel line. `ADR-0052` §5 makes a removed opponent byte-identical to one who
  never had a name; `TASK-041117` is the test that keeps it that way.
- Telling the account screen — `STORY-0412` owns it, and `ADR-0052`'s closing section says today the
  answer is that it does not.

## Tests

`web-client/src/profile/NameSurface.test.tsx`, describe block `"the name surface"`.

| Test | Proves |
| --- | --- |
| `tells the player their name was removed, and tells the player beside them nothing` | **One render holding both states**: a `NameSurface` for `aProfile({ displayName: null, displayNameRemoved: true })` beside one for `aProfile({ displayName: null, displayNameRemoved: false })`. `getAllByText(NAME_REMOVED_HEADING literal)` has length **1**, the body sentence appears once, and `getAllByRole("textbox")` has length **2**. Fails against a notice shown to everybody, against one shown to nobody, and against a notice that replaces the form instead of sitting above it. Two states in one render is what makes it non-vacuous — either single-fixture test passes against a constant |
| `keeps the notice out of the way of choosing again` | For the removed profile alone: the notice's node precedes the textbox in document order, `queryAllByRole("alert")` is empty, the rendered text contains no `banned`, `suspended`, `violation`, `report` or `appeal`, and a third profile with `displayName: "Ada", displayNameRemoved: true` — the impossible pair the server never sends — still shows the name and **no** notice. Fails against a notice below the form, against alarm styling, against moderation vocabulary `ADR-0052` §2 refuses, and against a branch keyed on `displayNameRemoved` alone |

Two tests added to 400, so the suite reports **402**.

## Acceptance criteria

- [ ] `the name surface > tells the player their name was removed, and tells the player beside them
      nothing` passes, with both states in one render
- [ ] `the name surface > keeps the notice out of the way of choosing again` passes
- [ ] The three tests from `TASK-041108` pass unchanged
- [ ] The notice's two strings come from `name-text.ts`; `NameSurface.tsx` contains no sentence of
      its own
- [ ] `grep -c 'role="alert"' web-client/src/profile/NameSurface.tsx` returns `0`
- [ ] `npm run --silent test` reports `Tests  402 passed (402)`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
