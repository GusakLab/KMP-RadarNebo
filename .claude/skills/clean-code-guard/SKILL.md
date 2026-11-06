---
name: "clean-code-guard"
description: "Guard pass after writing or editing production code: catches LLM failure modes (swallowed errors, hallucinated APIs, speculative abstractions, fake success returns) plus Clean Code, SOLID, DRY, KISS, and YAGNI violations before delivery. Also has a review mode for auditing code quality of specific files."
risk: "critical"
source: "community"
source_repo: "amElnagdy/guard-skills"
source_type: "community"
date_added: 2026-07-13
author: "community"
tags: []
tools: []
---


# clean-code-guard

Guard pass after first implementation pass, scoped to lines you added or changed in diff about to ship. Do not use it to rewrite untouched code or reformat later, unrelated edits; those follow repository conventions and minimal-diff posture (andrej-karpathy).

## When to Use

Use reactively after agent writes, edits or refactors production code. Re-run before delivery after each edit.

**Do not use** for conceptual questions, CI or git workflow, prose, data analysis or test-running tasks. For full merge review of PR, use `code-review-and-quality`; this skill only covers code quality.

## Compatibility

Portable instruction skill. Needs no MCP server, network access, API key, shell command, local executable or bundled script. Works in any runtime supporting `SKILL.md` plus directly linked [references/](references/) files.

Does not replace project linters, formatters, type checkers or test runners. Use project's own tools for mechanical verification; use this skill for judgement layer around code quality and review.

## How to use this skill

Three modes. Pick by user request.

**Guard-pass mode** (recommended): after code is generated, edited, refactored or fixed, check diff or target files against *Always-applied imperatives* below. Fix violations before presenting, committing or merging.

**Live mode** (explicit): user invokes this skill before writing code (e.g. risky edit). Apply same imperatives while writing, then run *Self-check before delivery* checklist. Fix any violated rule before showing user.

**Review mode** (user asks to review, audit, critique or rate code): walk [references/review-checklist.md](references/review-checklist.md) against target file(s), produce structured findings report. Do not edit code in review mode unless asked. For "review this PR" or "should I merge this?", use code-review instead.

In all modes, rule bodies live in [references/](references/). Read relevant reference file when:
- You don't fully remember a rule's reasoning.
- User pushes back on a rule and you need source citation.
- Code under review touches specific principle (e.g., subclassing → [references/solid.md](references/solid.md); deduplication → [references/dry-kiss-yagni.md](references/dry-kiss-yagni.md)).

Reference files:
- [references/naming-and-functions.md](references/naming-and-functions.md) — names, function size, parameters, command/query separation.
- [references/comments-and-formatting.md](references/comments-and-formatting.md) — when to comment, when to delete, matching neighbor style.
- [references/solid.md](references/solid.md) — SRP, OCP, LSP, ISP, DIP with modern phrasings and detection smells.
- [references/dry-kiss-yagni.md](references/dry-kiss-yagni.md) — knowledge vs code duplication, Sandi Metz's re-inline rule, McCabe complexity, Fowler's YAGNI cost categories.
- [references/ai-failure-modes.md](references/ai-failure-modes.md) — 15 systematic ways LLMs produce bad code. **AI agent reading this skill: read this file first.**
- [references/review-checklist.md](references/review-checklist.md) — structured walk-through for review mode.
- [references/law-of-demeter.md](references/law-of-demeter.md) — reaching through object chains; when it is and is not a coupling problem.
- [references/fail-fast.md](references/fail-fast.md) — where to detect and report invalid state; read with imperative 16.
- [references/sources.md](references/sources.md) — central bibliography for source URLs. Read only to verify or cite external source.

## Success criteria

Working when code-writing tasks avoid listed failure modes, code-review tasks produce prioritized findings with concrete evidence, and refactors preserve behavior unless user explicitly asks for behavior change.

## Always-applied imperatives

Apply to code you add or change in current diff. Imperatives 1-3 and 13 are strong heuristics with numeric targets: when surrounding code or language idiom differs (e.g. conventional `result`/`value` name in short scope, or function clearer kept whole), match neighbors and note trade-off instead of forcing churn. Rest are imperative.

### Functions and names

1. **Names reveal intent.** Never use `data`, `data2`, `result`, `result_final`, `item`, `temp`, `value`, `obj`, `info`, `helper`, `manager`, `utils`, or `handle_*`/`process_*`/`do_*` without qualifier. Name must answer *why it exists and what it does*. (Clean Code Ch. 2)
2. **Functions stay small.** Target ≤20 lines, one level of abstraction, one thing. If you can extract function with name that doesn't restate body, parent did more than one thing. (Clean Code Ch. 3)
3. **Four arguments is the hard ceiling.** At five, stop and add request/config object (record, struct, DTO or equivalent). Never use boolean flag arguments; split into two functions.
4. **No output arguments.** Function returns value (query) or has side effect (command). Never both. Command names use verbs; query names use nouns or getter-style names. (CQS)

### Comments and structure

5. **Comments explain *why*, never *what*.** Delete comments that paraphrase line below. Delete step-number scaffolding comments. Delete commented-out code; version control exists. (Clean Code Ch. 4)
6. **Match the file's existing style.** Before writing, read edited file and at least one neighbor. Mirror casing, import order, error handling, logging and HTTP/DB client choices. Do not introduce second pattern.

### SOLID

7. **One actor per module.** Class answers to one stakeholder group (Accounting, Auth, Reporting). Two unrelated subsystems reach into same class? Split it. (SRP, Uncle Bob 2014)
8. **Extension via new code, not edits.** New variant needs another type-tag branch in existing function? First refactor to registry, strategy or polymorphic dispatch. (OCP)
9. **No subclass refuses its parent's contract.** Never override method to signal "not implemented" or "unsupported operation." Never strengthen preconditions or weaken postconditions in override. Needing that means inheritance is wrong. (LSP)
10. **Abstractions live with the client, not the implementation.** New interface, protocol or abstract contract goes in consuming package, not next to concrete class. (DIP)

### DRY, KISS, YAGNI

11. **Delete duplicated *knowledge*, not duplicated *text*.** Two look-alike functions encoding different rules are not DRY violation. One rule expressed in code + docs + schema is. (Pragmatic Programmer, "DRY")
12. **The wrong abstraction is worse than duplication.** Abstraction grew branches for each caller's special case? Re-inline it into callers, delete dead branches, then re-abstract. (Sandi Metz, "The Wrong Abstraction")
13. **Complexity ceiling: cyclomatic ≤10, nesting depth ≤5.** Refactor before exceeding. (McCabe 1976)
14. **No speculative anything.** No optional parameter, config flag, env var, feature toggle, interface, factory or base class without present-day caller. Adding `enable_*`, `use_*_v2` or `*_mode`? Delete it, ship concrete behavior. (Fowler, "Yagni")

### AI-specific guardrails — the highest-leverage section

15. **Never swallow errors with broad catch-all handling.** Catch only specific error type you can recover from. If you cannot recover, let error propagate. Never return null/none/empty success from catch handler unless function contract documents that behavior. (Karpathy)
16. **Guard the boundary; trust the contract.** At trust boundary (external input, request/API payloads, deserialized or cross-process data, anything from untrusted source), validate even when happy path looks fine. *Inside* boundary, do not add null checks or runtime type checks for values whose declared type or caller contract already excludes that case. Guard test is not "could this theoretically be wrong" but "can untrusted data reach here." (arXiv 2409.19182)
17. **Verify every import and external call.** Before calling library method, confirm it exists in installed version (read package, check lockfile, or import and inspect). Do not generate code from what API "should" look like. (USENIX Security '25)
18. **No hardcoded "success" returns or mock fixtures in production code.** Never return `{"status": "ok", ...}` or canned data from function whose spec says it does real work. Cannot implement? Fail explicitly with language's unimplemented or unsupported-operation mechanism and say so. Never disable, skip or weaken test to make it pass. (Fowler, Claude Code issue #6984)
19. **Re-derive, do not copy from similar.** Tempted to copy function and modify it? Stop. Re-derive from spec. Off-by-one and wrong-null-semantic bugs almost always enter via copy-from-similar. (arXiv 2411.01414)
20. **Enumerate boundary cases before writing them.** For any range, off-by-one, null/empty/one/many, even/odd or unicode/byte boundary, write case list first: scratch notes or test names, not committed comments (imperative 5 still applies). Cover each case in code and tests before moving on.
21. **Strip dead code before delivery.** Run linter or grep pass for unused imports, unused symbols, unreachable branches and "just in case" exports. Remove them. Function nothing calls today does not stay for "someday."
22. **Read before write.** In unfamiliar repo, first read file you'll edit, one neighbor and any project rules file (CLAUDE.md, AGENTS.md, README's "conventions" section). Use project's existing helpers, error types and logging.
23. **No new dependency for what a few lines cover.** Before adding package, check standard library, installed dependencies, and whether a few lines of local code suffice. New dependency is permanent maintenance and supply-chain surface. Add one only when it owns real complexity you should not re-implement (cryptography, parsing, time zones — illustrative, not exhaustive), never to save ten lines. See [references/dry-kiss-yagni.md](references/dry-kiss-yagni.md).

### The floor — never cut these for simplicity

Rule 16 trusts contract *inside* boundary. Items below stay even while stripping speculation (14), defensive guards (16) and dead code (21). Removing one is behavior change, not cleanup: keep it, or flag it and ask.

- **Validation and sanitization at every trust boundary** — external input, request/API payloads, deserialized or cross-process data.
- **Error handling that prevents data loss.**
- **Security measures** — authorization, output escaping, parameterized queries, secret handling.
- **Behavior the user explicitly requested.** Idly mentioned ≠ requested, but do not drop what was asked for.

### Refactoring discipline

24. **Preserve observable behavior when refactoring.** When user asks to clean up, simplify or refactor existing code, do not change contract: same inputs give same outputs, same exceptions, same side effects, same ordering guarantees. Spot bug while refactoring? Flag it separately; ask before changing it. Refactoring is *"a change made to the internal structure of software to make it easier to understand and cheaper to modify without changing its observable behavior"* (Fowler, *Refactoring*). Bug fixes and refactors are two operations; never bundle them in one change.

## Self-check before delivery

Before showing user code you wrote or edited:

1. Walk imperatives 1–24 against your diff. Fix every violation.
2. New functions, count: lines ≤ 20? params ≤ 4? complexity feels ≤ 10? names reveal intent?
3. New comments: explains *why*? If it explains *what*, delete it.
4. New error handling: caught error type specific? Handler does something other than silently return?
5. New abstractions (interface, factory, base class, registry): second concrete user *today*? If no, inline it.
6. Read edited file and at least one neighbor? Style matches?
7. Any hardcoded "ok" return or fixture data? If yes, replace with real implementation or explicit unimplemented/unsupported-operation failure.
8. Refactor: changed observable behavior? If yes, you bundled bug fix; split it out and ask user.

Cannot answer yes to every check? Fix before shipping.

After guard pass, show user it ran (guard-pass and live modes; review mode reports through its own findings format). List each fix as `<file>[:<line>] — <what changed>`, omitting line number if unstable. Close with one line: `clean-code-guard: <N> fixed, <M> flagged for author`, or `clean-code-guard: clean` if nothing triggered. Report only changes you made. Never estimate quality score or percentage: no baseline exists, so number would be invented. This reports the pass; it does not block presenting or committing.

## When the user pushes back on a rule

Point to source name in relevant [references/](references/) file. Use [references/sources.md](references/sources.md) only when URL is needed. Rules are defensible: primary sources (Uncle Bob, Fowler, Hunt & Thomas, McCabe, Metz) plus published 2024–2026 research on LLM code generation. User has context-specific reason to override (e.g., constructor genuinely needs 8 params for config DTO)? Document exception in code comment naming overridden principle, reason and revisit trigger (condition for reconsidering). Exception comment without revisit trigger is itself finding on next pass: tradeoff with no exit is deferred debt.

## Troubleshooting

- Review mode producing style-only feedback: use
  [references/review-checklist.md](references/review-checklist.md), prioritize behavioral bugs, brittleness
  and maintainability risks.
- Rule conflicts with explicit project convention: follow project
  convention. Document exception only when it would otherwise surprise
  future maintainer.
- Skill feels too broad: apply "Do not use" list in When to Use first. Do not add
  runtime-specific rules to this general guard skill.

## What this skill does not do

- Run linters or static analysis. Those are tool-level concerns; this skill covers *what to write* and *what to look for*.
- Enforce language-specific formatter or linter preferences. Defer to project's style tooling.
- Replace tests. Clean code passes tests; tests do not pass without clean code, but clean code without tests is also defect.
