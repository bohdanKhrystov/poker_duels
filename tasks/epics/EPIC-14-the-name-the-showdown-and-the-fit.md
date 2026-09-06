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
sentence that has been false for the whole life of the client, and which
[`ADR-0126`](../../docs/adr/ADR-0126-the-table-shows-the-cards-and-marks-none-of-them.md) has now
settled will stay false: the table marks no card. `ADR-0012` gave a player an
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
questions had no merged answer when it opened — `DEC-129`, `DEC-130` and `DEC-133`–`DEC-139` now
have one — five of them **contradict a merged ADR**, a heavier thing than an open question and
marked as such, and one of them **opens a decision that was already open**. A second already-open
row, `DEC-108`, has since been **closed** by this epic's own answer to `DEC-135`.

| # | Item | Touches | Decides it |
| --- | --- | --- | --- |
| 1a | A player names themselves before their first duel, from a suggestion | `web-client`, `poker-server`, `design` | **Answered** — [`ADR-0119`](../../docs/adr/ADR-0119-the-name-is-asked-at-the-first-press-and-skipping-plays.md): the ask stands at the player's **own** first press, never on the invite path, and **skipping plays**. Registers **`DEC-142`**, the architect's, for the suggestion's generator |
| 1b | The account screen names the anonymous profile and owns the promotion; the post-win offer goes | `web-client`, `design` | **Answered** — [`ADR-0125`](../../docs/adr/ADR-0125-the-account-screen-names-the-anonymous-profile-and-owns-the-door.md): the offer is **retired whole**, nothing replaces it, the screen names the profile *anonymous* and it may say so only once it has been told. **Supersedes `ADR-0085`, `ADR-0086` and `ADR-0116` in full and `ADR-0036`'s offer block**; strikes `DEC-089` by deletion; registers **`DEC-147`**, the architect's |
| 1c | A name can be changed, at any time, in account settings | `poker-server`, `web-client`, `design` | **`DEC-131`** — the product owner's. **Contradicts `ADR-0029`**, whose title is *unique and **permanent*** |
| 1d | Signing out returns the browser to a **new** anonymous profile | `poker-server`, `web-client` | **`DEC-132`** — the product owner's. **Contradicts `ADR-0012`, `ADR-0027`, `ADR-0037`** |
| 1e | Duel coins carry across the promotion | — | **Nothing.** `ADR-0030` §1 already makes this true by construction — see *What is already true*. A story here **proves** it; no code is owed |
| 2a | A showdown shows the hands it reached, and stands long enough to read | `web-client`, `design` | **Answered** — [`ADR-0120`](../../docs/adr/ADR-0120-a-showdown-shows-the-hands-the-rules-showed-and-the-beat-that-shows-them-stands.md): what is shown is what the rules showed (`ADR-0008` whole), and the **last beat stands 2 s**. `poker-server` is no longer touched. The hold's mechanism is **`DEC-146`**, the architect's |
| 2b | The winning five is marked, in hand and on the board | `web-client`, `design` | **Answered — no.** [`ADR-0126`](../../docs/adr/ADR-0126-the-table-shows-the-cards-and-marks-none-of-them.md): **the table shows the cards and marks none of them.** `ADR-0095` §3 is applied to a statement made without words, not reopened; the five stay inside `poker-engine`, so **`poker-server` leaves this item** and no wire moves. What is left is one gate and one line on `STORY-1411`'s card |
| 2c | The pot travels to the winner | `web-client`, `design` | **A card.** `ADR-0115` already governs it — motion carries no fact, reduced motion stills it — and `ADR-0102` owns the pacing |
| 3a | The front door: the button is renamed, the wordmark is uncovered, the code field takes a link, the strip goes, `Account` grows | `web-client`, `design` | **A card.** The strip is answered — `ADR-0125` §5 makes `ProfileStrip`'s `no-profile` branch render nothing, so `No profile yet.` goes and the `profile` branch stays. The rename is answered above; the overlap is a defect |
| 3b | The waiting table: a copy button, the auto-start sentence, no `You` plate | `web-client`, `design` | **A card.** `ADR-0110` moved these surfaces and `ADR-0073`'s two promises stay verbatim |
| 3c | The table: the timebank figure, the disabled bar, the pot's chip pile | `web-client`, `design` | **Answered for the bar** — [`ADR-0127`](../../docs/adr/ADR-0127-a-control-stands-only-for-a-decision-the-server-has-opened.md): **no**, a control stands only for a decision the server has opened, so the `off` state keeps its box, its reserved rows and one still sentence. The human's *"show disabled controllers"* is recorded verbatim and **not followed**, and §7 makes the overrule two sentences. **`STORY-1406` is retired, not split**, and the item shrinks to its other two seams; the same ADR strikes `DEC-108`. The figure is a **defect** with a known cause and decides nothing |
| 3d | The table fits the phone — measured on the device, not on a headless viewport | `web-client`, `design`, `docs` | **Answered.** `DEC-136` → [`ADR-0121`](../../docs/adr/ADR-0121-the-table-reflows-never-scales-and-the-shape-is-measured-on-the-device.md): a **defect against `ADR-0103` §1's instrument**, not its mechanism — the reflow stays, the scale is refused, the width axis joins the contract, and the shape is device-measured |
| 4 | `Call` names what the player must add, not the total they will have in | `web-client` | **Answered.** `DEC-137` → [`ADR-0122`](../../docs/adr/ADR-0122-call-names-the-price-and-raise-to-names-the-total.md): the button prints the **price**, the other three verbs keep their totals, and the last-act mark goes bare on `Call`. No wire, so item 4 does not serialise against item 2b |
| 5 | A rematch offer reaches the rival wherever they are, as a surface they cannot miss | `web-client`, `design` | **Answered** — [`ADR-0123`](../../docs/adr/ADR-0123-a-standing-rematch-offer-follows-the-rival.md) settles `DEC-138`: the offer follows onto every screen as a **panel that never takes the screen**. No server half after all — the fact already reaches every screen. The mechanism is **`DEC-144`**, the architect's |
| 6 | Pressing play again returns the host to the room they still hold | `poker-server` | **`DEC-139`** — the product owner's, and it **opened the already-open `DEC-111`**: the second room is not hypothetical, it is what the product does today. **Both answered** by [`ADR-0124`](../../docs/adr/ADR-0124-one-waiting-room-and-play-again-hands-it-back.md) — one waiting room, the second press hands back the first, no client change; what is left is the architect's `DEC-110`, widened to the `WAITING` case |

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

**Corrected by
[`ADR-0120`](../../docs/adr/ADR-0120-a-showdown-shows-the-hands-the-rules-showed-and-the-beat-that-shows-them-stands.md),
which answers `DEC-133`: the shown hand is drawn — through the snapshot, not the event, and it
stands for 600 ms.** The paragraph above is exact about `HandRevealed` and wrong about the screen.
`broadcast` computes `revealedSeats(handEvents)` into `PlayerView.of` (`Addressed.kt:52,62`), which
populates a revealed seat's `holeCards`; `DuelTable.tsx:76-78` draws the rival with
`cards={rival.holeCards}`; `Hand.tsx` turns face up every place the view carries; and
`web-client/src/e2e/duel-secrecy.test.tsx`'s *"do reach it once the reveal has arrived"* has pinned
it over recorded frames since it was written. What item 2a is missing is **time**: a hand called
down to the river carries no `StreetDealt`, so `ADR-0102` gives its ending **one 600 ms step**
before `advanceReveal` releases the queued next hand, on a rival hand `ADR-0103` §3.2 draws at 24–40
px. **No story here builds a second drawing path.**

**The engine already computes the winning five, and says in its own KDoc that this is why.**
`BestHand.kt:8` — *"[cards] exists because the client highlights the winning five at showdown, and
recomputing …"* — and `BestHand.kt:11` is `public data class BestHand(val rank: HandRank, val
cards: List<Card>)`. **Nothing outside `poker-engine` references `BestHand`**: not the server, not
the descriptors, not the client. The value exists for a consumer that was never built, and the
client may not build it locally — deciding which five cards won is a game fact, and
[`ADR-0002`](../../docs/adr/ADR-0002-server-authoritative.md) forbids a client asserting one.

**Settled by
[`ADR-0126`](../../docs/adr/ADR-0126-the-table-shows-the-cards-and-marks-none-of-them.md), which
answers `DEC-134`: the consumer is not built, and the value stays inside the engine.** The
measurement above stands; what changes is that the missing consumer is now a **decision** rather
than a gap. The table marks no card — at a showdown, at a fold, at every street, in either seat, on
the board and for both winners of a split — so no wire field carries the five, `poker-server` leaves
item 2b, and the KDoc sentence is the stale one. Correcting it is **one line and not this epic's**:
*Out of scope* below forbids opening `poker-engine`, and the ADR is the correction of record until a
standalone ticket lands.

**The loser's hand is not sent, and that is a decision rather than an oversight.**
[`ADR-0008`](../../docs/adr/ADR-0008-loser-mucks-at-showdown.md): the last aggressor shows first,
the losing hand is never revealed, a mucked hand appears in **no event** exactly as a folded hand
does, and `CardSecrecyTest` guards both with one rule. It also names the only sanctioned way to
widen this: *"a **post-hand** disclosure written by the server after the hand is settled — never a
loosening of what the engine publishes mid-hand."* **`DEC-133` is answered without widening it**
([`ADR-0120`](../../docs/adr/ADR-0120-a-showdown-shows-the-hands-the-rules-showed-and-the-beat-that-shows-them-stands.md)
§1): only the **winner** shows — `Showdown.kt:61-76`'s `revealOrder` returns the winners, one seat
or both on a split — so *you always see the hand that beat you, and you never see what you beat*.
The engine non-negotiable in `CLAUDE.md` was not in play either way, and no post-hand disclosure is
written.

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
§2's continuous reflow, which is why `DEC-136` asked both halves at once. **Answered by `ADR-0121`**:
the reflow stays, the scale is refused on `R3` and on §3's give order, the contract gains
`scrollWidth ≤ clientWidth`, and 390 × 664 becomes a stand-in a device reading corrects downward.

**The action bar already has an `off` state, and prints a sentence in it.** `ADR-0103` quotes the
merged card: *"the bar reserves both rows in its `off` state"*, so the height item 3c asks about is
already reserved. What stands there today is the string `Waiting for your rival…`
(`ActionBar.tsx:290`). The open `DEC-108` asks the adjacent question for a *paused* duel — may the
bar stay enabled — so `DEC-135` must be answered without contradicting whatever answers that.
**Both are now answered by [`ADR-0127`](../../docs/adr/ADR-0127-a-control-stands-only-for-a-decision-the-server-has-opened.md)**:
a control stands only for a decision the server has opened, so the sentence stays and the faint
treatment keeps its one shipped meaning — *Sent — waiting on the server*. The measurement that
decided it is that `PendingTurn.legalActions` is written by `case "YourTurn"` alone
(`duel-state.ts:282-292`), so outside a turn the client holds no legal actions and every verb or
figure on a faint bar would be invented or stale.

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
it is about, and a player on the lobby screen is still that connection — and the reducer proves it
carries: `RematchOffered` is accumulated into `state.rematchOffers` (`duel-state.ts:377-380`) with
no reference to which screen is showing. **`DEC-138` is answered**:
[`ADR-0123`](../../docs/adr/ADR-0123-a-standing-rematch-offer-follows-the-rival.md) makes the offer
follow the rival as a panel above the screen that never takes it.

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
[`ADR-0124`](../../docs/adr/ADR-0124-one-waiting-room-and-play-again-hands-it-back.md) answered both
on 2026-09-06, and **against** the reading this paragraph invites: the browser's half is correct as
it stands and must not change. Keeping the memory across `Back to the lobby` would rejoin the room
on the next boot and put the waiting table straight back up — `ADR-0073`'s room with no door,
restored by the fix. The return is the **server's**: a second `CreateRoom` from a player who holds
a `WAITING` seat hands back the room they hold.

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
**Retired by
[`ADR-0125`](../../docs/adr/ADR-0125-the-account-screen-names-the-anonymous-profile-and-owns-the-door.md),
which answers `DEC-130`**: the offer goes whole, `ADR-0085`, `ADR-0086` and `ADR-0116` are
superseded in full and `ADR-0036`'s offer block with them, while `ADR-0036`'s first paragraph —
*never required, anonymous play stays fully ranked, no screen gates* — stands and is re-applied.
**`DEC-089` was open** and asked how that nudge should look; it is **struck by deletion** in the
same PR, because with no nudge the verdict is the only thing on the screen.

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
- **The showdown** (items 2a–2c), now drawable against [`ADR-0120`](../../docs/adr/ADR-0120-a-showdown-shows-the-hands-the-rules-showed-and-the-beat-that-shows-them-stands.md) §§1–3 — the states
  `duel-table-states.html` does not have: hands shown,
  the winning five marked, the pot in flight, and the same three under
  `prefers-reduced-motion` per [`ADR-0115`](../../docs/adr/ADR-0115-motion-never-carries-a-fact-and-reduced-motion-stills-every-surface.md).
- **The disabled bar** (item 3c) — the `off` state drawn as controls rather than as a sentence.
- **The corrected front door and waiting table** (items 3a–3b), including the copy button, whose
  two feedback lines already exist and move whole.
- **The phone frame** (item 3d), which `ADR-0121` keeps on `ADR-0103` §2's mechanism — so it is the
  existing frame re-drawn, never a scaled one, and its `.viewport.phone` box is 390 × 664 until a
  device reading under `ADR-0121` §2 moves it down.
- **The rematch panel** (item 5) — the product's first surface that sits *above* another screen,
  and **minting** rather than composing (`ADR-0091` §3), so it is worked with the human.
  [`ADR-0123`](../../docs/adr/ADR-0123-a-standing-rematch-offer-follows-the-rival.md) §8 names the
  states it owes: the offer standing, the accepted span, *That duel room is gone.*, the screen with
  the panel dismissed, each of the first three under `prefers-reduced-motion`, and the panel over
  two screens of different shape at both widths.

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

**Nine are answered, all on 2026-09-06.** Two of them settle a door and an ending. [`ADR-0119`](../../docs/adr/ADR-0119-the-name-is-asked-at-the-first-press-and-skipping-plays.md)
answers `DEC-129` — the name is asked at the player's own first press, the suggestion promises
nothing, and skipping plays — and
[`ADR-0120`](../../docs/adr/ADR-0120-a-showdown-shows-the-hands-the-rules-showed-and-the-beat-that-shows-them-stands.md)
answers `DEC-133` — a showdown shows the hands the rules showed, and the beat that shows them
stands. Neither supersedes anything.
[`ADR-0125`](../../docs/adr/ADR-0125-the-account-screen-names-the-anonymous-profile-and-owns-the-door.md)
answers `DEC-130` — the account screen names the anonymous profile and it is the only door to a
password — and it is the first ADR in this repository that **does** supersede: `ADR-0085`,
`ADR-0086` and `ADR-0116` in full, plus `ADR-0036`'s offer block, each named clause by clause. It
also **strikes the open `DEC-089`** by deletion.
[`ADR-0126`](../../docs/adr/ADR-0126-the-table-shows-the-cards-and-marks-none-of-them.md)
answers `DEC-134` — **no**: the table shows the cards and marks none of them, and it **registers
none** — a *no* has no wire question, so item 2's only possible wire move is gone and `ADR-0047`'s
lock is free.
[`ADR-0127`](../../docs/adr/ADR-0127-a-control-stands-only-for-a-decision-the-server-has-opened.md)
answers `DEC-135` — a control stands only for a decision the server has opened, so the bar's `off`
state keeps its sentence. `ADR-0127` registers none, and **strikes `DEC-108`**, which was open
outside this epic.
[`ADR-0123`](../../docs/adr/ADR-0123-a-standing-rematch-offer-follows-the-rival.md)
answers `DEC-138` — a standing rematch offer follows the rival, and it never takes the screen —
and registers **`DEC-144`**, the architect's, the cross-screen surface's mechanism, in its place.
[`ADR-0124`](../../docs/adr/ADR-0124-one-waiting-room-and-play-again-hands-it-back.md)
answers `DEC-139` — a player holds one waiting room, and pressing play again hands it back; it
**strikes `DEC-111`**, widens the architect's `DEC-110` to the `WAITING` case, and registers
**`DEC-145`** for the one route left into two duels at once. `DEC-137` is [`ADR-0122`](../../docs/adr/ADR-0122-call-names-the-price-and-raise-to-names-the-total.md)'s.
Every answered row has left the table below and stands under *Answered*; in their place the table
carries four of **the architect's**, registered by those answers — `DEC-142` for the suggestion's
generator, `DEC-146` for the beat's length, `DEC-144` for the cross-screen surface and `DEC-147`
for how the client learns a profile holds no credential. `ADR-0124`'s **`DEC-145`**, the product
owner's, blocks nothing here and stands in the registers rather than in the table below. The phone
is set out in full because it changes what a later ticket is measured against:

**`DEC-136` → [`ADR-0121`](../../docs/adr/ADR-0121-the-table-reflows-never-scales-and-the-shape-is-measured-on-the-device.md)
on 2026-09-06 — the table reflows, never scales, and the judged shape is measured on the device.**
It is a **defect against `ADR-0103` §1's instrument**, not a reason to change its mechanism, so
`ADR-0103` §§2–5 stand byte-unchanged and no clause is superseded. A uniform scale is still *one*
table and was the product owner's to pick, but it is refused on **`R3`** and on §3's give order —
the numbers give last, a transform shrinks them first, and `ADR-0106` §1 already rules that the
property governs the instrument. The contract gains its width twin (`scrollWidth ≤ clientWidth`)
and adds **no criterion**, so the photographed plates are `R3` `not met` today and three of the
human's four annotations were never blocked at all. The fit is measured at the **smallest viewport
the browser presents** (iOS Safari, bars fully expanded), which needs no listener; **390 × 664
stays** as `ADR-0096` §4's stand-in for a device, correctable **downward** by a hand-checked device
reading and raisable by nobody. `STORY-1405` is startable, and the **device reading comes before any
repair is measured** — nothing else establishes the shape the repair is aimed at. Named as the
human's: relaxing `R3` for the scale, raising the judged shape, and any list of devices the product
commits to.

| ID | Question | Whose | What it blocks |
| --- | --- | --- | --- |
| `DEC-131` | May a display name be **changed after it is set**, and what happens to the one given up — is it released for anyone to take, held, or retired? `ADR-0029` is titled *unique and **permanent***, `ADR-0038` can take a name away, `ADR-0051` registers one before it is held, and the leaderboard, the duel record and every finished duel print names that would now be able to move | The product owner's | Item 1c |
| `DEC-132` | Does **signing out abandon the anonymous profile and issue a new one**? `ADR-0012` binds a profile to a device id, `ADR-0027` puts the session above it, and `ADR-0037` calls the device a credential *until revoked*. A browser that gets a fresh profile at every sign-out is a **new** way to lose coins — the one thing the offer this epic **has** deleted existed to prevent — `ADR-0125` answered `DEC-130` first, so this decision is now written against a product that volunteers no warning about a browser-bound coin at all | The product owner's | Item 1d, and the shape of item 1e's proof |
| `DEC-142` | By what mechanism is a display-name **suggestion** produced — what generates the string, does the generator consult `name_registry` before offering it, and does the suggestion cross the wire at all? Registered 2026-09-06 by [`ADR-0119`](../../docs/adr/ADR-0119-the-name-is-asked-at-the-first-press-and-skipping-plays.md) §4, which fixes the product rules it must satisfy and chooses no generator. `ADR-0029` §5's refusal of an availability-check endpoint is applied, not reopened | The architect's | Item 1a's suggestion, and `STORY-1407`'s split |
| `DEC-144` | **By what mechanism does a surface render above every chosen screen, and where does its dismissal live?** Registered open 2026-09-06 by [`ADR-0123`](../../docs/adr/ADR-0123-a-standing-rematch-offer-follows-the-rival.md) §9, which fixes what a player sees and writes no repair. `Lobby.tsx` returns early per screen, so nothing in the shipped tree renders **across** screens at all: open are where the panel is mounted, how it meets `ADR-0114`'s `shown`/`ruling` and the layout-effect restore, where a dismissal lives so it survives a screen change but **not** a reload, whether the panel and `RematchControl` are one component or two, and how it is announced without **taking focus**. It may not move the wire, add a stored key, or change what `ADR-0123` §§1–7 say a player sees | The architect's | `STORY-1415`'s implementing ticket, not its card |
| `DEC-146` | By what mechanism does the client's **step queue give one beat a length different from a step**? [`ADR-0120`](../../docs/adr/ADR-0120-a-showdown-shows-the-hands-the-rules-showed-and-the-beat-that-shows-them-stands.md) §3 fixes the product half — a showdown's last beat stands **2,000 ms**, every other ending keeps **600 ms** — while `ADR-0102` §4 pins *a step is 600 ms, named once, at the boot seam*, reaching the store as a single parameter, and `drive-duel.tsx` boots it at `0` so `ADR-0100` §3's recorded-frame suites are neither edited nor re-recorded | The architect's | `STORY-1411`'s hold, and nothing else |
| `DEC-147` | By what means does the client learn that the profile it holds carries **no credential**? Registered by [`ADR-0125`](../../docs/adr/ADR-0125-the-account-screen-names-the-anonymous-profile-and-owns-the-door.md) §4, which rules that the account screen may call a profile *anonymous* only when it has been told and never from the absence of a session token. `GET /api/me` carries no such field, and `signedIn` is a false positive for anonymity in two reachable states — a `201` sign-up whose follow-up sign-in failed, and a player who signed out in this browser. `ADR-0125` chooses no mechanism and moves no wire | The architect's | Item 1b's anonymous block only — the offer's deletion and the strip's empty state wait on nothing |

### Answered

| ID | Answered by | What it means here |
| --- | --- | --- |
| `DEC-138` | [`ADR-0123`](../../docs/adr/ADR-0123-a-standing-rematch-offer-follows-the-rival.md) | **A standing rematch offer follows the rival, and it never takes the screen.** An offer **from the rival** follows a player onto every screen they can be on while they still hold the room — every `Screen` but `first`, **no carve-out**, mailed screens included — while a player who **left** the room (`forgetRoom()`, the page load behind *Back to the lobby*) is followed nowhere. The surface is a **panel above the screen and never a modal**: nothing beneath it is scrimmed or disabled, it takes no focus, **no keypress the player was already making can answer it**, it makes no sound, and it never retires itself on a timer — *cannot miss* is bought by standing there, not by blocking. Only an **incoming** offer follows, and only where the room's own screen is not showing, so one fact never has two live surfaces. **No string is minted**, and **dismissal hides the surface, never the offer** — nothing is sent, nothing is stored, and a reload brings the panel back on the offer the server restates (`ADR-0044` §5). Amends nothing: no wire, no `PROTOCOL_VERSION`, no server file, no stored key, and `ADR-0112` §2's *no notice, no dialog* stands untouched. For item 5: **`poker-server` leaves it**, the card is **minting** work with the human (`ADR-0091` §3, `ADR-0123` §8), and the mechanism is registered as **`DEC-144`**, the architect's |
| `DEC-139` | [ADR-0124](../../docs/adr/ADR-0124-one-waiting-room-and-play-again-hands-it-back.md) | **Yes — and the server is what returns them.** While a player holds a `WAITING` seat, a `CreateRoom` from them **opens no second room**: they are handed back the one they hold, same code, same seat, room left exactly as found, and **nothing is refused** — which is what keeps `ADR-0073` §3's promise true instead of breaking it, *"…and it brings you back"* now twice over. **The browser goes on forgetting** (`ADR-0072` §3 byte-unchanged, **no client change at all**): a remembered code is what boot rejoins on, so keeping it across `Back to the lobby` would put the waiting table straight back up and restore `ADR-0073`'s room with no door; the seat is the server's fact; and the server can answer a device that has never heard of the room. **Nothing is said and no string is added** — `ADR-0110` §6's enumeration untouched, on *"Dark, quiet, fast, minimal."* — with the observation that would reopen it stated. The ten minutes still runs from `Room.open` and a return does **not** restart it. Item 6's story is now unblocked on the product side and waits on the architect's `DEC-110` |
| `DEC-111` | [ADR-0124](../../docs/adr/ADR-0124-one-waiting-room-and-play-again-hands-it-back.md) | **No — one waiting room per player**, derived from `docs/vision.md`'s *"Two people, **one link**, one heads-up poker match, one winner"* with *"Not a multi-table poker room"* and *"A counter of duels won."* Answered **by construction**: a `WAITING` seat has exactly one source (`Room.open` is its only constructor, `RoomRegistry.create` has one production call site, no transition returns a room to `WAITING`), so what a second `CreateRoom` does *is* how many waiting rooms a player may hold — and it hands back the first. Amends `ADR-0105` §2's `WAITING` row only. **The harm is narrowed, not closed**: `DEC-145` registers the one route left into two duels at once — a holder of a waiting room taking a seat in **another** room by invite |
| `DEC-130` | [`ADR-0125`](../../docs/adr/ADR-0125-the-account-screen-names-the-anonymous-profile-and-owns-the-door.md) | **The account screen names the anonymous profile, and it is the only door to a password.** *That* the post-win offer goes is **the human's, recorded verbatim** — the whole block on `edits4.png` struck through with one word, *remove* — and §1 retires it whole: the surface, its strings, `offerAccount`, `DuelResult`'s `offer` prop, `pd.accountOfferSettled`, its module and its row in the one-module gate, in one diff. **Nothing replaces it** (§2): no banner, no badge, no reminder on any screen, and the standing door is the `Account` control the front door already renders unconditionally — so a win is **two presses** from the password form, refused deliberately because a pointer on the result screen is the struck block in a smaller font. **§3** names the profile *anonymous* and says three things — what it is, that it lives in this browser, and that a password keeps **every coin and every duel** (`ADR-0030` §1, which is item 1e's *sentence on a screen*) — words to the card, inside three refusals: **not a tier**, it states rather than urges, and the heading stays `Account`. **§4**: the screen asserts the state only when it has been **told**, never from a missing session token — which registers **`DEC-147`** and holds §3's block until that lands, while §§1, 2, 5 and 6 wait on nothing. **§5**: `ProfileStrip`'s `no-profile` branch renders nothing, so `No profile yet.` leaves the product and the `profile` branch is untouched. **§6**: *anonymous* is a state, never a name — `ADR-0058` stands, and a player may be **named and anonymous**. **Supersedes** `ADR-0085`, `ADR-0086` and `ADR-0116` in full and `ADR-0036`'s offer block; `ADR-0036`'s *never required* first paragraph stands and is re-applied. **Strikes `DEC-089`** by deletion. The cost is named, not argued away: this is the *"optional forever, with no prompt"* shape `ADR-0036` §Alternatives rejected, and a player who never opens the account screen is never told. Leaves the credential's shape (`ADR-0041`) to the human |
| `DEC-134` | [`ADR-0126`](../../docs/adr/ADR-0126-the-table-shows-the-cards-and-marks-none-of-them.md) | **No — the table shows the cards and marks none of them.** Every card is drawn like every other card of its kind: at a showdown, at a fold, at every street, in either seat, on the board and for **both** winners of a split, the five that made the winning hand are drawn exactly as the two that did not. A mark is anything separating one drawn card from another by *what it did* rather than *what it is* — ring, border, glow, tint, shadow, lift, scale, opacity, reordering, badge, connector, a keyable class, a `data-*`, a `title`, or anything in an `aria-label` beyond `card-text.ts`'s rank and suit — and **dimming the cards that did not play is the same statement**. `ADR-0095` §3 is **applied to a statement made without words, not reopened**: a highlight prints no string, so `HAND_TALK` could never see it, but the fact is the same kind and *these five played* is one string from *two pair, kings and jacks*. Licensed by the vision's *"replayed and **analysed afterwards**"*, dated **v0.4**; the human's annotation is the report, not the ruling. For this epic: **`poker-server` leaves item 2b**, there is no wire move, no `PROTOCOL_VERSION` step and no `atomic:` ticket, so **item 2 is client and design end to end** and `ADR-0047`'s lock stays free for `DEC-137`. `BestHand.cards` keeps its value and loses its stated reason — the KDoc is the stale sentence, its one-line repair is **not this epic's** (*Out of scope*: nothing here opens `poker-engine`). `STORY-1412` becomes **one gate** (*the drawing of a card does not depend on whether it played*) plus one line on `STORY-1411`'s showdown card, and **`STORY-1413` waits on nothing**. Costs named: the player who cannot read seven cards is charged a second time, something the human asked for in writing is declined, the deferred field gets dearer, and a gate must be written to hold a nothing. Reversal keeps `ADR-0095` §6's trigger and must settle **the mark and the name together** |
| `DEC-135` | [`ADR-0127`](../../docs/adr/ADR-0127-a-control-stands-only-for-a-decision-the-server-has-opened.md) | **No — a control stands only for a decision the server has opened.** When it is not the player's turn the bar keeps its box, keeps both rows reserved and holds **one still sentence**; it draws no button, sizing chip or amount field outside a turn — not live, not faint, not outlined, not as a placeholder. Measured: `PendingTurn.legalActions` is written by `case "YourTurn"` alone (`duel-state.ts:282-292`) and cleared by the next `Snapshot`, so the client holds **no legal actions outside a turn**; heads-up the coming decision is `{CHECK, BET}` or `{FOLD, CALL, RAISE}` and the client cannot know which, which makes **verbs-without-amounts the bigger claim, not the smaller one**. The faint treatment keeps its one shipped meaning — the card's *Sent — waiting on the server* — so two facts are never drawn alike. **The human's *"show disabled controllers"* is recorded verbatim and not followed**; §7 makes the overrule two sentences (which action shape, and what stands where an amount goes). For item 3c: **`STORY-1406` is retired rather than split** — its title states the opposite of the decision — and the item shrinks to the timebank figure and the pot's chip pile. Decides nothing about the sentence's *words* (whether the bar names the rival follows `ADR-0119`'s naming work), about pre-actions, or about the waiting-room screen, whose string sits on an empty seat plate and not in a bar |
| `DEC-108` | [`ADR-0127`](../../docs/adr/ADR-0127-a-control-stands-only-for-a-decision-the-server-has-opened.md) §4 | **Struck by this epic's own answer, on both halves.** *May* wait *and* you may act *be sayable at one moment?* **No.** *Which gives way?* **The notice, never the bar.** It was raised outside this epic by `STORY-1214` and is closed here because `DEC-135` could not be answered without settling what an unpressable bar means. No ticket falls out: `ADR-0113` §7 already deleted the pause, `TASK-130911` removed the line that named it, and `DUEL_PAUSED` is verified absent from every `.kt`/`.ts`/`.tsx` and from `ProtocolError` |
| `DEC-137` | [`ADR-0122`](../../docs/adr/ADR-0122-call-names-the-price-and-raise-to-names-the-total.md) | **The button names the price.** `callTo − committedThisStreet` — `ADR-0101` §1's `toCall` character for character, and the engine's own term recovered rather than a number manufactured. The human's frame reads `Call 200`. `Bet`, `Raise to` and `All in` keep totals; `ADR-0109`'s mark **goes bare on `Call`**, amending its §2 in one clause, because `PlayerCalled` carries only `to` and the price is unrecoverable a tick later. **No architect `DEC`** — `ActionBar.tsx:255` already computes the difference for the sizing base, so nothing moves the wire and `STORY-1414` waits on nothing. `duel-table-states.html:298`'s `Call 800` is the one card node in arrears, and `ActionBar.test.tsx`'s seventeen zero-commitment fixtures are why the repair needs a hand where the seats' commitments differ |
| `DEC-129` | [`ADR-0119`](../../docs/adr/ADR-0119-the-name-is-asked-at-the-first-press-and-skipping-plays.md) | **The name is asked at the player's own first press**, and skipping plays. A player holding no name meets the ask at their own press of *Create a duel room* or *Join the duel* — never on the invite path, never before a rematch, never on a resume — because the vision's own success condition is about the **invited** player, and she presses nothing: `main.tsx` reads the code from the URL and `boot.ts` sends `JoinRoom` on `Welcome`, outside React. The suggestion is the field's initial value and **promises nothing about availability** (`ADR-0029` §1 makes freedom a database fact and §5 refuses the endpoint that would answer it), and **nothing is written unless the player takes it** — `ADR-0051` §1 lets no string leave `name_registry`, so an auto-accepted suggestion would burn one name per browser that reached the screen. Deliberately **not a modal**: `DEC-138` owns whether this product gets one. Two costs named rather than hidden — the invited rival is not asked before her first duel, and the host meets one interstitial in a product whose positioning sentence is *fast*. For item 1a: the ask is a screen in place of the front door, and `STORY-1407` waits now only on `DEC-142`, the generator |
| `DEC-133` | [`ADR-0120`](../../docs/adr/ADR-0120-a-showdown-shows-the-hands-the-rules-showed-and-the-beat-that-shows-them-stands.md) | **A showdown shows the hands the rules showed, and the beat that shows them stands.** What is shown is unchanged — the winner's hand, both on a split, no hole card on a fold — with **no post-hand disclosure**, `ADR-0008` whole, **no wire move**, and `poker-server` out of item 2a altogether. What changes is the **pacing**: the last beat of a hand that turned a hand face up the viewer had not seen stands **2,000 ms**, every other ending keeping `ADR-0102` §4's 600 ms, the hold ending when the next hand's frames are applied and never on a fade. No panel, no overlay, **no new string**, no *mucked* line; faces come from the snapshot, never from `HandRevealed`. It registers **`DEC-146`** for the architect, corrects this epic's *"drawn by nothing"* measurement, and decides nothing about `DEC-134`, item 2c or `DEC-136` |

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
- **The suggestion's source** — now registered as **`DEC-142`** by `ADR-0119` §4. A suggested name must be free at the moment it is
  suggested and may not be free at the moment it is taken, which is why `ADR-0119` §4 promises nothing about
  one and forbids writing one the player has not taken.
  `ADR-0049` says a device binding is a row and revoking is **final**, and what `ADR-0050`'s *signs
  out everywhere but here* means for a browser that is about to be somebody else.
- **~~The winning five's path~~ — struck. Item 2 hands the architect nothing, and this epic
  contains no wire move at all.** `DEC-133` is answered by
  [`ADR-0120`](../../docs/adr/ADR-0120-a-showdown-shows-the-hands-the-rules-showed-and-the-beat-that-shows-them-stands.md),
  which moves no wire and hands over `DEC-146` instead — one beat, a different length — and
  `DEC-134` is answered **no** by
  [`ADR-0126`](../../docs/adr/ADR-0126-the-table-shows-the-cards-and-marks-none-of-them.md), which
  moves no wire either: `BestHand` reaches nothing, by decision rather than by omission. Nothing
  here claims `ADR-0047`'s lock, so **`DEC-137`'s answer may take it unopposed** if the price turns
  out to be a field.
- **The suggestion's source** (`DEC-129`) — a suggested name must be free at the moment it is
  suggested and may not be free at the moment it is taken.
- **The price's source** — asked, and answered *nobody's*: `ADR-0122` found that `ActionBar.tsx:255` already computes `callTo − committedThisStreet` for the sizing base under `ADR-0101` §1, so printing it adds no field and licenses no new derivation. The mechanism this epic forecast for item 4 does not exist.
- **The offer's surface** (`DEC-138`) — **now registered as `DEC-144`** and no longer a forecast:
  `ADR-0104` already puts the frame on the connection, and the reducer already accumulates
  `rematchOffers` on every screen, so nothing was owed on delivery. What `ADR-0123` handed over is
  narrower and certain — how a panel renders *above* a tree whose every branch returns early.

## Stories

**Not yet split.** The planner writes these once the decisions they wait on are merged; the seams
below are the split this epic expects, not stories that exist. The first ticket of every story that
puts a new surface in front of a player is its **design card** (`ADR-0091` §2), and the card merges
before the ticket that implements it is startable.

**The order is one chain.** Item 3's defects go first — they are decision-free, they are what the
human sees on every subsequent screenshot, and the timebank figure is a one-line correction with a
test. The account items follow their decisions in the order `DEC-129` → `DEC-130` → `DEC-131` →
`DEC-132`, because each later one is written against the screen the earlier one leaves behind. The
showdown was ordered last because it was the only item that could move the wire — but `ADR-0120` and
`ADR-0126` answered both of its decisions without moving it, so **item 2 serialises against nothing**.
Item 4 was expected to share `ADR-0047`'s lock and does not: `ADR-0122` moves no wire either, so the
lock stands unclaimed by this epic.

Items 4, 5 and 6 sit outside that chain: item 4 is one label, item 5 is a surface this product has
never had, and item 6 is one guard on the server — `ADR-0124` §2 leaves the browser's memory
byte-unchanged and writes no client code. Each waits only on its own decision.

| Seam | Title | Waits on |
| --- | --- | --- |
| `STORY-1401` | The timebank figure is whole seconds at every seat — *item 3c's defect; one call site, one test* | nothing |
| `STORY-1402` | The front door stops covering its own wordmark, and `Play duel` says what it does — *item 3a's overlap, spacing and rename* | nothing |
| `STORY-1403` | The room code field takes the link as well as the code — *item 3a; `roomCodeFromSearch` reused at a field* | nothing |
| `STORY-1404` | The waiting table gains a copy button and the sentence that says the duel starts itself — *item 3b* | nothing |
| `STORY-1405` | The table fits the phone it is played on — *item 3d; `ADR-0103` re-measured on the device, under `ADR-0121`* | nothing — `DEC-136` → `ADR-0121` |
| `STORY-1406` | The bar stands disabled where it printed a sentence — *item 3c* | `DEC-135` |
| `STORY-1407` | A player names themselves before their first duel — *item 1a; the product's first dialog, and by [`ADR-0119`](../../docs/adr/ADR-0119-the-name-is-asked-at-the-first-press-and-skipping-plays.md) §1 a screen rather than a modal* | `DEC-142`, and nothing else — `DEC-129` is answered |
| `STORY-1408` | The account screen says what an anonymous account is, and offers the promotion — *item 1b; the post-win offer and the profile strip's line go with it* | `DEC-130` |
| `STORY-1409` | A name can be changed in account settings — *item 1c* | `DEC-131` |
| `STORY-1410` | Signing out hands the browser a new anonymous profile — *item 1d, and the proof that a promotion moves no coin* | `DEC-132` |
| `STORY-1411` | The showdown shows the hands it reached, and stands long enough to read — *item 2a; a card, a hold, and tests that pin a drawing which already exists* | `DEC-146` |
| `STORY-1412` | The winning five is marked, in hand and on board — *item 2b; the wire move, `atomic:` under `ADR-0047`'s lock if a field is added* | `DEC-134` |
| `STORY-1413` | The pot travels to the winner — *item 2c; a card and a client story, `ADR-0115` and `ADR-0102` govern it* | `STORY-1412` |
| `STORY-1414` | `Call` says what it costs — *item 4; one label, and the mark goes bare on `Call`* | nothing — [`ADR-0122`](../../docs/adr/ADR-0122-call-names-the-price-and-raise-to-names-the-total.md) |
| `STORY-1415` | The rematch offer finds the rival wherever they are — *item 5; the product's first modal, so the card is the story's first ticket* | `DEC-138` |
| `STORY-1416` | Play again returns the host to the room they still hold — *item 6; strikes `DEC-111` with `DEC-139`* | `DEC-139` |

## Definition of done

- [ ] `DEC-129`–`DEC-139` and `DEC-146` are answered by merged ADRs, and every ADR that supersedes
      a merged one says which clause of which ADR it replaces.
- [ ] `DEC-144` — registered by `ADR-0123` §9 — is answered by a merged ADR before `STORY-1415`'s
      implementing ticket is startable.
- [x] `DEC-111` is struck by the PR that answers `DEC-139`, or is deliberately kept open with a
      reason stated in that ADR. **Struck** — `ADR-0124` answers both, and registers `DEC-145` for
      the one route left into two duels at once.
- [x] `DEC-089` is struck by the PR that answers `DEC-130`, or is deliberately kept open with a
      reason stated in that ADR. **Struck** by
      [`ADR-0125`](../../docs/adr/ADR-0125-the-account-screen-names-the-anonymous-profile-and-owns-the-door.md),
      by deletion: the nudge it asks about goes with the offer, so the verdict is the only thing on
      the result screen and no surface inherits the question.
- [ ] Every story is `done`, except `STORY-1406`, which
      [`ADR-0127`](../../docs/adr/ADR-0127-a-control-stands-only-for-a-decision-the-server-has-opened.md)
      **retired** — an answer of *no* leaves it no work, and it is never split.
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
