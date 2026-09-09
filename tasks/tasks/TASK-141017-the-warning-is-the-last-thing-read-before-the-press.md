---
schema: 2
id: TASK-141017
title: The warning is the last thing read before the press
type: task
status: done
parent: STORY-1410
module: web-client
estimate: XS
tier: haiku
review: standard
files_touched: 1
labels: [client, account]
depends_on: [TASK-141009]
verify:
  - cd web-client && npm ci
  - sh -c 'cd web-client && NO_COLOR=1 npx vitest run src/account/SignOutControl.test.tsx 2>&1 | grep -qF "SignOutControl.test.tsx  (10 tests)"'
  - sh -c 'cd web-client && grep -q "ADR-0143" src/account/SignOutControl.test.tsx'
  - git diff --exit-code -- web-client/src/account/SignOutControl.tsx web-client/src/account/account-text.ts
  - cd web-client && npm run check
  - cd web-client && npm run build
  - python3 .github/scripts/lint_tickets.py
---

## Goal

`SignOutControl.test.tsx` pins `ADR-0143` §2 obligation 2 for both sign-out sentences: the claim is
the **last thing a player reads before the control that performs the act**, with no other sentence
between them.

## Why this exists

`ADR-0143` §4 tabulates every irreversible act in the product and records that three of the four
conform today, `SignOutControl` among them. Nothing tests that. Grepping the whole of
`web-client/src` for `ADR-0143` returns no test at all, so the relation is asserted in an ADR and
enforced by nobody.

That matters more after `TASK-141008` and `TASK-141009` than it did before. `TASK-141008` added a
second sentence, `SIGN_OUT_HANDS_A_NEW_PROFILE`, and `TASK-141009` makes the control render
whichever of the two applies. Obligation 2 now has to hold for a sentence chosen at runtime rather
than for one literal, and the branch that chooses it is new code.

Obligation 2 is a **relation**, not a drawing (`ADR-0143` §2, and `ADR-0091` §2 leaves the drawing
to the card). Relations are what a later refactor breaks without breaking anything else — inserting
one explanatory line between the sentence and the button costs nothing at the type level and no
existing test would notice.

`TASK-141008`'s coder reported, unprompted, that its diff pins none of this and deferred it to
`TASK-141009`. `TASK-141009` mentions neither `ADR-0143` nor ordering anywhere in its Goal, Scope,
Files or Tests — so that deferral had nowhere to land. This is where it lands.

## Files

| File | Action |
| --- | --- |
| `web-client/src/account/SignOutControl.test.tsx` | modify |

Read, do not edit: `web-client/src/account/SignOutControl.tsx`,
`docs/adr/ADR-0143-irreversibility-is-said-last-and-never-coloured.md` §2 obligation 2 and §4,
`web-client/src/account/account-text.ts`.

## Scope

- Two tests, one per branch of `signOutWarning`, in the confirming step: the element carrying the
  sentence is the **immediately preceding sibling** of the group holding the confirming control, and
  no other text node sits between them. Assert the relation over the rendered DOM, not over the
  source.
- `ADR-0143` is named in a comment beside them, so the next grep for the ADR finds its test.
- The count gate moves 8 → 10. **I wrote this ticket with 5 → 7, and that was already stale when
  I filed it**: `TASK-141009` had taken the file to 8. Twenty-two literals in this epic have been
  corrected for exactly this reason, and filing a twenty-third while complaining about the other
  twenty-two is worth recording rather than quietly fixing. The lesson is the same one the epic
  keeps teaching: measure at dispatch, never at authoring.

## Out of scope

- **Colour.** `ADR-0143` §1 also refuses a danger register. That is a treatment claim about class
  names, it is a different assertion, and jsdom computes no styles — it belongs with the card, not
  here.
- **`RevokeControl` and the name form.** The same obligation binds both, and neither is tested
  either. Both are outside `STORY-1410`; they are named here so the gap is recorded rather than
  closed by implication.
- **`SignOutControl.tsx`.** `ADR-0143` §4 says it conforms today; this ticket observes that and
  changes nothing. The `git diff --exit-code` above says so.
- **`account-text.ts`.** `TASK-141008` owns the sentences.

## Tests

| Test | What it pins |
| --- | --- |
| `the keeping sentence is the last thing before the confirming press` | obligation 2 for `signOutWarning(false)` |
| `the abandoning sentence is the last thing before the confirming press` | obligation 2 for `signOutWarning(true)` |

## What would still pass if the coder got it wrong

- Asserting only that the sentence and the button are both present leaves the relation unpinned —
  that is what the five merged tests already do, and it is why this ticket exists.
- Asserting document order alone (`indexOf(sentence) < indexOf(button)`) is not obligation 2:
  `TASK-140914` shipped exactly that shape for the name form and review proved it permits the
  element to sit anywhere below. The assertion must pin **adjacency**, not precedence.
- Testing one branch only makes the other branch's sentence free to sit anywhere.

## Acceptance

- [ ] `SignOutControl.test.tsx` reports `(10 tests)` and all pass
- [ ] Inserting any sentence between the warning and the confirming control reddens both new tests,
      and the file is restored afterwards
- [ ] `SignOutControl.tsx` and `account-text.ts` are byte-identical to `develop`
- [ ] `npm run check` and `npm run build` exit 0 in `web-client`
