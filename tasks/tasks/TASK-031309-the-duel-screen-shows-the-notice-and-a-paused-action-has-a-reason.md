---
schema: 2
id: TASK-031309
title: The duel screen shows the notice, and a paused action has a reason
type: task
status: done
parent: STORY-0313
module: web-client
estimate: S
tier: haiku
review: standard
files_touched: 2
labels: [client, duel, ui, presence]
depends_on: [TASK-031308]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test 2>&1 | grep -qE 'Tests +[0-9]+ passed \([0-9]+\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'shows the presence beside the table it is about'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'explains a paused action with the presence it already holds'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'starts a second window fresh, though it carries the same remaining'
  - cd web-client && npm run check
---

## Goal

The duel screen renders the presence the store holds: the word on the rival's plate, the line and
the countdown beside the table, and a `DUEL_PAUSED` refusal that now has a reason on screen beside
it.

## Files

| File | Action |
| --- | --- |
| `web-client/src/lobby/Lobby.tsx` | modify — one import, two attributes, one element |
| `web-client/src/lobby/Lobby.test.tsx` | modify — three tests added, one array extended |
| `web-client/src/store/duel-state.ts` | read — the four fields the reducer now holds |

## Scope

- The duel branch of `Lobby` — the one guarded by `state.view !== null` — becomes:

  ```tsx
        <DuelTable view={state.view} rivalPresence={state.rivalPresence} />
        <PresenceNotice
          key={state.presenceCount}
          presence={state.rivalPresence}
          returned={state.rivalReturned}
          graceRemainingMillis={state.graceRemainingMillis}
        />
        <ActionBar … />
  ```

- **The `key` is the whole mechanism for a second window.** `presenceCount` changes on every
  presence frame and on nothing else, so a new frame mounts a fresh notice — the amount control's
  own idiom in `ActionBar`, where `rejectionCount` does exactly this job. Two grace windows in one
  duel carry the same `graceRemainingMillis`, so keying on the value would leave the first
  countdown running under the second window.
- The notice goes between the table and the bar. **Nothing pins that order** and no test asserts it:
  placement is `EPIC-06`'s (`ADR-0046` §6), and what this ticket fixes is only that the notice is on
  the duel screen at all.
- `ActionBar` is untouched. `YourTurn` is not withdrawn while the duel is paused — no frame exists
  to withdraw it (`ADR-0028` §6) — so the bar's own state is not a function of presence, and the
  `DUEL_PAUSED` sentence it already renders is unchanged.

## This ticket owns the enumeration its change unsettles

`Lobby.test.tsx`'s `keeps waiting through every frame that neither seats a table nor ends the duel`
carries a comment claiming its array is *"Every variant of `ServerMessage` except `Snapshot`, not a
representative two"*, and gives the reason: a leak on `DuelFinished` shipped green because the test
happened to name `Events` instead. The array is three variants short of that claim —
`RematchOffered` shipped after it, and `OpponentPresence` and `ActedForAbsent` arrive with
`STORY-0214`. This ticket adds all three:

```ts
      { type: "RematchOffered", seat: 1 },
      { type: "OpponentPresence", presence: "AWAY", graceRemainingMillis: 47000 },
      {
        type: "ActedForAbsent",
        seat: 1,
        handNumber: 3,
        actionSequence: 7,
        action: "CHECK",
      },
```

Three lines of data and no assertion changes: the test keeps its name, its loop and its
`getByText("Waiting for your rival")` after every frame. Two of the three are this story's; the
third is one line, and leaving a universal claim knowingly false in a file this ticket is already
editing costs more than adding it.

## Out of scope

- What a resume renders. `TASK-031310`.
- What the countdown reaching zero does. `TASK-031311`.
- Any word not already shipped. This ticket adds no string at all — every sentence it puts on screen
  came from `presence-text.ts` or from `ActionBar`'s existing `refusalText`.
- `ActedForAbsent`. It still falls through the reducer's `default`; `TASK-031314` folds it.

## Tests

`web-client/src/lobby/Lobby.test.tsx`, describe block `"the lobby"`. Three added, one array
extended.

Every test seats this client at **seat 1** (`RoomJoined` with `seat: 1`), so a screen that read the
rival off a literal `0`, or off `mySeat` with the comparison inverted, disagrees with the fixture.
The grace window is **47 000 ms**, whose whole-second reading `47` collides with no number the
file's `SNAPSHOT` carries (stacks `500`, pot `30`, `betToMatch` `20`, `minRaiseTo` `40`, blinds `10`
and `20`), so `getByText("47")` can only be the countdown.

`the lobby`

| Test | Proves |
| --- | --- |
| `shows the presence beside the table it is about` | after `RoomJoined(seat 1)`, `SNAPSHOT` and `OpponentPresence(AWAY, 47000)`: `Your rival is away. The duel is paused.` is on screen, `47` is on screen, and the plate named `Your rival` reads `Away` — the line, the number and the word, in one render |
| `explains a paused action with the presence it already holds` | after the same three frames plus `Failure(DUEL_PAUSED)`: **both** `The duel is paused. That action was not applied.` and `Your rival is away. The duel is paused.` are on screen at once. The bar's sentence alone is the mystery this story exists to end |
| `starts a second window fresh, though it carries the same remaining` | `OpponentPresence(AWAY, 47000)`, advance virtual time `20_000` (the number reads `27`), then a **byte-identical** `OpponentPresence(AWAY, 47000)` — the number reads `47` again. Fake timers, installed before `render` and released in `afterEach`; the two frames differ in nothing, so only the remount key can tell them apart |

Three tests. Six hundred and nine exist after `TASK-031308`, so the suite reports **612**.

Fake timers are installed **inside** the third test only, and `afterEach(() => vi.useRealTimers())`
guards the rest of the file — several tests here `await` a real promise and must keep their real
clock.

## Proof

| Command | Proves |
| --- | --- |
| a green `Tests N passed (N)` line | three ran, the extended one still runs, and every test before them still does |
| the three `--reporter=verbose` greps | each exists by name |
| `npm run check` | the two new attributes and the new element typecheck against the props `TASK-031306` and `TASK-031308` shipped |

**Name the edit that makes each assertion red:**

1. Drop the `key` from `<PresenceNotice>` → `starts a second window fresh, though it carries the
   same remaining` fails, `27` on screen against the `47` it looks for, and the other two still
   pass. Revert.
2. Drop `rivalPresence={state.rivalPresence}` from `<DuelTable>` → `shows the presence beside the
   table it is about` fails on the plate, while its line and its number are still there. That split
   is the point: the word and the sentence travel by two different props and one mutation kills only
   one of them. Revert.

Quote both in the PR.

## Acceptance criteria

- [ ] `the lobby > shows the presence beside the table it is about` passes
- [ ] `the lobby > explains a paused action with the presence it already holds` passes
- [ ] `the lobby > starts a second window fresh, though it carries the same remaining` passes
- [ ] `the lobby > keeps waiting through every frame that neither seats a table nor ends the duel`
      passes, and the only edit to it is the three frames added to `NOT_A_SNAPSHOT` — its name, its
      loop and its assertion are unchanged
- [ ] Every other test in `Lobby.test.tsx` is byte-identical to `develop`
- [ ] `Lobby.tsx` contains no string this story added and no seat literal
- [ ] `npm run --silent test` reports `Tests  N passed (N)` with no failures
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
