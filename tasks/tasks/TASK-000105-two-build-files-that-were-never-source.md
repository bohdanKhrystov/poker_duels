---
schema: 2
id: TASK-000105
title: Two build files that were never source
type: task
status: ready
parent: STORY-0001
module: poker-server
estimate: XS
tier: haiku
review: light
files_touched: 3
labels: [repo, hygiene]
depends_on: []
verify:
  - "! git ls-files --error-unmatch poker-server/build.gradle.kts.bak"
  - "! git ls-files --error-unmatch poker-server/build.gradle.kts.tmp"
  - git check-ignore -q poker-server/build.gradle.kts.bak
  - git check-ignore -q poker-server/build.gradle.kts.tmp
  - ./gradlew :poker-server:compileKotlin
---

## Goal

`poker-server/build.gradle.kts.bak` and `poker-server/build.gradle.kts.tmp` are gone from git, and
the pattern that let them in is ignored, so the next editor backup does not become a third one.

## Why this exists

Both are tracked on `develop` — `git ls-files` lists them — from a commit that predates several
tasks, and **three separate coders have reported them independently.** That is the cost: every agent
whose ticket names `poker-server/build.gradle.kts` sees two near-identical siblings beside it and
has to work out which is the real build file before it can spend its context on the ticket.

Nothing reads them. `grep -rn` across the repository, excluding `.git`, finds **no reference** to
either name in any Gradle script, workflow, script or document, and Gradle resolves
`build.gradle.kts` by exact name, so neither has ever been part of a build.

## Files

| File | Action |
| --- | --- |
| `poker-server/build.gradle.kts.bak` | delete |
| `poker-server/build.gradle.kts.tmp` | delete |
| `.gitignore` | modify |

Read, and do not edit:
`poker-server/build.gradle.kts` — the real build file, to confirm by eye that neither deleted file
carries a line the real one is missing.

## Scope

- `git rm` both files. Delete them; do not move them, do not fold anything from them into
  `poker-server/build.gradle.kts`.
- **Before deleting, diff each against `poker-server/build.gradle.kts` and put the diff in the PR
  body.** If either contains a line the real build file lacks, stop and say so rather than deleting
  it: this ticket assumes they are stale copies and that assumption is cheap to check once and
  expensive to be wrong about.
- `.gitignore` gains `*.bak` and `*.tmp` under the existing `# Gradle / JVM` block's neighbours —
  put them in their own short block with a one-line comment saying what they are for. The file
  already carries `*.log`, `*.class` and `*.hprof`, so a pattern for editor and shell leftovers is
  the same idea in the same place.

## Out of scope

- **`.claude/settings.json.bak`, which is also tracked and also matches the new `*.bak` pattern.**
  Found by the same `git ls-files` sweep and not reported by any of the three coders. It is left in
  place deliberately, not overlooked: `.claude/` is agent configuration rather than build output,
  the file may be a deliberate snapshot of a settings file somebody wanted to keep, and deleting a
  configuration backup is a different judgement from deleting a build leftover. **Name it in the
  PR body** so the decision to keep it is made by someone rather than by this ticket's silence.
  Note that a `.gitignore` pattern does not untrack an already-tracked file, so it survives this
  ticket either way — that is documented git behaviour, not a defect in the pattern.
- Any change to `poker-server/build.gradle.kts` itself. If the diff shows the real file is missing
  something, that is a separate ticket and this one stops.
- A repository-wide sweep for other stray files. The `git ls-files` output above is the whole
  finding: three files match, and two of them are this ticket.
- Adding a pre-commit hook or a CI step that refuses stray files. The `.gitignore` line is the
  cheap half; a gate against files somebody force-adds is a different mechanism and is not ticketed
  anywhere.

## Tests

**None, and the `verify:` block is the whole gate.** There is no behaviour here to assert: two files
leave git and a two-line pattern arrives. Four of the five commands are directional and **all four
fail on `develop` today**, which was checked before this ticket was written:

- `git ls-files --error-unmatch <path>` exits **0** for each file now, so the negated form fails
  now and passes only once the file is untracked.
- `git check-ignore -q <path>` exits **1** for each pattern now, so it fails now and passes only
  once `.gitignore` carries it. It matches on patterns and works on a path that no longer exists,
  which is why it can gate a file this ticket deletes.

`./gradlew :poker-server:compileKotlin` is the fifth and is the one that would catch the ticket's
one real risk — that something in the build did read one of them after all.

## Acceptance criteria

- [ ] `git ls-files` lists neither `poker-server/build.gradle.kts.bak` nor
      `poker-server/build.gradle.kts.tmp`
- [ ] `git check-ignore -q` exits 0 for both paths
- [ ] `./gradlew :poker-server:compileKotlin` exits 0
- [ ] `poker-server/build.gradle.kts` is byte-identical to its state before this ticket
- [ ] The PR body carries the diff of each deleted file against `poker-server/build.gradle.kts`
- [ ] The PR body names `.claude/settings.json.bak` as a third tracked match that this ticket
      deliberately leaves
- [ ] Every command in `verify:` exits 0

## Proof

1. Restore one file — `git checkout HEAD~1 -- poker-server/build.gradle.kts.bak` — and leave
   `.gitignore` as this ticket wrote it.
   **`! git ls-files --error-unmatch poker-server/build.gradle.kts.bak` reddens alone**, exiting 1
   because the negated command now succeeds. `git check-ignore` still exits 0: the pattern matches
   whether or not the file is tracked, which is the point of having both commands rather than
   either one. Revert.
2. Delete both files but leave `.gitignore` untouched.
   **Both `git check-ignore -q` commands redden**, exiting 1, while both `ls-files` commands pass.
   This is the half of the ticket that prevents recurrence, and it is the half a reviewer reading
   only the deletion would not notice was missing. Revert.
3. Write the pattern as `*.bak~` instead of `*.bak` — the plausible typo, since editor backups are
   often named that way.
   **`git check-ignore -q poker-server/build.gradle.kts.bak` reddens alone**, exiting 1;
   `*.tmp` still matches and its command passes. Run this one: a `.gitignore` line that looks right
   and matches nothing is the only way this ticket can merge green and do half its job. Revert.

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket.
