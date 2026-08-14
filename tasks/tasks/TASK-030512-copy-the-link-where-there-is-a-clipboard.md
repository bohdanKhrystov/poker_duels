---
schema: 2
id: TASK-030512
title: Copy the link where there is a clipboard, and keep it in reach where there is not
type: task
status: backlog
parent: STORY-0305
module: web-client
estimate: S
tier: haiku
review: standard
files_touched: 2
labels: [client, lobby]
depends_on: [TASK-030511]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test 2>&1 | grep -qE 'Tests +123 passed \(123\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'copies the invite link when the browser has a clipboard'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'offers no copy button when the browser has no clipboard'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'keeps the link in reach when the clipboard refuses'
  - cd web-client && npm run check
---

## Goal

One click copies the invite where the browser has a clipboard; where it has none, or the user
refuses it, the link is still on screen and still selectable.

## Files

`files_touched` counts the create/modify rows only.

| File | Action |
| --- | --- |
| `web-client/src/lobby/Lobby.tsx` | modify |
| `web-client/src/lobby/Lobby.test.tsx` | modify |

## Scope

- `WaitingForRival` renders `<CopyLink link={link} />` as the last child of its `<section>`.
- A third component in the same file:

  ```tsx
  /** Absent where the clipboard API is: the box above is always the fallback. */
  function CopyLink(props: { link: string }): ReactElement | null {
    const [outcome, setOutcome] = useState<"none" | "copied" | "refused">("none");
    if (!navigator.clipboard) {
      return null;
    }
    return (
      <>
        <button
          type="button"
          onClick={() => {
            void navigator.clipboard.writeText(props.link).then(
              () => setOutcome("copied"),
              () => setOutcome("refused"),
            );
          }}
        >
          Copy the link
        </button>
        {outcome === "copied" && <p>Link copied.</p>}
        {outcome === "refused" && <p>Copy it from the box above.</p>}
      </>
    );
  }
  ```

- **`then` takes both callbacks.** A one-argument `then` leaves the rejection unhandled, and Vitest
  fails the whole run with *"Vitest caught 1 unhandled error during the test run"* — a failure in
  a file nobody edited. The refusal is a state, not an exception.
- `useState` is called before the `navigator.clipboard` check, so the hook order never depends on
  a browser capability.
- The button's accessible name stays `"Copy the link"` in every outcome; the result is a separate
  line. A button that renames itself is a button tests and screen readers lose track of.
- No `useEffect`, and nothing reads the clipboard — only `writeText`.

## Out of scope

- A timeout that clears `"Link copied."` after a few seconds. That needs a timer and a cleanup,
  and this story has no timer anywhere.
- `document.execCommand("copy")` as a second fallback. The selectable field is the fallback.
- Copying the bare code rather than the link. The link is the product's one success condition.

## Tests

`web-client/src/lobby/Lobby.test.tsx`, describe block `"the lobby"`. Three `it` blocks appended
after `TASK-030511`'s; nothing already there is edited.

Two pieces are added above the existing helpers:

```tsx
function withClipboard(writeText: () => Promise<void>): void {
  Object.defineProperty(navigator, "clipboard", {
    value: { writeText },
    configurable: true,
  });
}

afterEach(() => {
  Reflect.deleteProperty(navigator, "clipboard");
});
```

`afterEach` joins the `vitest` import. jsdom implements no Clipboard API, so *not* calling
`withClipboard` is how a test says "this browser has none" — do not stub the whole `navigator`.

| Test | Proves |
| --- | --- |
| `copies the invite link when the browser has a clipboard` | with a `vi.fn(() => Promise.resolve())` clipboard, clicking `Copy the link` calls `writeText` with `"http://localhost:3000/?room=ABCDEFGH"`, and `await screen.findByText("Link copied.")` resolves |
| `offers no copy button when the browser has no clipboard` | with no clipboard defined, `queryByRole("button", { name: "Copy the link" })` is `null` while the invite field still holds the full link |
| `keeps the link in reach when the clipboard refuses` | with `() => Promise.reject(new Error("denied"))`, clicking leaves `await screen.findByText("Copy it from the box above.")` resolving and the invite field still holding the full link |

The two async tests are `async` and use `findByText`, which is what wraps the promise turn in
`act` — no manual `act` and no `await Promise.resolve()` is needed.

Three tests. One hundred and twenty exist, so the suite reports **123**.

## Proof

| Command | Proves |
| --- | --- |
| `Tests 123 passed (123)` | the three ran and the hundred-and-twenty before them still do |
| the three `--reporter=verbose` greps | each exists by name |
| `npm run check` | typechecks, lints, formats |

**Name the edit that makes each assertion red** — all three were run against this exact test file:

1. Delete the `if (!navigator.clipboard) return null;` guard → `offers no copy button when the
   browser has no clipboard` fails with `expected <button type="button"></button> to be null`.
   Revert.
2. Drop the second `then` callback, leaving `.then(() => setOutcome("copied"))` → `keeps the link
   in reach when the clipboard refuses` fails with `Unable to find an element with the text: Copy
   it from the box above.`, **and** the run reports `Unhandled Rejection … Error: denied`. Revert.
3. Pass anything but the link to `writeText` — `writeText("ABCDEFGH")` is what was run → `copies
   the invite link when the browser has a clipboard` fails with `expected "spy" to be called with
   arguments: [ Array(1) ]`. Revert.

Quote all three in the PR. The second is why `then` takes two callbacks and not one.

## Acceptance criteria

- [ ] `the lobby > copies the invite link when the browser has a clipboard` passes
- [ ] `the lobby > offers no copy button when the browser has no clipboard` passes
- [ ] `the lobby > keeps the link in reach when the clipboard refuses` passes
- [ ] `npm run --silent test` reports `Tests  123 passed (123)`
- [ ] The run reports no unhandled rejection
- [ ] The six `it` blocks from `TASK-030510` and `TASK-030511` are unedited, and their assertions
      are byte-identical
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
