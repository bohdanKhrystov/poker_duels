---
schema: 2
id: TASK-131011
title: The proof origin ADR-0117 described is built
type: task
status: ready
parent: STORY-1310
module: web-client
estimate: XS
tier: sonnet
review: standard
files_touched: 2
labels: [qa, harness, refresh]
depends_on: []
verify:
  - node -e "const p=require('./web-client/package.json'); if (p.scripts.preview !== 'vite preview') { console.error('scripts.preview is ' + JSON.stringify(p.scripts.preview)); process.exit(1) }"
  - grep -qF 'preview: { port: 4173, strictPort: true }' web-client/vite.config.ts
  - cd web-client && npm ci && FORCE_COLOR=0 NO_COLOR=1 npm run --silent check
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npm run --silent build && ! grep -q '@vite/client' dist/index.html
  - python3 .github/scripts/lint_tickets.py
---

## Goal

`web-client` can serve its built bundle on `http://localhost:4173`, which is what
[`ADR-0117`](../../docs/adr/ADR-0117-the-proofs-of-record-load-the-built-bundle.md) §1 decided and
what nothing ever implemented.

## Why this exists

`TASK-131006` — the last unfinished ticket in `EPIC-13`, and the only `blocked` ticket in the whole
backlog — has been blocked since 2026-09-04 for a reason that is not true.

`STORY-1310`'s `P3` row says the drive found a second, deeper instrument fault, reproduced 4/4: the
socket cut also severs Vite's dev-mode HMR socket, multiplexed through the same port, so the page
performs a real reload and `reconnecting.ts` can never be observed recovering in place. The row then
concludes:

> **This needs a decision no merged source settles** — whether `P3` is driven against a build
> without Vite's HMR client, or the relay is made to spare that socket, or some other instrument
> change — so a `DEC` belongs in `docs/adr/README.md`'s `## Open decisions`; not registered here.

Two things are wrong with that, and together they are why the ticket has sat for six days:

1. **The `DEC` was never registered.** `grep` over `docs/adr/README.md` and `tasks/BOARD.md` finds no
   open decision about the cut, the HMR client or the instrument. So nothing was ever going to
   unblock it — it was waiting on a question nobody asked.
2. **It needed no decision.** `ADR-0117` was merged on **2026-09-04 in PR #1389**; the `P3` row was
   written **later the same day, in PR #1392**. Its §1 decides exactly the first of the three
   options the row lists: the proofs of record load `dist/`, served by `vite preview`, on
   `http://localhost:4173` — a built bundle, with no HMR client to sever.

So the blocker is real but misnamed. It is not an open product or technical question. It is that
`ADR-0117`'s own two-line change was never cut into a ticket:

| `ADR-0117` §1 requires | State on `develop` |
| --- | --- |
| `web-client/package.json` gains `"preview": "vite preview"` | absent — `scripts` has `build dev check lint test typecheck format format:check` |
| `web-client/vite.config.ts` gains `preview: { port: 4173, strictPort: true }` | absent — no `preview` key at all |

Both files were last modified on 2026-08-14, three weeks before the ADR merged.

## Scope

The two lines `ADR-0117` §1 writes out, verbatim, and nothing else.

`strictPort: true` is copied because the ADR calls it load-bearing rather than tidiness: without it
a leftover listener makes Vite pick the next free port and a drive proceeds against an origin nobody
named — the same class of fault as `STORY-1216`'s stale tree.

## Out of scope

- **Re-driving `P3`.** That is `TASK-131006`, which this unblocks. Do not touch its row, its status,
  or `STORY-1310`'s table.
- **`preview.proxy`.** `ADR-0117` §1 refuses it in as many words: Vite resolves
  `preview.proxy ?? server.proxy`, so both modes read the one table `ADR-0026` established. Two
  tables would be two things that can disagree.
- **Pointing any existing drive, skill or agent at `:4173`.** `ADR-0117` supersedes `ADR-0088` §2
  step 3, and moving the drivers over is its own change with its own reviewer.
- **Registering a `DEC`.** The row asked for one; this ticket's finding is that the answer was
  already merged, so registering one now would record a question that has an answer.

## Files

| File | Change |
| --- | --- |
| `web-client/package.json` | one `scripts` entry: `"preview": "vite preview"` |
| `web-client/vite.config.ts` | one key: `preview: { port: 4173, strictPort: true }` |

## Tests

No unit test is added. Two of the four gates are literal-string checks on the two lines, which is
the right shape here for the reason [`ADR-0117`](../../docs/adr/ADR-0117-the-proofs-of-record-load-the-built-bundle.md)
§1 gives: the port number and the flag are the decision, so a gate that computed them from the
config would agree with any value the config happened to hold.

The load-bearing gate is the fourth:

```
npm run build && ! grep -q '@vite/client' dist/index.html
```

That is the one that rejects the defect `P3` actually hit. A `preview` script that served the dev
bundle would satisfy every other gate here and still ship an HMR client for the cut to sever, which
is precisely the state `develop` is in today. Probe it: add `@vite/client` to the built
`index.html` by hand and confirm the gate goes red before you rely on it.

## What this unblocks

`TASK-131006` — set it `ready` in a **separate** PR once this merges, not in this one. `STORY-1310`
and `STORY-1311` both read `ready` while `STORY-1311`'s fourteen tickets are all `done`; correcting
those two statuses is that PR's business too, and `EPIC-13` reads `done` above both of them.
