---
name: andrej-karpathy
description: "Working posture for coding agents on any code task: state assumptions and ask when unclear, write the minimum code the request needs, keep every changed line traceable to the request, and define verifiable success criteria before editing. Use as the default behavior for writing or changing code; for code-quality rules use clean-code-guard."
risk: safe
source: community
source_repo: multica-ai/andrej-karpathy-skills
source_type: community
license: MIT
license_source: "https://github.com/multica-ai/andrej-karpathy-skills/blob/main/skills/karpathy-guidelines/SKILL.md"
date_added: '2026-03-06'
author: renat
tags:
- coding-guidelines
- code-review
- llm-coding
- simplicity
tools:
- claude-code
- antigravity
- cursor
- gemini-cli
- codex-cli
---

# Karpathy Guidelines

Behavioral guidelines to cut common LLM coding mistakes, from [Andrej Karpathy's observations](https://x.com/karpathy/status/2015883857489522876) on LLM coding pitfalls.

**Tradeoff:** Biased toward caution over speed. For trivial tasks, use judgment.

## When to Use This Skill

- Writing, reviewing or refactoring code with an LLM.
- Change must stay surgical, without speculative abstractions.
- Assumptions, tradeoffs and verification criteria need to be explicit.
- Code got overcomplicated and needs simplifying.

## 1. Think Before Coding

**Don't assume. Don't hide confusion. Surface tradeoffs.**

Before implementing:
- State assumptions explicitly. If uncertain, ask.
- If multiple interpretations exist, present them. Don't pick silently.
- If simpler approach exists, say so. Push back when warranted.
- If something is unclear, stop. Name what's confusing. Ask.

## 2. Simplicity First

**Minimum code that solves the problem. Nothing speculative.**

- No features beyond what was asked.
- No abstractions for single-use code.
- No unrequested "flexibility" or "configurability".
- No error handling for impossible scenarios.
- If 200 lines could be 50, rewrite.

Ask yourself: "Would a senior engineer say this is overcomplicated?" If yes, simplify.

## 3. Surgical Changes

**Touch only what you must. Clean up only your own mess.**

When editing existing code:
- Don't "improve" adjacent code, comments or formatting.
- Don't refactor what isn't broken.
- Match existing style, even if you'd do it differently.
- Unrelated dead code: mention it, don't delete it.

When your changes create orphans:
- Remove imports/variables/functions that YOUR changes made unused.
- Don't remove pre-existing dead code unless asked.

Test: every changed line traces directly to user's request.

## 4. Goal-Driven Execution

**Define success criteria. Loop until verified.**

Turn tasks into verifiable goals:
- "Add validation" -> "Write tests for invalid inputs, then make them pass"
- "Fix the bug" -> "Write a test that reproduces it, then make it pass"
- "Refactor X" -> "Ensure tests pass before and after"

For multi-step tasks, state brief plan:

```text
1. [Step] -> verify: [check]
2. [Step] -> verify: [check]
3. [Step] -> verify: [check]
```

Strong success criteria let you loop independently. Weak criteria such as "make it work" need constant clarification.

## Examples

```text
User request: "Add validation to this form."

Better response:
1. Assumption: validation should run before submit and show inline errors.
2. Plan: add a small validator, add tests for invalid inputs, then verify existing submit behavior.
3. Scope: only touch the form component and its test file.
```

```text
User request: "Refactor this service."

Better response:
1. Ask what behavior must remain unchanged.
2. Identify a concrete smell, such as duplicated parsing logic.
3. Make the smallest refactor and run the existing service tests.
```

## Limitations

- Behavioral guardrails, not replacement for project-specific architecture or style rules.
- For emergency fixes, prioritize smallest verified correction over extensive planning.
- Exploratory prototypes can relax some caution. Still keep assumptions and verification explicit.
