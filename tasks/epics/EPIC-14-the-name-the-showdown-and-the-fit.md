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
questions had no merged answer when it opened — `DEC-129`–`DEC-139`, and **all eleven now have
one**, as do the `DEC-140` and `DEC-141` the planner raised while splitting item 3b, so **all
thirteen are answered** — five of them **contradict a merged ADR**, a heavier thing than an open
question and marked as such, and one of them **opens a decision that was already open**. A second
already-open row, `DEC-108`, has since been **closed** by this epic's own answer to `DEC-135`.
Of the follow-ups those answers registered, **all three are now answered** — `DEC-144` by
[`ADR-0138`](../../docs/adr/ADR-0138-the-panel-mounts-beside-the-lobby-and-the-dismissal-lives-in-the-mount.md),
`DEC-150` by
[`ADR-0140`](../../docs/adr/ADR-0140-the-clipboard-api-is-the-only-copy-this-client-attempts.md), and
`DEC-145`, the product owner's, on 2026-09-08 by
[`ADR-0141`](../../docs/adr/ADR-0141-taking-a-seat-elsewhere-releases-the-room-you-were-holding.md),
which registered `DEC-154` for the architect in its place. Five more were
answered on 2026-09-07 and stand under *Answered* below: `DEC-147`, by
[`ADR-0132`](../../docs/adr/ADR-0132-the-profile-says-whether-it-holds-a-password.md); the
pre-existing `DEC-110` that `ADR-0124` §6 widened, by
[`ADR-0133`](../../docs/adr/ADR-0133-the-room-a-player-holds-is-scanned-for.md); `DEC-151`, by
[`ADR-0134`](../../docs/adr/ADR-0134-a-rename-spends-before-it-replaces.md), so `STORY-1409`
waits on nothing — and whose §7 leaves the planner an ordering, the migration being
**behaviour-preserving on its own**, so the schema and the write path may land as **separate
tickets**; and `DEC-152`, by
[`ADR-0135`](../../docs/adr/ADR-0135-the-server-says-which-sign-out-this-is-and-the-browser-forgets-one-key.md),
so `STORY-1410` waits on nothing. Neither `STORY-1409`'s tickets nor `STORY-1410`'s are
`atomic:` — nothing either lands crosses the socket — but **`STORY-1408` and `STORY-1410` must
be serialised**, because `ADR-0132`'s `hasPassword` and `ADR-0135`'s device bit both edit
`docs/protocol.md`'s `/api/me` section and `web-client/src/profile/profile.ts`.

| # | Item | Touches | Decides it |
| --- | --- | --- | --- |
| 1a | A player names themselves before their first duel, from a suggestion | `web-client`, `poker-server`, `design` | **Answered** — [`ADR-0119`](../../docs/adr/ADR-0119-the-name-is-asked-at-the-first-press-and-skipping-plays.md): the ask stands at the player's **own** first press, never on the invite path, and **skipping plays**. Registers **`DEC-142`**, the architect's, for the suggestion's generator — **answered 2026-09-07** by [`ADR-0137`](../../docs/adr/ADR-0137-a-name-suggestion-is-drawn-in-the-browser.md): drawn in the browser from a bundled vocabulary, no registry consult, **nothing on the wire**, so `poker-server` is **not** touched by this item after all |
| 1b | The account screen names the anonymous profile and owns the promotion; the post-win offer goes | `web-client`, `design` | **Answered** — [`ADR-0125`](../../docs/adr/ADR-0125-the-account-screen-names-the-anonymous-profile-and-owns-the-door.md): the offer is **retired whole**, nothing replaces it, the screen names the profile *anonymous* and it may say so only once it has been told. **Supersedes `ADR-0085`, `ADR-0086` and `ADR-0116` in full and `ADR-0036`'s offer block**; strikes `DEC-089` by deletion; registered **`DEC-147`**, the architect's, which [`ADR-0132`](../../docs/adr/ADR-0132-the-profile-says-whether-it-holds-a-password.md) **answers** on 2026-09-07 — `GET /api/me` gains `hasPassword: Boolean` and the client derives nothing, so the anonymous block is startable and nothing in this item waits |
| 1c | A name can be changed, at any time, in account settings | `poker-server`, `web-client`, `design` | **Answered** — [`ADR-0130`](../../docs/adr/ADR-0130-a-name-can-be-changed-and-the-name-it-leaves-is-spent.md): **yes, and the name it leaves behind is spent.** *That* it can change is the human's, recorded verbatim; **what becomes of the string is derived** — **retired**, not released and not held. History prints the name as it **is**; one form, on the **account screen**; `PERMANENCE_LINE` leaves the product. **Supersedes `ADR-0029` §4, its title's *permanent* and §5's `403`, `ADR-0051` §3's rename clause and `ADR-0119` §3's obligation 2.** **`DEC-151`** is registered for the mechanism and **answered** by [`ADR-0134`](../../docs/adr/ADR-0134-a-rename-spends-before-it-replaces.md): the trigger becomes `player_display_name_never_released`, the write is four statements under `SELECT … FOR UPDATE`, the vacated row moves `TAKEN → REPLACED` with `retired_from` set, `403` leaves and `429` arrives, and the write is budgeted. **This item is unblocked on both halves.** |
| 1d | Signing out returns the browser to a **new** anonymous profile | `web-client` **and** `poker-server` — [`ADR-0135`](../../docs/adr/ADR-0135-the-server-says-which-sign-out-this-is-and-the-browser-forgets-one-key.md) adds one route and one comparison; no schema, no wire | **Answered** — [`ADR-0131`](../../docs/adr/ADR-0131-signing-out-of-your-own-account-hands-the-browser-a-new-profile.md): **yes when the account being left is the profile this browser owns, no when it is somebody else's.** The three ADRs this item was filed against are **not** contradicted after all — a browser handed a new profile is still a profile bound to a device (`ADR-0012`), precedence is untouched (`ADR-0027`), and the device stays a route until revoked (`ADR-0037`). What is superseded is **`ADR-0030`** §3's outcome clause and §8's *not on sign-out*, for that one case. Registers **`DEC-152`**, the architect's — **answered 2026-09-07 by [`ADR-0135`](../../docs/adr/ADR-0135-the-server-says-which-sign-out-this-is-and-the-browser-forgets-one-key.md)**: a new session-required `GET /api/me/device` answers `signOutHandsANewProfile`, `true` exactly when the `X-Device-Id` presented on that same request does not resolve, through a live binding, to a player other than the caller; `signOut` forgets `pd.deviceId` on `true` and on nothing else; the new profile comes from the next `Hello`; and **no wire, schema or `PROTOCOL_VERSION` moves** |
| 1e | Duel coins carry across the promotion | — | **Nothing.** `ADR-0030` §1 already makes this true by construction — see *What is already true*. A story here **proves** it; no code is owed |
| 2a | A showdown shows the hands it reached, and stands long enough to read | `web-client`, `design` | **Answered** — [`ADR-0120`](../../docs/adr/ADR-0120-a-showdown-shows-the-hands-the-rules-showed-and-the-beat-that-shows-them-stands.md): what is shown is what the rules showed (`ADR-0008` whole), and the **last beat stands 2 s**. `poker-server` is no longer touched. The hold's mechanism, **`DEC-146`**, is **answered** on 2026-09-07 by [`ADR-0136`](../../docs/adr/ADR-0136-a-beat-declares-its-own-length-and-zero-silences-every-beat.md): the beat carries its **kind** and the store owns both lengths, so `drive-duel.tsx` is not edited, the recorded-frame suites are not re-recorded, and **nothing crosses the socket** |
| 2b | The winning five is marked, in hand and on the board | `web-client`, `design` | **Answered — no.** [`ADR-0126`](../../docs/adr/ADR-0126-the-table-shows-the-cards-and-marks-none-of-them.md): **the table shows the cards and marks none of them.** `ADR-0095` §3 is applied to a statement made without words, not reopened; the five stay inside `poker-engine`, so **`poker-server` leaves this item** and no wire moves. What is left is one gate and one line on `STORY-1411`'s card |
| 2c | The pot travels to the winner | `web-client`, `design` | **A card.** `ADR-0115` already governs it — motion carries no fact, reduced motion stills it — and `ADR-0102` owns the pacing |
| 3a | The front door: the button is renamed, the wordmark is uncovered, the code field takes a link, the strip goes, `Account` grows | `web-client`, `design` | **A card.** The strip is answered — `ADR-0125` §5 makes `ProfileStrip`'s `no-profile` branch render nothing, so `No profile yet.` goes and the `profile` branch stays. The rename is answered above; the overlap is a defect |
| 3b | The waiting table: a copy button, the auto-start sentence, no `You` plate | `web-client`, `design` | **A card.** `ADR-0110` moved these surfaces and `ADR-0073`'s two promises stay verbatim |
| 3c | The table: the timebank figure, the disabled bar, the pot's chip pile | `web-client`, `design` | **Answered for the bar** — [`ADR-0127`](../../docs/adr/ADR-0127-a-control-stands-only-for-a-decision-the-server-has-opened.md): **no**, a control stands only for a decision the server has opened, so the `off` state keeps its box, its reserved rows and one still sentence. The human's *"show disabled controllers"* is recorded verbatim and **not followed**, and §7 makes the overrule two sentences. **`STORY-1406` is retired, not split**, and the item shrinks to its other two seams; the same ADR strikes `DEC-108`. The figure is a **defect** with a known cause and decides nothing |
| 3d | The table fits the phone — measured on the device, not on a headless viewport | `web-client`, `design`, `docs` | **Answered.** `DEC-136` → [`ADR-0121`](../../docs/adr/ADR-0121-the-table-reflows-never-scales-and-the-shape-is-measured-on-the-device.md): a **defect against `ADR-0103` §1's instrument**, not its mechanism — the reflow stays, the scale is refused, the width axis joins the contract, and the shape is device-measured |
| 4 | `Call` names what the player must add, not the total they will have in | `web-client` | **Answered.** `DEC-137` → [`ADR-0122`](../../docs/adr/ADR-0122-call-names-the-price-and-raise-to-names-the-total.md): the button prints the **price**, the other three verbs keep their totals, and the last-act mark goes bare on `Call`. No wire, so item 4 does not serialise against item 2b |
| 5 | A rematch offer reaches the rival wherever they are, as a surface they cannot miss | `web-client`, `design` | **Answered.** `DEC-138` → [`ADR-0123`](../../docs/adr/ADR-0123-a-standing-rematch-offer-follows-the-rival.md) — it follows, and never takes the screen; the mechanism `DEC-144` → [`ADR-0138`](../../docs/adr/ADR-0138-the-panel-mounts-beside-the-lobby-and-the-dismissal-lives-in-the-mount.md). No server half: the fact already reaches every screen |
| 6 | Pressing play again returns the host to the room they still hold | `poker-server` | **`DEC-139`** — the product owner's, and it **opened the already-open `DEC-111`**: the second room is not hypothetical, it is what the product does today. **Both answered** by [`ADR-0124`](../../docs/adr/ADR-0124-one-waiting-room-and-play-again-hands-it-back.md) — one waiting room, the second press hands back the first, no client change. The architect's `DEC-110`, widened to the `WAITING` case, is **answered** by [`ADR-0133`](../../docs/adr/ADR-0133-the-room-a-player-holds-is-scanned-for.md): a **lock-free scan** finds the room from the player, opening becomes find-or-create under a per-player stripe, and this item moves **no wire** |

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
screen, not a transfer** — and `DEC-132` was the one thing that could have made it false, because a
sign-out that abandons a profile is the first mechanism in this product that would strand coins.
**It does not.**
[`ADR-0131`](../../docs/adr/ADR-0131-signing-out-of-your-own-account-hands-the-browser-a-new-profile.md)
§2 abandons a profile only where the profile holds a credential — to be in that case you signed in
to it — so **nothing in this product abandons a profile a player cannot get back to**, and the
coins are as unmoved after a sign-out as they are across the promotion.

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
which is why `DEC-131` was registered rather than assumed. **It is answered** by
[`ADR-0130`](../../docs/adr/ADR-0130-a-name-can-be-changed-and-the-name-it-leaves-is-spent.md):
permanence goes, **`ADR-0051` §1's registry stays** — the name a player walks away from is
**retired**, spent for everyone including its former owner — and history keeps printing the name as
it **is**, because `ADR-0039` never let a name be copied onto a row.

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
**`DEC-145`** for the one route left into two duels at once. That widened `DEC-110` is itself
**answered on 2026-09-07** by
[`ADR-0133`](../../docs/adr/ADR-0133-the-room-a-player-holds-is-scanned-for.md) — the room is found
by a **lock-free scan** of the registry rather than by an index or by the connection's membership,
opening becomes find-or-create under a per-player stripe, and the `WAITING` half moves **no wire**,
so `STORY-1416` is startable and is not `atomic:`. The `PLAYING` half stays where it is —
`ADR-0105` §1's own ticket, outside this epic and **`atomic:`**, because its refusal spends a new
`ProtocolError` value, `ALREADY_IN_DUEL`, which moves `ADR-0047` §2's fingerprint and forces a
`PROTOCOL_VERSION` step. That is the one place `ADR-0133` **disagrees** with `ADR-0124` §6's
prediction, on the half `ADR-0124` left open; the `WAITING` hand-back moves no wire exactly as
predicted. It is the only decision in this epic answered by
the **architect** rather than the product owner, and the only one that reaches a question opened
before the epic existed. `DEC-137` is [`ADR-0122`](../../docs/adr/ADR-0122-call-names-the-price-and-raise-to-names-the-total.md)'s.
Every answered row has left the table below and stands under *Answered*; in their place the table
carries five of **the architect's**, registered by those answers — `DEC-142` for the suggestion's
generator, `DEC-146` for the beat's length, `DEC-144` for the cross-screen surface, and `DEC-151`
and `DEC-152` for the rename's and the sign-out's mechanisms. **`DEC-142` is answered on 2026-09-07**
by [`ADR-0137`](../../docs/adr/ADR-0137-a-name-suggestion-is-drawn-in-the-browser.md) — the
suggestion is drawn **in the browser**, from a bundled vocabulary, **nothing consults
`name_registry`** and **nothing crosses the wire**, so `STORY-1407` waits on nothing and its tickets
are not `atomic:`. `DEC-146` is **answered on
2026-09-07** by
[`ADR-0136`](../../docs/adr/ADR-0136-a-beat-declares-its-own-length-and-zero-silences-every-beat.md),
and stands under *Answered* below. A sixth, `DEC-147`, is
**answered on 2026-09-07** by
[`ADR-0132`](../../docs/adr/ADR-0132-the-profile-says-whether-it-holds-a-password.md): `GET /api/me`
gains `hasPassword: Boolean`, computed wherever a `ProfileResponse` is shaped and told only to the
player it is about, a body missing it reads `unavailable` rather than `false`, and the provider
re-reads after `signUp`'s `signed-up` — the one profile-changing outcome that does not already
reload. No wire moves, so item 1b's anonymous block is startable and `STORY-1408`'s ticket is not
`atomic:`. `ADR-0124`'s **`DEC-145`**, the product
owner's, blocked nothing here and stood in the registers rather than in the table below; it is **answered on 2026-09-08** by
[`ADR-0141`](../../docs/adr/ADR-0141-taking-a-seat-elsewhere-releases-the-room-you-were-holding.md) and
stands under *Answered*. The phone
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

**Two are open, and neither blocks anything: one the last split raised, and one [`ADR-0145`](../../docs/adr/ADR-0145-a-cards-scaffolding-is-composing-and-the-transcriber-names-the-mint.md) registered while answering a third.** Every decision this epic
raised or inherited is answered by a merged ADR — the fourteen product-owner answers recorded below
plus `DEC-156`'s, and seven the architect's — and the table that held them is empty. **Both of the
decisions the 2026-09-08 answers registered are answered as well**, by two different ADRs.

**`DEC-156` was raised on 2026-09-09, the product owner's, and is answered the same day.** It was
raised while landing `TASK-140909` by the coder that had just drawn the account screen's name form,
reading back its own work: four panels drawn alike, and only the name form spends something forever.
[`ADR-0143`](../../docs/adr/ADR-0143-irreversibility-is-said-last-and-never-coloured.md) answers it
and stands under *Answered* below. That ADR registered **`DEC-157`**, the product owner's — whether
*give this profile a password* owes an irreversibility sentence of its own, since it claims a handle
into `credential`'s unique index that nothing in the product gives back (`ADR-0039` refuses account
deletion in v0.1) while saying nothing about it. **`DEC-157` was answered on 2026-09-10** by
[`ADR-0146`](../../docs/adr/ADR-0146-a-handle-is-held-not-spent.md) — **no**, and it stands under
*Answered* below too. It blocked nothing and changed no ticket in this epic, in either direction.
The other row that remains in
`docs/adr/README.md` is `DEC-154`, which no answer of this epic's registered — it is `ADR-0141`'s
own, the architect's, and it blocks only the ticket that implements `ADR-0141`.

**`DEC-159` was registered on 2026-09-09, the architect's, and is answered the same day** by [`ADR-0144`](../../docs/adr/ADR-0144-a-negative-route-test-is-a-control-and-the-predicate-resolves-the-device-once.md). `ADR-0135` requires `GET /api/me/device`; the merged `DuelServerRoutesTest.theDeviceRouteIsNotInstalledOnAnyOtherVerb` asserted that route **unwired**, citing `ADR-0049` §5's *"only DELETE was asked for"*; repairing it took `TASK-141003` to **four** files, which the linter permits only under `atomic:`, which `ADR-0135` §8 forbids in as many words. **None of the three sources yields, because they were never jointly unsatisfiable.** `ADR-0049` §5 specifies `DELETE /api/me/device` and **contains no sentence constraining any other method on that path** — the prohibition was the **test comment's**, not an ADR's, so §1 withdraws the citation and supersedes nothing in `ADR-0049`. §2 rules that a negative route assertion is a **control for a positive row**, must name a verb no merged ADR has asked for, and **moves** when a later ADR asks for it: the test is renamed `…OnUnaskedVerbs` and issues `client.put`, keeping both inequalities and `TASK-040611`'s own reason, *"a `get(...)` typed beside the `delete(...)` would otherwise be invisible"*. §3 finds `ADR-0068` §4 already deciding the sizing question in the same direction — *"a scope that grew after the ticket was written… it is still a split, still today"* — so **no linter constant moves** and `ADR-0135` §8 stands; §4 lands the edit **before** the route, the only direction in which every intermediate assertion is true when it lands. **The second correction is made and named as an error**: §5 supersedes `ADR-0135` §4's `namesPlayer` mechanism, which cannot reproduce §5's own truth table and cannot be repaired by a sign, and computes the predicate from the shipped `resolve(token = null, deviceId)` instead — `Identity.Device`, `UnknownDevice` and `Anonymous` are §5's four rows, with **no new public surface**. §6 registers the ticket edits: new **`TASK-141015`** before `TASK-141003`, which stays three files and plain. **Registers no decision**, and names the recurrence as a trigger rather than claiming it is fixed: nothing detects an ADR asking for a verb a merged control denies, and **the second occurrence makes the verb list a mechanism decision**.

**`DEC-160` was registered on 2026-09-09, the architect's, and is answered the same day** by
[`ADR-0145`](../../docs/adr/ADR-0145-a-cards-scaffolding-is-composing-and-the-transcriber-names-the-mint.md):
**a design card's own scaffolding is composing, and the criterion is the transcriber, never the
level of detail.** It was raised while landing `TASK-141502`, whose `light` review returned `fail`
on the classification rather than on the artifact — every gate passed, the words matched
`RematchControl` exactly, and the card holds no motion. **§1 settles the reading the driver could
not**: `ADR-0091` §3 splits on what a card **creates for the product**, and has never been about
what the human has not yet seen, because §3's own *"**Either way** the verdict that matters stays
the human's visual one … and it may **trail the merge**"* puts the human's sight downstream of
**both** branches — the not-yet-seen reading empties the composing branch and contradicts the
paragraph it sits in, and `ADR-0091`'s first named cost says the same from the other side, that a
merged card ticket *"reads as* the look was approved *when it only means* the look was recorded and
is structurally sound."* **§2**: a card holds **subject**, which a coder transcribes into
`web-client/`, and **scaffolding** — `.wrap`, `.eyebrow`, `.lede`, `.frames`, `.frame`, `.note`,
`.stub`, `.screen` — which nothing transcribes; the split-time test is **name the file that will
transcribe it**, `ADR-0033` §1's consumer test lifted from values to drawings, with §3's *promotion
is one shared consumer away* carried over intact and a **direction** — a card copying **from**
shipped code mints nothing. **§3's mechanical floor** is three clauses read from the ticket's
`## Files` table plus `ls`, on `ADR-0091` §2's floor-plus-judgment pattern: writing
`design/tokens/tokens.css`; creating a file under `design/components/` or `design/graphics/`; or
scaffolding that depicts a **named product screen with no card under `design/screens/`**. The third
decides `TASK-141502` — `leaderboard.html` and `account.html` both exist, so the stand-in cannot
claim an authority already occupied, and had either card been missing the same drawing would have
been **minting**. **Specificity, the axis both parties argued, is rejected**: *abstract enough*
cannot be judged from a ticket, which moves the argument to the merge where it just cost a round
trip. Measured on `develop` at `702408ed`: `.wrap` in **20 of 21** cards, `1020px` in **19**,
`.frame` and `.note` in **all 12** screen cards, **406 raw `px` outside `:root`** — so
scaffolding-is-minting would reclassify all **86** `module: design` tickets, including the 43
already accepted at the pane. **§4: both merged tickets stand and nothing is owed** — `.panel` is
subject and `TASK-141501`'s `minting` label was right; the driver's conclusion is upheld and its
**stated ground corrected**, four raw lengths having landed rather than two, and `ADR-0103` §1
fixing 390 × 664 and **720 × 900**, so `960px` and `420px` have no external source. **`TASK-141511`
is composing**, its tier and dispatch unchanged. **§5 does not settle what the minting side costs**
and registers **`DEC-161`**, the architect's, below. **§6** records that `ADR-0091` §2's rule was
**never written into `.claude/agents/planner.md`**, the one file §2 names, and states what that file
owes — a defect with a known repair and no decision in it, and a ticket for the planner to cut from
the ADR.

**`DEC-161` was registered on 2026-09-09, the architect's, and is open.** `ADR-0091` §3 says a
minting card *"is worked interactively with the human, because taste does not survive a verify
block"*. Measured across all **86** `module: design` tickets, four carry the `minting` label —
`TASK-130301`, `TASK-130601`, `TASK-130701`, `TASK-141501` — and **all four are `review: standard`,
were dispatched to a coder and merged as ordinary PRs**. §3's interactive branch has not been
practised once since `ADR-0091` merged on 2026-08-30, and nothing failed; `.claude/agents/qa-manager.md`
independently reads §3 as prescribing a dispatched `module: design`, `review: light` ticket for a
whole missing card. So either the merged practice is a standing deviation owing a repair, or §3's
minting branch means something narrower than its words — a stricter tier and a labelled ticket,
rather than a different authorship channel. `ADR-0145` §5 registers it rather than answering it,
because `DEC-160` asks which **side** of the line scaffolding falls on and that is answerable
without settling what the other side costs. **Not in scope**: `ADR-0024` §3 assigns the visual
verdict to the human on every card, and `ADR-0091` names delegating taste as the one thing only the
human can give away — an answer that moved the **verdict** would be the human's. **It blocks
nothing**; its trigger is the next ticket a planner would label `minting`.

**`DEC-158` was registered on 2026-09-09, the architect's, and is open.** It was raised while
splitting `STORY-1415`, by reading `design/screens/rematch-states.html` beside the component it
was drawn from: measured on `develop` at `f20d07ed`, the card says `ImKate offers a rematch` and
`Rematch offered — waiting for ImKate` where `RematchControl.tsx:65,74` say *your rival*, and it
draws no frame at all for `That duel room is gone.` — a state that has shipped since
`TASK-030909`. **Which side is stale needed no decision**: `ADR-0123` §4 quotes *Your rival offers
a rematch* and calls it *"the result screen's own line"*, so
[`TASK-141511`](../tasks/TASK-141511-the-result-screens-card-says-the-words-the-result-screen-says.md)
repairs the sentences as a transcription with nothing to judge. What **is** undecided is the
gate: [`ADR-0142`](../../docs/adr/ADR-0142-a-text-module-is-checked-against-the-rendered-card.md)
§7 names *the first time a stale sentence is found on a card* as the trigger for reopening its
third direction, and this is that first time. It **blocks nothing** — no ticket in `STORY-1415`
waits on it, and `TASK-141503` handles `ADR-0142` §6's register in both landing orders through
`npm run check` alone.

**`DEC-155` was registered on 2026-09-08, the architect's, and is answered the same day.** It was
raised while landing `TASK-140811`, on a gap **two tickets reported independently**: `TASK-140710`
transcribed six strings from `design/screens/name-ask.html` and `TASK-140811` three from
`design/screens/account.html`, and in both cases the answer to *what would catch the module and the
card drifting apart* came back **none** — checked against `design/check-drift.sh`,
`design/check-frame-cards.sh` and both CI workflows. Each ticket's own `verify:` greps do compare the
two, and they do not outlive the merge, so the words the design gate holds and the words the product
ships were verified once and never again.

[`ADR-0142`](../../docs/adr/ADR-0142-a-text-module-is-checked-against-the-rendered-card.md) answers
it: **a text module is checked against the rendered card, by a test in the client's own suite.** It
amends nothing — it is `ADR-0091` §4's own *"Adoption is gated where consumption happens — the
client's own CI job"* carried out on a second fact — so the gate is
`web-client/src/design/card-text.test.ts`, reading each card with `readFileSync` off
`import.meta.url` exactly as `styles/tokens.test.ts` already reads `design/tokens/tokens.css`, with
**no workflow line and no dependency**. That placement is evidence-led rather than stylistic:
`design/check-frame-cards.sh` runs in **no workflow at all**, so *a gate only a ticket runs* has
already happened here once, to a design gate, unnoticed. The half-invented mechanism this epic left
behind is **not** the one chosen — the three `<!-- ANON-BLOCK: … -->` markers are **deleted** rather
than extended to every card, because nothing ever compared a marker to the `<p class="line">` beside
it and the gate reads the card's rendered text instead, comments stripped first. The costs are
named: roughly twenty-six declared register lines to bootstrap, one classification per future export
forever, and no carded sentence may ever span an inline element. And the yield is stated honestly —
the gate would have been **green throughout** at `TASK-140710`'s merge, so it repairs nothing this
epic shipped and its whole value is against the next edit.

**The other follow-up those answers registered is answered on 2026-09-08.** `DEC-150` — whether a
copy is achievable at all where `navigator.clipboard` is undefined, the architect's — is answered by
[`ADR-0140`](../../docs/adr/ADR-0140-the-clipboard-api-is-the-only-copy-this-client-attempts.md):
**none, and the reason is the missing read-back rather than the deprecation.**
`execCommand("copy")`'s boolean says the command was supported and enabled, not that the clipboard
holds the link, and the only in-page read-back —
`navigator.clipboard.readText` — sits on the same `[SecureContext]` interface that is missing by
hypothesis, so `ADR-0128` §4 is unmeetable and `Link copied.` stays gated on a resolved `writeText`.
The **silent** copy — attempt it, claim nothing — is refused on `ADR-0128` **§3** rather than §4:
the promise is that *"the screen says which"*, and a silent success prints
`Copy it from the box above.` on the exact devices the mechanism serves. **No file changes and no
ticket is owed** — `web-client/src/table/InvitePanel.tsx` already behaves this way, and the negative
is pinned three times, including `Lobby.test.tsx`'s seven-string enumeration of this epic's own
host-alone table. `ADR-0140` §6 names what would have to change before a mechanism could ever be
proved here: measured this run, jsdom 24.1.3 has no `document.execCommand` at all, and `ADR-0117`
§1's origin is secure, so a drive would need the bundle on a **non-secure** origin and a
paste-based read-back verb in `scripts/qa/drive.mjs`. It supersedes nothing, registers nothing, and
unblocks nothing — `ADR-0128` §7's *"`DEC-150` blocks nothing"* held to the end.

**`DEC-145` is answered the same day.** A player who holds a waiting room and then takes a seat in
another by invite — the product owner's — is answered by
[`ADR-0141`](../../docs/adr/ADR-0141-taking-a-seat-elsewhere-releases-the-room-you-were-holding.md):
**the seat is taken and the held room is released in the same act**, its code then answering
`UNKNOWN_ROOM` so that a released room is indistinguishable from a reaped one — no string, no wire,
no client file and nothing stored. Only a room still `WAITING` and holding nobody else is ever
released, a join that seats nobody changes nothing, and **a duel can still begin into a seat that is
already away**, which stays `ADR-0073` §3's promise working. It supersedes two sentences of
`ADR-0073`, amends `ADR-0105` §2's `WAITING` row on its `JoinRoom` half, and registers **`DEC-154`**
for the architect. It gates no story here and is not this epic's to close: `STORY-1416` is `done`,
and its *Out of scope* already excluded every change to `replyToJoinRoom`.

**One more was registered on 2026-09-07 by the planner rather than by an answer, and it was
answered the same day**: `DEC-153`, the architect's — how the client tells a **first** showing of
the rival's hand from a repeat of the same hand, and whether it is worth remembering anything to do
it. Answered by
[`ADR-0139`](../../docs/adr/ADR-0139-the-read-beat-is-spent-once-and-the-store-already-remembers.md):
**yes, and nothing new is stored.** `ADR-0120` §3's rule is *"a hand face up the viewer **has not
been shown before**"*, the sentence restating it *"as the client can evaluate it"* drops the word
**before**, and `TASK-141104` merged the restatement. Measured on `develop` at `5cce33c4` with a
throwaway `poker-server` probe over 60 duels, since reverted: **0** hand-completing `Snapshot`s in
**2,220** resume frames and **0** repeated deliveries across **604** hand endings — `act` calls
`advance` in the same call, so a live hand is never over — against **602** when the same probe is
asked what a runner would hand back without that call. `ADR-0139` takes the guard anyway and
**amends `ADR-0136` §1 in three sentences and in those three only**: `layOutReveal` gains
`held: PlayerView | null`, the `Snapshot` case passes `state.view`, and `DuelState` gains **no
field** — the memory was already there, so *"no previous view is remembered"* is superseded as a
statement about the argument list and about nothing else. The alternative — *write the invariant
down instead* — lost on where its pin would live: this epic's *Out of scope* forbids `poker-server`,
and a test deferred to a story outside the epic is a test nobody writes. **No story is created and
no decision is registered**; `ADR-0139` §9 writes the server invariant down as a fact with no test,
because after the guard nothing depends on it. It unblocks
[`TASK-141108`](../tasks/TASK-141108-the-read-beat-is-spent-once-and-a-repeat-is-a-step.md), which
stands **exactly as written**, and gated nothing else.

### Answered

| ID | Answered by | What it means here |
| --- | --- | --- |
| `DEC-157` | [`ADR-0146`](../../docs/adr/ADR-0146-a-handle-is-held-not-spent.md) | **No — *give this profile a password* owes no irreversibility sentence, because the act takes nothing from the player, and a handle is *held*, not *spent*.** Derived from *Positioning*'s *"The reference points are **Lichess** and **Chess.com**, not PokerStars. **Dark, quiet, fast, minimal.**"*, read as *a quiet screen does not warn about a cost it does not impose*; most of the work is merged ADRs — `ADR-0030` (*a claim adds a credential and moves nothing*), `ADR-0039`, `ADR-0031` §1, `ADR-0130` §2. **§1: no sentence, no second press, and no copy anywhere asserting that a handle is permanent or now unavailable to others.** **§2 applies `ADR-0143` §2 and states the trigger its §4 table left implicit** — an act owes those obligations when **a player who performs it ends up with less than they had**, never because the row it writes cannot be deleted, since under `ADR-0039` almost nothing here can be. **§3 answers the register's second half — *held***: `ADR-0130` §2's word describes a string that has **left** its owner, and a handle has left nobody; its derivation is entirely about visibility, and measurement kills the transfer — `ProfileResponse` carries **seven** fields and **no handle**, and `ADR-0031` §1 reads *"a handle is **never shown to anybody**"*, so a reissued handle would print a second player onto **no** row. **The decisive fact is a merged refusal to decide** — `ADR-0031` §1's *"**That is not foreclosed** … but it is not built, and no story should assume it"* — so *you cannot change this later* would settle handle permanence in shipped copy, one document after `ADR-0130` §5 paid to delete `PERMANENCE_LINE` (measured **0** occurrences in `web-client/src`). **§4** completes `ADR-0143` §4's fourth row as *nothing to say*, leaves the first three untouched, and measures both screen states: `ANONYMOUS_WAY_OUT` is already last before the panel in the state the card draws, and `AccountScreen.tsx:106` against `:133` leaves one reachable state where the form renders bare and can only answer the `409`. **§5: one margin sentence on `account.html`, no frame changes, no gate bought.** **§6** leaves *can a handle ever be changed* where `ADR-0031` §1 left it, and names — **without registering** — whether the screen should ever show a player their own handle. **§7:** reversed by a merged ADR **deciding** a handle is permanent, by one player asking to change theirs, or by the human. Supersedes nothing; corrects the word *spends* in `ADR-0143` §Consequences 5. **Nothing this epic ships changes**: no wire, no server file, no engine file, no string, no token, no control — one card note is owed and the planner cuts it. Costs named: a mistyped handle is still forever and unmentioned; a forgotten handle with no recovery address has no path back; **the answer is conditional on a decision nobody has made and nothing detects the condition changing**; and the gap is closed by ruling rather than by shipping |
| `DEC-160` | [`ADR-0145`](../../docs/adr/ADR-0145-a-cards-scaffolding-is-composing-and-the-transcriber-names-the-mint.md) | **A design card's own scaffolding is composing, and the criterion is the transcriber, never the level of detail.** `ADR-0091` §3 splits on what a card **creates for the product** — settled by §3's own *"**Either way** the verdict … may **trail the merge**"*, which puts the human's sight downstream of both branches, so the not-yet-seen reading empties the composing branch. A card holds **subject** (a coder transcribes it into `web-client/`) and **scaffolding** (`.wrap`, `.frame`, `.note`, `.stub`, `.screen` — nothing transcribes it); the split-time test is **name the file that will transcribe it**, `ADR-0033` §1's consumer test lifted from values to drawings, with a **direction** — copying *from* shipped code mints nothing. The floor is three clauses read from the `## Files` table plus `ls`: `tokens.css`; a new file under `components/` or `graphics/`; or scaffolding depicting a **named screen with no card under `design/screens/`** — the clause that decides `TASK-141502`, since `leaderboard.html` and `account.html` both exist. **Specificity is rejected as the axis**; authority is the axis. **Here**: `TASK-141501` and `TASK-141502` both stand, **nothing owed**, with the landing measurement corrected (`ADR-0103` §1 fixes 390 × 664 and **720 × 900**, so `960px` and `420px` have no source); the `fail` was a finding against the **split**, not the diff. **`TASK-141511` is composing** and its ticket does not change. Registers **`DEC-161`** for what the minting side costs, and records that `ADR-0091` §2's rule was **never written into `.claude/agents/planner.md`** |
| `DEC-156` | [`ADR-0143`](../../docs/adr/ADR-0143-irreversibility-is-said-last-and-never-coloured.md) | **Yes, an irreversible act gets weight — and the weight is words, position and a press, never a treatment.** Derived from *Positioning*'s *"The reference points are **Lichess** and **Chess.com**, not PokerStars. **Dark, quiet, fast, minimal.**"*, after correcting the premise by measurement: the account surface carries **three** irreversible acts, not one — the name, `REVOKE_PERMANENT`'s *"This device will never sign in to this account again. This cannot be undone."*, and `SIGN_OUT_WARNING` — and the product's shipped grammar for the other two is `RevokeControl`'s and `SignOutControl`'s **in-page second press in plain type**, *"never a native dialog… one shape for both confirmations on this screen"*. **§1: no danger register** — no accent, tint, border, box, rule, icon, badge, size or weight step, capitalisation or motion, on any screen, and none in the assistive register either (**no `role="alert"`**, no `aria-live` urgency, no hidden *Warning:* prefix). Measured: colour in this client names **what a thing is** — a result, a clock running out, whose turn it is, your own ranked row, a standing offer, a control — and has **never** named a consequence, all **eight** `role="status"` refusals across seven components being plain; and `tokens.css` reserves the only urgency hue in its own comment (*"Amber exists only for the turn clock running down."*), so a treatment needs a **new** hue and minting is the human's (`ADR-0091` §3). **§2: three obligations** on a screen of controls, stopping at the table's edge — say it cannot be undone; make that claim **the last thing read before the control**; take a **second press wherever no merged ADR has refused one**. **§3:** the name form takes the first two and **cannot** take the third, because `ADR-0130` §1's *"no confirmation step"* and §7's *"no confirmation press"* are **applied, not reopened** — leaving the display name the one irreversible act whose entire guard is a sentence. **§4** tables every irreversible act and registers the one that is unguarded as **`DEC-157`** rather than answering it. **§5: `TASK-140909`'s two name-form frames already conform** — the *gone for good* line is last, with only the field between it and the button — so the card owes **one margin sentence** recording that the flatness is the decision, and **no frame changes**; `TASK-140910`–`TASK-140912` keep transcribing into plain `text-small`. **No gate is bought**, and `ADR-0126` §Consequences 5's price is paid again knowingly. **§7:** reversed by **one player asking for a name back** — which needs no instrument, because `ADR-0130` §2 leaves no mechanism that could serve them — or by the human at the pane in one sentence; another agent re-reading the card is explicitly **not** evidence. **Supersedes nothing**; narrows `ADR-0130` §5's grant of layout to the card **by exactly one relation**. **Nothing this epic ships changes**: no wire, no server file, no engine file, no new string, no new token, no new control |
| `DEC-145` | [`ADR-0141`](../../docs/adr/ADR-0141-taking-a-seat-elsewhere-releases-the-room-you-were-holding.md) | **The seat elsewhere is taken, and the room they were holding is released in the same act.** The one route `ADR-0124` §7 left into two duels at once, closed from `docs/vision.md` in **two halves from two sentences**: the seat by the first success condition — *"Send a link. She opens it in a browser. We play a full heads-up match."* with *"Everything else is downstream of that moment."*, which `ADR-0094` §1 already read to ship this path, so it gains **no second qualification**; the room by *"One duel coin per win… A counter of duels won."* with *"Two people, **one link**, one heads-up poker match"* and *"Not a multi-table poker room."*, `ADR-0124`'s own three sentences for `DEC-111`. **A released room is indistinguishable from a reaped one** — `UNKNOWN_ROOM`, and `Lobby.tsx` prints the string it already prints — so **no string, no wire, no client file, nothing stored**. Bounded three ways: only a room still `WAITING` and holding nobody else (§3), only on a join that seated somebody (§4), and **never** because the holder merely left, dropped or closed the tab (§5) — a duel can still begin into a seat that is already away, and that stays `ADR-0073` §3's promise working. Nothing is said, to anyone (§6). Supersedes two sentences of `ADR-0073` as statements about a `WAITING` room in general, amends `ADR-0105` §2's `WAITING` row on its `JoinRoom` half, and registers **`DEC-154`** for the architect. **It changes nothing this epic ships**: `STORY-1416` is `done` and its `Out of scope` already excluded every change to `replyToJoinRoom` |
| `DEC-150` | [`ADR-0140`](../../docs/adr/ADR-0140-the-clipboard-api-is-the-only-copy-this-client-attempts.md) | **None — and the reason is the missing read-back, not the deprecation.** `navigator.clipboard.writeText` stays the only clipboard mechanism this client uses; where the API is undefined the press performs `ADR-0128` §3's hand-over and attempts nothing else. `execCommand("copy")` reports that the command was supported and enabled, never that the clipboard holds the link, and `navigator.clipboard.readText` — the one in-page read-back — is absent exactly where it would be needed, so §4's *"never of an attempt whose success the client cannot read"* cannot be met. The **silent** copy is refused on **§3**, whose promise is that *"the screen says which"*; a third, hedged outcome would be a string, and `ADR-0110` §6 makes that the product owner's. Nothing in this epic moves: no file, no card, no frame, no string, no ticket |
| `DEC-153` | [`ADR-0139`](../../docs/adr/ADR-0139-the-read-beat-is-spent-once-and-the-store-already-remembers.md) | **The read beat is spent once per hand, and the store already remembers.** The word being repaired is `before` — `ADR-0120` §3's rule says *has not been shown before*, its own restatement drops it, and `ADR-0136` §1 shipped the restatement while calling it *not a paraphrase*; that claim is **withdrawn**. `layOutReveal` gains `held: PlayerView \| null`, the `Snapshot` case passes `state.view` — still the *previous* view on that path — and the final step is `"read"` only when the rival's cards are **newly** shown. **`DuelState` gains no field**: `state.view` was already there, is written only by the `Snapshot` case, is cleared by nothing, and holds a `COMPLETE` view only by having **painted** it, since a frame arriving while a reveal stands is queued rather than applied — which is why the amendment to `ADR-0136` §1 is three sentences and why all three of its reasons survive. The guard sits in the **reducer** because `advanceReveal` folds queued frames back through `applyServerMessage`, a path a guard in `duel-store.ts` would miss. Two ways it could be wrong are closed by merged facts: `HandRevealed` fires only in `reachShowdownAndSettle`, which settles in the same engine step, so rival cards cannot appear before `COMPLETE`; and the hand comparison keeps the **next** hand a read. **Measured unreachable** — 0 in 2,220 resume frames, against 602 from a falsified detector — which is why the second answer was real; it lost because its pin needed `poker-server`, which this epic forbids. Costs named: the classification is no longer a pure function of one frame, a one-day-old ADR is amended by the story implementing it, a reload still forgets, and **the change is unfalsifiable in the product**. Unblocks `TASK-141108`, which stands exactly as written; **registers no decision and creates no story** |
| `DEC-144` | [`ADR-0138`](../../docs/adr/ADR-0138-the-panel-mounts-beside-the-lobby-and-the-dismissal-lives-in-the-mount.md) | **The panel mounts beside the lobby.** A props-less `RematchNotice` is `App`'s last child of `<main>`, beside `<Lobby />` — `Lobby.tsx` is not edited, the gate is a second call of `ADR-0114`'s `shown`/`ruling` with **no effect**, and the dismissal is one `useState` boolean living as long as the mount. `ADR-0123` §7's dismissal lifetime decided the mount point: state inside the cascade dies on one press of *Back*. Three of §2's prohibitions then hold structurally — outside every `<form>`, no timer, out of flow. The dismissal clears **during render** when the offer ends, or one *Not now* would hide every later offer for the tab's life. `role="status"`, no focus call, no key handler. **Nothing crosses the socket**, so `STORY-1415`'s ticket is not `atomic:` — it waits only on its minting card |
| `DEC-142` | [`ADR-0137`](../../docs/adr/ADR-0137-a-name-suggestion-is-drawn-in-the-browser.md) | **Drawn in the browser, from a bundled vocabulary, and nothing consults `name_registry`.** The three halves of the question, answered in order. **What generates it:** `suggestName(random, replacing?)` in `web-client/src/profile/name-suggestion.ts` — a pure function that takes **no player fact of any kind**, so `ADR-0029` §6 and `ADR-0067` hold by the signature rather than by care; `random` is a `[0, 1)` source defaulting to `Math.random` at the call site, `reconnecting.ts`'s shipped `jitter` idiom, and the index is clamped so a source returning `1` cannot join `undefined` into a **permanent** name. **Does it consult the registry: no** — no query, no endpoint, no filter, no hint, so `ADR-0029` §5 and `ADR-0051` §9 are applied and not reopened. **Does it cross the wire: no** — no socket frame, no HTTP route, no `docs/protocol.md` edit, no Kotlin file, no migration; `ADR-0047` §2's fingerprint cannot move and **`STORY-1407`'s tickets are not `atomic:`** under `ADR-0068` §3. The property `ADR-0119` §4 asked for — *a refusal rare enough that accepting normally works first try* — is therefore bought with **size** rather than knowledge: an import-free vocabulary module of word lists, **arity as data**, joined by one `U+0020`, held by three tests — the product of the list lengths `>= SUGGESTION_SPACE_FLOOR = 1_000_000`, every entry NFC and free of `Cc`, `Cf` and all whitespace, and the longest-entry sum plus separators `<= 32` code points. So a suggestion can be refused with `409` and **can never be refused with `400`** — the product never offers a string its own write path rejects. One draw per mount, stored nowhere; a reroll on `conflict` alone and only into a field still holding the unedited suggestion; and the reroll is free because `ADR-0134` §6 meters *spending* and refunds a `409`. **This ADR ships no word** — the vocabulary's contents stay `ADR-0119`'s deliberate omission and are the implementing ticket's, under the human's eye |
| `DEC-146` | [`ADR-0136`](../../docs/adr/ADR-0136-a-beat-declares-its-own-length-and-zero-silences-every-beat.md) | **A beat declares its own length, and zero silences every beat.** The beat carries its **kind**, not its length: `RevealStep` gains `hold: "step" \| "read"`, set once in `layOutReveal` from `ADR-0120` §3's own predicate over **`PlayerView.viewerSeat`** and `SeatView.holeCards` — a pure function of one frame, so the two seats answer differently for the same hand and a queued hand-ending `Snapshot` classifies itself on drain. The **store** owns both millisecond figures: `readMillis`, absent meaning `stepMillis`, mapped in `armTick` off the beat at the head of the queue, with `REVEAL_READ_MS = 2000` named beside `REVEAL_STEP_MS = 600` at `ADR-0102` §4's boot seam — **extended, not amended**, and `advanceReveal` byte-unchanged. **`stepMillis === 0` silences every beat**, gated in the store rather than in boot so all 113 call sites are covered: **`drive-duel.tsx` is not edited**, no file under `web-client/src/e2e/` is, and `scripted-duel.gen.json` is byte-unchanged — the recorded-frame suites go on proving the frames, which is all they ever proved, while the hold is proved by the injected `schedule`'s **`delayMillis` arguments**. No component learns the hold and the **DOM is byte-identical at both lengths**; the hold has **no CSS**, so `ADR-0115` §3's *a step is not motion* holds by construction and reduced motion cannot shorten or skip it. **Nothing crosses the socket** — two already-shipping fields, already filtered by `PlayerView.of`'s `showCards` — so `ADR-0047` §2's fingerprint cannot move and **`STORY-1411`'s implementing ticket is not `atomic:`**. Named without registering: a preflop showdown runs **3.8 s**, and the deciding hand's result screen arrives 2 s later |
| `DEC-151` | [`ADR-0134`](../../docs/adr/ADR-0134-a-rename-spends-before-it-replaces.md) | **A rename spends before it replaces, and no name is ever released.** The mechanism `ADR-0130` registered and designed none of, answered whole. **The permanence trigger becomes the never-released trigger** — `player_display_name_is_permanent()` is dropped and re-created as `player_display_name_is_never_released()`, carrying `ADR-0051` §3's body **minus one disjunct** (a name may leave a player only if the registry has already spent it), so the clause `ADR-0051` had to cut as an exception becomes the whole rule; `NULL → name`, the identical-name no-op and `name → NULL` are unchanged, `retire_display_name` stays the only route to nameless and is not edited. The **names** change because a schema telling a `psql` operator a column is *permanent* is `PERMANENCE_LINE` with a smaller audience. **The write is four statements in one transaction** — `SELECT … FOR UPDATE`, the `TAKEN` insert, the retirement of the string being left, then the column — with the profile locked **before any registry row**, the order the takedown already takes; that lock replaces `AND display_name IS NULL` as the interlock, so two concurrent `PUT`s from one player have no fifth outcome. `SetNameResult.AlreadyNamed` is **deleted**, `ADR-0051` §2's rollback now protects **two** strings, and statement 3 carries **no** `AND reason = 'TAKEN'` so an impossible row raises rather than passing quietly. **The vacated row moves `TAKEN → REPLACED`** — a fourth `reason`, with **`retired_from` populated**: spent for everyone under a fold that ignores `reason`, while recording which of the two ways the name left its holder, which is the fact `ADR-0130`'s own reversibility argument rests on and the one thing unrecoverable if not written today. Not `RELEASED`, because that is the answer `ADR-0130` §2 refused. **`ADR-0052` §6's bit reads false for a renamer for two independent reasons**, and `PostgresProfileReads`/`PostgresProfileWrites` change not at all. **`PUT /api/me/name` answers `200 \| 400 \| 401 \| 409 \| 429`** — `403` is deleted rather than repurposed, and a case-only self-rename is `409` forever. **Yes, the write is budgeted**, and the budget **meters spending rather than trying** — keyed by `PlayerId`, `ADR-0074` §2's reserve-then-refund, every refusal refunded because `ADR-0051` §2 says it burns nothing, and numbers unreachable by a human so it stays a rate limit rather than the quota `ADR-0130` §1 refuses. **Nothing crosses the socket** — `PROTOCOL_VERSION` does not move and this story's tickets are **not** `atomic:`. For item 1c: **`STORY-1409` is unblocked on both halves** |
| `DEC-152` | [`ADR-0135`](../../docs/adr/ADR-0135-the-server-says-which-sign-out-this-is-and-the-browser-forgets-one-key.md) | **The server says which sign-out this is, and the browser forgets one key.** `ADR-0131` §3 forbids every server act that *would be* the abandonment, so the only lever left is **what the browser holds** (`ADR-0002` licenses pulling it); §7 says the **case** is a server fact, and it is. The act is therefore the client's and the knowledge the server's, and the split **must fail safe**. **`player.device_id` does not exist** — `V7__device_binding.sql` dropped it (`ADR-0049` §1, §7) — and nothing is written to `device_binding`, `player`, `credential`, `duel` or `duel_result`; the row left behind stays **live**. **A new `forgetDeviceId` in `device-id.ts`**, reachable from `signOut` alone and in the abandoning case alone, before `reload()` and whatever the server answered; **exactly three keys move**, and `ADR-0119` §5's skipped bit is kept (`ADR-0131` §6). **The replacement profile is minted by `Identity.Anonymous` on the next `Hello`** — shipped and unchanged — so it is created **at that handshake, not at the press**, and a browser that closes the tab writes no rows at all. **The case is `GET /api/me/device`**, session-required on `ADR-0049` §5's reasoning, answering `DeviceStandingResponse(signOutHandsANewProfile)`: `true` **exactly when the `X-Device-Id` presented on that same request does not resolve, through a live binding, to a player other than the caller**, computed by one new `IdentityResolver.namesPlayer` with **`resolve` and all five `Identity` cases byte-unchanged**. No `409` guard is needed — a session is issued only by sign-in, so **`ADR-0131` §2's invariant is a property of the route's identity rule**. **`true` is unreachable while any live binding names another player**, so the outcome §2 forbids cannot be expressed; the revoked-binding browser, already handed a fresh profile silently today, now reads `true` and is told the truth. **Unknown is a keep**, and the read must not go through `readFromApi`. **`signedIn` never answers which sign-out this is.** **Nothing crosses the socket** — `PROTOCOL_VERSION` does not move and `STORY-1410`'s implementing ticket is **not** `atomic:`. Costs: the abandonment is **not durable** and **no undo or grace window is added**, so the forgotten-password cost stays exactly where `ADR-0131` left it; a second account-screen request; a route for one bit; a fact read on one screen and spent at a later press; and `STORY-1408` serialised against `STORY-1410`. Registers no decision |
| `DEC-131` | [`ADR-0130`](../../docs/adr/ADR-0130-a-name-can-be-changed-and-the-name-it-leaves-is-spent.md) | **A name can be changed, and the name it leaves behind is spent.** Two halves from two places: ***that* a name may be changed is the human's, recorded verbatim** (*"player name can be changes at any time in accounts settings"*), reversing their own `DEC-017` answer, and the ADR does not choose it; ***what becomes of the string* is derived** from the vision's *"**A leaderboard.** Ranked results over a season."* read as `ADR-0067` §1 read it — a row is **text and leads nowhere**, so the name is a row's only identity, and `ADR-0039` makes history a **live join**, so a reissued string would print a second player onto the first's finished duels. **§1**: a change is a `set` under every existing rule — canonicalisation, characters, the fold, `ADR-0038`'s screening — **no quota, no cooling-off, no confirmation**, and it never leaves the player nameless. **§2**: **retired, not released and not held** — spent for everyone including its former owner, so `ADR-0038`'s *retired forever, including the player it was taken from* becomes one rule for both ways a name leaves a player, and `ADR-0051` §1 is leaned on rather than amended. **§3**: **history shows the name as it *is***; no row is rewritten because no row holds a name, the old name appears nowhere, nothing marks a changed row, and — measured — the blast radius is **three surfaces** while the table is untouched (no `ServerMessage` carries a name; the opponent is *Your rival*). **§4**: nobody is told, and `ADR-0052` §1's removal notice never fires for a rename. **§5**: `PERMANENCE_LINE` and the `permanent` refusal sentence **leave the product**, replaced by two obligations said before the send. **§6**: **one form on the account screen**, doing both acts — the seam `ADR-0125` left open — with `NameSurface` off the front door, `ADR-0052`'s notice travelling with it, `ADR-0119` §1's ask untouched, and **no suggestion on a change**. **Supersedes** `ADR-0029` §4's rename clause, its title's *permanent* and §5's `403`; `ADR-0051` §3's *no exception at all*; `ADR-0119` §3's obligation 2. Costs named: **a rename is one-way**, the namespace burns per `PUT`, a rival can never find you again, other players' records change under them, and two merged migrations encode the clause §1 makes wrong. For item 1c: **`STORY-1409` is unblocked on the product side** and waits only on **`DEC-151`**, the architect's |
| `DEC-132` | [`ADR-0131`](../../docs/adr/ADR-0131-signing-out-of-your-own-account-hands-the-browser-a-new-profile.md) | **Yes when the account being left is the profile this browser owns; no when it is somebody else's.** Three rows: an own profile (a promotion, `ADR-0030` §1) → a **new, empty** anonymous profile; somebody else's account → the profile the browser owns, unchanged; no profile at all → a first-time visitor, which already ships. **Both halves are the human's and neither is chosen** — the abandoning half verbatim 2026-09-06 (*"if logout you comeback to new annonumus profile"*, the fourth clause of an item whose every other clause is about this browser's own profile and its promotion, and which never mentions signing into an account the browser does not own), the keeping half verbatim 2026-08-14 (*"i would like to keep anonimous accout if user log out"*, `ADR-0030` §2). What the ADR chooses is **which sentence governs which case**, and it says so as a **reading**, leaving the human one sentence that would correct it. A browser is handed a fresh profile **at most once per promotion**. **§2 answers this epic's own framing of the risk**: to be in the abandoning case you signed in to that profile, so it holds a password — **nothing is ever abandoned that cannot be signed back into**, and `DEC-152` may not build a route that is, which is also why **item 1e stays true**. **§3: nothing is revoked, deleted, moved or destroyed** — `ADR-0049`'s finality means *nothing here, because a sign-out is not a revocation*, on `ADR-0050`'s own *"Revocation is final forever; signing out is the cheapest act in the product."* **§4** licenses one abandonment at one confirmed press; `ADR-0027` §5's stale-client harm stays forbidden. **§5: the player is told before it happens**, on the confirmation `SignOutControl` already renders — **no new surface**, two obligations, `ADR-0125` §3's three refusals re-applied, licensed by `ADR-0037`'s *the account screens must state which routes are live* — so `SIGN_OUT_WARNING`'s *"This browser goes back to the profile it had before."* is **false the day the behaviour lands** and moves in the same diff. **§6** weighs `ADR-0119` §5 and leaves it **unamended**: a new profile is not an answer, so a player who skipped is not asked again and plays as `No name`. **Supersedes `ADR-0030` §3's outcome clause and its *keeps its device id* bullet, and §8's *not on sign-out* and *clears the token and only the token*, for one case only**, and **nothing** in `ADR-0012`, `ADR-0027`, `ADR-0037`, `ADR-0049` or `ADR-0050` — so item 1d's *contradicts three merged ADRs* turns out to be false, and the one it does contradict was not on the list. Costs named: a coin count seen falling to **zero** with one sentence of explanation; **a forgotten password now loses the account in one press that doing nothing used to undo**, the strongest argument against the decision and stated as such; empty profiles accumulating under `ADR-0039`; two sign-outs behaving differently under one label. Registers **`DEC-152`** |
| `DEC-147` | [`ADR-0132`](../../docs/adr/ADR-0132-the-profile-says-whether-it-holds-a-password.md) | **The profile says whether it holds a password, and the client derives nothing.** `ProfileResponse` gains a seventh field, **`hasPassword: Boolean`** — true exactly when a `credential` row exists for this player with `kind = CredentialKind.PASSWORD`, **no default value**, declared last. Named for the **kind** rather than the concept, because `ADR-0027` §1 keeps `credential` open to a passphrase or a third-party subject, so a `hasCredential` would one day read `true` for a player whose only stated way out is the password form; it freezes nothing about `DEC-027`. It is **computed on every response that carries the DTO and never a literal** — a fourth correlated `EXISTS` in `PostgresProfileReads.PROFILE_OF_SQL` *and in both statements in `PostgresProfileWrites`*, so `PUT /api/me/name`'s `200` cannot make `reportNameWrite` turn a claimed player anonymous — with the kind **bound** from `CredentialKind.PASSWORD` rather than spelled as SQL. **Told only to the player it is about**: no other DTO carries it, no route answers it for a player the caller did not resolve to, nothing filters or sorts on it, and it reaches no log line, no `ServerMessage` and no engine type. On the client, **a missing field reads `unavailable`, never `false`**, which is the one line that keeps `ADR-0125` §4's *not told at all* case reachable; the screen renders §3's block on `!hasPassword` and consults `signedIn` for it never. **Nothing shipped is re-keyed** — `showPasswordRoute`, `showSignUp`, `showSignInDoor` and `showAttach` keep their predicates, per `ADR-0125` §4's own words. **The held profile is re-read, never edited**: the provider re-reads `GET /api/me` after an outcome the server has confirmed changed the profile, and today that is exactly one — `signUp`'s `signed-up`, because sign-in and sign-out already `reload` while `sign-up.ts` passes `noReload` on purpose; without it the block would print *anonymous* beside `This profile now has a password.` **HTTP, not the socket**: `PROTOCOL_VERSION` does not move and `STORY-1408`'s ticket is **not `atomic:`** on that account, though it is still one diff or every profile read becomes `unavailable`. For item 1b: the **anonymous block is startable**, and the epic's blocked-part list loses its last entry for this story. Names a defect for the planner rather than fixing it — `PostgresProfileWrites` passes a literal `false` for `hasRecoveryEmail`, so setting a display name prints `RECOVERY_OFF` to a player whose recovery is on |
| `DEC-138` | [`ADR-0123`](../../docs/adr/ADR-0123-a-standing-rematch-offer-follows-the-rival.md) | **A standing rematch offer follows the rival, and it never takes the screen.** An offer **from the rival** follows a player onto every screen they can be on while they still hold the room — every `Screen` but `first`, **no carve-out**, mailed screens included — while a player who **left** the room (`forgetRoom()`, the page load behind *Back to the lobby*) is followed nowhere. The surface is a **panel above the screen and never a modal**: nothing beneath it is scrimmed or disabled, it takes no focus, **no keypress the player was already making can answer it**, it makes no sound, and it never retires itself on a timer — *cannot miss* is bought by standing there, not by blocking. Only an **incoming** offer follows, and only where the room's own screen is not showing, so one fact never has two live surfaces. **No string is minted**, and **dismissal hides the surface, never the offer** — nothing is sent, nothing is stored, and a reload brings the panel back on the offer the server restates (`ADR-0044` §5). Amends nothing: no wire, no `PROTOCOL_VERSION`, no server file, no stored key, and `ADR-0112` §2's *no notice, no dialog* stands untouched. For item 5: **`poker-server` leaves it**, the card is **minting** work with the human (`ADR-0091` §3, `ADR-0123` §8), and the mechanism is registered as **`DEC-144`**, the architect's |
| `DEC-139` | [ADR-0124](../../docs/adr/ADR-0124-one-waiting-room-and-play-again-hands-it-back.md) | **Yes — and the server is what returns them.** While a player holds a `WAITING` seat, a `CreateRoom` from them **opens no second room**: they are handed back the one they hold, same code, same seat, room left exactly as found, and **nothing is refused** — which is what keeps `ADR-0073` §3's promise true instead of breaking it, *"…and it brings you back"* now twice over. **The browser goes on forgetting** (`ADR-0072` §3 byte-unchanged, **no client change at all**): a remembered code is what boot rejoins on, so keeping it across `Back to the lobby` would put the waiting table straight back up and restore `ADR-0073`'s room with no door; the seat is the server's fact; and the server can answer a device that has never heard of the room. **Nothing is said and no string is added** — `ADR-0110` §6's enumeration untouched, on *"Dark, quiet, fast, minimal."* — with the observation that would reopen it stated. The ten minutes still runs from `Room.open` and a return does **not** restart it. Item 6's story is now unblocked on the product side and waits on the architect's `DEC-110` |
| `DEC-111` | [ADR-0124](../../docs/adr/ADR-0124-one-waiting-room-and-play-again-hands-it-back.md) | **No — one waiting room per player**, derived from `docs/vision.md`'s *"Two people, **one link**, one heads-up poker match, one winner"* with *"Not a multi-table poker room"* and *"A counter of duels won."* Answered **by construction**: a `WAITING` seat has exactly one source (`Room.open` is its only constructor, `RoomRegistry.create` has one production call site, no transition returns a room to `WAITING`), so what a second `CreateRoom` does *is* how many waiting rooms a player may hold — and it hands back the first. Amends `ADR-0105` §2's `WAITING` row only. **The harm is narrowed, not closed**: `DEC-145` registers the one route left into two duels at once — a holder of a waiting room taking a seat in **another** room by invite |
| `DEC-130` | [`ADR-0125`](../../docs/adr/ADR-0125-the-account-screen-names-the-anonymous-profile-and-owns-the-door.md) | **The account screen names the anonymous profile, and it is the only door to a password.** *That* the post-win offer goes is **the human's, recorded verbatim** — the whole block on `edits4.png` struck through with one word, *remove* — and §1 retires it whole: the surface, its strings, `offerAccount`, `DuelResult`'s `offer` prop, `pd.accountOfferSettled`, its module and its row in the one-module gate, in one diff. **Nothing replaces it** (§2): no banner, no badge, no reminder on any screen, and the standing door is the `Account` control the front door already renders unconditionally — so a win is **two presses** from the password form, refused deliberately because a pointer on the result screen is the struck block in a smaller font. **§3** names the profile *anonymous* and says three things — what it is, that it lives in this browser, and that a password keeps **every coin and every duel** (`ADR-0030` §1, which is item 1e's *sentence on a screen*) — words to the card, inside three refusals: **not a tier**, it states rather than urges, and the heading stays `Account`. **§4**: the screen asserts the state only when it has been **told**, never from a missing session token — which registers **`DEC-147`** and holds §3's block until that lands, while §§1, 2, 5 and 6 wait on nothing. **§5**: `ProfileStrip`'s `no-profile` branch renders nothing, so `No profile yet.` leaves the product and the `profile` branch is untouched. **§6**: *anonymous* is a state, never a name — `ADR-0058` stands, and a player may be **named and anonymous**. **Supersedes** `ADR-0085`, `ADR-0086` and `ADR-0116` in full and `ADR-0036`'s offer block; `ADR-0036`'s *never required* first paragraph stands and is re-applied. **Strikes `DEC-089`** by deletion. The cost is named, not argued away: this is the *"optional forever, with no prompt"* shape `ADR-0036` §Alternatives rejected, and a player who never opens the account screen is never told. Leaves the credential's shape (`ADR-0041`) to the human |
| `DEC-134` | [`ADR-0126`](../../docs/adr/ADR-0126-the-table-shows-the-cards-and-marks-none-of-them.md) | **No — the table shows the cards and marks none of them.** Every card is drawn like every other card of its kind: at a showdown, at a fold, at every street, in either seat, on the board and for **both** winners of a split, the five that made the winning hand are drawn exactly as the two that did not. A mark is anything separating one drawn card from another by *what it did* rather than *what it is* — ring, border, glow, tint, shadow, lift, scale, opacity, reordering, badge, connector, a keyable class, a `data-*`, a `title`, or anything in an `aria-label` beyond `card-text.ts`'s rank and suit — and **dimming the cards that did not play is the same statement**. `ADR-0095` §3 is **applied to a statement made without words, not reopened**: a highlight prints no string, so `HAND_TALK` could never see it, but the fact is the same kind and *these five played* is one string from *two pair, kings and jacks*. Licensed by the vision's *"replayed and **analysed afterwards**"*, dated **v0.4**; the human's annotation is the report, not the ruling. For this epic: **`poker-server` leaves item 2b**, there is no wire move, no `PROTOCOL_VERSION` step and no `atomic:` ticket, so **item 2 is client and design end to end** and `ADR-0047`'s lock stays free for `DEC-137`. `BestHand.cards` keeps its value and loses its stated reason — the KDoc is the stale sentence, its one-line repair is **not this epic's** (*Out of scope*: nothing here opens `poker-engine`). `STORY-1412` becomes **one gate** (*the drawing of a card does not depend on whether it played*) plus one line on `STORY-1411`'s showdown card, and **`STORY-1413` waits on nothing**. Costs named: the player who cannot read seven cards is charged a second time, something the human asked for in writing is declined, the deferred field gets dearer, and a gate must be written to hold a nothing. Reversal keeps `ADR-0095` §6's trigger and must settle **the mark and the name together** |
| `DEC-135` | [`ADR-0127`](../../docs/adr/ADR-0127-a-control-stands-only-for-a-decision-the-server-has-opened.md) | **No — a control stands only for a decision the server has opened.** When it is not the player's turn the bar keeps its box, keeps both rows reserved and holds **one still sentence**; it draws no button, sizing chip or amount field outside a turn — not live, not faint, not outlined, not as a placeholder. Measured: `PendingTurn.legalActions` is written by `case "YourTurn"` alone (`duel-state.ts:282-292`) and cleared by the next `Snapshot`, so the client holds **no legal actions outside a turn**; heads-up the coming decision is `{CHECK, BET}` or `{FOLD, CALL, RAISE}` and the client cannot know which, which makes **verbs-without-amounts the bigger claim, not the smaller one**. The faint treatment keeps its one shipped meaning — the card's *Sent — waiting on the server* — so two facts are never drawn alike. **The human's *"show disabled controllers"* is recorded verbatim and not followed**; §7 makes the overrule two sentences (which action shape, and what stands where an amount goes). For item 3c: **`STORY-1406` is retired rather than split** — its title states the opposite of the decision — and the item shrinks to the timebank figure and the pot's chip pile. Decides nothing about the sentence's *words* (whether the bar names the rival follows `ADR-0119`'s naming work), about pre-actions, or about the waiting-room screen, whose string sits on an empty seat plate and not in a bar |
| `DEC-108` | [`ADR-0127`](../../docs/adr/ADR-0127-a-control-stands-only-for-a-decision-the-server-has-opened.md) §4 | **Struck by this epic's own answer, on both halves.** *May* wait *and* you may act *be sayable at one moment?* **No.** *Which gives way?* **The notice, never the bar.** It was raised outside this epic by `STORY-1214` and is closed here because `DEC-135` could not be answered without settling what an unpressable bar means. No ticket falls out: `ADR-0113` §7 already deleted the pause, `TASK-130911` removed the line that named it, and `DUEL_PAUSED` is verified absent from every `.kt`/`.ts`/`.tsx` and from `ProtocolError` |
| `DEC-137` | [`ADR-0122`](../../docs/adr/ADR-0122-call-names-the-price-and-raise-to-names-the-total.md) | **The button names the price.** `callTo − committedThisStreet` — `ADR-0101` §1's `toCall` character for character, and the engine's own term recovered rather than a number manufactured. The human's frame reads `Call 200`. `Bet`, `Raise to` and `All in` keep totals; `ADR-0109`'s mark **goes bare on `Call`**, amending its §2 in one clause, because `PlayerCalled` carries only `to` and the price is unrecoverable a tick later. **No architect `DEC`** — `ActionBar.tsx:255` already computes the difference for the sizing base, so nothing moves the wire and `STORY-1414` waits on nothing. `duel-table-states.html:298`'s `Call 800` is the one card node in arrears, and `ActionBar.test.tsx`'s seventeen zero-commitment fixtures are why the repair needs a hand where the seats' commitments differ |
| `DEC-129` | [`ADR-0119`](../../docs/adr/ADR-0119-the-name-is-asked-at-the-first-press-and-skipping-plays.md) | **The name is asked at the player's own first press**, and skipping plays. A player holding no name meets the ask at their own press of *Create a duel room* or *Join the duel* — never on the invite path, never before a rematch, never on a resume — because the vision's own success condition is about the **invited** player, and she presses nothing: `main.tsx` reads the code from the URL and `boot.ts` sends `JoinRoom` on `Welcome`, outside React. The suggestion is the field's initial value and **promises nothing about availability** (`ADR-0029` §1 makes freedom a database fact and §5 refuses the endpoint that would answer it), and **nothing is written unless the player takes it** — `ADR-0051` §1 lets no string leave `name_registry`, so an auto-accepted suggestion would burn one name per browser that reached the screen. Deliberately **not a modal**: `DEC-138` owns whether this product gets one. Two costs named rather than hidden — the invited rival is not asked before her first duel, and the host meets one interstitial in a product whose positioning sentence is *fast*. For item 1a: the ask is a screen in place of the front door, and `STORY-1407`'s last blocker, `DEC-142`, is **answered on 2026-09-07** by [`ADR-0137`](../../docs/adr/ADR-0137-a-name-suggestion-is-drawn-in-the-browser.md) |
| `DEC-133` | [`ADR-0120`](../../docs/adr/ADR-0120-a-showdown-shows-the-hands-the-rules-showed-and-the-beat-that-shows-them-stands.md) | **A showdown shows the hands the rules showed, and the beat that shows them stands.** What is shown is unchanged — the winner's hand, both on a split, no hole card on a fold — with **no post-hand disclosure**, `ADR-0008` whole, **no wire move**, and `poker-server` out of item 2a altogether. What changes is the **pacing**: the last beat of a hand that turned a hand face up the viewer had not seen stands **2,000 ms**, every other ending keeping `ADR-0102` §4's 600 ms, the hold ending when the next hand's frames are applied and never on a fade. No panel, no overlay, **no new string**, no *mucked* line; faces come from the snapshot, never from `HandRevealed`. It registers **`DEC-146`** for the architect, corrects this epic's *"drawn by nothing"* measurement, and decides nothing about `DEC-134`, item 2c or `DEC-136` |
| `DEC-110` | [`ADR-0133`](../../docs/adr/ADR-0133-the-room-a-player-holds-is-scanned-for.md) | **The room a player holds is scanned for, and only the `PLAYING` refusal spends a frame.** The architect's, open since 2026-09-01 and widened here by `ADR-0124` §6; the last thing `STORY-1416` waited on. One lookup — `RoomRegistry.heldRoom(player)`, non-suspending, returning the whole `Room`, *held* meaning a seat in a `WAITING` or `PLAYING` room and **the method, not its callers**, excluding the other two — under a **total order** so no answer depends on hash iteration order. **A scan, not an index**, taking **no lock at all**: it reads the `@Volatile` `Holder.room` that `get(code)` and `reap`'s unlocked pass already read, so the rooms stay the one truth about where a player sits and the swap to an index later is a one-body change. Opening becomes `heldOrOpen` — `heldRoom ?: create` under a **per-player striped `Mutex`** — which is what makes `ADR-0124` §1's *one waiting room* true rather than usually true. The deadlock question is closed by construction: one lock, no suspension point inside it, *a player stripe is always outermost*, and nothing inside a room's critical section can take a lock because `mutate`'s blocks are not `suspend`. **Here**: `STORY-1416` builds the lookup, the stripes, `heldOrOpen` and `replyToCreateRoom`'s `WAITING` branch, sends the seat from `Room.seatOf` and **never the literal `0`**, changes **no client file**, moves **no wire**, is **not `atomic:`** — and **must leave the `PLAYING` case falling through to `create`**, because handing that room back silently would ship `ADR-0105` §1's refusal without §4's words. The `PLAYING` half is `ADR-0105` §1's own ticket, outside this epic: it spends `ALREADY_IN_DUEL`, a `PROTOCOL_VERSION` step and `ADR-0047`'s lock, which is the one place this ADR **disagrees** with `ADR-0124` §6's prediction — and `ADR-0124` scoped that prediction to its own half, so nothing is contradicted. Registers nothing |

### What the answers will hand the architect

Named here rather than registered, following
[`ADR-0105`](../../docs/adr/ADR-0105-one-duel-at-a-time-and-the-refusal-hands-back-the-duel.md) §6 —
*a `DEC` nobody is working is noise in the open table*. These are certain if their product question
answers one way, and each is registered by the ADR that answers it:

- **~~The rename's mechanism~~ — struck. Registered as `DEC-151` by
  [`ADR-0130`](../../docs/adr/ADR-0130-a-name-can-be-changed-and-the-name-it-leaves-is-spent.md) and
  **answered** on 2026-09-07 by
  [`ADR-0134`](../../docs/adr/ADR-0134-a-rename-spends-before-it-replaces.md).** Two of the three
  things named here were **answered rather than handed over**: a freed name is **not** takeable (§2
  retires it) and `ADR-0038`'s screening applies to the second name exactly as to the first (§1).
  The rest is now settled — the trigger becomes `player_display_name_never_released` with `ADR-0051`
  §3's body minus one disjunct, the write is four statements under `SELECT … FOR UPDATE`, the
  vacated row moves `TAKEN → REPLACED` with `retired_from` set, `PUT /api/me/name` loses `403` and
  gains `429`, and **yes**, the write is budgeted, keyed by `PlayerId` and metering spending rather
  than trying.
- **The new profile's mechanism** — registered as **`DEC-152`** by
  [`ADR-0131`](../../docs/adr/ADR-0131-signing-out-of-your-own-account-hands-the-browser-a-new-profile.md)
  §7, and **answered on 2026-09-07** by
  [`ADR-0135`](../../docs/adr/ADR-0135-the-server-says-which-sign-out-this-is-and-the-browser-forgets-one-key.md). Two of the three things this epic expected it to weigh are
  **answered rather than handed over**: `ADR-0049`'s finality means *nothing* here, because a
  sign-out is not a revocation and §3 keeps `ADR-0049` §2's statement out of the path entirely; and
  `ADR-0050`'s *signs out everywhere but here* is about the revoke button, which this path does not
  press. What is handed over is narrower — how a browser stops owning its profile at one confirmed
  press and nowhere else, and how the **confirmation** learns which of the two sign-outs it is
  before it acts, which the client cannot compute (`DuelSocket.kt:168` answers a session handshake
  with `deviceId = null`). Both are now settled: the browser forgets `pd.deviceId` and nothing else,
  from `signOut` and nowhere else, and a new session-required `GET /api/me/device` answers
  `signOutHandsANewProfile` before the press. **No schema, no wire, no `PROTOCOL_VERSION`** — so the
  implementing ticket is a plain ticket, not `atomic:`.
- **~~The suggestion's source~~ — answered 2026-09-07** by
  [`ADR-0137`](../../docs/adr/ADR-0137-a-name-suggestion-is-drawn-in-the-browser.md), which registered as
  `DEC-142` under `ADR-0119` §4. The note as written here was wrong in one word and the answer corrects it:
  a suggested name is **not** required to be free when it is suggested, because nothing may ask — `ADR-0029`
  §5 refuses the endpoint, `ADR-0051` §9 refuses the enumeration surface, and `ADR-0119` §5 forbids
  pre-registering one. **Rarity is bought with the size of the vocabulary instead**, at a tested floor of
  `1_000_000` combinations, drawn client-side by a pure function that touches no registry and no wire; and
  the one refusal a mechanism *can* eliminate — a `400` on a string the product itself offered — is
  eliminated by testing every word in the list.
- **~~The winning five's path~~ — struck. Item 2 hands the architect nothing, and this epic
  contains no wire move at all.** `DEC-133` is answered by
  [`ADR-0120`](../../docs/adr/ADR-0120-a-showdown-shows-the-hands-the-rules-showed-and-the-beat-that-shows-them-stands.md),
  which moves no wire and hands over `DEC-146` instead — one beat, a different length, and it too
  is **answered on 2026-09-07**, by
  [`ADR-0136`](../../docs/adr/ADR-0136-a-beat-declares-its-own-length-and-zero-silences-every-beat.md):
  the beat carries its **kind**, the store owns both lengths, `stepMillis === 0` silences every beat,
  and **nothing crosses the socket** here either. `DEC-134` is answered **no** by
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
`DEC-132`, because each later one is written against the screen the earlier one leaves behind. All
four are now answered; the chain's remaining order is a matter of the screens, not the decisions. The
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
| ~~`STORY-1406`~~ | ~~The bar stands disabled where it printed a sentence~~ — **retired** by [`ADR-0127`](../../docs/adr/ADR-0127-a-control-stands-only-for-a-decision-the-server-has-opened.md): its title states the opposite of what was decided, and there is no work left in it | — |
| `STORY-1407` | A player names themselves before their first duel — *item 1a; the product's first dialog, and by [`ADR-0119`](../../docs/adr/ADR-0119-the-name-is-asked-at-the-first-press-and-skipping-plays.md) §1 a screen rather than a modal* | ~~`DEC-142`~~ — **answered 2026-09-07** by [`ADR-0137`](../../docs/adr/ADR-0137-a-name-suggestion-is-drawn-in-the-browser.md); **nothing blocks it**, `DEC-129` is answered, and its tickets are **not** `atomic:` |
| `STORY-1408` | The account screen says what an anonymous account is, and offers the promotion — *item 1b; the post-win offer and the profile strip's line go with it* | nothing — `DEC-130` → `ADR-0125`, `DEC-147` → [`ADR-0132`](../../docs/adr/ADR-0132-the-profile-says-whether-it-holds-a-password.md). **Written and split into fourteen tickets on 2026-09-08**: the four deletion tickets are startable immediately (`ADR-0125` §§1, 2, 5, 6 need no new fact), three tickets are `atomic:` and probed to `exit 0`, and `ADR-0132` §7's *one diff* is honoured by ordering server → document → client, on the ADR's own asymmetry. The split raised **no decision**, and named `ADR-0132` §Residuals' `hasRecoveryEmail` literal as an owed defect ticket that is deliberately **not** in this story |
| `STORY-1409` | A name can be changed in account settings — *item 1c; [`ADR-0130`](../../docs/adr/ADR-0130-a-name-can-be-changed-and-the-name-it-leaves-is-spent.md) is the product answer, [`ADR-0134`](../../docs/adr/ADR-0134-a-rename-spends-before-it-replaces.md) the mechanism* | nothing — **both halves answered**: the name given up is **retired**, history prints the name as it **is**, the form is **one** form on the **account screen** and `PERMANENCE_LINE` leaves the product (`ADR-0130`); the trigger becomes `player_display_name_never_released`, the write is four statements under `SELECT … FOR UPDATE`, the vacated row moves `TAKEN → REPLACED` with `retired_from` set, `403` leaves and `429` arrives, and the write is budgeted (`ADR-0134`); **written and split into eighteen tickets on 2026-09-08**, and the split **checked** `ADR-0134` §7's ordering rather than trusting it — the migration alone reddens three merged tests, all of them about the **old** trigger's name, message or refusal, and no shipping writer of `player.display_name` moves, so it is behaviour-preserving for the product and not for the suite, and one merged assertion **inverts** and is rewritten inside the migration's own ticket. Five `atomic:` tickets, each probed to `exit 0`; the budget needed **no new machinery**, only `AttemptBudget`'s shipped `refund` on the other branch; `PERMANENCE_LINE` leaves in three **plain** tickets after its blast radius was measured at four client files and no unrelated test; and the story **serialises against `STORY-1407`, `STORY-1408` and `STORY-1410`**, which `STORY-1408`'s own note had only half right. **No decision was raised** |
| `STORY-1410` | Signing out hands the browser a new anonymous profile, and the confirmation says so — *item 1d, and the proof that a promotion moves no coin; [`ADR-0131`](../../docs/adr/ADR-0131-signing-out-of-your-own-account-hands-the-browser-a-new-profile.md) is the answer, and `SIGN_OUT_WARNING`'s second sentence moves in the same diff as the behaviour* | — `DEC-152` answered 2026-09-07 by [`ADR-0135`](../../docs/adr/ADR-0135-the-server-says-which-sign-out-this-is-and-the-browser-forgets-one-key.md): one session-required `GET /api/me/device`, one `forgetDeviceId` call in `signOut`, the new profile from the next `Hello`, and **nothing on the wire**; **written and split into fourteen tickets on 2026-09-08**, and the split **discharged the serialisation rather than inheriting it**: `DEC-147` is answered by `ADR-0132` and `TASK-140808`/`TASK-140809` have landed `hasPassword` in both contested files, while this story opens neither — a **new** `### Device standing` section and a **new** client module, because `ADR-0135` §Alternatives 1 refused a seventh `ProfileResponse` field on a measured fact. **Two `atomic:` tickets, both probed to `exit 0`** — `signOut`'s required field (5) and `AccountCalls.signOut`'s parameter (4, including `account-provider.test.tsx:198`, which a reading would have missed) — and **three groupings that looked atomic are not**, each falsified by probing. **The abandoning branch is unreachable until the last ticket**, because every intermediate seam passes a literal `false`, so behaviour and copy become true in one merge. `SIGN_OUT_WARNING`'s **value does not move**: `ADR-0131` §5 keeps the returning sentence, and what was false was saying it unconditionally — which is why `claimed-here-recovered-there.test.tsx` and `account-server.ts` are measured **out** of the story, that arc being `ADR-0131` §1 row two. **No decision was raised** |
| `STORY-1411` | The showdown shows the hands it reached, and stands long enough to read — *item 2a; a card, a hold, and tests that pin a drawing which already exists* | nothing — `DEC-146` answered 2026-09-07 by [`ADR-0136`](../../docs/adr/ADR-0136-a-beat-declares-its-own-length-and-zero-silences-every-beat.md): one union field on `RevealStep`, one predicate over the hand-completing view, `REVEAL_READ_MS` at the boot seam and one map in `armTick`; **no e2e file edited or re-recorded, no CSS, nothing on the wire** — a plain ticket, not `atomic:` |
| `STORY-1412` | No card on the table is marked, and a gate says so — *item 2b, answered **no** by [`ADR-0126`](../../docs/adr/ADR-0126-the-table-shows-the-cards-and-marks-none-of-them.md): no wire move, no `atomic:` ticket* | nothing — **written and split into two tickets on 2026-09-08**, exactly what `ADR-0126` §4 leaves: one gate and one line on the showdown card. **Where the gate lives was decided rather than assumed** — `ADR-0142` §1 restates `ADR-0091` §4 as a constraint, so a shell gate under `design/` may not read `web-client/`, and it becomes a vitest file beside `no-derivation.test.tsx` that `vitest run` globs into existence with **no workflow edited**. `design/check-frame-cards.sh` reads one card and **does** run in CI (`tickets.yml`, `TASK-140719`). Measured at `c6a41e6d`: **5** frames, **38** `class="pc`, **4** `back mucked`, **2** mentions of `ADR-0126` — and the frame-scoped `awk` is red on exactly the two frames that owe the line and green on the two that carry it. The gate is shown red against the defect it names by **two `verify:` mutations of `PlayingCard.tsx` itself**, both run here and both restoring the file. **No decision was raised** |
| `STORY-1413` | The pot travels to the winner — *item 2c; a card and a client story, `ADR-0115` and `ADR-0102` govern it* | `STORY-1412` |
| `STORY-1414` | `Call` says what it costs — *item 4; one label, and the mark goes bare on `Call`* | nothing — [`ADR-0122`](../../docs/adr/ADR-0122-call-names-the-price-and-raise-to-names-the-total.md) |
| [`STORY-1415`](../stories/STORY-1415-the-rematch-offer-finds-the-rival-wherever-they-are.md) | The rematch offer finds the rival wherever they are — *item 5; a panel, never a modal ([`ADR-0123`](../../docs/adr/ADR-0123-a-standing-rematch-offer-follows-the-rival.md) §2), mounted beside the lobby ([`ADR-0138`](../../docs/adr/ADR-0138-the-panel-mounts-beside-the-lobby-and-the-dismissal-lives-in-the-mount.md)). **Written and split into eleven tickets on 2026-09-09**, the first two its minting card* | nothing — the card is the first ticket, and `DEC-158`, raised by the split, blocks none of them |
| `STORY-1416` | Play again returns the host to the room they still hold — *item 6; strikes `DEC-111` with `DEC-139`* | **split on 2026-09-07 into four tickets, server only**; `DEC-139`/`DEC-111` by [`ADR-0124`](../../docs/adr/ADR-0124-one-waiting-room-and-play-again-hands-it-back.md), `DEC-110` by [`ADR-0133`](../../docs/adr/ADR-0133-the-room-a-player-holds-is-scanned-for.md) §9: build `heldRoom`, the stripes, `heldOrOpen` and the `WAITING` branch; **no wire, not `atomic:`**, and the `PLAYING` case still falls through to `create` — that half is `ADR-0105` §1's own ticket, outside this epic, and it **is** `atomic:`, because `ALREADY_IN_DUEL` moves `ADR-0047` §2's fingerprint and forces a `PROTOCOL_VERSION` step |

## Definition of done

- [x] `DEC-129`–`DEC-139` and `DEC-146` are answered by merged ADRs, and every ADR that supersedes
      a merged one says which clause of which ADR it replaces. **All eleven of `DEC-129`–`DEC-139`
      are answered** — `DEC-131` by
      [`ADR-0130`](../../docs/adr/ADR-0130-a-name-can-be-changed-and-the-name-it-leaves-is-spent.md),
      which names `ADR-0029` §4, its title's *permanent* and §5's `403`, `ADR-0051` §3 and
      `ADR-0119` §3's obligation 2, and `DEC-132` by
      [`ADR-0131`](../../docs/adr/ADR-0131-signing-out-of-your-own-account-hands-the-browser-a-new-profile.md),
      which names `ADR-0030` §3 and §8. **`DEC-146` is answered on 2026-09-07** by
      [`ADR-0136`](../../docs/adr/ADR-0136-a-beat-declares-its-own-length-and-zero-silences-every-beat.md),
      which supersedes nothing: `ADR-0102` §4's boot seam is **extended** by a second constant and
      its *zero means synchronous* widened from a step to every beat, with §§1–3 and §5 untouched.
      **This line is met**; the ADR merges before `STORY-1411`'s hold is implemented.
- [x] `DEC-151` — registered by `ADR-0130` — is answered by a merged ADR before `STORY-1409` ships;
      the namespace has no brake until it lands. **Answered** by
      [`ADR-0134`](../../docs/adr/ADR-0134-a-rename-spends-before-it-replaces.md) on 2026-09-07, and
      the brake is in it: §6 budgets the write, keyed by `PlayerId`, metering the writes that
      **spend** a string rather than the ones that fail.
- [x] `DEC-152` — registered by `ADR-0131` §7 — is answered by a merged ADR before `STORY-1410`'s
      implementing ticket is startable. Its design card and its copy wait on nothing.
      **Answered** — [`ADR-0135`](../../docs/adr/ADR-0135-the-server-says-which-sign-out-this-is-and-the-browser-forgets-one-key.md):
      one route, one comparison, one storage key, one prop, and nothing on the socket.
- [x] `DEC-144` — registered by `ADR-0123` §9 — is answered by a merged ADR before `STORY-1415`'s
      implementing ticket starts: [`ADR-0138`](../../docs/adr/ADR-0138-the-panel-mounts-beside-the-lobby-and-the-dismissal-lives-in-the-mount.md), 2026-09-07.
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

### The 390 × 664 reading

`ADR-0121` §3's two numbers, taken under §2 at the shape §4 fixes. `ADR-0089` §2 keeps this out of
every `verify:` block, so it is recorded rather than gated.

- **Taken:** 2026-09-07 by TASK-140501, on the running stack, two Chrome profiles at `size 390 664`
- **Commit:** `72421c7da1bb8456be9b9ff5342584b838b3fdbd`
- **Runner:** `node scripts/qa/drive.mjs <port> size 390 664` then `… eval '<the expression above>'`,
  profile `A` on port `9232`, profile `B` on port `9233`, each freshly cleared
  (`localStorage.clear()`) before B1 so the reading is a first-time device rather than one carrying
  an earlier session's profile — see the note below the table

| beat | `scrollHeight` / `clientHeight` | `scrollWidth` / `clientWidth` | worst plate overhang |
| --- | --- | --- | --- |
| B1 the front door | 664 / 664 | 390 / 390 | — (no plate on this screen) |
| B2 hand 1, first decision, no mark on either plate | 664 / 664 | 390 / 390 | −21 (both tabs, every plate) |
| B3 the same hand after the first raise | 664 / 664 | 414 / 390 | +24.3 (B's tab, the `Your rival` plate — A's seat, carrying `Raise to 200`, `D`, `Timebank 3:00` and the stack together) |
| B3 after `TASK-140502` — **predicted, not taken** | *664 / 664* | *390 / 390* | *−14.69* — see the note below |

**The fourth row is a prediction, not a reading.** Every other row in this table was *taken* on the
date above; that one is what `TASK-140502`'s change is expected to produce, carried from the ticket.
It was **not measured**: the `eval` verb `scripts/qa/drive.mjs` needs to read `scrollWidth` and
`clientWidth` is refused by the dispatched agent's own command classifier, so an eight-hour attempt
reached no number at all. The after-reading is therefore **owed**, and `EPIC-14`'s Definition of done
still carries it unticked. Nobody should cite −14.69 as observed.

B3 reproduces the defect: `scrollWidth` (414) exceeds `clientWidth` (390) and the worst plate's
`overhang` is positive. It reproduced on the **non-raiser's** tab (B, watching A's plate as `Your
rival`), not on the raiser's own tab (A's own reading at that beat was 390 / 390, overhang −14.56) —
the two tabs disagree at this beat and the worse number is recorded, per the instructions above. The
figure (+24.3) sits just under the planner's predicted band of +25 to +55; the reproduction stands
but at the narrow edge of it, and is not adjusted to fit.

**The device reading is the human's** (`ADR-0121` §4, `ADR-0088`): no CI job and no agent owns an
iPhone. It is taken on the phone in the photograph, in Safari, with the URL bar and tab bar fully
expanded, and it may correct 390 × 664 **downward on either axis** and may never raise it.

| device · browser · beat | judged height | judged width | fits |
| --- | --- | --- | --- |
| _to be filled by the human_ | | | |

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
