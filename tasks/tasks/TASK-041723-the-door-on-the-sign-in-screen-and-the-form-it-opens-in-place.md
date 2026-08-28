---
schema: 2
id: TASK-041723
title: The door on the sign-in screen, and the form it opens in place of the sign-in form
type: task
status: backlog
parent: STORY-0417
module: web-client
estimate: S
tier: sonnet
review: standard
files_touched: 2
labels: [client, account, recovery, routing, wiring]
depends_on: [TASK-041719, TASK-041722]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test -- src/lobby/Lobby.test.tsx 2>&1 | grep -qE 'Test Files +1 passed \(1\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- src/lobby/Lobby.test.tsx 2>&1 | grep -qE 'Tests +70 passed \(70\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- src/App.test.tsx 2>&1 | grep -qE 'Tests +37 passed \(37\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- src/account/SignInForm.test.tsx 2>&1 | grep -qE 'Tests +7 passed \(7\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- src/account/ForgotPasswordForm.test.tsx 2>&1 | grep -qE 'Tests +7 passed \(7\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- src/routing/screen.test.ts 2>&1 | grep -qE 'Tests +7 passed \(7\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'offers the way to a forgotten password under the sign-in form, refused or not'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'puts the recovery form where the sign-in form was, and takes the door with it'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'sends what was typed to the account seam, and never a handle or a password'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'opens the form and closes it again without touching the address'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'comes back to the sign-in form after a round trip through the lobby'
  - test "$(grep -oF 'from "../account/ForgotPasswordForm"' web-client/src/lobby/Lobby.tsx | wc -l | tr -d ' ')" = 1
  - test "$(grep -oF '<ForgotPasswordForm' web-client/src/lobby/Lobby.tsx | wc -l | tr -d ' ')" = 1
  - test "$(grep -oF '<SignInForm' web-client/src/lobby/Lobby.tsx | wc -l | tr -d ' ')" = 1
  - test "$(grep -oF '{FORGOT_PASSWORD_LABEL}' web-client/src/lobby/Lobby.tsx | wc -l | tr -d ' ')" = 1
  - test "$(grep -oF 'useEffect' web-client/src/lobby/Lobby.tsx | wc -l | tr -d ' ')" = 3
  - test "$(grep -oiF 'forgot' web-client/src/routing/screen.ts | wc -l | tr -d ' ')" = 0
  - cd web-client && npm run check
---

## Goal

The sign-in screen carries one control reading *Forgot your password?* below the sign-in form; it
opens `ForgotPasswordForm` **in place of** that form, wired to the account seam's `forgotPassword`,
and neither opening nor closing it changes the address.

## Files

| File | Action |
| --- | --- |
| `web-client/src/lobby/Lobby.tsx` | modify |
| `web-client/src/lobby/Lobby.test.tsx` | modify |

Read, and do not edit:

- `web-client/src/account/ForgotPasswordForm.tsx` — its two props, `forgotPassword` and `onCancel`.
- `web-client/src/lobby/Lobby.tsx`'s merged `screen === "sign-in"` branch, and the `WaitingForRival`
  and `CopyLink` local components at the foot of that file — the shape `SignInScreenBody` copies,
  including that `CopyLink` already holds `useState` of its own.
- `web-client/src/lobby/Lobby.test.tsx` lines 95–200 — `accountCallsFixture` (which already carries
  `forgotPassword`), `renderLobbyWithAccount`, and the `beforeEach` that resets
  `window.location.hash`.
- `web-client/src/account/account-text.ts` — `CANCEL`, `HANDLE_LABEL`, `PASSWORD_LABEL`,
  `SIGN_IN_LABEL`, `SIGN_IN_HEADING`, `SIGN_IN_REFUSED` and `ACCOUNT_HEADING`, for querying, and
  `web-client/src/account/recovery-text.ts` for `FORGOT_PASSWORD_LABEL`.
- [`ADR-0087`](../../docs/adr/ADR-0087-forgot-your-password-is-a-door-on-the-sign-in-screen.md) §2
  (one literal, two utterances, never both at once), §3 (no slug, and the fragment is not pushed, not
  replaced and not read by this flow), §4 (where the door is, and that it is conditional on nothing),
  §5 (the four states) and §7 (*which component holds the form* is the planner's — this ticket is
  that answer).

## Scope

- **One local component in `Lobby.tsx`, `SignInScreenBody`**, defined beside `WaitingForRival` at the
  foot of the file and rendered only by the `screen === "sign-in"` branch:

  ```tsx
  function SignInScreenBody(props: {
    readonly signIn: AccountCalls["signIn"];
    readonly forgotPassword: AccountCalls["forgotPassword"];
  }): ReactElement;
  ```

  It holds `const [askingForALink, setAskingForALink] = useState(false)` and nothing else.
  **Closed**: `<SignInForm signIn={props.signIn} />` and, under it, a `type="button"` control reading
  `FORGOT_PASSWORD_LABEL`. **Open**: `<ForgotPasswordForm forgotPassword={props.forgotPassword}
  onCancel={() => setAskingForALink(false)} />` and nothing else. Never both.
- **The mode is the screen's, not the tab's.** Because the component is mounted only inside the
  `sign-in` branch, leaving that screen unmounts it and the mode is gone — so the sign-in screen is
  always entered showing the sign-in form, by construction, with no reset handler and no effect to
  get wrong. `ADR-0087` §7 leaves this choice to the planner and this is it; the fifth test pins it.
- **The branch's existing `<h2>{SIGN_IN_HEADING}</h2>` and its *Back* control stay exactly where they
  are and are not moved into the new component.** The address is `#/sign-in` in both states
  (`ADR-0087` §3), so the screen keeps its own name; `FORGOT_PASSWORD_LABEL` is the *form's* heading
  and `ForgotPasswordForm` already renders it (`TASK-041722`).
- **The call comes off the seam**, `account.forgotPassword`, inside the branch that has already
  established `account !== null`. No new import from `../main`; `useAccount()` is already imported.
- **The door is conditional on nothing** — not on `signedIn`, not on a profile, not on whether a
  sign-in was attempted or refused (`ADR-0087` §4).
- **Nothing touches the address.** No `open`, no `leave`, no `clearToken`, no `pushState`, no
  `replaceState`, and no new `useEffect` anywhere in the file. A `verify:` line pins `useEffect` at
  its merged **3**.

## Out of scope

- **A slug, a `Screen` member or a `hashForScreen` case.** `ADR-0087` §3, and a `verify:` line reads
  `web-client/src/routing/screen.ts` for zero occurrences of `forgot`. `web-client/src/routing/` is
  not edited and `screen.test.ts` is pinned at its merged seven.
- **Making *Back* close the form.** `ADR-0087` §Consequences names this as the cost it is paying:
  opening the form changes no address, so the browser's *Back* leaves the screen instead of closing
  the form. `CANCEL` is the only way back and this ticket does not invent a second.
- **A door anywhere else** — not on the first screen, not on the account screen (`ADR-0087` §4,
  `ADR-0083` §3, `ADR-0060` §2). Nothing is added to `AccountScreen.tsx`, whose suite is untouched.
- **Any change inside `ForgotPasswordForm` or `SignInForm`.** A `verify:` line pins each of their
  suites, and a second sign-in form is refused by `<SignInForm` staying at one.
- **Touching `App.test.tsx`.** `forgotPassword` is **already** on `AccountCalls` (`TASK-041711`) and
  already in that file's `fakeAccountCalls`, so this diff adds no binding to the seam and its
  wholesale `vi.mock("./main", …)` needs nothing. A `verify:` line pins it at 37; if it reddens,
  stop and report rather than editing it — it has already caught four tickets in this epic.

## Tests

`web-client/src/lobby/Lobby.test.tsx`, appended inside the existing `describe("the lobby")`.
**65 merged tests become 70.** Set `window.location.hash = "#/sign-in"` before rendering; the
existing `beforeEach` already resets it.

| Test | Proves |
| --- | --- |
| `offers the way to a forgotten password under the sign-in form, refused or not` | Two states in one test. Fresh at `#/sign-in`: `getByRole("button", { name: FORGOT_PASSWORD_LABEL })` finds the door, and the sign-in form is there too (`getByLabelText(PASSWORD_LABEL)`). Then drive a sign-in whose spy answers `refused`, await `findByText(SIGN_IN_REFUSED)`, and the door is **still** there. `ADR-0087` §4's *conditional on nothing*: a door that appeared only after a failure would make a player who knows their password is gone type a wrong one to be shown the way out |
| `puts the recovery form where the sign-in form was, and takes the door with it` | Click the door. Presence first: `getByRole("heading", { name: FORGOT_PASSWORD_LABEL })`. Then four absences — `queryByRole("button", { name: FORGOT_PASSWORD_LABEL })` is `null` (§2: the control is gone once it is a heading), `queryByLabelText(PASSWORD_LABEL)` is `null`, `queryByLabelText(HANDLE_LABEL)` is `null` and `queryByRole("button", { name: SIGN_IN_LABEL })` is `null`. **Never two forms in view, and no password field in this flow** |
| `sends what was typed to the account seam, and never a handle or a password` | Open the door, type `" Zqx-Address-Zqx. "`, submit the form. The `forgotPassword` spy from `accountCallsFixture` was called **once** with exactly that string, and the `signIn` spy **zero** times. End to end from the address bar to the seam, which is the only thing this ticket can be wrong about. The fixture has a leading space, a trailing dot and capitals, so a normalisation anywhere along the way reddens here |
| `opens the form and closes it again without touching the address` | `window.location.hash` is `"#/sign-in"` before opening, still `"#/sign-in"` with the form open, and still `"#/sign-in"` after `CANCEL`; after `CANCEL` the sign-in form's password field and the door are both back and the recovery heading is gone. `ADR-0087` §3 — *the fragment is not pushed, not replaced and not read by this flow* — asserted three times rather than assumed once |
| `comes back to the sign-in form after a round trip through the lobby` | Open the form, press *Back*, then press *Account* and then the account screen's *Sign in* door. The sign-in form is showing, the door is showing, and `queryByRole("heading", { name: FORGOT_PASSWORD_LABEL })` is `null`. The mode belongs to the screen, not to the tab. **Measured**: each hash navigation must be awaited through `findBy*` — jsdom dispatches `hashchange` on a later task, so a synchronous `act(() => fireEvent.click(…))` on a navigating control leaves the old screen on the page and the next `getBy*` fails |

**Presence before absence, everywhere.** Without the presence half, every one of these passes for a
lobby that never reached the sign-in screen at all.

**`@testing-library/jest-dom` is not a dependency here** — `toBeInTheDocument()` throws
`Invalid Chai property`. Presence goes through a throwing query; absence goes through `queryBy*`
paired with `.toBeNull()`. Never write `expect(queryByText(x)).toBeDefined()`: it is **always true**,
`null` included. Note also that `queryByRole("form")` matches only a form carrying an accessible
name, so a bare `<form>` is not findable that way — query the controls, as the table above does.

**No `try` anywhere in the added code, and no `expect()` inside one.** No test sleeps on a real clock.
The merged 65 are pinned by the **count**, never by their names.

## Acceptance criteria

- [ ] `cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'offers the way to a forgotten password under the sign-in form, refused or not'`
      — passes, asserting the door before any attempt **and** after a refusal
- [ ] `cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'puts the recovery form where the sign-in form was, and takes the door with it'`
      — passes, the heading present and all four absences
- [ ] `cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'sends what was typed to the account seam, and never a handle or a password'`
      — passes, with the seam spy asserted called once with the exact fixture and `signIn` at zero
- [ ] `cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'opens the form and closes it again without touching the address'`
      — passes, with the hash asserted at all three points
- [ ] `cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'comes back to the sign-in form after a round trip through the lobby'`
      — passes, with the recovery heading asserted absent at the end
- [ ] `cd web-client && NO_COLOR=1 npm run --silent test -- src/lobby/Lobby.test.tsx 2>&1 | grep -qE 'Tests +70 passed \(70\)'`
      and `… | grep -qE 'Test Files +1 passed \(1\)'` — **exactly seventy**: the 65 merged plus these
      five. Both lines, because a collection error prints a *passing* `Tests` count with no failure
      line at all
- [ ] `cd web-client && NO_COLOR=1 npm run --silent test -- src/App.test.tsx 2>&1 | grep -qE 'Tests +37 passed \(37\)'`
      — unmoved. **Measured**: with this whole change applied, that file stayed green at 37
- [ ] `cd web-client && NO_COLOR=1 npm run --silent test -- src/account/SignInForm.test.tsx 2>&1 | grep -qE 'Tests +7 passed \(7\)'`
      and `cd web-client && NO_COLOR=1 npm run --silent test -- src/account/ForgotPasswordForm.test.tsx 2>&1 | grep -qE 'Tests +7 passed \(7\)'`
      — both children untouched
- [ ] `cd web-client && NO_COLOR=1 npm run --silent test -- src/routing/screen.test.ts 2>&1 | grep -qE 'Tests +7 passed \(7\)'`
      — the address space is unchanged, and no slug was minted
- [ ] `test "$(grep -oF 'from "../account/ForgotPasswordForm"' web-client/src/lobby/Lobby.tsx | wc -l | tr -d ' ')" = 1`
      and `test "$(grep -oF '<ForgotPasswordForm' web-client/src/lobby/Lobby.tsx | wc -l | tr -d ' ')" = 1`
      — imported once, rendered once. **Two narrow counts, never one over the bare identifier:
      measured on the shape `## Scope` prescribes, bare `ForgotPasswordForm` scores 3** — the named
      import, the module path in the same line, and the JSX element — which is the inversion that
      cost `TASK-041714` a dispatch. Neither count reads a closing tag, so `<X …/>` and `<X …></X>`
      both score one
- [ ] `test "$(grep -oF '<SignInForm' web-client/src/lobby/Lobby.tsx | wc -l | tr -d ' ')" = 1`
      — **still exactly one**, its merged value: the swap replaces the sign-in form at runtime rather
      than rendering a second copy of it. A count of 2 is two forms on one screen, which `ADR-0087`
      §5 forbids
- [ ] `test "$(grep -oF '{FORGOT_PASSWORD_LABEL}' web-client/src/lobby/Lobby.tsx | wc -l | tr -d ' ')" = 1`
      — the door's words, rendered once. **Hand-counted**: the import writes `{ FORGOT_PASSWORD_LABEL }`
      **with spaces** and does not match this needle, and the heading is the form's, not this file's,
      so the one occurrence is the door's JSX expression. `ADR-0087` §2's one literal, in the one
      place this file says it. Bare `FORGOT_PASSWORD_LABEL` scores 2 here — import plus door — which
      is why the braced form is the gate
- [ ] `test "$(grep -oF 'useEffect' web-client/src/lobby/Lobby.tsx | wc -l | tr -d ' ')" = 3`
      — **its merged value, measured on `develop`**: the `react` import plus the two merged effects.
      This flow adds none, because it touches no address
- [ ] `test "$(grep -oiF 'forgot' web-client/src/routing/screen.ts | wc -l | tr -d ' ')" = 0`
      — **measured at zero on `develop`**, and it stays there. `ADR-0087` §3: this ADR mints no slug
      and nothing may write `forgot-password` into `screen.ts` on its authority
- [ ] `cd web-client && npm run check` exits 0. Run it **bare**, reading `$?` directly: piping it into
      `tail` makes `$?` the pipe's status and has already shipped a false green on `TASK-041714`
- [ ] Every merged test in `Lobby.test.tsx` passes unchanged. No assertion moves and none is weakened
- [ ] No file outside the two listed differs
- [ ] Every command in `verify:` exits 0

## Proof

Run each step, record what you measured, and revert it. **These are experiments, not changes**, and
both files are inside this ticket's budget. **A prediction that fails is the finding**: report it as
measured and do not adjust it to match the ticket. Seven Proof steps in this story measured something
other than predicted, and every one was worth knowing.

1. **Render the door only after a refusal** — gate it on the sign-in outcome. Predict: `offers the way
   to a forgotten password under the sign-in form, refused or not` reddens on its **first** half and
   passes its second. Record which half; a test that only checked after a refusal would call this
   green, and that is the arrangement `ADR-0087` §4 refuses.
2. **Render `ForgotPasswordForm` beside `SignInForm` instead of in place of it.** Predict: `puts the
   recovery form where the sign-in form was, and takes the door with it` reddens on the
   `PASSWORD_LABEL` and `SIGN_IN_LABEL` absences and passes its presence half. Two forms in view is
   the arrangement `ADR-0083` §Alternatives refused in so many words.
3. **Keep the door rendered while the form is open.** Predict: the same test reddens on
   `queryByRole("button", { name: FORGOT_PASSWORD_LABEL })` **alone**. That absence is `ADR-0087` §2 —
   the words are never on screen twice at once — and nothing else asserts it.
4. **Pass a fresh arrow that swallows the call**, `forgotPassword={async () => ({ kind: "accepted" })}`.
   Predict: the form still renders and `sends what was typed to the account seam…` reddens on the
   **spy**, not on any presence. That is the mutation a presence-only test cannot see, and it is why
   this test drives the form rather than merely finding it.
5. **`trim()` the address between the form and the seam.** Predict: the same test reddens on the
   argument. The fixture's leading space and trailing dot are what make it visible; with a fixture
   that was already trimmed and lowercase, nothing would move.
6. **Call `open("sign-in")` when the door is pressed.** Predict: `opens the form and closes it again
   without touching the address` stays **green** on the hash, because the address it would push is the
   address it is already on — record that, and then call `leave()` instead and predict it reddens on
   the hash **and** on the form being gone. The first half of this step is the honest measurement:
   the hash assertions catch a *wrong* navigation, not every navigation.
7. **Hoist `askingForALink` into `Lobby` itself**, out of the local component. Predict: the first four
   tests stay green and `comes back to the sign-in form after a round trip through the lobby` reddens,
   because the state now outlives the screen. If it stays green too, the round-trip drive is not
   actually leaving the sign-in screen — check that the *Back* press lands on the first screen before
   believing the result.
8. **Vacuity check.** Return `null` from `SignInScreenBody`. Predict: every presence assertion in the
   five tests reddens and every absence stays green. Confirm each test carries a presence half.

> **A red run names a prefix, not a set.** Vitest stops reporting past its first hard failure.
>
> **Expect `The 'NO_COLOR' env is ignored due to the 'FORCE_COLOR' env being set.` on stderr.** It is
> wrong — measured, 0 escape bytes with it and 74 without — so every `grep -qE` needs it.

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket.

## Notes

**This whole change was applied to `develop` at `31894cee` and measured before the ticket was
written, then reverted.** With `SignInScreenBody` in `Lobby.tsx`, `ForgotPasswordForm` created and the
four constants added, the client suite reddened **one** test — `recovery-text.test.ts`'s key-set
`toEqual`, which is `TASK-041721`'s and lands first. `App.test.tsx` stayed at 37, `Lobby.test.tsx` at
65, `SignInForm.test.tsx` at 7; `tsc`, ESLint and Prettier were all clean. That is why this `Files`
table is two rows and why `App.test.tsx` is not one of them.

**Why a local component rather than a `useState` in `Lobby` — `ADR-0087` §7's answer.** State held in
`Lobby` outlives every screen `Lobby` renders, so a player who opened the form, pressed *Back* and
came round to *Sign in* again would find the recovery form waiting instead of the sign-in form they
asked for. Mounting the two states in a component the `sign-in` branch owns makes leaving the screen
reset the mode with no handler, no effect and nothing to keep in step. `Lobby.tsx` already defines
`WaitingForRival` and `CopyLink` this way, and `CopyLink` already holds `useState`, so this is the
file's own idiom rather than a new one. It also keeps `ADR-0087` §Consequences' named cost exactly as
named and no larger: *Back* still leaves the screen rather than closing the form.

**`forgotPassword`'s occurrence count in `Lobby.tsx` is deliberately not gated.** Measured on the
prescribed shape it is **6** — the JSX attribute and the seam read at the call site, the prop
declaration and its `AccountCalls` lookup, and the attribute and read inside `SignInScreenBody` — and
an ordinary `const { signIn, forgotPassword } = props` destructure moves it to 5 without changing a
thing that matters. `TASK-041715`'s `attachRecoveryEmail = 1` gate was wrong for exactly this reason,
and the shape here has more room to be wrong in. The seam wiring is gated by the **spy** in the third
test, which is the assertion that can actually fail for the right reason.
