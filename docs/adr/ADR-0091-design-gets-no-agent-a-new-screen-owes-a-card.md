# ADR-0091 — Design gets no agent: a new screen owes a card, and adoption is gated where it is consumed

- **Status:** Accepted
- **Date:** 2026-08-30
- **Resolves:** `DEC-084` — does design work need its own agent, or does it stay on the
  `coder`/`reviewer` workflow? Raised 2026-08-30 by the human; registered and answered in the
  same PR (the `DEC-039` path)
- **Constrains:** the agent roster, the planner's split procedure
  (`.claude/agents/planner.md`), the gate placement between `design/` and `web-client/`, and
  `EPIC-06`'s register
- **Applies:** [`ADR-0024`](ADR-0024-design-follows-the-code-workflow.md) — nothing here amends
  it; all five of its sections stand byte-unchanged

## Context

Two facts pull against each other, and a third — the one that raised the question — turns out
to have been misread.

**The existing workflow demonstrably ships design.** 42 merged commits under `design/`; four
stories and 43 tickets, all `done`; a token sheet, fifteen preview cards, SVG suits, a coin and
a wordmark — through the ordinary lifecycle: schema-2 tickets, a branch per task, `light`
structural review, the human's verdict given visually on the rendered card (`ADR-0024` §3),
authoring worked interactively under `EPIC-06`'s recorded deviation because *taste does not
survive a verify block*.

**But design coverage stopped at `EPIC-06`'s boundary.** `design/screens/` holds exactly the
v0.1 duel flow. `EPIC-04` and `EPIC-05` then shipped six of the seven members of the client's
`Screen` union — `duels`, `leaderboard`, `account`, `sign-in`, `verify`, `reset` — with no
design card, and the human has never seen any of them as a design. Nothing failed, because no
rule anywhere makes a new screen ask for a design pass: `EPIC-06`'s Definition-of-done line —
*"EPIC-03 builds its first screen without inventing a single color or size"* — bound `EPIC-03`
by name, and the later epics did not inherit it. `STORY-0503` even wrote *"Composes
`design/tokens/tokens.css`; authors no colour — `EPIC-06` owns the visual language"*, and then
had no card to compose against. Nor is the gap only whole screens: the account offer
(`STORY-0415`) and the profile strip ship on *carded* screens with no card of their own.

**The token-adoption numbers behind the question measure the wrong thing.** The raw grep —
`table` 5, `lobby` 2, `result` 1, `account`/`ladder`/`history` 0 distinct `--pd-` references —
counts *direct* mentions, and direct mention is not how this client consumes the sheet.
`app.css`'s `@theme static` block resets every Tailwind namespace and rebinds it to
`var(--pd-*)`, and three merged client tests hold that arrangement: `theme.test.ts` (a theme
line is a reset or a reference to a declared token, nothing else), `color-literals.test.ts` (no
colour literal anywhere in client source), `tokens.test.ts` (the vendored sheet is
byte-identical to `design/tokens/tokens.css`). So `bg-surface`, `gap-4` and `rounded-medium` in
the "zero-token" families all resolve to the sheet. What actually bypasses it, measured today,
is raw lengths inside Tailwind arbitrary values — `380px` ×21, `560px` ×2, `460px` ×1, `1.5em`
×1, across fifteen files in every screen family — an unminted column width copy-pasted
twenty-one times being exactly the *hundred small consistency calls nobody wants to grade one
at a time*. (Inline style objects are not a second leak: the client has one, and it references
a token.)

**And the drift tax `ADR-0024` predicted came due.** Its rejected generator alternative was
deferred *"while the system is four files… becomes a ticket the moment cards multiply or a grep
actually fires."* Cards multiplied and the greps fired: `TASK-060107` and `060110`–`060123` —
roughly fifteen of the epic's 43 tickets — are drift repair and gate hardening, not design.

The roster question sits on top of all of this. This workflow adds an agent when a **decision
needs an owner** — the architect because runs stalled on *how*, the product owner because they
stalled on *what*, the `qa-manager` because triage needed exactly one thing allowed to file bug
tickets. So *"do we need a designer agent"* decomposes into: what decision would it own, and
which of the failures above would it have prevented? Nothing here is the product owner's: that
every player-facing screen carries the identity is already the vision's *Positioning* sentence
and `EPIC-06`'s merged goal (*"the web client consumes these tokens and graphics rather than
inventing its own"*); which visual values the new cards carry stays in their tickets, under
`ADR-0024` §5.

## Decision

**1. No designer agent, and no design skill. The roster stands.**

`ADR-0024` §3 already assigned design's two judgments: taste to the human, given by looking at
the rendered card, and structure to the `light` review. A designer agent would sit between a
ticket and that human holding neither — a hop that produces a proposal the same eye must still
grade. No design run has ever stalled on a judgment such an agent could have owned; the
failures on record are a missing trigger (nothing dispatched any design work at all, and an
agent nobody dispatches designs nothing) and mechanical drift (which tooling addresses and
judgment does not). The taste context the counter-case worries about — *Lichess not casino,
dark, quiet, minimal* — is carried into implementation by the **card**: a versioned, rendered,
human-accepted reference that a coder transcribes, and that survives the session that made it.
That is `ADR-0024`'s own argument, applied.

**2. A story that gives the player a new screen owes the card first.**

At split time, a story whose tickets put a new screen in front of a player names, in its
`## Design notes`, the card(s) under `design/screens/` that its screens implement. If no such
card exists, **the split's first ticket is the card**. The mechanical floor of "new screen" is
a new member of the `Screen` union in `web-client/src/routing/screen.ts` — every undesigned
screen shipped so far crossed exactly that line. A new player-facing surface behind an existing
slug — the account offer was one — is the same rule applied by the planner's judgment.

The rule lives in **one** place: the planner's split procedure, `.claude/agents/planner.md` —
the plan-story rule. The planner is the only actor that decides what a story's tickets are, so
a rule binding anywhere later — an epic's Definition of done, checked at close; a story
template field, which only the planner could fill — either fires after the screens shipped or
is this rule wearing a costume. A second copy in another document would drift; this ADR is the
durable record and the agent file is the working copy.

**3. Who authors a card follows what the card does.**

`EPIC-06`'s recorded deviation — design worked interactively with the human, never dispatched
to a coder — was scoped *"in this epic"* and would otherwise expire with it. It generalises by
splitting on what the ticket creates. **Minting** — a new token, a new component, new visual
language — is worked interactively with the human, because taste does not survive a verify
block. **Composing** — a screen card assembled from the settled vocabulary — is an ordinary
dispatched ticket: `module: design`, estimate `S` read as one card one file, `review: light`,
conventions per `design/README.md` and `ADR-0033`. Either way the verdict that matters stays
the human's visual one (`ADR-0024` §3), and it may **trail the merge**, exactly as `EPIC-06`
practised it with batched pane sign-offs — so an unattended run never stalls at a pane. A
trailing rejection is a repair ticket against the card and against whatever implemented it.

**4. `check-drift.sh` does not reach into `web-client/`; the fourth client guard closes the
measured leak.**

The shell gate stays what it is: `design/`'s self-consistency check, running in `tickets.yml`.
Adoption is gated where consumption happens — the client's own CI job (`ADR-0026`) — which
already holds three guards for exactly this: the vendored sheet's byte-identity, the theme
block's reference-only rule, and the colour-literal ban. It gains a **fourth**: the literal
guard grows to refuse a raw length literal inside a Tailwind arbitrary value (`-[380px]`
fails; `-[var(--pd-…)]`, `-[calc(…)]` over variables, ratios and keywords pass). Measured
against today's client that flags 25 occurrences of four values; each becomes a token or a
named exemption in the guard, and *which* — the values themselves — is decided in those
tickets under the human's eye, never here (`ADR-0024` §5).

**5. The debt is registered, not forgiven.**

One story under `EPIC-06`, which reopens: a card set covering every `Screen` member no card
shows — today `duels`, `leaderboard`, `account`, `sign-in`, `verify`, `reset` — plus the
carded-screen accretions the planner judges worth a card, the account offer first among them.
The 2026-08-15 close and its metrics stand as history; a second close appends and rewrites
nothing. Separately, `ADR-0024`'s generator sentence has had its trigger met, so the ticket it
promised is now owed — whether the answer is a generator or a third way is that ticket's
question, not this ADR's.

## Consequences

**What it buys.** Future screens are designed by construction — the card exists before the
screen, so a coder transcribes an accepted look instead of re-deriving taste from prose at
Haiku temperature. The two silent-drift classes that produced this decision both become loud:
an unminted length fails the client job, and an unminted screen cannot survive its own story's
split. The roster stays as it is, and expensive judgment keeps happening where it already
happens — the planner at split, the human at the pane.

**What it costs.**

- **A screen can ship wearing a look the human later rejects.** The trailing visual review is
  what keeps unattended runs unattended, and its price is rework — the card *and* the
  implementation behind it. This is the cost most likely to be underestimated, because a
  merged card ticket reads as *the look was approved* when it only means *the look was
  recorded and is structurally sound*.
- Every screen-adding story is one ticket longer, forever, and its split carries one more
  rule.
- The length guard lands red: a repair story across fifteen files, and every future "just
  380px" costs a token-minting conversation instead of a paste.
- `EPIC-06` reopens after closing with frozen metrics. Its `done` of 2026-08-15 becomes
  wrong-in-hindsight on the record. Accepted rather than hidden — the trail is the second
  product.
- The trigger's mechanical floor sees slugs only. A surface behind an existing slug rides on
  the planner's judgment, which can miss. The escalation is written now rather than
  improvised later: if a screen ships undesigned **with this rule in force**, a slug↔card
  coverage register and its client-side test become a ticket — the same
  deferred-until-a-grep-fires shape `ADR-0024` used for the generator. Its bill (§Context)
  shows that shape does eventually get paid, and is still cheaper than building registers
  nobody has yet needed.

**What it forecloses.** Almost nothing, which is why *no agent* is safe to decide on today's
evidence and is chosen partly for being the cheapest answer to reverse: an agent file is one
commit if design work ever grows a decision worth owning. The one thing genuinely closed off
is *delegating taste itself* — that would amend `ADR-0024` §3's assignment of the verdict to
the human, and only the human can give that away. The timing runs the other way for the
trigger rule: it is cheapest now, while the debt is three screen families, and every epic
shipped without it adds another retrofit story.

## Alternatives considered

**A designer agent.** Strongest case: the roster has grown before when the work grew — `qa`
and `qa-manager` are recent — and a standing role could carry *Lichess not casino* across a
hundred small calls nobody wants to grade one at a time, past the point where a human's
attention scales. Rejected: an agent is added here when a decision needs an owner, and this
one would own none — taste is the human's at the render and structure is the light reviewer's
(`ADR-0024` §3), so an agent between them is a hop; and the hundred small calls turn out to be
either mechanical (token adoption — gated, §4) or settled once, in the card (§2). It would
also have prevented neither recorded failure: it cannot dispatch itself, and it does not fix
drift.

**A design skill, on the `qa-cases` precedent.** Strongest case: `ADR-0090` just proved a
skill can author reviewed content cheaply and land it through ordinary PRs — a `design-card`
skill could scaffold card conventions the same way. Rejected: `qa-cases` earned skillhood by
owning a real problem — composing whole suites under a licensing condition that needed its own
ADR. A card is one file through one ordinary ticket, and its conventions are already a written
procedure (`design/README.md`, `ADR-0033`) that a ticket's file list hands to any coder. A
skill here is invocation ceremony with nothing to own. Nothing forecloses one later; it would
change no register.

**Extend `check-drift.sh` into `web-client/`.** Strongest case: one gate in one place already
runs on every PR, and it caught every drift class on its own side of the fence — adoption
would just be clause seven. Rejected on direction and on ownership. `web-client` consumes
`design/`, and a consumer's conformance is checked in the consumer's contract tests — a
pattern this repo has merged three times over, `tokens.test.ts` already reading
`../../../design/tokens/tokens.css`. Reaching the other way would put the process workflow
(`tickets.yml`) in the business of gating product source that `build.yml`'s client job owns
(`ADR-0026`); two jobs would then own one fact, and the two copies of `tokens.css` would be
read by different gates in different languages — a new drift surface between the gates
themselves. And the reach would have caught nothing the client gates miss: what was invisible
was raw lengths (§4 closes it, client-side) and never-designed screens (§2 closes it, at
split).

**A mechanical coverage gate now — a slug↔card mapping, or a `lint_tickets.py` check.**
Strongest case: a rule in an agent file binds only an obedient agent, and `ADR-0084`
mechanised what could be mechanised. Rejected after measuring, per that same ADR's
discipline: eleven merged tickets name `routing/screen.ts` and a text lint cannot tell a
member-add from a read-mention, so the honest relation does not exist at the ticket layer; and
cards carry no machine-readable slug, so a mapping gate needs new metadata — a register
invented in service of its own check, which is the drift class this epic just spent fifteen
tickets paying down. Deferred, with the trigger named in §Consequences.

**A Definition-of-done line, or a richer story template.** Strongest case: the DoD line worked
once — `EPIC-06`'s third checkbox is why `EPIC-03` never invented a colour. Rejected: per-epic
memory is precisely what failed — `EPIC-04` and `EPIC-05` did not inherit the line, and a DoD
is checked at close, after the screens shipped. A template field (*design card: ___*) is the
same rule wearing a costume: only the planner could fill it, so the substance is §2's rule and
the field would be a second copy that drifts.
