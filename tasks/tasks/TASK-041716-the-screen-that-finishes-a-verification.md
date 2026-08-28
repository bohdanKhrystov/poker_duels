---
schema: 2
id: TASK-041716
title: The screen that finishes a verification, from a token it is handed once
type: task
status: backlog
parent: STORY-0417
module: web-client
estimate: S
tier: sonnet
review: standard
files_touched: 2
labels: [client, account, recovery, ui, security]
depends_on: [TASK-041711]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test -- src/account/VerifyScreen.test.tsx 2>&1 | grep -qE 'Test Files +1 passed \(1\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- src/account/VerifyScreen.test.tsx 2>&1 | grep -qE 'Tests +7 passed \(7\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'sends the token it was handed, once, at mount'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'renders one sentence per answer, and each is its own'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'sends nothing at all with no token, and says so without calling it an error'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'puts the token on no part of the screen'
  - test "$(grep -oF 'tokenFromHash' web-client/src/account/VerifyScreen.tsx | wc -l | tr -d ' ')" = 0
  - test "$(grep -oiE 'window\.|location|history' web-client/src/account/VerifyScreen.tsx | wc -l | tr -d ' ')" = 0
  - grep -qF 'from "./recovery-text"' web-client/src/account/VerifyScreen.tsx
  - cd web-client && npm run check
---

## Goal

`VerifyScreen` submits the token it is handed exactly once, on mount, and renders one sentence for
each of the four answers — putting the token on no part of the screen and reaching for no address bar
of its own.

## Files

| File | Action |
| --- | --- |
| `web-client/src/account/VerifyScreen.tsx` | create |
| `web-client/src/account/VerifyScreen.test.tsx` | create |

Read, and do not edit:

- `web-client/src/account/verify-email.ts` — `VerifyEmailOutcome`'s four kinds.
- `web-client/src/account/recovery-text.ts` — `VERIFY_HEADING`, `VERIFY_DONE`, `VERIFY_LINK_DEAD`,
  `VERIFY_ADDRESS_TAKEN`, `VERIFY_NO_LINK`.
- [`ADR-0081`](../../docs/adr/ADR-0081-a-mailed-link-is-a-fragment-route-and-the-token-is-the-segment-behind-the-slug.md)
  §5 — *"the token is read once, at mount, into component state"*, and §6 — a missing token is an
  empty input, not an unknown address; a stale link is a screen that renders and a refusal on
  submission.
- `web-client/src/history/HistoryScreen.tsx` — the merged prop-driven screen that runs one read in a
  mount effect. This is the shape to copy, including how it avoids running the effect twice under
  `React.StrictMode`.
- `web-client/src/store/boot-strict-mode.test.tsx` — what a double mount looks like in this
  repository, and why *once* has to be asserted rather than assumed.

## Scope

- **Props: a token and a call, and nothing else.**

  ```tsx
  export function VerifyScreen(props: {
    readonly token: string | null;
    readonly verify: (token: string) => Promise<VerifyEmailOutcome>;
  }): ReactElement;
  ```

  The screen knows nothing about navigation, the address bar or storage — `ADR-0060` §4, and a
  `verify:` line pins `window.`, `location` and `history` at zero occurrences.
- **`VERIFY_HEADING` always.** The screen renders for a live token, a dead token and no token at all,
  and looks the same until an answer arrives (`ADR-0081` §7).
- **With a token: one call, in a mount effect, once.** `React.StrictMode` mounts twice in
  development, and a second call would spend a token that is single-use by construction. Guard it the
  way `HistoryScreen` guards its read, not with a module-level flag.
- **With `token === null`: no call at all**, and `VERIFY_NO_LINK` on screen. `ADR-0081` §6 makes this
  the address a reload lands on, so it must not read as a failure and must not send a request whose
  answer is already known.
- **One sentence per outcome**: `verified` → `VERIFY_DONE`; `link-dead` → `VERIFY_LINK_DEAD`;
  `address-taken` → `VERIFY_ADDRESS_TAKEN`; `failed` → `VERIFY_LINK_DEAD`.
  **`failed` and `link-dead` share a sentence deliberately**: the player's next move is the same in
  both — ask for a new link — and the client cannot tell a dead token from a request that did not
  arrive without claiming something it does not know.
- **While the call is in flight the screen shows the heading and no outcome sentence.** No spinner
  text is added to `recovery-text.ts`; absence is the whole of it.
- **The token appears in no rendered output**, not in a heading, not in a hidden input, not in a
  `title` or `aria-label`.

## Out of scope

- **Reading the token from the address.** `tokenFromHash` is `TASK-041701`'s and `Lobby.tsx` calls
  it; a `verify:` line pins that name at zero occurrences here.
- **Clearing the fragment.** `TASK-041702`'s `clearToken`, called by `TASK-041717` from the lobby.
- **A retry control.** A single-use token retried is a token spent twice, and the answer would not
  change.
- **Any navigation, including back to the account screen.** `TASK-041717` renders the way back beside
  this screen, the way the record, the ladder and the account screens already do.
- **Any string literal a player reads.** All five come from `recovery-text.ts`.

## Tests

`web-client/src/account/VerifyScreen.test.tsx`, new. **Seven tests.** Query every sentence through
its **constant**.

```ts
const TOKEN = "zqx-verify-token-zqx";
```

| Test | Proves |
| --- | --- |
| `sends the token it was handed, once, at mount` | Render with `token={TOKEN}` and a spy answering `verified`. The spy was called **once**, with exactly `TOKEN`. Then render the same tree inside `<React.StrictMode>` and assert the spy was still called **once** for that mount. The second half is the whole reason this test exists: `main.tsx` renders under `StrictMode` |
| `renders one sentence per answer, and each is its own` | Four renders — `verified`, `link-dead`, `address-taken`, `failed`. Each shows its own sentence, and each asserts the other two distinct sentences are **absent**. `failed` and `link-dead` are asserted to show the **same** sentence, in the same test, so the collapse is visible rather than accidental |
| `sends nothing at all with no token, and says so without calling it an error` | `token={null}`: the spy was called **zero** times, `VERIFY_NO_LINK` is on screen, and `VERIFY_LINK_DEAD` is not. The count is the assertion — a screen that called with `""` and got a `400` would show the wrong sentence and pass a presence-only check |
| `shows the heading before any answer, and shows no outcome while it waits` | With a promise that never settles: `VERIFY_HEADING` is on screen and none of the four outcome sentences is. Presence before absence |
| `puts the token on no part of the screen` | With `token={TOKEN}` and a `verified` answer: the container's whole `textContent` does not contain `TOKEN`, and neither does its `innerHTML` — the second catches an attribute the first cannot see, which is the hole `TASK-041232` was filed to close on the sign-up forms. The presence half runs first: `VERIFY_DONE` is on screen |
| `asks again for nothing when the answer changes` | One render, answer `link-dead`; then rerender the same component with the same props. The spy is still at **one** call. A screen that re-ran its effect on every render would burn a link on a parent's re-render |
| `never renders two sentences at once` | Over all four outcomes in a loop: exactly one of the five sentences is present in each rendered screen, counted rather than checked one at a time. The guard against a screen that appends outcomes instead of replacing them |

**No `try` anywhere in the added code, and no `expect()` inside one.** Every asynchronous outcome is
awaited through `findByText`; no test sleeps on a real clock.

## Acceptance criteria

- [ ] `cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'sends the token it was handed, once, at mount'`
      — passes, including the `StrictMode` half asserting **one** call
- [ ] `cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'renders one sentence per answer, and each is its own'`
      — passes, four renders, with absences and with the deliberate `failed`/`link-dead` equality
- [ ] `cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'sends nothing at all with no token, and says so without calling it an error'`
      — passes, asserting the call count is **zero**
- [ ] `cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'puts the token on no part of the screen'`
      — passes, over `textContent` **and** `innerHTML`, with the presence half first
- [ ] `cd web-client && NO_COLOR=1 npm run --silent test -- src/account/VerifyScreen.test.tsx 2>&1 | grep -qE 'Tests +7 passed \(7\)'`
      and `… | grep -qE 'Test Files +1 passed \(1\)'` — **exactly seven**. Both lines, because a
      collection error prints a *passing* `Tests` count with no failure line at all
- [ ] `test "$(grep -oF 'tokenFromHash' web-client/src/account/VerifyScreen.tsx | wc -l | tr -d ' ')" = 0`
      and `test "$(grep -oiE 'window\.|location|history' web-client/src/account/VerifyScreen.tsx | wc -l | tr -d ' ')" = 0`
      — the screen reads no address and touches no history. Both read the whole file, comments
      included, so write the KDoc without those words
- [ ] `grep -qF 'from "./recovery-text"' web-client/src/account/VerifyScreen.tsx`
      — every sentence comes from the copy module
- [ ] `cd web-client && npm run check` exits 0. The whole-suite total is deliberately not pinned:
      this ticket and `TASK-041712`, `TASK-041713`, `TASK-041718` have pairwise disjoint `Files`
      tables and may be dispatched in one batch
- [ ] No file outside the two listed differs
- [ ] Every command in `verify:` exits 0

## Proof

Run each step, record what you measured, and revert it. **These are experiments, not changes**, and
both files are inside this ticket's budget.

1. **Drop the once-only guard** on the mount effect. Predict: `sends the token it was handed, once,
   at mount` reddens on its `StrictMode` half and `asks again for nothing when the answer changes`
   reddens too. Record both counts. If **neither** moves, the harness is not mounting twice and the
   test is not testing what it claims — say so.
2. **Call with `""` when the token is null.** Predict: `sends nothing at all with no token…` reddens
   on the **call count**, and possibly not on the sentence, since the double's answer decides that.
   Record which halves moved; that asymmetry is why the count is asserted.
3. **Collapse `address-taken` into `link-dead`.** Predict: `renders one sentence per answer…` reddens
   on both the presence and the absence for that arm.
4. **Render the token** — add `<p>{props.token}</p>`. Predict: `puts the token on no part of the
   screen` reddens on `textContent`. Then instead put it in `title={props.token}`: predict
   `textContent` stays clean and **`innerHTML` reddens**. Run both; the second is the mutation a
   text-only sweep cannot see.
5. **Append rather than replace** — keep every outcome sentence once rendered. Predict: `never renders
   two sentences at once` reddens. Since each render here produces one answer, drive it by rerendering
   with a second outcome; if that cannot be produced, say so and record that the test is weaker than
   its name.
6. **Vacuity check**: return `null` from the whole component. Predict: every presence assertion
   reddens and every absence stays green. Confirm each of the seven tests carries a presence half.

> **A red run names a prefix, not a set.** Vitest stops reporting past its first hard failure.
>
> **Expect `The 'NO_COLOR' env is ignored due to the 'FORCE_COLOR' env being set.` on stderr.** It is
> wrong — measured, 0 escape bytes with it and 74 without — so every `grep -qE` needs it.

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket.

## Notes

**Why `token: string | null` and not `token: string`.** `ADR-0081` §6: `#/verify` with no second
segment is a **known address with an empty input** — it is what the address becomes after the
fragment is replaced, and what a reload lands on. Making the prop non-nullable would push that case
into `Lobby.tsx` as a branch, and the screen would then have two callers with two ideas of what a
missing token means.

**`failed` and `link-dead` share a sentence, and that is a decision this ticket makes in the open.**
The alternative is a fifth sentence saying *something went wrong*, which tells the player nothing
they can act on and invites them to retry a token that is single-use. The test asserts the equality
so a later reader sees it was chosen rather than forgotten.
