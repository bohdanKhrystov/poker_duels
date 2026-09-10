# ADR-0154 — A gone room is the rematch surface's news, and the front door does not repeat it

- **Status:** Accepted
- **Date:** 2026-09-10
- **Resolves:** `DEC-166` — **when the room behind a standing rematch offer is gone and its holder
  is standing on the front door, which of two merged sentences does the player read: the front
  door's *No duel room has that code.* (`Lobby.tsx:494`) or the panel's *That duel room is gone.*
  (`ADR-0138` §4)?** **The panel's — and the front door says nothing at all.** Registered
  2026-09-10 by
  [`ADR-0151`](ADR-0151-the-press-is-one-fact-and-the-way-back-stops-navigating.md) §7.
- **Derived, not transcribed, and here is the sentence that licenses each half.** That the player
  reads **one** sentence and not two is [`docs/vision.md`](../vision.md)'s Positioning: *"Dark,
  quiet, fast, minimal."* Two error lines for one event is neither quiet nor minimal. That the one
  surviving sentence is the **panel's** is
  [`ADR-0150`](ADR-0150-back-to-the-lobby-keeps-the-room.md) §2 read literally — *"Nothing on the
  front door says a room is held … The only thing the held room may put on a screen is `ADR-0123`'s
  panel, and only when an offer actually stands."* A room that is **gone** is news about the room
  this tab holds, so the front door is the wrong mouth for it; and
  [`ADR-0044`](ADR-0044-a-rematch-is-one-intent-and-one-room-fact.md) §6 already assigned the frame
  — *"**This is the frame that ends a rematch**: the room has been reaped, and the client says so
  and offers the way back to the lobby."* Behind both stands the vision's first success condition,
  whose last beat is the press this is about: *"We play a full heads-up match. Someone wins. **We
  hit Rematch.**"*
- **Writes no word.** Both sentences are merged, neither is edited, and no third one is invented.
  `rematch-text.ts` is not opened.
- **Applies, and amends nothing in:**
  [`ADR-0138`](ADR-0138-the-panel-mounts-beside-the-lobby-and-the-dismissal-lives-in-the-mount.md)
  §4 — the panel's three states, its words and its retirement of `Rematch` ship exactly as merged;
  [`ADR-0123`](ADR-0123-a-standing-rematch-offer-follows-the-rival.md);
  [`ADR-0044`](ADR-0044-a-rematch-is-one-intent-and-one-room-fact.md) §§3 and 6;
  [`ADR-0072`](ADR-0072-a-tab-remembers-its-room-until-the-player-leaves-it.md) §§6–7, whose refusal
  to track which frame a `Failure` answered is a **premise** of this ADR and not a casualty of it;
  [`ADR-0151`](ADR-0151-the-press-is-one-fact-and-the-way-back-stops-navigating.md) §§1–6;
  [`ADR-0153`](ADR-0153-a-rematch-offer-owes-its-recipient-a-minute.md) §4 — nothing here mentions
  the sixty-second floor, and nothing here needs to.
- **Moves nothing outside the client.** No wire type, no `PROTOCOL_VERSION`, no server file, no
  engine change, no schema, no stored key, no new player-facing string, and no change to any colour
  or shape. `poker-engine` is not opened.
- **Registers, and does not answer:** `DEC-168` — **the product owner's** — whether the panel goes
  on saying *That duel room is gone.* for a refusal the player brought on themselves (§6). It
  **blocks nothing**.

## Context

**One field, and nothing in the client knows what it answered.** `Failure` sets `state.refusal` and
that is the whole of it (`duel-state.ts:375`); `REMATCH_UNAVAILABLE` alone is swallowed. The field
is cleared by `RoomJoined`, `YourTurn`, `Snapshot` and `DuelFinished`, and by nothing else — so a
refusal outlives the screen the press was made on. Finding out which frame it answered would mean
tracking an in-flight `OfferRematch`, and `ADR-0072` §7 refuses it by name: *"tracking an in-flight
`OfferRematch` to find out would be the client-side lock `ADR-0044` §3 says no client needs."* That
refusal is merged, it is right, and it is what makes this a choice between two sentences rather than
a lookup.

**Two surfaces read that one field, and both are correct about it.** `Lobby.tsx:494` renders
`refusalMessage(state.refusal)` — *No duel room has that code.* for `UNKNOWN_ROOM` — and it renders
it **only in the front door's return**; the result screen has no such line and never had one.
`RematchNotice` renders `ROOM_GONE` — *That duel room is gone.* — when
`state.refusal === "UNKNOWN_ROOM"`, and `RematchControl` does the same on the result screen. Two
sentences, one fact, and each is addressed to a different act: the front door's answers **a code the
player typed**, the panel's answers **an offer they were sent**.

**Until `ADR-0151` they could not share a screen; now they can, four ways.** The front door was
unreachable while a room was held, so `Lobby.tsx:494` only ever spoke to a tab that held nothing.
`ADR-0151` §2 makes the front door the second screen of a **held finished room**
(`finishedRoomScreen(standing, leftTheResult) === "front-door"`) and §4 puts the panel there with
it. The ways in: a panel `Rematch` press refused on the front door; a refusal taken on the result
screen while answering a rival's offer, carried through the way back; a refusal taken on the result
screen for the player's **own** offer, carried the same way; and a refusal that lands **after** the
way back for a press made before it.

**The two sentences are not symmetric, and that is the crux.** The panel's does a second job: it
**retires the `Rematch` control** (`ADR-0138` §4), which is how `RematchControl`'s own rule — *"A
button that can only fail is not offered"* — is kept once the room is known to be gone. The front
door's line does nothing but speak. So whichever surface yields, the panel cannot yield the
retirement, and any answer that silences the panel has to put a live-looking `Rematch` over a room
the client has been told is dead.

**What each choice costs is real in both directions.** Show both, and the player reads two errors
for one event, one of them about a code they never typed. Keep the front door's and drop the
panel's, and the sentence they read names a code they never typed **and** a dead button stays on
screen. Keep the panel's and drop the front door's, and a player who **did** type a code into the
front door's field while holding a finished room gets no answer to their press. There is no free
answer here; there is only the cheapest one to be wrong about.

**The deadline.** The story that implements `ADR-0150` and `ADR-0151` is unwritten, and the front
door's branch is among the first lines it touches. Decided now, this is one conditional in a ticket
that was going to edit that markup anyway; decided later, it is a coder choosing what a player is
told, inside a ticket where nobody will look for it. Nothing about the choice becomes harder to
reverse with time — no schema, no name, no stored key — which is exactly why it should be settled
cheaply and now.

## Decision

### 1. One event, one sentence, and it is the panel's

On the front door of a tab that is standing there **under a room it still holds** — `ADR-0151` §2's
`"front-door"` answer, the state that did not exist before it — the front door states **no
`UNKNOWN_ROOM` line**. What the player reads about a gone room is the panel's *That duel room is
gone.*, and that is the whole of it.

### 2. Every other front door is byte-unchanged

A tab that holds no room keeps `Lobby.tsx:494` exactly as merged, and this is the ordinary path: a
mistyped code, an invite link naming a room that has been reaped, and boot's stale-code correction
(`ADR-0072` §6, whose failed rejoin leaves `state.roomCode` null and the standing `none`). Those
players typed or followed a code, *No duel room has that code.* is the true and useful answer, and
nothing here touches them.

### 3. Only `UNKNOWN_ROOM` is withheld, and only there

`ROOM_FULL` — *That duel room already has a rival in it.* — and the default *The server refused
that.* go on being stated wherever they are stated today, the held-room front door included. They
collide with nothing, because the panel states neither; and on the held-room front door each can
only be the answer to a code the player typed in the field in front of them.

### 4. The panel is not edited, and neither is the result screen

`RematchNotice` and `RematchControl` ship as merged: the same three states, the same words, the same
retirement of `Rematch`, the same `Not now` beside all three (`ADR-0138` §4). The dismissal does not
give the front door its sentence back either — a player who presses `Not now` has dismissed the
panel, not asked the front door a question — so the withholding in §1 is a property of the screen
the player is on and of the refusal that arrived, not of whether the panel happens to be visible at
that instant.

### 5. What the player reads, by the way they arrived

| The way in | What is on screen |
| --- | --- |
| The panel's `Rematch`, pressed on the front door, refused | *That duel room is gone.* in the panel, alone, with `Rematch` retired and `Not now` below it |
| Refused on the result screen while answering a rival's offer, then the way back | the panel restates it on the front door; the front door adds nothing |
| Refused on the result screen for the player's **own** offer, then the way back | `RematchControl` already said it on the result screen; the front door adds nothing, and no panel stands because no rival's offer does |
| A refusal that lands after the way back, for a press made before it, with no rival's offer standing | **nothing is stated** — named as a cost below |

### 6. Registers, and does not answer: `DEC-168`

**`DEC-168` — the product owner's — does the panel go on saying *That duel room is gone.* for a
refusal the player brought on themselves?** `RematchNotice` branches on the same one field, so on
the held-room front door a mistyped room code now flips the panel to the gone-room sentence and
retires `Rematch` over an offer that may still be perfectly live — for the rest of the room's five
minutes, and for at least the sixty seconds `ADR-0153` §1 guarantees it. Newly reachable for exactly
the reason `DEC-166` was, **not** answered here, and not answerable by tracking the press: `ADR-0072`
§7 refuses that, and `RematchControl` shares the branch and the words (`ADR-0138` §4), so any answer
has to say what the **result screen** does too. **It blocks nothing** — §1's conditional ships either
way — and it is due with the story that implements `ADR-0150`, or the first UAT round that reaches
the front door with a room held.

## Consequences

**What it buys.** A player is told once, by the surface they acted on, about the thing they acted
on. `ADR-0150` §2's *"nothing on the front door says a room is held"* survives its first real test —
the front door goes on saying nothing about the held room even when the held room is the news — and
`ADR-0123` §3's *"one fact never has two live surfaces at once"* now holds for the room's death as
well as for the offer's life. It costs no wire, no version, no schema, no storage, no server change
and no word; the repair is the one conditional `ADR-0151` §7 predicted, and it reads a fact `Lobby`
already computes for `ADR-0151` §2's cascade, so nothing new is plumbed.

**What it costs.**

- **A press can now go unanswered.** A player holding a finished room who types a room code into the
  front door's field and names a room that does not exist reads **nothing** — the one surface that
  would have answered them is the one §1 silences, and the panel, if it is standing, answers about a
  different room. They will press again, and each press spends one of `ADR-0022`'s ten failed joins
  a minute. The same silence covers §5's fourth row, where a refusal for a result-screen press lands
  a round trip after the way back with no rival's offer standing. This is the price of choosing with
  one field and no provenance, it is the strongest argument the losing alternative had, and it is
  paid deliberately.
- **The front door's refusal line stops being a function of one field.** `Lobby.tsx:494` reads
  `state.refusal !== null` today and nothing else; after this it also reads which screen the room
  puts up. A reader of that line has to know about a second surface to know when it shows, and the
  reason lives in this ADR and in a comment rather than in anything a compiler or a gate enforces.
- **`UNKNOWN_ROOM` becomes the one refusal with a per-screen rule.** The front door's three
  sentences no longer behave alike (§3). If the panel ever states a second error, this asymmetry has
  to be re-decided rather than extended, and the next reader will find one arm of a `switch` treated
  differently from its siblings with the reason two files away.
- **It makes neither sentence true — it only stops them contradicting each other.** The panel's
  `ROOM_GONE` still fires on a refusal that may have answered something else entirely, and this ADR
  leaves that branch exactly as merged. `DEC-168` is that debt, named rather than papered over.
- **The result screen and the front door now say different things about the same event by design.**
  A player refused on the result screen reads a sentence; the same player, one press later, reads
  none. Bounded — they were told on the screen where they pressed — but it is a state in which
  walking between screens changes what the product is willing to say.

**What it forecloses.** Very little, and nothing structurally. It does not build provenance for
`Failure`, a second refusal field, a per-request correlation id, a toast, a notice on the front door
about the held room, or a way back to the result screen. It does not touch the panel, the result
screen, or any string. Reversing it is deleting one conjunct.

**What it deliberately leaves open.** `DEC-168`, above. Also untouched: what a player reads on the
held-room front door when a refusal that is *not* `UNKNOWN_ROOM` arrives for a rematch press —
`REMATCH_UNAVAILABLE` is swallowed by the reducer and states nothing anywhere, which is `ADR-0044`
§6's *transient* reading and is not reopened here.

## Alternatives considered

**Keep the front door's line and silence the panel's.** Its case is the stronger of the two obvious
ones: the front door is the screen the player is actually looking at, its line sits in the document
flow where the eye already is, and the panel is a corner surface a player may never read; it also
keeps `Lobby.tsx:494` a function of one field, which is the simpler thing to explain. Rejected on
two counts, either of which is enough. The sentence names *that code* to a player who typed no code
— the misattribution `DEC-166` was registered about. And the panel's sentence is not only a
sentence: it is what retires `Rematch` (`ADR-0138` §4), so silencing it leaves a live-looking button
over a room the client has been told is dead, breaking `RematchControl`'s own *"A button that can
only fail is not offered"* on the very screen `ADR-0150` invented.

**Show both and change nothing.** Its case: zero code, zero risk, both sentences are true, players
skim past error text, and the repository has more valuable places to spend a conditional. Rejected
because one of the two is addressed to an act the player did not perform, and because saying one
fact twice in two different forms is the opposite of the vision's *"Dark, quiet, fast, minimal."* —
a player who reads both has to work out whether one room died or two things went wrong.

**Give the front door the panel's words: make `refusalMessage("UNKNOWN_ROOM")` read *That duel room
is gone.*** Its case is real and it is the cheapest of all: one string, no conditional, no
per-screen rule, no asymmetry between the three refusals, and the sentence is true of every
`UNKNOWN_ROOM` there is, since `ADR-0044` §6 collapses *reaped*, *not a player* and *no room* into
one value. Rejected because it leaves **both** copies on screen — it fixes the misattribution and
not the duplication — and because it makes the front door worse at its own job: a player who
mistyped a code has not watched a room die, and *That duel room is gone.* tells them a room existed
when the likeliest truth is that they typed it wrong.

**Clear `state.refusal` when the player leaves the result screen.** Its case: a refusal is an answer
to a press on a screen, leaving that screen ends the conversation, one line in `ADR-0151` §3's
`leave()` does it, and it would fix §5's third and fourth rows as well as the collision. Rejected
because the panel reads the same field: clearing it would put `Rematch` back over a room known to be
gone the instant the player pressed the way back, which is the same dead button as the first
alternative reached by a different road. It would also have the client forget something the server
told it, against `ADR-0072` §6's *"a stale memory is corrected by the server, not by a clock"*.

**Track which press the refusal answered, and let each surface answer for its own.** The honest
answer, and the only one that gets all four of §5's rows right: the client dispatched both frames
itself and could remember which. Rejected because `ADR-0072` §7 refuses exactly this — *"the
client-side lock `ADR-0044` §3 says no client needs"* — and it is refused for a reason that has not
changed: the correlation would be a second, unsynchronised account of a conversation the socket
already owns, and it would be wrong in precisely the case that motivates it, a player who presses
twice on two surfaces. Overturning it is an amendment to two merged ADRs and a mechanism decision,
which is not what `DEC-166` asked for; if the residue this ADR names ever justifies the cost, that
is the road, and it starts with `DEC-168`.
