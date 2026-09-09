---
schema: 2
id: TASK-140918
title: A player who holds a name is offered the form that changes it
type: task
status: done
parent: STORY-1409
module: web-client
estimate: S
tier: sonnet
review: standard
files_touched: 2
labels: [client, account]
depends_on: [TASK-140917]
verify:
  - cd web-client && npm ci
  - sh -c 'cd web-client && NO_COLOR=1 npx vitest run src/profile/NameSurface.test.tsx 2>&1 | grep -qF "NameSurface.test.tsx  (13 tests)"'
  - sh -c 'cd web-client && NO_COLOR=1 npx vitest run src/lobby/Lobby.test.tsx 2>&1 | grep -qF "Lobby.test.tsx  (114 tests)"'
  - sh -c 'cd web-client && NO_COLOR=1 npx vitest run src/account/AccountScreen.test.tsx 2>&1 | grep -qF "AccountScreen.test.tsx  (23 tests)"'
  - sh -c '! grep -qiE "suggest" web-client/src/profile/NameSurface.tsx'
  - sh -c 'test $(grep -c "return (" web-client/src/profile/NameSurface.tsx) -eq 1'
  - cd web-client && npm run check
  - cd web-client && npm run build
  - python3 .github/scripts/lint_tickets.py
---

## Goal

One surface does both acts. A player who already holds a display name sees the name they hold, both
of `ADR-0130` §5's obligations, and an **empty** field they can write a new name into — no
suggestion, no confirmation press, no quota.

## Files

| File | Action |
| --- | --- |
| `web-client/src/profile/NameSurface.tsx` | modify |
| `web-client/src/profile/NameSurface.test.tsx` | modify |

Two. This lands **last** on purpose: with the form still on the front door it would put a rename
control there, which is the one placement `ADR-0130` §6 refuses. `TASK-140917` took it away one
ticket earlier, and `Lobby.test.tsx`'s count is a gate here so nothing about the front door moved
back.

Read, and do not edit:
[`ADR-0130`](../../docs/adr/ADR-0130-a-name-can-be-changed-and-the-name-it-leaves-is-spent.md) §1,
§5 and §6;
[`ADR-0134`](../../docs/adr/ADR-0134-a-rename-spends-before-it-replaces.md) §5 — a case-only change
of your own name is a `409`, forever.

## Scope

- **The early return for `displayName !== null` goes.** Today the surface renders one `<p>` with the
  name and nothing else. It now renders the name **and** the form, in the same section, with the two
  obligations still above the field.
- **The field starts empty**, on both branches. `ADR-0130` §5: the change form offers no suggestion,
  because *"a suggested replacement would be the product proposing that somebody stop being who they
  are"*. Pre-filling with the name the player holds is the same proposal in a quieter voice and is
  refused too.
- **`wonName` keeps its job** — after a successful write the surface renders the canonical string the
  server returned, never the typed one (`ADR-0029` §5) — and now that string may be a *replacement*
  rather than a first name. Its comment says so.
- **The in-flight guard stays and matters more.** `ADR-0130` §1 makes a second submit a *rename*, so
  two presses that both landed would spend two strings. `sends what the player typed, once, however
  many times the button is pressed` must stay green and unweakened.
- **The removal notice's condition is unchanged**:
  `profile.displayName === null && profile.displayNameRemoved`. A player who renamed themselves is
  never shown it (`ADR-0130` §4), and a player who holds a name cannot satisfy the first conjunct.
- **No control clears a name.** `ADR-0130` §1: there is no way back to `null` by choice, and the only
  transition that makes a named player nameless is still an operator's takedown.

## Out of scope

- **Anything the server does with the write.** `TASK-140902` and `TASK-140906` shipped it.
- **A *changes remaining* counter, a confirmation step or a warning about the budget.**
  `ADR-0130` §1 and §7; `ADR-0134` §6 — a rate limit is only ever seen after the fact.
- **A suggestion.** `ADR-0119` §4's generator belongs to `NameAsk` and `STORY-1407`; a `verify`
  command refuses the word in this file.
- **A *formerly known as* line, or any display of the name being given up.** `ADR-0130` §2 and §3.

## Tests

`NameSurface.test.tsx` — 10 tests after `TASK-140912`, 13 after.

| Test | Proves |
| --- | --- |
| `offers the form to a player who already holds a name` | *(new)* with `displayName: "Ada"`, the section shows `Ada`, both obligations, exactly one textbox whose value is `""`, and exactly one button |
| `a change is sent exactly as typed, and the surface adopts what the server answered` | *(new)* a holder of `Ada` types `Grace`, submits, `setName` is called once with `"Grace"`, and the section then shows the **server's** answer (`"Grace "` canonicalised to `"Grace"`) and no longer shows `Ada` |
| `offers no suggestion and never proposes the name already held` | *(new)* rendered for `displayName: "Ada"` **and** for `displayName: null`, the field's value is `""` in both — the two renders disagree on the profile and agree on the field |
| `shows the name the server sent, and offers no way to change it` | *(rewritten)* renamed to `shows the name the server sent, beside the form that can change it`; the merged assertion that the name is printed stays, the assertion that there is no form is the one that inverts |
| `sends what the player typed, once, however many times the button is pressed` | *(unchanged, must stay green)* two presses send one write |
| `tells the player their name was removed, and tells the player beside them nothing` | *(unchanged, must stay green)* the notice's condition did not move |

## What would still pass if the coder got it wrong

- **If the early return were deleted and the field pre-filled with the held name**, the first two
  tests pass: a textbox exists and a submit sends what it holds. `offers no suggestion and never
  proposes the name already held` is the only gate, and it needs **two renders that disagree** —
  one profile with a name and one without — because a field that always contained `""` and a field
  that echoed the profile are indistinguishable on the nameless render alone. `""` is exactly the
  value the bug leaves unchanged there.
- **If the surface printed the name but dropped the two obligations on the named branch**, the first
  test fails on the two `getByText` calls. They are asserted on the **named** render specifically,
  because `TASK-140912`'s test asserts them on the nameless one and neither implies the other.
- **If `wonName` were ignored and the typed string rendered**, the second test fails only because
  the fake server answers a *different* string from the one typed — `"Grace "` in, `"Grace"` back.
  A fake that echoed its input would make that test vacuous, and the test must therefore use a
  canonicalisation the client cannot perform for itself.
- **If the in-flight guard were lost while restructuring the branches**, the merged two-press test
  fails. Under `ADR-0130` §1 that defect now spends a second string irreversibly, which is why it is
  named here rather than left to the file's own history.
- **If the front door regressed**, nothing in this file notices; `Lobby.test.tsx`'s and
  `AccountScreen.test.tsx`'s counts are `verify` commands for that reason.

## Acceptance criteria

- [ ] `NameSurface.test.tsx.offers the form to a player who already holds a name` passes
- [ ] `NameSurface.test.tsx.a change is sent exactly as typed, and the surface adopts what the server
      answered` passes, with the fake answering a different string from the one typed
- [ ] `NameSurface.test.tsx.offers no suggestion and never proposes the name already held` passes,
      rendering both a named and a nameless profile
- [ ] `NameSurface.test.tsx` reports exactly 13 tests — 10 after `TASK-140912` plus exactly the three
      above, with the merged named-branch test rewritten rather than added
- [ ] `sends what the player typed, once, however many times the button is pressed` and
      `tells the player their name was removed, and tells the player beside them nothing` pass with
      no assertion weakened
- [ ] `Lobby.test.tsx` reports 114 tests and `AccountScreen.test.tsx` 23, both unedited — the
      ticket asked 105 and 18, both measured before `TASK-140911`, `TASK-140913`, `TASK-140914`
      and `TASK-140915` landed. Neither file is touched here, so the measured numbers are both
      what `develop` holds and what this change leaves
- [ ] The word `suggest` appears nowhere in `NameSurface.tsx`, case-insensitively
- [ ] `NameSurface.tsx` has exactly one `return (` — two on `develop` at `1c3c7fd9`, and losing the
      early return is the whole of the change
- [ ] `npm run check` and `npm run build` exit 0 in `web-client`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
