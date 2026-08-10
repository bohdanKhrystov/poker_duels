<!--
Title format:  <type>(<scope>): <description> (TICKET-ID)
e.g.           feat(engine): evaluate five-card hands into a comparable rank (TASK-010302)

The title becomes the permanent squash commit. Make it good.
-->

## Ticket

<!-- Exactly one. If there is more than one, the PR is too big. -->

`tasks/tasks/TASK-......-....md`

## What changed

<!-- Two or three sentences. What is now true that was not before. -->

## Acceptance criteria

<!-- Copy them from the ticket, ticked. Anything unticked means this is a draft. -->

- [ ]

## Notes for review

<!-- Anything a reviewer should look at hardest, or a decision worth challenging.
     Delete if there is nothing. -->

---

- [ ] Every acceptance criterion in the ticket is met
- [ ] Tests are included and pass locally
- [ ] The ticket's `status` is updated in this PR
- [ ] `tasks/BOARD.md` is updated
- [ ] Nothing outside the ticket's scope was changed — anything discovered along the way became
      a new ticket instead
- [ ] Any non-obvious decision is recorded as an ADR
