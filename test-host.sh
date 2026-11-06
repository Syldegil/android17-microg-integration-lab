#!/bin/sh
set -eu
root=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
work=${1:?Supply a fresh test directory}
mkdir "$work"
javac -d "$work" "$root/scripts/StatementNetworkPolicy.java" "$root/tests/StatementNetworkPolicyFixture.java"
java -cp "$work" StatementNetworkPolicyFixture
PYTHONDONTWRITEBYTECODE=1 python3 -m unittest discover -s "$root/tests" -v
python3 "$root/scripts/render_policy.py" --output "$work/MicrogSignaturePolicy.smali"
