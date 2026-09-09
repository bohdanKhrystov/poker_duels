---
schema: 2
id: TASK-140914
title: The account screen carries the name form
type: task
status: done
parent: STORY-1409
module: web-client
estimate: XS
tier: haiku
review: standard
files_touched: 2
labels: [client, account]
depends_on: [TASK-140913]
verify:
  - cd web-client && npm ci
  - grep -qF "NameSurface" web-client/src/account/AccountScreen.tsx
  - sh -c 'cd web-client && NO_COLOR=1 npx vitest run src/account/AccountScreen.test.tsx 2>&1 | grep -qF "AccountScreen.test.tsx  (23 tests)"'
  - sh -c 'cd web-client && NO_COLOR=1 npx vitest run src/lobby/Lobby.test.tsx 2>&1 | grep -qF "Lobby.test.tsx  (113 tests)"'
  - cd web-client && npm run check
  - cd web-client && npm run build
  - python3 .github/scripts/lint_tickets.py
---

## Goal

`AccountScreen` takes an optional `setName` and renders `NameSurface` under the heading when it has
both a profile in hand and that prop — `ADR-0130` §6's *one form, on the account screen*, built
before anything passes it.

## Files

| File | Action |
| --- | --- |
| `web-client/src/account/AccountScreen.tsx` | modify |
| `web-client/src/account/AccountScreen.test.tsx` | modify |

Two, and green on its own: the prop is **optional**, `Lobby` does not pass it yet, and the front
door still renders the surface, so nothing outside these two files changes behaviour. `Lobby.test.tsx`
holds at 104 tests, which is a `verify` gate rather than an assumption.

Read, and do not edit:
[`ADR-0130`](../../docs/adr/ADR-0130-a-name-can-be-changed-and-the-name-it-leaves-is-spent.md) §6;
`web-client/src/profile/NameSurface.tsx` — its two props and its `aria-label`;
`web-client/src/profile/profile-provider.tsx` — `useReportNameWrite` returns a no-op with no
provider above it, which is why `AccountScreen` stays renderable alone.

## Scope

- **One optional prop**, `readonly setName?: (name: string) => Promise<SetNameOutcome>`, beside the
  four optional props the screen already takes.
- **One conditional render**, immediately after the `<h2>{ACCOUNT_HEADING}</h2>` and before the
  route lines: `profile !== null && profile.kind === "profile" && setName !== undefined`.
- **The screen's KDoc gains a sentence**: it now carries the name form as well
  (`ADR-0130` §6), and `NameSurface`'s own provider hook degrades to a no-op with no
  `ProfileProvider` above, so the screen is still renderable in a test alone (`ADR-0060` §4).
- **The condition mirrors the one it will replace on the front door** — `Lobby.tsx:434` reads
  `profile !== null && profile.kind === "profile" && setName !== null` — so `TASK-140917` is a
  deletion rather than a translation.

## Out of scope

- **Passing the prop from `Lobby`.** `TASK-140915`.
- **Removing the front-door render.** `TASK-140917`.
- **Any change to `NameSurface`.** `TASK-140918` is what makes it offer a form to a named player.
- **Re-keying `showPasswordRoute`, `showSignUp`, `showSignInDoor` or `showAttach`.** `ADR-0132` §5
  and `ADR-0125` §4 keep every one of their predicates; this ticket adds one condition and moves no
  other.

## Tests

`AccountScreen.test.tsx` — 15 tests on `develop` at `1c3c7fd9`, 18 after.

| Test | Proves |
| --- | --- |
| `carries the name form when it is given one` | with a `profile` state and a `setName` spy, `getByLabelText("your display name")` finds the surface, and it follows the `Account` heading in document order |
| `carries no name form when it is given no setName` | the **same** profile state with `setName` omitted renders no `your display name` region — the prop, not the profile, is what decides |
| `carries no name form without a profile in hand` | with a `setName` spy and each of `null`, `loading`, `no-profile` and `unavailable` in turn, no `your display name` region renders |

## What would still pass if the coder got it wrong

- **If the condition were only `setName !== undefined`**, the first two tests pass and the third
  fails — which is why the third enumerates all four non-`profile` states rather than one. A single
  `null` case would leave `loading`, `no-profile` and `unavailable` unproven, and those are three
  different shapes of the discriminated union.
- **If the condition were only `profile.kind === "profile"`**, the first and third pass and the
  second fails. **The two tests differ in exactly one input** — the same profile, with and without
  the prop — so neither a constant `true` nor a constant `false` satisfies both.
- **If the surface were rendered above the heading**, both positive assertions still pass;
  the document-order assertion in the first test is the only gate, and `ADR-0125` §3 keeps `Account`
  the screen's one heading.
- **If a second `<h2>` arrived with the form**, nothing here fails. That is `ADR-0125` §3's rule
  rather than this ticket's, and `NameSurface` contributes no heading today — a fact
  `Lobby.test.tsx`'s `renders the lobby with no headings from the name surface` already pins and
  which `TASK-140917` moves rather than deletes.

## Acceptance criteria

- [ ] `AccountScreen.test.tsx.carries the name form when it is given one` passes
- [ ] `AccountScreen.test.tsx.carries no name form when it is given no setName` passes
- [ ] `AccountScreen.test.tsx.carries no name form without a profile in hand` passes, covering
      `null`, `loading`, `no-profile` and `unavailable`
- [ ] `AccountScreen.test.tsx` reports exactly 18 tests — 15 measured on `develop` at `1c3c7fd9`
      plus exactly the three above
- [ ] `Lobby.test.tsx` reports exactly 104 tests, all passing and unedited
- [ ] `npm run check` and `npm run build` exit 0 in `web-client`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
