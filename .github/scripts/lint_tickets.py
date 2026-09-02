#!/usr/bin/env python3
"""Validate the ticket backlog under tasks/.

Checks structure, not prose: IDs match filenames, parents exist and are one level up,
statuses are real, dependencies resolve, and nothing is orphaned. Run with no arguments
from the repository root.

Exits 0 when the backlog is well formed, 1 otherwise.
"""

from __future__ import annotations

import re
import sys
from pathlib import Path

TASKS = Path("tasks")
SKIP_DIRS = {"templates"}

# "dropped" is a ticket that was filed and then correctly abandoned — most often because its
# premise turned out to be false. It is not "done", and rewriting it as such would hide a
# real event in the trail; the ticket keeps its file and records why it was dropped.
STATUSES = {"backlog", "ready", "in-progress", "in-review", "blocked", "done", "dropped"}
SETTLED = {"done", "dropped"}

# Schema 1 is the original hand-written backlog. Schema 2 is the token-lean format an agent
# workflow consumes: smaller, tiered by model, and gated by executable verify commands.
# Both are accepted so that stories can be migrated one at a time rather than all at once.
LEGACY_ESTIMATES = {"S", "M"}
ESTIMATES_V2 = {"XS", "S"}
TIERS = {"haiku", "sonnet", "opus"}
REVIEW_LEVELS = {"light", "standard", "deep"}
MAX_FILES_TOUCHED = 3
# A change some merged gate refuses to let land in pieces — a protocol version bump, an
# interface signature dragging its implementers, a NOT NULL column breaking every fixture.
# ADR-0068: the ticket declares the true count and names the gate in `atomic:`. There is no
# ceiling (ADR-0069 deleted it — the set is monotone in a gate count nobody controls, so every
# number written down has been wrong); instead the count must equal the ticket's own Files
# table. Without `atomic:` nothing changes, the cap is 3, and the table is not read.
MIN_FILES_TOUCHED_ATOMIC = 4
FILES_TABLE_EDITS = {"create", "modify", "regenerate", "delete", "rename"}
FILES_TABLE_ACTIONS = FILES_TABLE_EDITS | {"read"}

ID_PATTERNS = {
    "epic": re.compile(r"^EPIC-\d{2}$"),
    "story": re.compile(r"^STORY-\d{4}$"),
    "task": re.compile(r"^TASK-\d{6}$"),
}
DIR_FOR_TYPE = {"epic": "epics", "story": "stories", "task": "tasks"}
LIST_RE = re.compile(r"^\[(.*)\]$")

errors: list[str] = []


def fail(where: str, message: str) -> None:
    errors.append(f"{where}: {message}")


def parse_frontmatter(path: Path) -> dict[str, object] | None:
    """Parse the flat YAML frontmatter our tickets use.

    Deliberately not a YAML parser: the schema is a fixed set of scalars and simple
    inline lists, and depending on PyYAML in CI to read six keys is not worth it.
    """
    text = path.read_text(encoding="utf-8")
    if not text.startswith("---\n"):
        fail(str(path), "missing frontmatter")
        return None
    end = text.find("\n---\n", 4)
    if end == -1:
        fail(str(path), "unterminated frontmatter")
        return None

    data: dict[str, object] = {}
    last_key: str | None = None
    for lineno, line in enumerate(text[4:end].splitlines(), start=2):
        if not line.strip() or line.lstrip().startswith("#"):
            continue

        # Block sequence item belonging to the previous key. Matched before the key/value
        # split because verify commands are full shell lines and contain colons of their own
        # (`./gradlew :poker-engine:test`), which a naive partition would tear apart.
        if line[:1].isspace() and line.lstrip().startswith("- "):
            if last_key is None:
                fail(str(path), f"line {lineno}: list item with no key above it")
                continue
            data.setdefault(last_key, [])
            if not isinstance(data[last_key], list):
                fail(str(path), f"line {lineno}: '{last_key}' has both a value and list items")
                continue
            data[last_key].append(line.lstrip()[2:].strip())
            continue

        if ":" not in line:
            fail(str(path), f"line {lineno}: not a key/value pair: {line!r}")
            continue
        key, _, raw = line.partition(":")
        key, raw = key.strip(), raw.strip()
        last_key = key
        match = LIST_RE.match(raw)
        if match:
            inner = match.group(1).strip()
            data[key] = [v.strip() for v in inner.split(",") if v.strip()] if inner else []
        elif raw == "":
            # `verify:` on its own line — items follow as a block sequence.
            data[key] = []
        else:
            data[key] = raw
    return data


def count_files_table_edits(where: str) -> int | None:
    """Count the edit rows of a ticket's `## Files` table.

    Read only for a ticket declaring `atomic:` (ADR-0069 §1): with no ceiling left, the one
    thing still checkable mechanically is that `files_touched` equals the ticket's own table.
    Deliberately positional rather than a markdown parser — the second cell is the action, and
    an unrecognised one fails rather than being skipped, so a typo cannot quietly undercount.

    Returns None when there is no Files table at all, which is its own failure.
    """
    lines = Path(where).read_text(encoding="utf-8").splitlines()
    try:
        start = next(i for i, line in enumerate(lines) if line.strip().lower() == "## files")
    except StopIteration:
        return None

    edits = 0
    for line in lines[start + 1 :]:
        if line.startswith("## "):
            break
        if not line.lstrip().startswith("|"):
            continue
        cells = [c.strip() for c in line.strip().strip("|").split("|")]
        if len(cells) < 2:
            continue
        action = cells[1].strip("*` ").lower()
        if action in ("action", "") or set(action) <= {"-", ":"}:
            continue  # header row, separator, or an empty cell
        if action not in FILES_TABLE_ACTIONS:
            fail(where, f"Files table row has an unknown action {cells[1]!r}: {line.strip()}")
            continue
        if action in FILES_TABLE_EDITS:
            edits += 1
    return edits


def check_task_schema(where: str, data: dict[str, object]) -> None:
    """Validate a task against schema 1 or schema 2, whichever it declares.

    A story is migrated as a unit, so both schemas coexist in the backlog while EPIC-01 is
    converted story by story. Schema 2 is the stricter one and is what agents consume.
    """
    schema = str(data.get("schema", "1"))
    estimate = data.get("estimate")

    if schema == "1":
        if estimate not in LEGACY_ESTIMATES:
            fail(where, f"estimate must be S or M on a schema-1 task, got {estimate!r}")
        return

    if schema != "2":
        fail(where, f"unknown schema {schema!r} — expected 1 or 2")
        return

    if estimate not in ESTIMATES_V2:
        fail(where, f"estimate must be XS or S on a schema-2 task (M is gone), got {estimate!r}")

    tier = data.get("tier")
    if tier not in TIERS:
        fail(where, f"tier must be one of {sorted(TIERS)}, got {tier!r}")

    review = data.get("review")
    if review not in REVIEW_LEVELS:
        fail(where, f"review must be one of {sorted(REVIEW_LEVELS)}, got {review!r}")

    # `atomic:` is the one declared exemption from the three-file cap (ADR-0068): a block
    # sequence naming, one per line, each merged gate that fails on a smaller commit. The linter
    # checks that it is there and that the count matches the Files table, never that the claim
    # is true — that is the reviewer's, and the ADR says so rather than implying otherwise.
    atomic = data.get("atomic")
    claimed_atomic = atomic is not None
    if claimed_atomic and not (
        isinstance(atomic, list) and atomic and all(str(g).strip() for g in atomic)
    ):
        fail(where, "atomic: must be a block sequence naming one merged gate per line")
        claimed_atomic = False

    touched = data.get("files_touched")
    try:
        touched_n = int(str(touched))
    except (TypeError, ValueError):
        fail(where, f"files_touched must be an integer, got {touched!r}")
    else:
        if claimed_atomic:
            # Below four, `atomic:` buys nothing and only blurs what its presence means. Above
            # it there is no ceiling: ADR-0069 replaced the number with an equality, because a
            # ceiling can only ever be the size of the last atomic ticket anybody wrote.
            if touched_n < MIN_FILES_TOUCHED_ATOMIC:
                fail(
                    where,
                    f"files_touched must be at least {MIN_FILES_TOUCHED_ATOMIC} on a task "
                    f"declaring atomic:, got {touched_n} — below that the key buys nothing",
                )
            table_edits = count_files_table_edits(where)
            if table_edits is None:
                fail(where, "a task declaring atomic: must have a '## Files' table")
            elif table_edits != touched_n:
                fail(
                    where,
                    f"files_touched is {touched_n} but the Files table has {table_edits} "
                    f"create/modify/regenerate/delete/rename rows — the field is a fact about "
                    f"the ticket and must equal its own table (ADR-0069)",
                )
        elif not 1 <= touched_n <= MAX_FILES_TOUCHED:
            fail(
                where,
                f"files_touched must be 1..{MAX_FILES_TOUCHED}, got {touched_n} — a change no "
                f"merged gate forbids splitting is two tickets; one that is atomic declares "
                f"atomic: and the true count (ADR-0068)",
            )

    # The verify block is what makes a cheap model reliable: done is "these commands exit 0",
    # not "the code looks right". A schema-2 task without one has no objective gate at all.
    verify = data.get("verify")
    if not isinstance(verify, list) or not verify:
        fail(where, "schema-2 task needs a non-empty 'verify:' block of shell commands")
    else:
        for command in verify:
            if not str(command).strip():
                fail(where, "empty command in 'verify:'")


def collect() -> dict[str, dict]:
    tickets: dict[str, dict] = {}
    for path in sorted(TASKS.rglob("*.md")):
        rel = path.relative_to(TASKS)
        if rel.parts[0] in SKIP_DIRS or len(rel.parts) == 1:
            continue  # templates, and README.md / BOARD.md at the root

        where = str(path)
        data = parse_frontmatter(path)
        if data is None:
            continue

        for field in ("id", "title", "type", "status"):
            if not data.get(field):
                fail(where, f"missing required field '{field}'")
        ticket_type = data.get("type")
        ticket_id = data.get("id")
        if not isinstance(ticket_type, str) or ticket_type not in ID_PATTERNS:
            fail(where, f"type must be one of {sorted(ID_PATTERNS)}, got {ticket_type!r}")
            continue
        if not isinstance(ticket_id, str) or not ID_PATTERNS[ticket_type].match(ticket_id):
            fail(where, f"id {ticket_id!r} does not match the {ticket_type} format")
            continue

        if ticket_id in tickets:
            fail(where, f"duplicate id, already used by {tickets[ticket_id]['path']}")
            continue
        if not path.name.startswith(f"{ticket_id}-"):
            fail(where, f"filename does not start with its id {ticket_id!r}")
        expected_dir = DIR_FOR_TYPE[ticket_type]
        if rel.parts[0] != expected_dir:
            fail(where, f"a {ticket_type} belongs in tasks/{expected_dir}/, found in {rel.parts[0]}/")

        status = data.get("status")
        if status not in STATUSES:
            fail(where, f"status {status!r} is not one of {sorted(STATUSES)}")

        if ticket_type == "task":
            check_task_schema(where, data)

        parent = data.get("parent")
        if ticket_type == "epic" and parent:
            fail(where, "an epic must not declare a parent")
        if ticket_type in ("story", "task") and not parent:
            fail(where, f"a {ticket_type} must declare a parent")

        for field in ("labels", "depends_on"):
            if field in data and not isinstance(data[field], list):
                fail(where, f"'{field}' must be a list, e.g. [a, b]")

        data["path"] = where
        tickets[ticket_id] = data
    return tickets


def check_links(tickets: dict[str, dict]) -> None:
    parent_type = {"story": "epic", "task": "story"}
    for ticket_id, data in tickets.items():
        where = data["path"]
        parent = data.get("parent")
        if parent:
            if parent not in tickets:
                fail(where, f"parent {parent} does not exist")
            else:
                expected = parent_type[data["type"]]
                if tickets[parent]["type"] != expected:
                    fail(where, f"parent {parent} must be an {expected}")
                elif data["type"] == "task" and not ticket_id.startswith(parent.replace("STORY-", "TASK-")):
                    fail(where, f"id {ticket_id} does not encode its parent {parent}")
                elif data["type"] == "story" and not ticket_id.startswith(parent.replace("EPIC-", "STORY-")):
                    fail(where, f"id {ticket_id} does not encode its parent {parent}")

        for dep in data.get("depends_on", []):
            if dep not in tickets:
                fail(where, f"depends_on references unknown ticket {dep}")
            elif dep == ticket_id:
                fail(where, "a ticket cannot depend on itself")

        # 'ready' on a task means an agent can start it right now, so its dependencies must
        # already be done. On an epic or a story it only means "specified", which says
        # nothing about whether the work can start yet.
        if data["type"] == "task" and data["status"] == "ready":
            unmet = [
                dep for dep in data.get("depends_on", [])
                if dep in tickets and tickets[dep]["status"] not in SETTLED
            ]
            if unmet:
                fail(where, f"status is 'ready' but these are not done: {', '.join(unmet)}")


def normalize_status(raw: str) -> str:
    """Normalize a board status cell for comparison.

    Takes the text before the first em dash, strips markdown formatting and whitespace,
    and lowercases. Handles bold (**status**), strikethrough (~~status~~), backticks,
    and prose after an em dash (— explanation).
    """
    # Take text before the first em dash
    text = raw.split(" — ")[0].strip()
    # Strip markdown formatting and whitespace
    text = text.replace("*", "").replace("~", "").replace("`", "").strip()
    return text.lower()


def split_table_row(line: str) -> list[str]:
    """Split a markdown table row into cells, respecting backtick-quoted code spans.

    Pipes inside backticks are not treated as separators. Without backtick awareness, a cell
    containing `grep -o … \| wc -l` would be incorrectly split at the escaped pipe, creating
    false cell boundaries and wrong status extraction. The board contains such cells (e.g.,
    TASK-041223), so this parser tracks backtick state to distinguish pipes inside code from
    table separators.
    """
    # Remove leading and trailing pipes and whitespace
    line = line.strip()
    if line.startswith("|"):
        line = line[1:]
    if line.endswith("|"):
        line = line[:-1]

    # Split on pipes that are not inside backticks
    cells: list[str] = []
    current_cell = ""
    in_backticks = False

    for char in line:
        if char == "`":
            in_backticks = not in_backticks
            current_cell += char
        elif char == "|" and not in_backticks:
            cells.append(current_cell.strip())
            current_cell = ""
        else:
            current_cell += char

    if current_cell:
        cells.append(current_cell.strip())

    return cells


def read_board_statuses() -> dict[str, str]:
    """Read task statuses from tasks/BOARD.md.

    Returns a dict mapping task ID to normalized status. Only recognizes rows with
    markdown links to task files: ](tasks/TASK-NNNNNN-….md). Prose mentions of task
    IDs outside of table rows are ignored.
    """
    board_path = TASKS / "BOARD.md"
    if not board_path.exists():
        return {}

    board_statuses: dict[str, str] = {}
    text = board_path.read_text(encoding="utf-8")

    for line in text.splitlines():
        # A board row is a line whose stripped form starts with | and contains a task link
        stripped = line.strip()
        if not stripped.startswith("|"):
            continue

        # Look for a task link: ](tasks/TASK-NNNNNN-….md)
        task_link_pattern = re.compile(r"\]\(tasks/(TASK-\d{6}-[^)]*\.md)\)")
        match = task_link_pattern.search(line)
        if not match:
            continue

        # Extract the task ID from the link
        link_text = match.group(1)
        task_id_match = re.match(r"(TASK-\d{6})", link_text)
        if not task_id_match:
            continue
        task_id = task_id_match.group(1)

        # Extract the last cell as the status
        cells = split_table_row(line)
        if not cells:
            continue
        status_cell = cells[-1]

        # Normalize and store
        board_statuses[task_id] = normalize_status(status_cell)

    return board_statuses


def check_board_register(tickets: dict[str, dict]) -> None:
    """Verify that board status cells match their corresponding ticket files.

    Fails if:
    - A task file has no corresponding board row
    - A board row's status differs from its file's status
    """
    board_statuses = read_board_statuses()

    # Check each task file has a board row with matching status
    for task_id, data in tickets.items():
        if data["type"] != "task":
            continue

        if task_id not in board_statuses:
            fail("tasks/BOARD.md", f"task {task_id} has no board row")
            continue

        board_status = board_statuses[task_id]
        file_status = str(data.get("status", "")).lower()

        if board_status != file_status:
            fail(
                "tasks/BOARD.md",
                f"task {task_id}: board says '{board_status}', file says '{file_status}'"
            )


def startable(tickets: dict[str, dict]) -> list[dict]:
    """Tasks an agent can begin right now, in dependency then id order.

    Startability is derived rather than stored: `ready` says the ticket is specified, and
    depends_on says whether anything is in the way. Keeping them separate means merging one
    ticket does not require editing every ticket downstream of it.
    """
    return [
        data
        for _, data in sorted(tickets.items())
        if data["type"] == "task"
        and data["status"] == "ready"
        and all(tickets.get(dep, {}).get("status") == "done" for dep in data.get("depends_on", []))
    ]


CONFLICT_MARKER_RE = re.compile(r"^(<{7} |={7}$|>{7} )", re.M)


def check_conflict_markers() -> None:
    """A resolved-looking merge that still carries markers reads as prose and lints as fine.

    This has reached `develop` once: a rebase output truncated past a second
    conflicting file, and the register was staged with its markers intact.
    """
    for base in (TASKS, Path("docs")):
        if not base.is_dir():
            continue
        for path in sorted(base.rglob("*.md")):
            text = path.read_text(encoding="utf-8", errors="replace")
            hits = CONFLICT_MARKER_RE.findall(text)
            if hits:
                line = next(
                    i for i, l in enumerate(text.splitlines(), 1)
                    if CONFLICT_MARKER_RE.match(l)
                )
                fail(str(path), f"unresolved conflict marker at line {line} ({len(hits)} in the file)")


def main() -> int:
    if not TASKS.is_dir():
        print("tasks/ not found — run from the repository root", file=sys.stderr)
        return 1

    want_startable = "--startable" in sys.argv[1:]

    check_conflict_markers()

    tickets = collect()
    check_links(tickets)
    check_board_register(tickets)

    counts = {t: sum(1 for d in tickets.values() if d["type"] == t) for t in ID_PATTERNS}
    if errors:
        print(f"{len(errors)} problem(s) in the backlog:\n", file=sys.stderr)
        for error in errors:
            print(f"  {error}", file=sys.stderr)
        return 1

    if want_startable:
        ready = startable(tickets)
        if not ready:
            print("no startable task — every ready task is blocked by a dependency")
            return 0
        for data in ready:
            print(
                f"{data['id']}  schema={data.get('schema', '1')}  "
                f"{data.get('estimate', '?')}  tier={data.get('tier', '-')}  "
                f"review={data.get('review', '-')}  {data['path']}"
            )
        return 0

    print(
        f"backlog ok — {counts['epic']} epics, {counts['story']} stories, {counts['task']} tasks"
    )
    return 0


if __name__ == "__main__":
    sys.exit(main())
