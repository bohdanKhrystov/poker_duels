---
schema: 2
id: TASK-140910
title: 403 leaves the client and 429 arrives
type: task
status: backlog
parent: STORY-1409
module: web-client
estimate: S
tier: sonnet
review: standard
files_touched: 5
atomic:
  - "web-client `npm run typecheck` — `refusalSentence` and `mayTryAgain` in `name-text.ts` switch exhaustively over `Exclude<SetNameOutcome[\"kind\"], \"named\">` with no `default`, so moving the union fails both with `TS2366` *Function lacks ending return statement* and `TS2678` on the dead arm"
  - "web-client `npm run typecheck` — `name-text.test.ts` passes the literal `\"permanent\"` to those two functions at four call sites and fails `TS2345`"
  - "web-client `npm run typecheck` — `NameSurface.test.tsx` types two case lists as that union and fails `TS2322` at lines 329 and 392"
  - "web-client `npm run typecheck` — `set-name.test.ts`'s status table is typed `SetNameOutcome[\"kind\"]` and fails `TS2322` at line 209"
labels: [client, account, protocol]
depends_on: [TASK-140909]
verify:
  - cd web-client && npm ci
  - sh -c '! grep -qF "permanent" web-client/src/profile/set-name.ts web-client/src/profile/name-text.ts web-client/src/profile/set-name.test.ts web-client/src/profile/name-text.test.ts'
  - sh -c '! grep -qF "\"permanent\"" web-client/src/profile/NameSurface.test.tsx'
  - sh -c '! grep -qF "403" web-client/src/profile/set-name.ts'
  - grep -qF "case 429" web-client/src/profile/set-name.ts
  - sh -c 'cd web-client && NO_COLOR=1 npx vitest run src/profile/set-name.test.ts 2>&1 | grep -qF "set-name.test.ts  (5 tests)"'
  - sh -c 'cd web-client && NO_COLOR=1 npx vitest run src/profile/name-text.test.ts 2>&1 | grep -qF "name-text.test.ts  (7 tests)"'
  - sh -c 'cd web-client && NO_COLOR=1 npx vitest run src/profile/NameSurface.test.tsx 2>&1 | grep -qF "NameSurface.test.tsx  (10 tests)"'
  - sh -c 'cd web-client && m=$(grep -o "NAME-FORM: throttled: .*" ../design/screens/account.html | sed "s/^NAME-FORM: throttled: //; s/ -->$//") && test -n "$m" && grep -qF "$m" src/profile/name-text.ts && grep -qF "$m" src/profile/name-text.test.ts'
  - cd web-client && npm run check
  - cd web-client && npm run build
  - python3 .github/scripts/lint_tickets.py
---

## Goal

`SetNameOutcome` models the statuses `PUT /api/me/name` actually answers: `permanent` (`403`) is
gone and `throttled` (`429`) stands in its place, carrying the card's sentence — a statement about
**requests**, not about the name the player typed.

## Files

Five, **probed not remembered** (`ADR-0069`, `ADR-0070`). The union member was swapped in
`set-name.ts` alone and, in `web-client`, `npm run check` and `npm run build` — the commands
`.github/workflows/build.yml` runs on a pull request — were run in full. `tsc --noEmit` named eleven
errors across the four files below in one batch; the minimal propagation was applied and the
commands were run again, then once more for a runtime failure the compiler could not see, until both
exited `0`.

| File | Action | Why it cannot be fewer |
| --- | --- | --- |
| `web-client/src/profile/set-name.ts` | modify | The union member and the `switch` arm — the change itself |
| `web-client/src/profile/name-text.ts` | modify | `TS2366` on both functions at `name-text.ts:23` and `:48`, plus `TS2678` on the dead `case "permanent"` |
| `web-client/src/profile/name-text.test.ts` | modify | `TS2345` at lines 37, 55, 70 and 85 |
| `web-client/src/profile/NameSurface.test.tsx` | modify | `TS2322` at lines 329 and 392 — **and** a runtime failure at line 372 that no compiler reports: the case list retypes cleanly while still holding the old sentence as a string literal |
| `web-client/src/profile/set-name.test.ts` | modify | `TS2322` at line 209 |

`NameSurface.tsx` is **not** in this list, and that is measured: it imports `refusalSentence` and
`mayTryAgain` and branches on no outcome kind, so the union moving under it changes nothing there.

Read, and do not edit:
[`ADR-0134`](../../docs/adr/ADR-0134-a-rename-spends-before-it-replaces.md) §5;
`design/screens/account.html` — the `NAME-FORM: throttled:` marker, which is the sentence.

## Scope

- **`set-name.ts`**: `| { readonly kind: "permanent" } // 403` becomes
  `| { readonly kind: "throttled" } // 429`, and `case 403:` becomes `case 429:`. The `default`
  arm already maps an unmodelled status to `unavailable`, so removing `403` leaves no hole
  (`ADR-0134` §5).
- **`name-text.ts`**: `refusalSentence`'s `permanent` arm becomes a `throttled` arm carrying the
  card's marker sentence **character for character**; `mayTryAgain`'s `throttled` stays in the
  `false` group, because a `429` is not a name the player can fix by typing a different one.
- **The three test files carry the change through**, and `NameSurface.test.tsx`'s case list swaps
  both the kind **and** the retyped literal sentence. That literal is deliberately retyped rather
  than imported (its own comment says so) — an assertion against `refusalSentence`'s output would
  compare the function to itself.
- **No word is chosen here.** The sentence comes from `TASK-140909`'s card; a `verify` command reads
  the marker out of the card and requires it verbatim in both `name-text.ts` and its test. A card
  sentence that reads wrongly is reported, never edited here.

## Out of scope

- **`PERMANENCE_LINE`.** `TASK-140913` deletes it. This ticket leaves it exported and rendered.
- **The two obligations.** `TASK-140911`.
- **`NameSurface.tsx`.** Measured: no gate names it.
- **`web-client/src/e2e/account-server.ts` and any stub that answers `429`.** Nothing in the arc
  drives a budget; adding one is a later ticket and is not ticketed.

## Tests

Per-file counts measured on `develop` at `1c3c7fd9` and unchanged by this ticket — every case is a
rewrite, not an addition: `set-name.test.ts` 5, `name-text.test.ts` 7, `NameSurface.test.tsx` 10.

`set-name.test.ts`

| Test | Proves |
| --- | --- |
| `gives each status its own outcome` | *(modified)* the status table's `{ 403, permanent }` row becomes `{ 429, throttled }`; the `400`, `409`, `401` and `500` rows are untouched, and a `403` from the server now falls to `unavailable` through the `default` |

`name-text.test.ts`

| Test | Proves |
| --- | --- |
| `gives each refusal a sentence of its own` | *(modified)* five distinct sentences, `throttled` among them, each equal to its literal — the card's for `throttled` |
| `never says a name is taken, and never names a holder` | *(modified)* the `throttled` sentence joins the four already swept for *taken*, *someone*, *somebody*, *another player* and *already has it* |
| `leaves a way back from a refused name and a conflicting one, and from nothing else` | *(modified)* `mayTryAgain("throttled")` is `false`, and `rejected`/`conflict` are still the only two `true`s |

`NameSurface.test.tsx`

| Test | Proves |
| --- | --- |
| `gives every refusal its own sentence, from one render each` | *(modified)* a `throttled` outcome renders the card's sentence, retyped as a literal |
| `keeps the form for the two refusals a player can act on, and takes it away for the rest` | *(modified)* `throttled` sits in the `goesAway` list beside `no-profile` and `unavailable` |

## What would still pass if the coder got it wrong

- **If `case 403:` were left beside `case 429:`**, every test passes — `set-name.test.ts`'s table
  drives statuses one at a time and never asserts that `403` is *unmodelled*. The `verify` grep for
  `403` in `set-name.ts` is the only gate, and it is a grep because the correct behaviour for a
  `403` is now indistinguishable from the `default`'s.
- **If the `throttled` sentence were invented rather than transcribed**, `name-text.test.ts` passes,
  because the test's literal would be invented to match. The `verify` command that reads the marker
  out of `design/screens/account.html` and requires it in **both** the source and the test is what
  forbids that, and it is the same shape `TASK-140811` used for the anonymous block.
- **If `mayTryAgain("throttled")` returned `true`**, the form would survive a `429` — arguably
  friendlier, and wrong: `ADR-0134` §5 says a `429` invalidates nothing the player typed, but
  `mayTryAgain` governs whether the field is offered *again*, and a player who resends immediately
  meets the same refusal. The `false` assertion and the `goesAway` list both catch it; neither may
  be weakened to make a nicer screen.
- **A sentence that mentioned the name** — *"That name could not be set just now"* — passes the
  distinctness test and the *taken* sweep. It is the card's job to refuse it and review's to check
  it; no gate can.

## Acceptance criteria

- [ ] `set-name.test.ts` reports exactly 5 tests, all passing
- [ ] `name-text.test.ts` reports exactly 7 tests, all passing
- [ ] `NameSurface.test.tsx` reports exactly 10 tests, all passing
- [ ] `permanent` appears nowhere in `set-name.ts`, `name-text.ts`, `set-name.test.ts` or
      `name-text.test.ts`, and the quoted literal `"permanent"` appears nowhere in
      `NameSurface.test.tsx`. Two lowercase uses survive this ticket on purpose and are
      `TASK-140912`'s: `NameSurface.tsx:55`'s stale comment and `NameSurface.test.tsx:70`'s test
      title, both of which are about `PERMANENCE_LINE` rather than about an outcome kind
- [ ] `403` appears nowhere in `set-name.ts`, and `case 429` does
- [ ] The `NAME-FORM: throttled:` sentence from `design/screens/account.html` appears verbatim in
      both `name-text.ts` and `name-text.test.ts`
- [ ] `npm run check` and `npm run build` exit 0 in `web-client`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
