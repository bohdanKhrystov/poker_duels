---
id: STORY-1214
title: A duel played by hand deadlocked on presence, and the catalogue could not have caught it
type: story
status: ready
parent: EPIC-12
labels: [qa, bug, presence, process]
depends_on: []
---

## Goal

A player reads presence about **the room they are sitting in**. The catalogue can see a product
that marks a present player away. And the trail records that a round dismissed this defect as its
own instrumentation, because that is the process lesson and not a footnote.

## Why

On 2026-09-01 the human played a real duel through two browsers and **could not play it**. Both
tables rendered correct game state — per-seat hole cards, stacks, blinds, board, all agreeing —
and each seat was told the *other* one had vanished. One screen said *Your rival is away. The duel
is paused.* with a countdown that ran to `0s` and stuck there; the other said *Your rival did not
come back. The duel continues, and the server acts for them.* Neither had left.

Three rounds of `/qa-cycle` — `qa`, `uat` and `audit` — ended `PASS` on a product that does this
on the first hand of a fresh room. §The catalogue's blind spot below says exactly why, and it is
not bad luck.

## This is not a round story

`STORY-1203`, `STORY-1204`, `STORY-1207`, `STORY-1208` and `STORY-1212` are the precedent: a story
under `EPIC-12` that repairs the cycle's machinery, or a defect the machinery missed, and **runs no
round**. Nothing here was found by a round, no `A(N)` or `B(N)` moves, and no verdict is recomputed.

## Placement, and why it is here

**Not `STORY-1213`.** `EPIC-12` §Termination rule 1 freezes a round's bug set at triage. The
defect was found by a human playing by hand, outside every round, so it enters the **ordinary
backlog** and may not extend a closed round's story. Round 1 of the audit focus stays closed at
`A(1) = 3`.

**Not a new round story.** No round was run. Filing this as round 2 would put a defect into the
cycle's ledger that its observers never saw, and `ADR-0089` §2c already forbids citing a round as
a coverage claim — inventing one is worse.

**`EPIC-12` rather than `EPIC-02` or `EPIC-03`.** Two reasons, and the second is the deciding one.
`docs/test-plan.md` is this epic's own machinery, so `TASK-121401` could live nowhere else. And the
presence repair's code home is **not yet knowable**: `DEC-107` decides whether the scoping belongs
in the transport, on the wire or in the client, which is to say whether the repair lands in
`poker-server`, in `web-client`, or in both behind a `PROTOCOL_VERSION` bump. Parenting it by
module today would be a guess dressed as a decision. `module:` on each ticket carries the code home
as far as it is known, which is how *"the presence defect belongs where its code lives"* is
recorded without inventing an answer to the question this story exists to ask.

## What was measured, and what was only inferred

Reproduced four times on the live stack at `e1a37a80`, two Chrome profiles on CDP ports 9242 and
9243, server on 8080, Vite on 5173. The wire was read with a throwaway `Network.webSocketFrameSent`
/ `webSocketFrameReceived` tap over CDP — an experiment, reverted, no repository file touched.

**1. The symptom.** Both tables render correct, agreeing game state; each marks the other absent.
A holds `YOUR TURN` under *Your rival is away. The duel is paused.*; B shows *Timed out* and
*Waiting for your rival…*. B was marked away the instant it joined, countdown starting near 60s.

**2. The server sends it — the client does not synthesise it.** Window A's wire, epoch ms:

```
1788289017171 IN   {"type":"Welcome","playerId":"d1d1edee-…","protocolVersion":5}
1788289021602 IN   {"type":"OpponentPresence","presence":"AWAY","graceRemainingMillis":60000}
1788289021922 OUT  {"type":"CreateRoom"}
1788289021924 IN   {"type":"RoomJoined","code":"0QA3WCB6","seat":0}
```

**The presence frame arrives 320 ms before the room exists.** A holds no seat anywhere at
`021602`. A second capture shows the same frame landing 307 ms after `Welcome` on a connection
that then sat in the lobby for ninety seconds. The frame is about the room A *left*, and it is
delivered to the connection A opened afterwards.

**3. Controlled negative.** With both profiles roomless — so a reload produces no presence frame at
all — a create-and-join was clean: **zero `OpponentPresence` frames on either wire, and neither
screen marked anything.** Remove the stale frame and the symptom goes with it, in both directions.

**4. The duel was never paused server-side.** Under *The duel is paused.*, A's `Call 100` was
**accepted**: A's stack moved 9,950 → 9,900 and B was handed the turn. The engine and the server
were correct throughout. The blocker is that the product tells a connected player to wait.

**5. The controls are live under the notice.** Every button read `disabled: false` — `Fold`,
`Call 100`, `Raise to 200`, `All in 10,000` and all four sizing chips. This is why a script clicks
straight through it and a person obeys the message and stops. It is also **not a defect**; see
`DEC-108`.

**6. A navigation is a disconnect.** `drive.mjs open` on A produced `OpponentPresence AWAY` on B
within milliseconds, then `PRESENT` 25 ms later when A resumed. `docs/test-plan.md`'s Reconnect
preamble says the opposite, in as many words.

**Read from source, and stated as a reading rather than a cause.** `SeatDelivery.kt`'s `deliver`
resolves `Addressed.seat` to a `PlayerId` through the room's seating and then takes
`connections.writerFor(player)` — that player's **current** connection, whatever room it is now in.
`ServerMessage.OpponentPresence` carries neither a room nor a seat, which `ADR-0028` §1 chose
deliberately. `duel-state.ts`'s `OpponentPresence` case applies the frame unconditionally. So there
is no point on the path at which a stale frame *could* be recognised. **That is why the repair is a
decision and not a patch**, and no ticket here presumes a mechanism.

**Not confirmed:** that a *present* player is ever folded. `RoomRegistry.expireGracePeriods()`
folding an expired seat is real code, but in every reproduction the hand stayed at Hand 1 Preflop
and nobody was folded — the stuck state was entirely in what the two screens said. The human's
report of constant folding is recorded here as unreproduced, and no ticket is written on it.

## The catalogue's blind spot

- **`CORE-06`** expects *"the two screens never disagree about the board, the pot or either
  stack"*. A and B agreed on all three and disagreed about presence. **The case passes on the
  broken product** — a universal-sounding sentence backed by a closed list of three that omits the
  thing that broke.
- **`CORE-18`** asserts the away marking **appears** when A closes. Nothing anywhere asserts it
  **stays absent when nobody has left**. A product that marks everyone away always, unconditionally
  and immediately passes it.
- **The Reconnect preamble** states a falsehood the whole section rests on, and it is the same
  falsehood that let `TASK-120502` be dropped.

`TASK-121401` repairs all three, plus the case that would have caught the deadlock.

## The trail: a round dismissed a real defect as its own instrumentation

[`TASK-120502`](../tasks/TASK-120502-the-rivals-presence-reaches-the-other-table.md) — *The rival's
presence reaches the other table* — was filed at `high` in round 1 of `/qa-cycle regression`,
reproduced by hand, then **reclassified as a harness defect and dropped** on 2026-08-29,
superseded by `TASK-120506`. Its drop note concluded *"A player who is still connected is present,
and the server is right to say so."* That sentence is false, and finding 1 is what it cost.

The ticket **stays `dropped`**, and an addendum is written on it in this PR rather than deferred to
a ticket. `tasks/README.md` is explicit that a dropped ticket keeps its file *"because rewriting it
as done would hide a real event in the trail"* — and the mistaken reclassification is itself the
event worth keeping. Un-dropping it would erase the one thing this story most needs recorded. The
repair lives in a live ticket instead, `TASK-121403`.

## Decisions

Both registered open by this story, in `docs/adr/README.md` and `tasks/BOARD.md`. Neither is
answered here.

| | |
| --- | --- |
| [`DEC-107`](../../docs/adr/README.md) | **The architect's** — where is a presence frame scoped to the room it is about? Blocked `TASK-121403`. **Answered and merged 2026-09-01** as [`ADR-0104`](../../docs/adr/ADR-0104-a-frame-reaches-the-connection-in-the-room-it-is-about.md); it also split off `DEC-109`, the product owner's, which blocks nothing here. |
| [`DEC-108`](../../docs/adr/README.md) | **The product owner's** — may the action bar stay enabled while the table says the duel is paused? Blocks no ticket here. |

`DEC-108` exists because `ADR-0046` §6 **already declined this question by name** —
*"Whether the action bar's controls look disabled while the duel is paused"* — leaving `YourTurn`
standing and `DUEL_PAUSED` as the refusal. So the live bar contradicts no merged source and is not
a bug; filing it as one would contradict a merged ADR. It is asked, once, of the owner whose
question it is. The case that would check the answer is written when the answer merges, and
`TASK-121401` deliberately does **not** write it — see that ticket's §The fourth amendment.

## Tasks

| ID | Title | Status |
| --- | --- | --- |
| [TASK-121401](../tasks/TASK-121401-the-catalogue-sees-a-present-player-marked-away.md) | The catalogue sees a present player marked away | done |
| [TASK-121402](../tasks/TASK-121402-the-duel-table-column-fits-the-phone-it-is-nested-in.md) | The duel table's column fits the phone it is nested in | ready |
| [TASK-121404](../tasks/TASK-121404-a-connections-room-becomes-a-session-type-the-directory-can-read.md) | A connection's room becomes a session type the directory can read | done |
| [TASK-121403](../tasks/TASK-121403-presence-is-about-the-room-the-reader-is-in.md) | Presence is about the room the reader is sitting in | ready |
| [TASK-121405](../tasks/TASK-121405-the-measured-reproduction-is-a-server-test.md) | The measured reproduction is a server test | backlog |
| [TASK-121406](../tasks/TASK-121406-the-store-is-scoped-to-the-room-the-server-last-named.md) | The store is scoped to the room the server last named | backlog |

**`TASK-121401` and `TASK-121402` are independent of everything, and `depends_on` says so.** They
share no file and no module — `docs/test-plan.md`, `web-client/`, `poker-server/` — and no merged
gate couples them, so either may be worked at any time. An earlier draft of this story chained
`121401 → 121402 → 121403` and gave *"the run is sequential"* as the reason. That is scheduling
convenience, and `depends_on` is not where it belongs: `tasks/README.md` makes CI refuse a `ready`
task whose dependency is unfinished, so the chain would have held the presence repair un-startable
behind an XS padding fix it shares nothing with. This repo's precedent for a real chain is
`STORY-0313`, which states the condition in as many words: *"every one of them touches at least one
file another touches."* Those two do not.

There is still a **preference** about order, and it is only that. Writing `TASK-121401` first gives
the repair a negative case to be measured against rather than one written afterwards to match it. A
scheduler should prefer that order; nothing enforces it, and nothing should.

### The presence repair is four tickets, and the chain among them is real

`DEC-107` was **answered and merged** on 2026-09-01 as
[`ADR-0104`](../../docs/adr/ADR-0104-a-frame-reaches-the-connection-in-the-room-it-is-about.md), so
`TASK-121403` is no longer blocked on a decision. Re-cutting it against that ADR — by stubbing the
change and running `.github/workflows/build.yml`'s pull-request gate set to exhaustion, which is
`ADR-0069` — produced four tickets rather than one, and the split is forced rather than chosen:

- **`TASK-121404`** moves `RoomMembership` into `duels.poker.server.session` and makes its `code`
  `@Volatile` (`ADR-0104` §2). The probe found this half **green on its own** — `./gradlew check
  -PrequireDocker=true` exits 0 with the move applied and nothing else — and `ADR-0068` is explicit
  that a change with a green intermediate state is two tickets, not one declared `atomic:`. It also
  gives the ADR's *"single most important word"* a gate: a reflection test that fails if the
  annotation is dropped, which is the only instrument that can, since its absence *"never [fails] on
  one thread."*
- **`TASK-121403`** is the repair, and keeps its ID because `ADR-0104` §Constrains, `docs/adr/README.md`
  and this story all name it as the ticket the ADR's *Files* table is re-cut into. It is `atomic:`
  at **6 files** on the Kotlin compiler: deleting `ConnectionDirectory.writerFor(player)` and giving
  `register` a third parameter breaks `SeatDelivery.kt`, `DuelSocket.kt` and three test files in one
  step. `ADR-0104` §6 says *"`TASK-121403` is therefore not `atomic:`"* — that sentence is about the
  `PROTOCOL_VERSION` bump it does not carry, and the version does not move; `atomic:` is a property
  of any merged gate that refuses the intermediate state, and the probe found one.
- **`TASK-121405`** is `ADR-0104` §7's first requirement as a socket test. No gate drags
  `DuelSocketDisconnectTest.kt` into the repair's blast radius, so it is a separate ticket; it is a
  regression guard rather than the proof of the fix, and it says so.
- **`TASK-121406`** is the client half, §4. It shares no file with the server tickets and could
  technically start at any time — it depends on `TASK-121403` because landing it first would ship,
  alone, the option `ADR-0104`'s *Alternatives* calls *"the trap in the option set"*: one line that
  turns the recorded trace green while the defect stands.

So `TASK-121404` is the story's single startable presence ticket, and the rest are `backlog` until
their dependency merges.

## Acceptance criteria

- [ ] `docs/test-plan.md` contains a case that is **red on today's product** and green on a
      product that never marks a connected player away.
- [ ] `CORE-06`'s enumeration includes who is seated and present.
- [ ] The Reconnect preamble states what was measured rather than its opposite.
- [ ] At 390 × 664 the duel table measures `scrollHeight ≤ clientHeight`, which `ADR-0103` requires
      and the shipped client does not do.
- [ ] `TASK-120502` carries an addendum saying its reclassification was wrong, and why it stays
      `dropped` anyway. **Written in this PR**, not deferred.
- [ ] `DEC-107` and `DEC-108` are open in both registers, each routed to a named owner.

## Out of scope

- **Answering either decision.** `CLAUDE.md` rule 5.
- **Reopening `STORY-1213`, or any round.** §Placement.
- **Any claim that a present player is folded.** Unreproduced; see §What was measured.
- **`TASK-120506`**, the harness ticket that superseded `TASK-120502`. It is `done` and what it
  built — a case that can end a browser session — is correct and still wanted. The error was the
  reclassification, not the ticket it produced.
- **A second orientation, a third shape, or any viewport below 390 × 664.** `ADR-0097` §5.
