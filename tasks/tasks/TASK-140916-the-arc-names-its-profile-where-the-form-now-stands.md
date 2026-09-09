---
schema: 2
id: TASK-140916
title: The arc names its profile where the form now stands
type: task
status: done
parent: STORY-1409
module: web-client
estimate: S
tier: sonnet
review: standard
files_touched: 1
labels: [client, account, e2e]
depends_on: [TASK-140915]
verify:
  - cd web-client && npm ci
  - sh -c 'cd web-client && NO_COLOR=1 npx vitest run src/e2e/claimed-here-recovered-there.test.tsx 2>&1 | grep -qF "claimed-here-recovered-there.test.tsx  (8 tests)"'
  - sh -c 'test $(grep -c "your display name" web-client/src/e2e/claimed-here-recovered-there.test.tsx) -eq 10'
  - sh -c 'test $(grep -c "ACCOUNT_HEADING" web-client/src/e2e/claimed-here-recovered-there.test.tsx) -ge 14'
  - cd web-client && npm run check
  - cd web-client && npm run build
  - python3 .github/scripts/lint_tickets.py
---

## Goal

The whole-client arc reaches the name form through the product's own `Account` door, so it proves
the same eight things about a profile's name and balance after `TASK-140917` takes the form off the
front door.

## Files

| File | Action |
| --- | --- |
| `web-client/src/e2e/claimed-here-recovered-there.test.tsx` | modify |

One. This file is the arc's whole surface; nothing else in the repository reaches
`your display name` from a booted client.

Read, and do not edit:
`web-client/src/lobby/Lobby.tsx` — the `Account` button and the `Back` button that returns from it;
`web-client/src/account/account-text.ts` — `ACCOUNT_HEADING`, which the file already imports.

## Scope

- **Ten call sites move behind the door**, counted on `develop` at `1c3c7fd9`. Every
  `await within(container).findByLabelText("your display name")` in this file is reached today from
  the front door. Each becomes: press the button named `ACCOUNT_HEADING`, then find the region, then
  press `Back` when the test next needs the front door — the profile strip, the room controls or a
  duel.
- **The pattern is verified, not proposed.** It was run on this tree against
  `names the profile and then claims it, and the claim moves neither`, with the front-door render
  already deleted, and that case passed:

  ```tsx
  act(() => {
    fireEvent.click(
      within(container).getByRole("button", { name: ACCOUNT_HEADING }),
    );
  });
  const nameRegion = await within(container).findByLabelText("your display name");
  // … type, submit, read back …
  act(() => {
    fireEvent.click(within(container).getByRole("button", { name: "Back" }));
  });
  const profileRegion = await within(container).findByLabelText("your profile");
  ```

- **Reads of the name must happen while the account screen is mounted.** The screens swap rather
  than stack, so a `nameRegion` captured before pressing `Back` is detached afterwards. Where a test
  reads the name **after** doing something else, move the read up beside the write or press
  `Account` again — `names the profile and then claims it` needs both, and the verified patch above
  does exactly that.
- **`getByLabelText("your profile")` becomes `findByLabelText`** at the sites that follow a `Back`
  press: the front door remounts, and the synchronous query can win the race.
- **Assertions are not weakened.** Every `expect` in the file stays, comparing the same values; only
  the navigation around them changes.

## Out of scope

- **Deleting the front-door render.** `TASK-140917` — this ticket lands while the form stands on
  both screens, which is what makes it green on its own.
- **Adding a case that renames.** The arc names once per client today; proving a rename end to end
  is worth a later ticket and is **not ticketed**.
- **`web-client/src/e2e/account-server.ts`.** It answers no `429` and this ticket adds none.
- **`Lobby.test.tsx` and `drive-arc.test.tsx`.** Neither reaches the name surface.

## Tests

`claimed-here-recovered-there.test.tsx` — 8 tests on `develop` at `1c3c7fd9`, 8 after. No test is
added, removed or renamed.

| Test | Proves |
| --- | --- |
| `names the profile and then claims it, and the claim moves neither` | *(navigation only)* the balance and the name after a sign-up equal the ones before it |
| `signs in from the second client and reads back the same balance name and duel` | *(navigation only)* |
| `signing out on the second client returns it to the profile it had` | *(navigation only)* |
| `the first client is unaffected by the second signing out` | *(navigation only)* |
| the other four | *(unchanged)* |

## What would still pass if the coder got it wrong

- **This ticket is green before and after `TASK-140917`, which is the point and also the risk**: with
  the form still on the front door, a patch that pressed `Account` and then queried the *whole
  container* would find the front door's copy and pass, and then fail the moment `TASK-140917`
  lands. Every query must therefore be reached **after** the press and, where the test already scopes
  with `within(...)`, stay scoped.
- **The count gate is an equality at 10**, so a repair that deleted a hard call site rather than
  moving it fails — deleting one would read 9. The `ACCOUNT_HEADING` count is a **floor** of 14
  rather than an equality: the file names that constant 13 times today, some tests already stand on
  the account screen when they read the name, and a test may legitimately need two presses. A floor
  strictly above today's count is the most a mechanical gate can say here.
- **If an assertion were dropped to make a query resolve**, no gate notices. That is what
  `review: standard` is for here, and the acceptance criterion below states it: the diff is
  navigation, and every `expect` compares the same two values it compared before.
- **A `nameRegion` read after `Back`** throws rather than passing quietly, so that failure mode is
  loud.

## Acceptance criteria

- [ ] `claimed-here-recovered-there.test.tsx` reports exactly 8 tests, all passing
- [ ] `your display name` appears exactly 10 times in the file — none deleted, none added
- [ ] Every one of those 10 is reached after a press of the `Account` button, and none is queried
      against the front door
- [ ] `ACCOUNT_HEADING` appears at least 14 times — strictly more than the 13 measured on `develop`
      at `1c3c7fd9`
- [ ] No `expect` in the file is deleted or weakened; the diff is navigation and query timing only
- [ ] `npm run check` and `npm run build` exit 0 in `web-client`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
