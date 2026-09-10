#!/usr/bin/env bash
# The QA stack: what this script starts, and — since TASK-121606 — everything it can also stop.
#
# `kill`, `pkill` and `killall` are in settings.json's deny list, and deny beats allow. This script
# was written to own only what it could stop without them, and said so in this header: *working
# around the deny list from inside a script would defeat the point of having one*. That sentence
# held until five agents were found wedged for three days, each held open by a server, a test runner
# or a poll loop it had started and could not stop — while a finished worktree's database sat on
# :5432 and every local connection reached it instead of this project's.
#
# So the rule is narrowed, not dropped. `down` and `reap` do signal a process, and the deny list
# still stands, because neither verb can express the thing the deny list forbids:
#
#   * neither takes a pid from its caller — no argument names one, so none can be passed in;
#   * a process is a candidate only if its command line names a path inside $ROOT, which is this
#     checkout and the agent worktrees beneath it, and nothing else on this machine;
#   * this script's own ancestry is excluded, so a run can never stop the session driving it.
#
# That is a strictly smaller capability than `Bash(kill:*)` rather than a hole in it: the blanket
# allow reaches any pid on the machine; these verbs reach only what this repository started.
#
#   database   docker-compose up -d / down
#   browsers   launched here, closed over the DevTools protocol (Browser.close)
#   server     JVM and Vite — started as harness background tasks, stopped here by attribution
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$ROOT"

# One project name, wherever this script is invoked from. Compose otherwise derives the project
# from the working directory's basename, so every worktree's copy of this script would drive a
# *different* project against the *same* published host port (TASK-121603). This is a literal, not
# derived from $ROOT or `basename` — deriving it is the defect this ticket fixes.
COMPOSE_PROJECT="poker_duels"

# docker compose (space) is a CLI plugin that is not installed on every machine here; the
# standalone binary is. ADR-0088 §2 step 1 is written with the space and fails as written.
compose() {
    if docker compose version >/dev/null 2>&1; then
        docker compose -p "$COMPOSE_PROJECT" "$@"
    else
        docker-compose -p "$COMPOSE_PROJECT" "$@"
    fi
}

# Asked of compose, never written down: compose names a container after the project, which is now
# the literal $COMPOSE_PROJECT above, so every checkout on this machine resolves the same running
# database instead of starting its own. `|| true`: `compose ps` failing for any reason but "nothing
# is up" would otherwise kill this script under `set -e` mid-`status`, whose job is to report
# `down`, not die.
db_container() { compose ps -q postgres 2>/dev/null || true; }

HEALTH="http://localhost:8080/health"
WEB="http://localhost:5173/"

die() { echo "stack: $*" >&2; exit 1; }

# --- stopping, and the attribution rule that keeps it narrow -------------------------------------

# The pids between this one and init. Excluded from every candidate set below, so a `down` invoked
# by the session cannot stop the session: this shell, its parent, and the harness above it.
ancestors() {
    local p="$$" out=""
    while [ -n "$p" ] && [ "$p" != "0" ] && [ "$p" != "1" ]; do
        out="$out $p"
        p="$(ps -o ppid= -p "$p" 2>/dev/null | tr -d ' ')"
    done
    printf '%s' "$out"
}

# Every pid whose command line names a path inside $ROOT — this checkout, and therefore every
# `.claude/worktrees/agent-*` beneath it. `ps -eo` rather than `pgrep` because the match has to be
# on the whole command line: a JVM is identifiable only by its classpath, a vite by its module path.
# The two self-matches are dropped by name, since both name $ROOT only because this script does.
ours() {
    local anc; anc="$(ancestors)"
    ps -eo pid=,command= 2>/dev/null | while read -r pid rest; do
        case " $anc " in *" $pid "*) continue ;; esac
        case "$rest" in *"$ROOT/"*) ;; *) continue ;; esac
        case "$rest" in
            *scripts/qa/stack.sh*) continue ;;
            *'ps -eo pid=,command='*) continue ;;
            # A harness tool-call shell names $ROOT because it `cd`s there, and a sibling call is
            # not this script's ancestor — without this it would be a candidate. Its children are
            # not excluded, so a build running under another call is still in reach: `down` is for
            # a machine between rounds, not one mid-build.
            *snapshot-zsh-*) continue ;;
        esac
        printf '%s\t%s\n' "$pid" "$rest"
    done
}

# Headless Chrome carries no $ROOT path — its profile lives under the session's tmp — so it is
# matched on this script's own launch signature instead: all three flags, exactly as `chrome-up`
# writes them below. A browser started any other way on this machine carries no such set.
ours_chrome() {
    local anc; anc="$(ancestors)"
    ps -eo pid=,command= 2>/dev/null | while read -r pid rest; do
        case " $anc " in *" $pid "*) continue ;; esac
        case "$rest" in *--headless=new*) ;; *) continue ;; esac
        case "$rest" in *--no-first-run*) ;; *) continue ;; esac
        case "$rest" in *--remote-debugging-port*) ;; *) continue ;; esac
        printf '%s\t%s\n' "$pid" "$rest"
    done
}

# TERM, then KILL to whatever is still there five seconds later. A wedged `vitest list` — the shape
# that held two of the five agents — does not reliably answer TERM.
stop_pids() {
    local pids="$1" p alive=""
    [ -n "$pids" ] || return 0
    for p in $pids; do kill "$p" 2>/dev/null || true; done
    for _ in $(seq 1 5); do
        alive=""
        for p in $pids; do kill -0 "$p" 2>/dev/null && alive="$alive $p"; done
        [ -n "$alive" ] || return 0
        sleep 1
    done
    for p in $alive; do kill -9 "$p" 2>/dev/null || true; done
}

case "${1:-}" in

db-up)
    compose up -d >/dev/null 2>&1 || die "compose up failed"
    # `up -d` has already created the container by the time it returns, so one resolution is
    # enough; empty means compose named nothing, and looping against that name would just be a
    # sixty-second way of finding out what `die` can say immediately.
    id="$(db_container)"
    [ -n "$id" ] || die "compose named no postgres container"
    for _ in $(seq 1 60); do
        if docker exec "$id" pg_isready -U poker -d poker_duels >/dev/null 2>&1; then
            echo "db: accepting connections"; exit 0
        fi
        sleep 1
    done
    die "database never accepted connections"
    ;;

db-down)
    compose down >/dev/null 2>&1 || true
    echo "db: down"
    ;;

# The seam docs/test-plan.md interpolates and the self-test asserts on: prints the resolved id and
# nothing else, or dies — a caller composing a shell command around empty stdout deserves a clear
# failure, not `docker exec "" psql ...`.
db-container)
    id="$(db_container)"
    [ -n "$id" ] || die "compose named no postgres container"
    echo "$id"
    ;;

# Prints the duel server's runtime classpath. poker-server carries no `application` plugin, so
# there is no :run task (ADR-0088 §2 step 2 concedes this). The init script is written to a temp
# file rather than the repo so the build is not modified to be testable.
cp)
    init="$(mktemp -t qa-cp-XXXXXX).gradle"
    cat > "$init" <<'GRADLE'
allprojects {
    tasks.register("printRuntimeCp") {
        doLast {
            def ss = project.extensions.findByName("sourceSets")
            if (ss != null) println("CP=" + ss.getByName("main").runtimeClasspath.asPath)
        }
    }
}
GRADLE
    ./gradlew -q :poker-server:classes >/dev/null 2>&1 || die "server did not compile"
    ./gradlew -q -I "$init" :poker-server:printRuntimeCp 2>/dev/null | grep '^CP=' | head -1 | sed 's/^CP=//'
    rm -f "$init" 2>/dev/null || true
    ;;

wait-server)
    for _ in $(seq 1 90); do
        if [ "$(curl -s -m 2 "$HEALTH" 2>/dev/null)" = "OK" ]; then echo "server: OK"; exit 0; fi
        sleep 1
    done
    die "server never answered $HEALTH"
    ;;

wait-web)
    for _ in $(seq 1 90); do
        code=$(curl -s -m 2 -o /dev/null -w '%{http_code}' "$WEB" 2>/dev/null || echo 000)
        if [ "$code" = "200" ]; then echo "web: 200"; exit 0; fi
        sleep 1
    done
    die "dev server never answered $WEB"
    ;;

# chrome-up <port> <profile-dir>
chrome-up)
    port="${2:?port}"; profile="${3:?profile dir}"
    chrome="/Applications/Google Chrome.app/Contents/MacOS/Google Chrome"
    [ -x "$chrome" ] || die "Chrome not found at $chrome"
    mkdir -p "$profile"
    "$chrome" --headless=new --disable-gpu --no-first-run \
        --remote-debugging-port="$port" --user-data-dir="$profile" about:blank \
        >/dev/null 2>&1 &
    for _ in $(seq 1 30); do
        if curl -s -m 2 "http://localhost:$port/json/version" >/dev/null 2>&1; then
            echo "chrome: $port up"; exit 0
        fi
        sleep 1
    done
    die "chrome on $port never answered"
    ;;

# chrome-down <port...> — Browser.close over CDP. No kill, verified working 2026-08-29.
chrome-down)
    shift
    for port in "$@"; do
        node -e '
          const p = process.argv[1];
          (async () => {
            try {
              const t = await (await fetch(`http://localhost:${p}/json/new?url=about:blank`, {method:"PUT"})).json();
              const ws = new WebSocket(t.webSocketDebuggerUrl);
              await new Promise(r => ws.addEventListener("open", r));
              ws.send(JSON.stringify({id:1, method:"Browser.close"}));
              await new Promise(r => setTimeout(r, 1500));
            } catch (e) { /* already gone */ }
          })();
        ' "$port" 2>/dev/null || true
        echo "chrome: $port closed"
    done
    ;;

status)
    printf 'db:     %s\n' "$(docker exec "$(db_container)" pg_isready -U poker -d poker_duels >/dev/null 2>&1 && echo up || echo down)"
    printf 'server: %s\n' "$([ "$(curl -s -m 2 "$HEALTH" 2>/dev/null)" = "OK" ] && echo up || echo down)"
    printf 'web:    %s\n' "$([ "$(curl -s -m 2 -o /dev/null -w '%{http_code}' "$WEB" 2>/dev/null)" = "200" ] && echo up || echo down)"
    ;;

# Names the checkout and commit behind whatever is listening on a port, so a round can tell what it
# is about to drive instead of assuming it (STORY-1216 round 1: a stale worktree, 92 commits behind,
# answered both ports and nothing in the harness could tell). Read-only, no denied verb: `lsof` for
# the pid, `ps` for the command line the pid was launched with. The checkout root is the longest
# prefix of a path found in that command line that contains a `.git` entry — walking up from the
# full path finds the closest, i.e. longest, such prefix first. Always exits 0: this must be
# runnable in CI with no stack up, where both lines read `(nothing listening)`.
served)
    resolve() {
        local port="$1" pid cmdline tok dir root commit
        pid="$(lsof -ti tcp:"$port" -sTCP:LISTEN 2>/dev/null | head -1)" || true
        [ -n "$pid" ] || { echo "(nothing listening)"; return 0; }
        cmdline="$(ps -o command= -p "$pid" 2>/dev/null)" || true
        [ -n "$cmdline" ] || { echo "(unresolved)"; return 0; }
        root=""
        for tok in $(printf '%s' "$cmdline" | tr ' :' '\n\n'); do
            case "$tok" in
                /*) ;;
                *) continue ;;
            esac
            dir="$tok"
            [ -d "$dir" ] || dir="$(dirname "$dir")"
            while [ -n "$dir" ] && [ "$dir" != "/" ]; do
                if [ -e "$dir/.git" ]; then root="$dir"; break 2; fi
                dir="$(dirname "$dir")"
            done
        done
        [ -n "$root" ] || { echo "(unresolved)"; return 0; }
        commit="$(git -C "$root" rev-parse --short HEAD 2>/dev/null)" || true
        [ -n "$commit" ] || { echo "(unresolved)"; return 0; }
        echo "$root $commit"
    }
    echo "web: $(resolve 5173)"
    echo "server: $(resolve 8080)"
    ;;

# Everything a round leaves running, in the order that keeps the report readable: a browser that
# outlives its server spends its last seconds writing failed requests into its profile. Prints one
# line per process stopped, so a round's log says what it tore down rather than that it tried.
down)
    # One snapshot, reported and then acted on. Two calls would let a process appear between the
    # report and the signal, so the log would describe a set that was never the set stopped.
    snap="$( { ours_chrome; ours; } | sort -u )"
    n=0
    while IFS="$(printf '\t')" read -r pid rest; do
        [ -n "$pid" ] || continue
        printf 'stopped %s  %s\n' "$pid" "$(printf '%s' "$rest" | cut -c1-90)"
        n=$((n + 1))
    done <<EOF
$snap
EOF
    stop_pids "$(printf '%s' "$snap" | cut -f1 | tr '\n' ' ')"
    compose down >/dev/null 2>&1 || true
    echo "db: down"
    echo "down: $n process(es) stopped"
    ;;

# The cross-session sweep. `down` clears what this round started; `reap` also clears what *finished
# agents* left behind — the leak that held five of them open for three days — and the postgres
# containers of compose projects named after a worktree, one of which was holding :5432 against this
# project's own database. `--dry-run` reports and stops, which is what a skill should run first.
reap)
    dry=""; verb="reaped"
    if [ "${2:-}" = "--dry-run" ]; then dry=1; verb="would reap"; fi
    # One snapshot, for the same reason `down` takes one.
    snap="$( { ours; ours_chrome; } | sort -u )"
    while IFS="$(printf '\t')" read -r pid rest; do
        [ -n "$pid" ] || continue
        printf '%s pid %s  %s\n' "$verb" "$pid" "$(printf '%s' "$rest" | cut -c1-90)"
    done <<EOF
$snap
EOF
    [ -n "$dry" ] || stop_pids "$(printf '%s' "$snap" | cut -f1 | tr '\n' ' ')"

    # Containers of any compose project that is not this one. `docker rm -f` only: `docker rmi` and
    # every `prune` are denied, and an image is not what leaked — a container holding a port is.
    docker ps -a --format '{{.ID}}	{{.Names}}' 2>/dev/null | while IFS="$(printf '\t')" read -r cid cname; do
        case "$cname" in
            "$COMPOSE_PROJECT"-*) continue ;;
            agent-*) ;;
            *) continue ;;
        esac
        if [ -n "$dry" ]; then
            echo "would reap container $cid  $cname"
        else
            docker rm -f "$cid" >/dev/null 2>&1 && echo "reaped container $cid  $cname"
        fi
    done
    exit 0
    ;;

*)
    cat >&2 <<USAGE
usage: scripts/qa/stack.sh <command>

  db-up | db-down          the database, via docker-compose
  db-container             print postgres's resolved container id
  cp                       print the duel server's runtime classpath
  wait-server | wait-web   block until the server / dev server answers
  chrome-up <port> <dir>   headless Chrome on its own profile
  chrome-down <port...>    close it over CDP (no kill)
  status                   what is up
  served                   the checkout root and commit behind :5173 and :8080
  down                     stop everything this checkout started, then the database
  reap [--dry-run]         also clear what finished agents left, and foreign compose containers

`down` and `reap` take no pid and reach nothing outside \$ROOT — see the header for why that is a
narrower rule than the deny list rather than a way around it.
USAGE
    exit 2
    ;;
esac
