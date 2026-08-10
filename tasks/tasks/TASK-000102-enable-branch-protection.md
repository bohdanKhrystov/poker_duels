---
id: TASK-000102
title: Enable branch protection on main and develop
type: task
status: blocked
parent: STORY-0001
estimate: S
labels: [process, meta, ci]
depends_on: []
---

## Goal

Make the branching rules enforced by GitHub rather than observed by convention.

## Why this is not done

GitHub refuses both classic branch protection and repository rulesets on a **private repository
on the free plan**:

```
403  Upgrade to GitHub Pro or make this repository public to enable this feature.
```

`poker_duels` is private and on the free plan, so as of `TASK-000101` the branch model is
documented and followed, but nothing at the server stops a direct push to `develop` or `main`.
This ticket exists so that gap is visible rather than assumed away.

## Blocked on

One of, whichever is preferred:

- making the repository **public** — free, and consistent with the case-study goal in
  [`docs/vision.md`](../../docs/vision.md); or
- **GitHub Pro**, which enables protection on private repositories.

Until one of those happens this ticket stays `blocked`. It is not under-specified — everything
below is ready to apply the moment the constraint lifts.

## Scope

Apply to **both** `main` and `develop`:

| Rule | Value |
| --- | --- |
| Pull request required | yes |
| Required approving reviews | 0 — the gate is `/code-review` plus a human merge, per [`ADR-0006`](../../docs/adr/ADR-0006-mandatory-review-gate.md) |
| Dismiss stale reviews on push | yes |
| Required status check | `lint backlog`, strict |
| Linear history required | yes |
| Force pushes | blocked |
| Deletions | blocked |
| Conversation resolution required | yes |
| Enforce for administrators | **yes** — the rule is worthless if the owner is exempt |

The command, ready to run:

```sh
for BR in develop main; do
  gh api -X PUT "repos/bohdanKhrystov/poker_duels/branches/$BR/protection" --input - <<'JSON'
  { "required_status_checks": { "strict": true, "contexts": ["lint backlog"] },
    "enforce_admins": true,
    "required_pull_request_reviews": {
      "required_approving_review_count": 0,
      "dismiss_stale_reviews": true,
      "require_last_push_approval": false },
    "restrictions": null,
    "required_linear_history": true,
    "allow_force_pushes": false,
    "allow_deletions": false,
    "required_conversation_resolution": true }
JSON
done
```

## Out of scope

- Deciding whether the repository goes public. That is a judgement call for the owner, not a
  task step.
- Repository merge settings — squash-only, auto-delete-branch and `develop` as default were all
  applied in `TASK-000101`; they do not require a paid plan.

## Acceptance criteria

- [ ] A direct push to `develop` is rejected by GitHub, verified by attempting one.
- [ ] A direct push to `main` is rejected, verified the same way.
- [ ] A force push to either branch is rejected.
- [ ] `lint backlog` is a required check on both branches.
- [ ] Protection applies to the repository owner as well — `enforce_admins` is true.
- [ ] `CONTRIBUTING.md` no longer carries the "not yet enforced" note.

## Tests

Verified by attempting the operations the rules forbid and confirming GitHub refuses them. A
protection rule nobody has tried to violate has not been tested.

## Definition of done

Standard, per [`tasks/README.md`](../README.md): build green, tests green, `/code-review` run
with findings fixed or answered, CI green, status `done`, `BOARD.md` updated, and
**squash-merged into `develop`** by a PR linking this ticket. Not done until the PR is merged.
