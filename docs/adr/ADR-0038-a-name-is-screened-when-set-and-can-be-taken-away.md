# ADR-0038 — A display name is screened when set, can be taken away, and is burned when it is

- **Status:** Accepted
- **Date:** 2026-08-15
- **Resolves:** `DEC-017` — **the human's product call**, made as *"blocklist + takedown"*, with
  the follow-on question answered *"retired forever"*. Completes the split `ADR-0021` opened when
  it settled the name's shape and refused to settle its rules.
- **Amends:** [ADR-0029](ADR-0029-a-display-name-is-unique-and-permanent.md) — uniqueness must
  now consult more than the set of names in use, and "permanent" gains exactly one exception.
- **Constrains:** `STORY-0410` (which it unblocks), `STORY-0401`'s write path, and the schema
  `STORY-0403` lands.

## Context

`ADR-0029` made a display name unique case-insensitively and permanent once set, and said
nothing about which names may be set. `DEC-017` recorded why that combination is unstable:
permanence turns an offensive name, a squatted name and a homoglyph impersonation into the same
problem, because none of them can be undone by the player who caused them or by anyone else.

`ADR-0021` had already refused to answer this by implication, declining to add constraints
nobody had asked for. That refusal is what left `STORY-0410` blocked and is why the question
arrives here intact rather than half-decided by a schema.

The live cost of getting it wrong is not symmetric. A name refused at set time is a small
annoyance to one player who picks another. A name that should never have been allowed, under
`ADR-0029`'s permanence, sits on the leaderboard forever with no mechanism able to remove it.

## Decision

**A name is screened when it is set.** A blocklist is consulted alongside `ADR-0029`'s
uniqueness check, and a name that matches is refused at the moment of setting, with the same
kind of error a taken name produces. The blocklist's contents are operational data, not
architecture: this ADR fixes that one exists and is consulted, not what is in it.

**A name can be taken away afterwards.** An operator may force-rename a profile. This is the
mechanism `ADR-0029`'s permanence otherwise forecloses, and it exists so that a name which slips
past the blocklist — or which was never on it, such as an impersonation only a human recognises —
is not permanent by accident. The operator, for as long as this project has one person running
it, is the human; the path does not need a role system to exist and will not grow one
speculatively.

**A name taken away is retired, not released.** The vacated string does not return to the pool.
It goes to a retired set that the uniqueness check consults *in addition to* names in use, so
nobody can claim it again — including the player it was taken from.

**Uniqueness therefore has three sources of truth**: names currently held, names retired by a
takedown, and the blocklist. All three are consulted case-insensitively under the ICU collation
`ADR-0029` pinned. Adding the second and third to that check is the schema consequence
`STORY-0403` must carry.

**A force-rename gives the player a name, it does not leave them nameless.** `ADR-0021` makes
`player.display_name` nullable and the read path already handles the unset case, so a renamed
profile may return to unset and be asked to choose again — but it must never end up holding a
name it did not choose and cannot change, which permanence would then trap it in.

## Consequences

- **`STORY-0410` is unblocked** and now has three pieces of work: the blocklist check on the
  write path, the operator force-rename path, and the retired set with its effect on uniqueness.
- **The retired set only grows.** That is accepted deliberately: it is small, it is the price of
  closing the re-registration loop, and a name pool of practical size is not threatened by it.
- **Homoglyph impersonation is not solved**, and this ADR says so rather than implying otherwise.
  A blocklist catches strings someone thought of in advance; it does not catch a Cyrillic *а* in
  a Latin name. Closing that needs a script restriction, which was on the table and was not
  chosen — it refuses legitimate names to catch an attack nobody has yet attempted here. If
  impersonation appears in practice, that is a new decision with `ADR-0029`'s ICU pin already in
  place as the hook.
- **`ADR-0029`'s "permanent" is now "permanent to the player".** An operator can change a name;
  the player still cannot. The distinction is the whole mechanism, and any future reader of
  `ADR-0029` should arrive here.
- Screening at set time means the blocklist is on the critical path of a write that
  `STORY-0401` already implements. It must fail closed — a blocklist that cannot be read refuses
  the name rather than accepting it.

## Alternatives considered

**Takedown only, with no upfront screen.** Cheapest, and it needs no blocklist to curate.
Rejected because every bad name is live until somebody reports it, and under permanence "live
until reported" means live on a leaderboard indefinitely. Screening is the cheap half; doing only
the expensive half is the wrong economy.

**Charset restriction, plus blocklist, plus takedown.** The only option that closes homoglyph
impersonation. Rejected as disproportionate today: it refuses legitimate mixed-script names from
real players to defend against an attack this product has not seen, and `ADR-0029`'s ICU pin
means it can be added later without rework.

**Nothing at all.** Honest, zero build, and it was a real option — but it is the one answer that
cannot be walked back. Adding a mechanism later does not fix the names already taken, because
permanence has already made them permanent. Every other option leaves a door; this one closes it
on the way in.
