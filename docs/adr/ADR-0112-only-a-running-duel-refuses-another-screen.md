# ADR-0112 — Only a running duel refuses another screen, and the refusal restores the address

- **Status:** Accepted
- **Date:** 2026-09-02
- **Resolves:** `DEC-119` — while a player **holds a room**, what does the **address** name, and
  may they leave the table by asking for another screen? Registered by
  [`EPIC-13`](../../tasks/epics/EPIC-13-the-living-table.md) on the human's refresh feedback of
  2026-09-02, whose reported symptom did not reproduce.
- **Where the answer came from:** derived. The split is
  [`ADR-0105`](ADR-0105-one-duel-at-a-time-and-the-refusal-hands-back-the-duel.md) §2's merged
  line — *"**Running** means `PLAYING`, and the other two states are not refused"* — applied to
  navigation instead of seating. The running half rests on `docs/vision.md`'s *What it is* —
  ***"A duel is a match, not a hand."*** — and the success condition everything is downstream of:
  *"We play a full heads-up match. Someone wins. We hit Rematch."* — while a match runs, the
  product is the match. The look-away half rests on the same list's own entries —
  ***"A leaderboard."***, ***"Replay and honest feedback."*** — which make the chosen screens
  product rather than chrome, plus two merged promises a blanket refusal already contradicts:
  [`ADR-0073`](ADR-0073-the-waiting-screen-says-back-to-the-lobby-and-the-room-stays-open.md) §3
  tells the waiting host on screen that they may walk away, and
  [`ADR-0086`](ADR-0086-the-offers-answer-is-one-key-owned-beside-the-predicate-it-feeds.md) §6's
  accept loads the account screen from a held `FINISHED` room. The refusal's silence is
  *Positioning* — *"Dark, quiet, fast, minimal."*
- **Applies, and reopens none of:**
  [`ADR-0076`](ADR-0076-a-screen-the-player-chose-has-an-address.md) §§1–2 (the address names a
  screen the player chose; the table, the wait and the result have no address, permanently) and
  §3's frame rule (a frame that seats a duel overrules the ask and the fragment is replaced);
  `ADR-0076` §6's last row (the result's `<a href="/">` and the waiting screen's *Back to the
  lobby* stay real page loads — a look-away crosses no store boundary, because the room is kept);
  `ADR-0105` §3 (a running duel is handed back — the table, never a lobby);
  [`ADR-0072`](ADR-0072-a-tab-remembers-its-room-until-the-player-leaves-it.md) (nothing here
  leaves a room: the seat, the memory and the socket are untouched by a look-away);
  [`ADR-0060`](ADR-0060-the-record-is-its-own-screen-and-the-lobby-is-the-door.md) §4 (the chosen
  screens stay ignorant of navigation and are not redesigned).
- **Qualifies** `ADR-0076` §3 by exactly one case: a held room whose duel is **not running** no
  longer overrules an ask the **player** made, so the branch order §3 fixed is re-fixed by
  `DEC-123`'s answer. Every arriving frame, and every running duel, outranks the address exactly
  as §3 wrote it.
- **Constrains:** `EPIC-13` item 8's story, and nothing else. No wire change, no
  `PROTOCOL_VERSION` move, no engine change, no schema, no new string, no new control.
- **Registers, and does not answer:** `DEC-123` — **the architect's** — the mechanism (§7).
- **Does not depend on** where a host waits. Everything below is phrased in **room states**,
  never in screens, so `DEC-116`'s answer — wherever it puts the waiting host — changes nothing
  here.

## Context

**The human asked for one thing, and it already ships.** Verbatim, 2026-09-02: *"when page is
refreshed user shoud stay on the same page (refresh duel page shoud not redirect to lobby; and
same for all other pages)"*. Measured the same day with `scripts/qa/drive.mjs` and
`location.reload()` on a localhost dev server: a host in a live duel on a bare `/`, a rival on
`?room=CODE`, a host on the waiting screen, and `#/leaderboard` on a room-free browser **all
survive a refresh**. `boot.ts` writes the room code to storage on `RoomJoined` and re-sends
`JoinRoom` on the next socket's `Welcome`; `use-screen.ts` reads `window.location.hash` at boot.
The reported symptom did not reproduce.

**What reproduced is the inverse, and it is a shipped generalization rather than a decided one.**
On a browser holding a room — waiting or playing — `location.hash = '#/leaderboard'` reads back
**empty** within 2 s and the room's screen stands, so `duels`, `leaderboard` and `account` are
unreachable mid-duel. The mechanism is one effect in `Lobby.tsx`: whenever `outcome`, `view` or
`roomCode` is non-null and the screen is not `first`, it calls `leave()`, which restores `/`. Its
comment cites `ADR-0076` §3 — but §3's scenario is a **frame arriving** over a chosen screen
(*"A player on `#/duels` whom a frame seats is shown the duel… and the client replaces the
fragment with `/` so the address does not lie about where they are"*). The effect fires
identically for the converse — a seated player **asking** — which §3 never mentions. `ADR-0105`
§3 hands a player back *"the table, never a lobby"*, which licenses **refusing** a navigation;
erasing the address is a further act; and no merged source says whether a `WAITING` or `FINISHED`
room refuses at all. That gap is this decision.

**Two merged promises already contradict a blanket refusal.**

- `ADR-0073` §3 renders, on the waiting screen: *"The room stays open. That link still works for
  your rival, and it brings you back."* A host told on screen that they may walk out of the
  product entirely is today refused a glance at the ladder inside it.
- `ADR-0086` §6's accept on the result screen is `<a href="/#/account">` — *"the anchor still
  loads the account screen, and that page load is what replaces this tree."* By construction that
  page load now boots, rejoins the remembered `FINISHED` room (`ADR-0072` keeps the memory, boot
  re-sends `JoinRoom`, resume restates the result), and the restored `outcome` re-seats the tree —
  whereupon the effect erases `#/account` and the account screen never shows. **Derived, not
  driven**: the result screen is precisely the path the measurement did not cover, which is why
  §6 owes it a reproduction rather than a belief. Either way, two merged sources collide on that
  screen, and something has to give.

**Mid-duel, the pull goes the other way, and it is about the rival.** There is no resign, no turn
clock, and a `PLAYING` room is never reaped — `ADR-0105`'s named cost, still true; the clock is
`EPIC-13` item 4 and not this decision. A connected player reading the ladder mid-hand stalls the
duel with nothing anywhere saying so, and the bill lands on a present, blameless rival — the same
wrong-bill argument `ADR-0105` used to reject pausing a duel for a player who went elsewhere. No
attention surface exists that could make mid-duel wandering honest — nothing marks a turn's
arrival on any other screen — and inventing one to license an answer would be this decision
growing a feature.

**The deadline.** `EPIC-13` cannot split item 8 without this. Its Definition of done binds the
story either way: *"The reported refresh symptom is either reproduced and fixed, or recorded as
not reproducible with the paths that were tried written down."*

## Decision

### 1. The address names the screen the player chose, and never the room

`ADR-0076` §§1–2, applied and reaffirmed. While a player holds a room, the address is `/` until
they choose a screen; the table, the wait and the result have no address and get none; no
`Screen` member is added for any of them. **The address is not expected to name a duel.** A
reload while seated lands on `/` and the frames put the player back — measured, on every path
tried — and the address never claims to. `?room=CODE` stays what `ADR-0076` §4 made it: an
instruction consumed at boot, not a route, untouched here.

### 2. While the held room's duel is running, an ask for another screen moves nothing

The duel stays on screen — `ADR-0105` §3's sentence, extended from a refused seating to a refused
navigation: the product hands the player the table, never a lobby. *Running* means `PLAYING`,
grace window included — `ADR-0105` §2's definition, borrowed whole.

**The ask is refused and the address is restored to `/`, and these are one act, not two.** An
address left reading `#/leaderboard` over a duel table would name a screen the player is not
shown — the exact lie `ADR-0076` §3 replaces the fragment to prevent. At rest, the address never
names a screen the player is not looking at; how promptly a transition settles is the mechanism's
(§7).

**The refusal is silent.** No notice, no dialog, no new string: the screen the player is looking
at is the whole answer, and `ADR-0105` §4's discipline holds — a screen that needs a new sentence
for this has outgrown this decision and owes a new ADR, not an invented line.

### 3. While the held room holds no running duel, the ask is honored

`WAITING`, `FINISHED` or `ABANDONED`: the chosen screen shows and the address names it — `duels`,
`leaderboard`, `account`, and the door-gated `sign-in` behind it, all of them. Nothing about the
room moves: the seat is kept, the tab's memory is kept — leaving stays the act of the explicit
controls `ADR-0072` gave it to — the socket stays open, so the rival never sees an absence, and
frames keep applying, so `/` is always the room's screen **as it now stands**, not as it was
left. The way back is the one every chosen screen already renders: its in-page *Back*, or the
browser's, to `/`. No new control exists and none is drawn.

Phrased in states rather than screens on purpose: `DEC-116` may move where a host waits, and this
rule does not care where.

### 4. A frame that seats a running duel overrules any chosen screen

`ADR-0076` §3's frame rule, reaffirmed as written: the rival arrives at the `WAITING` room, or a
rematch this player already offered is agreed — the player is shown the duel and the fragment is
replaced with `/`. §3's look-away is bounded by exactly this: a player may look away from a room
that is waiting; the moment a duel runs, the duel wins the screen. A standing rematch **offer**
seats nothing (`Room.offerRematch` agrees only when both seats have offered — `ADR-0105` §2) and
therefore overrules nothing: a player reading the ladder learns of it when they return, and this
ADR gives them no notice. That is a cost, named below.

### 5. The account is reachable by the same rule, and the result screen's offer works as merged

Asked by name in `DEC-119`, answered by name: **mid-duel, no** — nothing merged needs an account
mid-hand, and a match's length is exactly the commitment *"a duel is a match, not a hand"* names —
**from a `WAITING` or `FINISHED` room, yes**. In particular `ADR-0086` §6's accept lands on the
account screen it names; the derived collision in Context is resolved in `ADR-0086`'s favour, and
§6 below owes it a drive.

The mailed `verify` and `reset` screens follow the same rule, with one hard requirement: a mailed
link refused mid-duel **must not spend its token** — the same mail must work after the duel. How
the token survives the refusal is §7's.

### 6. What the implementing story owes: the reproduction attempt

`EPIC-13`'s Definition of done binds it: *"The reported refresh symptom is either reproduced and
fixed, or recorded as not reproducible with the paths that were tried written down."* The paths
not yet driven, owed before item 8 closes:

- a refresh **on the result screen** (a held `FINISHED` room, `outcome` standing);
- a refresh **during a runout** (`ADR-0102` §5 says the reload jumps to the end; confirm no lobby
  shows on the way);
- a **genuinely dropped socket** — a reconnect through `reconnecting.ts`, not a reload;
- **real latency**, where the rejoin round-trip is visible — a lobby flash localhost sampling
  could not see;
- the **`AccountOffer` accept path** of §5, whose failure is so far derived rather than observed;
- a **mailed link opened while a room is held**, in both a waiting and a playing state.

A dismissal without the attempt does not satisfy the DoD row.

### 7. Registers, and does not answer: `DEC-123` — the architect's — the mechanism

How the client serves this answer: how a chosen screen renders over a held non-running room (the
branch order `ADR-0076` §3 fixed is qualified above, and re-fixing it is design); where the
room-state read lives — the store's `roomCode`, `view` and `outcome` already form the ladder, so
no wire change is implied, but that observation is not a design; how promptly §2's refusal
restores `/` (the measured 2 s in which the address lied is a mechanism artifact, not a licence);
and how a mailed token refused mid-duel survives unspent. Nothing in `DEC-123` may move the wire
or `PROTOCOL_VERSION` on this ADR's account.

## Consequences

**What it buys.** The shipped tree stops contradicting its own promises: the waiting host's
freedom (`ADR-0073` §3) now includes the product's own screens, and the result screen's account
offer leads where it says it does (`ADR-0086` §6). Mid-duel, the product says one thing and now
says it on purpose — the duel is the screen — so the behaviour measured on a playing browser is
confirmed as intent, not repaired as a defect. The address tells the truth at rest in every
state. And the screens the roadmap builds on — the record, the ladder, the account — stop being
unreachable from the two states a player actually sits in between hands of nothing: waiting and
finished.

**What it costs.**

- **Mid-duel confinement is total, silent, and today unbounded.** No resign, no turn clock, a
  `PLAYING` room never reaped: a stalled duel confines its players away from every other screen
  indefinitely until `EPIC-13` item 4's clock lands. This ADR chooses that with eyes open rather
  than inheriting it by accident, and a player who types `#/account` mid-duel gets `/` and no
  explanation.
- **`ADR-0076` §3's *"always"* now carries a qualification that lives in a different file.** Its
  own named cost — two navigation authorities held apart by prose — grows to three: the store,
  the fragment, and now the room's state, with nothing mechanical enforcing any of it.
- **The look-away hides the room's own surfaces.** A waiting host on the ladder does not see
  their invite link; a finished player on the ladder does not see a rematch offer arrive. No
  notice exists, and none is designed here.
- **A waiting host can be yanked.** Mid-scroll on the ladder, their rival arrives, and §4 pulls
  them to the table with their chosen fragment erased. Correct — the duel they asked for
  started — and still startling, and no card draws the transition.
- **A mailed link clicked mid-duel visibly does nothing.** The token is preserved (§5), but the
  player is told nothing; if the token has a lifetime, a long duel can outlive it, and the mail
  must be requested again.
- **The reproduction obligation is real work** (§6) attached to a story that might otherwise have
  been one branch change.

**What it forecloses.** Nothing new permanently. It reaffirms `ADR-0076` §2's permanent
foreclosure — no address for a screen the server decides — and forecloses the blanket erasure
only as today's product, not as a forever-impossibility: the day a turn clock bounds every duel,
honouring mid-duel navigation becomes arguable on evidence, and it is one small ADR then. That
reversibility is why this shape wins on an afternoon of measurements: client rendering only, no
wire, no storage, no string minted, nothing a player keeps.

## Alternatives considered

**Refuse for every held room — bless the shipped effect as written.** The strongest case in the
set: one rule with no state ladder, `ADR-0076` §3's letter kept whole, zero code moved, and the
room's own surfaces — invite link, rematch offer — always in front of the player who holds them.
Rejected because it converts a shipped generalization into product intent at exactly the two
places merged promises contradict it: it breaks `ADR-0086` §6's accept on the result screen (the
anchor derivably bounces), and it makes `ADR-0073` §3 incoherent — a host told they may abandon
the wait wholesale would be refused a glance at the ladder inside the same product. It also
confines a player in states `ADR-0105` §2 already argued are *"no duel, no opponent, no coin"*.

**Honor everywhere, running duels included.** Its case: the address always obeys the player, the
freedom matches the reference products' feel, and once a turn clock exists, wandering costs the
wanderer alone. Rejected because no clock exists on `develop` today and no surface marks a turn's
arrival anywhere but the table — so a mid-duel wanderer stalls the duel silently and the bill
lands on the present rival, `ADR-0105`'s wrong-bill argument transposed. It also overturns
`ADR-0076` §3 root and branch rather than qualifying it: for the honor to mean anything, an
arriving frame could no longer pull the player to the duel they asked for. Revisitable the day a
clock bounds every duel; not before.

**Refuse, but leave the fragment standing.** Its case: the player's ask stays visible in the bar,
and a future mechanism could treat it as a pending navigation to honour later. Rejected on merged
text: the address would name a screen the player is not shown, the lie `ADR-0076` §3 replaces the
fragment to prevent and §7 already strips from unknown fragments — and a pending-ask model is a
new concept bought so that the bar may lie in the meantime.

**Honor by leaving — a navigation forgets the room, the way *Back to the lobby* does.** Its case:
one authority survives (the store), the leave semantics are already merged (`ADR-0072`), and no
branch order changes — the ask simply becomes the existing exit plus the chosen screen. Rejected
because it makes a glance destructive: a waiting host who peeked at the ladder loses the tab half
of *"it brings you back"*, and a finished player loses the rematch moment entirely — a held room's
memory destroyed by a control that looks like reading. `ADR-0073` §4 refused a confirmation on the
ground that nothing is destroyed; this would quietly make that false.

**Give the table an address, so the bar can always name where the player is.** Its case: the
complaint *"the address stops naming where the player is"* dissolves permanently, and a refresh of
`#/table` is trivially *the same page*. Rejected because `ADR-0076` §2 forecloses it
*"permanently, and by rule"*: whether this browser holds a seat is the server's answer, an address
that claimed it would be a second claim about an entitlement the server owns, and the problem it
would solve is already solved — every reload path tried survives on the frames alone. Reopening a
permanent foreclosure on no new evidence is amendment for its own sake, and it is not done here.
