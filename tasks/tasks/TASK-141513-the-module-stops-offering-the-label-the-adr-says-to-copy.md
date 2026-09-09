---
schema: 2
id: TASK-141513
title: The module stops offering the label the ADR says to copy
type: task
status: ready
parent: STORY-1415
module: web-client
estimate: XS
tier: haiku
review: light
files_touched: 2
labels: [client, rematch, notice, text]
depends_on: [TASK-141511]
verify:
  - cd web-client && npm ci
  - sh -c 'cd web-client && test $(grep -c "export const" src/result/rematch-text.ts) -eq 5'
  - sh -c 'cd web-client && ! grep -q "NOT_NOW" src/result/rematch-text.ts'
  - sh -c 'cd web-client && test $(grep -c "NOT_NOW" src/result/RematchNotice.tsx) -eq 2'
  - sh -c 'cd web-client && NO_COLOR=1 npx vitest run src/design/card-text.test.ts 2>&1 | grep -qF "card-text.test.ts  (10 tests)"'
  - git diff --exit-code -- web-client/src/result/RematchNotice.tsx web-client/src/result/RematchControl.tsx
  - cd web-client && npm run check
  - cd web-client && npm run build
  - python3 .github/scripts/lint_tickets.py
---

## Goal

`rematch-text.ts` stops exporting `NOT_NOW`. Nothing imports it, and `ADR-0138` §4 says in as many
words that this label is **copied, not imported**.

## Why this exists

§4 sets out the module's contents as *"one set of words … the four things both surfaces state"*, and
its table lists five constants: `RIVAL_OFFERS`, `REMATCH_LABEL`, `DEALING_LEAD`, `DEALING_TAIL`,
`ROOM_GONE`. `NOT_NOW` is not among them. The section then says plainly:

> **`Not now` is copied, not imported.** The panel declares its own constant with the same value.
> Importing `account-offer-text.ts`'s `OFFER_DISMISS` would tie a rematch panel's label to an
> unrelated surface.

`TASK-141503` added a sixth export anyway. Measured on `develop` at `6cc436fb`:
`rematch-text.ts:6` exports `NOT_NOW = "Not now"`, `RematchNotice.tsx:17` declares its **own**
local `NOT_NOW` and uses it at line 115, `RematchNotice.test.tsx:8` declares another, and **no file
imports the module's**. It is dead the day it was written.

**The harm is not the dead line.** It is that the shared module now offers exactly the import §4
closed off. A later coder reaching for the panel's dismiss label finds it exported from the module
both surfaces share, imports it because that looks like the tidy thing to do, and the panel's label
is tied to the result screen's module — which is the coupling §4 refused, arrived at by the obvious
route rather than the forbidden one.

`ADR-0142`'s register also carries `NOT_NOW` in its `carded` list, so the register currently asserts
a card carries a string no component reads from the module.

Found on 2026-09-10 while reviewing `TASK-141511`, from a reviewer's aside that the constant was
*"defined but unused"*.

## Files

| File | Action |
| --- | --- |
| `web-client/src/result/rematch-text.ts` | modify |
| `web-client/src/design/card-text.test.ts` | modify |

Read, do not edit: `web-client/src/result/RematchNotice.tsx`,
`docs/adr/ADR-0138-the-panel-mounts-beside-the-lobby-and-the-dismissal-lives-in-the-mount.md` §4,
`docs/adr/ADR-0142-a-text-module-is-checked-against-the-rendered-card.md` §3.

## Scope

- Delete `export const NOT_NOW` from `rematch-text.ts`, leaving five exports.
- Remove `"NOT_NOW"` from `card-text.test.ts`'s `carded` list, since the export it named is gone.
  `ADR-0142` §3's partition is checked **both ways**, so a name left behind fails.
- Nothing else moves. `RematchNotice.tsx` keeps its own local constant and its two occurrences; the
  `git diff --exit-code` above says so.

## Out of scope

- **The panel's local constant.** `ADR-0138` §4 requires it. This ticket removes the redundant copy
  from the shared module, not the one the ADR asks for.
- **`account-offer-text.ts`'s `OFFER_DISMISS`.** Untouched, and the reason §4 gives for copying at
  all.
- **The other five exports.** All are imported and all stay.

## Tests

| Gate | What it pins |
| --- | --- |
| `export const` count is 5 | the sixth is gone |
| no `NOT_NOW` in `rematch-text.ts` | it is gone by name, not merely renamed |
| `NOT_NOW` appears twice in `RematchNotice.tsx` | the panel's own copy survives — declaration and use |
| `card-text.test.ts` at 10 tests | the register still passes with the name removed from `carded` |

## What would still pass if the coder got it wrong

- Deleting the export and leaving `"NOT_NOW"` in the `carded` list fails `ADR-0142`'s both-ways
  partition — which is the point of running that file's suite rather than only counting exports.
- Deleting the panel's **local** constant instead would satisfy an export count and break the panel;
  the `NOT_NOW` count of two in `RematchNotice.tsx` is what refuses it.
- Renaming rather than deleting leaves the temptation in place under another name, which is the whole
  defect.

## Acceptance

- [ ] `rematch-text.ts` has five exports and no `NOT_NOW`
- [ ] `RematchNotice.tsx` and `RematchControl.tsx` are byte-identical to `develop`
- [ ] `card-text.test.ts` reports `(10 tests)` and all pass
- [ ] `npm run check` and `npm run build` exit 0 in `web-client`
