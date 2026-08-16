---
id: STORY-0313
title: The table names an absent opponent
type: story
status: blocked
parent: EPIC-03
module: web-client
labels: [client, ui, presence, resilience]
depends_on: [STORY-0307, STORY-0310, STORY-0214]
---

## Goal

A player whose opponent has dropped is told so at the table: that they are away, how long the server
will wait, when the waiting ended, and when they came back — and every action the server took for an
absent seat is labelled in the log as the server's, not as a choice the opponent made.

## Why

`ADR-0028` answered `DEC-018` after this epic was written, and the epic's registers have promised the
pause state ever since — pointing at `STORY-0310`, which renders none of it because `EPIC-03` writes
no Kotlin and the frames do not exist. `DEC-038` asked who ships the other half.
[`ADR-0045`](../../docs/adr/ADR-0045-presence-belongs-to-the-table.md) answers it: the server half is
`EPIC-02`'s [`STORY-0214`](STORY-0214-the-wire-names-an-absent-opponent.md), and **this story is its
own, not a reopened `STORY-0310`.**

The reason is `ADR-0028` §5. Four of its five emission points reach the player who *stayed* — at the
table, mid-duel — and exactly one reaches the returning client. Presence is a table feature that
reconnect observes once; filing it under reconnect would misname it permanently.

**Blocked until `STORY-0214` merges.** Nothing here is startable before then, because
`web-client/src/protocol/protocol.gen.ts` does not yet name `SeatPresence`, `OpponentPresence` or
`ActedForAbsentSeat`. **This story writes no Kotlin**, which is `EPIC-03`'s standing rule and the
rule that produced `DEC-038`.

## The wire it renders

Three frames' worth, all server → client, all already specified:

- **`OpponentPresence(presence, graceRemainingMillis)`**, addressed to this seat alone and carrying
  no seat number — it is always *about the opponent*. `PRESENT`, `AWAY` (the window is running, the
  duel is **paused**) or `ABSENT` (the window ran out, the duel is **live** and the server gives up
  that seat's turns for it). `graceRemainingMillis` is present exactly when `AWAY`.
- **`ActedForAbsentSeat(seat, handNumber, actionSequence, action)`**, to both seats, `action` only
  ever `FOLD` or `CHECK`.
- Nothing new client → server. The client sends **nothing** because of any of this.

## Design notes

- **The countdown is started once and never acted upon.** `graceRemainingMillis` becomes a local
  deadline against the client's own elapsed-time source; the server sends one frame per transition,
  never one per second. `ADR-0028` §3 is a rule, not a suggestion: reaching zero re-enables no
  control, sends nothing, marks no hand lost and assumes no resumption. The duel is paused until an
  `OpponentPresence` carrying `ABSENT` or `PRESENT` says otherwise, and an action sent before then is
  refused with `DUEL_PAUSED` and moves nothing.
- **The countdown is expected to reach zero early**, by up to the server's sweep period plus latency,
  and `AWAY` with zero remaining is a legal frame. Both render as *waiting*, never as an event. This
  is `ADR-0025`'s stated precision, not drift to correct.
- **`YourTurn` is not withdrawn while the duel is paused.** No frame exists to withdraw it, so the
  bar's own state is untouched by presence; `OpponentPresence` is what turns a `DUEL_PAUSED` refusal
  from a mystery into a reason.
- **The mark attaches by coordinate, not by order.** `(handNumber, actionSequence)` identifies the
  decision point uniquely, and the client already holds those coordinates from `YourTurn` and echoes
  them in `Act`. Ordering is a courtesy the server offers; a reducer must not depend on it.
- **Presence is state, so the store holds it and the resume path re-establishes it.** A returning
  client is always sent the opponent's current presence, including `PRESENT` — that is why a reload
  mid-pause does not show a normal table.
- **A returning player is told nothing about what the server did while they were away** beyond the
  state they come back to. `ADR-0028` §6 declines the journal that would be needed; this story
  renders no summary and invents none.
- `ADR-0032` holds: presence arrives as ordinary frames folded by the store, and no component holds
  the `Connection`. The client asserts nothing — it renders three states and a number the server
  sent.

## The words it renders

Settled by [`ADR-0046`](../../docs/adr/ADR-0046-the-table-says-away-timed-out-and-back.md), which
answers `DEC-039`. **Every string below is verbatim.** A rendering ticket quotes them; it does not
paraphrase them, and it invents no sixth string.

The other player is **your rival** in every string — never *opponent*, whatever the wire type is
called.

**The seat's status line**, joining `seat-status.ts`'s existing words:

| `SeatPresence` | The word |
| --- | --- |
| `AWAY` | `Away` |
| `ABSENT` | `Timed out` |
| `PRESENT` | nothing of its own — the seat's ordinary status returns |

The order is `Folded` → `All in` → `Away` / `Timed out` → `Your turn` / `Their turn` → nothing.
Presence outranks the turn (`Their turn` on an empty seat blames a pause on thinking) and never
outranks a fact about the hand.

**The line that explains the state:**

| State | The line |
| --- | --- |
| `AWAY` | `Your rival is away. The duel is paused.` |
| `ABSENT` | `Your rival did not come back. The duel continues, and the server acts for them.` |
| `PRESENT`, after this client held `AWAY` or `ABSENT` | `Your rival is back.` |
| `PRESENT`, with no away or absent state held | nothing at all |

That last row is the trap: a resuming client is **always** sent its rival's presence, `PRESENT`
included, so a `PRESENT` arriving at a client that never held `AWAY` or `ABSENT` is a status quo, not
a return, and renders nothing. `Your rival is back.` clears on the next `Snapshot` and on nothing
else — never on a timer.

**The countdown** carries no word of its own; `The duel is paused.` above it is its label. It counts
down in whole seconds, reaches zero and stays there, and **nothing a player reads changes at zero** —
no *time's up*, no error colour, no sound, no second string. Its numeral shape (`0:45`, `45s`) is the
design's.

**An action the server took** names the server as the subject:

| The frame | The words |
| --- | --- |
| `FOLD` / `CHECK`, about the rival's seat | `The server folded for your rival.` / `The server checked for your rival.` |
| `FOLD` / `CHECK`, about this client's own seat | `The server folded for you.` / `The server checked for you.` |
| either, when the client holds no seat | `The server folded for an absent seat.` / `The server checked for an absent seat.` |

Never the rival as the subject, never `(timed out)` appended to the rival's own action, never
`auto-fold`. Showing the **most recent** mark satisfies this story; no action log, scrollback or
replay view is designed here or required.

**Refused words**, for the same reason each time — the server does not know why a socket closed, and
this is Lichess, not a casino: *opponent*, *disconnected*, *offline*, *left*, *quit*, *abandoned*,
*forfeited*, *sitting out*, and any exclamation mark, sympathy or celebration.

## Tasks

| ID | Title | Status |
| --- | --- | --- |
| — | *Not yet split. Splittable the day `STORY-0214` merges — the copy is settled by `ADR-0046`.* | — |

## Acceptance criteria

- [ ] With the opponent away, the table says so and shows a countdown that started from the frame's
      `graceRemainingMillis`, not from a constant.
- [ ] **The countdown reaching zero changes nothing the client does** — no control is enabled, no
      message is sent, no state is entered — and a test proves it by advancing virtual time past zero
      and asserting the socket saw nothing.
- [ ] An `AWAY` frame carrying zero remaining renders as waiting, not as an error and not as the
      opponent having timed out.
- [ ] `ABSENT` and `PRESENT` each replace the previous state, and the table returns to normal on
      `PRESENT` with no reload.
- [ ] An action attempted during a pause is refused with `DUEL_PAUSED` and the screen explains it
      with the presence it already holds.
- [ ] An event the server took for an absent seat is labelled as the server's, for a `CHECK` as well
      as a `FOLD`, matched by `(handNumber, actionSequence)` and not by arrival order.
- [ ] A client that reconnects mid-pause renders the pause from the presence frame the resume
      carried, not a normal table.
- [ ] **A `PRESENT` frame arriving at a client that never held `AWAY` or `ABSENT` renders no return
      line** — the resuming client is always sent one, and its rival never left (`ADR-0046` §2).
- [ ] Every string on screen is one of `ADR-0046`'s, verbatim, and a test names each. No **rendered**
      text says *opponent*, *disconnected*, *left* or *sitting out* — the rule is about copy, not
      identifiers, and the wire type is still `OpponentPresence`.
- [ ] No client module computes a presence, a deadline or an expiry; every value rendered came from a
      frame.
- [ ] No test sleeps on a real clock.

## Out of scope

- **Any Kotlin.** The frames are `EPIC-02`'s `STORY-0214`. A rendering that needs another field
  raises a decision; it does not edit the server.
- **Any string `ADR-0046` did not write.** The words are settled and closed; a state that seems to
  need a sixth string raises a decision rather than inventing one.
- **Placement, layout, colour and the countdown's typography** — the design's, `EPIC-06`'s. No screen
  under `design/` has an away state today.
- **An action log.** Showing the most recent mark is enough (`ADR-0046` §4); scrollback and replay are
  nobody's story yet.
- A summary of what happened while *this* player was away. `ADR-0028` §6 declines it server-side, so
  there is nothing to render.
- Any client-side timeout, forfeit or resumption. The server owns all three.
- Sound, animation or celebration on a return. The epic defers all of it.
