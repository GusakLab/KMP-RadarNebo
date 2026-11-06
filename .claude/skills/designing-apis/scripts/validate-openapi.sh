#!/usr/bin/env bash
# Lint OpenAPI/AsyncAPI specifications with Spectral.
# Usage: validate-openapi.sh <spec-file> [more spec files...]
# Requires: spectral (npm install -g @stoplight/spectral-cli)
# Spectral needs a ruleset; if the project has no .spectral.yaml, create one with:
#   echo 'extends: ["spectral:oas", "spectral:asyncapi"]' > .spectral.yaml

set -euo pipefail

if [ "$#" -eq 0 ]; then
    echo "Usage: $0 <spec-file> [more spec files...]" >&2
    exit 2
fi

if ! command -v spectral >/dev/null 2>&1; then
    echo "Error: spectral not found. Install: npm install -g @stoplight/spectral-cli" >&2
    exit 1
fi

status=0
for spec in "$@"; do
    echo "Linting: $spec"
    spectral lint "$spec" || status=1
done
exit "$status"
