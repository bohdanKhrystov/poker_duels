#!/usr/bin/env bash
# Proves that :poker-server:verifyProtocolTypes actually fails on drift, rather than
# always passing. A verify task that never fails is worse than none, because everyone
# trusts it — so this script demonstrates the failure instead of asserting it in prose.
#
# Run from the repository root.
set -euo pipefail

protocol_file="web-client/src/protocol/protocol.gen.ts"

# Restore the committed file on any exit — success, failure, or interruption — so this
# script never leaves the repository dirty.
trap 'git checkout -- "${protocol_file}"' EXIT

echo "== Step 1: baseline run against the committed, untouched file =="
if ! ./gradlew :poker-server:verifyProtocolTypes; then
    echo "FAIL: verifyProtocolTypes failed before anything was perturbed." >&2
    echo "The committed file and the emitter already disagree; that is a separate bug." >&2
    exit 1
fi
echo "Baseline passed: the committed file matches freshly generated output."
echo

echo "== Step 2: perturb the committed file =="
echo "// drift-check-perturbation" >> "${protocol_file}"
echo "Appended a line to ${protocol_file}."
echo

echo "== Step 3: re-run, expecting a non-zero exit =="
if ./gradlew :poker-server:verifyProtocolTypes; then
    echo "FAIL: verifyProtocolTypes succeeded on a perturbed file — it does not detect drift." >&2
    exit 1
fi
echo "verifyProtocolTypes correctly failed: the perturbed run was rejected as drift."
echo

echo "== Step 4: restore =="
echo "The EXIT trap will now restore ${protocol_file} via 'git checkout --'."
