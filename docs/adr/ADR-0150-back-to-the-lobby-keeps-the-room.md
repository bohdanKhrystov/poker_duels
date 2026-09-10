# ADR-0150 — *Back to the lobby* keeps the room, and the offer follows the player who pressed it

- **Status:** Accepted
- **Date:** 2026-09-10
- **Resolves:** `DEC-162` — **does a standing rematch offer follow a rival who presses *Back to the
  lobby*?** **Yes — and the way it does is that the press stops ending the room.** Registered
  2026-09-09 by
  [`STORY-1216`](../../tasks/stories/STORY-1216-round-1-epic-14-the-stack-served-a-tree-three-days-old.md)
  §*Found outside the round*, on a measurement of `develop` at `ea4bd05c` which found the set of
  screens a player can reach while holding a finished room to be **empty**.
- **Where the answer came from: the human's, stated 2026-09-10, recorded here and not chosen.** The
  call, in the human's own terms: ***Back to the lobby* stops ending the room** — a player who
  presses it keeps their finished room while they browse the lobby, the leaderboard and their
  account, so a rival's standing rematch offer reaches them on any of those screens. `DEC-162` named
  **two** repairs and said they are different products; **this ADR does not choose between them and
  argues for neither**. It is the human saying which reading of his own sentence stands — the
  sentence [`ADR-0123`](ADR-0123-a-standing-rematch-offer-follows-the-rival.md) quotes and then read
  the other way: *"it shoud appear on any screen, like if opp come back to lobby it or any other
  page in still shoud be shown"*. The reading recorded here is the one closest to those words: the
  sentence names *come back to lobby* as a case where the offer **is** shown.
- **The cost is the human's too, accepted with the call, and stated in Consequences:** rooms now
  outlive the result screen, so the three-to-five-minute room lifetime has to cover a player who has
  wandered off to another screen rather than only one sitting on the result. Measured, that lifetime
  is five minutes by default (`RoomTimeouts.DEFAULT_FINISHED_MILLIS`) and whatever a deployment sets
  (`ROOM_FINISHED_TIMEOUT_MILLIS`).
- **Supersedes** [`ADR-0123`](ADR-0123-a-standing-rematch-offer-follows-the-rival.md) §1's carve-out,
  on two named sentences, and replaces one premise in its §Context. Both are quoted whole in §1 and
  §Context below, with what stands beside them — §1's **rule** survives intact and this ADR leans on
  it.
- **Supersedes, on one clause,**
  [`ADR-0072`](ADR-0072-a-tab-remembers-its-room-until-the-player-leaves-it.md) §5: its wiring of the
  result screen's way back to the forget — *"`Lobby.tsx`'s `state.outcome !== null` branch passes
  `onLeave={useForgetRoom()}`'s result"* — and, **read as a statement about that one control**, §5's
  closing *"This is the only control that forgets today."* §§1–4 and 6–9 stand byte-unchanged;
  `forgetRoom` keeps its meaning, its contract and its remaining callers.
- **Touches nothing in:** [`ADR-0044`](ADR-0044-a-rematch-is-one-intent-and-one-room-fact.md) (one
  intent, one room fact, silence as the decline, `UNKNOWN_ROOM` as the frame that ends a rematch);
  [`ADR-0073`](ADR-0073-the-waiting-screen-says-back-to-the-lobby-and-the-room-stays-open.md) and
  [`ADR-0124`](ADR-0124-one-waiting-room-and-play-again-hands-it-back.md) §2 — both are about a
  **`WAITING`** room's way out, and the waiting screen's `Back to the lobby` goes on forgetting;
  [`ADR-0112`](ADR-0112-only-a-running-duel-refuses-another-screen.md) §§2–4;
  [`ADR-0114`](ADR-0114-one-predicate-answers-every-ask-and-a-mailed-screen-waits.md) §5;
  [`ADR-0123`](ADR-0123-a-standing-rematch-offer-follows-the-rival.md) §§2, 4–7 and 10.
- **Moves nothing outside the client.** No wire type, no `PROTOCOL_VERSION`, no server file, no
  engine change, no schema, no stored key, and **no new player-facing string** — every word on every
  screen is already merged.
- **Registers, and does not answer:** `DEC-163` — **the architect's** — the mechanism (§7); and
  `DEC-164` — **the architect's** — whether the finished room's five minutes still covers the window
  §1 opens (§7).

## Context

**The rule `ADR-0123` §1 wrote was true on no screen.** Measured on `develop` at `ea4bd05c` while
triaging `STORY-1216`: `RematchNotice` renders only while `shown !== "first"`
([`ADR-0138`](ADR-0138-the-panel-mounts-beside-the-lobby-and-the-dismissal-lives-in-the-mount.md)
§2); the three controls that open a chosen screen — `open("duels")`, `open("leaderboard")`,
`open("account")` — are rendered **only** in `Lobby.tsx`'s front-door branch (`Lobby.tsx:533`, `536`,
`542`); the result branch (`Lobby.tsx:340`) renders `DuelResult` and nothing else; and `DuelResult`'s
one way off the screen is an `<a href="/">` whose `onClick` is `forgetRoom` (`DuelResult.tsx:60-66`,
`Lobby.tsx:354`). A player holding a finished room could therefore be on exactly one screen, and the
panel is the one surface that never appears there. `STORY-1415`'s thirteen merged tickets ship a
surface no press can produce — and every one of their gates was green while they did it, because a
ticket's `verify:` block tests a component and no gate in this repository asks whether a press can
reach one.

**`ADR-0123` §Context rests on a premise the markup does not carry.** Its words: *"So the product now
invites a player to leave the only screen on which a rematch can be seen, and a rival who offers into
that absence is offering into nothing."* The invitation it names is `ADR-0112` §3's ruling that a
`FINISHED` room **honours** an ask for `duels`, `leaderboard`, `account` or `sign-in` — a ruling
about an ask the client offers no way to make. **What replaces the premise is this ADR:** the
invitation is created here, by the press itself, which now leads to the front door with the room
still held. `ADR-0112` §3 stops being a rule about a screen nobody can ask for.

**Two repairs, and they are different products.** Give the result screen a route to the chosen
screens, so that the walk `ADR-0123` §Context imagined becomes possible while the press goes on
ending the room; or make the press stop ending the room, so that the walk it already offers keeps the
player in it. The first is the smaller change and leaves `ADR-0072` §5 whole. The second is the
reading closest to the human's sentence, and it puts a room's life underneath a player who is no
longer looking at it.

**Whose call it is.** Neither the architect nor the product owner: the sentence being reinterpreted
is the human's own, and `ADR-0123` recorded its *whether* as *"the human's, stated verbatim"*. Only
the person whose sentence it was may say which reading stands. That is why this ADR transcribes and
does not derive — the vision licenses a rematch being reachable, but it cannot say which of two
merged readings of one message was meant.

**The deadline.** The surface is merged and unreachable **now**. Each QA round re-finds it, each
finding costs a triage, and the longer the gap stands the more likely a coder repairs it inside a
ticket, where the choice between two products would be made by whoever happened to be holding the
keyboard.

## Decision

### 1. *Back to the lobby* on the result screen stops ending this browser's hold on the room

A player who presses it **keeps the room**. The browser goes on holding it, the socket goes on
carrying frames about it, and every frame the room sends — a rival's `RematchOffered` above all —
goes on reaching this tab.

**What the press was ending, precisely.** It never told the server anything: `ADR-0072` §4 is
explicit — *"It tells the server nothing — there is no leave on the wire — and the socket that is
open keeps its seat"*. What it ended was **this browser's hold**: `forgetRoom()` cleared `pd.roomCode`
and the `<a href="/">` reloaded the tab into an empty store, so the next socket rejoined nothing.
That is what stops. **The room's own life on the server is unchanged by this ADR** — still
`FINISHED`, still reaped on `lastActivityAt` idleness after `RoomTimeouts.finishedMillis`
(`RoomRegistry.kt:841`), still ended by nothing a client sends.

**This supersedes `ADR-0123` §1's carve-out.** Two sentences go, quoted whole:

- *"**A player who has left the room is not followed, and cannot be.**"*
- *"`Back to the lobby` calls `forgetRoom()` and loads `/`; after it this browser holds no room and
  no frame about one is addressed to it."* — the control's name is italicised in the original and is
  set in ticks here so the quotation renders. Superseded **as a statement about the result screen's
  control**; it stays true of the waiting screen's control of the same name (§6).

**The rest of §1 stands, and this ADR leans on it.** Its last sentence — *"Walking to another screen
is **not** leaving — `ADR-0112` §3 keeps the seat, the tab's memory and the socket — and that
distinction is the whole rule: **the offer follows a player who is still in the room, and nobody
else.**"* — is untouched and is now what carries this decision: after this ADR a player who pressed
*Back to the lobby* **is still in the room**, so §1's rule includes them rather than excluding them.
So is §1's screen list — every member of `Screen` except `first` — to which §3 adds `first` itself,
for the case where `first` is the front door rather than the result screen.

### 2. After the press the player is on the lobby's front door, and nothing on it mentions the room

The press leaves the result screen and shows the front door — the `Play duel` control, the room-code
field, and the `Your duels`, `Leaderboard` and `Account` controls — with the finished room still
held. From there the player browses as any player does: the three chosen screens are honoured, as
`ADR-0112` §3 and `ADR-0114` §5 already rule they are.

**Nothing on the front door says a room is held.** No banner, no badge, no counter, no string. This is
`ADR-0124` §3's precedent — *"The host lands at their table with no notice, no banner, no toast and
no dialog"* — and the vision's *"Dark, quiet, fast, minimal."* The only thing the held room may put on
a screen is `ADR-0123`'s panel, and only when an offer actually stands.

**No control returns to the result screen.** The verdict, the coin line and the meta line are left
behind by the press, exactly as they are left behind today. Nothing is lost with them: the offer is
answerable from the panel's own `Rematch` control (`ADR-0123` §4), which sends the same
`OfferRematch` the result screen sends.

### 3. The offer stands on the front door too, and `ADR-0123` §3's rule moves with it

The panel appears on the lobby's front door as well as on `duels`, `leaderboard`, `account` and the
rest of §1's set. This is the human's sentence read literally: the offer reaches the player on **any**
of the screens they can be on while holding the room, and after §2 the front door is one of them.

**`ADR-0123` §3's rule is unchanged; only the test it is computed by moves.** Its rule —
*"**one fact never has two live surfaces at once**"* — stands whole: the result screen keeps
`RematchControl` exactly as merged, the panel stands wherever the result screen is not, and the two
are never on screen together. What moves is what *"where the room's own screen is not"* is measured
by: **the result screen not being shown**, rather than `shown !== "first"` (`ADR-0138` §2). Those two
were the same fact until this ADR and are not the same fact after it, because `first` may now be the
front door with a finished room standing behind it.

§3's other boundary is untouched: **only an incoming offer follows**. A player's own standing offer
follows them nowhere, on the front door as anywhere else.

### 4. What still ends the room, and what the player may do while it stands

Exactly these, and this ADR adds none of them:

- **The room's own idle reaping** — `RoomTimeouts.finishedMillis`, five minutes by default.
- **A rematch that is agreed** — the panel's `Rematch` press, matched by the rival's, seats a new
  duel and takes the screen (`ADR-0123` §6, `ADR-0112` §4). Both merged.
- **Starting another duel from the front door.** Measured: `heldRoom` names only `WAITING` and
  `PLAYING` rooms (`RoomRegistry.kt:151-155`), so `ADR-0124` §1's hand-back does not apply to a
  finished room and `Play duel` opens a fresh one; a `RoomJoined` naming a different room resets the
  store (`duel-state.ts:261-282`), which clears the outcome and the offers with it. The rival's
  standing offer is then unanswerable, and the rival is told nothing — `ADR-0044` §6's silence,
  reachable one press earlier than before.
- **Signing out**, which forgets the room code among everything else (`sign-out.ts:70`). Unchanged.

There is no *leave* on the wire, and this ADR does not add one. A player who wants a room gone before
its five minutes are up cannot ask for that; see Consequences.

### 5. The press mints no storage key

It is remembered for the life of the page it was made on. A **screen change keeps it** — the address
changes, the page does not. A **reload does not**: a reloaded tab re-acquires the room it holds, the
resume delivers the finished duel's frames and restates any standing offer (`replyToJoinRoom`,
`ADR-0044` §5), and the tab lands back on the result screen, from which one press leaves again.

This is `ADR-0123` §7's own answer to the same question about the same panel, four days old and
applied here to the press rather than to the dismissal: *"no new key, no new persistence question,
and one less thing to unpick if this shape turns out wrong."* Storing it stays one clause away.

### 6. What does not change

The wire, `PROTOCOL_VERSION`, `poker-engine`, the schema, every server file, and every word on every
screen. `forgetRoom` keeps its contract (`ADR-0072` §4) and both of its remaining callers: the
waiting screen's way back (`Lobby.tsx:439`) and sign-out (`sign-out.ts:70`). `ADR-0072` §3's *"Exactly
two things clear the memory"* stands — what changes is which control calls the first of them.

**The waiting screen is not touched.** `ADR-0073` §3's *"The control changes none of that"* and
`ADR-0124` §2's *"The browser goes on forgetting, and what returns the host is the server"* are both
about a `WAITING` room, where the forget is doing a second job — keeping the front door reachable —
that `ADR-0124` §2 names in as many words. For a **finished** room that job passes to whatever
`DEC-163` builds.

### 7. Registers, and does not answer

**`DEC-163` — the architect's — the mechanism.** Entirely *how*: where the "this player has left the
result screen" fact lives so that it survives a screen change and not a reload (§5); how
`Lobby.tsx`'s `state.outcome !== null` branch stops being the first thing a held finished room
renders, so that the front door can show while the room stands (§2); what the control becomes, given
that `ADR-0072` §5's `<a href="/">` is a full-page navigation that would undo its own effect under
§5's no-key rule; and how `RematchNotice`'s screen gate computes §3's *the result screen is not
showing* in place of `shown !== "first"` (`ADR-0138` §2), without becoming a second rule beside
`ADR-0114`'s one predicate. **Nothing in `DEC-163` may move the wire or `PROTOCOL_VERSION`, add a
stored key, or change what §§1–5 say a player sees.**

**`DEC-164` — the architect's — does the finished room's five minutes still cover the window §1
opens?** The holder may now spend the whole of it on another screen, and a wandering holder generates
no room activity: reaping is idleness on `lastActivityAt` (`RoomRegistry.kt:841`), the default is five
minutes (`RoomTimeouts.DEFAULT_FINISHED_MILLIS`), and a deployment may set its own
(`ROOM_FINISHED_TIMEOUT_MILLIS`). *Nothing changes* is a complete answer, and so is a refresh on the
holder's presence. **If the answer is a different number rather than a different mechanism, the
number is a promise to a player and comes back to the product owner.**

## Consequences

**What it buys.** `STORY-1415`'s thirteen merged tickets become reachable: the panel finally has
screens to stand on, and the rule `ADR-0123` §1 wrote becomes true of at least one player. The
vision's last beat — *"We play a full heads-up match. Someone wins. **We hit Rematch.**"* — survives a
rival who walks away from the result screen, which is what the human asked for in the message that
produced `ADR-0123`. It costs no wire, no schema, no storage, no server change and no new word, and
both players can check it with two browsers.

**What it costs.**

- **Rooms now outlive the result screen, so the three-to-five-minute room lifetime has to cover a
  player who has wandered off to another screen rather than only one sitting on the result.** This
  is the cost **the human accepted with the call**, in his own terms. Before this, a finished room's
  holder was either on the result screen watching for the offer, or gone; now they can spend the
  entire window on the ladder, the account screen or a mailed recovery form, and the room is reaped
  on idleness while they do. A rival who offers late reaches a
  panel whose `Rematch` press answers `Failure(UNKNOWN_ROOM)` and a sentence instead of a duel —
  `ADR-0044`'s *"the button can lie"*, now told to a player who has been away from the result for
  minutes rather than seconds. `DEC-164` measures whether the number still fits; this ADR does not
  change it, and until it is answered the window is what ships.
- **The client gains a state it has never had: holds a room, shows the front door.** Every branch that
  assumed *a room the tab holds is the screen the tab shows* has to be read again — that assumption is
  `ADR-0124` §2's *"the forget is what keeps the door open"*, and for a finished room this ADR takes
  that job away from the forget without saying what takes it up. `DEC-163` is that debt, and it is
  owed before a single ticket can be written.
- **One label, two behaviours.** `Back to the lobby` on the waiting screen still forgets the room
  (`WaitingTable.tsx:47-54`); on the result screen it stops doing so. Four identical words, two
  meanings — and the asymmetry is sharper than it looks, because the waiting screen says what
  happens next (*"The room stays open. That link still works for your rival, and it brings you
  back."*) while the result screen says nothing either way, before or after this ADR. Left
  deliberately: reconciling them is a decision about the waiting room, and this one is about the
  finished room.
- **A reload undoes the press** (§5). A player who left the result screen and then refreshed is back
  on it, and presses again. Bounded by the room's own five minutes, and one clause to reverse.
- **The front door is now a screen a player can be on while an offer stands, and pressing `Play duel`
  there abandons the offer without being asked.** The rival is told nothing, before or after —
  `ADR-0044` §6's silence, now reachable one press earlier (§4).
- **`ADR-0123` §1's promise is still not kept for a player who genuinely leaves** — a closed tab, a
  cleared storage, another device. Nothing follows them, and nothing in this ADR changes that.

**What it forecloses.** Very little, and nothing structurally. It does **not** build: a *leave* on the
wire, a decline, a control that ends a room deliberately, a longer room lifetime, any notice on the
front door about the room being held, or a way back to the result screen. The other repair `DEC-162`
named — a route from the result screen to the chosen screens — is not built and is **not** ruled out:
it would compose with this decision rather than contradict it, and if `DEC-163`'s mechanism turns out
expensive it is still there.

**What it deliberately leaves open.** A player has no way to say *I am done with this room* — their
only answers to a standing offer are `Not now`, which hides the panel until the offer ends, and
waiting the room out. That gap is older than this ADR (`ADR-0044` §6 chose silence over a decline)
and this ADR widens the window in which it is felt without closing it.

## Alternatives considered

**A route from the result screen to the chosen screens — the other repair `DEC-162` named.** The
strongest case in the set. It is the smaller change by a distance: `ADR-0072` §5 stays whole, the
press goes on ending the room, the client keeps its one invariant — a tab holds a room only while the
room's own screen is showing — no new client state exists to design, and `DEC-163` would not need to
exist at all. It also satisfies `ADR-0123` §1 exactly as written, which is the rule the thirteen
merged tickets were built against, so nothing shipped would need re-reading. Rejected because the
human chose the other reading of his own sentence, and this ADR does not argue with that; the product
reason he could have given is that it hangs three navigation controls on the one screen this product
keeps to a verdict, a coin line, a rematch and a way out.

**Do nothing: accept the carve-out as the whole truth and retire the panel.** Its case: it is what the
code does today, honestly; the offer is still answerable on the result screen, which is where a player
who wants a rematch is most likely to be sitting; and retiring `STORY-1415`'s surface would leave a
smaller product with no unreachable code in it. Rejected because it contradicts the human's sentence
in the plainest way available — *"if opp come back to lobby it or any other page in still shoud be
shown"* — and because it throws away thirteen merged tickets to avoid one mechanism decision.

**Keep the room on the server, but let the browser go on forgetting.** Its case: no client state at
all, no `DEC-163`, and it is the `ADR-0002`-shaped answer — which room a player holds is the server's
fact, not a browser's cache, and `ADR-0133`'s scan already finds the room a player holds without any
help from storage. Rejected on a measurement: the offer's surface reads the store, and a browser that
has forgotten the room sends no rejoin, so no `RematchOffered` is addressed to it — the panel would
have nothing to render. Making the scan name `FINISHED` rooms as well would put a finished room in
`Play duel`'s path (`RoomRegistry.kt:151-155`, `ADR-0124` §1), which is a different and worse product.

**Store the press, so the front door survives a reload.** Its case: the player's intent is
unmistakable — *I am done looking at this result* — and a refresh that drops them back on it is the
client forgetting something the player said; one key in `localStorage` is cheap and the pattern is
already in the repository. Rejected because `ADR-0123` §7 answered exactly this trade, for exactly
this panel, four days ago and on the same reasoning, and consistency between two neighbouring
behaviours is worth more than the one press a reload costs. It stays one clause away.

**Raise the finished room's five minutes now, since the wander is the whole point.** Its case: the
human accepted the wander as a *cost*, the obvious mitigation is a bigger number, and it is one
configuration value with no code behind it — `ROOM_FINISHED_TIMEOUT_MILLIS` is already read.
Rejected because nobody has measured what the current number covers, no player has hit the wall, and
a number chosen against an imagined wander is a guess that costs server memory for every finished
room. Registered as `DEC-164` instead, where it will be measured.

**Give the player a control that ends the room deliberately — *Leave this room*, or *Done*.** Its
case: once a room outlives its screen, a player has no way to stop a rival reaching them for five
minutes, and a duelling product should let you decline a duel outright rather than by waiting.
Rejected as a decision nobody has asked for: it needs either a wire message, which `ADR-0044` §6
deliberately does not have, or a client-only forget that would tell the player something false about
the server. Inventing the general *leave* from this one request is how a decision becomes a feature.
Named in Consequences as left open.
