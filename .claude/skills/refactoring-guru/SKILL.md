---
name: refactoring-guru
description: "Diagnose a named code smell and apply the smallest behavior-preserving refactoring, with verification before and after and an explicit stop condition. Use for structural refactors (extract, move, consolidate, change ownership), not for feature or bug work; for readability cleanup of recent changes use code-simplification."
license: MIT
---

# Refactoring.Guru Skill

Refactoring is a small, smell-driven, behavior-preserving change with verification and a stop condition. It is not general cleanup, and it is not a chance to apply patterns.

Before you make decisions, read and apply [refactoring-guru.mini.md](refactoring-guru.mini.md), which contains the decision rules, trigger rules, and final checklist. Use [refactoring-guru.md](refactoring-guru.md) as the deeper reference when you need the full smell catalog (bloaters, OO abusers, change preventers, dispensables, couplers) or a tradeoff the mini rules don't settle. For the mechanics of a specific technique, see [references/catalog/README.md](references/catalog/README.md).

## Workflow

### 1. Diagnose the smell

Name the smell and record its symptom, the maintenance cost it causes now, its scope, and the cleaner end state you expect. If you can't name a smell with a present cost, don't refactor.

### 2. Establish the behavior boundary and proof

- Decide what must not change: public interfaces, return values, error types and messages, side effects, ordering, and serialized or reflected names. Treat every bug you discover as a separate change.
- Pick the proof you will run before and after the change. Use existing tests when they cover the code you are changing.
- **When no tests cover the code**, write characterization tests first. They record what the code *currently* does, including odd behavior, not what it should do. For output-heavy code, use a golden-master snapshot: capture outputs for a representative set of inputs, then compare after each step.
- Run the proof once before editing to confirm it passes on the current code.

### 3. Pick the smallest technique

Use the trigger rules in the mini file to map the smell to a treatment, then open the matching entry in [references/catalog/README.md](references/catalog/README.md) for the mechanics. Escalate (method object, new class, polymorphism, pattern) only when the smaller treatment is blocked.

### 4. Apply in small steps

- Move one ownership boundary at a time (one method, field, or responsibility per step).
- Keep every intermediate state buildable and testable. Never leave the tree broken between steps.
- Don't add dependencies or configuration unless correctness requires them.
- When signatures or other externally reachable APIs change, keep a transition path (delegating old entry point, deprecation) unless breaking them is in scope.

### 5. Verify

Run the same proof after each risky step and at the end: moves, signature changes, state-flow changes, and algorithm substitutions. If a test needs to be modified to pass, behavior changed. Stop and separate the change.

### 6. Stop

Stop when the named smell is gone or materially reduced and the requested structure is reached. Record any other smells you noticed as follow-ups instead of expanding the change.

## Report

Name the smell, the technique(s) applied, the proof that was run (with its result before and after), any behavior changes split out, and any smells left untreated on purpose.

## Related

- `code-simplification`: readability cleanup of recent changes (naming, nesting, dead code) without structural moves.
- `code-refactoring-tech-debt`: choosing *which* debt to refactor across a codebase.
- `gof-patterns`: judging whether an existing pattern is over-engineered before you remove or introduce one.
