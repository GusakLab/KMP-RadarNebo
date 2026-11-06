# OBEY Code Complete by Steve McConnell

## Purpose

Code generation, edits, and reviews must optimize for:
- low-defect construction
- readable, intention-revealing code
- controlled complexity
- defensive programming where appropriate
- strong routine and class design
- practical correctness over style theater

Binding policy: `MUST` binds, `SHOULD` is strong default, `MUST NOT` is forbidden.

---

## Primary Directive

Construction quality is not accidental.

When uncertain, pick option that:
1. lowers defect probability
2. eases inspection and reasoning
3. reduces control-flow complexity
4. uses data and routines clearly
5. guards against invalid states and misuse

Do not trade clarity for cleverness, minimal keystrokes, or fashionable idioms.

---

## Foundational Construction Rules

1. Write for human readers first.
2. Favor clarity, locality, explicitness over trickiness.
3. Make correctness easier than incorrectness.

---

## Construction Prerequisites and Decisions

1. Construction is not isolated typing. Confirm requirements, architecture, major risks, and conventions are clear enough for the change.
2. Settle major decisions before large implementation: language constraints, error policy, data representation, reuse, integration, testing.
3. Under upstream uncertainty, build small validated slice, not speculative code.
4. Keep software metaphor or design model only if it helps make concrete construction decisions.
5. Measure twice before cutting when early decision is costly to reverse.

---

## Pseudocode Programming Process

1. For complex routines, sketch precise pseudocode or comments before details.
2. Refine until pseudocode names real steps at consistent abstraction level.
3. Convert clear pseudocode to code. Keep only comments adding intent, constraints, or rationale.
4. Do not use pseudocode in place of understanding the algorithm.

---

## Routine Design Rules

1. One clear purpose per routine.
2. Routine name states result or action precisely.
3. Keep interface as small as practical.
4. Avoid long parameter lists and flag arguments.
5. Separate setup, validation, computation, side effects when conceptually different.
6. Make return values meaningful and hard to misuse.
7. Prefer guard clauses and straightforward structure over deep nesting.

Anti-patterns (MUST NOT):
- routines doing several unrelated things
- names describing implementation, not purpose
- many hidden side effects
- boolean parameters switching routine mode

---

## Variable and Data Rules

1. Names reveal purpose and meaning.
2. Keep scope as small as practical.
3. Initialize variables deliberately.
4. Prefer named constants or stable values for variables not meant to change.
5. Use stronger types when primitives hide meaning.

Anti-patterns (MUST NOT):
- loop/index/temp variables reused beyond purpose
- long-lived mutable locals with many meanings
- values with unclear units or semantics

---

## Data Type Rules

1. Pick types that make invalid or ambiguous values harder to represent.
2. Name constants for magic values, units, bounds, sentinel meanings.
3. Booleans only for true binary meanings; replace flag fields with clearer states when needed.
4. Enumerations or named alternatives for closed sets.
5. Arrays, records, maps, tables only where shape communicates meaning.
6. Encapsulate unusual data structures behind purpose-revealing routines or types.
7. Keep units, ranges, precision, encoding, ownership visible near affected data.

---

## Control Flow Rules

1. Prefer simplest control flow expressing the logic.
2. Replace complicated boolean logic with named predicates or clearer structure.
3. Use case/switch only when it improves clarity.
4. Eliminate impossible paths and dead branches.
5. Avoid surprising exits unless they clarify the routine.

Anti-patterns (MUST NOT):
- deeply nested conditionals
- complicated loop exits with hidden state changes
- control flow depending on side effects in expressions
- clever one-liners obscuring logic

---

## Statement, Conditional, and Loop Rules

1. Order straight-line code so dependencies precede use and related statements stay together.
2. Keep conditionals positive and direct when possible.
3. Put normal path where readers find it fast.
4. Give loops clear initialization, termination, update.
5. Keep loop bodies focused; extract work when loop hides several responsibilities.
6. Avoid unusual control structures unless clearer than ordinary ones.

---

## Defensive Programming Rules

1. Validate inputs at trust boundaries.
2. Assert or check invariants where programmer assumptions matter.
3. Separate recoverable conditions from programming errors.
4. Fail diagnosably.
5. Never silently continue from corrupted or impossible state.

Anti-patterns (MUST NOT):
- assuming all callers correct
- burying invalid state until distant failure
- swallowing exceptions without context

---

## Error Handling Rules

1. Handle errors at right abstraction level.
2. Preserve useful context.
3. Do not let error handling dominate normal path.
4. Standardize similar failure handling.
5. Prefer explicit, well-understood failure semantics over ad hoc conventions.

---

## Table-Driven and Data-Driven Rules

1. Prefer data-driven logic over long repeated condition chains when mapping is stable and explicit.
2. Use tables or maps for configuration-like decisions.
3. Keep structure obvious and validated.
4. Do not hide complex logic in inscrutable data encodings.

---

## Class and Module Design Rules

1. Each class or module owns one focused responsibility.
2. Separate interface from implementation.
3. Hide representation and incidental detail.
4. Keep classes cohesive.
5. Cut coupling with clear contracts and limited knowledge of internals.

Anti-patterns (MUST NOT):
- god classes
- modules mixing persistence, formatting, business logic, integration
- public surfaces exposing internal bookkeeping

---

## Complexity Management Rules

1. Treat rising complexity as defect risk.
2. Split large or tangled routines and modules.
3. Remove duplication that multiplies maintenance.
4. Pick designs that shrink what maintainer holds in working memory.

---

## Construction with Preconditions and Postconditions

1. Make routine assumptions explicit.
2. Encode key invariants near code they protect.
3. Keep contracts simple, testable.
4. Assertions for programmer mistakes, validation for external input, domain errors for expected business failures.

---

## Comment Rules

1. Comments explain intent, rationale, contracts, non-obvious facts.
2. Do not comment obvious code; improve it instead.
3. Keep comments accurate or delete them.

---

## Coding Standards Rules

1. Stay consistent within codebase.
2. Formatting, naming, and file structure serve readability.
3. Standardize common idioms so readers do not relearn style per module.
4. Shared convention beats local personal taste.

---

## Incremental Construction Rules

1. Build in small, verifiable increments.
2. Integrate often enough to surface conflicts and misunderstanding early.
3. Do not let partial work rot in long-lived isolation.
4. Review and improve code during construction, not only after.

---

## Quality, Collaboration, Debugging, and Refactoring

1. Scale reviews, inspections, pair work, tests, static checks to code risk.
2. Debugging is diagnosis: reproduce, isolate, explain, fix, verify. Do not guess.
3. Fix root cause when practical, not only symptom.
4. Add tests around defects so same failure is easier to detect next time.
5. Refactor when structure hides intent, duplicates knowledge, or raises defect probability.
6. Keep refactoring separate from behavior changes when that aids review.

---

## Performance, Integration, Tools, and Craftsmanship

1. Do not tune performance until requirement and evidence justify it.
2. When tuning, measure before and after. Keep clarity unless tradeoff is explicit.
3. Use tools, scripts, debuggers, profilers, editors, build automation to cut error-prone manual work.
4. Prefer self-documenting code. Document where code cannot express intent, constraints, or usage.
5. Personal discipline, curiosity, and withstanding careful review are part of construction quality.

---

## Review Rules

When reviewing, look for:
- unclear names
- weak routine boundaries
- long parameter lists
- unnecessary nesting
- hidden side effects
- poor defensive checks at trust boundaries
- duplicated logic
- confusing control flow
- god classes or mixed responsibilities
- comments compensating for poor structure

---

## Forbidden Patterns

### Cleverness over Clarity
- dense tricks hard to inspect
- compressed expressions saving lines but raising interpretation cost

### Routine Bloat
- one routine doing several phases and concerns
- long signatures with many unrelated parameters

### Defensive Vacuum
- no validation at trust boundaries
- no checks around critical assumptions
- silent fallback from impossible state

### Comment-as-Crutch
- obvious comments over bad code
- stale, misleading comments

### Consistency Neglect
- arbitrary naming and formatting changes
- module-specific mini dialects in one codebase

---

## Testing Rules

1. Test routines on normal, boundary, invalid inputs.
2. Test defensive checks where boundary validation matters.
3. Align tests with routine contracts.
4. Test complex data-driven logic with representative tables and edge cases.

---

## Review Checklist

Before finalizing any change, verify:
- Names clear, intention-revealing?
- Routines focused, reasonably small?
- Control flow straightforward?
- Trust boundaries defended?
- Contracts and invariants explicit enough?
- Complexity reduced, or at least not increased?
- Classes/modules cohesive?
- Cleverness that harms inspection avoided?
- Comments only where they add value?
- Style consistent with codebase?

If any answer is no, revise before shipping.
