---
schema: 2
id: TASK-030709
title: The bar states what the server refused, and retries nothing
type: task
status: backlog
parent: STORY-0307
module: web-client
estimate: S
tier: haiku
review: standard
files_touched: 2
labels: [client, duel, ui]
depends_on: [TASK-030708]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test 2>&1 | grep -qE 'Tests +228 passed \(228\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF "states a rejection in the server's own numbers"
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'says a paused duel did not apply the action'
  - cd web-client && npm run check
---

## Goal

The bar carries one reserved line for the server's last word about this seat's action: a
`Rejection` in the server's own numbers, or a `Failure` said plainly. Showing it sends nothing and
moves nothing.

## Files

`files_touched` counts the create/modify rows only.

| File | Action |
| --- | --- |
| `web-client/src/table/ActionBar.tsx` | modify — two props, one line, three helpers |
| `web-client/src/table/ActionBar.test.tsx` | modify — **the helper gains two props**, and three tests are added |
| `web-client/src/table/rejection-text.ts` | read — `rejectionText` |

## Scope

- The imports gain `ProtocolError` and `Rejection` from `../protocol`, and
  `import { rejectionText } from "./rejection-text";`
- `ActionBar`'s props gain the two the line reads, and the line goes last inside the `<section>`:

  ```tsx
  export function ActionBar(props: {
    turn: PendingTurn | null;
    rejection: Rejection | null;
    refusal: ProtocolError | null;
    send: (message: ClientMessage) => void;
  }): ReactElement {
  ```

  ```tsx
      <Notice rejection={props.rejection} refusal={props.refusal} />
    </section>
  ```

- The three new functions, at the end of the file:

  ```tsx
  /**
   * The line the server's last word about this seat's action goes on. It is
   * reserved whether or not there is anything to say, so saying something moves
   * nothing.
   */
  function Notice(props: {
    rejection: Rejection | null;
    refusal: ProtocolError | null;
  }): ReactElement {
    return (
      <p className="min-h-[calc(var(--pd-fs-small)*var(--pd-lh-body))] text-center text-small text-text-muted">
        {noticeText(props.rejection, props.refusal)}
      </p>
    );
  }

  /** A rejection is the server's answer to the action itself, so it wins. */
  function noticeText(
    rejection: Rejection | null,
    refusal: ProtocolError | null,
  ): string {
    if (rejection !== null) return rejectionText(rejection);
    if (refusal !== null) return refusalText(refusal);
    return "";
  }

  /**
   * A refused frame, said plainly and once. `DUEL_PAUSED` means the action was
   * not applied, so the client says so and sends nothing again — retrying is how
   * a client turns one refusal into two.
   */
  function refusalText(error: ProtocolError): string {
    return error === "DUEL_PAUSED"
      ? "The duel is paused. That action was not applied."
      : "The server did not apply that action.";
  }
  ```

- The reserved height is the `BetLine` idiom `TASK-030614` already uses, so an empty line and a full
  one are the same height and the bar never jumps.
- `DUEL_PAUSED` gets its own sentence because the wire means something specific by it: your action
  was not applied, do not re-send. Every other in-duel `Failure` gets the general one — the client
  has nothing true to add, and `DEC-018`'s answer (`ADR-0028`) is not on this protocol version.

## This ticket owns the two edits its change forces

1. **The test helper** gains the two required props, and nothing else about it moves:

   ```tsx
   <ActionBar
     turn={props.turn === undefined ? aTurn() : props.turn}
     rejection={props.rejection ?? null}
     refusal={props.refusal ?? null}
     send={props.send ?? send}
   />
   ```

2. **`comes back to life on the next turn, at the new minimum`** (`TASK-030707`) builds its two
   `<ActionBar>` elements directly rather than through the helper, so each gains
   `rejection={null} refusal={null}`. **No assertion in that test changes** — it keeps its name, its
   click, its rerender, its `'2400'` expectation and its `toHaveBeenCalledTimes(2)`. This is a
   compile break, not a behaviour change: without the two props `tsc` fails.

No other test in the file changes, and the suite's count moves only by the three tests added below.

## Out of scope

- Clearing the line. The store sets `rejection` and, until `TASK-030712` lands, never unsets it, so
  a rejection from hand 3 is still in state at hand 40.
  [`ADR-0043`](../../docs/adr/ADR-0043-a-rejection-closes-no-decision-point.md) answered that (the
  next `YourTurn`, `Snapshot` or `DuelFinished` clears it) and `TASK-030712` lands it. This ticket
  renders the prop it is handed.
- Retrying anything. There is no `useEffect` in this file and none is added.
- The lobby's `refusalMessage`. That one answers `UNKNOWN_ROOM` and `ROOM_FULL` before a duel
  exists; these two sentences answer a refused action inside one. Neither should learn the other's
  cases.

## Tests

`web-client/src/table/ActionBar.test.tsx`, describe block `"the action bar"`. Three tests are added.

| Test | Proves |
| --- | --- |
| `states a rejection in the server's own numbers` | with `rejection: { type: "AmountTooSmall", attempted: 900, minimum: 1200 }`, the text `900 is under the minimum of 1,200.` is on screen |
| `says a paused duel did not apply the action` | with `refusal: "DUEL_PAUSED"`, the text `The duel is paused. That action was not applied.` is on screen |
| `has nothing to say when nothing was refused` | with neither, no text matches `/refused\|minimum\|paused/`, and `send` was never called — the line is reserved and silent |

Three tests. Two hundred and twenty-five exist, so the suite reports **228**.

## Proof

| Command | Proves |
| --- | --- |
| `Tests 228 passed (228)` | the three ran and the two hundred and twenty-five before them still do |
| the two `--reporter=verbose` greps | each exists by name |
| `npm run check` | typechecks — the two new props are required, so every call site was updated |

**Name the edit that makes each assertion red:**

1. Return `""` from `noticeText` when `refusal` is set → `says a paused duel did not apply the
   action` fails with `Unable to find an element with the text`. Revert.
2. Prefer `refusal` over `rejection` in `noticeText`, and set both in the first test → `states a
   rejection in the server's own numbers` fails. Revert.

## Acceptance criteria

- [ ] `the action bar > states a rejection in the server's own numbers` passes
- [ ] `the action bar > says a paused duel did not apply the action` passes
- [ ] `the action bar > has nothing to say when nothing was refused` passes
- [ ] All fifteen earlier `the action bar` tests still pass; only the two edits named above were
      made to the file, and no assertion in them changed
- [ ] `ActionBar.tsx` still contains no `useEffect` and no `useRef`
- [ ] `npm run --silent test` reports `Tests  228 passed (228)`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
