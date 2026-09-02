---
schema: 2
id: TASK-130501
title: The action-bar card draws the typed total, and the three refusals it can express
type: task
status: ready
parent: STORY-1305
module: design
estimate: S
tier: sonnet
review: standard
files_touched: 1
labels: [design, table, action-bar]
depends_on: []
verify:
  - ./design/check-drift.sh
  - awk 'index($0, "class=\"bar") { n++ } END { exit (n != 7) }' design/components/action-bar.html
  - awk 'index($0, "<span class=\"stepper\">") { n++ } END { exit (n != 7) }' design/components/action-bar.html
  - awk '{ n += gsub(/class="chip/, "") } END { exit (n != 35) }' design/components/action-bar.html
  - awk '{ n += gsub(/class="chip sel"/, "") } END { exit (n != 3) }' design/components/action-bar.html
  - awk 'index($0, "class=\"total\"") { n++ } END { exit (n != 7) }' design/components/action-bar.html
  - awk 'index($0, "class=\"notice\"") { n++ } END { exit (n != 7) }' design/components/action-bar.html
  - awk 'index($0, "<h2>") { n++ } END { exit (n != 7) }' design/components/action-bar.html
  - awk 'index($0, "class=\"l\"") { n++ } END { exit (n != 6) }' design/components/action-bar.html
  - awk 'index($0, "value=\"250\"") { n++ } END { exit (n != 1) }' design/components/action-bar.html
  - awk 'index($0, "value=\"1,200\"") { n++ } END { exit (n != 3) }' design/components/action-bar.html
  - awk 'index($0, "value=\"500\"") { n++ } END { exit (n != 1) }' design/components/action-bar.html
  - awk 'index($0, "value=\"20,000\"") { n++ } END { exit (n != 1) }' design/components/action-bar.html
  - awk 'index($0, "value=\"1o0\"") { n++ } END { exit (n != 1) }' design/components/action-bar.html
  - awk 'index($0, "500 is under the minimum of 1,200.") { n++ } END { exit (n != 1) }' design/components/action-bar.html
  - awk 'index($0, "20,000 is over the maximum of 13,400.") { n++ } END { exit (n != 1) }' design/components/action-bar.html
  - awk 'index($0, "That is not an amount.") { n++ } END { exit (n != 1) }' design/components/action-bar.html
  - awk 'index($0, "Raise to <span class=\"amt\">") { n++ } END { exit (n != 2) }' design/components/action-bar.html
  - awk 'index($0, ">Raise to</button>") { n++ } END { exit (n != 3) }' design/components/action-bar.html
  - awk 'index($0, "The total is typed as well as pressed") { n++ } END { exit (n != 1) }' design/components/action-bar.html
  - awk 'index($0, "  .total {") { n++ } END { exit (n != 1) }' design/components/action-bar.html
  - awk '$0 ~ /^  \.total \{/ && $0 ~ /\}$/ { n++ } END { exit (n != 1) }' design/components/action-bar.html
  - awk 'index($0, ".total") { n++ } END { exit (n != 2) }' design/components/action-bar.html
  - awk 'index($0, "  .notice {") { n++ } END { exit (n != 1) }' design/components/action-bar.html
  - awk 'index($0, "--pd-fs-small: 0.8125rem;") { n++ } END { exit (n != 1) }' design/components/action-bar.html
  - awk 'index($0, "aria-") { n++ } END { exit (n != 0) }' design/components/action-bar.html
  - awk 'index($0, "role=") { n++ } END { exit (n != 0) }' design/components/action-bar.html
  - awk 'index($0, "data-") { n++ } END { exit (n != 0) }' design/components/action-bar.html
  - awk 'index($0, "@keyframes") { n++ } END { exit (n != 0) }' design/components/action-bar.html
  - awk 'index($0, "animation") { n++ } END { exit (n != 0) }' design/components/action-bar.html
  - awk 'index($0, "transition") { n++ } END { exit (n != 0) }' design/components/action-bar.html
  - sh -c 'grep -q "class=\"total\"" design/components/action-bar.html && ! grep -q "class=\"total refused\"" design/components/action-bar.html'
  - python3 .github/scripts/lint_tickets.py
---

## Goal

`design/components/action-bar.html` draws the amount as a **field a player types into**, and draws
the three states only a typed entry can reach — under the server's minimum, over the stack, and not
an amount at all — each saying why in the server's own numbers, so the human can judge the control
and its copy before any client code exists.

## Why the card is first, and what stays the human's

`EPIC-13` *Design first*, `ADR-0091` §2 and `ADR-0111` §7 by name: the card that draws the field
draws its illegal states, and it merges before the implementing ticket is startable.

**That the field exists is the human's instruction and is not re-litigated** (`ADR-0111` header).
This card draws it; it does not weigh whether to.

Taste is the human's (`ADR-0024` §3): the field's border, padding, colour, alignment and nominal
width are this card's to offer and the human's to accept. Four things are **not** taste, because
`ADR-0111` settled them:

- **The words are already merged.** `500 is under the minimum of 1,200.` and
  `20,000 is over the maximum of 13,400.` are `rejection-text.ts`'s sentences with this turn's own
  bounds in them; `That is not an amount.` is `ADR-0111` §3's sentence, verbatim — no *error*, no
  *invalid input*, no exclamation mark. Copy them character for character; a gate pins each at one.
- **Nothing the player typed is ever rewritten** (§1, §3). The three refusal frames show the entry
  **exactly as typed** — `500`, `20,000`, `1o0` — never nudged to a bound, never blanked.
- **The action button never prints a different amount** (§7, `ADR-0100` §2). On the three refusal
  frames the aggressive button reads `Raise to` with **no figure**. A corrected total there is the
  clamp coming back through the paint, and it would also break the e2e driver's read-before-click
  contract. A gate pins `Raise to <span class="amt">` at 2 — the two legal frames and no others.
- **There is no silent dead button** (§2, and §Alternative 3). Every refusal frame both keeps the
  buttons live *and* carries the sentence. The card may not answer a press with silence.

## The four constraints that are not taste

- **The field takes the stepper's readout slot, and the ± buttons stay drawn.** `DEC-102` — the
  stepper's step — is **open and stays the product owner's** (`ADR-0111` §Consequences). Deleting
  the ± from this card would read as answering it. So the drawing keeps
  `<span class="stepper">`, and only its `<span>`-of-digits becomes an `<input class="total">`.
  `<span class="stepper">` is gated at 7 for that reason.
- **The client will ship a *subset* of what this card draws.** `ActionBar.tsx` has no stepper and
  will build the field alone, so the row the client must fit is **narrower** than the row drawn
  here. That makes this card's fit judgement conservative in the right direction, and it is why the
  fit can be judged from a drawing that includes controls nobody builds.
- **The bar's height still does not change with its state**, which is this card's own standing rule
  (*"both rows are reserved in every state"*). The notice line joins that rule: **all seven** bars
  carry a `<p class="notice">`, reserved by `min-height`, empty on the four that have nothing to
  say. That is what `ActionBar.tsx` already does with its own notice line, so this card stops being
  a row short of the client.
- **Mint no new token.** Every `--pd-*` the drawing uses must already be declared in
  `design/tokens/tokens.css`, and any token this card uses in a `var()` must also be **inlined in
  its own `:root`** or the drawing renders wrong while `check-drift.sh` stays quiet. The one
  addition needed is `--pd-fs-small: 0.8125rem;`, the sheet's exact value, for the notice line's
  reserved height. A new token would drag the vendored `web-client/src/styles/tokens.css` in with
  it (`tokens.test.ts` compares buffers) — a second file this ticket does not have.

## What is already true, measured on `develop` 2026-09-03

- `class="bar` **4**, `<span class="stepper">` **4**, `<h2>` **4**, `class="l"` **3** — one frame
  (*facing 400*) has no caption today, which is why the caption count goes to 6 and not 7.
- **The chip gates count occurrences, not lines, and this matters.** The card packs two chips per
  line (`<button class="chip">min</button><button class="chip">⅓</button>`), so a line-counting
  `index($0, …)` gate would silently depend on how the new frames wrap. Counted with `gsub`,
  `class="chip` is **20** today (four frames × five) and `class="chip sel"` is **3**. Three new
  frames of five chips each take the first to **35** and leave the second at 3 whatever the packing.
- `class="total"`, `class="notice"`, `input`, `13,400`, `aria-`, `role=`, `data-`, `@keyframes`,
  `animation` and `transition` are all **0**. The refusal gates below can therefore be exact.
- `Raise to <span class="amt">` appears **2** times (the *facing 400* and *sent* frames) and
  `>Raise to</button>` **0**, so both directions of the *never a different amount* rule are
  gateable.
- `--pd-fs-small` is declared in the sheet as `0.8125rem` and is **not** inlined in this card;
  `--pd-lh-body`, `--pd-font-mono`, `--pd-fs-body`, `--pd-hairline`, `--pd-text`, `--pd-text-muted`,
  `--pd-radius-medium`, `--pd-space-2` and `--pd-space-3` all already are.
- The `.l` caption idiom is `<span class="l">lower case — em dash</span>`, one per row.

## Files

| File | Action |
| --- | --- |
| `design/components/action-bar.html` | modify |
| `docs/adr/ADR-0111-an-illegal-typed-amount-is-refused-in-the-servers-own-numbers.md` | read |
| `web-client/src/table/rejection-text.ts` | read |
| `design/README.md` | read |

## Scope

- **Two CSS rules.** `.total`, written as **one physical line**, starting at column 3
  (`  .total {`) and ending with `}` — that is not fussiness: `TASK-130502` copies this rule into
  both screen cards and gates the copy by `sort -u`ing the `^  .total {` line out of all three
  files, so a rule wrapped across two lines would leave half of itself uncompared. And `.notice`,
  the line the refusal stands on, muted, centred, and
  reserved with `min-height: calc(var(--pd-fs-small) * var(--pd-lh-body))` — the client's own
  reservation. `.total` also joins the existing `.bar.disabled` selector list so a sent bar's field
  goes faint with everything else; that is the second and last line mentioning `.total`, which is
  what the gate at 2 pins.
- **`--pd-fs-small: 0.8125rem;` is added to the card's inlined `:root`**, the sheet's exact value.
- **The four existing frames keep their numbers and gain the field.** Each
  `<span class="stepper">…<span>N</span>…</span>` becomes
  `<span class="stepper"><button>−</button><input class="total" value="N"><button>+</button></span>`
  — `250` on the first frame, `1,200` on the other three. Each of the four bars also gains
  `<p class="notice"></p>`, empty.
- **Three new frames, in this order, after *Your move — facing 400*.** Each is a full `.bar` with
  the same five chips (**none** carrying `sel` — a typed total is not a preset), the same field, a
  `Fold` / `Call 400` / `Raise to` action row, and a notice:

  | `<h2>` | Field's `value` | The `.notice` says | `.l` caption |
  | --- | --- | --- | --- |
  | `Typed total — under the server's minimum` | `500` | `500 is under the minimum of 1,200.` | `the entry stands exactly as typed — no reset, no nudge — and the press sends nothing` |
  | `Typed total — over the stack` | `20,000` | `20,000 is over the maximum of 13,400.` | `the bound is this turn's own all-in total, in the server's own numbers` |
  | `Typed total — not an amount` | `1o0` | `That is not an amount.` | `an empty field and a negative are this same state; nothing is ever coerced to a number` |

  On all three the aggressive button is `<button class="btn fill">Raise to</button>` — the verb with
  **no figure**.
- **The lede gains this sentence, verbatim:** `The total is typed as well as pressed; an illegal
  one is refused in the server's own numbers and nothing is rewritten.`
- **The field is drawn the same way in every state.** The three refusal frames differ from the legal
  ones in exactly three places — the field's `value`, the missing figure on the button, and the
  notice's sentence. No second class, no `.total.refused`, no colour change on the field itself; a
  gate refuses `class="total refused"`. `ADR-0111` §2 *permits* a card to mark the state but does
  not require it, and one rule is what makes `TASK-130504`'s transcription deterministic.
- **Nothing about the field moves.** No `@keyframes`, no `animation`, no `transition` — `ADR-0115`
  refuses a fact that lives only in motion, and all three counts stay at 0.
- **The field speaks nothing on this card.** This card carries no `aria-`, no `role=` and no
  `data-` anywhere today and still carries none afterwards; the accessible name is the screen
  cards' and the client's (`TASK-130502`, `TASK-130504`).

## Out of scope

- **Building or deciding the stepper.** `DEC-102` is open and stays the product owner's
  (`ADR-0111` §Consequences). The ± buttons are drawn, unchanged, and nothing here says what they
  step by.
- **The two screen cards.** `design/screens/duel-table.html` and `duel-table-states.html` carry the
  field in place in `TASK-130502`. Do not open them.
- **Any client code.** `ActionBar.tsx` is `TASK-130504`.
- **A marked state on the field itself** — see *Scope*. Reachable later as a new card plus a repair
  ticket if the human wants it; it is not this diff.
- **Clamping, keystroke masking, auto-correction on blur, and any act conversion.** `ADR-0111`
  §§1, 3, 5 foreclose all four; reaching them again needs a superseding ADR, not a card.

## Tests

**No test file, and none is possible**: a design card is HTML nobody imports, and `ADR-0089` §2b
forbids a browser measurement being a gate. The gates are the `verify:` block — nineteen say what
must now be on the card, eight refuse what must not have appeared, and `check-drift.sh` says the
tokens, values, suit glyphs and lockup still hold.

| Marker | Count today | Count after |
| --- | --- | --- |
| `class="bar` / `<span class="stepper">` | 4 / 4 | **7 / 7** |
| `class="chip` / `class="chip sel"` *(occurrences, `gsub`)* | 20 / 3 | **35 / 3** |
| `class="total"` / `class="notice"` | 0 / 0 | **7 / 7** |
| `<h2>` / `class="l"` | 4 / 3 | **7 / 6** |
| `value="250"` / `value="1,200"` | 0 / 0 | **1 / 3** |
| `value="500"` / `value="20,000"` / `value="1o0"` | 0 each | **1** each |
| the three sentences | 0 each | **1** each |
| `Raise to <span class="amt">` / `>Raise to</button>` | 2 / 0 | **2 / 3** |
| `  .total {` / `.total` / `  .notice {` | 0 / 0 / 0 | **1 / 2 / 1** |
| `--pd-fs-small: 0.8125rem;` | 0 | **1** |
| `aria-` / `role=` / `data-` | 0 / 0 / 0 | **0 / 0 / 0** |
| `@keyframes` / `animation` / `transition` | 0 / 0 / 0 | **0 / 0 / 0** |

## Acceptance criteria

- [ ] `./design/check-drift.sh` exits 0
- [ ] `class="bar` appears exactly 7 times and `<span class="stepper">` exactly 7; counted as
      occurrences rather than lines, `class="chip` is exactly 35 and `class="chip sel"` still
      exactly 3
- [ ] `class="total"` and `class="notice"` each appear exactly 7 times
- [ ] `<h2>` appears exactly 7 times and `class="l"` exactly 6
- [ ] `value="250"` appears once, `value="1,200"` three times, and `value="500"`, `value="20,000"`
      and `value="1o0"` once each
- [ ] `500 is under the minimum of 1,200.`, `20,000 is over the maximum of 13,400.` and
      `That is not an amount.` each appear exactly once
- [ ] `Raise to <span class="amt">` still appears exactly twice and `>Raise to</button>` exactly
      three times — the refusal frames print the verb and no figure
- [ ] The lede contains `The total is typed as well as pressed` exactly once
- [ ] `  .total {` appears once and that same line ends with `}` — the rule is one physical line —
      `.total` appears on exactly two lines in total, `  .notice {` once, and
      `--pd-fs-small: 0.8125rem;` once
- [ ] The card still contains no `aria-`, no `role=`, no `data-`, no `@keyframes`, no `animation`
      and no `transition`, and no `class="total refused"`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
