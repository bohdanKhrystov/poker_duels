# ADR-0046 — The table says *away*, *timed out* and *back*, and names the server when it acts

- **Status:** Accepted
- **Date:** 2026-08-16
- **Resolves:** `DEC-039` — what words does a player read for the three presence states of
  [`ADR-0028`](ADR-0028-the-wire-names-an-absent-opponent.md), and for an action the server took on
  an absent seat's behalf? Minted and answered in this change: the question has lived since
  2026-08-16 as the `## Open input` section of
  [`STORY-0313`](../../tasks/stories/STORY-0313-the-table-names-an-absent-opponent.md) rather than
  as a numbered row in any register.
- **Where the answer came from:** [`docs/vision.md`](../vision.md), *Positioning* — *"The reference
  points are **Lichess** and **Chess.com**, not PokerStars. Dark, quiet, fast, minimal. The
  vocabulary is duelling, not gambling: challenge, duel, rematch, rival, streak, season — never
  buy-in, bankroll, jackpot, bonus."* That sentence fixes the noun for the other player (*rival*),
  the register every string here is written in, and most of the words that are refused. Two of the
  three state words are the human's already:
  `DEC-018` was answered verbatim as *"away + countdown + mark timeout folds"*, so this ADR **spells**
  *away* and *timed out* rather than choosing them.
- **Takes a question `ADR-0028` reserved to the human**, and says so rather than quietly assuming it.
  That ADR closes by leaving *"every word a player actually reads"* to the human, on 2026-08-14. The
  `product-owner` role was created on 2026-08-15 and
  [`docs/workflow.md`](../workflow.md#who-answers-a-dec)'s routing table now sends *"what a player
  sees"* to it, deriving from the vision. Nothing below adds or subtracts a commitment
  `docs/vision.md` makes, so it is answered rather than escalated — and a string is the cheapest
  thing in this product to reverse: no schema, no wire, no stored data, nothing a player keeps. Any
  line here is overruled by one sentence from the human.
- **Constrains:** [`STORY-0313`](../../tasks/stories/STORY-0313-the-table-names-an-absent-opponent.md)
  — every string it renders. It constrains no Kotlin, no wire type and no protocol version.

## Context

`ADR-0028` bought honesty on the wire and stopped one word short of the player. Five emission
points, three states and one mark are specified down to their `init` blocks; the ADR then reserves
the words. The frames can therefore be built, decoded, stored and never read by anyone, and the
whole value of the design sits in a string that does not exist. `ADR-0045` recorded exactly that
when it filed the story: *"`STORY-0313` cannot be split into rendering tickets when it unblocks."*
Six things are in tension.

**The register already exists, and it is narrow.** What has shipped: `Waiting for your rival…`,
`The duel is paused. That action was not applied.`, `Victory` / `Defeat` / `Draw` / `Duel over`,
`Folded` / `All in` / `Your turn` / `Their turn`, `+1 duel coin`, `Your rival`. Two shapes and no
third: short capitalised fragments for what a seat is doing, plain full sentences for anything that
needs explaining. No exclamation mark, no sympathy, no jargon, nothing a casino would print. A new
state that arrives in a different voice reads as a different product.

**The three states are not three degrees of one thing.** `AWAY` **pauses** the duel and an action
sent during it comes back `DUEL_PAUSED`; `ABSENT` means the duel is **live again** with the server
giving up that seat's turns for it; `PRESENT` is ordinary play. One "your rival is gone" word for
the first two would be wrong in the way that matters most to the person reading it — whether their
next click will do anything.

**The server does not know why the socket closed.** A shut tab, a locked phone, a train tunnel and
somebody storming off all reach `RoomRegistry.disconnect` identically. Every word that names a cause
— *disconnected*, *left*, *quit* — asserts something the server has no evidence for, in a product
whose entire claim is that the server is the only thing telling the truth.

**The fourth case has two ways to be wrong and no neutral ground.** `foldAbsent` submits an ordinary
action down the ordinary path: the engine's log says `PlayerFolded`, exactly as it would for a fold
somebody chose, and `duel-rules.md` Part 3 is explicit that *"it is the server that decides to submit
a fold on their behalf"*. The player who stayed watches their rival fold without folding. Name the
rival as the actor and the copy says they chose it. Name no actor and every reader supplies the
rival anyway. Name the absence too loudly and the copy accuses somebody of quitting who may have
walked into a lift.

**One `PRESENT` frame means two different things to its two recipients.** `ADR-0028` §5: the seat
that stayed is sent `PRESENT` **only** when the other seat had actually been away, while a returning
client is **always** sent its rival's current presence, `PRESENT` included. So "your rival is back"
cannot be attached to the frame — attached to the frame, it tells a player who reloaded the page that
their rival returned from an absence that never happened.

**There is nowhere to put a sentence about an action.** `DuelState.narration` accumulates every
`GameEvent` the server sends and no component reads it: the client renders no action log at all. The
mark is a fact about a decision point in a list the player cannot see.

### The deadline

Nothing is stalled today — `STORY-0313` is blocked on `STORY-0214`, which is itself behind
`STORY-0213` in `ADR-0045`'s version queue. Two reasons to answer now, and neither argues for a
particular answer.

Copy is the input the planner needs **before** it can split the story, so answering is what turns
`STORY-0313` from *blocked* into *ready the day it unblocks*.

And **strings acquire tests here.** The strings this repository has shipped are pinned by tests
quoting them verbatim — `TASK-030802` asserts the four verdicts by name, and `TASK-030710` asserts
that `Fold`, `Call`, `Bet`, `Raise` and `All in` appear nowhere in a bar the server did not offer them
in. Choosing the copy after the rendering tickets are written means changing the words *and* the
tests that quote them. Choosing now costs a document.

## Decision

### 0. The other player is **your rival**, in every string

Never *opponent*, never *player 2*, never *your opponent*. The wire type is `OpponentPresence` and
the copy does not follow type names — `ADR-0028` says so itself. `DuelTable` and `DuelResult` already
say `Your rival`, and the vision names *rival* as a word this product uses. Where a display name
later exists, it takes the same slot in the same sentences.

### 1. The three states, in the seat's status line

The line `seat-status.ts` already fills with `Folded`, `All in`, `Your turn`, `Their turn` or
nothing:

| `SeatPresence` | The word |
| --- | --- |
| `AWAY` | `Away` |
| `ABSENT` | `Timed out` |
| `PRESENT` | nothing of its own — the seat's ordinary status returns |

**Presence outranks the turn and never outranks the hand.** The order is `Folded` → `All in` →
`Away` / `Timed out` → `Your turn` / `Their turn` → nothing. `Their turn` on a seat nobody is sitting
at attributes a pause to thinking; `Folded` and `All in` stay true whoever is at the keyboard.

### 2. The three states, in the line that explains them

One line, in the table's sentence voice:

| State | The line |
| --- | --- |
| `AWAY` | `Your rival is away. The duel is paused.` |
| `ABSENT` | `Your rival did not come back. The duel continues, and the server acts for them.` |
| `PRESENT`, after this client held `AWAY` or `ABSENT` | `Your rival is back.` |
| `PRESENT`, with no away or absent state held | nothing at all |

The second sentence of each is the one that answers *what does this mean for me* — **paused** is why
the next action is refused, **continues** is why the hands keep coming. The first sentence never
says why the rival is away, because nothing knows.

The last row is the one to get right, and it is the failure this ADR most expects: a client that
reloads mid-duel is always sent its rival's presence, so `PRESENT` arriving at a client that never
held `AWAY` or `ABSENT` is a status quo and renders **nothing**. Telling a returning player that
their rival is back when the rival never left is the one way this copy can state a falsehood.

`Your rival is back.` clears on the next `Snapshot` and on nothing else — never on a timer, never on
a fade. The client acquires no clock it did not already have (`ADR-0028` §3), and a line that outlives
the moment costs a player nothing.

### 3. The countdown is a number, and never an event

It starts from the frame's `graceRemainingMillis` (60 s by default, `RoomTimeouts`), counts down in
whole seconds, reaches zero and stays there.

**It carries no word of its own** — `The duel is paused.` above it is its label — and there is no
second string for zero. No *time's up*, no *expired*, no error colour, no sound, no change of any
kind. The design fixes the numeral's shape (`0:45`, `45s`); the product rule is only that **nothing a
player reads changes when the countdown reaches zero**, because the countdown reaching zero is not a
fact about the duel. It is expected to arrive there early by up to a sweep period plus latency, and
`AWAY` with zero remaining is a frame the server legitimately sends.

### 4. An action the server took names the server

`ActedForAbsentSeat` reads as a sentence with a subject:

| The frame | The words |
| --- | --- |
| `FOLD`, about the rival's seat | `The server folded for your rival.` |
| `CHECK`, about the rival's seat | `The server checked for your rival.` |
| `FOLD`, about this client's own seat | `The server folded for you.` |
| `CHECK`, about this client's own seat | `The server checked for you.` |
| either, when the client holds no seat | `The server folded for an absent seat.` / `The server checked for an absent seat.` |

Four rules, in order of how easily each is broken:

- **The subject is always the server.** The absent player is never the subject of the sentence.
- **The verb is the past tense of the `ActionType` the frame carried, and nothing else.** `FOLD` and
  `CHECK` are the only two it can carry — the type's `init` refuses the rest.
- **Nothing is said about why the seat is absent.** The presence line already says as much as is
  known, and repeating it on every action turns a fact into an accusation.
- **The sentence must be false if the reader stops after the verb.** `The server folded…` is not a
  claim about the rival; `Your rival folded…` is.

*The server* is not a new character on screen: `The server did not apply that action.` and
`The server refused that.` both ship today.

Forbidden, by name: `Your rival folded` for an action they did not take; `(timed out)` or `(away)`
appended to the rival's own action; `auto-fold`, `auto-check`, `default fold`, `timeout fold`.

**Where it goes.** The mark reaches the player who stayed at the time it happens, and showing the
**most recent** one is enough to satisfy this ADR. No action log, no scrollback and no replay view is
designed here, and `STORY-0313` is not required to build one.

### 5. The words this copy refuses, and why

| Not used | Because |
| --- | --- |
| *opponent* | the vision fixes the noun as *rival*; the wire type name is not copy |
| *disconnected*, *connection lost*, *offline* | names a cause the server cannot see |
| *left*, *quit*, *abandoned*, *gave up* | asserts an intention, and none of them is knowable |
| *forfeited*, *forfeit* | false: an absent seat is still playing, badly, and can still win the duel |
| *sitting out*, *sit out* | a cash-game word from a room with a waiting list. Two seats, never three |
| *timed out* for anything but `ABSENT` | it is the one state where a window actually ran out |
| any exclamation mark, and any word of sympathy or celebration | dark, quiet, minimal |

### 6. What this does not write

- **Anything a returning player reads about their own absence.** `ADR-0028` §6 builds no journal and
  `resumeFrames` replays nothing, so there is no fact to render. Copy that needs a frame that does
  not exist is a server decision in disguise.
- **Anything a spectator reads.** `OpponentPresence` is recipient-relative and a watcher has no
  opponent (`ADR-0028` §6, `ADR-0040`).
- **Placement, layout, colour and the countdown's typography.** The design's, `EPIC-06`'s — `design/`
  has no away state in any screen today.
- **Whether the action bar's controls look disabled while the duel is paused.** `ADR-0028` §6 keeps
  `YourTurn` standing and `DUEL_PAUSED` as the refusal, and `STORY-0313`'s criteria already assume a
  live bar and an explained refusal. What that refusal *says* is unchanged and already shipped.
- **Whether `Waiting for your rival…` and the presence line appear at once.** Both are true at the
  same time and neither contradicts the other; resolving the redundancy is the design's.

## Consequences

**What it buys.** `STORY-0313` becomes splittable the day `STORY-0214` merges — the last input
`ADR-0045` named as missing is supplied, and the planner writes rendering tickets instead of routing
a question. The honesty `ADR-0028` paid a wire break for reaches a person: a player learns that their
rival is away, that the duel is paused, that the waiting ended, that the server is now acting, and
which of those actions were not their rival's. Every string is derived from a frame the wire already
carries, so no client has to work anything out. And the two words the human chose in `DEC-018` are
the two words on the screen.

**What it costs.**

- **`Timed out` will collide with a turn clock.** `ADR-0028` records that a per-action turn clock
  *"still fits alongside"* this design. The day one ships, a seat plate reading `Timed out` means
  two different things — a grace window that expired, and a decision that went unmade — and the
  repair is a rename in a client that by then has strings pinned by tests, plus whatever the design
  has built around them. This is the most likely reason a future ADR supersedes this one.
- **The mark needs a home the client does not have.** `narration` is rendered nowhere, so
  `STORY-0313` has to invent somewhere for a sentence to appear or the honesty stays on the wire and
  never reaches a player. §4's "the most recent one is enough" bounds that work; it does not remove
  it, and it is work this decision creates and does not do.
- **`Your rival is back.` requires the client to remember what it was last told.** The store must
  hold the previous presence to tell a return from a resume — a second field of the kind `ADR-0043`
  added in `rejectionCount`, client bookkeeping the server never sent. Every such field is one more
  place the client can be wrong about the table while looking confident.
- **`The duel continues, and the server acts for them.` teaches a player how to beat an absent
  seat.** Bet and it folds; check and it checks. `ADR-0028` recorded that absence became exploitable
  information; this sentence hands it to a player who had not worked it out, in plain English. That
  is the honest trade the vision asks for — *showing a player the maths is more interesting than
  hiding it* — and it is written down here so nobody is surprised when a rival's disconnection turns
  into a stack.
- **Two string sets now have to stay in step.** The presence line says *the server acts for them* and
  the mark says *The server folded for your rival.* Nothing can test tone, so the only thing keeping
  them consistent is this table and whoever reads it next.

**What it forecloses.** Very little, and cheaply. A string is a one-line change with no migration:
if `Away` reads wrong in front of a real player, it is replaced in an afternoon plus the tests that
quote it. What it does close off deliberately is the shape of the fourth case — a client built around
a full sentence naming the server would need re-shaping, not re-wording, to become a badge on the
rival's own action, and that is exactly the shape §4 rejects.

## Alternatives considered

**Say nothing new: let `Waiting for your rival…` cover it.** The strongest case is cost — zero new
strings, zero new client state, no home to find for a sentence, and the line is already true, since
the rival is indeed being waited on. Rejected because it makes a paused duel indistinguishable from a
slow rival, which is the exact dishonesty `DEC-018` was raised to end, and it leaves `DUEL_PAUSED` a
refusal with nothing on screen to explain it. It would spend a protocol break on frames nobody reads.

**`Disconnected` and `Reconnected`.** The words every other game uses, understood instantly, with no
ambiguity against a future turn clock — and `Reconnected` is a cleaner return word than *back*.
Rejected because the server does not know it. A shut tab and a dead router are one fact to
`RoomRegistry.disconnect`, and a product whose whole position is that the server tells the truth
cannot afford the small lie that makes a player doubt the large statements. *Away* says exactly what
is known and no more.

**`Sitting out`.** The term of art, genuinely neutral, not accusatory, and it describes a seat whose
turns are being given up precisely. Rejected on the vision's own sentence: it is a cash-game word
from a nine-handed room with an empty chair and a waiting list, and this product has two seats and
never a third. Importing it would put the casino's vocabulary on a screen built to refuse it.

**Name the rival as the actor and qualify it: `Your rival folded (timed out)`.** The strongest case
of the six. It goes exactly where the action goes, so it needs no new place on screen at all, it
reads naturally to anybody who has used a poker client, and it says both facts in one line. Rejected
because it makes the rival the subject of a decision they did not take, and a parenthesis does not
undo a subject — a reader who stops after the verb has been told something false. It is the wire half
of `ADR-0023`'s indistinguishability, reintroduced in prose after `ADR-0028` spent a version step
retracting it.

**An actor-free mark: `Folded — absent`, or a badge on the event.** Terse, log-shaped, matches the
status-fragment half of the register, needs no sentence to place, and it survives unchanged into a
future scrollback log where a full sentence per line would be heavy. It was close. Rejected because
it leaves the reader to supply the actor, and every reader supplies the rival: the one thing
`ActedForAbsentSeat` exists to say is that *somebody else acted*, and a label that omits the actor
spends a wire break to say nothing. If a log ever ships, shortening the sentence is a copy change
that can keep the subject.

**`Your rival is still away.` for `ABSENT`.** Its case is real: it is a state, not an event, and a
line reading *did not come back* is a transition announcement left on screen for twenty hands.
Rejected because it differs from the `AWAY` line by one word while the states differ by whether the
player's next action does anything at all. A one-word difference for a paused-versus-live distinction
is a difference nobody notices at the moment they most need to.
