# Simplification Rule Index

Read this during Step 2 of the simplification process (Identify Simplification Opportunities) to find the rule that matches the pattern in front of you. Then open only that rule file. Each rule has an incorrect and a correct example and a "When NOT to use" section.

Apply categories in priority order. Context discovery and behavior preservation outrank every stylistic rule, and project conventions override all of them.

## 1. Context Discovery (CRITICAL)

- [`ctx-read-claude-md`](rules/ctx-read-claude-md.md) - Always read CLAUDE.md before simplifying
- [`ctx-detect-lint-config`](rules/ctx-detect-lint-config.md) - Check for linting and formatting configs
- [`ctx-follow-existing-patterns`](rules/ctx-follow-existing-patterns.md) - Match existing code style in file and project
- [`ctx-project-over-generic`](rules/ctx-project-over-generic.md) - Project conventions override generic best practices

## 2. Behavior Preservation (CRITICAL)

- [`behave-preserve-outputs`](rules/behave-preserve-outputs.md) - Preserve all return values and outputs
- [`behave-preserve-errors`](rules/behave-preserve-errors.md) - Preserve error messages, types, and handling
- [`behave-preserve-api`](rules/behave-preserve-api.md) - Preserve public function signatures and types
- [`behave-preserve-side-effects`](rules/behave-preserve-side-effects.md) - Preserve side effects (logging, I/O, state changes)
- [`behave-no-semantics-change`](rules/behave-no-semantics-change.md) - Forbid subtle semantic changes
- [`behave-verify-before-commit`](rules/behave-verify-before-commit.md) - Verify behavior preservation before finalizing

## 3. Scope Management (HIGH)

- [`scope-recent-code-only`](rules/scope-recent-code-only.md) - Focus on recently modified code only
- [`scope-minimal-diff`](rules/scope-minimal-diff.md) - Keep changes small and reviewable
- [`scope-no-unrelated-refactors`](rules/scope-no-unrelated-refactors.md) - No unrelated refactors
- [`scope-no-global-rewrites`](rules/scope-no-global-rewrites.md) - Avoid global rewrites and architectural changes
- [`scope-respect-boundaries`](rules/scope-respect-boundaries.md) - Respect module and component boundaries

## 4. Control Flow Simplification (HIGH)

- [`flow-early-return`](rules/flow-early-return.md) - Use early returns to reduce nesting
- [`flow-guard-clauses`](rules/flow-guard-clauses.md) - Use guard clauses for preconditions
- [`flow-no-nested-ternaries`](rules/flow-no-nested-ternaries.md) - Never use nested ternary operators
- [`flow-explicit-over-dense`](rules/flow-explicit-over-dense.md) - Prefer explicit control flow over dense expressions
- [`flow-flatten-nesting`](rules/flow-flatten-nesting.md) - Flatten deep nesting to maximum 2-3 levels
- [`flow-single-responsibility`](rules/flow-single-responsibility.md) - Each code block should do one thing
- [`flow-positive-conditions`](rules/flow-positive-conditions.md) - Prefer positive conditions over double negatives
- [`flow-optional-chaining`](rules/flow-optional-chaining.md) - Use optional chaining and nullish coalescing
- [`flow-boolean-simplification`](rules/flow-boolean-simplification.md) - Simplify boolean expressions

## 5. Naming and Clarity (MEDIUM-HIGH)

- [`name-intention-revealing`](rules/name-intention-revealing.md) - Use intention-revealing names
- [`name-nouns-for-data`](rules/name-nouns-for-data.md) - Use nouns for data, verbs for actions
- [`name-avoid-abbreviations`](rules/name-avoid-abbreviations.md) - Avoid cryptic abbreviations
- [`name-consistent-vocabulary`](rules/name-consistent-vocabulary.md) - Use consistent vocabulary throughout
- [`name-avoid-generic`](rules/name-avoid-generic.md) - Avoid generic names
- [`name-string-interpolation`](rules/name-string-interpolation.md) - Prefer string interpolation over concatenation

## 6. Duplication Reduction (MEDIUM)

- [`dup-rule-of-three`](rules/dup-rule-of-three.md) - Apply the rule of three
- [`dup-no-single-use-helpers`](rules/dup-no-single-use-helpers.md) - Avoid single-use helper functions
- [`dup-extract-for-clarity`](rules/dup-extract-for-clarity.md) - Extract only when it improves clarity
- [`dup-avoid-over-abstraction`](rules/dup-avoid-over-abstraction.md) - Prefer duplication over premature abstraction
- [`dup-data-driven`](rules/dup-data-driven.md) - Use data-driven patterns over repetitive conditionals

## 7. Dead Code Elimination (MEDIUM)

- [`dead-remove-unused`](rules/dead-remove-unused.md) - Delete unused code artifacts
- [`dead-delete-not-comment`](rules/dead-delete-not-comment.md) - Delete code, never comment it out
- [`dead-remove-obvious-comments`](rules/dead-remove-obvious-comments.md) - Remove comments that state the obvious
- [`dead-keep-why-comments`](rules/dead-keep-why-comments.md) - Keep comments that explain why, not what
- [`dead-remove-todo-fixme`](rules/dead-remove-todo-fixme.md) - Remove stale TODO/FIXME comments

## 8. Language Idioms (LOW-MEDIUM)

- [`idiom-ts-strict-types`](rules/idiom-ts-strict-types.md) - Use strict types over any (TypeScript)
- [`idiom-ts-const-assertions`](rules/idiom-ts-const-assertions.md) - Use const assertions and readonly (TypeScript)
- [`idiom-rust-question-mark`](rules/idiom-rust-question-mark.md) - Use ? for error propagation (Rust)
- [`idiom-rust-iterator-chains`](rules/idiom-rust-iterator-chains.md) - Use iterator chains when clearer (Rust)
- [`idiom-python-comprehensions`](rules/idiom-python-comprehensions.md) - Use comprehensions for simple transforms (Python)
- [`idiom-go-error-handling`](rules/idiom-go-error-handling.md) - Handle errors immediately (Go)
- [`idiom-prefer-language-builtins`](rules/idiom-prefer-language-builtins.md) - Prefer language and stdlib builtins

