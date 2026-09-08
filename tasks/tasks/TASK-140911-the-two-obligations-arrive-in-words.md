---
schema: 2
id: TASK-140911
title: The two obligations arrive, in the card's words
type: task
status: backlog
parent: STORY-1409
module: web-client
estimate: XS
tier: haiku
review: light
files_touched: 2
labels: [client, account]
depends_on: [TASK-140910]
verify:
  - cd web-client && npm ci
  - grep -qF "CHANGEABLE_LINE" web-client/src/profile/name-text.ts
  - grep -qF "SPENT_LINE" web-client/src/profile/name-text.ts
  - sh -c 'cd web-client && m=$(grep -o "NAME-FORM: changeable: .*" ../design/screens/account.html | sed "s/^NAME-FORM: changeable: //; s/ -->$//") && test -n "$m" && grep -qF "$m" src/profile/name-text.ts && grep -qF "$m" src/profile/name-text.test.ts'
  - sh -c 'cd web-client && m=$(grep -o "NAME-FORM: spent: .*" ../design/screens/account.html | sed "s/^NAME-FORM: spent: //; s/ -->$//") && test -n "$m" && grep -qF "$m" src/profile/name-text.ts && grep -qF "$m" src/profile/name-text.test.ts'
  - sh -c 'cd web-client && NO_COLOR=1 npx vitest run src/profile/name-text.test.ts 2>&1 | grep -qF "name-text.test.ts  (8 tests)"'
  - cd web-client && npm run check
  - cd web-client && npm run build
  - python3 .github/scripts/lint_tickets.py
---

## Goal

`name-text.ts` exports `CHANGEABLE_LINE` and `SPENT_LINE`, the two sentences
`design/screens/account.html`'s `NAME-FORM` markers carry, copied character for character.

Nothing here chooses a word: a `verify` command reads each marker out of the merged card and
requires the sentence verbatim in **both** `name-text.ts` and `name-text.test.ts`. It carries an
explicit `test -n` on the extracted string, because a gate that loops over an empty match exits `0`
and proves nothing.

## Files

| File | Action |
| --- | --- |
| `web-client/src/profile/name-text.ts` | modify |
| `web-client/src/profile/name-text.test.ts` | modify |

Read, and do not edit:
`design/screens/account.html` — the `NAME-FORM: changeable:` and `NAME-FORM: spent:` markers;
[`ADR-0130`](../../docs/adr/ADR-0130-a-name-can-be-changed-and-the-name-it-leaves-is-spent.md) §5.

## Scope

- **Two exported constants**, beside `NAME_REMOVED_HEADING` and `NAME_REMOVED_BODY`, with a KDoc-style
  comment naming `ADR-0130` §5 and the card as their source.
- **`PERMANENCE_LINE` stays exported and rendered** for one more ticket. This ticket only adds; both
  new constants are unused by any component until `TASK-140912`, which is fine —
  `eslint --max-warnings 0` does not flag an exported constant, measured.
- **One new test**, `says a name can be changed and that the one given up is gone`, asserting both
  constants against their retyped literals. Seven tests become eight.

## Out of scope

- **Rendering them.** `TASK-140912`.
- **Deleting `PERMANENCE_LINE` or its test.** `TASK-140913`.
- **Choosing or editing a word.** `ADR-0091` §2: the card owns them. A sentence that reads wrongly is
  reported and `TASK-140909` is reopened; it is never fixed here, because the gate compares the two
  files and would go green on a matched pair of wrong sentences.

## Tests

`name-text.test.ts` — 7 tests on `develop` at `1c3c7fd9` and after `TASK-140910`, 8 after this ticket.

| Test | Proves |
| --- | --- |
| `says a name can be changed and that the one given up is gone` | `CHANGEABLE_LINE` and `SPENT_LINE` equal their retyped literals — the card's two sentences, and no third |

## What would still pass if the coder got it wrong

- **A test asserting `expect(CHANGEABLE_LINE).toBeDefined()`** passes against an empty string. The
  test asserts an **equality against a retyped literal**, which is the shape the four merged golden
  tests in this file already use.
- **A test that imported the constant and asserted it against itself** would be vacuous, which is
  why the assertion is a literal and why the `verify` loop reads the sentence from a **third** place
  — the merged card — and requires it in the source *and* the test. Two files agreeing is not
  evidence; three files, one of them the card that decided it, is.
- **Both constants set to the same sentence** would pass a per-constant equality if the two literals
  were copy-pasted. The two markers on the card are different sentences and the loop is run twice,
  once per marker, so a duplicated constant fails one of the two.

## Acceptance criteria

- [ ] `name-text.test.ts`'s new test passes and asserts both constants against retyped literals
- [ ] `name-text.test.ts` reports exactly 8 tests — 7 measured on `develop` at `1c3c7fd9` plus the
      one above
- [ ] The `NAME-FORM: changeable:` and `NAME-FORM: spent:` sentences from
      `design/screens/account.html` each appear verbatim in `name-text.ts` and in `name-text.test.ts`
- [ ] `npm run check` and `npm run build` exit 0 in `web-client`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
