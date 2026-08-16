---
id: STORY-0415
title: The offer — an account after a first win, dismissed for good
type: story
status: backlog
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

| ID | Title | Status |
| --- | --- | --- |
| — | *Not yet split. Run `/plan-story STORY-0415` once `STORY-0412` has merged.* | — |

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
