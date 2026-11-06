# Review-Mode Checklist

When user asks to **review, audit, critique, or rate code** (not write it), walk this checklist. Do not edit code unless asked. Output findings report.

## Contents

- Output format
- Pre-flight: is this a refactor or a rewrite?
- Walk order
  - Section A: naming and functions
  - Section B: comments and formatting
  - Section C: SOLID
  - Section D: DRY, KISS, YAGNI
  - Section E: AI failure modes
- What to do with each finding
- When the review is contested
- What this review does not do

## Output format

Use template exactly.

```
# Code review: <file or scope>

## Summary
<2–3 sentence verdict: ship / needs work / rewrite>
Counts: <N> critical, <M> important, <K> nits  (must equal the findings listed below)

## Critical findings
<must-fix before merge; omit this heading if none>
- `<file>:<line>` — <tag>: <what's wrong> [`<quoted code or behavior>`]. Fix: <concrete change>.
  <continuation line only when the fix is code-sized>

## Important findings
<should fix but not blocking; omit if none>
- ...

## Nits
<style, naming, minor structure; max 3, each with a fix; omit if none>
- ...

## What's good
<0–3 genuine, specific positives; omit on a clean review — do not manufacture praise>

## Coverage
One line per section: the findings it produced, or `clean` (walked it, found nothing). A blank section is an unbacked claim, not a pass — fill it before delivering.
- Section A (naming & functions): <findings, or `clean`>
- Section B (comments & formatting): <findings, or `clean`>
- Section C (SOLID): <findings, or `clean`>
- Section D (DRY/KISS/YAGNI): <findings, or `clean`>
- Section E (AI failure modes): <findings, or `clean`>
```

Severity:
- **Critical** — security, correctness, data loss, swallowed exceptions, hardcoded "success" returns.
- **Important** — design defects costing maintenance: SOLID violations, premature abstractions, parameter explosion, generic naming.
- **Nit** — style, single-letter names outside loops, missing docstring contracts on public APIs.

Every finding needs quoted code or observed behavior plus named fix. Missing either means not a finding: drop it. Report only counted findings. Never estimated quality score, "X% cleaner," or maintainability index: no baseline exists, so number would be invented.

## Pre-flight: is this a refactor or a rewrite?

Classify review before walking sections:

- **Refactor review:** user wants code cleaner, not different. **Observable behavior must not change** — same inputs, outputs, exceptions, side effects. If a suggestion alters behavior, mark it *separate finding* labelled "Behavior change — confirm with author". Do not bundle with refactor recommendations. Refactoring is *"a change made to the internal structure of software... without changing its observable behavior"* (Fowler, *Refactoring*).
- **Code-review for correctness:** user wants bugs found. Behavior changes in scope. Flag Critical if they affect contract.

If unclear which, ask before writing review.

## Walk order

### Section A — naming and functions

Pull [naming-and-functions.md](naming-and-functions.md) if source citations needed.

1. Scan all identifiers. Flag generic ones: `data`, `result`, `item`, `temp`, `value`, `obj`, `info`, `helper`, `manager`, `utils`, `handle_*`, `process_*`, `do_*` without qualifier.
2. Per function: lines ≤20? params ≤4? one thing? one level of abstraction? Flag violations.
3. Flag boolean flag arguments.
4. Flag functions that both return value *and* mutate observable state ambiguously (CQS violation).
5. Flag getter-style or predicate-style functions that mutate.

### Section B — comments and formatting

Pull [comments-and-formatting.md](comments-and-formatting.md) if needed.

1. Flag every comment paraphrasing code below it.
2. Flag commented-out code blocks.
3. Flag step-number, "First...", or "Then..." scaffolding comments.
4. Flag docstrings restating signature with no contract.
5. Flag style inconsistencies with surrounding file (casing, quoting, import order).

### Section C — SOLID

Pull [solid.md](solid.md) if needed.

1. (SRP) Class with methods serving two unrelated stakeholder groups?
2. (OCP) Conditional or switch dispatch on type tag that grew with codebase?
3. (LSP) Subclass with unimplemented/unsupported-operation failure, strengthened preconditions, or weakened postconditions?
4. (ISP) Interface where concrete client uses only subset of methods?
5. (DIP) High-level module importing concrete from low-level module? Abstractions in same package as concrete?

### Section D — DRY, KISS, YAGNI

Pull [dry-kiss-yagni.md](dry-kiss-yagni.md) if needed.

1. (DRY) ≥5-line duplicated blocks. Confirm knowledge duplication before recommending extraction.
2. (DRY-Metz) Wrong abstractions: per-caller branches and special-case flags piling up in shared function.
3. (KISS) Function with cyclomatic >10 or nesting >5? (Estimate from branches and loops; exact metrics not needed.)
4. (YAGNI) Optional parameters never called, config flags with one path, abstractions with one implementation, wrappers around libraries that "make them swappable."

### Section E — AI failure modes (highest leverage)

Pull [ai-failure-modes.md](ai-failure-modes.md) for every check here.

1. Catch-all error handler swallowing without recovery? Critical.
2. Defensive guards for types/values system already excludes — *inside* trust boundary? (Validating external or untrusted input at boundary is not a defensive guard; do not flag it.)
3. Premature abstraction — interface or factory with one implementation?
4. Comment pollution — line-by-line restating, step-number scaffolding, or documentation comments paraphrasing signatures?
5. Logic duplicating an existing helper in same repo?
6. Imports or library methods to verify exist in installed version?
7. Generic naming (cross-check with Section A).
8. Long function mixing concerns (cross-check with Section A).
9. 5+ parameter functions without config object (cross-check with Section A).
10. Inconsistency with surrounding file style (cross-check with Section B).
11. Dead code, unused imports, unreachable branches, half-implementations?
12. **Hardcoded "success" returns, mock fixtures, fake values in production code?** Critical.
13. Code that looks copy-pasted from similar function (off-by-one, wrong null semantic)?
14. Speculative configurability — flags, env vars, optional params without callers?
15. **"Simplification" that deleted boundary validation, or cleanup path (`finally`/`close`/`defer`/context-manager) the contract relied on?** Behavior change, not cleanup. Critical.

## What to do with each finding

Finding must name its fix: code change OR specific structural action. "Nameable" is the bar, not "codeable."

- ❌ "This error handling could perhaps be more specific." (no named replacement — drop it)
- ✅ "`L42 except Exception` swallows the DB error → catch `OperationalError`, let the rest propagate."
- ✅ "`L88–140 processOrder` mixes validation, pricing, persistence → extract `validate()` and `price()`."

Per finding: quote offending code (file + line), name principle or AI failure mode in `references/`, give fix (code if small, else named structural action), assign severity (Critical / Important / Nit).

## When the review is contested

If user pushes back on finding, cite source from relevant `references/` file. If user has context-specific reason to override, record inline comment naming **the principle, the reason, and a revisit trigger**. Example: `// clean-code exception: 4-arg ceiling — config DTO, all fields required at construction; revisit when an optional field appears.` (prefix illustrative, not required tag). On later pass, well-formed marker downgrades finding to *Documented exception*. Do not re-flag it. Marker with **no revisit trigger is itself a finding**: exception with no exit is deferred debt. Name principle, not rule number.

## What this review does not do

- Run linters or formatters.
- Execute code or run tests. Add finding instead: *"No tests for the new `charge` path — recommend adding."*
- Enforce language-specific style (Black, Prettier, PHPCS). Defer to project style tooling unless user explicitly asks.
