# ADR-0085 — *"Not again"* is this browser, and an answer is what spends the offer

- **Status:** Accepted
- **Date:** 2026-08-27
- **Resolves:** `DEC-079` — is [`ADR-0036`](ADR-0036-an-account-is-offered-never-required.md)'s
  *"not again"* a fact about the **player** or about **this browser**, and what **spends** the
  offer? **Derived from the vision, not chosen.** The roadmap's `v0.1` row — *"Two browsers, one
  room link, one complete duel, rematch. **No accounts.**"* — is the vision counting a player as a
  browser at exactly the stage this offer addresses, since the offer is shown only to a player who
  holds no credential. The *Positioning* sentence — *"Dark, quiet, fast, minimal"*, with a
  vocabulary that is *"challenge, duel, rematch, rival, streak, season — never buy-in, bankroll,
  jackpot, bonus"* — is what settles the second half against a prompt that comes back after the
  player has answered it
- **Amends** [`ADR-0036`](ADR-0036-an-account-is-offered-never-required.md) §Consequences' third
  bullet on one clause — *"cannot live in `localStorage` alone… **It belongs on the profile**"* —
  because §Context below measures that its stated reason does not hold on the shipped client. Its
  §Decision is **untouched**: the offer is still made after a first win, still dismissible, still
  permanent, still never required, and that half was the human's call in `DEC-025`
- **Amends** [`ADR-0056`](ADR-0056-a-throttled-sign-up-says-so-and-keeps-what-was-typed.md) §5's
  parenthetical *"by the player choosing 'Not now' **and by nothing else**"*, and the `STORY-0415`
  line in its §6. §5's holding stands byte-unchanged: **a `429` consumes no handle, no session, no
  profile, and dismisses nothing**
- **Constrains:** `STORY-0415`'s three unwritten tickets — the persistence, the `Lobby` wiring and
  the whole-client arc — and narrows `DEC-080` to two questions (§7)
- **No wire change, no schema change, no new endpoint.** `PROTOCOL_VERSION` does not move and no
  migration is added

## Context

`STORY-0415` applies `ADR-0036`. Two merged documents describe where its dismissal lives, and only
one of them can be built.

**`ADR-0036` §Consequences:** *"Dismissal is state that must survive. 'Permanently dismissed' cannot
live in `localStorage` alone, because the device* is *the identity for an anonymous player and
clearing storage would resurrect the prompt forever. It belongs on the profile."*

**`STORY-0415` §Design notes:** *"it is stored under a key this module owns, the way `TASK-030304`
and `TASK-031001` each own exactly one"*, asserted *"through the injected storage"*.

The first follows the player to every device and survives a storage clear. The second does neither.
Four forces decide between them, and three of the four are measurements rather than preferences.

**`ADR-0036`'s stated reason does not hold on the shipped client.** The prompt is not what a storage
clear resurrects. `pd.deviceId` lives in the same bucket:
[`device-id.ts`](../../web-client/src/protocol/device-id.ts) owns that key, and `main.tsx` injects
the real `localStorage` into every one of its callers. Clearing site data therefore takes the device
id with it, so
[`ADR-0049`](ADR-0049-a-device-binding-is-a-row-and-revoking-is-final.md) §4's `resolve` finds no
live binding and mints a *fresh, empty profile*. The player is not re-prompted; the player is gone,
and their coins with them. The browser that comes back holds zero coins and no wins, so it sees no
offer at all until it wins a duel — and when it does, it has a new coin genuinely at risk and has
never been offered anything. The harm in that scenario is real and it is the one the offer exists to
warn about, but it is not the harm the bullet named, and no storage location repairs it.

**For every player who can see this offer, the two answers are indistinguishable.** `ADR-0036` shows
it only to a player holding no credential. `ADR-0049` §1's `device_binding_live_player` index fixes
**at most one live binding per player**, so an anonymous profile is exactly one browser. The only
way one player reaches a second browser is by signing in — which requires the credential that
switches the offer off. A profile-borne flag would differ from a browser-borne one only for players
who never see the prompt either way.

**Two of the predicate's three terms are already browser-local, and `TASK-041502` has shipped their
shape.** `offerAccount` takes `verdict` (read off the server's `DuelOutcome`), `signedIn`
(*"whether this browser holds a session token"*) and `settled`. Making the third term travel while
the second does not gives one predicate two different ideas of who the player is, and the cases
where they disagree are exactly the cases nobody writes a test for.

**The other direction has to be built at the one moment the player has no account.** A profile-borne
flag needs a column, a field on `GET /api/me`, and an endpoint that accepts *"no thanks"* from an
anonymous device — a server half for a `module: web-client` story, to record a bit that nothing else
reads, about a profile that is one cleared browser away from not existing.

### The second half, and why neither merged sentence can simply be followed

`DEC-049`/`ADR-0056` §5 and `STORY-0415`'s third criterion give two different spending rules, and
each one, taken literally, produces a behaviour the other half of the record forbids.

- **Only *"Not now"* spends it** means the prompt returns after every win, forever, to a player who
  took the offer and did not finish the form. That is `ADR-0036` §Decision's *"reminder badge that
  never goes away"* in a different shape, aimed at the players most invested in the product.
- **Being shown spends it** means a player who taps *Rematch* without reading the prompt has spent
  it on a glance. The offer's purpose is to *tell* them the coins are device-bound; `ADR-0036`
  §Alternatives rejected saying nothing because *"a player who has never been told their coins are
  device-bound learns it by losing them"*, and spending the telling on an unread render reaches the
  same outcome by a slower route.

### The deadline

Nothing is hosted — `EPIC-07` runs no server — so a column and a `GET /api/me` field are cheap to
**add** today and, per `ADR-0049` §*The deadline, honestly*, expensive to **remove** once a process
is running against the schema. The asymmetry runs one way: the browser-local answer can become the
profile-borne one additively, on the day there is evidence for it; the reverse is a shipped field
that has to be taken out. The reason to settle it now is that the three unwritten tickets cannot be
written until it is settled, not that either answer expires.

## Decision

### 1. *"Not again"* is a fact about **this browser**

The bit that says the offer has been answered lives in this browser's storage, written and read
**through the injected `Storage`**, beside `pd.deviceId` and `pd.sessionToken`. It is **never sent
to the server**: no column records it, no field on `GET /api/me` carries it, no endpoint accepts it,
and no request is made when the player answers.

The key's name, the module that owns it, and the third entry it adds to
`web-client/src/protocol/one-module-owns-each-storage-key.test.ts` are `DEC-080`'s. This ADR fixes
only that the fact **does not travel** — everything else about how it is stored is the architect's.

### 2. An **answer** spends the offer, and nothing else does

- **Both controls are answers.** Taking the offer spends it, and *"Not now"* spends it. Either one
  sets the bit, and both are permanent.
- **Nothing else sets it.** Not a `429`. Not a failed, refused or abandoned sign-up. Not the passage
  of time, not a rematch, not a reload, not a later win, and not the prompt merely having been
  rendered.
- **An offer that was rendered and never answered is not spent.** A player who hits *Rematch*
  straight past it, or closes the tab, has answered nothing, and the offer is made again after their
  next win.
- **Nothing in the product ever clears the bit.** There is no un-dismiss, no setting, no *"offer it
  again"*, and no support path. The only way back to an account is the account screen's own address
  and the control the lobby already renders for it.

`ADR-0056` §5 is preserved exactly, and reads more simply than before: a `429` spends nothing. What
spent the offer, for a player who reaches the form at all, was their own answer a moment earlier.

### 3. What a player sees — the whole rule, case by case

| The player | What they see |
| --- | --- |
| Wins a duel, holds no credential, has answered nothing | **The offer**, on the result screen, with two controls |
| Wins again, having pressed *Not now* | **Nothing.** No offer, no badge, no reminder, on any screen |
| Wins again, having taken the offer and never signed up — including after a `429` | **Nothing on the result screen.** The account screen keeps its own address and the lobby's control still opens it |
| Wins again, having been shown the offer and pressed neither control | **The offer again** |
| Loses or draws, whatever the bit says | **Nothing** |
| Holds a credential in this browser | **Nothing, ever** |
| Wins a first duel in a **new browser**, having dismissed the offer in another | **The offer** — that browser is a different profile with a different coin, and it appears only once that profile wins, not on arrival |
| Returns after clearing site data | The same as a new browser — and the profile, the coins and the leaderboard place are gone too, which is a larger loss than the prompt |

### 4. What the dismissal survives

It survives everything `localStorage` survives in this browser: a reload, a new tab, closing and
reopening the browser, a rematch, a losing streak, a season boundary, and any number of later wins.

It does not survive clearing site data, and it does not reach another browser, another device or
another profile. Nothing in the product tries to make it.

### 5. The trigger needs no *"first win"* fact from the server

Under §2 the offer is made on **a win this browser has not answered for**, which for every player
who has never answered *is* their first win. `ADR-0036`'s *"the trigger is the first duel won, not
the first duel played"* is honoured — the contrast it draws is *won versus played*, and the first
win that gets an answer is the last one that asks.

So the product does not require the server to say which win was the first, and
`offerAccount(verdict, signedIn, settled)` as `TASK-041502` already writes it is the complete rule.

### 6. What is deliberately not built

- No *"remind me later"*, no snooze, no expiry, no count of how many times the prompt was shown.
- No badge, no lobby banner, no second prompt on any screen.
- No un-dismiss, and no way for a player to ask to be offered again.
- No attempt to recognise the same human across two browsers, and no sync of the bit between them.
- No copy change — the words are `TASK-041501`'s and this ADR authors none.

### 7. What the backlog gains, exactly

- **`STORY-0415`'s third acceptance criterion needs one clause.** *"It does not appear a second time
  after a second win"* becomes *"…to a player who answered it"*, and the unanswered row of §3's
  table becomes its own criterion.
- **`ADR-0056` §6's `STORY-0415` line** becomes: a `429` is not a dismissal, and the sign-up the
  player accepted is still there with what they typed — not that the result-screen prompt returns.
- **`DEC-080` is narrowed to two questions**: which key and which module, and therefore the third
  entry in the one-module gate. Its wire, column and endpoint branches are closed by §1, and its
  *what says this win is the first* branch by §5.
- **`TASK-041501`–`TASK-041504` keep every `Files` row, test and `verify:` line.** `settled` now has
  a source and `offerAccount`'s three terms are exactly right. Four sentences in them say `DEC-079`
  is *open* and are stale rather than wrong — `TASK-041501` §Out of scope, `TASK-041502` §Scope and
  §Out of scope and its closing note, `TASK-041503` §Out of scope, `TASK-041504` §Out of scope. They
  are the planner's to fold in, and **each refusal stands**: reading `settled` from anywhere is
  still the persistence ticket's work and not `TASK-041502`'s, whatever `DEC-079` now says.

## Consequences

**What it buys.** `STORY-0415` stays a client story and needs no server half, so its three unwritten
tickets can be written from §3's table today. All three terms of the offer's predicate now mean the
same thing by the same measure — this browser — so there is no state in which two of them disagree
about who the player is. `ADR-0036`'s promise is kept in the only place it can be kept at the moment
it is made, which is a browser holding no account. And the answer is the reversible one: the profile
column is additive whenever there is evidence for it, and a shipped field is not un-shippable.

**What it costs.**

- **A player who accepts and abandons is never asked again, and that is the sharpest cost here.**
  The player who pressed *Create an account* is the one most likely to finish, and this rule stops
  re-asking them. The only remaining door is the lobby's account control, which nothing on the
  result screen points at. If the product later measures that this loses accounts, the reversal has
  to be an ADR that supersedes this one — which is the point of writing it as a rule.
- **The offer reappears on a second browser, and the product cannot tell it is the same human.**
  Dismiss it on the laptop, and the phone will offer it after that browser's first win. Each showing
  names a real, different coin, so the behaviour is correct — and the human still experiences a
  product that asked twice after being told *not again*, with no mechanism that could know.
- **A mis-tap spends it.** *Not now* sits beside *Rematch* on the result screen. A player who meant
  one and hit the other is never offered again, and §2 deliberately provides no way back.
- **A player who hit *Rematch* past the offer is offered it again**, and may read the second showing
  as being ignored. One bit cannot tell *read it and moved on* from *never saw it*, and this ADR
  takes the side that risks asking twice over the side that risks never telling them.
- **`ADR-0036` §Consequences is amended on a point it argued for**, on a measurement rather than a
  change of mind. A reader who finds `ADR-0036` alone now gets the wrong answer about storage — the
  standing cost of every amendment, paid again here.
- **One more key in this browser's storage.** One more thing a privacy-minded clear removes, one
  more entry the one-module gate carries, and one more literal that has to stay a single-line string
  for that gate to see it.
- **`signedIn` means *this browser holds a session token*, and this ADR does not change that.** A
  player who signed up from the lobby without ever being offered, then signs out on that browser,
  then wins, is offered an account they already hold. The bit does not stop it, because they never
  answered a prompt. Named, not solved — it belongs to whatever revisits what `signedIn` means.

**What it forecloses.**

- **A dismissal that follows a player across devices.** Not available without an account, and the
  offer is made precisely to players who do not have one.
- **Anything server-side that wants to know whether a player was ever offered an account.** Nothing
  records it: no conversion funnel, no *"asked and declined"* metric, no later prompt targeted at
  the never-offered. This is named because it is the first thing a growth argument will ask for, and
  it should cost an ADR rather than a column somebody adds quietly.
- **Reconstructing the dismissal after a storage clear.** There is nothing to reconstruct it from,
  and there would not have been under the profile-borne shape either, since the profile is
  unreachable by then.

It does **not** foreclose moving the fact to the profile later — a column, a field, a write on each
of the two controls, and the browser key becomes a cache or is deleted — nor any of `DEC-080`'s
remaining freedom over which key and which module.

## Alternatives considered

**The flag on the profile, as `ADR-0036` §Consequences instructs.** The strongest case in the file,
and it is a real one: it is the merged instruction, written by the ADR that owns this feature; it is
the only shape under which *not again* is a promise about a **player** rather than about a browser,
so a human with a laptop and a phone is asked once; it is the only shape that could ever answer
*"did this player decline an account?"*; and it puts the dismissal where the coins are, so the fact
and the thing it protects live or die together. Rejected on three grounds, any one of which would
have been enough. Its stated reason is not true of the shipped client — clearing storage takes
`pd.deviceId` with it, so what is resurrected is not the prompt but a fresh, empty profile, and the
old one is unreachable. It is behaviourally indistinguishable for every player who can ever see the
offer, because the offer requires *no credential* and `ADR-0049` §1 gives such a player exactly one
live browser. And it must be built at the single moment the product's whole position is that the
player has no account, turning a client story into a server one to store a bit nothing else reads.
Its case gets stronger the day the product has a server-side reason to ask the question — and on
that day the change is additive.

**Being shown spends it: one offer per player, ever.** The simplest rule available, the literal
reading of `STORY-0415`'s third criterion, and the strongest possible *not again* — the product asks
once, and no player can see the prompt twice under any circumstance. It also needs no judgement
about which control counts. Rejected because the prompt shares a screen with *Rematch*, and a player
who taps *Rematch* has read nothing: this rule spends the telling on a glance, and `ADR-0036`
§Alternatives rejected the silent option for exactly the outcome that produces — *"a player who has
never been told their coins are device-bound learns it by losing them"*.

**Only *"Not now"* spends it, as `ADR-0056` §5's parenthetical reads.** Honest and defensible:
*"Not now"* is the only control on which the player says anything about the future, taking the offer
says only *show me*, and it is merged text that this ADR would otherwise have to amend. Rejected
because it produces a prompt that returns after every win, forever, to the player who accepted and
did not finish — `ADR-0036` §Decision's *"reminder badge that never goes away"* in another shape,
aimed at the most engaged players. §5's substance survives untouched: the `429` spends nothing, and
what spent the offer was the player's own answer.

**A dismissal with an expiry — ask again next season, or after N more wins.** The commercial answer,
and its case is not weak: a player declining on their first win is protecting one coin, a player
with forty is protecting forty, so the stake the offer names grows and a cheap *no* might later be a
considered *yes*. Rejected because `ADR-0036` §Decision named this exact pressure in advance —
*"this is the half of the decision most likely to erode under a growth argument later, so it is
stated as a rule rather than as a default"* — and because a prompt on a timer is what the vision's
*Positioning* refuses: *"Dark, quiet, fast, minimal"*, in a vocabulary that has no word for a nudge.
If the product ever wants it, it takes an ADR that supersedes this one and says out loud that *not
again* now means *not for a while*.

**Leave both halves to whoever writes the fifth ticket.** The cheapest option, and it would have
shipped something. Rejected because the question is not a gap in the record but a **contradiction**
inside it: choosing in a ticket would decide which of two merged sentences is wrong, in a file
nobody re-reads, and both `ADR-0036` §Consequences and `ADR-0056` §5 would have gone on saying
something the code no longer does.

## What this does not settle

- **`DEC-080`**, narrowed by §7 to the key, the module and the gate entry. Still the architect's.
- **`signedIn`'s meaning**, and the signed-out-account-holder case in §Consequences. Not this ADR's,
  and not urgent: it needs a player who signed up without ever being offered.
- **The words the offer says**, which are `TASK-041501`'s. This ADR authors no player-facing string.
- **Whether anything ever measures the offer.** Nothing does, by §*What it forecloses*, and turning
  that around is a decision rather than an implementation detail.
