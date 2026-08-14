---
schema: 2
id: TASK-030206
title: The theme's type, spacing and radii are the tokens and nothing else
type: task
status: ready
parent: STORY-0302
module: web-client
estimate: S
tier: haiku
review: standard
files_touched: 1
labels: [client, design, styling]
depends_on: [TASK-030205]
verify:
  - cd web-client && npm ci
  - cd web-client && npm run build
  - grep -rqE -e '--font-ui: *var\(--pd-font-ui\)' web-client/dist/assets
  - grep -rqE -e '--text-title: *var\(--pd-fs-title\)' web-client/dist/assets
  - grep -rqE -e '--spacing-5: *var\(--pd-space-5\)' web-client/dist/assets
  - grep -rqE -e '--radius-card: *var\(--pd-radius-card\)' web-client/dist/assets
  - grep -rqE -e '--shadow-pop: *var\(--pd-shadow-pop\)' web-client/dist/assets
  - grep -rl -e '--spacing:' web-client/dist/assets | grep -c . | grep -qx 0
  - cd web-client && NO_COLOR=1 npm run --silent test 2>&1 | grep -qE 'Tests +11 passed \(11\)'
  - cd web-client && npm run check
  - ./gradlew :poker-server:verifyProtocolTypes
---

## Goal

Tailwind's type, weight, leading, tracking, spacing, radius and shadow utilities resolve to `--pd-`
custom properties, and Tailwind's own scales are gone.

## Files

| File | Action |
| --- | --- |
| `web-client/src/styles/app.css` | modify |

## Scope

- Append to the existing `@theme static` block, below the colours, keeping the file's one
  declaration per line. Each group resets Tailwind's namespace first, then maps the tokens
  one-for-one:

  ```css
    --font-*: initial;
    --font-ui: var(--pd-font-ui);
    --font-mono: var(--pd-font-mono);

    --text-*: initial;
    --text-micro: var(--pd-fs-micro);
    --text-small: var(--pd-fs-small);
    --text-body: var(--pd-fs-body);
    --text-large: var(--pd-fs-large);
    --text-title: var(--pd-fs-title);
    --text-display: var(--pd-fs-display);
    --text-hero: var(--pd-fs-hero);

    --font-weight-*: initial;
    --font-weight-regular: var(--pd-weight-regular);
    --font-weight-medium: var(--pd-weight-medium);
    --font-weight-bold: var(--pd-weight-bold);

    --leading-*: initial;
    --leading-tight: var(--pd-lh-tight);
    --leading-body: var(--pd-lh-body);

    --tracking-*: initial;
    --tracking-caps: var(--pd-track-caps);

    --spacing: initial;
    --spacing-1: var(--pd-space-1);
    --spacing-2: var(--pd-space-2);
    --spacing-3: var(--pd-space-3);
    --spacing-4: var(--pd-space-4);
    --spacing-5: var(--pd-space-5);
    --spacing-6: var(--pd-space-6);
    --spacing-7: var(--pd-space-7);
    --spacing-8: var(--pd-space-8);
    --spacing-9: var(--pd-space-9);

    --radius-*: initial;
    --radius-small: var(--pd-radius-small);
    --radius-medium: var(--pd-radius-medium);
    --radius-card: var(--pd-radius-card);
    --radius-pill: var(--pd-radius-pill);

    --shadow-*: initial;
    --shadow-pop: var(--pd-shadow-pop);
  ```

- `--spacing: initial;` is the bare key, not a `*` glob, and it is the load-bearing line of this
  ticket. Tailwind derives `p-4`, `gap-2`, `w-64` and the rest by multiplying a single `--spacing`
  base, so leaving it in place would let any component invent a size that is not on the ladder.
  Cleared, the numbered utilities resolve only to `--spacing-1` … `--spacing-9`, which are the
  design system's nine steps — so `p-4` is the ladder's step 4 (12px), not Tailwind's 1rem. That is
  the intent: `tokens.css` says "one ladder, named by step".
- The theme test from `TASK-030205` covers these lines unchanged — it already accepts
  `--x-*: initial;`, `--x: initial;` and `--x: var(--pd-y);` and checks every referenced token
  exists. It needs no edit, and its assertions do not move.

## Out of scope

- Breakpoints, z-index, easing, transitions, `--inset-shadow-*` and `--drop-shadow-*`. They are
  structural, carry no design value from `EPIC-06`, and clearing them buys nothing.
- Reformatting or reordering the colour declarations above.
- Adding a scale step the token sheet does not have. A size the client needs and `design/` lacks is
  an `EPIC-06` ticket — raise it and stop.
- Using any of these utilities — `TASK-030208`.

## Proof

| Command | Proves |
| --- | --- |
| the five `--x: var(--pd-y)` greps over `dist/assets` | one sample per group reached the bundle as a reference: font stack, type scale, spacing ladder, radius, shadow |
| `--spacing:` appears nowhere in `dist/assets` | Tailwind's spacing base is cleared, so no utility can multiply its way to an off-ladder size |
| `Tests 11 passed (11)` | the theme test and the colour-literal guard both still hold over the bigger block |

Watch it fail: delete `--spacing: initial;`, rebuild, and the `--spacing:` grep goes red because
Tailwind emits its own base again. Restore it and say in the PR what you saw.

## Acceptance criteria

- [ ] The `@theme static` block resets `--font-*`, `--text-*`, `--font-weight-*`, `--leading-*`,
      `--tracking-*`, `--spacing`, `--radius-*` and `--shadow-*`
- [ ] It maps all twenty-nine type, weight, leading, tracking, spacing, radius and shadow tokens
- [ ] The built CSS contains `--font-ui`, `--text-title`, `--spacing-5`, `--radius-card` and
      `--shadow-pop`, each as a `var(--pd-…)` reference
- [ ] The built CSS contains no `--spacing:` declaration
- [ ] `npm run --silent test` reports `Tests  11 passed (11)`
- [ ] `cd web-client && npm run check` exits 0
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
