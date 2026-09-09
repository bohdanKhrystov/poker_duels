---
schema: 2
id: TASK-141020
title: A required prop is required in the typechecker
type: task
status: ready
parent: STORY-1410
module: web-client
estimate: XS
tier: sonnet
review: standard
files_touched: 2
labels: [client, account]
depends_on: [TASK-141013]
verify:
  - cd web-client && npm ci
  - sh -c 'cd web-client && NO_COLOR=1 npx vitest run src/account/SignOutControl.test.tsx 2>&1 | grep -qF "SignOutControl.test.tsx  (9 tests)"'
  - sh -c 'cd web-client && NO_COLOR=1 npx vitest run src/account/AccountScreen.test.tsx 2>&1 | grep -qF "AccountScreen.test.tsx  (25 tests)"'
  - sh -c 'cd web-client && test $(grep -c "@ts-expect-error" src/account/SignOutControl.test.tsx) -eq 1'
  - sh -c 'cd web-client && test $(grep -c "@ts-expect-error" src/account/AccountScreen.test.tsx) -eq 1'
  - git diff --exit-code -- web-client/src/account/SignOutControl.tsx web-client/src/account/AccountScreen.tsx
  - cd web-client && npm run check
  - cd web-client && npm run build
  - python3 .github/scripts/lint_tickets.py
---

## Goal

`signOutHandsANewProfile` is required on both `SignOutControl` and `AccountScreen`, and the
typechecker is made to say so. Today both declarations are correct and nothing would notice if
either became optional.

## Why this exists

`ADR-0135` §7 states the rule and its reason: *"a component that can be mounted without it is a
component whose absence is invisible from the outside."* Both `TASK-141009` and `TASK-141013`
obeyed it, and in both reviews the check was a human reading the declaration rather than a gate.

Then `TASK-141013`'s coder and reviewer each ran the obvious mutation — change
`readonly signOutHandsANewProfile: boolean` to `?: boolean` and give it `= false` at the destructure
— and it passed `tsc --noEmit`, `npm run check`, and all 1265 tests. No `grep` gate in either ticket
looks for optional syntax, and every test fixture supplies the prop, so nothing fails.

A default of `false` is not a neutral default. `ADR-0135` §6 makes `false` the *reassuring* answer:
the browser keeps the profile it owns. An unwired caller would therefore be told, silently and
wrongly, that signing out is safe. That is the direction §7 exists to prevent, and it is currently
prevented only by nobody having edited the type.

## Files

| File | Action |
| --- | --- |
| `web-client/src/account/SignOutControl.test.tsx` | modify |
| `web-client/src/account/AccountScreen.test.tsx` | modify |

Read, do not edit: `web-client/src/account/SignOutControl.tsx`,
`web-client/src/account/AccountScreen.tsx`,
`docs/adr/ADR-0135-the-server-says-which-sign-out-this-is-and-the-browser-forgets-one-key.md` §§6, 7.

## Scope

- One test per file. Each renders its component with **every other required prop supplied** and
  `signOutHandsANewProfile` omitted, under a `// @ts-expect-error` naming `ADR-0135` §7.
- The mechanism is the point: while the prop is required, the omission is an error and the directive
  is used. If the prop is ever made optional, the error disappears, the directive becomes unused,
  and `tsc` fails with *"Unused '@ts-expect-error' directive"*. The gate is the build, not an
  assertion.
- Each test also asserts the component still rendered, so it is a test and not a bare type
  annotation.
- Count gates: `SignOutControl.test.tsx` 8 → 9, `AccountScreen.test.tsx` 24 → 25.

## Out of scope

- **The components.** Both declarations are already correct; this ticket observes them and changes
  neither. The `git diff --exit-code` above says so.
- **Every other required prop in the client.** The same argument applies to many of them, and
  sweeping the codebase is not this ticket. These two carry `ADR-0135` §7 by name, which is why
  these two.
- **A lint rule.** Forbidding optional props by rule is a wider decision with its own costs, and it
  would need an ADR. If someone wants that later, this ticket is the evidence that the gap is real.

## Tests

| Test | What it pins |
| --- | --- |
| `SignOutControl cannot be mounted without being told which sign-out this is` | the prop is required on `SignOutControl` |
| `AccountScreen cannot be mounted without being told which sign-out this is` | the prop is required on `AccountScreen` |

## What would still pass if the coder got it wrong

- **Omitting any other required prop as well** makes the `@ts-expect-error` fire for that prop
  instead, so the directive stays used even after `signOutHandsANewProfile` becomes optional — and
  the gate silently stops testing what it names. Every other required prop must be supplied. This is
  the whole risk in this ticket and the reason it is `tier: sonnet` for two small tests.
- **`@ts-ignore` instead of `@ts-expect-error`** never fails when the error goes away; it is the
  same line with none of the value.
- **Asserting only that the render threw or did not throw** proves nothing about the type: the
  omitted prop is `undefined` at runtime either way, and the component may well render.
- A `grep` for `?:` would pass against `signOutHandsANewProfile ?: boolean` with a space, or a type
  widened elsewhere; the typechecker is what actually knows.

## Acceptance

- [ ] `SignOutControl.test.tsx` reports `(9 tests)` and `AccountScreen.test.tsx` `(25 tests)`, all
      passing
- [ ] Making either prop optional with a `= false` default makes `npm run check` fail on the unused
      `@ts-expect-error`, and both files are restored afterwards
- [ ] `SignOutControl.tsx` and `AccountScreen.tsx` are byte-identical to `develop`
- [ ] `npm run check` and `npm run build` exit 0 in `web-client`
