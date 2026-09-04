#!/usr/bin/env bash
# The QA stack, in the pieces that can be started and stopped without a denied verb.
#
# `kill`, `pkill` and `killall` are in settings.json's deny list, and deny beats allow — no local
# override reaches them. So this script owns only what it can also stop:
#
#   database   docker-compose up -d / down
#   browsers   launched here, closed over the DevTools protocol (Browser.close)
#
# The JVM server and the Vite dev server are NOT started here. They are harness background tasks
# owned by the qa-cycle skill and stopped with TaskStop, because stopping them any other way needs
# a verb this repository denies. `cp` and the `wait-*` subcommands are what the skill needs to do
# that. Working around the deny list from inside a script would defeat the point of having one.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$ROOT"

# docker compose (space) is a CLI plugin that is not installed on every machine here; the
# standalone binary is. ADR-0088 §2 step 1 is written with the space and fails as written.
compose() { if docker compose version >/dev/null 2>&1; then docker compose "$@"; else docker-compose "$@"; fi; }

# Asked of compose, never written down: compose names a container after the project, which
# defaults to the checkout directory's basename, so a literal here is right only in the checkout it
# was written in. `|| true`: `compose ps` failing for any reason but "nothing is up" would
# otherwise kill this script under `set -e` mid-`status`, whose job is to report `down`, not die.
db_container() { compose ps -q postgres 2>/dev/null || true; }

HEALTH="http://localhost:8080/health"
WEB="http://localhost:5173/"

die() { echo "stack: $*" >&2; exit 1; }

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

The JVM server and Vite are the skill's background tasks, not this script's — see the header.
USAGE
    exit 2
    ;;
esac
