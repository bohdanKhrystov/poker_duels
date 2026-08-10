---
id: EPIC-00
title: Ways of working
type: epic
status: ready
labels: [process, meta]
---

## Goal

The machinery that makes everything else possible: a protected branching model, a ticket system
an agent can work from, decision records, and the documents that serve as the project's memory.

This epic is also the visible half of Product B. What it produces is not scaffolding around the
real work — for the case study, it *is* the work.

## Why now

Nothing else can start. A task cannot be "done" without a definition of done, an agent cannot
pick up work that has not been written down, and no code should reach an integration branch
without review. Every rule here exists to keep the amount of context needed per unit of work
small, which is the constraint the whole project is organised around.

## Scope

- Branch model, protection rules, and the squash-merge policy.
- The in-repo ticket system: hierarchy, IDs, templates, statuses, lifecycle.
- CI that validates the backlog's structure.
- The founding documents: vision, architecture, duel rules, workflow.
- The founding decision records.

## Out of scope

| Not here | Where |
| --- | --- |
| Gradle, ktlint, detekt, build CI | STORY-0101 |
| Any production code | EPIC-01 onward |
| The design system and art pipeline | EPIC-06 |
| Deployment | EPIC-07 |
| Publishing the case study | EPIC-10 |

## Stories

| ID | Title | Status |
| --- | --- | --- |
| [STORY-0001](../stories/STORY-0001-repository-and-ticket-system.md) | Repository, documents and ticket system | ready |

## Definition of done

- [ ] `main` and `develop` exist and are protected, with squash merge as the only option.
- [ ] A ticket can be picked up, worked, and merged by following the written process alone.
- [ ] CI rejects a malformed backlog.
- [ ] Every founding decision has an ADR.

## Metrics

| | |
| --- | --- |
| Tasks completed | |
| Accepted on first review | |
| Manual human edits | |
