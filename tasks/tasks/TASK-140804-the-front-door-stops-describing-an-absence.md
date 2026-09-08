---
schema: 2
id: TASK-140804
title: The front door stops describing an absence
type: task
status: done
parent: STORY-1408
module: web-client
estimate: S
tier: haiku
review: standard
files_touched: 4
atomic:
  - web-client `npm run test` — `ProfileStrip.test.tsx`'s `says there is no profile yet, and raises no alarm` asserts the string is on screen
  - web-client `npm run test` — `drive-arc.test.tsx`'s `a first boot mints a device id and asks the server nothing` awaits `findByText("No profile yet.")` and times out without it
  - "`ADR-0125` §Consequences — the manual plan owes its edit **in the diff that lands §5 and not before**, because both of its claims are true until this branch renders nothing"
labels: [client, profile, docs]
depends_on: [TASK-140803]
verify:
  - cd web-client && npm ci
  - sh -c '! grep -rqF "No profile yet." web-client/src'
  - sh -c '! grep -qF "No profile yet." docs/test-plan.md'
  - grep -qF '04-01' docs/test-plan.md
  - grep -qF 'Duel coins' docs/test-plan.md
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'renders nothing at all when there is no profile'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'renders nothing at all when the read did not land'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'a first boot mints a device id and asks the server nothing'
  - cd web-client && npm run check
  - cd web-client && npm run build
  - python3 .github/scripts/lint_tickets.py
---

## Goal

`ProfileStrip`'s `no-profile` branch renders `null`, exactly as its `unavailable` sibling already
does, and the string `No profile yet.` leaves the product — `ADR-0125` §5. The manual test plan stops
reasoning from it in the same diff.

## Files

Four. Three were **probed** (`ADR-0069`, `ADR-0070`): the branch was changed to `return null` in one
tree and the client gate set from `.github/workflows/build.yml` was run until it exited `0`; it named
two test files. The fourth is an obligation `ADR-0125` §Consequences puts on **this** diff by name.

| File | Action | Why it cannot be fewer |
| --- | --- | --- |
| `web-client/src/profile/ProfileStrip.tsx` | modify | The branch itself. Nothing else in this ticket makes any other row fail |
| `web-client/src/profile/ProfileStrip.test.tsx` | modify | `says there is no profile yet, and raises no alarm` does `getByText("No profile yet.")` and fails the moment the branch renders nothing |
| `web-client/src/e2e/drive-arc.test.tsx` | modify | `a first boot mints a device id and asks the server nothing` awaits `within(container).findByText("No profile yet.")`, which now never appears; the case times out |
| `docs/test-plan.md` | modify | `ADR-0125` §Consequences: *"The manual test plan owes an edit, in the diff that lands §5 and not before"* — both claims are **true until this branch changes**, so a separate earlier ticket would make the plan wrong, and a later one would leave it wrong |

Read, and do not edit:
[`ADR-0125`](../../docs/adr/ADR-0125-the-account-screen-names-the-anonymous-profile-and-owns-the-door.md)
§5 and the last bullet of §Consequences' *What it costs*;
`web-client/src/profile/profile-strip.ts` — the `ProfileStripState` union, unchanged here.

## Scope

- Replace `ProfileStrip`'s whole `no-profile` `<section>` with `return null`, and write the reason
  beside it in the words the `unavailable` branch already uses: the player cannot act on it. **The
  `profile` branch is untouched** — the name, the coin count and the recent-duels list stay exactly
  where they are (`ADR-0125` §5: *"This ADR changes nothing else about the strip"*).
- Rewrite `ProfileStrip.test.tsx`'s `says there is no profile yet, and raises no alarm` into
  `renders nothing at all when there is no profile`, asserting `container.innerHTML` is `""` — the
  same assertion its `unavailable` sibling already makes. **Keep** `renders nothing at all when the
  read did not land`: two cases asserting the same thing about two different inputs is what keeps the
  pair from passing on a `ProfileStrip` that returns `null` for everything, and the `profile` cases
  above them are the third input that stops that.
- In `drive-arc.test.tsx`, replace the `findByText("No profile yet.")` await. The case's subject is
  unchanged — a first boot mints a device id and sends the server nothing — so it still has to wait
  for the mount's profile read to settle before asserting. Wait on the minted id itself
  (`await waitFor(() => expect(readDeviceId(storage)).toBe(ALICE.deviceId))`; `waitFor` is not
  imported in that file yet) and keep `expect(server.requests).toEqual([])` after it. Do not turn the
  case synchronous — an assertion that runs before the read resolves proves nothing about the read.
- In `docs/test-plan.md`, edit exactly two places and nothing else:
  1. the paragraph beginning **"Read the strip after a reload, never on the first load of a fresh
     profile."** — its sentence *"A fresh profile's first render is therefore `No profile yet.`"* is
     now false. The paragraph's **rule survives**: the read still runs at mount before the socket
     mints `pd.deviceId`, HTTP still refuses a device the socket has not minted, and the strip is
     still never re-read — so the reason to read after a reload is unchanged and only the observable
     it names has to change. A fresh profile's first render is now **no strip at all**.
  2. `04-01`'s *fails if* cell — *"the strip still reads `No profile yet.` after the reload"* names a
     string that no longer exists. The failure it describes is still real: after the reload the strip
     is **absent**, i.e. no `<n> Duel coins` line appears. Say that instead. `04-01`'s *do* and
     *expect* columns are unchanged — `A wait "Duel coins"` still describes the second `open`.

## Out of scope

- The `unavailable` branch, and every other case in `ProfileStrip.test.tsx`.
- Where the strip sits on the front door and how — `EPIC-14` item 3a's card, and `ADR-0125` §5 says
  so explicitly.
- Any other case or paragraph in `docs/test-plan.md`. `04-02`, `04-03`, `04-04` and `04-05` name
  other strings and other files.
- `account-text.ts`'s `NO_PROFILE_YET` — a **different** string (*"This browser has no profile yet.
  Reload the page and try again."*), on the sign-up form, owned by `ADR-0056` and not touched by
  `ADR-0125`.

## Tests

`ProfileStrip.test.tsx`

| Test | Proves |
| --- | --- |
| `renders nothing at all when there is no profile` | The `no-profile` branch renders empty markup — `container.innerHTML` is `""` |
| `renders nothing at all when the read did not land` | Unchanged. The `unavailable` branch still renders empty markup, so the two answers agree and neither is a special case |

`drive-arc.test.tsx`

| Test | Proves |
| --- | --- |
| `a first boot mints a device id and asks the server nothing` | Unchanged subject, new wait: a first boot over a fresh storage mints `ALICE.deviceId` and leaves `server.requests` empty |

## Acceptance criteria

- [ ] `ProfileStrip.test.tsx.renders nothing at all when there is no profile` passes
- [ ] `ProfileStrip.test.tsx.renders nothing at all when the read did not land` passes, unedited
- [ ] `drive-arc.test.tsx.a first boot mints a device id and asks the server nothing` passes, and
      still awaits before it asserts
- [ ] `grep -rF "No profile yet." web-client/src` finds nothing
- [ ] `grep -F "No profile yet." docs/test-plan.md` finds nothing
- [ ] `docs/test-plan.md` still contains the row id `04-01` and the string `Duel coins`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md).
