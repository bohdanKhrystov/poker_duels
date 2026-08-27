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

Split on 2026-08-27, and **partially**: four tickets, plus a fifth, sixth and seventh that are not
written because `DEC-079` and `DEC-080` decide their shape. See `## Open decisions` below.

| ID | Title | Status |
| --- | --- | --- |
| [TASK-041501](../tasks/TASK-041501-the-words-the-offer-says.md) | The words the offer says, and the one word `ADR-0036` already chose | backlog |
| [TASK-041502](../tasks/TASK-041502-whether-the-offer-is-made-at-all.md) | Whether the offer is made — a win, no credential, and not already settled | backlog |
| [TASK-041503](../tasks/TASK-041503-the-offer-and-the-page-load-that-reaches-the-account-screen.md) | The offer itself, and the page load that reaches the account screen | backlog |
| [TASK-041504](../tasks/TASK-041504-the-result-screen-carries-an-offer-it-does-not-make.md) | The result screen carries an offer it does not make, and gives nothing up for it | backlog |
| — | *The persistence, the `Lobby` wiring, and the whole-client arc — not yet written. They are `DEC-079`'s and `DEC-080`'s to shape.* | — |

`TASK-041501` is the head; the other three depend on it for **sequencing only** and their `Files`
tables are pairwise disjoint, so all three are startable together once it merges.

## Open decisions

- **`DEC-079` — the product owner's.** Is *"not again"* a fact about the player or about this
  browser, and what spends the offer? `ADR-0036` §Consequences says the flag *"belongs on the
  profile"*; this story's own design notes say a key this client module owns. `DEC-049` says *"only
  'Not now' dismisses"*; this story's third criterion says a second win never shows it. Both halves
  are what a player experiences and both are derivable from `docs/vision.md`.
- **`DEC-080` — the architect's, downstream of `DEC-079`.** What carries it: the field `GET /api/me`
  gains if any, the endpoint that records a dismissal, what says *this win is the first*, and
  whether this story grows a server half.

Neither blocks `TASK-041501`–`TASK-041504`, which hold under either answer.

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
  storage key has to be added to, if `DEC-079` lands on client storage.
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
