---
schema: 2
id: TASK-041217
title: The account screen states which routes sign in to this profile, in both states
type: task
status: done
parent: STORY-0412
module: web-client
estimate: S
tier: sonnet
review: standard
files_touched: 2
labels: [client, account, ui]
depends_on: [TASK-041216]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test -- src/account/AccountScreen.test.tsx 2>&1 | grep -qE 'Tests +6 passed \(6\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'says the device signs in, and says it stopped, from the server fact alone'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'says nothing about a password route to a browser holding no session'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'asserts no route of its own while the profile has not landed'
  - cd web-client && npm run check
---

## Goal

The screen `ADR-0037` made part of the decision exists: a player reads, in words, which routes
currently sign in to their profile, and both states come from what the server sent.

## Files

| File | Action |
| --- | --- |
| `web-client/src/account/AccountScreen.tsx` | create |
| `web-client/src/account/AccountScreen.test.tsx` | create |

Read, and do not edit: `web-client/src/account/account-text.ts`;
`web-client/src/profile/ProfileStrip.tsx` (the four-state shape to follow);
[`ADR-0037`](../../docs/adr/ADR-0037-the-device-is-a-credential-until-revoked.md);
[`ADR-0050`](../../docs/adr/ADR-0050-revoking-the-device-signs-out-everywhere-but-here.md) §3, §4.

## Scope

- One export:

  ```ts
  export function AccountScreen(props: {
    readonly profile: ProfileStripState | null;
    readonly signedIn: boolean;
  }): ReactElement;
  ```

  The props are passed in rather than read from context, so the screen is renderable in a test with
  no provider — `HistoryScreen` and `LadderScreen` are built the same way (`ADR-0060` §4).
- `ACCOUNT_HEADING` as the one `<h2>`, and the device-route sentence from `deviceRouteLine(
  profile.deviceRouteLive)` — the **only** place `deviceRouteLive` is turned into words, and the
  whole of what the screen reads (`ADR-0050` §4).
- `PASSWORD_ROUTE_LIVE` is rendered **exactly when `signedIn` is true.** That is not a guess about
  the server: `POST /api/auth/sign-in` is the only endpoint in `docs/protocol.md` that ever returns a
  `sessionToken` — sign-up issues none and reset-password issues none — so a browser holding a live
  session is a browser whose player has a password, by construction. Carry that derivation as a
  comment; it is the reason no `hasCredential` field was asked for.
- With no profile in hand (`null`, `no-profile` or `unavailable`) the screen states **no route at
  all**. The client asserts neither route: `ADR-0037` entitles a player to the truth, and a sentence
  rendered from an absent read is not one.
- The two route sentences are stated as facts about the account, in the same block, so a player reads
  the whole answer in one place rather than inferring it from what is missing.
- Slots for the controls the following tickets add: this ticket renders the heading and the
  statement and leaves the rest of the screen empty.

## Out of scope

- **The sign-up form, the revoke control and the sign-out control.** `TASK-041218`, `TASK-041220` and
  `TASK-041221`. Each is a component this screen will place; none is built here.
- **The door and the address.** `TASK-041222` puts `#/account` in `screen.ts` and the branch in
  `Lobby.tsx`; nothing imports this component until then, and an unimported component is expected at
  this point in the chain.
- **`hasRecoveryEmail`, and any sentence about recovery.** `ADR-0050` §4 says `deviceRouteLive` is
  the whole of what the screen reads; `STORY-0417` adds the field and the branch. **A refusal, not an
  omission** — a criterion greps for it.
- Any colour, spacing or type beyond the token classes the neighbouring screens already use.
  `EPIC-06` owns the visual language.

## Tests

`web-client/src/account/AccountScreen.test.tsx`, describe block `"the account screen"`. Build the
profile with `aProfile` so no wire body is spelled out by hand.

| Test | Proves |
| --- | --- |
| `says the device signs in, and says it stopped, from the server fact alone` | Two renders in one test: `deviceRouteLive: true` shows `DEVICE_ROUTE_LIVE` and not `DEVICE_ROUTE_REVOKED`, and `deviceRouteLive: false` shows the opposite. **Both states and both absences**, so a screen that printed one sentence always cannot pass |
| `says the password signs in to a browser holding a session` | `signedIn: true` shows `PASSWORD_ROUTE_LIVE` |
| `says nothing about a password route to a browser holding no session` | `signedIn: false` with a profile in hand: `PASSWORD_ROUTE_LIVE` is absent, and the device sentence is still there. Fails against a screen that states both routes unconditionally |
| `asserts no route of its own while the profile has not landed` | With `profile: null`, neither device sentence and no password sentence is on screen, and the heading still is. `ADR-0002`: a client may never assert a fact it was not told |
| `says nothing about routes when the profile read failed` | The same for `{ kind: "unavailable" }` and for `{ kind: "no-profile" }`, asserted in one test for both. These are the states a screen most easily renders as *revoked*, which would be a lie |
| `carries exactly one heading` | Exactly one `<h2>`, and its text is `ACCOUNT_HEADING`. The rule `App.test.tsx` already enforces for the record and the ladder |

Six tests in a new file: `npm run test -- src/account/AccountScreen.test.tsx` reports **6**.

## Acceptance criteria

- [ ] `the account screen > says the device signs in, and says it stopped, from the server fact
      alone` passes, asserting **both** states and **both** absences
- [ ] `the account screen > says the password signs in to a browser holding a session` passes
- [ ] `the account screen > says nothing about a password route to a browser holding no session`
      passes
- [ ] `the account screen > asserts no route of its own while the profile has not landed` passes
- [ ] `the account screen > says nothing about routes when the profile read failed` passes for
      **both** failure states
- [ ] `the account screen > carries exactly one heading` passes
- [ ] `grep -ci 'recovery\|hasRecoveryEmail' web-client/src/account/AccountScreen.tsx` returns `0`
- [ ] Every player-facing string on the screen comes from `account-text.ts`: `grep -cE '>[A-Z][a-z]+ '
      web-client/src/account/AccountScreen.tsx` returns `0`
- [ ] `npm run test -- src/account/AccountScreen.test.tsx` reports `Tests  6 passed (6)`
- [ ] No file outside the two listed differs
- [ ] Every command in `verify:` exits 0

## Proof

1. Render `DEVICE_ROUTE_LIVE` unconditionally instead of calling `deviceRouteLine`.
   **`says the device signs in, and says it stopped, from the server fact alone` reddens on the
   second render**, and only because that test asserts the *absence* of the other sentence as well as
   the presence of the right one. Drop the absence assertions first and watch the mutation pass — a
   `getByText` for the sentence you expect cannot see a screen that prints both. Revert.
2. Render the device sentence with `deviceRouteLine(profile !== null)`.
   **`asserts no route of its own while the profile has not landed` still passes** (nothing renders)
   while `says the device signs in…` reddens on the revoked render. Run it: it is the plausible
   confusion between *is there a profile* and *is the route live*, and only one of the six tests sees
   it.
3. Render `PASSWORD_ROUTE_LIVE` whenever a profile is in hand, ignoring `signedIn`.
   **`says nothing about a password route to a browser holding no session` reddens alone.** This is
   the mutation that ships a lie to every anonymous player, and it is one deleted condition.
4. Return `null` from the component when `profile` is `null`.
   **`asserts no route of its own while the profile has not landed` reddens on the heading**, which
   is why that test asserts the heading is still there. A screen that vanishes while a read is in
   flight is a screen the player thinks is broken.
5. Change the `<h2>` to an `<h3>`.
   **`carries exactly one heading` reddens.** Worth running once: `App.test.tsx`'s equivalent tests
   for the other two screens are how a duplicated heading was caught before.

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket.

## Notes

**Proof step 5 predicts one reddening test and two redden — and keeping the second was right.** Changing
the `<h2>` to an `<h3>` also trips `asserts no route of its own while the profile has not landed`,
whose `getByRole("heading", { level: 2 })` stops matching. The coder declined to relax that query to
make the narrower prediction true. Review confirmed the level-2 constraint is required by `## Scope`
(*"as the one `<h2>`"*) and by the Tests table, so the test is correctly strict and **the ticket's
prose is what is wrong**. Weakening it would have bought a tidy result and a weaker gate.

**The screen decides from `deviceRouteLive` and nothing else, and the argument rests on a documented
contract rather than on inspection.** `says the device signs in, and says it stopped, from the server
fact alone` renders twice from `aProfile({ deviceRouteLive: true })` and then `false`, every other
field at its fixture default — and `profile-fixture.ts` documents that its fields are **mutually
independent**. A screen reading any other field would see it constant across both renders and print
the same sentence twice. Both renders assert **presence of the right sentence and absence of the
wrong one**; a presence-only pair would pass a screen printing both at once.

**Queries are role-based, which matters for a pair that does not exist yet.** `ADR-0083` requires
`SIGN_IN_HEADING` and `SIGN_IN_LABEL` to be two literals holding the **identical** text, so a string
query becomes ambiguous the day `TASK-041226` lands. Writing role queries now means those tests do not
have to be revisited then.

**Absence is asserted synchronously.** `queryByText(...).toBeNull()`, not `findBy`/`waitFor` — those
retry until a condition holds and cannot express *this did not happen*, and a retry-based absence
check also cannot distinguish a self-correcting flicker from correctness. That distinction cost
`TASK-041203` a review round and produced its most careful finding.

**Two unlisted files were read and neither edited**: `profile/profile-strip.ts` for the type the named
`ProfileStrip.tsx` imports, and `profile/profile-fixture.ts` for the `aProfile` the Tests section
requires by name. `ADR-0070` §4 permits exactly that — reading is allowed, editing is not — and the
diff touches neither.
