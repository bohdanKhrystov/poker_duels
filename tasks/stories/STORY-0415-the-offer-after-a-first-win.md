---
id: STORY-0415
title: The offer — an account after a first win, dismissed for good
type: story
status: ready
parent: EPIC-04
module: web-client
labels: [client, ui, auth]
depends_on: [STORY-0412]
---

## Goal

After a player wins their first duel, the client offers them an account — naming the coin they now
have to lose — and *"not now"* means not again.

## Why

[`ADR-0036`](../../docs/adr/ADR-0036-an-account-is-offered-never-required.md) answers `DEC-025`: an
account is **never required**, anonymous play stays fully ranked, and the one place identity is
raised is after a first win, because that is the first moment the player has something to protect.

## Design notes

- **The trigger is the first duel *won*, not the first duel played.** A player who has not won has
  nothing to protect and the prompt is noise.
- **Dismissal is permanent.** *Not now* means not again — stated as a rule rather than a default,
  because this is the half most likely to erode under a growth argument later. It survives a reload,
  which means it is stored, which means it is stored under a key this module owns, the way
  `TASK-030304` and `TASK-031001` each own exactly one.
- **It is an offer, not a gate.** Dismissing returns the player to exactly where they were with every
  capability intact: no reduced coin, no withheld leaderboard place, no badge that never goes away.
- **It names the actual stake** — the coin that exists and could be lost — rather than asking
  abstractly. That is `ADR-0036`'s wording and it is the reason the trigger is a win.
- **It opens `STORY-0412`'s screen** rather than growing a second sign-up form.
- The offer reads the win from the server's outcome, and derives nothing: it does not count duels
  itself, and it does not infer a win from a coin balance.

## Tasks

Split on 2026-08-27, and **partially**: four tickets, plus a fifth, sixth and seventh that were not
written because `DEC-079` and `DEC-080` decided their shape. **Both are now answered** — `DEC-079`
the same day, `DEC-080` on 2026-08-28 — see `## Answered decisions` below, so the three remaining
tickets are writable in full.

| ID | Title | Status |
| --- | --- | --- |
| [TASK-041501](../tasks/TASK-041501-the-words-the-offer-says.md) | The words the offer says, and the one word `ADR-0036` already chose | backlog |
| [TASK-041502](../tasks/TASK-041502-whether-the-offer-is-made-at-all.md) | Whether the offer is made — a win, no credential, and not already settled | backlog |
| [TASK-041503](../tasks/TASK-041503-the-offer-and-the-page-load-that-reaches-the-account-screen.md) | The offer itself, and the page load that reaches the account screen | backlog |
| [TASK-041504](../tasks/TASK-041504-the-result-screen-carries-an-offer-it-does-not-make.md) | The result screen carries an offer it does not make, and gives nothing up for it | backlog |
| — | *The persistence, the `Lobby` wiring, and the whole-client arc — not yet written, and no longer blocked: `DEC-079` is answered (`ADR-0085`) and `DEC-080` is answered (`ADR-0086`).* | — |

`TASK-041501` is the head; the other three depend on it for **sequencing only** and their `Files`
tables are pairwise disjoint, so all three are startable together once it merges.

## Answered decisions

**Neither is open. This story is blocked on nothing.**

- **`DEC-079` — the product owner's. Answered on 2026-08-27** by
  [`ADR-0085`](../../docs/adr/ADR-0085-not-again-is-this-browser-and-an-answer-spends-the-offer.md):
  ***"not again"* is this browser, and an answer is what spends the offer.** The bit is written and
  read through the injected `Storage` and **never sent** — no column, no `GET /api/me` field, no
  endpoint, no wire change — so this story keeps `module: web-client` and grows no server half.
  Both controls are answers and both are permanent; a `429`, an abandoned sign-up, a rematch and a
  reload spend nothing; **an offer shown but never answered is made again after the next win**. The
  trigger therefore needs no *first-win* fact from the server: `offerAccount`'s three terms are the
  whole rule. `ADR-0085` §3 is a case-by-case table of what a player sees, written to be read
  straight into a `## Tests` table.
- **`DEC-080` — the architect's, narrowed by that answer** to **which key and which module owns
  it**, and therefore the third entry `one-module-owns-each-storage-key.test.ts` gains.
  **Answered on 2026-08-28** by
  [`ADR-0086`](../../docs/adr/ADR-0086-the-offers-answer-is-one-key-owned-beside-the-predicate-it-feeds.md):
  the key is **`pd.accountOfferSettled`** and **`web-client/src/result/account-offer-settled.ts`**
  is the only production file that names the literal, exporting
  `ACCOUNT_OFFER_SETTLED_STORAGE_KEY`, `readOfferSettled(storage)` and `markOfferSettled(storage)`
  over the injected `Storage`. The stored value is the sentinel `"1"` and **anything unrecognised
  reads as *not settled***, which is `ADR-0085` §Consequences' own tie-break rather than a new one.
  **No clearing function is exported** and `signOut` is unchanged. The module sits in `result/`
  beside `account-offer.ts` and not in `protocol/`, because every caller is on the result screen and
  `protocol/`'s three keys are each a wire fact this one is not. The short name `pd.accountOffer` is
  **refused on a measurement**: the gate scans with `String.includes`, so a key that is a prefix of
  another returns two files for one row. `ADR-0086` §5 writes the third row out verbatim, §6 fixes
  that `markOfferSettled` runs from the accept anchor's click handler (`DuelResult`'s `onLeave`
  precedent) and never on the account screen's load, and §7 is the persistence ticket's file list.

Neither blocked `TASK-041501`–`TASK-041504`, which hold under either answer.

**Two edits this story owes its next planner, both named in `ADR-0085` §7 and neither made here:**
the third acceptance criterion below becomes *"It does not appear a second time after a second win
**to a player who answered it**"*, with the unanswered case — shown, neither control pressed, so
offered again after the next win — added as its own criterion; and `ADR-0056` §6's `STORY-0415` line
is restated as *a `429` is not a dismissal, and the sign-up the player accepted is still there with
what they typed*, rather than as the result-screen prompt returning.

## What the split measured, so the next pass need not re-derive it

- **The offer's accept control cannot be an in-page navigation.** `ADR-0076` §3 is enforced by an
  effect in `Lobby.tsx`: while `state.outcome !== null` — exactly when the result screen shows —
  any screen but `first` is replaced back to `/`. Measured: rendering `Lobby` over a store holding a
  `DuelFinished` and then setting `window.location.hash = "#/account"` leaves `hash=""`, no account
  screen, and *Victory* still on screen. The same navigation with no outcome reaches the account
  screen. So the accept control is `<a href="/#/account">`, a real page load — which `ADR-0076` §6
  requires of the result screen's links anyway.
- **A `hashchange` is a task, not a microtask.** A probe that sets `location.hash` inside a
  synchronous `act()` observes nothing; it must await a flush.
- `web-client/src/protocol/one-module-owns-each-storage-key.test.ts` is the merged gate a new
  storage key has to be added to — and `DEC-079` did land on client storage, so it gains a third
  entry, written out verbatim in `ADR-0086` §5. That gate scans production text with
  `String.includes`, which is why `ADR-0086` refuses a key that is a prefix of another, and it
  **must be edited in the same ticket that creates the module**: `pd.roomCode` has been in the
  client without a row since it was added, which is what *add the row next time* looks like.
- `web-client/src/virtual-time.test.ts` fails any test file that reaches for a timer without
  installing fake ones first.
- Suite at the split: **811 tests / 103 files**. The four tickets take it to **822 / 106**.

## Acceptance criteria

- [ ] The offer appears after a won duel and not after a lost or drawn one — all three asserted.
- [ ] It does not appear for a player who already holds a credential.
- [ ] It does not appear a second time after a second win.
- [ ] Dismissing it once suppresses it across a reload, asserted through the injected storage.
- [ ] Dismissing leaves every capability intact: the player can still play, still earns the coin, and
      nothing is disabled.
- [ ] Accepting it opens the account screen rather than a form of its own.
- [ ] The offer's trigger reads a server-sent outcome, and no test asserts it from a derived count.

## Out of scope

- Requiring an account for anything — forbidden by `ADR-0036`.
- The account screen itself — `STORY-0412`.
- Any second prompt, reminder or badge.
