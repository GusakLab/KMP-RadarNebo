# OBEY Refactoring.Guru

## When to use

Use when changing existing code where smells, technique choice, behavior preservation and cleanup scope matter.

## Primary bias to correct

Refactoring is not general cleanup or pattern application. It is small, smell-driven, behavior-preserving treatment, verified, with a stop condition.

## Decision rules

- Keep refactoring apart from feature work and bug fixes. Name any behavior change as such and isolate it from structural edits.
- Diagnose smell before picking technique: symptom, maintenance cost, scope, cleaner end state, verification path, stop condition.
- Use smallest treatment that directly reduces the smell. Escalate only when it is blocked.
- Use small named transformations, not broad redesign. Code stays runnable and understandable.
- Keep every intermediate state buildable and testable. Move one ownership boundary at a time.
- Prove behavior before editing. If no tests cover the code, first add characterization or golden-master tests of current behavior.
- Run relevant checks after risky moves, public interface changes, state-flow changes, algorithm substitution.
- Stop when named smell is gone or materially reduced. Log new smells separately unless they block current change.
- Rule of Three: tolerate uncertain duplication early. Refactor third similar occurrence unless similarity is coincidental.
- Technical debt compounds. Pay down debt that slows current change speed, correctness or team understanding.
- Scan smells by category: bloaters, object-orientation abusers, change preventers, dispensables, couplers, incomplete library gaps.
- Bloaters: try extraction, parameter/data modeling and responsibility splits before method objects, subclasses or interfaces.
- Switch/type-code smells: isolate decision first. Use polymorphism, subclasses or state/strategy only for stable, repeated variation.
- Change preventers: move behavior and data to owner of changing concept, so one conceptual change has one main edit site.
- Dispensables: delete or inline unused structure, but first check public, generated, reflected, serialized, plugin-facing and framework extension uses.
- Couplers: reduce navigation and private knowledge. Keep delegating layers only if they hide volatile structure, policy or a real boundary.
- Comments are for rationale, constraints, contracts, hard algorithms. If a comment explains unclear code, use names, variables, methods or assertions instead.
- Keep behavior with data it changes unless separation deliberately supports interchangeable behavior.
- Getters and setters do not finish encapsulation. Move behavior inward when callers still manipulate exposed data.
- Avoid speculative abstractions: no wrappers, parameter objects, interfaces, superclasses or hierarchy variants without real concept or client.
- When changing signatures, constructors, visibility, type hierarchy or externally reachable APIs, preserve public compatibility or give transition path.
- Before extraction or movement, identify inputs, outputs, mutated variables, callers, visibility, construction paths, invariants.
- Before condition consolidation or algorithm substitution, verify side effects, ordering, truth tables, edge cases, performance-sensitive behavior.
- Before data reorganization, decide identity, value/reference semantics, mutability, equality, lifecycle ownership, association direction, synchronization.
- Before generalization, prove shared behavior is real. Preserve substitutability; avoid inheriting unused behavior.
- Choose exceptions deliberately: simple conditional, useful comment, intentional strategy separation, small extension point or clear duplication may beat mechanical treatment.

## Trigger rules

- When method needs comments, scrolling or local-state reconstruction, try `Extract Method`. If locals block it, use `Replace Temp with Query`, `Introduce Parameter Object` or `Preserve Whole Object`.
- When class has multiple reasons to change, use `Extract Class`. Extract subclass/interface only for stable variants or real client-facing subsets.
- When primitives, arrays, magic numbers or type codes carry meaning, model the concept only if it adds naming, validation, behavior or safer variation handling.
- When parameter list outgrows local reasoning, replace derived parameters or preserve whole object. Introduce parameter object only for a real recurring concept.
- When one change needs edits across many files, move methods/fields or extract ownership to centralize that knowledge.
- When client code navigates object chains, hide delegate or move behavior closer to data. Do not add pure forwarding.
- When class mostly forwards, remove middle man unless it protects boundary policy or volatile structure.
- When method both queries and mutates, separate query from modifier unless atomic read-modify is the public contract.
- When branches repeat behavior, decompose, consolidate or move duplicate fragments only after checking side effects and execution order.
- When null checks dominate, introduce null object only if absence can obey same interface. Keep absence explicit when it is an error.
- When inheritance creates refused bequest or intimacy, push members down or replace inheritance with delegation.
- Before deleting dead or speculative code, verify external reachability and test-only access.
- When library class is incomplete, use foreign method for one narrow gap. Use local extension only for substantial repeated gaps.
- When cleanup keeps expanding, stop at diagnosed smell and report next smell separately.

## Final checklist

- Change clearly refactoring, feature work or bug fix?
- Which smell diagnosed, and what cost did it create?
- Smallest suitable treatment tried before riskier structure?
- Behavior preserved under relevant checks?
- Named smell materially better?
- No speculative abstraction or mechanical pattern use?
- Public compatibility, state flow, ownership checked?
- Intentionally untreated smell documented, not hidden?
