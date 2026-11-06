# OBEY Refactoring.Guru

## When to use

Use as compact always-on bias for safe, smell-driven refactoring of existing code.

## Primary bias to correct

Refactoring is not cleanup for its own sake. Diagnose one smell, treat it with smallest behavior-preserving move, verify, stop.

## Decision rules

- Keep structural refactoring apart from feature and bug behavior. If behavior changes, name and isolate it.
- Diagnose smell before picking technique: symptom, cost, smallest treatment, verification, stop condition.
- Prefer small named transformations over broad redesign.
- Keep program runnable. Run relevant checks after risky movement, state-flow changes, public API changes, algorithm replacement.
- Stop once diagnosed smell is reduced. Do not chase new smells unless they block current change.
- Try extraction, naming, movement, inlining, encapsulation before adding hierarchy, polymorphism, wrappers or method objects.
- Do not abstract from coincidental similarity, random parameter bags, simple conditionals or speculative future needs.
- Keep behavior with data it changes unless separation intentionally supports interchangeable behavior.
- Preserve public compatibility when changing signatures, constructors, visibility, hierarchy or externally reachable code.
- Delete or inline dispensable code only after checking public, generated, reflected, serialized, plugin-facing, framework and test-only uses.

## Trigger rules

- When method needs comments or local-state reconstruction, extract named method after checking inputs, outputs, mutated variables.
- When class changes for unrelated reasons, extract that responsibility. Use subclass/interface only for stable variants or real client subsets.
- When conditionals repeat by type or state, isolate decision before polymorphism. Leave simple honest conditionals alone.
- When duplicate code appears third time, refactor unless similarity will likely diverge.
- When parameter lists or primitive clumps carry one concept, model it. Do not pass huge object to hide dependency.
- When clients navigate chains or internals, hide delegate or move behavior. Remove pass-through middle men that add no policy.
- Before null, error code, assertion, value/reference or association changes, verify semantics.

## Final checklist

- Smell named?
- Smallest treatment?
- Behavior verified?
- Stop condition reached?
- No hidden feature change?
- No speculative abstraction?
