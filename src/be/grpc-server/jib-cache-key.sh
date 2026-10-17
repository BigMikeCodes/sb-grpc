#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
POM_PATH="$SCRIPT_DIR/pom.xml"

mapfile -t FROM_IMAGES < <(grep -Po '(?<=<from.image>).*?(?=</from.image>)' "$POM_PATH" || true)

if [[ ${#FROM_IMAGES[@]} -ne 1 || -z "${FROM_IMAGES[0]-}" ]]; then
    printf 'Expected exactly one non-empty <from.image> value in %s\n' "$POM_PATH" >&2
    exit 1
fi

printf '%s\n' "${FROM_IMAGES[0]}"
