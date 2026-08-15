# ADR-0034 — The value gate reads CSS regions, string-aware, and fails rather than guesses

- **Status:** Accepted
- **Date:** 2026-08-15
- **Resolves:** `DEC-035` as registered on [`tasks/BOARD.md`](../../tasks/BOARD.md) — how does
  `design/check-drift.sh` read a CSS value? (The row lives on the board, not in this
  directory's own register; striking it there rides the driver's next board PR. The durable
  reference is the question, not the number.)
- **Constrains:** [`TASK-060121`](../../tasks/tasks/TASK-060121-semicolonless-declaration-enters-the-gate.md)
  (which it unblocks), [`TASK-060123`](../../tasks/tasks/TASK-060123-the-gates-remaining-silent-edges.md),
  and every future clause of `design/check-drift.sh` that has to read CSS
- **Corrects:** two premises stated in `DEC-035`'s own registration — see Context, point 4

## Context

Clause 3 of the design gate compares every `--pd-NAME: VALUE` a card inlines against the
canonical sheet. Its reader is the awk program `EXTRACT`: the regex `--pd-[a-z0-9-]*[ \t]*:[^;]*;`
run over the whole file, followed — in the same program — by a character loop that strips
whitespace *outside quoted strings*. The gate is therefore already half string-aware: it
normalizes as if strings existed and matches as if they did not. That split is the whole bug.

**1. Four failures, each reproduced against the live `EXTRACT` while writing this ADR.**

- A final declaration with no `;` — the sheet's own last one, `--pd-shadow-card` — never enters
  the compared set. Drift it and every clause passes green.
- The same shape with text after it runs the match on to the next `;`, swallowing that text
  into the value.
- A `;` inside a quoted string (`url("data:image/svg+xml;utf8,…")`) truncates sheet and card at
  the same wrong place, so a real drift compares equal and passes green.
- A string wrapped across lines with a legal `\` continuation extracts as `"Segoe \␤    UI"` on
  the sheet and `"Segoe UI"` on a one-line card: a **false** drift, exit 1, on semantically
  identical CSS. This one is live-adjacent — `tokens.css` wraps `--pd-font-ui` and
  `--pd-font-mono`, both carrying quoted names with spaces, and all 14 cards inline both.

**2. The reader also has no idea what part of a file is CSS.** `<p>set --pd-x: 9px;</p>` and
`<!-- --pd-y: 8px; -->` each emit a record today. Prose and commented-out declarations are, to
this reader, declarations.

**3. Three attempts have been built and withdrawn** (#511, rounds 1–3; the detail is in
`TASK-060121`'s Deviations). Widening the terminator to `[;}]`, rewriting the reader as a
character walker, and enforcing the semicolon by counting candidates against records each fixed
one symptom and opened another. The pattern, not any one attempt, is the finding: **every
widening of a regex makes more prose visible, every narrowing drops more real declarations**,
and neither direction can reach a `;` inside a string.

**4. Two premises in `DEC-035`'s registration are wrong, and the correction moves the answer.**

- *"A real parser forfeits the stock-tools property `ADR-0024` relies on."*
  [`ADR-0024`](ADR-0024-design-follows-the-code-workflow.md) contains no such constraint — it is
  a convention of this script's header and its sibling gate tickets (`TASK-060111`…`-060114`),
  which is real but is not an architectural decision anyone would be overturning.
- *"…forfeits"* is wrong on the facts too: `check-drift.sh` invokes `perl` seven times and
  already contains two hand-rolled, quote-aware character walkers — `SYMEXTRACT` and `ANATOMY`
  — each with an inline self-test, and `ANATOMY` already scopes itself to `<style>` bodies and
  strips comments while tracking string state. A string-aware value reader spends nothing this
  file has not already spent. Its honest cost is a third walker to maintain.

**5. Budget is the binding constraint** ([`ADR-0007`](ADR-0007-token-lean-agent-workflow.md)).
Three review rounds have gone into this one clause. Whatever is decided has to be provable in a
single pass, which rules out any answer that improves the reader incrementally.

**6. Two clocks.** Every failure above is latent: no token value in the tree holds a quoted `;`,
`{`, `}` or a line break, and every declaration carries its semicolon. That makes a reader swap
*provably inert today* — byte-identical output across the whole tree — and unprovable the day it
stops being true. Separately, `TASK-060122` puts the gate in CI, after which the false-drift
class stops blocking one design ticket and starts blocking every PR.

## Decision

**`EXTRACT` is deleted and the value clause's reader becomes a third stock-perl walker,
`VALUES`, in `design/check-drift.sh`, modelled on the `ANATOMY` walker beside it.** Its contract
is the following seven points, and its emitted normal form is unchanged, so `LOAD`, `COMPARE`
and `PAIRCHECK` are not touched.

1. **It reads CSS regions, not files.** For `*.html` the input is the concatenation of every
   `<style>…</style>` body; for `*.css` it is the whole file. Text outside a style block no
   longer produces a value record. Names *printed* on a card remain covered by clause 1's name
   gate, which is a whole-file grep by design and does not move.
2. **Comments are stripped after region extraction, string-aware, and CSS comments only.** No
   `<!--…-->` strip is applied over the whole source.
3. **Strings are opaque and escape-aware.** `"` and `'` open a string; a `\` inside a string
   consumes the next character; a `;`, `{`, `}`, `/*` or quote inside a string is content.
4. **A declaration is `--pd-NAME`, `:`, then a value terminated by the first `;` at paren depth
   zero outside a string, or by the `}` that closes its block.** The semicolon-less final
   declaration is read correctly, not refused.
5. **Normalization preserves today's normal form**, with one addition: whitespace outside
   strings is removed as now, and *inside* a string a run of whitespace collapses to one space
   and a `\`-newline continuation is dropped — so the wrapped and unwrapped spellings of one
   string compare equal.
6. **The reader is total or loud.** It reads a file completely or exits non-zero, naming the
   file and the token: an unterminated string, an unterminated comment, an unclosed `<style>`,
   or a value that opens a `{` (a legal custom-property shape this gate does not read) fails the
   gate. It never returns a partial set. The callers' existing `st=` discipline carries this.
7. **The proof obligation — the part all three attempts lacked.** `TASK-060121` is not done
   until:
   - the readable language of point 3–4 is written into the script's header comment, beside the
     clause list;
   - an inline self-test probe covers each named shape, next to the existing `probe=`,
     `symprobe` and `anat_probe` asserts: the semicolon-less final declaration, a quoted `;`, an
     escaped quote, a commented-out declaration, a declaration in prose, a `\`-wrapped string,
     and each refusal in point 6;
   - the verify block carries one mutation fixture per silence on a scratch copy, red on today's
     tree for the two silent cases;
   - and the swap is proved **inert**: `VALUES`' output on every file under `design/` is
     byte-identical to `EXTRACT`'s output today.

## Consequences

**What it buys.** The clause that decides whether a design value has drifted stops having a case
where green means unchecked. The three shapes that produced a silent pass, a garbled value and a
false alarm are read correctly, and every shape the reader cannot read fails by name instead of
vanishing. The false-drift class — the one that would redden 14 cards after a reflow — is closed
before `TASK-060122` makes it everyone's problem.

**What it costs.**

- A third hand-rolled walker in a file that will then carry three, all of them the repo's own
  code with the repo's own bugs. The mitigation is the same one the other two have — an inline
  self-test that must fail loudly on a broken interpreter — and it is not a large mitigation.
- **Printed values leave the value clause.** No card prints one today (verified: no `--pd-NAME:`
  occurrence outside a `<style>` body in any of the 14 cards), so coverage does not move on the
  current tree; but a card that later prints a token's value in prose will not have that value
  compared. The fix, if that day comes, is an explicit machine-readable marker for a printed
  value — a new ticket — not a wider reader.
- A custom property whose value legally contains `{…}` now fails the gate instead of being
  silently mis-read. Deliberate, and it forecloses brace-bearing token streams in `--pd-*`
  without a follow-up decision.
- `perl` becomes load-bearing for a third clause. A machine without it fails the whole gate,
  which was already true and is now more true.
- `TASK-060121` becomes **implementable, not unnecessary**. It stays one file, but it is no
  longer an `S`: four proofs, a probe per shape and the inert-swap check. The planner
  re-estimates and rewrites it to this contract; the review stays `light` per `EPIC-06`.

**What it forecloses.** Making the value clause read prose again — that would now be an explicit
reversal rather than a side effect of a regex.

**Deadline.** The inert-swap proof exists only while no token exercises a silence. That is true
today and nobody is maintaining it deliberately; the first quoted `;` or wrapped string in the
sheet removes the cheapest evidence this change will ever have. This argues for deciding now,
not for deciding this way.

**What reopens it.** The day the reader needs to understand CSS beyond a flat declaration list —
at-rule preludes carrying values, nested rules, `@supports` conditions — the answer is not a
fourth walker but a real parser as a dependency, hosted by the client toolchain
([`ADR-0026`](ADR-0026-vite-and-npm-drive-the-web-client.md)). Reaching for a fifth edge case in
`VALUES` is the signal.

## Alternatives considered

**Accept the limit and record it.** The strongest case, and it led until point 4 of the Context
turned out to be false: every failure is latent, the tree is one sheet and 14 cards written by
agents to a convention, three review rounds are already spent, and a paragraph in the script
header costs a fraction of a fourth attempt at code that has failed three times — precisely the
trade `ADR-0007` tells us to make. Rejected on the shape of the limit rather than its cost. To
be honest it would have to read "no token value ever holds a quoted `;`, `{`, `}` **or a line
break**", and the sheet already wraps two quoted font stacks that all 14 cards inline: the
convention is one re-wrap away from a red gate on semantically identical CSS, and nothing a
reader of the sheet can see tells them they are about to cross it. A boundary you cross by
reflowing a line is a tripwire, not a limit. Second reason: the silence it keeps — a real drift
comparing equal — is the one failure a gate may not have, because it teaches readers that green
means nothing.

**Widen the terminator to `[;}]`** (attempt 1). Strongest case: three characters of diff, no new
program, and it fixes both semicolon-less symptoms at their root — this was `TASK-060121` as
originally written and it was right about the root cause of the symptom it addressed. Rejected
on the reproduction: with no notion of CSS regions, a wider terminator makes previously-invisible
text visible — prose and commented-out declarations lacking a `;` now terminate at the next `}`
and enter the compared set as false drift — and a value legally containing braces leaves the
gate silently. It also never reaches the quoted-`;` silence, the graver of the two.

**Enforce the semicolon by counting candidates against records** (attempt 3). Strongest case: no
parser, and it converts a silence into a loud failure, which is the right direction; it also
generalizes, since any shape the reader drops shows up as a count mismatch — the same trick
clause 4's `cited`/`strict` count and `extract_syms`' open-tag count already use successfully
elsewhere in this file. Rejected because those counts work over shapes that only appear in one
context, and this one does not: a commented-out or prose `--pd-x:` is a candidate that
legitimately yields no record, so the check fires on legal files — it hard-failed the sheet on a
legally wrapped quoted value — and where candidates and records agree exactly, the quoted-`;`
case, it sees nothing at all. Telling a candidate from a mention is the parsing problem again,
one indirection later.

**A real CSS parser as a dependency** (postcss, under the client's existing npm toolchain).
Strongest case: correctness stops being ours to maintain, every edge in this ADR disappears at
once, and `web-client` already carries Node and npm, so the repo does not gain a language. It is
also the only option that stays right as CSS grows. Rejected on reach, not on principle: the
gate is called from 16 ticket verify blocks and — after `TASK-060122` — a CI job that needs
nothing installed, and hanging all of that on `npm ci` in a different module makes the design
gate unrunnable in a fresh checkout, for a corpus whose CSS is a flat list of custom properties.
Held in reserve as the named reopening path above.

**Generate each card's inlined copies from the sheet and compare the regions byte-for-byte.**
Strongest case: it deletes the problem instead of parsing around it — a generated region needs
no reader at all, no string awareness, no comment stripping — and `ADR-0024`'s own consequences
already named a generator as the eventual answer to this duplication, "deliberately not built
while the system is four files". It is now fifteen. Rejected as an answer to a different
question: cards inline the subset of values they demonstrate, hand-placed in their own `:root`
blocks, so a generator must own subsetting, placement and the render surface's self-containment
rule — a change to how cards are authored, decided by whoever proposes it, not a change to how a
value is read.
