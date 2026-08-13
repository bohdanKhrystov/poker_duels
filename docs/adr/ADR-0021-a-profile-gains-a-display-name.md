# ADR-0021 — A profile gains a player-chosen display name

- **Status:** Accepted
- **Date:** 2026-08-13
- **Resolves:** `DEC-016` (technical shape; the product rules split off as `DEC-017`)
- **Amends:** the schema established by `TASK-020904`; the read path of `STORY-0211`
- **Constrains:** `EPIC-03` (results list), `EPIC-04` (identity)

## Context

A result line names the opponent, and today the only things `player` holds are its `id` and its
`device_id`. The device id is the sole authentication token (`ADR-0012`) — showing it to the other
player would hand over their account — so `DuelSummaryResponse` ships `opponentPlayerId`, a UUID
that no human wants to read on a results screen.

The product decision is made and is not re-argued here: **a profile gains a player-chosen display
name.** What remains is the technical shape — where the name lives, how it reaches the read path,
what the wire carries — and one discipline problem: four product rules about names (uniqueness,
renaming, the unset fallback, moderation) are still open, and a schema or a DTO can quietly answer
any of them by accident. The shape chosen here must stay compatible with every answer the human
might give.

## Decision

**`player` gains a nullable `display_name` column; the read path joins it in; the wire carries it
as a nullable string.**

- **Schema.** The next migration in the chain (`V3` as of this writing — migrations are immutable,
  so this is a new file, never an edit) adds to `player`:

  ```sql
  ALTER TABLE player ADD COLUMN display_name TEXT
      CONSTRAINT player_display_name_length CHECK (char_length(display_name) BETWEEN 1 AND 32);
  ```

  `NULL` means *never set* — every existing row and every freshly created profile has no name until
  its player chooses one. The length check is a storage bound, not moderation: any wire-writable
  text field needs a ceiling, and 32 characters is generous for a name while useless for a payload.
  **Deliberately no `UNIQUE` constraint** — whether two players may share a name is `DEC-017`, the
  human's, and a constraint added now would answer it silently.

- **Write path.** The name is set over HTTP — `PUT /api/me/name` — authenticated by the same device
  id header as `GET /api/me`, refusing an absent or unknown device and creating nothing, for the
  same crawler-guard reason. It lands on a new port (`ProfileWrites`, next to `ProfileReads` in
  `duels.poker.server.http`, implemented in `duels.poker.server.db`): `ProfileReads`' contract is
  that nothing on it creates or mutates anything, and tests rely on that, so the first HTTP write
  gets its own port rather than eroding that guarantee. The server trims the input and enforces the
  1–32 length; it applies no other filtering yet (`DEC-017`).

- **Read path.** `RECENT_DUELS_SQL` in `PostgresProfileReads` gains one more join — `player p ON
  p.id = o.player_id` — selecting `p.display_name`. Still one query, no N+1, per `STORY-0211`'s
  rule. `DuelSummaryResponse` gains `opponentDisplayName: String?` and `ProfileResponse` gains
  `displayName: String?`. **`opponentPlayerId` stays**: the id is the stable identity a client can
  correlate on, the name is a label — exactly the split its KDoc promised when `DEC-016` was
  registered.

- **Wire type.** Nullable `String`, no default value — the file's standing rule, so the field is
  always present, as `null` when the opponent never set a name. The server never fabricates a
  placeholder ("Anonymous", "Player-3f2a"): what a client renders for `null` is a product choice
  (`DEC-017`), and baking a fallback into the wire would make it forever.

- **Current name at read time.** The join reads the name as it stands when the list is requested,
  so a rename relabels past result lines. Nothing snapshots the name at duel time.

- **Never an authentication factor.** No code path resolves a device, session, or player *from* a
  name. `DeviceId` remains the sole credential; the name is data about a player, reached only by
  joining on `player.id`.

### The deadline, honestly

`ADR-0019` had a real deadline: `hands_played` exists only at the instant a duel finishes, so the
column had to exist before the first real duel or the data was gone. **This decision has no such
deadline.** A display name does not exist until a player types it, and the read-time join means a
name set next year correctly labels duels played today. The only variant with now-or-never pressure
is snapshotting the name into `duel_result` at duel time — rejected below on its own demerits, not
postponed. The reason to decide now is ordinary sequencing: `EPIC-03`'s results list needs the
field, and the planner needs a shape to ticket — not data loss.

## Consequences

**What it buys.** The result line can show a human a name instead of a UUID, `EPIC-03` is
unblocked, and `EPIC-04`'s real identity attaches to a profile that already has the name column it
would have wanted.

**What it costs.**

- A third migration, and the first HTTP endpoint that writes — a new surface with validation and a
  new port, where before the entire HTTP layer was provably read-only.
- Every open product rule constrains a later ticket: if the human decides names are unique, a
  future migration must add the constraint *and resolve any duplicates already stored* — that
  dedup is the price of not assuming uniqueness now, and it is bounded (rename or suffix the
  losers) but not free.
- `DuelSummaryResponse` changes shape again — a wire-visible addition to a response already in
  `develop`, though additive and nullable, so no client breaks.

**What it forecloses.** Showing the name *as it was at duel time* for any duel played before a
future snapshot column exists. That is the one genuinely irreversible edge here, accepted
deliberately: the current-name model is what players know from Lichess, and nothing in the vision
asks for historical names.

**What it does not settle** — carried as `DEC-017`, the human's: uniqueness, rename policy, what a
result line shows for a never-set name, and moderation. The shape above is compatible with every
answer: uniqueness is a later constraint plus dedup, rename limits are a server rule on the write
path, the unset fallback is a client rendering choice over an honest `null`, and moderation is
added validation on `PUT /api/me/name`.

## Alternatives considered

**A client-derived nickname from the player id** (e.g. `Player-3F2A`). Zero schema, zero writes,
deterministic, available today. Rejected: the human decided names are player-chosen — and a
derived label dressed as a name would read as an identity nobody chose.

**The opponent's player id as the label.** Honest and already shipped. Rejected by the decision
this ADR records: an id is not a name.

**Snapshot the name into `duel_result` at duel time.** Its strongest case: history shows who you
actually played, as they were named then — and it is the only variant with a real deadline, which
argues for deciding it now rather than never. Rejected: it stores every name twice per duel,
diverges from the current-name convention players already understand, and the deadline is only an
argument if the product wants historical names, which nothing suggests.

**A separate `display_name` table.** Room for history, multiple aliases, per-name metadata.
Rejected: the need is one optional attribute of `player`, 1:1; a column is the whole requirement,
and a table is structure nobody asked to keep correct.

**Add `UNIQUE` now.** Its strongest case: retrofitting uniqueness later means deduplicating live
data. Rejected: it silently answers `DEC-017`'s product question in the schema, and omitting it is
the cheap-to-reverse choice — a constraint can be added later; un-ringing one that refused a real
player's chosen name cannot.
