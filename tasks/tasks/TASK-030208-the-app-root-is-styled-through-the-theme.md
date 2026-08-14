---
schema: 2
id: TASK-030208
title: The app root is styled through the theme, proven by a test
type: task
status: done
parent: STORY-0302
module: web-client
estimate: XS
tier: haiku
review: standard
files_touched: 3
labels: [client, design, styling]
depends_on: [TASK-030207]
verify:
  - cd web-client && npm ci
  - cd web-client && npm run build
  - grep -rqE -e '\.text-title *\{' web-client/dist/assets
  - grep -rqE -e '\.bg-bg *\{' web-client/dist/assets
  - grep -rqF -e 'var(--pd-fs-title)' web-client/dist/assets
  - grep -rl 'oklch(' web-client/dist/assets | grep -c . | grep -qx 0
  - grep -rl -e '--spacing:' web-client/dist/assets | grep -c . | grep -qx 0
  - cd web-client && NO_COLOR=1 npm run --silent test 2>&1 | grep -qE 'Tests +12 passed \(12\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'renders the application heading'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'gives the heading a token-derived class'
  - cd web-client && npm run check
  - ./gradlew :poker-server:verifyProtocolTypes
---

## Goal

The app root is styled entirely through theme utilities, a test asserts the heading carries one, and
the build shows that utility compiled from the token it names.

## Files

| File | Action |
| --- | --- |
| `web-client/src/App.tsx` | modify |
| `web-client/src/App.test.tsx` | modify |
| `web-client/src/styles/app.css` | modify — **only** if the build proves source detection needs it, see below |

## Scope

- `App.tsx` becomes the heading inside a styled root, using only utilities the theme defines:

  ```tsx
  export function App() {
    return (
      <main className="min-h-screen bg-bg p-6 font-ui text-text">
        <h1 className="text-title">Poker Duels</h1>
      </main>
    );
  }
  ```

  `bg-bg`, `font-ui`, `text-text`, `p-6` and `text-title` all resolve to `--pd-` properties;
  `min-h-screen` is structural and carries no design value. Run `npm run format` and commit the
  result — `TASK-030207`'s plugin owns the class order, and the order above is what it produces.
- `App.test.tsx` gains a second `it` inside the existing `describe("App")` block. **The existing
  test does not change**: the heading's text is still `Poker Duels`, and
  `expect(screen.getByRole("heading").textContent).toBe("Poker Duels")` still holds — a class
  attribute is not text. Do not touch that assertion.
- Assert on the class list, not on the string: `className.split(" ")` and `toContain("text-title")`,
  so the sorter is free to reorder without breaking the test.

## Out of scope

- Any screen, any layout, any component beyond the root. `STORY-0305` onwards own screens; this
  ticket styles the one component that already exists, because the story has to prove the wiring
  somewhere and refuses to build a screen to do it.
- An inline `style={{ … }}`. Everything goes through a utility.
- New tokens, new utilities, new theme keys.

## Tests

`web-client/src/App.test.tsx`, describe block `"App"`

| Test | Proves |
| --- | --- |
| `renders the application heading` | **unchanged, from `TASK-030106`** — the heading still reads `Poker Duels` |
| `gives the heading a token-derived class` | `screen.getByRole("heading").className.split(" ")` contains `text-title` |

jsdom does not run Tailwind, so the test can only prove the class is on the element. What proves the
class *means* something is the build assertion below — the two together are the story's third
acceptance criterion.

## Proof

| Command | Proves |
| --- | --- |
| `.text-title {` in `dist/assets` | Tailwind saw `App.tsx`, recognised the class, and generated a rule for it. If source detection missed the component, no rule would exist |
| `.bg-bg {` in `dist/assets` | the same for the awkward name — proof that clearing the default palette did not cost us the mapping |
| `var(--pd-fs-title)` in `dist/assets` | the rule resolves, through `--text-title`, to the design token |
| `gives the heading a token-derived class` | the class is on the rendered element, not just in a file |
| `renders the application heading` still listed | the older assertion was not quietly rewritten to fit |

Watch it fail: rename `text-title` to `text-headline` in `App.tsx` and rebuild — the class is not in
the theme, so no rule is generated and the first grep goes red while `npm run build` stays green.
That is the failure a screen would otherwise ship with. Revert.

If a utility that exists in the theme produces no rule in the build, the cause is Tailwind's source
detection not reaching `src/`. That is the one case in which this ticket may touch a third file: add
`@source "../../src";` beside the imports in `app.css` and say so in the PR. Expect not to need it —
`App.tsx` is in the module graph the Vite plugin scans.

### Amended by the driver: what the two moved assertions actually guard

`TASK-030205`'s `oklch(` assertion and `TASK-030206`'s `--spacing:` assertion were moved here on the
reasoning that this ticket is the first with a colour and spacing call site, so they would finally be
able to fail. **That reasoning was incomplete.** Measured on this ticket's own component:

| component uses | resets present | resets removed |
| --- | --- | --- |
| only mapped classes (`bg-bg`, `p-6`) | 0 / 0 | **0 / 0** |
| plus `text-red-500` and `p-11` | 0 / 0 | **1 / 1** |

Both assertions fire only on a **compound** failure: an off-theme class in a component *and* the
corresponding reset missing. Either alone leaves them green. With the reset present, an off-theme
class compiles to nothing at all — which is the design working, and also why the assertion cannot
observe it.

So they are **defence in depth, not proof that the reset works.** They are kept because they cost
nothing and would catch that compound regression, but no ticket should claim they demonstrate the
reset.

What does demonstrate it is direct experiment, already recorded in `TASK-030205` and `TASK-030206`:
removing `--color-*: initial` makes `text-red-500` emit `oklch(63.7% .237 25.331)`, and removing
`--spacing: initial` makes `p-11` emit `padding:calc(var(--spacing) * 11)`.

The transferable lesson, and the third variant of it in this story: **a reset is invisible to any
assertion while the codebase obeys it.** Testing one requires deliberately violating it, which is a
thing a temporary experiment can do and a permanent `verify:` block cannot.

### Also amended: the `.text-title` grep does not prove what the Proof section claims

The Proof section says that `.text-title {` appearing in `dist/assets` shows *"Tailwind saw
`App.tsx`, recognised the class, and generated a rule for it. If source detection missed the
component, no rule would exist."* **That is not true.**

Renaming `text-title` to `text-headline` in `App.tsx` alone leaves `.text-title {` in the bundle,
because `App.test.tsx` contains the literal string `"text-title"` in its `toContain(...)` assertion —
Tailwind scans `.tsx` test files too and treats that string as a class candidate. The rule only
disappears when the name is renamed in **both** files.

So the assertion's subject is kept alive by the assertion's own sibling test. What genuinely caught
the rename was the unit test:

```
AssertionError: expected [ 'text-headline' ] to include 'text-title'
```

The grep is still worth keeping — it fails if the class stops resolving entirely — but it is not
evidence of source detection, and the ticket should not have claimed it was.

## Acceptance criteria

- [ ] `App > renders the application heading` passes, with its assertion unchanged
- [ ] `App > gives the heading a token-derived class` passes
- [ ] `npm run --silent test` reports `Tests  12 passed (12)`
- [ ] `App.tsx` contains no `style={{` and no colour literal
- [ ] The built CSS contains a `.text-title` rule and a `.bg-bg` rule
- [ ] The built CSS contains `var(--pd-fs-title)`
- [ ] `cd web-client && npm run check` exits 0
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
