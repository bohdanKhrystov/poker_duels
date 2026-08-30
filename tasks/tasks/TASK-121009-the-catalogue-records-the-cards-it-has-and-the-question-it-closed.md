---
schema: 2
id: TASK-121009
title: The catalogue records the cards it now has, and the question two rounds have closed
type: task
status: ready
parent: STORY-1210
module: docs
estimate: XS
tier: haiku
review: light
files_touched: 1
labels: [qa, uat, harness]
depends_on: []
verify:
  - grep -qF 'design/screens/duels.html' docs/test-plan.md
  - grep -qF 'design/screens/leaderboard.html' docs/test-plan.md
  - grep -qF 'design/screens/account.html' docs/test-plan.md
  - grep -qF 'design/screens/sign-in.html' docs/test-plan.md
  - grep -qF 'finishedAtText' docs/test-plan.md
  - grep -qF 'ADR-0061' docs/test-plan.md
  - test "$(grep -c 'Settled, and not a finding' docs/test-plan.md)" -eq 1
  - python3 .github/scripts/lint_tickets.py
---

## Goal

`docs/test-plan.md` §UAT names the cards that exist and lists the observations a merged source has
already blessed, so a round is never told four carded screens have no card and never spends a finding
on designed behaviour again.

## The defect

Round 2 of `/qa-cycle uat regression`, 2026-08-30, commit `07df9e7f`. Two arrears, both in the
catalogue, both cheap, both with a measured cost.

**1. The screen inventory says four screens have no card, on the round that proved they do.** The
`card` column still prints `—` for `duels`, `leaderboard`, `account` and `sign-in`:

```
| `duels`       | the duel history list, headed `Opponent name`     | — | walked | `04-01` |
| `leaderboard` | the season standings, with the viewer's own rank line | — | walked | `05-01`, `05-03` |
| `account`     | claiming a profile with a password, or … that profile's own page | — | walked | `04-02`, `04-03`, `04-05` |
| `sign-in`     | the sign-in form, reached from account, with a `Forgot your password?` link | — | walked | `04-03`, `04-04`, `04-05` |
```

All four cards merged into `develop` in round 1's repairs — `d500f56d`, `17d641e8`, `07df9e7f`,
`ebb00c49` — and round 2 judged conformance against every one of them. A catalogue that says a card
is missing is a catalogue that invites a missing-card finding `ADR-0092` §4 grades `high`, and its
dedupe key is the card's own path.

**2. Two rounds have now spent findings on designed behaviour.** The duel-history date renders in the
reader's locale. `STORY-1205` ruled it not a defect; `STORY-1209` ruled it again; round 2 filed it
**twice more**, both at `high`, one against the lobby strip and one against the full `duels` screen.
That is four findings on one settled question, and the observer is not at fault — it filed under
`FINDINGS` with its uncertainty named, which is exactly what `ADR-0092`'s classifier asks of a
borderline item. **What is missing is a place for a round to read the answer.**

The answer, in two merged sources:

- `web-client/src/profile/profile-text.ts` documents `finishedAtText` as *"When the duel finished,
  **in the reader's locale**"* and implements it as `new Intl.DateTimeFormat(options?.locales, …)`.
- `ADR-0061` §*What it costs* names that behaviour and **accepts** it verbatim: *"`finishedAtText`
  renders instants 'in the reader's locale', so a player far enough east or west can read a duel as
  finishing on 1 September and find it counted in August."*

A merged source that blesses what shipped **closes the question permanently** — re-raising it would
itself contradict a merged source — so the right home is the catalogue, not a card. A card draws one
screen; this is one function consumed by two.

## Files

| File | Action |
| --- | --- |
| `docs/test-plan.md` | modify |

## Scope

- **Fill the inventory's `card` column** for `duels`, `leaderboard`, `account` and `sign-in` with
  their merged paths, in the same `design/screens/….html` form the seven rows above already use.
  `verify` and `reset` keep their `—`: their cards are still owed, and the paragraph under the table
  already says why no missing-card finding is filed for either.
- **Add a short `### Settled, and not a finding` subsection** to §UAT, immediately after the
  classifier paragraph that ends *"…the `uat` agent's `QUESTIONS` section is its only route."* It is a
  table with three columns — the observation, the merged sources that settle it, and the round that
  ruled — and it opens with one sentence saying that an entry here is closed, that re-raising it
  would itself contradict a merged source, and that it is neither a finding nor a promotable
  question.
- **One row today**: duel-history timestamps rendering in the reader's locale, settled by
  `profile-text.ts`'s `finishedAtText` and `ADR-0061` §*What it costs*, ruled in `STORY-1205`,
  `STORY-1209` and `STORY-1210`.
- The subsection is a **list of answers, not a list of refusals**: a later entry is added only when a
  triage rules an observation closed by a named merged source, and the ruling's own round story is
  cited so the reasoning is one click away.

## Out of scope

- **Adding, changing or regrading any case.** §UAT adds no case and touches no row above it
  (`ADR-0092` §7), and this ticket does not start.
- **Any claim of coverage.** `ADR-0089` §2c: nothing here may be cited in an epic's `Metrics`, a
  Definition of done or a ticket's `verify:` as coverage, and filling a `card` column is a fact about
  the repository, never a statement that a screen was validated.
- **The `record`/`frames` sentences.** `TASK-121008` adds those to the same section; they are that
  ticket's, and two tickets writing the same paragraph is a merge conflict by construction.
- **Deciding that English-only dates are wanted.** If they are, that is a product request routed to
  the `product-owner`, and this row is what an answer would supersede. Recording the blessing is not
  endorsing it forever.
- **`verify` and `reset`.** Their cards are `ADR-0091` §5's retrofit story's, and their rows stay
  `out of scope`.

## Tests

No test file: this is a document. The `verify:` block gates what a command honestly can, and **each
line fails today** — every count below was measured at `07df9e7f`:

| Command | Proves | Today |
| --- | --- | --- |
| four `grep -qF 'design/screens/….html'` | each merged card is named in the catalogue | all four count `0` |
| `grep -qF 'finishedAtText'` | the settled row names the owning function, not just the symptom | count `0` |
| `grep -qF 'ADR-0061'` | it names the ADR that accepts the behaviour, so a reader can check the ruling rather than trust it | count `0` |
| `grep -c 'Settled, and not a finding' … -eq 1` | the subsection exists **once** — an equality, not a floor, so a second copy pasted into another section fails instead of passing | count `0` |

## Acceptance criteria

- [ ] §UAT's screen inventory names all four merged cards, and `verify`/`reset` still read `—`
- [ ] §UAT carries exactly one `### Settled, and not a finding` subsection, placed after the
      classifier paragraph
- [ ] Its one row names the observation, `profile-text.ts`'s `finishedAtText`, `ADR-0061`
      §*What it costs*, and the three round stories that ruled on it
- [ ] Its opening sentence says an entry is closed and is neither a finding nor a promotable question
- [ ] No case row anywhere in `docs/test-plan.md` is added, removed or changed —
      `git diff` shows edits only inside §UAT
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
