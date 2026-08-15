# ADR-0041 — A handle and a password are the only credential v0.1 ships

- **Status:** Accepted
- **Date:** 2026-08-15
- **Resolves:** `DEC-031` — **the human's call**, made as *"handle + password only, for now"*.
- **Constrains:** `STORY-0412`'s account screens and `EPIC-06`'s design for them. Blocks nothing.

## Context

`ADR-0027` gives a credential a `kind`, which makes adding a provider additive rather than a
migration. `ADR-0031` settles the email question completely — an optional, verified recovery
address in its own table, and a lowercase login **handle** that is neither the display name nor
the email — while saying nothing about third-party sign-in.

So the schema has been ready for a provider since `ADR-0027`, and the question `DEC-031`
registered was never "can we?" but "are the account screens designed for one credential or
several?" A provider row is not something that can be tastefully retrofitted into a sign-in
screen designed without it, which is why the question is due before the screens are designed
rather than before they are built.

## Decision

**v0.1 and v0.2 ship exactly one credential kind: a handle and a password.** `credential.kind`
carries `"password"` and nothing else.

**The account screens are designed for one credential.** No provider row, no "or continue
with…" divider, no space held open for buttons that do not exist. `EPIC-06` designs the screen
that ships.

**The door stays open and unfinished.** `ADR-0027`'s `kind` is not removed, narrowed, or
constrained to a single value — it keeps doing what it was built for. Adding a provider later
remains additive in the schema and is a redesign of the sign-in screen, which is an honest price
rather than a hidden one.

This is deliberately *"not now"* rather than *"never"*. The third option on the table was to
settle it closed — one credential kind forever — and it was not chosen. Nothing here should be
cited as a decision against third-party sign-in.

## Consequences

- **Nothing is blocked and nothing is built.** `STORY-0403`, `STORY-0404` and `STORY-0405`
  proceed exactly as `ADR-0027` and `ADR-0031` describe them.
- No OAuth dependency, no provider outage in the sign-in path, no third party learning who plays
  here. All three are real benefits of the answer and none of them is the reason for it — the
  reason is that the screens have to be designed for something.
- **If the answer is reopened, the cost is a screen, not a migration.** Recording that here means
  a future proposal is arguing about design work rather than discovering the schema question
  after the fact.
- `ADR-0031`'s recovery email carries the whole weight of account recovery, since there is no
  provider to fall back on. That was already true; it is more visible now.

## Alternatives considered

**Design for third-party sign-in now.** Avoids the retrofit entirely and is the answer if a
provider is genuinely coming. Rejected because nothing indicates it is, and designing a row of
buttons that ship disabled — or worse, ship working and unmaintained — costs `EPIC-04` an
integration story and `EPIC-06` a design for a feature nobody has asked for.

**Settle it closed — password only, forever.** Attractive for the independence it guarantees, and
it would make this ADR a stronger statement. Rejected because it claims more than is known: there
is no evidence yet about how players want to sign in, and recording a permanent refusal would
mean superseding an ADR rather than extending one the first time that evidence appears.
