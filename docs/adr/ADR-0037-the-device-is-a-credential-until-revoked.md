# ADR-0037 — The device stays a credential until the player revokes it

- **Status:** Accepted
- **Date:** 2026-08-15
- **Resolves:** `DEC-030` — **the human's call**, made as *"until the player revokes"*. The
  question was framed as risk acceptance rather than architecture, and it is recorded as such.
- **Amends:** [ADR-0030](ADR-0030-a-claim-adds-a-credential-and-moves-nothing.md), whose claim
  path leaves `player.device_id` in place forever with no way to remove it.
- **Constrains:** `STORY-0406` (the claim), `STORY-0412` (the account screens), and the session
  rules `STORY-0405` owns.

## Context

`ADR-0030` attaches a credential to the `player` row already resolved and moves nothing — no
second profile, no copied `duel_result`, and deliberately no `UPDATE player`. `player.device_id`
keeps pointing at the profile it created. That is what makes sign-out restore the anonymous
profile by subtraction, and it is a good property.

Its side effect is that an account has two sign-in routes forever, and only one of them is the
password. Anyone holding the original device is inside the account regardless of what the
password is. Nothing about that is a defect in `ADR-0030` — it simply never asked the question.

The question is not really technical. All three answers are implementable and none is hard. What
they differ on is **what a player is entitled to believe when they set a password.** Leaving it
at "forever" means the account screens must never imply the password protects anything, because
it does not. Setting it at "not at all" means the password genuinely is the credential, and a
player who forgets it and declined `ADR-0031`'s optional recovery email has lost the account with
no path back — `ADR-0031` is explicit that declining the email means no recovery path at all.

## Decision

**The device id remains a valid sign-in route after a credential is attached, until the player
revokes it.** Attaching a password adds a route; it does not remove one.

**The player may revoke the device binding**, from the account screens, at any time after a
credential exists. Revocation:

- clears the device's standing as a credential for that profile — a `Hello` presenting only that
  device id no longer resolves to it;
- is offered **only when a credential already exists**, since revoking the sole route to a
  profile would strand it;
- **does not kill the revoking session.** The player stays signed in on the device they are
  holding; they simply have to present the password next time. Signing someone out of the screen
  they are using to secure their account is hostile, and the session `ADR-0027` issued is
  independent of how it was obtained.

**The account screens must state which routes are live.** A player who has not revoked is
entitled to know the device still signs in; a player who has is entitled to see that it does
not. This is the substance of the decision, not decoration — the whole reason the middle option
was chosen over the status quo is that the status quo cannot be described honestly on a screen
that also asks for a password.

**What revocation looks like in the schema is left to `STORY-0406`.** `ADR-0030` says
`player.device_id` is never rewritten, and this ADR does not overturn that; whether revocation
nulls the column, moves the binding to its own row, or marks it revoked beside the credential is
a technical question with more than one defensible answer and no reason to guess it here.

## Consequences

- The default is unchanged from what is shipped: a claimed account keeps both routes. Players who
  never visit settings are exactly where `ADR-0030` left them.
- **A cautious player can reach the strong guarantee, and a careless one cannot fall into it.**
  That asymmetry is the point of the middle option.
- `EPIC-04` gains a revoke path, a settings affordance, and a rule about sessions on revocation.
  It is a small story, but it is a story, and `STORY-0412`'s design must carry it.
- A revoked device that later returns is an anonymous device with no profile — it mints a fresh
  one under `ADR-0012` rather than failing. Nothing else needs to change for that to be true.
- **`ADR-0031`'s recovery email becomes materially more important** for anyone who revokes: they
  have deliberately reduced themselves to one route. The revoke affordance should say so at the
  moment of revoking.

## Alternatives considered

**Forever, the status quo.** Zero build, and it never strands anyone. Rejected because it makes
the account screens either dishonest or awkward: the product would be asking for a password while
the device remains a bypass, and there would be no way for a player who wants a real credential
to get one.

**Retired at the moment of claim.** The only option where the password is unambiguously *the*
credential, and the cleanest thing to describe. Rejected on the interaction with `ADR-0031`: a
player who claims an account, declines the optional email and later forgets the password has lost
their coins outright, and the product would have taken away a working device route to make that
possible. Forcing every claiming player through that risk to serve the subset who want it is the
wrong default — offering it is not.
