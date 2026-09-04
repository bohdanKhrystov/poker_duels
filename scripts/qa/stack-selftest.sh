#!/usr/bin/env bash
# Hermetic self-test for stack.sh's derived container name (TASK-131010). Puts a stub
# docker/docker-compose/curl first on PATH, runs stack.sh as a CHILD process — never sourced, since
# sourcing would share this shell's `set -e` and run its `case` in this process — and asserts on
# what the stub recorded. No daemon is contacted, so this is a real red/green with Colima stopped
# and in CI. Convention: scripts/qa/delay.mjs's selftest()/selftestCut() — each assertion prints
# what it proves on success and, on failure, its label and the recorded call log.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
STACK="$ROOT/scripts/qa/stack.sh"
STUBDIR="$(mktemp -d -t stack-selftest-XXXXXX)"
trap 'rm -rf "$STUBDIR"' EXIT
LOG1="$STUBDIR/log1"; LOG2="$STUBDIR/log2"; LOG3="$STUBDIR/log3"; LOG4="$STUBDIR/log4"
LOG5A="$STUBDIR/log5a"; LOG5B="$STUBDIR/log5b"

# One stub file serves both docker and docker-compose, told apart by $0's basename —
# docker-compose is a symlink to it, the multi-call-binary trick. It answers `compose version`
# from $STUB_PLUGIN so both compose() branches can be driven, prints $STUB_ID for `ps`, and exits 0
# for everything else including `exec` — the point is what was called, never whether a database
# answered.
cat > "$STUBDIR/docker" <<'STUBEOF'
#!/usr/bin/env bash
name="$(basename "$0")"
echo "$name $*" >> "$STUB_LOG"
if [ "$name" = "docker" ] && [ "${1:-}" = "compose" ]; then shift; fi
case "${1:-}" in
    version)
        if [ "${STUB_PLUGIN:-0}" = "1" ]; then exit 0; else exit 1; fi
        ;;
    ps)
        if [ -n "${STUB_ID:-}" ]; then printf '%s\n' "$STUB_ID"; fi
        exit 0
        ;;
    *)
        exit 0
        ;;
esac
STUBEOF
chmod +x "$STUBDIR/docker"
ln -s docker "$STUBDIR/docker-compose"
printf '#!/usr/bin/env bash\nexit 1\n' > "$STUBDIR/curl"
chmod +x "$STUBDIR/curl"

# Prints the label and, for every log given, its recorded calls, then exits 1 — the shape the
# negative control's reference output is in.
fail() {
    local label="$1"
    shift
    echo "SELFTEST FAIL: $label"
    echo "--- calls ---"
    cat "$@"
    exit 1
}

# run <log> <id> <plugin 0|1> <stack.sh args...> — a CHILD invocation (never sourced), the stub
# directory first on PATH, sets RUN_OUT / RUN_RC / RUN_S (elapsed whole seconds) for the caller.
run() {
    local log="$1" id="$2" plugin="$3"
    shift 3
    : > "$log"
    SECONDS=0
    set +e
    RUN_OUT="$(PATH="$STUBDIR:$PATH" STUB_LOG="$log" STUB_ID="$id" STUB_PLUGIN="$plugin" bash "$STACK" "$@" 2>&1)"
    RUN_RC=$?
    set -e
    RUN_S=$SECONDS
}

run "$LOG1" pd-selftest-alpha-1 1 db-up
[ "$RUN_RC" -eq 0 ] && grep -q 'docker exec pd-selftest-alpha-1 pg_isready' "$LOG1" ||
    fail "db-up did not probe pd-selftest-alpha-1" "$LOG1"
echo "A1 db-up probes the id compose named (plugin branch): ok"

run "$LOG2" pd-selftest-beta-2 0 db-up
[ "$RUN_RC" -eq 0 ] && grep -q 'docker exec pd-selftest-beta-2 pg_isready' "$LOG2" ||
    fail "db-up did not probe pd-selftest-beta-2 on the standalone branch" "$LOG2"
echo "A2 db-up probes a different id compose named (standalone branch): ok"

run "$LOG3" pd-selftest-gamma-3 1 status
{ [ "$RUN_RC" -eq 0 ] && grep -q 'docker exec pd-selftest-gamma-3 pg_isready' "$LOG3" &&
    grep -Eq 'db:[[:space:]]+up' <<< "$RUN_OUT"; } ||
    fail "status did not probe pd-selftest-gamma-3" "$LOG3"
echo "A3 status probes the id compose named: ok"

run "$LOG4" pd-selftest-delta-4 1 db-container
[ "$RUN_RC" -eq 0 ] && [ "$RUN_OUT" = "pd-selftest-delta-4" ] ||
    fail "db-container did not print exactly pd-selftest-delta-4" "$LOG4"
echo "A4 db-container prints exactly the id and nothing else: ok"

run "$LOG5A" "" 1 db-up
up_rc=$RUN_RC; up_s=$RUN_S
run "$LOG5B" "" 1 db-container
[ "$up_rc" -ne 0 ] && [ "$up_s" -lt 10 ] && [ "$RUN_RC" -ne 0 ] && [ "$RUN_S" -lt 10 ] ||
    fail "db-up or db-container did not fail fast when compose named no container" "$LOG5A" "$LOG5B"
echo "A5 db-up and db-container fail in under 10s when compose names no container: ok"

if grep -q 'poker_duels-postgres-1' "$LOG1" "$LOG2" "$LOG3" "$LOG4" "$LOG5A" "$LOG5B"; then
    fail "a recorded call named the retired literal" "$LOG1" "$LOG2" "$LOG3" "$LOG4" "$LOG5A" "$LOG5B"
fi
echo "A6 no recorded call names the retired literal: ok"
