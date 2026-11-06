# OBEY Code Complete by Steve McConnell

## When to use

Use for implementing, changing, reviewing, debugging, refactoring, or tuning production code where construction discipline must cut defects and keep code easy to inspect.

## Primary bias to correct

Construction quality is not accidental. Typing code, making it work once, or clever idioms do not complete construction. Pick option that lowers defect risk and eases reasoning.

## Decision rules

- Before large construction work, confirm requirements, architecture, major risks, conventions, language constraints, error policy, data representation, reuse, integration, and testing approach are clear enough.
- If upstream uncertainty remains, build small validated slice, not speculative code. Make expensive-to-reverse decisions deliberately.
- Optimize for human readers first: clarity, locality, explicitness, visible control flow, consistent conventions, practical correctness over cleverness, minimal keystrokes, or fashion.
- For complex routines, sketch precise pseudocode or intent comments at consistent abstraction level, then convert to code. Keep only comments still explaining intent, constraints, contracts, or rationale.
- Routines: cohesive, precisely named, small interface, hard to misuse. Separate setup, validation, computation, side effects when conceptually different.
- Make variable and data meaning explicit: purpose-revealing names, small scope, deliberate initialization, named constants, stronger types, visible units or sentinel meanings.
- Pick types that make invalid or ambiguous values harder to represent. Booleans only for true binary meanings, enumerations for closed sets, records/maps/tables only when shape communicates meaning.
- Keep control flow simple enough to verify: shallow nesting, named predicates for complex conditions, clear normal path, clear loop initialization/termination/update, no side-effect-dependent expressions or clever one-liners.
- Use table-driven or data-driven logic for stable explicit mappings only when table is clearer, obvious, synchronized with rules, and validated. Do not hide complex behavior in inscrutable encodings.
- Validate input at trust boundaries. Use assertions, invariant checks, simple contracts for programmer assumptions; validation or domain errors for expected external or business failures.
- Handle errors at right abstraction, preserve diagnostic context, standardize similar failures, keep normal path readable. Never silently continue from corrupted or impossible state.
- Classes and modules: focused, cohesive, bounded by clear contracts. Hide representation and internal bookkeeping. Avoid mixing persistence, formatting, business, integration concerns.
- Treat rising complexity as defect risk: split tangled routines or modules, remove duplication that multiplies maintenance, shrink what maintainer holds in working memory.
- Build in small, verifiable increments. Integrate often enough to expose conflicts, keep partial work from rotting, review and improve code during construction.
- Scale reviews, inspections, pair work, tests, static checks, regression tests to defect risk. Debug by reproducing, isolating, explaining, fixing, verifying root causes, not by guessing.
- Refactor when structure hides intent, duplicates knowledge, or raises defect probability. Keep refactoring separate from behavior change when that aids review.
- Tune performance only when requirements and evidence justify it. Measure before and after; keep clarity unless explicit measured tradeoff warrants cost.
- Use tools, scripts, debuggers, profilers, editors, build automation to cut error-prone manual work, not to replace understanding.
- Use layout, comments, documentation, coding standards to lower reader effort. Self-documenting structure first. Comments explain intent, assumptions, constraints, limitations, usage, or non-obvious facts.

## Trigger rules

- When coding starts from proposed solution, restate requirement, architecture fit, risks, conventions, success constraints before implementing.
- When routine is hard to name, mixes phases, has flag arguments, long parameters, or hidden side effects, redesign interface or split routine.
- When readers must decode units, ranges, precision, encoding, ownership, status, magic values, or primitive flags, move meaning into names, constants, types, or structures.
- When input crosses user, file, network, external-system, or other trust boundary, decide what gets validated, rejected, recovered from, asserted, kept diagnosable.
- When branches, loops, recursion, exits, or exception paths get hard to verify, simplify before adding logic.
- When repeated branching maps stable categories, ranges, conversions, validation, dispatch, or configuration-like rules, consider validated table.
- When class or module exposes representation, becomes god object, or mixes unrelated responsibilities, restore abstraction boundary.
- When tests cover only happy path, add normal, boundary, invalid-input, defensive-check, routine-contract, data-driven edge cases.
- When debugging starts from guess, first make failure repeatable, collect evidence, isolate path, explain cause.
- When refactoring poorly understood or risky code, add tests or analysis first and keep behavior changes separate.
- When performance work begins, set target, measure current behavior, change one thing, remeasure, document any clarity tradeoff.
- When comments restate obvious mechanics or go stale, rewrite code or delete comment. When code cannot express intent, constraints, or usage, add close accurate comment.
- When local style diverges, follow shared formatting, naming, file-structure, idiom conventions instead of creating module-specific dialect.

## Final checklist

- Requirements, architecture fit, risks, conventions, construction approach clear enough.
- Names, routines, data, classes, layout, comments, standards cut reader effort.
- Inputs, errors, assertions, contracts, invariants, impossible states, trust boundaries deliberate.
- Control flow, loops, tables, recursion, exits, exception paths simple enough to inspect.
- Tests, reviews, debugging, refactoring, integration, tooling, tuning evidence-based.
- Change small enough to verify; survives careful review.
