---
name: qa-cycle
description: Run the quality cycle — QA tests the running product for a scope, the manager triages and files bug tickets, build-epic repairs them, QA retests and smokes. Enforces the termination rules so the loop cannot run forever or grow its own backlog. Use when the user asks to test the product, QA an epic, run a regression, or fix what QA finds.
---

# The quality cycle

Takes a **scope** and runs the loop until it reaches a **named exit state**. Every exit is a
success, including the four that are stops.

```
/qa-cycle smoke              the shortest path that proves the product is alive
/qa-cycle epic EPIC-03       one epic's feature suite, plus smoke
/qa-cycle regression         every suite in the catalogue
```

Read [`EPIC-12`](../../../tasks/epics/EPIC-12-quality-and-defect-repair.md) before running. Its
`## Termination` section is the contract this skill enforces, and the two agents
([`qa`](../../agents/qa.md), [`qa-manager`](../../agents/qa-manager.md)) hold the halves.

## The three conditions this harness runs under

[`ADR-0089`](../../../docs/adr/ADR-0089-a-browser-drives-this-client-for-a-qa-round-never-for-a-gate.md)
permits a browser-driving harness here **only while all three of these hold**. They are the terms
that made the cost zero. If any stops holding, the ADR stops licensing the harness and the question
returns as a new `DEC`.

- **a. No dependency.** Nothing enters any module's dependency set to drive a browser. `drive.mjs`
  uses Node built-ins only; Chrome is a machine-local binary this repository does not vendor, pin
  or ship.
- **b. No gate.** `build.yml` keeps its two jobs. No pull request, `verify:` block or ticket waits
  on a QA case — and **a cycle is started by a human's command.** Not a merge, not a cron, and
  **not another skill invoking this one as a step.** If you arrived here from an automated trigger,
  stop.
- **c. No coverage claim.** The product of a run is a **dated round record**. Neither it nor
  `docs/test-plan.md` may be cited as coverage in an epic's `Metrics`, a Definition of done, or a
  ticket's `verify:`. A `PASS` is a statement about one run, on one machine, at one commit.

`ADR-0088` §2's hand-check **remains the proof of record** and §3's receipt is still the one line a
merge cannot write. A QA round substitutes for neither.

Before running, confirm the ADR is merged:

```
grep -n "ADR-0089" docs/adr/README.md | head -3
```

If it is absent, or `DEC-082` is still listed under `## Open decisions`, **stop** and say the cycle
is not licensed yet. Do not run it anyway.

## The stack, and why it is split

`kill`, `pkill`, `killall` and `rm` are all in `settings.json`'s **deny** list, and deny beats
allow — no local override reaches them. The lifecycle is designed around that rather than against
it:

| Piece | Started by | Stopped by |
| --- | --- | --- |
| PostgreSQL | `scripts/qa/stack.sh db-up` | `scripts/qa/stack.sh db-down` (`docker-compose down`) |
| Duel server | **this skill**, as a background Bash task | `TaskStop` on that task |
| Dev server | **this skill**, as a background Bash task | `TaskStop` on that task |
| Two browsers | `scripts/qa/stack.sh chrome-up` | `scripts/qa/stack.sh chrome-down` (CDP `Browser.close`) |

The JVM and Vite are the skill's own background tasks precisely because stopping them any other
way needs a denied verb. Do not make the script do it internally — that defeats the point of
having a deny list.

### Bringing it up

```bash
scripts/qa/stack.sh db-up
CP=$(scripts/qa/stack.sh cp)
```

Then start the server as a **background** task, and the dev server likewise:

```bash
java -cp "$CP" duels.poker.server.ApplicationKt     # run_in_background: true
cd web-client && npm run dev                        # run_in_background: true
```

```bash
scripts/qa/stack.sh wait-server
scripts/qa/stack.sh wait-web
```

Browsers get **fresh profile directories every round**, from `mktemp -d`. Not reused and not
cleaned — `rm` is denied, and a reused profile is worse than a stale directory:

```bash
A=$(mktemp -d); B=$(mktemp -d)
scripts/qa/stack.sh chrome-up 9232 "$A"
scripts/qa/stack.sh chrome-up 9233 "$B"
```

**A fresh profile is not optional.** Clearing `pd.roomCode` is not enough: the server re-seats a
returning device by `pd.deviceId` (`ADR-0018`), so a profile that played before rejoins its old
room even with client storage cleared. Measured on 2026-08-29. A round that reuses profiles tests
the wrong thing and reports defects that are artefacts.

If the stack does not come up, retry **once**. On a second failure, tear down, report
`STOP_INFRA`, and stop.

## The loop

Round `N`, starting at 1.

1. **Test.** Dispatch the `qa` agent with the scope and the two browser ports. It returns a report
   in the fixed shape its definition sets.
2. **Triage.** Dispatch `qa-manager` with that report and the round number. It dedupes, sets
   severity, writes the round story and its bug tickets, and returns a verdict.
3. **Act on the verdict**, and only the verdict:

   | Verdict | Do |
   | --- | --- |
   | `PASS` | tear down, report, **end** |
   | `PROCEED` | go to step 4 |
   | `STOP_DIVERGING` / `STOP_BUDGET` / `STOP_BLOCKED` | tear down, report, **end** |

4. **Repair.** Run the fix set through `build-epic` on `EPIC-12` — bugs are ordinary tasks under
   the round story, so it needs no special handling. Wait for the tickets to merge.
5. **Retest.** `N = N + 1`, back to step 1 with the **same** scope plus the smoke suite.

**Findings during retest belong to round `N+1`, never to round `N`.** The manager enforces this,
but do not hand it round `N`'s report a second time — a round whose bug set can grow has no fixed
point, and the loop never terminates.

## The stopping rules

Restated here because a skill that hides its own exit conditions is how a loop becomes infinite.
`qa-manager` computes and enforces them; this skill obeys without arguing.

- `B(N)` is the count of `blocker` + `high` in round `N`'s report, after dedupe **and after
  harness defects are excluded** (`ADR-0089` §4). A case that fails but does not reproduce by hand
  is a defect in `scripts/qa/` or the catalogue, is repaired there, and **never counts toward
  `B(N)`** — otherwise a stale catalogue reads as a product getting worse and the cycle ends
  `STOP_DIVERGING` on a healthy product.
- **Convergence:** if `B(N) >= B(N-1)`, stop with `STOP_DIVERGING`. Not "one more round".
- **Budget:** at most **three** rounds per invocation.
- **Fix set:** at most **eight** tickets; only `blocker` and `high` ever enter it.
- **Frozen set:** round `N` repairs only what round `N` reported.

Never override a stop because the next round "would probably fix it". The human asked for this
loop not to run forever, in those words. A stop with a clear account is the deliverable.

## Teardown — always, including on failure

```bash
scripts/qa/stack.sh chrome-down 9232 9233
scripts/qa/stack.sh db-down
```

and `TaskStop` the server and dev-server background tasks. A round that leaves a listener on `8080`
or `5173` makes the next round fail on a port collision and report it as a product defect.

## Report to the user

```
CYCLE: <scope>
EXIT: PASS | STOP_DIVERGING | STOP_BUDGET | STOP_BLOCKED | STOP_INFRA
ROUNDS: <n>
B: <B(1)> → <B(2)> → …
FILED: <ticket ids>      MERGED: <ticket ids>
DEFERRED: <ids, to the backlog>
STILL OPEN: <what is not fixed, and the honest reason>
```

Say what is still broken. A cycle that ends `STOP_DIVERGING` with three unfixed blockers named is
more useful than one that reports `PASS` because the suite did not look.
