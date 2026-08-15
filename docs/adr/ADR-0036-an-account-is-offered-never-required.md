# ADR-0036 — An account is offered after a first win, and never required

- **Status:** Accepted
- **Date:** 2026-08-15
- **Resolves:** `DEC-025` — **the human's product call**, made as *"prompted, never required"*.
  This ADR does not choose it; it records it and works out what it costs `EPIC-04`.
- **Constrains:** `EPIC-04`'s story list — it decides that the fifteenth story exists and what it
  may do; and every screen that could be tempted to gate on identity.

## Context

`ADR-0012` gives every player a device-bound anonymous profile, and `ADR-0014` pays that profile
duel coins like any other. The result, shipped and working, is that an anonymous player is a
full participant: they duel, they win, they hold coins, they take a leaderboard place. Nothing
in the product has ever asked them to sign up, because nothing has ever needed to.

`EPIC-04` adds credentials on top of that. The question `DEC-025` registered is whether adding
them changes the deal — whether an account stays a convenience, becomes a prompt, or becomes a
wall. It was registered precisely because the epic's shape depends on the answer and a guess
inside an epic reads as settled to everyone who arrives later.

Two facts constrain the answer. **A device-bound profile is one dropped phone away from
nothing** — `ADR-0027`'s sessions expire at 30 days absolute, and `ADR-0031` makes the recovery
email optional, so a player who loses the device and has no credential loses the coins. That is
a real harm and it argues for asking. **And the product is Lichess, not casino** — a ladder
people can simply play, without an interstitial between wanting to duel and duelling.

## Decision

**An account is never required to play, and anonymous play stays fully ranked.** An anonymous
device profile duels, earns duel coins and takes a leaderboard place, exactly as it does today.
No screen gates on having a credential.

**After a player's first win, the client offers one.** The offer names the actual stake — coins
that exist and could be lost — rather than asking abstractly:

- The trigger is the **first duel won**, not the first duel played. A player who has not yet won
  has nothing to protect and the prompt would be noise.
- It is **dismissible, and dismissal is permanent.** "Not now" means not again. This is the half
  of the decision most likely to erode under a growth argument later, so it is stated as a rule
  rather than as a default.
- It is an **offer, not a gate**: dismissing it returns the player to exactly where they were,
  with every capability intact.
- Declining does not degrade anything — no reduced coin, no withheld leaderboard place, no
  reminder badge that never goes away.

**`EPIC-04` gains one story** for the prompt and its trigger rule. It depends on the claim flow
(`STORY-0406`) existing, since the prompt's whole purpose is to start one.

## Consequences

- **Nothing in `EPIC-04` blocks on identity.** Every other story is additive to a working
  anonymous product, which is what `ADR-0012` intended and what this preserves.
- **The prompt needs a fact the server does not currently send**: whether this is the player's
  first win. `duel_result` rows already carry what is needed and `TASK-021106`'s recent-duels
  query already reads them, so this is a read-path question rather than new state — but it is a
  question, and the prompt story owns it.
- **Dismissal is state that must survive.** "Permanently dismissed" cannot live in `localStorage`
  alone, because the device *is* the identity for an anonymous player and clearing storage would
  resurrect the prompt forever. It belongs on the profile.
- **The leaderboard will contain anonymous profiles**, indefinitely and by design. Any later
  feature that wants to assume a leaderboard entry has a credential behind it is contradicted
  here and needs its own decision.
- `ADR-0030`'s claim path becomes the prompt's destination rather than a flow a player has to
  find on their own.

## Alternatives considered

**Optional forever, with no prompt.** The purest reading of `ADR-0012` and the cheapest to
build. Rejected because it is silent at exactly the moment the player has something to lose: a
player who has never been told their coins are device-bound learns it by losing them.

**Ranked requires an account; anonymous play is casual only.** Closest to Lichess, and the
strongest ladder integrity story — every ranked result attached to a recoverable identity.
Rejected on cost and on shape: it splits a duel into two kinds, touches the coin rule, the
result path and every screen that shows a coin, and it retracts something `ADR-0012` and
`ADR-0014` already shipped. Withdrawing a capability players have is worse than never offering
it.

**Eventually mandatory, after a grace period.** Rejected as the hardest to walk back. A wall
that appears after N duels is discovered by the players most invested in the product, at the
moment they are most invested, and no amount of later softening undoes that.
