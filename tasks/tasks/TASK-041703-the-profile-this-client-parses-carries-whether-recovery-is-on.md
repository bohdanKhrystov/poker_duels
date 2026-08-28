---
schema: 2
id: TASK-041703
title: The profile this client parses carries whether recovery is on
type: task
status: backlog
parent: STORY-0417
module: web-client
estimate: S
tier: haiku
review: standard
files_touched: 6
atomic:
  - tsc --noEmit (in web-client's npm run check) — `aProfile` in profile-fixture.ts returns an object literal that no longer satisfies `PlayerProfile`; measured `TS2322 … Type 'undefined' is not assignable to type 'boolean'`
  - tsc --noEmit — set-name-provider.test.tsx builds a `PlayerProfile` literal by hand; measured `TS2741 Property 'hasRecoveryEmail' is missing`
  - vitest run src/profile/profile-fixture.test.ts — its two key-set `toEqual`s over `aProfile()` and `meBody()` enumerate every field; measured 2 failed
  - vitest run (whole suite) — 20 further tests across profile.test.ts, profile-strip.test.ts, set-name.test.ts, profile-no-derivation.test.tsx, drive-arc.test.tsx and claimed-here-recovered-there.test.tsx fail while `meBody` in profile-fixture.ts omits the field the parser now requires; measured 22 failed in total on that commit
  - vitest run src/e2e/account-server.test.ts — the fake server's `/api/me` body is compared with `toEqual` in four tests, and the clients driven against it stop reading a profile at all; measured 4 failed after profile-fixture.ts was repaired
labels: [client, account, profile, recovery]
depends_on: [TASK-041701]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test -- src/profile/profile-fixture.test.ts 2>&1 | grep -qE 'Test Files +1 passed \(1\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- src/profile/profile-fixture.test.ts 2>&1 | grep -qE 'Tests +4 passed \(4\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- src/e2e/account-server.test.ts 2>&1 | grep -qE 'Tests +19 passed \(19\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- src/profile/profile.test.ts 2>&1 | grep -qE 'Tests +11 passed \(11\)'
  - grep -qF 'readonly hasRecoveryEmail: boolean;' web-client/src/profile/profile.ts
  - test "$(grep -oF 'hasRecoveryEmail' web-client/src/profile/profile-fixture.ts | wc -l | tr -d ' ')" = 2
  - test "$(grep -oF '"hasRecoveryEmail"' web-client/src/profile/profile-fixture.test.ts | wc -l | tr -d ' ')" = 2
  - test "$(grep -oF 'hasRecoveryEmail' web-client/src/e2e/account-server.ts | wc -l | tr -d ' ')" = 2
  - grep -qF 'hasRecoveryEmail' web-client/src/e2e/account-server.test.ts
  - grep -qF 'hasRecoveryEmail' web-client/src/profile/set-name-provider.test.tsx
  - test "$(grep -oF 'hasRecoveryEmail' web-client/src/profile/profile.test.ts | wc -l | tr -d ' ')" = 0
  - test "$(grep -oF 'hasRecoveryEmail' web-client/src/account/AccountScreen.tsx | wc -l | tr -d ' ')" = 0
  - cd web-client && npm run check
---

## Goal

`PlayerProfile` carries `hasRecoveryEmail`, `profileFromBody` requires it on the wire with no
default, and every fixture and fake server in this client supplies it — so a later ticket can say
*recovery is on* or *recovery is off* from a fact the server actually sent.

## Files

Six files, and every one of them is forced — see `atomic:` and `## Proof`, which is a probe that was
run rather than a file list that was remembered.

| File | Action | Why it cannot be fewer |
| --- | --- | --- |
| `web-client/src/profile/profile.ts` | modify | the field and its parse; this is the ticket |
| `web-client/src/profile/profile-fixture.ts` | modify | `tsc` — `aProfile` stops satisfying `PlayerProfile` the moment the field is required, and the whole suite reddens while `meBody` omits it |
| `web-client/src/profile/profile-fixture.test.ts` | modify | `vitest` — its two `Object.keys(...).sort()` `toEqual`s enumerate every field of both builders and fail on a new one |
| `web-client/src/profile/set-name-provider.test.tsx` | modify | `tsc` — it is the one test outside the fixtures that hand-builds a `PlayerProfile` literal |
| `web-client/src/e2e/account-server.ts` | modify | `vitest` — the fake `/api/me` it serves stops parsing, which takes down `drive-arc` and `claimed-here-recovered-there` |
| `web-client/src/e2e/account-server.test.ts` | modify | `vitest` — four of its tests compare that body with `toEqual`, so the body cannot gain a field without them |

Read, and do not edit:

- `docs/protocol.md` *Profile endpoint* — the `hasRecoveryEmail` row, and the sentence saying `false`
  covers three cases this field does not distinguish.
- [`ADR-0031`](../../docs/adr/ADR-0031-an-optional-verified-recovery-email.md) §6.3 — the address
  itself is returned by no endpoint, so there is nothing else on this wire to parse.
- `web-client/src/profile/profile-strip.ts` — how `PlayerProfile` reaches a screen. **Not edited**:
  it passes the whole profile through and needs no change, which was measured.

## Scope

- **`PlayerProfile` gains one field**, last, beside `deviceRouteLive`:

  ```ts
  /** `true` only when this player holds a **verified** recovery address (`ADR-0031` §3). */
  readonly hasRecoveryEmail: boolean;
  ```

  The KDoc says what `false` does **not** mean: it covers never attached, attached but unverified,
  and detached, and the client may not tell them apart because the server does not.
- **`profileFromBody` requires it, with no default**, in the same `typeof … === "boolean"` chain
  `deviceRouteLive` sits in, and copies it into the returned object. A body without it is `null`,
  which `readProfile` turns into `unavailable` — the `TASK-041208` rule, applied to the second
  boolean on this response.
- **`aProfile` gains `hasRecoveryEmail: false`** and **`meBody` gains `hasRecoveryEmail: false`**.
  `false` in both, so no existing test silently starts asserting a recovery state it never asked for.
- **`profile-fixture.test.ts`'s two key lists gain `"hasRecoveryEmail"`**, in sorted position — this
  ticket owns those two assertions because it is what invalidates them.
- **`account-server.ts` serves the field on both `/api/me` bodies it builds**, `false` in both, and
  `account-server.test.ts`'s body comparisons gain it. Nothing about recovery is implemented in that
  fake server: it answers a constant, exactly as it answers a constant `deviceRouteLive`.

## Out of scope

- **Any test asserting that a body *missing* the field is not a profile.** That is `TASK-041704`, and
  it is a separate ticket for a reason `ADR-0070` §4 states: its propagation exception excludes
  *adds a test*, and the probe below reached green **without** `profile.test.ts`, which is the proof
  no gate names it. A `verify:` line pins `hasRecoveryEmail` at zero occurrences in that file.
- **Any sentence on any screen.** `TASK-041712`. A `verify:` line pins `hasRecoveryEmail` at zero
  occurrences in `AccountScreen.tsx`, which is where `TASK-041217`'s own zero-grep criterion left it.
- **`ProfileStripState`, `ProfileStrip`, `NameSurface` or anything that renders a profile.** They
  carry the profile whole and compile untouched — measured.
- **Making the field optional, or defaulting it to `false` when absent.** `profile.ts` documents *no
  defaults* and `TASK-041208` gated that rule for `deviceRouteLive`; a default here would make a
  server that forgot the field indistinguishable from a player with no address.

## Tests

**This ticket adds no test, and its `Tests` count does not move.** Its gates are `tsc`, the two
key-set assertions it edits, and the 836-test suite, all named in `atomic:` and all measured below.

Two merged assertions move, and this ticket owns them because its change is what invalidates them:

`web-client/src/profile/profile-fixture.test.ts`

| Test | What moves |
| --- | --- |
| `builds a profile carrying every field PlayerProfile declares` | its `Object.keys(aProfile()).sort()` list gains `"hasRecoveryEmail"` between `"displayNameRemoved"` and `"playerId"`. Nothing else in the assertion changes and nothing is weakened |
| `builds bodies carrying every field the wire declares, opponent id included` | its `Object.keys(meBody()).sort()` list gains the same entry in the same position. The `duelRowBody` half of that test is untouched |

Four merged assertions in `web-client/src/e2e/account-server.test.ts` gain one line each — the
`toEqual` body comparisons in `answers each device id with its own player`, `a bearer token outranks
the device id on the profile read`, `the device id still answers when no token is carried` and
`signing out returns the browser to the device it holds`. No assertion is deleted, none is weakened,
and no test name changes.

## Acceptance criteria

- [ ] `cd web-client && NO_COLOR=1 npm run --silent test -- src/profile/profile-fixture.test.ts 2>&1 | grep -qE 'Tests +4 passed \(4\)'`
      and `… | grep -qE 'Test Files +1 passed \(1\)'` — **exactly four**, unmoved: this ticket edits
      two assertions in that file and adds no test. Both lines, because a collection error prints a
      *passing* `Tests` count with no failure line at all
- [ ] `cd web-client && NO_COLOR=1 npm run --silent test -- src/e2e/account-server.test.ts 2>&1 | grep -qE 'Tests +19 passed \(19\)'`
      and `cd web-client && NO_COLOR=1 npm run --silent test -- src/profile/profile.test.ts 2>&1 | grep -qE 'Tests +11 passed \(11\)'`
      — the two other suites this diff can reach, both at their merged counts. **Per-file counts and
      not a whole-suite figure**, because this ticket and `TASK-041702` have disjoint `Files` tables
      and may be dispatched in one batch, which moves any absolute total
- [ ] `grep -qF 'readonly hasRecoveryEmail: boolean;' web-client/src/profile/profile.ts`
      — declared, required, and not optional. The `?` form would not match this string
- [ ] `test "$(grep -oF 'hasRecoveryEmail' web-client/src/profile/profile-fixture.ts | wc -l | tr -d ' ')" = 2`
      — **both** builders carry it. One would leave every wire-body test asserting a body the parser
      rejects
- [ ] `test "$(grep -oF '"hasRecoveryEmail"' web-client/src/profile/profile-fixture.test.ts | wc -l | tr -d ' ')" = 2`
      — **both** key lists. The quotes are part of the needle, so a bare mention in a comment does
      not satisfy it
- [ ] `test "$(grep -oF 'hasRecoveryEmail' web-client/src/e2e/account-server.ts | wc -l | tr -d ' ')" = 2`
      — the fake server builds two `/api/me` bodies and both carry it
- [ ] `grep -qF 'hasRecoveryEmail' web-client/src/e2e/account-server.test.ts` and
      `grep -qF 'hasRecoveryEmail' web-client/src/profile/set-name-provider.test.tsx` — the two files
      `tsc` and the four `toEqual`s force
- [ ] `test "$(grep -oF 'hasRecoveryEmail' web-client/src/profile/profile.test.ts | wc -l | tr -d ' ')" = 0`
      — this ticket does **not** reach into `TASK-041704`'s file. Reads the whole file, comments
      included, so the word may not appear in prose there either
- [ ] `test "$(grep -oF 'hasRecoveryEmail' web-client/src/account/AccountScreen.tsx | wc -l | tr -d ' ')" = 0`
      — no screen states anything yet
- [ ] `cd web-client && npm run check` exits 0, and `cd web-client && npm run build` exits 0
- [ ] Every merged test passes; the only assertions that move are the six named in `## Tests`, each
      gaining one entry and none weakened
- [ ] No file outside the six listed differs
- [ ] Every command in `verify:` exits 0

## Proof

**The six files were measured, not remembered.** The probe below was run in a worktree on `develop`
at `278c56fb` and reverted; `git status` was the file list. Run it again yourself and record what you
get — do not copy these numbers if your run differs.

1. **Stub the declaration**: add `hasRecoveryEmail: boolean` to `PlayerProfile`, the `typeof` clause
   to `profileFromBody`, and the copy into its returned object. Nothing else.
2. **Run `npx tsc --noEmit`.** Measured: **two** paths — `src/profile/profile-fixture.ts` (`TS2322`,
   `Type 'undefined' is not assignable to type 'boolean'`) and `src/profile/set-name-provider.test.tsx`
   (`TS2741`). Apply the minimal repair to each — one field, no behaviour — and run again: clean.
3. **Run `npx vitest run`.** Measured: **22 failed**, across **seven** files, all of them invisible to
   step 2 because `tsc` had nothing to say about them:
   `profile-no-derivation.test.tsx` (4), `profile.test.ts` (5), `profile-strip.test.ts` (1),
   `set-name.test.ts` (1), `profile-fixture.test.ts` (1), `drive-arc.test.tsx` (2) and
   `claimed-here-recovered-there.test.tsx` (8).
4. **Add the field to `meBody`** — one line in a file already in the list — and run again. Measured:
   **6 failed** across **two** files, `e2e/account-server.test.ts` (4) and
   `profile/profile-fixture.test.ts` (2). **Twenty of the twenty-two failures were a fixture, not a
   file.** This is why the list is six and not nine: a red run names a *prefix*, and repairing the
   builder cleared six files that a remembered list would have named as rows.
5. **Repair the remaining four assertions** in `account-server.ts`, `account-server.test.ts` and the
   two key lists. Run the whole gate set the `client` job in `.github/workflows/build.yml` runs —
   `npm ci`, `npm run check` (typecheck, lint, `prettier --check`, `vitest run`) and `npm run build`.
   Measured: **836 passed (836)** over **107** files, green, plus a clean production build.
   `prettier --check` named one file on the way, which was the probe's own indentation and not a new
   row.
6. **`profile.test.ts` is not in the list, and step 5 is the proof.** The gate set exits 0 without it,
   so no gate names it; `atomic:` may only name a gate that **fails** on the smaller commit
   (`ADR-0068`), and there is none for that file. Its tests are `TASK-041704`'s.
7. **The Kotlin job is unaffected.** No file in this diff is under a Gradle source set, so
   `./gradlew check -PrequireDocker=true` cannot change colour on account of it. Say so if you find
   otherwise.

Then, with the change applied, two mutations — an experiment, not a change, and both inside the
budget:

8. **Drop the `typeof … hasRecoveryEmail === "boolean"` clause** from `profileFromBody`, keeping the
   copy. Predict: the suite stays **green**, because every fixture supplies the field. Record it.
   That green run is the finding, and it is exactly why `TASK-041704` exists.
9. **Copy `deviceRouteLive` into `hasRecoveryEmail`** in the returned object. Predict: the suite stays
   **green** — every fixture sets `deviceRouteLive: true` and `hasRecoveryEmail: false`, so this
   should redden something; if it does not, say which fixture makes the two indistinguishable, because
   that is a second thing `TASK-041704` has to close.

> **A red run names a prefix, not a set.** Vitest stops reporting past its first hard failure: a
> syntax error in a client source file was measured on this repo failing **twelve** test files at
> collection and printing `667 passed` with **no failure count at all**. Step 4 is the same lesson in
> its milder form.
>
> **Expect `The 'NO_COLOR' env is ignored due to the 'FORCE_COLOR' env being set.` on stderr.** It is
> wrong — measured, 0 escape bytes with it and 74 without — so every `grep -qE` needs it.

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket.

## Notes

**Why `atomic: 6` rather than a split.** Each `atomic:` line is an exit code, not an opinion: `tsc`
refuses two of the files and `vitest` refuses three more, on the *same* commit that declares the
field required. What would be a split — declaring the field optional first and tightening later — is
a commit that ships a parser accepting a body the protocol says is complete, which is the defect
`TASK-041208` was written to prevent for the field next to it.

**This ticket deliberately reverses one merged criterion, and it is recorded rather than absorbed.**
`TASK-041207`'s acceptance criteria include `grep -c 'hasRecoveryEmail' web-client/src/profile/profile.ts`
returning `0`, with its own *Out of scope* saying *"it is on the same response and belongs to
`STORY-0417`"*. That criterion was a fence around that ticket's scope, not a standing rule, and this
is the ticket it was pointing at. It is a criterion in a merged ticket, not a live gate: nothing in
CI runs it.

`grep -c` counts matching **lines** and exits **1** on zero matches, so every zero-expectation above
is wrapped as `test "$(… | wc -l | tr -d ' ')" = 0`. `-F` keeps `.` and `(` literal.
