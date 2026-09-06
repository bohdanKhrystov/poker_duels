---
id: EPIC-14
title: The player has a name, the showdown shows its hand, the duel keeps its room, and every screen fits
type: epic
status: backlog
labels: [client, server, design, account, table]
---

## Goal

Give the player an identity they chose, make the end of a hand legible, and put every screen
inside the device it is being played on.

`EPIC-13` made the table *say what is happening*. This epic is what the human found when they
then played the product the way a player will: two devices — a laptop and an iPhone — against a
duel server on the local network, on **2026-09-06**. Six things came back — three in the first
message, three the human added the same day. A player is never
asked who they are and the profile surfaces say so in the negative (*No profile yet.*). A hand
that reaches showdown ends without ever showing what won it — no rival cards, no winning five,
and a pot that vanishes from the middle instead of arriving at a stack. And the screens
themselves: the laptop's have wrong copy, wrong spacing and one number printed to fifteen
decimal places, while the phone's table scrolls, which a merged ADR says it never does. Then the
three that came after: a `Call` button that names a total where a player wants a price, a rematch
offer that only exists on the one screen its rival may have already left, and a room that the
server keeps alive for ten minutes while the browser that made it throws the memory away.

## Why now

**The product is now played, not driven.** `EPIC-12`'s cycles drove it, `EPIC-13`'s Definition of
done drove it, and both drove Chrome on a desktop viewport. This is the first report from a real
phone over a real network, and it is the first list where every item was found by *playing* rather
than by walking a rubric. That is exactly the class of defect `ADR-0096`'s rubric cannot reach: no
criterion asks whether the player knows who they are, and none asks what a showdown looks like.

**Two of the three items are the oldest debts in the repository.** `ADR-0008` decided in
**August** what a showdown may publish, and `BestHand.kt` has carried a `cards` field ever since
whose KDoc says it exists *"because the client highlights the winning five at showdown"* — a
sentence that has been false for the whole life of the client. `ADR-0012` gave a player an
anonymous device profile on day one and `ADR-0030` made claiming it free of migrations, but
nothing has ever *named* that state to the person in it.

**And one item is a promise already merged and now contradicted.** `ADR-0103` §1 states that at
390 × 664, at every beat, the duel table's column fits: `scrollHeight ≤ clientHeight`, every
control and every number on screen unscrolled. The iPhone photograph in *The feedback* shows a
table that scrolls and elements clipped at both edges. Either the mechanism is broken or the
mechanism is wrong, and this epic is where that is settled rather than assumed.

## The feedback, verbatim

The human's message, quoted whole:

> 1)Account flow changes: First time you play dialog "choose your name" appears with
>   sugestion(before duel).This name is tied to annonymus profile which is tied to browser. When
>   you go to profile it says: "Annonymus Account"; You can "promote" your account to real by set
>   email and password; if logout you comeback to new annonumus profile;when account promoted duel
>   coins trasfered from annonymus to real profile; player name can be changes at any time in
>   accounts settings;
> 2)Show down: if we went to showdown villan card shoud be show; chips from center shoud go to
>   winner and winning combination higlited(cards + board)
> 3)On desctop i added screenshots with scrolling,spacings,copy and wrong elements issues. On
>   mobile screen was scrollable on my iphone, it shoud be adapted(details on screenshot) to make
>   all elemets visible/accesible without scrolling

Added by the human the same day, and taken as items 4, 5 and 6:

> item 4: "call" bug -  when i have call options it says full bet size(like i bet 100 -> opp raise
>   300 -> i see call 300). in this case call bnt shoud show the diff i need to add
>
> item 5: rematch flow - when rematch offered it shoud be shown as popup; it shoud appear on any
>   screen, like if opp come back to lobby it or any other page in still shoud be shown
>
> item6: room lifecycle - after i created a room same room shoud be "alive" for some time(like 3-5
>   min). for casses like: i go back to lobby and press play again. in this case i shoud be back to
>   the same room

### The screenshots, transcribed

Five annotated captures, taken against `192.168.0.142:5173` — the local-network stack of the same
evening. **The images are not in the repository**, by the human's choice: they live at
`~/Desktop/sceenshoots/` on the author's machine, and this section is the source of record. Every
annotation below is transcribed with the surface it points at; nothing is summarised away, and one
ambiguity was resolved by asking rather than by reading (marked).

**`edits1.png` — the front door.**

| Points at | Says |
| --- | --- |
| The `Create a duel room` button, which **overlaps the `Poker Duels` wordmark**, bracketed by two vertical marks | *play duel* — **the human's answer, asked and given 2026-09-06: rename the button.** The overlap is recorded here as a defect in its own right |
| The `Room code` field | *shoud also accept link* |
| The whole profile strip, reading `No profile yet.`, crossed out | *remove* |
| The `Account` nav item, boxed | *bigger font* |

**`edits2.png` — the host waiting at the table** (`ADR-0110`'s state, shipped).

| Points at | Says |
| --- | --- |
| The `Waiting for your rival` bar | *dual will start automatically when rival joins* |
| The invite-link box, holding `http://192.168.0.142:5173/?room=918RHERX` | *copy btn* |
| The host's own seat plate, reading `You`, crossed out | *remove* |

The bare code (`918RHERX`), `Back to the lobby` and *"The room stays open. That link still works
for your rival, and it brings you back."* carry no mark: they stay as they are.

**`edits3.png` — the table mid-hand** (flop, the rival to act, `Pot 1,200`).

| Points at | Says |
| --- | --- |
| A red arrow drawn **from the chip pile beside `Pot`** to the rival's seat plate, with the pile boxed | *showdown animation: chips move to the winner; villan card shown if required; winning combination higlited, winning cards + board card that make combination* |
| The player's own plate, reading `Timebank 1:58.623999999999995`, boxed | — (the box is the annotation) |
| The panel reading `Waiting for your rival…`, boxed | *show disabled controllers* |

**`edits4.png` — the result screen** (`Victory`, `+1 duel coin`, `7 hands · You 20,000 · Your
rival 0`).

| Points at | Says |
| --- | --- |
| The entire account offer — heading *Your duel coins are only in this browser*, its paragraph, `Keep them with a password` and `Not now` — crossed out | *remove* |

`Rematch` and `Back to the lobby` carry no mark.

**`mobile.jpeg` — iPhone, Safari, the table mid-hand** (preflop, `Pot 150`).

| Points at | Says |
| --- | --- |
| The rival's seat plate, clipped at the left edge | *opponent shoud fit screen* |
| The whole table area, boxed | *table area shoud be scaled to fit the screen; scroll is nececerry only if table area reach some reasonable min heigt/width* |
| The player's own seat plate, clipped at the left edge | *info bar should fit screen* |
| The action bar | *conroller shoud fit screen* |

## Scope

Six items, sixteen seams. The **Decides it** column is why this epic opens `backlog`: eleven
questions have no merged answer, five of them **contradict a merged ADR** — a heavier thing than an
open question, and marked as such — and one of them **opens a decision that was already open**.

| # | Item | Touches | Decides it |
| --- | --- | --- | --- |
| 1a | A player names themselves before their first duel, from a suggestion | `web-client`, `poker-server`, `design` | **Answered** — [`ADR-0119`](../../docs/adr/ADR-0119-the-name-is-asked-at-the-first-press-and-skipping-plays.md): the ask stands at the player's **own** first press, never on the invite path, and **skipping plays**. Registers **`DEC-142`**, the architect's, for the suggestion's generator |
| 1b | The account screen names the anonymous profile and owns the promotion; the post-win offer goes | `web-client`, `design` | **`DEC-130`** — the product owner's. **Contradicts `ADR-0036`, `ADR-0085`, `ADR-0086`, `ADR-0116`**; would moot the open `DEC-089` |
| 1c | A name can be changed, at any time, in account settings | `poker-server`, `web-client`, `design` | **`DEC-131`** — the product owner's. **Contradicts `ADR-0029`**, whose title is *unique and **permanent*** |
| 1d | Signing out returns the browser to a **new** anonymous profile | `poker-server`, `web-client` | **`DEC-132`** — the product owner's. **Contradicts `ADR-0012`, `ADR-0027`, `ADR-0037`** |
| 1e | Duel coins carry across the promotion | — | **Nothing.** `ADR-0030` §1 already makes this true by construction — see *What is already true*. A story here **proves** it; no code is owed |
| 2a | A showdown shows the hands it reached | `poker-server`, `web-client`, `design` | **`DEC-133`** — the product owner's. **Bounded by `ADR-0008`**, which forbids loosening what the engine publishes mid-hand and names the one way out |
| 2b | The winning five is marked, in hand and on the board | `poker-server`, `web-client`, `design` | **`DEC-134`** — the product owner's. **Sits against `ADR-0095`**, *the table … never names a hand* |
| 2c | The pot travels to the winner | `web-client`, `design` | **A card.** `ADR-0115` already governs it — motion carries no fact, reduced motion stills it — and `ADR-0102` owns the pacing |
| 3a | The front door: the button is renamed, the wordmark is uncovered, the code field takes a link, the strip goes, `Account` grows | `web-client`, `design` | **A card**, plus `DEC-130` for the strip. The rename is answered above; the overlap is a defect |
| 3b | The waiting table: a copy button, the auto-start sentence, no `You` plate | `web-client`, `design` | **A card.** `ADR-0110` moved these surfaces and `ADR-0073`'s two promises stay verbatim |
| 3c | The table: the timebank figure, the disabled bar, the pot's chip pile | `web-client`, `design` | **`DEC-135`** for the bar — the product owner's, adjacent to the open `DEC-108`. The figure is a **defect** with a known cause and decides nothing |
| 3d | The table fits the phone — measured on the device, not on a headless viewport | `web-client`, `design`, `docs` | **`DEC-136`** — the product owner's. **Contradicts `ADR-0103`** §1 in fact and §2 in mechanism |
| 4 | `Call` names what the player must add, not the total they will have in | `web-client`, possibly `poker-server` | **`DEC-137`** — the product owner's. `action-text.ts` refuses the arithmetic **by name**, and `ADR-0109`'s mark prints the same total |
| 5 | A rematch offer reaches the rival wherever they are, as a surface they cannot miss | `web-client`, `poker-server`, `design` | **`DEC-138`** — the product owner's. Touches `ADR-0044`, and `ADR-0112`/`ADR-0114` are what let the rival be elsewhere at all |
| 6 | Pressing play again returns the host to the room they still hold | `web-client`, `poker-server` | **`DEC-139`** — the product owner's, and it **opens the already-open `DEC-111`**: the second room is not hypothetical, it is what the product does today |

## What is already true

Measured on `develop` at `7c39fd3d` while this epic was written, so that no story re-discovers it
and no ticket is sized against a guess.

**Item 1e is already true, and the correct implementation is that nothing happens.**
[`ADR-0030`](../../docs/adr/ADR-0030-a-claim-adds-a-credential-and-moves-nothing.md) §1: claiming
is one `INSERT` of a credential row against the `player` row the request already resolves to —
*"No `player` row is created. No `duel_result` row is written, moved, copied or deleted.
`player.device_id`, `player.coin_balance`, `player.display_name` and `player.created_at` are
untouched."* The ADR says outright that *"the correct implementation of the human's 'migrate
balance and history' is that no row migrates at all"*. The promotion the human describes is the
shipped `POST /api/auth/sign-up`. **What this epic owes item 1e is a test and a sentence on a
screen, not a transfer** — and `DEC-132` is the one thing that could make it false, because a
sign-out that abandons a profile is the first mechanism in this product that would strand coins.

**The engine already reveals at a showdown, and no client file draws it.**
`StreetProgression.kt:124-133` emits `ShowdownReached` and then one `HandRevealed` per seat in
`revealOrder`; `EventRedaction.kt:13` passes `HandRevealed` to **both** seats unfiltered, because
the engine emits it only for a hand actually shown. It is on the wire —
`web-client/src/protocol/protocol.gen.ts:97,166`. Outside that generated file, **no client source
mentions `HandRevealed` at all**. The hand that is shown is sent, arrives, and is drawn by nothing.
Item 2a therefore begins as a **client** gap, and `DEC-133` decides only whether more must be sent
than is sent today.

**The engine already computes the winning five, and says in its own KDoc that this is why.**
`BestHand.kt:8` — *"[cards] exists because the client highlights the winning five at showdown, and
recomputing …"* — and `BestHand.kt:11` is `public data class BestHand(val rank: HandRank, val
cards: List<Card>)`. **Nothing outside `poker-engine` references `BestHand`**: not the server, not
the descriptors, not the client. The value exists for a consumer that was never built, and the
client may not build it locally — deciding which five cards won is a game fact, and
[`ADR-0002`](../../docs/adr/ADR-0002-server-authoritative.md) forbids a client asserting one.

**The loser's hand is not sent, and that is a decision rather than an oversight.**
[`ADR-0008`](../../docs/adr/ADR-0008-loser-mucks-at-showdown.md): the last aggressor shows first,
the losing hand is never revealed, a mucked hand appears in **no event** exactly as a folded hand
does, and `CardSecrecyTest` guards both with one rule. It also names the only sanctioned way to
widen this: *"a **post-hand** disclosure written by the server after the hand is settled — never a
loosening of what the engine publishes mid-hand."* `DEC-133` has a shape before it has an answer,
and the engine non-negotiable in `CLAUDE.md` is not in play either way.

**Chips do not travel.** `ChipPile` is three discs, always, that arrive *"on mount with the
`chip-flight` animation and then stand still"* (`ChipPile.tsx:7-16`). It is mounted in three
places, each keyed on the number beside it: the seat's stack (`SeatPlate.tsx:91`), the bet line
(`DuelTable.tsx:137`) and the pot (`PotStrip.tsx:114`). At an award the pot's pile **unmounts** and
the winner's pile **remounts**, each playing the same one-shot flight it plays everywhere else.
Nothing crosses the table. The arrow the human drew on `edits3.png` describes a motion the product
does not have.

**The timebank figure is a defect with a single, exact cause.** `bankFigure` documents
*"@param seconds Whole seconds remaining"* and floors nothing (`turn-clock.ts:35-40`). Of its two
call sites, one passes `secondsRemaining(...)`, whole; the other passes
`clock.bankRemainingMillis[seat] / 1000` raw (`turn-clock.ts:105`). `118.624 % 60` is
`58.623999999999995` — the string in the screenshot, to the digit. That call site is the
`seat !== clock.seat` branch: **the plate of the seat the clock does not name**, which is precisely
where the human photographed it, on their own plate while the rival was to act. No decision is
involved and `DEC-135` does not gate it.

**The phone fit is a merged promise, stated as a number.**
[`ADR-0103`](../../docs/adr/ADR-0103-the-table-fits-the-phone-and-the-cards-give-before-the-numbers.md)
§1: at **390 × 664**, at every beat, `document.documentElement.scrollHeight ≤ clientHeight`, and
every control and every number is on screen unscrolled — chosen as one number *"a person can read
in one `eval`"* rather than as a list of five things. §2: it is **one table at two widths**,
changing continuously with the column's own width, with nothing removed, collapsed or moved. The
photograph contradicts §1 directly, and its clipped plates contradict §2's *"nothing is removed"*
in the worst way — by removing it off the edge rather than by design. The human's own proposal —
*scale the table area, scroll only below a reasonable minimum* — is a **different mechanism** from
§2's continuous reflow, which is why `DEC-136` asks both halves at once.

**The action bar already has an `off` state, and prints a sentence in it.** `ADR-0103` quotes the
merged card: *"the bar reserves both rows in its `off` state"*, so the height item 3c asks about is
already reserved. What stands there today is the string `Waiting for your rival…`
(`ActionBar.tsx:290`). The open `DEC-108` asks the adjacent question for a *paused* duel — may the
bar stay enabled — so `DEC-135` must be answered without contradicting whatever answers that.

**The `Call` button prints the server's own total, and the client is forbidden to net it.**
`actionText` returns `{ verb: "Call", amount: actions.callTo }` (`action-text.ts:48-49`) under a
KDoc that refuses item 4's arithmetic in as many words: *"Every figure here is the server's or the
player's own: `callTo` and `allInTo` came off the wire, and `to` is what the player set on the
amount control. **Nothing is priced, netted or worked out.**"* The client is not missing the second
number — `PlayerView.committedThisStreet` is on the view and `Lobby.tsx:154` already sums both
seats' copies of it for the sizing row — it is **forbidden to subtract them**, by that KDoc and by
the `no-derivation` gates. So item 4 is not a defect in a calculation: there is no calculation, and
`DEC-137` is which number the button is allowed to name. Whatever it answers must also say what
`ADR-0109`'s last-act mark does, since that prints *"the event's own `to` total … nothing
computed"* for the same act a tick later.

**The rematch exists on exactly one screen.** `RematchControl.tsx` is rendered by `DuelResult.tsx`
and nowhere else. [`ADR-0044`](../../docs/adr/ADR-0044-a-rematch-is-one-intent-and-one-room-fact.md)
makes a rematch one intent and one room fact, which is the wire half item 5 needs and does not
change. What makes item 5 possible at all is `EPIC-13`'s own work:
[`ADR-0112`](../../docs/adr/ADR-0112-only-a-running-duel-refuses-another-screen.md) rules that only a
**running** duel refuses another screen, so a `FINISHED` room **honours** an ask for the lobby — the
rival walking away from the result screen is behaviour the product just decided to allow, and item 5
is the consequence nobody drew. Delivery is `ADR-0104`'s: a frame reaches the connection in the room
it is about, and a player on the lobby screen is still that connection.

**The room already outlives the browser's memory of it — by ten minutes.**
`RoomTimeouts.DEFAULT_WAITING_MILLIS` is `10 * 60 * 1000` (`RoomTimeouts.kt:35`), twice the top of
the human's *"like 3-5 min"*, and `ADR-0073`'s promise — *"The room stays open. That link still
works for your rival, and it brings you back."* — is already true on the server. **The half that is
not true is the tab's.**
[`ADR-0072`](../../docs/adr/ADR-0072-a-tab-remembers-its-room-until-the-player-leaves-it.md) §3 says
*exactly two* things clear `pd.roomCode`, and the first is `forgetRoom()` — *the player leaving* —
which is what `Back to the lobby` calls (`Lobby.tsx:374`). And
[`ADR-0105`](../../docs/adr/ADR-0105-one-duel-at-a-time-and-the-refusal-hands-back-the-duel.md) §2
refuses a second room only while a duel is **`PLAYING`**. Put together: a host who goes back to the
lobby and presses play again **opens a second waiting room**, while the first lives out its ten
minutes holding the code they may already have sent their rival. That is the open **`DEC-111`** —
*may one player hold more than one `WAITING` room?* — arriving as a symptom instead of as a
question, and item 6 is where it gets answered.

**The strings and fields item 3 names are one line each.** `No profile yet.` is
`ProfileStrip.tsx:35`, and what the client prints where a name would be is
[`ADR-0058`](../../docs/adr/ADR-0058-where-a-name-would-be-the-client-prints-no-name.md)'s. The
`Room code` label is `Lobby.tsx:419`. **A link is already parsed for a code** — `room-link.ts:11`'s
`roomCodeFromSearch` does it for the rival who *navigates* to the invite — so item 3a's *"shoud
also accept link"* is that function reused at a field, not a new parser.

**The account offer is four merged ADRs deep.**
[`ADR-0036`](../../docs/adr/ADR-0036-an-account-is-offered-never-required.md) (after the first
**win**, dismissible, *"dismissal is permanent"*, an offer and never a gate),
[`ADR-0085`](../../docs/adr/ADR-0085-not-again-is-this-browser-and-an-answer-spends-the-offer.md),
[`ADR-0086`](../../docs/adr/ADR-0086-the-offers-answer-is-one-key-owned-beside-the-predicate-it-feeds.md)
and [`ADR-0116`](../../docs/adr/ADR-0116-the-accept-is-a-door-and-a-door-that-does-not-open-spends-nothing.md).
`ADR-0036` also says *"no screen gates on having a credential"*, which item 1a must be read
against — a name is not a credential, but a dialog before a duel is a gate on something.
**`DEC-089` is open** and asks how that nudge should look; if `DEC-130` removes the block, the
answering ADR strikes `DEC-089` in the same PR rather than leaving it to be answered about a
surface that no longer exists.

**A display name is unique and permanent, and the guarantee is an index.**
[`ADR-0029`](../../docs/adr/ADR-0029-a-display-name-is-unique-and-permanent.md) §1: a
case-insensitive unique index under a pinned ICU collation, *"the index is the reservation"*, and
many unnamed profiles coexist because `NULL` folds to `NULL`. §2 canonicalises what is stored.
`ADR-0038` screens a name when set and can take it away; `ADR-0051` registers it before it is held.
Item 1c does not soften a convention — it asks to change a thing whose permanence is in a title,
which is why `DEC-131` is registered rather than assumed.

## Design first

[`ADR-0091`](../../docs/adr/ADR-0091-design-gets-no-agent-a-new-screen-owes-a-card.md) §2 applies to
every item here, and this epic adds the product's **first dialog** and its **first cross-table
motion**, so the card is not a formality on either:

- **The name dialog** (item 1a) — a surface no card draws, with at least: the suggestion offered,
  a name typed, a name refused as taken, and the skip `ADR-0119` §2 requires — one of two equally
  reachable ways out, both of which end in the duel the player pressed for.
- **The account screen's anonymous state** (item 1b) and **the rename control** (item 1c).
- **The showdown** (items 2a–2c) — the states `duel-table-states.html` does not have: hands shown,
  the winning five marked, the pot in flight, and the same three under
  `prefers-reduced-motion` per [`ADR-0115`](../../docs/adr/ADR-0115-motion-never-carries-a-fact-and-reduced-motion-stills-every-surface.md).
- **The disabled bar** (item 3c) — the `off` state drawn as controls rather than as a sentence.
- **The corrected front door and waiting table** (items 3a–3b), including the copy button, whose
  two feedback lines already exist and move whole.
- **The phone frame** (item 3d), which under `DEC-136` may be a card that changes the mechanism
  `ADR-0103` §2 fixed, and therefore cannot be drawn before that decision merges.

## Out of scope

- **Account deletion.** [`ADR-0039`](../../docs/adr/ADR-0039-v01-offers-no-account-deletion.md)
  declined it for v0.1. Item 1d's *new anonymous profile* will be tempting to widen into deleting
  the old one; it is a different question and nobody has asked it.
- **Replay and the analysis board.** `ADR-0008` foreclosed showing the loser's hand *in the log*,
  and `EPIC-08` owns whatever that becomes. Item 2a is about the table at the moment of the
  showdown, not about what is stored.
- **The engine.** Nothing here opens `poker-engine`. `BestHand` already exists; a reveal rule is
  `ADR-0008`'s and any widening is the **server's** post-hand disclosure by that ADR's own terms.
  A story that finds it needs the engine registers a `DEC` and does not widen.
- **`DEC-102`** — what one press of the sizing stepper moves. Item 3c touches the bar's `off`
  state and nothing else in it.
- **Reaping a `PLAYING` room** and **resigning a duel** — `ADR-0105`'s other two named costs,
  left where `EPIC-13` left them. Item 6 is about a `WAITING` room and nothing else.
- **`DEC-126`** — what serves the built bundle in a real deployment. Item 3d is measured on a
  device against the dev server, exactly as `ADR-0117` §6 already requires of the proofs of record.
- **Changing the ten-minute waiting timeout.** Item 6 asks that the player be returned to the room
  they already have; the number `RoomTimeouts` reaps against already exceeds what was asked, and
  moving it is a separate question nobody has raised.
- **A password rule change.** `ADR-0048` stands; item 1b changes where promotion is offered, not
  what it asks for.

## Open decisions

**The epic opened with eleven, all the product owner's, none answered.** Five of them ask to change
something a merged ADR decided, and one re-opens a question that has been open since 2026-09-01 —
which is why this epic opens `backlog` and why no story below is startable: an ADR is an answer only
once it is **merged**, and an ADR that supersedes another must say so in the PR that merges it.

**`DEC-129` is answered** — [`ADR-0119`](../../docs/adr/ADR-0119-the-name-is-asked-at-the-first-press-and-skipping-plays.md),
2026-09-06, superseding and amending nothing — and its row has left the table below. In its place
the table now carries **`DEC-142`**, the architect's, which that ADR registered for the suggestion's
generator.

| ID | Question | Whose | What it blocks |
| --- | --- | --- | --- |
| `DEC-130` | Does the **account screen name the anonymous state** — the human's *"Anonymous Account"* — and become the **only** door to promotion, retiring the post-win offer entirely? This asks to reverse `ADR-0036`'s central mechanism and to delete the surface `ADR-0085`, `ADR-0086` and `ADR-0116` were written about. The **open `DEC-089`** asks how that surface should look and would be mooted; the answering ADR strikes it in the same PR | The product owner's | Items 1b, 3a's profile strip and 3d's result screen |
| `DEC-131` | May a display name be **changed after it is set**, and what happens to the one given up — is it released for anyone to take, held, or retired? `ADR-0029` is titled *unique and **permanent***, `ADR-0038` can take a name away, `ADR-0051` registers one before it is held, and the leaderboard, the duel record and every finished duel print names that would now be able to move | The product owner's | Item 1c |
| `DEC-132` | Does **signing out abandon the anonymous profile and issue a new one**? `ADR-0012` binds a profile to a device id, `ADR-0027` puts the session above it, and `ADR-0037` calls the device a credential *until revoked*. A browser that gets a fresh profile at every sign-out is a **new** way to lose coins — the one thing the offer this epic may delete exists to prevent — so this decision and `DEC-130` must be answered against each other | The product owner's | Item 1d, and the shape of item 1e's proof |
| `DEC-133` | **How much of a showdown does the table show?** `ADR-0008` sends the shown hand and never the mucked one. The human asks for *"villan card"*, and the drawing says *"shown if required"*. Options run from *draw what is already sent* (no wire move at all) to a **post-hand disclosure** — which `ADR-0008` names as the only sanctioned widening, server-side, after the hand settles, never in the engine | The product owner's | Item 2a |
| `DEC-134` | May the table **mark the five cards that won** — the winner's hole cards and the board cards that complete the hand? `ADR-0095` says the table *never names a hand*; a highlight names one without words. `BestHand.cards` exists in the engine for exactly this consumer and reaches nothing today, and the client may not compute it (`ADR-0002`) | The product owner's | Item 2b |
| `DEC-135` | When it is not the player's turn, does the bar stand as **disabled controls** rather than the sentence `Waiting for your rival…`? The `off` state already reserves the height. The open **`DEC-108`** asks whether the bar may stay *enabled* while a duel is paused, and the two answers must agree about what a bar means when it cannot be pressed | The product owner's | Item 3c's bar only |
| `DEC-136` | **Does the table scale, or does it keep reflowing — and against which viewport is the fit measured?** `ADR-0103` §1 fixes 390 × 664 and §2 forbids a different table at a different width; the human proposes scaling the table area with a floor below which scrolling is allowed. iOS Safari's chrome makes the real viewport smaller and *variable*, so the number in §1 may be the reason the promise held in a headless browser and failed on the device | The product owner's | Item 3d, and item 3d's card |
| `DEC-137` | **Does the `Call` button name the total (`callTo`) or what the player must add** — the *"diff i need to add"*? A price is what a player decides against, and every poker client the human has used prints one; the total is what the server sent and what `ADR-0109`'s mark prints for the same act. Neither number is computed today, and an increment would be: `action-text.ts` states *"Nothing is priced, netted or worked out"*, so a yes either sends a second figure or licenses one subtraction against gates that currently forbid all of them. It must also rule on the mark, and on whether `Raise to` keeps saying *to* | The product owner's | Item 4 |
| `DEC-138` | **Does a rematch offer follow the rival off the result screen, and as what?** The human asks for a popup *"on any screen"*. `ADR-0044` already makes the intent and the room fact; what is unowned is the surface — this product has no modal of any kind, and `ADR-0112` §, which lets a `FINISHED` room honour an ask for the lobby, is what created the situation of an offer whose recipient is somewhere else. It decides where the offer may appear, what happens to it when it is ignored, and whether declining it is a thing a player can do | The product owner's | Item 5 |
| `DEC-139` | **Does pressing play again return the host to the room they already hold?** And with it, the question that has been open since 2026-09-01: **`DEC-111`**, *may one player hold more than one `WAITING` room at once?* Today the answer is *yes, silently* — `Back to the lobby` forgets the code (`ADR-0072` §3) while `ADR-0105` §2 refuses only a `PLAYING` room, so pressing play again opens a second room and orphans the code the player may already have sent. The server keeps the first for ten minutes, so *returning* to it needs no new lifetime — only a decision about what the browser remembers, and what a second press means | The product owner's | Item 6. **Answering it strikes `DEC-111` in the same PR**, or says why that row stays |
| `DEC-142` | By what mechanism is a display-name **suggestion** produced — what generates the string, does the generator consult `name_registry` before offering it, and does the suggestion cross the wire at all? Registered 2026-09-06 by [`ADR-0119`](../../docs/adr/ADR-0119-the-name-is-asked-at-the-first-press-and-skipping-plays.md) §4, which fixes the product rules it must satisfy and chooses no generator. `ADR-0029` §5's refusal of an availability-check endpoint is applied, not reopened | The architect's | Item 1a's suggestion, and `STORY-1407`'s split |

### What the answers will hand the architect

Named here rather than registered, following
[`ADR-0105`](../../docs/adr/ADR-0105-one-duel-at-a-time-and-the-refusal-hands-back-the-duel.md) §6 —
*a `DEC` nobody is working is noise in the open table*. These are certain if their product question
answers one way, and each is registered by the ADR that answers it:

- **The rename's mechanism** (`DEC-131`) — the unique index is the reservation, so a rename is a
  reservation released and taken under one constraint, against rows the leaderboard and the duel
  record already print. Includes whether a freed name is takeable and what `ADR-0038`'s screening
  does on the second name.
- **The new profile's mechanism** (`DEC-132`) — what a sign-out does to `player.device_id` when
  `ADR-0049` says a device binding is a row and revoking is **final**, and what `ADR-0050`'s
  *signs out everywhere but here* means for a browser that is about to be somebody else.
- **The winning five's path** (`DEC-134`) and **the reveal's** (`DEC-133`) — how `BestHand` or a
  post-hand disclosure reaches the client. Any answer that adds a field moves
  `PROTOCOL_VERSION`, which makes it an `atomic:` ticket sized by `ADR-0070`'s probe under
  `ADR-0047`'s one-bumping-branch-at-a-time lock — the only wire move this epic can contain.
- **The suggestion's source** — now registered as **`DEC-142`** by `ADR-0119` §4. A suggested name must be free at the moment it is
  suggested and may not be free at the moment it is taken, which is why `ADR-0119` §4 promises nothing about
  one and forbids writing one the player has not taken.
- **The price's source** (`DEC-137`) — if the button names an increment, is it a field the server
  sends beside `callTo`, or the one subtraction the never-derives gates are told to admit? The same
  choice `ADR-0107` faced for the pot and answered by admitting exactly one sum.
- **The offer's delivery** (`DEC-138`) — `ADR-0104` puts a frame on the connection in the room it is
  about, and the rival on the lobby screen is that connection, so the question is what the client
  does with a frame for a room whose screen it is not showing.

## Stories

**Not yet split.** The planner writes these once the decisions they wait on are merged; the seams
below are the split this epic expects, not stories that exist. The first ticket of every story that
puts a new surface in front of a player is its **design card** (`ADR-0091` §2), and the card merges
before the ticket that implements it is startable.

**The order is one chain.** Item 3's defects go first — they are decision-free, they are what the
human sees on every subsequent screenshot, and the timebank figure is a one-line correction with a
test. The account items follow their decisions in the order `DEC-129` → `DEC-130` → `DEC-131` →
`DEC-132`, because each later one is written against the screen the earlier one leaves behind. The
showdown is last of the built work because it is the only item that can move the wire, and
`ADR-0047`'s lock means it serialises against everything — unless `DEC-137` answers with a field,
in which case item 4 shares that lock and one of the two goes first.

Items 4, 5 and 6 sit outside that chain: item 4 is one label, item 5 is a surface this product has
never had, and item 6 is the browser's memory rather than the server's. Each waits only on its own
decision.

| Seam | Title | Waits on |
| --- | --- | --- |
| `STORY-1401` | The timebank figure is whole seconds at every seat — *item 3c's defect; one call site, one test* | nothing |
| `STORY-1402` | The front door stops covering its own wordmark, and `Play duel` says what it does — *item 3a's overlap, spacing and rename* | nothing |
| `STORY-1403` | The room code field takes the link as well as the code — *item 3a; `roomCodeFromSearch` reused at a field* | nothing |
| `STORY-1404` | The waiting table gains a copy button and the sentence that says the duel starts itself — *item 3b* | nothing |
| `STORY-1405` | The table fits the phone it is played on — *item 3d; `ADR-0103` re-measured on the device* | `DEC-136` |
| `STORY-1406` | The bar stands disabled where it printed a sentence — *item 3c* | `DEC-135` |
| `STORY-1407` | A player names themselves before their first duel — *item 1a; the product's first dialog, and by [`ADR-0119`](../../docs/adr/ADR-0119-the-name-is-asked-at-the-first-press-and-skipping-plays.md) §1 a screen rather than a modal* | `DEC-142`, and nothing else — `DEC-129` is answered |
| `STORY-1408` | The account screen says what an anonymous account is, and offers the promotion — *item 1b; the post-win offer and the profile strip's line go with it* | `DEC-130` |
| `STORY-1409` | A name can be changed in account settings — *item 1c* | `DEC-131` |
| `STORY-1410` | Signing out hands the browser a new anonymous profile — *item 1d, and the proof that a promotion moves no coin* | `DEC-132` |
| `STORY-1411` | The showdown shows the hands it reached — *item 2a; a drawing of frames already sent, plus whatever `DEC-133` adds* | `DEC-133` |
| `STORY-1412` | The winning five is marked, in hand and on board — *item 2b; the wire move, `atomic:` under `ADR-0047`'s lock if a field is added* | `DEC-134` |
| `STORY-1413` | The pot travels to the winner — *item 2c; a card and a client story, `ADR-0115` and `ADR-0102` govern it* | `STORY-1412` |
| `STORY-1414` | `Call` says what it costs — *item 4; one label, and whatever `DEC-137` says the mark does* | `DEC-137` |
| `STORY-1415` | The rematch offer finds the rival wherever they are — *item 5; the product's first modal, so the card is the story's first ticket* | `DEC-138` |
| `STORY-1416` | Play again returns the host to the room they still hold — *item 6; strikes `DEC-111` with `DEC-139`* | `DEC-139` |

## Definition of done

- [ ] `DEC-129`–`DEC-139` are answered by merged ADRs, and every ADR that supersedes a merged one
      says which clause of which ADR it replaces.
- [ ] `DEC-111` is struck by the PR that answers `DEC-139`, or is deliberately kept open with a
      reason stated in that ADR.
- [ ] `DEC-089` is struck by the PR that answers `DEC-130`, or is deliberately kept open with a
      reason stated in that ADR.
- [ ] Every story is `done`.
- [ ] Every surface this epic adds is drawn on a card under `design/` **before** its implementing
      ticket is startable, and each card draws every state of what it draws (`ADR-0091` §2),
      including the reduced-motion form of every motion (`ADR-0115`).
- [ ] A duel driven to a showdown on two browsers shows: both hands where the rules say they are
      shown, the five cards that won marked in hand and on board, and the pot arriving at the
      winner's stack.
- [ ] A player who has never played is asked for a name, plays under it, finds their account
      described as anonymous, promotes it with an email and a password, keeps every duel coin
      across that promotion, changes their name afterwards, signs out, and is somebody new.
- [ ] A hand is played where the two seats' commitments differ, and the `Call` button says the
      number `DEC-137` chose — read off the screen, not off a test.
- [ ] One player leaves a duel's result screen for another screen, the other offers a rematch, and
      the first is told where they are.
- [ ] A host creates a room, goes back to the lobby, presses play again, and lands in **the room
      they already had** — same code, and no second room left waiting behind them.
- [ ] The duel table is walked on a **real iPhone** over the local network and, at every beat,
      shows every control and every number without a scroll — the same claim `ADR-0103` §1 makes,
      measured where it failed rather than where it passed.
- [ ] `docs/test-plan.md` and the QA catalogue carry the cases for every item above, and
      `docs/adr/README.md`'s open table lists none of `DEC-129`–`DEC-139`.

## Metrics

Filled in when the epic closes; feeds the Product B case study.

| | |
| --- | --- |
| Tasks completed | |
| Accepted on first review | |
| Average review iterations | |
| Test lines / production lines | |
| Tasks re-scoped mid-flight | |
| Manual human edits | |
