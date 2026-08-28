---
id: STORY-0415
title: The offer — an account after a first win, dismissed for good
type: story
status: done
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
written because `DEC-079` and `DEC-080` decided their shape. Both were answered — `DEC-079` the same
day, `DEC-080` on 2026-08-28 — and the split was **completed on 2026-08-28**, at **nine** rather than
seven. Two of the five new tickets exist for reasons measured rather than chosen, and both are
recorded in `## What the split measured`: `ADR-0086` §6 needs a prop on a merged component, and the
`pd.roomCode` gap `ADR-0086` names had to become a ticket rather than a discovery made twice.

| ID | Title | Status |
| --- | --- | --- |
| [TASK-041501](../tasks/TASK-041501-the-words-the-offer-says.md) | The words the offer says, and the one word `ADR-0036` already chose | done |
| [TASK-041502](../tasks/TASK-041502-whether-the-offer-is-made-at-all.md) | Whether the offer is made — a win, no credential, and not already settled | done |
| [TASK-041503](../tasks/TASK-041503-the-offer-and-the-page-load-that-reaches-the-account-screen.md) | The offer itself, and the page load that reaches the account screen | done |
| [TASK-041504](../tasks/TASK-041504-the-result-screen-carries-an-offer-it-does-not-make.md) | The result screen carries an offer it does not make, and gives nothing up for it | done |
| [TASK-041505](../tasks/TASK-041505-the-one-key-the-offers-answer-lives-under.md) | The one key the offer's answer lives under, and the gate row that owns it | backlog |
| [TASK-041506](../tasks/TASK-041506-the-accept-control-is-an-answer-too.md) | The accept control is an answer too, and says so before the page loads | backlog |
| [TASK-041507](../tasks/TASK-041507-the-lobby-fills-the-offer-slot-and-answers-for-it.md) | The lobby fills the offer slot, and either control answers it | backlog |
| [TASK-041508](../tasks/TASK-041508-the-offer-across-two-boots-of-a-whole-client.md) | The offer across two boots of a whole client, answered and unanswered | backlog |
| [TASK-041509](../tasks/TASK-041509-the-room-code-key-gets-the-row-it-never-had.md) | The room code key gets the row it never had | backlog |

`TASK-041501` was the head. `TASK-041505` and `TASK-041506` depend only on the four merged tickets
and their `Files` tables are disjoint, so they are **one batch**; `TASK-041507` needs both,
`TASK-041508` needs `TASK-041507`, and `TASK-041509` needs only `TASK-041505` — it shares that
ticket's one file and nothing else, so it can run beside `TASK-041507` or `TASK-041508` but never
beside `TASK-041505`.

`TASK-041507` is the story's only `atomic:` ticket, at **four** files: `App.test.tsx`'s
`vi.mock("./main", …)` replaces that module wholesale for all 37 of its tests, and 25 of them throw
the moment `Lobby.tsx` imports a binding the factory does not return — measured, not predicted.

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

**Both edits `ADR-0085` §7 named were made on 2026-08-28**, by the pass that completed the split: the
third acceptance criterion below gained its *"to a player who answered it"* clause and the unanswered
case became a criterion of its own, and `ADR-0056` §6's `STORY-0415` line now reads *a `429` is not a
dismissal, and the sign-up the player accepted is still there with what they typed*, marked as
amended by `ADR-0085` §7 rather than silently rewritten.

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

### Measured on 2026-08-28, completing the split

- **`App.test.tsx` forecloses a three-file wiring ticket.** Its `vi.mock("./main", …)` (line 41)
  takes no `importOriginal` and returns a fixed object, so any binding `Lobby.tsx` imports from
  `../main` must appear in that factory. Adding `offerSettledHere` and `settleOfferHere` to
  `main.tsx` and importing them in `Lobby.tsx` without touching that file measured
  `Tests 25 failed | 808 passed (833)`, every failure reading `No "offerSettledHere" export is
  defined on the "./main" mock`. This is the third time this file has cost a ticket — `TASK-041223`
  and `TASK-041229` were both blocked by it — so `TASK-041507` names it in `atomic:` and in its
  `Files` table.
- **`ArcWiring` has exactly two builders**, `drive-arc.test.tsx` and
  `claimed-here-recovered-there.test.tsx`, so two required fields on it is a three-file ticket. The
  second builder was named by `tsc --noEmit` — `error TS2739: Type '{ history: null; signedIn:
  false; }' is missing the following properties from type 'ArcWiring'` — which is a typecheck
  failure that hides every test result behind it.
- **The one-module gate's substring scan cuts both ways, harmlessly one way.** Making
  `markOfferSettled` write a second key `pd.accountOfferSettledAt` **from the same module** leaves
  the row green, because the scan collects file *names*. The refused short name `pd.accountOffer` is
  dangerous only because a second **file** would then match.
- **`pd.roomCode` resolves to exactly one production file**, `room-memory.ts`, and a probe writing
  that literal into `store/boot.ts` reddens a row for it — so the gap `ADR-0086` named is closable in
  six lines, and it is `TASK-041509`.
- Suite: **822 / 106** merged → **828 / 107** with `TASK-041505` → **829** → **832** → **835** →
  **836** with `TASK-041509`. Every number measured, in that order.

## Acceptance criteria

- [ ] The offer appears after a won duel and not after a lost or drawn one — all three asserted.
- [ ] It does not appear for a player who already holds a credential.
- [ ] It does not appear a second time after a second win **to a player who answered it** — either
      control, `ADR-0085` §2.
- [ ] It **is** offered again after a later win to a player who was shown it and pressed neither
      control, since nothing was answered — `ADR-0085` §3's fourth row.
- [ ] Dismissing it once suppresses it across a reload, asserted through the injected storage.
- [ ] Dismissing leaves every capability intact: the player can still play, still earns the coin, and
      nothing is disabled.
- [ ] Accepting it opens the account screen rather than a form of its own.
- [ ] The offer's trigger reads a server-sent outcome, and no test asserts it from a derived count.

## Out of scope

- Requiring an account for anything — forbidden by `ADR-0036`.
- The account screen itself — `STORY-0412`.
- Any second prompt, reminder or badge.
