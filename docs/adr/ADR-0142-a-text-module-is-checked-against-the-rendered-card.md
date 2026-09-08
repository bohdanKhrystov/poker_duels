# ADR-0142 — A text module is checked against the rendered card, in the client's own suite

- **Status:** Accepted
- **Date:** 2026-09-08
- **Resolves:** `DEC-155` — **the architect's** — by what mechanism does a text module stay in step
  with the design card it was transcribed from? Registered 2026-09-08 while landing `TASK-140811`,
  on a gap `TASK-140710` and `TASK-140811` reported independently.
- **Applies:** [`ADR-0091`](ADR-0091-design-gets-no-agent-a-new-screen-owes-a-card.md) §4 and
  [`ADR-0024`](ADR-0024-design-follows-the-code-workflow.md) §2. **It amends neither.** §1 below is
  `ADR-0091` §4's own sentence carried out on a second fact.
- **Constrains:** where a design-conformance gate may live, what `design/screens/*.html` may rely on
  a gate reading, and the shape of every future transcription ticket.

## Context

**The words are checked once and never again.** `TASK-140710` transcribed six strings from
`design/screens/name-ask.html` into `web-client/src/profile/name-ask-text.ts`; `TASK-140811`
transcribed three from `design/screens/account.html` into `web-client/src/account/account-text.ts`.
Both tickets carried `verify:` greps comparing module to card, and a `verify:` block does not
outlive its merge. Nothing in `design/check-drift.sh`, `design/check-frame-cards.sh`,
`.github/workflows/build.yml` or `.github/workflows/tickets.yml` compares a transcribed module to
its card. `ADR-0091` §2 makes the card the place a screen's words are decided; after the merge,
nothing holds the code to it.

**Four things pull against each other, and one of them is already a scar.**

*The gate has to be somewhere, and one of the two obvious homes is closed by a merged ADR.*
`ADR-0091` §4 says, in its own words: *"`check-drift.sh` does not reach into `web-client/`"* and
*"Adoption is gated where consumption happens — the client's own CI job (`ADR-0026`)"*, and its
rejected alternative *"Extend `check-drift.sh` into `web-client/`"* gives the reason: *"a consumer's
conformance is checked in the consumer's contract tests — a pattern this repo has merged three times
over, `tokens.test.ts` already reading `../../../design/tokens/tokens.css`."* So a shell gate under
`design/` reading a TypeScript module is not an open option. That is a constraint, not a preference,
and it is the first force.

*A new shell gate is a gate nobody wires — measured, not feared.* `design/check-frame-cards.sh` was
written by `TASK-141107` as a real invariant gate over `duel-table-states.html`. It appears in the
`verify:` block of three tickets and **in no workflow**: `tickets.yml` runs `./design/check-drift.sh`
and nothing runs `check-frame-cards.sh`. So the exact failure `DEC-155` names — *a gate that only a
ticket runs* — has already happened once here, to a design gate, and went unnoticed. (**Corrected
2026-09-08, after this ADR merged**: `TASK-140719` wired that gate into `tickets.yml` precisely
because this paragraph found it unwired. The paragraph is left standing because the argument it makes
is about the risk a shell gate carries, and that risk was real — it had already cost this repository
one unwired gate. What is no longer true is the present tense.) Any answer that
ends in "and someone adds a line to a workflow" inherits that risk. A file the existing runner
already globs does not.

*Half a mechanism exists, and it is a third copy of the words.* `account.html` carries three
`<!-- ANON-BLOCK: … -->` comments, each holding a sentence verbatim. They were invented by
`TASK-140810` purely so `TASK-140811`'s `awk` could read the sentences back out, and the only gate on
them is that ticket's `awk … END { exit (n != 3) }` — **a count of three**. Nothing compares a marker
to the `<p class="line">` beside it. The marker is therefore an unverified duplicate sitting next to
the rendered text a human actually approved at the pane (`ADR-0024` §3). Extending markers to every
card would build the gate on the copy nobody checks, while the thing the human accepted drifts
freely — which inverts `ADR-0091` §2. And the markers exist for a shallow reason: `account.html`
wraps its `<p class="line">` text across two source lines and `name-ask.html` does not, so a
line-oriented reader can join one card's sentences and not the other's.

*Reading a module's strings is not the same as reading its source.* Two of `account-text.ts`'s
exports — `SIGN_OUT_WARNING` and `SIGN_UP_THROTTLED` — are written as `"…" + "…"`. Their runtime
values exist nowhere in the file as a single literal; `grep -c` for `SIGN_OUT_WARNING`'s full
sentence against `account-text.ts` returns **0**. A `grep -F`-over-source gate is structurally blind
to exactly the longest and most consequential strings, and blind *silently*, because a string it
cannot see is a string it never has to report on.

**And the direction question is not rhetorical.** The nearest precedent, `HttpEndpointDocumentationTest`,
is asymmetric per DTO and in *both* directions depending on the DTO:
`theDocumentedFieldNamesAllExist` asserts documented ⊆ reflected for `ProfileResponse` and
`DuelSummaryResponse`; `theSignUpSectionNamesEveryFieldTheRequestHas` and
`theRecentDuelsSectionNamesEveryFieldTheResponseHas` assert reflected ⊆ documented for
`SignUpRequest` and `RecentDuelsResponse`. **No DTO in that file gets both**, so for every one of
them a staleness of one kind is invisible. Repeating that shape here without saying so would be the
cheapest way to ship a gate that reads as complete and is not.

**The measurements this decision is built on**, taken against `4b59ee42` and, where noted, against
`56596a6b`, `TASK-140710`'s merge:

| Fact | Measured |
| --- | --- |
| `name-ask-text.ts` exported string constants | 6, of which **6** appear in `name-ask.html`'s rendered text |
| `account-text.ts` exported string constants | 27, of which **12** appear in `account.html`'s rendered text — the three `TASK-140811` transcribed among them, found in the rendered `<p class="line">` text with comments already stripped, so no marker was read |
| `*-text.ts` modules under `web-client/src` | 13; two are named by `DEC-155` |
| `SIGN_OUT_WARNING`'s value as one literal in its own source | 0 occurrences |
| Workflows invoking `check-frame-cards.sh` | 0 **as measured on 2026-09-08 when this ADR was written**. [`TASK-140719`](../../tasks/tasks/TASK-140719-the-frame-card-gate-runs-in-ci.md) has since wired it into `tickets.yml`, on this ADR's own finding. The count is left as it was measured because §Context's argument rests on what was true then; the reader should know it is no longer true now |
| The six `name-ask` strings at `TASK-140710`'s merge | all 6 on the card — this gate would have been **green** |

## Decision

### 1. The check is a test in the client's own suite

It is a vitest file, `web-client/src/design/card-text.test.ts`, carrying
`/* @vitest-environment node */` and reading each card with `readFileSync` off a
`new URL("../../../design/screens/…", import.meta.url)` — byte for byte the arrangement
`web-client/src/styles/tokens.test.ts` already uses to read `design/tokens/tokens.css`.

This is `ADR-0091` §4 applied, not amended: the client consumes `design/`, so the client's contract
tests are where conformance is checked, and `build.yml`'s `client` job already runs `npm run check`
→ `vitest run` on every pull request. **No workflow file changes**, because `vitest run` globs the
file into existence as a gate. No dependency is added: vitest and node's `fs` are both already
present. `ADR-0088` §1 is untouched on both of its counts — its dependency clause names browser
drivers specifically (*"No Playwright, Puppeteer, Selenium, WebDriver or Cypress dependency enters
`web-client/package.json`"*), and nothing enters it at all here; and its second clause,
*"`.github/workflows/build.yml` keeps its two jobs"*, is the one this decision actually leans on.

`design/check-drift.sh` and `design/check-frame-cards.sh` are neither replaced nor extended. They
stay what they are — `design/`'s self-consistency, one file's frames — and this gate sits beside
them on the other side of the fence, pointing the other way.

That `check-frame-cards.sh` runs in no workflow is a **separate defect**, found while deciding this
and deliberately not fixed here: wiring it is a one-line change to `tickets.yml` that belongs to its
own ticket, not to an ADR about text modules. It is recorded here because the evidence it supplies —
that a design gate can sit unwired and unnoticed — is load-bearing for §1, and a finding nobody
writes down is a finding nobody acts on.

### 2. The card is read as rendered text, and a comment is not card text

The reader takes a card's HTML and returns a list of **text units**, a unit being the contiguous
text of one element:

1. every `<!-- … -->` comment is replaced by the separator,
2. every `<script>…</script>` and `<style>…</style>` body is replaced by the separator,
3. every remaining tag is replaced by the separator,
4. `&amp; &lt; &gt; &quot; &apos; &nbsp;` and numeric character references are decoded,
5. the result is split on the separator, each piece has its whitespace runs collapsed to one space
   and is trimmed, and empty pieces are dropped.

**The separator is NUL, and deliberately not a newline.** A newline separator is silently wrong: it
also splits at the card's *own* line wraps, which cut `account.html`'s three `<p class="line">`
sentences in half and made all three read as absent from a card that plainly carries them. Measured
on this branch before the sentinel was chosen.

Comments are stripped **first and on purpose**: a marker comment must not be able to satisfy this
gate. What the gate reads is what the card renders, which is what the human accepted.

A carded string is matched as a substring of **one** unit, so a sentence spanning an inline element
does not match. That failure is loud — the string is reported absent — and never a silent pass.

### 3. The `ANON-BLOCK` markers are not the mechanism, and they are removed

No card gains marker comments. `design/screens/account.html`'s three `<!-- ANON-BLOCK: … -->` lines
are deleted by the ticket that implements this ADR, and `TASK-140810`'s count-of-three gate goes with
them. §2's normalisation is what they were standing in for, and an uninspected duplicate of a
sentence, sitting one line from the sentence, is drift bait rather than a defence against drift.
Deleting a comment changes nothing a human sees, so `ADR-0024` §3's visual verdict is not in play.

### 4. The unit of correspondence is a module and a card, paired one to one

The test declares a `PAIRS` table of *module path → card path*. A module names exactly one card;
several modules may name the same card, which `name-ask.html` already needs — it carries both
`name-ask-text.ts`'s six strings and one of `name-text.ts`'s.

**Not the frame.** `name-ask.html`'s four frames do not each carry all six strings: the *Nothing
reached the server* frame carries the heading and *Duel without a name* and nothing else of the six.
Frame scope therefore needs a per-string module→frame map — new metadata invented in service of its
own check, which is the shape `ADR-0091`'s rejected coverage gate names and refuses.

**Not the marked block**, per §3.

### 5. Each pair declares a partition, and the partition is checked in both directions

A pair declares two lists of **export names**: `carded`, and `notCarded` with a one-line reason each.
The test asserts, for every pair:

- **a.** every exported string constant of the module is in exactly one of the two lists — so an
  export in neither fails, and a new export cannot enter the module unclassified;
- **b.** every name in either list is an actual export of the module — so deleting a carded string
  and leaving its register entry fails, and so does renaming one;
- **c.** for every name in `carded`, the **imported value** — normalised by collapsing whitespace
  runs to one space and trimming — is a substring of one text unit of the paired card.

The value in **c** is obtained by importing the module, never by grepping its source, for the reason
measured in §Context: a hand-concatenated string has no single literal to grep.

**Which strings are carded and which are not is not decided here.** That call belongs to the card and
its ticket (`ADR-0091` §2, `ADR-0024` §5). This ADR decides only that the call is *declared*, in one
place, where forgetting to make it fails a test.

### 6. Coverage is total over `*-text.ts`, so a new module cannot escape the register

The test globs `web-client/src/**/*-text.ts` (excluding `*.test.ts`) and asserts every match is
either in `PAIRS` or in a `NO_CARD` list carrying a one-line reason. Thirteen such modules exist
today; two are paired, and the other eleven enter `NO_CARD` — most of them under one reason, that no
card yet exists for their screen, which is `ADR-0091` §5's registered and still-open debt. The list
is that debt written where a gate can see it.

Without this clause `PAIRS` is a hand-list, and the next transcription ticket that forgets to add a
row reproduces `DEC-155` one level up.

### 7. Direction: names both ways, values one way, and the residual is named out loud

- **Export name ⇄ register: both directions, closed** by §5a and §5b. Nothing enters or leaves a
  paired module silently. This is what `HttpEndpointDocumentationTest` gives no DTO.
- **Carded value ⇒ card text: enforced** by §5c.
- **Card text ⇒ module: not enforced.** A sentence on a card that no module carries is invisible to
  this gate. The reason is not oversight: a card is mostly not product copy. `name-ask.html` alone
  carries an `<h1>`, four `<h2>` frame headings, and `<p class="note">` blocks naming
  `mayTryAgain` and `ADR-0119 §2`; `account.html` carries a note citing five ADRs and an open `DEC`.
  Separating product copy from card furniture requires marking it — which is precisely the marker
  register §3 refuses, and would cost every card what it costs.

The named trigger for reopening the third bullet: **the first time a stale sentence is found on a
card** — in a review, a UAT round, or a repair ticket. At that point the class has fired once, which
is this repository's standing condition for building the register that catches it (`ADR-0024`'s
generator clause, `ADR-0091` §Consequences).

## Consequences

**What it buys.** The words the human accepted and the words the product ships are compared on every
pull request, by a file that needs no workflow entry to run. A coder who edits a carded string
without touching the card fails `client`; so does one who edits the card without touching the module.
A new export cannot slip into a paired module unclassified, and a removed one cannot leave a stale
register entry behind. `check-drift.sh` keeps its scope and its language, and no dependency enters
`package.json`.

**What it costs.**

- **A bootstrap register of roughly twenty-six declared lines**, written once and reviewed by hand:
  fifteen `notCarded` entries for `account-text.ts` — 27 exports, 12 on the card — and eleven
  `NO_CARD` entries for the unpaired text modules. This is the real price of §5's and §6's totality,
  and it is not small. A reviewer must actually read those twenty-six reasons, or the register
  becomes a list of excuses that passes.
- **Every future text module costs one classification**, forever, and every new export in a paired
  module costs another. A ticket that adds a refusal message now edits a register as well.
- **A carded sentence may never span an inline element.** If a card ever wants `<em>` inside a
  sentence the code owns, §2 reports the string absent and the card must be reworded or the string
  split. That is a real constraint on future card authoring, paid to keep the reader dependency-free.
- **`account.html` loses three comments**, and with them `TASK-140810`'s count gate. Anyone reading
  that merged ticket will find a gate that no longer exists.
- **The gate cannot tell a card from an *approved* card.** It proves the module matches the file;
  the human's visual verdict still trails the merge (`ADR-0091` §3), so a green gate over a card
  nobody has looked at means only that the two copies agree — never that the words are right.

**What it forecloses.** Marker comments as a design-card convention: after §3 no card carries them,
and a future gate wanting bounded regions in a card starts from nothing. That is deliberate and it is
the one genuinely closed door. It is also the cheapest thing here to reverse — markers are additive,
and a later ADR that needs regions can add them to the cards that need them without touching §1, §2,
§4 or §5.

**Why this shape, on thin evidence.** Two pairs is a small sample to design a register from. The
parts that would be expensive to undo — the gate's *home* — are the parts a merged ADR already fixed
(§1), so the discretion left is mostly in the register's shape, and a register that lives in one test
file is a one-commit change if it proves wrong. Nothing here is written into a card, a build file or
a workflow, which is what keeps the reversal cheap.

**The deadline is real and runs one way.** The bootstrap register is sized by the number of text
modules that exist when it is written: thirteen today, and one more with every screen `EPIC-14` and
its successors ship. Deciding now costs twenty-six lines; the same decision after two more epics
costs more, and every transcription ticket that lands in between is another pair of files verified
once and never again. This is a reason to decide now, not a reason to have decided differently.

## Alternatives considered

**Extend the `ANON-BLOCK` markers to every card, and have a shell gate read them.** The strongest
case: half of it is already built and merged, it is the cheapest thing to finish, the marker is
trivially machine-readable with no HTML parsing at all, and it makes explicit — in the card, where a
designer can see it — exactly which sentences the code is required to copy, which is real
documentation value that §2's approach gives up. Rejected on three counts. The marker is a copy of
the rendered sentence that **nothing checks**: `TASK-140810`'s gate counts markers and never compares
one to the `<p class="line">` above it, so the gate would certify code against an unverified
duplicate while the accepted render drifts — inverting `ADR-0091` §2, which is the clause the whole
exercise exists to enforce. It buys nothing §2 does not: the only reason a marker was needed is that
`account.html` wraps its lines, and whitespace normalisation dissolves that reason entirely —
measured, all three sentences found in the rendered text with no marker read. And it taxes every card
forever with an obligation whose omission is invisible on the card itself.

**A `grep -F` for each literal, from a shell gate or a `verify:` line.** Strongest case: it is the
simplest possible thing, it needs no parser on either side, it is what both tickets already did by
hand, and it would have passed both of them. Rejected because it is silently incomplete on the module
side — `SIGN_OUT_WARNING` and `SIGN_UP_THROTTLED` are hand-concatenated and their values appear zero
times in the source as a literal, so the two longest sentences in `account-text.ts` are precisely the
ones a grep gate cannot see — and because a per-literal grep list is a hand-list with no completeness
assertion, so a string added later is outside the gate by default. It also lands in the wrong half of
the repository under `ADR-0091` §4.

**A Node one-liner in `tickets.yml`, beside `check-drift.sh`.** Strongest case: it keeps every design
gate in one workflow where a reader looks for them, and the `tickets` job is fast and has no
dependency on `npm ci`, so it would fail sooner than the `client` job does. Rejected on `ADR-0091`
§4's placement rule, quoted in §Context, and on the evidence of `check-frame-cards.sh`: a gate whose
existence depends on someone remembering a workflow line is the failure mode `DEC-155` was registered
about, and this repository has an unwired design gate sitting in `design/` right now to prove it. A
one-liner would also have to reach a TypeScript module's *values*, which is the thing node cannot do
without the toolchain the `client` job already has.

**Frame-scoped or block-scoped correspondence.** Strongest case: it is strictly more precise — it
would catch a string that moved to the wrong frame, which file scope cannot, and `check-frame-cards.sh`
exists because whole-file counts let exactly that defect through on `duel-table-states.html`. That
precedent is the strongest argument against file scope and it is a real one. Rejected because the
analogy does not carry: `check-frame-cards.sh` compares a frame against a *measured expectation table*
for two named frames, whereas a module has no frame — mapping six strings onto four frames that carry
different subsets of them (measured: the fourth carries two of six) needs a per-string frame register
in addition to everything §5 already asks for. The precision is real and unbought; the trigger for
buying it is a string appearing on the wrong frame, and that has not happened.

**Assert `carded` values only, with no partition and no `*-text.ts` sweep.** Strongest case: it is
the whole of what `DEC-155` literally asks for, it costs none of the twenty-six bootstrap lines, and
it catches every drift either ticket was worried about. Rejected because it decays exactly the way
the thing it replaces decayed: a hand-list with no completeness assertion covers what someone
remembered on the day, and the next export, the next module and the next deletion are all outside it
with nothing saying so. The twenty-six lines are the price of the gate being able to state what it
does *not* cover.

**Do nothing, and rely on ticket `verify:` blocks.** Strongest case: it is what happened, it cost
nothing, and it was **correct both times** — all six `name-ask` strings matched their card at
`TASK-140710`'s merge and still match today, so no drift has actually occurred and this ADR fixes no
live defect. Rejected because the property being defended is not *are they equal now* but *will the
next edit be caught*, and a `verify:` block answers only the first. Two independent tickets asking
the same question and getting *none* for an answer is the signal that the second question has no
owner.
