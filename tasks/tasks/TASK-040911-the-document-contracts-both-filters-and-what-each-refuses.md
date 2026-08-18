---
schema: 2
id: TASK-040911
title: The document contracts both filters, and what each of them refuses
type: task
status: backlog
parent: STORY-0409
module: poker-server
estimate: S
tier: haiku
review: light
files_touched: 2
labels: [server, docs, protocol, history, filters]
depends_on: [TASK-040910]
verify:
  - ./gradlew :poker-server:test --tests 'duels.poker.server.http.HttpEndpointDocumentationTest'
  - ./gradlew :poker-server:check -PrequireDocker=true
---

## Goal

`docs/protocol.md` states what `outcome` and `opponent` accept, what each refuses, and the one thing
the endpoint does not yet check.

## Files

| File | Action |
| --- | --- |
| `docs/protocol.md` | modify — the `### Recent duels endpoint` section only |
| `poker-server/src/test/kotlin/duels/poker/server/http/HttpEndpointDocumentationTest.kt` | modify — one test |
| `poker-server/src/main/kotlin/duels/poker/server/http/DuelFilter.kt` | read — the rules being written down |

## The trap this ticket exists to avoid

`recentDuelsSection` is the span from `### Recent duels endpoint` to
`Each duel summary in the array contains:`, and `theRecentDuelsSectionNamesEveryFieldTheResponseHas`
asserts `RecentDuelsResponse`'s property set is **exactly** `{duels, nextCursor}` and that both are
in that section's field table. Add the two parameters as **bullets** beside `limit` and `after`,
never as `| outcome |` and `| opponent |` table rows: a row there reads as a field of the response,
which is what `TASK-040811` recorded when `nextCursor` nearly landed in the wrong span.

## Scope — the document

Everything new goes inside `### Recent duels endpoint`, in the existing query-parameter bullet list,
and **above** the line `Each duel summary in the array contains:`.

- `outcome` (optional): `WON`, `LOST` or `DREW` — the same three spellings a duel summary's own
  `outcome` field uses, so a client can hand one straight back. Any other value, **including a
  lower-case spelling**, is `400 Bad Request` and nothing is read. The outcome is read from the
  duel's stored coin delta, never asserted by a client (`ADR-0002`).
- `opponent` (optional): a **case-insensitive substring** of the opponent's display name. NFC
  normalised, then 1–32 code points — the display name's own bound — so blank or longer is
  `400 Bad Request` and nothing is read. `%` and `_` match **literally**: the term is a string, not
  a pattern. An opponent who has never set a name matches no search; the server fabricates no
  placeholder to match against (`ADR-0029` §6). Searching by name returns **duels**, never players:
  no path here turns a name into an identity (`ADR-0029` §7).
- One sentence on the two together: a parameter that is present but unusable refuses the whole
  request, so a request with a good `opponent` and a bad `outcome` is `400` rather than a page
  filtered by name alone.
- One sentence recording what is **not** yet checked, because a contract that overstates itself is
  worse than one with a gap in it: `after` names a position in the `finishedAt`/`duelId` order and
  is meaningful only alongside the filter that produced it; v0.1 does not yet refuse a cursor
  replayed under a different filter, so a client that changes a filter must start a new page walk
  rather than reuse `nextCursor` (`DEC-050`).

## Scope — the test

One new test in `HttpEndpointDocumentationTest`, beside `theRecentDuelsSectionDocumentsTheCursor`
and written to the same shape. **Assert properties ⇒ documented only** for this section; a
documented ⇒ exists check would demand that `outcome` and `opponent` be properties of
`RecentDuelsResponse`, which is exactly what they are not.

## Out of scope

- Any change to `DuelSummaryResponse`, its section, or the tests over it.
- Any change to the existing `limit`, `after`, `duels` or `nextCursor` text beyond adding the one
  sentence about `after` above. In particular the strings `defaults to \`10\`` and `capped at \`50\``
  stay verbatim — `theDocumentStatesTheLimitDefaultAndCap` matches on them.
- `PROTOCOL_VERSION`, `protocol.gen.ts` and `docs/protocol-versions.md`. `RecentDuelsResponse` is
  reachable from neither message root, so none of them moves (`ADR-0053`; `TASK-040806` recorded it).
- The client's half of the contract — `STORY-0413`.

## Tests

`HttpEndpointDocumentationTest`

| Test | Proves |
| --- | --- |
| `theRecentDuelsSectionDocumentsTheFilters` | `recentDuelsSection` contains every one of `outcome`, `opponent`, `WON`, `substring`, `literally` and `400` — the two parameter names, the spelling of a value, the shape of the match, the promise about wildcards and the refusal. Six `assertTrue`s with distinct messages, in the style the neighbouring cursor test already uses. It fails against a document that names the parameters and skips what they refuse, which is the half most often left out |

## Acceptance criteria

- [ ] `HttpEndpointDocumentationTest.theRecentDuelsSectionDocumentsTheFilters` passes and asserts all
      six strings
- [ ] `outcome` and `opponent` appear in `docs/protocol.md` as bullets, not as the first cell of a
      table row
- [ ] Both bullets sit above the line `Each duel summary in the array contains:`
- [ ] The document states that a `%` or `_` in `opponent` matches literally
- [ ] The document states the `DEC-050` gap: `after` is meaningful only with the filter that
      produced it, and a replayed cursor is not yet refused
- [ ] Every test already in `HttpEndpointDocumentationTest` passes with its assertions unchanged, in
      particular `theRecentDuelsSectionNamesEveryFieldTheResponseHas`,
      `theDocumentedFieldNamesAllExist`, `theDocumentMarksTheNextCursorNullable` and
      `theDocumentStatesTheLimitDefaultAndCap`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
