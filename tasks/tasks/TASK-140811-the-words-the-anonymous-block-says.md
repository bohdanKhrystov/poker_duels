---
schema: 2
id: TASK-140811
title: The words the anonymous block says
type: task
status: backlog
parent: STORY-1408
module: web-client
estimate: XS
tier: haiku
review: light
files_touched: 2
labels: [client, account, copy]
depends_on: [TASK-140810]
verify:
  - cd web-client && npm ci
  - awk 'NR==FNR { if (match($0, /<!-- ANON-BLOCK: .* -->/)) { want[++n] = substr($0, RSTART+17, RLENGTH-21) } ; next } { all = all $0 "\n" } END { if (n != 3) exit 1; for (i = 1; i <= n; i++) if (index(all, want[i]) == 0) exit 1 }' design/screens/account.html web-client/src/account/account-text.ts
  - awk 'NR==FNR { if (match($0, /<!-- ANON-BLOCK: .* -->/)) { want[++n] = substr($0, RSTART+17, RLENGTH-21) } ; next } { all = all $0 "\n" } END { if (n != 3) exit 1; for (i = 1; i <= n; i++) if (index(all, want[i]) == 0) exit 1 }' design/screens/account.html web-client/src/account/account-text.test.ts
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF "states every sentence exactly, character for character"
  - sh -c '! grep -qiF "real account" web-client/src/account/account-text.ts'
  - sh -c '! grep -qiF "upgrade" web-client/src/account/account-text.ts'
  - sh -c '! grep -qiF "promote" web-client/src/account/account-text.ts'
  - cd web-client && npm run check
  - cd web-client && npm run build
  - python3 .github/scripts/lint_tickets.py
---

## Goal

`account-text.ts` owns the three sentences `ADR-0125` §3 requires, **character for character as the
merged design card prints them**, and `account-text.test.ts` states each one as a literal.

## Files

| File | Action |
| --- | --- |
| `web-client/src/account/account-text.ts` | modify |
| `web-client/src/account/account-text.test.ts` | modify |

Read, and do not edit:
`design/screens/account.html` — the **No password yet** frame and its three `<!-- ANON-BLOCK: … -->`
comments. The sentences are **its** decision, landed by `TASK-140810`; this ticket copies them and
chooses none of them;
[`ADR-0125`](../../docs/adr/ADR-0125-the-account-screen-names-the-anonymous-profile-and-owns-the-door.md)
§3.

## Scope

- Add three exported string constants to `account-text.ts` — one per `<!-- ANON-BLOCK: … -->` line,
  in the order the card prints them, each holding that line's sentence **exactly**: same words, same
  punctuation, same spacing, no added full stop and none removed. Suggested names, in the file's
  existing register: `ANONYMOUS_STATE`, `ANONYMOUS_COST`, `ANONYMOUS_WAY_OUT`. Give the group one
  KDoc block citing `ADR-0125` §3 and naming which obligation each discharges.
- Add all three names to the sorted export list in `account-text.test.ts`'s
  `states every sentence exactly, character for character` — that assertion is an **exact** list, so
  an extra or a missing export fails there even if every literal still matches — and add one
  `expect(accountText.X).toBe("…")` per constant, written as a literal.
- **Write the literals out.** Do not build them from a shared prefix, a template or another constant:
  a golden string that references the thing it is checking cannot catch it.

## Out of scope

- Rendering. `AccountScreen.tsx` does not import these until `TASK-140812`, and that is deliberate —
  a string with no reader still fails this ticket's export-list assertion if it is missing, so the
  words are gated before the screen exists to show them.
- `NO_PROFILE_YET`, `SIGNED_UP`, `PASSWORD_ROUTE_LIVE`, `SIGN_UP_LABEL`, `deviceRouteLine` and every
  other existing export. None of them moves, and `ADR-0125` §3 leaves `Give this profile a password`
  as the words for the act.
- Choosing or editing any word. If a card sentence reads wrongly to you, **report it and stop** — it
  is the card's, under the human's eye, and changing it here would put two versions of one sentence
  in the repository.

## Tests

`account-text.test.ts` — the file's single case `states every sentence exactly, character for
character` grows by three literals and three list entries. No new `it` block.

| Test | Proves |
| --- | --- |
| `states every sentence exactly, character for character` | `Object.keys(accountText).sort()` equals the exact list including the three new names, and each new constant equals its literal. The two `awk` gates in `verify:` then prove those literals are the card's, in both files |

## Acceptance criteria

- [ ] Each of the card's three `<!-- ANON-BLOCK: … -->` sentences appears verbatim in
      `account-text.ts` and again in `account-text.test.ts` — both `awk` gates exit 0
- [ ] `states every sentence exactly, character for character` passes, with its export list extended
      by exactly three names and no name removed
- [ ] `grep -i` finds no `real account`, `upgrade` or `promote` in `account-text.ts`
- [ ] `npm run check` and `npm run build` exit 0
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md).
