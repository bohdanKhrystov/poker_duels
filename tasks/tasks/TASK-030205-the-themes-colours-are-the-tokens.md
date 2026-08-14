---
schema: 2
id: TASK-030205
title: The theme's colours are the tokens and nothing else
type: task
status: ready
parent: STORY-0302
module: web-client
estimate: S
tier: sonnet
review: standard
files_touched: 2
labels: [client, design, styling]
depends_on: [TASK-030204]
verify:
  - cd web-client && npm ci
  - cd web-client && npm run build
  - grep -rqE -e '--color-surface: *var\(--pd-surface\)' web-client/dist/assets
  - grep -rqE -e '--color-suit-red: *var\(--pd-suit-red\)' web-client/dist/assets
  - grep -rl 'oklch(' web-client/dist/assets | grep -c . | grep -qx 0
  - cd web-client && NO_COLOR=1 npm run --silent test 2>&1 | grep -qE 'Tests +11 passed \(11\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'declares nothing but resets and references to tokens that exist'
  - cd web-client && npm run check
  - ./gradlew :poker-server:verifyProtocolTypes
---

## Goal

Tailwind's colour utilities resolve to `--pd-` custom properties, and Tailwind's own palette is gone.

## Files

| File | Action |
| --- | --- |
| `web-client/src/styles/app.css` | modify |
| `web-client/src/styles/theme.test.ts` | create |

`web-client/src/styles/tokens.css` is read while writing the block — it is the list below — but it
is not edited.

## Scope

- `app.css` gains a `@theme static` block between the imports and the `:root` rule. **`static`
  matters**: without it Tailwind emits only the theme variables some utility already uses, so a
  token nothing references yet would silently vanish from the bundle and from every assertion about
  it.
- The block opens with `--color-*: initial;`, which deletes Tailwind's built-in palette. Without it
  `text-red-500` is a valid class in this codebase and the story is decoration.
- Then one line per colour token, mechanically: the theme key is the token name with `--pd-`
  replaced by `--color-`, and the value is a reference to that same token. All twenty-three of them,
  in the order `tokens.css` declares them:

  ```css
  @theme static {
    --color-*: initial;
    --color-bg: var(--pd-bg);
    --color-surface: var(--pd-surface);
    --color-surface-raised: var(--pd-surface-raised);
    --color-hairline: var(--pd-hairline);
    --color-overlay: var(--pd-overlay);
    --color-text: var(--pd-text);
    --color-text-muted: var(--pd-text-muted);
    --color-text-faint: var(--pd-text-faint);
    --color-text-inverse: var(--pd-text-inverse);
    --color-accent: var(--pd-accent);
    --color-accent-fill: var(--pd-accent-fill);
    --color-accent-fill-hover: var(--pd-accent-fill-hover);
    --color-on-accent: var(--pd-on-accent);
    --color-accent-subtle: var(--pd-accent-subtle);
    --color-win: var(--pd-win);
    --color-loss: var(--pd-loss);
    --color-warn: var(--pd-warn);
    --color-card-face: var(--pd-card-face);
    --color-suit-red: var(--pd-suit-red);
    --color-suit-black: var(--pd-suit-black);
    --color-card-back: var(--pd-card-back);
    --color-coin: var(--pd-coin);
    --color-coin-deep: var(--pd-coin-deep);
  }
  ```

  The names read oddly in places — `bg-bg`, `text-text-muted` — and that is the price of a mapping
  with no judgement in it. A prettier alias is a name this client invented, and the client invents
  nothing.
- One declaration per line, no blank line inside a group, closing `}` alone on its line: the test
  parses this block.

## Out of scope

- Type, spacing, radii and shadow — `TASK-030206`, appended to the same block.
- `--pd-focus` and `--pd-focus-offset`. They are an `outline` shorthand and its offset, and Tailwind
  has no namespace for them; components will use `outline: var(--pd-focus)` in CSS directly.
- `@theme inline`. The tokens are one dark palette declared once on `:root`, so there is no scope
  for an inlined reference to resolve differently in.
- Adding, renaming or retuning a token. A colour the client needs and `design/tokens/tokens.css`
  does not have is an `EPIC-06` ticket — raise it and stop.
- Using any of these utilities — `TASK-030208`.

## Tests

`web-client/src/styles/theme.test.ts`, describe block `"the Tailwind theme"`

| Test | Proves |
| --- | --- |
| `declares nothing but resets and references to tokens that exist` | every line in the `@theme static` block is either `--x-*: initial;`, `--x: initial;`, or `--x: var(--pd-y);` where `--pd-y` is declared in `tokens.css` |

How it works: read `app.css` and `tokens.css` through `import.meta.url`; take the text between
`@theme static {` and the first line that is exactly `}`; ignore blank lines and comments; classify
each remaining line; collect the ones that do not fit, or that reference a `--pd-` name absent from
`tokens.css`; assert that collection is empty. **Also assert the block held more than twenty
declarations** — a parse that silently matched nothing would otherwise pass forever.

This one test is what makes "the theme references, never restates" enforceable: a hex written here
fails it, and so does a reference to a token that does not exist.

## Proof

| Command | Proves |
| --- | --- |
| `--color-surface: var(--pd-surface)` in `dist/assets` | the theme reached the bundle as a reference, not a value |
| `--color-suit-red: var(--pd-suit-red)` in `dist/assets` | a token no utility uses is still emitted — `@theme static` is doing its job |
| no `oklch(` anywhere in `dist/assets` | Tailwind's built-in palette is gone. Every default colour it ships is written in `oklch()`, and nothing else in this bundle uses that function |
| `declares nothing but resets and references to tokens that exist` | no hex, and no invented token name |

Watch it fail twice: replace one value with its hex from `tokens.css`, run `npm run check`, and both
the theme test and the colour-literal guard go red. Then delete `--color-*: initial;`, rebuild, and
the `oklch(` grep goes red. Restore both and say in the PR what each failure said.

## Acceptance criteria

- [ ] `app.css` contains a `@theme static` block with `--color-*: initial;` and twenty-three
      `--color-…: var(--pd-…)` declarations
- [ ] `the Tailwind theme > declares nothing but resets and references to tokens that exist` passes
- [ ] The built CSS contains `--color-surface:var(--pd-surface)` and `--color-suit-red:var(--pd-suit-red)`
- [ ] The built CSS contains no `oklch(`
- [ ] `npm run --silent test` reports `Tests  11 passed (11)`
- [ ] `cd web-client && npm run check` exits 0
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
