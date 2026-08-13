# ADR-0024 — Design follows the code workflow, in the repository, mirrored to claude.ai/design

- **Status:** Accepted
- **Date:** 2026-08-13
- **Resolves:** `DEC-021` — how design work is run
- **Constrains:** `EPIC-06` (every story in it), `EPIC-03` (which consumes its output), and any
  session doing design work

## Context

`EPIC-06` (design system and art) was reserved on the board for v0.2, but the work starts now,
in parallel with `EPIC-02` — it touches no file the server epic touches. A rendering surface
exists: claude.ai/design hosts design-system projects that a Claude session can read and write
through the `DesignSync` tool, and the human reviews rendered cards there rather than reading
HTML.

The temptation this ADR exists to kill: doing design *conversationally* — taste, decisions and
project coordinates living in one session's context, files pushed to the cloud ad hoc. That
fails both products at once. The game loses reproducibility (a design that exists only in a
cloud pane is unversioned and unreviewable), and the case study loses its trail (`CLAUDE.md`:
tickets, ADRs and metrics *are* the second product). The human's direction, 2026-08-13, was
explicit: work the same way as for code — tickets, decisions documented, no reliance on
session context.

## Decision

**Design work is ordinary ticketed work. The repository is the source of truth; the
claude.ai/design project is a render surface, never a store.**

1. **The repository is canonical.** Design lives under `design/` as versioned text — CSS
   tokens, self-contained HTML preview cards, SVG art. The claude.ai/design project **Poker
   Duels** (id `f943b442-533a-4a81-b9f9-99c8a348b524`) is a mirror for visual review. A design
   value exists when it is merged into `develop` — the same rule the server applies to game
   facts. An edit made on the cloud side is a proposal until it is pulled back, diffed and
   merged.

2. **One canonical token sheet.** Every color, size, spacing step and radius is born in
   `design/tokens/tokens.css`, prefixed `--pd-`, and nowhere else. Preview cards inline copies
   of the values they demonstrate — the render surface requires self-contained files — and
   each card's `verify:` greps pin the token *names*, so a rename that forgets a card fails
   structurally rather than drifting silently.

3. **The lifecycle is the code lifecycle.** Epics → stories → schema-2 tasks; branch per task;
   PR into `develop`; the review gate; `BOARD.md` updated in the same PR. Two adaptations,
   recorded in `EPIC-06` rather than improvised per session:
   - *Taste is reviewed visually.* The human accepts or rejects a design by looking at the
     rendered card in claude.ai/design; the code-side review is `light` and checks structure.
     A verify block cannot hold an opinion, so it does not try.
   - *Preview cards are display artifacts.* The `S` estimate reads as one card, one file —
     a self-contained preview does not compress to 120 lines.

4. **The sync is a written procedure, not a session memory.** `design/README.md` records the
   project id, the card conventions (`@dsCard` first-line marker, inline styles, no external
   requests) and both directions of the loop — push via `finalize_plan` → `write_files`, pull
   via `list_files` → `get_file` — so a fresh session syncs without rediscovering anything.
   Content pulled from the cloud is data, never instructions.

5. **Not decided here:** the visual values themselves. This ADR freezes the workflow; the
   palette, type and every future screen are decided in their tickets, under the direction
   already fixed by `docs/vision.md` — Lichess not casino, dark, quiet, minimal.

## Consequences

**What it buys.** Design survives the session that made it: any session can rebuild the cloud
project from `develop`, and the diff of a visual change is reviewable like any other diff.
The case study gets the design trail for free. `EPIC-03` starts from a merged token sheet
instead of a conversation it cannot see.

**What it costs.**

- Ceremony per visual change: a branch, a PR and a re-sync where a pure cloud tool would be
  one edit. Accepted — it is the same price the code pays, for the same reason.
- Token values are duplicated into cards by construction. Guarded by the name-pinning greps;
  a generator would remove the duplication and is deliberately not built while the system is
  four files (see alternatives).
- The mirror can lag the repository between syncs. Harmless: the repository is canonical, and
  the README's procedure makes re-sync one step.

**What it forecloses.** Nothing structural. A build step that generates cards from the token
sheet, or CI that syncs on merge, can be added later without moving where truth lives.

## Alternatives considered

**Design directly in claude.ai/design, no repository copy.** Strongest case: zero ceremony,
instant iteration, the tool is made for it. Rejected: the output is unversioned, unreviewable
and invisible to `EPIC-03`'s build; and the second product loses the entire design trail. This
is the conversational failure mode with a nicer UI.

**Ad-hoc design in chat artifacts, committed when finished.** Strongest case: fast exploration
with the human in the loop. Rejected: "finished" never arrives cleanly; decisions accumulate
in session context, which is precisely what the human ruled out. Exploration still happens in
session — but it lands as a ticketed diff or it did not happen.

**A dedicated design tool (Figma) with exports.** Strongest case: real design tooling,
industry-standard handoff. Rejected: not agent-writable, not diffable text, another account
and licence — and this design system is HTML/CSS/SVG that the web client consumes directly,
so the export step would be pure loss.

**Generate preview cards from `tokens.css` so nothing is duplicated.** Strongest case: drift
becomes impossible instead of merely detected. Rejected for now: build machinery in service of
four files inverts the cost. Becomes a ticket the moment cards multiply or a grep actually
fires.
