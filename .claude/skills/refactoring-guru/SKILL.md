---
name: refactoring-guru
description: "Diagnose a named code smell and apply the smallest behavior-preserving refactoring, with verification before and after and an explicit stop condition. Use for structural refactors (extract, move, consolidate, change ownership), not for feature or bug work; for readability cleanup of recent changes use code-simplification."
license: MIT
---

# Refactoring.Guru Skill

Refactoring: small, smell-driven, behavior-preserving change, verified, with stop condition. Not general cleanup. Not an excuse to apply patterns.

Before deciding anything, read and apply [refactoring-guru.mini.md](refactoring-guru.mini.md): decision rules, trigger rules, final checklist. Deeper reference for full smell catalog (bloaters, OO abusers, change preventers, dispensables, couplers) or tradeoffs mini rules don't settle: [refactoring-guru.md](refactoring-guru.md).

## Workflow

### 1. Diagnose the smell

Name smell. Record symptom, current maintenance cost, scope, expected cleaner end state. Can't name a smell with present cost: don't refactor.

### 2. Establish the behavior boundary and proof

- Decide what must not change: public interfaces, return values, error types and messages, side effects, ordering, serialized or reflected names. Treat each found bug as separate change.
- Pick proof to run before and after. Use existing tests if they cover changed code.
- **When no tests cover the code**, write characterization tests first. They record *current* behavior, oddities included, not intended behavior. Output-heavy code: golden-master snapshot. Capture outputs for representative set of inputs, compare after each step.
- Run proof once before editing; confirm it passes on current code.

### 3. Pick the smallest technique

Map smell to treatment via mini file trigger rules. Then open matching entry in [references/catalog/README.md](references/catalog/README.md) for mechanics. Escalate (method object, new class, polymorphism, pattern) only when smaller treatment is blocked.

### 4. Apply in small steps

- Move one ownership boundary per step (one method, field, or responsibility).
- Keep every intermediate state buildable and testable. Never leave tree broken between steps.
- No new dependencies or configuration unless correctness requires them.
- Changing signatures or other externally reachable APIs: keep transition path (delegating old entry point, deprecation) unless breaking them is in scope.

### 5. Verify

Rerun same proof after each risky step (moves, signature changes, state-flow changes, algorithm substitutions) and at end. If a test needs editing to pass, behavior changed. Stop and split out that change.

### 6. Stop

Stop when named smell is gone or materially reduced and requested structure reached. Log other smells as follow-ups; don't expand the change.

## Report

Smell, technique(s) applied, proof run (results before and after), split-out behavior changes, smells deliberately left untreated.

## Related

- `code-simplification`: readability cleanup of recent changes (naming, nesting, dead code) without structural moves.
- `code-refactoring-tech-debt`: choosing *which* debt to refactor across codebase.
- `gof-patterns`: judging if existing pattern is over-engineered before removing or adding one.
