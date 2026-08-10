---
schema: 2
id: TASK-010107
title: Make checkNoDependencies configuration-cache safe
type: task
status: ready
parent: STORY-0101
module: poker-engine
estimate: XS
tier: haiku
review: standard
files_touched: 1
labels: [build, architecture]
depends_on: [TASK-010103]
verify:
  - ./gradlew :poker-engine:checkNoDependencies --configuration-cache
  - ./gradlew :poker-engine:checkNoDependencies
---

## Goal

`checkNoDependencies` runs under Gradle's configuration cache instead of failing to serialise.

## Context

Found while reviewing [`TASK-010103`](TASK-010103-engine-dependency-rule.md), and deliberately
left out of it — that ticket's scope was the rule, not the cache. The task body reads
`project.configurations` inside `doLast`, which captures the `Project` instance at execution
time. With the configuration cache on, that fails:

```
cannot serialize object of type 'org.gradle.api.internal.project.DefaultProject' …
not supported with the configuration cache
```

It passes today only because the build does not enable the configuration cache yet. It will bite
whenever anyone turns it on, including CI.

## Files

| File | Action |
| --- | --- |
| `poker-engine/build.gradle.kts` | modify |

## Scope

Capture the declared dependency coordinates of `implementation`, `api`, `compileOnly` and
`runtimeOnly` during the **configuration** phase, hold them as a task input, and inspect only
that captured state in `doLast`. No reference to `Project` or `configurations` at execution time.

The rule itself must not change: the same four configurations, the same narrow `kotlin-stdlib`
exemption, test configurations still exempt.

## Out of scope

- Enabling the configuration cache for the build — a separate decision, and a separate ticket.
- Any other module or check.

## Tests

None in Kotlin. The `verify` block is the check: the task must pass both with and without
`--configuration-cache`.

## Acceptance criteria

- [ ] `./gradlew :poker-engine:checkNoDependencies --configuration-cache` exits 0.
- [ ] `./gradlew :poker-engine:checkNoDependencies` still exits 0.
- [ ] Temporarily adding `implementation("com.google.guava:guava:33.0.0-jre")` still makes both
      forms exit non-zero. Remove the line again before finishing.
- [ ] The task body contains no reference to `project` or `configurations` inside `doLast`.

## Definition of done

Standard, per [`tasks/README.md`](../README.md): `verify` green, review passed, CI green, status
`done`, `BOARD.md` updated, squash-merged into `develop`. Not done until the PR is merged.
