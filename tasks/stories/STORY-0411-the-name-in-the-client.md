---
id: STORY-0411
title: The name in the client — shown, and settable
type: story
status: done
parent: EPIC-04
module: web-client
labels: [client, ui, profiles, identity]
depends_on: [STORY-0402]
---

## Goal

The client shows the name the server sent — for me, and for the opponent on a result line — offers a
player who has none the chance to set one, and renders each of the server's refusals as something a
person can act on.

## Why

`EPIC-03` renders `opponentPlayerId` and has no way to set a name; it named this story as where both
are fixed. It is also the first client work in this epic, and `STORY-0412` and `STORY-0413` queue
behind it because all three extend the same store and screen shell.

## Design notes

- **The client derives nothing.** It renders `displayName` and `opponentDisplayName` exactly as
  received, and never falls back to `opponentPlayerId`, never builds `Player-3F2A`, never
  title-cases or trims for display. The name it shows is a server fact
  ([`ADR-0002`](../../docs/adr/ADR-0002-server-authoritative.md)).
- **`null` is rendered, not hidden.** What a nameless player looks like is decided here, inside
  `EPIC-06`'s language, and `ADR-0029` §6 leaves the treatment to the client on the one condition
  that the client never asks the server for a placeholder. Whatever the treatment is, it composes
  `design/tokens/tokens.css` and authors no colour.
- **The set-name form sends what the player typed and shows what came back.** The server trims and
  normalises, and `ADR-0029` §5 returns the whole profile for that reason: the field is repopulated
  from the response, never from the input.
- **Three failures, three sentences**: `400` (a name the rules refuse), `409` (not available),
  `403` (you already have one, and it cannot change). `403` is the one a client must not offer a
  retry for, and the copy has to say *permanent* — a form that invites a retry it can never satisfy
  is worse than a form that refuses. The `409` sentence reads **"That name is not available. Try
  another."** and never *taken*:
  [`ADR-0051`](../../docs/adr/ADR-0051-a-name-is-registered-before-it-is-held.md) §2 answers `409`
  for a held name, a blocked name and a name retired from **this very player**, indistinguishably,
  and for that last player *taken* is false
  ([`ADR-0052`](../../docs/adr/ADR-0052-a-takedown-is-told-to-the-player-it-happened-to.md) §7).
- **Setting a name is offered once and is permanent** — the screen must say so **before** the send,
  not after. `ADR-0029` costs a typo forever, and a player is entitled to know that at the moment
  they can still avoid it. The line reads **"A name is chosen once. You cannot change it later, and
  it can be taken away."** — permanence *to the player*, since `ADR-0038` made a name removable and
  no screen had yet said so (`ADR-0052` §7).
- **A fourth state: the name was removed.** A player who holds no name and had one taken away reads
  `ADR-0052` §2's four sentences above the same form, derived from `ProfileResponse.displayNameRemoved`
  and never delivered, pushed or dismissed. Nobody else is told anything: a duel line against them
  renders exactly as any nameless opponent's does (`ADR-0052` §5).
- No client test sleeps on a real clock, and no test asserts a value that is only ever the fixture's
  default.

## Tasks

Split on 2026-08-18 into **seventeen**, following `ADR-0029`, `ADR-0051`, `ADR-0052` and
`ADR-0053`, and **eighteen** since 2026-08-26. The chain is linear: every ticket touches at least one
file the one before it touched, and the run is sequential.

**The eighteenth was filed after the story closed, against merged code, and it is a test that could
not fail.** `TASK-041110`'s `sends what the player typed, once, however many times the button is
pressed` dispatches two bare `fireEvent.click()` calls to prove the in-flight guard sends one
request. `@testing-library/react` wraps every `fireEvent` in its own `act()`, so React flushes
between the two: the second click lands on a button that already carries `disabled`, no second
submit starts, and the call count of `1` measures the `isSubmitting` **state**. Delete
`NameSurface.tsx`'s `submitInFlight` ref — the one line whose own comment explains that state has not
caught up when the second submit runs — and every one of the nine tests still passes. `TASK-041118`
nests both dispatches in a single outer `act`, which is what leaves the second submit reaching the
handler with the state uncommitted, and its Proof is that mutation run **twice**: green against the
merged test, red against the fixed one. Found when the identical mutation reddened nothing while
`TASK-041218` was being planned, and confirmed independently at review against both commits.

**Three judgements the split had to make, recorded here rather than left in a ticket:**

- **The first three tickets add no behaviour at all.** `PlayerProfile` and `RecentDuel` gain
  required fields, and adding them today breaks four test files at `tsc` and eight assertions at
  runtime — measured by making the change and running `npm run typecheck` and the suite, not
  estimated. Seven files is beyond any ticket's budget, so the construction moves behind one fixture
  module first and the field then lands in three files. This is the shape `TASK-041017` took on the
  server side of `STORY-0410`.
- **A `null` display name is rendered through one function**, `nameOrNone`, rather than by an `??`
  in each component. `ADR-0052` §5 requires a nameless opponent and an opponent whose name was
  removed to be indistinguishable, and two components deciding separately is how they drift apart.
- **`DEC-051` blocked `TASK-041114` and the three tickets behind it, and nothing before them.** It
  was answered the same day by
  [`ADR-0058`](../../docs/adr/ADR-0058-where-a-name-would-be-the-client-prints-no-name.md) — the
  client prints **`No name`** — and the split stands unchanged, because the answer is a single
  string returned by the one function above.

| ID | Title | Status |
| --- | --- | --- |
| [TASK-041101](../tasks/TASK-041101-one-fixture-builds-every-profile-a-test-uses.md) | One fixture builds every profile and duel line a test uses | ready |
| [TASK-041102](../tasks/TASK-041102-the-reads-tests-build-through-the-fixture.md) | The strip's read tests build through the fixture | backlog |
| [TASK-041103](../tasks/TASK-041103-the-components-profiles-build-through-the-fixture.md) | The component tests build their profiles through the fixture | backlog |
| [TASK-041104](../tasks/TASK-041104-the-profile-read-carries-the-name-and-the-removal.md) | The profile read carries the name, and whether one was removed | backlog |
| [TASK-041105](../tasks/TASK-041105-a-duel-line-carries-the-opponents-name-and-no-id.md) | A duel line carries the opponent's name, and still not their id | backlog |
| [TASK-041106](../tasks/TASK-041106-one-put-sets-the-name-and-every-answer-is-its-own-outcome.md) | One PUT sets the name, and every answer is its own outcome | backlog |
| [TASK-041107](../tasks/TASK-041107-the-words-the-name-surface-says.md) | The words the name surface says, and which of them leave a way back | backlog |
| [TASK-041108](../tasks/TASK-041108-the-surface-shows-a-name-or-offers-to-set-one.md) | The name surface shows the name, or offers to set one and says what that costs | backlog |
| [TASK-041109](../tasks/TASK-041109-the-surface-says-a-name-was-removed-only-when-it-was.md) | The surface says a name was removed, only to the player it happened to | backlog |
| [TASK-041110](../tasks/TASK-041110-the-surface-sends-once-and-shows-what-came-back.md) | The surface sends once, and shows the name that came back | backlog |
| [TASK-041111](../tasks/TASK-041111-each-refusal-says-its-own-sentence.md) | Each refusal says its own sentence, and only two leave the form | backlog |
| [TASK-041112](../tasks/TASK-041112-the-write-reaches-the-tree-the-read-already-does.md) | The write reaches the tree the same way the read already does | backlog |
| [TASK-041113](../tasks/TASK-041113-the-lobby-shows-the-name-surface-and-the-table-does-not.md) | The lobby shows the name surface, and the duel table never does | backlog |
| [TASK-041114](../tasks/TASK-041114-the-word-for-a-player-with-no-name.md) | The word for a player who has no name | backlog |
| [TASK-041115](../tasks/TASK-041115-the-strip-prints-the-players-own-name.md) | The strip prints the player's own name, or what stands for none | backlog |
| [TASK-041116](../tasks/TASK-041116-a-duel-line-names-the-opponent.md) | A duel line names the opponent it was played against | backlog |
| [TASK-041117](../tasks/TASK-041117-no-name-on-the-screen-is-built-from-a-player-id.md) | No name on the screen is built from a player id, and a takedown is invisible | backlog |
| [TASK-041118](../tasks/TASK-041118-two-clicks-in-one-act-or-the-guard-is-not-under-test.md) | Two clicks inside one act, or the in-flight guard is not the thing under test | ready |

## Acceptance criteria

- [ ] The profile strip shows the player's own name when there is one, and the agreed treatment when
      there is not — both asserted, from two distinct fixtures.
- [ ] A result line shows the opponent's name when there is one, and never shows a player id.
- [ ] Setting a name sends one request, and the field afterwards holds the **canonical** string the
      server returned, asserted with an input the server would change.
- [ ] Each of `400`, `409` and `403` renders its own sentence, and only `400` and `409` leave the
      form retryable.
- [ ] The screen states that the choice is permanent before the request is sent, in the words
      `ADR-0052` §7 fixes.
- [ ] A player who holds no name and had one removed reads `ADR-0052` §2's notice above the same
      form; a player who never set one reads nothing — both asserted in **one** render.
- [ ] A duel line for an opponent whose name was removed is byte-identical to one for an opponent
      who never set a name.
- [ ] The client sends nothing derived: no request body contains a player id, and no rendered name
      is computed from one.
- [ ] The suite's own count is asserted, and no test sleeps on a real clock.

## Open decisions

**None.** `DEC-051` — what the client prints where a display name would be, for a player who has
none — was answered on 2026-08-18 by
[`ADR-0058`](../../docs/adr/ADR-0058-where-a-name-would-be-the-client-prints-no-name.md): **`No
name`**, the same two words on every surface, about every player, whatever the reason there is no
name, and no second-person variant on the player's own strip. `TASK-041114` takes the string
verbatim; `TASK-041115` and `TASK-041116` call the function and are unchanged.

## Out of scope

- The account screens — `STORY-0412`.
- The history screen — `STORY-0413`.
- The offer to make an account — `STORY-0415`.
- Any colour or type decision — `EPIC-06` owns the language this composes.
