---
id: STORY-1201
title: The QA harness — two agents, one skill, one catalogue
type: story
status: done
parent: EPIC-12
labels: [process, meta, qa]
depends_on: []
---

## This is a retrospective record

**Written after the code merged, not before it.** The harness this story names shipped in two
pull requests — `#1159` (`b1c8a753`) and `#1161` (`7f7b905f`) — with **no story file and no
tickets**. This file is written now to close that gap in the trail.

It is deliberately not dressed up as a plan. There were no tickets, so there is no `## Tasks`
table with rows that were never filed, and no acceptance criterion phrased as though a coder had
been asked to meet it. The acceptance criteria below are **structural checks against merged code**:
each is a command a reader can run, and each was run at commit `5848e529` before this file was
committed.

`EPIC-12` is one half of this repository's second deliverable — *the documented process*. A record
that says *this shipped without a ticket, and here is the check that it is real* is worth more to
that deliverable than a fabricated split would be. Tickets written now could only re-specify merged
code, and a coder could satisfy them with an empty diff.

## Why there is no ticket, and the evidence for it

The git history supports a specific answer rather than a guess. The times below are UTC, taken from
the merge commits and from the pull requests themselves.

| When | What |
| --- | --- |
| 10:39:05 | `#1158` opens, carrying the whole harness |
| 10:39:18 | `EPIC-12` merges (`50dffbfe`, `#1157`). Its Stories table lists `STORY-1201` as **`blocked on DEC-082`** — not `ready to split`, which the epic did not yet say |
| 10:40:43 | `#1158` is closed 98 seconds old: its diff carried `EPIC-12`'s files, which had just merged separately via `#1157` |
| 10:40:45 | `#1159` reopens the harness from a clean base |
| 10:59:11 | `ADR-0089` merges (`bb897f14`, `#1160`), answering `DEC-082`. **The same commit flips `STORY-1201` to `ready to split`** |
| 11:04:13 | `#1159` merges (`b1c8a753`) — **five minutes** after the ADR that licensed it |
| 11:25:34 | `#1161` merges (`7f7b905f`) |

The cause is visible in `#1159`'s own body, which says why it was opened before it could be merged:

> **Draft: do not merge before `DEC-082` is answered.** `ADR-0088` §1 refuses a browser runner in
> this repository by name, and this PR is one. It exists so the architect answering `DEC-082` can
> read what is actually proposed rather than a description of it.

So the harness was written as **the concrete artefact `DEC-082` would be decided on**, not as the
output of a split. That was a defensible thing to do: `ADR-0088` §1 forbade the whole category by
name, and a description of a browser runner is a much weaker thing for an architect to rule on than
the runner.

But it left no moment at which a split was possible, and the epic's own file is the proof. Until
10:59 it recorded `STORY-1201` as **`blocked on DEC-082`**, which was accurate: `tasks/README.md`
is explicit that *"the ADR must be merged before a ticket built on it is startable"*. The status
`ready to split` first enters the file in `bb897f14` — **the very commit that merged `ADR-0089` and
removed the block** (`git log -S 'ready to split' -- tasks/epics/EPIC-12-*.md` names no earlier
one). Five minutes later the harness merged.

So `ready to split` never described a state anyone could have acted on. It was true for five
minutes, and for all five of them the thing it described was already written, already reviewed as a
proposal, and already sitting in a mergeable pull request. The status went from `blocked` to
code-merged with no window in between: the split was not skipped, it was never reachable. Nobody
returned to it afterwards.

**The lesson, stated once and not generalised further than the evidence:** a proposal written to
settle a `DEC` becomes merged code the moment the `DEC` is answered, and the story that would have
governed it never gets written. Nothing in the workflow noticed. `tasks/BOARD.md` recorded the gap
in prose on 2026-08-29 — *"a gap in the trail, not in the code"* — which is how it came to be
closed here rather than never.

## What shipped

### `#1159` — `b1c8a753`, six files, 1,023 lines, all new

| File | Role |
| --- | --- |
| [`.claude/agents/qa.md`](../../.claude/agents/qa.md) | Tests the running product for one scope — `epic <ID>`, `smoke` or `regression`. `tools: Read, Bash, Grep` — **no `Write`, no `Edit`**, so it cannot fix what it finds, file a ticket, or edit the catalogue to make a case pass |
| [`.claude/agents/qa-manager.md`](../../.claude/agents/qa-manager.md) | Triages one report against the round ledger. The **only** role that writes a bug ticket, and the only one that can end the loop |
| [`.claude/skills/qa-cycle/SKILL.md`](../../.claude/skills/qa-cycle/SKILL.md) | The loop and its five exit states. Enforces `EPIC-12` §Termination |
| [`scripts/qa/stack.sh`](../../scripts/qa/stack.sh) | Database via `docker-compose`; headless Chrome up, and down over CDP `Browser.close` |
| [`scripts/qa/drive.mjs`](../../scripts/qa/drive.mjs) | One Chrome profile over the DevTools protocol, on Node's built-in `WebSocket` and `fetch` — **no npm package** |
| [`docs/test-plan.md`](../../docs/test-plan.md) | The catalogue: `## SMOKE`, `## CORE` for the v0.1 spine, and a `### Template` for per-epic suites |

The stack lifecycle is designed around the deny list rather than around convenience. `kill`, `pkill`
and `killall` are denied in `settings.json` and deny beats allow, so `stack.sh` deliberately does
**not start what it could not then stop** — the JVM server and Vite are the skill's background
tasks, stopped with the harness's own task-stop, and the script says so in its header instead of
starting them.

### `#1161` — `7f7b905f`, one file, 56 lines added and 2 removed in `SKILL.md`

The cycle reports itself over Telegram through `scripts/notify/notify.py`, on the **round**
boundary. `build-epic` reports at each epic boundary for the reason `ADR-0042` gives — a run
designed not to stop is silent for hours by construction, and *round 2 of 3* and *dead an hour ago*
look identical from outside. Four reports: `heartbeat` (a two-hourly cron plus a forced one per
round), `stop`, `blocked` the moment a verdict is `STOP_BLOCKED`, and `budget` first on a usage
warning. The verdict is the news, not the findings — a round that found eleven defects sends one
message, and the terminal report always sends, whichever of the five states it reached.

## Acceptance criteria

Every command was run from the repository root against the merged tree and exited 0. They check
**structure**, which is all a record can honestly check about merged code; none of them is a
coverage claim, and `ADR-0089` §2c forbids citing any of this as one.

**The four that could plausibly pass for the wrong reason — the two denied-verb checks, the
`drive.mjs` one and the job count — were each probed in the failing direction**, because a check
nobody has seen fail is not evidence. The other six compare an exact string or count a set of
filenames, and fail visibly when they fail. Two mechanisms bit during that probing and are recorded
so the next person does not pay for them twice:

- `grep -E` here does **not** support `^` inside a mid-pattern alternation. `(^|[^A-Za-z])kill`
  matches *nothing at all* rather than erroring — it reported zero hits on a file with three, and
  would have passed on a file containing a real `kill`.
- `grep -vq` over a piped multi-line stream **disagrees between grep binaries** on this machine:
  `printf 'has deny\nhas not\n' | grep -vq den` exits 1 under the `grep` an agent shell resolves,
  and 0 under `/usr/bin/grep`. A criterion built on it could not fail.

Both live in `grep`, so the two criteria that need a *negative across many lines* are written in
`awk`, which behaved identically under every probe. The rest either use plain `-q` over a single
search or hand their **output** to `awk`, which does the deciding; each was run under both binaries
and gave the same answer.

- [x] **The six files exist.**

      ls -1 .claude/agents/qa.md .claude/agents/qa-manager.md .claude/skills/qa-cycle/SKILL.md docs/test-plan.md scripts/qa/stack.sh scripts/qa/drive.mjs | awk 'END{exit (NR==6)?0:1}'

- [x] **`qa` cannot write anything.** Its frontmatter grants exactly `Read, Bash, Grep`, which is
      what makes *"fixes nothing and files nothing"* a property of the tool grant rather than a
      promise in prose.

      grep -q '^tools: Read, Bash, Grep$' .claude/agents/qa.md

- [x] **`qa-manager` is the role that writes.**

      grep -q '^tools: Read, Write, Edit, Bash, Glob, Grep$' .claude/agents/qa-manager.md

- [x] **No denied verb is invoked in `stack.sh`.** With `#` comments and the `USAGE` heredoc
      removed, no line contains `kill`, `pkill` or `killall` as a word. The three occurrences that
      exist — `stack.sh:4`, `:102`, `:136` — are all prose forbidding it, and this command is what
      distinguishes prose from an invocation. Probed both ways before being written down: appending
      `kill -9 "$PID"` makes it exit 1, and appending a line containing the word *skill* leaves it
      at 0.

      awk '/^USAGE$/{h=0;next} h{next} /<<USAGE/{h=1;next} {sub(/#.*/,""); s=" " $0 " "; if (s ~ /[^A-Za-z](kill|pkill|killall)[^A-Za-z]/) bad=1} END{exit bad}' scripts/qa/stack.sh

- [x] **`drive.mjs` does not name a denied verb at all.** It closes a browser with CDP
      `Browser.close`, so it has no reason to.

      ! grep -qE '\b(kill|pkill|killall)\b' scripts/qa/drive.mjs

- [x] **Every mention in the agent and the skill forbids the verb rather than using one.** Two
      lines name them — `.claude/agents/qa.md:51` and `.claude/skills/qa-cycle/SKILL.md:53` — and
      both also say `den`ied/`den`y. The `n>0` term is not decoration: without it a pattern that
      matched nothing would pass, and a check that passes on an empty set is a check that has
      stopped looking. Probed four ways: the real files exit 0; appending
      `Just run kill on the process if it hangs.` to `SKILL.md` exits 1; a file with no mention at
      all exits 1; and adding a line containing the word *skill* leaves it at 0.

      awk '{s=" " $0 " "; if (s ~ /[^A-Za-z](kill|pkill|killall)[^A-Za-z]/) {n++; if ($0 !~ /den/) bad=1}} END{exit (n>0 && !bad) ? 0 : 1}' .claude/agents/qa.md .claude/skills/qa-cycle/SKILL.md

- [x] **The catalogue has a smoke suite, a CORE suite and a per-epic template.**

      grep -E '^(## SMOKE|## CORE|### Template)' docs/test-plan.md | awk 'END{exit (NR==3)?0:1}'

- [x] **The skill names all five exit states** `EPIC-12` §Termination defines.

      grep -oE '\b(PASS|STOP_BUDGET|STOP_DIVERGING|STOP_BLOCKED|STOP_INFRA)\b' .claude/skills/qa-cycle/SKILL.md | sort -u | awk 'END{exit (NR==5)?0:1}'

- [x] **`.github/workflows/build.yml` still has exactly two jobs** — `check` and `client`
      (`ADR-0089` §2b). Probed: adding a third job makes the count 3.

      awk '/^jobs:/{f=1;next} f && /^[^ ]/{f=0} f && /^  [A-Za-z0-9_-]+:[[:space:]]*$/{c++} END{exit (c==2)?0:1}' .github/workflows/build.yml

- [x] **No module gained a browser-driver dependency** (`ADR-0088` §1, `ADR-0089` §2a). The only
      `package.json` outside `node_modules` is `web-client/package.json`.

      ! grep -rqiE 'playwright|puppeteer|selenium|webdriver|cypress' --include=package.json --exclude-dir=node_modules .

## What this record does not claim

- **That the harness works.** These are structural checks on files. The evidence that the harness
  runs is [`STORY-1202`](STORY-1202-the-first-round-smoke-passed-and-one-case-did-not-run-as-written.md),
  round 1, and that is a statement about one run on one machine at one commit.
- **Coverage, of anything.** `ADR-0089` §2c: neither a round record nor `docs/test-plan.md` may be
  cited as coverage in an epic's `Metrics`, a Definition of done, or a ticket's `verify:`. Nothing
  in this file may appear in a `verify:` block either.
- **That the process worked here.** It did not; that is the whole subject of §*Why there is no
  ticket*. The code is sound and the trail was broken, and only the second of those is repaired by
  this file.

## Out of scope

- **Writing tickets.** There is nothing for a coder to do. Tickets manufactured against merged code
  can only be satisfied by an empty diff, which is worse than the gap they would paper over.
- **Ticking any `EPIC-12` Definition-of-done box other than `STORY-1201` is `done`.** In
  particular the harness-versus-product-defect box stays unticked: `STORY-1202` §Note 1 explains at
  length that `SMK-03` never failed — a tester's judgement absorbed it — so the box's condition,
  *"a failing case that did not reproduce by hand"*, was not met. Re-earning it is a round's job,
  not a record's.
- **Changing any file under `.claude/`, `scripts/qa/` or `docs/`.** A record that edits the thing it
  records is no longer a record. The one defect known in the harness is filed and merged already:
  `TASK-120201`.
- **Retrofitting a process fix.** That the workflow does not notice a `DEC`-proposal becoming merged
  code is a real finding, written down above. Acting on it is a decision about the workflow and
  belongs in an ADR or an epic, not in the story that happened to surface it.
