---
schema: 2
id: TASK-121301
title: The runout arrives street by street on the screen, not only in the log
type: task
status: ready
parent: STORY-1213
module: web-client
estimate: S
tier: sonnet
review: deep
files_touched: 10
labels: [qa, audit, bug, R1, manual-verify]
depends_on: []
atomic:
  - web-client/src/store/duel-state.test.ts — `npm run test`, measured 2026-09-01 — "starts with nothing the server has not sent" and "exports only the reducer and the initial state" are red on the first commit that gives `duel-state.ts` a field or an export, whatever order the rest lands in
  - web-client/src/e2e/whole-duel.test.tsx, duel-secrecy.test.tsx, claimed-here-recovered-there.test.tsx, drive-duel.test.tsx — `npm run test`, measured 2026-09-01 — all four go red on any commit where the store's default step is 600 and `drive-duel.tsx` does not boot at `0`: the run reported `Test Files 5 failed | 112 passed (117)`, those four plus `duel-state.test.ts`, against `1 failed` with the driver at `0`
  - ADR-0100 §3 — those four files may not be edited and no frame may be re-recorded, so the only repair for the gate above is `drive-duel.tsx`, in the same commit
  - ADR-0102 §8 — the store must publish the steps and the table must paint them together; every other cut leaves a store field with no consumer, "which is the precise shape of the defect being repaired"
  - tasks/BOARD.md — the merged DEC-105 answer note — "it stays one atomic `module: web-client` ticket, re-cut whole by the planner"
verify:
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npm run --silent test -- --reporter=verbose src/store/duel-state.test.ts 2>&1 | grep -qF "lays out one step per StreetDealt and a final step for the whole snapshot"
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npm run --silent test -- --reporter=verbose src/store/duel-state.test.ts 2>&1 | grep -qF "a snapshot at COMPLETE with no events before it takes one step, not four"
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npm run --silent test -- --reporter=verbose src/store/duel-state.test.ts 2>&1 | grep -qF "a snapshot that does not end a hand lays out no steps at all"
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npm run --silent test -- --reporter=verbose src/store/duel-store.test.ts 2>&1 | grep -qF "a step of zero releases in the same turn and schedules nothing"
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npm run --silent test -- --reporter=verbose src/store/duel-store.test.ts 2>&1 | grep -qF "ordinary play never calls the injected schedule"
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npm run --silent test -- --reporter=verbose src/store/duel-store.test.ts 2>&1 | grep -qF "frames that arrive during the steps are applied in arrival order once the last step has stood"
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npm run --silent test -- --reporter=verbose src/table/DuelTable.test.tsx 2>&1 | grep -qF "paints the snapshot's own cards when the StreetDealt carried different ones"
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npm run --silent test -- --reporter=verbose src/table/DuelTable.test.tsx 2>&1 | grep -qF "a runout paints three cards then four then five, naming Flop, Turn and River"
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npm run --silent test -- --reporter=verbose src/table/DuelTable.test.tsx 2>&1 | grep -qF "holds the award line back until the last step"
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npm run --silent test -- src/store/duel-state.test.ts 2>&1 | grep -qE "Tests +64 passed \(64\)"
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npm run --silent test -- src/store/duel-store.test.ts 2>&1 | grep -qE "Tests +9 passed \(9\)"
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npm run --silent test -- src/table/DuelTable.test.tsx 2>&1 | grep -qE "Tests +22 passed \(22\)"
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npm run --silent test -- src/table/PotStrip.test.tsx 2>&1 | grep -qE "Tests +5 passed \(5\)"
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npm run --silent test -- src/table/no-derivation.test.tsx 2>&1 | grep -qE "Tests +6 passed \(6\)"
  - sh -c 'shasum -a 256 web-client/src/table/no-derivation.test.tsx | grep -q "^cd7fdc758fcaac94bff05af154be66de6c0b3a3b873330d4a41950357be85ed8 "'
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npm run --silent test -- src/e2e/whole-duel.test.tsx src/e2e/duel-secrecy.test.tsx src/e2e/claimed-here-recovered-there.test.tsx src/e2e/drive-duel.test.tsx 2>&1 | grep -qE "Tests +24 passed \(24\)"
  - sh -c 'grep -qx "export const REVEAL_STEP_MS = 600;" web-client/src/store/boot.ts'
  - sh -c '! grep -q -- 600 web-client/src/store/duel-store.ts web-client/src/table/DuelTable.tsx web-client/src/lobby/Lobby.tsx'
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npm run --silent check
  - cd web-client && FORCE_COLOR=0 npm run --silent build
  - python3 .github/scripts/lint_tickets.py
---

## Goal

When a hand ends in an all-in and the engine runs the board out, a player can tell that the flop,
the turn and the river happened — `R1` (`ADR-0096` §2) is met at beat 5, as it already is at the
other seven, and `ADR-0095` §4's award banner gets a moment to be read before the next hand lands.

## The decision this is cut against

`DEC-105` is **answered and merged**:
[`ADR-0102`](../../docs/adr/ADR-0102-a-hand-ends-in-steps-and-the-client-owns-the-clock.md) — *a hand
ends in steps, and the client owns the clock*. The registration half of this ticket is spent; nothing
below asks for a `DEC`, and no new one is owed. `poker-server` is not changed, `poker-engine` is not
opened, the wire does not move and `PROTOCOL_VERSION` stays where `develop` has it (§7).

The whole mechanism, in five sentences the tests below pin one at a time:

1. **A `Snapshot` whose `view.street` is `COMPLETE` is painted as a sequence of steps** — one for
   each `StreetDealt` in the `Events` frame that **immediately preceded it**, in the order the server
   sent them, then a final step carrying the whole snapshot, *Hand complete*, and `ADR-0095`'s award
   line (§§1–2).
2. **Every frame that arrives while steps remain is queued, in arrival order, and applied when the
   last step has stood** (§1). Nothing is dropped and nothing is reordered — FIFO, `OpponentPresence`
   included (§6).
3. **Exactly two fields lag**: the board is `view.board.cards.slice(0, n_k)`, where `n_k` is the
   snapshot's own board length minus the cards carried by the steps *after* `k`; and the street label
   is that `StreetDealt`'s own `street`. Everything else on the screen is the snapshot's, unlagged
   (§2).
4. **A `Snapshot` that does not end a hand is applied at once**, with no step, no timer and no queue
   — which is every frame in ordinary play, and why `R1` cannot regress at the other seven beats
   (§1).
5. **A step is `REVEAL_STEP_MS = 600`, named once in `web-client/src/store/boot.ts` and reaching the
   store as a parameter, with `0` meaning synchronous** — released in the same turn, scheduling
   nothing, byte-for-byte today's behaviour (§4).

Two consequences of that shape need no code and must not be built:

- **A resuming or reconnecting client jumps to the end structurally** (§5). `DuelResume.kt`'s
  `resumeFrames` passes `newEvents = emptyList()` and `broadcast` only emits an `Events` frame for
  non-empty visible events, so a resuming seat is sent no `StreetDealt` and there are no street steps
  to take. **No flag, no field and no special case** — a resume `Snapshot` at `COMPLETE` takes §2's
  single final step, and that uniformity is deliberate.
- **`drive-duel.tsx` boots at `0`** (§4), which is what keeps `ADR-0100` §3's evidence — *no frame is
  re-recorded and none of the four e2e files is edited* — intact.

## The defect

Round 1 of `/qa-cycle audit smoke` answered `R1` **`not met` at beat 5** — an all-in call — and `met`
at the other seven. With `record` armed on both tabs, the acting player's next two frames were:

- frame *N* — `THEIR TURN | 10,000 | committed 100 | Pot 0 | Blinds 50/100 · Hand 3 · Preflop`, **no
  community cards**;
- frame *N+1* — `Your rival wins 19,800 | Blinds 50/100 · Hand 3 · Hand complete`, **five community
  cards**.

Nothing came between them, on either browser. `ADR-0102`'s Context confirms this ticket's original
source reading on three points and adds the fourth that decides the answer: **the next hand is dealt
in the same call and its frames are delivered in the same batch** — `DuelAction.kt`'s `act` ends with
`if (!result.newState.isHandOver) return …; val advanced = advance(played, seeds); return
DuelStep(advanced.runner, outbound + advanced.outbound)`, and `DuelSocket.kt` hands the whole list to
one `deliver`. `DuelActionTest.afoldEndsTheHandAndOpensTheNext` asserts snapshots for hand 1 and hand
2 in one `outbound`. So slicing the board alone is overwritten a millisecond into the first step, and
**whatever paces a runout must also govern when the frames behind it are applied**.

## Files

The set was **measured, not remembered** (`ADR-0069`, `ADR-0070`). The mechanism above was stubbed in
one tree and `.github/workflows/build.yml`'s pull-request gate set was run one command at a time, so
no failing prefix could hide a later gate: in `web-client/`, `npm ci`, `npm run typecheck`, `npm run
lint`, `npm run format:check`, `npm run test` and `npm run build`, each with the probe live and again
after each round of propagation, until the set exited 0. `./gradlew check -PrequireDocker=true` was
run on the same tree with the probe reverted — Docker up (Engine 29.5.2), 40 tasks executed, no suite
skipped, `verifyProtocolTypes` and `verifyDuelScript` both run, exit 0. It could not have been
reddened by the probe either way: the only `web-client` paths any Gradle task reads are
`src/protocol/protocol.gen.ts` and `src/e2e/scripted-duel.gen.json` (`poker-server/build.gradle.kts`),
and the probe touched neither. `./design/check-drift.sh` and `python3 .github/scripts/lint_tickets.py`
were run too, green. The probe was then reverted and `git status` came back empty.
**`files_touched: 2` on the previous cut was a pre-decision guess and is not evidence.**

| File | Action | Why it cannot be fewer |
| --- | --- | --- |
| `web-client/src/store/duel-state.ts` | modify | the change itself — the step state, the queue, and the pure per-tick advance the store calls |
| `web-client/src/store/duel-state.test.ts` | modify | `npm run test` — **2 failures**, both structural: *starts with nothing the server has not sent* deep-equals the whole initial state, and *exports only the reducer and the initial state* pins the module's export list. The first commit that gives `duel-state.ts` a field or an export reddens both |
| `web-client/src/store/duel-store.ts` | modify | `npm run typecheck` — `createDuelStore` is where the injected schedule and the step cost arrive; a queue with no clock cannot drain |
| `web-client/src/store/duel-store.test.ts` | modify | the ticket's own proofs 4 and 6 (§8) live here — *the injected schedule not called* is only assertable where the schedule is injected |
| `web-client/src/store/boot.ts` | modify | `ADR-0102` §4 — the constant is named **once**, here, and reaches the store as a parameter |
| `web-client/src/table/DuelTable.tsx` | modify | the board prefix — `<BoardCards cards={view.board.cards} />` is the one place the whole board reaches the screen |
| `web-client/src/table/DuelTable.test.tsx` | modify | proofs 1 and 2 (§8): the two-input test that tells a copy from a constant, and the three intermediate boards |
| `web-client/src/table/PotStrip.tsx` | modify | the street label and the award line. It reads `view.street` directly, and the step's street must arrive as its **own prop** — a doctored view would be the client assembling a view the server never sent, which `ADR-0102`'s *Consequences* rules out by name |
| `web-client/src/lobby/Lobby.tsx` | modify | `npm run typecheck` — the one production render site of `DuelTable`; the step reaches the table through no other producer |
| `web-client/src/e2e/drive-duel.tsx` | modify | `npm run test` — **all four recorded-frame suites go red** the moment the default step is 600 and this driver does not boot at `0`: `Test Files 5 failed \| 112 passed (117)`, those four plus `duel-state.test.ts`, against `1 failed` with the driver at `0`. `ADR-0100` §3 forbids editing those four, so this is the only repair |
| `web-client/src/table/no-derivation.test.tsx` | read | `ADR-0102` §3 — **byte-unchanged and green**. Read to see what the `HAND_TALK` and derived-number matchers forbid; a step that reddens it was built on a computed number, and the fix is the step, never the matcher |
| `docs/adr/ADR-0102-a-hand-ends-in-steps-and-the-client-owns-the-clock.md` | read | §§1–5 the mechanism, §6 why this asserts no game fact, §8 the six proofs |

**Two files the gate set did *not* name, recorded so nobody adds them.** `web-client/src/e2e/drive-arc.tsx`
also calls `bootDuelClient` directly and needs **no** parameter: it was left at the 600 default
through both probe runs and `claimed-here-recovered-there.test.tsx` and `drive-arc.test.tsx` stayed
green, because it never replays past a hand-completing `Snapshot`. And `web-client/src/table/PotStrip.test.tsx`
keeps all **5** of its tests green unchanged — including *names the street the view names even when
the board disagrees*, which still holds because the street prop is absent in every one of them. Its
count is gated below so a coder notices if that stops being true.

## Scope

- The store lays out `ADR-0102` §2's steps for a hand-completing `Snapshot`, queues everything behind
  them in arrival order, and drains the queue when the last step has stood.
- The table paints the lagged board and the lagged street label, and no other lagged field.
- `boot.ts` names `REVEAL_STEP_MS = 600` once and passes it down; `0` releases in the same turn and
  schedules nothing; `drive-duel.tsx` boots at `0`.
- `R1` is met at **every** beat afterwards, not only at beat 5 — ordinary play must schedule nothing,
  and the injected schedule is asserted **not called** for it.

## Out of scope

- **Reconstructing a board from `StreetDealt` payloads.** Every card face painted is read from
  `view.board.cards`; the events are read only for *how many* cards a step adds and *which street to
  name* — both fields the server set (`ADR-0102` §3). A client may not assert a game fact
  (`CLAUDE.md`, `ADR-0002`).
- **Editing `no-derivation.test.tsx`, by one byte.** Its checksum is a `verify:` line.
- **Editing any of the four recorded-frame e2e suites, or re-recording `scripted-duel.gen.json`.**
  `ADR-0100` §3. `drive-duel.tsx` — the driver, not a suite — is the whole of the permitted change.
- **Holding the seat plates back, or any other composite view.** `ADR-0102`'s *Consequences* names
  the spoiler — a runout's stacks settle at the first step — and refuses the fix, because painting
  some fields from the new view and others from the previous one is badly wrong for a hand that opens
  all-in. Whoever wants it files it.
- **Naming a made hand.** `ADR-0095` §3 closed it permanently.
- **`Pot 0` during the reveal.** `ADR-0102`'s *Consequences* fifth item: it is `DEC-104`, the product
  owner's, open, and answering it here would be guessing a product decision in an architecture
  costume.
- **A `data-testid`, a prop or a flag added to any component for the sake of a test.** The step is a
  production seam at boot (`ADR-0102` §4), not a test-only door in `ADR-0100` §5's sense.
- **`R2`'s overflow and `R4`'s spacing.** `TASK-121302` and `TASK-121303`.

## Tests

Nine tests across three files, one per proof in `ADR-0102` §8 plus the two that pin §2's step layout.
The titles below are the strings the `verify:` block greps for, so they must match **exactly**.

`web-client/src/store/duel-state.test.ts` — 61 today, **64** after

| Test | Proves |
| --- | --- |
| `lays out one step per StreetDealt and a final step for the whole snapshot` | §2's arithmetic: an `Events` frame of `StreetDealt(FLOP, 3)`, `StreetDealt(TURN, 1)`, `StreetDealt(RIVER, 1)` before a five-card `COMPLETE` snapshot lays out **four** steps whose board lengths are 3, 4, 5, 5 and whose streets are `FLOP`, `TURN`, `RIVER` and the snapshot's own |
| `a snapshot at COMPLETE with no events before it takes one step, not four` | §5 — the resume, structurally. The same snapshot with **no** preceding `Events` frame lays out exactly one step. Proof 5 |
| `a snapshot that does not end a hand lays out no steps at all` | §1 — a mid-hand snapshot with one `StreetDealt` before it lays out none, so ordinary play is never paced |

`web-client/src/store/duel-store.test.ts` — 6 today, **9** after

| Test | Proves |
| --- | --- |
| `a step of zero releases in the same turn and schedules nothing` | Proof 6 — at a step of `0` the hand-completing snapshot is fully applied when `apply` returns, and the injected schedule is asserted **not called**. This is the property `drive-duel.tsx` leans on |
| `ordinary play never calls the injected schedule` | Proof 4 — a mid-hand `Snapshot` preceded by one `StreetDealt`, at a **non-zero** step, applies synchronously with the schedule asserted **not called**. The no-regression gate for `R1` at the other seven beats |
| `frames that arrive during the steps are applied in arrival order once the last step has stood` | Proof 3 — with the next hand's `Events`, `Snapshot` and `YourTurn` delivered during the steps: none of hand *N+1* is in the state before the last step, **all** of it is after, and in that order. Nothing dropped, nothing reordered |

`web-client/src/table/DuelTable.test.tsx` — 19 today, **22** after

| Test | Proves |
| --- | --- |
| `paints the snapshot's own cards when the StreetDealt carried different ones` | Proof 1 — **two inputs that disagree**, or the test cannot tell a copy from a constant: a `StreetDealt` whose `cards` are not the snapshot's board prefix, and the assertion is that the screen shows the **snapshot's** cards |
| `a runout paints three cards then four then five, naming Flop, Turn and River` | Proof 2 — three intermediate boards, each with its own street label, driven through the store rather than by handing the component a step by hand |
| `holds the award line back until the last step` | Proof 2's other half — the award line appears at **none** of the intermediate steps and appears at the last. Asserted on both sides, because "absent at step 1" alone passes for a table that never shows it |

**Why no new `PotStrip.test.tsx` case.** `PotStrip` is rendered inside `DuelTable`, and the three
tests above exercise its street label and its award line through the real tree rather than through a
hand-built prop — which is also the only way proof 1's disagreement can be staged honestly. Its
existing 5 tests are gated unchanged.

### Which `verify:` lines are red today, and which must stay green

Every one of the 21 commands was run against `develop` at `27d6ba76` before this ticket was filed, so
the block is known to be well formed and known to discriminate. **13 are red today and go green only
with the repair**: the nine name greps, the three raised counts (`duel-state.test.ts` 61 → 64,
`duel-store.test.ts` 6 → 9, `DuelTable.test.tsx` 19 → 22), and the `REVEAL_STEP_MS` declaration.
**Eight are green today and must still be green afterwards**: `PotStrip.test.tsx` at 5,
`no-derivation.test.tsx` at 6, its SHA-256, the four recorded-frame suites at 24, `600` appearing in
none of `duel-store.ts` / `DuelTable.tsx` / `Lobby.tsx`, `npm run check`, `npm run build` and the
ticket linter. A block where every line already passes gates nothing; a block where every line already
fails proves nothing about the ones that must not move. Both halves are here on purpose.

## Acceptance criteria

- [ ] `duel-state.test.ts` — `lays out one step per StreetDealt and a final step for the whole snapshot` passes
- [ ] `duel-state.test.ts` — `a snapshot at COMPLETE with no events before it takes one step, not four` passes
- [ ] `duel-state.test.ts` — `a snapshot that does not end a hand lays out no steps at all` passes
- [ ] `duel-store.test.ts` — `a step of zero releases in the same turn and schedules nothing` passes
- [ ] `duel-store.test.ts` — `ordinary play never calls the injected schedule` passes
- [ ] `duel-store.test.ts` — `frames that arrive during the steps are applied in arrival order once the last step has stood` passes
- [ ] `DuelTable.test.tsx` — `paints the snapshot's own cards when the StreetDealt carried different ones` passes
- [ ] `DuelTable.test.tsx` — `a runout paints three cards then four then five, naming Flop, Turn and River` passes
- [ ] `DuelTable.test.tsx` — `holds the award line back until the last step` passes
- [ ] `duel-state.test.ts` reports **64 passed (64)**, `duel-store.test.ts` **9 passed (9)**,
      `DuelTable.test.tsx` **22 passed (22)** — the counts are the measured bases 61, 6 and 19 plus
      the three tests each names above, so an extra test or a deleted one fails the gate
- [ ] `PotStrip.test.tsx` reports **5 passed (5)** and `no-derivation.test.tsx` **6 passed (6)** —
      neither gains, loses or weakens a case
- [ ] `no-derivation.test.tsx` is byte-identical to `develop`'s copy — its SHA-256 is still
      `cd7fdc758fcaac94bff05af154be66de6c0b3a3b873330d4a41950357be85ed8`
- [ ] The four recorded-frame suites report **24 passed (24)** together, and `git diff` shows none of
      the four files, and no line of `scripted-duel.gen.json`, changed (`ADR-0100` §3)
- [ ] `boot.ts` declares `export const REVEAL_STEP_MS = 600;` and the literal `600` appears in none
      of `duel-store.ts`, `DuelTable.tsx` or `Lobby.tsx` — `ADR-0102` §4's *named once*, checked from
      both ends
- [ ] **Manual reproduction, at 390 × 664, both browsers** (`manual-verify`): play a hand to an
      all-in call before the river. On both screens the board reaches five cards through at least one
      intermediate state showing three or four community cards, the street label passes through the
      streets that were dealt, and the award line appears only once the board is complete
- [ ] The same walk at the other seven beats shows no regression: `R1` still `met` at 1, 2, 3, 4, 6,
      7 and 8, and no beat gains a pause
- [ ] Every command in `verify:` exits 0

## Why the browser walk is a criterion and not a `verify:` line

`ADR-0089` §2b — *"No pull request, `verify:` block or ticket waits on a QA case"* — is one of the
three conditions that license the QA harness at all, and reaching for `scripts/qa/` in a `verify:`
line breaks it rather than bending it. `ADR-0102` §8 says the same in its own words: a browser fact
may never be a gate here, and the `manual-verify` label stays.

**Do not invent a `grep` that passes either way.** The `verify:` block above gates the mechanism —
named tests, per-file counts, a checksum and the four suites' 24 — and says nothing about what a
person sees. That is the honest division and it is deliberate.

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket: `verify` green,
review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into `develop`.
