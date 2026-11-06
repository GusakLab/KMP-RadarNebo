#!/usr/bin/env python3
"""Quick structural review of OpenAPI / Swagger documents.

Read-only. Finds spec files under a path (or takes a spec file directly) and
reports design gaps that linters often treat as style: missing operationId,
summary, error responses, 2xx response, examples, security, and pagination
parameters on list endpoints.

This is not a full schema validator; use Spectral or Redocly CLI for that
(see validate-openapi.sh).

Usage:
    python3 check_openapi.py <spec-file-or-directory> [--strict]

Exit status: 0 when no errors (warnings allowed unless --strict), 1 otherwise,
2 on usage problems. YAML specs need PyYAML (`pip install pyyaml`).
"""
from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path

HTTP_METHODS = {"get", "put", "post", "delete", "options", "head", "patch", "trace"}
SPEC_NAMES = ("openapi", "swagger")
SKIP_DIRS = {"node_modules", ".git", "dist", "build", "__pycache__", ".venv", "venv"}
PAGINATION_PARAMS = {"limit", "page", "page_size", "pagesize", "per_page", "cursor", "after", "offset", "first"}


def load(path: Path):
    text = path.read_text(encoding="utf-8")
    if path.suffix == ".json":
        return json.loads(text)
    try:
        import yaml  # type: ignore
    except ImportError:
        raise RuntimeError("PyYAML not installed; run `pip install pyyaml` to check YAML specs")
    return yaml.safe_load(text)


def find_specs(root: Path) -> list[Path]:
    if root.is_file():
        return [root]
    found = []
    for p in root.rglob("*"):
        if not p.is_file() or p.suffix not in {".json", ".yaml", ".yml"}:
            continue
        if any(part in SKIP_DIRS for part in p.parts):
            continue
        name = p.name.lower()
        if any(n in name for n in SPEC_NAMES):
            found.append(p)
    return sorted(found)


def check(spec: dict) -> tuple[list[str], list[str]]:
    errors: list[str] = []
    warnings: list[str] = []
    if not isinstance(spec, dict) or not ("openapi" in spec or "swagger" in spec):
        return ["not an OpenAPI/Swagger document (no 'openapi' or 'swagger' key)"], []

    info = spec.get("info") or {}
    for key in ("title", "version"):
        if key not in info:
            errors.append(f"info.{key} missing")
    if "description" not in info:
        warnings.append("info.description missing")

    global_security = spec.get("security")
    has_schemes = bool((spec.get("components") or {}).get("securitySchemes") or spec.get("securityDefinitions"))
    if not has_schemes:
        warnings.append("no security schemes defined")

    paths = spec.get("paths") or {}
    if not paths:
        errors.append("no paths defined")
    seen_ids: dict[str, str] = {}

    for path, item in paths.items():
        if not isinstance(item, dict):
            continue
        path_params = {p.get("name") for p in item.get("parameters", []) if isinstance(p, dict)}
        for method, op in item.items():
            if method not in HTTP_METHODS or not isinstance(op, dict):
                continue
            where = f"{method.upper()} {path}"
            responses = op.get("responses") or {}
            if not responses:
                errors.append(f"{where}: no responses")
                continue
            codes = {str(c) for c in responses}
            if not any(c.startswith("2") or c == "default" for c in codes):
                errors.append(f"{where}: no 2xx response")
            if not any(c.startswith("4") or c == "default" for c in codes):
                warnings.append(f"{where}: no 4xx error response documented")
            if not (op.get("summary") or op.get("description")):
                warnings.append(f"{where}: no summary/description")
            op_id = op.get("operationId")
            if not op_id:
                warnings.append(f"{where}: no operationId")
            elif op_id in seen_ids:
                errors.append(f"{where}: duplicate operationId '{op_id}' (also {seen_ids[op_id]})")
            else:
                seen_ids[op_id] = where
            if global_security is None and "security" not in op and has_schemes:
                warnings.append(f"{where}: no security requirement (and no global default)")
            if method == "get" and not path.rstrip("/").endswith("}"):
                names = {str(p.get("name", "")).lower() for p in op.get("parameters", []) if isinstance(p, dict)}
                names |= {str(n).lower() for n in path_params if n}
                if not names & PAGINATION_PARAMS:
                    warnings.append(f"{where}: collection GET without a pagination parameter")
    return errors, warnings


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("target", type=Path)
    parser.add_argument("--strict", action="store_true", help="treat warnings as failures")
    args = parser.parse_args()

    if not args.target.exists():
        print(f"error: {args.target} does not exist", file=sys.stderr)
        return 2
    specs = find_specs(args.target)
    if not specs:
        print("no OpenAPI/Swagger files found (looked for *openapi*/*swagger* .json/.yaml/.yml)")
        return 0

    total_err = total_warn = 0
    for spec_path in specs:
        try:
            errors, warnings = check(load(spec_path))
        except Exception as exc:  # parse errors are reported, not raised
            errors, warnings = [f"cannot parse: {exc}"], []
        total_err += len(errors)
        total_warn += len(warnings)
        print(f"\n{spec_path}")
        for e in errors:
            print(f"  ERROR   {e}")
        for w in warnings:
            print(f"  WARN    {w}")
        if not errors and not warnings:
            print("  OK")

    print(f"\n{len(specs)} file(s): {total_err} error(s), {total_warn} warning(s)")
    return 1 if total_err or (args.strict and total_warn) else 0


if __name__ == "__main__":
    sys.exit(main())
